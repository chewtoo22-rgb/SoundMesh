package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.example.MainActivity
import com.example.R
import com.example.model.AudioSourceType
import com.example.viewmodel.SoundMeshViewModel

/** The started service owns the mesh for its entire lifetime, including Activity recreation. */
class SoundMeshService : Service(), ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
    private val binder = LocalBinder()
    private val channelId = "soundmesh_playback_channel"
    private val notificationId = 101
    private var foregroundTypes = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
    private var foregroundActive = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var controllerCreated = false

    val controller: SoundMeshViewModel by lazy {
        controllerCreated = true
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SoundMeshViewModel(application, ::prepareForeground) as T
        }
        ViewModelProvider(this, factory)[SoundMeshViewModel::class.java]
    }

    inner class LocalBinder : Binder() {
        fun getService(): SoundMeshService = this@SoundMeshService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SoundMesh:streaming")
            .apply { setReferenceCounted(false) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            if (controllerCreated) controller.stopMesh()
            wakeLock?.let { if (it.isHeld) it.release() }
            foregroundActive = false
            foregroundTypes = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        // Reopening the Activity must not downgrade an already active projection/mic.
        promoteForeground(foregroundTypes)
        return START_NOT_STICKY // Capture consent cannot survive process death.
    }

    private fun promoteForeground(types: Int) {
        val notification = buildNotification(getString(R.string.app_name), "SoundMesh active • tap to open")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification, types)
        } else {
            startForeground(notificationId, notification)
        }
        foregroundTypes = types
        foregroundActive = true
        wakeLock?.let { if (!it.isHeld) it.acquire() }
    }

    private fun prepareForeground(source: AudioSourceType) {
        val types = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or when {
            source == AudioSourceType.SYSTEM_AUDIO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            source == AudioSourceType.PARTY_MIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            else -> 0
        }
        if (!foregroundActive) {
            // A visible UI can restart the started lifetime after the notification's Stop.
            startService(Intent(this, SoundMeshService::class.java).setAction(ACTION_START))
        }
        promoteForeground(types)
    }

    fun beginSystemCapture(resultCode: Int, consentData: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            controller.reportError("System audio capture requires Android 10 or newer")
            return
        }
        try {
            controller.releaseSystemCapture()
            // Android requires projection foreground promotion BEFORE getMediaProjection.
            prepareForeground(AudioSourceType.SYSTEM_AUDIO)
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = manager.getMediaProjection(resultCode, consentData)
                ?: error("Capture consent was unavailable")
            controller.setMediaProjection(projection)
        } catch (e: Exception) {
            controller.releaseSystemCapture()
            controller.reportError("Cannot start capture: ${e.message}")
            prepareForeground(AudioSourceType.PARTY_BEATS)
        }
    }

    fun updateStatus(title: String, status: String) {
        if (!foregroundActive) return
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notificationId, buildNotification(title, status))
    }

    override fun onDestroy() {
        viewModelStore.clear() // Releases sockets, AudioRecord, AudioTrack and projection token.
        wakeLock?.let { if (it.isHeld) it.release() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val open = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, SoundMeshService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification_soundmesh)
            .setContentIntent(open)
            .addAction(R.drawable.ic_notification_soundmesh, "Stop SoundMesh", stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW)
                .apply { description = getString(R.string.notification_channel_desc); setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START = "com.example.soundmesh.START"
        const val ACTION_STOP = "com.example.soundmesh.STOP"
    }
}
