#!/bin/bash

# Dead Ball - Build and Run Script for macOS
# This script compiles the Java project and runs it with JavaFX

set -e

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "🎮 Dead Ball - Game Builder"
echo "============================"

JAVAFX_PATH="./javafx-sdk-26"

if [ ! -d "$JAVAFX_PATH" ]; then
    echo "${RED}❌ JavaFX not found at: $JAVAFX_PATH${NC}"
    echo "Update build.sh with your local JavaFX SDK path."
    exit 1
fi

echo -e "${GREEN}✓ Using local JavaFX: $JAVAFX_PATH${NC}"

# Compile
echo ""
echo "📦 Compiling..."
mkdir -p build
javac -d build \
    --module-path "$JAVAFX_PATH/lib" \
    --add-modules javafx.controls,javafx.graphics,javafx.media \
    -encoding UTF-8 \
    src/com/deadball/utils/GameConstants.java \
    src/com/deadball/entities/Entity.java \
    src/com/deadball/entities/Player.java \
    src/com/deadball/entities/Ball.java \
    src/com/deadball/entities/Goalkeeper.java \
    src/com/deadball/entities/Goal.java \
    src/com/deadball/audio/SoundManager.java \
    src/com/deadball/ui/HUD.java \
    src/com/deadball/core/ShotHistory.java \
    src/com/deadball/core/HighScore.java \
    src/com/deadball/core/Game.java \
    src/com/deadball/scene3d/GameScene3D.java \
    src/com/deadball/Main.java

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Compilation successful!${NC}"
else
    echo -e "${RED}❌ Compilation failed!${NC}"
    exit 1
fi

# Run
echo ""
echo "▶️  Launching game..."
java \
    --module-path "$JAVAFX_PATH/lib" \
    --add-modules javafx.controls,javafx.graphics,javafx.media \
    -cp build \
    com.deadball.Main

echo -e "${GREEN}Game ended.${NC}"
