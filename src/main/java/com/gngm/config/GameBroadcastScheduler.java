package com.gngm.config;

import com.gngm.controller.GameWebSocketController;
import com.gngm.service.GameEngineService;
import com.gngm.service.MatchService;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class GameBroadcastScheduler {
    private final GameEngineService gameEngineService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MatchService matchService;

    public GameBroadcastScheduler(GameEngineService gameEngineService, SimpMessagingTemplate messagingTemplate, MatchService matchService) {
        this.gameEngineService = gameEngineService;
        this.messagingTemplate = messagingTemplate;
        this.matchService = matchService;
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

    // Clean up inactive matches every 10 minutes
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void cleanupInactiveMatches() {
        matchService.cleanupInactiveMatches();
    }
}