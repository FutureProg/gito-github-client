package com.dmitrymalkovich.android.githubanalytics.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import com.dmitrymalkovich.android.githubanalytics.R;

import static android.content.Context.NOTIFICATION_SERVICE;

public class NotificationsManager {

    private static NotificationsManager instance;

    public static synchronized NotificationsManager getInstance() {
        if (instance == null) {
            instance = new NotificationsManager();
        }
        return instance;
    }

    @SuppressWarnings("deprecation")
    public void sendNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
        Notification notification  = new Notification.Builder(context)
                .setContentTitle("You have a new notification!")
                .setContentText("material-design-dimens was starred")
                .setSmallIcon(R.drawable.ic_star_black_24dp)
                .setAutoCancel(true).getNotification();
        notificationManager.notify(0, notification);
    }
}
