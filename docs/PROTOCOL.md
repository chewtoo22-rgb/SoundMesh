# SoundMesh wire protocol

Transport: **UDP** on the local LAN (multicast group `239.255.42.99` plus unicast where useful).

| Port | Purpose |
|------|---------|
| `9876` | Audio + control (`AUDIO_PORT`) |
| `9877` | Discovery helpers (`DISCOVERY_PORT`) |

## Binary header (22 bytes, big-endian)

| Offset | Size | Field |
|--------|------|--------|
| 0 | 2 | Magic `0x53 0x4D` (`SM`) |
| 2 | 1 | Message type |
| 3 | 1 | Flags (reserved, currently `0`) |
| 4 | 8 | Sequence number |
| 12 | 8 | Timestamp (nanos, presentation or event time) |
| 20 | 2 | Payload length |
| 22 | N | Payload |

Helpers: `MeshProtocol.create*`, `MeshProtocol.parseHeader`, `MeshProtocol.isValidMagic`.

## Message types

| Code | Name | Payload |
|------|------|---------|
| `0x01` | `TYPE_AUDIO_DATA` | Raw interleaved stereo PCM 16-bit LE frames |
| `0x02` | `TYPE_SYNC_PING` | Sync probe |
| `0x03` | `TYPE_SYNC_PONG` | `speakerId|speakerReceiveNanos|speakerSendNanos`; header echoes ping sequence/send time |
| `0x04` | `TYPE_ANNOUNCE_SPEAKER` | `id\|name\|battery\|…` pipe fields |
| `0x05` | `TYPE_MASTER_BEACON` | `name\|ip\|isPlaying` |
| `0x06` | `TYPE_COMMAND_VOLUME` | Volume command |
| `0x07` | `TYPE_COMMAND_PLAYSTATE` | Play / pause |
| `0x08` | `TYPE_IDENTIFY_CHIRP` | Locate speaker |
| `0x09` | `TYPE_AUTO_SYNC_ALIGN` | `targetPlayNano\|delayOffsetMs` |
| `0x0A` | `TYPE_CONFIG_UPDATE` | `latencyMode\|audioProfile` |
| `0x0B` | `TYPE_MASTER_STATS` | Telemetry pipe fields |
| `0x0C` | `TYPE_SPEAKER_TUNING_UPDATE` | Per-speaker DSP assignment |
| `0x0D` | `TYPE_CLOCK_SYNC` | `speakerId|masterMinusSpeakerNanos|roundTripNanos`; ping sequence |
| `0x0E` | `TYPE_STREAM_SESSION` | Master lifetime UUID; changed session resets receiver clock and sequences |

## Audio framing

- Sample rate: **44100 Hz**
- Channels: **2** (stereo)
- Sample format: **16-bit PCM**
- Default samples per frame: **512** (~11.6 ms) → **2048** payload bytes

Receivers use a bounded priority queue ordered by presentation timestamp, drop oldest when backlog grows, and ignore non-increasing sequence numbers to avoid doubled frames.

## Extending the protocol

1. Add a `TYPE_*` constant in `MeshProtocol`.
2. Build packets with the shared header helpers.
3. Validate with `parseHeader` before reading the payload.
4. Add encode/decode unit tests under `app/src/test`.

## Clock synchronization (1.1)

Master sends t1, speaker timestamps receive t2 and response t3, master timestamps t4.
Round-trip network time is `(t4-t1)-(t3-t2)` and master-minus-speaker offset is
`((t1-t2)+(t4-t3))/2`. Invalid or >1 second exchanges are rejected. The master returns
the lowest-RTT of the last 8 samples; delayed replies cannot overwrite newer estimates.
Receivers translate audio PTS using the measured offset and local trim. Auto-align
never treats future PTS as the current clock. Symmetric network delay is assumed;
hardware output latency and clock-rate correction still need physical-device testing.
Install 1.1 on every phone; old builds only provide a coarse ping offset.
