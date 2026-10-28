package villager_trade_rebalance.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;

public class NonBookLibrarianRowsGameTest {

	@GameTest(maxTicks = 200)
	public void noviceKeepsNonBookOffers(GameTestHelper context) {
		VillagerTradeRebalanceGameTestHelper.restoreDefaultConfig();
		VillagerTradeRebalanceGameTestHelper.buildFloor(context, 4);
		context.setBlock(2, 1, 2, Blocks.LECTERN);

		Villager villager = VillagerTradeRebalanceGameTestHelper.spawnVillager(context, 1, 1, 1);
		VillagerTradeRebalanceGameTestHelper.assignProfession(villager, VillagerProfession.LIBRARIAN);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);

		context.succeedWhen(() -> {
			boolean foundNonBook = false;
			for (MerchantOffer offer : villager.getOffers()) {
				if (!offer.getResult().is(Items.ENCHANTED_BOOK)) {
					foundNonBook = true;
					break;
				}
			}
			if (!foundNonBook) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Novice librarian lost non-book trades after rewrite");
			}
		});
	}
}
