package com.gngm.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "weapons")
@Data
public class Weapon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int weaponOrder;

    private int damage;

    private double fireRate;

    public Weapon() {}

    public Weapon(Long id, String name, int weaponOrder, int damage, double fireRate) {
        this.id = id;
        this.name = name;
        this.weaponOrder = weaponOrder;
        this.damage = damage;
        this.fireRate = fireRate;
    }
} 