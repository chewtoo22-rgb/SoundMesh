package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.DeviceRole
import com.example.service.SoundMeshService
import com.example.ui.MasterControllerScreen
import com.example.ui.SpeakerReceiverScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.SonicAmber
import com.example.ui.theme.SonicCyan
import com.example.ui.theme.SonicEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.SoundMeshViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SoundMeshViewModel by viewModels()
    private lateinit var mediaProjectionLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup Media Projection Launcher for Audio Playback Capture (All Apps)
        mediaProjectionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val projection = projectionManager.getMediaProjection(result.resultCode, result.data!!)
                if (projection != null) {
                    viewModel.setMediaProjection(projection)
                }
            }
        }

        // Setup Runtime Permissions Launcher
        permissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            // Permissions handled
        }

        requestAppPermissions()
        startMeshForegroundService()

        setContent {
            MyApplicationTheme {
                val state by viewModel.state.collectAsState()

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBg),
                    containerColor = ObsidianBg,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        SoundMeshTopBar(
                            currentRole = state.role,
                            onRoleSelected = { viewModel.selectRole(it) },
                            connectedCount = state.connectedSpeakers.size,
                            isPlaying = state.isPlaying
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (state.role == DeviceRole.MASTER) {
                            MasterControllerScreen(
                                state = state,
                                onTogglePlay = { viewModel.togglePlay() },
                                onMasterVolumeChange = { viewModel.setMasterVolume(it) },
                                onMasterMuteToggle = { viewModel.toggleMasterMute() },
                                onSelectAudioSource = { viewModel.selectAudioSource(it) },
                                onRequestSystemAudioCapture = { launchSystemAudioCapture() },
                                onSetSynthMode = { viewModel.setSynthMode(it) },
                                onTestSyncPulse = { viewModel.testSyncPulse() },
                                onDelayOffsetChange = { viewModel.setSyncDelayOffset(it) },
                                onTriggerAutoSync = { viewModel.triggerAutoSyncCalibration() },
                                onSelectLatencyMode = { viewModel.setLatencyMode(it) },
                                onSelectSoundQuality = { viewModel.setSoundQuality(it) },
                                onSelectAudioProfile = { viewModel.setAudioProfile(it) },
                                onSelectEqualizerPreset = { viewModel.setEqualizerPreset(it) },
                                onUpdateEqualizerBand = { index, gain -> viewModel.updateEqualizerBand(index, gain) },
                                onToggleEqualizer = { viewModel.toggleEqualizer(it) },
                                onSpeakerVolumeChange = { id, vol -> viewModel.updateSpeakerVolume(id, vol) },
                                onSpeakerMuteToggle = { id -> viewModel.toggleSpeakerMute(id) },
                                onSpeakerChannelChange = { id, ch -> viewModel.updateSpeakerChannel(id, ch) },
                                onPingSpeaker = { id -> viewModel.pingSpeaker(id) },
                                onRemoveSpeaker = { id -> viewModel.removeSpeaker(id) },
                                onAddDemoSpeaker = { viewModel.addDemoSpeaker() },
                                onSwitchToSpeakerMode = { viewModel.selectRole(DeviceRole.SPEAKER) }
                            )
                        } else {
                            SpeakerReceiverScreen(
                                state = state,
                                onLocalVolumeChange = { viewModel.setLocalSpeakerVolume(it) },
                                onLocalChannelChange = { viewModel.setLocalSpeakerChannel(it) },
                                onLatencyTrimChange = { viewModel.setLocalLatencyTrim(it) },
                                onConnectToMasterIp = { viewModel.connectToMasterIp(it) },
                                onSwitchToMasterMode = { viewModel.selectRole(DeviceRole.MASTER) }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionsLauncher.launch(needed.toTypedArray())
        }
    }

    private fun launchSystemAudioCapture() {
        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        } catch (e: Exception) {
            // Fallback or permission check
        }
    }

    private fun startMeshForegroundService() {
        try {
            val intent = Intent(this, SoundMeshService::class.java).apply {
                action = SoundMeshService.ACTION_START
                putExtra(SoundMeshService.EXTRA_TITLE, "SoundMesh")
                putExtra(SoundMeshService.EXTRA_STATUS, "Wireless Multi-Speaker Mesh Active")
            }
            ContextCompat.startForegroundService(this, intent)
        } catch (_: Exception) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundMeshTopBar(
    currentRole: DeviceRole,
    onRoleSelected: (DeviceRole) -> Unit,
    connectedCount: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCardElevated),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(ObsidianBorder)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo + Title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_soundmesh_logo),
                    contentDescription = "SoundMesh Logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isPlaying) SonicEmerald else SonicAmber)
                        )
                    }
                    Text(
                        text = if (currentRole == DeviceRole.MASTER) "Master Broadcaster" else "Satellite Receiver",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonicCyan,
                        fontSize = 11.sp
                    )
                }
            }

            // Segmented Role Switcher: [ Master | Speaker ]
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianCard)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Master Pill
                val isMaster = currentRole == DeviceRole.MASTER
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isMaster) SonicCyan else ObsidianCard)
                        .clickable { onRoleSelected(DeviceRole.MASTER) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("role_selector_master"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cast,
                            contentDescription = null,
                            tint = if (isMaster) ObsidianCardElevated else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Master",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMaster) ObsidianCardElevated else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isMaster) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Speaker Pill
                val isSpeaker = currentRole == DeviceRole.SPEAKER
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSpeaker) SonicCyan else ObsidianCard)
                        .clickable { onRoleSelected(DeviceRole.SPEAKER) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("role_selector_speaker"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speaker,
                            contentDescription = null,
                            tint = if (isSpeaker) ObsidianCardElevated else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Speaker",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSpeaker) ObsidianCardElevated else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSpeaker) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "SoundMesh $name", modifier = modifier, color = TextPrimary)
}
