package com.epsi.rpg.combat;

import com.epsi.rpg.event.GameEvent;
import com.epsi.rpg.model.Player;
import com.epsi.rpg.model.SoloEnemy;
import com.epsi.rpg.repository.PlayerRepository;
import com.epsi.rpg.repository.SoloEnemyRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SoloCombatStrategy implements CombatStrategy {

    private final SoloEnemyRepository enemyRepo;
    private final PlayerRepository playerRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom random = new SecureRandom();

    public SoloCombatStrategy(SoloEnemyRepository enemyRepo, PlayerRepository playerRepo,
                              ApplicationEventPublisher eventPublisher) {
        this.enemyRepo = enemyRepo;
        this.playerRepo = playerRepo;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String getMode() { return "SOLO"; }

    @Override
    public void execute(Player attacker, int damage, boolean riposte, String targetId) {
        SoloEnemy enemy = enemyRepo.findById(attacker.getId())
                .orElseThrow(() -> new IllegalStateException("Ennemi introuvable pour " + attacker.getId()));

        enemy.setHp(enemy.getHp() - damage);

        if (enemy.getHp() <= 0) {
            int newLevel = enemy.getLevel() + 1;
            attacker.setXp(attacker.getXp() + 30 + attacker.getLevel() * 5);

            // Drop d'item (30% de chance)
            if (random.nextDouble() < 0.3) {
                String drop = random.nextBoolean() ? "🧪" : "🛡️";
                attacker.getInventory().add(drop);
                eventPublisher.publishEvent(new GameEvent(this, "Objet trouvé : " + drop));
            }

            playerRepo.save(attacker);
            spawnEnemy(attacker.getId(), newLevel);
        } else {
            enemyRepo.save(enemy);
            if (riposte) {
                int rDmg = 5 + enemy.getLevel() / 2;
                attacker.setHp(attacker.getHp() - rDmg);
                playerRepo.save(attacker);
            }
        }
    }

    private void spawnEnemy(String playerId, int level) {
        String name = (level % 10 == 0) ? "INSTANCE BOSS #" + (level / 10) : "Processus Malveillant";
        String sprite = (level % 10 == 0) ? "👹" : "👾";
        int hp = (int) (60 * Math.pow(1.15, level));

        SoloEnemy enemy = enemyRepo.findById(playerId).orElse(new SoloEnemy());
        enemy.setPlayerId(playerId);
        enemy.setName(name);
        enemy.setHp(hp);
        enemy.setMaxHp(hp);
        enemy.setLevel(level);
        enemy.setSprite(sprite);
        enemyRepo.save(enemy);
    }
}
