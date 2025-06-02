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
        private long matchId;

        public BotData(int id, String username, long matchId) {
            this.id = id;
            this.username = username;
            this.matchId = matchId;
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
        public long getMatchId() { return matchId; }
    }

    public void addBot(long matchId) {
        if (bots.size() < MAX_BOTS) {
            int botId = botIdCounter++;
            String username = "Bot" + botId;
            BotData bot = new BotData(botId, username, matchId);
            bots.put(botId, bot);
            gameEngineService.addPlayer(matchId, botId, username);
            System.out.println("🤖 Added bot: " + username + " to match " + matchId);
        }
    }

    public void removeBot(int botId) {
        BotData bot = bots.remove(botId);
        if (bot != null) {
            gameEngineService.removePlayer(bot.getMatchId(), botId);
            System.out.println("🤖 Removed bot: " + botId + " from match " + bot.getMatchId());
        }
    }

    @Scheduled(fixedRate = 100) // Update bots every 100ms
    public void updateBots() {
        for (BotData bot : bots.values()) {
            updateBotBehavior(bot);
        }
    }
    private void updateBotBehavior(BotData bot) {
        // Simple bot AI: move randomly and shoot occasionally
        GameEngineService.MatchState match = gameEngineService.matches.get(bot.getMatchId());
        if (match == null) return;
        GameEngineService.Player player = match.players.get(bot.getId());
        if (player == null || !player.alive) return;
        // Move towards target
        double dx = bot.getTargetX() - player.x;
        double dy = bot.getTargetY() - player.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < 50) {
            bot.setTargetX(Math.random() * 1600);
            bot.setTargetY(Math.random() * 1200);
        } else {
            double moveSpeed = 2.0;
            double moveX = (dx / distance) * moveSpeed;
            double moveY = (dy / distance) * moveSpeed;
            double rotation = Math.atan2(dy, dx);
            gameEngineService.movePlayer(bot.getMatchId(), bot.getId(), moveX, moveY, rotation);
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - bot.getLastShootTime() > 2000) {
            GameEngineService.Player nearestPlayer = findNearestPlayer(player, match);
            if (nearestPlayer != null) {
                double shootDx = nearestPlayer.x - player.x;
                double shootDy = nearestPlayer.y - player.y;
                double shootDistance = Math.sqrt(shootDx * shootDx + shootDy * shootDy);
                if (shootDistance < 400) {
                    double shootDirection = Math.atan2(shootDy, shootDx);
                    gameEngineService.playerShoot(bot.getMatchId(), bot.getId(), shootDirection);
                    bot.setLastShootTime(currentTime);
                }
            }
        }
    }
    private GameEngineService.Player findNearestPlayer(GameEngineService.Player botPlayer, GameEngineService.MatchState match) {
        GameEngineService.Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (GameEngineService.Player player : match.players.values()) {
            if (player.id == botPlayer.id || !player.alive) continue;
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