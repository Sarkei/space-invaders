package com.spaceinvaders.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.EnumSet;
import java.util.Set;

/**
 * Das Raumschiff des Spielers (UML: SpaceShip extends SpaceObject).
 */
public class SpaceShip extends SpaceObject {

    private static final int W = 50;
    private static final int H = 35;

    /** Aktuelle Munitionsmenge des Spielers */
    public int ammo;

    /** Aktive persistente Upgrades */
    public final Set<Upgrade> upgrades = EnumSet.noneOf(Upgrade.class);

    /** Anzahl der noch verbleibenden Schild-Ladungen */
    public int shieldHits = 0;

    /** Unverwundbarkeits-Ticks nach einem Treffer (GHOST_MODE) */
    public int ghostModeTicks = 0;

    /** Verbleibende Spezial-Durchdringungs-Schüsse (SUPER_SHOT) */
    public int superPierceShots = 0;

    public boolean hasUpgrade(Upgrade u) {
        return upgrades.contains(u);
    }

    public SpaceShip(int x, int y) {
        super(x, y);
        this.speed     = 5;
        this.direction = 0;
        this.ammo      = 20; // wird vom GameEngine nach Difficulty überschrieben
    }

    @Override
    public void move() {
        x += speed * direction;
        // Spielfeld-Grenzen einhalten
        if (x < 0)             x = 0;
        if (x + W > BOARD_WIDTH) x = BOARD_WIDTH - W;
    }

    @Override
    public void draw(Graphics2D g) {
        if (bild != null) {
            g.drawImage(bild, x, y, W, H, null);
            return;
        }
        // Geometrisches Schiff-Design
        g.setColor(new Color(0, 220, 100));
        // Rumpf
        g.fillRect(x + 15, y + 15, 20, 18);
        // Kanzel
        g.fillRect(x + 20, y + 5, 10, 13);
        // Flügel links
        int[] wxL = {x, x + 15, x + 15};
        int[] wyL = {y + 30, y + 18, y + 33};
        g.fillPolygon(wxL, wyL, 3);
        // Flügel rechts
        int[] wxR = {x + 35, x + 50, x + 35};
        int[] wyR = {y + 18, y + 30, y + 33};
        g.fillPolygon(wxR, wyR, 3);
        // Triebwerk-Glühen
        g.setColor(new Color(0, 140, 255, 200));
        g.fillRect(x + 10, y + 31, 8,  5);
        g.fillRect(x + 32, y + 31, 8,  5);
        // Cockpit-Glas
        g.setColor(new Color(150, 230, 255, 180));
        g.fillOval(x + 21, y + 6, 8, 7);
    }

    @Override public int getWidth()  { return W; }
    @Override public int getHeight() { return H; }
}
