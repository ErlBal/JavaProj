package com.gngm.controller;

import com.gngm.entity.Player;
import com.gngm.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final PlayerService playerService;

    @Autowired
    public AdminController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/players")
    public ResponseEntity<List<Player>> getAllPlayers() {
        return ResponseEntity.ok(playerService.getAllPlayers());
    }

    @GetMapping("/players/{id}")
    public ResponseEntity<Player> getPlayerById(@PathVariable Long id) {
        return ResponseEntity.ok(playerService.getPlayerById(id));
    }

    @PostMapping("/players/{id}/ban")
    public ResponseEntity<Player> banPlayer(
            @PathVariable Long id,
            @RequestParam Long adminId) {
        return ResponseEntity.ok(playerService.banPlayer(id, adminId));
    }

    @PostMapping("/players/{id}/unban")
    public ResponseEntity<Player> unbanPlayer(
            @PathVariable Long id,
            @RequestParam Long adminId) {
        return ResponseEntity.ok(playerService.unbanPlayer(id, adminId));
    }

    @DeleteMapping("/players/{id}")
    public ResponseEntity<Void> deletePlayer(
            @PathVariable Long id,
            @RequestParam Long adminId) {
        playerService.deletePlayer(id, adminId);
        return ResponseEntity.ok().build();
    }
} 