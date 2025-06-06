package com.gngm.dto;

import lombok.Data;

@Data
public class MatchCreateRequest {
    private Long mapId;
    private Long playerId;
    private String matchName;
    private Integer maxPlayers;
} 