package villager_trade_rebalance.enchant;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Default librarian book pool and vanilla max levels. Fortune is blocked (Looting parallel).
 */
public final class LibrarianEnchantments {

	public static final String EFFICIENCY = "minecraft:efficiency";
	public static final String UNBREAKING = "minecraft:unbreaking";
	public static final String SILK_TOUCH = "minecraft:silk_touch";
	public static final String FORTUNE = "minecraft:fortune";
	public static final String LOOTING = "minecraft:looting";
	public static final String MENDING = "minecraft:mending";
	public static final String SHARPNESS = "minecraft:sharpness";
	public static final String PROTECTION = "minecraft:protection";
	public static final String POWER = "minecraft:power";
	public static final String INFINITY = "minecraft:infinity";
	public static final String BINDING = "minecraft:binding_curse";
	public static final String VANISHING = "minecraft:vanishing_curse";
	public static final String WIND_BURST = "minecraft:wind_burst";
	public static final String LUCK_OF_THE_SEA = "minecraft:luck_of_the_sea";
	public static final String LURE = "minecraft:lure";
	public static final String FEATHER_FALLING = "minecraft:feather_falling";
	public static final String RESPIRATION = "minecraft:respiration";
	public static final String DEPTH_STRIDER = "minecraft:depth_strider";
	public static final String SWIFT_SNEAK = "minecraft:swift_sneak";
	public static final String SOUL_SPEED = "minecraft:soul_speed";

	private LibrarianEnchantments() {
	}

	public static Set<String> structureLootExclusive() {
		Set<String> ids = new LinkedHashSet<>();
		ids.add(RESPIRATION);
		ids.add(DEPTH_STRIDER);
		ids.add(SWIFT_SNEAK);
		ids.add(SOUL_SPEED);
		return ids;
	}

	public static Set<String> defaultAllowlist() {
		Set<String> ids = new LinkedHashSet<>();
		ids.add(EFFICIENCY);
		ids.add(UNBREAKING);
		ids.add(SILK_TOUCH);
		ids.add("minecraft:smite");
		ids.add("minecraft:bane_of_arthropods");
		ids.add("minecraft:knockback");
		ids.add("minecraft:fire_aspect");
		ids.add("minecraft:sweeping_edge");
		ids.add("minecraft:punch");
		ids.add("minecraft:flame");
		ids.add("minecraft:piercing");
		ids.add("minecraft:quick_charge");
		ids.add("minecraft:multishot");
		ids.add("minecraft:loyalty");
		ids.add("minecraft:impaling");
		ids.add("minecraft:channeling");
		ids.add("minecraft:riptide");
		ids.add("minecraft:density");
		ids.add("minecraft:breach");
		ids.add("minecraft:lunge");
		ids.add("minecraft:fire_protection");
		ids.add("minecraft:blast_protection");
		ids.add("minecraft:projectile_protection");
		ids.add(FEATHER_FALLING);
		ids.add("minecraft:thorns");
		ids.add("minecraft:aqua_affinity");
		ids.add("minecraft:frost_walker");
		return ids;
	}

	public static Set<String> defaultBlocked() {
		Set<String> ids = new LinkedHashSet<>();
		ids.add(FORTUNE);
		ids.add(LOOTING);
		ids.add(MENDING);
		ids.add(SHARPNESS);
		ids.add(PROTECTION);
		ids.add(POWER);
		ids.add(INFINITY);
		ids.add(BINDING);
		ids.add(VANISHING);
		ids.add(WIND_BURST);
		ids.add(LUCK_OF_THE_SEA);
		ids.add(LURE);
		ids.addAll(structureLootExclusive());
		return ids;
	}

	public static Map<String, Integer> defaultMaxLevels() {
		Map<String, Integer> max = new LinkedHashMap<>();
		max.put(EFFICIENCY, 5);
		max.put(UNBREAKING, 3);
		max.put(SILK_TOUCH, 1);
		max.put("minecraft:smite", 5);
		max.put("minecraft:bane_of_arthropods", 5);
		max.put("minecraft:knockback", 2);
		max.put("minecraft:fire_aspect", 2);
		max.put("minecraft:sweeping_edge", 3);
		max.put("minecraft:punch", 2);
		max.put("minecraft:flame", 1);
		max.put("minecraft:piercing", 4);
		max.put("minecraft:quick_charge", 3);
		max.put("minecraft:multishot", 1);
		max.put("minecraft:loyalty", 3);
		max.put("minecraft:impaling", 5);
		max.put("minecraft:channeling", 1);
		max.put("minecraft:riptide", 3);
		max.put("minecraft:density", 5);
		max.put("minecraft:breach", 4);
		max.put("minecraft:lunge", 3);
		max.put("minecraft:fire_protection", 4);
		max.put("minecraft:blast_protection", 4);
		max.put("minecraft:projectile_protection", 4);
		max.put("minecraft:feather_falling", 4);
		max.put("minecraft:thorns", 3);
		max.put("minecraft:respiration", 3);
		max.put("minecraft:aqua_affinity", 1);
		max.put("minecraft:depth_strider", 3);
		max.put("minecraft:frost_walker", 2);
		max.put("minecraft:soul_speed", 3);
		max.put("minecraft:swift_sneak", 3);
		return max;
	}
}
