package com.gngm.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Integer kills = 0;

    @Column(nullable = false)
    private Integer deaths = 0;

    @Column(nullable = false)
    private Integer wins = 0;

    @Column(nullable = false)
    private Boolean banned = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.PLAYER;

    // Game state fields (not persisted to database)
    @Column(insertable = false, updatable = false)
    private transient double x = 0;

    @Column(insertable = false, updatable = false)
    private transient double y = 0;

    @Column(insertable = false, updatable = false)
    private transient double rotation = 0;

    public Player(String username, String password) {
        this.username = username;
        this.password = password;
        this.kills = 0;
        this.deaths = 0;
        this.wins = 0;
        this.banned = false;
        this.role = Role.PLAYER;
    }

    // Game state methods
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getRotation() { return rotation; }
    public void setRotation(double rotation) { this.rotation = rotation; }
} 