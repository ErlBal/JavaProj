package com.gngm.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Column(nullable = false)
    private String mapName;

    @Column(nullable = false)
    private Integer maxPlayers = 4;

    @Column(nullable = false)
    private Boolean isActive = true;

    public Match(String mapName, Integer maxPlayers, LocalDateTime startTime) {
        this.mapName = mapName;
        this.maxPlayers = maxPlayers;
        this.startTime = startTime;
        this.isActive = true;
    }
} 