# Contributing to SoundMesh

Thanks for helping improve the mesh.

## Dev setup

1. Clone the repo and open it in Android Studio (Ladybug+ / recent AGP).
2. Use JDK 17.
3. `./gradlew test assembleDebug`

## Guidelines

- Prefer small, focused PRs.
- Keep the dark “obsidian” visual language consistent.
- Audio path changes should preserve 44.1 kHz stereo PCM framing (`MeshProtocol`) unless you also update receivers.
- Add or extend unit tests for protocol and pure logic (`app/src/test`).
- Do not commit secrets, keystores, or `google-services.json`.

## Architecture reminders

- **Master** owns capture/synth + `MeshAudioBroadcaster`.
- **Speaker** owns `MeshAudioReceiver` + local DSP / channel trim.
- `SoundMeshViewModel` is the single UI state source; engines report levels/latency via callbacks.
- Foreground `SoundMeshService` only keeps the process alive — it does not own the audio engines.

## Protocol

See `MeshProtocol.kt` for the binary header and message types. New control messages should:

1. Allocate a new `TYPE_*` constant.
2. Build packets through the shared header helpers.
3. Validate with `parseHeader` on receive.
4. Cover encode/decode in a unit test.
