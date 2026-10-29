package villager_trade_rebalance.map;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Pure career-level rules for cartographer treasure maps. No world, catalog, or NBT.
 */
public final class CartographerMapPolicy {

	private CartographerMapPolicy() {
	}

	/**
	 * Highest map tier unlocked at a career level (1 Novice … 5 Master): {@code 0,1,2,2,3}.
	 */
	public static int highestMapTier(int careerLevel) {
		return switch (careerLevel) {
			case 2 -> 1;
			case 3, 4 -> 2;
			case 5 -> 3;
			default -> 0;
		};
	}

	/**
	 * Lanes unlocked at a career level: {@code {}} / {@code {1}} / {@code {1,2}} / {@code {1,2}} / {@code {1,2,3}}.
	 */
	public static Set<Integer> unlockedLanes(int careerLevel) {
		int highest = highestMapTier(careerLevel);
		Set<Integer> lanes = new LinkedHashSet<>();
		for (int lane = 1; lane <= highest; lane++) {
			lanes.add(lane);
		}
		return Set.copyOf(lanes);
	}

	/**
	 * Search radius for a lane: {@code b * 2^(highestMapTier - lane)} when the lane is unlocked.
	 */
	public static Optional<Integer> radius(int careerLevel, int lane, int baseRadius) {
		if (!unlockedLanes(careerLevel).contains(lane)) {
			return Optional.empty();
		}
		int exponent = highestMapTier(careerLevel) - lane;
		return Optional.of(baseRadius * (1 << exponent));
	}

	/**
	 * No remaining destinations → abort and lock. Do not emit a blank map.
	 */
	public static LocateDecision emptySetDecision(Collection<?> remainingDestinations) {
		if (remainingDestinations == null || remainingDestinations.isEmpty()) {
			return LocateDecision.ABORT_AND_LOCK;
		}
		return LocateDecision.LOCATE;
	}
}
