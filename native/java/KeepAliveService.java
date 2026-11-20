package np.ganesh.sangeet;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;

/** Keeps the app alive while music / radio plays so Android does not stop it when the screen is locked. */
public class KeepAliveService extends Service {
    static final String CH = "sgs_play";
    static final int NID = 7102;
    private PowerManager.WakeLock wl;
    private WifiManager.WifiLock wf;

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent in, int flags, int startId) {
        String title = in != null && in.getStringExtra("title") != null ? in.getStringExtra("title") : "";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CH) == null) {
                NotificationChannel ch = new NotificationChannel(CH, "Now playing", NotificationManager.IMPORTANCE_LOW);
                nm.createNotificationChannel(ch);
            }
        }
        Intent open = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent op = open == null ? null : PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new NotificationCompat.Builder(this, CH)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("श्री गणेश संगीत यन्त्र")
                .setContentText(title.isEmpty() ? "बज्दैछ" : title)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(op)
                .build();
        if (Build.VERSION.SDK_INT >= 29) startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(NID, n);
        try {
            if (wl == null) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sgs:play");
                wl.setReferenceCounted(false);
            }
            if (!wl.isHeld()) wl.acquire(12L * 60 * 60 * 1000L);
            if (wf == null) {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                wf = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "sgs:playwifi");
                wf.setReferenceCounted(false);
            }
            if (!wf.isHeld()) wf.acquire();
        } catch (Exception e) { /* ignore */ }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        try { if (wl != null && wl.isHeld()) wl.release(); } catch (Exception e) { /* ignore */ }
        try { if (wf != null && wf.isHeld()) wf.release(); } catch (Exception e) { /* ignore */ }
        super.onDestroy();
    }
}
