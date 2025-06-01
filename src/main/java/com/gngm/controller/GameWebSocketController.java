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
        public int playerId;
        public String username;
    }

    public static class MovementMessage {
        public int playerId;
        public double deltaX;
        public double deltaY;
        public double rotation;
    }

    public static class ShootingMessage {
        public int playerId;
        public double direction;
    }

    public static class RespawnMessage {
        public int playerId;
    }

    // Handle player joining
    @MessageMapping("/game/join")
    public void handlePlayerJoin(PlayerJoinMessage message) {
        System.out.println("Player joining: " + message.playerId + " - " + message.username);
        gameEngine.addPlayer(message.playerId, message.username);
    }

    // Handle player movement
    @MessageMapping("/game/move")
    public void handlePlayerMovement(MovementMessage message) {
        gameEngine.movePlayer(message.playerId, message.deltaX, message.deltaY, message.rotation);
    }

    // Handle player shooting
    @MessageMapping("/game/shoot")
    public void handlePlayerShooting(ShootingMessage message) {
        gameEngine.playerShoot(message.playerId, message.direction);
    }

    // Handle player respawn
    @MessageMapping("/game/respawn")
    public void handlePlayerRespawn(RespawnMessage message) {
        System.out.println("Player respawning: " + message.playerId);
        gameEngine.respawnPlayer(message.playerId);
    }
}
