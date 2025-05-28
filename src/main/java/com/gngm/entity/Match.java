package com.gngm.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "matches")
@Data
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany
    @JoinTable(
        name = "match_players",
        joinColumns = @JoinColumn(name = "match_id"),
        inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    private Set<Player> players = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private Player winner;

    private LocalDateTime startTime;
    
    private LocalDateTime endTime;

    private String mapName;

    private int maxPlayers;

    private boolean isActive;

    // Default constructor for JPA
    public Match() {
    }

    // Constructor for convenience
    public Match(String mapName, int maxPlayers) {
        this.mapName = mapName;
        this.maxPlayers = maxPlayers;
        this.startTime = LocalDateTime.now();
        this.isActive = true;
    }
} 