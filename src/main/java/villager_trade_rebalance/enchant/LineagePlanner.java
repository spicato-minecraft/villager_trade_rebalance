package villager_trade_rebalance.enchant;

import villager_trade_rebalance.config.LibrarianBooksConfig;

import java.util.List;
import java.util.Optional;

/**
 * Infers a same-enchant lineage from current book offers so each LVT trade set can roll independently.
 */
public final class LineagePlanner {

	private LineagePlanner() {
	}

	public static Optional<String> inferLineage(LibrarianBooksConfig config, List<BookSpec> books) {
		if (books == null || books.isEmpty()) {
			return Optional.empty();
		}
		for (BookSpec book : books) {
			if (LibrarianBookPolicy.isAllowed(config, book.enchantId())) {
				return Optional.of(book.enchantId());
			}
		}
		return Optional.empty();
	}

	public static List<BookSpec> expectedBooks(LibrarianBooksConfig config, String enchantId, int careerLevel) {
		return LibrarianBookPolicy.expectedLevels(config, enchantId, careerLevel).stream()
				.map(level -> new BookSpec(enchantId, level))
				.toList();
	}

	public record BookSpec(String enchantId, int level) {
	}
}
