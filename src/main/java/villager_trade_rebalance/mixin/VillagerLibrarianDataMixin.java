package villager_trade_rebalance.mixin;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import villager_trade_rebalance.persist.GrandfatherPolicy;
import villager_trade_rebalance.persist.LibrarianTradeData;
import villager_trade_rebalance.persist.LibrarianTradeState;
import villager_trade_rebalance.persist.LibrarianTradeStorage;

@Mixin(Villager.class)
public abstract class VillagerLibrarianDataMixin implements LibrarianTradeData {

	@Unique
	private LibrarianTradeState villager_trade_rebalance$state = LibrarianTradeState.uninitialized();

	@Override
	public LibrarianTradeState villager_trade_rebalance$getState() {
		return villager_trade_rebalance$state;
	}

	@Override
	public void villager_trade_rebalance$setState(LibrarianTradeState state) {
		this.villager_trade_rebalance$state = state != null ? state : LibrarianTradeState.uninitialized();
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void villager_trade_rebalance$writeNbt(ValueOutput output, CallbackInfo ci) {
		LibrarianTradeStorage.writeTo(output, villager_trade_rebalance$state);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void villager_trade_rebalance$readNbt(ValueInput input, CallbackInfo ci) {
		Villager self = (Villager) (Object) this;
		LibrarianTradeState stored = LibrarianTradeStorage.readFrom(input);
		MerchantOffers offers = self.getOffers();
		boolean hasOffers = offers != null && !offers.isEmpty();
		villager_trade_rebalance$state = GrandfatherPolicy.onLoad(stored, hasOffers);
	}
}
