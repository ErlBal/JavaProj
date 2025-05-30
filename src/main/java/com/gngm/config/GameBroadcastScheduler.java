package com.gngm.config;

import com.gngm.controller.GameWebSocketController;
import com.gngm.service.GameEngineService;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class GameBroadcastScheduler {
    private final GameEngineService gameEngineService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameBroadcastScheduler(GameEngineService gameEngineService, SimpMessagingTemplate messagingTemplate) {
        this.gameEngineService = gameEngineService;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRate = 16)
    public void broadcastGameState() {
        gameEngineService.updateProjectiles();
        gameEngineService.cleanupDeadPlayers();
        messagingTemplate.convertAndSend("/topic/game/state", new GameWebSocketController.GameStateMessage(
            gameEngineService.getPlayerStates(),
            gameEngineService.getActiveProjectiles()
        ));
    }
} 