package villager_trade_rebalance.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.Blocks;
import villager_trade_rebalance.VillagerTradeRebalance;
import villager_trade_rebalance.config.ModConfig;

public class ConfigDisabledLeavesVanillaGameTest {

	@GameTest(maxTicks = 200)
	public void disabledConfigDoesNotMarkNewRule(GameTestHelper context) {
		VillagerTradeRebalanceGameTestHelper.restoreDefaultConfig();
		VillagerTradeRebalance.CONFIG = ModConfig.parse("{ \"enabled\": false }");
		VillagerTradeRebalanceGameTestHelper.buildFloor(context, 4);
		context.setBlock(2, 1, 2, Blocks.LECTERN);

		Villager villager = VillagerTradeRebalanceGameTestHelper.spawnVillager(context, 1, 1, 1);
		VillagerTradeRebalanceGameTestHelper.assignProfession(villager, VillagerProfession.LIBRARIAN);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);

		context.succeedWhen(() -> {
			try {
				if (VillagerTradeRebalanceGameTestHelper.state(villager).initialized()) {
					throw VillagerTradeRebalanceGameTestHelper.fail(
							"Disabled config should skip rewrite and leave librarian state uninitialized");
				}
			} finally {
				VillagerTradeRebalanceGameTestHelper.restoreDefaultConfig();
			}
		});
	}
}
