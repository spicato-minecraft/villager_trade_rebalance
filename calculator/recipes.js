const RECIPES = {
  "iron_helmet": {
    "iron_ingot": 5
  },
  "iron_chestplate": {
    "iron_ingot": 8
  },
  "iron_leggings": {
    "iron_ingot": 7
  },
  "iron_boots": {
    "iron_ingot": 4
  },
  "iron_axe": {
    "iron_ingot": 3,
    "stick": 2
  },
  "shield": {
    "iron_ingot": 1,
    "planks": 6
  },
  "stone_axe": {
    "cobblestone": 3,
    "stick": 2
  },
  "stone_shovel": {
    "cobblestone": 1,
    "stick": 2
  },
  "stone_pickaxe": {
    "cobblestone": 3,
    "stick": 2
  },
  "stone_hoe": {
    "cobblestone": 2,
    "stick": 2
  },
  "diamond_hoe": {
    "diamond": 2,
    "stick": 2
  },
  "diamond_pickaxe": {
    "diamond": 3,
    "stick": 2
  },
  "bow": {
    "stick": 3,
    "string": 3
  },
  "crossbow": {
    "stick": 3,
    "string": 2,
    "iron_ingot": 1,
    "tripwire_hook": 1
  },
  "arrow": {
    "flint": 1,
    "stick": 1,
    "feather": 1
  },
  "tripwire_hook": {
    "iron_ingot": 1,
    "stick": 1,
    "planks": 1
  },
  "bread": {
    "wheat": 3
  },
  "cookie": {
    "wheat": 2,
    "cocoa_beans": 1
  },
  "pumpkin_pie": {
    "pumpkin": 1,
    "egg": 1,
    "sugar": 1
  },
  "cake": {
    "milk_bucket": 3,
    "sugar": 2,
    "egg": 1,
    "wheat": 3
  },
  "rabbit_stew": {
    "cooked_rabbit": 1,
    "carrot": 1,
    "baked_potato": 1,
    "bowl": 1
  },
  "golden_carrot": {
    "gold_nugget": 8,
    "carrot": 1
  },
  "glistering_melon_slice": {
    "gold_nugget": 8,
    "melon_slice": 1
  },
  "bookshelf": {
    "planks": 6,
    "book": 3
  },
  "book": {
    "paper": 3,
    "leather": 1
  },
  "paper": {
    "sugar_cane": 3
  },
  "lantern": {
    "iron_nugget": 8,
    "torch": 1
  },
  "glass": {
    "glass_block": 1
  },
  "compass": {
    "iron_ingot": 4,
    "redstone": 1
  },
  "clock": {
    "gold_ingot": 4,
    "redstone": 1
  },
  "red_candle": {
    "candle": 1,
    "red_dye": 1
  },
  "yellow_candle": {
    "candle": 1,
    "yellow_dye": 1
  },
  "empty_map": {
    "paper": 8,
    "compass": 1
  },
  "item_frame": {
    "stick": 8,
    "leather": 1
  },
  "campfire": {
    "coal": 3,
    "stick": 3,
    "log": 3
  },
  "shears": {
    "iron_ingot": 2
  },
  "painting": {
    "stick": 8,
    "wool": 1
  },
  "brick": {
    "clay_ball": 1
  },
  "chiseled_stone_bricks": {
    "stone_bricks": 1
  },
  "stone_bricks": {
    "stone": 1
  },
  "dripstone_block": {
    "pointed_dripstone": 4
  },
  "polished_andesite": {
    "andesite": 4
  },
  "polished_diorite": {
    "diorite": 4
  },
  "polished_granite": {
    "granite": 4
  },
  "quartz_pillar": {
    "quartz_block": 1
  },
  "quartz_block": {
    "quartz": 4
  },
  "leather_helmet": {
    "leather": 5
  },
  "leather_chestplate": {
    "leather": 8
  },
  "leather_leggings": {
    "leather": 7
  },
  "leather_boots": {
    "leather": 4
  },
  "leather_horse_armor": {
    "leather": 7
  },
  "bell": {
    "gold_ingot": 3,
    "iron_ingot": 4
  },
  "wool": {
    "wool": 1
  },
  "carpet": {
    "wool": 2
  },
  "bed": {
    "wool": 3,
    "planks": 3
  },
  "banner": {
    "wool": 6,
    "stick": 1
  },
  "cooked_cod": {
    "cod": 1,
    "fuel": 1
  },
  "cooked_salmon": {
    "salmon": 1,
    "fuel": 1
  },
  "cooked_chicken": {
    "chicken": 1,
    "fuel": 1
  },
  "cooked_porkchop": {
    "porkchop": 1,
    "fuel": 1
  },
  "cod_bucket": {
    "bucket": 1,
    "cod": 1
  },
  "oak_boat": {
    "planks": 5
  },
  "spruce_boat": {
    "planks": 5
  },
  "jungle_boat": {
    "planks": 5
  },
  "acacia_boat": {
    "planks": 5
  },
  "dark_oak_boat": {
    "planks": 5
  },
  "mangrove_boat": {
    "planks": 5
  },
  "cherry_boat": {
    "planks": 5
  },
  "bamboo_raft": {
    "bamboo": 5
  },
  "redstone": {
    "redstone": 1
  },
  "lapis_lazuli": {
    "lapis_lazuli": 1
  },
  "glowstone": {
    "glowstone_dust": 4
  },
  "apple": {
    "apple": 1
  },
  "flint": {
    "gravel": 10
  }
};
const MATERIAL_RECIPES = {
  "planks": {
    "inputs": {
      "log": 1
    },
    "output": 4
  },
  "stick": {
    "inputs": {
      "planks": 2
    },
    "output": 4
  },
  "fuel": {
    "inputs": {
      "coal": 1
    },
    "output": 8
  },
  "glass_block": {
    "inputs": {
      "sand": 1,
      "fuel": 1
    },
    "output": 1
  },
  "glass_pane": {
    "inputs": {
      "glass_block": 6
    },
    "output": 16
  },
  "glass_bottle": {
    "inputs": {
      "glass_block": 3
    },
    "output": 3
  },
  "paper": {
    "inputs": {
      "sugar_cane": 3
    },
    "output": 3
  },
  "sugar": {
    "inputs": {
      "sugar_cane": 1
    },
    "output": 1
  },
  "book": {
    "inputs": {
      "paper": 3,
      "leather": 1
    },
    "output": 1
  },
  "writable_book": {
    "inputs": {
      "book": 1,
      "ink_sac": 1,
      "feather": 1
    },
    "output": 1
  },
  "iron_nugget": {
    "inputs": {
      "iron_ingot": 1
    },
    "output": 9
  },
  "gold_nugget": {
    "inputs": {
      "gold_ingot": 1
    },
    "output": 9
  },
  "bucket": {
    "inputs": {
      "iron_ingot": 3
    },
    "output": 1
  },
  "bowl": {
    "inputs": {
      "planks": 3
    },
    "output": 4
  },
  "torch": {
    "inputs": {
      "coal": 1,
      "stick": 1
    },
    "output": 4
  },
  "stone_bricks": {
    "inputs": {
      "stone": 1
    },
    "output": 1
  },
  "brick": {
    "inputs": {
      "clay_ball": 1,
      "fuel": 1
    },
    "output": 1
  },
  "flint": {
    "inputs": {
      "gravel": 10
    },
    "output": 1
  },
  "baked_potato": {
    "inputs": {
      "potato": 1,
      "fuel": 1
    },
    "output": 1
  },
  "cooked_rabbit": {
    "inputs": {
      "rabbit": 1,
      "fuel": 1
    },
    "output": 1
  },
  "compass": {
    "inputs": {
      "iron_ingot": 4,
      "redstone": 1
    },
    "output": 1
  },
  "tripwire_hook": {
    "inputs": {
      "iron_ingot": 1,
      "stick": 1,
      "planks": 1
    },
    "output": 2
  },
  "quartz_block": {
    "inputs": {
      "quartz": 4
    },
    "output": 1
  },
  "oak_boat": {
    "inputs": {
      "planks": 5
    },
    "output": 1
  },
  "spruce_boat": {
    "inputs": {
      "planks": 5
    },
    "output": 1
  },
  "birch_boat": {
    "inputs": {
      "planks": 5
    },
    "output": 1
  },
  "jungle_boat": {
    "inputs": {
      "planks": 5
    },
    "output": 1
  },
  "acacia_boat": {
    "inputs": {
      "planks": 5
    },
    "output": 1
  },
  "dark_oak_boat": {
    "inputs": {
      "planks": 5
    },
    "output": 1
  },
  "cherry_boat": {
    "inputs": {
      "planks": 5
    },
    "output": 1
  },
  "mangrove_boat": {
    "inputs": {
      "planks": 5
    },
    "output": 1
  },
  "bamboo_raft": {
    "inputs": {
      "bamboo": 5
    },
    "output": 1
  }
};
