package villager_trade_rebalance.cartographer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.functions.ExplorationMapFunction;
import villager_trade_rebalance.VillagerTradeRebalance;
import villager_trade_rebalance.cartographer.CartographerDestinations.MapTypeSpec;
import villager_trade_rebalance.config.CartographerMapsConfig;
import villager_trade_rebalance.map.CartographerMapPolicy;
import villager_trade_rebalance.map.LocateDecision;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;

/**
 * Purchase-time locate for undecided cartographer lanes. Stamps {@code map_id} onto the
 * delivered clone only — never onto {@link MerchantOffer#getResult()}.
 */
public final class TreasureMapLocator {

	public static final String LANE_KEY = "vtr_map_lane";

	private static volatile int lastSearchRadiusForTest = -1;

	private TreasureMapLocator() {
	}

	/** Last locate radius used by {@link #stampDelivered}; GameTest hook only. */
	public static int lastSearchRadiusForTest() {
		return lastSearchRadiusForTest;
	}

	public static OptionalInt laneOf(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return OptionalInt.empty();
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null || data.isEmpty()) {
			return OptionalInt.empty();
		}
		CompoundTag tag = data.copyTag();
		if (!tag.contains(LANE_KEY)) {
			return OptionalInt.empty();
		}
		int lane = tag.getIntOr(LANE_KEY, 0);
		if (lane < CartographerDestinations.TIER_1 || lane > CartographerDestinations.TIER_3) {
			return OptionalInt.empty();
		}
		return OptionalInt.of(lane);
	}

	public static OptionalInt laneOf(MerchantOffer offer) {
		if (offer == null) {
			return OptionalInt.empty();
		}
		return laneOf(offer.getResult());
	}

	public static void writeLane(ItemStack stack, int lane) {
		CompoundTag tag = new CompoundTag();
		CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
		if (existing != null && !existing.isEmpty()) {
			tag = existing.copyTag();
		}
		tag.putInt(LANE_KEY, lane);
		CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
	}

	/**
	 * Locates a destination for {@code stored}'s lane and writes map data onto {@code delivered}.
	 * Returns {@link LocateDecision#ABORT_AND_LOCK} when no type locates; the stored offer is left
	 * undecided (no {@code map_id}).
	 */
	public static LocateDecision stampDelivered(ServerLevel level, Villager villager, MerchantOffer stored, ItemStack delivered) {
		if (level == null || villager == null || stored == null || delivered == null || delivered.isEmpty()) {
			return LocateDecision.ABORT_AND_LOCK;
		}
		OptionalInt laneOpt = laneOf(stored);
		if (laneOpt.isEmpty()) {
			return LocateDecision.LOCATE;
		}
		if (VillagerTradeRebalance.CONFIG == null) {
			return LocateDecision.ABORT_AND_LOCK;
		}
		CartographerMapsConfig maps = VillagerTradeRebalance.CONFIG.cartographerMaps();
		int lane = laneOpt.getAsInt();
		int career = villager.getVillagerData().level();
		var radius = CartographerMapPolicy.radius(career, lane, maps.baseRadius());
		if (radius.isEmpty()) {
			return LocateDecision.ABORT_AND_LOCK;
		}
		lastSearchRadiusForTest = radius.get();
		String variant = villager.getVillagerData().type().unwrapKey()
				.map(key -> key.identifier().toString())
				.orElse("");
		Set<String> failed = new LinkedHashSet<>();
		Random random = new Random(villager.getRandom().nextLong());
		while (true) {
			var pick = CartographerDestinations.pickNext(lane, variant, failed, random);
			if (pick.isEmpty()) {
				return CartographerMapPolicy.emptySetDecision(List.of());
			}
			MapTypeSpec spec = pick.get();
			if (tryLocate(level, villager.blockPosition(), spec, radius.get(), delivered)) {
				writeLocatedDisplay(delivered, lane);
				return LocateDecision.LOCATE;
			}
			failed.add(spec.typeId());
			failed.add(spec.itemId());
			failed.add(spec.destinationTag());
			spec.decorationId().ifPresent(failed::add);
		}
	}

	private static boolean tryLocate(ServerLevel level, BlockPos origin, MapTypeSpec spec, int radius, ItemStack delivered) {
		HolderSet<Structure> destinations = resolveDestination(level, spec.destinationTag());
		if (destinations == null) {
			return false;
		}
		BlockPos found = level.findNearestMapStructure(destinations, origin, radius, ExplorationMapFunction.DEFAULT_SKIP_EXISTING);
		if (found == null) {
			return false;
		}
		MapItem.applyNewSavedData(
				level,
				delivered,
				found.getX(),
				found.getZ(),
				ExplorationMapFunction.DEFAULT_ZOOM,
				true,
				true
		);
		MapItem.renderBiomePreviewMap(level, delivered);
		MapItemSavedData.addTargetDecoration(delivered, found, "+", decorationFor(level, spec));
		return delivered.has(DataComponents.MAP_ID);
	}

	/**
	 * Shop templates stay {@code Tier N} only. After locate, the delivered clone also shows
	 * the vanilla world {@code map_id} so same-tier purchases can be told apart.
	 */
	public static void writeLocatedDisplay(ItemStack stack, int lane) {
		if (stack == null || stack.isEmpty()) {
			return;
		}
		List<Component> lines = new ArrayList<>();
		lines.add(loreLine("Tier " + lane));
		MapId mapId = stack.get(DataComponents.MAP_ID);
		if (mapId != null) {
			lines.add(loreLine("#" + mapId.id()));
		}
		stack.set(DataComponents.LORE, new ItemLore(lines));
	}

	private static Component loreLine(String text) {
		return Component.literal(text).withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false));
	}

	private static HolderSet<Structure> resolveDestination(ServerLevel level, String destinationTag) {
		if (destinationTag == null || destinationTag.isBlank()) {
			return null;
		}
		String raw = destinationTag.startsWith("#") ? destinationTag.substring(1) : destinationTag;
		Identifier id = Identifier.tryParse(raw);
		if (id == null) {
			return null;
		}
		TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, id);
		return level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(tag).orElse(null);
	}

	private static Holder<MapDecorationType> decorationFor(ServerLevel level, MapTypeSpec spec) {
		if (spec.decorationId().isEmpty()) {
			return ExplorationMapFunction.DEFAULT_DECORATION;
		}
		Identifier id = Identifier.tryParse(spec.decorationId().get());
		if (id == null) {
			return ExplorationMapFunction.DEFAULT_DECORATION;
		}
		return level.registryAccess()
				.lookupOrThrow(Registries.MAP_DECORATION_TYPE)
				.get(ResourceKey.create(Registries.MAP_DECORATION_TYPE, id))
				.map(holder -> (Holder<MapDecorationType>) holder)
				.orElse(ExplorationMapFunction.DEFAULT_DECORATION);
	}
}
