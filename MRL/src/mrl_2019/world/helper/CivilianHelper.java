package mrl_2019.world.helper;

import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.helper.info.CivilianInfo;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * User: mrl_2019
 * Date: Dec 3, 2010
 * Time: 2:02:07 PM
 */
public class CivilianHelper implements IHelper {

    protected MrlWorldHelper world;

    protected Map<EntityID, CivilianInfo> civilianInfoMap = new HashMap<>();
    protected Map<Point2D, Set<EntityID>> heardPoints = new HashMap<Point2D, Set<EntityID>>();
    protected ScenarioInfo scenarioInfo;
    protected AgentInfo agentInfo;
    protected WorldInfo worldInfo;

    public CivilianHelper(MrlWorldHelper world, ScenarioInfo scenarioInfo, AgentInfo agentInfo, WorldInfo worldInfo) {
        this.world = world;
        this.scenarioInfo = scenarioInfo;
        this.agentInfo = agentInfo;
        this.worldInfo = worldInfo;
    }

    @Override
    public void init() {
//        myCurrentPosition = world.getSelfLocation();
//        myPreviousPosition = world.getSelfLocation();
    }

    public void setInfoMap(EntityID id) {
        civilianInfoMap.put(id, new CivilianInfo(world));
    }

    @Override
    public void update() {
        CivilianInfo civilianInfo;
        for (EntityID civId : world.getAllCivilians()) {
            Civilian civ = (Civilian) world.getEntity(civId);

            civilianInfo = civilianInfoMap.get(civId);
            if (civilianInfo != null) {
//                civilianInfo.updatePossibleBuilding();
                if (civ != null && civ.isPositionDefined()) {
                    removePossibleBuildings(civ.getID());
                }
            } else {
                setInfoMap(civId);
            }
        }
        Set<EntityID> heardCivilians = world.getHeardCivilians();
        Pair<Integer, Integer> location = worldInfo.getLocation(agentInfo.getID());
        for (EntityID civID : heardCivilians) {
            addHeardCivilianPoint(civID, location.first(), location.second());
        }
        updateHeardCivilianPosition();
//        myPreviousPosition = world.getSelfLocation();
    }

    public void addHeardCivilianPoint(EntityID id, Integer x, Integer y) {
        Point2D point = new Point2D(x, y);
        Set<EntityID> entityIDs = heardPoints.get(point);
        if (entityIDs == null) {
            entityIDs = new HashSet<EntityID>();
            heardPoints.put(point, entityIDs);
        }
        entityIDs.add(id);
    }

    public void updateHeardCivilianPosition() {
//        Map<Point2D, Set<EntityID>> toCheckCivilians = new HashMap<Point2D, Set<EntityID>>();
//        Set<EntityID> civilianSet;
        boolean shouldCheck = false;
        for (Point2D point : heardPoints.keySet()) {
            for (EntityID civilianId : heardPoints.get(point)) {
                StandardEntity entity = world.getEntity(civilianId);
                if (entity != null && ((Civilian) entity).isPositionDefined()) {
                    continue;
                } else {
//                    civilianSet=toCheckCivilians.get(point);
//                    if(civilianSet==null){
//                        civilianSet=new HashSet<EntityID>();
//                    }
//                    civilianSet.add(civilianId);
//                    toCheckCivilians.put(point,civilianSet);
                    shouldCheck = true;
                    break;

                }

            }
            if (shouldCheck) {
                break;
            }
        }

        if (shouldCheck) {
            for (Point2D point : heardPoints.keySet()) {
                ArrayList<EntityID> possibleList = getGuessedBuildings(point);

                for (EntityID civilianId : heardPoints.get(point)) {
                    StandardEntity entity = world.getEntity(civilianId);
                    if (entity != null && ((Civilian) entity).isPositionDefined()) {
                        continue;
                    }
                    CivilianInfo civilianInfo = civilianInfoMap.get(civilianId);

                    civilianInfo.updatePossibleBuilding(possibleList);
                    civilianInfoMap.put(civilianId, civilianInfo);
                }
            }
        }
        heardPoints.clear();
    }

    private ArrayList<EntityID> getGuessedBuildings(Point2D point) {
        ArrayList<EntityID> builds = new ArrayList<EntityID>();
        Collection<StandardEntity> ens = world.getObjectsInRange((int) point.getX(), (int) point.getY(), (int) (world.getVoiceRange() * 1.3));
        for (StandardEntity entity : ens) {
            if (entity instanceof Building) {
                builds.add(entity.getID());
            }
        }
        return builds;
    }

    public Set<EntityID> getPossibleBuildings(EntityID id) {
        return civilianInfoMap.get(id).getPossibleBuildings();
    }

    public void removePossibleBuildings(EntityID id) {
        civilianInfoMap.get(id).clearPossibleBuildings();
    }


//    public static Comparator Civilian_BENEFITComparator = new Comparator() {

    public Map<EntityID, CivilianInfo> getCivilianInfoMap() {
        return civilianInfoMap;
    }

}
