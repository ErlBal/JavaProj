package com.gngm.service;

import com.gngm.entity.Player;
import com.gngm.entity.Weapon;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameEngineService {
    private final Map<Long, PlayerState> playerStates = new ConcurrentHashMap<>();
    private final Map<Long, Projectile> activeProjectiles = new ConcurrentHashMap<>();
    private static final double MOVEMENT_SPEED = 5.0;
    private static final double ROTATION_SPEED = 3.0;

    public static class PlayerState {
        private double x;
        private double y;
        private double rotation;
        private double health;
        private Weapon currentWeapon;
        private boolean isMoving;
        private boolean isShooting;

        public PlayerState(double x, double y) {
            this.x = x;
            this.y = y;
            this.rotation = 0;
            this.health = 100;
            this.isMoving = false;
            this.isShooting = false;
        }

        // Getters and setters
        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
        public double getRotation() { return rotation; }
        public void setRotation(double rotation) { this.rotation = rotation; }
        public double getHealth() { return health; }
        public void setHealth(double health) { this.health = health; }
        public Weapon getCurrentWeapon() { return currentWeapon; }
        public void setCurrentWeapon(Weapon weapon) { this.currentWeapon = weapon; }
        public boolean isMoving() { return isMoving; }
        public void setMoving(boolean moving) { isMoving = moving; }
        public boolean isShooting() { return isShooting; }
        public void setShooting(boolean shooting) { isShooting = shooting; }
    }

    public static class Projectile {
        private double x;
        private double y;
        private double direction;
        private double speed;
        private int damage;
        private long playerId;

        public Projectile(double x, double y, double direction, double speed, int damage, long playerId) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.speed = speed;
            this.damage = damage;
            this.playerId = playerId;
        }

        // Getters and setters
        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
        public double getDirection() { return direction; }
        public void setDirection(double direction) { this.direction = direction; }
        public double getSpeed() { return speed; }
        public void setSpeed(double speed) { this.speed = speed; }
        public int getDamage() { return damage; }
        public void setDamage(int damage) { this.damage = damage; }
        public long getPlayerId() { return playerId; }
        public void setPlayerId(long playerId) { this.playerId = playerId; }
    }

    public void initializePlayer(Player player) {
        playerStates.put(player.getId(), new PlayerState(0, 0));
    }

    public void updatePlayerMovement(long playerId, double deltaX, double deltaY, double rotation) {
        PlayerState state = playerStates.get(playerId);
        if (state != null) {
            // Update position based on movement direction and speed
            state.setX(state.getX() + deltaX * MOVEMENT_SPEED);
            state.setY(state.getY() + deltaY * MOVEMENT_SPEED);
            state.setRotation(rotation);
        }
    }

    public void handleShooting(long playerId, double direction) {
        PlayerState state = playerStates.get(playerId);
        if (state != null && state.getCurrentWeapon() != null) {
            Weapon weapon = state.getCurrentWeapon();
            Projectile projectile = new Projectile(
                state.getX(),
                state.getY(),
                direction,
                weapon.getFireRate() * 10, // Convert fire rate to projectile speed
                weapon.getDamage(),
                playerId
            );
            activeProjectiles.put(System.currentTimeMillis(), projectile);
        }
    }

    public void updateProjectiles() {
        // Update projectile positions and check for collisions
        activeProjectiles.forEach((id, projectile) -> {
            projectile.setX(projectile.getX() + Math.cos(projectile.getDirection()) * projectile.getSpeed());
            projectile.setY(projectile.getY() + Math.sin(projectile.getDirection()) * projectile.getSpeed());
            
            // Check for collisions with players
            checkProjectileCollisions(projectile);
        });
    }

    private void checkProjectileCollisions(Projectile projectile) {
        playerStates.forEach((playerId, state) -> {
            if (playerId != projectile.getPlayerId()) { // Don't check collision with shooter
                double dx = state.getX() - projectile.getX();
                double dy = state.getY() - projectile.getY();
                double distance = Math.sqrt(dx * dx + dy * dy);
                
                if (distance < 20) { // Collision radius
                    state.setHealth(state.getHealth() - projectile.getDamage());
                    // Remove projectile after hit
                    activeProjectiles.values().remove(projectile);
                }
            }
        });
    }

    public PlayerState getPlayerState(long playerId) {
        return playerStates.get(playerId);
    }

    public Map<Long, Projectile> getActiveProjectiles() {
        return activeProjectiles;
    }

    public Map<Long, PlayerState> getPlayerStates() {
        return playerStates;
    }
} 