# Space Invaders

Space-Invaders-Variante in Java mit sauber getrennter Spiellogik: `GameEngine` verwaltet den
Zustand (`GameState`), Objekte wie `SpaceShip`, `Alien`, `Bullet` und `AmmoDrop` liegen im
Package `com.spaceinvaders.core`. Gespielt wird wahlweise über einen Swing-Client
(`SpaceInvadersMain`) oder einen Web-Renderer; Schwierigkeitsgrade und Upgrades sind
eingebaut. Build mit Maven, Start per Docker Compose.

**Technik:** `Java` · `Swing` · `Maven` · `Docker Compose`

## Inhalt

- `.gitattributes`
- `.gitignore`
- `Dockerfile`
- `docker-compose.yaml`
- `pom.xml`
- `src/`

_Insgesamt 25 Dateien._

## Sprachen

| Sprache | Dateien |
|---|---|
| Java | 16 |
| Markdown | 1 |
| JavaScript | 1 |
| HTML | 1 |

## Start

```bash
cp .env.example .env   # Werte eintragen
docker compose up -d
```

## Hinweise

- Zugangsdaten stehen **nicht** im Repo. Für lokale Werte `.env` anlegen (siehe `.env.example`).
- Importstand aus Google Drive; die Historie beginnt mit diesem Import.
