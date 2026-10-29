package villager_trade_rebalance.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import villager_trade_rebalance.cartographer.CartographerOfferRewriter;
import villager_trade_rebalance.cartographer.TreasureMapLocator;
import villager_trade_rebalance.map.LocateDecision;
import villager_trade_rebalance.persist.CartographerTradeData;

public class CartographerUndecidedLanesGameTest {

	/** G2 + G13: apprentice T1 lane, emerald+compass payment, stored offer has no map_id. */
	@GameTest(maxTicks = 200)
	public void apprenticeOffersUndecidedLaneWithoutMapId(GameTestHelper context) {
		VillagerTradeRebalanceGameTestHelper.restoreDefaultConfig();
		VillagerTradeRebalanceGameTestHelper.buildFloor(context, 4);
		context.setBlock(2, 1, 2, Blocks.CARTOGRAPHY_TABLE);

		Villager villager = VillagerTradeRebalanceGameTestHelper.spawnVillager(context, 1, 1, 1);
		VillagerTradeRebalanceGameTestHelper.assignProfession(villager, VillagerProfession.CARTOGRAPHER);
		VillagerTradeRebalanceGameTestHelper.setLevel(villager, 2);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);

		context.succeedWhen(() -> {
			long lanes = villager.getOffers().stream()
					.filter(offer -> TreasureMapLocator.laneOf(offer).orElse(-1) == 1)
					.count();
			if (lanes != 1) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Expected exactly one lane-1 treasure map, had " + lanes);
			}
			MerchantOffer lane = villager.getOffers().stream()
					.filter(offer -> TreasureMapLocator.laneOf(offer).orElse(-1) == 1)
					.findFirst()
					.orElseThrow();
			if (lane.getResult().has(DataComponents.MAP_ID)) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Stored lane offer must stay undecided (no map_id)");
			}
			if (!lane.getItemCostA().itemStack().is(Items.EMERALD) || lane.getItemCostB().isEmpty()
					|| !lane.getItemCostB().get().itemStack().is(Items.COMPASS)) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Lane offer must cost emeralds and a compass");
			}
			boolean leftoverVanilla = villager.getOffers().stream().anyMatch(offer ->
					CartographerOfferRewriter.isVanillaLocatedMap(offer, CartographerOfferRewriter.catalogItemIds()));
			if (leftoverVanilla) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Vanilla located maps should be stripped");
			}
			if (!((CartographerTradeData) villager).villager_trade_rebalance$getCartographerState().initialized()) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Cartographer should be marked new-rule after generation");
			}
		});
	}

	/** G4: stampDelivered writes map_id on the delivered clone only (never notifyTrade). */
	@GameTest(maxTicks = 200)
	public void stampWritesMapIdOnDeliveredCloneOnly(GameTestHelper context) {
		VillagerTradeRebalanceGameTestHelper.restoreDefaultConfig();
		VillagerTradeRebalanceGameTestHelper.buildFloor(context, 4);
		context.setBlock(2, 1, 2, Blocks.CARTOGRAPHY_TABLE);

		Villager villager = VillagerTradeRebalanceGameTestHelper.spawnVillager(context, 1, 1, 1);
		VillagerTradeRebalanceGameTestHelper.assignProfession(villager, VillagerProfession.CARTOGRAPHER);
		VillagerTradeRebalanceGameTestHelper.setLevel(villager, 2);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);

		MerchantOffer stored = villager.getOffers().stream()
				.filter(offer -> TreasureMapLocator.laneOf(offer).orElse(-1) == 1)
				.findFirst()
				.orElseThrow(() -> VillagerTradeRebalanceGameTestHelper.fail("Missing undecided lane"));
		ItemStack delivered = stored.assemble();
		LocateDecision decision = TreasureMapLocator.stampDelivered(context.getLevel(), villager, stored, delivered);

		context.succeedWhen(() -> {
			if (stored.getResult().has(DataComponents.MAP_ID)) {
				throw VillagerTradeRebalanceGameTestHelper.fail("stampDelivered wrote map_id onto the stored template");
			}
			if (decision == LocateDecision.LOCATE) {
				if (!delivered.has(DataComponents.MAP_ID)) {
					throw VillagerTradeRebalanceGameTestHelper.fail("Successful locate must stamp the delivered clone");
				}
				String expectedId = "#" + delivered.get(DataComponents.MAP_ID).id();
				var lore = delivered.get(DataComponents.LORE);
				boolean showsId = lore != null && lore.lines().stream()
						.anyMatch(line -> expectedId.equals(line.getString()));
				boolean showsTier = lore != null && lore.lines().stream()
						.anyMatch(line -> "Tier 1".equals(line.getString()));
				if (!showsTier || !showsId) {
					throw VillagerTradeRebalanceGameTestHelper.fail(
							"Delivered map lore must show tier and map id, had " + lore);
				}
			} else if (decision == LocateDecision.ABORT_AND_LOCK) {
				if (delivered.has(DataComponents.MAP_ID)) {
					throw VillagerTradeRebalanceGameTestHelper.fail("Abort must not emit a located map");
				}
			} else {
				throw VillagerTradeRebalanceGameTestHelper.fail("Unexpected locate decision " + decision);
			}
		});
	}
}
