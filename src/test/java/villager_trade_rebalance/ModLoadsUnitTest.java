package villager_trade_rebalance;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ModLoadsUnitTest {

	@BeforeAll
	static void bootstrapMinecraft() {
		MinecraftTestBootstrap.init();
	}

	@Test
	void modMainClassLoads() {
		assertNotNull(VillagerTradeRebalance.MOD_ID);
		assertEquals("villager_trade_rebalance", VillagerTradeRebalance.MOD_ID);
	}
}
