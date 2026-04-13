package com.epsi.rpg.repository;

import com.epsi.rpg.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, String> {
}
