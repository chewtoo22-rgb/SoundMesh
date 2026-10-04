# Changelog

All notable changes to SoundMesh are documented here.

## 1.1-test — 2026-10-04

- Four-timestamp clock synchronization with network-delay compensation and a low-jitter sample filter.
- Auto-Sync buffer changes no longer overwrite clock offsets or reset duplicate-frame protection.
- Service-owned engines survive Activity teardown; notification Stop releases audio/network resources.
- Correct media-projection/microphone foreground types; capture revocation pauses playback and requests new consent.
- Blocking AudioRecord teardown unblocks reads before releasing the recorder.
- Restarted-master session IDs reset sequence/clock state; master timeout and explicit-IP pinning support reconnecting.
- Exact nanosecond synth pacing avoids accumulating a 44.1 kHz rate mismatch.
- Unit tests must pass before APK publication; clock/session and service-lifetime regressions covered.
- Version code incremented to 2. Hardware sync and Bluetooth latency remain device-test items.

## Unreleased

### Added
- Project README, MIT LICENSE, CONTRIBUTING guide
- Wire protocol documentation (`docs/PROTOCOL.md`)
- Unit tests for `MeshProtocol` header encode/decode
- Shared protocol header builder + `parseHeader` validation helpers

### Changed
- Branding: `SoundMeshTheme` (legacy `MyApplicationTheme` kept as alias)
- Android theme resource → `Theme.SoundMesh`
- Notification channel / service copy use string resources
- Notification small icon uses launcher mipmap (more reliable than large logo art)
- Android CI uses Gradle wrapper, caches JDK, uploads APK artifact
- `.gitignore` expanded for keystores, APKs, and secrets

### Fixed
- Safer payload length handling when building audio packets
