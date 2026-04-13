package com.epsi.rpg.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_logs")
public class GameLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String msg;

    @Column(nullable = false)
    private LocalDateTime ts;

    public GameLog() {}

    public GameLog(String msg) {
        this.msg = msg;
        this.ts = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getMsg() { return msg; }
    public LocalDateTime getTs() { return ts; }
}
