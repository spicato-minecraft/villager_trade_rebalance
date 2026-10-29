package villager_trade_rebalance.map;

/**
 * Whether locate should run when the remaining destination set is considered.
 * Empty remaining destinations abort and lock; they must not emit a blank map.
 */
public enum LocateDecision {
	LOCATE,
	ABORT_AND_LOCK
}
