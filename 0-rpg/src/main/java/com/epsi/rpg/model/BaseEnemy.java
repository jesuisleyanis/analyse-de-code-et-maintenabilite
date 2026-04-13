package com.epsi.rpg.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseEnemy {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private int hp;

    @Column(name = "max_hp", nullable = false)
    private int maxHp;

    @Column(nullable = false)
    private int level;

    @Column(length = 10)
    private String sprite;

    protected BaseEnemy() {}

    protected BaseEnemy(String name, int hp, int maxHp, int level, String sprite) {
        this.name = name;
        this.hp = hp;
        this.maxHp = maxHp;
        this.level = level;
        this.sprite = sprite;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = Math.max(0, hp); }

    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getSprite() { return sprite; }
    public void setSprite(String sprite) { this.sprite = sprite; }
}
