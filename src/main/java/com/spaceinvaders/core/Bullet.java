package com.spaceinvaders.core;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Ein Geschoss (UML: Bullet extends SpaceObject).
 *
 * direction = -1 → bewegt sich nach oben  (Spieler-Schuss)
 * direction = +1 → bewegt sich nach unten (Alien-Schuss)
 */
public class Bullet extends SpaceObject {

    private static final int DEFAULT_H = 15;

    private final boolean fromPlayer;
    private final boolean piercing;
    private final boolean explosive;
    private final int     bWidth;

    /** Vollständiger Konstruktor */
    public Bullet(int x, int y, boolean fromPlayer, boolean piercing, boolean explosive, int speed, int width) {
        super(x, y);
        this.fromPlayer = fromPlayer;
        this.piercing   = piercing;
        this.explosive  = explosive;
        this.bWidth     = width;
        this.speed      = speed;
        this.direction  = fromPlayer ? -1 : 1;
    }

    /** Rückwärts-kompatibler Konstruktor für Alien-Schüsse */
    public Bullet(int x, int y, boolean fromPlayer) {
        this(x, y, fromPlayer, false, false, fromPlayer ? 10 : 5, 5);
    }

    @Override
    public void move() {
        y += speed * direction;
    }

    @Override
    public void draw(Graphics2D g) {
        if (fromPlayer) {
            if (explosive) {
                // Orange-roter Explosionsschuss
                g.setColor(new Color(255, 100, 0));
                g.fillRect(x, y, bWidth, DEFAULT_H);
                g.setColor(new Color(255, 200, 0, 160));
                g.fillRect(x - 2, y + DEFAULT_H - 5, bWidth + 4, 5);
            } else if (piercing) {
                // Blau-weißer Panzerschuss
                g.setColor(new Color(150, 200, 255));
                g.fillRect(x, y, bWidth, DEFAULT_H);
                g.setColor(new Color(255, 255, 255, 150));
                g.fillRect(x + 1, y + 2, Math.max(1, bWidth - 2), DEFAULT_H - 6);
            } else {
                // Standard gelb
                g.setColor(new Color(255, 230, 50));
                g.fillRect(x, y, bWidth, DEFAULT_H);
                g.setColor(new Color(255, 180, 0, 150));
                g.fillRect(x - 1, y + DEFAULT_H - 4, bWidth + 2, 4);
            }
        } else {
            g.setColor(new Color(255, 60, 60));
            g.fillRect(x, y, bWidth, DEFAULT_H);
            g.setColor(new Color(255, 100, 100, 150));
            g.fillRect(x - 1, y, bWidth + 2, 4);
        }
    }

    public boolean isFromPlayer() { return fromPlayer; }
    public boolean isPiercing()   { return piercing; }
    public boolean isExplosive()  { return explosive; }

    @Override public int getWidth()  { return bWidth; }
    @Override public int getHeight() { return DEFAULT_H; }
}
