package com.spaceinvaders.desktop;

import com.spaceinvaders.core.Difficulty;
import com.spaceinvaders.core.GameEngine;
import com.spaceinvaders.core.GameState;
import com.spaceinvaders.core.SpaceObject;
import com.spaceinvaders.core.Upgrade;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Haupt-Spielpanel für den Desktop-Modus.
 */
public class SpaceInvadersMain extends JPanel
        implements KeyListener, ActionListener, Runnable {

    private static final int WIDTH  = SpaceObject.BOARD_WIDTH;
    private static final int HEIGHT = SpaceObject.BOARD_HEIGHT;
    private static final int FPS    = 60;

    public JFrame fenster;
    private Timer  timer;
    private Thread gamethread;

    private final GameEngine engine = new GameEngine();
    private final int[][] stars = new int[200][3];

    private enum Screen { START, PLAYING, UPGRADE_SELECT, GAME_OVER }
    private Screen screen = Screen.START;

    // Startmenü
    private String     nameInput       = "";
    private Difficulty selDifficulty   = Difficulty.MEDIUM;
    private boolean    nameFieldActive = false;
    private int        cursorBlink     = 0;
    private int        mouseX = -1, mouseY = -1;
    private boolean    spaceHeld = false;

    // Klick-Bereiche Start
    private final Rectangle rcNameField = new Rectangle(250, 230, 400, 35);
    private final Rectangle rcEasy      = new Rectangle(250, 325, 120, 35);
    private final Rectangle rcMedium    = new Rectangle(400, 325, 120, 35);
    private final Rectangle rcHard      = new Rectangle(550, 325, 120, 35);
    private final Rectangle rcStart     = new Rectangle(340, 420, 220, 40);

    // Klick-Bereiche Upgrade-Auswahl (3 Karten nebeneinander)
    private final Rectangle[] rcUpgrade = {
        new Rectangle(75,  210, 225, 205),
        new Rectangle(338, 210, 225, 205),
        new Rectangle(600, 210, 225, 205)
    };

    // Klick-Bereiche Game Over
    private final Rectangle rcNewGame = new Rectangle(260, 490, 170, 40);
    private final Rectangle rcQuit    = new Rectangle(470, 490, 170, 40);

    // Bestenliste (lokal, CSV)
    private static final String SCORES_FILE = "data/scores.csv";
    private List<String[]> leaderboard = new ArrayList<>();
    private boolean scoreSavedThisRound = false;

    // Letzter Spielzustand für die Game-Over-Anzeige
    private String  lastPlayerName = "";
    private int     lastScore      = 0;
    private String  lastDifficulty = "";
    private int     lastWave       = 1;
    private boolean lastWon        = false;

    // Fonts
    private static final Font HUD_FONT    = new Font("Courier New", Font.BOLD, 14);
    private static final Font TITLE_FONT  = new Font("Courier New", Font.BOLD, 32);
    private static final Font MEDIUM_FONT = new Font("Courier New", Font.BOLD, 18);
    private static final Font SMALL_FONT  = new Font("Courier New", Font.PLAIN, 13);
    private static final Font TINY_FONT   = new Font("Courier New", Font.PLAIN, 11);

    // -----------------------------------------------------------------------

    public SpaceInvadersMain() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ev -> {
            if (ev.getID() == KeyEvent.KEY_PRESSED)  keyPressed(ev);
            if (ev.getID() == KeyEvent.KEY_RELEASED) keyReleased(ev);
            if (ev.getID() == KeyEvent.KEY_TYPED)    keyTyped(ev);
            return false;
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e)  { handleMouseClick(e.getX(), e.getY()); }
            @Override public void mousePressed(MouseEvent e)  { if (e.getButton() == MouseEvent.BUTTON1 && screen == Screen.PLAYING) spaceHeld = true; }
            @Override public void mouseReleased(MouseEvent e) { if (e.getButton() == MouseEvent.BUTTON1) spaceHeld = false; }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e)   { mouseX = e.getX(); mouseY = e.getY(); updateCursor(); }
            @Override public void mouseDragged(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
        });

        Random rng = new Random();
        for (int i = 0; i < stars.length; i++) {
            stars[i][0] = rng.nextInt(WIDTH);
            stars[i][1] = rng.nextInt(HEIGHT);
            stars[i][2] = rng.nextInt(200) + 55;
        }

        loadLeaderboard();

        timer = new Timer(1000 / FPS, this);
        timer.start();
        gamethread = new Thread(this, "GameLogicThread");
        gamethread.setDaemon(true);
        gamethread.start();
    }

    // -----------------------------------------------------------------------
    // Bestenliste (lokal, CSV)
    // -----------------------------------------------------------------------

    private void loadLeaderboard() {
        leaderboard.clear();
        File f = new File(SCORES_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(";", -1);
                if (p.length >= 3) leaderboard.add(p);
            }
            leaderboard.sort((a, b) -> Integer.compare(safeInt(b[1]), safeInt(a[1])));
        } catch (IOException ignored) {}
    }

    private void saveScore(String name, int score, String difficulty) {
        new File("data").mkdirs();
        try (PrintWriter pw = new PrintWriter(new FileWriter(SCORES_FILE, true))) {
            pw.println(name + ";" + score + ";" + difficulty + ";" + LocalDate.now());
        } catch (IOException ignored) {}
        loadLeaderboard();
    }

    private static int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    // -----------------------------------------------------------------------
    // Cursor & Klick-Logik
    // -----------------------------------------------------------------------

    private void updateCursor() {
        if (screen == Screen.START) {
            boolean hand = rcNameField.contains(mouseX, mouseY)
                    || rcEasy.contains(mouseX, mouseY) || rcMedium.contains(mouseX, mouseY)
                    || rcHard.contains(mouseX, mouseY)
                    || (rcStart.contains(mouseX, mouseY) && !nameInput.isBlank());
            setCursor(hand ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        } else if (screen == Screen.UPGRADE_SELECT) {
            boolean hand = false;
            for (Rectangle r : rcUpgrade) hand |= r.contains(mouseX, mouseY);
            setCursor(hand ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        } else if (screen == Screen.GAME_OVER) {
            boolean hand = rcNewGame.contains(mouseX, mouseY) || rcQuit.contains(mouseX, mouseY);
            setCursor(hand ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void handleMouseClick(int mx, int my) {
        if (screen == Screen.START) {
            if (rcNameField.contains(mx, my))                          { nameFieldActive = true; }
            else if (rcEasy.contains(mx, my))                          { selDifficulty = Difficulty.EASY;   nameFieldActive = false; }
            else if (rcMedium.contains(mx, my))                        { selDifficulty = Difficulty.MEDIUM; nameFieldActive = false; }
            else if (rcHard.contains(mx, my))                          { selDifficulty = Difficulty.HARD;   nameFieldActive = false; }
            else if (rcStart.contains(mx, my) && !nameInput.isBlank()) { startGame(); }
            else                                                        { nameFieldActive = false; }
        } else if (screen == Screen.UPGRADE_SELECT) {
            GameState st = engine.getState();
            if (st == null || !st.isAwaitingUpgrade()) return;
            List<Upgrade> choices = st.getUpgradeChoices();
            for (int i = 0; i < rcUpgrade.length; i++) {
                if (rcUpgrade[i].contains(mx, my) && i < choices.size()) {
                    engine.selectUpgrade(choices.get(i));
                    screen = Screen.PLAYING;
                    spaceHeld = false;
                    return;
                }
            }
        } else if (screen == Screen.GAME_OVER) {
            if (rcNewGame.contains(mx, my)) goToStart();
            else if (rcQuit.contains(mx, my)) System.exit(0);
        }
    }

    private void startGame() {
        engine.initGame(nameInput.trim(), selDifficulty);
        screen = Screen.PLAYING;
        scoreSavedThisRound = false;
        spaceHeld = false;
        nameFieldActive = false;
        setCursor(Cursor.getDefaultCursor());
    }

    private void goToStart() {
        screen = Screen.START;
        nameInput = "";
        nameFieldActive = false;
        spaceHeld = false;
        setCursor(Cursor.getDefaultCursor());
    }

    // -----------------------------------------------------------------------
    // Runnable
    // -----------------------------------------------------------------------

    @Override
    public void run() {
        while (true) {
            if (screen == Screen.PLAYING) {
                GameState st = engine.getState();
                // Upgrade-Auswahl anzeigen wenn Welle abgeschlossen
                if (st != null && st.isAwaitingUpgrade()) {
                    screen = Screen.UPGRADE_SELECT;
                    spaceHeld = false;
                } else {
                    if (spaceHeld) engine.shoot();
                    engine.update();
                    if (st != null && st.isGameOver()) {
                        if (!scoreSavedThisRound) {
                            lastPlayerName = st.getPlayerName();
                            lastScore      = st.getScore();
                            lastDifficulty = st.getDifficulty().name();
                            lastWave       = st.getWave();
                            lastWon        = st.getLives() > 0;
                            saveScore(lastPlayerName, lastScore, lastDifficulty);
                            scoreSavedThisRound = true;
                        }
                        screen = Screen.GAME_OVER;
                    }
                }
            }
            try { Thread.sleep(1000 / FPS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        cursorBlink++;
        repaint();
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics e) {
        super.paintComponent(e);
        Graphics2D g = (Graphics2D) e;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBackground(g);
        switch (screen) {
            case START          -> drawStartScreen(g);
            case PLAYING        -> drawGame(g);
            case UPGRADE_SELECT -> { drawGame(g); drawUpgradeScreen(g); }
            case GAME_OVER      -> drawGameOverScreen(g);
        }
    }

    private void drawBackground(Graphics2D g) {
        GradientPaint gp = new GradientPaint(0, 0, new Color(0, 0, 20), 0, HEIGHT, new Color(0, 0, 8));
        g.setPaint(gp);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        for (int[] s : stars) {
            int b = s[2];
            g.setColor(new Color(b, b, b));
            g.fillRect(s[0], s[1], 1, 1);
        }
    }

    // ── Start-Screen ─────────────────────────────────────────────────────────

    private void drawStartScreen(Graphics2D g) {
        g.setFont(TITLE_FONT);
        String title = "SPACE INVADERS";
        drawNeonText(g, title, (WIDTH - g.getFontMetrics().stringWidth(title)) / 2, 110, new Color(0, 220, 100));

        g.setFont(SMALL_FONT);
        g.setColor(new Color(100, 200, 150));
        String sub = "[ Pfeiltasten / WASD bewegen  |  SPACE / LMT schiessen  |  Upgrades sammeln ]";
        g.drawString(sub, (WIDTH - g.getFontMetrics().stringWidth(sub)) / 2, 145);

        // Name-Feld
        g.setFont(MEDIUM_FONT);
        g.setColor(new Color(180, 230, 180));
        g.drawString("Dein Name:", 250, 220);

        boolean hoverF = rcNameField.contains(mouseX, mouseY);
        Color fBorder  = nameFieldActive ? new Color(0, 255, 130) : hoverF ? new Color(0, 180, 90) : new Color(60, 100, 60);
        g.setColor(nameFieldActive ? new Color(0, 20, 10) : new Color(3, 8, 3));
        g.fillRect(rcNameField.x, rcNameField.y, rcNameField.width, rcNameField.height);
        g.setColor(fBorder);
        g.setStroke(new BasicStroke(nameFieldActive ? 2.5f : 1.5f));
        g.drawRect(rcNameField.x, rcNameField.y, rcNameField.width, rcNameField.height);
        g.setColor(nameInput.isEmpty() && !nameFieldActive ? new Color(80, 80, 80) : Color.WHITE);
        g.drawString(nameInput.isEmpty() && !nameFieldActive ? "Hier klicken und tippen…" : nameInput,
                rcNameField.x + 10, rcNameField.y + 24);
        if (nameFieldActive && (cursorBlink / 30) % 2 == 0) {
            int cx = rcNameField.x + 10 + g.getFontMetrics().stringWidth(nameInput);
            g.setColor(new Color(0, 255, 130));
            g.fillRect(cx + 2, rcNameField.y + 8, 2, 20);
        }
        if (nameFieldActive) {
            g.setFont(SMALL_FONT); g.setColor(new Color(0, 180, 90));
            g.drawString("✎ Tippe deinen Namen…", rcNameField.x, rcNameField.y + rcNameField.height + 16);
        }

        // Schwierigkeit
        g.setFont(MEDIUM_FONT); g.setColor(new Color(180, 230, 180));
        g.drawString("Schwierigkeitsgrad:", 250, 315);
        drawDiffButton(g, "LEICHT", rcEasy,   Difficulty.EASY);
        drawDiffButton(g, "MITTEL", rcMedium, Difficulty.MEDIUM);
        drawDiffButton(g, "SCHWER", rcHard,   Difficulty.HARD);

        g.setFont(SMALL_FONT); g.setColor(new Color(120, 180, 120));
        String info = switch (selDifficulty) {
            case EASY   -> "LEICHT: 5 Leben  |  80 Munition  |  Langsame Aliens";
            case MEDIUM -> "MITTEL: 3 Leben  |  50 Munition  |  Normale Aliens";
            case HARD   -> "SCHWER: 2 Leben  |  30 Munition  |  Schnelle Aliens";
        };
        g.drawString(info, (WIDTH - g.getFontMetrics().stringWidth(info)) / 2, 398);

        // Start-Button
        boolean ready   = !nameInput.isBlank();
        boolean hoverSt = rcStart.contains(mouseX, mouseY) && ready;
        g.setColor(ready ? (hoverSt ? new Color(0, 50, 25) : new Color(0, 25, 12)) : new Color(5, 8, 5));
        g.fillRect(rcStart.x, rcStart.y, rcStart.width, rcStart.height);
        g.setColor(ready ? (hoverSt ? new Color(100, 255, 160) : new Color(0, 200, 100)) : new Color(50, 80, 50));
        g.setStroke(new BasicStroke(ready ? 2 : 1));
        g.drawRect(rcStart.x, rcStart.y, rcStart.width, rcStart.height);
        g.setFont(MEDIUM_FONT);
        g.setColor(ready ? (hoverSt ? new Color(120, 255, 180) : new Color(0, 230, 120)) : new Color(50, 80, 50));
        String startTxt = ready ? "▶  SPIELEN" : "[ Name eingeben ]";
        g.drawString(startTxt, rcStart.x + (rcStart.width - g.getFontMetrics().stringWidth(startTxt)) / 2, rcStart.y + 27);

        g.setFont(TINY_FONT); g.setColor(new Color(70, 110, 70));
        String hint = "Klick auf Feld tippen  |  Klick auf Schwierigkeit  |  ENTER oder Klick auf Spielen";
        g.drawString(hint, (WIDTH - g.getFontMetrics().stringWidth(hint)) / 2, 510);
    }

    private void drawDiffButton(Graphics2D g, String label, Rectangle r, Difficulty d) {
        boolean active = selDifficulty == d, hover = r.contains(mouseX, mouseY);
        g.setColor(active ? new Color(0, 35, 18) : hover ? new Color(0, 20, 10) : new Color(3, 8, 3));
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(active ? new Color(0, 220, 120) : hover ? new Color(0, 150, 80) : new Color(50, 90, 50));
        g.setStroke(new BasicStroke(active ? 2.5f : 1.5f));
        g.drawRect(r.x, r.y, r.width, r.height);
        g.setFont(SMALL_FONT);
        g.setColor(active ? new Color(0, 240, 130) : hover ? new Color(0, 180, 100) : new Color(70, 120, 70));
        g.drawString(label, r.x + (r.width - g.getFontMetrics().stringWidth(label)) / 2, r.y + 23);
        if (active) { g.setFont(TINY_FONT); g.setColor(new Color(0, 180, 90)); g.drawString("✔", r.x + r.width - 14, r.y + 12); }
    }

    // ── Spiel ─────────────────────────────────────────────────────────────────

    private void drawGame(Graphics2D g) {
        if (engine.getState() == null) return;
        GameState st = engine.getState();
        drawHUD(g, st);
        g.setColor(new Color(0, 80, 40)); g.setStroke(new BasicStroke(1));
        g.drawLine(0, 55, WIDTH, 55);
        st.getAliens().stream().filter(a -> a.isAlive()).forEach(a -> a.draw(g));
        st.getBullets().forEach(b -> b.draw(g));
        st.getAmmoDrops().forEach(d -> d.draw(g));
        st.getUpgradePickups().forEach(u -> u.draw(g));
        // Ghost-Modus: Schiff blinkt
        SpaceObject player = st.getPlayer();
        if (st.getPlayer().ghostModeTicks == 0 || (cursorBlink / 5) % 2 == 0) {
            player.draw(g);
        }
        drawControlsBar(g, st);
    }

    private void drawHUD(Graphics2D g, GameState st) {
        g.setFont(HUD_FONT);
        g.setColor(new Color(0, 200, 100));
        g.drawString("SCORE: " + st.getScore(), 10, 20);

        String waveStr = "WELLE " + st.getWave();
        g.drawString(waveStr, (WIDTH - g.getFontMetrics().stringWidth(waveStr)) / 2, 20);

        String nm = st.getPlayerName().toUpperCase();
        g.setColor(new Color(150, 200, 150));
        g.drawString(nm, WIDTH - g.getFontMetrics().stringWidth(nm) - 10, 20);

        g.setColor(new Color(220, 60, 60));
        StringBuilder h = new StringBuilder("LEBEN: ");
        for (int i = 0; i < st.getLives(); i++) h.append("♥ ");
        g.drawString(h.toString(), WIDTH - g.getFontMetrics().stringWidth(h.toString()) - 10, 38);

        g.setColor(new Color(0, 200, 220));
        g.drawString("AMMO: " + st.getPlayer().ammo, 10, 38);

        // Schild-Anzeige
        if (st.getPlayer().shieldHits > 0) {
            g.setColor(new Color(80, 150, 255));
            g.drawString("  SH:" + st.getPlayer().shieldHits, 120, 38);
        }

        // Aktive Upgrades als kleine Badges
        drawUpgradeBadges(g, st);
    }

    private void drawUpgradeBadges(Graphics2D g, GameState st) {
        if (st.getPlayer().upgrades.isEmpty()
                && st.getPlayer().superPierceShots == 0) return;

        g.setFont(TINY_FONT);
        int ux = 10;
        int uy = 52;

        // Supershots als spezielle Badge
        if (st.getPlayer().superPierceShots > 0) {
            Color uc = new Color(0xFFCC00);
            drawBadge(g, "MG:" + st.getPlayer().superPierceShots, ux, uy, uc);
            ux += 50;
        }

        for (Upgrade u : st.getPlayer().upgrades) {
            if (ux > WIDTH - 50) break;
            Color uc = new Color(u.colorRGB);
            drawBadge(g, upgAbbrev(u), ux, uy, uc);
            ux += 35;
        }
    }

    private void drawBadge(Graphics2D g, String label, int x, int y, Color c) {
        int w = Math.max(30, g.getFontMetrics(TINY_FONT).stringWidth(label) + 8);
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 45));
        g.fillRoundRect(x, y, w, 13, 3, 3);
        g.setColor(c);
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x, y, w, 13, 3, 3);
        g.setFont(TINY_FONT);
        g.drawString(label, x + (w - g.getFontMetrics().stringWidth(label)) / 2, y + 10);
    }

    private static String upgAbbrev(Upgrade u) {
        return switch (u) {
            case DOUBLE_SHOT  -> "2x";
            case TRIPLE_SHOT  -> "3x";
            case FAST_FIRE    -> "FF";
            case FAST_BULLET  -> "FB";
            case PIERCING     -> "PC";
            case WIDER_BULLET -> "WB";
            case EXPLOSION    -> "EX";
            case SUPER_SHOT   -> "MG";
            case SHIELD       -> "SH";
            case GHOST_MODE   -> "GM";
            case EXTRA_LIFE   -> "+L";
            case FAST_SHIP    -> "TS";
            case SPEED_AURA   -> "SA";
            case AUTO_FIRE    -> "AF";
            case MAGNET       -> "MG";
            case AMMO_REGEN   -> "AR";
            case EXTRA_DROPS  -> "ED";
            case SCORE_BOOST  -> "2P";
            case AMMO_20      -> "+20";
            case AMMO_30      -> "+30";
        };
    }

    private void drawControlsBar(Graphics2D g, GameState st) {
        int barY = HEIGHT - 22;
        g.setColor(new Color(0, 0, 0, 170)); g.fillRect(0, barY, WIDTH, 22);
        g.setColor(new Color(0, 60, 30)); g.setStroke(new BasicStroke(1)); g.drawLine(0, barY, WIDTH, barY);
        g.setFont(TINY_FONT);
        g.setColor(new Color(0, 200, 100)); g.drawString("◄ ►", 8, barY + 15);
        g.setColor(new Color(60, 110, 60)); g.drawString("/ A D", 38, barY + 15);
        g.setColor(new Color(80, 130, 80)); g.drawString("Bewegen", 80, barY + 15);
        g.setColor(new Color(0, 60, 30)); g.drawLine(150, barY + 3, 150, barY + 18);
        g.setColor(spaceHeld ? new Color(255, 230, 50) : new Color(0, 200, 100));
        g.drawString("SPACE/W/↑/LMT", 158, barY + 15);
        g.setColor(new Color(80, 130, 80)); g.drawString("Schiessen", 288, barY + 15);
        g.setColor(new Color(0, 60, 30)); g.drawLine(380, barY + 3, 380, barY + 18);
        g.setColor(new Color(0, 210, 210)); g.drawString("◆", 388, barY + 15);
        g.setColor(new Color(80, 130, 80)); g.drawString("Cyan=+Ammo", 402, barY + 15);
        g.setColor(new Color(0, 60, 30)); g.drawLine(510, barY + 3, 510, barY + 18);
        g.setColor(new Color(255, 200, 0)); g.drawString("◆", 518, barY + 15);
        g.setColor(new Color(80, 130, 80)); g.drawString("Gold=Upgrade", 532, barY + 15);

        if (st.getPlayer().ammo == 0) {
            g.setColor(new Color(220, 60, 60, 200)); g.fillRect(WIDTH / 2 - 120, barY - 22, 240, 20);
            g.setFont(new Font("Courier New", Font.BOLD, 11)); g.setColor(new Color(255, 110, 110));
            g.drawString("!! KEINE MUNITION – Drops einsammeln !!", WIDTH / 2 - 118, barY - 7);
        }
    }

    // ── Upgrade-Auswahl-Screen ───────────────────────────────────────────────

    private void drawUpgradeScreen(Graphics2D g) {
        GameState st = engine.getState();
        if (st == null || !st.isAwaitingUpgrade()) return;

        // Dunkle Overlay-Ebene
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Titel
        g.setFont(TITLE_FONT);
        String title = "WELLE " + st.getWave() + " ABGESCHLOSSEN!";
        drawNeonText(g, title, (WIDTH - g.getFontMetrics().stringWidth(title)) / 2, 90, new Color(0, 220, 100));

        // Bonus-Punkte
        g.setFont(SMALL_FONT);
        g.setColor(new Color(255, 200, 50));
        String bonus = "+" + st.getLastWaveBonus() + " Wellen-Bonus!";
        g.drawString(bonus, (WIDTH - g.getFontMetrics().stringWidth(bonus)) / 2, 118);

        // Untertitel
        g.setFont(MEDIUM_FONT);
        g.setColor(new Color(150, 210, 255));
        String sub = "Wähle ein Upgrade für Welle " + (st.getWave() + 1) + ":";
        g.drawString(sub, (WIDTH - g.getFontMetrics().stringWidth(sub)) / 2, 155);

        g.setFont(TINY_FONT);
        g.setColor(new Color(80, 140, 80));
        String hint = "Klicke auf eine Karte  |  oder drücke  [ 1 ]  [ 2 ]  [ 3 ]";
        g.drawString(hint, (WIDTH - g.getFontMetrics().stringWidth(hint)) / 2, 175);

        // Die 3 Upgrade-Karten
        List<Upgrade> choices = st.getUpgradeChoices();
        for (int i = 0; i < choices.size() && i < 3; i++) {
            drawUpgradeCard(g, choices.get(i), rcUpgrade[i], i + 1);
        }
    }

    private void drawUpgradeCard(Graphics2D g, Upgrade u, Rectangle r, int num) {
        boolean hover = r.contains(mouseX, mouseY);
        Color   c     = new Color(u.colorRGB);
        Color   cDark = new Color(c.getRed() / 6, c.getGreen() / 6, c.getBlue() / 6);

        // Hintergrund
        g.setColor(hover ? cDark.brighter() : cDark);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);

        // Rand (leuchtet beim Hover)
        g.setColor(hover ? c.brighter() : c);
        g.setStroke(new BasicStroke(hover ? 2.5f : 1.5f));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);

        // Nummern-Badge oben links
        g.setColor(c);
        g.setFont(new Font("Courier New", Font.BOLD, 18));
        g.fillRoundRect(r.x + 8, r.y + 8, 24, 24, 5, 5);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Courier New", Font.BOLD, 14));
        g.drawString(String.valueOf(num), r.x + 14, r.y + 25);

        // Upgrade-Name
        g.setFont(new Font("Courier New", Font.BOLD, 14));
        g.setColor(hover ? Color.WHITE : new Color(c.getRed() + 60, c.getGreen() + 60, c.getBlue() + 60));
        String name = u.displayName;
        // Langer Name: Schriftgröße verkleinern
        if (g.getFontMetrics().stringWidth(name) > r.width - 20) {
            g.setFont(new Font("Courier New", Font.BOLD, 11));
        }
        int nameW = g.getFontMetrics().stringWidth(name);
        g.drawString(name, r.x + (r.width - nameW) / 2, r.y + 65);

        // Trennlinie
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 80));
        g.setStroke(new BasicStroke(1));
        g.drawLine(r.x + 15, r.y + 75, r.x + r.width - 15, r.y + 75);

        // Beschreibung (mehrzeilig wenn nötig)
        g.setFont(TINY_FONT);
        g.setColor(new Color(200, 200, 200));
        String desc = u.description;
        int maxW = r.width - 20;
        FontMetrics fm = g.getFontMetrics();
        // Beschreibung in Zeilen aufteilen
        List<String> lines = wrapText(desc, fm, maxW);
        int lineY = r.y + 98;
        for (String line : lines) {
            g.drawString(line, r.x + 10, lineY);
            lineY += fm.getHeight() + 2;
            if (lineY > r.y + r.height - 30) break;
        }

        // Auswählen-Hinweis unten
        g.setFont(TINY_FONT);
        g.setColor(hover ? c : new Color(c.getRed(), c.getGreen(), c.getBlue(), 140));
        String selHint = "[ " + num + " ] Auswählen";
        g.drawString(selHint, r.x + (r.width - g.getFontMetrics().stringWidth(selHint)) / 2, r.y + r.height - 12);
    }

    private static List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String word : words) {
            String test = cur.isEmpty() ? word : cur + " " + word;
            if (fm.stringWidth(test) > maxWidth && !cur.isEmpty()) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(test);
            }
        }
        if (!cur.isEmpty()) lines.add(cur.toString());
        return lines;
    }

    // ── Game-Over-Screen ─────────────────────────────────────────────────────

    private void drawGameOverScreen(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 230));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setFont(TITLE_FONT);
        String headline = lastWon ? "GEWONNEN!" : "GAME OVER";
        Color  hlColor  = lastWon ? new Color(0, 230, 100) : new Color(220, 50, 50);
        drawNeonText(g, headline, (WIDTH - g.getFontMetrics().stringWidth(headline)) / 2, 80, hlColor);

        g.setFont(MEDIUM_FONT);
        g.setColor(Color.WHITE);
        String scoreStr = "Score: " + lastScore;
        g.drawString(scoreStr, (WIDTH - g.getFontMetrics().stringWidth(scoreStr)) / 2, 120);

        g.setFont(SMALL_FONT);
        g.setColor(new Color(120, 170, 120));
        String infoStr = lastPlayerName.toUpperCase() + "  |  " + diffLabel(lastDifficulty) + "  |  Welle " + lastWave;
        g.drawString(infoStr, (WIDTH - g.getFontMetrics().stringWidth(infoStr)) / 2, 145);

        g.setColor(new Color(0, 120, 60)); g.setStroke(new BasicStroke(1));
        g.drawLine(100, 160, WIDTH - 100, 160);

        g.setFont(new Font("Courier New", Font.BOLD, 16));
        g.setColor(new Color(0, 210, 110));
        String lbTitle = "BESTENLISTE";
        g.drawString(lbTitle, (WIDTH - g.getFontMetrics().stringWidth(lbTitle)) / 2, 185);

        g.setFont(TINY_FONT); g.setColor(new Color(0, 150, 70));
        g.drawString("#", 120, 205); g.drawString("Name", 170, 205);
        g.drawString("Score", 380, 205); g.drawString("Schwierigkeit", 490, 205);
        g.drawString("Datum", 680, 205);
        g.setColor(new Color(0, 80, 40));
        g.drawLine(110, 208, WIDTH - 110, 208);

        int maxRows = Math.min(leaderboard.size(), 10);
        for (int i = 0; i < maxRows; i++) {
            String[] row = leaderboard.get(i);
            int ry = 225 + i * 22;
            boolean isMe = row[0].equalsIgnoreCase(lastPlayerName) && safeInt(row[1]) == lastScore;
            if (isMe) {
                g.setColor(new Color(0, 50, 20));
                g.fillRoundRect(110, ry - 14, WIDTH - 220, 18, 4, 4);
                g.setColor(new Color(0, 180, 80));
                g.setStroke(new BasicStroke(1));
                g.drawRoundRect(110, ry - 14, WIDTH - 220, 18, 4, 4);
            }
            Color rowColor = isMe ? new Color(0, 255, 130)
                    : i == 0  ? new Color(255, 200, 0)
                    : i == 1  ? new Color(190, 190, 190)
                    : i == 2  ? new Color(200, 120, 60)
                    :           new Color(100, 160, 100);
            g.setFont(SMALL_FONT); g.setColor(rowColor);
            String medal = i == 0 ? "1." : i == 1 ? "2." : i == 2 ? "3." : (i + 1) + ".";
            g.drawString(medal,                  120, ry);
            g.drawString(cap(row[0], 14),        170, ry);
            g.drawString(row[1],                 380, ry);
            g.drawString(diffLabel(row.length > 2 ? row[2] : ""), 490, ry);
            g.setFont(TINY_FONT); g.setColor(new Color(80, 130, 80));
            g.drawString(row.length > 3 ? row[3] : "-", 680, ry);
        }

        if (leaderboard.isEmpty()) {
            g.setFont(SMALL_FONT); g.setColor(new Color(70, 110, 70));
            g.drawString("Noch keine Einträge.", WIDTH / 2 - 90, 250);
        }

        g.setColor(new Color(0, 80, 40));
        g.drawLine(100, 468, WIDTH - 100, 468);

        drawOverlayButton(g, "▶  Neues Spiel", rcNewGame);
        drawOverlayButton(g, "✕  Beenden",     rcQuit);

        g.setFont(TINY_FONT); g.setColor(new Color(60, 100, 60));
        String kh = "[ R ] Neues Spiel  |  [ ESC ] Beenden";
        g.drawString(kh, (WIDTH - g.getFontMetrics().stringWidth(kh)) / 2, 548);
    }

    private void drawOverlayButton(Graphics2D g, String label, Rectangle r) {
        boolean hover = r.contains(mouseX, mouseY);
        g.setColor(hover ? new Color(0, 50, 25) : new Color(0, 18, 8));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
        g.setColor(hover ? new Color(0, 230, 120) : new Color(0, 150, 70));
        g.setStroke(new BasicStroke(hover ? 2 : 1.5f));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 6, 6);
        g.setFont(SMALL_FONT);
        g.setColor(hover ? new Color(0, 255, 140) : new Color(0, 210, 110));
        int lw = g.getFontMetrics().stringWidth(label);
        g.drawString(label, r.x + (r.width - lw) / 2, r.y + 26);
    }

    private void drawNeonText(Graphics2D g, String text, int x, int y, Color c) {
        for (int d = 4; d >= 1; d--) {
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 40));
            g.drawString(text, x - d, y - d); g.drawString(text, x + d, y + d);
        }
        g.setColor(c); g.drawString(text, x, y);
    }

    private String diffLabel(String d) {
        return switch (d == null ? "" : d.toUpperCase()) {
            case "EASY"   -> "Leicht";
            case "MEDIUM" -> "Mittel";
            case "HARD"   -> "Schwer";
            default       -> d == null ? "-" : d;
        };
    }

    private String cap(String s, int max) {
        if (s == null) return "-";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    // -----------------------------------------------------------------------
    // KeyListener
    // -----------------------------------------------------------------------

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (screen == Screen.START) {
            if (code == KeyEvent.VK_ENTER && !nameInput.isBlank()) { startGame(); }
            else if (code == KeyEvent.VK_BACK_SPACE && !nameInput.isEmpty() && nameFieldActive) {
                nameInput = nameInput.substring(0, nameInput.length() - 1);
            } else if (code == KeyEvent.VK_1 || code == KeyEvent.VK_NUMPAD1) { selDifficulty = Difficulty.EASY; }
            else if (code == KeyEvent.VK_2 || code == KeyEvent.VK_NUMPAD2)   { selDifficulty = Difficulty.MEDIUM; }
            else if (code == KeyEvent.VK_3 || code == KeyEvent.VK_NUMPAD3)   { selDifficulty = Difficulty.HARD; }
        } else if (screen == Screen.PLAYING) {
            switch (code) {
                case KeyEvent.VK_LEFT,  KeyEvent.VK_A                  -> engine.setMoveLeft(true);
                case KeyEvent.VK_RIGHT, KeyEvent.VK_D                  -> engine.setMoveRight(true);
                case KeyEvent.VK_SPACE, KeyEvent.VK_UP, KeyEvent.VK_W  -> spaceHeld = true;
            }
        } else if (screen == Screen.UPGRADE_SELECT) {
            GameState st = engine.getState();
            if (st == null) return;
            List<Upgrade> choices = st.getUpgradeChoices();
            int idx = switch (code) {
                case KeyEvent.VK_1, KeyEvent.VK_NUMPAD1 -> 0;
                case KeyEvent.VK_2, KeyEvent.VK_NUMPAD2 -> 1;
                case KeyEvent.VK_3, KeyEvent.VK_NUMPAD3 -> 2;
                default -> -1;
            };
            if (idx >= 0 && idx < choices.size()) {
                engine.selectUpgrade(choices.get(idx));
                screen = Screen.PLAYING;
            }
        } else if (screen == Screen.GAME_OVER) {
            if (code == KeyEvent.VK_R)           goToStart();
            else if (code == KeyEvent.VK_ESCAPE) System.exit(0);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        switch (code) {
            case KeyEvent.VK_LEFT,  KeyEvent.VK_A                  -> engine.setMoveLeft(false);
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D                  -> engine.setMoveRight(false);
            case KeyEvent.VK_SPACE, KeyEvent.VK_UP, KeyEvent.VK_W  -> spaceHeld = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (screen == Screen.START && nameFieldActive) {
            char c = e.getKeyChar();
            if (nameInput.length() < 16 && (Character.isLetterOrDigit(c) || c == '_' || c == '-')) {
                nameInput += c;
            }
        }
    }

    // -----------------------------------------------------------------------
    // main()
    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SpaceInvadersMain panel = new SpaceInvadersMain();
            JFrame frame = new JFrame("Space Invaders");
            panel.fenster = frame;
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
            frame.toFront();
            Timer ft = new Timer(250, ev -> {
                ((Timer) ev.getSource()).stop();
                frame.setAlwaysOnTop(false);
                panel.requestFocusInWindow();
            });
            ft.setRepeats(false); ft.start();
            frame.addWindowFocusListener(new WindowFocusListener() {
                @Override public void windowGainedFocus(WindowEvent e) { panel.requestFocusInWindow(); }
                @Override public void windowLostFocus(WindowEvent e)   {}
            });
        });
    }
}
