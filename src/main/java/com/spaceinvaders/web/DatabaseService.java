package com.spaceinvaders.web;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Verwaltet die Bestenliste in einer H2-Datenbank (file-based, persistent).
 */
@Service
public class DatabaseService {

    @Autowired
    private JdbcTemplate jdbc;

    @PostConstruct
    public void init() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS leaderboard (
                id         INT AUTO_INCREMENT PRIMARY KEY,
                player_name VARCHAR(50) NOT NULL,
                score       INT         NOT NULL,
                difficulty  VARCHAR(10) NOT NULL,
                played_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
            )
            """);
    }

    public void saveScore(String playerName, int score, String difficulty) {
        jdbc.update(
            "INSERT INTO leaderboard (player_name, score, difficulty) VALUES (?, ?, ?)",
            playerName, score, difficulty
        );
    }

    public List<Map<String, Object>> getTopScores(int limit) {
        return jdbc.queryForList(
            "SELECT player_name, score, difficulty, played_at " +
            "FROM leaderboard ORDER BY score DESC LIMIT ?",
            limit
        );
    }
}
