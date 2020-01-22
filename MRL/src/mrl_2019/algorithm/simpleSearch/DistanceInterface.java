package mrl_2019.algorithm.simpleSearch;

import adf.agent.info.WorldInfo;
import rescuecore2.worldmodel.EntityID;

public class DistanceInterface {
    private WorldInfo worldInfo;

    public DistanceInterface(WorldInfo worldInfo) {
        this.worldInfo = worldInfo;
    }

    /**
     * Computes the distance between two entities in the simulation world.
     *
     * @param id1 id of the first entity.
     * @param id2 id of the second entity.
     * @return euclidean distance in mm(?) or -1 if one of the entities does not exist.
     */
    public int getDistance(EntityID id1, EntityID id2) {
        return worldInfo.getDistance(id1, id2);
    }
}

