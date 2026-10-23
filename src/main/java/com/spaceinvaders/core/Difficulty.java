package com.spaceinvaders.core;

/**
 * Schwierigkeitsgrade mit den zugehörigen Spielparametern.
 */
public enum Difficulty {

    EASY(5, 80, 1, 100, 160),
    MEDIUM(3, 50, 2, 70, 140),
    HARD(2, 30, 3, 40, 120);

    /** Startleben des Spielers */
    public final int lives;

    /** Startmunition des Spielers */
    public final int startAmmo;

    /** Bewegungsgeschwindigkeit der Aliens in px/Tick */
    public final int alienSpeed;

    /** Ticks zwischen Alien-Schüssen (kleiner = häufiger) */
    public final int alienShootInterval;

    /** Ticks zwischen Munitions-Drops (kleiner = häufiger) */
    public final int ammoDropInterval;

    Difficulty(int lives, int startAmmo, int alienSpeed,
               int alienShootInterval, int ammoDropInterval) {
        this.lives           = lives;
        this.startAmmo       = startAmmo;
        this.alienSpeed      = alienSpeed;
        this.alienShootInterval = alienShootInterval;
        this.ammoDropInterval   = ammoDropInterval;
    }
}
