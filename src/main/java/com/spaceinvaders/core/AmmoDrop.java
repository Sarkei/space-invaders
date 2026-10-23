package com.spaceinvaders.core;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Munitions-Pickup, das von oben nach unten fällt.
 * Der Spieler sammelt es auf, indem er es berührt (+ammo).
 */
public class AmmoDrop extends SpaceObject {

    private static final int W = 22;
    private static final int H = 22;

    /** Anzahl Munition, die dieser Drop gibt */
    private final int amount;

    public AmmoDrop(int x, int y) {
        super(x, y);
        this.amount    = 8;
        this.speed     = 2;
        this.direction = 1;
    }

    @Override
    public void move() {
        y += speed;
    }

    @Override
    public void draw(Graphics2D g) {
        // Cyan-Diamant
        int cx = x + W / 2;
        int cy = y + H / 2;
        int[] px = {cx, cx + W / 2, cx, cx - W / 2};
        int[] py = {y,  cy,         y + H, cy};
        g.setColor(new Color(0, 220, 220));
        g.fillPolygon(px, py, 4);
        g.setColor(new Color(0, 255, 255));
        g.drawPolygon(px, py, 4);
        // „+" Symbol
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.drawString("+", cx - 3, cy + 4);
    }

    public int getAmount() { return amount; }

    @Override public int getWidth()  { return W; }
    @Override public int getHeight() { return H; }
}
