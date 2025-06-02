package com.gngm.repository;

import com.gngm.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByIsActiveTrue();
    List<Match> findByPlayersId(Long playerId);
    Optional<Match> findByMatchName(String matchName);
    List<Match> findByMatchNameContaining(String search);
} 