/**
 * Living-markets ledger math. No DOM.
 * Rate unit: items per villager per Minecraft day (one tick = one day unless pulsesPerDay > 1).
 */
(function (global) {
  const FILL_MID = 0.5;

  function clone(obj) {
    return JSON.parse(JSON.stringify(obj));
  }

  function round1(n) {
    return Math.round(n * 10) / 10;
  }

  function round2(n) {
    return Math.round(n * 100) / 100;
  }

  function totalVillagers(town) {
    let n = Number(town.unemployed) || 0;
    for (const p of Object.values(town.professions || {})) n += Number(p.n) || 0;
    return n;
  }

  function offerTouches(offer, commodity) {
    if (!offer || offer.enabled === false) return false;
    if (offer.item === commodity) return true;
    return !!(offer.components && Object.prototype.hasOwnProperty.call(offer.components, commodity));
  }

  function professionTouches(prof, commodity) {
    if ((prof.produce || []).some((r) => r.commodity === commodity)) return true;
    if ((prof.consume || []).some((r) => r.commodity === commodity)) return true;
    return (prof.offers || []).some((o) => offerTouches(o, commodity));
  }

  function tradersOf(town, commodity) {
    let n = 0;
    for (const p of Object.values(town.professions || {})) {
      if (professionTouches(p, commodity)) n += Number(p.n) || 0;
    }
    return n;
  }

  function listCommodityIds(town) {
    const ids = new Set();
    for (const p of Object.values(town.professions || {})) {
      if (!(Number(p.n) || 0)) continue;
      for (const r of p.produce || []) if (r.commodity) ids.add(r.commodity);
      for (const r of p.consume || []) if (r.commodity) ids.add(r.commodity);
      for (const o of p.offers || []) {
        if (!o || o.enabled === false) continue;
        if (o.item) ids.add(o.item);
        for (const c of Object.keys(o.components || {})) ids.add(c);
      }
    }
    return [...ids].sort();
  }

  function capKnobs(town, commodity) {
    const knobs = town.knobs || {};
    const perTrader =
      knobs.capPerTrader && knobs.capPerTrader[commodity] != null
        ? Number(knobs.capPerTrader[commodity])
        : Number(knobs.defaultCapPerTrader) || 32;
    const perMouth =
      knobs.capPerMouth && knobs.capPerMouth[commodity] != null
        ? Number(knobs.capPerMouth[commodity])
        : Number(knobs.defaultCapPerMouth) || 0;
    return { perTrader, perMouth };
  }

  function capOf(town, commodity) {
    const { perTrader, perMouth } = capKnobs(town, commodity);
    return Math.max(0, perTrader * tradersOf(town, commodity) + perMouth * totalVillagers(town));
  }

  function midOf(town, commodity) {
    return capOf(town, commodity) * FILL_MID;
  }

  function isFinished(town, commodity) {
    for (const p of Object.values(town.professions || {})) {
      for (const o of p.offers || []) {
        if (o && o.enabled !== false && o.item === commodity && o.direction === "buy") return true;
      }
    }
    return false;
  }

  function produceRate(town, commodity) {
    let r = 0;
    for (const p of Object.values(town.professions || {})) {
      const n = Number(p.n) || 0;
      for (const row of p.produce || []) {
        if (row.commodity === commodity) r += n * (Number(row.rate) || 0);
      }
    }
    return r;
  }

  function consumeRate(town, commodity) {
    let r = 0;
    for (const p of Object.values(town.professions || {})) {
      const n = Number(p.n) || 0;
      for (const row of p.consume || []) {
        if (row.commodity === commodity) r += n * (Number(row.rate) || 0);
      }
    }
    return r;
  }

  function isIdentityRecipe(offer) {
    if (!offer || !offer.item) return false;
    const comps = offer.components || {};
    const keys = Object.keys(comps);
    if (!keys.length) return true;
    return keys.length === 1 && keys[0] === offer.item;
  }

  function parseComponents(text) {
    const out = {};
    String(text || "")
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean)
      .forEach((part) => {
        let m = part.match(/^([a-z0-9_]+)\s*[:=x×]\s*([0-9.]+)$/i);
        if (!m) m = part.match(/^([0-9.]+)\s*[x×]?\s*([a-z0-9_]+)$/i);
        if (m && m[1] && Number.isNaN(Number(m[1]))) {
          const qty = Number(m[2]);
          if (qty > 0) out[m[1]] = qty;
        } else if (m) {
          const qty = Number(m[1]);
          const id = m[2];
          if (qty > 0 && id) out[id] = qty;
        }
      });
    return out;
  }

  function formatComponents(components) {
    return Object.entries(components || {})
      .map(([k, v]) => `${k}:${v}`)
      .join(", ");
  }

  function blankOffer(professionId, index) {
    const n = index == null ? Date.now() : index;
    return {
      id: `${professionId || "offer"}_${n}`,
      name: "New offer",
      direction: "buy",
      levels: "Novice",
      item: "item",
      components: {},
      enabled: true,
      restockPerVillagerDay: 4,
      vanillaEmeralds: 1,
    };
  }

  function addOffer(town, professionId, partial) {
    const p = town.professions[professionId];
    if (!p) return null;
    if (!p.offers) p.offers = [];
    const offer = Object.assign(blankOffer(professionId, p.offers.length), partial || {});
    p.offers.push(offer);
    ensureStocks(town);
    return offer;
  }

  function removeOffer(town, professionId, index) {
    const p = town.professions[professionId];
    if (!p || !p.offers) return;
    p.offers.splice(index, 1);
  }

  function replaceProfessionPool(town, professionId, offers) {
    const p = town.professions[professionId];
    if (!p) return;
    const oldCaps = snapshotCaps(town);
    p.offers = clone(offers || []);
    rescaleStocksAfterCapChange(town, oldCaps);
    ensureStocks(town);
  }

  function restockDrainOf(town, commodity) {
    let r = 0;
    for (const p of Object.values(town.professions || {})) {
      const n = Number(p.n) || 0;
      for (const o of p.offers || []) {
        if (!o || o.enabled === false || o.direction !== "buy") continue;
        if (isIdentityRecipe(o)) continue;
        const qty = o.components && o.components[commodity];
        if (!qty) continue;
        r += n * (Number(o.restockPerVillagerDay) || 0) * qty;
      }
    }
    return r;
  }

  function restockFillOf(town, commodity) {
    let r = 0;
    for (const p of Object.values(town.professions || {})) {
      const n = Number(p.n) || 0;
      for (const o of p.offers || []) {
        if (!o || o.enabled === false || o.direction !== "buy") continue;
        if (o.item !== commodity) continue;
        r += n * (Number(o.restockPerVillagerDay) || 0);
      }
    }
    return r;
  }

  function contributors(town, commodity) {
    const names = [];
    for (const [id, p] of Object.entries(town.professions || {})) {
      if ((Number(p.n) || 0) > 0 && professionTouches(p, commodity)) names.push(id);
    }
    return names;
  }

  function ensureStocks(town) {
    if (!town.stocks) town.stocks = {};
    if (!town.needs) town.needs = {};
    for (const id of listCommodityIds(town)) {
      const cap = capOf(town, id);
      if (town.stocks[id] == null) town.stocks[id] = midOf(town, id);
      town.stocks[id] = Math.max(0, Math.min(cap, Number(town.stocks[id]) || 0));
    }
  }

  function rescaleStocksAfterCapChange(town, oldCaps) {
    if (!town.stocks) town.stocks = {};
    for (const id of listCommodityIds(town)) {
      const cap = capOf(town, id);
      const prevCap = oldCaps[id];
      const prevStock = town.stocks[id];
      if (prevCap > 0 && prevStock != null) {
        town.stocks[id] = Math.max(0, Math.min(cap, (prevStock / prevCap) * cap));
      } else if (town.stocks[id] == null) {
        town.stocks[id] = cap * FILL_MID;
      } else {
        town.stocks[id] = Math.max(0, Math.min(cap, town.stocks[id]));
      }
    }
  }

  function snapshotCaps(town) {
    const caps = {};
    for (const id of listCommodityIds(town)) caps[id] = capOf(town, id);
    return caps;
  }

  function hungriestFill(town, offer) {
    const fills = [];
    const push = (id) => {
      const cap = capOf(town, id);
      if (cap <= 0) {
        fills.push(1);
        return;
      }
      fills.push((town.stocks[id] || 0) / cap);
    };
    if (offer.item) push(offer.item);
    for (const c of Object.keys(offer.components || {})) push(c);
    if (!fills.length) return 1;
    return Math.min(...fills);
  }

  function offerPrice(town, offer, fairEmeralds) {
    const hungriest = hungriestFill(town, offer);
    const scarcity = 1 + (1 - hungriest) * 0.8;
    const markup = Number(town.knobs && town.knobs.markup) || 1;
    const base = (fairEmeralds != null ? fairEmeralds : Number(offer.vanillaEmeralds) || 1) * markup;
    return Math.max(1, round1(base * scarcity));
  }

  function rowFor(town, commodity) {
    ensureStocks(town);
    const cap = capOf(town, commodity);
    const mid = cap * FILL_MID;
    const stock = town.stocks[commodity] || 0;
    const P = produceRate(town, commodity);
    const C = consumeRate(town, commodity);
    const restockDrain = restockDrainOf(town, commodity);
    const restockFill = restockFillOf(town, commodity);
    const daysToDigest = C > 0 ? round1(Math.max(0, cap - mid) / C) : null;
    const daysToRefillRaw = P > 0 ? round1(mid / P) : null;
    const daysToRefillFinished = restockFill > 0 ? round1(mid / restockFill) : null;
    return {
      id: commodity,
      finished: isFinished(town, commodity),
      cap: round1(cap),
      mid: round1(mid),
      stock: round1(stock),
      fill: cap > 0 ? round2(stock / cap) : 0,
      headroom: round1(Math.max(0, cap - stock)),
      idleHeadroom: round1(Math.max(0, cap - mid)),
      idleShopDepth: round1(mid),
      produce: round1(P),
      consume: round1(C),
      restockDrain: round1(restockDrain),
      restockFill: round1(restockFill),
      need: !!town.needs[commodity],
      traders: tradersOf(town, commodity),
      contributors: contributors(town, commodity),
      daysToDigestFullDump: daysToDigest,
      daysToRefill: isFinished(town, commodity) ? daysToRefillFinished : daysToRefillRaw,
    };
  }

  function snapshot(town) {
    ensureStocks(town);
    const commodities = listCommodityIds(town).map((id) => rowFor(town, id));
    return {
      totalVillagers: totalVillagers(town),
      commodities,
      byId: Object.fromEntries(commodities.map((r) => [r.id, r])),
    };
  }

  function craftableCount(town, offer) {
    if (!offer.components || !Object.keys(offer.components).length) return Infinity;
    let n = Infinity;
    for (const [c, qty] of Object.entries(offer.components)) {
      if (!qty) continue;
      n = Math.min(n, (town.stocks[c] || 0) / qty);
    }
    return n;
  }

  function restockFloorOk(town, offer) {
    const floor = Number(town.knobs && town.knobs.restockRawFloor) || 0;
    for (const c of Object.keys(offer.components || {})) {
      const cap = capOf(town, c);
      if (cap <= 0) continue;
      if ((town.stocks[c] || 0) < floor * cap) return false;
    }
    return true;
  }

  function tickDay(town) {
    ensureStocks(town);
    const pulses = Math.max(1, Number(town.clock && town.clock.pulsesPerDay) || 1);
    const scale = 1 / pulses;
    for (let p = 0; p < pulses; p++) {
      for (const id of listCommodityIds(town)) {
        const cap = capOf(town, id);
        const mid = cap * FILL_MID;
        let stock = town.stocks[id] || 0;
        const P = produceRate(town, id) * scale;
        const C = consumeRate(town, id) * scale;
        if (stock < mid && !town.needs[id]) stock += P;
        if (stock > mid) stock -= C;
        town.stocks[id] = Math.max(0, Math.min(cap, stock));
      }

      for (const prof of Object.values(town.professions || {})) {
        const n = Number(prof.n) || 0;
        for (const o of prof.offers || []) {
          if (!o || o.enabled === false || o.direction !== "buy") continue;
          const item = o.item;
          if (!item) continue;
          const capF = capOf(town, item);
          const midF = capF * FILL_MID;
          if ((town.stocks[item] || 0) >= midF) continue;
          if (!isIdentityRecipe(o) && !restockFloorOk(town, o)) continue;
          const want = n * (Number(o.restockPerVillagerDay) || 0) * scale;
          const room = capF - (town.stocks[item] || 0);
          if (isIdentityRecipe(o)) {
            const add = Math.min(want, room);
            if (add > 0) town.stocks[item] = (town.stocks[item] || 0) + add;
            continue;
          }
          const crafted = Math.min(want, room, craftableCount(town, o));
          if (crafted <= 0) continue;
          for (const [c, qty] of Object.entries(o.components || {})) {
            town.stocks[c] = Math.max(0, (town.stocks[c] || 0) - crafted * qty);
          }
          town.stocks[item] = Math.min(capF, (town.stocks[item] || 0) + crafted);
        }
      }
    }
    if (town.scenario) {
      town.scenario.emeralds =
        (Number(town.scenario.emeralds) || 0) + (Number(town.scenario.emeraldIncomePerDay) || 0);
    }
    return snapshot(town);
  }

  function tickDays(town, days) {
    const n = Math.max(0, Math.floor(Number(days) || 0));
    for (let i = 0; i < n; i++) tickDay(town);
    return snapshot(town);
  }

  function dump(town, commodity, amount) {
    ensureStocks(town);
    const cap = capOf(town, commodity);
    const add = Math.max(0, Number(amount) || 0);
    const before = town.stocks[commodity] || 0;
    town.stocks[commodity] = Math.min(cap, before + add);
    return { added: town.stocks[commodity] - before, stock: town.stocks[commodity], cap };
  }

  function findOffer(town, offerId) {
    for (const p of Object.values(town.professions || {})) {
      const o = (p.offers || []).find((x) => x.id === offerId);
      if (o) return o;
    }
    return null;
  }

  function shop(town, offerId, amount, fairEmeralds) {
    ensureStocks(town);
    const offer = findOffer(town, offerId);
    if (!offer || offer.direction !== "buy") return { ok: false, reason: "Unknown buy offer" };
    const qty = Math.max(0, Number(amount) || 0);
    const have = town.stocks[offer.item] || 0;
    const took = Math.min(qty, have);
    if (took <= 0) return { ok: false, reason: "No finished stock", took: 0 };
    const unit = offerPrice(town, offer, fairEmeralds);
    const cost = round1(unit * took);
    const wallet = town.scenario || (town.scenario = { emeralds: 0 });
    if ((Number(wallet.emeralds) || 0) < cost) {
      return { ok: false, reason: "Not enough emeralds", cost, emeralds: wallet.emeralds, took: 0 };
    }
    town.stocks[offer.item] = have - took;
    wallet.emeralds = round1((Number(wallet.emeralds) || 0) - cost);
    return { ok: true, took, cost, unitPrice: unit, emeralds: wallet.emeralds };
  }

  function setNeed(town, commodity, on) {
    if (!town.needs) town.needs = {};
    if (on) town.needs[commodity] = true;
    else delete town.needs[commodity];
  }

  function resetToMid(town) {
    town.stocks = {};
    for (const id of listCommodityIds(town)) town.stocks[id] = midOf(town, id);
  }

  function setPopulation(town, professionId, n) {
    const oldCaps = snapshotCaps(town);
    if (!town.professions[professionId]) return;
    town.professions[professionId].n = Math.max(0, Number(n) || 0);
    rescaleStocksAfterCapChange(town, oldCaps);
  }

  function setUnemployed(town, n) {
    const oldCaps = snapshotCaps(town);
    town.unemployed = Math.max(0, Number(n) || 0);
    rescaleStocksAfterCapChange(town, oldCaps);
  }

  function applyPreset(town, presetPop) {
    const oldCaps = snapshotCaps(town);
    town.unemployed = presetPop.unemployed || 0;
    for (const [id, p] of Object.entries(town.professions || {})) {
      p.n = presetPop.professions && presetPop.professions[id] != null ? presetPop.professions[id] : 0;
    }
    rescaleStocksAfterCapChange(town, oldCaps);
  }

  function invariantReport(town) {
    const snap = snapshot(town);
    const burn = (town.scenario && town.scenario.repairBurn) || {};
    const mine = (town.scenario && town.scenario.miningYield) || {};
    const flags = [];
    const rows = ["iron_ingot", "diamond", "coal"].map((id) => {
      const row = snap.byId[id];
      const shoppable = row ? row.restockDrain : 0;
      const maxDump = row ? row.idleHeadroom : 0;
      const b = Number(burn[id]) || 0;
      const m = Number(mine[id]) || 0;
      if (b > 0 && shoppable >= b) flags.push(`${id}: village restock drain ≥ repair burn (town can replace the mine)`);
      if (m > 0 && maxDump < m * 0.15) flags.push(`${id}: dump room is tiny vs a mining session`);
      return {
        id,
        shoppablePerDay: shoppable,
        maxDump,
        burn: b,
        mining: m,
      };
    });
    return { totalVillagers: snap.totalVillagers, anchors: rows, flags };
  }

  function exportModKnobs(town) {
    const professions = {};
    for (const [id, p] of Object.entries(town.professions || {})) {
      const restock = {};
      for (const o of p.offers || []) {
        if (o.direction === "buy" && o.item) restock[o.item] = Number(o.restockPerVillagerDay) || 0;
      }
      professions[id] = {
        produce: clone(p.produce || []),
        consume: clone(p.consume || []),
        restock_per_villager_day: restock,
        offers: (p.offers || []).map((o) => ({
          id: o.id,
          item: o.item,
          direction: o.direction,
          enabled: o.enabled !== false,
          components: clone(o.components || {}),
        })),
      };
    }
    return {
      pulses_per_day: Number(town.clock && town.clock.pulsesPerDay) || 1,
      default_cap_per_trader: Number(town.knobs && town.knobs.defaultCapPerTrader) || 32,
      default_cap_per_mouth: Number(town.knobs && town.knobs.defaultCapPerMouth) || 0,
      cap_per_trader: clone((town.knobs && town.knobs.capPerTrader) || {}),
      cap_per_mouth: clone((town.knobs && town.knobs.capPerMouth) || {}),
      restock_raw_floor: Number(town.knobs && town.knobs.restockRawFloor) || 0,
      markup: Number(town.knobs && town.knobs.markup) || 1,
      professions,
    };
  }

  function allBuyOffers(town) {
    const out = [];
    for (const [pid, p] of Object.entries(town.professions || {})) {
      for (const o of p.offers || []) {
        if (o && o.direction === "buy" && o.enabled !== false) out.push({ profession: pid, offer: o });
      }
    }
    return out;
  }

  const api = {
    FILL_MID,
    clone,
    totalVillagers,
    tradersOf,
    listCommodityIds,
    capOf,
    midOf,
    produceRate,
    consumeRate,
    restockDrainOf,
    restockFillOf,
    ensureStocks,
    snapshotCaps,
    rescaleStocksAfterCapChange,
    snapshot,
    rowFor,
    tickDay,
    tickDays,
    dump,
    shop,
    findOffer,
    setNeed,
    resetToMid,
    setPopulation,
    setUnemployed,
    applyPreset,
    invariantReport,
    exportModKnobs,
    offerPrice,
    hungriestFill,
    allBuyOffers,
    isIdentityRecipe,
    parseComponents,
    formatComponents,
    blankOffer,
    addOffer,
    removeOffer,
    replaceProfessionPool,
  };

  if (typeof module !== "undefined" && module.exports) module.exports = api;
  global.LedgerLab = api;
})(typeof window !== "undefined" ? window : globalThis);
