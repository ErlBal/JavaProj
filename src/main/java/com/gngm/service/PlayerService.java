package com.gngm.service;

import com.gngm.dto.PlayerLoginRequest;
import com.gngm.dto.PlayerRegistrationRequest;
import com.gngm.entity.Player;
import com.gngm.entity.Role;
import com.gngm.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public PlayerService(PlayerRepository playerRepository, PasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Player registerPlayer(PlayerRegistrationRequest request) {
        if (playerRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        Player player = new Player(request.getUsername(), request.getPassword());
        player.setPassword(passwordEncoder.encode(request.getPassword()));
        return playerRepository.save(player);
    }

    public Player authenticatePlayer(PlayerLoginRequest request) {
        Player player = playerRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Player not found"));

        if (!passwordEncoder.matches(request.getPassword(), player.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        if (player.getBanned()) {
            throw new RuntimeException("Player is banned");
        }

        return player;
    }

    public Player authenticatePlayer(com.gngm.dto.AuthenticationRequest request) {
        return authenticatePlayer(new com.gngm.dto.PlayerLoginRequest(request.getUsername(), request.getPassword()));
    }

    // Admin methods
    @Transactional
    public Player banPlayer(Long playerId, Long adminId) {
        Player admin = playerRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admins can ban players");
        }

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        player.setBanned(true);
        return playerRepository.save(player);
    }

    @Transactional
    public Player unbanPlayer(Long playerId, Long adminId) {
        Player admin = playerRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admins can unban players");
        }

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        player.setBanned(false);
        return playerRepository.save(player);
    }

    @Transactional
    public void deletePlayer(Long playerId, Long adminId) {
        Player admin = playerRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admins can delete players");
        }

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        playerRepository.delete(player);
    }

    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    public Player getPlayerById(Long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));
    }
} 