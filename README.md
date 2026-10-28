# Villager Trade Rebalance

Reprices villager trades so emerald costs reflect how hard items are to obtain elsewhere in the modpack.

**Status:** Librarian enchanted-book slice is implemented. Other professions still use vanilla prices. Use `calculator/` for the village ledger lab (capacity/flow) and gather-time overlay.

## Compatibility

- **Minecraft:** 26.2
- **Fabric Loader:** ≥ 0.19.3
- **Fabric API:** Required
- **Java:** ≥ 25

## Build & test

```bash
./gradlew test
./gradlew runGameTest
./gradlew build
./gradlew runClient
```

## Economy calculator

```bash
cd calculator && python3 serve.py
```

Open http://127.0.0.1:8080/ — edits auto-save to `calculator/calibration.json`. See `calculator/README.md`.

## Librarian books (v1)

New librarians roll one allowlisted enchant at Level I. Each career level-up offers the next level of **that same enchant**. When the enchant is maxed, later levels skip the book slot. Fortune, Looting, Mending, Sharpness, Protection, Power, Infinity, curses, Wind Burst, fishing books, and **structure-loot exclusives** (Respiration, Depth Strider, Swift Sneak, Soul Speed) are blocked. Feather Falling is available from librarians.

Base book cost is 48–64 emeralds. Hero of the Village and demand discounts still apply. Villagers that already had trades when the mod is first loaded are grandfathered onto the vanilla pool.

Biome-specific librarian tables are not used in this version.

## Design

Tracked in Notion: **Librarian enchanted book trade rules** (under Epic: Villager trade rebalance)

## JDK 25

Minecraft 26.2 mods require **JDK 25** to compile and run. This project uses the [Foojay toolchain resolver](https://github.com/gradle/foojay-toolchains) in `settings.gradle` so Gradle can download JDK 25 automatically when it is not installed locally.

If you already have JDK 25, set `JAVA_HOME` to that installation before running Gradle.
