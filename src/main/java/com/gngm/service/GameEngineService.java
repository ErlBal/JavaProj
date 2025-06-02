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
    
    // Multi-match support
    public static class MatchState {
        public String mapName;
        public Map<Integer, Player> players = new ConcurrentHashMap<>();
        public Map<String, Projectile> projectiles = new ConcurrentHashMap<>();
        public MatchState(String mapName) {
            this.mapName = mapName;
        }
    }
    public final Map<Long, MatchState> matches = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void startGameLoop() {
        // Update all matches at 60 FPS
        gameLoop.scheduleAtFixedRate(() -> {
            for (Long matchId : matches.keySet()) {
                // Update player positions by velocity
                MatchState match = matches.get(matchId);
                if (match != null) {
                    // Remove wall collision: players can move freely within bounds
                    for (Player player : match.players.values()) {
                        if (player.alive) {
                            double newX = Math.max(20, Math.min(MAP_WIDTH - 20, player.x + player.vx));
                            double newY = Math.max(20, Math.min(MAP_HEIGHT - 20, player.y + player.vy));
                            // No wall collision check
                            // System.out.println("Moving player " + player.id + " from (" + player.x + ", " + player.y + ") to (" + newX + ", " + newY + ") with vx=" + player.vx + ", vy=" + player.vy);
                            player.x = newX;
                            player.y = newY;
                        }
                    }
                }
                updateProjectiles(matchId);
                checkCollisions(matchId);
                cleanupDeadPlayers(matchId);
                broadcastGameState(matchId);
            }
        }, 0, 16, TimeUnit.MILLISECONDS);
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
        public double vx = 0; // velocity x
        public double vy = 0; // velocity y
        
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
    
    // Add a match
    public void createMatch(long matchId, String mapName) {
        matches.put(matchId, new MatchState(mapName));
    }

    // Add player to match
    public void addPlayer(long matchId, int playerId, String username) {
        MatchState match = matches.get(matchId);
        if (match == null) {
            // Auto-create match with default map if missing
            match = new MatchState("Map1");
            matches.put(matchId, match);
        }
        match.players.put(playerId, new Player(playerId, username));
        broadcastGameState(matchId);
    }

    // Remove player from match
    public void removePlayer(long matchId, int playerId) {
        MatchState match = matches.get(matchId);
        if (match != null) {
            match.players.remove(playerId);
            broadcastGameState(matchId);
        }
    }

    // Move player in match
    public void movePlayer(long matchId, int playerId, double vx, double vy, double rotation) {
        MatchState match = matches.get(matchId);
        if (match != null) {
            Player player = match.players.get(playerId);
            if (player != null && player.alive) {
                player.vx = vx;
                player.vy = vy;
                player.rotation = rotation;
                System.out.println("Set velocity for player " + playerId + ": vx=" + vx + ", vy=" + vy);
            } else {
                System.out.println("Player not found or not alive: " + playerId);
            }
        } else {
            System.out.println("Match not found: " + matchId);
        }
    }

    // Player shoots in match
    public void playerShoot(long matchId, int playerId, double direction) {
        MatchState match = matches.get(matchId);
        if (match != null) {
            Player player = match.players.get(playerId);
            if (player != null && player.alive) {
                Projectile bullet = new Projectile(player.x, player.y, direction, playerId);
                match.projectiles.put(bullet.id, bullet);
                broadcastGameState(matchId);
            }
        }
    }

    // Update projectiles in match
    public void updateProjectiles(long matchId) {
        MatchState match = matches.get(matchId);
        if (match != null) {
            // Remove wall collision: projectiles only disappear when out of bounds or after 3 seconds
            long currentTime = System.currentTimeMillis();
            match.projectiles.entrySet().removeIf(entry -> {
                Projectile proj = entry.getValue();
                if (currentTime - proj.createTime > 3000) {
                    return true;
                }
                proj.x += Math.cos(proj.direction) * PROJECTILE_SPEED * 0.016;
                proj.y += Math.sin(proj.direction) * PROJECTILE_SPEED * 0.016;
                return proj.x < 0 || proj.x > MAP_WIDTH || proj.y < 0 || proj.y > MAP_HEIGHT;
            });
        }
    }

    // Check collisions in match
    public void checkCollisions(long matchId) {
        MatchState match = matches.get(matchId);
        if (match != null) {
            match.projectiles.entrySet().removeIf(projEntry -> {
                Projectile proj = projEntry.getValue();
                for (Player player : match.players.values()) {
                    if (player.id == proj.playerId || !player.alive) {
                        continue;
                    }
                    double dx = proj.x - player.x;
                    double dy = proj.y - player.y;
                    if (Math.sqrt(dx * dx + dy * dy) < 25) {
                        player.health -= PROJECTILE_DAMAGE;
                        if (player.health <= 0) {
                            player.alive = false;
                            player.health = 0;
                            player.deathTime = System.currentTimeMillis();
                        }
                        return true;
                    }
                }
                return false;
            });
        }
    }

    // Respawn player in match
    public void respawnPlayer(long matchId, int playerId) {
        MatchState match = matches.get(matchId);
        if (match != null) {
            Player player = match.players.get(playerId);
            if (player != null) {
                player.health = MAX_HEALTH;
                player.alive = true;
                player.x = 100 + Math.random() * (MAP_WIDTH - 200);
                player.y = 100 + Math.random() * (MAP_HEIGHT - 200);
                broadcastGameState(matchId);
            }
        }
    }

    // Broadcast game state for a match
    private void broadcastGameState(long matchId) {
        MatchState match = matches.get(matchId);
        if (match != null) {
            try {
                GameState state = new GameState(match.players, match.projectiles);
                messagingTemplate.convertAndSend("/topic/game/state/" + matchId, state);
            } catch (Exception e) {
                System.err.println("Error broadcasting: " + e.getMessage());
            }
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
    public void cleanupDeadPlayers(long matchId) {
        MatchState match = matches.get(matchId);
        if (match != null) {
            long currentTime = System.currentTimeMillis();
            for (Player player : match.players.values()) {
                if (!player.alive && currentTime - player.deathTime > 3000) {
                    respawnPlayer(matchId, player.id);
                }
            }
        }
    }
}
