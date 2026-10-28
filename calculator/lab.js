(function (global) {
  const L = () => global.LedgerLab;
  const D = () => global.LabDefaults;

  const PROF_ORDER = [
    "armorer", "butcher", "cartographer", "cleric", "farmer", "fisherman",
    "fletcher", "leatherworker", "librarian", "mason", "shepherd",
    "toolsmith", "weaponsmith",
  ];

  let town = null;
  let selectedProf = "farmer";
  let saveTimer = null;

  function displayName(id) {
    const reg = global.ItemRegistry && global.ItemRegistry.byId && global.ItemRegistry.byId[id];
    if (reg && reg.name) return reg.name;
    return (D().DISPLAY_NAMES && D().DISPLAY_NAMES[id]) || String(id).replace(/_/g, " ");
  }

  function escapeHtml(s) {
    return String(s ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function fairForOffer(offer) {
    const fn = global.TradeCalibrator && global.TradeCalibrator.fairEmeraldsForItem;
    if (typeof fn !== "function") return null;
    return fn(offer.item) ?? fn(Object.keys(offer.components || {})[0]);
  }

  function persistSoon() {
    const el = document.getElementById("lab-save-status");
    if (el) {
      el.textContent = "Pending…";
      el.className = "save-status pending";
    }
    clearTimeout(saveTimer);
    saveTimer = setTimeout(flushLab, 400);
  }

  async function flushLab() {
    const el = document.getElementById("lab-save-status");
    try {
      const res = await fetch("lab.json", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(town, null, 2),
      });
      if (!res.ok && res.status !== 204) throw new Error(String(res.status));
      if (el) {
        el.textContent = "Saved lab.json";
        el.className = "save-status saved";
        setTimeout(() => { if (el.textContent === "Saved lab.json") el.textContent = ""; }, 2000);
      }
    } catch (_) {
      if (el) {
        el.textContent = "Save failed — is serve.py running?";
        el.className = "save-status error";
      }
    }
  }

  async function loadLab() {
    try {
      const res = await fetch("lab.json", { cache: "no-store" });
      if (res.ok) {
        const data = await res.json();
        if (data && data.version === 1 && data.professions) return data;
      }
    } catch (_) { /* offline */ }
    const t = L().clone(D().DEFAULT_TOWN);
    L().resetToMid(t);
    return t;
  }

  function setFieldIfIdle(id, value) {
    const el = document.getElementById(id);
    if (!el || el === document.activeElement) return;
    el.value = value;
  }

  function renderTown() {
    const grid = document.getElementById("town-pop-grid");
    if (!grid) return;
    if (!grid.contains(document.activeElement)) {
      const rows = PROF_ORDER.map((id) => {
        const n = town.professions[id] ? town.professions[id].n : 0;
        return `<tr>
          <td class="town-n"><input type="number" min="0" step="1" data-pop="${escapeHtml(id)}" value="${n}"></td>
          <td><div class="item-name">${escapeHtml(id)}</div></td>
        </tr>`;
      });
      rows.push(`<tr>
        <td class="town-n"><input type="number" min="0" step="1" id="town-unemployed" value="${town.unemployed || 0}"></td>
        <td><div class="item-name">unemployed</div><div class="item-id">mouths only</div></td>
      </tr>`);
      grid.innerHTML = rows.join("");
    }

    setFieldIfIdle("town-cap-trader", town.knobs.defaultCapPerTrader);
    setFieldIfIdle("town-cap-mouth", town.knobs.defaultCapPerMouth);
    setFieldIfIdle("town-floor", town.knobs.restockRawFloor);
    setFieldIfIdle("town-markup", town.knobs.markup);
    setFieldIfIdle("town-pulses", town.clock.pulsesPerDay);

    const snap = L().snapshot(town);
    document.getElementById("town-stats").innerHTML =
      `<span><strong>${snap.totalVillagers}</strong> villagers</span>` +
      `<span><strong>${snap.commodities.length}</strong> ledger rows</span>` +
      `<span>Idle dump room (iron): <strong>${snap.byId.iron_ingot ? snap.byId.iron_ingot.idleHeadroom : 0}</strong></span>`;
  }

  const VILLAGER_LEVELS = ["Novice", "Apprentice", "Journeyman", "Expert", "Master"];

  function hashName(name) {
    const s = String(name || "offer");
    let h = 2166136261;
    for (let i = 0; i < s.length; i++) {
      h ^= s.charCodeAt(i);
      h = Math.imul(h, 16777619);
    }
    return (h >>> 0).toString(16).padStart(8, "0");
  }

  function parseLiveNumber(value) {
    const s = String(value).trim();
    if (s === "" || s === "-" || s === "." || s === "-.") return null;
    const n = Number(s);
    return Number.isFinite(n) ? n : null;
  }

  function capPerVillager(commodity) {
    if (!commodity) return 0;
    const knobs = town.knobs || {};
    if (knobs.capPerTrader && knobs.capPerTrader[commodity] != null) {
      return Number(knobs.capPerTrader[commodity]) || 0;
    }
    return Number(knobs.defaultCapPerTrader) || 0;
  }

  function ensureItemCap(commodity) {
    if (!commodity) return;
    if (!town.knobs.capPerTrader) town.knobs.capPerTrader = {};
    if (town.knobs.capPerTrader[commodity] == null) {
      town.knobs.capPerTrader[commodity] = Number(town.knobs.defaultCapPerTrader) || 0;
    }
  }

  function setCapPerVillager(commodity, value) {
    if (!commodity) return;
    const oldCaps = L().snapshotCaps(town);
    if (!town.knobs.capPerTrader) town.knobs.capPerTrader = {};
    town.knobs.capPerTrader[commodity] = Math.max(0, Number(value) || 0);
    L().rescaleStocksAfterCapChange(town, oldCaps);
  }

  function levelSelect(current) {
    const cur = VILLAGER_LEVELS.includes(current) ? current : "Novice";
    return VILLAGER_LEVELS.map((lvl) =>
      `<option value="${lvl}" ${lvl === cur ? "selected" : ""}>${lvl}</option>`
    ).join("");
  }

  function capInput(commodity) {
    return `<input type="number" min="0" step="1" data-cap-item="${escapeHtml(commodity)}" value="${capPerVillager(commodity)}">`;
  }

  function renderProfession() {
    const p = town.professions[selectedProf];
    if (!p) return;
    document.getElementById("prof-select").value = selectedProf;
    setFieldIfIdle("prof-n", p.n || 0);

    const prod = document.getElementById("prof-produce-body");
    prod.innerHTML = (p.produce || []).map((row, i) => `<tr>
      <td><div class="item-name">${escapeHtml(displayName(row.commodity))}</div><div class="item-id">${escapeHtml(row.commodity)}</div></td>
      <td><input type="number" step="0.25" min="0" data-pc="produce" data-i="${i}" data-f="rate" value="${row.rate}"></td>
      <td>${capInput(row.commodity)}</td>
      <td><button type="button" class="btn" data-del-pc="produce" data-i="${i}">Remove</button></td>
    </tr>`).join("") || `<tr><td colspan="4" class="empty-msg">Empty — search and add one item</td></tr>`;

    const cons = document.getElementById("prof-consume-body");
    cons.innerHTML = (p.consume || []).map((row, i) => `<tr>
      <td><div class="item-name">${escapeHtml(displayName(row.commodity))}</div><div class="item-id">${escapeHtml(row.commodity)}</div></td>
      <td><input type="number" step="0.25" min="0" data-pc="consume" data-i="${i}" data-f="rate" value="${row.rate}"></td>
      <td>${capInput(row.commodity)}</td>
      <td><button type="button" class="btn" data-del-pc="consume" data-i="${i}">Remove</button></td>
    </tr>`).join("") || `<tr><td colspan="4" class="empty-msg">Empty — search and add one item</td></tr>`;

    const ob = document.getElementById("prof-offer-body");
    ob.innerHTML = (p.offers || []).map((o, i) => {
      const bom = L().formatComponents(o.components || {});
      const restockDisabled = o.direction !== "buy" ? "disabled" : "";
      const dir = o.direction === "sell" ? "sell" : "buy";
      const dirLabel = dir === "buy" ? "Buys" : "Sells";
      return `<tr>
        <td><input type="checkbox" data-off="${i}" data-f="enabled" ${o.enabled !== false ? "checked" : ""}></td>
        <td>
          <input data-off="${i}" data-f="name" value="${escapeHtml(o.name)}" style="min-width:9rem">
          <div class="item-id">${escapeHtml(o.item || "")} · ${escapeHtml(o.id)}</div>
        </td>
        <td><select data-off="${i}" data-f="levels">${levelSelect(o.levels)}</select></td>
        <td><button type="button" class="btn dir-toggle dir-${dir}" data-toggle-dir="${i}">${dirLabel}</button></td>
        <td class="recipe-breakdown">${escapeHtml(bom) || "identity restock"}</td>
        <td>${capInput(o.item)}</td>
        <td><input type="number" min="0" step="0.5" data-off="${i}" data-f="vanillaEmeralds" value="${o.vanillaEmeralds ?? 1}"></td>
        <td><input type="number" min="0" step="0.25" data-off="${i}" data-f="restockPerVillagerDay" value="${o.restockPerVillagerDay || 0}" ${restockDisabled}></td>
        <td style="white-space:nowrap">
          <button type="button" class="btn" data-dup-off="${i}">Dup</button>
          <button type="button" class="btn" data-del-off="${i}">Del</button>
        </td>
      </tr>`;
    }).join("") || `<tr><td colspan="9" class="empty-msg">Empty pool — search the item registry and add one item</td></tr>`;
    renderItemSearch();
    renderPcSearch("produce");
    renderPcSearch("consume");
  }

  function registryCount() {
    return (global.ItemRegistry && global.ItemRegistry.MC_ITEMS && global.ItemRegistry.MC_ITEMS.length) || 0;
  }

  function searchHits(q) {
    if (!q || !global.ItemRegistry || !global.ItemRegistry.searchItems) return [];
    return global.ItemRegistry.searchItems(q, 40);
  }

  function fillSearchResults(box, q, taken, itemAttr, extraAttrs, alreadyLabel, emptyHint) {
    if (!box) return;
    box.hidden = false;
    if (!q) {
      box.innerHTML = `<div class="item-search-hint">${emptyHint}</div>`;
      return;
    }
    const hits = searchHits(q);
    if (!hits.length) {
      box.innerHTML = `<div class="item-search-hint">No items match “${escapeHtml(q)}”.</div>`;
      return;
    }
    box.innerHTML = hits.map((it) => {
      const inPool = taken.has(it.id);
      let attrs = `${itemAttr}="${escapeHtml(it.id)}"`;
      for (const [k, v] of Object.entries(extraAttrs || {})) {
        attrs += ` ${k}="${escapeHtml(v)}"`;
      }
      return `<button type="button" ${attrs} ${inPool ? "class=\"in-pool\" disabled" : ""}>
        <span>${escapeHtml(it.name)}</span>
        <span class="item-id">${escapeHtml(it.id)}${inPool ? ` · ${alreadyLabel}` : ""}</span>
      </button>`;
    }).join("");
  }

  function renderItemSearch() {
    const box = document.getElementById("prof-item-results");
    const input = document.getElementById("prof-item-search");
    if (!box || !input) return;
    const prof = town.professions[selectedProf];
    const dir = (document.getElementById("prof-add-dir") || {}).value || "buy";
    const taken = new Set((prof.offers || []).filter((o) => o.direction === dir).map((o) => o.item));
    fillSearchResults(
      box,
      input.value.trim(),
      taken,
      "data-add-offer-item",
      null,
      "already in this direction",
      `Type an id or name — ${registryCount()} Minecraft items. Adds one offer to this profession.`
    );
  }

  function renderPcSearch(kind) {
    const box = document.getElementById(`prof-${kind}-results`);
    const input = document.getElementById(`prof-${kind}-search`);
    if (!box || !input) return;
    const list = town.professions[selectedProf][kind] || [];
    const taken = new Set(list.map((row) => row.commodity));
    fillSearchResults(
      box,
      input.value.trim(),
      taken,
      "data-add-pc-item",
      { "data-add-pc": kind },
      "already on this list",
      `Type an id or name — ${registryCount()} Minecraft items. Adds one ${kind} row.`
    );
  }

  function addPcItem(kind, itemId) {
    const reg = global.ItemRegistry && global.ItemRegistry.byId && global.ItemRegistry.byId[itemId];
    if (!reg) return;
    const p = town.professions[selectedProf];
    if (!p[kind]) p[kind] = [];
    if (p[kind].some((row) => row.commodity === itemId)) return;
    p[kind].push({ commodity: itemId, rate: 1 });
    ensureItemCap(itemId);
    refreshAll();
    persistSoon();
  }

  function addRegistryItem(itemId) {
    const reg = global.ItemRegistry && global.ItemRegistry.byId && global.ItemRegistry.byId[itemId];
    if (!reg) return;
    const p = town.professions[selectedProf];
    const dir = (document.getElementById("prof-add-dir") || {}).value || "buy";
    if ((p.offers || []).some((o) => o.item === itemId && o.direction === dir)) return;
    const recipes = typeof RECIPES !== "undefined" ? RECIPES : {};
    let components = {};
    if (dir === "sell") components = { [itemId]: 1 };
    else if (recipes[itemId]) components = L().clone(recipes[itemId]);
    L().addOffer(town, selectedProf, {
      id: uniqueOfferId(p, hashName(reg.name)),
      name: reg.name,
      item: itemId,
      direction: dir,
      levels: "Novice",
      components,
      restockPerVillagerDay: dir === "buy" ? 4 : 0,
    });
    ensureItemCap(itemId);
    for (const c of Object.keys(components)) ensureItemCap(c);
    refreshAll();
    persistSoon();
  }

  function renderLedger() {
    const tbody = document.getElementById("ledger-body");
    if (!tbody) return;
    const q = (document.getElementById("ledger-search").value || "").trim().toLowerCase();
    const snap = L().snapshot(town);
    let rows = snap.commodities.filter((r) => (Number(r.stock) || 0) > 0 && r.contributors.length > 0);
    if (q) rows = rows.filter((r) => `${r.id} ${r.contributors.join(" ")}`.toLowerCase().includes(q));
    tbody.innerHTML = rows.map((r) => {
      const need = r.need ? "Yes" : "";
      const kind = r.finished ? "finished" : "raw";
      return `<tr>
        <td><div class="item-name">${escapeHtml(displayName(r.id))}</div><div class="item-id">${escapeHtml(r.id)} · ${kind}</div></td>
        <td>${r.cap}</td>
        <td>${r.mid}</td>
        <td>${r.stock} <span class="item-id">(${Math.round(r.fill * 100)}%)</span></td>
        <td>${r.idleHeadroom}</td>
        <td>${r.idleShopDepth}</td>
        <td>${r.produce}</td>
        <td>${r.consume}</td>
        <td>${r.restockDrain}</td>
        <td>${r.restockFill}</td>
        <td>${r.daysToDigestFullDump ?? "—"} / ${r.daysToRefill ?? "—"}</td>
        <td class="recipe-breakdown">${escapeHtml(r.contributors.join(", "))}${need ? " · need" : ""}</td>
      </tr>`;
    }).join("") || `<tr><td colspan="12" class="empty-msg">No supply stock</td></tr>`;
    document.getElementById("ledger-stats").innerHTML =
      `<span><strong>${snap.totalVillagers}</strong> villagers</span>` +
      `<span><strong>${rows.length}</strong> shown</span>` +
      `<span>Wallet <strong>${town.scenario.emeralds}</strong> em</span>`;
  }

  function fillCommoditySelects() {
    const ids = L().listCommodityIds(town);
    for (const selId of ["scen-dump-item", "scen-need-item"]) {
      const sel = document.getElementById(selId);
      const cur = sel.value;
      sel.innerHTML = ids.map((id) => `<option value="${escapeHtml(id)}">${escapeHtml(displayName(id))}</option>`).join("");
      if (ids.includes(cur)) sel.value = cur;
    }
    const shop = document.getElementById("scen-shop-offer");
    const curS = shop.value;
    const offers = L().allBuyOffers(town);
    shop.innerHTML = offers.map(({ profession, offer }) =>
      `<option value="${escapeHtml(offer.id)}">${escapeHtml(profession)} — ${escapeHtml(offer.name)}</option>`
    ).join("");
    if ([...shop.options].some((o) => o.value === curS)) shop.value = curS;
  }

  function renderScenario() {
    fillCommoditySelects();
    document.getElementById("scen-emeralds").value = town.scenario.emeralds;
    document.getElementById("scen-income").value = town.scenario.emeraldIncomePerDay;
    const log = document.getElementById("scen-log");
    if (!log.dataset.ready) {
      log.textContent = "Idle at midpoint. Dump, shop, toggle a need, then tick days.";
      log.dataset.ready = "1";
    }
  }

  function renderInvariants() {
    const burn = town.scenario.repairBurn;
    document.getElementById("inv-burn-iron").value = burn.iron_ingot;
    document.getElementById("inv-burn-diamond").value = burn.diamond;
    document.getElementById("inv-burn-coal").value = burn.coal;
    const mine = town.scenario.miningYield;
    document.getElementById("inv-mine-iron").value = mine.iron_ingot;
    document.getElementById("inv-mine-diamond").value = mine.diamond;
    document.getElementById("inv-mine-coal").value = mine.coal;

    const inv = L().invariantReport(town);
    document.getElementById("inv-body").innerHTML = inv.anchors.map((a) => `<tr>
      <td>${escapeHtml(displayName(a.id))}</td>
      <td>${a.shoppablePerDay}</td>
      <td>${a.maxDump}</td>
      <td>${a.burn}</td>
      <td>${a.mining}</td>
    </tr>`).join("");
    const flags = document.getElementById("inv-flags");
    if (!inv.flags.length) {
      flags.innerHTML = `<span class="delta-ok">No invariant flags. Village is a valve, not a mine replacement (on these three anchors).</span>`;
    } else {
      flags.innerHTML = inv.flags.map((f) => `<div class="delta-high">${escapeHtml(f)}</div>`).join("");
    }
  }

  function refreshAll() {
    renderTown();
    renderProfession();
    renderLedger();
    renderScenario();
    renderInvariants();
  }

  function refreshLedgerSurfaces() {
    renderTown();
    renderLedger();
    renderScenario();
    renderInvariants();
  }

  function uniqueOfferId(prof, base) {
    const ids = new Set((prof.offers || []).map((o) => o.id));
    let id = base;
    let i = 2;
    while (ids.has(id)) id = `${base}_${i++}`;
    return id;
  }

  function logScen(msg) {
    const log = document.getElementById("scen-log");
    log.dataset.ready = "1";
    log.textContent = `${new Date().toLocaleTimeString()} — ${msg}\n` + (log.textContent || "");
  }

  function bind() {
    function applyTownPop(t) {
      const n = parseLiveNumber(t.value);
      if (n == null) return;
      if (t.dataset.pop) {
        L().setPopulation(town, t.dataset.pop, n);
        refreshLedgerSurfaces();
        persistSoon();
        return;
      }
      if (t.id === "town-unemployed") {
        L().setUnemployed(town, n);
        refreshLedgerSurfaces();
        persistSoon();
      }
    }
    document.getElementById("town-pop-grid").addEventListener("input", (e) => applyTownPop(e.target));
    document.getElementById("town-pop-grid").addEventListener("change", (e) => applyTownPop(e.target));

    function applyTownKnob(id, value) {
      const n = parseLiveNumber(value);
      if (n == null) return;
      const oldCaps = L().snapshotCaps(town);
      if (id === "town-cap-trader") town.knobs.defaultCapPerTrader = n;
      if (id === "town-cap-mouth") town.knobs.defaultCapPerMouth = n;
      if (id === "town-floor") town.knobs.restockRawFloor = n;
      if (id === "town-markup") town.knobs.markup = n || 1;
      if (id === "town-pulses") town.clock.pulsesPerDay = Math.max(1, n);
      L().rescaleStocksAfterCapChange(town, oldCaps);
      refreshLedgerSurfaces();
      persistSoon();
    }
    ["town-cap-trader", "town-cap-mouth", "town-floor", "town-markup", "town-pulses"].forEach((id) => {
      const el = document.getElementById(id);
      el.addEventListener("input", (e) => applyTownKnob(id, e.target.value));
      el.addEventListener("change", (e) => applyTownKnob(id, e.target.value));
    });

    document.getElementById("town-presets").addEventListener("click", (e) => {
      const key = e.target.dataset.preset;
      if (!key || !D().PRESETS[key]) return;
      L().applyPreset(town, D().PRESETS[key]);
      refreshAll();
      persistSoon();
    });

    document.getElementById("btn-reset-mid").addEventListener("click", () => {
      L().resetToMid(town);
      logScen("Reset stocks to midpoint.");
      refreshAll();
      persistSoon();
    });

    document.getElementById("btn-export-knobs").addEventListener("click", () => {
      const payload = {
        updated_at: new Date().toISOString(),
        mod_knobs: L().exportModKnobs(town),
        town_snapshot: {
          unemployed: town.unemployed,
          population: Object.fromEntries(PROF_ORDER.map((id) => [id, town.professions[id].n])),
        },
      };
      const blob = new Blob([JSON.stringify(payload, null, 2)], { type: "application/json" });
      const a = document.createElement("a");
      a.href = URL.createObjectURL(blob);
      a.download = "living-markets-knobs.json";
      a.click();
      URL.revokeObjectURL(a.href);
    });

    const sel = document.getElementById("prof-select");
    PROF_ORDER.forEach((id) => {
      const opt = document.createElement("option");
      opt.value = id;
      opt.textContent = id;
      sel.appendChild(opt);
    });
    sel.addEventListener("change", () => {
      selectedProf = sel.value;
      renderProfession();
    });
    document.getElementById("prof-item-search").addEventListener("input", renderItemSearch);
    document.getElementById("prof-add-dir").addEventListener("change", renderItemSearch);
    document.getElementById("prof-produce-search").addEventListener("input", () => renderPcSearch("produce"));
    document.getElementById("prof-consume-search").addEventListener("input", () => renderPcSearch("consume"));
    function applyProfN(value) {
      const n = parseLiveNumber(value);
      if (n == null) return;
      L().setPopulation(town, selectedProf, n);
      refreshLedgerSurfaces();
      persistSoon();
    }
    document.getElementById("prof-n").addEventListener("input", (e) => applyProfN(e.target.value));
    document.getElementById("prof-n").addEventListener("change", (e) => applyProfN(e.target.value));
    function applyProfessionEdit(t) {
      if (t.dataset.capItem) {
        const n = parseLiveNumber(t.value);
        if (n == null) return;
        setCapPerVillager(t.dataset.capItem, n);
        refreshLedgerSurfaces();
        persistSoon();
        return;
      }
      if (t.dataset.pc) {
        const list = town.professions[selectedProf][t.dataset.pc];
        const row = list[Number(t.dataset.i)];
        if (!row) return;
        if (t.dataset.f === "rate") {
          const n = parseLiveNumber(t.value);
          if (n == null) return;
          row.rate = n;
        } else {
          row[t.dataset.f] = t.value;
        }
        refreshLedgerSurfaces();
        persistSoon();
        return;
      }
      if (t.dataset.off != null) {
        const o = town.professions[selectedProf].offers[Number(t.dataset.off)];
        if (!o) return;
        const f = t.dataset.f;
        if (f === "enabled") o.enabled = t.checked;
        else if (f === "restockPerVillagerDay" || f === "vanillaEmeralds") {
          const n = parseLiveNumber(t.value);
          if (n == null) return;
          o[f] = n;
        }
        else if (f === "name") {
          o.name = t.value;
          o.id = uniqueOfferId(
            { offers: (town.professions[selectedProf].offers || []).filter((x) => x !== o) },
            hashName(o.name)
          );
        }
        else if (f === "id" || f === "item" || f === "components" || f === "direction") return;
        else o[f] = t.value;
        refreshLedgerSurfaces();
        persistSoon();
      }
    }
    document.getElementById("panel-profession").addEventListener("input", (e) => applyProfessionEdit(e.target));
    document.getElementById("panel-profession").addEventListener("change", (e) => applyProfessionEdit(e.target));
    document.getElementById("panel-profession").addEventListener("click", (e) => {
      const tog = e.target.closest("[data-toggle-dir]");
      if (tog) {
        const o = town.professions[selectedProf].offers[Number(tog.dataset.toggleDir)];
        if (!o) return;
        o.direction = o.direction === "buy" ? "sell" : "buy";
        if (o.direction === "sell") o.restockPerVillagerDay = 0;
        else if (!o.restockPerVillagerDay) o.restockPerVillagerDay = 4;
        refreshAll();
        persistSoon();
        return;
      }
      const addOffer = e.target.closest("[data-add-offer-item]");
      if (addOffer && !addOffer.disabled) {
        addRegistryItem(addOffer.dataset.addOfferItem);
        return;
      }
      const addPc = e.target.closest("[data-add-pc]");
      if (addPc && !addPc.disabled) {
        addPcItem(addPc.dataset.addPc, addPc.dataset.addPcItem);
        return;
      }
      const delPc = e.target.closest("[data-del-pc]");
      if (delPc) {
        town.professions[selectedProf][delPc.dataset.delPc].splice(Number(delPc.dataset.i), 1);
        refreshAll();
        persistSoon();
        return;
      }
      const delOff = e.target.closest("[data-del-off]");
      if (delOff) {
        L().removeOffer(town, selectedProf, Number(delOff.dataset.delOff));
        refreshAll();
        persistSoon();
        return;
      }
      const dup = e.target.closest("[data-dup-off]");
      if (dup) {
        const p = town.professions[selectedProf];
        const src = p.offers[Number(dup.dataset.dupOff)];
        if (!src) return;
        const copy = L().clone(src);
        copy.name = `${src.name} (copy)`;
        copy.id = uniqueOfferId(p, hashName(copy.name));
        p.offers.splice(Number(dup.dataset.dupOff) + 1, 0, copy);
        refreshAll();
        persistSoon();
      }
    });

    document.getElementById("btn-clear-pool").addEventListener("click", () => {
      if (!confirm(`Clear all offers for ${selectedProf}?`)) return;
      L().replaceProfessionPool(town, selectedProf, []);
      refreshAll();
      persistSoon();
    });

    document.getElementById("ledger-search").addEventListener("input", renderLedger);

    document.getElementById("btn-dump").addEventListener("click", () => {
      const id = document.getElementById("scen-dump-item").value;
      const amt = Number(document.getElementById("scen-dump-amt").value) || 0;
      const r = L().dump(town, id, amt);
      logScen(`Dump ${amt} ${id}: +${r.added.toFixed(1)} (stock ${r.stock.toFixed(1)} / cap ${r.cap.toFixed(1)})`);
      refreshAll();
      persistSoon();
    });
    document.getElementById("btn-shop").addEventListener("click", () => {
      const id = document.getElementById("scen-shop-offer").value;
      const amt = Number(document.getElementById("scen-shop-amt").value) || 0;
      const offer = L().findOffer(town, id);
      const r = L().shop(town, id, amt, offer ? fairForOffer(offer) : null);
      logScen(r.ok ? `Shop ${r.took}× ${id} for ${r.cost} em (${r.unitPrice}/ea)` : `Shop failed: ${r.reason}`);
      refreshAll();
      persistSoon();
    });
    document.getElementById("btn-need").addEventListener("click", () => {
      const id = document.getElementById("scen-need-item").value;
      const on = !town.needs[id];
      L().setNeed(town, id, on);
      logScen(`Need on ${id}: ${on ? "ON (produce paused)" : "off"}`);
      refreshAll();
      persistSoon();
    });
    document.getElementById("btn-tick").addEventListener("click", () => {
      const days = Number(document.getElementById("scen-days").value) || 1;
      L().tickDays(town, days);
      logScen(`Ticked ${days} day(s).`);
      refreshAll();
      persistSoon();
    });
    document.getElementById("scen-emeralds").addEventListener("change", (e) => {
      town.scenario.emeralds = Number(e.target.value) || 0;
      persistSoon();
    });
    document.getElementById("scen-income").addEventListener("change", (e) => {
      town.scenario.emeraldIncomePerDay = Number(e.target.value) || 0;
      persistSoon();
    });

    function bindInv(id, obj, key) {
      document.getElementById(id).addEventListener("change", (e) => {
        obj[key] = Number(e.target.value) || 0;
        renderInvariants();
        persistSoon();
      });
    }
    bindInv("inv-burn-iron", town.scenario.repairBurn, "iron_ingot");
    bindInv("inv-burn-diamond", town.scenario.repairBurn, "diamond");
    bindInv("inv-burn-coal", town.scenario.repairBurn, "coal");
    bindInv("inv-mine-iron", town.scenario.miningYield, "iron_ingot");
    bindInv("inv-mine-diamond", town.scenario.miningYield, "diamond");
    bindInv("inv-mine-coal", town.scenario.miningYield, "coal");
  }

  async function init() {
    town = await loadLab();
    if (!town.scenario) town.scenario = L().clone(D().DEFAULT_TOWN.scenario);
    L().ensureStocks(town);
    bind();
    refreshAll();
  }

  global.LedgerLabUI = { init, refreshAll, getTown: () => town };
})(typeof window !== "undefined" ? window : globalThis);
