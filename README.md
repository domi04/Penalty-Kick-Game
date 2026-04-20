# Dead Ball - Penalty Kick Game Prototype (PD3)

## Project Structure

```
code/
├── src/
│   └── com/deadball/
│       ├── core/
│       │   └── Game.java              # Main game loop and state management
│       ├── entities/
│       │   ├── Entity.java            # Abstract base class for all game objects
│       │   ├── Player.java            # Player/Striker entity
│       │   ├── Ball.java              # Ball physics and movement
│       │   ├── Goalkeeper.java        # AI goalkeeper with dive logic
│       │   └── Goal.java              # Goal frame and collision detection
│       ├── ui/
│       │   └── HUD.java               # Heads-up display (score, power bar, etc)
│       ├── utils/
│       │   └── GameConstants.java     # All game configuration constants
│       └── Main.java                  # JavaFX entry point
├── assets/                            # Placeholder for sprites/audio/images
├── build/                             # Compiled .class files (generated)
├── build.sh                           # Build and run script
└── README.md                          # This file
```

## Requirements

- **Java 17 LTS** or higher
- **JavaFX 17 SDK** or higher
- **macOS, Linux, or Windows** (with appropriate setup)

## Installation

### macOS (Using Homebrew)

```bash
# Install Java 17
brew install openjdk@17

# Install JavaFX SDK
brew install javafx-sdk

# Make the build script executable
chmod +x code/build.sh
```

### Manual JavaFX Setup

Download JavaFX SDK from: https://gluonhq.com/products/javafx/

## Compilation & Running

### Using the Build Script (Mac/Linux)

```bash
cd code
./build.sh
```

### For windows
```cmd
build.bat
```

This file is AI generated and not tested on a windows machine so it may require adjustments to work on your specific system. Make sure to set the correct path to your JavaFX SDK in the script.

### Manual Compilation

```bash
# Set JavaFX path
export JAVAFX_PATH=/path/to/javafx-sdk

# Compile
mkdir -p build
javac -d build \
    --module-path "$JAVAFX_PATH/lib" \
    --add-modules javafx.controls,javafx.graphics,javafx.fxml \
    -encoding UTF-8 \
    src/com/deadball/**/*.java

# Run
java --module-path "$JAVAFX_PATH/lib" \
    --add-modules javafx.controls,javafx.graphics,javafx.fxml \
    -cp build \
    com.deadball.Main
```

## Game Controls

| Key | Action |
|-----|--------|
| **← / → / ↑ / ↓** | Move aiming reticle left/right/up/down |
| **SPACE** | Shoot (locks power bar and launches ball) |
| **Q** | Quit game |

## Gameplay

1. **Main Menu**: Press SPACE to start a new match
2. **Aiming**: Use ← / → arrow keys to position the reticle across the goal
3. **Power Management**: The power bar oscillates automatically - time your shot! Spacebar launches the ball with the current power level
4. **Keeper AI**: The goalkeeper dives randomly (40% left, 40% right, 20% center)
5. **Scoring**: Ball must enter goal AND keeper cannot block
6. **5 Rounds**: Each side takes 5 shots. Most goals wins!

## Game Architecture

### Core Classes

- **Game.java**: Main game loop, state machine (MENU/PLAYING/RESULT), input handling
- **Player.java**: Manages reticle position, power bar state, shot history
- **Ball.java**: Ball physics, trajectory calculation, flight detection
- **Goalkeeper.java**: AI dive prediction, blocking logic
- **Goal.java**: Goal frame rendering, zone detection, flash effects
- **HUD.java**: Score display, power bar, round indicators, result messages

### State Flow

```
START
  ↓
MENU (Player presses SPACE)
  ↓
PLAYING
  ├─ AIM Phase (Reticle moves, power oscillates)
  ├─ SHOOT Phase (Ball flies to goal)
  ├─ RESULT Phase (Goal/Save/Miss display, 2 sec delay)
  └─ Next round (repeat 5×)
  ↓
RESULT SCREEN (Win/Lose, option to replay)
```

## Features

✅ Working

- Main menu with Play/Quit
- Real-time aiming with arrow keys
- Oscillating power bar with SPACE to shoot
- Ball physics and trajectory
- Goalkeeper AI (random dive selection)
- Goal detection and collision
- Score tracking over 5 rounds
- Win/Lose determination
- Round indicator with result icons
- HUD with all relevant information

⏳ Planned

- Adaptive goalkeeper AI (learns from player patterns)
- Sprites and animations for player, ball, keeper, goal and crowd
- Pressure meter (increases after misses)
- Keeper tendency hint (heatmap overlay)
- Audio effects (crowd cheer, ball kick, etc)
- Advanced animations
- Multiple difficulty levels
- High score saving

## References

- [JavaFX Documentation](https://openjfx.io/)
- [Java 17 Documentation](https://docs.oracle.com/en/java/javase/17/)
- Game Design Document: `Team Deadball_GDD_Phase2.docx`
- Phase 1 Proposal: `DeadBall_Phase1_Proposal.docx`

---
