package villager_trade_rebalance;

import org.junit.jupiter.api.Test;
import villager_trade_rebalance.persist.CartographerTradeState;
import villager_trade_rebalance.persist.GrandfatherPolicy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartographerGrandfatherPolicyTest {

	@Test
	void loadWithLocatedMapOfferAndNoFlagGrandfathers() {
		CartographerTradeState state = GrandfatherPolicy.cartographerOnLoad(
				CartographerTradeState.uninitialized(),
				true
		);
		assertTrue(state.initialized());
		assertTrue(state.grandfathered());
	}

	@Test
	void loadWithoutOffersStaysUninitialized() {
		CartographerTradeState state = GrandfatherPolicy.cartographerOnLoad(
				CartographerTradeState.uninitialized(),
				false
		);
		assertFalse(state.initialized());
		assertFalse(state.grandfathered());
	}

	@Test
	void runtimeGenerationMarksNewRule() {
		CartographerTradeState state = GrandfatherPolicy.cartographerOnRuntimeGeneration(
				CartographerTradeState.uninitialized()
		);
		assertTrue(state.initialized());
		assertFalse(state.grandfathered());
	}

	@Test
	void loadPreservesStoredNewRule() {
		CartographerTradeState state = GrandfatherPolicy.cartographerOnLoad(
				CartographerTradeState.newRule(),
				true
		);
		assertTrue(state.initialized());
		assertFalse(state.grandfathered());
	}
}
