// ═══════════════════════════════════════════════════════
//  Space Invaders – Frontend (HTML5 Canvas + WebSocket)
// ═══════════════════════════════════════════════════════

'use strict';

// ── Zustand ──────────────────────────────────────────────────────────────────
const state = {
    screen:     'start',   // 'start' | 'game' | 'leaderboard'
    difficulty: 'MEDIUM',
    ws:         null,
    gameData:   null,
    connected:  false,
    _lbSaved:   false,
};

const diffInfo = {
    EASY:   'LEICHT: 5 Leben | 80 Munition | Langsame Aliens',
    MEDIUM: 'MITTEL: 3 Leben | 50 Munition | Normale Aliens',
    HARD:   'SCHWER: 2 Leben | 30 Munition | Schnelle Aliens',
};

// ── DOM-Referenzen ────────────────────────────────────────────────────────────
const startScreen       = document.getElementById('startScreen');
const gameScreen        = document.getElementById('gameScreen');
const leaderboardScreen = document.getElementById('leaderboardScreen');
const playerNameInput   = document.getElementById('playerName');
const startBtn          = document.getElementById('startBtn');
const leaderboardBtn    = document.getElementById('leaderboardBtn');
const backBtn           = document.getElementById('backBtn');
const diffBtns          = document.querySelectorAll('.diff-btn');
const diffInfoBox       = document.getElementById('diffInfo');
const lbBody            = document.getElementById('lbBody');
const canvas            = document.getElementById('gameCanvas');
const ctx               = canvas.getContext('2d');
const starCanvas        = document.getElementById('starfield');
const starCtx           = starCanvas.getContext('2d');

// ── Mausposition auf dem Canvas ──────────────────────────────────────────────
const mousePos = { x: -1, y: -1 };
canvas.addEventListener('mousemove', e => {
    const rect   = canvas.getBoundingClientRect();
    const scaleX = 900 / rect.width;
    const scaleY = 600 / rect.height;
    mousePos.x = (e.clientX - rect.left) * scaleX;
    mousePos.y = (e.clientY - rect.top)  * scaleY;
});

// Upgrade-Karten Positionen (statisch, wie im Desktop)
const UPGRADE_CARDS = [
    { x: 75,  y: 195, w: 215, h: 200 },
    { x: 338, y: 195, w: 215, h: 200 },
    { x: 600, y: 195, w: 215, h: 200 },
];

// ── Sterne (Hintergrund-Animation) ────────────────────────────────────────────
const STARS = Array.from({ length: 250 }, () => ({
    x: Math.random() * window.innerWidth,
    y: Math.random() * window.innerHeight,
    size: Math.random() * 1.5 + 0.3,
    b:  Math.random(),
    db: (Math.random() - 0.5) * 0.015,
}));

function resizeStarfield() {
    starCanvas.width  = window.innerWidth;
    starCanvas.height = window.innerHeight;
}
window.addEventListener('resize', resizeStarfield);
resizeStarfield();

function animateStars() {
    starCtx.fillStyle = '#000010';
    starCtx.fillRect(0, 0, starCanvas.width, starCanvas.height);
    STARS.forEach(s => {
        s.b += s.db;
        if (s.b < 0 || s.b > 1) s.db = -s.db;
        const a = 0.3 + s.b * 0.7;
        starCtx.fillStyle = `rgba(200,220,255,${a})`;
        starCtx.fillRect(s.x, s.y, s.size, s.size);
    });
    requestAnimationFrame(animateStars);
}
animateStars();

// ── Start-Screen-Logik ────────────────────────────────────────────────────────
playerNameInput.addEventListener('input', () => {
    startBtn.disabled = playerNameInput.value.trim().length === 0;
});

diffBtns.forEach(btn => {
    btn.addEventListener('click', () => {
        state.difficulty = btn.dataset.diff;
        diffBtns.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        diffInfoBox.textContent = diffInfo[state.difficulty];
    });
});

startBtn.addEventListener('click', startGame);
playerNameInput.addEventListener('keydown', e => {
    if (e.key === 'Enter' && !startBtn.disabled) startGame();
});

leaderboardBtn.addEventListener('click', () => {
    showScreen('leaderboard');
    loadLeaderboard();
});

backBtn.addEventListener('click', () => showScreen('start'));

// ── Screen-Wechsel ────────────────────────────────────────────────────────────
function showScreen(name) {
    state.screen = name;
    startScreen.classList.toggle      ('hidden', name !== 'start');
    gameScreen.classList.toggle       ('hidden', name !== 'game');
    leaderboardScreen.classList.toggle('hidden', name !== 'leaderboard');
    if (name === 'game') startRenderLoop();
    else stopRenderLoop();
}

// ── WebSocket-Verbindung ──────────────────────────────────────────────────────
function connectWS(onOpen) {
    const proto = location.protocol === 'https:' ? 'wss' : 'ws';
    const ws = new WebSocket(`${proto}://${location.host}/ws/game`);

    ws.onopen = () => {
        state.connected = true;
        onOpen(ws);
    };
    ws.onmessage = e => {
        const msg = JSON.parse(e.data);
        if (msg.type === 'STATE') handleGameState(msg.data);
    };
    ws.onclose = () => {
        state.connected = false;
        state.ws = null;
    };
    ws.onerror = () => ws.close();
    state.ws = ws;
}

function startGame() {
    const name = playerNameInput.value.trim();
    if (!name) return;
    showScreen('game');
    keysDown.clear();
    if (state.ws && state.ws.readyState === WebSocket.OPEN) {
        sendWS({ type: 'START', name, difficulty: state.difficulty });
    } else {
        connectWS(() => sendWS({ type: 'START', name, difficulty: state.difficulty }));
    }
}

function sendWS(obj) {
    if (state.ws && state.ws.readyState === WebSocket.OPEN) {
        state.ws.send(JSON.stringify(obj));
    }
}

function releaseAllKeys() {
    keysDown.clear();
    ['ArrowLeft', 'ArrowRight', 'a', 'A', 'd', 'D', ' ', 'ArrowUp', 'w', 'W'].forEach(k =>
        sendWS({ type: 'KEY', key: k, pressed: false }));
}

function pauseGame() {
    if (!state.gameData || state.gameData.phase === 'PAUSED'
        || state.gameData.phase === 'GAME_OVER' || state.gameData.phase === 'UPGRADE_SELECT') return;
    releaseAllKeys();
    sendWS({ type: 'PAUSE' });
}

function resumeGame() {
    if (!state.gameData || state.gameData.phase !== 'PAUSED') return;
    sendWS({ type: 'RESUME' });
}

function togglePause() {
    if (!state.gameData) return;
    if (state.gameData.phase === 'PAUSED') resumeGame();
    else pauseGame();
}

// ── Tastatur-Input ────────────────────────────────────────────────────────────
const keysDown = new Set();
const FIRE_KEYS = new Set([' ', 'ArrowUp', 'w', 'W']);

document.addEventListener('keydown', e => {
    if (state.screen !== 'game') return;

    // Pause / Fortsetzen
    if (e.key === 'Escape' || e.key === 'p' || e.key === 'P') {
        if (state.gameData && (state.gameData.phase === 'GAME_OVER' || state.gameData.phase === 'UPGRADE_SELECT')) return;
        e.preventDefault();
        togglePause();
        return;
    }

    // Fortsetzen mit Enter/Leertaste wenn pausiert
    if (state.gameData && state.gameData.phase === 'PAUSED') {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            resumeGame();
        }
        return;
    }

    // Upgrade-Auswahl per Tastatur
    if (state.gameData && state.gameData.phase === 'UPGRADE_SELECT') {
        const idx = { '1': 0, '2': 1, '3': 2 }[e.key];
        const choices = state.gameData.upgradeChoices;
        if (idx !== undefined && choices && choices[idx]) {
            sendWS({ type: 'SELECT_UPGRADE', upgrade: choices[idx].key });
        }
        e.preventDefault();
        return;
    }

    const k = e.key;
    if (!keysDown.has(k)) {
        keysDown.add(k);
        sendWS({ type: 'KEY', key: k, pressed: true });
        e.preventDefault();
    }
});

document.addEventListener('keyup', e => {
    if (state.screen !== 'game') return;
    if (state.gameData && state.gameData.phase === 'PAUSED') return;
    const k = e.key;
    if (keysDown.has(k)) {
        keysDown.delete(k);
        sendWS({ type: 'KEY', key: k, pressed: false });
        e.preventDefault();
    }
});

// ── Game-State verarbeiten ────────────────────────────────────────────────────
let latestGameData  = null;
let renderLoopActive = false;

function handleGameState(data) {
    state.gameData = data;
    latestGameData = data;
}

function startRenderLoop() {
    if (renderLoopActive) return;
    renderLoopActive = true;
    requestAnimationFrame(renderLoop);
}

function stopRenderLoop() {
    renderLoopActive = false;
    latestGameData = null;
}

function renderLoop() {
    if (!renderLoopActive) return;
    if (state.screen === 'game' && latestGameData) {
        renderGame(latestGameData);
    }
    requestAnimationFrame(renderLoop);
}

// ── Canvas-Rendering ──────────────────────────────────────────────────────────
const GAME_STARS = Array.from({ length: 60 }, () => ({
    x:  Math.random() * 900,
    y:  Math.random() * 600,
    sz: Math.random() * 1.2 + 0.4,
    b:  0.3 + Math.random() * 0.5,
}));

// Hintergrund einmal vorzeichnen (spart CPU pro Frame)
const bgCanvas = document.createElement('canvas');
bgCanvas.width = 900; bgCanvas.height = 600;
const bgCtx = bgCanvas.getContext('2d');
bgCtx.fillStyle = '#000010';
bgCtx.fillRect(0, 0, 900, 600);
GAME_STARS.forEach(s => {
    bgCtx.fillStyle = `rgba(200,220,255,${0.2 + s.b * 0.5})`;
    bgCtx.fillRect(s.x, s.y, s.sz, s.sz);
});

let starTick = 0;

function renderGame(d) {
    if (!d) return;

    ctx.drawImage(bgCanvas, 0, 0);
    starTick++;

    // HUD-Trennlinie
    ctx.strokeStyle = '#004422';
    ctx.lineWidth = 1;
    ctx.beginPath(); ctx.moveTo(0, 58); ctx.lineTo(900, 58); ctx.stroke();

    // Spielobjekte
    if (d.aliens)         d.aliens.forEach(a        => drawAlien(a));
    if (d.ammoDrops)      d.ammoDrops.forEach(drop  => drawAmmoDrop(drop));
    if (d.upgradePickups) d.upgradePickups.forEach(u => drawUpgradePickup(u));
    if (d.bullets)        d.bullets.forEach(b        => drawBullet(b));
    if (d.player) {
        const ghostFlash = d.player.ghostTicks > 0 && Math.floor(starTick / 4) % 2 === 0;
        if (!ghostFlash) drawPlayer(d.player);
    }

    drawHUD(d);

    ctx.strokeStyle = '#003322';
    ctx.beginPath(); ctx.moveTo(0, 595); ctx.lineTo(900, 595); ctx.stroke();

    // Overlays
    if (d.phase === 'UPGRADE_SELECT') drawUpgradeSelect(d);
    if (d.phase === 'PAUSED')         drawPaused(d);
    if (d.phase === 'GAME_OVER')      drawGameOver(d);
    if (d.phase === 'WAITING')        drawWaiting();
}

// ── Spieler ───────────────────────────────────────────────────────────────────
function drawPlayer(p) {
    const { x, y } = p;
    ctx.fillStyle = '#00e070';
    ctx.fillRect(x + 15, y + 15, 20, 18);
    ctx.beginPath(); ctx.moveTo(x + 25, y); ctx.lineTo(x + 15, y + 15); ctx.lineTo(x + 35, y + 15); ctx.closePath(); ctx.fill();
    ctx.beginPath(); ctx.moveTo(x, y + 30); ctx.lineTo(x + 15, y + 20); ctx.lineTo(x + 15, y + 33); ctx.closePath(); ctx.fill();
    ctx.beginPath(); ctx.moveTo(x + 35, y + 20); ctx.lineTo(x + 50, y + 30); ctx.lineTo(x + 35, y + 33); ctx.closePath(); ctx.fill();
    ctx.fillStyle = 'rgba(0,140,255,0.75)';
    ctx.fillRect(x + 11, y + 31, 8, 5);
    ctx.fillRect(x + 31, y + 31, 8, 5);
    ctx.fillStyle = 'rgba(150,230,255,0.7)';
    ctx.beginPath(); ctx.ellipse(x + 25, y + 10, 5, 4, 0, 0, Math.PI * 2); ctx.fill();
    // Schild-Effekt
    if (p.shieldHits > 0) {
        ctx.strokeStyle = 'rgba(60,150,255,0.6)';
        ctx.lineWidth = 2;
        ctx.beginPath(); ctx.ellipse(x + 25, y + 17, 32, 22, 0, 0, Math.PI * 2); ctx.stroke();
    }
}

// ── Aliens ────────────────────────────────────────────────────────────────────
function drawAlien(a) {
    const { x, y, type, af: frame } = a;
    switch (type) {
        case 1: drawGrunt(x, y, frame);     break;
        case 2: drawSoldier(x, y, frame);   break;
        case 3: drawCommander(x, y, frame); break;
    }
}

function drawGrunt(x, y, frame) {
    ctx.fillStyle = '#44ff44';
    ctx.fillRect(x + 6, y + 6, 24, 15); ctx.fillRect(x + 3, y + 11, 5, 9); ctx.fillRect(x + 28, y + 11, 5, 9);
    const lo = frame === 0 ? 0 : 3;
    ctx.fillRect(x + 6, y + 21, 5, 5 + lo); ctx.fillRect(x + 14, y + 21, 5, 5); ctx.fillRect(x + 22, y + 21, 5, 5 + lo);
    ctx.fillStyle = '#000'; ctx.fillRect(x + 9, y + 8, 5, 5); ctx.fillRect(x + 22, y + 8, 5, 5);
    ctx.fillStyle = '#fff'; ctx.fillRect(x + 10, y + 9, 3, 3); ctx.fillRect(x + 23, y + 9, 3, 3);
}

function drawSoldier(x, y, frame) {
    ctx.fillStyle = '#4488ff';
    ctx.fillRect(x + 4, y + 8, 28, 16); ctx.fillRect(x + 8, y + 3, 20, 7);
    ctx.fillRect(x, y + 14, 5, 8); ctx.fillRect(x + 31, y + 14, 5, 8);
    const lo = frame === 0 ? 0 : 3;
    ctx.fillRect(x + 6, y + 24, 4, 4 + lo); ctx.fillRect(x + 14, y + 24, 4, 4); ctx.fillRect(x + 22, y + 24, 4, 4 + lo);
    ctx.fillStyle = '#000'; ctx.fillRect(x + 9, y + 10, 5, 5); ctx.fillRect(x + 22, y + 10, 5, 5);
    ctx.fillStyle = '#aaddff'; ctx.fillRect(x + 10, y + 11, 3, 3); ctx.fillRect(x + 23, y + 11, 3, 3);
}

function drawCommander(x, y, frame) {
    ctx.fillStyle = '#dd3333';
    ctx.fillRect(x + 3, y + 7, 30, 18); ctx.fillRect(x + 7, y + 2, 22, 7);
    const to = frame === 0 ? 0 : 3;
    ctx.fillRect(x, y + 17, 4, 8 + to); ctx.fillRect(x + 32, y + 17, 4, 8 + to);
    ctx.fillRect(x + 7, y + 23, 4, 5); ctx.fillRect(x + 14, y + 23, 4, 5 + to); ctx.fillRect(x + 21, y + 23, 4, 5);
    ctx.fillStyle = '#ff9900'; ctx.fillRect(x + 9, y + 9, 6, 6); ctx.fillRect(x + 21, y + 9, 6, 6);
    ctx.fillStyle = '#fff'; ctx.fillRect(x + 10, y + 10, 3, 3); ctx.fillRect(x + 22, y + 10, 3, 3);
}

// ── Geschosse ─────────────────────────────────────────────────────────────────
function drawBullet(b) {
    if (b.fp) {
        ctx.fillStyle = '#ffee44';
        ctx.fillRect(b.x, b.y, b.w, b.h);
        ctx.fillStyle = 'rgba(255,200,0,0.4)';
        ctx.fillRect(b.x - 1, b.y + b.h - 4, b.w + 2, 4);
    } else {
        ctx.fillStyle = '#ff4444';
        ctx.fillRect(b.x, b.y, b.w, b.h);
        ctx.fillStyle = 'rgba(255,80,80,0.4)';
        ctx.fillRect(b.x - 1, b.y, b.w + 2, 4);
    }
}

// ── Munitions-Drop (cyan) ─────────────────────────────────────────────────────
function drawAmmoDrop(d) {
    const cx = d.x + d.w / 2, cy = d.y + d.h / 2;
    ctx.beginPath();
    ctx.moveTo(cx, d.y); ctx.lineTo(d.x + d.w, cy);
    ctx.lineTo(cx, d.y + d.h); ctx.lineTo(d.x, cy);
    ctx.closePath();
    ctx.fillStyle = '#00cccc'; ctx.fill();
    ctx.strokeStyle = '#00ffff'; ctx.lineWidth = 1.5; ctx.stroke();
    ctx.fillStyle = '#fff';
    ctx.font = 'bold 10px Courier New'; ctx.textAlign = 'center';
    ctx.fillText('+', cx, cy + 4); ctx.textAlign = 'left';
}

// ── Upgrade-Drop (gold) ───────────────────────────────────────────────────────
function drawUpgradePickup(u) {
    const cx = u.x + u.w / 2, cy = u.y + u.h / 2;
    const r  = u.w / 2 - 1;

    // Glow-Aura
    ctx.fillStyle = 'rgba(255,200,0,0.18)';
    ctx.beginPath();
    ctx.moveTo(cx, cy - r - 5); ctx.lineTo(cx + r + 5, cy);
    ctx.lineTo(cx, cy + r + 5); ctx.lineTo(cx - r - 5, cy);
    ctx.closePath(); ctx.fill();

    // Haupt-Diamant
    ctx.fillStyle = '#ffc800';
    ctx.beginPath();
    ctx.moveTo(cx, cy - r); ctx.lineTo(cx + r, cy);
    ctx.lineTo(cx, cy + r); ctx.lineTo(cx - r, cy);
    ctx.closePath(); ctx.fill();

    // Glanzeffekt
    ctx.fillStyle = 'rgba(255,245,160,0.65)';
    ctx.beginPath();
    ctx.moveTo(cx, cy - r); ctx.lineTo(cx + r / 2, cy - r / 3); ctx.lineTo(cx, cy);
    ctx.closePath(); ctx.fill();

    // Rahmen
    ctx.strokeStyle = '#cc8800'; ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(cx, cy - r); ctx.lineTo(cx + r, cy);
    ctx.lineTo(cx, cy + r); ctx.lineTo(cx - r, cy);
    ctx.closePath(); ctx.stroke();

    // Label
    ctx.font = 'bold 8px Courier New'; ctx.fillStyle = '#000';
    ctx.textAlign = 'center'; ctx.fillText('UP', cx, cy + 3); ctx.textAlign = 'left';
}

// ── HUD ───────────────────────────────────────────────────────────────────────
function drawHUD(d) {
    ctx.font = 'bold 13px "Courier New"';
    ctx.fillStyle = '#00dd66';
    ctx.fillText(`SCORE: ${d.score}`, 10, 22);

    const waveStr = `WELLE ${d.wave}`;
    ctx.fillText(waveStr, (900 - ctx.measureText(waveStr).width) / 2, 22);

    ctx.fillStyle = '#88bb88';
    const nameStr = (d.playerName || '').toUpperCase();
    ctx.fillText(nameStr, 900 - ctx.measureText(nameStr).width - 10, 20);

    ctx.fillStyle = '#dd4444';
    let hearts = 'LEBEN: ';
    for (let i = 0; i < d.lives; i++) hearts += '♥ ';
    ctx.fillText(hearts, 900 - ctx.measureText(hearts).width - 10, 40);

    // Munition
    const p = d.player;
    ctx.fillStyle = p && p.ammo === 0 ? '#dd4444' : '#00cccc';
    ctx.fillText(`AMMO: ${p ? p.ammo : 0}`, 10, 42);
    if (p && p.ammo === 0) {
        ctx.fillStyle = '#dd4444';
        ctx.fillText(' !! KEINE MUNITION !!', 100, 42);
    }

    // Schild-Anzeige
    if (p && p.shieldHits > 0) {
        ctx.fillStyle = '#4488ff';
        ctx.fillText(`  SH:${p.shieldHits}`, 160, 42);
    }
    // Geistermodus
    if (p && p.ghostTicks > 0) {
        ctx.fillStyle = 'rgba(140,160,255,0.8)';
        ctx.font = '11px "Courier New"';
        ctx.fillText('[GHOST]', 10, 54);
    }
}

// ── Upgrade-Auswahl Overlay ───────────────────────────────────────────────────
function drawUpgradeSelect(d) {
    // Dunkle Ebene
    ctx.fillStyle = 'rgba(0,0,0,0.82)';
    ctx.fillRect(0, 0, 900, 600);

    // Titel
    ctx.font = 'bold 28px "Courier New"';
    ctx.fillStyle = '#00ff88';
    const title = `WELLE ${d.wave} ABGESCHLOSSEN!`;
    ctx.fillText(title, (900 - ctx.measureText(title).width) / 2, 88);

    // Wellen-Bonus
    if (d.lastWaveBonus > 0) {
        ctx.font = '14px "Courier New"';
        ctx.fillStyle = '#ffcc00';
        const bonus = `+${d.lastWaveBonus} Wellen-Bonus!`;
        ctx.fillText(bonus, (900 - ctx.measureText(bonus).width) / 2, 116);
    }

    // Untertitel
    ctx.font = 'bold 16px "Courier New"';
    ctx.fillStyle = '#99ccff';
    const sub = `Wähle ein Upgrade für Welle ${d.wave + 1}:`;
    ctx.fillText(sub, (900 - ctx.measureText(sub).width) / 2, 152);

    ctx.font = '11px "Courier New"';
    ctx.fillStyle = '#446644';
    const hint = 'Klick auf eine Karte  oder  [ 1 ] [ 2 ] [ 3 ]';
    ctx.fillText(hint, (900 - ctx.measureText(hint).width) / 2, 172);

    // 3 Upgrade-Karten
    if (!d.upgradeChoices) return;
    d.upgradeChoices.forEach((choice, i) => {
        if (i < UPGRADE_CARDS.length) {
            drawUpgradeCard(choice, UPGRADE_CARDS[i], i + 1);
        }
    });
}

function drawUpgradeCard(choice, card, num) {
    const { x, y, w, h } = card;
    const hover = mousePos.x >= x && mousePos.x <= x + w && mousePos.y >= y && mousePos.y <= y + h;
    const col   = choice.color || '#00ccaa';

    // Hex-Farbe → RGB-Teile
    const r = parseInt(col.slice(1, 3), 16);
    const g = parseInt(col.slice(3, 5), 16);
    const b = parseInt(col.slice(5, 7), 16);

    // Hintergrund
    ctx.fillStyle = `rgba(${r},${g},${b},${hover ? 0.18 : 0.1})`;
    roundRect(ctx, x, y, w, h, 8); ctx.fill();

    // Rahmen
    ctx.strokeStyle = hover ? col : `rgba(${r},${g},${b},0.55)`;
    ctx.lineWidth = hover ? 2.5 : 1.5;
    roundRect(ctx, x, y, w, h, 8); ctx.stroke();

    // Nummer-Badge
    ctx.fillStyle = col;
    roundRect(ctx, x + 8, y + 8, 26, 26, 5); ctx.fill();
    ctx.fillStyle = '#000';
    ctx.font = 'bold 15px "Courier New"';
    ctx.fillText(String(num), x + 14, y + 27);

    // Upgrade-Name
    ctx.font = 'bold 13px "Courier New"';
    ctx.fillStyle = hover ? '#fff' : col;
    const name = choice.name || '';
    ctx.fillText(name, x + (w - ctx.measureText(name).width) / 2, y + 68);

    // Trennlinie
    ctx.strokeStyle = `rgba(${r},${g},${b},0.25)`;
    ctx.lineWidth = 1;
    ctx.beginPath(); ctx.moveTo(x + 15, y + 78); ctx.lineTo(x + w - 15, y + 78); ctx.stroke();

    // Beschreibung (Zeilenumbruch)
    ctx.font = '11px "Courier New"';
    ctx.fillStyle = '#cccccc';
    const words = (choice.desc || '').split(' ');
    let line = '', lineY = y + 100;
    words.forEach(word => {
        const test = line ? line + ' ' + word : word;
        if (ctx.measureText(test).width > w - 20 && line) {
            ctx.fillText(line, x + 10, lineY);
            line = word; lineY += 16;
        } else { line = test; }
    });
    if (line && lineY <= y + h - 28) ctx.fillText(line, x + 10, lineY);

    // Auswählen-Hinweis
    ctx.font = '10px "Courier New"';
    ctx.fillStyle = hover ? col : `rgba(${r},${g},${b},0.55)`;
    const selHint = `[ ${num} ] Auswählen`;
    ctx.fillText(selHint, x + (w - ctx.measureText(selHint).width) / 2, y + h - 12);
}

// Canvas roundRect Polyfill
function roundRect(ctx, x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.lineTo(x + w - r, y);   ctx.quadraticCurveTo(x + w, y,     x + w, y + r);
    ctx.lineTo(x + w, y + h - r); ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
    ctx.lineTo(x + r, y + h);   ctx.quadraticCurveTo(x, y + h,     x, y + h - r);
    ctx.lineTo(x, y + r);       ctx.quadraticCurveTo(x, y,         x + r, y);
    ctx.closePath();
}

// ── Pause-Overlay ─────────────────────────────────────────────────────────────
function drawPaused(d) {
    ctx.fillStyle = 'rgba(0,0,0,0.72)';
    ctx.fillRect(0, 0, 900, 600);

    ctx.font = 'bold 42px "Courier New"';
    ctx.fillStyle = '#00ff88';
    const title = 'PAUSIERT';
    ctx.fillText(title, (900 - ctx.measureText(title).width) / 2, 270);

    ctx.font = '16px "Courier New"';
    ctx.fillStyle = '#99ccaa';
    const sub = `Score: ${d.score}  |  Welle ${d.wave}  |  Ammo: ${d.ammo}`;
    ctx.fillText(sub, (900 - ctx.measureText(sub).width) / 2, 310);

    ctx.font = '14px "Courier New"';
    ctx.fillStyle = '#669966';
    const hint = '[ ESC / P / ENTER / LEERTASTE ] Weiterspielen';
    ctx.fillText(hint, (900 - ctx.measureText(hint).width) / 2, 350);
}

// ── Game-Over Overlay ─────────────────────────────────────────────────────────
function drawGameOver(d) {
    ctx.fillStyle = 'rgba(0,0,0,0.78)';
    ctx.fillRect(0, 0, 900, 600);

    ctx.font = 'bold 40px "Courier New"';
    const headline = d.lives > 0 ? 'GEWONNEN!' : 'GAME OVER';
    const hColor   = d.lives > 0 ? '#00ff88' : '#ff3322';
    // Neon-Glow
    for (let dd = 4; dd >= 1; dd--) {
        const alpha = 0.04 * (5 - dd);
        ctx.fillStyle = d.lives > 0 ? `rgba(0,255,136,${alpha})` : `rgba(255,51,34,${alpha})`;
        ctx.fillText(headline, (900 - ctx.measureText(headline).width) / 2 - dd, 260 - dd);
    }
    ctx.fillStyle = hColor;
    ctx.fillText(headline, (900 - ctx.measureText(headline).width) / 2, 260);

    ctx.font = 'bold 22px "Courier New"';
    ctx.fillStyle = '#ffffff';
    const sc = `SCORE: ${d.score}  |  Welle ${d.wave}`;
    ctx.fillText(sc, (900 - ctx.measureText(sc).width) / 2, 310);

    ctx.font = '14px "Courier New"';
    ctx.fillStyle = '#669966';
    const hint = '[ ENTER / Klick ] Neues Spiel';
    ctx.fillText(hint, (900 - ctx.measureText(hint).width) / 2, 360);

    // Bestenliste nach kurzer Verzögerung anzeigen
    if (!state._lbSaved) {
        state._lbSaved = true;
        setTimeout(() => {
            if (state.screen === 'game') {
                loadLeaderboard();
            }
        }, 1500);
    }
}

function drawWaiting() {
    ctx.fillStyle = 'rgba(0,0,0,0.6)';
    ctx.fillRect(0, 0, 900, 600);
    ctx.font = 'bold 20px "Courier New"';
    ctx.fillStyle = '#00aa55';
    const t = 'Verbindung wird hergestellt…';
    ctx.fillText(t, (900 - ctx.measureText(t).width) / 2, 300);
}

// ── Canvas-Klick ──────────────────────────────────────────────────────────────
canvas.addEventListener('click', e => {
    const rect   = canvas.getBoundingClientRect();
    const scaleX = 900 / rect.width;
    const scaleY = 600 / rect.height;
    const cx = (e.clientX - rect.left) * scaleX;
    const cy = (e.clientY - rect.top)  * scaleY;

    // Upgrade-Karte klicken
    if (state.gameData && state.gameData.phase === 'UPGRADE_SELECT') {
        const choices = state.gameData.upgradeChoices;
        UPGRADE_CARDS.forEach((card, i) => {
            if (cx >= card.x && cx <= card.x + card.w &&
                cy >= card.y && cy <= card.y + card.h) {
                if (choices && choices[i]) {
                    sendWS({ type: 'SELECT_UPGRADE', upgrade: choices[i].key });
                }
            }
        });
        return;
    }

    // Game Over → zurück zum Start
    if (state.gameData && state.gameData.phase === 'GAME_OVER') {
        state._lbSaved = false;
        showScreen('start');
        return;
    }

    // Pause → Fortsetzen per Klick
    if (state.gameData && state.gameData.phase === 'PAUSED') {
        resumeGame();
    }
});

// ── Globale Keyboard-Shortcuts (Game Over) ────────────────────────────────────
document.addEventListener('keydown', e => {
    if (e.key === 'Enter' && state.screen === 'game'
        && state.gameData && state.gameData.phase === 'GAME_OVER') {
        state._lbSaved = false;
        showScreen('start');
    }
});

// ── Bestenliste laden ─────────────────────────────────────────────────────────
async function loadLeaderboard() {
    lbBody.innerHTML = '<tr><td colspan="5" style="color:#336644;text-align:center">Lädt…</td></tr>';
    try {
        const res  = await fetch('/api/leaderboard');
        const data = await res.json();
        if (data.length === 0) {
            lbBody.innerHTML = '<tr><td colspan="5" style="color:#336644;text-align:center">Noch keine Einträge</td></tr>';
            return;
        }
        lbBody.innerHTML = data.map((row, i) => {
            const rankClass = i === 0 ? 'rank-gold' : i === 1 ? 'rank-silver' : i === 2 ? 'rank-bronze' : '';
            const medal     = i === 0 ? '1.' : i === 1 ? '2.' : i === 2 ? '3.' : `${i + 1}.`;
            const diff      = { EASY: 'Leicht', MEDIUM: 'Mittel', HARD: 'Schwer' }[row.DIFFICULTY] || row.DIFFICULTY;
            const date      = row.PLAYED_AT ? new Date(row.PLAYED_AT).toLocaleDateString('de-DE') : '—';
            return `<tr>
                <td class="${rankClass}">${medal}</td>
                <td class="${rankClass}">${escHtml(row.PLAYER_NAME)}</td>
                <td class="${rankClass}" style="font-weight:bold">${row.SCORE}</td>
                <td>${diff}</td>
                <td style="color:#669966">${date}</td>
            </tr>`;
        }).join('');
    } catch {
        lbBody.innerHTML = '<tr><td colspan="5" style="color:#aa3322;text-align:center">Fehler beim Laden</td></tr>';
    }
}

function escHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
