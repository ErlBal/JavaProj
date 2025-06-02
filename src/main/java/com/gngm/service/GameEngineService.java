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
                updateProjectiles(matchId);
                checkCollisions(matchId);
                cleanupDeadPlayers(matchId);
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
        if (match != null) {
            match.players.put(playerId, new Player(playerId, username));
            broadcastGameState(matchId);
        }
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
    public void movePlayer(long matchId, int playerId, double deltaX, double deltaY, double rotation) {
        MatchState match = matches.get(matchId);
        if (match != null) {
            Player player = match.players.get(playerId);
            if (player != null && player.alive) {
                List<Wall> walls = wallConfigurations.getOrDefault(match.mapName, List.of());
                double newX = Math.max(20, Math.min(MAP_WIDTH - 20, player.x + deltaX));
                double newY = Math.max(20, Math.min(MAP_HEIGHT - 20, player.y + deltaY));
                double playerSize = 40;
                if (!collidesWithWall(newX, newY, playerSize, walls)) {
                    player.x = newX;
                    player.y = newY;
                }
                player.rotation = rotation;
            }
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
            List<Wall> walls = wallConfigurations.getOrDefault(match.mapName, List.of());
            long currentTime = System.currentTimeMillis();
            match.projectiles.entrySet().removeIf(entry -> {
                Projectile proj = entry.getValue();
                if (currentTime - proj.createTime > 3000) {
                    return true;
                }
                proj.x += Math.cos(proj.direction) * PROJECTILE_SPEED * 0.016;
                proj.y += Math.sin(proj.direction) * PROJECTILE_SPEED * 0.016;
                return proj.x < 0 || proj.x > MAP_WIDTH || proj.y < 0 || proj.y > MAP_HEIGHT
                    || collidesWithWall(proj.x, proj.y, 6, walls);
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

    public static class Wall {
        public double x, y, width, height;
        public Wall(double x, double y, double width, double height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    private static final Map<String, List<Wall>> wallConfigurations = Map.of(
        "Map1", List.of(
            new Wall(200, 200, 100, 20),
            new Wall(400, 300, 150, 20),
            new Wall(600, 400, 200, 20)
        ),
        "Map2", List.of(
            new Wall(100, 100, 120, 30),
            new Wall(300, 250, 180, 25),
            new Wall(500, 350, 220, 30)
        )
    );

    public static Map<String, List<Wall>> getWallConfigurations() {
        return wallConfigurations;
    }

    private boolean collidesWithWall(double x, double y, double size, List<Wall> walls) {
        for (Wall wall : walls) {
            if (x + size/2 > wall.x && x - size/2 < wall.x + wall.width &&
                y + size/2 > wall.y && y - size/2 < wall.y + wall.height) {
                return true;
            }
        }
        return false;
    }
}
