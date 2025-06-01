package com.gngm.controller;

import com.gngm.dto.AuthenticationRequest;
import com.gngm.dto.AuthenticationResponse;
import com.gngm.dto.PlayerRegistrationRequest;
import com.gngm.entity.Player;
import com.gngm.security.JwtService;
import com.gngm.service.PlayerService;
import com.gngm.service.GameEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final PlayerService playerService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final GameEngineService gameEngineService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public AuthController(PlayerService playerService, JwtService jwtService, AuthenticationManager authenticationManager, UserDetailsService userDetailsService, GameEngineService gameEngineService, SimpMessagingTemplate messagingTemplate) {
        this.playerService = playerService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.gameEngineService = gameEngineService;
        this.messagingTemplate = messagingTemplate;
    }    @PostMapping("/register")
    public AuthenticationResponse register(@RequestBody PlayerRegistrationRequest request) {
        Player player = playerService.registerPlayer(request);
        // Player will be initialized in game state when they join via WebSocket
        UserDetails userDetails = userDetailsService.loadUserByUsername(player.getUsername());
        String jwt = jwtService.generateToken(userDetails);
        return AuthenticationResponse.builder().token(jwt).id(player.getId()).build();
    }    @PostMapping("/login")
    public AuthenticationResponse login(@RequestBody AuthenticationRequest request) {
        Player player = playerService.authenticatePlayer(request);
        // Player will be initialized in game state when they join via WebSocket
        UserDetails userDetails = userDetailsService.loadUserByUsername(player.getUsername());
        String jwt = jwtService.generateToken(userDetails);
        return AuthenticationResponse.builder().token(jwt).id(player.getId()).build();
    }
} 