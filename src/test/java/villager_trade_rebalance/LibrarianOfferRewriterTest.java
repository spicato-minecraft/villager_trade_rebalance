package villager_trade_rebalance;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import villager_trade_rebalance.config.LibrarianBooksConfig;
import villager_trade_rebalance.enchant.LibrarianEnchantments;
import villager_trade_rebalance.enchant.LineagePlanner.BookSpec;
import villager_trade_rebalance.trade.LibrarianOfferRewriter;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibrarianOfferRewriterTest {

	@BeforeAll
	static void bootstrap() {
		MinecraftTestBootstrap.init();
	}

	@Test
	void bookTradeXpMatchesVanillaCareerSteps() {
		assertEquals(1, LibrarianOfferRewriter.bookTradeXp(1));
		assertEquals(5, LibrarianOfferRewriter.bookTradeXp(2));
		assertEquals(10, LibrarianOfferRewriter.bookTradeXp(3));
		assertEquals(15, LibrarianOfferRewriter.bookTradeXp(4));
		assertEquals(30, LibrarianOfferRewriter.bookTradeXp(5));
	}

	@Test
	void emeraldOnlyBookOfferIsNotCompliant() {
		MerchantOffer offer = new MerchantOffer(
				new ItemCost(Items.EMERALD, 48),
				Optional.empty(),
				new ItemStack(Items.ENCHANTED_BOOK),
				12,
				1,
				0.2f
		);
		assertFalse(LibrarianOfferRewriter.hasBookCost(offer));
		assertFalse(LibrarianOfferRewriter.isCompliantBookOffer(offer, LibrarianBooksConfig.defaults(), new BookSpec(LibrarianEnchantments.EFFICIENCY, 1)));
	}

	@Test
	void emeraldAndBookOfferWithLevelCostIsCompliant() {
		LibrarianBooksConfig config = LibrarianBooksConfig.defaults();
		BookSpec efficiencyI = new BookSpec(LibrarianEnchantments.EFFICIENCY, 1);
		MerchantOffer offer = new MerchantOffer(
				new ItemCost(Items.EMERALD, 36),
				Optional.of(new ItemCost(Items.BOOK, 1)),
				new ItemStack(Items.ENCHANTED_BOOK),
				12,
				1,
				0.2f
		);
		assertTrue(LibrarianOfferRewriter.hasBookCost(offer));
		assertTrue(LibrarianOfferRewriter.isCompliantBookOffer(offer, config, efficiencyI));
		assertFalse(LibrarianOfferRewriter.isCompliantBookOffer(
				new MerchantOffer(
						new ItemCost(Items.EMERALD, 52),
						Optional.of(new ItemCost(Items.BOOK, 1)),
						new ItemStack(Items.ENCHANTED_BOOK),
						12,
						1,
						0.2f
				),
				config,
				efficiencyI
		));
	}
}
