package com.deadball.entities;

import javafx.scene.canvas.GraphicsContext;
import com.deadball.utils.GameConstants;

public class Goal {
    private boolean flashNet;
    private double flashTimer;
    
    public Goal() {
        this.flashNet = false;
        this.flashTimer = 0.0;
    }
    
    public boolean checkBallInGoal(Ball ball) {
        double ballX = ball.getPosX();
        double ballY = ball.getPosY();
        
        return ballX >= GameConstants.GOAL_LEFT &&
               ballX <= GameConstants.GOAL_RIGHT &&
               ballY >= GameConstants.GOAL_TOP &&
               ballY <= GameConstants.GOAL_BOTTOM;
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
    
    public void render(GraphicsContext gc) {
        // Drawn by JavaFX 3D SubScene
    }
}
