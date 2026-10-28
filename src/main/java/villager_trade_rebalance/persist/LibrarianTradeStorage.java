package villager_trade_rebalance.persist;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class LibrarianTradeStorage {

	public static final String NBT_KEY = "VillagerTradeRebalance";

	public static final Codec<LibrarianTradeState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("initialized").forGetter(LibrarianTradeState::initialized),
			Codec.BOOL.fieldOf("grandfathered").forGetter(LibrarianTradeState::grandfathered)
	).apply(instance, LibrarianTradeState::new));

	private LibrarianTradeStorage() {
	}

	public static void writeTo(ValueOutput output, LibrarianTradeState state) {
		if (state == null || !state.initialized()) {
			return;
		}
		output.storeNullable(NBT_KEY, CODEC, state);
	}

	public static LibrarianTradeState readFrom(ValueInput input) {
		return input.read(NBT_KEY, CODEC).orElse(LibrarianTradeState.uninitialized());
	}
}
