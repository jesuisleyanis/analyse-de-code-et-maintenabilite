package com.epsi.rpg.model;

import jakarta.persistence.*;

@Entity
@Table(name = "solo_enemies")
public class SoloEnemy extends BaseEnemy {

    @Id
    @Column(name = "player_id")
    private String playerId;

    public SoloEnemy() {}

    public SoloEnemy(String playerId, String name, int hp, int maxHp, int level, String sprite) {
        super(name, hp, maxHp, level, sprite);
        this.playerId = playerId;
    }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
}
