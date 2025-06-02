package com.gngm.controller;

import com.gngm.dto.MatchResponse;
import com.gngm.entity.Match;
import com.gngm.entity.Player;
import com.gngm.service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gngm.dto.MatchCreateRequest;
import com.gngm.dto.MatchJoinRequest;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    @Autowired
    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping("")
    public ResponseEntity<Match> createMatch(@RequestBody MatchCreateRequest request) {
        // Check if matchName already exists (case-sensitive)
        if (matchService.findByMatchName(request.getMatchName()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
        Match match = new Match();
        match.setMatchName(request.getMatchName());
        match.setMaxPlayers(request.getMaxPlayers() != null ? request.getMaxPlayers() : 4);
        match.setStartTime(LocalDateTime.now());
        match.setIsActive(true);
        matchService.save(match);
        return ResponseEntity.ok(match);
    }

    @PostMapping("/matches/join")
    public ResponseEntity<Match> joinMatch(@RequestBody MatchJoinRequest request) {
        Optional<Match> matchOpt = matchService.findByMatchName(request.getMatchName());
        if (matchOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        Match match = matchOpt.get();
        // ... add player to match logic ...
        return ResponseEntity.ok(match);
    }

    @GetMapping("/matches")
    public ResponseEntity<List<Match>> listMatches(@RequestParam(required = false) String search) {
        List<Match> matches;
        if (search != null && !search.isEmpty()) {
            matches = matchService.findByMatchNameContaining(search);
        } else {
            matches = matchService.findAll();
        }
        return ResponseEntity.ok(matches);
    }

    @PostMapping("/active")
    public ResponseEntity<List<Match>> getActiveMatches() {
        return ResponseEntity.ok(matchService.getActiveMatches());
    }

    @PostMapping("/{matchId}/leave")
    public ResponseEntity<Match> leaveMatch(
            @PathVariable Long matchId,
            @RequestParam Long playerId) {
        return ResponseEntity.ok(matchService.leaveMatch(matchId, playerId));
    }

    @GetMapping("/player/{playerId}/current")
    public ResponseEntity<Match> getCurrentPlayerMatch(@PathVariable Long playerId) {
        Optional<Match> activeMatch = matchService.findPlayerActiveMatch(playerId);
        if (activeMatch.isPresent()) {
            return ResponseEntity.ok(activeMatch.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<Match>> getPlayerMatches(@PathVariable Long playerId) {
        return ResponseEntity.ok(matchService.getPlayerMatches(playerId));
    }
}