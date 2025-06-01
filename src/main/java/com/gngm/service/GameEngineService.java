package com.gngm.service;

import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class GameEngineService {

    private final SimpMessagingTemplate messagingTemplate;
    
    // Game constants
    private static final int MAP_WIDTH = 1600;
    private static final int MAP_HEIGHT = 1200;
    private static final double PROJECTILE_SPEED = 300.0;
    private static final int PROJECTILE_DAMAGE = 25;
    private static final int MAX_HEALTH = 100;
    
    // Game state
    private final Map<Integer, Player> players = new ConcurrentHashMap<>();
    private final Map<String, Projectile> projectiles = new ConcurrentHashMap<>();
    private final ScheduledExecutorService gameLoop = Executors.newSingleThreadScheduledExecutor();
    
    @PostConstruct
    public void startGameLoop() {
        // Update game at 60 FPS
        gameLoop.scheduleAtFixedRate(this::updateGame, 0, 16, TimeUnit.MILLISECONDS);
    }
      // Simple Player class
    public static class Player {
        public int id;
        public String username;
        public double x = MAP_WIDTH / 2.0;
        public double y = MAP_HEIGHT / 2.0;
        public double rotation = 0;
        public int health = MAX_HEALTH;        public boolean alive = true;
        public long deathTime = 0;
        
        public Player(int id, String username) {
            this.id = id;
            this.username = username;
            // Random spawn position
            this.x = 100 + Math.random() * (MAP_WIDTH - 200);
            this.y = 100 + Math.random() * (MAP_HEIGHT - 200);
        }
    }
    
    // Simple Projectile class
    public static class Projectile {
        public String id = UUID.randomUUID().toString();
        public double x, y;
        public double direction;
        public int playerId;
        public long createTime = System.currentTimeMillis();
        
        public Projectile(double x, double y, double direction, int playerId) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.playerId = playerId;
        }
    }
    
    // Game state response
    public static class GameState {
        public Map<Integer, Player> players;
        public Map<String, Projectile> projectiles;
        
        public GameState(Map<Integer, Player> players, Map<String, Projectile> projectiles) {
            this.players = players;
            this.projectiles = projectiles;
        }
    }
    
    // Add player
    public void addPlayer(int playerId, String username) {
        System.out.println("Adding player: " + playerId + " - " + username);
        players.put(playerId, new Player(playerId, username));
        broadcastGameState();
    }
    
    // Remove player
    public void removePlayer(int playerId) {
        System.out.println("Removing player: " + playerId);
        players.remove(playerId);
        broadcastGameState();
    }
    
    // Move player
    public void movePlayer(int playerId, double deltaX, double deltaY, double rotation) {
        Player player = players.get(playerId);
        if (player != null && player.alive) {
            // Update position with bounds checking
            player.x = Math.max(20, Math.min(MAP_WIDTH - 20, player.x + deltaX));
            player.y = Math.max(20, Math.min(MAP_HEIGHT - 20, player.y + deltaY));
            player.rotation = rotation;
        }
    }
    
    // Player shoots
    public void playerShoot(int playerId, double direction) {
        Player player = players.get(playerId);
        if (player != null && player.alive) {
            System.out.println("Player " + playerId + " shoots at direction " + direction);
            Projectile bullet = new Projectile(player.x, player.y, direction, playerId);
            projectiles.put(bullet.id, bullet);
            broadcastGameState();
        }
    }
    
    // Game loop update
    private void updateGame() {
        updateProjectiles();
        checkCollisions();
        // Broadcast less frequently to reduce network load
        if (System.currentTimeMillis() % 100 < 16) {
            broadcastGameState();
        }
    }
    
    // Update projectiles
    public void updateProjectiles() {
        long currentTime = System.currentTimeMillis();
        
        projectiles.entrySet().removeIf(entry -> {
            Projectile proj = entry.getValue();
            
            // Remove old projectiles (3 seconds)
            if (currentTime - proj.createTime > 3000) {
                return true;
            }
            
            // Move projectile
            proj.x += Math.cos(proj.direction) * PROJECTILE_SPEED * 0.016;
            proj.y += Math.sin(proj.direction) * PROJECTILE_SPEED * 0.016;
            
            // Remove if out of bounds
            return proj.x < 0 || proj.x > MAP_WIDTH || proj.y < 0 || proj.y > MAP_HEIGHT;
        });
    }
    
    // Check collisions
    private void checkCollisions() {
        projectiles.entrySet().removeIf(projEntry -> {
            Projectile proj = projEntry.getValue();
            
            for (Player player : players.values()) {
                if (player.id == proj.playerId || !player.alive) {
                    continue;
                }
                
                // Distance check
                double dx = proj.x - player.x;
                double dy = proj.y - player.y;
                if (Math.sqrt(dx * dx + dy * dy) < 25) {
                    // Hit!
                    player.health -= PROJECTILE_DAMAGE;
                    System.out.println("Player " + player.id + " hit! Health: " + player.health);
                    
                    if (player.health <= 0) {
                        player.alive = false;
                        player.health = 0;
                        player.deathTime = System.currentTimeMillis();
                        System.out.println("Player " + player.id + " eliminated!");
                    }
                    
                    return true; // Remove projectile
                }
            }
            return false;
        });
    }
    
    // Respawn player
    public void respawnPlayer(int playerId) {
        Player player = players.get(playerId);
        if (player != null) {
            player.health = MAX_HEALTH;
            player.alive = true;
            player.x = 100 + Math.random() * (MAP_WIDTH - 200);
            player.y = 100 + Math.random() * (MAP_HEIGHT - 200);
            System.out.println("Player " + playerId + " respawned");
            broadcastGameState();
        }
    }
    
    // Broadcast game state
    private void broadcastGameState() {
        try {
            GameState state = new GameState(players, projectiles);
            messagingTemplate.convertAndSend("/topic/game/state", state);
        } catch (Exception e) {
            System.err.println("Error broadcasting: " + e.getMessage());
        }
    }
    
    // Get game state
    public GameState getGameState() {
        return new GameState(players, projectiles);
    }
    
    // Get players map (for GameBroadcastScheduler)
    public Map<Integer, Player> getPlayerStates() {
        return new ConcurrentHashMap<>(players);
    }
    
    // Get projectiles map (for GameBroadcastScheduler)
    public Map<String, Projectile> getActiveProjectiles() {
        return new ConcurrentHashMap<>(projectiles);
    }
    
    // Cleanup dead players method (for GameBroadcastScheduler)
    public void cleanupDeadPlayers() {
        // Respawn dead players after 3 seconds
        long currentTime = System.currentTimeMillis();
        for (Player player : players.values()) {
            if (!player.alive && currentTime - player.deathTime > 3000) {
                respawnPlayer(player.id);
            }
        }
    }
}
