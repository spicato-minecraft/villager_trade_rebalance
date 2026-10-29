package villager_trade_rebalance.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import villager_trade_rebalance.VillagerTradeRebalance;
import villager_trade_rebalance.cartographer.CartographerDestinations;
import villager_trade_rebalance.cartographer.TreasureMapLocator;
import villager_trade_rebalance.config.ModConfig;
import villager_trade_rebalance.map.LocateDecision;

public class CartographerStampAndLocateGameTest {

	@GameTest(maxTicks = 200)
	public void secondStampLeavesStoredWithoutMapId(GameTestHelper context) {
		Villager villager = VillagerTradeRebalanceGameTestHelper.prepareCartographer(context, 2);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);
		MerchantOffer stored = VillagerTradeRebalanceGameTestHelper.requireLane(villager, 1);
		ItemStack first = stored.assemble();
		TreasureMapLocator.stampDelivered(context.getLevel(), villager, stored, first);
		ItemStack second = stored.assemble();
		TreasureMapLocator.stampDelivered(context.getLevel(), villager, stored, second);

		context.succeedWhen(() -> {
			if (stored.getResult().has(DataComponents.MAP_ID)) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Second stamp must not bake map_id onto the stored offer");
			}
		});
	}

	@GameTest(maxTicks = 200)
	public void plainsVariantExcludesPlainsVillage(GameTestHelper context) {
		Villager villager = VillagerTradeRebalanceGameTestHelper.prepareCartographer(context, 2);
		VillagerTradeRebalanceGameTestHelper.setVillagerType(villager, VillagerType.PLAINS);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);

		context.succeedWhen(() -> {
			boolean includesHomeVillage = CartographerDestinations.eligibleTypes(1, CartographerDestinations.VARIANT_PLAINS)
					.stream()
					.anyMatch(spec -> CartographerDestinations.PLAINS_VILLAGE.equals(spec.typeId()));
			if (includesHomeVillage) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Plains cartographer T1 catalog must exclude plains_village");
			}
			MerchantOffer stored = VillagerTradeRebalanceGameTestHelper.requireLane(villager, 1);
			ItemStack delivered = stored.assemble();
			LocateDecision decision = TreasureMapLocator.stampDelivered(context.getLevel(), villager, stored, delivered);
			if (decision == LocateDecision.LOCATE && delivered.is(Items.PLAINS_VILLAGE_MAP)) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Plains variant must not deliver plains_village_map");
			}
		});
	}

	@GameTest(maxTicks = 200)
	public void tinyRadiusAbortsWithoutWritingMapId(GameTestHelper context) {
		Villager villager = VillagerTradeRebalanceGameTestHelper.prepareCartographer(context, 2);
		VillagerTradeRebalance.CONFIG = ModConfig.parse("""
				{ "cartographer_maps": { "base_radius": 1 } }
				""");
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);
		MerchantOffer stored = VillagerTradeRebalanceGameTestHelper.requireLane(villager, 1);
		ItemStack delivered = stored.assemble();
		LocateDecision decision = TreasureMapLocator.stampDelivered(context.getLevel(), villager, stored, delivered);

		context.succeedWhen(() -> {
			try {
				if (decision != LocateDecision.ABORT_AND_LOCK) {
					throw VillagerTradeRebalanceGameTestHelper.fail("base_radius 1 should abort locate, was " + decision);
				}
				if (stored.getResult().has(DataComponents.MAP_ID) || delivered.has(DataComponents.MAP_ID)) {
					throw VillagerTradeRebalanceGameTestHelper.fail("Abort must leave stored and delivered without map_id");
				}
			} finally {
				VillagerTradeRebalanceGameTestHelper.restoreDefaultConfig();
			}
		});
	}

	@GameTest(maxTicks = 200)
	public void successfulLocateLeavesStoredUndecidedForRestamp(GameTestHelper context) {
		Villager villager = VillagerTradeRebalanceGameTestHelper.prepareCartographer(context, 2);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);
		MerchantOffer stored = VillagerTradeRebalanceGameTestHelper.requireLane(villager, 1);
		ItemStack first = stored.assemble();
		LocateDecision firstDecision = TreasureMapLocator.stampDelivered(context.getLevel(), villager, stored, first);

		context.succeedWhen(() -> {
			if (stored.getResult().has(DataComponents.MAP_ID)) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Stored offer uses must not bake a located map");
			}
			if (stored.assemble().has(DataComponents.MAP_ID)) {
				throw VillagerTradeRebalanceGameTestHelper.fail("assemble() after stamp must still be undecided");
			}
			if (firstDecision == LocateDecision.LOCATE && !first.has(DataComponents.MAP_ID)) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Successful locate should stamp the delivered clone");
			}
			ItemStack second = stored.assemble();
			TreasureMapLocator.stampDelivered(context.getLevel(), villager, stored, second);
			if (stored.getResult().has(DataComponents.MAP_ID)) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Second stamp stored offer must still have no map_id");
			}
		});
	}

	@GameTest(maxTicks = 200)
	public void stampDoesNotThrowWhenSomeTagsAreMissing(GameTestHelper context) {
		Villager villager = VillagerTradeRebalanceGameTestHelper.prepareCartographer(context, 3);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);
		MerchantOffer stored = VillagerTradeRebalanceGameTestHelper.requireLane(villager, 2);
		ItemStack delivered = stored.assemble();

		context.succeedWhen(() -> {
			TreasureMapLocator.stampDelivered(context.getLevel(), villager, stored, delivered);
			if (stored.getResult().has(DataComponents.MAP_ID)) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Missing-tag fallback must not bake the stored offer");
			}
		});
	}
}
