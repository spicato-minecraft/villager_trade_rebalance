package villager_trade_rebalance.cartographer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import villager_trade_rebalance.config.CartographerMapsConfig;
import villager_trade_rebalance.map.CartographerMapPolicy;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * After vanilla cartographer generation: drop located vanilla maps and ensure one undecided
 * offer per unlocked lane. Datapack-created rows with {@code vtr_map_lane} are kept.
 */
public final class CartographerOfferRewriter {

	/** Vanilla explorer-map {@code reputation_discount}. */
	public static final float PRICE_MULTIPLIER = 0.2f;

	private CartographerOfferRewriter() {
	}

	public static MerchantOffers rewrite(MerchantOffers offers, int careerLevel, CartographerMapsConfig config) {
		if (offers == null || !config.enabled()) {
			return offers;
		}
		Set<Integer> unlocked = CartographerMapPolicy.unlockedLanes(careerLevel);
		Set<String> catalogItems = catalogItemIds();
		MerchantOffers rewritten = new MerchantOffers();
		Set<Integer> seenLanes = new HashSet<>();
		for (MerchantOffer offer : offers) {
			OptionalInt lane = TreasureMapLocator.laneOf(offer);
			if (lane.isPresent()) {
				int value = lane.getAsInt();
				if (unlocked.contains(value) && seenLanes.add(value) && !offer.getResult().has(DataComponents.MAP_ID)) {
					rewritten.add(offer);
				}
				continue;
			}
			if (isVanillaLocatedMap(offer, catalogItems)) {
				continue;
			}
			rewritten.add(offer);
		}
		for (int lane = CartographerDestinations.TIER_1; lane <= CartographerDestinations.TIER_3; lane++) {
			if (unlocked.contains(lane) && !seenLanes.contains(lane)) {
				rewritten.add(createUndecided(lane, config));
			}
		}
		return rewritten;
	}

	public static MerchantOffer createUndecided(int lane, CartographerMapsConfig config) {
		ItemStack result = new ItemStack(Items.FILLED_MAP);
		result.set(DataComponents.CUSTOM_NAME, Component.literal("Treasure Map"));
		result.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Tier " + lane))));
		TreasureMapLocator.writeLane(result, lane);
		return new MerchantOffer(
				new ItemCost(Items.EMERALD, emeraldsFor(lane, config)),
				Optional.of(new ItemCost(Items.COMPASS, 1)),
				result,
				0,
				config.maxUses(),
				xpFor(lane),
				PRICE_MULTIPLIER
		);
	}

	public static boolean isVanillaLocatedMap(MerchantOffer offer, Set<String> catalogItems) {
		ItemStack result = offer.getResult();
		if (result == null || result.isEmpty()) {
			return false;
		}
		if (TreasureMapLocator.laneOf(result).isPresent()) {
			return false;
		}
		if (result.has(DataComponents.MAP_ID)) {
			return true;
		}
		return catalogItems.contains(itemId(result));
	}

	public static int xpFor(int lane) {
		return switch (lane) {
			case 2 -> 10;
			case 3 -> 30;
			default -> 5;
		};
	}

	public static int emeraldsFor(int lane, CartographerMapsConfig config) {
		return switch (lane) {
			case 2 -> config.tier2Emeralds();
			case 3 -> config.tier3Emeralds();
			default -> config.tier1Emeralds();
		};
	}

	public static Set<String> catalogItemIds() {
		Set<String> ids = new HashSet<>();
		for (CartographerDestinations.MapTypeSpec spec : CartographerDestinations.allTypes()) {
			ids.add(spec.itemId());
		}
		return Set.copyOf(ids);
	}

	private static String itemId(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}
}
