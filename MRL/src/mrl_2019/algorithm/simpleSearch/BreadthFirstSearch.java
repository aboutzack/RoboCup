package mrl_2019.algorithm.simpleSearch;

import javolution.util.FastMap;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

public class BreadthFirstSearch implements SearchAlgorithm {

    @Override
    public List<EntityID> search(EntityID start, EntityID goal, Graph graph, DistanceInterface distanceMatrix) {
        HashSet<EntityID> goals = new HashSet<EntityID>();
        goals.add(goal);
        return search(start, goals, graph, distanceMatrix);
    }

    @Override
    public List<EntityID> search(EntityID start, Collection<EntityID> goals, Graph graph, DistanceInterface distanceMatrix) {
        List<EntityID> open = new LinkedList<EntityID>();
        Map<EntityID, EntityID> ancestors = new FastMap<EntityID, EntityID>();
        open.add(start);
        EntityID next = null;
        boolean found = false;
        ancestors.put(start, start);
        do {
            next = open.remove(0);
            if (isGoal(next, goals)) {
                found = true;
                break;
            }
            Collection<EntityID> neighbours = graph.getNeighbors(next);
            if (neighbours.isEmpty()) {
                continue;
            }
            for (EntityID neighbour : neighbours) {
                if (isGoal(neighbour, goals)) {
                    ancestors.put(neighbour, next);
                    next = neighbour;
                    found = true;
                    break;
                } else {
                    if (!ancestors.containsKey(neighbour)) {
                        open.add(neighbour);
                        ancestors.put(neighbour, next);
                    }
                }
            }
        } while (!found && !open.isEmpty());
        if (!found) {
            // No path
            return null;
        }
        // Walk back from goal to start
        EntityID current = next;
        List<EntityID> path = new LinkedList<EntityID>();
        do {
            path.add(0, current);
            current = ancestors.get(current);
            if (current == null) {
                throw new RuntimeException("Found a node with no ancestor! Something is broken.");
            }
        } while (current != start);
        return path;
    }

    private boolean isGoal(EntityID e, Collection<EntityID> test) {
        return test.contains(e);
    }

}
