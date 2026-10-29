package villager_trade_rebalance;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import villager_trade_rebalance.persist.LibrarianTradeStorage;
import villager_trade_rebalance.persist.VillagerTradePersistState;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibrarianTradeStorageCompatTest {

	@BeforeAll
	static void bootstrap() {
		MinecraftTestBootstrap.init();
	}

	@Test
	void oldInitializedGrandfatheredTagLoadsWithoutCartographerFields() {
		CompoundTag bag = new CompoundTag();
		bag.putBoolean("initialized", true);
		bag.putBoolean("grandfathered", true);

		VillagerTradePersistState state = LibrarianTradeStorage.CODEC.parse(NbtOps.INSTANCE, bag)
				.result()
				.orElseThrow();

		assertTrue(state.librarian().initialized());
		assertTrue(state.librarian().grandfathered());
		assertFalse(state.cartographer().initialized());
		assertFalse(state.cartographer().grandfathered());
	}
}
