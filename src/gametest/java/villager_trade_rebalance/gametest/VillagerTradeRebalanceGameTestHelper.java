package villager_trade_rebalance.gametest;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import villager_trade_rebalance.VillagerTradeRebalance;
import villager_trade_rebalance.config.ModConfig;
import villager_trade_rebalance.enchant.LibrarianBookPolicy;
import villager_trade_rebalance.mixin.VillagerAccessorMixin;
import villager_trade_rebalance.persist.LibrarianTradeData;
import villager_trade_rebalance.persist.LibrarianTradeState;
import villager_trade_rebalance.trade.LibrarianOfferRewriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class VillagerTradeRebalanceGameTestHelper {

	private VillagerTradeRebalanceGameTestHelper() {
	}

	static void restoreDefaultConfig() {
		VillagerTradeRebalance.CONFIG = ModConfig.parse("{}");
	}

	static void buildFloor(GameTestHelper context, int size) {
		for (int x = 0; x <= size; x++) {
			for (int z = 0; z <= size; z++) {
				context.setBlock(x, 0, z, Blocks.STONE);
			}
		}
	}

	static Villager spawnVillager(GameTestHelper context, int x, int y, int z) {
		return (Villager) context.spawn(EntityTypes.VILLAGER, x, y, z);
	}

	static void assignProfession(Villager villager, ResourceKey<VillagerProfession> profession) {
		villager.setVillagerData(villager.getVillagerData()
				.withProfession(villager.registryAccess(), profession)
				.withLevel(1));
	}

	static void setLevel(Villager villager, int level) {
		villager.setVillagerData(villager.getVillagerData().withLevel(level));
	}

	static void updateTrades(Villager villager) {
		((VillagerAccessorMixin) villager).villager_trade_rebalance$invokeUpdateTrades((ServerLevel) villager.level());
	}

	static LibrarianTradeState state(Villager villager) {
		return ((LibrarianTradeData) villager).villager_trade_rebalance$getState();
	}

	static void setState(Villager villager, LibrarianTradeState state) {
		((LibrarianTradeData) villager).villager_trade_rebalance$setState(state);
	}

	static ItemStack enchantedBook(GameTestHelper context, ResourceKey<Enchantment> enchantment, int level) {
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		Holder<Enchantment> holder = context.getLevel().registryAccess()
				.lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(enchantment);
		ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		stored.set(holder, level);
		book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
		return book;
	}

	static MerchantOffer bookOffer(GameTestHelper context, ResourceKey<Enchantment> enchantment, int level, int emeralds) {
		return new MerchantOffer(
				new ItemCost(Items.EMERALD, emeralds),
				Optional.of(new ItemCost(Items.BOOK, 1)),
				enchantedBook(context, enchantment, level),
				12,
				1,
				0.2f
		);
	}

	static MerchantOffer bookshelfOffer() {
		return new MerchantOffer(
				new ItemCost(Items.EMERALD, 9),
				new ItemStack(Items.BOOKSHELF),
				12,
				1,
				0.05f
		);
	}

	static List<MerchantOffer> bookOffers(Villager villager) {
		List<MerchantOffer> books = new ArrayList<>();
		for (MerchantOffer offer : villager.getOffers()) {
			if (offer.getResult().is(Items.ENCHANTED_BOOK)) {
				books.add(offer);
			}
		}
		return books;
	}

	static String bookEnchantId(MerchantOffer offer) {
		return LibrarianOfferRewriter.readBook(offer)
				.map(spec -> spec.enchantId())
				.orElseThrow(() -> fail("Offer is not an enchanted book"));
	}

	static int bookLevel(MerchantOffer offer) {
		return LibrarianOfferRewriter.readBook(offer)
				.map(spec -> spec.level())
				.orElseThrow(() -> fail("Offer is not an enchanted book"));
	}

	static boolean isAllowlistedLevelOne(MerchantOffer offer) {
		var books = VillagerTradeRebalance.CONFIG.librarianBooks();
		String id = bookEnchantId(offer);
		return LibrarianBookPolicy.isAllowed(books, id) && bookLevel(offer) == 1;
	}

	static void roundTripAdditionalSaveData(GameTestHelper context, Villager source, Villager target) {
		HolderLookup.Provider lookup = context.getLevel().registryAccess();
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, lookup);
		((VillagerAccessorMixin) source).villager_trade_rebalance$invokeAddAdditionalSaveData(output);
		CompoundTag tag = output.buildResult();
		ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, lookup, tag);
		((VillagerAccessorMixin) target).villager_trade_rebalance$invokeReadAdditionalSaveData(input);
	}

	static GameTestAssertException fail(String message) {
		return new GameTestAssertException(Component.literal(message), 0);
	}
}
