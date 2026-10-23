package com.spaceinvaders.core;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;

/**
 * Abstrakte Basisklasse aller Spielobjekte (UML: SpaceObject).
 *
 * Hinweis zum UML: draw() erhält Graphics2D als Parameter, da Swing
 * eine Zeichenfläche benötigt. Im Web-Modus wird draw() nie aufgerufen —
 * stattdessen serialisiert GameEngine.toJsonMap() den Zustand für den Browser.
 */
public abstract class SpaceObject {

    public static final int BOARD_WIDTH  = 900;
    public static final int BOARD_HEIGHT = 600;

    protected int x;
    protected int y;

    public int speed;
    public int direction; // -1 = links/oben, +1 = rechts/unten

    /**
     * Sprites (wird im Desktop-Modus gesetzt; im Web-Modus null).
     * transient: wird bei JSON-Serialisierung ignoriert.
     */
    public transient Image bild;

    public SpaceObject(int x, int y) {
        this.x         = x;
        this.y         = y;
        this.speed     = 1;
        this.direction = 1;
    }

    /**
     * Zeichnet das Objekt auf die übergebene Zeichenfläche.
     * Wird ausschließlich im Desktop-Modus (Swing) aufgerufen.
     */
    public abstract void draw(Graphics2D g);

    /**
     * Bewegt das Objekt gemäß speed und direction.
     * Subklassen können überschreiben für spezielle Bewegungsmuster.
     */
    public void move() {
        x += speed * direction;
    }

    /** Kollisionsrechteck für dieses Objekt. */
    public Rectangle getBounds() {
        return new Rectangle(x, y, getWidth(), getHeight());
    }

    /** Prüft Kollision mit einem anderen Spielobjekt. */
    public boolean intersects(SpaceObject other) {
        return getBounds().intersects(other.getBounds());
    }

    public abstract int getWidth();
    public abstract int getHeight();

    public int getX() { return x; }
    public int getY() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
}
