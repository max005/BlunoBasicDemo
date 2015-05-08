package com.example.blunobasicdemo.notification;

/**
 * Created by Max005 on 2015/4/27.
 */
import android.app.Notification;
import android.support.v4.app.NotificationCompat;

import com.example.blunobasicdemo.R;

/**
 * Collection of notification priority presets.
 */
public class PriorityPresets {
    public static final PriorityPreset MIN = new SimplePriorityPreset(R.string.min_priority, Notification.PRIORITY_MIN);
    public static final PriorityPreset LOW = new SimplePriorityPreset(R.string.low_priority, Notification.PRIORITY_LOW);
    public static final PriorityPreset DEFAULT = new SimplePriorityPreset(R.string.default_priority, Notification.PRIORITY_DEFAULT);
    public static final PriorityPreset HIGH = new SimplePriorityPreset(R.string.high_priority, Notification.PRIORITY_HIGH);
    public static final PriorityPreset MAX = new SimplePriorityPreset(R.string.max_priority, Notification.PRIORITY_MAX);

    public static final PriorityPreset[] PRESETS = new PriorityPreset[] {
            MIN,
            LOW,
            DEFAULT,
            HIGH,
            MAX
    };

    /**
     * Simple notification priority preset that sets a priority using
     * {@link android.support.v4.app.NotificationCompat.Builder#setPriority}
     */
    private static class SimplePriorityPreset extends PriorityPreset {
        private final int mPriority;

        public SimplePriorityPreset(int nameResId, int priority) {
            super(nameResId);
            mPriority = priority;
        }

        @Override
        public void apply(NotificationCompat.Builder builder,
                          NotificationCompat.WearableExtender wearableOptions) {
            builder.setPriority(mPriority);
        }
    }
}
