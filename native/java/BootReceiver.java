package np.ganesh.sangeet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** After phone restart / app update, set the saved alarms again. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        AlarmScheduler.rescheduleAll(c);
    }
}
