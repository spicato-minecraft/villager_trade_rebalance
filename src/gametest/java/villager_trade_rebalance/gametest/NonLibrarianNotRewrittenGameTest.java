package villager_trade_rebalance.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.Blocks;

public class NonLibrarianNotRewrittenGameTest {

	@GameTest(maxTicks = 200)
	public void farmerDoesNotReceiveLibrarianState(GameTestHelper context) {
		VillagerTradeRebalanceGameTestHelper.restoreDefaultConfig();
		VillagerTradeRebalanceGameTestHelper.buildFloor(context, 4);
		context.setBlock(2, 1, 2, Blocks.COMPOSTER);

		Villager villager = VillagerTradeRebalanceGameTestHelper.spawnVillager(context, 1, 1, 1);
		VillagerTradeRebalanceGameTestHelper.assignProfession(villager, VillagerProfession.FARMER);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);

		context.succeedWhen(() -> {
			if (VillagerTradeRebalanceGameTestHelper.state(villager).initialized()) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Farmer should not receive librarian new-rule state");
			}
		});
	}
}
