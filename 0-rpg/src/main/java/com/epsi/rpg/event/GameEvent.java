package com.epsi.rpg.event;

import org.springframework.context.ApplicationEvent;

/**
 * Observer Pattern — événement métier publié via le système d'événements Spring.
 * Les listeners (observers) réagissent à ces événements de manière découplée.
 */
public class GameEvent extends ApplicationEvent {

    private final String message;

    public GameEvent(Object source, String message) {
        super(source);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
