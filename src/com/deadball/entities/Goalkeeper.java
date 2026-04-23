package com.deadball.entities;

import com.deadball.utils.GameConstants;

public class Goalkeeper extends Entity {
    private final double[] diveProbabilities; // [left, center, right], sums to 1
    private int currentDiveDirection; // -1 = left, 0 = center, 1 = right
    /** Multiplies {@link GameConstants#KEEPER_SAVE_REACH_X} when computing a save. */
    private double reachMultiplier;

    public Goalkeeper(double startX, double startY) {
        super(startX, startY);
        this.diveProbabilities = new double[]{
            GameConstants.KEEPER_LEFT_PROBABILITY,
            GameConstants.KEEPER_CENTER_PROBABILITY,
            GameConstants.KEEPER_RIGHT_PROBABILITY
        };
        this.currentDiveDirection = 0;
        this.reachMultiplier = 1.0;
    }

    /** Replace the weighted probabilities; values are normalised so they sum to 1. */
    public void setDiveProbabilities(double left, double center, double right) {
        double sum = Math.max(1e-6, left + center + right);
        diveProbabilities[0] = left / sum;
        diveProbabilities[1] = center / sum;
        diveProbabilities[2] = right / sum;
    }

    /** Reset to the configured baseline probabilities. */
    public void resetProbabilitiesToBaseline() {
        setDiveProbabilities(
            GameConstants.KEEPER_LEFT_PROBABILITY,
            GameConstants.KEEPER_CENTER_PROBABILITY,
            GameConstants.KEEPER_RIGHT_PROBABILITY);
    }

    public void setReachMultiplier(double multiplier) {
        this.reachMultiplier = Math.max(0.1, multiplier);
    }

    public void decideDive() {
        double random = Math.random();
        if (random < diveProbabilities[0]) {
            currentDiveDirection = -1;
        } else if (random < diveProbabilities[0] + diveProbabilities[1]) {
            currentDiveDirection = 0;
        } else {
            currentDiveDirection = 1;
        }
    }
    
    /**
     * Saves only if the ball ends up where the keeper’s body is (horizontal overlap) and low enough
     * to be reachable (not a screamer into the top shelf / over the bar).
     */
    public boolean isBlocking(Ball ball) {
        double by = ball.getPosY();
        if (by < GameConstants.KEEPER_MIN_BALL_Y_FOR_SAVE) {
            return false;
        }
        double bx = ball.getPosX();
        double kx = posX;
        if (currentDiveDirection == -1) {
            kx -= GameConstants.KEEPER_DIVE_SHIFT_X;
        } else if (currentDiveDirection == 1) {
            kx += GameConstants.KEEPER_DIVE_SHIFT_X;
        }
        double reach = GameConstants.KEEPER_SAVE_REACH_X * reachMultiplier;
        return Math.abs(bx - kx) <= reach;
    }
    
    public void reset() {
        currentDiveDirection = 0;
    }
    
    public int getCurrentDiveDirection() {
        return currentDiveDirection;
    }
    
    @Override
    public void update(double deltaTime) { }
}
