package com.example.afinal.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.afinal.MainActivity
import com.example.afinal.R // Đảm bảo import R để lấy icon app
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent!!.hasError()) {
            Log.e("GeofenceReceiver", "Lỗi Geofence: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        // Nếu sự kiện là đi VÀO vùng (ENTER)
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            triggeringGeofences?.forEach { geofence ->
                val locationId = geofence.requestId
                // Gửi thông báo cho từng địa điểm kích hoạt
                sendNotification(context, locationId)
            }
        }
    }

    private fun sendNotification(context: Context, locationId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "student_stories_channel"

        // 1. Tạo Notification Channel (Bắt buộc cho Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Student Stories Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Tạo Intent để mở MainActivity khi bấm vào thông báo
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Truyền ID địa điểm qua extra để MainActivity xử lý
            putExtra("notification_story_id", locationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            locationId.hashCode(), // RequestCode riêng biệt để không bị ghi đè
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // 3. Xây dựng thông báo
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Icon app của bạn
            .setContentTitle("Bạn đang ở gần một địa điểm thú vị!")
            .setContentText("Chạm để nghe câu chuyện tại $locationId") // Bạn có thể map ID sang tên thật nếu muốn phức tạp hơn
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Bấm xong tự biến mất
            .build()

        notificationManager.notify(locationId.hashCode(), notification)
    }
}