package villager_trade_rebalance.persist;

/**
 * Entity-level cartographer map state. Profession-scoped so librarian flags stay independent.
 */
public record CartographerTradeState(boolean initialized, boolean grandfathered) {

	public static CartographerTradeState uninitialized() {
		return new CartographerTradeState(false, false);
	}

	public static CartographerTradeState newRule() {
		return new CartographerTradeState(true, false);
	}

	public static CartographerTradeState ofGrandfathered() {
		return new CartographerTradeState(true, true);
	}
}
