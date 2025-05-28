package com.gngm.security;

import com.gngm.entity.Player;
import com.gngm.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class PlayerUserDetailsService implements UserDetailsService {
    private final PlayerRepository playerRepository;

    @Autowired
    public PlayerUserDetailsService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Player not found: " + username));
        
        return new org.springframework.security.core.userdetails.User(
            player.getUsername(),
            player.getPassword(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + player.getRole().name()))
        );
    }
} 