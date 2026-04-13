package com.epsi.rpg.event;

import com.epsi.rpg.model.GameLog;
import com.epsi.rpg.repository.GameLogRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observer Pattern — listener qui persiste automatiquement
 * chaque GameEvent en base sous forme de log.
 */
@Component
public class GameEventListener {

    private final GameLogRepository logRepository;

    public GameEventListener(GameLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @EventListener
    public void onGameEvent(GameEvent event) {
        logRepository.save(new GameLog(event.getMessage()));
    }
}
