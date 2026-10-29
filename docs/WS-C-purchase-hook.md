# WS-C purchase hook (26.3)

Read-only discovery against `minecraft-merged-deobf-26.3`. No production mixin. Librarian code is out of scope.

Jar: `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.3/minecraft-merged-deobf-26.3.jar`

26.3 packages (Yarn-style names moved under `npc.villager`):

- `net.minecraft.world.item.trading.MerchantOffer`
- `net.minecraft.world.item.trading.Merchant`
- `net.minecraft.world.entity.npc.villager.AbstractVillager`
- `net.minecraft.world.entity.npc.villager.Villager` (does **not** override `notifyTrade`)
- `net.minecraft.world.inventory.MerchantMenu`
- `net.minecraft.world.inventory.MerchantResultSlot`
- `net.minecraft.world.inventory.MerchantContainer` (not requested; required to see `assemble`)

## Recommended hook

**Class:** `net.minecraft.world.inventory.MerchantResultSlot`  
**Method:** `onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V`  
**Injection:** `@At("HEAD")`

`onTake`’s `ItemStack` argument is the **delivered** result: the object already sitting in merchant slot 2 after `MerchantOffer.assemble()`. Mutate that argument (write `map_id` / destination there). Read the stored offer only through `MerchantContainer.getActiveOffer()` (field `slots` on the result slot) so `vtr:map_lane` can be copied off the template. Do **not** call `offer.getResult().set(...)` and do **not** write `map_id` onto that field.

`HEAD` runs before `checkTakeAchievements`, payment `take()`, and `Merchant.notifyTrade`, so the player-bound clone is still the live object and LVT’s later lock is unaffected.

## Why `Merchant.notifyTrade` is too late

Purchase order in 26.3:

1. `MerchantContainer.updateSellItem` matches a live `MerchantOffer` from `Merchant.getOffers()`, then `setItem(2, offer.assemble())`.
2. `MerchantOffer.assemble()` is `this.result.copy()`. The stored offer’s `result` field is left untouched.
3. `MerchantOffer.getResult()` returns that **same field** (no copy).
4. Click / shift-click removes the slot-2 clone. `MerchantResultSlot.onTake(player, deliveredClone)` runs.
5. Only then `merchant.notifyTrade(activeOffer)` — argument is the **stored** offer, not the delivered stack.
6. `AbstractVillager.notifyTrade` bumps uses, awards XP, and fires `CriteriaTriggers.TRADE` with `offer.getResult()` (the stored field again). `Villager` does not override this.

If locate/stamp happens at `notifyTrade`:

- The player already holds a clone from step 2. Mutating `offer.getResult()` writes `map_id` onto the merchant’s persistent template (and every later `assemble()` copy).
- The method has no `ItemStack` parameter for the delivered clone. There is nothing left to stamp without touching the stored field.

`Merchant.notifyTradeUpdated(ItemStack)` is the preview-slot callback from `updateSellItem`. It fires on every payment change, not on purchase.

## Alternative rejected

| Target | Why not |
|---|---|
| `Merchant.notifyTrade` / `AbstractVillager.notifyTrade` (`HEAD` or `TAIL`) | Too late; no delivered stack; `getResult()` is the stored field. `TAIL` is already LVT lock. |
| `MerchantOffer.assemble` `@At("RETURN")` | Returns a copy (safe for the stored field) but `updateSellItem` calls it on every payment/selection change. Stamping `map_id` here locates on **preview**, burns map IDs, and still is not “on purchase.” |
| `MerchantOffer.getResult` | Live field. Any mutation is a stored-offer write. |
| `MerchantContainer.updateSellItem` after `assemble` | Same preview-time problem as `assemble`. |
| `MerchantMenu.quickMoveStack` | Shift-click copies the slot stack **before** `onTake` (`ItemStack.copy()` into the return local). The inventory move uses the slot object; hooking the menu is easy to miss the click-take path. `onTake` covers both. |

## LVT interaction (notifyTrade lock, offer copies)

`locked_villager_trades` `VillagerTradeLockMixin` injects `AbstractVillager.notifyTrade` at **`TAIL`**: if the villager is unlocked and has 2+ trade sets, it locks the current set and records level.

`MerchantMenuMixin` (LVT) is menu **ctor `TAIL`** plus `showProgressBar`. It syncs selected/locked set index. Switching sets does `LockedTradesStorage.copyOffers(selected)` then `villager.setOffers(...)`. `MerchantOffer.copy()` uses the copy ctor, which `ItemStack.copy()`s `result`. A stored `vtr:map_lane` survives the snapshot; a stored `map_id` would also survive and bake the destination into every LVT copy.

Implications for WS-C:

- Purchase mixin must **not** share `notifyTrade` `TAIL` with the lock.
- `onTake` `HEAD` is earlier than `notifyTrade`. LVT still sees a completed trade and locks as today.
- LVT’s offer copies are why the **stored** result must stay undecided: `copy()` would duplicate a baked `map_id` into the locked snapshot.

## Stored offer contract

The merchant’s stored `MerchantOffer.result` **must** keep a custom component later named `vtr:map_lane` and **must not** receive `map_id`.

- Lane identity lives on the template so LVT snapshots, menu preview, and restock all show the same undecided lane.
- `map_id` / exploration destination belong only on the delivered clone produced by `assemble()`.
- `getResult()` is the template; `assemble()` is the per-take copy. Stamp the copy.

## javap notes used

```
MerchantOffer.getResult()     → return this.result;
MerchantOffer.assemble()      → return this.result.copy();
MerchantOffer.copy()          → new MerchantOffer(this)  // result.copy()
MerchantContainer.updateSellItem
  → activeOffer = matched offer
  → setItem(2, activeOffer.assemble())
MerchantResultSlot.onTake
  → checkTakeAchievements(stack)
  → offer = slots.getActiveOffer()
  → offer.take(payA, payB)
  → merchant.notifyTrade(offer)
AbstractVillager.notifyTrade  → increaseUses, rewardTradeXp, TRADE trigger(getResult())
Merchant                      → abstract notifyTrade(MerchantOffer)
Villager                      → no notifyTrade override
```
