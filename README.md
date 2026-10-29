# Villager Trade Rebalance

Reprices villager trades so emerald costs reflect how hard items are to obtain elsewhere in the modpack.

**Status:** Librarian enchanted-book slice is first. Cartographer undecided treasure-map lanes are second. Emerald obtainment pricing for all professions is still not done. Use `calculator/` for the village ledger lab (capacity/flow) and gather-time overlay.

## Compatibility

- **Minecraft:** 26.3
- **Fabric Loader:** ≥ 0.19.5
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

## Cartographer maps (v2)

Cartographers sell **undecided treasure maps** instead of a fixed destination. Three lanes: **Apprentice T1**, **Journeyman T2**, **Master T3**. Expert has no new lane; search radii stay on the Journeyman offer.

The stored offer keeps `custom_data` `vtr_map_lane` and **no** `map_id`. The destination is chosen **on purchase** with vanilla locate. Twelve uses means twelve new locates. After locate, the delivered map's lore shows **Tier N** and the world map id (`#12`) so same-tier copies can be told apart.

Newest-lane radius is **b** (config default `100`, vanilla `search_radius` units). The previous lane uses **2b**; the next-older lane uses **4b**.

- **T1:** 26.3 apprentice map types, with vanilla villager-variant gates.
- **T2:** ocean monument, trial chambers, and `#ruined_portal_rebalance:paired_portals`.
- **T3:** woodland mansion.

Camp-loot explorer maps are not sold. Set `cartographer_maps.enabled` to `false` to restore vanilla maps. Existing offers that already have `map_id` are grandfathered onto the vanilla pool.

Compatible with **locked_villager_trades**: mixin priority 50, and `updateTrades` is never cancelled. Identical cartographer menus across LVT preview sets is intended.

GameTest **G21** (LVT dual-mod lock/preview) is deferred until `locked_villager_trades` is a GameTest dependency.

## Design

Tracked in Notion under Epic: Villager trade rebalance — librarian book rules and the cartographer undecided treasure-map plan.

## JDK 25

Minecraft 26.3 mods require **JDK 25** to compile and run. This project uses the [Foojay toolchain resolver](https://github.com/gradle/foojay-toolchains) in `settings.gradle` so Gradle can download JDK 25 automatically when it is not installed locally.

If you already have JDK 25, set `JAVA_HOME` to that installation before running Gradle.
