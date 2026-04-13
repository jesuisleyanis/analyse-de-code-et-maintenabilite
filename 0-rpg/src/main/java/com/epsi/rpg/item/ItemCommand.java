package com.epsi.rpg.item;

import com.epsi.rpg.model.Player;

/**
 * Command Pattern — chaque type d'objet (potion, bouclier, etc.)
 * encapsule son effet dans une commande dédiée.
 */
public interface ItemCommand {

    boolean matches(String item);

    void execute(Player player);
}
