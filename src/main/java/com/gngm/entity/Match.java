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

    @ManyToOne
    @JoinColumn(name = "map_id")
    private MapEntity map;

    @ManyToMany
    @JoinTable(
        name = "match_players",
        joinColumns = @JoinColumn(name = "match_id"),
        inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    private Set<Player> players = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private MatchState state;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private Player winner;

    private LocalDateTime createdAt;

    public enum MatchState {
        WAITING,
        IN_PROGRESS,
        FINISHED
    }
} 