package com.spaceinvaders.core;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Ein feindlicher Alien (UML: Alien extends SpaceObject).
 *
 * Typen:
 *  1 = Grunt  (grün,  10 Punkte – unterste Reihe)
 *  2 = Soldier(blau,  20 Punkte – mittlere Reihen)
 *  3 = Commander (rot, 30 Punkte – oberste Reihe)
 */
public class Alien extends SpaceObject {

    private static final int W = 36;
    private static final int H = 28;

    private final int type;
    private final int points;
    private boolean alive;

    /** Animations-Frame (0 oder 1) – wird vom GameEngine gesetzt */
    public int animFrame = 0;

    /** Horizontale Geschwindigkeit in px/Tick (float für feine Typ-Unterschiede) */
    public float horizSpeed = 1f;

    /** Vertikale Drift-Geschwindigkeit (px/Tick) */
    public float vertSpeed = 0.04f;

    /** Subpixel-Akkumulatoren */
    public float horizAccum = 0f;
    public float vertAccum  = 0f;

    /** Horizontale Geschwindigkeit je Alien-Typ (1=Grunt langsam, 3=Commander schnell) */
    public static float horizMultForType(int type) {
        return switch (type) {
            case 3 -> 1.50f;  // Commander – flink und unberechenbar
            case 2 -> 1.00f;  // Soldier – Standard
            default -> 0.65f; // Grunt – träge
        };
    }

    /** Vertikaler Drift je Alien-Typ */
    public static float vertMultForType(int type) {
        return switch (type) {
            case 3 -> 1.40f;
            case 2 -> 1.00f;
            default -> 0.70f;
        };
    }

    public Alien(int x, int y, int type) {
        super(x, y);
        this.type   = type;
        this.alive  = true;
        this.speed  = 1;
        this.points = switch (type) {
            case 3 -> 30;
            case 2 -> 20;
            default -> 10;
        };
    }

    @Override
    public void draw(Graphics2D g) {
        if (!alive) return;
        if (bild != null) {
            g.drawImage(bild, x, y, W, H, null);
            return;
        }
        switch (type) {
            case 1 -> drawGrunt(g);
            case 2 -> drawSoldier(g);
            case 3 -> drawCommander(g);
        }
    }

    private void drawGrunt(Graphics2D g) {
        g.setColor(new Color(60, 220, 60));
        g.fillRect(x + 6, y + 6, 24, 15);
        g.fillRect(x + 3, y + 11, 5, 9);
        g.fillRect(x + 28, y + 11, 5, 9);
        // Beine
        int legOff = animFrame == 0 ? 0 : 2;
        g.fillRect(x + 6,  y + 21, 5, 5 + legOff);
        g.fillRect(x + 14, y + 21, 5, 5);
        g.fillRect(x + 22, y + 21, 5, 5 + legOff);
        // Augen
        g.setColor(Color.BLACK);
        g.fillOval(x + 9,  y + 8, 5, 5);
        g.fillOval(x + 22, y + 8, 5, 5);
        g.setColor(Color.WHITE);
        g.fillOval(x + 10, y + 9, 3, 3);
        g.fillOval(x + 23, y + 9, 3, 3);
    }

    private void drawSoldier(Graphics2D g) {
        g.setColor(new Color(50, 130, 255));
        g.fillRect(x + 4, y + 8, 28, 16);
        g.fillRect(x + 8, y + 3, 20, 7);
        // Scheren/Arme
        g.fillRect(x,     y + 14, 5, 8);
        g.fillRect(x + 31, y + 14, 5, 8);
        // Beine
        int legOff = animFrame == 0 ? 0 : 2;
        g.fillRect(x + 6,  y + 24, 4, 4 + legOff);
        g.fillRect(x + 14, y + 24, 4, 4);
        g.fillRect(x + 22, y + 24, 4, 4 + legOff);
        // Augen
        g.setColor(Color.BLACK);
        g.fillOval(x + 9,  y + 10, 5, 5);
        g.fillOval(x + 22, y + 10, 5, 5);
        g.setColor(new Color(180, 220, 255));
        g.fillOval(x + 10, y + 11, 3, 3);
        g.fillOval(x + 23, y + 11, 3, 3);
    }

    private void drawCommander(Graphics2D g) {
        g.setColor(new Color(220, 50, 50));
        g.fillRect(x + 3, y + 7, 30, 18);
        g.fillRect(x + 7, y + 2, 22, 7);
        // Tentakel
        int tOff = animFrame == 0 ? 0 : 3;
        g.fillRect(x,     y + 17, 4, 8 + tOff);
        g.fillRect(x + 32, y + 17, 4, 8 + tOff);
        g.fillRect(x + 7,  y + 23, 4, 5);
        g.fillRect(x + 14, y + 23, 4, 5 + tOff);
        g.fillRect(x + 21, y + 23, 4, 5);
        // Augen (orange leuchtend)
        g.setColor(new Color(255, 160, 0));
        g.fillOval(x + 9,  y + 9,  6, 6);
        g.fillOval(x + 21, y + 9,  6, 6);
        g.setColor(Color.WHITE);
        g.fillOval(x + 10, y + 10, 3, 3);
        g.fillOval(x + 22, y + 10, 3, 3);
    }

    public boolean isAlive()       { return alive; }
    public void setAlive(boolean v){ this.alive = v; }
    public int  getType()          { return type; }
    public int  getPoints()        { return points; }

    @Override public int getWidth()  { return W; }
    @Override public int getHeight() { return H; }
}
