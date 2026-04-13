package com.epsi.rpg.item;

import com.epsi.rpg.model.Player;
import org.springframework.stereotype.Component;

@Component
public class ShieldCommand implements ItemCommand {

    @Override
    public boolean matches(String item) {
        return item.contains("🛡️");
    }

    @Override
    public void execute(Player player) {
        player.setMaxHp(player.getMaxHp() + 50);
        player.setHp(player.getHp() + 50);
    }
}
