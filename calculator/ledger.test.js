#!/usr/bin/env node
"use strict";

const assert = require("assert");
const Ledger = require("./ledger.js");
const Defaults = require("./lab-defaults.js");

function town() {
  const t = Ledger.clone(Defaults.DEFAULT_TOWN);
  Ledger.resetToMid(t);
  return t;
}

function testCapScalesWithPopulation() {
  const t = town();
  const iron0 = Ledger.capOf(t, "iron_ingot");
  Ledger.setPopulation(t, "armorer", (t.professions.armorer.n || 0) + 8);
  const iron1 = Ledger.capOf(t, "iron_ingot");
  assert.ok(iron1 > iron0, "more armorers should raise iron cap");
}

function testSharedIronTraders() {
  const t = town();
  const traders = Ledger.tradersOf(t, "iron_ingot");
  const expected =
    t.professions.armorer.n +
    t.professions.toolsmith.n +
    t.professions.weaponsmith.n;
  assert.equal(traders, expected);
}

function testMeanReversionDump() {
  const t = town();
  const mid = Ledger.midOf(t, "wheat");
  Ledger.dump(t, "wheat", 1000);
  const afterDump = t.stocks.wheat;
  assert.ok(afterDump > mid);
  Ledger.tickDays(t, 20);
  assert.ok(t.stocks.wheat <= afterDump);
  assert.ok(t.stocks.wheat >= mid * 0.9, "consume should walk glut back toward mid");
}

function testNeedPausesProduce() {
  const t = town();
  t.stocks.wheat = 1;
  Ledger.setNeed(t, "wheat", true);
  const before = t.stocks.wheat;
  Ledger.tickDays(t, 5);
  assert.equal(t.stocks.wheat, before, "need should pause wheat produce");
  Ledger.setNeed(t, "wheat", false);
  Ledger.tickDays(t, 5);
  assert.ok(t.stocks.wheat > before, "produce should resume after need ends");
}

function testRestockDrainsRaw() {
  const t = town();
  Ledger.addOffer(t, "armorer", {
    id: "iron_armor",
    item: "iron_chestplate",
    direction: "buy",
    components: { iron_ingot: 8 },
    restockPerVillagerDay: 4,
  });
  t.stocks.iron_chestplate = 0;
  t.stocks.iron_ingot = Ledger.capOf(t, "iron_ingot") * 0.8;
  const ironBefore = t.stocks.iron_ingot;
  Ledger.tickDay(t);
  assert.ok(t.stocks.iron_chestplate > 0, "restock should fill finished iron armor");
  assert.ok(t.stocks.iron_ingot < ironBefore, "restock should drain iron");
}

function testShopAndDump() {
  const t = town();
  Ledger.addOffer(t, "farmer", {
    id: "farmer_bread",
    item: "bread",
    direction: "buy",
    components: { wheat: 3 },
    restockPerVillagerDay: 4,
  });
  t.scenario.emeralds = 999;
  t.stocks.bread = 10;
  const r = Ledger.shop(t, "farmer_bread", 2, 1);
  assert.ok(r.ok);
  assert.equal(t.stocks.bread, 8);
  const d = Ledger.dump(t, "wheat", 5);
  assert.ok(d.added > 0);
}

function testExportKnobs() {
  const t = town();
  Ledger.addOffer(t, "armorer", {
    id: "iron_armor",
    item: "iron_chestplate",
    direction: "buy",
    components: { iron_ingot: 8 },
    restockPerVillagerDay: 4,
  });
  const knobs = Ledger.exportModKnobs(t);
  assert.equal(knobs.default_cap_per_trader, 32);
  assert.ok(knobs.professions.armorer.offers.length > 0);
  assert.ok("iron_chestplate" in knobs.professions.armorer.restock_per_villager_day);
}

function testInvariantsShape() {
  const t = town();
  const inv = Ledger.invariantReport(t);
  assert.equal(inv.anchors.length, 3);
  assert.ok(inv.totalVillagers > 0);
}

function testHamletVsIronHallCap() {
  const a = town();
  Ledger.applyPreset(a, Defaults.PRESETS.hamlet);
  Ledger.resetToMid(a);
  const hamlet = Ledger.capOf(a, "iron_ingot");
  const b = town();
  Ledger.applyPreset(b, Defaults.PRESETS.iron_hall);
  Ledger.resetToMid(b);
  const hall = Ledger.capOf(b, "iron_ingot");
  assert.ok(hall > hamlet * 2, "iron hall should dwarf hamlet iron cap");
}

function testParseComponents() {
  const a = Ledger.parseComponents("iron_ingot:6, stick:2");
  assert.equal(a.iron_ingot, 6);
  assert.equal(a.stick, 2);
  const b = Ledger.parseComponents("3x cobblestone");
  assert.equal(b.cobblestone, 3);
}

function testRebuildPoolAddsTank() {
  const t = town();
  const before = Ledger.listCommodityIds(t).includes("prismarine");
  assert.equal(before, false);
  Ledger.replaceProfessionPool(t, "mason", []);
  Ledger.addOffer(t, "mason", {
    name: "Prismarine bricks",
    item: "prismarine_bricks",
    direction: "buy",
    components: { prismarine: 4 },
    restockPerVillagerDay: 4,
  });
  assert.ok(Ledger.listCommodityIds(t).includes("prismarine"));
  assert.ok(Ledger.listCommodityIds(t).includes("prismarine_bricks"));
  assert.ok(Ledger.tradersOf(t, "prismarine") > 0);
}

function testIdentityRestockFillsRaw() {
  const t = town();
  const offer = Ledger.addOffer(t, "toolsmith", {
    id: "tool_buy_iron",
    item: "iron_ingot",
    direction: "buy",
    components: {},
    restockPerVillagerDay: 4,
  });
  assert.ok(Ledger.isIdentityRecipe(offer));
  t.stocks.iron_ingot = 0;
  Ledger.tickDay(t);
  assert.ok(t.stocks.iron_ingot > 0, "identity restock should fill iron without a BOM");
}

function testIdleProfessionDoesNotListCommodity() {
  const t = town();
  t.professions.shepherd.n = 0;
  t.stocks.wool = 8;
  Ledger.ensureStocks(t);
  assert.ok(!Ledger.listCommodityIds(t).includes("wool"));
  assert.equal(t.stocks.wool, 8, "idle tanks should keep saved stock");
}

testCapScalesWithPopulation();
testSharedIronTraders();
testMeanReversionDump();
testNeedPausesProduce();
testRestockDrainsRaw();
testShopAndDump();
testExportKnobs();
testInvariantsShape();
testHamletVsIronHallCap();
testParseComponents();
testRebuildPoolAddsTank();
testIdentityRestockFillsRaw();
testIdleProfessionDoesNotListCommodity();
console.log("ledger.test.js: all passed");
