package villager_trade_rebalance;

import org.junit.jupiter.api.Test;
import villager_trade_rebalance.cartographer.CartographerDestinations;
import villager_trade_rebalance.cartographer.CartographerDestinations.MapTypeSpec;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static villager_trade_rebalance.cartographer.CartographerDestinations.BURIED_TRIAL_CHAMBERS;
import static villager_trade_rebalance.cartographer.CartographerDestinations.DESERT_VILLAGE;
import static villager_trade_rebalance.cartographer.CartographerDestinations.JUNGLE_PYRAMID;
import static villager_trade_rebalance.cartographer.CartographerDestinations.OCEAN_MONUMENT;
import static villager_trade_rebalance.cartographer.CartographerDestinations.PAIRED_PORTALS_TAG;
import static villager_trade_rebalance.cartographer.CartographerDestinations.PLAINS_VILLAGE;
import static villager_trade_rebalance.cartographer.CartographerDestinations.RUINED_PORTAL;
import static villager_trade_rebalance.cartographer.CartographerDestinations.SAVANNA_VILLAGE;
import static villager_trade_rebalance.cartographer.CartographerDestinations.SNOWY_VILLAGE;
import static villager_trade_rebalance.cartographer.CartographerDestinations.SWAMP_HUT;
import static villager_trade_rebalance.cartographer.CartographerDestinations.TAIGA_VILLAGE;
import static villager_trade_rebalance.cartographer.CartographerDestinations.TIER_1;
import static villager_trade_rebalance.cartographer.CartographerDestinations.TIER_2;
import static villager_trade_rebalance.cartographer.CartographerDestinations.TIER_3;
import static villager_trade_rebalance.cartographer.CartographerDestinations.VARIANT_DESERT;
import static villager_trade_rebalance.cartographer.CartographerDestinations.VARIANT_JUNGLE;
import static villager_trade_rebalance.cartographer.CartographerDestinations.VARIANT_PLAINS;
import static villager_trade_rebalance.cartographer.CartographerDestinations.VARIANT_SAVANNA;
import static villager_trade_rebalance.cartographer.CartographerDestinations.VARIANT_SNOW;
import static villager_trade_rebalance.cartographer.CartographerDestinations.VARIANT_SWAMP;
import static villager_trade_rebalance.cartographer.CartographerDestinations.VARIANT_TAIGA;
import static villager_trade_rebalance.cartographer.CartographerDestinations.WOODLAND_MANSION;

class CartographerDestinationsTest {

	private static final List<String> ALL_VARIANTS = List.of(
			VARIANT_PLAINS, VARIANT_TAIGA, VARIANT_SNOW, VARIANT_SAVANNA,
			VARIANT_DESERT, VARIANT_JUNGLE, VARIANT_SWAMP
	);

	@Test
	void u8FullT1VariantMatrix() {
		assertEligible(PLAINS_VILLAGE, VARIANT_TAIGA, VARIANT_SNOW, VARIANT_SAVANNA, VARIANT_DESERT);
		assertEligible(DESERT_VILLAGE, VARIANT_SAVANNA, VARIANT_JUNGLE);
		assertEligible(SAVANNA_VILLAGE, VARIANT_PLAINS, VARIANT_JUNGLE, VARIANT_DESERT);
		assertEligible(SNOWY_VILLAGE, VARIANT_TAIGA, VARIANT_SWAMP);
		assertEligible(TAIGA_VILLAGE, VARIANT_SWAMP, VARIANT_SNOW, VARIANT_PLAINS);
		assertEligible(JUNGLE_PYRAMID, VARIANT_SWAMP, VARIANT_SAVANNA, VARIANT_DESERT);
		assertEligible(SWAMP_HUT, VARIANT_TAIGA, VARIANT_SNOW, VARIANT_JUNGLE);

		assertFalse(typeIds(TIER_1, VARIANT_PLAINS).contains(PLAINS_VILLAGE),
				"plains villager never sells plains village map");
		assertEquals(Set.of(VARIANT_TAIGA, VARIANT_SNOW, VARIANT_JUNGLE), variantsThatCanSell(SWAMP_HUT));
		assertEquals(Set.of(VARIANT_SWAMP, VARIANT_SAVANNA, VARIANT_DESERT), variantsThatCanSell(JUNGLE_PYRAMID));
		assertEquals(Set.of(SAVANNA_VILLAGE, TAIGA_VILLAGE), typeIds(TIER_1, VARIANT_PLAINS));
		assertEquals(Set.of(SNOWY_VILLAGE, TAIGA_VILLAGE, JUNGLE_PYRAMID), typeIds(TIER_1, VARIANT_SWAMP));
	}

	@Test
	void u9T2MonumentTrialPairedPortalT3MansionOnly() {
		for (String variant : ALL_VARIANTS) {
			assertEquals(Set.of(OCEAN_MONUMENT, BURIED_TRIAL_CHAMBERS, RUINED_PORTAL), typeIds(TIER_2, variant));
			assertEquals(Set.of(WOODLAND_MANSION), typeIds(TIER_3, variant));
		}

		MapTypeSpec portal = CartographerDestinations.typesInLane(TIER_2).stream()
				.filter(spec -> RUINED_PORTAL.equals(spec.typeId()))
				.findFirst()
				.orElseThrow();
		assertEquals(PAIRED_PORTALS_TAG, portal.destinationTag());
		assertFalse(portal.destinationTag().contains("on_ruined_portal_maps"));

		MapTypeSpec monument = CartographerDestinations.typesInLane(TIER_2).stream()
				.filter(spec -> OCEAN_MONUMENT.equals(spec.typeId()))
				.findFirst()
				.orElseThrow();
		assertEquals("minecraft:ocean_monument_map", monument.itemId());
		assertEquals("#minecraft:on_ocean_monument_maps", monument.destinationTag());

		MapTypeSpec mansion = CartographerDestinations.typesInLane(TIER_3).getFirst();
		assertEquals("minecraft:woodland_mansion_map", mansion.itemId());
		assertEquals("#minecraft:on_woodland_mansion_maps", mansion.destinationTag());
		assertEquals(1, CartographerDestinations.typesInLane(TIER_3).size());
	}

	@Test
	void u10CampLootIdsNeverInAnyTier() {
		Set<String> itemIds = CartographerDestinations.allTypes().stream()
				.map(MapTypeSpec::itemId)
				.collect(Collectors.toSet());
		Set<String> destinations = CartographerDestinations.allTypes().stream()
				.map(MapTypeSpec::destinationTag)
				.collect(Collectors.toSet());

		assertFalse(itemIds.contains("minecraft:buried_ancient_city_map"));
		assertFalse(itemIds.contains("minecraft:buried_mineshaft_map"));
		assertFalse(itemIds.contains("minecraft:desert_pyramid_map"));
		assertFalse(itemIds.contains("minecraft:warm_ocean_ruins_map"));
		assertFalse(itemIds.contains("minecraft:abandoned_camp_map"));

		assertFalse(destinations.contains("#minecraft:on_ancient_city_maps"));
		assertFalse(destinations.contains("#minecraft:on_mineshaft_maps"));
		assertFalse(destinations.contains("#minecraft:on_desert_pyramid_maps"));
		assertFalse(destinations.contains("#minecraft:on_ocean_ruin_warm_maps"));
		assertFalse(destinations.stream().anyMatch(tag -> tag.contains("abandoned_camp")));
	}

	@Test
	void u11FallbackSkipsFailedThenEmpty() {
		Random random = new Random(1L);
		var afterOneFailure = CartographerDestinations.pickNext(
				TIER_1, VARIANT_PLAINS, Set.of(SAVANNA_VILLAGE), random);
		assertEquals(TAIGA_VILLAGE, afterOneFailure.orElseThrow().typeId());

		assertTrue(CartographerDestinations.pickNext(
				TIER_1, VARIANT_PLAINS, Set.of(SAVANNA_VILLAGE, TAIGA_VILLAGE), random
		).isEmpty());

		var skippedMonument = CartographerDestinations.pickNext(
				TIER_2, VARIANT_PLAINS, Set.of(OCEAN_MONUMENT), random);
		assertTrue(Set.of(BURIED_TRIAL_CHAMBERS, RUINED_PORTAL).contains(skippedMonument.orElseThrow().typeId()));

		assertTrue(CartographerDestinations.pickNext(
				TIER_2, VARIANT_JUNGLE,
				Set.of(OCEAN_MONUMENT, BURIED_TRIAL_CHAMBERS, PAIRED_PORTALS_TAG),
				random
		).isEmpty());
	}

	private static void assertEligible(String typeId, String... allowed) {
		Set<String> expected = Set.of(allowed);
		assertEquals(expected, variantsThatCanSell(typeId), typeId);
		for (String variant : ALL_VARIANTS) {
			boolean eligible = typeIds(TIER_1, variant).contains(typeId);
			assertEquals(expected.contains(variant), eligible, typeId + " vs " + variant);
		}
	}

	private static Set<String> typeIds(int lane, String variant) {
		return CartographerDestinations.eligibleTypes(lane, variant).stream()
				.map(MapTypeSpec::typeId)
				.collect(Collectors.toCollection(java.util.LinkedHashSet::new));
	}

	private static Set<String> variantsThatCanSell(String typeId) {
		return ALL_VARIANTS.stream()
				.filter(variant -> typeIds(TIER_1, variant).contains(typeId))
				.collect(Collectors.toCollection(java.util.LinkedHashSet::new));
	}
}
