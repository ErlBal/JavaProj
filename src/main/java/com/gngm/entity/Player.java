package com.gngm.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Table(name = "players")
@Data
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String password;

    private int kills;

    private int deaths;

    private int wins;

    private boolean banned;

    @Enumerated(EnumType.STRING)
    private Role role = Role.PLAYER;

    // Default constructor for JPA
    public Player() {
    }

    // Constructor for convenience
    public Player(String username, String password) {
        this.username = username;
        this.password = password;
        this.kills = 0;
        this.deaths = 0;
        this.wins = 0;
        this.banned = false;
        this.role = Role.PLAYER;
    }
} 