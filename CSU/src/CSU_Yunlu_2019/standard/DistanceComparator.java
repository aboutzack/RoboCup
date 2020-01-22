package CSU_Yunlu_2019.standard;

import java.util.Comparator;

import adf.agent.info.WorldInfo;
import rescuecore2.standard.entities.StandardEntity;

/**
 * A comparator that sorts entities by distance to a reference point.
 */
public class DistanceComparator implements Comparator<StandardEntity> {
	private StandardEntity reference;
	private WorldInfo world;

	/**
	 * Create a DistanceSorter.
	 * 
	 * @param reference
	 *            The reference point to measure distances from.
	 * @param world
	 *            The world model.
	 */
	public DistanceComparator(StandardEntity reference, WorldInfo world) {
		this.reference = reference;
		this.world = world;
	}

	/**
	 * Compares the standard entities according to distance.
	 * 
	 * @param a
	 *            First StandardEntity to compare
	 * @param b
	 *            Second StandardEntity to compare
	 * @return The difference between distances.
	 */

	@Override
	public int compare(StandardEntity a, StandardEntity b) {
		int d1 = world.getDistance(reference, a);
		int d2 = world.getDistance(reference, b);
		return d1 - d2;
	}
}
