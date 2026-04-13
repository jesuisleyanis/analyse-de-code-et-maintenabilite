package com.epsi.rpg.combat;

import com.epsi.rpg.event.GameEvent;
import com.epsi.rpg.model.Player;
import com.epsi.rpg.repository.PlayerRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class PvpCombatStrategy implements CombatStrategy {

    private final PlayerRepository playerRepo;
    private final ApplicationEventPublisher eventPublisher;

    public PvpCombatStrategy(PlayerRepository playerRepo, ApplicationEventPublisher eventPublisher) {
        this.playerRepo = playerRepo;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String getMode() { return "PVP"; }

    @Override
    public void execute(Player attacker, int damage, boolean riposte, String targetId) {
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("Un identifiant de cible est requis en mode PVP");
        }

        Player target = playerRepo.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur cible introuvable : " + targetId));

        target.setHp(target.getHp() - damage);
        playerRepo.save(target);

        eventPublisher.publishEvent(new GameEvent(this,
                "[PVP] " + attacker.getName() + " attaque " + target.getName()));
    }
}
