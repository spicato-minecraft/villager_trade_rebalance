package villager_trade_rebalance;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import villager_trade_rebalance.cartographer.CartographerOfferRewriter;
import villager_trade_rebalance.cartographer.TreasureMapLocator;
import villager_trade_rebalance.config.CartographerMapsConfig;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartographerOfferRewriterTest {

	@BeforeAll
	static void bootstrap() {
		MinecraftTestBootstrap.init();
	}

	@Test
	void careerTwoStripsVanillaMapsAndEnsuresLaneOne() {
		CartographerMapsConfig config = CartographerMapsConfig.defaults();
		MerchantOffers input = new MerchantOffers();
		input.add(paperOffer());
		input.add(vanillaVillageMap());

		MerchantOffers rewritten = CartographerOfferRewriter.rewrite(input, 2, config);

		assertTrue(rewritten.stream().anyMatch(offer -> offer.getResult().is(Items.EMERALD)));
		assertEquals(1, rewritten.stream().filter(offer -> TreasureMapLocator.laneOf(offer).isPresent()).count());
		MerchantOffer lane = rewritten.stream()
				.filter(offer -> TreasureMapLocator.laneOf(offer).orElse(-1) == 1)
				.findFirst()
				.orElseThrow();
		assertEquals(1, TreasureMapLocator.laneOf(lane).orElseThrow());
		assertFalse(lane.getResult().has(DataComponents.MAP_ID));
		assertTrue(lane.getResult().is(Items.FILLED_MAP));
		assertEquals(config.tier1Emeralds(), lane.getItemCostA().count());
		assertTrue(lane.getItemCostB().isPresent());
		assertTrue(lane.getItemCostB().get().itemStack().is(Items.COMPASS));
		assertEquals(5, lane.getXp());
		assertEquals(config.maxUses(), lane.getMaxUses());
		assertTrue(rewritten.stream().noneMatch(offer -> offer.getResult().is(Items.PLAINS_VILLAGE_MAP)));
	}

	@Test
	void keepsExistingLaneMarkerAndDoesNotDuplicate() {
		CartographerMapsConfig config = CartographerMapsConfig.defaults();
		MerchantOffer existing = CartographerOfferRewriter.createUndecided(1, config);
		MerchantOffers input = new MerchantOffers();
		input.add(existing);
		input.add(vanillaVillageMap());

		MerchantOffers rewritten = CartographerOfferRewriter.rewrite(input, 2, config);

		assertEquals(1, rewritten.stream().filter(offer -> TreasureMapLocator.laneOf(offer).isPresent()).count());
		assertTrue(rewritten.contains(existing));
	}

	@Test
	void filledMapWithoutMapIdIsNotGrandfatherSignalAndIsStrippedUnlessMarked() {
		CartographerMapsConfig config = CartographerMapsConfig.defaults();
		MerchantOffers input = new MerchantOffers();
		input.add(new MerchantOffer(new ItemCost(Items.EMERALD, 8), new ItemStack(Items.FILLED_MAP), 12, 5, 0.2f));

		MerchantOffers rewritten = CartographerOfferRewriter.rewrite(input, 2, config);

		assertEquals(1, rewritten.size());
		assertEquals(1, TreasureMapLocator.laneOf(rewritten.getFirst()).orElseThrow());
	}

	@Test
	void careerFiveEnsuresThreeLanes() {
		MerchantOffers rewritten = CartographerOfferRewriter.rewrite(new MerchantOffers(), 5, CartographerMapsConfig.defaults());
		assertEquals(3, rewritten.size());
		java.util.Set<Integer> lanes = rewritten.stream()
				.map(offer -> TreasureMapLocator.laneOf(offer).orElseThrow())
				.collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
		assertEquals(java.util.Set.of(1, 2, 3), lanes);
		int xp2 = rewritten.stream().filter(offer -> TreasureMapLocator.laneOf(offer).orElse(-1) == 2)
				.findFirst().orElseThrow().getXp();
		int xp3 = rewritten.stream().filter(offer -> TreasureMapLocator.laneOf(offer).orElse(-1) == 3)
				.findFirst().orElseThrow().getXp();
		assertEquals(10, xp2);
		assertEquals(30, xp3);
	}

	@Test
	void disabledConfigReturnsSameInstance() {
		CartographerMapsConfig disabled = new CartographerMapsConfig(false, 100, 12, 8, 13, 14);
		MerchantOffers input = new MerchantOffers();
		input.add(vanillaVillageMap());
		assertTrue(CartographerOfferRewriter.rewrite(input, 2, disabled) == input);
	}

	@Test
	void locatedDisplayKeepsTierAndAddsMapId() {
		ItemStack delivered = new ItemStack(Items.FILLED_MAP);
		delivered.set(DataComponents.MAP_ID, new MapId(42));
		TreasureMapLocator.writeLocatedDisplay(delivered, 2);
		var lore = delivered.get(DataComponents.LORE);
		assertTrue(lore != null);
		java.util.List<String> lines = lore.lines().stream().map(net.minecraft.network.chat.Component::getString).toList();
		assertEquals(java.util.List.of("Tier 2", "#42"), lines);
	}

	@Test
	void locatedMapIdIsTreatedAsVanillaEvenOnFilledMap() {
		ItemStack located = new ItemStack(Items.FILLED_MAP);
		located.set(DataComponents.MAP_ID, new MapId(7));
		MerchantOffer offer = new MerchantOffer(
				new ItemCost(Items.EMERALD, 8),
				Optional.of(new ItemCost(Items.COMPASS, 1)),
				located,
				12,
				5,
				0.2f
		);
		assertTrue(CartographerOfferRewriter.isVanillaLocatedMap(offer, CartographerOfferRewriter.catalogItemIds()));
	}

	private static MerchantOffer paperOffer() {
		return new MerchantOffer(new ItemCost(Items.PAPER, 24), new ItemStack(Items.EMERALD), 16, 2, 0.05f);
	}

	private static MerchantOffer vanillaVillageMap() {
		return new MerchantOffer(
				new ItemCost(Items.EMERALD, 8),
				Optional.of(new ItemCost(Items.COMPASS, 1)),
				new ItemStack(Items.PLAINS_VILLAGE_MAP),
				12,
				5,
				0.2f
		);
	}
}
