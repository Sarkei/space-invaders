package com.spaceinvaders.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Hält den gesamten Spielzustand.
 * Wird von GameEngine verwaltet und von beiden Renderers (Swing + Web) gelesen.
 */
public class GameState {

    private SpaceShip player;
    private List<Alien>         aliens         = new ArrayList<>();
    private List<Bullet>        bullets        = new ArrayList<>();
    private List<AmmoDrop>      ammoDrops      = new ArrayList<>();
    private List<UpgradePickup> upgradePickups = new ArrayList<>();

    /** true während der Upgrade-Auswahl zwischen zwei Wellen */
    private boolean awaitingUpgrade = false;

    /** Die drei zufällig gewählten Upgrade-Optionen */
    private List<Upgrade> upgradeChoices = new ArrayList<>();

    /** Bonus-Punkte für den Abschluss der letzten Welle (nur Anzeige) */
    private int lastWaveBonus = 0;

    private int    lives;
    private int    score;
    private int    wave;
    private String playerName = "Spieler";
    private Difficulty difficulty = Difficulty.MEDIUM;

    private boolean running   = false;
    private boolean paused    = false;
    private boolean gameOver  = false;
    private boolean scoreSaved = false;

    // --- Getters & Setters ---

    public SpaceShip getPlayer()                { return player; }
    public void setPlayer(SpaceShip p)          { this.player = p; }

    public List<Alien> getAliens()              { return aliens; }
    public void setAliens(List<Alien> a)        { this.aliens = a; }

    public List<Bullet> getBullets()            { return bullets; }
    public void setBullets(List<Bullet> b)      { this.bullets = b; }

    public List<AmmoDrop> getAmmoDrops()        { return ammoDrops; }
    public void setAmmoDrops(List<AmmoDrop> d)  { this.ammoDrops = d; }

    public int  getLives()                      { return lives; }
    public void setLives(int v)                 { this.lives = v; }

    public int  getScore()                      { return score; }
    public void setScore(int v)                 { this.score = v; }

    public int  getWave()                       { return wave; }
    public void setWave(int v)                  { this.wave = v; }

    public String getPlayerName()               { return playerName; }
    public void setPlayerName(String v)         { this.playerName = v; }

    public Difficulty getDifficulty()           { return difficulty; }
    public void setDifficulty(Difficulty d)     { this.difficulty = d; }

    public boolean isRunning()                  { return running; }
    public void setRunning(boolean v)           { this.running = v; }

    public boolean isPaused()                   { return paused; }
    public void setPaused(boolean v)            { this.paused = v; }

    public boolean isGameOver()                 { return gameOver; }
    public void setGameOver(boolean v)          { this.gameOver = v; }

    public boolean isScoreSaved()               { return scoreSaved; }
    public void setScoreSaved(boolean v)        { this.scoreSaved = v; }

    public List<UpgradePickup> getUpgradePickups()            { return upgradePickups; }
    public void setUpgradePickups(List<UpgradePickup> v)      { this.upgradePickups = v; }

    public boolean isAwaitingUpgrade()                        { return awaitingUpgrade; }
    public void setAwaitingUpgrade(boolean v)                 { this.awaitingUpgrade = v; }

    public List<Upgrade> getUpgradeChoices()                  { return Collections.unmodifiableList(upgradeChoices); }
    public void setUpgradeChoices(List<Upgrade> v)            { this.upgradeChoices = new ArrayList<>(v); }

    public int getLastWaveBonus()                             { return lastWaveBonus; }
    public void setLastWaveBonus(int v)                       { this.lastWaveBonus = v; }
}
