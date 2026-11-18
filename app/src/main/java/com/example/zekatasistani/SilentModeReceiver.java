package com.example.zekatasistani;
import android.app.NotificationManager; import android.content.BroadcastReceiver; import android.content.Context; import android.content.Intent; import android.content.SharedPreferences; import android.os.Build; import java.util.HashSet; import java.util.Set;
public class SilentModeReceiver extends BroadcastReceiver {
    private static final String PREFS = "prayer_silent_windows";
    private static final String ACTIVE = "active_windows";
    @Override public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT < 23) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || !manager.isNotificationPolicyAccessGranted()) return;
        String key = intent.getStringExtra("windowKey"); if (key == null || key.isEmpty()) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> active = new HashSet<>(prefs.getStringSet(ACTIVE, new HashSet<>()));
        boolean wasActive = active.contains(key);
        if ("start".equals(intent.getAction())) {
            if (active.isEmpty()) prefs.edit().putInt("previous_filter", manager.getCurrentInterruptionFilter()).apply();
            active.add(key);
        } else if (wasActive) active.remove(key);
        prefs.edit().putStringSet(ACTIVE, active).apply();
        if (!active.isEmpty()) manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
        else if (wasActive) manager.setInterruptionFilter(prefs.getInt("previous_filter", NotificationManager.INTERRUPTION_FILTER_ALL));
    }
}