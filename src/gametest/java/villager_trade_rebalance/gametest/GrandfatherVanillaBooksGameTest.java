package villager_trade_rebalance.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import villager_trade_rebalance.persist.LibrarianTradeState;

public class GrandfatherVanillaBooksGameTest {

	@GameTest(maxTicks = 200)
	public void loadedVanillaLibrarianKeepsMending(GameTestHelper context) {
		VillagerTradeRebalanceGameTestHelper.restoreDefaultConfig();
		VillagerTradeRebalanceGameTestHelper.buildFloor(context, 4);
		context.setBlock(2, 1, 2, Blocks.LECTERN);

		Villager source = VillagerTradeRebalanceGameTestHelper.spawnVillager(context, 1, 1, 1);
		VillagerTradeRebalanceGameTestHelper.assignProfession(source, VillagerProfession.LIBRARIAN);
		VillagerTradeRebalanceGameTestHelper.setState(source, LibrarianTradeState.uninitialized());

		MerchantOffers seeded = new MerchantOffers();
		seeded.add(VillagerTradeRebalanceGameTestHelper.bookshelfOffer());
		seeded.add(VillagerTradeRebalanceGameTestHelper.bookOffer(context, Enchantments.MENDING, 1, 20));
		source.setOffers(seeded);

		Villager loaded = VillagerTradeRebalanceGameTestHelper.spawnVillager(context, 3, 1, 1);
		VillagerTradeRebalanceGameTestHelper.roundTripAdditionalSaveData(context, source, loaded);
		VillagerTradeRebalanceGameTestHelper.updateTrades(loaded);

		context.succeedWhen(() -> {
			if (!VillagerTradeRebalanceGameTestHelper.state(loaded).grandfathered()) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Loaded villager with existing offers should be grandfathered");
			}
			var books = VillagerTradeRebalanceGameTestHelper.bookOffers(loaded);
			boolean hasMending = books.stream()
					.anyMatch(offer -> "minecraft:mending".equals(VillagerTradeRebalanceGameTestHelper.bookEnchantId(offer)));
			if (!hasMending) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Grandfathered librarian lost vanilla Mending book");
			}
		});
	}
}
