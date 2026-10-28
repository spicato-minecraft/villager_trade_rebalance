package villager_trade_rebalance.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.Blocks;
import villager_trade_rebalance.VillagerTradeRebalance;
import villager_trade_rebalance.enchant.LibrarianBookPolicy;

public class NewRuleStatePersistsGameTest {

	@GameTest(maxTicks = 200)
	public void newRuleLibrarianStaysAllowlistedAfterReload(GameTestHelper context) {
		VillagerTradeRebalanceGameTestHelper.restoreDefaultConfig();
		VillagerTradeRebalanceGameTestHelper.buildFloor(context, 4);
		context.setBlock(2, 1, 2, Blocks.LECTERN);

		Villager source = VillagerTradeRebalanceGameTestHelper.spawnVillager(context, 1, 1, 1);
		VillagerTradeRebalanceGameTestHelper.assignProfession(source, VillagerProfession.LIBRARIAN);
		VillagerTradeRebalanceGameTestHelper.updateTrades(source);

		Villager loaded = VillagerTradeRebalanceGameTestHelper.spawnVillager(context, 3, 1, 1);
		context.succeedWhen(() -> {
			if (!VillagerTradeRebalanceGameTestHelper.state(source).initialized()
					|| VillagerTradeRebalanceGameTestHelper.state(source).grandfathered()) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Fresh librarian should be new-rule before save");
			}
			VillagerTradeRebalanceGameTestHelper.roundTripAdditionalSaveData(context, source, loaded);
			if (!VillagerTradeRebalanceGameTestHelper.state(loaded).initialized()
					|| VillagerTradeRebalanceGameTestHelper.state(loaded).grandfathered()) {
				throw VillagerTradeRebalanceGameTestHelper.fail("New-rule flag was lost on save/load");
			}
			VillagerTradeRebalanceGameTestHelper.updateTrades(loaded);
			var books = VillagerTradeRebalanceGameTestHelper.bookOffers(loaded);
			if (books.isEmpty()) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Reloaded new-rule librarian has no book");
			}
			var booksConfig = VillagerTradeRebalance.CONFIG.librarianBooks();
			for (var book : books) {
				String id = VillagerTradeRebalanceGameTestHelper.bookEnchantId(book);
				if (!LibrarianBookPolicy.isAllowed(booksConfig, id)) {
					throw VillagerTradeRebalanceGameTestHelper.fail("Reloaded book used a blocked enchant: " + id);
				}
			}
		});
	}
}
