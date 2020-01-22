package mrl_2019.algorithm.simpleSearch;

import rescuecore2.misc.collections.LazyMap;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

public class Graph {
    private Map<EntityID, Set<EntityID>> graph;
    private int accessCount;

    public Graph(StandardWorldModel world) {
        resetAccessCount();
        graph = new LazyMap<EntityID, Set<EntityID>>() {
            @Override
            public Set<EntityID> createValue() {
                return new HashSet<EntityID>();
            }
        };
        for (Entity next : world) {
            if (next instanceof Area) {
                Collection<EntityID> areaNeighbours = ((Area) next).getNeighbours();
                graph.get(next.getID()).addAll(areaNeighbours);
            }
        }
    }

    public Graph(List<Area> areas) {
        resetAccessCount();
        graph = new LazyMap<EntityID, Set<EntityID>>() {
            @Override
            public Set<EntityID> createValue() {
                return new HashSet<EntityID>();
            }
        };
        for (Entity next : areas) {
            if (next instanceof Area) {
                Collection<EntityID> areaNeighbours = ((Area) next).getNeighbours();
                graph.get(next.getID()).addAll(areaNeighbours);
            }
        }
    }

    /**
     * Retrieves all neighbors of a specific node in the graph.
     *
     * @param id the entity id of the node.
     * @return set containing the entity ids of all neighbor nodes.
     */
    public Set<EntityID> getNeighbors(EntityID id) {
        accessCount++;
        return graph.get(id);
    }

    public int getAccessCount() {
        return accessCount;
    }

    public void resetAccessCount() {
        this.accessCount = 0;
    }


}