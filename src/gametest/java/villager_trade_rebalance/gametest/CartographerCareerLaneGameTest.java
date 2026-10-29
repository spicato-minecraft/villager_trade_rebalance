package villager_trade_rebalance.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import villager_trade_rebalance.VillagerTradeRebalance;
import villager_trade_rebalance.cartographer.TreasureMapLocator;
import villager_trade_rebalance.map.CartographerMapPolicy;

public class CartographerCareerLaneGameTest {

	@GameTest(maxTicks = 200)
	public void noviceHasZeroLaneMarkedMaps(GameTestHelper context) {
		Villager villager = VillagerTradeRebalanceGameTestHelper.prepareCartographer(context, 1);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);

		context.succeedWhen(() -> {
			long lanes = VillagerTradeRebalanceGameTestHelper.markedLaneCount(villager);
			if (lanes != 0) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Novice cartographer must have zero lane-marked maps, had " + lanes);
			}
		});
	}

	@GameTest(maxTicks = 200)
	public void updateTradesKeepsT1WhenOtherRowsAreGlassOrPaper(GameTestHelper context) {
		Villager villager = VillagerTradeRebalanceGameTestHelper.prepareCartographer(context, 2);
		MerchantOffers seeded = new MerchantOffers();
		seeded.add(VillagerTradeRebalanceGameTestHelper.paperOffer());
		seeded.add(VillagerTradeRebalanceGameTestHelper.glassPaneOffer());
		villager.setOffers(seeded);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);

		context.succeedWhen(() -> {
			if (VillagerTradeRebalanceGameTestHelper.laneCount(villager, 1) != 1) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Expected T1 lane after updateTrades with glass/paper rows");
			}
			boolean keptFiller = villager.getOffers().stream().anyMatch(offer ->
					offer.getResult().is(Items.EMERALD) || offer.getResult().is(Items.GLASS_PANE)
							|| offer.getItemCostA().itemStack().is(Items.PAPER));
			if (!keptFiller) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Glass/paper rows should remain beside the T1 lane");
			}
		});
	}

	@GameTest(maxTicks = 200)
	public void journeymanHasTwoLanesWithRadii2bAndB(GameTestHelper context) {
		Villager villager = VillagerTradeRebalanceGameTestHelper.prepareCartographer(context, 3);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);
		int base = VillagerTradeRebalance.CONFIG.cartographerMaps().baseRadius();

		context.succeedWhen(() -> {
			assertLanes(villager, 1, 2);
			if (VillagerTradeRebalanceGameTestHelper.laneCount(villager, 3) != 0) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Journeyman must not unlock T3");
			}
			stampAndAssertRadius(context, villager, 1, CartographerMapPolicy.radius(3, 1, base).orElseThrow());
			stampAndAssertRadius(context, villager, 2, CartographerMapPolicy.radius(3, 2, base).orElseThrow());
		});
	}

	@GameTest(maxTicks = 200)
	public void levelTwoToThreeAddsT2AndT1Uses2b(GameTestHelper context) {
		Villager villager = VillagerTradeRebalanceGameTestHelper.prepareCartographer(context, 2);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);

		context.succeedWhen(() -> {
			if (VillagerTradeRebalanceGameTestHelper.laneCount(villager, 1) != 1) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Apprentice should start with T1");
			}
			if (VillagerTradeRebalanceGameTestHelper.laneCount(villager, 2) != 0) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Apprentice should not have T2 yet");
			}

			VillagerTradeRebalanceGameTestHelper.setLevel(villager, 3);
			VillagerTradeRebalanceGameTestHelper.updateTrades(villager);

			assertLanes(villager, 1, 2);
			int base = VillagerTradeRebalance.CONFIG.cartographerMaps().baseRadius();
			stampAndAssertRadius(context, villager, 1, base * 2);
		});
	}

	@GameTest(maxTicks = 200)
	public void expertHasLanesOneAndTwoButNoLaneThree(GameTestHelper context) {
		Villager villager = VillagerTradeRebalanceGameTestHelper.prepareCartographer(context, 4);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);

		context.succeedWhen(() -> {
			assertLanes(villager, 1, 2);
			if (VillagerTradeRebalanceGameTestHelper.laneCount(villager, 3) != 0) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Expert must not unlock T3");
			}
		});
	}

	@GameTest(maxTicks = 200)
	public void masterHasThreeLanesWithRadii4b2bAndB(GameTestHelper context) {
		Villager villager = VillagerTradeRebalanceGameTestHelper.prepareCartographer(context, 5);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);
		int base = VillagerTradeRebalance.CONFIG.cartographerMaps().baseRadius();

		context.succeedWhen(() -> {
			assertLanes(villager, 1, 2, 3);
			stampAndAssertRadius(context, villager, 1, base * 4);
			stampAndAssertRadius(context, villager, 2, base * 2);
			stampAndAssertRadius(context, villager, 3, base);
		});
	}

	private static void assertLanes(Villager villager, int... lanes) {
		for (int lane : lanes) {
			if (VillagerTradeRebalanceGameTestHelper.laneCount(villager, lane) != 1) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Expected exactly one lane-" + lane
						+ " offer, had " + VillagerTradeRebalanceGameTestHelper.laneCount(villager, lane));
			}
			MerchantOffer offer = VillagerTradeRebalanceGameTestHelper.requireLane(villager, lane);
			if (offer.getResult().has(DataComponents.MAP_ID)) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Lane " + lane + " stored offer must stay undecided");
			}
		}
	}

	private static void stampAndAssertRadius(GameTestHelper context, Villager villager, int lane, int expectedRadius) {
		MerchantOffer stored = VillagerTradeRebalanceGameTestHelper.requireLane(villager, lane);
		ItemStack delivered = stored.assemble();
		TreasureMapLocator.stampDelivered(context.getLevel(), villager, stored, delivered);
		if (TreasureMapLocator.lastSearchRadiusForTest() != expectedRadius) {
			throw VillagerTradeRebalanceGameTestHelper.fail("Lane " + lane + " radius should be " + expectedRadius
					+ " but was " + TreasureMapLocator.lastSearchRadiusForTest());
		}
		if (stored.getResult().has(DataComponents.MAP_ID)) {
			throw VillagerTradeRebalanceGameTestHelper.fail("stampDelivered wrote map_id onto stored lane " + lane);
		}
	}
}
