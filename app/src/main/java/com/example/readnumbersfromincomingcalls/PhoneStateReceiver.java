package com.example.readnumbersfromincomingcalls;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.Manifest;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class PhoneStateReceiver extends BroadcastReceiver {


    public static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "incoming_call_channel";
    private View popupView;
    private WindowManager windowManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            if (incomingNumber != null) {
                Log.d("PhoneStateReceiver", "Incoming number: " + incomingNumber);
                showNotification(context, incomingNumber);
                showPopupOverlay(context, incomingNumber);
            }
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            // Call answered
            removeNotification(context);
            removePopupOverlay();
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            // Call ended or declined
            removeNotification(context);
            removePopupOverlay();
        }
    }

    private void showNotification(Context context, String incomingNumber) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Incoming Call Channel";
            String description = "Channel for incoming call notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("incoming_number", incomingNumber);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Add action to dismiss the notification
        Intent dismissIntent = new Intent(context, DismissNotificationReceiver.class);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, 0, dismissIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Incoming Call")
                .setContentText("Incoming number: " + incomingNumber)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_cancel, "Dismiss", dismissPendingIntent);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void removeNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void showPopupOverlay(Context context, String incomingNumber) {
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        if (popupView != null) {
            windowManager.removeView(popupView);
        }

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        popupView = inflater.inflate(R.layout.popup_overlay, null);

        TextView popupTextView = popupView.findViewById(R.id.popupTextView);
        popupTextView.setText("Incoming call from: " + incomingNumber);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT);

        params.x = 0;
        params.y = 50;

        // Check if we have the overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            windowManager.addView(popupView, params);
        }
    }

    private void removePopupOverlay() {
        if (popupView != null && windowManager != null) {
            windowManager.removeView(popupView);
            popupView = null;
        }
    }
}
