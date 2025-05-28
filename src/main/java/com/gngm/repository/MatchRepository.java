package com.gngm.repository;

import com.gngm.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByIsActiveTrue();
    List<Match> findByPlayersId(Long playerId);
} 