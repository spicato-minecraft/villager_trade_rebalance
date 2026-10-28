package villager_trade_rebalance;

import org.junit.jupiter.api.Test;
import villager_trade_rebalance.persist.GrandfatherPolicy;
import villager_trade_rebalance.persist.LibrarianTradeState;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrandfatherPolicyTest {

	@Test
	void loadWithExistingOffersAndNoTagGrandfathers() {
		LibrarianTradeState state = GrandfatherPolicy.onLoad(LibrarianTradeState.uninitialized(), true);
		assertTrue(state.initialized());
		assertTrue(state.grandfathered());
	}

	@Test
	void loadWithoutOffersStaysUninitialized() {
		LibrarianTradeState state = GrandfatherPolicy.onLoad(LibrarianTradeState.uninitialized(), false);
		assertFalse(state.initialized());
		assertFalse(state.grandfathered());
	}

	@Test
	void runtimeGenerationMarksNewRule() {
		LibrarianTradeState state = GrandfatherPolicy.onRuntimeGeneration(LibrarianTradeState.uninitialized());
		assertTrue(state.initialized());
		assertFalse(state.grandfathered());
	}

	@Test
	void runtimeGenerationPreservesGrandfather() {
		LibrarianTradeState state = GrandfatherPolicy.onRuntimeGeneration(LibrarianTradeState.ofGrandfathered());
		assertTrue(state.grandfathered());
	}

	@Test
	void loadPreservesStoredNewRuleAndGrandfather() {
		LibrarianTradeState newRule = GrandfatherPolicy.onLoad(LibrarianTradeState.newRule(), true);
		assertTrue(newRule.initialized());
		assertFalse(newRule.grandfathered());

		LibrarianTradeState grandfathered = GrandfatherPolicy.onLoad(LibrarianTradeState.ofGrandfathered(), false);
		assertTrue(grandfathered.initialized());
		assertTrue(grandfathered.grandfathered());
	}
}
