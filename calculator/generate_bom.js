#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const D = require("./lab-defaults.js");

const bom = {
  source: "lab-defaults.js — profession offer pools (blank canvas; fill in the lab)",
  professions: {},
};
for (const [id, p] of Object.entries(D.PROFESSIONS)) {
  bom.professions[id] = p.offers.map((o) => ({
    id: o.id,
    name: o.name,
    direction: o.direction,
    levels: o.levels,
    item: o.item,
    components: o.components,
  }));
}

const out = path.join(__dirname, "bom.json");
fs.writeFileSync(out, JSON.stringify(bom, null, 2) + "\n");
console.log("wrote", out);
