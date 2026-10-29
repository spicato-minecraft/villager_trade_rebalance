package villager_trade_rebalance;

import org.junit.jupiter.api.Test;
import villager_trade_rebalance.config.CartographerMapsConfig;
import villager_trade_rebalance.config.LibrarianBooksConfig;
import villager_trade_rebalance.config.ModConfig;
import villager_trade_rebalance.enchant.LibrarianEnchantments;

import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModConfigParseTest {

	@Test
	void parseKeepsFortuneBlockedEvenIfOmitted() {
		ModConfig config = ModConfig.parse("""
				{
				  "enabled": true,
				  "librarian_books": {
				    "allowlist": ["minecraft:efficiency"],
				    "blocked": ["minecraft:mending"]
				  }
				}
				""");
		assertTrue(config.librarianBooks().blocked().contains(LibrarianEnchantments.FORTUNE));
		assertTrue(config.librarianBooks().allowlist().contains(LibrarianEnchantments.EFFICIENCY));
	}

	@Test
	void disableFlagParses() {
		ModConfig config = ModConfig.parse("{ \"enabled\": false }");
		assertFalse(config.enabled());
	}

	@Test
	void librarianBooksDisableFlagParses() {
		ModConfig config = ModConfig.parse("""
				{
				  "librarian_books": { "enabled": false }
				}
				""");
		assertTrue(config.enabled());
		assertFalse(config.librarianBooks().enabled());
	}

	@Test
	void parseStripsStructureExclusivesFromAllowlist() {
		ModConfig config = ModConfig.parse("""
				{
				  "librarian_books": {
				    "allowlist": ["minecraft:efficiency", "minecraft:respiration", "minecraft:soul_speed"],
				    "blocked": []
				  }
				}
				""");
		assertTrue(config.librarianBooks().allowlist().contains(LibrarianEnchantments.EFFICIENCY));
		assertFalse(config.librarianBooks().allowlist().contains(LibrarianEnchantments.RESPIRATION));
		assertTrue(config.librarianBooks().blocked().contains(LibrarianEnchantments.RESPIRATION));
		assertTrue(config.librarianBooks().blocked().contains(LibrarianEnchantments.SOUL_SPEED));
	}

	@Test
	void parseAppliesMaxLevelOverrides() {
		ModConfig config = ModConfig.parse("""
				{
				  "librarian_books": {
				    "max_levels": { "minecraft:efficiency": 3 }
				  }
				}
				""");
		assertEquals(3, config.librarianBooks().maxLevels().get(LibrarianEnchantments.EFFICIENCY));
	}

	@Test
	void sanitizedRemovesBlockedIdsFromAllowlist() {
		var dirty = new LibrarianBooksConfig(
				true,
				36,
				64,
				new LinkedHashSet<>(java.util.List.of(
						LibrarianEnchantments.EFFICIENCY,
						LibrarianEnchantments.FORTUNE
				)),
				new LinkedHashSet<>(java.util.List.of(LibrarianEnchantments.FORTUNE)),
				java.util.Map.of()
		);
		var clean = dirty.sanitized();
		assertTrue(clean.allowlist().contains(LibrarianEnchantments.EFFICIENCY));
		assertFalse(clean.allowlist().contains(LibrarianEnchantments.FORTUNE));
	}

	@Test
	void cartographerMapsDisableFlagParses() {
		ModConfig config = ModConfig.parse("""
				{
				  "cartographer_maps": { "enabled": false }
				}
				""");
		assertTrue(config.enabled());
		assertFalse(config.cartographerMaps().enabled());
		assertEquals(100, config.cartographerMaps().baseRadius());
		assertEquals(12, config.cartographerMaps().maxUses());
		assertEquals(8, config.cartographerMaps().tier1Emeralds());
		assertEquals(13, config.cartographerMaps().tier2Emeralds());
		assertEquals(14, config.cartographerMaps().tier3Emeralds());
	}

	@Test
	void missingCartographerMapsUsesDefaults() {
		ModConfig config = ModConfig.parse("{ \"enabled\": true }");
		CartographerMapsConfig maps = config.cartographerMaps();
		CartographerMapsConfig defaults = CartographerMapsConfig.defaults();
		assertTrue(maps.enabled());
		assertEquals(defaults.baseRadius(), maps.baseRadius());
		assertEquals(defaults.maxUses(), maps.maxUses());
		assertEquals(defaults.tier1Emeralds(), maps.tier1Emeralds());
		assertEquals(defaults.tier2Emeralds(), maps.tier2Emeralds());
		assertEquals(defaults.tier3Emeralds(), maps.tier3Emeralds());
	}

	@Test
	void cartographerMapsParsesExplicitValues() {
		ModConfig config = ModConfig.parse("""
				{
				  "cartographer_maps": {
				    "enabled": true,
				    "base_radius": 100,
				    "max_uses": 12,
				    "tier1_emeralds": 8,
				    "tier2_emeralds": 13,
				    "tier3_emeralds": 14
				  }
				}
				""");
		CartographerMapsConfig maps = config.cartographerMaps();
		assertTrue(maps.enabled());
		assertEquals(100, maps.baseRadius());
		assertEquals(12, maps.maxUses());
		assertEquals(8, maps.tier1Emeralds());
		assertEquals(13, maps.tier2Emeralds());
		assertEquals(14, maps.tier3Emeralds());
	}
}
