package net.kcci.HomeIot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.StringDef;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.StringDef;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    String TAG = "MyFirebaseMessagingService";
    private static final String GROUP_NOTI_KSH = "notiKSH";
    public MyFirebaseMessagingService() {
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "onNewToken 호출됨 : " + token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "onMessageReceived 호출됨.");
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

        NotificationCompat.Builder builder = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(Channel.HOME) == null) {
                NotificationChannel channel = new NotificationChannel(Channel.HOME, Channel.HOME, NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }
            builder = new NotificationCompat.Builder(getApplicationContext(), Channel.HOME);
        }else {
            builder = new NotificationCompat.Builder(getApplicationContext());
        }

        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();

        builder.setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_launcher_background);

        Notification notification = builder.build();
        notificationManager.notify(1, notification);
        sendToActivity(getApplicationContext(), title, body);
        sendNotification(getApplicationContext(),title, body);
    }

    private void sendToActivity(Context context, String collapse_key, String contents) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("from", collapse_key);
        intent.putExtra("contents", contents);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }
    private void sendNotification(Context context, String collapsekey, String contents) {
        String channel;
        int id;
        Log.d(TAG,collapsekey);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("from", collapsekey);
        intent.putExtra("contents", contents);
        PendingIntent pi = PendingIntent.getActivity(this,  0 /* Request code */, intent,  PendingIntent.FLAG_ONE_SHOT);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(collapsekey.equals("Home")) {
            channel = Channel.HOME;
            id = 1;
        }
        else
        {
            channel = Channel.COMMENT;
            id = 2;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel);
        builder = builder
                .setSmallIcon(R.drawable.home_24)
                .setContentTitle(context.getString(R.string.app_name))
                .setTicker(collapsekey)
                .setContentText(collapsekey +":"+ contents)
                .setContentIntent(pi)
                .setAutoCancel(true);
        notificationManager.notify(id, builder.build());
    }
    public static void createChannel(Context context) {
        NotificationChannelGroup group1 = new NotificationChannelGroup(GROUP_NOTI_KSH, GROUP_NOTI_KSH);
        getManager(context).createNotificationChannelGroup(group1);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel notificationChannel = new NotificationChannel(Channel.HOME, context.getString(R.string.notification_channel_home_title), importance);
        notificationChannel.setDescription(context.getString(R.string.notification_channel_home_description));
        notificationChannel.setGroup(GROUP_NOTI_KSH);
        notificationChannel.setLightColor(Color.GREEN);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        getManager(context).createNotificationChannel(notificationChannel);

        NotificationChannel channelComment = new NotificationChannel(Channel.COMMENT, context.getString(R.string.notification_channel_comment_title), importance);
        channelComment.setDescription(context.getString(R.string.notification_channel_comment_description));
        channelComment.setGroup(GROUP_NOTI_KSH);
        channelComment.setLightColor(Color.BLUE);
        channelComment.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        getManager(context).createNotificationChannel(channelComment);
    }
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            Channel.HOME,
            Channel.COMMENT,
            Channel.NOTICE
    })
    public @interface Channel {
        String HOME = "Home";
        String COMMENT = "comment";
        String NOTICE = "notice";
    }

    private static android.app.NotificationManager getManager(Context context) {
        return (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
