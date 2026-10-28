package villager_trade_rebalance.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Villager.class)
public interface VillagerAccessorMixin {

	@Invoker("updateTrades")
	void villager_trade_rebalance$invokeUpdateTrades(ServerLevel level);

	@Invoker("addAdditionalSaveData")
	void villager_trade_rebalance$invokeAddAdditionalSaveData(ValueOutput output);

	@Invoker("readAdditionalSaveData")
	void villager_trade_rebalance$invokeReadAdditionalSaveData(ValueInput input);
}
