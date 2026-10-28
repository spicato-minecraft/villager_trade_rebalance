package villager_trade_rebalance.enchant;

import villager_trade_rebalance.config.LibrarianBooksConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pure rules for librarian enchanted-book trades: allowlist, lineage, and base emerald cost.
 */
public final class LibrarianBookPolicy {

	private LibrarianBookPolicy() {
	}

	public static boolean isAllowed(LibrarianBooksConfig config, String enchantId) {
		if (enchantId == null || enchantId.isBlank()) {
			return false;
		}
		if (config.blocked().contains(enchantId)) {
			return false;
		}
		return config.allowlist().contains(enchantId);
	}

	public static int maxLevel(LibrarianBooksConfig config, String enchantId) {
		return config.maxLevels().getOrDefault(enchantId, 1);
	}

	public static boolean isMaxed(LibrarianBooksConfig config, String enchantId, int level) {
		return level >= maxLevel(config, enchantId);
	}

	public static Optional<Integer> nextLevel(LibrarianBooksConfig config, String enchantId, int currentLevel) {
		int max = maxLevel(config, enchantId);
		int next = currentLevel + 1;
		if (next > max) {
			return Optional.empty();
		}
		return Optional.of(next);
	}

	/**
	 * Book levels offered after a career level has generated: {@code 1..min(careerLevel, max)}.
	 */
	public static List<Integer> expectedLevels(LibrarianBooksConfig config, String enchantId, int careerLevel) {
		int count = Math.min(Math.max(careerLevel, 0), maxLevel(config, enchantId));
		List<Integer> levels = new ArrayList<>(count);
		for (int level = 1; level <= count; level++) {
			levels.add(level);
		}
		return levels;
	}

	public static String rollNoviceEnchant(LibrarianBooksConfig config) {
		return rollNoviceEnchant(config, ThreadLocalRandom.current().nextInt(config.allowlist().size()));
	}

	public static String rollNoviceEnchant(LibrarianBooksConfig config, int index) {
		List<String> allow = List.copyOf(config.allowlist());
		if (allow.isEmpty()) {
			throw new IllegalStateException("Librarian allowlist is empty");
		}
		int safeIndex = Math.floorMod(index, allow.size());
		return allow.get(safeIndex);
	}

	/**
	 * Emerald count for a book: {@code baseCostMin} at level I, {@code baseCostMax} at this
	 * enchant's max level, interpolated in between. Single-level books (Silk Touch) use the min.
	 */
	public static int baseCost(LibrarianBooksConfig config, String enchantId, int level) {
		int min = config.baseCostMin();
		int max = config.baseCostMax();
		if (max < min) {
			int swap = min;
			min = max;
			max = swap;
		}
		int maxEnchantLevel = Math.max(1, maxLevel(config, enchantId));
		int clamped = Math.min(Math.max(level, 1), maxEnchantLevel);
		if (maxEnchantLevel == 1) {
			return min;
		}
		int span = max - min;
		return min + Math.round(span * (clamped - 1) / (float) (maxEnchantLevel - 1));
	}
}
