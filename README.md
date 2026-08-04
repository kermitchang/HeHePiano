# KermitPiano

KermitPiano is a Kotlin Multiplatform desktop piano-practice prototype built with Compose Multiplatform. It imports Standard MIDI Files, renders them through a shared 88-key layout, and supports Practice, Follow Song, and Full 88 views.

## Requirements

- JDK 21
- The checked-in Gradle Wrapper

## Build

Run the complete build and shared unit-test suite:

```shell
./gradlew build
```

## Run

Launch the desktop application:

```shell
./gradlew :composeApp:run
```

The app starts in **Practice** mode. Use **Open MIDI** to choose an external file, or **Library** to select a song from `source/midi/`. Imported files are copied into the local library without replacing an existing file; name collisions receive a numeric suffix.

## Local MIDI Library

Place `.mid` or `.midi` files in `source/midi/`. The desktop app finds the project root from its working directory, scans the library at startup, and rescans it when **Refresh Library** is selected.

The checked-in `FFVII - Tifas Theme [mk].mid` is a parser and layout test song.

## Piano Audio

Audio is optional. The desktop backend uses one long-lived interactive [FluidSynth](https://www.fluidsynth.org/) process and a user-provided `.sf2` SoundFont. No sound bank is downloaded or committed automatically.

1. Install FluidSynth yourself, for example on macOS: `brew install fluid-synth`.
2. Put a licensed SoundFont at `source/soundfonts/piano.sf2`.
3. Record its name, author, source URL, and licence in `source/soundfonts/LICENSE.example.txt`; retain a licence text such as `LICENSE-<name>.txt` when supplied.

To use a SoundFont stored elsewhere, set `KERMITPIANO_SOUNDFONT` before launching the app:

```shell
KERMITPIANO_SOUNDFONT=/absolute/path/to/piano.sf2 ./gradlew :composeApp:run
```

The lookup order is the configured path, the `kermitpiano.soundfont` JVM property, `KERMITPIANO_SOUNDFONT`, `source/soundfonts/piano.sf2`, then another valid `.sf2` file in that directory. The selected path and any lookup failure are printed at startup. Enable **Debug** from the More menu to inspect the audio backend and send a **Test C4** note once audio is ready.

If FluidSynth or a SoundFont is missing, the app stays playable in NoAudio mode and shows its audio status. Everything in `source/soundfonts/` is intentionally ignored by Git except the directory documentation and `LICENSE-*.txt` files, preventing proprietary or licence-restricted audio assets from being committed accidentally.

## Keyboard Mapping

Key-down events play the following MIDI notes. Releasing a key releases only that note; repeated key-down events are ignored until the key is released.

| Keyboard key | Note | MIDI |
| --- | --- | ---: |
| A | C4 | 60 |
| W | C#4 | 61 |
| S | D4 | 62 |
| E | D#4 | 63 |
| D | E4 | 64 |
| F | F4 | 65 |
| T | F#4 | 66 |
| G | G4 | 67 |
| Y | G#4 | 68 |
| H | A4 | 69 |
| U | A#4 | 70 |
| J | B4 | 71 |
| K | C5 | 72 |
| L | D5 | 74 |

## Architecture

The project currently has one `composeApp` module. It keeps business rules in shared `commonMain` code and places only the desktop entry point and Compose Desktop integration in `jvmMain`.

```text
Compose UI (app)
    ├── keyboard input
    ├── playback controls
    ├── waterfall renderer
    └── virtual piano
            │
            ▼
Core domain
    ├── music       MIDI note value and labels
    ├── timeline    GameTime, SongTime, tempo, playback and loops
    ├── judgement   Perfect, Good, Miss and Combo
    └── song        Song model and SongRepository boundary
```

- `TimelineEngine` keeps the monotonic game clock, song playback time, speed, pause/resume, restart, and looping separate from rendering.
- `WaterfallRenderer` uses one Canvas and renders only visible notes from a time-sorted song.
- `SongRepository` supplies materialized `Song` objects. `DemoSongRepository` is the current implementation.
- Keyboard and playback controllers own mutable state and expose read-only `StateFlow` to the UI.

## Project Structure

```text
KermitPiano/
├── composeApp/
│   └── src/
│       ├── commonMain/     Shared UI, features, and domain code
│       ├── commonTest/     Shared unit tests
│       └── jvmMain/        Desktop entry point
├── gradle/                 Version catalog and wrapper support
├── AGENTS.md               Repository engineering rules
└── README.md
```

## Future Plan

- Persist the local song library and per-song track mappings.
- Add USB MIDI input behind the existing input boundary.
- Add a native audio backend after measured FluidSynth process latency is insufficient.
- Extend the waterfall and practice feedback for larger songs.
- Support Linux Desktop and Raspberry Pi 4 ARM64 alongside macOS Desktop.
