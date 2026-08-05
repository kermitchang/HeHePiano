# HeHePiano

> [**English**](README.md) | **繁體中文**

HeHePiano 是一個以 **Kotlin Multiplatform + Compose Multiplatform** 打造的桌面鋼琴練習原型。它能匯入標準 MIDI 檔案（Standard MIDI Files），透過共用的 88 鍵佈局呈現樂譜，並支援 **練習（Practice）**、**跟譜（Follow Song）** 與 **完整 88 鍵（Full 88）** 三種檢視模式。

## 環境需求

- JDK 21
- 專案內建的 Gradle Wrapper

## 建置（Build）

執行完整建置與共用單元測試：

```shell
./gradlew build
```

## 執行（Run）

啟動桌面應用程式：

```shell
./gradlew :composeApp:run
```

應用程式預設以 **練習（Practice）** 模式啟動。可使用 **Open MIDI** 選擇外部檔案，或透過 **Library** 從 `source/midi/` 挑選歌曲；已存在於本地樂曲庫的檔案會在啟動時掃描，也可以重複分析。

## 本地 MIDI 樂曲庫

將 `.mid` 或 `.midi` 檔案放入 `source/midi/`。桌面應用會從工作目錄找出專案根目錄，啟動時掃描樂曲庫，並在選擇 **Refresh Library**（重新整理樂曲庫）時重新掃描。個人與第三方 MIDI 檔案刻意由 Git 忽略，不會上傳。

## 鋼琴音訊（Piano Audio）

音訊是選配功能。桌面後端使用一個長駐的互動式 [FluidSynth](https://www.fluidsynth.org/) 程序，搭配使用者提供的 `.sf2` SoundFont 音色檔。系統不會自動下載或提交任何音色庫。

1. 自行安裝 FluidSynth，例如 macOS：`brew install fluid-synth`。
2. 將具有合法授權的 SoundFont 放在 `source/soundfonts/piano.sf2`。
3. 在 `source/soundfonts/LICENSE.example.txt` 記錄其名稱、作者、來源網址與授權；若供應商有提供授權文字（如 `LICENSE-<name>.txt`），請一併保留。

若要使用存放在其他位置的 SoundFont，可在啟動前設定 `HEHEPIANO_SOUNDFONT`：

```shell
HEHEPIANO_SOUNDFONT=/absolute/path/to/piano.sf2 ./gradlew :composeApp:run
```

查詢順序為：設定的路徑 → `hehepiano.soundfont` JVM 屬性 → `HEHEPIANO_SOUNDFONT` 環境變數 → `source/soundfonts/piano.sf2` → 該目錄下其他有效的 `.sf2` 檔案。選定路徑與任何查詢失敗訊息都會在啟動時印出。音訊就緒後，可從 More 選單開啟 **Debug** 檢視音訊後端，並發送 **Test C4** 測試音。

若缺少 FluidSynth 或 SoundFont，應用仍會以 NoAudio 模式正常運作，並顯示音訊狀態。`source/soundfonts/` 內所有內容（除目錄說明文件與 `LICENSE-*.txt` 外）都被 Git 忽略，避免意外提交有版權或授權限制的音訊資產。

## MIDI 匯入安全性

按下 **Open MIDI** 後會先選取檔案，再於 UI 執行緒之外讀取與解析。空檔案及超過 16 MiB 的檔案會在解析前拒絕；介面明確呈現 `Analyzing`、`Ready` 與 `Failure` 狀態，格式錯誤或無法讀取時會顯示可處理的錯誤，不會阻塞播放或讓應用程式崩潰。音軌左右手對應只會在分析完成後套用。

## Demo 模式（自動演奏）

MIDI 歌曲匯入後，確認音訊就緒，開啟 **Demo Mode** 再按 **Play**。程式會沿用瀑布流使用的同一條播放時間軸，將未標記為 `Ignore` 的 MIDI 音符排程，並把 NoteOn／NoteOff 送到鋼琴音訊引擎。匯入時會保留音符長度、力度與 MIDI channel，因此可以正確聽到和弦與延音，而不是固定長度的單音。暫停、重播、速度調整、切換歌曲與歌曲結束時，都會安全釋放自動演奏中的音符。

Demo 模式開啟期間會暫停電腦鍵盤與 USB MIDI 輸入，避免手動彈奏干擾自動演奏。第一版會以目前設定的鋼琴 SoundFont 播放所有選取的 MIDI channel；延音踏板、各 channel 專用音色、彎音與其他控制器自動化留待後續擴充。

## 左右手練習與自動伴奏

MIDI Analysis 面板新增 **Practice Part**：**Left Hand**、**Right Hand**、**Both Hands**。音軌上的 `LEFT`／`RIGHT`／`IGNORE` 仍負責定義音軌內容屬於哪隻手；Practice Part 則決定演奏者與電腦各自負責什麼：

- **Left Hand**：使用者彈左手，電腦自動彈右手伴奏。
- **Right Hand**：使用者彈右手，電腦自動彈左手伴奏。
- **Both Hands**：使用者彈雙手，不啟用自動伴奏。

既有的 **Demo Mode** 仍是完整自動演奏模式：電腦彈奏雙手並暫停手動輸入。匯入後的 `SongNote` 會保留左右手、力度、MIDI channel、開始時間與音符長度。

第一版預期左右手內容已分在不同 MIDI 音軌；同一音軌混合左右手的 MIDI，仍需要之後的逐音符拆分規則。

## 瀑布音符長度

瀑布區塊會依 MIDI NoteOn 到 NoteOff 的 duration 繪製。短音符會是短區塊，延音音符會是長區塊；區塊底端在 NoteOn 時抵達判定線，頂端在 NoteOff 時抵達判定線。極短音符仍會保留最小高度以維持可見性。

## 鍵盤對應（Keyboard Mapping）

按下按鍵即發出對應 MIDI 音符。放開按鍵只釋放該音符；重複按下會被忽略，直到按鍵放開。

| 鍵盤按鍵 | 音符 | MIDI |
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

## USB MIDI 琴輸入（AK490 等）

除了電腦鍵盤，應用也支援透過 `javax.sound.midi` 讀取 **USB MIDI 琴**（例如 Midiplus AK490 Pro）的輸入：

- 啟動時自動掃描並連接名稱含 `AK490` 的 MIDI 裝置（可於 `UsbMidiInput` 的 `deviceNameContains` 調整關鍵字）。
- 琴鍵的 **NoteOn / NoteOff** 會即時路由到音訊引擎發聲。
- USB NoteOn 的力度（velocity）會保留；USB MIDI 與電腦鍵盤共用同一份按鍵狀態，因此虛擬鋼琴會正確呈現兩種輸入的重疊音符。
- **Pitch Bend（彎音）輪** → 音高滑動效果。
- **Modulation（調變）輪** → 顫音/震音效果（CC1）。
- 琴上的 **Octave + / - 按鈕** 由琴本身硬體處理八度移位，應用直接接收移位後的正確音高。

如果未偵測到 MIDI 琴，應用會正常啟動並於啟動日誌顯示提示，不影響其他功能。

## 架構（Architecture）

專案目前只有一個 `composeApp` 模組。業務規則放在共用的 `commonMain` 程式碼中，僅將桌面入口與 Compose Desktop 整合放在 `jvmMain`。

```text
Compose UI (app)
    ├── state holder/actions 狀態持有者與動作
    ├── keyboard input     鍵盤輸入（電腦鍵盤 + USB MIDI 琴）
    ├── playback controls  播放控制
    ├── demo scheduler      Demo 自動演奏排程
    ├── waterfall renderer 瀑布流譜面
    └── virtual piano      虛擬鋼琴
            │
            ▼
Core domain
    ├── music       MIDI 音符數值與名稱
    ├── timeline    遊戲時間、歌曲時間、速度、播放與循環
    ├── judgement   Perfect、Good、Miss 與 Combo
    └── song        歌曲模型與 SongRepository 邊界
```

- `TimelineEngine` 將單調遊戲時鐘、歌曲播放時間、速度、暫停/繼續、重啟與循環，與渲染邏輯分離。
- `WaterfallRenderer` 使用單一 Canvas，只渲染時間排序後的可見音符。
- `SongRepository` 提供具體化的 `Song` 物件，目前實作為 `DemoSongRepository`。
- `HeHePianoStateHolder` 統一持有歌曲、播放、匯入、樂曲庫、視窗與除錯狀態；`HeHePianoApp` 只渲染唯讀 `StateFlow` 並分派具型別的動作。
- `PlayerInputTracker` 以來源分開計數並合併電腦鍵盤與 USB MIDI 音符，避免兩個來源重疊時過早釋放按鍵。
- `AutoPlayScheduler` 是共用且可決定性的 MIDI 事件排程器；`AutoPlayOutput` 以小型邊界連接音訊與虛擬琴鍵狀態，領域層不依賴 FluidSynth。
- Compose UI 已拆成 App shell、`PianoTopBar`、歌曲／匯入面板與共用視覺 token；業務規則不放在 composable 中。

## 專案結構（Project Structure）

```text
HeHePiano/
├── composeApp/
│   └── src/
│       ├── commonMain/     共用 UI、功能與領域程式碼
│       ├── commonTest/     共用單元測試
│       └── jvmMain/        桌面入口與 JVM 整合
├── gradle/                 版本目錄與 Wrapper 支援
├── setup/
│   └── run-piano-pi4.sh    Raspberry Pi 4 啟動腳本
├── AGENTS.md               儲存庫工程規範
└── README.md               本說明文件
```

### Raspberry Pi 4 啟動方式

專案提供 Pi4 專用啟動腳本 `setup/run-piano-pi4.sh`：

```shell
./setup/run-piano-pi4.sh            # 有接螢幕時啟動（硬體渲染）
./setup/run-piano-pi4.sh --headless # 無螢幕/SSH 遠端（Xvfb + 軟體渲染）
./setup/run-piano-pi4.sh --build    # 只編譯不啟動
```

## 未來規劃（Future Plan）

- 持久化本地樂曲庫與每首歌曲的音軌對應設定。
- 在量測到 FluidSynth 程序延遲不足後，加入原生音訊後端。
- 針對較大的歌曲擴展瀑布流與練習回饋。
- 在 macOS Desktop 之外，持續支援 Linux Desktop 與 Raspberry Pi 4 ARM64。
