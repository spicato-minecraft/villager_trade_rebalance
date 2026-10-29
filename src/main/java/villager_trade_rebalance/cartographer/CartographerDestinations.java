package villager_trade_rebalance.cartographer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Vanilla 26.3 cartographer explorer-map catalog (T1 biome-gated, T2/T3 all variants).
 * Ruined portals locate {@code #ruined_portal_rebalance:paired_portals}, not vanilla portal maps.
 */
public final class CartographerDestinations {

	public static final int TIER_1 = 1;
	public static final int TIER_2 = 2;
	public static final int TIER_3 = 3;

	public static final String VARIANT_PLAINS = "minecraft:plains";
	public static final String VARIANT_TAIGA = "minecraft:taiga";
	public static final String VARIANT_SNOW = "minecraft:snow";
	public static final String VARIANT_SAVANNA = "minecraft:savanna";
	public static final String VARIANT_DESERT = "minecraft:desert";
	public static final String VARIANT_JUNGLE = "minecraft:jungle";
	public static final String VARIANT_SWAMP = "minecraft:swamp";

	public static final String PLAINS_VILLAGE = "plains_village";
	public static final String DESERT_VILLAGE = "desert_village";
	public static final String SAVANNA_VILLAGE = "savanna_village";
	public static final String SNOWY_VILLAGE = "snowy_village";
	public static final String TAIGA_VILLAGE = "taiga_village";
	public static final String JUNGLE_PYRAMID = "jungle_pyramid";
	public static final String SWAMP_HUT = "swamp_hut";
	public static final String OCEAN_MONUMENT = "ocean_monument";
	public static final String BURIED_TRIAL_CHAMBERS = "buried_trial_chambers";
	public static final String RUINED_PORTAL = "ruined_portal";
	public static final String WOODLAND_MANSION = "woodland_mansion";

	public static final String PAIRED_PORTALS_TAG = "#ruined_portal_rebalance:paired_portals";

	private static final List<MapTypeSpec> CATALOG = List.of(
			t1(PLAINS_VILLAGE, "minecraft:plains_village_map", "#minecraft:on_plains_village_maps",
					"minecraft:village_plains", VARIANT_TAIGA, VARIANT_SNOW, VARIANT_SAVANNA, VARIANT_DESERT),
			t1(DESERT_VILLAGE, "minecraft:desert_village_map", "#minecraft:on_desert_village_maps",
					"minecraft:village_desert", VARIANT_SAVANNA, VARIANT_JUNGLE),
			t1(SAVANNA_VILLAGE, "minecraft:savanna_village_map", "#minecraft:on_savanna_village_maps",
					"minecraft:village_savanna", VARIANT_PLAINS, VARIANT_JUNGLE, VARIANT_DESERT),
			t1(SNOWY_VILLAGE, "minecraft:snowy_village_map", "#minecraft:on_snowy_village_maps",
					"minecraft:village_snowy", VARIANT_TAIGA, VARIANT_SWAMP),
			t1(TAIGA_VILLAGE, "minecraft:taiga_village_map", "#minecraft:on_taiga_village_maps",
					"minecraft:village_taiga", VARIANT_SWAMP, VARIANT_SNOW, VARIANT_PLAINS),
			t1(JUNGLE_PYRAMID, "minecraft:jungle_pyramid_map", "#minecraft:on_jungle_pyramid_maps",
					"minecraft:jungle_temple", VARIANT_SWAMP, VARIANT_SAVANNA, VARIANT_DESERT),
			t1(SWAMP_HUT, "minecraft:swamp_hut_map", "#minecraft:on_swamp_hut_maps",
					"minecraft:swamp_hut", VARIANT_TAIGA, VARIANT_SNOW, VARIANT_JUNGLE),
			allVariants(TIER_2, OCEAN_MONUMENT, "minecraft:ocean_monument_map",
					"#minecraft:on_ocean_monument_maps", "minecraft:monument"),
			allVariants(TIER_2, BURIED_TRIAL_CHAMBERS, "minecraft:buried_trial_chambers_map",
					"#minecraft:on_buried_trial_chambers_maps", "minecraft:trial_chambers"),
			allVariants(TIER_2, RUINED_PORTAL, "minecraft:filled_map",
					PAIRED_PORTALS_TAG, "minecraft:ruined_portal"),
			allVariants(TIER_3, WOODLAND_MANSION, "minecraft:woodland_mansion_map",
					"#minecraft:on_woodland_mansion_maps", "minecraft:mansion")
	);

	private CartographerDestinations() {
	}

	public static List<MapTypeSpec> allTypes() {
		return CATALOG;
	}

	public static List<MapTypeSpec> typesInLane(int lane) {
		List<MapTypeSpec> out = new ArrayList<>();
		for (MapTypeSpec spec : CATALOG) {
			if (spec.lane() == lane) {
				out.add(spec);
			}
		}
		return List.copyOf(out);
	}

	public static List<MapTypeSpec> eligibleTypes(int lane, String villagerVariantId) {
		String variant = normalizeVariant(villagerVariantId);
		List<MapTypeSpec> out = new ArrayList<>();
		for (MapTypeSpec spec : CATALOG) {
			if (spec.lane() != lane) {
				continue;
			}
			if (spec.allowsVariant(variant)) {
				out.add(spec);
			}
		}
		return List.copyOf(out);
	}

	public static Optional<MapTypeSpec> pickNext(int lane, String villagerVariantId, Set<String> typesThatFailedLocate, Random random) {
		Set<String> failed = typesThatFailedLocate == null ? Set.of() : typesThatFailedLocate;
		List<MapTypeSpec> remaining = new ArrayList<>();
		for (MapTypeSpec spec : eligibleTypes(lane, villagerVariantId)) {
			if (!spec.matchesAny(failed)) {
				remaining.add(spec);
			}
		}
		if (remaining.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(remaining.get(random.nextInt(remaining.size())));
	}

	public static String normalizeVariant(String villagerVariantId) {
		if (villagerVariantId == null || villagerVariantId.isBlank()) {
			return "";
		}
		return villagerVariantId.indexOf(':') >= 0 ? villagerVariantId : "minecraft:" + villagerVariantId;
	}

	private static MapTypeSpec t1(String typeId, String itemId, String destinationTag, String decoration, String... variants) {
		Set<String> allowed = new LinkedHashSet<>();
		Collections.addAll(allowed, variants);
		return new MapTypeSpec(TIER_1, typeId, itemId, destinationTag, Optional.of(decoration), Set.copyOf(allowed));
	}

	private static MapTypeSpec allVariants(int lane, String typeId, String itemId, String destinationTag, String decoration) {
		return new MapTypeSpec(lane, typeId, itemId, destinationTag, Optional.of(decoration), Set.of());
	}

	/**
	 * @param allowedVillagerVariants empty means every villager variant may sell this type
	 */
	public record MapTypeSpec(
			int lane,
			String typeId,
			String itemId,
			String destinationTag,
			Optional<String> decorationId,
			Set<String> allowedVillagerVariants
	) {
		public MapTypeSpec {
			allowedVillagerVariants = Set.copyOf(allowedVillagerVariants);
		}

		public boolean allowsVariant(String villagerVariantId) {
			if (allowedVillagerVariants.isEmpty()) {
				return true;
			}
			return allowedVillagerVariants.contains(normalizeVariant(villagerVariantId));
		}

		public boolean matchesAny(Set<String> keys) {
			if (keys.contains(typeId) || keys.contains(itemId) || keys.contains(destinationTag)) {
				return true;
			}
			if (destinationTag.startsWith("#") && keys.contains(destinationTag.substring(1))) {
				return true;
			}
			return decorationId.isPresent() && keys.contains(decorationId.get());
		}
	}
}
