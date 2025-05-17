package com.example.pocketguard.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.pocketguard.R

object NotificationUtils {
    private const val TAG = "NotificationUtils"
    private const val CHANNEL_ID = "budget_channel"
    private const val CHANNEL_NAME = "Budget Notifications"
    private const val CHANNEL_DESCRIPTION = "Notifications for budget updates and alerts"

    fun showNotification(context: Context, title: String, message: String, id: Int) {
        try {
            Log.d(TAG, "Attempting to show notification: $title")
            Log.d(TAG, "Notification message: $message")
            Log.d(TAG, "Notification ID: $id")
            
            createNotificationChannel(context)

            // Check permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionStatus = ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                )
                Log.d(TAG, "Notification permission status: $permissionStatus")
                
                if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Notification permission not granted")
                    return
                }
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_circle_notifications_24)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(longArrayOf(0, 500, 200, 500))

            Log.d(TAG, "Notification built successfully")

            try {
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.notify(id, builder.build())
                Log.d(TAG, "Notification sent successfully: $title")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending notification: ${e.message}", e)
                e.printStackTrace()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Log.d(TAG, "Creating notification channel")
                
                // Get the default notification sound
                val soundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                Log.d(TAG, "Notification sound URI: $soundUri")
                
                // Create audio attributes for the notification sound
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                    setSound(soundUri, audioAttributes)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                }

                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channel: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }
} 