package com.epsi.rpg.model;

import jakarta.persistence.*;

@Entity
@Table(name = "world_boss")
public class WorldBoss extends BaseEnemy {

    @Id
    private int id;

    public WorldBoss() {}

    public WorldBoss(int id, String name, int hp, int maxHp, int level, String sprite) {
        super(name, hp, maxHp, level, sprite);
        this.id = id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}
