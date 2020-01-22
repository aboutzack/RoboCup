package mrl_2019.world.entity;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;

import java.util.List;

/**
 * User: mrl_2019
 * Edited by Mostafa Shabani.
 * Date: May 20, 2010
 * Time: 1:19:23 AM
 */
public class Entrance {
    Road neighbour;
    List<Building> ownerBuildings;
    EntityID id;

    public Entrance(Road neighbour, List<Building> ownerBuildings) {
        this.neighbour = neighbour;
        this.ownerBuildings = ownerBuildings;
        this.id = neighbour.getID();
    }

    public Road getNeighbour() {
        return neighbour;
    }

    public List<Building> getBuildings() {
        return ownerBuildings;
    }

//    public boolean isSeenAndBlocked(RoadHelper roadHelper) {
//        return !roadHelper.isPassable(neighbour.getID()) && roadHelper.isSeen(neighbour.getID());
//    }

//    public boolean isBlockedOrNotSeen(RoadHelper roadHelper) {
//        return roadHelper.isPassable(neighbour.getID());
//    }

//    public boolean isSeen(RoadHelper roadHelper) {
//        return roadHelper.isSeen(neighbour.getID());
//    }

    public EntityID getID() {
        return neighbour.getID();
    }

    @Override
    public String toString() {
        return "Entrance[bd:" + ownerBuildings + " ,id:" + neighbour + "]";
    }
}
