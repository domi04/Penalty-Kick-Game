package com.deadball.utils;

public interface GameConstants {
    // Screen dimensions
    int SCREEN_WIDTH = 1024;
    int SCREEN_HEIGHT = 768;
    
    // Goal dimensions and positioning
    int GOAL_TOP = 150;
    int GOAL_BOTTOM = 550;
    int GOAL_LEFT = 300;
    int GOAL_RIGHT = 724;
    
    // Penalty spot
    int PENALTY_SPOT_X = SCREEN_WIDTH / 2;
    int PENALTY_SPOT_Y = SCREEN_HEIGHT - 100;
    
    // Power bar
    double POWER_OSCILLATION_PERIOD = 1.5; // seconds
    
    // Aiming (screen coords: smaller Y = higher on screen)
    double RETICLE_SPEED = 220.0; // pixels per second, horizontal
    double RETICLE_SPEED_VERTICAL = 200.0;
    /** Aim point can miss wide / high / low (outside the goal frame). */
    double AIM_RETICLE_X_MIN = 120;
    double AIM_RETICLE_X_MAX = 904;
    int AIM_RETICLE_Y_MIN = 20;

    /** Keeper shifts this many pixels when diving (sim space); must overlap ball X to save. */
    double KEEPER_DIVE_SHIFT_X = 125.0;
    /** Half-width of save collision in sim pixels (body + reach). */
    double KEEPER_SAVE_REACH_X = 35.0;
    /** Saves only if ball is not too high (screen Y must be ≥ this; crossbar is GOAL_TOP). */
    int KEEPER_MIN_BALL_Y_FOR_SAVE = GOAL_TOP + 22;
    
    // Ball physics
    double BALL_BASE_SPEED = 800.0; // pixels per second
    /** Speed multiplier at power=0 vs power=1 (applied on top of BALL_BASE_SPEED). */
    double BALL_POWER_SPEED_MIN = 0.42;
    double BALL_POWER_SPEED_MAX = 1.08;
    /** Light air drag per second (keeps ball reaching the goal; still favors high power). */
    double BALL_AIR_DRAG = 0.09;
    /**
     * If speed drops this low while still in front of the goal box, stop (rolled dead on grass).
     * Not applied while the ball is still clearly on its way (above this band).
     */
    double BALL_MIN_SPEED_GROUND = 18.0;
    /** Low-speed stop only after sim Y is inside the goal frame (not in the band just in front). */
    int BALL_ROLL_DEAD_MAX_Y = GOAL_BOTTOM;
    /** Safety: end shot if stuck too long (seconds). */
    double BALL_MAX_FLIGHT_TIME = 6.0;
    /**
     * When aim was inside the goal, keep the ball in flight until it has covered this fraction of the
     * distance to the locked reticle (matches 3D lerp); otherwise the result triggers at the goal mouth.
     */
    double BALL_GOAL_STOP_MIN_PROGRESS = 0.97;

    /** How close to a post / crossbar counts as hitting woodwork (sim pixels). */
    double POST_HIT_TOLERANCE_X = 16.0;
    double POST_HIT_TOLERANCE_Y = 18.0;

    // Keeper AI (baseline dive probabilities when no history bias is applied)
    double KEEPER_LEFT_PROBABILITY = 0.4;
    double KEEPER_CENTER_PROBABILITY = 0.2;
    double KEEPER_RIGHT_PROBABILITY = 0.4;

    /** Save-reach multiplier per level (indexed by level-1: L1, L2, L3). Higher = easier saves. */
    double[] KEEPER_REACH_MULT_BY_LEVEL = { 1.00, 1.25, 1.35 };
    /**
     * How strongly the L3 adaptive keeper biases toward the player's preferred zone.
     * 0 = pure random, 1 = fully copy the observed distribution.
     */
    double KEEPER_ADAPTIVE_BIAS = 0.7;
    /** Shots needed before the adaptive bias ramps in fully (linear ramp from 0 to this). */
    int KEEPER_ADAPTIVE_RAMP_SHOTS = 3;

    // Pressure meter (0..1). Goals drop it; saves and misses raise it.
    double PRESSURE_ON_GOAL = -0.15;
    double PRESSURE_ON_SAVE = 0.25;
    double PRESSURE_ON_MISS = 0.15;
    /** Maximum additional multiplier applied to the power oscillation when pressure = 1. */
    double PRESSURE_POWER_SPEEDUP = 0.6;
    
    // Game states
    String STATE_MENU = "MENU";
    String STATE_PLAYING = "PLAYING";
    /** Between-level celebration banner (auto-advances). */
    String STATE_LEVEL_COMPLETE = "LEVEL_COMPLETE";
    /** Between-level failure banner (waits for retry or back-to-menu). */
    String STATE_LEVEL_FAILED = "LEVEL_FAILED";
    /** Campaign completed (all levels won). */
    String STATE_RESULT = "RESULT";
    
    // Levels / rounds
    int ROUNDS_PER_LEVEL = 5;
    /** Campaign length; each level is a full shootout of {@link #ROUNDS_PER_LEVEL} rounds. */
    int LEVEL_COUNT = 3;
    /** Backwards-compat alias used by HUD icons and older call sites. */
    int MAX_ROUNDS = ROUNDS_PER_LEVEL;
    /** How long the LEVEL_COMPLETE banner is shown before advancing (seconds). */
    double LEVEL_TRANSITION_SECONDS = 2.5;

    /** Short one-line description of each level (shown on the menu and level transitions). */
    static String levelDescription(int level) {
        switch (level) {
            case 1: return "Level 1 — Random keeper";
            case 2: return "Level 2 — Keeper: longer reach, tighter dives";
            case 3: return "Level 3 — Adaptive keeper reads your aim history";
            default: return "Level " + level;
        }
    }
}
