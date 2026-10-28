package villager_trade_rebalance.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import villager_trade_rebalance.persist.LibrarianTradeState;

public class MaxedLineageSkipsExtraBookGameTest {

	@GameTest(maxTicks = 200)
	public void silkTouchLineageDoesNotAddSecondBook(GameTestHelper context) {
		VillagerTradeRebalanceGameTestHelper.restoreDefaultConfig();
		VillagerTradeRebalanceGameTestHelper.buildFloor(context, 4);
		context.setBlock(2, 1, 2, Blocks.LECTERN);

		Villager villager = VillagerTradeRebalanceGameTestHelper.spawnVillager(context, 1, 1, 1);
		VillagerTradeRebalanceGameTestHelper.assignProfession(villager, VillagerProfession.LIBRARIAN);
		VillagerTradeRebalanceGameTestHelper.setState(villager, LibrarianTradeState.newRule());

		MerchantOffers seeded = new MerchantOffers();
		seeded.add(VillagerTradeRebalanceGameTestHelper.bookshelfOffer());
		seeded.add(VillagerTradeRebalanceGameTestHelper.bookOffer(context, Enchantments.SILK_TOUCH, 1, 36));
		villager.setOffers(seeded);

		VillagerTradeRebalanceGameTestHelper.setLevel(villager, 2);
		VillagerTradeRebalanceGameTestHelper.updateTrades(villager);

		context.succeedWhen(() -> {
			var books = VillagerTradeRebalanceGameTestHelper.bookOffers(villager);
			if (books.size() != 1) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Maxed Silk Touch lineage should keep exactly one book, had "
						+ books.size());
			}
			if (!"minecraft:silk_touch".equals(VillagerTradeRebalanceGameTestHelper.bookEnchantId(books.getFirst()))) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Expected Silk Touch but was "
						+ VillagerTradeRebalanceGameTestHelper.bookEnchantId(books.getFirst()));
			}
			if (VillagerTradeRebalanceGameTestHelper.bookLevel(books.getFirst()) != 1) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Silk Touch should stay at level 1");
			}
		});
	}
}
