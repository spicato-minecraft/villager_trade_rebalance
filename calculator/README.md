# Village ledger lab

Static HTML app in **villager_trade_rebalance**: living-markets **capacity / flow** lab plus gather-time fair emerald **reference**.

## Quick start

From this directory:

```bash
python3 serve.py
```

Open [http://127.0.0.1:8080/](http://127.0.0.1:8080/)

- Town / profession / ledger / scenario / invariants auto-save to `lab.json`
- Gather tests auto-save to `calibration.json`

```bash
node ledger.test.js
```

##     Primary surface (ledger)

The ledger stays on the left; **Town** and **Profession** configs sit on the right. Drag the divider to resize. Caps:

`cap(c) = cap_per_trader(c) × traders_of(c) + cap_per_mouth(c) × total_villagers`

Idle fill is midpoint (`cap / 2`). Rates are **items per villager per Minecraft day**.

Two loops:

| Loop | When | Effect |
|------|------|--------|
| Raw produce | stock below mid, need off | fill raw |
| Raw consume | stock above mid | drain raw |
| Crafted restock | finished below mid and raw above floor | fill finished, drain BOM |

**Export mod knobs** downloads `living-markets-knobs.json` (`pulses_per_day`, `cap_per_*`, produce/consume, restock rates).

## Tabs

1. **Lab** — split view: ledger (supply stock) | Town / Professions config tabs
2. **Scenario** — dump, shop, need (pause produce), tick days (secondary menu)
3. **Invariants** — village drain vs repair burn vs mining session
4. **Gather tests / Derivatives / Fair trades** — obtainment-cost overlay (secondary menu)

## Regenerating data

```bash
python3 generate_model.py   # gather tests + craftable trade list
node generate_bom.js        # bom.json from lab-defaults.js offer pools
node generate_items.js      # items-registry.js from Prismarine Java 26.1
```

## Files

| File | Role |
|------|------|
| `index.html` | Lab + reference UI |
| `ledger.js` | Cap, midpoint, tick, dump/shop/need (no DOM) |
| `lab-defaults.js` | Profession N, P/C rates, empty offer pools, town presets |
| `items-registry.js` | All Java item ids + display names |
| `lab.js` | Lab UI |
| `bom.json` | Snapshot of current default pools (empty until you add offers) |
| `ledger.test.js` | Node tests for ledger math |
| `materials.js` | `GATHER_TESTS` + `DERIVED_MATERIALS` |
| `recipes.js` | Trade recipes + `MATERIAL_RECIPES` |
| `trades.js` | Craftable trade list |
| `calibration.json` | Gather-test calibration |
| `lab.json` | Saved town (created on first save) |
| `generate_model.py` | Build gather/trade JS |
| `serve.py` | Local server + PUT persistence |

## Gather-time overlay (reference)

You only run **narrow gather tests** on base materials. Fair buy emeralds = gather minutes / stick-anchor batch. **32 sticks = 1 emerald**.

See `calibration.json` v2 schema in git history or existing file. Markup on the Town tab scales that overlay; you still balance with **cap and rates**, not by typing emerald costs on the ledger.
