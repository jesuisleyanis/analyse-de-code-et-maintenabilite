package com.epsi.rpg.service;

import com.epsi.rpg.combat.CombatStrategy;
import com.epsi.rpg.event.GameEvent;
import com.epsi.rpg.item.ItemCommand;
import com.epsi.rpg.model.*;
import com.epsi.rpg.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GameService {

    private static final String KEY_LEVEL = "level";
    private static final String KEY_MAX_HP = "maxHp";
    private static final String MSG_JOUEUR_INTROUVABLE = "Joueur introuvable";

    private final PlayerRepository playerRepo;
    private final SoloEnemyRepository enemyRepo;
    private final WorldBossRepository bossRepo;
    private final GameLogRepository logRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, CombatStrategy> strategies;
    private final List<ItemCommand> itemCommands;
    private final SecureRandom random = new SecureRandom();

    public GameService(PlayerRepository playerRepo, SoloEnemyRepository enemyRepo,
                       WorldBossRepository bossRepo, GameLogRepository logRepo,
                       ApplicationEventPublisher eventPublisher,
                       List<CombatStrategy> strategyList, List<ItemCommand> itemCommands) {
        this.playerRepo = playerRepo;
        this.enemyRepo = enemyRepo;
        this.bossRepo = bossRepo;
        this.logRepo = logRepo;
        this.eventPublisher = eventPublisher;
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(CombatStrategy::getMode, Function.identity()));
        this.itemCommands = itemCommands;
    }

    @Transactional
    public Map<String, Object> getHeroState(String playerId) {
        Player player = ensurePlayerExists(playerId);
        processLevelUp(player);
        return buildState(player);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllPlayers() {
        return playerRepo.findAll().stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("class", p.getPlayerClass());
            m.put(KEY_LEVEL, p.getLevel());
            m.put("hp", p.getHp());
            m.put(KEY_MAX_HP, p.getMaxHp());
            return m;
        }).toList();
    }

    @Transactional
    public Map<String, Object> setupHero(String playerId, String name, String playerClass, String mode) {
        if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId requis");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name requis");
        if (playerClass == null || playerClass.isBlank()) throw new IllegalArgumentException("class requis");

        Player player = ensurePlayerExists(playerId);
        player.setName(name);
        player.setPlayerClass(playerClass);

        if (mode != null && !mode.isBlank()) {
            player.setMode(GameMode.valueOf(mode));
        }

        playerRepo.save(player);
        eventPublisher.publishEvent(new GameEvent(this, "Le héros " + name + " a commencé sa quête."));
        return buildState(player);
    }

    @Transactional
    public Map<String, Object> attack(String playerId, String targetId) {
        Player player = playerRepo.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_JOUEUR_INTROUVABLE));

        if (!player.isAlive()) return buildState(player);

        int damage = 10 + random.nextInt(15);
        String mode = player.getMode().name();

        CombatStrategy strategy = strategies.get(mode);
        if (strategy == null) throw new IllegalArgumentException("Mode inconnu : " + mode);

        strategy.execute(player, damage, true, targetId);

        player = playerRepo.findById(playerId).orElseThrow();
        processLevelUp(player);
        return buildState(player);
    }

    @Transactional
    public Map<String, Object> autoTick(String playerId) {
        Player player = playerRepo.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_JOUEUR_INTROUVABLE));

        if (player.getAutoLvl() <= 0 || !player.isAlive()) return buildState(player);

        int damage = player.getAutoLvl() * 5;
        String mode = player.getMode().name();

        CombatStrategy strategy = strategies.get(mode);
        if (strategy == null) return buildState(player);

        strategy.execute(player, damage, false, null);

        player = playerRepo.findById(playerId).orElseThrow();
        processLevelUp(player);
        return buildState(player);
    }

    @Transactional
    public Map<String, Object> heal(String playerId) {
        Player player = playerRepo.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_JOUEUR_INTROUVABLE));

        player.setHp(player.getMaxHp());
        playerRepo.save(player);

        eventPublisher.publishEvent(new GameEvent(this, "Système restauré pour " + player.getName()));
        return buildState(player);
    }

    @Transactional
    public boolean upgradeAuto(String playerId) {
        Player player = playerRepo.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_JOUEUR_INTROUVABLE));

        int cost = (int) Math.round(100 * Math.pow(1.3, player.getAutoLvl()));
        if (player.getXp() < cost) return false;

        player.setXp(player.getXp() - cost);
        player.setAutoLvl(player.getAutoLvl() + 1);
        playerRepo.save(player);

        eventPublisher.publishEvent(new GameEvent(this, "Auto-attaque améliorée au niveau " + player.getAutoLvl()));
        return true;
    }

    @Transactional
    public Map<String, Object> useItem(String playerId, int itemIndex) {
        Player player = playerRepo.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_JOUEUR_INTROUVABLE));

        List<String> inventory = player.getInventory();
        if (itemIndex < 0 || itemIndex >= inventory.size()) return buildState(player);

        String item = inventory.get(itemIndex);
        for (ItemCommand command : itemCommands) {
            if (command.matches(item)) {
                command.execute(player);
                break;
            }
        }

        inventory.remove(itemIndex);
        playerRepo.save(player);
        return buildState(player);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLogs() {
        List<GameLog> logs = logRepo.findTop200ByOrderByTsDesc();
        List<GameLog> chronological = new ArrayList<>(logs);
        Collections.reverse(chronological);
        return chronological.stream().map(log -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("msg", log.getMsg());
            m.put("ts", log.getTs());
            return m;
        }).toList();
    }

    @Transactional
    public void reset() {
        logRepo.deleteAll();
        enemyRepo.deleteAll();
        playerRepo.deleteAll();
        bossRepo.deleteAll();
        bossRepo.save(new WorldBoss(1, "LE MONOLITHE EPSI", 2000, 2000, 10, "👹"));
        eventPublisher.publishEvent(new GameEvent(this, "Système RPG initialisé (XP Exponentielle activée)."));
    }

    private Player ensurePlayerExists(String id) {
        return playerRepo.findById(id).orElseGet(() -> {
            Player player = new Player(id, "Invité", "Guerrier",
                    new Player.PlayerStats(100, 100, 1, 0, 0),
                    GameMode.SOLO, List.of("🧪", "🛡️"));
            playerRepo.save(player);
            enemyRepo.save(new SoloEnemy(id, "Bug Racine", 80, 80, 1, "👾"));
            return player;
        });
    }

    private void processLevelUp(Player player) {
        boolean leveled = false;
        while (player.getXp() >= player.getNextLevelXp()) {
            int cost = player.getNextLevelXp();
            player.setXp(player.getXp() - cost);
            player.setLevel(player.getLevel() + 1);
            player.setMaxHp(player.getMaxHp() + 30);
            player.setHp(player.getMaxHp());
            eventPublisher.publishEvent(new GameEvent(this, "Montée de niveau ! Niveau " + player.getLevel()));
            leveled = true;
        }
        if (leveled) playerRepo.save(player);
    }

    private Map<String, Object> buildState(Player player) {
        player = playerRepo.findById(player.getId()).orElseThrow();

        Map<String, Object> pData = new LinkedHashMap<>();
        pData.put("name", player.getName());
        pData.put("hp", player.getHp());
        pData.put(KEY_MAX_HP, player.getMaxHp());
        pData.put(KEY_LEVEL, player.getLevel());
        pData.put("xp", player.getXp());
        pData.put("class", player.getPlayerClass());
        pData.put("nextLevelXp", player.getNextLevelXp());
        pData.put("autoAttackLevel", player.getAutoLvl());
        pData.put("mode", player.getMode().name());
        pData.put("inventory", player.getInventory());

        Map<String, Object> eData = new LinkedHashMap<>();
        if (player.getMode() == GameMode.COOP) {
            fillBossData(eData);
        } else {
            Optional<SoloEnemy> enemyOpt = enemyRepo.findById(player.getId());
            if (enemyOpt.isPresent()) {
                SoloEnemy enemy = enemyOpt.get();
                eData.put("name", enemy.getName());
                eData.put("hp", enemy.getHp());
                eData.put(KEY_MAX_HP, enemy.getMaxHp());
                eData.put("sprite", enemy.getSprite());
                eData.put(KEY_LEVEL, enemy.getLevel());
            } else {
                fillBossData(eData);
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("player", pData);
        res.put("enemy", eData);
        return res;
    }

    private void fillBossData(Map<String, Object> eData) {
        WorldBoss boss = bossRepo.findById(1).orElseThrow();
        eData.put("name", boss.getName());
        eData.put("hp", boss.getHp());
        eData.put(KEY_MAX_HP, boss.getMaxHp());
        eData.put("sprite", boss.getSprite());
        eData.put(KEY_LEVEL, boss.getLevel());
    }
}
