# SoundMesh 1.1 device test

This is an installable test build, not a physical-device certification. CI validates
compilation, clock/protocol behavior and service lifetime. These hardware checks
are still pending. Record phone model, Android version, network and audio route.

## Install and baseline

1. Install SoundMesh-1.1-test.apk on every test phone. The launcher says SoundMesh Test;
   its separate `.test` app ID keeps the existing SoundMesh installation intact.
2. Stop any older SoundMesh instance so it does not compete for the same UDP ports.
3. Put two phones on the same Wi-Fi or hotspot. Avoid guest/client-isolated networks.
4. Choose Master on phone A and Speaker on phone B. Use the displayed master IP
   if discovery does not connect automatically. Choose Party Beats, then Play.
5. Check sound and volume changes, then add a third speaker if available.

## Timing and reconnection

- Run Auto-Sync. The network buffer changes without claiming acoustic phase lock.
- Compare nearby speakers for echo; use local delay trim to compensate for device output.
- Listen for ten minutes. Record interruptions, drift and selected latency mode.
- Stop the master from its notification, reopen and start Party Beats again. Speaker
  should reconnect and accept new sequence numbers instead of remaining silent.
- Disable master Wi-Fi for >8 seconds. Speaker should report master unavailable;
  restore the same connection and check automatic recovery.
- A manually selected master IP should not switch to another active master.

## Capture and background

- On Android 10+, choose System Audio, Grant Access, approve capture, then Play.
  Start audio in a source app that permits playback capture. Some apps prevent it.
- Press Home or recreate/rotate the app; playback should continue while consent remains.
- Try screen off. New Android versions may revoke projection on lock; if that happens,
  SoundMesh should pause and show that fresh capture access is needed, not stream silence.
- Revoke capture in Android's capture indicator/Quick Settings. Reopen SoundMesh;
  verify stopped playback and grant fresh consent before restarting.
- Switch between System Audio, Microphone and Party Beats. Previous captures must stop.
- Deny microphone permission: an actionable status should replace a crash or fake playback.
- Test microphone background behavior at low volume to avoid acoustic feedback.

## Bluetooth and cleanup

- Route a speaker phone to its paired Bluetooth accessory using Android's output controls.
  Verify sound and adjust delay trim; Bluetooth latency is device dependent.
- Use notification Stop on every phone. Audio and the persistent notification should end.
- Reopen the app after Stop and check that Start SoundMesh establishes a fresh session.

### Results to send back

Phone models/Android versions; audio source; Wi-Fi or hotspot; speaker count;
latency mode/trim; what failed and approximate time. A short screen recording or
exact error text helps identify capture, transport or audio-route failures.
