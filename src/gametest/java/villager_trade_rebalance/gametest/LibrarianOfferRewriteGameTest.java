package villager_trade_rebalance.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import villager_trade_rebalance.VillagerTradeRebalance;
import villager_trade_rebalance.config.LibrarianBooksConfig;
import villager_trade_rebalance.config.ModConfig;
import villager_trade_rebalance.enchant.LibrarianBookPolicy;
import villager_trade_rebalance.trade.LibrarianOfferRewriter;

public class LibrarianOfferRewriteGameTest {

	@GameTest(maxTicks = 100)
	public void rewriteKeepsNonBooksAndOneLineage(GameTestHelper context) {
		VillagerTradeRebalanceGameTestHelper.restoreDefaultConfig();
		LibrarianBooksConfig config = VillagerTradeRebalance.CONFIG.librarianBooks();

		MerchantOffers input = new MerchantOffers();
		input.add(VillagerTradeRebalanceGameTestHelper.bookshelfOffer());
		input.add(VillagerTradeRebalanceGameTestHelper.bookOffer(context, Enchantments.EFFICIENCY, 1, 12));
		input.add(VillagerTradeRebalanceGameTestHelper.bookOffer(context, Enchantments.UNBREAKING, 1, 12));

		MerchantOffers rewritten = LibrarianOfferRewriter.rewrite(
				input,
				1,
				config,
				context.getLevel().registryAccess(),
				RandomSource.create(1)
		);

		context.succeedWhen(() -> {
			boolean keptBookshelf = rewritten.stream().anyMatch(offer -> offer.getResult().is(Items.BOOKSHELF));
			if (!keptBookshelf) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Rewrite dropped the non-book bookshelf offer");
			}
			long books = rewritten.stream().filter(offer -> offer.getResult().is(Items.ENCHANTED_BOOK)).count();
			if (books != 1) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Career 1 rewrite should leave one lineage book, had " + books);
			}
			MerchantOffer book = rewritten.stream()
					.filter(offer -> offer.getResult().is(Items.ENCHANTED_BOOK))
					.findFirst()
					.orElseThrow();
			if (!"minecraft:efficiency".equals(VillagerTradeRebalanceGameTestHelper.bookEnchantId(book))) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Expected Efficiency lineage, was "
						+ VillagerTradeRebalanceGameTestHelper.bookEnchantId(book));
			}
			int expectedCost = LibrarianBookPolicy.baseCost(config, "minecraft:efficiency", 1);
			if (book.getItemCostA().count() != expectedCost) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Expected policy cost " + expectedCost
						+ " but was " + book.getItemCostA().count());
			}
		});
	}

	@GameTest(maxTicks = 100)
	public void rewriteNoopsWhenLibrarianBooksDisabled(GameTestHelper context) {
		ModConfig parsed = ModConfig.parse("""
				{ "librarian_books": { "enabled": false } }
				""");
		MerchantOffers input = new MerchantOffers();
		input.add(VillagerTradeRebalanceGameTestHelper.bookOffer(context, Enchantments.MENDING, 1, 10));

		MerchantOffers rewritten = LibrarianOfferRewriter.rewrite(
				input,
				1,
				parsed.librarianBooks(),
				context.getLevel().registryAccess(),
				RandomSource.create(1)
		);

		context.succeedWhen(() -> {
			if (rewritten != input) {
				throw VillagerTradeRebalanceGameTestHelper.fail("Disabled librarian_books should return the same offers instance");
			}
		});
	}
}
