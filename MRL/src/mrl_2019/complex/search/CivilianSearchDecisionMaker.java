package mrl_2019.complex.search;


import adf.agent.info.AgentInfo;
import adf.agent.info.WorldInfo;
import mrl_2019.MRLConstants;
import mrl_2019.util.Util;
import mrl_2019.viewer.MrlPersonalData;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.entity.MrlBuilding;
import mrl_2019.world.entity.MrlRoad;
import mrl_2019.world.entity.Path;
import mrl_2019.world.helper.CivilianHelper;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * @author Mahdi
 */
public class CivilianSearchDecisionMaker {
    private Set<EntityID> shouldDiscoverBuildings;
    private Set<EntityID> shouldFindCivilians;
    private Set<EntityID> unreachableCivilians;
    private MrlWorldHelper world;
    private AgentInfo agentInfo;
    private WorldInfo worldInfo;
    private Map<EntityID, Integer> notVisitable;
    private Set<EntityID> validBuildings;
    private Set<StandardEntity> sBulding;


    public CivilianSearchDecisionMaker(MrlWorldHelper world, AgentInfo ai, WorldInfo wi) {
        this.world = world;
        agentInfo = ai;
        worldInfo = wi;
        notVisitable = new HashMap<>();
    }


    public void initialize() {
        shouldFindCivilians = new HashSet<>();
        shouldDiscoverBuildings = new HashSet<>();
        unreachableCivilians = new HashSet<>();
        validBuildings = new HashSet<>();
        sBulding = new HashSet<>();
    }

    public void update() {

//        if (searchInPartition) {
//            validBuildings.clear();
//            validPaths.clear();
//            Partition myPartition = world.getPartitionManager().findHumanPartition(world.getSelfHuman());
//            if (myPartition == null) {
//                validBuildings.addAll(world.getBuildingIDs());
//                validPaths.addAll(world.getPaths());
//            } else {
//                Set<Partition> humanPartitionsMap = world.getPartitionManager().findHumanPartitionsMap(world.getSelfHuman());
//
//                for (Partition partition : humanPartitionsMap) {
//                    validBuildings.addAll(partition.getBuildingIDs());
//                    validPaths.addAll(partition.getPaths());
//                }
//
//
//            }
//            shouldDiscoverBuildings.retainAll(validBuildings);
//        } else {
        validBuildings.addAll(world.getBuildingIDs());
//        }


        //shouldntDiscover();
        removeZeroBrokennessBuildings();
        removeBurningBuildings();
        removeVisitedBuildings();

        setShouldFindCivilians();
        shouldFindCivilians.removeAll(unreachableCivilians);
        setShouldDiscoverBuildings();

//        removeZeroBrokennessBuildings(); aval inja farakhani mishod
//        removeBurningBuildings();
//        removeVisitedBuildings();
        if (!(agentInfo.me() instanceof PoliceForce)) {
            removeUnreachableBuildings();
        }
        updateCivilianPossibleValues();
        //        MrlPersonalData.VIEWER_DATA.setCivilianData(agentInfo.getID(), shouldDiscoverBuildings, civilianInProgress, buildingInProgress);

    }

    private void removeUnreachableBuildings() {
        List<EntityID> toRemove = new ArrayList<>();
        MrlBuilding mrlBuilding;
        for (EntityID bID : shouldDiscoverBuildings) {
            mrlBuilding = world.getMrlBuilding(bID);
            if (!mrlBuilding.isVisitable()) {
                toRemove.add(bID);
            }
        }
        for (EntityID bID : notVisitable.keySet()) {
            if (notVisitable.get(bID) < MRLConstants.BUILDING_PASSABLY_RESET_TIME) {
                toRemove.add(bID);
            }
        }
        shouldDiscoverBuildings.removeAll(toRemove);

        MrlPersonalData.VIEWER_DATA.setUnreachableBuildings(agentInfo.getID(), new HashSet<>(toRemove));
    }

    private void removeVisitedBuildings() {
        shouldDiscoverBuildings.removeAll(world.getVisitedBuildings());
    }

    //    @Override
//    public Path getNextPath() {
//        if (shouldDiscoverPaths.isEmpty()) {
//            return null;
//        }
//        pathInProgress = shouldDiscoverPaths.remove(0);
//        return pathInProgress;
//    }
//
    Area getNextArea() {
        EntityID greatestValue = null;
        double maxValue = 0;
        MrlBuilding mrlBuilding;
        for (EntityID buildingID : shouldDiscoverBuildings) {
            mrlBuilding = world.getMrlBuilding(buildingID);
            double value = mrlBuilding.getCivilianPossibleValue();
            if (value > maxValue) {
                maxValue = value;
                greatestValue = buildingID;
            }
        }

        if (greatestValue == null) {
            return null;
        }

        return world.getEntity(greatestValue, Area.class);
    }


    Area getBetterTarget(Area presentTarget) {
        Area bestArea = getNextArea();
        MrlBuilding presentBuildingTarget = world.getMrlBuilding(presentTarget.getID());
        if (bestArea instanceof Building) {
            MrlBuilding bestBuilding = world.getMrlBuilding(bestArea.getID());
            if (bestBuilding.getCivilianPossibleValue() >= presentBuildingTarget.getCivilianPossibleValue() * 2) {
                return bestArea;
            }
        }
        return null;
    }

    /**
     * set civilian possible value every cycle.
     * number of civilian whom voice of them heard around / time to arrive
     * finally *2 value for buildings that in a same path with current agent.
     */
    private void updateCivilianPossibleValues() {
        MrlBuilding mrlBuilding;
        for (EntityID bID : shouldDiscoverBuildings) {
            mrlBuilding = world.getMrlBuilding(bID);
            double civilianPossibleValue = mrlBuilding.getCivilianPossibly().size();
            if (civilianPossibleValue != 0) {
                StandardEntity position = world.getSelfPosition();
                double distance = Util.distance(worldInfo.getLocation(agentInfo.getID()), worldInfo.getLocation(mrlBuilding.getSelfBuilding()));
                double timeToArrive = distance / MRLConstants.MEAN_VELOCITY_OF_MOVING;
                if (timeToArrive > 0) {
                    civilianPossibleValue /= timeToArrive;
                    //set double value for buildings that inside current path!
                    if (position instanceof Road) {
                        MrlRoad mrlRoad = world.getMrlRoad(position.getID());
                        for (Path path : mrlRoad.getPaths()) {
                            if (path.getBuildings().contains(mrlBuilding.getSelfBuilding())) {
                                civilianPossibleValue *= 2;
                            }
                        }
                    }
                } else {
                    civilianPossibleValue = 0;
                }
            }
            mrlBuilding.setCivilianPossibleValue(civilianPossibleValue);
        }
    }


    private void removeBurningBuildings() {
        Building building;
        Set<EntityID> toRemove = new HashSet<>();
        for (EntityID buildingID : shouldDiscoverBuildings) {
            building = (Building) worldInfo.getEntity(buildingID);
            if (building.isFierynessDefined() && building.getFieryness() > 0 && building.getFieryness() != 4) {
                toRemove.add(buildingID);
            }
        }
        if (toRemove.size() > 0) {
            System.out.print("");
        }
        shouldDiscoverBuildings.removeAll(toRemove);
    }

    private void setShouldFindCivilians() {
        shouldFindCivilians.clear();
        for (EntityID civId : world.getAllCivilians()) {
            StandardEntity civEntity = world.getEntity(civId);

            Civilian civilian = (Civilian) civEntity;
            if (civilian == null || !civilian.isPositionDefined()) {
                shouldFindCivilians.add(civId);
            }
        }
    }

    /**
     * Fill buildings that have civilian possibly to discover them!
     * It will get information of possible buildings of civilians whom heard voice of them from CivilianHelper
     */
    private void setShouldDiscoverBuildings() {
        CivilianHelper civilianHelper = world.getHelper(CivilianHelper.class);
        Set<EntityID> possibleBuildings;
        MrlBuilding mrlBuilding;
        shouldDiscoverBuildings.clear();
        for (EntityID civId : shouldFindCivilians) {
            possibleBuildings = new HashSet<>(civilianHelper.getPossibleBuildings(civId));
            for (EntityID possibleBuildingID : possibleBuildings) {
                mrlBuilding = world.getMrlBuilding(possibleBuildingID);
//                if(mrlBuilding.isVisited()){
//                    continue;
//                }
                mrlBuilding.addCivilianPossibly(civId);
                shouldDiscoverBuildings.add(possibleBuildingID);
            }
        }
    }

    private void removeZeroBrokennessBuildings() {
        Building building;
        List<EntityID> toRemove = new ArrayList<>();
        for (EntityID buildingID : shouldDiscoverBuildings) {
            building = (Building) worldInfo.getEntity(buildingID);
            if (building.isBrokennessDefined() && building.getBrokenness() == 0) {
                toRemove.add(buildingID);
            }
        }
        shouldDiscoverBuildings.removeAll(toRemove);
    }

//    private void shouldntDiscover(){
//        shouldDiscoverBuildings.clear();
//        if (sBulding !=null && sBulding.size()>0){
//            for (StandardEntity building :sBulding){
//                if (building instanceof Refuge
//                && building instanceof AmbulanceCentre
//                && building instanceof PoliceOffice
//                && building instanceof FireStation){
//                    shouldDiscoverBuildings.remove(building);
//
//                }
//            }
//        }
//    }
}


