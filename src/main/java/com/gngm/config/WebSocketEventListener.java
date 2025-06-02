package com.gngm.config;

import com.gngm.service.MatchService;
import com.gngm.service.PlayerService;
import com.gngm.service.GameEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);
    
    private final MatchService matchService;
    private final PlayerService playerService;
    private final GameEngineService gameEngineService;
    
    // Track connected players by session ID
    private final ConcurrentHashMap<String, Long> connectedPlayers = new ConcurrentHashMap<>();

    @Autowired
    public WebSocketEventListener(MatchService matchService, PlayerService playerService, GameEngineService gameEngineService) {
        this.matchService = matchService;
        this.playerService = playerService;
        this.gameEngineService = gameEngineService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // You could extract player ID from headers if needed
        // For now, we'll track it when they send their first message
        logger.info("WebSocket connection established: {}", sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        logger.info("WebSocket connection closed: {}", sessionId);

        // Get the player ID for this session
        Long playerId = connectedPlayers.remove(sessionId);

        if (playerId != null) {
            try {
                // Remove player from active matches
                var playerMatches = matchService.getPlayerMatches(playerId);
                for (var match : playerMatches) {
                    if (match.getIsActive()) {
                        logger.info("Removing disconnected player {} from match {}", playerId, match.getId());
                        matchService.leaveMatch(match.getId(), playerId);
                        gameEngineService.removePlayer(match.getId(), playerId.intValue());
                    }
                }
                logger.info("Player disconnected: {}", playerId);
            } catch (Exception e) {
                logger.error("Error handling disconnection for player {}: {}", playerId, e.getMessage());
            }
        }
    }
    
    // Method to register a player for this session
    public void registerPlayerSession(String sessionId, Long playerId) {
        connectedPlayers.put(sessionId, playerId);
        logger.info("Registered player {} for session {}", playerId, sessionId);
    }
    
    // Method to get player ID for a session
    public Long getPlayerForSession(String sessionId) {
        return connectedPlayers.get(sessionId);
    }
}
