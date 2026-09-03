package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class SoundMeshService : Service() {

    private val binder = LocalBinder()
    private val channelId = "soundmesh_playback_channel"
    private val notificationId = 101

    inner class LocalBinder : Binder() {
        fun getService(): SoundMeshService = this@SoundMeshService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "SoundMesh Active"
        val status = intent?.getStringExtra(EXTRA_STATUS) ?: "Multi-speaker mesh streaming active"

        if (action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification(title, status)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Use mediaPlayback or mediaProjection based on mode
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            }
            startForeground(notificationId, notification, foregroundServiceType)
        } else {
            startForeground(notificationId, notification)
        }

        return START_STICKY
    }

    fun updateStatus(title: String, status: String) {
        val notification = buildNotification(title, status)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(notificationId, notification)
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_soundmesh_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SoundMesh Audio Stream",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps wireless speaker mesh active in background"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START = "com.example.soundmesh.START"
        const val ACTION_STOP = "com.example.soundmesh.STOP"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_STATUS = "extra_status"
    }
}
