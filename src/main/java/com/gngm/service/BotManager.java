package com.gngm.service;

import com.gngm.entity.BotPlayer;
import com.gngm.entity.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BotManager {
    private final Map<Long, BotPlayer> bots = new ConcurrentHashMap<>();
    private final GameEngineService gameEngineService;
    private static final int MAX_BOTS = 3; // Maximum number of bots in the game
    private static long botIdCounter = 10000; // Start from a high number to avoid collision with real players

    @Autowired
    public BotManager(GameEngineService gameEngineService) {
        this.gameEngineService = gameEngineService;
    }

    public void addBot() {
        if (bots.size() < MAX_BOTS) {
            BotPlayer bot = new BotPlayer();
            bot.setId(botIdCounter++); // Assign unique ID
            bot.setUsername("Bot" + bot.getId());
            bot.setPassword("bot"); // Bots don't need real passwords
            bots.put(bot.getId(), bot);
            gameEngineService.initializePlayer(bot);
        }
    }

    public void removeBot(Long botId) {
        bots.remove(botId);
    }

    @Scheduled(fixedRate = 16) // Update bots at 60 FPS
    public void updateBots() {
        double currentTime = System.currentTimeMillis();
        
        // Get the first real player's position (for bot targeting)
        Map<Long, GameEngineService.PlayerState> playerStates = gameEngineService.getPlayerStates();
        double playerX = 0;
        double playerY = 0;
        
        // Find first alive player
        for (GameEngineService.PlayerState state : playerStates.values()) {
            if (state.isAlive()) {
                playerX = state.getX();
                playerY = state.getY();
                break;
            }
        }

        // Update each bot
        for (BotPlayer bot : bots.values()) {
            GameEngineService.PlayerState botState = gameEngineService.getPlayerState(bot.getId());
            if (botState != null && botState.isAlive()) {
                bot.update(currentTime, playerX, playerY);
                
                // Update bot's position in game engine
                gameEngineService.updatePlayerMovement(
                    bot.getId(),
                    Math.cos(bot.getRotation()) * BotPlayer.BOT_MOVEMENT_SPEED,
                    Math.sin(bot.getRotation()) * BotPlayer.BOT_MOVEMENT_SPEED,
                    bot.getRotation()
                );

                // Handle bot shooting
                if (currentTime - bot.getLastShootTime() > BotPlayer.BOT_SHOOTING_COOLDOWN) {
                    double dx = playerX - bot.getX();
                    double dy = playerY - bot.getY();
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    
                    if (distance < BotPlayer.BOT_SHOOTING_RANGE) {
                        gameEngineService.handleShooting(bot.getId(), bot.getRotation());
                    }
                }
            }
        }
    }

    public Map<Long, BotPlayer> getBots() {
        return bots;
    }
} 