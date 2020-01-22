package mrl_2019.algorithm.simpleSearch;

import rescuecore2.worldmodel.EntityID;

import java.util.Collection;
import java.util.List;

public interface SearchAlgorithm {
    /**
     * Do a search from one location to the closest of a set of goals.
     *
     * @param start          The location we start at.
     * @param goal           The goals we want to reach.
     * @param graph          a connectivity graph of all the places in the world
     * @param distanceMatrix A matrix containing the pre-computed distances between each
     *                       two entities in the world.
     * @return The path from start to one of the goals, or null if no path can be found.
     */
    public List<EntityID> search(EntityID start, EntityID goal,
                                 Graph graph, DistanceInterface distanceMatrix);

    /**
     * Do a search from one location to the closest of a set of goals.
     *
     * @param start          The location we start at.
     * @param goals          The set of possible goals.
     * @param graph          a connectivity graph of all the places in the world
     * @param distanceMatrix A matrix containing the pre-computed distances between each
     *                       two entities in the world.
     * @return The path from start to one of the goals, or null if no path can be found.
     */
    public List<EntityID> search(EntityID start, Collection<EntityID> goals,
                                 Graph graph, DistanceInterface distanceMatrix);
}
