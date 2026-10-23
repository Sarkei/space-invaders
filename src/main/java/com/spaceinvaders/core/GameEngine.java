package com.spaceinvaders.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Enthält die gesamte Spiellogik.
 * Wird von SpaceInvadersMain (Desktop) und GameWebSocketHandler (Web) verwendet.
 */
public class GameEngine {

    public static final int BOARD_WIDTH  = SpaceObject.BOARD_WIDTH;
    public static final int BOARD_HEIGHT = SpaceObject.BOARD_HEIGHT;

    private static final int ALIEN_W        = 36;
    private static final int ALIEN_H        = 28;
    private static final int SHOOT_COOLDOWN = 15;

    private GameState state;
    private boolean   moveLeft;
    private boolean   moveRight;
    private int       shootCooldownTimer;
    private int       alienShootTimer;
    private int       ammoDropTimer;
    private int       upgradeDropTimer;
    private int       animTick;
    private int       ammoRegenTimer;
    private int       autoFireTimer;
    private final Random random = new Random();

    // -----------------------------------------------------------------------

    public void initGame(String playerName, Difficulty difficulty) {
        state = new GameState();
        state.setPlayerName(playerName == null || playerName.isBlank() ? "Spieler" : playerName);
        state.setDifficulty(difficulty);
        state.setLives(difficulty.lives);
        state.setScore(0);
        state.setWave(1);
        state.setGameOver(false);
        state.setRunning(true);
        state.setScoreSaved(false);
        state.setAwaitingUpgrade(false);
        state.setPaused(false);

        SpaceShip player = new SpaceShip(BOARD_WIDTH / 2 - 25, BOARD_HEIGHT - 70);
        player.ammo = difficulty.startAmmo;
        state.setPlayer(player);

        spawnAlienWave();

        alienShootTimer    = 0;
        ammoDropTimer      = 0;
        upgradeDropTimer   = random.nextInt(600) + 600;
        shootCooldownTimer = 0;
        ammoRegenTimer     = 0;
        autoFireTimer      = 0;
        animTick           = 0;
        moveLeft           = false;
        moveRight          = false;
    }

    // -----------------------------------------------------------------------
    // Wellen-Spawning (zufällig verteilt, keine Reihen/Haufen)
    // -----------------------------------------------------------------------

    private void spawnAlienWave() {
        List<Alien> aliens = new ArrayList<>();
        int wave  = state.getWave();
        int count = Math.min(10 + wave * 2, 28);

        final int minY     = 55;
        final int maxY     = BOARD_HEIGHT - 200;
        final int minDistX = 50;
        final int minDistY = 42;

        int placed = 0, attempts = 0;
        int maxAttempts = count * 60;

        while (placed < count && attempts < maxAttempts) {
            attempts++;

            int x, y;
            // ~35 % starten seitlich außerhalb, Rest irgendwo im oberen Spielfeld
            if (random.nextFloat() < 0.35f) {
                x = random.nextBoolean()
                        ? -ALIEN_W - random.nextInt(140)
                        : BOARD_WIDTH + random.nextInt(140);
                y = minY + random.nextInt(maxY - minY);
            } else {
                x = 10 + random.nextInt(BOARD_WIDTH - ALIEN_W - 20);
                y = minY + random.nextInt(maxY - minY);
            }

            boolean tooClose = false;
            for (Alien existing : aliens) {
                if (Math.abs(existing.getX() - x) < minDistX
                        && Math.abs(existing.getY() - y) < minDistY) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose) continue;

            int type      = 1 + random.nextInt(3);
            int direction = random.nextBoolean() ? 1 : -1;

            float baseHoriz = state.getDifficulty().alienSpeed + wave * 0.3f + random.nextFloat() * 0.6f;
            float baseVert  = 0.022f + wave * 0.007f + random.nextFloat() * 0.012f;
            float jitter    = 0.85f + random.nextFloat() * 0.30f;

            Alien a = new Alien(x, y, type);
            a.horizSpeed = Math.max(0.4f, baseHoriz * Alien.horizMultForType(type) * jitter);
            a.vertSpeed  = baseVert * Alien.vertMultForType(type) * jitter;
            a.speed      = Math.max(1, Math.round(a.horizSpeed));
            a.direction  = direction;
            aliens.add(a);
            placed++;
        }

        state.setAliens(aliens);
        state.setBullets(new ArrayList<>());
        state.setAmmoDrops(new ArrayList<>());
        state.setUpgradePickups(new ArrayList<>());
    }

    // -----------------------------------------------------------------------
    // Haupt-Update (60× pro Sekunde)
    // -----------------------------------------------------------------------

    public void update() {
        if (state == null || !state.isRunning() || state.isGameOver()) return;
        if (state.isPaused() || state.isAwaitingUpgrade()) return;

        animTick++;

        updatePlayer();
        updateAliens();
        updateBullets();
        updateAmmoDrops();
        updateUpgradePickups();
        checkCollisions();
        updateAlienShooting();
        updateAmmoDropSpawning();
        updateUpgradeDropSpawning();
        updatePassiveUpgrades();
        checkWinCondition();
    }

    private void updatePlayer() {
        SpaceShip p = state.getPlayer();
        if (moveLeft)       { p.direction = -1; p.move(); }
        else if (moveRight) { p.direction =  1; p.move(); }
        if (shootCooldownTimer > 0) shootCooldownTimer--;
        if (p.ghostModeTicks > 0)   p.ghostModeTicks--;
    }

    private void updateAliens() {
        List<Alien> aliens = state.getAliens();
        int frame = (animTick / 28) % 2;
        SpaceShip p = state.getPlayer();

        for (Alien a : aliens) {
            if (!a.isAlive()) continue;

            a.animFrame = frame;

            a.horizAccum += a.horizSpeed;
            while (a.horizAccum >= 1.0f) {
                a.setX(a.getX() + a.direction);
                a.horizAccum -= 1.0f;
            }

            a.vertAccum += a.vertSpeed;
            while (a.vertAccum >= 1.0f) {
                a.setY(a.getY() + 1);
                a.vertAccum -= 1.0f;
            }

            if (a.direction == 1  && a.getX() > BOARD_WIDTH + 55)  a.setX(-ALIEN_W);
            if (a.direction == -1 && a.getX() < -ALIEN_W - 55)     a.setX(BOARD_WIDTH + 10);

            if (a.getY() + ALIEN_H >= p.getY()) {
                state.setGameOver(true);
                state.setRunning(false);
                return;
            }
        }
    }

    private void updateBullets() {
        Iterator<Bullet> it = state.getBullets().iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.move();
            if (b.getY() < -20 || b.getY() > BOARD_HEIGHT + 20) it.remove();
        }
    }

    private void updateAmmoDrops() {
        SpaceShip p = state.getPlayer();
        boolean magnet = p.hasUpgrade(Upgrade.MAGNET);
        int pCX = p.getX() + p.getWidth() / 2;

        Iterator<AmmoDrop> it = state.getAmmoDrops().iterator();
        while (it.hasNext()) {
            AmmoDrop d = it.next();
            d.move();
            // Magnet zieht Drops horizontal an
            if (magnet) {
                int dCX = d.getX() + d.getWidth() / 2;
                if (Math.abs(pCX - dCX) < 220) {
                    d.setX(d.getX() + (int) Math.signum(pCX - dCX) * 3);
                }
            }
            if (d.getY() > BOARD_HEIGHT + 20) it.remove();
        }
    }

    private void updateUpgradePickups() {
        SpaceShip p = state.getPlayer();
        boolean magnet = p.hasUpgrade(Upgrade.MAGNET);
        int pCX = p.getX() + p.getWidth() / 2;

        Iterator<UpgradePickup> it = state.getUpgradePickups().iterator();
        while (it.hasNext()) {
            UpgradePickup up = it.next();
            up.move();
            if (magnet) {
                int upCX = up.getX() + up.getWidth() / 2;
                if (Math.abs(pCX - upCX) < 220) {
                    up.setX(up.getX() + (int) Math.signum(pCX - upCX) * 3);
                }
            }
            if (up.getY() > BOARD_HEIGHT + 20) it.remove();
        }
    }

    private void checkCollisions() {
        SpaceShip           player        = state.getPlayer();
        List<Alien>         aliens        = state.getAliens();
        List<Bullet>        bullets       = state.getBullets();
        List<AmmoDrop>      ammoDrops     = state.getAmmoDrops();
        List<UpgradePickup> upgPickups    = state.getUpgradePickups();
        int scoreMultiplier = player.hasUpgrade(Upgrade.SCORE_BOOST) ? 2 : 1;

        // Spieler-Geschosse treffen Aliens
        Iterator<Bullet> bIt = bullets.iterator();
        while (bIt.hasNext()) {
            Bullet b = bIt.next();
            if (!b.isFromPlayer()) continue;
            boolean hitAny = false;
            for (Alien a : aliens) {
                if (!a.isAlive()) continue;
                if (b.intersects(a)) {
                    a.setAlive(false);
                    state.setScore(state.getScore() + a.getPoints() * state.getWave() * scoreMultiplier);
                    // Explosionsschaden – Aliens im Radius 65px ebenfalls töten
                    if (b.isExplosive()) {
                        int cx = a.getX() + a.getWidth() / 2;
                        int cy = a.getY() + a.getHeight() / 2;
                        for (Alien nb : aliens) {
                            if (!nb.isAlive()) continue;
                            int dx = nb.getX() + nb.getWidth() / 2  - cx;
                            int dy = nb.getY() + nb.getHeight() / 2 - cy;
                            if (dx * dx + dy * dy < 65 * 65) {
                                nb.setAlive(false);
                                state.setScore(state.getScore() + nb.getPoints() * state.getWave() * scoreMultiplier);
                            }
                        }
                    }
                    hitAny = true;
                    if (!b.isPiercing()) { bIt.remove(); break; }
                    // Piercing: Schuss bleibt erhalten, greift weitere Aliens an
                }
            }
            // Supershot-Zähler dekrementieren
            if (hitAny && player.superPierceShots > 0) player.superPierceShots--;
        }

        // Alien-Geschosse treffen Spieler
        bIt = bullets.iterator();
        while (bIt.hasNext()) {
            Bullet b = bIt.next();
            if (b.isFromPlayer()) continue;
            if (b.intersects(player)) {
                bIt.remove();
                if (player.shieldHits > 0) {
                    player.shieldHits--;
                } else if (player.ghostModeTicks > 0) {
                    // Unverwundbar – ignorieren
                } else {
                    state.setLives(state.getLives() - 1);
                    if (player.hasUpgrade(Upgrade.GHOST_MODE)) {
                        player.ghostModeTicks = 120; // 2 Sekunden
                    }
                    if (state.getLives() <= 0) {
                        state.setGameOver(true);
                        state.setRunning(false);
                    }
                }
            }
        }

        // Spieler sammelt AmmoDrop auf
        Iterator<AmmoDrop> aIt = ammoDrops.iterator();
        while (aIt.hasNext()) {
            AmmoDrop drop = aIt.next();
            if (drop.intersects(player)) {
                player.ammo += drop.getAmount();
                aIt.remove();
            }
        }

        // Spieler sammelt UpgradePickup auf
        Iterator<UpgradePickup> uIt = upgPickups.iterator();
        while (uIt.hasNext()) {
            UpgradePickup up = uIt.next();
            if (up.intersects(player)) {
                applyUpgrade(up.getUpgrade());
                uIt.remove();
            }
        }
    }

    private void updateAlienShooting() {
        alienShootTimer++;
        // Schuss-Intervall verringert sich mit steigender Welle
        int interval = Math.max(20, state.getDifficulty().alienShootInterval - state.getWave() * 5);
        if (alienShootTimer < interval) return;
        alienShootTimer = 0;

        List<Alien> live = state.getAliens().stream()
                .filter(Alien::isAlive).collect(Collectors.toList());
        if (live.isEmpty()) return;

        // Ab Welle 3 schießen manchmal 2 Aliens gleichzeitig
        int numShooters = (state.getWave() >= 3) ? (random.nextBoolean() ? 2 : 1) : 1;
        numShooters = Math.min(numShooters, live.size());
        Collections.shuffle(live);
        for (int i = 0; i < numShooters; i++) {
            Alien shooter = live.get(i);
            state.getBullets().add(new Bullet(
                    shooter.getX() + shooter.getWidth() / 2 - 2,
                    shooter.getY() + shooter.getHeight(),
                    false));
        }
    }

    private void updateAmmoDropSpawning() {
        ammoDropTimer++;
        int interval = player().hasUpgrade(Upgrade.EXTRA_DROPS)
                ? state.getDifficulty().ammoDropInterval / 2
                : state.getDifficulty().ammoDropInterval;
        if (ammoDropTimer < interval) return;
        ammoDropTimer = 0;
        int ax = random.nextInt(BOARD_WIDTH - 30) + 5;
        state.getAmmoDrops().add(new AmmoDrop(ax, -25));
    }

    private void updateUpgradeDropSpawning() {
        upgradeDropTimer--;
        if (upgradeDropTimer > 0) return;
        // Nächster Drop in 600–1400 Ticks (10–23 Sekunden)
        upgradeDropTimer = random.nextInt(800) + 600;
        int ax      = random.nextInt(BOARD_WIDTH - 30) + 5;
        Upgrade[] all = Upgrade.values();
        Upgrade rndUpg = all[random.nextInt(all.length)];
        state.getUpgradePickups().add(new UpgradePickup(ax, -30, rndUpg));
    }

    private void updatePassiveUpgrades() {
        SpaceShip p = player();
        if (p == null) return;

        // AMMO_REGEN: +1 Munition alle 5 Sekunden (300 Ticks)
        if (p.hasUpgrade(Upgrade.AMMO_REGEN)) {
            ammoRegenTimer++;
            if (ammoRegenTimer >= 300) { ammoRegenTimer = 0; p.ammo++; }
        } else {
            ammoRegenTimer = 0;
        }

        // AUTO_FIRE: schießt alle 2 Sekunden (120 Ticks)
        if (p.hasUpgrade(Upgrade.AUTO_FIRE)) {
            autoFireTimer++;
            if (autoFireTimer >= 120) { autoFireTimer = 0; shoot(); }
        } else {
            autoFireTimer = 0;
        }
    }

    private void checkWinCondition() {
        boolean anyAlive = state.getAliens().stream().anyMatch(Alien::isAlive);
        if (!anyAlive) {
            int bonus = state.getWave() * 300;
            state.setScore(state.getScore() + bonus);
            state.setLastWaveBonus(bonus);
            // Upgrade-Auswahl vor nächster Welle
            state.setUpgradeChoices(pickRandomUpgrades(3));
            state.setAwaitingUpgrade(true);
        }
    }

    // -----------------------------------------------------------------------
    // Upgrade-Logik
    // -----------------------------------------------------------------------

    /**
     * Wählt count zufällige Upgrades aus dem gesamten Pool.
     * Bereits aktive persistente Upgrades werden bevorzugt ausgeschlossen.
     */
    private List<Upgrade> pickRandomUpgrades(int count) {
        List<Upgrade> all = new ArrayList<>(Arrays.asList(Upgrade.values()));
        Collections.shuffle(all, random);
        return all.subList(0, Math.min(count, all.size()));
    }

    /**
     * Wird vom Desktop-UI und WebSocket-Handler aufgerufen wenn der Spieler
     * ein Upgrade aus der Auswahl nach einer Welle wählt.
     */
    public void selectUpgrade(Upgrade u) {
        if (!state.isAwaitingUpgrade()) return;
        applyUpgrade(u);
        state.setAwaitingUpgrade(false);
        state.setWave(state.getWave() + 1);
        spawnAlienWave();
    }

    private void applyUpgrade(Upgrade u) {
        SpaceShip p = player();
        switch (u) {
            case EXTRA_LIFE  -> state.setLives(state.getLives() + 1);
            case AMMO_20     -> p.ammo += 20;
            case AMMO_30     -> p.ammo += 30;
            case SHIELD      -> p.shieldHits++;
            case SUPER_SHOT  -> p.superPierceShots += 5;
            case FAST_SHIP   -> { p.upgrades.add(u); p.speed = Math.min(p.speed + 2, 13); }
            case SPEED_AURA  -> { p.upgrades.add(u); p.speed = Math.min(p.speed + 2, 13); }
            default          -> p.upgrades.add(u);
        }
    }

    // -----------------------------------------------------------------------
    // Schießen
    // -----------------------------------------------------------------------

    public void shoot() {
        if (state == null || !state.isRunning() || state.isGameOver()) return;
        if (state.isAwaitingUpgrade()) return;
        if (shootCooldownTimer > 0) return;
        SpaceShip p = player();
        if (p.ammo <= 0) return;

        boolean superShot = p.superPierceShots > 0;
        boolean piercing  = superShot || p.hasUpgrade(Upgrade.PIERCING);
        boolean explosive = superShot || p.hasUpgrade(Upgrade.EXPLOSION);
        int     spd       = (p.hasUpgrade(Upgrade.FAST_BULLET) || p.hasUpgrade(Upgrade.SPEED_AURA)) ? 14 : 10;
        int     w         = p.hasUpgrade(Upgrade.WIDER_BULLET) ? 11 : 5;
        int     cooldown  = p.hasUpgrade(Upgrade.FAST_FIRE) ? 7 : SHOOT_COOLDOWN;

        if (p.hasUpgrade(Upgrade.TRIPLE_SHOT)) {
            addPlayerBullet(p.getX() + p.getWidth() / 2 - w / 2, p.getY() - 16, spd, w, piercing, explosive);
            addPlayerBullet(p.getX() + 4,                          p.getY() - 6,  spd, w, piercing, explosive);
            addPlayerBullet(p.getX() + p.getWidth() - 4 - w,       p.getY() - 6,  spd, w, piercing, explosive);
        } else if (p.hasUpgrade(Upgrade.DOUBLE_SHOT)) {
            addPlayerBullet(p.getX() + 8,                    p.getY() - 10, spd, w, piercing, explosive);
            addPlayerBullet(p.getX() + p.getWidth() - 8 - w, p.getY() - 10, spd, w, piercing, explosive);
        } else {
            addPlayerBullet(p.getX() + p.getWidth() / 2 - w / 2, p.getY() - 16, spd, w, piercing, explosive);
        }

        p.ammo--;
        shootCooldownTimer = cooldown;
    }

    private void addPlayerBullet(int x, int y, int spd, int w, boolean piercing, boolean explosive) {
        state.getBullets().add(new Bullet(x, y, true, piercing, explosive, spd, w));
    }

    // -----------------------------------------------------------------------
    // Eingabe-API
    // -----------------------------------------------------------------------

    public void setMoveLeft(boolean v)  { moveLeft  = v; }
    public void setMoveRight(boolean v) { moveRight = v; }

    /** Pausiert das laufende Spiel (Web: ESC-Taste). */
    public void pauseGame() {
        if (state == null || !state.isRunning() || state.isGameOver() || state.isAwaitingUpgrade()) return;
        state.setPaused(true);
        moveLeft  = false;
        moveRight = false;
    }

    /** Setzt ein pausiertes Spiel fort. */
    public void resumeGame() {
        if (state == null || state.isGameOver()) return;
        state.setPaused(false);
    }

    public GameState getState() { return state; }

    private SpaceShip player() {
        return state == null ? null : state.getPlayer();
    }

    // -----------------------------------------------------------------------
    // JSON-Serialisierung für den Web-Modus
    // -----------------------------------------------------------------------

    public Map<String, Object> toJsonMap() {
        if (state == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("phase", "WAITING");
            return empty;
        }

        Map<String, Object> map = new HashMap<>();
        SpaceShip p = state.getPlayer();

        Map<String, Object> playerMap = new HashMap<>();
        playerMap.put("x",    p.getX());
        playerMap.put("y",    p.getY());
        playerMap.put("w",    p.getWidth());
        playerMap.put("h",    p.getHeight());
        playerMap.put("ammo", p.ammo);
        playerMap.put("shieldHits", p.shieldHits);
        playerMap.put("ghostTicks", p.ghostModeTicks);
        map.put("player", playerMap);

        List<Map<String, Object>> alienList = state.getAliens().stream()
                .filter(Alien::isAlive)
                .map(a -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("x",  a.getX()); m.put("y", a.getY());
                    m.put("w",  a.getWidth()); m.put("h", a.getHeight());
                    m.put("type", a.getType()); m.put("af", a.animFrame);
                    return m;
                }).collect(Collectors.toList());
        map.put("aliens", alienList);

        List<Map<String, Object>> bulletList = state.getBullets().stream()
                .map(b -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("x", b.getX()); m.put("y", b.getY());
                    m.put("w", b.getWidth()); m.put("h", b.getHeight());
                    m.put("fp", b.isFromPlayer());
                    return m;
                }).collect(Collectors.toList());
        map.put("bullets", bulletList);

        List<Map<String, Object>> dropList = state.getAmmoDrops().stream()
                .map(d -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("x", d.getX()); m.put("y", d.getY());
                    m.put("w", d.getWidth()); m.put("h", d.getHeight());
                    m.put("amt", d.getAmount());
                    return m;
                }).collect(Collectors.toList());
        map.put("ammoDrops", dropList);

        List<Map<String, Object>> upgDropList = state.getUpgradePickups().stream()
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("x", u.getX()); m.put("y", u.getY());
                    m.put("w", u.getWidth()); m.put("h", u.getHeight());
                    m.put("name", u.getUpgrade().displayName);
                    return m;
                }).collect(Collectors.toList());
        map.put("upgradePickups", upgDropList);

        map.put("lives",           state.getLives());
        map.put("score",           state.getScore());
        map.put("wave",            state.getWave());
        map.put("ammo",            p.ammo);
        map.put("playerName",      state.getPlayerName());
        map.put("difficulty",      state.getDifficulty().name());
        map.put("gameOver",        state.isGameOver());
        map.put("awaitingUpgrade", state.isAwaitingUpgrade());
        map.put("lastWaveBonus",   state.getLastWaveBonus());

        if (state.isAwaitingUpgrade()) {
            List<Map<String, Object>> choices = state.getUpgradeChoices().stream()
                    .map(u -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("key",   u.name());
                        m.put("name",  u.displayName);
                        m.put("desc",  u.description);
                        m.put("color", String.format("#%06X", u.colorRGB));
                        return m;
                    }).collect(Collectors.toList());
            map.put("upgradeChoices", choices);
        }

        map.put("phase", state.isGameOver() ? "GAME_OVER"
                : state.isAwaitingUpgrade() ? "UPGRADE_SELECT"
                : state.isPaused() ? "PAUSED"
                : state.isRunning() ? "PLAYING" : "WAITING");
        map.put("paused", state.isPaused());

        return map;
    }
}
