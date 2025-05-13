package com.gngm.service;

import com.gngm.dto.PlayerLoginRequest;
import com.gngm.dto.PlayerRegistrationRequest;
import com.gngm.entity.Player;
import com.gngm.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public PlayerService(PlayerRepository playerRepository, BCryptPasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Player registerPlayer(PlayerRegistrationRequest request) {
        if (playerRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        Player player = new Player();
        player.setUsername(request.getUsername());
        player.setPassword(passwordEncoder.encode(request.getPassword()));
        player.setKills(0);
        player.setDeaths(0);
        player.setWins(0);

        return playerRepository.save(player);
    }

    public Player authenticatePlayer(PlayerLoginRequest request) {
        Player player = playerRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Player not found"));

        if (!passwordEncoder.matches(request.getPassword(), player.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        return player;
    }
} 