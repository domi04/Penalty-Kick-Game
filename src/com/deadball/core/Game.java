package com.deadball.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import com.deadball.audio.SoundManager;
import com.deadball.entities.*;
import com.deadball.ui.HUD;
import com.deadball.utils.GameConstants;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class Game {
    private int playerScore; // goals in current level
    private int aiScore; // ai goals in current level (non-goal outcomes)
    private int currentRound; // 1..ROUNDS_PER_LEVEL inside a level
    private int currentLevel; // 1..LEVEL_COUNT (campaign progress)
    private String gameState; // MENU, PLAYING, LEVEL_COMPLETE, LEVEL_FAILED, RESULT
    private Player player;
    private Ball ball;
    private Goalkeeper goalkeeper;
    private Goal goal;
    private HUD hud;
    /** Per-level aim / outcome tracking used by adaptive keeper and HUD heatmap. */
    private final ShotHistory shotHistory = new ShotHistory();
    /** Player pressure 0..1. Rises on saves/misses, falls on goals, reset per level. */
    private double pressure;
    /** Sum of goals scored across all completed levels in the current campaign run. */
    private int campaignGoalTotal;
    /** Last campaign total persisted to disk and shown on the result screen. */
    private int lastCampaignGoals;
    private boolean lastCampaignWasNewHighScore;
    private final HighScore highScore = new HighScore();
    private final SoundManager sound = new SoundManager(Paths.get("assets", "sounds"));
    
    // Input state
    private Set<KeyCode> keysPressed;
    private boolean spacePressedThisFrame;
    private boolean exitRequested;
    
    // Game phase state
    private String gamePhase; // AIM, SHOOT, RESULT
    private double resultDisplayTimer;
    /** Between-level banner auto-advance timer (LEVEL_COMPLETE). */
    private double levelTransitionTimer;
    private String lastResultMessage;
    /** Captured when the player shoots; straight paths can clip the net on the way to a wide aim. */
    private double lastShotAimX;
    private double lastShotAimY;

    public Game() {
        this.playerScore = 0;
        this.aiScore = 0;
        this.currentRound = 1;
        this.currentLevel = 1;
        this.gameState = GameConstants.STATE_MENU;
        this.keysPressed = new HashSet<>();
        this.spacePressedThisFrame = false;
        this.exitRequested = false;
        this.gamePhase = "AIM";

        initializeEntities();
        highScore.load();
    }
    
    private void initializeEntities() {
        player = new Player(GameConstants.PENALTY_SPOT_X, GameConstants.PENALTY_SPOT_Y);
        ball = new Ball(GameConstants.PENALTY_SPOT_X, 
                       GameConstants.PENALTY_SPOT_Y);
        goalkeeper = new Goalkeeper((GameConstants.GOAL_LEFT + GameConstants.GOAL_RIGHT) / 2.0,
                                   GameConstants.GOAL_TOP + 40);
        goal = new Goal();
        hud = new HUD();
    }
    
    /** Starts the campaign at level 1. */
    public void startNewGame() {
        currentLevel = 1;
        campaignGoalTotal = 0;
        hud.reset();
        startLevel(currentLevel);
    }

    /** Sets up a fresh shootout for the given level (scores/round reset; HUD clears icons). */
    private void startLevel(int level) {
        currentLevel = level;
        playerScore = 0;
        aiScore = 0;
        currentRound = 1;
        gameState = GameConstants.STATE_PLAYING;
        gamePhase = "AIM";
        resultDisplayTimer = 0.0;
        levelTransitionTimer = 0.0;

        hud.resetRoundIcons();
        hud.setScores(playerScore, aiScore);
        hud.setCurrentRound(currentRound);
        hud.setLevel(currentLevel, GameConstants.LEVEL_COUNT);

        // Fresh history each level so adaptive keeper (L3) learns from this level's shots.
        shotHistory.clear();
        pressure = 0.0;

        // Per-level keeper tuning: reach multiplier is level-driven, probabilities only get
        // history-biased in L3 (set just before the dive in executeShot()).
        double[] reach = GameConstants.KEEPER_REACH_MULT_BY_LEVEL;
        int reachIdx = Math.min(reach.length - 1, Math.max(0, currentLevel - 1));
        goalkeeper.setReachMultiplier(reach[reachIdx]);
        goalkeeper.resetProbabilitiesToBaseline();

        resetRound();
    }

    /** Repeat the current level after a failure. */
    private void retryLevel() {
        startLevel(currentLevel);
    }

    private void resetRound() {
        player.resetRound();
        goalkeeper.reset();
        ball.clearShotRay();
        ball.setInFlight(false);
        ball.setPosition(GameConstants.PENALTY_SPOT_X, GameConstants.PENALTY_SPOT_Y);
        gamePhase = "AIM";
        resultDisplayTimer = 0.0;
    }
    
    public void handleKeyPress(KeyEvent event) {
        KeyCode code = event.getCode();
        keysPressed.add(code);

        if (code == KeyCode.SPACE) {
            spacePressedThisFrame = true;
        }

        // Q/ESC from the menu or the campaign-end screen quits the app.
        if ((code == KeyCode.Q || code == KeyCode.ESCAPE)
                && (gameState.equals(GameConstants.STATE_RESULT)
                        || gameState.equals(GameConstants.STATE_MENU))) {
            exitRequested = true;
            return;
        }

        // Q/ESC from a failed level returns to the main menu (not an app quit).
        if (gameState.equals(GameConstants.STATE_LEVEL_FAILED)
                && (code == KeyCode.Q || code == KeyCode.ESCAPE)) {
            gameState = GameConstants.STATE_MENU;
            consumeSpaceAfterStartingMatch();
            return;
        }

        // SPACE/ENTER on the campaign-end screen starts a brand-new campaign.
        if (gameState.equals(GameConstants.STATE_RESULT)
                && (code == KeyCode.SPACE || code == KeyCode.ENTER)) {
            startNewGame();
            consumeSpaceAfterStartingMatch();
            return;
        }

        // SPACE/ENTER on a failed level retries that level.
        if (gameState.equals(GameConstants.STATE_LEVEL_FAILED)
                && (code == KeyCode.SPACE || code == KeyCode.ENTER)) {
            retryLevel();
            consumeSpaceAfterStartingMatch();
            return;
        }

        if (gameState.equals(GameConstants.STATE_MENU)) {
            if (code == KeyCode.SPACE || code == KeyCode.ENTER) {
                startNewGame();
                consumeSpaceAfterStartingMatch();
            }
        }
    }

    /** SPACE/ENTER also set shot flags; clear so the first frame of play is not an auto-shot. */
    private void consumeSpaceAfterStartingMatch() {
        spacePressedThisFrame = false;
        keysPressed.remove(KeyCode.SPACE);
    }
    
    public void handleKeyRelease(KeyEvent event) {
        keysPressed.remove(event.getCode());
    }

    public void handleMouseClick(MouseEvent event) {
        if (!gameState.equals(GameConstants.STATE_MENU)) {
            return;
        }

        double mouseX = event.getX();
        double mouseY = event.getY();
        double buttonX = GameConstants.SCREEN_WIDTH / 2.0 - 120;
        double playButtonY = GameConstants.SCREEN_HEIGHT / 2.0 + 20;
        double exitButtonY = GameConstants.SCREEN_HEIGHT / 2.0 + 90;
        double buttonWidth = 240;
        double buttonHeight = 44;

        if (isInsideButton(mouseX, mouseY, buttonX, playButtonY, buttonWidth, buttonHeight)) {
            startNewGame();
            consumeSpaceAfterStartingMatch();
        } else if (isInsideButton(mouseX, mouseY, buttonX, exitButtonY, buttonWidth, buttonHeight)) {
            exitRequested = true;
        }
    }

    private boolean isInsideButton(double x, double y, double buttonX, double buttonY, double width, double height) {
        return x >= buttonX && x <= buttonX + width && y >= buttonY && y <= buttonY + height;
    }
    
    public void update(double deltaTime) {
        // LEVEL_COMPLETE auto-advances after a short celebration banner.
        if (gameState.equals(GameConstants.STATE_LEVEL_COMPLETE)) {
            levelTransitionTimer -= deltaTime;
            if (levelTransitionTimer <= 0) {
                startLevel(currentLevel + 1);
            }
            return;
        }

        if (!gameState.equals(GameConstants.STATE_PLAYING)) {
            return;
        }
        
        // Update entities
        player.update(deltaTime);
        ball.update(deltaTime);
        goalkeeper.update(deltaTime);
        goal.update(deltaTime);
        hud.update(deltaTime);
        
        double moveH = 0.0;
        if (keysPressed.contains(KeyCode.LEFT)) {
            moveH -= 1.0;
        }
        if (keysPressed.contains(KeyCode.RIGHT)) {
            moveH += 1.0;
        }
        double moveV = 0.0;
        if (keysPressed.contains(KeyCode.UP)) {
            moveV -= 1.0;
        }
        if (keysPressed.contains(KeyCode.DOWN)) {
            moveV += 1.0;
        }
        player.setReticleMovement(moveH, moveV);
        
        // Game phase logic
        if (gamePhase.equals("AIM")) {
            // In AIM phase, player moves reticle and power bar oscillates
            // When space is pressed, shoot immediately with current power
            if (spacePressedThisFrame) {
                gamePhase = "SHOOT";
                executeShot();
            }
        } else if (gamePhase.equals("SHOOT")) {
            if (!ball.isInFlight()) {
                gamePhase = "RESULT";
                evaluateShot();
                resultDisplayTimer = 2.0;
            }
        } else if (gamePhase.equals("RESULT")) {
            resultDisplayTimer -= deltaTime;
            if (resultDisplayTimer <= 0) {
                currentRound++;
                if (currentRound > GameConstants.ROUNDS_PER_LEVEL) {
                    finishLevel();
                } else {
                    resetRound();
                }
            }
        }
        
        spacePressedThisFrame = false;
        
        // Update HUD
        hud.setScores(playerScore, aiScore);
        hud.setPowerBar(player.getPowerLevel());
        hud.setCurrentRound(currentRound);
        hud.setLevel(currentLevel, GameConstants.LEVEL_COUNT);
        hud.setPressure(pressure);
        player.setOscillationSpeedMultiplier(1.0 + pressure * GameConstants.PRESSURE_POWER_SPEEDUP);
    }

    /**
     * Called after the last round of a level. Strict win rule: playerScore must be greater than
     * aiScore (a tie fails the level). On a win, advance to the next level or finish the campaign;
     * on a loss, show the failed-level screen (retry or return to menu).
     */
    private void finishLevel() {
        boolean levelWon = playerScore > aiScore;
        if (levelWon) {
            campaignGoalTotal += playerScore;
            if (currentLevel >= GameConstants.LEVEL_COUNT) {
                lastCampaignGoals = campaignGoalTotal;
                lastCampaignWasNewHighScore = highScore.submitCampaignResult(campaignGoalTotal);
                endGame();
                sound.play(SoundManager.Clip.CAMPAIGN_COMPLETE);
            } else {
                gameState = GameConstants.STATE_LEVEL_COMPLETE;
                levelTransitionTimer = GameConstants.LEVEL_TRANSITION_SECONDS;
                hud.showResultMessage("", 0.0);
                sound.play(SoundManager.Clip.LEVEL_COMPLETE);
            }
        } else {
            gameState = GameConstants.STATE_LEVEL_FAILED;
            hud.showResultMessage("", 0.0);
            sound.play(SoundManager.Clip.LEVEL_FAILED);
        }
    }
    
    private void executeShot() {
        double targetX = player.getReticleX();
        double targetY = Math.min(player.getReticleY(), GameConstants.GOAL_BOTTOM);
        lastShotAimX = targetX;
        lastShotAimY = targetY;
        double power = player.getPowerLevel();

        ball.launch(targetX, targetY, power, wasAimedInsideGoalFrame(targetX, targetY));
        sound.play(SoundManager.Clip.KICK);

        // Adaptive keeper (L3): bias dive probabilities toward the zones the player has favoured
        // so far this level. Earlier levels keep the baseline distribution.
        if (currentLevel >= 3 && shotHistory.totalShots() > 0) {
            double w = Math.min(1.0,
                    shotHistory.totalShots() / (double) GameConstants.KEEPER_ADAPTIVE_RAMP_SHOTS)
                    * GameConstants.KEEPER_ADAPTIVE_BIAS;
            double left = (1.0 - w) * GameConstants.KEEPER_LEFT_PROBABILITY
                    + w * shotHistory.shareOfShots(ShotHistory.Zone.LEFT);
            double center = (1.0 - w) * GameConstants.KEEPER_CENTER_PROBABILITY
                    + w * shotHistory.shareOfShots(ShotHistory.Zone.CENTER);
            double right = (1.0 - w) * GameConstants.KEEPER_RIGHT_PROBABILITY
                    + w * shotHistory.shareOfShots(ShotHistory.Zone.RIGHT);
            goalkeeper.setDiveProbabilities(left, center, right);
        } else {
            goalkeeper.resetProbabilitiesToBaseline();
        }

        goalkeeper.decideDive();
        
        hud.showResultMessage("", 0.0);
    }
    
    private void evaluateShot() {
        boolean ballInGoal = goal.checkBallInGoal(ball);
        boolean aimInsideGoal = wasAimedInsideGoalFrame();
        boolean keeperSaved = ballInGoal && goalkeeper.isBlocking(ball);
        boolean countsAsGoal = ballInGoal && aimInsideGoal && !keeperSaved;

        if (countsAsGoal) {
            playerScore++;
            lastResultMessage = "GOAL!";
            goal.triggerFlash();
            hud.recordRoundResult(true);
            pressure += GameConstants.PRESSURE_ON_GOAL;
            sound.play(SoundManager.Clip.GOAL);
        } else {
            if (keeperSaved) {
                lastResultMessage = "SAVED!";
                pressure += GameConstants.PRESSURE_ON_SAVE;
                sound.play(SoundManager.Clip.SAVE);
            } else {
                lastResultMessage = classifyMiss(ball);
                pressure += GameConstants.PRESSURE_ON_MISS;
                sound.play(SoundManager.Clip.MISS);
            }
            aiScore++;
            hud.recordRoundResult(false);
        }
        pressure = Math.max(0.0, Math.min(1.0, pressure));

        shotHistory.record(lastShotAimX, countsAsGoal);

        hud.showResultMessage(lastResultMessage, 2.0);
    }

    public double getPressure() {
        return pressure;
    }

    public ShotHistory getShotHistory() {
        return shotHistory;
    }

    private static boolean wasAimedInsideGoalFrame(double aimX, double aimY) {
        return aimX >= GameConstants.GOAL_LEFT && aimX <= GameConstants.GOAL_RIGHT
                && aimY >= GameConstants.GOAL_TOP && aimY <= GameConstants.GOAL_BOTTOM;
    }

    private boolean wasAimedInsideGoalFrame() {
        return wasAimedInsideGoalFrame(lastShotAimX, lastShotAimY);
    }

    private static boolean hitWoodwork(Ball ball) {
        double bx = ball.getPosX();
        double by = ball.getPosY();
        double tx = GameConstants.POST_HIT_TOLERANCE_X;
        double ty = GameConstants.POST_HIT_TOLERANCE_Y;
        int gl = GameConstants.GOAL_LEFT;
        int gr = GameConstants.GOAL_RIGHT;
        int gt = GameConstants.GOAL_TOP;
        int gb = GameConstants.GOAL_BOTTOM;

        boolean leftPost = Math.abs(bx - gl) <= tx && by >= gt - ty && by <= gb + ty;
        boolean rightPost = Math.abs(bx - gr) <= tx && by >= gt - ty && by <= gb + ty;
        // Crossbar only if the ball is not clearly over the bar (by < gt); those are OVER, not POST.
        boolean crossbar = by >= gt && by <= gt + ty
                && bx >= gl - tx && bx <= gr + tx;
        return leftPost || rightPost || crossbar;
    }

    /**
     * Non-goal outcomes (not a keeper save — those are handled before this is called).
     */
    private static String classifyMiss(Ball ball) {
        double bx = ball.getPosX();
        double by = ball.getPosY();
        int gt = GameConstants.GOAL_TOP;
        int gb = GameConstants.GOAL_BOTTOM;
        int gl = GameConstants.GOAL_LEFT;
        int gr = GameConstants.GOAL_RIGHT;

        boolean wideOfPosts = bx < gl - 20 || bx > gr + 20;
        if (by < gt) {
            if (wideOfPosts) {
                return "WIDE!";
            }
            return "OVER THE BAR!";
        }
        if (hitWoodwork(ball)) {
            return "POST!";
        }
        if (wideOfPosts) {
            if (by >= gt - 40 && by <= gb + 45) {
                return "WIDE!";
            }
        }
        if (by > gb + 12) {
            return "SHORT!";
        }
        return "MISS!";
    }

    private void endGame() {
        gameState = GameConstants.STATE_RESULT;
    }

    /** 2D overlay (HUD, menus, aim reticle). World is drawn in the JavaFX 3D SubScene. */
    public void renderUiOverlay(GraphicsContext gc) {
        double w = GameConstants.SCREEN_WIDTH;
        double h = GameConstants.SCREEN_HEIGHT;

        if (gameState.equals(GameConstants.STATE_MENU)) {
            renderMenu(gc);
        } else if (gameState.equals(GameConstants.STATE_PLAYING)) {
            gc.clearRect(0, 0, w, h);
            hud.render(gc);
            if ("AIM".equals(gamePhase)) {
                renderAimHeatmap(gc);
            }
        } else if (gameState.equals(GameConstants.STATE_LEVEL_COMPLETE)) {
            gc.clearRect(0, 0, w, h);
            hud.render(gc);
            renderLevelBanner(gc, "LEVEL " + currentLevel + " COMPLETE!",
                    Color.GOLD,
                    "Next: " + GameConstants.levelDescription(currentLevel + 1));
        } else if (gameState.equals(GameConstants.STATE_LEVEL_FAILED)) {
            gc.clearRect(0, 0, w, h);
            hud.render(gc);
            renderLevelBanner(gc, "LEVEL " + currentLevel + " FAILED",
                    Color.web("#ff6666"),
                    "SPACE retry   Q / ESC main menu");
        } else if (gameState.equals(GameConstants.STATE_RESULT)) {
            gc.clearRect(0, 0, w, h);
            hud.render(gc);
            renderResultScreen(gc);
        }
    }

    public Ball getBall() {
        return ball;
    }

    public Player getPlayer() {
        return player;
    }

    public Goalkeeper getGoalkeeper() {
        return goalkeeper;
    }

    public Goal getGoal() {
        return goal;
    }
    
    private void renderMenu(GraphicsContext gc) {
        // Clear
        gc.setFill(Color.web("#003300"));
        gc.fillRect(0, 0, GameConstants.SCREEN_WIDTH, GameConstants.SCREEN_HEIGHT);
        
        // Stadium background effect
        gc.setFill(Color.web("#1a1a1a"));
        gc.fillRect(0, GameConstants.SCREEN_HEIGHT * 0.6,
                   GameConstants.SCREEN_WIDTH, GameConstants.SCREEN_HEIGHT * 0.4);
        
        // Title
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 64));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText("DEAD BALL", GameConstants.SCREEN_WIDTH / 2.0, 150);
        
        // Subtitle
        gc.setFont(javafx.scene.text.Font.font("Arial", 24));
        gc.fillText("A Penalty Kick Showdown", GameConstants.SCREEN_WIDTH / 2.0, 200);

        gc.setFont(javafx.scene.text.Font.font("Arial", 16));
        gc.setFill(Color.web("#cccccc"));
        gc.fillText(GameConstants.LEVEL_COUNT + "-level campaign  ·  "
                        + GameConstants.ROUNDS_PER_LEVEL + " rounds per level  ·  outscore the keeper to advance",
                GameConstants.SCREEN_WIDTH / 2.0, 230);

        gc.setFont(javafx.scene.text.Font.font("Arial", 14));
        gc.setFill(Color.web("#aad59c"));
        double ly = 270;
        for (int i = 1; i <= GameConstants.LEVEL_COUNT; i++) {
            gc.fillText(GameConstants.levelDescription(i),
                    GameConstants.SCREEN_WIDTH / 2.0, ly);
            ly += 22;
        }

        gc.setFont(javafx.scene.text.Font.font("Arial", 16));
        gc.setFill(Color.web("#ffdd55"));
        gc.fillText("High Score: " + highScore.getBestCampaignGoals() + " goals (campaign)",
                GameConstants.SCREEN_WIDTH / 2.0, ly + 10);

        // Instructions
        gc.setFont(javafx.scene.text.Font.font("Arial", 20));
        gc.setFill(Color.LIGHTGREEN);
        gc.fillText("Press SPACE to Play", GameConstants.SCREEN_WIDTH / 2.0,
                   GameConstants.SCREEN_HEIGHT / 2.0);
        gc.fillText("Press Q to Quit", GameConstants.SCREEN_WIDTH / 2.0,
                   GameConstants.SCREEN_HEIGHT / 2.0 + 50);

        double buttonX = GameConstants.SCREEN_WIDTH / 2.0 - 120;
        double playButtonY = GameConstants.SCREEN_HEIGHT / 2.0 + 20;
        double exitButtonY = GameConstants.SCREEN_HEIGHT / 2.0 + 90;
        double buttonWidth = 240;
        double buttonHeight = 44;

        gc.setFill(Color.web("#1f6f3c"));
        gc.fillRoundRect(buttonX, playButtonY, buttonWidth, buttonHeight, 16, 16);
        gc.fillRoundRect(buttonX, exitButtonY, buttonWidth, buttonHeight, 16, 16);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRoundRect(buttonX, playButtonY, buttonWidth, buttonHeight, 16, 16);
        gc.strokeRoundRect(buttonX, exitButtonY, buttonWidth, buttonHeight, 16, 16);
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 22));
        gc.fillText("PLAY", GameConstants.SCREEN_WIDTH / 2.0, playButtonY + 29);
        gc.fillText("EXIT", GameConstants.SCREEN_WIDTH / 2.0, exitButtonY + 29);
    }
    
    private void renderResultScreen(GraphicsContext gc) {
        gc.setFill(Color.web("#000000", 0.8));
        gc.fillRect(0, 0, GameConstants.SCREEN_WIDTH, GameConstants.SCREEN_HEIGHT);

        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setFont(javafx.scene.text.Font.font("Arial", 56));
        gc.setFill(Color.GOLD);
        gc.fillText("CAMPAIGN COMPLETE!", GameConstants.SCREEN_WIDTH / 2.0,
                GameConstants.SCREEN_HEIGHT / 2.0 - 100);

        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 28));
        gc.fillText("All " + GameConstants.LEVEL_COUNT + " levels won",
                GameConstants.SCREEN_WIDTH / 2.0,
                GameConstants.SCREEN_HEIGHT / 2.0 - 30);

        gc.setFont(javafx.scene.text.Font.font("Arial", 34));
        gc.fillText("Total goals: " + lastCampaignGoals,
                GameConstants.SCREEN_WIDTH / 2.0,
                GameConstants.SCREEN_HEIGHT / 2.0 + 30);

        gc.setFont(javafx.scene.text.Font.font("Arial", 22));
        if (lastCampaignWasNewHighScore) {
            gc.setFill(Color.web("#ffd34d"));
            gc.fillText("NEW HIGH SCORE!",
                    GameConstants.SCREEN_WIDTH / 2.0,
                    GameConstants.SCREEN_HEIGHT / 2.0 + 70);
        } else {
            gc.setFill(Color.LIGHTGRAY);
            gc.fillText("High score: " + highScore.getBestCampaignGoals(),
                    GameConstants.SCREEN_WIDTH / 2.0,
                    GameConstants.SCREEN_HEIGHT / 2.0 + 70);
        }

        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 20));
        gc.fillText("Press SPACE to play the campaign again",
                GameConstants.SCREEN_WIDTH / 2.0,
                GameConstants.SCREEN_HEIGHT / 2.0 + 120);
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(javafx.scene.text.Font.font("Arial", 18));
        gc.fillText("Press Q or ESC to quit",
                GameConstants.SCREEN_WIDTH / 2.0,
                GameConstants.SCREEN_HEIGHT / 2.0 + 165);
    }

    /**
     * Draws a three-zone strip near the top showing how many shots this level landed in the
     * left / center / right third of the goal. Darker means fewer shots, brighter red means a
     * zone the player has favoured (and that the L3 keeper will read against them).
     */
    private void renderAimHeatmap(GraphicsContext gc) {
        int boxW = 90;
        int boxH = 26;
        int gap = 4;
        int totalW = boxW * 3 + gap * 2;
        int startX = (GameConstants.SCREEN_WIDTH - totalW) / 2;
        int y = 108;

        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setFont(javafx.scene.text.Font.font("Arial", 11));
        gc.setFill(Color.web("#c8c8c8"));
        gc.fillText("AIM HISTORY (this level)",
                GameConstants.SCREEN_WIDTH / 2.0, y - 4);

        ShotHistory.Zone[] zones = {
                ShotHistory.Zone.LEFT,
                ShotHistory.Zone.CENTER,
                ShotHistory.Zone.RIGHT };
        String[] labels = { "LEFT", "CENTER", "RIGHT" };
        int totalShots = shotHistory.totalShots();

        for (int i = 0; i < 3; i++) {
            int x = startX + i * (boxW + gap);
            int shots = shotHistory.shotsIn(zones[i]);
            int goals = shotHistory.goalsIn(zones[i]);
            double intensity = totalShots == 0 ? 0.0 : (double) shots / totalShots;
            // Base grey -> saturated red as intensity rises.
            int r = (int) (60 + 195 * intensity);
            int g = (int) (60 + 20 * (1 - intensity));
            int b = (int) (70 + 30 * (1 - intensity));
            gc.setFill(Color.rgb(r, g, b, 0.85));
            gc.fillRect(x, y + 4, boxW, boxH);
            gc.setStroke(Color.web("#111111"));
            gc.setLineWidth(1);
            gc.strokeRect(x, y + 4, boxW, boxH);

            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 11));
            gc.fillText(labels[i], x + boxW / 2.0, y + 16);
            gc.setFont(javafx.scene.text.Font.font("Arial", 10));
            gc.setFill(Color.web("#ffe29a"));
            gc.fillText(goals + " / " + shots, x + boxW / 2.0, y + 28);
        }
    }

    /** Shared banner overlay used for LEVEL_COMPLETE and LEVEL_FAILED screens. */
    private void renderLevelBanner(GraphicsContext gc, String headline, Color headlineColor,
                                   String subtitle) {
        gc.setFill(Color.web("#000000", 0.7));
        gc.fillRect(0, GameConstants.SCREEN_HEIGHT / 2.0 - 110,
                GameConstants.SCREEN_WIDTH, 220);

        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setFont(javafx.scene.text.Font.font("Arial", 52));
        gc.setFill(headlineColor);
        gc.fillText(headline, GameConstants.SCREEN_WIDTH / 2.0,
                GameConstants.SCREEN_HEIGHT / 2.0 - 30);

        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 28));
        gc.fillText("Score: " + playerScore + " - " + aiScore,
                GameConstants.SCREEN_WIDTH / 2.0,
                GameConstants.SCREEN_HEIGHT / 2.0 + 20);

        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(javafx.scene.text.Font.font("Arial", 20));
        gc.fillText(subtitle, GameConstants.SCREEN_WIDTH / 2.0,
                GameConstants.SCREEN_HEIGHT / 2.0 + 65);
    }
    
    public String getGameState() {
        return gameState;
    }

    public String getGamePhase() {
        return gamePhase;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public boolean isExitRequested() {
        return exitRequested;
    }
}
