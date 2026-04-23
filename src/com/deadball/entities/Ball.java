package com.deadball.entities;

import com.deadball.utils.GameConstants;

public class Ball extends Entity {
    private boolean inFlight;
    private double flightTime;
    /** If false, passing through the goal box does not end flight (slice-through to a wide/high aim). */
    private boolean aimWasInsideGoal;
    /** When true, 3D draws the ball on the line from shot start to locked reticle aim. */
    private boolean hasShotRay;
    private double shotStartX;
    private double shotStartY;
    private double shotAimX;
    private double shotAimY;

    public Ball(double startX, double startY) {
        super(startX, startY);
        this.inFlight = false;
        this.flightTime = 0.0;
        this.hasShotRay = false;
    }

    /** Clears locked aim ray (e.g. new round); ball is placed at sim position only in 3D. */
    public void clearShotRay() {
        hasShotRay = false;
    }

    public boolean hasShotRay() {
        return hasShotRay;
    }

    public double getShotStartX() {
        return shotStartX;
    }

    public double getShotStartY() {
        return shotStartY;
    }

    public double getShotAimX() {
        return shotAimX;
    }

    public double getShotAimY() {
        return shotAimY;
    }

    /**
     * Parametric progress along the shot ray ({@code 0} at kick, {@code 1} at the aim point); same as
     * 3D placement. Unbounded above {@code 1} if the sim carries past the reticle.
     */
    public double getShotRayProgress() {
        if (!hasShotRay) {
            return 0.0;
        }
        double dx = shotAimX - shotStartX;
        double dy = shotAimY - shotStartY;
        double len2 = dx * dx + dy * dy;
        if (len2 < 1e-9) {
            return 1.0;
        }
        return ((posX - shotStartX) * dx + (posY - shotStartY) * dy) / len2;
    }
    
    public void launch(double targetX, double targetY, double power, boolean aimInsideGoalFrame) {
        flightTime = 0.0;
        aimWasInsideGoal = aimInsideGoalFrame;
        shotStartX = posX;
        shotStartY = posY;
        shotAimX = targetX;
        shotAimY = targetY;
        hasShotRay = true;

        double deltaX = targetX - posX;
        double deltaY = targetY - posY;

        double len = Math.hypot(deltaX, deltaY);
        if (len < 1e-6) {
            deltaX = 0.0;
            deltaY = -1.0;
            len = 1.0;
        }

        double nx = deltaX / len;
        double ny = deltaY / len;
        double speedFactor = GameConstants.BALL_POWER_SPEED_MIN
                + power * (GameConstants.BALL_POWER_SPEED_MAX - GameConstants.BALL_POWER_SPEED_MIN);
        double spd = GameConstants.BALL_BASE_SPEED * speedFactor;
        inFlight = true;

        velocityX = spd * nx;
        velocityY = spd * ny;
    }

    public boolean isInFlight() {
        return inFlight;
    }
    
    public void setInFlight(boolean inFlight) {
        this.inFlight = inFlight;
    }
    
    public boolean isInGoal() {
        return posX >= GameConstants.GOAL_LEFT && 
               posX <= GameConstants.GOAL_RIGHT &&
               posY >= GameConstants.GOAL_TOP && 
               posY <= GameConstants.GOAL_BOTTOM;
    }
    
    @Override
    public void update(double deltaTime) {
        if (!inFlight) {
            return;
        }

        flightTime += deltaTime;
        if (flightTime >= GameConstants.BALL_MAX_FLIGHT_TIME) {
            inFlight = false;
            return;
        }

        double drag = Math.exp(-GameConstants.BALL_AIR_DRAG * deltaTime);
        velocityX *= drag;
        velocityY *= drag;

        posX += velocityX * deltaTime;
        posY += velocityY * deltaTime;

        double spd = Math.hypot(velocityX, velocityY);
        boolean waitingToReachAimInGoal = aimWasInsideGoal && isInGoal()
                && getShotRayProgress() < GameConstants.BALL_GOAL_STOP_MIN_PROGRESS;
        if (spd < GameConstants.BALL_MIN_SPEED_GROUND
                && posY <= GameConstants.BALL_ROLL_DEAD_MAX_Y
                && !waitingToReachAimInGoal) {
            inFlight = false;
            return;
        }

        if (isInGoal()) {
            if (aimWasInsideGoal) {
                double p = getShotRayProgress();
                if (p >= 1.0 || p >= GameConstants.BALL_GOAL_STOP_MIN_PROGRESS) {
                    inFlight = false;
                    return;
                }
            }
        }
        if (posY < GameConstants.GOAL_TOP) {
            inFlight = false;
            return;
        }
        if (posX < GameConstants.GOAL_LEFT - 120 || posX > GameConstants.GOAL_RIGHT + 120
                || posY > GameConstants.SCREEN_HEIGHT + 50) {
            inFlight = false;
        }
    }
}
