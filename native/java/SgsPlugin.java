package np.ganesh.sangeet;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

/** Bridge between the web page (window.Capacitor.Plugins.Sgs) and Android alarms / services. */
@CapacitorPlugin(name = "Sgs")
public class SgsPlugin extends Plugin {

    /** replace all native radio alarms with the list from the web page */
    @PluginMethod
    public void syncAlarms(PluginCall call) {
        Context c = getContext();
        try {
            JSONArray old = AlarmStore.list(c);
            for (int i = 0; i < old.length(); i++) {
                JSONObject o = old.optJSONObject(i);
                if (o != null) AlarmScheduler.cancel(c, o.optString("id"));
            }
            JSArray arr = call.getArray("alarms");
            JSONArray nw = new JSONArray();
            long now = System.currentTimeMillis();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    nw.put(o);
                    AlarmScheduler.schedule(c, o, now);
                }
            }
            AlarmStore.save(c, nw);
            JSObject r = new JSObject();
            r.put("count", nw.length());
            call.resolve(r);
        } catch (Exception e) {
            call.reject("syncAlarms: " + e.getMessage());
        }
    }

    /** ids of one-time alarms that already rang natively */
    @PluginMethod
    public void popFired(PluginCall call) {
        try {
            JSObject r = new JSObject();
            r.put("ids", new JSArray(AlarmStore.popFired(getContext()).toString()));
            call.resolve(r);
        } catch (Exception e) {
            call.reject("popFired: " + e.getMessage());
        }
    }

    @PluginMethod
    public void keepAlive(PluginCall call) {
        Context c = getContext();
        boolean on = Boolean.TRUE.equals(call.getBoolean("on", false));
        try {
            if (on) {
                Intent i = new Intent(c, KeepAliveService.class).putExtra("title", call.getString("title", ""));
                ContextCompat.startForegroundService(c, i);
            } else {
                c.stopService(new Intent(c, KeepAliveService.class));
            }
        } catch (Exception e) { /* background start not allowed - ignore */ }
        call.resolve();
    }

    @PluginMethod
    public void stopAlarmPlay(PluginCall call) {
        try { getContext().stopService(new Intent(getContext(), AlarmPlayService.class)); } catch (Exception e) { /* ignore */ }
        call.resolve();
    }

    @PluginMethod
    public void batterySettings(PluginCall call) {
        Context c = getContext();
        try {
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + c.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception e) {
            try {
                Intent i = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
            } catch (Exception e2) { /* ignore */ }
        }
        call.resolve();
    }

    @PluginMethod
    public void info(PluginCall call) {
        Context c = getContext();
        JSObject r = new JSObject();
        r.put("sdk", Build.VERSION.SDK_INT);
        r.put("notifications", NotificationManagerCompat.from(c).areNotificationsEnabled());
        boolean ign = false;
        if (Build.VERSION.SDK_INT >= 23) {
            PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
            ign = pm != null && pm.isIgnoringBatteryOptimizations(c.getPackageName());
        }
        r.put("batteryUnrestricted", ign);
        call.resolve(r);
    }
}
