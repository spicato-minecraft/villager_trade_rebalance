package villager_trade_rebalance.persist;

/**
 * Entity-level librarian book state. Lineage itself is inferred from current offers
 * so locked_villager_trades can generate independent sets on the same villager.
 */
public record LibrarianTradeState(boolean initialized, boolean grandfathered) {

	public static LibrarianTradeState uninitialized() {
		return new LibrarianTradeState(false, false);
	}

	public static LibrarianTradeState newRule() {
		return new LibrarianTradeState(true, false);
	}

	public static LibrarianTradeState ofGrandfathered() {
		return new LibrarianTradeState(true, true);
	}
}
