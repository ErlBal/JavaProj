package com.gngm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponse {
    private Long id;
    private String mapName;
    private List<String> playerUsernames;
    private String state;
    private String winnerUsername;
} 