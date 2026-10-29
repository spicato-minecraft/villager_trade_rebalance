package villager_trade_rebalance.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import villager_trade_rebalance.VillagerTradeRebalance;
import villager_trade_rebalance.cartographer.TreasureMapLocator;
import villager_trade_rebalance.map.LocateDecision;

/**
 * Stamps a located treasure map onto the delivered clone before payment is taken.
 * Never writes {@code map_id} onto the stored offer template.
 */
@Mixin(MerchantResultSlot.class)
public abstract class MerchantResultSlotMixin {

	@Shadow
	@Final
	private MerchantContainer slots;

	@Shadow
	@Final
	private Merchant merchant;

	@Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
	private void villager_trade_rebalance$stampTreasureMap(Player player, ItemStack delivered, CallbackInfo ci) {
		if (player == null || player.level().isClientSide() || !(player.level() instanceof ServerLevel level)) {
			return;
		}
		if (VillagerTradeRebalance.CONFIG == null
				|| !VillagerTradeRebalance.CONFIG.enabled()
				|| !VillagerTradeRebalance.CONFIG.cartographerMaps().enabled()) {
			return;
		}
		if (!(merchant instanceof Villager villager)) {
			return;
		}
		MerchantOffer stored = slots.getActiveOffer();
		if (stored == null || TreasureMapLocator.laneOf(stored).isEmpty()) {
			return;
		}
		LocateDecision decision = TreasureMapLocator.stampDelivered(level, villager, stored, delivered);
		if (decision != LocateDecision.ABORT_AND_LOCK) {
			return;
		}
		delivered.setCount(0);
		stored.setToOutOfStock();
		ci.cancel();
	}
}
