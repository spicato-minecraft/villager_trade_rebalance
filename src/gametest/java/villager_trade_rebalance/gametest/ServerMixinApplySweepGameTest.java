package villager_trade_rebalance.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class ServerMixinApplySweepGameTest {

	/** G20: cartographer purchase mixin targets stay loaded (Villager + MerchantResultSlot). */
	@GameTest(maxTicks = 20)
	public void loadsCommonMixinTargets(GameTestHelper context) {
		load("net.minecraft.world.entity.npc.villager.Villager");
		load("net.minecraft.world.inventory.MerchantResultSlot");
		context.succeed();
	}

	private static void load(String name) {
		try {
			Class.forName(name);
		} catch (ClassNotFoundException e) {
			throw VillagerTradeRebalanceGameTestHelper.fail("Mixin target failed to load: " + name);
		}
	}
}
