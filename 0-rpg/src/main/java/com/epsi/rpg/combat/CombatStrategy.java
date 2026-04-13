package com.epsi.rpg.combat;

import com.epsi.rpg.model.Player;

/**
 * Strategy Pattern — chaque mode de combat (SOLO, COOP, PVP)
 * implémente sa propre logique via cette interface.
 */
public interface CombatStrategy {

    String getMode();

    void execute(Player attacker, int damage, boolean riposte, String targetId);
}
