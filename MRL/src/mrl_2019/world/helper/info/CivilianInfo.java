package mrl_2019.world.helper.info;

import javolution.util.FastSet;
import mrl_2019.world.MrlWorldHelper;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * User: mrl_2019
 * Date: Dec 3, 2010
 * Time: 2:04:46 PM
 */
public class CivilianInfo {

    protected MrlWorldHelper world;
    private final Set<EntityID> possibleBuildings = new FastSet<EntityID>();
    private List<Pair> heardPositions = new ArrayList<Pair>();
    private boolean isFound = false;
    int voiceRange;

    public CivilianInfo(MrlWorldHelper world) {
        this.world = world;
        voiceRange = world.getVoiceRange();
    }

    public void updatePossibleBuilding() {
        if (isFound) {
            return;
        }

        Set<EntityID> possibleList = new HashSet<EntityID>();
        for (Pair pair : heardPositions) {
            if (possibleBuildings.isEmpty()) {
                possibleBuildings.addAll(getGuessedBuildings(pair));
            } else {
                ArrayList<EntityID> toRemove = new ArrayList<EntityID>();
                possibleList.addAll(getGuessedBuildings(pair));
                for (EntityID building : possibleBuildings) {
                    if (!possibleList.contains(building) && world.getVisitedBuildings().contains(building)) {
                        toRemove.add(building);
                    }
                }
                possibleBuildings.removeAll(toRemove);
            }
        }
        heardPositions.clear();
    }

    public void updatePossibleBuilding(ArrayList<EntityID> possibleList) {
        if (isFound) {
            return;
        }
        if (possibleBuildings.isEmpty()) {
            possibleBuildings.addAll(possibleList);
        } else {
            ArrayList<EntityID> toRemove = new ArrayList<EntityID>();
            for (EntityID building : possibleBuildings) {
                if (!possibleList.contains(building) && world.getVisitedBuildings().contains(building)) {
                    toRemove.add(building);
                }
            }
            possibleBuildings.removeAll(toRemove);
        }
        heardPositions.clear();
    }

    private ArrayList<EntityID> getGuessedBuildings(Pair pair) {
        ArrayList<EntityID> builds = new ArrayList<EntityID>();
        Collection<StandardEntity> ens = world.getObjectsInRange((Integer) pair.first(), (Integer) pair.second(), (int) (voiceRange * 1.3));
        for (StandardEntity entity : ens) {
            if (entity instanceof Building) {
                builds.add(entity.getID());
            }
        }
        return builds;
    }

    public List<Pair> getHeardPositions() {
        return heardPositions;
    }

    public Set<EntityID> getPossibleBuildings() {
        return possibleBuildings;
    }


    public void clearPossibleBuildings() {
        isFound = true;
        possibleBuildings.clear();
    }
}