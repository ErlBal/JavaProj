package com.gngm.controller;

import com.gngm.service.GameEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final GameEngineService gameEngine;

    // Message classes
    public static class PlayerJoinMessage {
        public long matchId;
        public int playerId;
        public String username;
    }

    public static class MovementMessage {
        public long matchId;
        public int playerId;
        public double deltaX;
        public double deltaY;
        public double rotation;
    }

    public static class ShootingMessage {
        public long matchId;
        public int playerId;
        public double direction;
    }

    public static class RespawnMessage {
        public long matchId;
        public int playerId;
    }

    // Handle player joining
    @MessageMapping("/game/join")
    public void handlePlayerJoin(PlayerJoinMessage message) {
        System.out.println("Player joining: " + message.playerId + " - " + message.username + " to match " + message.matchId);
        gameEngine.addPlayer(message.matchId, message.playerId, message.username);
    }

    // Handle player movement
    @MessageMapping("/game/move")
    public void handlePlayerMovement(MovementMessage message) {
        gameEngine.movePlayer(message.matchId, message.playerId, message.deltaX, message.deltaY, message.rotation);
    }

    // Handle player shooting
    @MessageMapping("/game/shoot")
    public void handlePlayerShooting(ShootingMessage message) {
        gameEngine.playerShoot(message.matchId, message.playerId, message.direction);
    }

    // Handle player respawn
    @MessageMapping("/game/respawn")
    public void handlePlayerRespawn(RespawnMessage message) {
        System.out.println("Player respawning: " + message.playerId + " in match " + message.matchId);
        gameEngine.respawnPlayer(message.matchId, message.playerId);
    }
}
