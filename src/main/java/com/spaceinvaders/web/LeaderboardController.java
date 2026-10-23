package com.spaceinvaders.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST-API für die Bestenliste.
 *
 * GET  /api/leaderboard          → Top-10-Einträge
 * POST /api/leaderboard          → Neuen Eintrag speichern
 */
@RestController
@RequestMapping("/api")
public class LeaderboardController {

    @Autowired
    private DatabaseService db;

    @GetMapping("/leaderboard")
    public List<Map<String, Object>> getLeaderboard() {
        return db.getTopScores(10);
    }

    @PostMapping("/leaderboard")
    public ResponseEntity<Void> saveScore(@RequestBody Map<String, Object> body) {
        String name       = (String)  body.getOrDefault("playerName", "Spieler");
        int    score      = ((Number) body.getOrDefault("score",      0)).intValue();
        String difficulty = (String)  body.getOrDefault("difficulty", "MEDIUM");
        db.saveScore(name, score, difficulty);
        return ResponseEntity.ok().build();
    }
}
