<h1 align="center">🌻 Plants vs. Zombies 2 — Java Edition 🧟</h1>

<p align="center">
  <em>A full-blown, LibGDX-powered remake of Plants vs. Zombies 2 — adventure chapters, five mini-games, animated brains, and a server-authoritative online <strong>I, Zombie</strong> duel.</em>
</p>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white">
  <img alt="LibGDX" src="https://img.shields.io/badge/LibGDX-1.12.1-red?logo=libgdx&logoColor=white">
  <img alt="Gradle" src="https://img.shields.io/badge/Gradle-8.14.3-02303A?logo=gradle&logoColor=white">
  <img alt="Networking" src="https://img.shields.io/badge/Multiplayer-TCP%20%2B%20JSON-4c1?logo=socketdotio&logoColor=white">
  <img alt="Course" src="https://img.shields.io/badge/Sharif%20University-Advanced%20Programming-1f6feb">
</p>

---

## 📖 Overview

The lawn has gone through time. Zombies from Ancient Egypt, the Ice Age, and the Dark Ages are marching for your brains, and your only defense is a garden of plants with wildly unfair superpowers.

This project is a from-scratch Java re-creation of **Plants vs. Zombies 2**, built over three phases: a full single-player campaign, a suite of classic mini-games, and a **client-server online mode** where two players face off in a real-time *I, Zombie* battle — one commands the plants, the other unleashes the horde.

---

## 👥 Team — Group 51

| Contributor | Student ID |
|---|---|
| [Ronak Aboutalebi](https://github.com/roab7) | 404105394 |
| [Reyhaneh Arabi](https://github.com/reyarabi) | 404106099 |
| [Reyhaneh Mohseni](https://github.com/ryhn86m) | 404106309 |

<sub>Advanced Programming — Computer Engineering Department, Sharif University of Technology.</sub>

---

## ✨ Highlights

- 🗺️ **Adventure mode** across themed worlds — Ancient Egypt, Ice Age, and the Dark Ages, each with its own zombies, hazards, and mechanics (graves, necromancy tiles, sun-stealing pharaohs, and more).
- 🎪 **Five mini-games** — Vasebreaker, Wall-nut Bowling, I&nbsp;Zombie, Beghouled, and Zombotany.
- 🧟‍♂️ **Server-authoritative online duel** — a 2-player *I, Zombie* match where the server is the single source of truth and both clients render the exact same world, tick for tick.
- 🌱 **Deep progression** — a greenhouse, a shop with daily offers, plant collection & upgrades, quests, news, and a server-backed leaderboard.
- 🎞️ **Real PvZ2 animations** — skeletal PAM animations driven at runtime, with per-state clips for walking, eating, freezing, spinning, and dying.
- 💾 **Persistent accounts** — register, log in from any machine, and find your coins, progress, and unlocked plants waiting for you, all stored server-side.

---

## 🎮 Game Modes

### 🏛️ Adventure
Fight through chapters of increasing difficulty. Plant sunflowers, build defenses, manage sun, and survive escalating waves — including flag waves and gargantuar finales that literally shake the screen.

### 🕹️ Mini-Games
| Mini-game | The twist |
|---|---|
| **Vasebreaker** | Smash vases to reveal plants… and the zombies hiding inside. |
| **Wall-nut Bowling** | Roll wall-nuts down the lanes to bowl over the undead. |
| **I, Zombie** | Flip the script — *you* are the zombies, eating your way to the brains. |
| **Beghouled** | Match-3 meets lawn defense. |
| **Zombotany** | Zombies wearing plant heads. Exactly as unsettling as it sounds. |

### 🌐 Online — *I, Zombie* (2 Players)
Two players connect to the server and battle head-to-head:

- 🌱 **The Plant player** defends the brains.
- 🧟 **The Zombie player** places the horde and marches for the brains.
- ⏱️ **The server runs the match** — a fixed-rate authoritative tick loop validates every action (sun, cooldown, tile, role), advances the world, and broadcasts an identical snapshot to both clients.
- 🏆 **Win conditions** differ by side: eat every brain to win as the zombies, or hold the line until the timer runs out to win as the plants.

---

## 🛠️ Tech Stack

| Layer | Tools |
|---|---|
| **Language** | Java 21 |
| **Build** | Gradle 8.14.3 (wrapper included) |
| **Rendering** | LibGDX 1.12.1 (LWJGL3 desktop backend, FreeType) |
| **Animation** | libPVZ (PAM skeletal animations) + pvz-skin |
| **Networking** | Raw TCP sockets, line-delimited JSON, Jackson |
| **Persistence** | SQLite (xerial JDBC) on the server |
| **Utilities** | Gson, Apache Commons CSV, TenPatch, Lombok |

---

## 🚀 Getting Started

### Prerequisites
- **JDK 21** (a Gradle toolchain is configured — the wrapper handles Gradle itself).

### ▶️ Play single-player
```bash
./gradlew run
```

### 🌐 Play online (I, Zombie)
Online mode is **server-authoritative**, so start the server first, then launch a client for each player:

```bash
# Terminal 1 — start the game server (defaults to port 5050)
./gradlew runServer

# Terminal 2 (and a second machine/instance) — launch the game and log in
./gradlew run
```

Then register or log in, queue up (or challenge a friend by username), and duel.

> 💡 On Windows, use `gradlew.bat` instead of `./gradlew`.

---

## 🗂️ Project Structure

```
src/main/java/
├── Main.java                 # Desktop entry point
├── controllers/              # Menu & gameplay controllers
├── models/                   # Core game logic (no rendering)
│   ├── Plant/  Zombie/  Board/
│   ├── games/                # Game loop, GameState, chapters
│   ├── projectile/  quests/  greenHouse/
│   └── minigames/            # Vasebreaker, I-Zombie, Beghouled, …
│       └── iZombie/multiplayer/   # Online match engine
├── views/                    # LibGDX rendering & UI
│   └── graphical/            # Screens, HUD, animation systems
├── network/                  # Client-server layer
│   ├── protocol/             # Message types & DTOs (JSON)
│   ├── server/               # GameServer, router, match, matchmaking
│   └── client/               # NetworkClient & client services
└── Data/                     # Database & asset loaders
src/main/resources/           # plants.json, zombies.json, skins, assets
```

---

## 🧩 Development Phases

| Phase | Focus |
|---|---|
| **Phase 1** | Core game logic — plants, zombies, board, waves, win/lose conditions. |
| **Phase 2** | Graphical client — LibGDX rendering, animations, menus, mini-games, and full single-player polish. |
| **Phase 3** | Going online — a client-server architecture with accounts, matchmaking, a leaderboard, in-match reactions, and the server-authoritative *I, Zombie* duel. |

---

<p align="center"><sub>Built with ☕ Java, 🎮 LibGDX, and an unreasonable number of zombies.</sub></p>
