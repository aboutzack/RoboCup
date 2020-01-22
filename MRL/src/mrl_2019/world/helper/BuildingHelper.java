package mrl_2019.world.helper;

import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import mrl_2019.MRLConstants;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.entity.Entrance;
import mrl_2019.world.entity.MrlBuilding;
import mrl_2019.world.entity.MrlRoad;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Siavash
 */
public class BuildingHelper implements IHelper {

    private MrlWorldHelper worldHelper;
    protected ScenarioInfo scenarioInfo;
    protected AgentInfo agentInfo;
    protected WorldInfo worldInfo;


    public BuildingHelper(MrlWorldHelper worldHelper, ScenarioInfo scenarioInfo, AgentInfo agentInfo, WorldInfo worldInfo) {
        this.worldHelper = worldHelper;
        this.scenarioInfo = scenarioInfo;
        this.agentInfo = agentInfo;
        this.worldInfo = worldInfo;
    }

    @Override
    public void init() {

    }

    @Override
    public void update() {


        for (MrlBuilding mrlBuilding : worldHelper.getMrlBuildings()) {
            if (worldHelper.getBuildingsSeen().contains(mrlBuilding.getSelfBuilding())) {
                boolean reachable = false;
                if (mrlBuilding.isOneEntranceOpen(worldHelper)) {
                    reachable = true;
                }
                MrlRoad mrlRoad;
                if (reachable) {
                    boolean tempReachable = false;
                    for (Road road : BuildingHelper.getEntranceRoads(worldHelper, mrlBuilding.getSelfBuilding())) {
                        mrlRoad = worldHelper.getMrlRoad(road.getID());
                        if (mrlRoad.isReachable()) {
                            tempReachable = true;
                            break;
                        }
                    }
                    if (!tempReachable) {
                        reachable = false;
                    }
                }
                mrlBuilding.setReachable(reachable);
            } else {
                if (mrlBuilding.getSelfBuilding() instanceof Refuge) {
                    mrlBuilding.resetOldReachable(MRLConstants.REFUGE_PASSABLY_RESET_TIME);
                } else {
                    mrlBuilding.resetOldReachable(MRLConstants.BUILDING_PASSABLY_RESET_TIME);
                }
            }
            mrlBuilding.getCivilianPossibly().clear();

            //the following instruction remove Burnt buildings from visitedBuildings and add it into emptyBuildings list.
            if (isBuildingBurnt(mrlBuilding.getSelfBuilding())) {
                worldHelper.setBuildingVisited(mrlBuilding.getID(), false);
            }
        }



    }

    private boolean isBuildingBurnt(Building building) {
        if (building == null || !building.isFierynessDefined()) {
            return false;
        }
        int fieriness = building.getFieryness();

        return fieriness != 0 && fieriness != 4 && fieriness != 5;
    }

    /**
     * Returns a list of {@link rescuecore2.standard.entities.Road} containing roads that ends to {@code building}
     *
     * @param world
     * @param building building to find entrance roads
     * @return List of entrance roads
     * @author Siavash
     */
    public static List<Road> getEntranceRoads(MrlWorldHelper world, Building building) {
        ArrayList<Road> entranceRoads = new ArrayList<Road>();
        MrlBuilding mrlBuilding = world.getMrlBuilding(building.getID());
        for(Entrance entrance : mrlBuilding.getEntrances()){
            entranceRoads.add(entrance.getNeighbour());
        }
        return entranceRoads;


        // throw new UnsupportedOperationException();
    }

    public static List<Area> getEntranceAreas(WorldInfo world, Building building) {
        ArrayList<Area> entranceAreas = new ArrayList<Area>();
        if (building != null && building.getNeighbours() != null) {
            for (EntityID entityID : building.getNeighbours()) {
                Area area = (Area) world.getEntity(entityID);
                entranceAreas.add(area);
            }
        }
        return entranceAreas;
    }

    /**
     * check is this building have fieriness 1,2,3,6,7,8 or not!
     * this building probably have no alive human!
     *
     * @param building building that want know have this condition or not
     * @return answer
     */
    public static boolean hasPossibleAliveHuman(Building building) {
        return (building.isFierynessDefined() && !(building.getFieryness() == 0 || building.getFieryness() == 4 || building.getFieryness() == 5));
    }


}
