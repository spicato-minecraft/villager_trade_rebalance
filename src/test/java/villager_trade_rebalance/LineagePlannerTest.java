package villager_trade_rebalance;

import org.junit.jupiter.api.Test;
import villager_trade_rebalance.config.LibrarianBooksConfig;
import villager_trade_rebalance.enchant.LibrarianEnchantments;
import villager_trade_rebalance.enchant.LineagePlanner;
import villager_trade_rebalance.enchant.LineagePlanner.BookSpec;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineagePlannerTest {

	private final LibrarianBooksConfig config = LibrarianBooksConfig.defaults();

	@Test
	void infersLineageFromMatchingBooks() {
		var lineage = LineagePlanner.inferLineage(config, List.of(
				new BookSpec(LibrarianEnchantments.EFFICIENCY, 1),
				new BookSpec(LibrarianEnchantments.EFFICIENCY, 2)
		));
		assertEquals(LibrarianEnchantments.EFFICIENCY, lineage.orElseThrow());
	}

	@Test
	void mixedBooksUseFirstAllowlistedEnchant() {
		var lineage = LineagePlanner.inferLineage(config, List.of(
				new BookSpec(LibrarianEnchantments.EFFICIENCY, 1),
				new BookSpec(LibrarianEnchantments.UNBREAKING, 1)
		));
		assertEquals(LibrarianEnchantments.EFFICIENCY, lineage.orElseThrow());
	}

	@Test
	void rejectsBlockedOnlyBooks() {
		assertTrue(LineagePlanner.inferLineage(config, List.of(
				new BookSpec(LibrarianEnchantments.MENDING, 1)
		)).isEmpty());
		assertTrue(LineagePlanner.inferLineage(config, List.of(
				new BookSpec(LibrarianEnchantments.MENDING, 1),
				new BookSpec(LibrarianEnchantments.FORTUNE, 1)
		)).isEmpty());
	}

	@Test
	void expectedBooksStopsAtMax() {
		assertEquals(1, LineagePlanner.expectedBooks(config, LibrarianEnchantments.SILK_TOUCH, 3).size());
		assertEquals(3, LineagePlanner.expectedBooks(config, LibrarianEnchantments.UNBREAKING, 5).size());
	}
}
