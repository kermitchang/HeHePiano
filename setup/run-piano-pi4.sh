#!/usr/bin/env bash
# ============================================================================
# KermitPiano 啟動腳本（Pi4）
# ============================================================================
# 用法:
#   ./run-piano.sh          啟動 KermitPiano（有螢幕時）
#   ./run-piano.sh --build  只編譯不啟動
#   ./run-piano.sh --headless  無螢幕模式（Xvfb + 軟體渲染）
# ============================================================================
set -euo pipefail

cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-arm64}"
export PATH="$JAVA_HOME/bin:$PATH"

echo "📦 KermitPiano Launcher (Pi4)"
echo "   JDK: $(java -version 2>&1 | head -1)"

if [ "${1:-}" = "--build" ]; then
    echo "🔨 編譯中..."
    ./gradlew build --no-daemon
    echo "✅ 編譯完成"
    exit 0
fi

# ---- Headless 模式：Xvfb + 軟體渲染 ----
if [ "${1:-}" = "--headless" ]; then
    echo "🖥️  Headless 模式（虛擬顯示器 + 軟體渲染）"
    if ! command -v Xvfb >/dev/null 2>&1; then
        echo "❌ Xvfb 未安裝，請先: sudo apt install -y xvfb"
        exit 1
    fi
    pgrep -f "Xvfb :99" >/dev/null || (Xvfb :99 -screen 0 1280x800x24 >/dev/null 2>&1 &)
    export DISPLAY=:99
    export SKIKO_RENDER_API=SOFTWARE
    echo "🎹 啟動 KermitPiano（軟體渲染）..."
    ./gradlew :composeApp:run --quiet
    exit 0
fi

# ---- 一般模式 ----
# 檢查顯示環境
if [ -z "${DISPLAY:-}" ] && [ -z "${WAYLAND_DISPLAY:-}" ]; then
    echo "⚠️  沒有偵測到顯示伺服器"
    echo "   請用: ./run-piano.sh --headless"
    exit 1
fi

# 嘗試硬體渲染，失敗自動降級軟體渲染
echo "🎹 啟動 KermitPiano（硬體渲染）..."
if ! ./gradlew :composeApp:run --quiet 2>&1; then
    echo "⚠️  硬體渲染失敗，改用軟體渲染..."
    export SKIKO_RENDER_API=SOFTWARE
    ./gradlew :composeApp:run --quiet
fi
