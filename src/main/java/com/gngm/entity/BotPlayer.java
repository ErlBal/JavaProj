package com.gngm.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "bot_players")
public class BotPlayer extends Player {
    public static final double BOT_MOVEMENT_SPEED = 3.0;
    public static final double BOT_SHOOTING_RANGE = 300.0;
    public static final double BOT_SHOOTING_COOLDOWN = 1000.0; // ms
    public static final double BOT_ROTATION_SPEED = 2.0;

    private double lastShootTime;
    private double targetX;
    private double targetY;
    private boolean isMovingToTarget;

    public BotPlayer() {
        super();
        this.lastShootTime = 0;
        this.isMovingToTarget = false;
        setRandomTarget();
    }

    public void setRandomTarget() {
        // Set random target within game bounds (0 to 1000)
        this.targetX = Math.random() * 1000;
        this.targetY = Math.random() * 1000;
        this.isMovingToTarget = true;
    }

    public void update(double currentTime, double playerX, double playerY) {
        if (!isMovingToTarget) {
            setRandomTarget();
        }

        // Calculate direction to target
        double dx = targetX - getX();
        double dy = targetY - getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        // If reached target, set new target
        if (distance < 50) {
            setRandomTarget();
            return;
        }

        // Calculate movement
        double direction = Math.atan2(dy, dx);
        double moveX = Math.cos(direction) * BOT_MOVEMENT_SPEED;
        double moveY = Math.sin(direction) * BOT_MOVEMENT_SPEED;

        // Update position
        setX(getX() + moveX);
        setY(getY() + moveY);
        setRotation(direction);

        // Check if player is in shooting range
        double playerDx = playerX - getX();
        double playerDy = playerY - getY();
        double playerDistance = Math.sqrt(playerDx * playerDx + playerDy * playerDy);

        if (playerDistance < BOT_SHOOTING_RANGE && currentTime - lastShootTime > BOT_SHOOTING_COOLDOWN) {
            // Calculate direction to player
            double shootDirection = Math.atan2(playerDy, playerDx);
            setRotation(shootDirection);
            lastShootTime = currentTime;
            // Signal to shoot (this will be handled by the game engine)
        }
    }

    // Getters and setters for bot-specific properties
    public double getLastShootTime() { return lastShootTime; }
    public void setLastShootTime(double lastShootTime) { this.lastShootTime = lastShootTime; }
    public double getTargetX() { return targetX; }
    public void setTargetX(double targetX) { this.targetX = targetX; }
    public double getTargetY() { return targetY; }
    public void setTargetY(double targetY) { this.targetY = targetY; }
    public boolean isMovingToTarget() { return isMovingToTarget; }
    public void setMovingToTarget(boolean movingToTarget) { isMovingToTarget = movingToTarget; }
} 