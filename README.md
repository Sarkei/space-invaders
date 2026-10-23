# Space Invaders

Klassisches Space-Invaders-Spiel in Java: Spieler, Aliens, Projektile und Drop-Items
(Ammo, Extras) stecken im Package `com.spaceinvaders.core`. Build über Maven, Start als
Container über Docker Compose.

**Technik:** `Java` · `Maven` · `Docker Compose`

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
