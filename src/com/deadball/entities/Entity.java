package com.deadball.entities;

public abstract class Entity {
    protected double posX;
    protected double posY;
    protected double velocityX;
    protected double velocityY;
    
    public Entity(double startX, double startY) {
        this.posX = startX;
        this.posY = startY;
        this.velocityX = 0.0;
        this.velocityY = 0.0;
    }
    
    public abstract void update(double deltaTime);
    
    public void setPosition(double x, double y) {
        this.posX = x;
        this.posY = y;
    }
    
    public double getPosX() {
        return posX;
    }
    
    public double getPosY() {
        return posY;
    }
}
