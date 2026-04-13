package com.epsi.rpg.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "players")
public class Player {

    @Id
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "player_class", nullable = false, length = 50)
    private String playerClass;

    @Column(nullable = false)
    private int hp;

    @Column(name = "max_hp", nullable = false)
    private int maxHp;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false)
    private int xp;

    @Column(name = "auto_lvl", nullable = false)
    private int autoLvl;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private GameMode mode;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "player_inventory", joinColumns = @JoinColumn(name = "player_id"))
    @Column(name = "item")
    @OrderColumn(name = "item_index")
    private List<String> inventory = new ArrayList<>();

    public Player() {}

    public Player(String id, String name, String playerClass, PlayerStats stats,
                  GameMode mode, List<String> inventory) {
        this.id = id;
        this.name = name;
        this.playerClass = playerClass;
        this.hp = stats.hp();
        this.maxHp = stats.maxHp();
        this.level = stats.level();
        this.xp = stats.xp();
        this.autoLvl = stats.autoLvl();
        this.mode = mode;
        this.inventory = new ArrayList<>(inventory);
    }

    public record PlayerStats(int hp, int maxHp, int level, int xp, int autoLvl) {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPlayerClass() { return playerClass; }
    public void setPlayerClass(String playerClass) { this.playerClass = playerClass; }

    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = Math.max(0, hp); }

    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public int getAutoLvl() { return autoLvl; }
    public void setAutoLvl(int autoLvl) { this.autoLvl = autoLvl; }

    public GameMode getMode() { return mode; }
    public void setMode(GameMode mode) { this.mode = mode; }

    public List<String> getInventory() { return inventory; }
    public void setInventory(List<String> inventory) { this.inventory = new ArrayList<>(inventory); }

    public boolean isAlive() { return hp > 0; }

    public int getNextLevelXp() {
        return (int) (100 * Math.pow(1.4, (double) level - 1));
    }
}
