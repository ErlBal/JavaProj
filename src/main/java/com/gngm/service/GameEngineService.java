package com.gngm.service;

import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.web.client.RestTemplate;

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
    private static final double PLAYER_RADIUS = 20.0;
    private static final String WALLS_API_URL = "http://localhost:9090/api/map/walls";
    
    // Game state
    private final Map<Integer, Player> players = new ConcurrentHashMap<>();
    private final Map<String, Projectile> projectiles = new ConcurrentHashMap<>();
    private final ScheduledExecutorService gameLoop = Executors.newSingleThreadScheduledExecutor();
    private List<Map<String, Object>> wallRects = new ArrayList<>();
    private long lastWallFetch = 0;
    
    // Multi-match support
    public static class MatchState {
        public String mapName;
        public Map<Integer, Player> players = new ConcurrentHashMap<>();
        public Map<String, Projectile> projectiles = new ConcurrentHashMap<>();
        public boolean gameOver = false;
        public String winnerName = null;
        public long winnerAnnounceTime = 0;
        public MatchState(String mapName) {
            this.mapName = mapName;
        }
    }
    public final Map<Long, MatchState> matches = new ConcurrentHashMap<>();
    
    // --- Gun Game Weapon System ---
    public static class Weapon {
        public final String name;
        public final int damage;
        public final double fireRate;
        public final double spread; // in radians
        public final int bulletsPerShot;
        public Weapon(String name, int damage, double fireRate, double spread, int bulletsPerShot) {
            this.name = name;
            this.damage = damage;
            this.fireRate = fireRate;
            this.spread = spread;
            this.bulletsPerShot = bulletsPerShot;
        }
    }
    // Gun Game weapon order
    public static final List<Weapon> GUN_GAME_WEAPONS = List.of(
        new Weapon("Pistol", 25, 1.0, Math.toRadians(5), 1),
        new Weapon("PP", 10, 0.2, Math.toRadians(7), 1),
        new Weapon("Machine Gun", 18, 0.3, Math.toRadians(7), 1),
        new Weapon("Shotgun", 7, 1.2, Math.toRadians(30), 6),
        new Weapon("Rifle", 50, 1.5, 0.0, 1)
    );
    
    @PostConstruct
    public void startGameLoop() {
        gameLoop.scheduleAtFixedRate(() -> {
            fetchWallsIfNeeded();
            List<Long> matchesToRemove = new ArrayList<>();
            for (Long matchId : matches.keySet()) {
                MatchState match = matches.get(matchId);
                if (match != null) {
                    if (match.gameOver) {
                        // Pause game logic, just broadcast state
                        broadcastGameState(matchId);
                        // After 4 seconds, end match
                        if (System.currentTimeMillis() - match.winnerAnnounceTime > 4000) {
                            matchesToRemove.add(matchId);
                        }
                        continue;
                    }
                    for (Player player : match.players.values()) {
                        if (player.alive) {
                            double tryX = Math.max(20, Math.min(MAP_WIDTH - 20, player.x + player.vx));
                            double tryY = Math.max(20, Math.min(MAP_HEIGHT - 20, player.y + player.vy));
                            double newX = player.x;
                            double newY = player.y;
                            if (!collidesWithWall(tryX, player.y)) {
                                newX = tryX;
                            }
                            if (!collidesWithWall(newX, tryY)) {
                                newY = tryY;
                            }
                            player.x = newX;
                            player.y = newY;
                        }
                    }
                    updateProjectiles(matchId);
                    checkCollisions(matchId);
                    cleanupDeadPlayers(matchId);
                    broadcastGameState(matchId);
                }
            }
            // Remove finished matches
            for (Long id : matchesToRemove) {
                // Broadcast winner/gameOver state one last time before removal
                broadcastGameState(id);
                matches.remove(id);
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
        public int currentWeaponIndex = 0;
        public long lastShootTime = 0;
        
        public Player(int id, String username) {
            this.id = id;
            this.username = username;
            // Random spawn position
            this.x = 100 + Math.random() * (MAP_WIDTH - 200);
            this.y = 100 + Math.random() * (MAP_HEIGHT - 200);
            this.currentWeaponIndex = 0;
        }
        public Weapon getWeapon() {
            return GUN_GAME_WEAPONS.get(Math.max(0, Math.min(currentWeaponIndex, GUN_GAME_WEAPONS.size() - 1)));
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
        if (match != null && !match.gameOver) {
            Player player = match.players.get(playerId);
            if (player != null && player.alive) {
                player.vx = vx;
                player.vy = vy;
                player.rotation = rotation;
                System.out.println("Set velocity for player " + playerId + ": vx=" + vx + ", vy=" + vy);
            } else {
                System.out.println("Player not found or not alive: " + playerId);
            }
        } else if (match != null && match.gameOver) {
            // Ignore movement during game over
        } else {
            System.out.println("Match not found: " + matchId);
        }
    }

    // Player shoots in match
    public void playerShoot(long matchId, int playerId, double direction) {
        MatchState match = matches.get(matchId);
        if (match != null && !match.gameOver) {
            Player player = match.players.get(playerId);
            if (player != null && player.alive) {
                Weapon weapon = player.getWeapon();
                long now = System.currentTimeMillis();
                if (now - player.lastShootTime < (long)(weapon.fireRate * 1000)) {
                    return; // fire rate limit
                }
                player.lastShootTime = now;
                for (int i = 0; i < weapon.bulletsPerShot; i++) {
                    double spreadAngle = weapon.spread * (Math.random() - 0.5);
                    double shotDir = direction + spreadAngle;
                    Projectile bullet = new Projectile(player.x, player.y, shotDir, playerId);
                    match.projectiles.put(bullet.id, bullet);
                }
                broadcastGameState(matchId);
            }
        }
    }

    // Update projectiles in match
    public void updateProjectiles(long matchId) {
        MatchState match = matches.get(matchId);
        if (match != null) {
            long currentTime = System.currentTimeMillis();
            match.projectiles.entrySet().removeIf(entry -> {
                Projectile proj = entry.getValue();
                if (currentTime - proj.createTime > 3000) {
                    return true;
                }
                proj.x += Math.cos(proj.direction) * PROJECTILE_SPEED * 0.016;
                proj.y += Math.sin(proj.direction) * PROJECTILE_SPEED * 0.016;
                // Remove if out of bounds or hits wall
                return proj.x < 0 || proj.x > MAP_WIDTH || proj.y < 0 || proj.y > MAP_HEIGHT || projectileHitsWall(proj.x, proj.y);
            });
        }
    }

    // Check collisions in match
    public void checkCollisions(long matchId) {
        MatchState match = matches.get(matchId);
        if (match != null && !match.gameOver) {
            match.projectiles.entrySet().removeIf(projEntry -> {
                Projectile proj = projEntry.getValue();
                for (Player player : match.players.values()) {
                    if (player.id == proj.playerId || !player.alive) {
                        continue;
                    }
                    double dx = proj.x - player.x;
                    double dy = proj.y - player.y;
                    if (Math.sqrt(dx * dx + dy * dy) < 25) {
                        Player shooter = match.players.get(proj.playerId);
                        if (shooter != null) {
                            Weapon weapon = shooter.getWeapon();
                            player.health -= weapon.damage;
                            if (player.health <= 0) {
                                player.alive = false;
                                player.health = 0;
                                player.deathTime = System.currentTimeMillis();
                                // Gun Game: advance killer's weapon
                                shooter.currentWeaponIndex++;
                                if (shooter.currentWeaponIndex >= GUN_GAME_WEAPONS.size()) {
                                    match.gameOver = true;
                                    match.winnerName = shooter.username;
                                    match.winnerAnnounceTime = System.currentTimeMillis();
                                    System.out.println("WINNER: " + shooter.username + " (ID: " + shooter.id + ")");
                                    shooter.currentWeaponIndex = GUN_GAME_WEAPONS.size() - 1;
                                }
                            }
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
                // Add winner/gameOver info if present
                if (match.gameOver) {
                    // Use reflection or extend GameState if needed for frontend
                    // For now, just print
                    System.out.println("Broadcasting WINNER: " + match.winnerName);
                }
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

    private void fetchWallsIfNeeded() {
        // Fetch wall data every 10 seconds or if empty
        if (wallRects.isEmpty() || System.currentTimeMillis() - lastWallFetch > 10000) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                List<Map<String, Object>> walls = restTemplate.getForObject(WALLS_API_URL, List.class);
                if (walls != null) {
                    wallRects = walls;
                    lastWallFetch = System.currentTimeMillis();
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch wall data: " + e.getMessage());
            }
        }
    }

    private boolean collidesWithWall(double x, double y) {
        for (Map<String, Object> wall : wallRects) {
            double wx = ((Number) wall.get("x")).doubleValue();
            double wy = ((Number) wall.get("y")).doubleValue();
            double ww = ((Number) wall.get("width")).doubleValue();
            double wh = ((Number) wall.get("height")).doubleValue();
            // AABB vs circle collision
            double closestX = Math.max(wx, Math.min(x, wx + ww));
            double closestY = Math.max(wy, Math.min(y, wy + wh));
            double dx = x - closestX;
            double dy = y - closestY;
            if (dx * dx + dy * dy < PLAYER_RADIUS * PLAYER_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private boolean projectileHitsWall(double x, double y) {
        for (Map<String, Object> wall : wallRects) {
            double wx = ((Number) wall.get("x")).doubleValue();
            double wy = ((Number) wall.get("y")).doubleValue();
            double ww = ((Number) wall.get("width")).doubleValue();
            double wh = ((Number) wall.get("height")).doubleValue();
            if (x >= wx && x <= wx + ww && y >= wy && y <= wy + wh) {
                return true;
            }
        }
        return false;
    }
}
