# SoundMesh

**A different way to listen.**

Turn up to **10 Android phones** into a synchronized, low-latency wireless multi-speaker system. Stream system audio (Spotify, YouTube, games…), live mic, or built-in party synth across the room with anti-echo sync, spatial zones, 5-band EQ, and 5.1-style channel routing.

---

## Features

| Area | What you get |
|------|----------------|
| **Roles** | One **Master** broadcaster + multiple **Speaker** receivers |
| **Audio sources** | System-wide capture (MediaProjection), live mic, built-in synthesizer / test tones |
| **Sync** | Latency modes (Ultra-Low / Balanced / Rock-Solid), fine delay trim, auto-calibration pulse |
| **Spatial** | Zones (Left / Center / Right / Surround / Sub / Whole Room) + channel routing (L/R/C/SUR/SUB/Mono) |
| **DSP** | 5-band EQ with presets, audio profiles (Party Wall, 5.1, Bass Blast, Vocal, Wide Stage) |
| **Quality** | 44.1 kHz PCM streaming paths, bandwidth-aware modes |
| **UI** | Dark “obsidian” audiophile theme, live visualizer, per-speaker cards, pairing flow |
| **Background** | Foreground service keeps the mesh alive while the screen is off |

---

## How it works (high level)

1. **Master phone** captures or generates audio and broadcasts over Wi‑Fi (UDP mesh protocol).
2. **Speaker phones** discover / connect to the master, receive PCM frames, apply local volume / channel / EQ / latency trim, and play in sync.
3. Shared clock + delay offset + optional calibration pulse reduce echo and drift across devices on the same network (ideally the same Wi‑Fi or hotspot).

> Best results: same 5 GHz Wi‑Fi or phone hotspot, devices reasonably close, Balanced or Rock-Solid latency mode for media.

---

## Requirements

- Android **8.0+** (API 24+); target SDK 36
- Wi‑Fi (or hotspot) between devices
- For system audio capture: user grants **MediaProjection** (screen/audio capture) permission
- Mic permission for live party mic mode
- Notification permission (Android 13+) for the foreground service

---

## Build & run

```bash
git clone https://github.com/chewtoo22-rgb/SoundMesh.git
cd SoundMesh

# Optional: copy env template if you use secrets plugin features
cp .env.example .env

# Build debug APK
./gradlew assembleDebug

# Install on a connected device
./gradlew installDebug
```

Open the app → choose **Master** or **Speaker** in the top bar.

- **Master**: pick audio source, grant capture if needed, start playback; speakers appear as they connect.
- **Speaker**: connect to master IP (or use discovery/pairing UI), set channel/zone/volume/trim.

CI builds a debug APK on every push to `main` (see **Actions** → Android CI).

---

## Project layout

```
app/src/main/java/com/example/
├── MainActivity.kt              # Role switch + capture permission + service start
├── audio/
│   ├── capture/                 # System audio (MediaProjection)
│   ├── dsp/                     # EQ / profiles / processing
│   ├── mesh/                    # Protocol, broadcaster, receiver
│   └── synth/                   # Built-in tones / beats
├── model/                       # Roles, channels, zones, EQ, MeshState
├── service/                     # Foreground service
├── ui/                          # Master & Speaker screens + components
├── util/                        # System stats helpers
└── viewmodel/                   # SoundMeshViewModel (single source of truth)
```

---

## Configuration notes

- `applicationId`: `com.aistudio.soundmesh.wzqj`
- Kotlin + Jetpack Compose + Material 3
- Min SDK 24 / Target SDK 36
- Signing for release uses env vars: `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` (see `app/build.gradle.kts`)

---

## Privacy & permissions

SoundMesh uses:

- **Internet / Wi‑Fi** – mesh transport  
- **Record audio** – mic mode and capture pipeline  
- **Media projection** – system audio from other apps (explicit user consent)  
- **Foreground service** – keep streaming when backgrounded  
- **Bluetooth connect** (optional) – device naming / accessory awareness on newer Android versions  

Audio stays on your local network; there is no cloud streaming path in the core mesh.

---

## Roadmap ideas

- Stronger discovery (mDNS / NSD) and QR pairing polish  
- Opus / compressed fallback for weak networks  
- Multi-room groups and saved speaker layouts  
- Wear OS / tablet layout refinements  
- Package rename from `com.example` → stable production namespace  

---

## License

MIT — see [LICENSE](LICENSE).

---

*Built for parties, living rooms, and anyone who wants more speakers without more boxes.*
