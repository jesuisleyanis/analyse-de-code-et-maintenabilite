package com.epsi.rpg.config;

import com.epsi.rpg.event.GameEvent;
import com.epsi.rpg.model.WorldBoss;
import com.epsi.rpg.repository.WorldBossRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final WorldBossRepository worldBossRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DatabaseInitializer(WorldBossRepository worldBossRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.worldBossRepository = worldBossRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void run(String... args) {
        if (worldBossRepository.count() == 0) {
            worldBossRepository.save(new WorldBoss(1, "LE MONOLITHE EPSI", 2000, 2000, 10, "👹"));
        }
        eventPublisher.publishEvent(new GameEvent(this, "Système RPG initialisé (XP Exponentielle activée)."));
    }
}
