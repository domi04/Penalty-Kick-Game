package com.deadball.entities;

import javafx.scene.canvas.GraphicsContext;
import com.deadball.utils.GameConstants;

public class Player extends Entity {
    private double reticleX;
    private double reticleY;
    private double powerLevel;
    private double powerPhase;
    private double reticleMoveH;
    private double reticleMoveV;
    /** Multiplier on the power-bar oscillation rate (raised by pressure to hurt timing). */
    private double oscillationSpeedMultiplier = 1.0;

    public Player(double startX, double startY) {
        super(startX, startY);
        this.reticleX = (GameConstants.GOAL_LEFT + GameConstants.GOAL_RIGHT) / 2.0;
        this.reticleY = (GameConstants.GOAL_TOP + GameConstants.GOAL_BOTTOM) / 2.0;
        this.powerLevel = 0.0;
        this.powerPhase = 0.0;
        this.reticleMoveH = 0.0;
        this.reticleMoveV = 0.0;
    }

    /** @param horizontal -1 left, 1 right; @param vertical -1 up (smaller Y), 1 down */
    public void setReticleMovement(double horizontal, double vertical) {
        this.reticleMoveH = horizontal;
        this.reticleMoveV = vertical;
    }
    
    public double getPowerLevel() {
        return powerLevel;
    }
    
    public double getReticleX() {
        return reticleX;
    }

    public double getReticleY() {
        return reticleY;
    }
    
    public void setOscillationSpeedMultiplier(double multiplier) {
        this.oscillationSpeedMultiplier = Math.max(0.1, multiplier);
    }

    public void resetRound() {
        powerPhase = 0.0;
        powerLevel = 0.0;
        reticleMoveH = 0.0;
        reticleMoveV = 0.0;
        oscillationSpeedMultiplier = 1.0;
        reticleX = (GameConstants.GOAL_LEFT + GameConstants.GOAL_RIGHT) / 2.0;
        reticleY = (GameConstants.GOAL_TOP + GameConstants.GOAL_BOTTOM) / 2.0;
    }
    
    @Override
    public void update(double deltaTime) {
        reticleX += reticleMoveH * GameConstants.RETICLE_SPEED * deltaTime;
        reticleY += reticleMoveV * GameConstants.RETICLE_SPEED_VERTICAL * deltaTime;

        reticleX = Math.max(GameConstants.AIM_RETICLE_X_MIN,
                Math.min(GameConstants.AIM_RETICLE_X_MAX, reticleX));
        reticleY = Math.max(GameConstants.AIM_RETICLE_Y_MIN,
                Math.min(GameConstants.GOAL_BOTTOM, reticleY));
        
        powerPhase += deltaTime * oscillationSpeedMultiplier;
        double period = GameConstants.POWER_OSCILLATION_PERIOD;
        double normalizedPhase = (powerPhase % period) / period;
        powerLevel = Math.abs(Math.sin(normalizedPhase * Math.PI));
    }
    
    @Override
    public void render(GraphicsContext gc) {
        // Drawn by JavaFX 3D SubScene
    }
    
}
