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
        for (Long matchId : gameEngineService.matches.keySet()) {
            gameEngineService.updateProjectiles(matchId);
            // Optionally, add cleanupDeadPlayers per match if needed
            // gameEngineService.cleanupDeadPlayers(matchId);
            // Logging per match (optional)
            var match = gameEngineService.matches.get(matchId);
            if (match != null && System.currentTimeMillis() - lastLogTime > 3000) {
                var players = match.players;
                System.out.println("=== GAME STATE DEBUG (Match " + matchId + ") ===");
                System.out.println("Current player count: " + players.size());
                players.forEach((playerId, player) -> {
                    System.out.println("Player " + playerId + ": " + player.username +
                        " at (" + player.x + ", " + player.y + ") health=" + player.health +
                        " alive=" + player.alive);
                });
                System.out.println("========================");
                lastLogTime = System.currentTimeMillis();
            }
        }
        
        // Create GameState and broadcast
        GameEngineService.GameState gameState = gameEngineService.getGameState();
        messagingTemplate.convertAndSend("/topic/game/state", gameState);
    }
    
    private long lastLogTime = 0;

    // Clean up inactive matches every 10 minutes
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void cleanupInactiveMatches() {
        matchService.cleanupInactiveMatches();
    }
}