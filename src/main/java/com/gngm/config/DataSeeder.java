package com.gngm.config;

import com.gngm.entity.MapEntity;
import com.gngm.entity.Weapon;
import com.gngm.repository.MapRepository;
import com.gngm.repository.WeaponRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataSeeder {
    @Bean
    public CommandLineRunner seedData(WeaponRepository weaponRepository, MapRepository mapRepository) {
        return args -> {
            if (weaponRepository.count() == 0) {
                weaponRepository.saveAll(List.of(
                    new Weapon(null, "Pistol", 1, 20, 2.0),
                    new Weapon(null, "Shotgun", 2, 50, 0.8),
                    new Weapon(null, "SMG", 3, 15, 10.0),
                    new Weapon(null, "Rifle", 4, 35, 4.0),
                    new Weapon(null, "Knife", 5, 100, 0.5)
                ));
            }
            if (mapRepository.count() == 0) {
                mapRepository.saveAll(List.of(
                    new MapEntity(null, "Warehouse", "{\"layout\":\"simple\"}"),
                    new MapEntity(null, "Mansion", "{\"layout\":\"complex\"}")
                ));
            }
        };
    }
} 