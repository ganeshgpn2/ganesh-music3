package np.ganesh.sangeet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONObject;

/** Fires at the alarm time (even when the app is closed / phone locked) and starts the radio service. */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        String id = in.getStringExtra("id");
        if (id == null) return;
        JSONObject a = null;
        JSONArray l = AlarmStore.list(c);
        for (int i = 0; i < l.length(); i++) {
            JSONObject o = l.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) { a = o; break; }
        }
        if (a == null) return;

        Intent s = new Intent(c, AlarmPlayService.class)
                .putExtra("url", a.optString("url"))
                .putExtra("name", a.optString("name"))
                .putExtra("dur", a.optInt("dur", 0));
        try { ContextCompat.startForegroundService(c, s); } catch (Exception e) { /* ignore */ }

        JSONArray days = a.optJSONArray("days");
        if (days != null && days.length() > 0) {
            AlarmScheduler.schedule(c, a, System.currentTimeMillis() + 60000L);   // repeat
        } else {
            AlarmStore.remove(c, id);          // once: finished
            AlarmStore.addFired(c, id);        // web page will switch it off when opened
        }
    }
}
