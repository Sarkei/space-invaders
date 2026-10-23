package com.spaceinvaders.core;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Seltener Upgrade-Drop der vom Himmel faellt.
 * Enthält ein zufälliges Upgrade und kann vom Spieler aufgesammelt werden.
 */
public class UpgradePickup extends SpaceObject {

    private static final int W = 26;
    private static final int H = 26;

    private final Upgrade upgrade;

    public UpgradePickup(int x, int y, Upgrade upgrade) {
        super(x, y);
        this.upgrade   = upgrade;
        this.speed     = 2;
        this.direction = 1;
    }

    @Override
    public void move() {
        y += speed;
    }

    @Override
    public void draw(Graphics2D g) {
        int cx = x + W / 2;
        int cy = y + H / 2;
        int r  = W / 2 - 1;

        int[] xp = { cx,    cx + r, cx,    cx - r };
        int[] yp = { cy - r, cy,    cy + r, cy     };

        // Glüh-Aura
        g.setColor(new Color(255, 215, 0, 50));
        int[] xg = { cx, cx + r + 4, cx, cx - r - 4 };
        int[] yg = { cy - r - 4, cy, cy + r + 4, cy };
        g.fillPolygon(xg, yg, 4);

        // Haupt-Diamant
        g.setColor(new Color(255, 200, 0));
        g.fillPolygon(xp, yp, 4);

        // Glanzeffekt
        g.setColor(new Color(255, 245, 160, 180));
        g.fillPolygon(new int[]{ cx, cx + r / 2, cx }, new int[]{ cy - r, cy - r / 3, cy }, 3);

        // Rahmen
        g.setColor(new Color(220, 130, 0));
        g.setStroke(new BasicStroke(1.5f));
        g.drawPolygon(xp, yp, 4);

        // "UP"-Label
        g.setFont(new Font("Courier New", Font.BOLD, 8));
        g.setColor(Color.BLACK);
        g.drawString("UP", cx - 6, cy + 4);
    }

    public Upgrade getUpgrade() { return upgrade; }

    @Override public int getWidth()  { return W; }
    @Override public int getHeight() { return H; }
}
