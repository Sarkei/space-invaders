# Spielassets – Space Invaders

Das Spiel zeichnet alle Objekte **programmatisch** (Java2D / HTML5 Canvas),
sodass hier keine externen Bilder zwingend benötigt werden.

## Optionale Sprites einbinden

Lege PNG-Dateien in diesem Ordner ab und benenne sie:

| Datei              | Beschreibung             |
|--------------------|--------------------------|
| `ship.png`         | Spielerschiff            |
| `alien1.png`       | Alien Typ 1 (Grunt)      |
| `alien2.png`       | Alien Typ 2 (Soldier)    |
| `alien3.png`       | Alien Typ 3 (Commander)  |
| `bullet.png`       | Geschoss                 |
| `ammodrop.png`     | Munitions-Pickup         |
| `background.png`   | Weltraum-Hintergrund     |

## Desktop (Swing) – Sprites laden

In `SpaceInvadersMain` nach dem `engine.initGame(...)` Aufruf:
```java
SpaceShip p = engine.getState().getPlayer();
p.bild = ImageIO.read(getClass().getResourceAsStream("/static/assets/ship.png"));
```

## Web-Version

Im Web-Modus werden keine Server-seitigen Bilder benötigt.
Canvas-Sprites können in `game.js` über `new Image()` geladen werden.
