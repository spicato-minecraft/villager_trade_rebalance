package villager_trade_rebalance.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import villager_trade_rebalance.VillagerTradeRebalance;
import villager_trade_rebalance.enchant.LibrarianBookPolicy;
import villager_trade_rebalance.mixin.VillagerAccessorMixin;

public class LibrarianNoviceBookGameTest {

	@GameTest(maxTicks = 200)
	public void noviceLibrarianBookIsAllowlistedLevelOne(GameTestHelper context) {
		VillagerTradeRebalanceGameTestHelper.restoreDefaultConfig();
		for (int x = 0; x <= 4; x++) {
			for (int z = 0; z <= 4; z++) {
				context.setBlock(x, 0, z, Blocks.STONE);
			}
		}
		context.setBlock(2, 1, 2, Blocks.LECTERN);

		Villager villager = (Villager) context.spawn(EntityTypes.VILLAGER, 1, 1, 1);
		villager.setVillagerData(villager.getVillagerData()
				.withProfession(villager.registryAccess(), VillagerProfession.LIBRARIAN)
				.withLevel(1));
		((VillagerAccessorMixin) villager).villager_trade_rebalance$invokeUpdateTrades((ServerLevel) villager.level());

		context.succeedWhen(() -> {
			boolean foundBook = false;
			for (MerchantOffer offer : villager.getOffers()) {
				if (!offer.getResult().is(Items.ENCHANTED_BOOK)) {
					continue;
				}
				foundBook = true;
				if (offer.getXp() < 1) {
					throw fail("Enchanted-book trade awarded no villager XP");
				}
				if (!offer.getItemCostA().itemStack().is(Items.EMERALD)) {
					throw fail("Enchanted-book trade must cost emeralds");
				}
				if (offer.getItemCostB().isEmpty() || !offer.getItemCostB().get().itemStack().is(Items.BOOK)) {
					throw fail("Enchanted-book trade must also cost a book");
				}
				ItemEnchantments stored = offer.getResult()
						.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
				stored.entrySet().forEach(entry -> {
					String id = entry.getKey().unwrapKey().orElseThrow().identifier().toString();
					int level = stored.getLevel(entry.getKey());
					var books = VillagerTradeRebalance.CONFIG.librarianBooks();
					if (!LibrarianBookPolicy.isAllowed(books, id)) {
						throw fail("Blocked enchant on novice librarian: " + id);
					}
					if (level != 1) {
						throw fail("Expected level 1 for " + id + " but was " + level);
					}
					int expectedCost = LibrarianBookPolicy.baseCost(books, id, level);
					if (offer.getItemCostA().count() != expectedCost) {
						throw fail("Level I " + id + " should cost " + expectedCost
								+ " emeralds but was " + offer.getItemCostA().count());
					}
				});
			}
			if (!foundBook) {
				throw fail("Novice librarian did not offer an enchanted book");
			}
		});
	}

	private static GameTestAssertException fail(String message) {
		return new GameTestAssertException(Component.literal(message), 0);
	}
}
