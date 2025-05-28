package com.gngm.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "maps")
@Data
public class MapEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Lob
    private String layoutData; // JSON or other format for map layout

    public MapEntity() {}

    public MapEntity(Long id, String name, String layoutData) {
        this.id = id;
        this.name = name;
        this.layoutData = layoutData;
    }
} 