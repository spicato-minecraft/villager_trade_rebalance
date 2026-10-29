package villager_trade_rebalance.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * G21 LVT dual-mod: {@code locked_villager_trades} is not a GameTest dependency of this
 * project. Dual-mod lock/preview coverage is deferred — do not add a Gradle dependency
 * just for this case. See README.
 */
public class CartographerLvtDualModGameTest {

	@GameTest(maxTicks = 20)
	public void dualModCoverageDeferredWithoutLvtDependency(GameTestHelper context) {
		context.succeed();
	}
}
