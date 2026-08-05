# HeHePiano

> **English** | [**繁體中文**](README.zh-TW.md)

HeHePiano is a Kotlin Multiplatform desktop piano-practice prototype built with Compose Multiplatform. It imports Standard MIDI Files, renders them through a shared 88-key layout, and supports Practice, Follow Song, and Full 88 views.

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

The app starts in **Practice** mode. Use **Open MIDI** to choose an external file, or **Library** to select a song from `source/midi/`. Files already in the local library are scanned at startup and can be analyzed repeatedly.

## Local Source Assets

The `source/` directory holds local assets used while developing and testing the desktop app. Asset binaries are intentionally excluded from Git; only their documentation and applicable licence text are tracked.

### `source/midi/`

This is the local MIDI library. Place `.mid` or `.midi` files here; the app finds the project root from its working directory, scans the library at startup, and rescans it when **Refresh Library** is selected. Files chosen through **Open MIDI** are analyzed for the current session; persistence into this directory is not automatic.

Personal and third-party MIDI files are ignored by Git. For a reproducible local parser and layout test, download Bach's *English Suite II: Prelude*, BWV 807 from [Mutopia Project](https://www.ibiblio.org/pub/multimedia/mutopia/BachJS/BWV807/bach-english-suite-2-prelude/) and save its MIDI file as `source/midi/Bach-BWV807-English-Suite-II-Prelude.mid`. Mutopia identifies this edition as `Mutopia-2008/06/17-84`, sourced from Bach-Gesellschaft and placed in the public domain.

### `source/soundfonts/`

This is the local SoundFont library used for piano audio. The desktop backend uses one long-lived interactive [FluidSynth](https://www.fluidsynth.org/) process and a user-provided `.sf2` SoundFont. No sound bank is committed automatically.

1. Install FluidSynth yourself, for example on macOS: `brew install fluid-synth`.
2. Download [GeneralUser GS](https://schristiancollins.com/generaluser.php) from its author, S. Christian Collins, and save the included `GeneralUser-GS.sf2` as `source/soundfonts/piano.sf2`.
3. Keep the supplied licence as `source/soundfonts/LICENSE-GeneralUser-GS.txt`, and record the version, author, source URL, and licence in `source/soundfonts/LICENSE.example.txt`.

To use a SoundFont stored elsewhere, set `HEHEPIANO_SOUNDFONT` before launching the app:

```shell
HEHEPIANO_SOUNDFONT=/absolute/path/to/piano.sf2 ./gradlew :composeApp:run
```

The lookup order is the configured path, the `hehepiano.soundfont` JVM property, `HEHEPIANO_SOUNDFONT`, `source/soundfonts/piano.sf2`, then another valid `.sf2` file in that directory. The selected path and any lookup failure are printed at startup. Enable **Debug** from the More menu to inspect the audio backend and send a **Test C4** note once audio is ready.

If FluidSynth or a SoundFont is missing, the app stays playable in NoAudio mode and shows its audio status. Everything in `source/soundfonts/` is intentionally ignored by Git except the directory documentation and `LICENSE-*.txt` files, preventing proprietary or licence-restricted audio assets from being committed accidentally.

## MIDI Import Safety

**Open MIDI** first selects a file, then loads and parses it off the UI thread. Empty files and files larger than 16 MiB are rejected before parsing. The UI exposes `Analyzing`, `Ready`, and `Failure` states, so a malformed or unreadable file reports an actionable error without blocking playback or crashing the application. Track hand mappings are only applied after the analysis is ready.

## Demo Mode

When a MIDI song has been imported, enable **Demo Mode** after audio is ready and press **Play**. The app schedules the selected (non-`Ignore`) MIDI notes against the same playback timeline used by the waterfall, then sends NoteOn and NoteOff events to the piano audio engine. Imported note duration, velocity, and MIDI channel are preserved, so chords and note lengths are heard instead of a sequence of fixed-length beeps. Pause, Restart, speed changes, song changes, and the end of the song release all demo notes safely.

While Demo Mode is enabled, computer-keyboard and USB-MIDI input is suspended so manual notes cannot interfere with the automatic performance. The first version renders every selected MIDI channel through the configured piano SoundFont; sustain pedals, program-specific instruments, pitch bend, and other controller automation remain future extensions.

## Hand Practice and Accompaniment

The MIDI Analysis panel has a **Practice Part** selector: **Left Hand**, **Right Hand**, or **Both Hands**. Track mappings still decide whether each MIDI track is `LEFT`, `RIGHT`, or `IGNORE`; the practice selection decides who performs the mapped notes:

- **Left Hand**: the player performs left-hand notes while the computer accompanies with right-hand notes.
- **Right Hand**: the player performs right-hand notes while the computer accompanies with left-hand notes.
- **Both Hands**: the player performs both hands and automatic accompaniment is disabled.

The existing **Demo Mode** remains a full-song mode: it plays both hands and suspends manual input. Imported `SongNote` values retain their mapped hand, velocity, MIDI channel, start time, and duration.

This first version expects left- and right-hand material to be assigned to separate MIDI tracks. A track that mixes both hands still needs a future note-splitting policy.

## Waterfall Note Lengths

Waterfall bars use each MIDI note's NoteOn-to-NoteOff duration. A short note renders as a short bar; a sustained note renders as a longer bar whose bottom edge reaches the judgement line at NoteOn and whose top edge reaches it at NoteOff. A small minimum height keeps very short notes visible.

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

## USB MIDI Keyboard Input (AK490 and others)

Besides the computer keyboard, the app also reads from a **USB MIDI keyboard** (e.g. Midiplus AK490 Pro) via `javax.sound.midi`:

- On startup it scans and connects to a MIDI device whose name contains `AK490` (tune the keyword via `deviceNameContains` in `UsbMidiInput`).
- **NoteOn / NoteOff** events are routed to the audio engine immediately.
- Note velocity is preserved for USB NoteOn events, and USB notes share the same active-note state as computer-keyboard notes in the virtual piano.
- **Pitch Bend wheel** → pitch slide effect.
- **Modulation wheel** → vibrato/tremolo effect (CC1).
- The keyboard's **Octave + / - buttons** are handled in hardware by the keyboard itself; the app receives the already-shifted notes.

If no MIDI keyboard is detected, the app starts normally and prints a notice to the startup log.

## Architecture

The project currently has one `composeApp` module. It keeps business rules in shared `commonMain` code and places only the desktop entry point and Compose Desktop integration in `jvmMain`.

```text
Compose UI (app)
    ├── state holder and actions
    ├── keyboard + USB MIDI input
    ├── playback controls
    ├── demo-mode scheduler
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
- `HeHePianoStateHolder` is the single owner for song, playback, import, library, viewport, and debug UI state; `HeHePianoApp` renders its read-only `StateFlow` and dispatches typed actions.
- `PlayerInputTracker` merges computer-keyboard and USB MIDI notes with per-source reference counting, so overlapping NoteOn/NoteOff events do not release a key that another source still holds.
- `AutoPlayScheduler` is a shared, deterministic MIDI event scheduler; `AutoPlayOutput` is the small boundary that connects it to audio and virtual-key state without making the domain depend on FluidSynth.
- Compose UI is split into the app shell, `PianoTopBar`, song/import panels, and shared visual tokens; business rules remain outside composables.

## Project Structure

```text
HeHePiano/
├── composeApp/
│   └── src/
│       ├── commonMain/     Shared UI, features, and domain code
│       ├── commonTest/     Shared unit tests
│       └── jvmMain/        Desktop entry point and JVM integration
├── gradle/                 Version catalog and wrapper support
├── setup/
│   └── run-piano-pi4.sh    Raspberry Pi 4 launcher script
├── AGENTS.md               Repository engineering rules
└── README.md
```

### Raspberry Pi 4 Launch

A Pi4-specific launcher is provided at `setup/run-piano-pi4.sh`:

```shell
./setup/run-piano-pi4.sh            # launch with a display (hardware rendering)
./setup/run-piano-pi4.sh --headless # headless / SSH (Xvfb + software rendering)
./setup/run-piano-pi4.sh --build    # build only, do not launch
```

## Future Plan

- Persist the local song library and per-song track mappings.
- Add a native audio backend after measured FluidSynth process latency is insufficient.
- Extend the waterfall and practice feedback for larger songs.
- Keep supporting Linux Desktop and Raspberry Pi 4 ARM64 alongside macOS Desktop.
