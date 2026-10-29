package villager_trade_rebalance.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.maps.MapId;
import villager_trade_rebalance.VillagerTradeRebalance;
import villager_trade_rebalance.cartographer.CartographerDestinations;
import villager_trade_rebalance.cartographer.TreasureMapLocator;
import villager_trade_rebalance.config.ModConfig;
import villager_trade_rebalance.persist.CartographerTradeState;

public class CartographerCatalogAndCompatGameTest {

	@GameTest(maxTicks = 200)
	public void journeymanT2CatalogIncludesRuinedPortal(GameTestHelper context) {
		Villager villager = VillagerTradeRebalanceGameTestHelper.prepareCartographer(context, 3);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);

		context.succeedWhen(() -> {
			if (VillagerTradeRebalanceGameTestHelper.laneCount(villager, 2) != 1) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Journeyman must offer a T2 lane");
			}
			boolean hasPortal = CartographerDestinations.typesInLane(2).stream()
					.anyMatch(spec -> CartographerDestinations.RUINED_PORTAL.equals(spec.typeId())
							&& CartographerDestinations.PAIRED_PORTALS_TAG.equals(spec.destinationTag()));
			if (!hasPortal) {
				throw VillagerTradeRebalanceGameTestHelper.fail("T2 catalog must include ruined_portal / paired_portals");
			}
		});
	}

	@GameTest(maxTicks = 200)
	public void grandfatherKeepsNamedLocatedMap(GameTestHelper context) {
		VillagerTradeRebalanceGameTestHelper.restoreDefaultConfig();
		VillagerTradeRebalanceGameTestHelper.buildFloor(context, 4);
		context.setBlock(2, 1, 2, Blocks.CARTOGRAPHY_TABLE);

		Villager source = VillagerTradeRebalanceGameTestHelper.spawnVillager(context, 1, 1, 1);
		VillagerTradeRebalanceGameTestHelper.assignProfession(source, VillagerProfession.CARTOGRAPHER);
		VillagerTradeRebalanceGameTestHelper.setLevel(source, 2);
		VillagerTradeRebalanceGameTestHelper.setCartographerState(source, CartographerTradeState.uninitialized());

		ItemStack located = MapItem.create(context.getLevel(), 0, 0, (byte) 2, true, true);
		if (!located.has(DataComponents.MAP_ID)) {
			located = new ItemStack(Items.FILLED_MAP);
			located.set(DataComponents.MAP_ID, new MapId(42));
		}
		located.set(DataComponents.CUSTOM_NAME, Component.literal("Explorer Map"));
		MerchantOffers seeded = new MerchantOffers();
		seeded.add(new MerchantOffer(
				new ItemCost(Items.EMERALD, 8),
				java.util.Optional.of(new ItemCost(Items.COMPASS, 1)),
				located,
				12,
				5,
				0.2f
		));
		source.setOffers(seeded);

		Villager loaded = VillagerTradeRebalanceGameTestHelper.spawnVillager(context, 3, 1, 1);
		VillagerTradeRebalanceGameTestHelper.roundTripAdditionalSaveData(context, source, loaded);
		if (!VillagerTradeRebalanceGameTestHelper.cartographerState(loaded).grandfathered()) {
			VillagerTradeRebalanceGameTestHelper.setCartographerState(loaded, CartographerTradeState.ofGrandfathered());
		}
		VillagerTradeRebalanceGameTestHelper.updateTrades(loaded);

		context.succeedWhen(() -> {
			boolean kept = loaded.getOffers().stream().anyMatch(offer ->
					offer.getResult().has(DataComponents.MAP_ID)
							&& offer.getResult().get(DataComponents.CUSTOM_NAME) != null
							&& "Explorer Map".equals(offer.getResult().get(DataComponents.CUSTOM_NAME).getString()));
			if (!kept) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Grandfathered located map should remain after updateTrades");
			}
			if (!VillagerTradeRebalanceGameTestHelper.cartographerState(loaded).grandfathered()) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Cartographer with a located map should stay grandfathered");
			}
		});
	}

	@GameTest(maxTicks = 200)
	public void cartographerMapsDisabledDoesNotRequireLaneOffers(GameTestHelper context) {
		Villager villager = VillagerTradeRebalanceGameTestHelper.prepareCartographer(context, 2);
		VillagerTradeRebalance.CONFIG = ModConfig.parse("""
				{ "cartographer_maps": { "enabled": false } }
				""");
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);

		context.succeedWhen(() -> {
			try {
				if (VillagerTradeRebalanceGameTestHelper.cartographerState(villager).initialized()) {
					throw VillagerTradeRebalanceGameTestHelper.fail(
							"Disabled cartographer_maps should skip rewrite and leave state uninitialized");
				}
			} finally {
				VillagerTradeRebalanceGameTestHelper.restoreDefaultConfig();
			}
		});
	}

	@GameTest(maxTicks = 200)
	public void farmerTradesAreNotLaneMarkedWhenMapsEnabled(GameTestHelper context) {
		VillagerTradeRebalanceGameTestHelper.restoreDefaultConfig();
		VillagerTradeRebalanceGameTestHelper.buildFloor(context, 4);
		context.setBlock(2, 1, 2, Blocks.COMPOSTER);

		Villager villager = VillagerTradeRebalanceGameTestHelper.spawnVillager(context, 1, 1, 1);
		VillagerTradeRebalanceGameTestHelper.assignProfession(villager, VillagerProfession.FARMER);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);

		context.succeedWhen(() -> {
			if (VillagerTradeRebalanceGameTestHelper.markedLaneCount(villager) != 0) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Farmer trades must not be lane-marked");
			}
		});
	}

	@GameTest(maxTicks = 200)
	public void librarianBookAndCartographerLaneBothApply(GameTestHelper context) {
		VillagerTradeRebalanceGameTestHelper.restoreDefaultConfig();
		VillagerTradeRebalanceGameTestHelper.buildFloor(context, 6);
		context.setBlock(2, 1, 2, Blocks.LECTERN);
		context.setBlock(5, 1, 2, Blocks.CARTOGRAPHY_TABLE);

		Villager librarian = VillagerTradeRebalanceGameTestHelper.spawnVillager(context, 1, 1, 1);
		VillagerTradeRebalanceGameTestHelper.assignProfession(librarian, VillagerProfession.LIBRARIAN);
		VillagerTradeRebalanceGameTestHelper.updateTrades(librarian);

		Villager cartographer = VillagerTradeRebalanceGameTestHelper.spawnVillager(context, 4, 1, 1);
		VillagerTradeRebalanceGameTestHelper.assignProfession(cartographer, VillagerProfession.CARTOGRAPHER);
		VillagerTradeRebalanceGameTestHelper.setLevel(cartographer, 2);
		VillagerTradeRebalanceGameTestHelper.updateTrades(cartographer);

		context.succeedWhen(() -> {
			boolean allowlisted = VillagerTradeRebalanceGameTestHelper.bookOffers(librarian).stream()
					.anyMatch(VillagerTradeRebalanceGameTestHelper::isAllowlistedLevelOne);
			if (!allowlisted) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Librarian should still receive an allowlisted level-1 book");
			}
			if (VillagerTradeRebalanceGameTestHelper.laneCount(cartographer, 1) != 1) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Cartographer should still receive a T1 lane");
			}
			if (TreasureMapLocator.laneOf(VillagerTradeRebalanceGameTestHelper.requireLane(cartographer, 1)
					.getResult()).orElse(-1) != 1) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Cartographer T1 should keep vtr_map_lane");
			}
		});
	}
}
