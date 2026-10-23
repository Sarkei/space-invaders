# Space Invaders

Ein Space-Invaders-Variantenspiel in Java 21 mit strikt getrennter Spiellogik und zwei
Renderern: einem Swing-Client für den Desktop und einem Browser-Frontend, das denselben
`GameEngine`-Code über WebSockets fernsteuert. Die Spiellogik kennt kein AWT-Fenster —
sie liefert entweder `Graphics2D`-Zeichenaufrufe (Desktop) oder eine JSON-Struktur (Web).

## Spielprinzip & Features

- **Wellen:** Alien-Wellen werden zufällig über das obere Spielfeld verteilt (keine Reihen).
  Anzahl pro Welle: `min(10 + welle * 2, 28)`; ca. 35 % starten seitlich außerhalb des Bildes
  und fliegen herein. Jede Welle erhöht Horizontal- und Driftgeschwindigkeit.
- **Drei Alien-Typen:** Grunt (grün, 10 Punkte, langsam), Soldier (blau, 20 Punkte),
  Commander (rot, 30 Punkte, schnellster Typ). Punkte pro Kill: `Punkte × Welle × Multiplikator`.
- **Munition statt Dauerfeuer:** Der Spieler hat ein begrenztes Magazin und muss cyanfarbene
  Ammo-Drops (+8 Schuss, fallen mit Speed 2) einsammeln. Bei 0 Munition erscheint ein
  Warnbanner im Desktop-HUD.
- **Upgrades:** 20 Einträge im `Upgrade`-Enum (Angriff, Verteidigung, Utility, Sofortboni),
  z. B. Doppel-/Dreifachschuss, Panzerbrechend, Sprengkopf, Schild, Phasenmodus, Autofeuer,
  Magnet, Munitionsfabrik, Punkte-Multiplikator. Nach jeder geräumten Welle stoppt das Spiel
  und bietet **3 zufällige Upgrades** zur Auswahl; Wellenbonus `300 × Welle`.
- **Drops:** Goldene Upgrade-Pickups fallen alle 600–1400 Ticks (ca. 10–23 s) zufällig herab.
- **Schwierigkeitsgrade** (`Difficulty`):

  | Grad | Leben | Startmunition | Alien-Speed | Schussintervall | Ammo-Drop-Intervall |
  |---|---|---|---|---|---|
  | EASY | 5 | 80 | 1 | 100 Ticks | 160 Ticks |
  | MEDIUM | 3 | 50 | 2 | 70 Ticks | 140 Ticks |
  | HARD | 2 | 30 | 3 | 40 Ticks | 120 Ticks |

- **Gegnerfeuer:** Schussintervall sinkt mit der Welle (`max(20, Intervall − Welle×5)`);
  ab Welle 3 schießen gelegentlich zwei Aliens gleichzeitig.
- **Spielende:** Leben auf 0 oder ein Alien erreicht die Höhe des Spielers.

## Architektur

| Klasse | Rolle |
|---|---|
| `SpaceObject` | Abstrakte Basis: Position, `speed`, `direction`, `getBounds()`, `intersects()`, `draw(Graphics2D)`. Spielfeld 900×600. |
| `SpaceShip` | Spielerschiff (50×35), Magazin, `Set<Upgrade> upgrades`, Schildladungen, Unverwundbarkeits-Ticks, Superschüsse; bleibt in den Feldgrenzen. |
| `Alien` | Alien mit Typ, Punktrwert, Animationsframe und Subpixel-Akkumulatoren für horizontale/vertikale Drift. |
| `Bullet` | Geschoss mit Richtung (Spieler = nach oben), `piercing`- und `explosive`-Flag, variabler Breite. |
| `AmmoDrop` / `UpgradePickup` | Fallende Pickups; `UpgradePickup` trägt ein zufälliges `Upgrade`. |
| `Difficulty` / `Upgrade` | Enums mit allen Spielparametern bzw. dem Upgrade-Pool inkl. Anzeigename, Beschreibung, Farbe. |
| `GameState` | Datencontainer: Spieler, Listen (Aliens, Kugeln, Drops, Pickups), Leben, Score, Welle, Phase-Flags, Upgrade-Auswahl. |
| `GameEngine` | Komplette Spiellogik: Wellen-Spawning, `update()` pro Tick, Kollisionen, Upgrade-Anwendung, Schießen, `toJsonMap()` für den Web-Modus. |
| `SpaceInvadersMain` | Swing-Renderer + Eingabe: Screens START, PLAYING, UPGRADE_SELECT, GAME_OVER, HUD, Sternefeld. |
| `WebMain`, `WebSocketConfig`, `GameWebSocketHandler` | Spring-Boot-Web-Modus: Spielsession pro WebSocket unter `/ws/game`, 50 ms Tick. |
| `DatabaseService`, `LeaderboardController` | Bestenliste in H2, REST `GET/POST /api/leaderboard` (Top 10). |

Die Engine wird von beiden Renderern identisch genutzt; im Web-Modus wird `draw()` nie
aufgerufen, sondern `toJsonMap()` serialisiert den Zustand pro Tick.

## Technik

- **Java 21**, Maven, Spring Boot 3.5.15 (Parent), Artefakt `space-invaders:1.0.0-SNAPSHOT`.
- Abhängigkeiten: `spring-boot-starter-web`, `-websocket`, `-jdbc`, H2 (runtime).
- **Desktop:** Swing, 900×600, 60 FPS — ein `javax.swing.Timer` für Repaint, ein
  Daemon-Thread (`GameLogicThread`) für die Simulation; Fenster nicht skalierbar.
- **Web:** HTML5-Canvas-Frontend (`game.js`) zeichnet den Zustand aus `requestAnimationFrame`,
  Server tickt mit `TICK_MS = 50` (20 Updates/s, `scheduleWithFixedDelay` gegen Stau).
- **Bestenliste:** Desktop `data/scores.csv` (Format `name;score;difficulty;datum`),
  Web H2-Datei `jdbc:h2:file:${DATA_DIR:./data}/spaceinvaders`.
- **Docker:** zweistufiges Image (`eclipse-temurin:21-jdk-alpine` baut, `21-jre-alpine` läuft),
  `mvn dependency:go-offline` als Cache-Layer, Headless-AWT, `MaxRAMPercentage=75`.

## Projektstruktur

```
pom.xml                       Maven-Build (Spring-Boot-Parent, exec-Plugin)
Dockerfile                    Multi-Stage-Build → app.jar
docker-compose.yaml           Service, Port 22226:3000, Volume, Healthcheck
src/main/java/com/spaceinvaders/
  core/                       Spiellogik: GameEngine, GameState, SpaceShip, Alien,
                              Bullet, AmmoDrop, UpgradePickup, Upgrade, Difficulty, SpaceObject
  desktop/SpaceInvadersMain   Swing-Client (Renderer + Eingabe)
  web/                        WebMain, WebSocketConfig, GameWebSocketHandler,
                              LeaderboardController, DatabaseService
src/main/resources/
  application.properties      Port, H2-Datasource, Logging
  static/                     index.html, game.js (Canvas-Renderer)
```

## Start

**Desktop (Swing):**

```bash
mvn compile exec:java          # mainClass: com.spaceinvaders.desktop.SpaceInvadersMain
```

**Web (lokal):**

```bash
mvn spring-boot:run            # http://localhost:8080
# oder
mvn package -DskipTests && java -jar target/space-invaders-*.jar
```

**Docker Compose:**

```bash
docker compose up -d --build   # http://localhost:22226
```

Compose setzt `PORT=3000`, `DATA_DIR=/app/data` und mountet das Volume
`space_invaders_data` auf `/app/data` (Bestenliste bleibt erhalten).

**Steuerung:**

| Aktion | Desktop | Web |
|---|---|---|
| Links / Rechts | `←` / `A`, `→` / `D` | `←` / `A`, `→` / `D` |
| Schießen | `Leertaste`, `↑`, `W`, linke Maustaste | `Leertaste`, `↑`, `W` |
| Pause | – | `ESC` oder `P` (Fortsetzen: `Enter`/`Leertaste`) |
| Start | Name eintippen, `Enter` | Startformular |
| Schwierigkeit | `1` / `2` / `3` oder Klick | Auswahl im Startformular |
| Upgrade wählen | `1` / `2` / `3` oder Kartenklick | `1` / `2` / `3` oder Klick |
| Neustart / Beenden | `R` / `ESC` (Game-Over-Screen) | `Enter` (zurück zum Start) |

## Hinweise

- Die bestehende Repo-README verweist auf `cp .env.example .env`; eine solche Datei liegt
  nicht im Repo. Compose liest `.env` mit `required: false`, der Start funktioniert also ohne.
- Portangaben sind inkonsistent: Compose mappt `22226:3000`, das Dockerfile deklariert
  `EXPOSE 8080` und prüft den Healthcheck gegen 8080 — beim Start ohne Compose muss `PORT`
  passend gesetzt werden, sonst schlägt der Container-Healthcheck fehl.
- Das Netzwerk `cloudflare` ist in Compose als `external: true` deklariert und muss auf dem
  Host bereits existieren, sonst bricht `docker compose up` ab.
- Desktop- und Web-Bestenliste sind getrennt (CSV vs. H2); Punktestände werden nicht synchronisiert.
- Das Spiel speichert nur bei Game Over (Web: einmalig pro Session, `scoreSaved`-Flag).
