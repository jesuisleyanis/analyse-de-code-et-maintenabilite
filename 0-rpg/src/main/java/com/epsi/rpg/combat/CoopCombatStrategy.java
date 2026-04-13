package com.epsi.rpg.combat;

import com.epsi.rpg.event.GameEvent;
import com.epsi.rpg.model.Player;
import com.epsi.rpg.model.WorldBoss;
import com.epsi.rpg.repository.PlayerRepository;
import com.epsi.rpg.repository.WorldBossRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class CoopCombatStrategy implements CombatStrategy {

    private final WorldBossRepository bossRepo;
    private final PlayerRepository playerRepo;
    private final ApplicationEventPublisher eventPublisher;

    public CoopCombatStrategy(WorldBossRepository bossRepo, PlayerRepository playerRepo,
                              ApplicationEventPublisher eventPublisher) {
        this.bossRepo = bossRepo;
        this.playerRepo = playerRepo;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String getMode() { return "COOP"; }

    @Override
    public void execute(Player attacker, int damage, boolean riposte, String targetId) {
        WorldBoss boss = bossRepo.findById(1)
                .orElseThrow(() -> new IllegalStateException("World boss introuvable"));

        boss.setHp(boss.getHp() - damage);

        if (boss.getHp() <= 0) {
            int newLevel = boss.getLevel() + 1;
            int newHp = (int) (boss.getMaxHp() * 1.25);
            boss.setHp(newHp);
            boss.setMaxHp(newHp);
            boss.setLevel(newLevel);
            bossRepo.save(boss);

            attacker.setXp(attacker.getXp() + attacker.getLevel() * 20);
            playerRepo.save(attacker);

            eventPublisher.publishEvent(new GameEvent(this, "Boss mondial terrassé ! Difficulté augmentée."));
        } else {
            bossRepo.save(boss);
            if (riposte) {
                attacker.setHp(attacker.getHp() - 12);
                playerRepo.save(attacker);
            }
        }
    }
}
