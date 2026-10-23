package com.spaceinvaders.core;

/**
 * Pool mit 20 möglichen Spaceship-Upgrades.
 * colorRGB bestimmt die Kartenfarbe im Auswahl-Screen.
 */
public enum Upgrade {

    // ─── Angriff ───────────────────────────────────────────────────────────
    DOUBLE_SHOT ("Doppelschuss",     "Schießt 2 Geschosse gleichzeitig",           0xFF4422),
    TRIPLE_SHOT ("Dreifachschuss",   "Schießt 3 Geschosse gleichzeitig",           0xFF2200),
    FAST_FIRE   ("Schnellfeuer",     "Doppelte Schussrate",                        0xFF6600),
    FAST_BULLET ("Hyperprojektil",   "Geschosse fliegen schneller",                0xFFAA00),
    PIERCING    ("Panzerbrechend",   "Geschosse durchbohren mehrere Aliens",       0xFF0066),
    WIDER_BULLET("Schrotladung",     "Doppelt breite Geschosse",                   0xFF8833),
    EXPLOSION   ("Sprengkopf",       "Geschosse explodieren beim Aufprall",        0xFF3300),
    SUPER_SHOT  ("Megawaffe",        "+5 Spezialschuesse (Durchdringung+Explosion)",0xFFCC00),

    // ─── Verteidigung ──────────────────────────────────────────────────────
    SHIELD      ("Energieschild",    "Absorbiert den naechsten Treffer",           0x3399FF),
    GHOST_MODE  ("Phasenmodus",      "2s Unverwundbarkeit nach jedem Treffer",     0x6688FF),
    EXTRA_LIFE  ("Notfallkapsel",    "+1 Leben sofort",                            0x44CCFF),

    // ─── Utility ───────────────────────────────────────────────────────────
    FAST_SHIP   ("Turboschub",       "Raumschiff 40% schneller",                   0x00DDCC),
    SPEED_AURA  ("Warpfeld",         "Schiff und Geschosse dauerhaft schneller",   0x00BBAA),
    AUTO_FIRE   ("Autofeuer",        "Schiesst automatisch alle 2 Sekunden",       0x00CCAA),
    MAGNET      ("Anzugsfeld",       "Drops werden automatisch angezogen",         0x00AA88),
    AMMO_REGEN  ("Munitionsfabrik",  "+1 Munition alle 5 Sekunden",                0x00CCFF),
    EXTRA_DROPS ("Versorgungskanal", "Munitions-Drops erscheinen doppelt so oft",  0x00BBCC),
    SCORE_BOOST ("Multiplikator",    "Doppelte Punkte fuer jeden Kill",            0xFFD700),

    // ─── Sofort-Boni ───────────────────────────────────────────────────────
    AMMO_20     ("Munitionspaket",   "+20 Munition sofort",                        0x88DDAA),
    AMMO_30     ("Grosses Paket",    "+30 Munition sofort",                        0x88FFAA);

    public final String displayName;
    public final String description;
    public final int    colorRGB;

    Upgrade(String displayName, String description, int colorRGB) {
        this.displayName = displayName;
        this.description  = description;
        this.colorRGB     = colorRGB;
    }
}
