package villager_trade_rebalance.trade;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import villager_trade_rebalance.VillagerTradeRebalance;
import villager_trade_rebalance.config.LibrarianBooksConfig;
import villager_trade_rebalance.enchant.LibrarianBookPolicy;
import villager_trade_rebalance.enchant.LineagePlanner;
import villager_trade_rebalance.enchant.LineagePlanner.BookSpec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Rebuilds librarian enchanted-book offers to a same-enchant lineage without touching non-book rows.
 */
public final class LibrarianOfferRewriter {

	private LibrarianOfferRewriter() {
	}

	public static MerchantOffers rewrite(
			MerchantOffers offers,
			int careerLevel,
			LibrarianBooksConfig config,
			HolderLookup.Provider registries,
			RandomSource random
	) {
		if (offers == null || offers.isEmpty() || !config.enabled()) {
			return offers;
		}

		List<BookSpec> existingBooks = new ArrayList<>();
		for (MerchantOffer offer : offers) {
			readBook(offer).ifPresent(existingBooks::add);
		}

		String lineage = LineagePlanner.inferLineage(config, existingBooks)
				.orElseGet(() -> LibrarianBookPolicy.rollNoviceEnchant(config, random.nextInt()));
		if (!LibrarianBookPolicy.isAllowed(config, lineage)) {
			VillagerTradeRebalance.LOGGER.warn("Rolled or inferred a blocked librarian enchant {}; skipping rewrite", lineage);
			return offers;
		}

		List<BookSpec> expected = LineagePlanner.expectedBooks(config, lineage, careerLevel);
		Holder<Enchantment> enchant = resolveEnchantment(registries, lineage);
		if (enchant == null) {
			return offers;
		}

		MerchantOffers rewritten = new MerchantOffers();
		Iterator<BookSpec> expectedBooks = expected.iterator();
		for (MerchantOffer offer : offers) {
			Optional<BookSpec> book = readBook(offer);
			if (book.isEmpty()) {
				rewritten.add(offer);
				continue;
			}
			if (!expectedBooks.hasNext()) {
				continue;
			}
			BookSpec next = expectedBooks.next();
			if (book.get().equals(next) && isCompliantBookOffer(offer, config, next)) {
				rewritten.add(offer);
			} else {
				int emeraldCost = LibrarianBookPolicy.baseCost(config, next.enchantId(), next.level());
				int xp = offer.getXp() > 0 ? offer.getXp() : bookTradeXp(careerLevel);
				rewritten.add(createBook(enchant, next.level(), emeraldCost, offer.getUses(), xp));
			}
		}
		while (expectedBooks.hasNext()) {
			BookSpec next = expectedBooks.next();
			rewritten.add(createBook(enchant, next.level(), LibrarianBookPolicy.baseCost(config, next.enchantId(), next.level()), 0, bookTradeXp(careerLevel)));
		}
		return rewritten;
	}

	public static Optional<BookSpec> readBook(MerchantOffer offer) {
		ItemStack result = offer.getResult();
		if (!result.is(Items.ENCHANTED_BOOK)) {
			return Optional.empty();
		}
		ItemEnchantments stored = result.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		String enchantId = null;
		int level = 0;
		for (var entry : stored.entrySet()) {
			Optional<ResourceKey<Enchantment>> key = entry.getKey().unwrapKey();
			if (key.isEmpty()) {
				continue;
			}
			enchantId = key.get().identifier().toString();
			level = stored.getLevel(entry.getKey());
			break;
		}
		if (enchantId == null) {
			return Optional.empty();
		}
		return Optional.of(new BookSpec(enchantId, level));
	}

	private static Holder<Enchantment> resolveEnchantment(HolderLookup.Provider registries, String enchantId) {
		Identifier id = Identifier.tryParse(enchantId);
		if (id == null) {
			return null;
		}
		return registries.lookupOrThrow(Registries.ENCHANTMENT)
				.get(ResourceKey.create(Registries.ENCHANTMENT, id))
				.orElse(null);
	}

	private static int baseEmeraldCost(MerchantOffer offer) {
		ItemCost cost = offer.getItemCostA();
		if (cost == null || !cost.itemStack().is(Items.EMERALD)) {
			return 0;
		}
		return cost.count();
	}

	public static boolean isCompliantBookOffer(MerchantOffer offer, LibrarianBooksConfig config) {
		return isCompliantBookOffer(offer, config, readBook(offer).orElse(null));
	}

	public static boolean isCompliantBookOffer(MerchantOffer offer, LibrarianBooksConfig config, BookSpec book) {
		if (book == null) {
			return false;
		}
		return baseEmeraldCost(offer) == LibrarianBookPolicy.baseCost(config, book.enchantId(), book.level())
				&& hasBookCost(offer)
				&& offer.getXp() > 0;
	}

	public static boolean hasBookCost(MerchantOffer offer) {
		return offer.getItemCostB().map(cost -> cost.itemStack().is(Items.BOOK)).orElse(false);
	}

	/** Vanilla librarian enchanted-book career XP by villager level. */
	public static int bookTradeXp(int careerLevel) {
		return switch (careerLevel) {
			case 2 -> 5;
			case 3 -> 10;
			case 4 -> 15;
			case 5 -> 30;
			default -> 1;
		};
	}

	private static MerchantOffer createBook(Holder<Enchantment> enchant, int level, int emeraldCost, int uses, int xp) {
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		stored.set(enchant, level);
		book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
		return new MerchantOffer(
				new ItemCost(Items.EMERALD, emeraldCost),
				Optional.of(new ItemCost(Items.BOOK, 1)),
				book,
				uses,
				12,
				Math.max(1, xp),
				0.2f
		);
	}
}
