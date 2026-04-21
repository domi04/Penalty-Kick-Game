package com.deadball.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import com.deadball.utils.GameConstants;

public class HUD {
    private int playerScore;
    private int aiScore;
    private double powerBarValue;
    private int currentRound;
    private int currentLevel;
    private int levelCount;
    private double pressure;
    private boolean[] roundResults; // true = goal, false = miss
    private String resultMessage;
    private double resultMessageTimer;
    
    public HUD() {
        this.playerScore = 0;
        this.aiScore = 0;
        this.powerBarValue = 0.0;
        this.currentRound = 1;
        this.currentLevel = 1;
        this.levelCount = GameConstants.LEVEL_COUNT;
        this.roundResults = new boolean[GameConstants.ROUNDS_PER_LEVEL];
        this.resultMessage = "";
        this.resultMessageTimer = 0.0;
    }
    
    public void setScores(int playerScore, int aiScore) {
        this.playerScore = playerScore;
        this.aiScore = aiScore;
    }
    
    public void setPowerBar(double value) {
        this.powerBarValue = Math.max(0.0, Math.min(1.0, value));
    }
    
    public void setCurrentRound(int round) {
        this.currentRound = Math.max(1, Math.min(GameConstants.ROUNDS_PER_LEVEL, round));
    }

    public void setLevel(int level, int total) {
        this.currentLevel = Math.max(1, level);
        this.levelCount = Math.max(1, total);
    }

    public void setPressure(double value) {
        this.pressure = Math.max(0.0, Math.min(1.0, value));
    }

    /** Clears per-level round icons (used when a new level starts or campaign restarts). */
    public void resetRoundIcons() {
        roundResults = new boolean[GameConstants.ROUNDS_PER_LEVEL];
        currentRound = 1;
    }
    
    public void recordRoundResult(boolean scored) {
        if (currentRound <= GameConstants.ROUNDS_PER_LEVEL) {
            roundResults[currentRound - 1] = scored;
        }
    }
    
    public void showResultMessage(String message, double duration) {
        this.resultMessage = message;
        this.resultMessageTimer = duration;
    }
    
    public void update(double deltaTime) {
        if (resultMessageTimer > 0) {
            resultMessageTimer -= deltaTime;
        }
    }
    
    public void render(GraphicsContext gc) {
        // Score display (top center)
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 32));
        gc.setTextAlign(TextAlignment.CENTER);
        String scoreText = playerScore + " - " + aiScore;
        gc.fillText(scoreText, GameConstants.SCREEN_WIDTH / 2.0, 50);
        
        // Player/AI labels
        gc.setFont(Font.font("Arial", 14));
        gc.fillText("You", GameConstants.SCREEN_WIDTH / 2.0 - 40, 35);
        gc.fillText("AI", GameConstants.SCREEN_WIDTH / 2.0 + 40, 35);
        
        // Level + round indicator (top left)
        gc.setFont(Font.font("Arial", 16));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(Color.web(GameConstants.HUD_ACCENT));
        gc.fillText("Level " + currentLevel + " / " + levelCount, 20, 40);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 13));
        gc.fillText("Round: " + currentRound + " / " + GameConstants.ROUNDS_PER_LEVEL, 20, 60);
        
        // Power bar (left edge)
        drawPowerBar(gc);

        // Pressure meter (right edge)
        drawPressureMeter(gc);
        
        // Round result icons (top, below score)
        drawRoundIcons(gc);

        gc.setFont(Font.font("Arial", 11));
        gc.setFill(Color.web("#c8c8c8"));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("←→↑↓ aim (miss wide/high)   SPACE shoot   Q / ESC quit",
                GameConstants.SCREEN_WIDTH / 2.0, GameConstants.SCREEN_HEIGHT - 12);

        // Result message (center screen)
        if (resultMessageTimer > 0) {
            drawResultMessage(gc);
        }
    }
    
    private void drawPowerBar(GraphicsContext gc) {
        int barX = 20;
        int barY = 100;
        int barWidth = 30;
        int barHeight = 150;
        
        // Background (dark)
        gc.setFill(Color.web("#333333"));
        gc.fillRect(barX, barY, barWidth, barHeight);
        
        // Border
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRect(barX, barY, barWidth, barHeight);
        
        // Power fill (green to red)
        double fillHeight = powerBarValue * barHeight;
        Color barColor = Color.web("#00FF00");
        if (powerBarValue > 0.7) {
            barColor = Color.web("#FFAA00");
        }
        if (powerBarValue > 0.85) {
            barColor = Color.web(GameConstants.HUD_ACCENT_SOFT);
        }
        
        gc.setFill(barColor);
        gc.fillRect(barX + 2, barY + barHeight - fillHeight, barWidth - 4, fillHeight);
        
        // Sweet spot indicator (top region)
        gc.setStroke(Color.web(GameConstants.HUD_ACCENT));
        gc.setLineWidth(2);
        double sweetSpotStart = barY + barHeight * 0.2;
        gc.strokeLine(barX, sweetSpotStart, barX + barWidth, sweetSpotStart);
        gc.setFill(Color.web(GameConstants.HUD_ACCENT_SOFT));
        gc.setFont(Font.font("Arial", 8));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("Sweet Spot", barX + 34, sweetSpotStart - 5);
    }
    
    private void drawPressureMeter(GraphicsContext gc) {
        int barWidth = 30;
        int barHeight = 150;
        int barX = GameConstants.SCREEN_WIDTH - 20 - barWidth;
        int barY = 100;

        gc.setFill(Color.web("#333333"));
        gc.fillRect(barX, barY, barWidth, barHeight);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRect(barX, barY, barWidth, barHeight);

        double fillHeight = pressure * barHeight;
        Color fill;
        if (pressure < 0.33) {
            fill = Color.web("#4cd47a"); // calm green
        } else if (pressure < 0.66) {
            fill = Color.web("#e88850"); // tense (orange, not yellow)
        } else {
            fill = Color.web("#e85050"); // panic red
        }
        gc.setFill(fill);
        gc.fillRect(barX + 2, barY + barHeight - fillHeight, barWidth - 4, fillHeight);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 10));
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText("PRESSURE", barX - 6, barY + 12);
        gc.setFont(Font.font("Arial", 9));
        gc.setFill(Color.web("#c8c8c8"));
        gc.fillText(String.format("%d%%", (int) Math.round(pressure * 100)), barX - 6, barY + 26);
    }

    private void drawRoundIcons(GraphicsContext gc) {
        int iconStartX = GameConstants.SCREEN_WIDTH / 2 - 100;
        int iconY = 80;
        int iconSpacing = 40;
        
        gc.setFont(Font.font("Arial", 12));
        gc.setTextAlign(TextAlignment.CENTER);
        
        for (int i = 0; i < GameConstants.ROUNDS_PER_LEVEL; i++) {
            int x = iconStartX + (i * iconSpacing);
            
            // Draw round indicator circle
            if (i < currentRound - 1) {
                // Completed round
                if (roundResults[i]) {
                    gc.setFill(Color.GREEN);
                    gc.fillOval(x - 12, iconY - 12, 24, 24);
                    gc.setFill(Color.WHITE);
                    gc.fillText("✓", x, iconY + 7);
                } else {
                    gc.setFill(Color.RED);
                    gc.fillOval(x - 12, iconY - 12, 24, 24);
                    gc.setFill(Color.WHITE);
                    gc.fillText("✗", x, iconY + 7);
                }
            } else if (i == currentRound - 1) {
                // Current round
                gc.setStroke(Color.web(GameConstants.HUD_ACCENT));
                gc.setLineWidth(3);
                gc.strokeOval(x - 12, iconY - 12, 24, 24);
                gc.setFill(Color.WHITE);
                gc.fillText((i + 1) + "", x, iconY + 7);
            } else {
                // Future round
                gc.setFill(Color.web("#666666"));
                gc.fillOval(x - 12, iconY - 12, 24, 24);
                gc.setFill(Color.WHITE);
                gc.fillText((i + 1) + "", x, iconY + 7);
            }
        }
    }
    
    private void drawResultMessage(GraphicsContext gc) {
        gc.setFont(Font.font("Arial", 48));
        gc.setTextAlign(TextAlignment.CENTER);
        
        // Background semi-transparent
        gc.setFill(Color.web("#000000", 0.7));
        gc.fillRect(0, GameConstants.SCREEN_HEIGHT / 2 - 60,
                   GameConstants.SCREEN_WIDTH, 120);
        
        if (resultMessage.contains("GOAL")) {
            gc.setFill(Color.web(GameConstants.HUD_SUCCESS));
        } else if (resultMessage.contains("SAVED")) {
            gc.setFill(Color.web("#6ec8ff"));
        } else if (resultMessage.contains("POST")) {
            gc.setFill(Color.web("#ffaa44"));
        } else if (resultMessage.contains("OVER")) {
            gc.setFill(Color.web("#ffcc66"));
        } else if (resultMessage.contains("WIDE")) {
            gc.setFill(Color.web("#ff6666"));
        } else if (resultMessage.contains("SHORT")) {
            gc.setFill(Color.web("#cc88aa"));
        } else {
            gc.setFill(Color.RED);
        }

        gc.fillText(resultMessage,
                   GameConstants.SCREEN_WIDTH / 2.0,
                   GameConstants.SCREEN_HEIGHT / 2.0 + 20);
    }
    
    public void reset() {
        playerScore = 0;
        aiScore = 0;
        powerBarValue = 0.0;
        currentRound = 1;
        currentLevel = 1;
        levelCount = GameConstants.LEVEL_COUNT;
        pressure = 0.0;
        roundResults = new boolean[GameConstants.ROUNDS_PER_LEVEL];
        resultMessage = "";
        resultMessageTimer = 0.0;
    }
}
