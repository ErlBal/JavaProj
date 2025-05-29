package com.gngm.controller;

import com.gngm.service.GameEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class GameWebSocketController {

    private final GameEngineService gameEngineService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public GameWebSocketController(GameEngineService gameEngineService, SimpMessagingTemplate messagingTemplate) {
        this.gameEngineService = gameEngineService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/game/move")
    @SendTo("/topic/game/state")
    public void handleMovement(MovementMessage message) {
        // Log when movement is received
        System.out.println("[GameWebSocketController] Received movement: " + message);
        gameEngineService.updatePlayerMovement(
            message.getPlayerId(),
            message.getDeltaX(),
            message.getDeltaY(),
            message.getRotation()
        );
        broadcastGameState();
    }

    @MessageMapping("/game/shoot")
    @SendTo("/topic/game/state")
    public void handleShooting(ShootingMessage message) {
        // Log when shooting is received
        System.out.println("[GameWebSocketController] Received shooting: " + message);
        gameEngineService.handleShooting(message.getPlayerId(), message.getDirection());
        broadcastGameState();
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
        }
        public Map<Long, GameEngineService.Projectile> getProjectiles() { return projectiles; }
        public void setProjectiles(Map<Long, GameEngineService.Projectile> projectiles) {
            this.projectiles = projectiles;
        }
    }
} 