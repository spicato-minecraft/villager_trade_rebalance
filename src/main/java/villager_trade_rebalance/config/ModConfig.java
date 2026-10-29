package villager_trade_rebalance.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import villager_trade_rebalance.VillagerTradeRebalance;
import villager_trade_rebalance.enchant.LibrarianEnchantments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class ModConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private boolean enabled = true;
	private LibrarianBooksConfig librarianBooks = LibrarianBooksConfig.defaults();
	private CartographerMapsConfig cartographerMaps = CartographerMapsConfig.defaults();

	public boolean enabled() {
		return enabled;
	}

	public LibrarianBooksConfig librarianBooks() {
		return librarianBooks;
	}

	public CartographerMapsConfig cartographerMaps() {
		return cartographerMaps;
	}

	public static ModConfig load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("villager_trade_rebalance.json");
		ModConfig config = new ModConfig();
		if (Files.exists(path)) {
			try {
				config = parse(Files.readString(path));
			} catch (Exception e) {
				VillagerTradeRebalance.LOGGER.warn("Failed to load config, using defaults: {}", e.getMessage());
				config = new ModConfig();
			}
		} else {
			config.save();
		}
		VillagerTradeRebalance.LOGGER.info(
				"Loaded config from {}: enabled={} librarianBooks.enabled={} cost={}-{} allowlist={} cartographerMaps.enabled={}",
				path.toAbsolutePath(),
				config.enabled,
				config.librarianBooks.enabled(),
				config.librarianBooks.baseCostMin(),
				config.librarianBooks.baseCostMax(),
				config.librarianBooks.allowlist().size(),
				config.cartographerMaps.enabled()
		);
		return config;
	}

	public static ModConfig parse(String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		ModConfig config = new ModConfig();
		if (root.has("enabled")) {
			config.enabled = root.get("enabled").getAsBoolean();
		}
		if (root.has("librarian_books") && root.get("librarian_books").isJsonObject()) {
			config.librarianBooks = parseLibrarianBooks(root.getAsJsonObject("librarian_books")).sanitized();
		} else {
			config.librarianBooks = LibrarianBooksConfig.defaults();
		}
		if (root.has("cartographer_maps") && root.get("cartographer_maps").isJsonObject()) {
			config.cartographerMaps = parseCartographerMaps(root.getAsJsonObject("cartographer_maps")).sanitized();
		} else {
			config.cartographerMaps = CartographerMapsConfig.defaults();
		}
		return config;
	}

	private static LibrarianBooksConfig parseLibrarianBooks(JsonObject obj) {
		LibrarianBooksConfig defaults = LibrarianBooksConfig.defaults();
		boolean booksEnabled = !obj.has("enabled") || obj.get("enabled").getAsBoolean();
		int min = obj.has("base_cost_min") ? obj.get("base_cost_min").getAsInt() : defaults.baseCostMin();
		int max = obj.has("base_cost_max") ? obj.get("base_cost_max").getAsInt() : defaults.baseCostMax();
		Set<String> allow = obj.has("allowlist")
				? stringSet(obj.getAsJsonArray("allowlist"))
				: new LinkedHashSet<>(defaults.allowlist());
		Set<String> blocked = obj.has("blocked")
				? stringSet(obj.getAsJsonArray("blocked"))
				: new LinkedHashSet<>(defaults.blocked());
		if (!blocked.contains(LibrarianEnchantments.FORTUNE)) {
			blocked.add(LibrarianEnchantments.FORTUNE);
		}
		for (String enchant : LibrarianEnchantments.structureLootExclusive()) {
			blocked.add(enchant);
			allow.remove(enchant);
		}
		Map<String, Integer> maxLevels = new LinkedHashMap<>(defaults.maxLevels());
		if (obj.has("max_levels") && obj.get("max_levels").isJsonObject()) {
			for (var entry : obj.getAsJsonObject("max_levels").entrySet()) {
				maxLevels.put(entry.getKey(), entry.getValue().getAsInt());
			}
		}
		return new LibrarianBooksConfig(booksEnabled, min, max, allow, blocked, maxLevels);
	}

	private static CartographerMapsConfig parseCartographerMaps(JsonObject obj) {
		CartographerMapsConfig defaults = CartographerMapsConfig.defaults();
		boolean mapsEnabled = !obj.has("enabled") || obj.get("enabled").getAsBoolean();
		int baseRadius = obj.has("base_radius") ? obj.get("base_radius").getAsInt() : defaults.baseRadius();
		int maxUses = obj.has("max_uses") ? obj.get("max_uses").getAsInt() : defaults.maxUses();
		int tier1 = obj.has("tier1_emeralds") ? obj.get("tier1_emeralds").getAsInt() : defaults.tier1Emeralds();
		int tier2 = obj.has("tier2_emeralds") ? obj.get("tier2_emeralds").getAsInt() : defaults.tier2Emeralds();
		int tier3 = obj.has("tier3_emeralds") ? obj.get("tier3_emeralds").getAsInt() : defaults.tier3Emeralds();
		return new CartographerMapsConfig(mapsEnabled, baseRadius, maxUses, tier1, tier2, tier3);
	}

	private static Set<String> stringSet(JsonArray array) {
		Set<String> values = new LinkedHashSet<>();
		for (JsonElement element : array) {
			values.add(element.getAsString());
		}
		return values;
	}

	public void save() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("villager_trade_rebalance.json");
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(toJson()));
		} catch (IOException e) {
			VillagerTradeRebalance.LOGGER.warn("Failed to save config: {}", e.getMessage());
		}
	}

	JsonObject toJson() {
		JsonObject root = new JsonObject();
		root.addProperty("enabled", enabled);
		JsonObject books = new JsonObject();
		books.addProperty("enabled", librarianBooks.enabled());
		books.addProperty("base_cost_min", librarianBooks.baseCostMin());
		books.addProperty("base_cost_max", librarianBooks.baseCostMax());
		JsonArray allow = new JsonArray();
		librarianBooks.allowlist().forEach(allow::add);
		books.add("allowlist", allow);
		JsonArray blocked = new JsonArray();
		librarianBooks.blocked().forEach(blocked::add);
		books.add("blocked", blocked);
		JsonObject maxLevels = new JsonObject();
		librarianBooks.maxLevels().forEach(maxLevels::addProperty);
		books.add("max_levels", maxLevels);
		root.add("librarian_books", books);
		JsonObject maps = new JsonObject();
		maps.addProperty("enabled", cartographerMaps.enabled());
		maps.addProperty("base_radius", cartographerMaps.baseRadius());
		maps.addProperty("max_uses", cartographerMaps.maxUses());
		maps.addProperty("tier1_emeralds", cartographerMaps.tier1Emeralds());
		maps.addProperty("tier2_emeralds", cartographerMaps.tier2Emeralds());
		maps.addProperty("tier3_emeralds", cartographerMaps.tier3Emeralds());
		root.add("cartographer_maps", maps);
		return root;
	}
}
