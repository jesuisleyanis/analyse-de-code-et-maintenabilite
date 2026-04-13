package com.epsi.rpg.repository;

import com.epsi.rpg.model.GameLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameLogRepository extends JpaRepository<GameLog, Long> {
    List<GameLog> findTop200ByOrderByTsDesc();
}
