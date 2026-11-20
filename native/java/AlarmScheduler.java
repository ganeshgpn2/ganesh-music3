package np.ganesh.sangeet;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;

public class AlarmScheduler {
    static final String ACTION = "np.ganesh.sangeet.ALARM";

    static PendingIntent pi(Context c, String id) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION).putExtra("id", id);
        return PendingIntent.getBroadcast(c, id.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** next trigger time (ms) strictly after 'after'; days use JS getDay(): 0=Sunday..6=Saturday; empty = every day / once */
    static long next(JSONObject a, long after) {
        try {
            String[] t = a.optString("time", "06:00").split(":");
            int h = Integer.parseInt(t[0].trim()), m = Integer.parseInt(t[1].trim());
            JSONArray days = a.optJSONArray("days");
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(after);
            c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, m); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
            for (int i = 0; i < 9; i++) {
                boolean dayOk = days == null || days.length() == 0;
                if (!dayOk) {
                    int wd = c.get(Calendar.DAY_OF_WEEK) - 1;
                    for (int k = 0; k < days.length(); k++) if (days.optInt(k, -1) == wd) { dayOk = true; break; }
                }
                if (dayOk && c.getTimeInMillis() > after) return c.getTimeInMillis();
                c.add(Calendar.DAY_OF_MONTH, 1);
            }
        } catch (Exception e) { /* ignore */ }
        return -1;
    }

    static void schedule(Context c, JSONObject a, long after) {
        String id = a.optString("id");
        if (id.isEmpty()) return;
        long t = next(a, after);
        if (t < 0) return;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent show = null;
        Intent launch = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        if (launch != null) show = PendingIntent.getActivity(c, 0, launch, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(t, show), pi(c, id));
        } catch (Exception e) { /* ignore */ }
    }

    static void cancel(Context c, String id) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi(c, id));
    }

    static void rescheduleAll(Context c) {
        JSONArray l = AlarmStore.list(c);
        long now = System.currentTimeMillis();
        for (int i = 0; i < l.length(); i++) {
            JSONObject o = l.optJSONObject(i);
            if (o != null) schedule(c, o, now);
        }
    }
}
