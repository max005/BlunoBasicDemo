package com.example.blunobasicdemo.notification;

import android.app.Notification;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import com.example.blunobasicdemo.R;

/**
 * Created by Max005 on 2015/4/27.
 */

public class NotificationPresets {
    public static final NotificationPreset BASIC = new BasicNotificationPreset();

    private static NotificationCompat.Builder applyBasicOptions(Context context,
                                                                NotificationCompat.Builder builder, NotificationCompat.WearableExtender wearableOptions,
                                                                NotificationPreset.BuildOptions options) {
        builder.setContentTitle(options.titlePreset)
                .setContentText(options.textPreset)
                .setSmallIcon(R.drawable.ic_launcher);
        options.actionsPreset.apply(context, builder, wearableOptions);
        options.priorityPreset.apply(builder, wearableOptions);
        if (options.includeLargeIcon) {
            builder.setLargeIcon(BitmapFactory.decodeResource(
                    context.getResources(), R.drawable.warning));
        }
        if (options.isLocalOnly) {
            builder.setLocalOnly(true);
        }
        if (options.vibrate) {
            builder.setVibrate(new long[] {0, 100, 50, 100} );
        }
        return builder;
    }

    private static class BasicNotificationPreset extends NotificationPreset {
        public BasicNotificationPreset() {
            super(R.string.basic_example, R.string.example_content_title,
                    R.string.example_content_text);
        }

        @Override
        public Notification[] buildNotifications(Context context, BuildOptions options) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            NotificationCompat.WearableExtender wearableOptions =
                    new NotificationCompat.WearableExtender();
            applyBasicOptions(context, builder, wearableOptions, options);
            builder.extend(wearableOptions);
            return new Notification[] { builder.build() };
        }
    }
}
