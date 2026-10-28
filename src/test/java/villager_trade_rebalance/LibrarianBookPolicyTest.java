package villager_trade_rebalance;

import org.junit.jupiter.api.Test;
import villager_trade_rebalance.config.LibrarianBooksConfig;
import villager_trade_rebalance.enchant.LibrarianBookPolicy;
import villager_trade_rebalance.enchant.LibrarianEnchantments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibrarianBookPolicyTest {

	private final LibrarianBooksConfig config = LibrarianBooksConfig.defaults();

	@Test
	void structureExclusiveBooksAreBlocked() {
		assertFalse(LibrarianBookPolicy.isAllowed(config, LibrarianEnchantments.RESPIRATION));
		assertFalse(LibrarianBookPolicy.isAllowed(config, LibrarianEnchantments.DEPTH_STRIDER));
		assertFalse(LibrarianBookPolicy.isAllowed(config, LibrarianEnchantments.SWIFT_SNEAK));
		assertFalse(LibrarianBookPolicy.isAllowed(config, LibrarianEnchantments.SOUL_SPEED));
	}

	@Test
	void featherFallingIsAllowlisted() {
		assertTrue(LibrarianBookPolicy.isAllowed(config, LibrarianEnchantments.FEATHER_FALLING));
	}

	@Test
	void allowlistIncludesToolAndNicheBooks() {
		assertTrue(LibrarianBookPolicy.isAllowed(config, LibrarianEnchantments.EFFICIENCY));
		assertTrue(LibrarianBookPolicy.isAllowed(config, LibrarianEnchantments.SILK_TOUCH));
		assertTrue(LibrarianBookPolicy.isAllowed(config, "minecraft:smite"));
		assertTrue(LibrarianBookPolicy.isAllowed(config, "minecraft:frost_walker"));
	}

	@Test
	void hardBlocksFortuneLootingMendingAndMainPath() {
		assertFalse(LibrarianBookPolicy.isAllowed(config, LibrarianEnchantments.FORTUNE));
		assertFalse(LibrarianBookPolicy.isAllowed(config, LibrarianEnchantments.LOOTING));
		assertFalse(LibrarianBookPolicy.isAllowed(config, LibrarianEnchantments.MENDING));
		assertFalse(LibrarianBookPolicy.isAllowed(config, LibrarianEnchantments.SHARPNESS));
		assertFalse(LibrarianBookPolicy.isAllowed(config, LibrarianEnchantments.PROTECTION));
		assertFalse(LibrarianBookPolicy.isAllowed(config, LibrarianEnchantments.POWER));
		assertFalse(LibrarianBookPolicy.isAllowed(config, LibrarianEnchantments.INFINITY));
		assertFalse(LibrarianBookPolicy.isAllowed(config, LibrarianEnchantments.BINDING));
		assertFalse(LibrarianBookPolicy.isAllowed(config, LibrarianEnchantments.VANISHING));
		assertFalse(LibrarianBookPolicy.isAllowed(config, LibrarianEnchantments.WIND_BURST));
		assertFalse(LibrarianBookPolicy.isAllowed(config, LibrarianEnchantments.LUCK_OF_THE_SEA));
	}

	@Test
	void efficiencyLineageStepsToFive() {
		assertEquals(2, LibrarianBookPolicy.nextLevel(config, LibrarianEnchantments.EFFICIENCY, 1).orElseThrow());
		assertEquals(5, LibrarianBookPolicy.nextLevel(config, LibrarianEnchantments.EFFICIENCY, 4).orElseThrow());
		assertTrue(LibrarianBookPolicy.nextLevel(config, LibrarianEnchantments.EFFICIENCY, 5).isEmpty());
		assertEquals(5, LibrarianBookPolicy.expectedLevels(config, LibrarianEnchantments.EFFICIENCY, 5).size());
	}

	@Test
	void silkTouchIsMaxedAtLevelOne() {
		assertTrue(LibrarianBookPolicy.isMaxed(config, LibrarianEnchantments.SILK_TOUCH, 1));
		assertTrue(LibrarianBookPolicy.nextLevel(config, LibrarianEnchantments.SILK_TOUCH, 1).isEmpty());
		assertEquals(1, LibrarianBookPolicy.expectedLevels(config, LibrarianEnchantments.SILK_TOUCH, 5).size());
	}

	@Test
	void baseCostScalesWithEnchantLevel() {
		assertEquals(36, LibrarianBookPolicy.baseCost(config, LibrarianEnchantments.EFFICIENCY, 1));
		assertEquals(43, LibrarianBookPolicy.baseCost(config, LibrarianEnchantments.EFFICIENCY, 2));
		assertEquals(50, LibrarianBookPolicy.baseCost(config, LibrarianEnchantments.EFFICIENCY, 3));
		assertEquals(57, LibrarianBookPolicy.baseCost(config, LibrarianEnchantments.EFFICIENCY, 4));
		assertEquals(64, LibrarianBookPolicy.baseCost(config, LibrarianEnchantments.EFFICIENCY, 5));
		assertTrue(LibrarianBookPolicy.baseCost(config, LibrarianEnchantments.EFFICIENCY, 1)
				< LibrarianBookPolicy.baseCost(config, LibrarianEnchantments.EFFICIENCY, 5));
	}

	@Test
	void singleLevelBooksUseLowestCost() {
		assertEquals(36, LibrarianBookPolicy.baseCost(config, LibrarianEnchantments.SILK_TOUCH, 1));
	}

	@Test
	void threeLevelBooksHitBothEnds() {
		assertEquals(36, LibrarianBookPolicy.baseCost(config, LibrarianEnchantments.UNBREAKING, 1));
		assertEquals(50, LibrarianBookPolicy.baseCost(config, LibrarianEnchantments.UNBREAKING, 2));
		assertEquals(64, LibrarianBookPolicy.baseCost(config, LibrarianEnchantments.UNBREAKING, 3));
	}
}
