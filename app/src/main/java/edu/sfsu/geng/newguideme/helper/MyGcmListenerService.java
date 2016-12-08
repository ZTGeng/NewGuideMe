package edu.sfsu.geng.newguideme.helper;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmListenerService;

import java.util.Date;

import edu.sfsu.geng.newguideme.R;

public class MyGcmListenerService extends GcmListenerService {
    private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String senderName = data.getString("sender");
        String roomId = data.getString("room_id");
//        String des = data.getString("des");
//        String rate = data.getString("rate"); // <- this is VI's rate
        long time = data.getLong("time");
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Sender: " + senderName + " roomId: " + roomId + " time: " + (new Date(time)).toString());

//        if (from.startsWith("/topics/")) {
//            // message received from some topic.
//        } else {
//            // normal downstream message.
//        }

        // [START_EXCLUDE]
        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */
        sendNotification(senderName, roomId);
        // [END_EXCLUDE]
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param senderName GCM message received.
     * @param roomId GCM message received.
     */
    private void sendNotification(final String senderName, final String roomId) {
        Intent helperWaitActivity = new Intent(MyGcmListenerService.this, HelperWaitActivity.class);
        helperWaitActivity.putExtra("blindId", roomId);
        helperWaitActivity.putExtra("blindName", senderName);
        helperWaitActivity.putExtra("needJoin", true);
        helperWaitActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(MyGcmListenerService.this, 0 /* Request code */,
                helperWaitActivity, PendingIntent.FLAG_ONE_SHOT);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MyGcmListenerService.this)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle("GuideMe Message")
                .setContentText(String.format(getString(R.string.notify_message), senderName))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());

    }
}
