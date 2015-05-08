package com.example.blunobasicdemo.notification;

/**
 * Created by HuangYuChang on 15/4/27.
 */
import android.support.v4.app.NotificationCompat;
/**
 * Base class for notification priority presets.
 */
public abstract class PriorityPreset extends NamedPreset {
    public PriorityPreset(int nameResId) {
        super(nameResId);
    }

    /** Apply the priority to a notification builder */
    public abstract void apply(NotificationCompat.Builder builder,
                               NotificationCompat.WearableExtender wearableOptions);
}
