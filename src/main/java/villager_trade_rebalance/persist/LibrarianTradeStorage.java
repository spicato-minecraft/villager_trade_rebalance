package villager_trade_rebalance.persist;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class LibrarianTradeStorage {

	public static final String NBT_KEY = "VillagerTradeRebalance";

	public static final Codec<VillagerTradePersistState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("initialized").forGetter(state -> state.librarian().initialized()),
			Codec.BOOL.fieldOf("grandfathered").forGetter(state -> state.librarian().grandfathered()),
			Codec.BOOL.optionalFieldOf("cartographer_initialized", false)
					.forGetter(state -> state.cartographer().initialized()),
			Codec.BOOL.optionalFieldOf("cartographer_grandfathered", false)
					.forGetter(state -> state.cartographer().grandfathered())
	).apply(instance, (initialized, grandfathered, cartographerInitialized, cartographerGrandfathered) ->
			new VillagerTradePersistState(
					new LibrarianTradeState(initialized, grandfathered),
					new CartographerTradeState(cartographerInitialized, cartographerGrandfathered)
			)));

	private LibrarianTradeStorage() {
	}

	public static void writeTo(ValueOutput output, LibrarianTradeState librarian, CartographerTradeState cartographer) {
		VillagerTradePersistState state = new VillagerTradePersistState(
				librarian != null ? librarian : LibrarianTradeState.uninitialized(),
				cartographer != null ? cartographer : CartographerTradeState.uninitialized()
		);
		if (!state.hasAnyInitialized()) {
			return;
		}
		output.storeNullable(NBT_KEY, CODEC, state);
	}

	public static VillagerTradePersistState readFrom(ValueInput input) {
		return input.read(NBT_KEY, CODEC).orElse(VillagerTradePersistState.uninitialized());
	}
}
