#!/usr/bin/env python3
"""Build materials-first trade model: gatherable materials + craftable trades only."""

from __future__ import annotations

import json
import re
from pathlib import Path

DIR = Path(__file__).parent

# Material crafting/smelting: inputs per batch → output count (for derivative pricing).
# Gather tests target base leaves only (e.g. sand); glass/panes/bottles derive from these.
MATERIAL_RECIPES: dict[str, dict] = {
    "planks": {"inputs": {"log": 1}, "output": 4},
    "stick": {"inputs": {"planks": 2}, "output": 4},
    "fuel": {"inputs": {"coal": 1}, "output": 8},
    "glass_block": {"inputs": {"sand": 1, "fuel": 1}, "output": 1},
    "glass_pane": {"inputs": {"glass_block": 6}, "output": 16},
    "glass_bottle": {"inputs": {"glass_block": 3}, "output": 3},
    "paper": {"inputs": {"sugar_cane": 3}, "output": 3},
    "sugar": {"inputs": {"sugar_cane": 1}, "output": 1},
    "book": {"inputs": {"paper": 3, "leather": 1}, "output": 1},
    "writable_book": {"inputs": {"book": 1, "ink_sac": 1, "feather": 1}, "output": 1},
    "iron_nugget": {"inputs": {"iron_ingot": 1}, "output": 9},
    "gold_nugget": {"inputs": {"gold_ingot": 1}, "output": 9},
    "bucket": {"inputs": {"iron_ingot": 3}, "output": 1},
    "bowl": {"inputs": {"planks": 3}, "output": 4},
    "torch": {"inputs": {"coal": 1, "stick": 1}, "output": 4},
    "stone_bricks": {"inputs": {"stone": 1}, "output": 1},
    "brick": {"inputs": {"clay_ball": 1, "fuel": 1}, "output": 1},
    "flint": {"inputs": {"gravel": 10}, "output": 1},
    "baked_potato": {"inputs": {"potato": 1, "fuel": 1}, "output": 1},
    "cooked_rabbit": {"inputs": {"rabbit": 1, "fuel": 1}, "output": 1},
    "compass": {"inputs": {"iron_ingot": 4, "redstone": 1}, "output": 1},
    "tripwire_hook": {"inputs": {"iron_ingot": 1, "stick": 1, "planks": 1}, "output": 2},
    "quartz_block": {"inputs": {"quartz": 4}, "output": 1},
    "oak_boat": {"inputs": {"planks": 5}, "output": 1},
    "spruce_boat": {"inputs": {"planks": 5}, "output": 1},
    "birch_boat": {"inputs": {"planks": 5}, "output": 1},
    "jungle_boat": {"inputs": {"planks": 5}, "output": 1},
    "acacia_boat": {"inputs": {"planks": 5}, "output": 1},
    "dark_oak_boat": {"inputs": {"planks": 5}, "output": 1},
    "cherry_boat": {"inputs": {"planks": 5}, "output": 1},
    "mangrove_boat": {"inputs": {"planks": 5}, "output": 1},
    "bamboo_raft": {"inputs": {"bamboo": 5}, "output": 1},
}

RECIPES: dict[str, dict[str, int]] = {
    "iron_helmet": {"iron_ingot": 5},
    "iron_chestplate": {"iron_ingot": 8},
    "iron_leggings": {"iron_ingot": 7},
    "iron_boots": {"iron_ingot": 4},
    "iron_axe": {"iron_ingot": 3, "stick": 2},
    "shield": {"iron_ingot": 1, "planks": 6},
    "stone_axe": {"cobblestone": 3, "stick": 2},
    "stone_shovel": {"cobblestone": 1, "stick": 2},
    "stone_pickaxe": {"cobblestone": 3, "stick": 2},
    "stone_hoe": {"cobblestone": 2, "stick": 2},
    "diamond_hoe": {"diamond": 2, "stick": 2},
    "diamond_pickaxe": {"diamond": 3, "stick": 2},
    "bow": {"stick": 3, "string": 3},
    "crossbow": {"stick": 3, "string": 2, "iron_ingot": 1, "tripwire_hook": 1},
    "arrow": {"flint": 1, "stick": 1, "feather": 1},
    "tripwire_hook": {"iron_ingot": 1, "stick": 1, "planks": 1},
    "bread": {"wheat": 3},
    "cookie": {"wheat": 2, "cocoa_beans": 1},
    "pumpkin_pie": {"pumpkin": 1, "egg": 1, "sugar": 1},
    "cake": {"milk_bucket": 3, "sugar": 2, "egg": 1, "wheat": 3},
    "rabbit_stew": {"cooked_rabbit": 1, "carrot": 1, "baked_potato": 1, "bowl": 1},
    "golden_carrot": {"gold_nugget": 8, "carrot": 1},
    "glistering_melon_slice": {"gold_nugget": 8, "melon_slice": 1},
    "bookshelf": {"planks": 6, "book": 3},
    "book": {"paper": 3, "leather": 1},
    "paper": {"sugar_cane": 3},
    "lantern": {"iron_nugget": 8, "torch": 1},
    "glass": {"glass_block": 1},
    "compass": {"iron_ingot": 4, "redstone": 1},
    "clock": {"gold_ingot": 4, "redstone": 1},
    "red_candle": {"candle": 1, "red_dye": 1},
    "yellow_candle": {"candle": 1, "yellow_dye": 1},
    "empty_map": {"paper": 8, "compass": 1},
    "item_frame": {"stick": 8, "leather": 1},
    "campfire": {"coal": 3, "stick": 3, "log": 3},
    "shears": {"iron_ingot": 2},
    "painting": {"stick": 8, "wool": 1},
    "brick": {"clay_ball": 1},
    "chiseled_stone_bricks": {"stone_bricks": 1},
    "stone_bricks": {"stone": 1},
    "dripstone_block": {"pointed_dripstone": 4},
    "polished_andesite": {"andesite": 4},
    "polished_diorite": {"diorite": 4},
    "polished_granite": {"granite": 4},
    "quartz_pillar": {"quartz_block": 1},
    "quartz_block": {"quartz": 4},
    "leather_helmet": {"leather": 5},
    "leather_chestplate": {"leather": 8},
    "leather_leggings": {"leather": 7},
    "leather_boots": {"leather": 4},
    "leather_horse_armor": {"leather": 7},
    "bell": {"gold_ingot": 3, "iron_ingot": 4},
    "wool": {"wool": 1},
    "carpet": {"wool": 2},
    "bed": {"wool": 3, "planks": 3},
    "banner": {"wool": 6, "stick": 1},
    "cooked_cod": {"cod": 1, "fuel": 1},
    "cooked_salmon": {"salmon": 1, "fuel": 1},
    "cooked_chicken": {"chicken": 1, "fuel": 1},
    "cooked_porkchop": {"porkchop": 1, "fuel": 1},
    "cod_bucket": {"bucket": 1, "cod": 1},
    "oak_boat": {"planks": 5},
    "spruce_boat": {"planks": 5},
    "jungle_boat": {"planks": 5},
    "acacia_boat": {"planks": 5},
    "dark_oak_boat": {"planks": 5},
    "mangrove_boat": {"planks": 5},
    "cherry_boat": {"planks": 5},
    "bamboo_raft": {"bamboo": 5},
    "redstone": {"redstone": 1},
    "lapis_lazuli": {"lapis_lazuli": 1},
    "glowstone": {"glowstone_dust": 4},
    "apple": {"apple": 1},
    "flint": {"gravel": 10},
}

MATERIAL_META: dict[str, dict] = {
    "sand": {"display_name": "Sand", "category": "mining", "default_qty": 32},
    "stick": {"display_name": "Stick", "category": "wood", "default_qty": 32, "anchor": True},
    "planks": {"display_name": "Planks", "category": "wood", "default_qty": 32, "derived": True},
    "log": {"display_name": "Log", "category": "wood", "default_qty": 32},
    "coal": {"display_name": "Coal", "category": "mining", "default_qty": 32},
    "iron_ingot": {"display_name": "Iron Ingot", "category": "mining", "default_qty": 32},
    "gold_ingot": {"display_name": "Gold Ingot", "category": "mining", "default_qty": 32},
    "gold_nugget": {"display_name": "Gold Nugget", "category": "mining", "default_qty": 32, "derived": True},
    "iron_nugget": {"display_name": "Iron Nugget", "category": "mining", "default_qty": 32, "derived": True},
    "diamond": {"display_name": "Diamond", "category": "mining", "default_qty": 16},
    "cobblestone": {"display_name": "Cobblestone", "category": "mining", "default_qty": 64},
    "stone": {"display_name": "Stone", "category": "mining", "default_qty": 64},
    "granite": {"display_name": "Granite", "category": "mining", "default_qty": 64},
    "andesite": {"display_name": "Andesite", "category": "mining", "default_qty": 64},
    "diorite": {"display_name": "Diorite", "category": "mining", "default_qty": 64},
    "nether_quartz": {"display_name": "Nether Quartz", "category": "nether", "default_qty": 32},
    "quartz": {"display_name": "Quartz", "category": "nether", "default_qty": 32},
    "redstone": {"display_name": "Redstone Dust", "category": "mining", "default_qty": 32},
    "lapis_lazuli": {"display_name": "Lapis Lazuli", "category": "mining", "default_qty": 32},
    "glowstone_dust": {"display_name": "Glowstone Dust", "category": "nether", "default_qty": 32},
    "wheat": {"display_name": "Wheat", "category": "farming", "default_qty": 32},
    "carrot": {"display_name": "Carrot", "category": "farming", "default_qty": 32},
    "potato": {"display_name": "Potato", "category": "farming", "default_qty": 32},
    "beetroot": {"display_name": "Beetroot", "category": "farming", "default_qty": 32},
    "pumpkin": {"display_name": "Pumpkin", "category": "farming", "default_qty": 16},
    "melon": {"display_name": "Melon", "category": "farming", "default_qty": 16},
    "melon_slice": {"display_name": "Melon Slice", "category": "farming", "default_qty": 32},
    "sweet_berries": {"display_name": "Sweet Berries", "category": "farming", "default_qty": 32},
    "nether_wart": {"display_name": "Nether Wart", "category": "nether", "default_qty": 32},
    "sugar_cane": {"display_name": "Sugar Cane", "category": "farming", "default_qty": 32},
    "paper": {"display_name": "Paper", "category": "crafted", "default_qty": 32, "derived": True},
    "leather": {"display_name": "Leather", "category": "animal_farm", "default_qty": 32},
    "rabbit_hide": {"display_name": "Rabbit Hide", "category": "animal_farm", "default_qty": 32},
    "turtle_scute": {"display_name": "Turtle Scute", "category": "farming", "default_qty": 16},
    "rotten_flesh": {"display_name": "Rotten Flesh", "category": "mob_hunt", "default_qty": 32},
    "string": {"display_name": "String", "category": "mob_hunt", "default_qty": 32},
    "feather": {"display_name": "Feather", "category": "animal_farm", "default_qty": 32},
    "flint": {"display_name": "Flint", "category": "mining", "default_qty": 32, "derived": True},
    "gravel": {"display_name": "Gravel", "category": "mining", "default_qty": 64},
    "clay_ball": {"display_name": "Clay Ball", "category": "mining", "default_qty": 32},
    "glass_pane": {"display_name": "Glass Pane", "category": "crafted", "default_qty": 32, "derived": True},
    "glass_bottle": {"display_name": "Glass Bottle", "category": "crafted", "default_qty": 32, "derived": True},
    "book": {"display_name": "Book", "category": "crafted", "default_qty": 16, "derived": True},
    "ink_sac": {"display_name": "Ink Sac", "category": "fishing", "default_qty": 32},
    "writable_book": {"display_name": "Book and Quill", "category": "crafted", "default_qty": 8, "derived": True},
    "lava_bucket": {"display_name": "Lava Bucket", "category": "nether", "default_qty": 1},
    "bucket": {"display_name": "Bucket", "category": "crafted", "default_qty": 1, "derived": True},
    "cod": {"display_name": "Raw Cod", "category": "fishing", "default_qty": 32},
    "salmon": {"display_name": "Raw Salmon", "category": "fishing", "default_qty": 32},
    "tropical_fish": {"display_name": "Tropical Fish", "category": "fishing", "default_qty": 16},
    "pufferfish": {"display_name": "Pufferfish", "category": "fishing", "default_qty": 16},
    "dried_kelp_block": {"display_name": "Dried Kelp Block", "category": "farming", "default_qty": 16},
    "beef": {"display_name": "Raw Beef", "category": "animal_farm", "default_qty": 32},
    "porkchop": {"display_name": "Raw Porkchop", "category": "animal_farm", "default_qty": 32},
    "chicken": {"display_name": "Raw Chicken", "category": "animal_farm", "default_qty": 32},
    "mutton": {"display_name": "Raw Mutton", "category": "animal_farm", "default_qty": 32},
    "rabbit": {"display_name": "Raw Rabbit", "category": "animal_farm", "default_qty": 32},
    "rabbit_foot": {"display_name": "Rabbit's Foot", "category": "animal_farm", "default_qty": 16},
    "wool": {"display_name": "Wool", "category": "animal_farm", "default_qty": 32},
    "fuel": {"display_name": "Fuel (smelt)", "category": "mining", "default_qty": 32, "derived": True},
    "egg": {"display_name": "Egg", "category": "animal_farm", "default_qty": 16},
    "sugar": {"display_name": "Sugar", "category": "crafted", "default_qty": 32, "derived": True},
    "cocoa_beans": {"display_name": "Cocoa Beans", "category": "farming", "default_qty": 32},
    "milk_bucket": {"display_name": "Milk Bucket", "category": "animal_farm", "default_qty": 8},
    "bowl": {"display_name": "Bowl", "category": "crafted", "default_qty": 32, "derived": True},
    "cooked_rabbit": {"display_name": "Cooked Rabbit", "category": "crafted", "default_qty": 32, "derived": True},
    "baked_potato": {"display_name": "Baked Potato", "category": "farming", "default_qty": 32, "derived": True},
    "candle": {"display_name": "Candle", "category": "crafted", "default_qty": 32},
    "torch": {"display_name": "Torch", "category": "crafted", "default_qty": 32, "derived": True},
    "glass_block": {"display_name": "Glass Block", "category": "crafted", "default_qty": 32, "derived": True},
    "bamboo": {"display_name": "Bamboo", "category": "farming", "default_qty": 32},
    "pointed_dripstone": {"display_name": "Pointed Dripstone", "category": "mining", "default_qty": 32},
    "stone_bricks": {"display_name": "Stone Bricks", "category": "crafted", "default_qty": 32, "derived": True},
    "apple": {"display_name": "Apple", "category": "farming", "default_qty": 32},
    "red_dye": {"display_name": "Red Dye", "category": "crafted", "default_qty": 32},
    "yellow_dye": {"display_name": "Yellow Dye", "category": "crafted", "default_qty": 32},
    "ender_pearl": {
        "display_name": "Ender Pearl",
        "category": "end",
        "default_qty": 16,
        "notes": "Manual only — End/enderman hunt; not mob-farm or overworld hunt model",
    },
}

# Standard pen: active cull/feed/breed work + fixed baby→adult growth (Java Edition = 20 min).
ANIMAL_FARM_ACTIVE_MIN = 2

ANIMAL_GROWTH_MIN: dict[str, float] = {
    "cow": 20,
    "pig": 20,
    "chicken": 20,
    "sheep": 20,
    "rabbit": 20,
}

# material_id -> (species, avg items per grow-kill-or-collect cycle)
ANIMAL_PRODUCT_YIELDS: dict[str, tuple[str, float]] = {
    "beef": ("cow", 2),
    "leather": ("cow", 1),
    "milk_bucket": ("cow", 1),
    "porkchop": ("pig", 2),
    "chicken": ("chicken", 1),
    "feather": ("chicken", 1),
    "egg": ("chicken", 1),
    "mutton": ("sheep", 1),
    "wool": ("sheep", 1),
    "rabbit": ("rabbit", 1),
    "rabbit_hide": ("rabbit", 1),
    "rabbit_foot": ("rabbit", 0.1),
}

ANIMAL_FARM_READY_WHEN = (
    "Standard animal pen; breed, feed, cull/collect cycle (derived time)"
)

# Dedicated hostile-mob hunting block (setup excluded); risk applied separately.
MOB_HUNT_ACTIVE_MIN = 6
MOB_HUNT_DEFAULT_RISK = 1.25

# material_id -> mob, avg drops per hunt cycle, risk multiplier (death/repair cost overlay)
MOB_HUNT_PRODUCTS: dict[str, dict] = {
    "rotten_flesh": {"mob": "zombie", "yield_per_cycle": 1, "risk_r": 1.25},
    "string": {"mob": "spider", "yield_per_cycle": 1, "risk_r": 1.25},
}

MOB_HUNT_READY_WHEN = (
    "Farmable mob spawner or darkroom ready; dedicated hunting session (derived time)"
)

WOOL_ALIASES = [
    "white_wool", "brown_wool", "black_wool", "gray_wool",
]
DYE_SELL = [
    "white_dye", "gray_dye", "black_dye", "light_blue_dye", "lime_dye",
    "yellow_dye", "light_gray_dye", "orange_dye", "red_dye", "pink_dye",
    "brown_dye", "purple_dye", "blue_dye", "green_dye", "magenta_dye", "cyan_dye",
]

NON_CRAFTABLE_BUY_PATTERNS = [re.compile(r"enchant", re.I), re.compile(r"random", re.I)]
NON_CRAFTABLE_BUY_IDS = {
    "filled_map", "saddle", "experience_bottle", "suspicious_stew",
    "tipped_arrow", "fishing_rod", "chainmail_helmet", "chainmail_chestplate",
    "chainmail_leggings", "chainmail_boots", "globe_banner_pattern", "name_tag",
}


def item_suffix(item_id: str) -> str:
    return item_id.replace("minecraft:", "")


def _color_recipe(suffix: str) -> dict[str, int] | None:
    colors = [
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black",
    ]
    for c in colors:
        if suffix == f"{c}_wool":
            return {"wool": 1}
        if suffix == f"{c}_carpet":
            return {"wool": 2}
        if suffix == f"{c}_bed":
            return {"wool": 3, "planks": 3}
        if suffix == f"{c}_banner":
            return {"wool": 6, "stick": 1}
        if suffix == f"{c}_terracotta":
            return {"clay_ball": 4}
        if suffix == f"{c}_glazed_terracotta":
            return {"clay_ball": 4}
    return None


def get_recipe(trade: dict) -> dict[str, int]:
    suffix = item_suffix(trade["item_id"])
    if trade["direction"] == "sell":
        mat = suffix
        if mat in WOOL_ALIASES:
            return {"wool": trade["quantity"]}
        if mat in DYE_SELL:
            return {mat: trade["quantity"]}
        return {mat: trade["quantity"]}
    if suffix in RECIPES:
        return dict(RECIPES[suffix])
    color = _color_recipe(suffix)
    if color:
        return color
    if suffix in MATERIAL_META:
        return {suffix: 1}
    return {}


def is_craftable_buy(trade: dict) -> bool:
    if trade["direction"] != "buy":
        return True
    suffix = item_suffix(trade["item_id"])
    if suffix in NON_CRAFTABLE_BUY_IDS:
        return False
    notes = trade.get("notes") or ""
    name = trade.get("display_name") or ""
    for pat in NON_CRAFTABLE_BUY_PATTERNS:
        if pat.search(name) or pat.search(notes):
            return False
    return bool(get_recipe(trade))


def load_legacy_trades() -> list[dict]:
    legacy_path = DIR / "trades_legacy_full.js"
    path = legacy_path if legacy_path.exists() else DIR / "trades.js"
    text = path.read_text(encoding="utf-8")
    start = text.index("[")
    end = text.rindex("];")
    return json.loads(text[start : end + 1])


def expand_material_to_bases(mat_id: str, qty: float = 1.0, seen: set[str] | None = None) -> dict[str, float]:
    """Expand a material through MATERIAL_RECIPES to base gather leaves."""
    if seen is None:
        seen = set()
    if mat_id in seen:
        return {}
    if mat_id in ANIMAL_PRODUCT_YIELDS or mat_id in MOB_HUNT_PRODUCTS:
        return {mat_id: qty}
    if mat_id in MATERIAL_RECIPES:
        seen = seen | {mat_id}
        recipe = MATERIAL_RECIPES[mat_id]
        scale = qty / recipe["output"]
        out: dict[str, float] = {}
        for ing, ing_qty in recipe["inputs"].items():
            for base_id, base_qty in expand_material_to_bases(ing, ing_qty * scale, seen).items():
                out[base_id] = out.get(base_id, 0) + base_qty
        return out
    return {mat_id: qty}


def expand_trade_to_bases(trade: dict) -> dict[str, float]:
    out: dict[str, float] = {}
    recipe = trade.get("recipe", get_recipe(trade))
    for mat_id, count in recipe.items():
        for base_id, base_qty in expand_material_to_bases(mat_id, float(count)).items():
            out[base_id] = out.get(base_id, 0) + base_qty
    return out


def format_recipe_formula(mat_id: str) -> str:
    if mat_id not in MATERIAL_RECIPES:
        return ""
    r = MATERIAL_RECIPES[mat_id]
    ins = ", ".join(f"{v}× {k}" for k, v in r["inputs"].items())
    return f"{ins} → {r['output']}× {mat_id}"


def is_manual_gather(mat_id: str) -> bool:
    meta = MATERIAL_META.get(mat_id, {})
    if meta.get("anchor"):
        return True
    if meta.get("derived"):
        return False
    if mat_id in MATERIAL_RECIPES:
        return False
    if mat_id in ANIMAL_PRODUCT_YIELDS or mat_id in MOB_HUNT_PRODUCTS:
        return False
    return True


def collect_materials(trades: list[dict]) -> tuple[list[dict], list[dict]]:
    """Return (gather_tests, derived_materials)."""
    # Map each material → trades that need it (directly or via expansion)
    material_trades: dict[str, set[str]] = {}
    for trade in trades:
        recipe = trade.get("recipe", get_recipe(trade))
        for mat_id in recipe:
            material_trades.setdefault(mat_id, set()).add(trade["id"])
            for base_id in expand_material_to_bases(mat_id, 1.0):
                material_trades.setdefault(base_id, set()).add(trade["id"])

    gather_seen: set[str] = set()
    gather_tests: list[dict] = []

    def add_gather(mat_id: str) -> None:
        if mat_id in gather_seen:
            return
        if not is_manual_gather(mat_id):
            return
        if mat_id in WOOL_ALIASES:
            mat_id = "wool"
        if mat_id in gather_seen:
            return
        gather_seen.add(mat_id)
        meta = MATERIAL_META.get(mat_id, {
            "display_name": mat_id.replace("_", " ").title(),
            "category": "other",
            "default_qty": 32,
        })
        entry = {
            "id": mat_id,
            "display_name": meta["display_name"],
            "category": meta["category"],
            "default_qty": meta["default_qty"],
            "obtainment": "gather",
            "unlocks": sorted(material_trades.get(mat_id, set())),
        }
        if meta.get("anchor"):
            entry["anchor"] = True
        gather_tests.append(entry)

    for trade in trades:
        for base_id in expand_trade_to_bases(trade):
            add_gather(base_id)
        # Also include direct sell inputs that are manual (e.g. stick sold directly)
        for mat_id in trade.get("recipe", get_recipe(trade)):
            add_gather(mat_id)

    derived_seen: set[str] = set()
    derived_materials: list[dict] = []

    def add_derived(mat_id: str, obtainment: str, extra: dict | None = None) -> None:
        if mat_id in derived_seen:
            return
        derived_seen.add(mat_id)
        meta = MATERIAL_META.get(mat_id, {
            "display_name": mat_id.replace("_", " ").title(),
            "category": "other",
            "default_qty": 32,
        })
        entry = {
            "id": mat_id,
            "display_name": meta["display_name"],
            "category": meta["category"],
            "default_qty": meta["default_qty"],
            "obtainment": obtainment,
            "unlocks": sorted(material_trades.get(mat_id, set())),
        }
        if mat_id in MATERIAL_RECIPES:
            entry["formula"] = format_recipe_formula(mat_id)
            entry["recipe"] = MATERIAL_RECIPES[mat_id]
            entry["bases"] = expand_material_to_bases(mat_id, 1.0)
        if extra:
            entry.update(extra)
        derived_materials.append(entry)

    for trade in trades:
        for mat_id in trade.get("recipe", get_recipe(trade)):
            if mat_id in MATERIAL_RECIPES:
                add_derived(mat_id, "recipe")
            elif mat_id in ANIMAL_PRODUCT_YIELDS:
                species, yield_per_cycle = ANIMAL_PRODUCT_YIELDS[mat_id]
                add_derived(mat_id, "animal_farm", {
                    "animal": species,
                    "growth_min": ANIMAL_GROWTH_MIN[species],
                    "yield_per_cycle": yield_per_cycle,
                    "cycle_min": ANIMAL_FARM_ACTIVE_MIN + ANIMAL_GROWTH_MIN[species],
                })
            elif mat_id in MOB_HUNT_PRODUCTS:
                hunt = MOB_HUNT_PRODUCTS[mat_id]
                add_derived(mat_id, "mob_hunt", {
                    "mob": hunt["mob"],
                    "yield_per_cycle": hunt["yield_per_cycle"],
                    "risk_r": hunt["risk_r"],
                    "hunt_min": MOB_HUNT_ACTIVE_MIN,
                })

    gather_tests.sort(key=lambda m: (m["category"], m["display_name"]))
    derived_materials.sort(key=lambda m: (m["obtainment"], m["category"], m["display_name"]))
    return gather_tests, derived_materials


def filter_trades(trades: list[dict]) -> list[dict]:
    out = []
    for t in trades:
        if t["direction"] == "sell":
            t = dict(t)
            t["recipe"] = get_recipe(t)
            out.append(t)
            continue
        if not is_craftable_buy(t):
            continue
        recipe = get_recipe(t)
        if not recipe:
            continue
        t = dict(t)
        t["recipe"] = recipe
        out.append(t)
    return out


def main() -> None:
    legacy = load_legacy_trades()
    trades = filter_trades(legacy)
    gather_tests, derived_materials = collect_materials(trades)
    meta = {
        "version": "2.1-derivatives",
        "trade_count": len(trades),
        "gather_test_count": len(gather_tests),
        "derived_count": len(derived_materials),
        "excluded": "non-craftable buy, enchanted, maps, saddle, pearls, chainmail",
    }

    (DIR / "recipes.js").write_text(
        "const RECIPES = " + json.dumps(RECIPES, indent=2) + ";\n"
        "const MATERIAL_RECIPES = " + json.dumps(MATERIAL_RECIPES, indent=2) + ";\n",
        encoding="utf-8",
    )
    (DIR / "materials.js").write_text(
        "const GATHER_TESTS = " + json.dumps(gather_tests, indent=2) + ";\n"
        "const DERIVED_MATERIALS = " + json.dumps(derived_materials, indent=2) + ";\n"
        "// Legacy alias\nconst MATERIALS = GATHER_TESTS.concat(DERIVED_MATERIALS.filter(m => m.obtainment === 'animal_farm' || m.obtainment === 'mob_hunt'));\n"
        "const MODEL_META = " + json.dumps(meta, indent=2) + ";\n"
        "const ANIMAL_FARM_MODEL = " + json.dumps({
            "active_min": ANIMAL_FARM_ACTIVE_MIN,
            "ready_when": ANIMAL_FARM_READY_WHEN,
            "growth_min": ANIMAL_GROWTH_MIN,
            "formula": "cycle_min = active_min + growth_min; t_avg = cycle_min × (cal_qty / yield_per_cycle)",
        }, indent=2) + ";\n"
        "const MOB_HUNT_MODEL = " + json.dumps({
            "active_min": MOB_HUNT_ACTIVE_MIN,
            "default_risk": MOB_HUNT_DEFAULT_RISK,
            "ready_when": MOB_HUNT_READY_WHEN,
            "formula": "t_avg = active_min × (cal_qty / yield_per_cycle) × risk_r",
        }, indent=2) + ";\n",
        encoding="utf-8",
    )
    (DIR / "trades.js").write_text(
        "const TRADES = " + json.dumps(trades, indent=2) + ";\n"
        f"const TRADE_META = {json.dumps(meta, indent=2)};\n",
        encoding="utf-8",
    )
    (DIR / "trades.json").write_text(json.dumps({"trades": trades, "meta": meta}, indent=2) + "\n", encoding="utf-8")

    sell = sum(1 for t in trades if t["direction"] == "sell")
    buy = len(trades) - sell
    print(f"Gather tests: {len(gather_tests)}")
    print(f"Derived materials: {len(derived_materials)}")
    print(f"Trades: {len(trades)} ({sell} sell, {buy} buy)")
    print(f"Excluded: {len(legacy) - len(trades)} from {len(legacy)}")


if __name__ == "__main__":
    main()
