package com.deadball.entities;

import com.deadball.utils.GameConstants;

public class Goal {
    private boolean flashNet;
    private double flashTimer;
    
    public Goal() {
        this.flashNet = false;
        this.flashTimer = 0.0;
    }
    
    public boolean checkBallInGoal(Ball ball) {
        return GameConstants.isInsideGoalFrame(ball.getPosX(), ball.getPosY());
    }
    
    public void triggerFlash() {
        flashNet = true;
        flashTimer = 0.3; // Flash duration in seconds
    }

    public boolean isNetFlashVisible() {
        return flashNet && flashTimer > 0;
    }
    
    public void update(double deltaTime) {
        if (flashTimer > 0) {
            flashTimer -= deltaTime;
        } else {
            flashNet = false;
        }
    }
}
