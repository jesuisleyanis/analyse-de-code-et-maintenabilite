package com.epsi.rpg.controller;

import com.epsi.rpg.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:3000")
public class GameController {

    private static final String PLAYER_ID = "playerId";

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/hero/{id}")
    public Map<String, Object> getHero(@PathVariable String id) {
        return gameService.getHeroState(id);
    }

    @GetMapping("/players")
    public List<Map<String, Object>> getAllPlayers() {
        return gameService.getAllPlayers();
    }

    @PostMapping("/hero/setup")
    public Map<String, Object> setupHero(@RequestBody Map<String, String> body) {
        return gameService.setupHero(
                body.get(PLAYER_ID),
                body.get("name"),
                body.get("class"),
                body.get("mode"));
    }

    @GetMapping("/logs")
    public List<Map<String, Object>> getLogs() {
        return gameService.getLogs();
    }

    @PostMapping("/battle/attack")
    public Map<String, Object> attack(@RequestBody Map<String, String> body) {
        return gameService.attack(body.get(PLAYER_ID), body.get("targetId"));
    }

    @PostMapping("/battle/auto-tick")
    public Map<String, Object> autoTick(@RequestBody Map<String, String> body) {
        return gameService.autoTick(body.get(PLAYER_ID));
    }

    @PostMapping("/hero/heal")
    public Map<String, Object> heal(@RequestBody Map<String, String> body) {
        return gameService.heal(body.get(PLAYER_ID));
    }

    @PostMapping("/hero/upgrade-auto")
    public ResponseEntity<String> upgradeAuto(@RequestBody Map<String, String> body) {
        boolean success = gameService.upgradeAuto(body.get(PLAYER_ID));
        return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().body("Pas assez d'XP");
    }

    @PostMapping("/hero/use-item")
    public Map<String, Object> useItem(@RequestBody Map<String, Object> body) {
        return gameService.useItem(
                (String) body.get(PLAYER_ID),
                ((Number) body.get("itemIndex")).intValue());
    }

    @PostMapping("/hero/reset")
    public void reset() {
        gameService.reset();
    }
}
