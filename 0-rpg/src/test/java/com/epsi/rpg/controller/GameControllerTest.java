package com.epsi.rpg.controller;

import com.epsi.rpg.model.Player;
import com.epsi.rpg.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class GameControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate db;

    @Autowired
    private PlayerRepository playerRepo;

    private MockMvc mvc;

    private static final String PID  = "test-player-1";
    private static final String PID2 = "test-player-2";

    @BeforeEach
    void setup() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(post("/api/v1/hero/reset"));
    }

    // --- helpers -------------------------------------------------------------

    private void initPlayer(String id) throws Exception {
        mvc.perform(get("/api/v1/hero/" + id));
    }

    private String json(Object... kvPairs) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kvPairs.length; i += 2) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(kvPairs[i]).append("\":");
            Object v = kvPairs[i + 1];
            if (v instanceof Number) {
                sb.append(v);
            } else {
                sb.append("\"").append(v).append("\"");
            }
        }
        return sb.append("}").toString();
    }

    private void setPlayerInventory(String playerId, List<String> items) {
        Player p = playerRepo.findById(playerId).orElseThrow();
        p.setInventory(items);
        playerRepo.saveAndFlush(p);
    }

    // Tests for hero retrieval endpoint

    @Test
    void getHero_newPlayer_createsWithDefaults() throws Exception {
        mvc.perform(get("/api/v1/hero/" + PID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.name").value("Invité"))
                .andExpect(jsonPath("$.player.level").value(1))
                .andExpect(jsonPath("$.player.hp").value(100))
                .andExpect(jsonPath("$.player.maxHp").value(100))
                .andExpect(jsonPath("$.enemy").exists());
    }

    @Test
    void getHero_existingPlayer_returnsCurrentState() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET name = 'Héros' WHERE id = ?", PID);

        mvc.perform(get("/api/v1/hero/" + PID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.name").value("Héros"));
    }

    // Tests for players listing endpoint

    @Test
    void getAllPlayers_returnsAllRegisteredPlayers() throws Exception {
        initPlayer(PID);
        initPlayer(PID2);

        mvc.perform(get("/api/v1/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    // Tests for hero setup endpoint

    @Test
    void setupHero_withMode_updatesNameClassAndMode() throws Exception {
        initPlayer(PID);

        mvc.perform(post("/api/v1/hero/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID, "name", "Aragorn", "class", "Guerrier", "mode", "COOP")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.name").value("Aragorn"))
                .andExpect(jsonPath("$.player.mode").value("COOP"));
    }

    @Test
    void setupHero_withBlankMode_updatesOnlyNameAndClass() throws Exception {
        initPlayer(PID);

        mvc.perform(post("/api/v1/hero/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID, "name", "Legolas", "class", "Archer", "mode", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.name").value("Legolas"));
    }

    @Test
    void setupHero_withNullMode_updatesOnlyNameAndClass() throws Exception {
        initPlayer(PID);

        mvc.perform(post("/api/v1/hero/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":\"" + PID + "\",\"name\":\"Gimli\",\"class\":\"Nain\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.name").value("Gimli"));
    }

    // Tests for logs endpoint

    @Test
    void getLogs_returnsLogList() throws Exception {
        initPlayer(PID);

        mvc.perform(get("/api/v1/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // Tests for solo attack endpoint

    @Test
    void attack_soloMode_enemyAlive_playerReceivesCounterDamage() throws Exception {
        initPlayer(PID);
        db.update("UPDATE solo_enemies SET hp = 9999 WHERE player_id = ?", PID);

        mvc.perform(post("/api/v1/battle/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.hp").value(lessThan(100)));
    }

    @Test
    void attack_soloMode_enemyKilled_spawnsNextLevel() throws Exception {
        initPlayer(PID);
        db.update("UPDATE solo_enemies SET hp = 1, level = 1 WHERE player_id = ?", PID);

        mvc.perform(post("/api/v1/battle/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enemy.level").value(2));
    }

    @Test
    void attack_soloMode_level10Multiple_spawnsBossEnemy() throws Exception {
        initPlayer(PID);
        db.update("UPDATE solo_enemies SET hp = 1, level = 9 WHERE player_id = ?", PID);

        mvc.perform(post("/api/v1/battle/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enemy.name").value(containsString("INSTANCE BOSS")));
    }

    @Test
    void attack_deadPlayer_returnsStateWithoutCombat() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET hp = 0 WHERE id = ?", PID);

        int enemyHpBefore = db.queryForObject(
                "SELECT hp FROM solo_enemies WHERE player_id = ?", Integer.class, PID);

        mvc.perform(post("/api/v1/battle/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enemy.hp").value(enemyHpBefore));
    }

    // Tests for coop attack endpoint

    @Test
    void attack_coopMode_bossAlive_reducesBossHpAndCounters() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET mode = 'COOP' WHERE id = ?", PID);
        db.update("UPDATE world_boss SET hp = 2000 WHERE id = 1");

        mvc.perform(post("/api/v1/battle/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enemy.hp").value(lessThan(2000)));
    }

    @Test
    void attack_coopMode_bossKilled_respawnsStronger() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET mode = 'COOP' WHERE id = ?", PID);
        db.update("UPDATE world_boss SET hp = 1, max_hp = 2000, level = 10 WHERE id = 1");

        mvc.perform(post("/api/v1/battle/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enemy.hp").value(greaterThanOrEqualTo(2000)));
    }

    // Tests for PVP attack endpoint

    @Test
    void attack_pvpMode_reducesTargetHp() throws Exception {
        initPlayer(PID);
        initPlayer(PID2);
        db.update("UPDATE players SET mode = 'PVP' WHERE id = ?", PID);

        int hpBefore = db.queryForObject(
                "SELECT hp FROM players WHERE id = ?", Integer.class, PID2);

        mvc.perform(post("/api/v1/battle/attack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID, "targetId", PID2)))
                .andExpect(status().isOk());

        int hpAfter = db.queryForObject(
                "SELECT hp FROM players WHERE id = ?", Integer.class, PID2);
        assertTrue(hpAfter < hpBefore);
    }

    // Tests for auto-tick endpoint

    @Test
    void autoTick_autoLvlZero_returnsStateImmediately() throws Exception {
        initPlayer(PID);

        mvc.perform(post("/api/v1/battle/auto-tick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player").exists());
    }

    @Test
    void autoTick_autoLvlPositive_triggersCombat() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET auto_lvl = 2 WHERE id = ?", PID);
        db.update("UPDATE solo_enemies SET hp = 9999 WHERE player_id = ?", PID);

        mvc.perform(post("/api/v1/battle/auto-tick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enemy.hp").value(lessThan(9999)));
    }

    // Tests for heal endpoint

    @Test
    void heal_restoresPlayerToMaxHp() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET hp = 10 WHERE id = ?", PID);

        mvc.perform(post("/api/v1/hero/heal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.hp").value(100));
    }

    // Tests for upgrade auto-attack endpoint

    @Test
    void upgradeAuto_enoughXp_incrementsAutoLvl() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET xp = 9999 WHERE id = ?", PID);

        mvc.perform(post("/api/v1/hero/upgrade-auto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID)))
                .andExpect(status().isOk());

        int autoLvl = db.queryForObject(
                "SELECT auto_lvl FROM players WHERE id = ?", Integer.class, PID);
        assertEquals(1, autoLvl);
    }

    @Test
    void upgradeAuto_notEnoughXp_returnsBadRequest() throws Exception {
        initPlayer(PID);

        mvc.perform(post("/api/v1/hero/upgrade-auto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("playerId", PID)))
                .andExpect(status().isBadRequest());
    }

    // Tests for use-item endpoint

    @Test
    void useItem_potion_healsToMaxHp() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET hp = 10 WHERE id = ?", PID);
        setPlayerInventory(PID, List.of("🧪"));

        mvc.perform(post("/api/v1/hero/use-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":\"" + PID + "\",\"itemIndex\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.hp").value(100));
    }

    @Test
    void useItem_shield_increasesMaxHp() throws Exception {
        initPlayer(PID);
        setPlayerInventory(PID, List.of("🛡️"));

        mvc.perform(post("/api/v1/hero/use-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":\"" + PID + "\",\"itemIndex\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.maxHp").value(150));
    }

    @Test
    void useItem_invalidIndex_returnsStateUnchanged() throws Exception {
        initPlayer(PID);

        mvc.perform(post("/api/v1/hero/use-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":\"" + PID + "\",\"itemIndex\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player").exists());
    }

    // Tests for XP and level-up logic

    @Test
    void getHero_xpSufficient_triggersLevelUp() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET xp = 200 WHERE id = ?", PID);

        mvc.perform(get("/api/v1/hero/" + PID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.level").value(greaterThan(1)));
    }

    @Test
    void getHero_multipleLevelUps_handledByLoop() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET xp = 9999 WHERE id = ?", PID);

        mvc.perform(get("/api/v1/hero/" + PID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.level").value(greaterThan(2)));
    }

    @Test
    void getHero_coopMode_returnsWorldBossAsEnemy() throws Exception {
        initPlayer(PID);
        db.update("UPDATE players SET mode = 'COOP' WHERE id = ?", PID);

        mvc.perform(get("/api/v1/hero/" + PID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enemy.name").value("LE MONOLITHE EPSI"));
    }

    @Test
    void getHero_missingEnemy_fallsBackToWorldBoss() throws Exception {
        initPlayer(PID);
        db.update("DELETE FROM solo_enemies WHERE player_id = ?", PID);

        mvc.perform(get("/api/v1/hero/" + PID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enemy.name").value("LE MONOLITHE EPSI"));
    }

    // Tests for reset endpoint

    @Test
    void reset_dropsAndRecreatesAllTables() throws Exception {
        initPlayer(PID);

        mvc.perform(post("/api/v1/hero/reset"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/hero/" + PID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.name").value("Invité"));
    }
}
