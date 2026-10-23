package com.spaceinvaders.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spaceinvaders.core.Difficulty;
import com.spaceinvaders.core.GameEngine;
import com.spaceinvaders.core.GameState;
import com.spaceinvaders.core.Upgrade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Verwaltet WebSocket-Spielsitzungen.
 *
 * Protokoll Client → Server:
 *   {"type":"START",   "name":"Spieler1", "difficulty":"EASY"}
 *   {"type":"KEY",     "key":"ArrowLeft", "pressed":true}
 *   {"type":"PAUSE"}   / {"type":"RESUME"}
 *   {"type":"SELECT_UPGRADE", "upgrade":"DOUBLE_SHOT"}
 *
 * Protokoll Server → Client:
 *   {"type":"STATE",   "data":{ ... GameEngine.toJsonMap() ... }}
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final int TICK_MS = 50; // 20 Updates/Sek. – stabiler als 60/s über Netzwerk

    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private DatabaseService dbService;

    // -----------------------------------------------------------------------

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), new GameSession(session));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        GameSession gs = sessions.get(session.getId());
        if (gs == null) return;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = mapper.readValue(message.getPayload(), Map.class);
            String type = (String) msg.getOrDefault("type", "");

            switch (type) {
                case "START" -> {
                    String name    = (String) msg.getOrDefault("name", "Spieler");
                    String diffStr = ((String) msg.getOrDefault("difficulty", "MEDIUM")).toUpperCase();
                    Difficulty diff;
                    try { diff = Difficulty.valueOf(diffStr); }
                    catch (IllegalArgumentException ex) { diff = Difficulty.MEDIUM; }
                    gs.startGame(name, diff);
                }
                case "KEY" -> {
                    if (gs.engine.getState() != null && gs.engine.getState().isPaused()) return;
                    String  key     = (String)  msg.getOrDefault("key", "");
                    boolean pressed = Boolean.TRUE.equals(msg.get("pressed"));
                    gs.handleKey(key, pressed);
                }
                case "SHOOT" -> {
                    if (gs.engine.getState() == null || gs.engine.getState().isPaused()) return;
                    gs.engine.shoot();
                }
                case "PAUSE"  -> gs.pauseGame();
                case "RESUME" -> gs.resumeGame();
                case "RESTART" -> gs.stopGame();
                case "SELECT_UPGRADE" -> {
                    String upgName = (String) msg.get("upgrade");
                    if (upgName != null) {
                        try { gs.engine.selectUpgrade(Upgrade.valueOf(upgName)); }
                        catch (IllegalArgumentException ignored) {}
                    }
                }
            }
        } catch (Exception ignored) { /* ungültige Nachrichten ignorieren */ }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        GameSession gs = sessions.remove(session.getId());
        if (gs != null) gs.stopGame();
    }

    // -----------------------------------------------------------------------

    private class GameSession {

        final WebSocketSession wsSession;
        final GameEngine engine = new GameEngine();

        private ScheduledExecutorService executor;
        private ScheduledFuture<?>       gameLoop;

        volatile boolean fireHeld = false;
        volatile boolean sendBusy  = false;
        long lastSendMs = 0;

        GameSession(WebSocketSession wsSession) {
            this.wsSession = wsSession;
        }

        void startGame(String name, Difficulty difficulty) {
            if (gameLoop != null) gameLoop.cancel(false);
            fireHeld = false;
            engine.initGame(name, difficulty);

            if (executor == null || executor.isShutdown()) {
                executor = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "game-loop-" + wsSession.getId());
                    t.setDaemon(true);
                    return t;
                });
            }
            // fixedDelay: nächster Tick erst NACH Abschluss → keine Task-Stauung bei langsamer Verbindung
            gameLoop = executor.scheduleWithFixedDelay(this::tick, 0, TICK_MS, TimeUnit.MILLISECONDS);
        }

        void tick() {
            try {
                GameState st = engine.getState();
                if (st == null) return;

                if (!st.isPaused()) {
                    if (fireHeld) engine.shoot();
                    engine.update();
                    st = engine.getState();
                }

                if (st != null && st.isGameOver() && !st.isScoreSaved()) {
                    dbService.saveScore(st.getPlayerName(), st.getScore(), st.getDifficulty().name());
                    st.setScoreSaved(true);
                }

                long now = System.currentTimeMillis();
                long interval = st.isPaused() ? 400 : TICK_MS;
                if (now - lastSendMs < interval) return;
                if (sendBusy) return;

                sendState();
                lastSendMs = now;
            } catch (Exception ignored) { /* Netzwerkfehler tolerieren */ }
        }

        void sendState() {
            if (!wsSession.isOpen()) {
                if (gameLoop != null) gameLoop.cancel(false);
                return;
            }
            sendBusy = true;
            try {
                Map<String, Object> msg = new HashMap<>();
                msg.put("type", "STATE");
                msg.put("data", engine.toJsonMap());
                String json = mapper.writeValueAsString(msg);
                wsSession.sendMessage(new TextMessage(json));
            } catch (Exception ignored) {
            } finally {
                sendBusy = false;
            }
        }

        void pauseGame() {
            fireHeld = false;
            engine.pauseGame();
            lastSendMs = 0; // sofort PAUSED-Status senden
            sendState();
        }

        void resumeGame() {
            engine.resumeGame();
            lastSendMs = 0;
            sendState();
        }

        void handleKey(String key, boolean pressed) {
            switch (key) {
                case "ArrowLeft",  "a", "A"   -> engine.setMoveLeft(pressed);
                case "ArrowRight", "d", "D"   -> engine.setMoveRight(pressed);
                case " ", "ArrowUp", "w", "W" -> fireHeld = pressed;
            }
        }

        void stopGame() {
            if (gameLoop != null) gameLoop.cancel(false);
            if (executor != null) executor.shutdownNow();
        }
    }
}
