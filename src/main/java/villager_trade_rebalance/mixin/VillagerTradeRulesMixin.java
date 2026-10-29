package villager_trade_rebalance.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import villager_trade_rebalance.VillagerTradeRebalance;
import villager_trade_rebalance.cartographer.CartographerOfferRewriter;
import villager_trade_rebalance.config.CartographerMapsConfig;
import villager_trade_rebalance.config.LibrarianBooksConfig;
import villager_trade_rebalance.persist.CartographerTradeData;
import villager_trade_rebalance.persist.CartographerTradeState;
import villager_trade_rebalance.persist.GrandfatherPolicy;
import villager_trade_rebalance.persist.LibrarianTradeData;
import villager_trade_rebalance.persist.LibrarianTradeState;
import villager_trade_rebalance.trade.LibrarianOfferRewriter;

/**
 * Applies librarian book rewrite and cartographer undecided-map lanes after vanilla generation.
 * Must not cancel {@code updateTrades} so locked_villager_trades can still snapshot sets.
 * Mixin priority 50 so this TAIL runs before default-priority LVT TAIL copies the offers.
 */
@Mixin(value = Villager.class, priority = 50)
public abstract class VillagerTradeRulesMixin {

	@Inject(method = "updateTrades", at = @At("TAIL"))
	private void villager_trade_rebalance$applyTradeRules(ServerLevel level, CallbackInfo ci) {
		if (VillagerTradeRebalance.CONFIG == null || !VillagerTradeRebalance.CONFIG.enabled()) {
			return;
		}
		Villager self = (Villager) (Object) this;
		if (self.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)) {
			villager_trade_rebalance$rewriteLibrarian(self, level);
			return;
		}
		if (self.getVillagerData().profession().is(VillagerProfession.CARTOGRAPHER)) {
			villager_trade_rebalance$rewriteCartographer(self);
		}
	}

	@Unique
	private static void villager_trade_rebalance$rewriteLibrarian(Villager self, ServerLevel level) {
		LibrarianBooksConfig books = VillagerTradeRebalance.CONFIG.librarianBooks();
		if (!books.enabled()) {
			return;
		}

		LibrarianTradeData data = (LibrarianTradeData) self;
		LibrarianTradeState state = GrandfatherPolicy.onRuntimeGeneration(data.villager_trade_rebalance$getState());
		data.villager_trade_rebalance$setState(state);
		if (state.grandfathered()) {
			return;
		}

		MerchantOffers current = self.getOffers();
		MerchantOffers rewritten = LibrarianOfferRewriter.rewrite(
				current,
				self.getVillagerData().level(),
				books,
				level.registryAccess(),
				self.getRandom()
		);
		if (rewritten != current) {
			self.setOffers(rewritten);
		}
	}

	@Unique
	private static void villager_trade_rebalance$rewriteCartographer(Villager self) {
		CartographerMapsConfig maps = VillagerTradeRebalance.CONFIG.cartographerMaps();
		if (!maps.enabled()) {
			return;
		}

		CartographerTradeData data = (CartographerTradeData) self;
		CartographerTradeState state = GrandfatherPolicy.cartographerOnRuntimeGeneration(
				data.villager_trade_rebalance$getCartographerState()
		);
		data.villager_trade_rebalance$setCartographerState(state);
		if (state.grandfathered()) {
			return;
		}

		MerchantOffers current = self.getOffers();
		MerchantOffers rewritten = CartographerOfferRewriter.rewrite(
				current,
				self.getVillagerData().level(),
				maps
		);
		if (rewritten != current) {
			self.setOffers(rewritten);
		}
	}
}
