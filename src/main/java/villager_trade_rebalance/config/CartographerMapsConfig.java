package villager_trade_rebalance.config;

public record CartographerMapsConfig(
		boolean enabled,
		int baseRadius,
		int maxUses,
		int tier1Emeralds,
		int tier2Emeralds,
		int tier3Emeralds
) {
	public static CartographerMapsConfig defaults() {
		return new CartographerMapsConfig(true, 100, 12, 8, 13, 14);
	}

	public CartographerMapsConfig sanitized() {
		return new CartographerMapsConfig(
				enabled,
				Math.max(1, baseRadius),
				Math.max(1, maxUses),
				Math.max(1, tier1Emeralds),
				Math.max(1, tier2Emeralds),
				Math.max(1, tier3Emeralds)
		);
	}
}
