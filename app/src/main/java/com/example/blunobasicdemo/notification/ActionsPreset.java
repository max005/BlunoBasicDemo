package com.example.blunobasicdemo.notification;

/**
 * Created by HuangYuChang on 15/4/27.
 */import android.content.Context;
import android.support.v4.app.NotificationCompat;

/**
 * Base class for notification actions presets.
 */
public abstract class ActionsPreset extends NamedPreset {
    public ActionsPreset(int nameResId) {
        super(nameResId);
    }

    /** Apply the priority to a notification builder */
    public abstract void apply(Context context, NotificationCompat.Builder builder,
                               NotificationCompat.WearableExtender wearableOptions);
}
