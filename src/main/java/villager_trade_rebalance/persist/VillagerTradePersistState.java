package villager_trade_rebalance.persist;

/**
 * Combined VTR villager NBT bag: librarian flags plus optional cartographer flags.
 */
public record VillagerTradePersistState(LibrarianTradeState librarian, CartographerTradeState cartographer) {

	public VillagerTradePersistState {
		librarian = librarian != null ? librarian : LibrarianTradeState.uninitialized();
		cartographer = cartographer != null ? cartographer : CartographerTradeState.uninitialized();
	}

	public static VillagerTradePersistState uninitialized() {
		return new VillagerTradePersistState(
				LibrarianTradeState.uninitialized(),
				CartographerTradeState.uninitialized()
		);
	}

	public boolean hasAnyInitialized() {
		return librarian.initialized() || cartographer.initialized();
	}
}
