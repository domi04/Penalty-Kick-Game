@echo off
REM Dead Ball - Build and Run Script for Windows
REM This script compiles the Java project and runs it with JavaFX

setlocal enabledelayedexpansion

REM Colors for output (Windows doesn't support ANSI by default, so we use text)
echo.
echo Dead Ball - Game Builder
echo ============================

set JAVAFX_PATH=.\javafx-sdk-26

if not exist "%JAVAFX_PATH%" (
    echo.
    echo Error: JavaFX not found at: %JAVAFX_PATH%
    echo Update build.bat with your local JavaFX SDK path.
    pause
    exit /b 1
)

echo Using local JavaFX: %JAVAFX_PATH%

REM Compile
echo.
echo Compiling...
if not exist build mkdir build

javac -d build ^
    --module-path "%JAVAFX_PATH%\lib" ^
    --add-modules javafx.controls,javafx.graphics ^
    -encoding UTF-8 ^
    src\com\deadball\utils\GameConstants.java ^
    src\com\deadball\entities\Entity.java ^
    src\com\deadball\entities\Player.java ^
    src\com\deadball\entities\Ball.java ^
    src\com\deadball\entities\Goalkeeper.java ^
    src\com\deadball\entities\Goal.java ^
    src\com\deadball\ui\HUD.java ^
    src\com\deadball\core\Game.java ^
    src\com\deadball\scene3d\GameScene3D.java ^
    src\com\deadball\Main.java

if %errorlevel% equ 0 (
    echo Compilation successful!
) else (
    echo.
    echo Error: Compilation failed!
    pause
    exit /b 1
)

REM Run
echo.
echo Launching game...
java ^
    --module-path "%JAVAFX_PATH%\lib" ^
    --add-modules javafx.controls,javafx.graphics ^
    -cp build ^
    com.deadball.Main

echo.
echo Game ended.
pause
