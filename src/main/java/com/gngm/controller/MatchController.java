package com.gngm.controller;

import com.gngm.entity.Match;
import com.gngm.service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Optional;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    @Autowired
    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    public ResponseEntity<Match> createMatch(
            @RequestParam String mapName,
            @RequestParam int maxPlayers) {
        return ResponseEntity.ok(matchService.createMatch(mapName, maxPlayers));
    }

    @PostMapping("/{matchId}/join")
    public ResponseEntity<Match> joinMatch(
            @PathVariable Long matchId,
            @RequestParam Long playerId) {
        return ResponseEntity.ok(matchService.joinMatch(matchId, playerId));
    }

    @PostMapping("/{matchId}/end")
    public ResponseEntity<Match> endMatch(
            @PathVariable Long matchId,
            @RequestParam Long winnerId) {
        return ResponseEntity.ok(matchService.endMatch(matchId, winnerId));
    }

    @GetMapping("/active")
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