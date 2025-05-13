package com.gngm.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

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
    }
} 