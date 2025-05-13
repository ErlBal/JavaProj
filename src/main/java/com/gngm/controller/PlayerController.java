package com.gngm.controller;

import com.gngm.dto.PlayerLoginRequest;
import com.gngm.dto.PlayerRegistrationRequest;
import com.gngm.entity.Player;
import com.gngm.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;

    @Autowired
    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/register")
    public ResponseEntity<Player> registerPlayer(@RequestBody PlayerRegistrationRequest request) {
        Player player = playerService.registerPlayer(request);
        return ResponseEntity.ok(player);
    }

    @PostMapping("/login")
    public ResponseEntity<Player> loginPlayer(@RequestBody PlayerLoginRequest request) {
        Player player = playerService.authenticatePlayer(request);
        return ResponseEntity.ok(player);
    }
} 