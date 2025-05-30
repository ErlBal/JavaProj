package com.gngm.service;

import com.gngm.entity.Player;
import com.gngm.entity.Weapon;
import com.gngm.repository.WeaponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameEngineService {
    private final Map<Long, PlayerState> playerStates = new ConcurrentHashMap<>();
    private final Map<Long, Projectile> activeProjectiles = new ConcurrentHashMap<>();
    private static final double MOVEMENT_SPEED = 5.0;
    private static final double ROTATION_SPEED = 3.0;

    @Autowired
    private WeaponRepository weaponRepository;

    public static class PlayerState {
        private double x;
        private double y;
        private double rotation;
        private double health;
        private Weapon currentWeapon;
        private boolean isMoving;
        private boolean isShooting;
        private boolean isAlive;
        private String username;
        private long deathTime;

        public PlayerState(double x, double y) {
            this.x = x;
            this.y = y;
            this.rotation = 0;
            this.health = 100;
            this.isMoving = false;
            this.isShooting = false;
            this.isAlive = true;
            this.username = null;
            this.deathTime = 0;
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
        public boolean isAlive() { return isAlive; }
        public void setAlive(boolean alive) { isAlive = alive; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public long getDeathTime() { return deathTime; }
        public void setDeathTime(long deathTime) { this.deathTime = deathTime; }
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
        PlayerState state = new PlayerState(0, 0);
        // Set default weapon to pistol
        Weapon pistol = weaponRepository.findByName("Pistol").orElse(null);
        state.setCurrentWeapon(pistol);
        state.setUsername(player.getUsername());
        playerStates.put(player.getId(), state);
    }

    public void updatePlayerMovement(long playerId, double deltaX, double deltaY, double rotation) {
        PlayerState state = playerStates.get(playerId);
        if (state != null) {
            // Calculate new position
            double newX = state.getX() + deltaX * MOVEMENT_SPEED;
            double newY = state.getY() + deltaY * MOVEMENT_SPEED;
            
            // Get canvas dimensions from the client
            double canvasWidth = 1000;  // Default width
            double canvasHeight = 1000; // Default height
            
            // Boundary checks
            newX = Math.max(0, Math.min(newX, canvasWidth));
            newY = Math.max(0, Math.min(newY, canvasHeight));
            
            // Update position
            state.setX(newX);
            state.setY(newY);
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
            double newX = projectile.getX() + Math.cos(projectile.getDirection()) * projectile.getSpeed();
            double newY = projectile.getY() + Math.sin(projectile.getDirection()) * projectile.getSpeed();
            
            // Get canvas dimensions
            double canvasWidth = 1000;  // Default width
            double canvasHeight = 1000; // Default height
            
            // Check if projectile is out of bounds
            if (newX < 0 || newX > canvasWidth || newY < 0 || newY > canvasHeight) {
                activeProjectiles.remove(id);
                return;
            }
            
            // Update position
            projectile.setX(newX);
            projectile.setY(newY);
            
            // Check for collisions with players
            checkProjectileCollisions(projectile);
        });
    }

    private void checkProjectileCollisions(Projectile projectile) {
        playerStates.forEach((playerId, state) -> {
            if (playerId != projectile.getPlayerId() && state.isAlive()) { // Don't check collision with shooter or dead players
                double dx = state.getX() - projectile.getX();
                double dy = state.getY() - projectile.getY();
                double distance = Math.sqrt(dx * dx + dy * dy);
                
                if (distance < 20) { // Collision radius
                    state.setHealth(state.getHealth() - projectile.getDamage());
                    // Check if player died
                    if (state.getHealth() <= 0) {
                        state.setAlive(false);
                        state.setHealth(0);
                        state.setDeathTime(System.currentTimeMillis()); // Record death time
                    }
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

    // Add method to remove dead players after a delay
    public void cleanupDeadPlayers() {
        long currentTime = System.currentTimeMillis();
        playerStates.entrySet().removeIf(entry -> {
            PlayerState state = entry.getValue();
            // Remove if not alive and 3 seconds have passed since death
            return !state.isAlive() && (currentTime - state.getDeathTime() > 3000);
        });
    }
} 