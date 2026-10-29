package villager_trade_rebalance.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import villager_trade_rebalance.persist.CartographerTradeData;
import villager_trade_rebalance.persist.CartographerTradeState;
import villager_trade_rebalance.persist.GrandfatherPolicy;
import villager_trade_rebalance.persist.LibrarianTradeData;
import villager_trade_rebalance.persist.LibrarianTradeState;
import villager_trade_rebalance.persist.LibrarianTradeStorage;
import villager_trade_rebalance.persist.VillagerTradePersistState;

@Mixin(Villager.class)
public abstract class VillagerLibrarianDataMixin implements LibrarianTradeData, CartographerTradeData {

	@Unique
	private LibrarianTradeState villager_trade_rebalance$state = LibrarianTradeState.uninitialized();

	@Unique
	private CartographerTradeState villager_trade_rebalance$cartographerState = CartographerTradeState.uninitialized();

	@Override
	public LibrarianTradeState villager_trade_rebalance$getState() {
		if (villager_trade_rebalance$state == null) {
			villager_trade_rebalance$state = LibrarianTradeState.uninitialized();
		}
		return villager_trade_rebalance$state;
	}

	@Override
	public void villager_trade_rebalance$setState(LibrarianTradeState state) {
		this.villager_trade_rebalance$state = state != null ? state : LibrarianTradeState.uninitialized();
	}

	@Override
	public CartographerTradeState villager_trade_rebalance$getCartographerState() {
		if (villager_trade_rebalance$cartographerState == null) {
			villager_trade_rebalance$cartographerState = CartographerTradeState.uninitialized();
		}
		return villager_trade_rebalance$cartographerState;
	}

	@Override
	public void villager_trade_rebalance$setCartographerState(CartographerTradeState state) {
		this.villager_trade_rebalance$cartographerState = state != null ? state : CartographerTradeState.uninitialized();
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void villager_trade_rebalance$writeNbt(ValueOutput output, CallbackInfo ci) {
		LibrarianTradeStorage.writeTo(output, villager_trade_rebalance$state, villager_trade_rebalance$cartographerState);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void villager_trade_rebalance$readNbt(ValueInput input, CallbackInfo ci) {
		Villager self = (Villager) (Object) this;
		VillagerTradePersistState stored = LibrarianTradeStorage.readFrom(input);
		MerchantOffers offers = self.getOffers();
		boolean hasOffers = offers != null && !offers.isEmpty();
		villager_trade_rebalance$state = GrandfatherPolicy.onLoad(stored.librarian(), hasOffers);
		villager_trade_rebalance$cartographerState = GrandfatherPolicy.cartographerOnLoad(
				stored.cartographer(),
				villager_trade_rebalance$hasLocatedMapOffer(offers)
		);
	}

	@Unique
	private static boolean villager_trade_rebalance$hasLocatedMapOffer(MerchantOffers offers) {
		if (offers == null || offers.isEmpty()) {
			return false;
		}
		for (MerchantOffer offer : offers) {
			ItemStack result = offer.getResult();
			if (result == null || result.isEmpty()) {
				continue;
			}
			if (result.has(DataComponents.MAP_ID)) {
				return true;
			}
		}
		return false;
	}
}
