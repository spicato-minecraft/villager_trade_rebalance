package villager_trade_rebalance;

import org.junit.jupiter.api.Test;
import villager_trade_rebalance.config.CartographerMapsConfig;
import villager_trade_rebalance.config.ModConfig;
import villager_trade_rebalance.map.CartographerMapPolicy;
import villager_trade_rebalance.map.LocateDecision;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartographerMapPolicyTest {

	@Test
	void u1HighestMapTierForCareerLevels1To5() {
		assertEquals(0, CartographerMapPolicy.highestMapTier(1));
		assertEquals(1, CartographerMapPolicy.highestMapTier(2));
		assertEquals(2, CartographerMapPolicy.highestMapTier(3));
		assertEquals(2, CartographerMapPolicy.highestMapTier(4));
		assertEquals(3, CartographerMapPolicy.highestMapTier(5));
	}

	@Test
	void u2UnlockedLanesMatchCareerUnlockSets() {
		assertEquals(Set.of(), CartographerMapPolicy.unlockedLanes(1));
		assertEquals(Set.of(1), CartographerMapPolicy.unlockedLanes(2));
		assertEquals(Set.of(1, 2), CartographerMapPolicy.unlockedLanes(3));
		assertEquals(Set.of(1, 2), CartographerMapPolicy.unlockedLanes(4));
		assertEquals(Set.of(1, 2, 3), CartographerMapPolicy.unlockedLanes(5));
	}

	@Test
	void u3JourneymanRadiiAre200And100WhenBaseIs100() {
		assertEquals(200, CartographerMapPolicy.radius(3, 1, 100).orElseThrow());
		assertEquals(100, CartographerMapPolicy.radius(3, 2, 100).orElseThrow());
		assertEquals(200, CartographerMapPolicy.radius(4, 1, 100).orElseThrow());
		assertEquals(100, CartographerMapPolicy.radius(4, 2, 100).orElseThrow());
	}

	@Test
	void u4MasterRadiiAre400And200And100WhenBaseIs100() {
		assertEquals(400, CartographerMapPolicy.radius(5, 1, 100).orElseThrow());
		assertEquals(200, CartographerMapPolicy.radius(5, 2, 100).orElseThrow());
		assertEquals(100, CartographerMapPolicy.radius(5, 3, 100).orElseThrow());
	}

	@Test
	void u5RadiusForLockedLaneIsEmpty() {
		assertTrue(CartographerMapPolicy.radius(1, 1, 100).isEmpty());
		assertTrue(CartographerMapPolicy.radius(2, 2, 100).isEmpty());
		assertTrue(CartographerMapPolicy.radius(4, 3, 100).isEmpty());
		assertTrue(CartographerMapPolicy.radius(5, 4, 100).isEmpty());
	}

	@Test
	void u6ConfigParseSanitizesInvalidValues() {
		ModConfig config = ModConfig.parse("""
				{
				  "cartographer_maps": {
				    "enabled": true,
				    "base_radius": 0,
				    "max_uses": -3,
				    "tier1_emeralds": 0,
				    "tier2_emeralds": -1,
				    "tier3_emeralds": 0
				  }
				}
				""");
		CartographerMapsConfig maps = config.cartographerMaps();
		assertTrue(maps.enabled());
		assertEquals(1, maps.baseRadius());
		assertEquals(1, maps.maxUses());
		assertEquals(1, maps.tier1Emeralds());
		assertEquals(1, maps.tier2Emeralds());
		assertEquals(1, maps.tier3Emeralds());
	}

	@Test
	void u7EmptySetPolicyAbortsAndLocks() {
		assertEquals(LocateDecision.ABORT_AND_LOCK, CartographerMapPolicy.emptySetDecision(Set.of()));
		assertEquals(LocateDecision.ABORT_AND_LOCK, CartographerMapPolicy.emptySetDecision(List.of()));
		assertEquals(LocateDecision.ABORT_AND_LOCK, CartographerMapPolicy.emptySetDecision(null));
		assertEquals(LocateDecision.LOCATE, CartographerMapPolicy.emptySetDecision(Set.of("minecraft:monument")));
		assertFalse(CartographerMapPolicy.emptySetDecision(Set.of()).equals(LocateDecision.LOCATE));
	}
}
