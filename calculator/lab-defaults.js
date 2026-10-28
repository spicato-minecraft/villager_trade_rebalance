/**
 * Default town: blank trade pools + starter P/C rates.
 * Offers.direction: "sell" = player sells to villager; "buy" = player buys from villager.
 */
(function (global) {
  const PROFESSIONS = {
    armorer: {
      n: 2,
      produce: [{ commodity: "iron_ingot", rate: 0 }],
      consume: [
        { commodity: "coal", rate: 2 },
        { commodity: "iron_ingot", rate: 0.5 },
      ],
      offers: [],
    },
    butcher: {
      n: 1,
      produce: [],
      consume: [{ commodity: "coal", rate: 2 }],
      offers: [],
    },
    cartographer: {
      n: 1,
      produce: [],
      consume: [{ commodity: "paper", rate: 2 }],
      offers: [],
    },
    cleric: {
      n: 1,
      produce: [],
      consume: [{ commodity: "nether_wart", rate: 0.5 }],
      offers: [],
    },
    farmer: {
      n: 4,
      produce: [
        { commodity: "wheat", rate: 8 },
        { commodity: "carrot", rate: 4 },
        { commodity: "potato", rate: 4 },
        { commodity: "beetroot", rate: 2 },
      ],
      consume: [{ commodity: "wheat", rate: 1 }],
      offers: [],
    },
    fisherman: {
      n: 1,
      produce: [{ commodity: "cod", rate: 2 }],
      consume: [{ commodity: "coal", rate: 1 }],
      offers: [],
    },
    fletcher: {
      n: 2,
      produce: [{ commodity: "arrow", rate: 4 }],
      consume: [
        { commodity: "stick", rate: 4 },
        { commodity: "flint", rate: 2 },
        { commodity: "feather", rate: 2 },
      ],
      offers: [],
    },
    leatherworker: {
      n: 1,
      produce: [],
      consume: [{ commodity: "leather", rate: 1 }],
      offers: [],
    },
    librarian: {
      n: 2,
      produce: [{ commodity: "enchanted_book", rate: 0.25 }],
      consume: [{ commodity: "paper", rate: 4 }],
      offers: [],
    },
    mason: {
      n: 1,
      produce: [{ commodity: "brick", rate: 4 }],
      consume: [{ commodity: "clay_ball", rate: 4 }],
      offers: [],
    },
    shepherd: {
      n: 1,
      produce: [{ commodity: "wool", rate: 6 }],
      consume: [{ commodity: "wheat", rate: 1 }],
      offers: [],
    },
    toolsmith: {
      n: 2,
      produce: [],
      consume: [
        { commodity: "coal", rate: 2 },
        { commodity: "iron_ingot", rate: 0.5 },
      ],
      offers: [],
    },
    weaponsmith: {
      n: 2,
      produce: [],
      consume: [
        { commodity: "coal", rate: 2 },
        { commodity: "iron_ingot", rate: 0.5 },
      ],
      offers: [],
    },
  };

  const PRESETS = {
    hamlet: {
      label: "Hamlet",
      unemployed: 2,
      professions: { farmer: 2, fletcher: 1, librarian: 1 },
    },
    mixed: {
      label: "Mixed village",
      unemployed: 4,
      professions: {
        armorer: 2, butcher: 1, cartographer: 1, cleric: 1, farmer: 4, fisherman: 1,
        fletcher: 2, leatherworker: 1, librarian: 2, mason: 1, shepherd: 1,
        toolsmith: 2, weaponsmith: 2,
      },
    },
    iron_hall: {
      label: "Iron hall",
      unemployed: 2,
      professions: { armorer: 8, toolsmith: 8, weaponsmith: 8, farmer: 2, fletcher: 1 },
    },
    farm_town: {
      label: "Farm town",
      unemployed: 6,
      professions: { farmer: 12, butcher: 4, shepherd: 2, fletcher: 1, librarian: 1 },
    },
  };

  const DEFAULT_TOWN = {
    version: 1,
    clock: { pulsesPerDay: 1 },
    knobs: {
      defaultCapPerTrader: 32,
      defaultCapPerMouth: 0,
      restockRawFloor: 0.25,
      markup: 1.5,
      capPerTrader: {},
      capPerMouth: {
        wheat: 4,
        bread: 2,
        carrot: 2,
        potato: 2,
        beetroot: 2,
      },
    },
    unemployed: 4,
    professions: PROFESSIONS,
    stocks: {},
    needs: {},
    scenario: {
      emeralds: 64,
      emeraldIncomePerDay: 8,
      repairBurn: { iron_ingot: 12, diamond: 1, coal: 8 },
      miningYield: { iron_ingot: 48, diamond: 3, coal: 32 },
    },
  };

  const DISPLAY_NAMES = {
    iron_ingot: "Iron ingot",
    diamond: "Diamond",
    coal: "Coal",
    wheat: "Wheat",
    bread: "Bread",
    stick: "Stick",
  };

  const api = { PROFESSIONS, PRESETS, DEFAULT_TOWN, DISPLAY_NAMES };

  if (typeof module !== "undefined" && module.exports) module.exports = api;
  global.LabDefaults = api;
})(typeof window !== "undefined" ? window : globalThis);
