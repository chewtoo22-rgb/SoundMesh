# Changelog

All notable changes to SoundMesh are documented here.

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
