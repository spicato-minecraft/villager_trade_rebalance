package villager_trade_rebalance.persist;

/**
 * Grandfather is decided on world load only: existing profession offers and no saved flag.
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

	public static CartographerTradeState cartographerOnLoad(CartographerTradeState stored, boolean hasLocatedMapOffer) {
		if (stored != null && stored.initialized()) {
			return stored;
		}
		if (hasLocatedMapOffer) {
			return CartographerTradeState.ofGrandfathered();
		}
		return CartographerTradeState.uninitialized();
	}

	public static CartographerTradeState cartographerOnRuntimeGeneration(CartographerTradeState current) {
		if (current != null && current.initialized()) {
			return current;
		}
		return CartographerTradeState.newRule();
	}
}
