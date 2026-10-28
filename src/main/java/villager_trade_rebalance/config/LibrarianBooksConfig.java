package villager_trade_rebalance.config;

import villager_trade_rebalance.enchant.LibrarianEnchantments;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public record LibrarianBooksConfig(
		boolean enabled,
		int baseCostMin,
		int baseCostMax,
		Set<String> allowlist,
		Set<String> blocked,
		Map<String, Integer> maxLevels
) {
	public static LibrarianBooksConfig defaults() {
		return new LibrarianBooksConfig(
				true,
				36,
				64,
				LibrarianEnchantments.defaultAllowlist(),
				LibrarianEnchantments.defaultBlocked(),
				LibrarianEnchantments.defaultMaxLevels()
		);
	}

	public LibrarianBooksConfig sanitized() {
		int min = Math.max(1, baseCostMin);
		int max = Math.max(min, baseCostMax);
		Set<String> allow = new LinkedHashSet<>(allowlist);
		allow.removeAll(blocked);
		Map<String, Integer> levels = new LinkedHashMap<>(LibrarianEnchantments.defaultMaxLevels());
		if (maxLevels != null) {
			levels.putAll(maxLevels);
		}
		return new LibrarianBooksConfig(enabled, min, max, allow, new LinkedHashSet<>(blocked), levels);
	}
}
