package np.ganesh.sangeet;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/** Keeps the radio alarms (sent from the web page) so they survive app close / reboot. */
public class AlarmStore {
    static SharedPreferences sp(Context c) { return c.getSharedPreferences("sgs_alarms", Context.MODE_PRIVATE); }

    static JSONArray list(Context c) {
        try { return new JSONArray(sp(c).getString("list", "[]")); } catch (Exception e) { return new JSONArray(); }
    }
    static void save(Context c, JSONArray a) { sp(c).edit().putString("list", a.toString()).apply(); }

    static void remove(Context c, String id) {
        JSONArray old = list(c), nw = new JSONArray();
        for (int i = 0; i < old.length(); i++) {
            JSONObject o = old.optJSONObject(i);
            if (o != null && !id.equals(o.optString("id"))) nw.put(o);
        }
        save(c, nw);
    }

    static void addFired(Context c, String id) {
        JSONArray f;
        try { f = new JSONArray(sp(c).getString("fired", "[]")); } catch (Exception e) { f = new JSONArray(); }
        f.put(id);
        sp(c).edit().putString("fired", f.toString()).apply();
    }
    static JSONArray popFired(Context c) {
        JSONArray f;
        try { f = new JSONArray(sp(c).getString("fired", "[]")); } catch (Exception e) { f = new JSONArray(); }
        sp(c).edit().putString("fired", "[]").apply();
        return f;
    }
}
