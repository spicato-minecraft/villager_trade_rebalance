package villager_trade_rebalance.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class ServerMixinApplySweepGameTest {

	@GameTest(maxTicks = 20)
	public void loadsCommonMixinTargets(GameTestHelper context) {
		load("net.minecraft.world.entity.npc.villager.Villager");
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
