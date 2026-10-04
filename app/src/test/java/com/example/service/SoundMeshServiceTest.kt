package com.example.service

import android.content.Intent
import androidx.lifecycle.ViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34, 35, 36])
class SoundMeshServiceTest {
    private class RecordingEngine : ViewModel() {
        var stopped = false
        override fun onCleared() { stopped = true }
    }

    @Test fun screenUnbindKeepsEngineAliveAndServiceDestructionClosesIt() {
        val lifecycle = Robolectric.buildService(SoundMeshService::class.java).create()
        val service = lifecycle.get()
        val engine = RecordingEngine()
        service.viewModelStore.put("engine", engine)
        val first = service.onBind(Intent()) as SoundMeshService.LocalBinder
        assertSame(service, first.getService())
        service.onUnbind(Intent()) // Activity closes or is recreated
        assertFalse(engine.stopped)
        val second = service.onBind(Intent()) as SoundMeshService.LocalBinder
        assertSame(first.getService().viewModelStore, second.getService().viewModelStore)
        lifecycle.destroy()
        assertTrue(engine.stopped)
    }

    @Test fun processRestartDoesNotSilentlyRestartCaptureWithoutConsent() {
        val lifecycle = Robolectric.buildService(SoundMeshService::class.java).create()
        assertEquals(android.app.Service.START_NOT_STICKY, lifecycle.get().onStartCommand(null, 0, 1))
        lifecycle.destroy()
    }
}
