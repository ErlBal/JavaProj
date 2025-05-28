package com.gngm.service;

import com.gngm.entity.Match;
import com.gngm.entity.Player;
import com.gngm.repository.MatchRepository;
import com.gngm.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;

    @Autowired
    public MatchService(MatchRepository matchRepository, PlayerRepository playerRepository) {
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional
    public Match createMatch(String mapName, int maxPlayers) {
        Match match = new Match(mapName, maxPlayers, LocalDateTime.now());
        return matchRepository.save(match);
    }

    @Transactional
    public Match joinMatch(Long matchId, Long playerId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        if (match.getPlayers().size() >= match.getMaxPlayers()) {
            throw new RuntimeException("Match is full");
        }

        if (!match.getIsActive()) {
            throw new RuntimeException("Match is not active");
        }

        match.getPlayers().add(player);
        return matchRepository.save(match);
    }

    @Transactional
    public Match endMatch(Long matchId, Long winnerId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        
        Player winner = playerRepository.findById(winnerId)
                .orElseThrow(() -> new RuntimeException("Winner not found"));

        match.setWinner(winner);
        match.setEndTime(LocalDateTime.now());
        match.setIsActive(false);

        // Update player stats
        winner.setWins(winner.getWins() + 1);
        playerRepository.save(winner);

        return matchRepository.save(match);
    }

    public List<Match> getActiveMatches() {
        return matchRepository.findByIsActiveTrue();
    }

    public List<Match> getPlayerMatches(Long playerId) {
        return matchRepository.findByPlayersId(playerId);
    }
} 