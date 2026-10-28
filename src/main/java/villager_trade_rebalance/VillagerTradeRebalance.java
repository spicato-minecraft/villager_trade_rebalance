package villager_trade_rebalance;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import villager_trade_rebalance.config.ModConfig;

public class VillagerTradeRebalance implements ModInitializer {
	public static final String MOD_ID = "villager_trade_rebalance";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static ModConfig CONFIG;

	@Override
	public void onInitialize() {
		CONFIG = ModConfig.load();
		LOGGER.info("Villager Trade Rebalance loaded (librarian enchanted-book slice)");
	}
}
