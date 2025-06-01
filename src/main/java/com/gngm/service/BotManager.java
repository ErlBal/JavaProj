package com.gngm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BotManager {
    private final Map<Integer, BotData> bots = new ConcurrentHashMap<>();
    private final GameEngineService gameEngineService;
    private static final int MAX_BOTS = 3;
    private static int botIdCounter = 10000;

    @Autowired
    public BotManager(GameEngineService gameEngineService) {
        this.gameEngineService = gameEngineService;
    }

    // Simple bot data class
    public static class BotData {
        private int id;
        private String username;
        private double targetX;
        private double targetY;
        private long lastShootTime;

        public BotData(int id, String username) {
            this.id = id;
            this.username = username;
            this.targetX = Math.random() * 1600;
            this.targetY = Math.random() * 1200;
            this.lastShootTime = 0;
        }

        // Getters and setters
        public int getId() { return id; }
        public String getUsername() { return username; }
        public double getTargetX() { return targetX; }
        public double getTargetY() { return targetY; }
        public long getLastShootTime() { return lastShootTime; }
        public void setTargetX(double targetX) { this.targetX = targetX; }
        public void setTargetY(double targetY) { this.targetY = targetY; }
        public void setLastShootTime(long lastShootTime) { this.lastShootTime = lastShootTime; }
    }

    public void addBot() {
        if (bots.size() < MAX_BOTS) {
            int botId = botIdCounter++;
            String username = "Bot" + botId;
            BotData bot = new BotData(botId, username);
            bots.put(botId, bot);
            gameEngineService.addPlayer(botId, username);
            System.out.println("🤖 Added bot: " + username);
        }
    }

    public void removeBot(int botId) {
        if (bots.remove(botId) != null) {
            gameEngineService.removePlayer(botId);
            System.out.println("🤖 Removed bot: " + botId);
        }
    }

    @Scheduled(fixedRate = 100) // Update bots every 100ms
    public void updateBots() {
        for (BotData bot : bots.values()) {
            updateBotBehavior(bot);
        }
    }    private void updateBotBehavior(BotData bot) {
        // Simple bot AI: move randomly and shoot occasionally
        GameEngineService.GameState gameState = gameEngineService.getGameState();
        GameEngineService.Player player = gameState.players.get(bot.getId());
        if (player == null || !player.alive) return;

        // Move towards target
        double dx = bot.getTargetX() - player.x;
        double dy = bot.getTargetY() - player.y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < 50) {
            // Reached target, set new random target
            bot.setTargetX(Math.random() * 1600);
            bot.setTargetY(Math.random() * 1200);
        } else {
            // Move towards target
            double moveSpeed = 2.0;
            double moveX = (dx / distance) * moveSpeed;
            double moveY = (dy / distance) * moveSpeed;
            double rotation = Math.atan2(dy, dx); // Calculate rotation towards target
            
            gameEngineService.movePlayer(bot.getId(), moveX, moveY, rotation);
        }

        // Occasionally shoot
        long currentTime = System.currentTimeMillis();
        if (currentTime - bot.getLastShootTime() > 2000) { // Shoot every 2 seconds
            // Find nearest player to shoot at
            GameEngineService.Player nearestPlayer = findNearestPlayer(player, gameState);
            if (nearestPlayer != null) {
                double shootDx = nearestPlayer.x - player.x;
                double shootDy = nearestPlayer.y - player.y;
                double shootDistance = Math.sqrt(shootDx * shootDx + shootDy * shootDy);
                
                if (shootDistance < 400) { // Only shoot if within range
                    double shootDirection = Math.atan2(shootDy, shootDx);
                    gameEngineService.playerShoot(bot.getId(), shootDirection);
                    bot.setLastShootTime(currentTime);
                }
            }
        }
    }    private GameEngineService.Player findNearestPlayer(GameEngineService.Player botPlayer, GameEngineService.GameState gameState) {
        GameEngineService.Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (GameEngineService.Player player : gameState.players.values()) {
            if (player.id == botPlayer.id || !player.alive) continue; // Skip self and dead players
            
            double dx = player.x - botPlayer.x;
            double dy = player.y - botPlayer.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        
        return nearest;
    }
}