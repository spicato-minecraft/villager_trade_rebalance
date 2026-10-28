package villager_trade_rebalance.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import villager_trade_rebalance.VillagerTradeRebalance;
import villager_trade_rebalance.enchant.LibrarianBookPolicy;

import java.util.List;

public class LibrarianLineageLevelUpGameTest {

	@GameTest(maxTicks = 200)
	public void apprenticeContinuesNoviceEnchant(GameTestHelper context) {
		VillagerTradeRebalanceGameTestHelper.restoreDefaultConfig();
		VillagerTradeRebalanceGameTestHelper.buildFloor(context, 4);
		context.setBlock(2, 1, 2, Blocks.LECTERN);

		Villager villager = VillagerTradeRebalanceGameTestHelper.spawnVillager(context, 1, 1, 1);
		VillagerTradeRebalanceGameTestHelper.assignProfession(villager, VillagerProfession.LIBRARIAN);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);

		context.succeedWhen(() -> {
			List<MerchantOffer> noviceBooks = VillagerTradeRebalanceGameTestHelper.bookOffers(villager);
			if (noviceBooks.size() != 1) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Novice librarian should offer exactly one enchanted book");
			}
			String lineage = VillagerTradeRebalanceGameTestHelper.bookEnchantId(noviceBooks.getFirst());
			int noviceLevel = VillagerTradeRebalanceGameTestHelper.bookLevel(noviceBooks.getFirst());
			if (noviceLevel != 1) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Novice book should be level 1");
			}

			VillagerTradeRebalanceGameTestHelper.setLevel(villager, 2);
			VillagerTradeRebalanceGameTestHelper.updateTrades(villager);

			List<MerchantOffer> apprenticeBooks = VillagerTradeRebalanceGameTestHelper.bookOffers(villager);
			if (apprenticeBooks.isEmpty()) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Apprentice librarian should still have a lineage book");
			}
			for (MerchantOffer book : apprenticeBooks) {
				if (!lineage.equals(VillagerTradeRebalanceGameTestHelper.bookEnchantId(book))) {
					throw VillagerTradeRebalanceGameTestHelper.fail("Apprentice book swapped lineage to "
							+ VillagerTradeRebalanceGameTestHelper.bookEnchantId(book));
				}
			}
			var books = VillagerTradeRebalance.CONFIG.librarianBooks();
			int max = LibrarianBookPolicy.maxLevel(books, lineage);
			int highest = apprenticeBooks.stream()
					.mapToInt(VillagerTradeRebalanceGameTestHelper::bookLevel)
					.max()
					.orElse(0);
			int expectedHighest = Math.min(2, max);
			if (highest != expectedHighest) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Expected highest " + lineage + " level "
						+ expectedHighest + " but was " + highest);
			}
		});
	}
}
