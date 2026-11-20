package np.ganesh.sangeet;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;

/** Plays the alarm radio natively (works with screen locked / app closed) and stops itself after the set time. */
public class AlarmPlayService extends Service {
    static final String CH = "sgs_alarm";
    static final int NID = 7101;
    static final String STOP = "np.ganesh.sangeet.STOP_ALARM";

    private MediaPlayer mp;
    private Ringtone tone;
    private final Handler h = new Handler(Looper.getMainLooper());
    private PowerManager.WakeLock wl;
    private WifiManager.WifiLock wf;
    private String url = "", name = "";
    private int retries = 0;
    private boolean stopped = false;
    private final Runnable stopR = new Runnable() { @Override public void run() { finish(); } };

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent in, int flags, int startId) {
        if (in != null && STOP.equals(in.getAction())) { finish(); return START_NOT_STICKY; }
        url = in != null && in.getStringExtra("url") != null ? in.getStringExtra("url") : "";
        name = in != null && in.getStringExtra("name") != null ? in.getStringExtra("name") : "Radio";
        int dur = in != null ? in.getIntExtra("dur", 0) : 0;

        createChannel();
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 29) startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(NID, n);

        stopped = false; retries = 0;
        locks();
        audible();
        startPlayer();
        h.removeCallbacks(stopR);
        long ms = dur > 0 ? dur * 60000L : 3L * 60 * 60 * 1000L;   // no time set: max 3 hours
        h.postDelayed(stopR, ms);
        return START_NOT_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CH) == null) {
                NotificationChannel ch = new NotificationChannel(CH, "Alarm radio", NotificationManager.IMPORTANCE_HIGH);
                ch.setSound(null, null);
                nm.createNotificationChannel(ch);
            }
        }
    }

    private Notification buildNotification() {
        Intent open = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent op = open == null ? null : PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent st = new Intent(this, AlarmPlayService.class).setAction(STOP);
        PendingIntent sp = PendingIntent.getService(this, 1, st, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Builder(this, CH)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("अलार्म · रेडियो बज्दैछ")
                .setContentText(name)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(op)
                .addAction(android.R.drawable.ic_media_pause, "बन्द गर्नुहोस्", sp)
                .build();
    }

    private void locks() {
        try {
            if (wl == null) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sgs:alarm");
                wl.setReferenceCounted(false);
            }
            if (!wl.isHeld()) wl.acquire(4L * 60 * 60 * 1000L);
            if (wf == null) {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                wf = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "sgs:alarmwifi");
                wf.setReferenceCounted(false);
            }
            if (!wf.isHeld()) wf.acquire();
        } catch (Exception e) { /* ignore */ }
    }

    /** make sure the alarm volume is not near zero, otherwise the user will not hear anything */
    private void audible() {
        try {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            int cur = am.getStreamVolume(AudioManager.STREAM_ALARM);
            if (cur < max * 0.4) am.setStreamVolume(AudioManager.STREAM_ALARM, (int) Math.ceil(max * 0.6), 0);
        } catch (Exception e) { /* ignore */ }
    }

    private AudioAttributes attrs() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
    }

    private void startPlayer() {
        if (stopped) return;
        releasePlayer();
        if (url == null || url.isEmpty()) { fallbackTone(); return; }
        try {
            mp = new MediaPlayer();
            mp.setAudioAttributes(attrs());
            mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mp.setDataSource(url);
            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override public void onPrepared(MediaPlayer p) { if (!stopped) { retries = 0; p.start(); } }
            });
            mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override public boolean onError(MediaPlayer p, int what, int extra) { retry(); return true; }
            });
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer p) { retry(); }
            });
            mp.prepareAsync();
        } catch (Exception e) {
            retry();
        }
    }

    private void retry() {
        if (stopped) return;
        if (retries++ < 8) {
            h.postDelayed(new Runnable() { @Override public void run() { startPlayer(); } }, 3000);
        } else {
            fallbackTone();     // radio could not be played: ring the normal alarm tone so the user still wakes up
        }
    }

    private void fallbackTone() {
        if (stopped || tone != null) return;
        try {
            Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (u == null) u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            tone = RingtoneManager.getRingtone(this, u);
            if (tone != null) {
                tone.setAudioAttributes(attrs());
                if (Build.VERSION.SDK_INT >= 28) tone.setLooping(true);
                tone.play();
            }
        } catch (Exception e) { /* ignore */ }
    }

    private void releasePlayer() {
        try { if (mp != null) { mp.setOnCompletionListener(null); mp.setOnErrorListener(null); mp.release(); } } catch (Exception e) { /* ignore */ }
        mp = null;
    }

    private void finish() {
        stopped = true;
        h.removeCallbacksAndMessages(null);
        releasePlayer();
        try { if (tone != null) tone.stop(); } catch (Exception e) { /* ignore */ }
        tone = null;
        try { if (wl != null && wl.isHeld()) wl.release(); } catch (Exception e) { /* ignore */ }
        try { if (wf != null && wf.isHeld()) wf.release(); } catch (Exception e) { /* ignore */ }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopped = true;
        h.removeCallbacksAndMessages(null);
        releasePlayer();
        try { if (tone != null) tone.stop(); } catch (Exception e) { /* ignore */ }
        try { if (wl != null && wl.isHeld()) wl.release(); } catch (Exception e) { /* ignore */ }
        try { if (wf != null && wf.isHeld()) wf.release(); } catch (Exception e) { /* ignore */ }
        super.onDestroy();
    }
}
