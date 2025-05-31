package com.gngm.controller;

import com.gngm.service.GameEngineService;
import com.gngm.service.BotManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class GameWebSocketController {

    private final GameEngineService gameEngineService;
    private final BotManager botManager;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public GameWebSocketController(GameEngineService gameEngineService, BotManager botManager, SimpMessagingTemplate messagingTemplate) {
        this.gameEngineService = gameEngineService;
        this.botManager = botManager;
        this.messagingTemplate = messagingTemplate;
    }    @MessageMapping("/game/move")
    @SendTo("/topic/game/state")
    public void handleMovement(MovementMessage message) {
        gameEngineService.updatePlayerMovement(
            message.getPlayerId(),
            message.getDeltaX(),
            message.getDeltaY(),
            message.getRotation()
        );
        broadcastGameState();
    }

    @MessageMapping("/game/join")
    public void handlePlayerJoin(PlayerJoinMessage message) {
        // Initialize player if not already initialized
        if (gameEngineService.getPlayerState(message.getPlayerId()) == null) {
            // Create a simple Player object for initialization
            com.gngm.entity.Player player = new com.gngm.entity.Player();
            player.setId(message.getPlayerId());
            player.setUsername(message.getUsername());
            
            // Initialize player with random starting position
            gameEngineService.initializePlayerWithPosition(player, 
                Math.random() * 800 + 100, // Random X between 100-900
                Math.random() * 600 + 100  // Random Y between 100-700
            );
        }
        broadcastGameState();
    }

    @MessageMapping("/game/shoot")
    @SendTo("/topic/game/state")
    public void handleShooting(ShootingMessage message) {
        gameEngineService.handleShooting(message.getPlayerId(), message.getDirection());
        broadcastGameState();
    }

    @MessageMapping("/game/addBot")
    @SendTo("/topic/game/state")
    public GameStateMessage addBot() {
        System.out.println("Adding new bot...");
        botManager.addBot();
        return new GameStateMessage(
            gameEngineService.getPlayerStates(),
            gameEngineService.getActiveProjectiles()
        );
    }

    @MessageMapping("/game/removeBot")
    @SendTo("/topic/game/state")
    public GameStateMessage removeBot(BotMessage message) {
        System.out.println("Removing bot with ID: " + message.getBotId());
        botManager.removeBot(message.getBotId());
        return new GameStateMessage(
            gameEngineService.getPlayerStates(),
            gameEngineService.getActiveProjectiles()
        );
    }

    private void broadcastGameState() {
        // Update projectiles
        gameEngineService.updateProjectiles();
        
        // Create game state message
        GameStateMessage state = new GameStateMessage(
            gameEngineService.getPlayerStates(),
            gameEngineService.getActiveProjectiles()
        );
        // Broadcast to all connected clients
        messagingTemplate.convertAndSend("/topic/game/state", state);
    }

    // Message classes
    public static class MovementMessage {
        private long playerId;
        private double deltaX;
        private double deltaY;
        private double rotation;

        // Getters and setters
        public long getPlayerId() { return playerId; }
        public void setPlayerId(long playerId) { this.playerId = playerId; }
        public double getDeltaX() { return deltaX; }
        public void setDeltaX(double deltaX) { this.deltaX = deltaX; }
        public double getDeltaY() { return deltaY; }
        public void setDeltaY(double deltaY) { this.deltaY = deltaY; }
        public double getRotation() { return rotation; }
        public void setRotation(double rotation) { this.rotation = rotation; }
    }

    public static class ShootingMessage {
        private long playerId;
        private double direction;

        // Getters and setters
        public long getPlayerId() { return playerId; }
        public void setPlayerId(long playerId) { this.playerId = playerId; }
        public double getDirection() { return direction; }
        public void setDirection(double direction) { this.direction = direction; }
    }

    public static class BotMessage {
        private long botId;

        // Getters and setters
        public long getBotId() { return botId; }
        public void setBotId(long botId) { this.botId = botId; }
    }

    public static class GameStateMessage {
        private Map<Long, GameEngineService.PlayerState> playerStates;
        private Map<Long, GameEngineService.Projectile> projectiles;

        public GameStateMessage(Map<Long, GameEngineService.PlayerState> playerStates,
                              Map<Long, GameEngineService.Projectile> projectiles) {
            this.playerStates = playerStates;
            this.projectiles = projectiles;
        }

        // Getters and setters
        public Map<Long, GameEngineService.PlayerState> getPlayerStates() { return playerStates; }
        public void setPlayerStates(Map<Long, GameEngineService.PlayerState> playerStates) {
            this.playerStates = playerStates;
        }        public Map<Long, GameEngineService.Projectile> getProjectiles() { return projectiles; }
        public void setProjectiles(Map<Long, GameEngineService.Projectile> projectiles) {
            this.projectiles = projectiles;
        }
    }

    public static class PlayerJoinMessage {
        private long playerId;
        private String username;

        // Getters and setters
        public long getPlayerId() { return playerId; }
        public void setPlayerId(long playerId) { this.playerId = playerId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }
} 