package villager_trade_rebalance.persist;

/**
 * Grandfather is decided on world load only: existing offers and no saved tag.
 * Runtime generation (empty offers → vanilla fills them) is always new-rule.
 */
public final class GrandfatherPolicy {

	private GrandfatherPolicy() {
	}

	public static LibrarianTradeState onLoad(LibrarianTradeState stored, boolean hasExistingOffers) {
		if (stored != null && stored.initialized()) {
			return stored;
		}
		if (hasExistingOffers) {
			return LibrarianTradeState.ofGrandfathered();
		}
		return LibrarianTradeState.uninitialized();
	}

	public static LibrarianTradeState onRuntimeGeneration(LibrarianTradeState current) {
		if (current != null && current.initialized()) {
			return current;
		}
		return LibrarianTradeState.newRule();
	}
}
