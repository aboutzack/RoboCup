package mrl_2019.extaction;


import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.action.fire.ActionExtinguish;
import adf.agent.action.fire.ActionRefill;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.information.MessageFireBrigade;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.PathPlanning;
import com.mrl.debugger.remote.VDClient;
import mrl_2019.MRLConstants;
import mrl_2019.complex.firebrigade.FireBrigadeUtilities;
import mrl_2019.complex.firebrigade.MrlFireBrigadeWorld;
import mrl_2019.complex.firebrigade.PreExtinguish;
import mrl_2019.util.Util;
import mrl_2019.viewer.MrlPersonalData;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.entity.MrlBuilding;
import mrl_2019.world.entity.MrlRoad;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.io.Serializable;
import java.util.*;

import static rescuecore2.misc.Handy.objectsToIDs;
import static rescuecore2.standard.entities.StandardEntityURN.HYDRANT;
import static rescuecore2.standard.entities.StandardEntityURN.REFUGE;

public class ActionFireFighting extends ExtAction {
    private PathPlanning pathPlanning;

    private int maxExtinguishDistance;
    private int maxExtinguishPower;
    private int thresholdRest;
    private int kernelTime;
    private int refillCompleted;
    private int refillRequest;
    private boolean refillFlag;
    private ExtAction actionExtMove;
    private static final int EXTINGUISH_DISTANCE_THRESHOLD = 5000;
    //    private MrlFireClustering clustering;
    private MrlFireBrigadeWorld worldHelper;
    private MessageManager messageManager;
    private EntityID target;
    private Action lastAction = null;

//    private Map<StandardEntity, Set<EntityID>> observableAreas;
//    private Map<StandardEntity, Set<EntityID>> visibleFromAreas;

    public ActionFireFighting(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, DevelopData developData) {
        super(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
        this.maxExtinguishDistance = scenarioInfo.getFireExtinguishMaxDistance();
        this.maxExtinguishPower = scenarioInfo.getFireExtinguishMaxSum();
        this.thresholdRest = developData.getInteger("ActionFireFighting.rest", 100);
        int maxWater = scenarioInfo.getFireTankMaximum();
        this.refillCompleted = (maxWater / 10) * developData.getInteger("ActionFireFighting.refill.completed", 10);
        this.refillRequest = this.maxExtinguishPower * developData.getInteger("ActionFireFighting.refill.request", 1);
        this.refillFlag = false;

        this.target = null;
//        observableAreas = new HashMap<>();
//        visibleFromAreas = new HashMap<>();
        switch (scenarioInfo.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("ActionFireFighting.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
//                this.clustering = moduleManager.getModule("SampleBuildingDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                this.actionExtMove = moduleManager.getExtAction("TacticsFireBrigade.ActionExtMove", "adf.sample.extaction.ActionExtMove");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("ActionFireFighting.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
//                this.clustering = moduleManager.getModule("SampleBuildingDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                this.actionExtMove = moduleManager.getExtAction("TacticsFireBrigade.ActionExtMove", "adf.sample.extaction.ActionExtMove");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("ActionFireFighting.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
//                this.clustering = moduleManager.getModule("SampleBuildingDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                this.actionExtMove = moduleManager.getExtAction("TacticsFireBrigade.ActionExtMove", "adf.sample.extaction.ActionExtMove");
                break;
        }

        this.worldHelper = (MrlFireBrigadeWorld) MrlWorldHelper.load(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);

    }

    @Override
    public ExtAction precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        this.pathPlanning.precompute(precomputeData);
//        this.clustering.precompute(precomputeData);
        this.actionExtMove.precompute(precomputeData);

//        fillProperties();
//        fillVisibleSets();
//        ProcessAreaVisibility.process((MrlFireBrigadeWorld) worldHelper, worldInfo, scenarioInfo, true);

        try {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        } catch (NoSuchConfigOptionException e) {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override
    public ExtAction resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }

        fillProperties();
//        fillVisibleSets();
//        ProcessAreaVisibility.process((MrlFireBrigadeWorld) worldHelper, worldInfo, scenarioInfo, false);


        this.worldHelper.resume(precomputeData);
        this.pathPlanning.resume(precomputeData);
//        this.clustering.resume(precomputeData);
        this.actionExtMove.resume(precomputeData);

        try {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        } catch (NoSuchConfigOptionException e) {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override
    public ExtAction preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }

//        fillVisibleSets();
//        ProcessAreaVisibility.process((MrlFireBrigadeWorld) worldHelper, worldInfo, scenarioInfo, false);

        this.worldHelper.preparate();
        this.pathPlanning.preparate();
//        this.clustering.preparate();
        this.actionExtMove.preparate();
        fillProperties();

        try {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        } catch (NoSuchConfigOptionException e) {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override
    public ExtAction updateInfo(MessageManager messageManager) {
        this.messageManager = messageManager;
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.worldHelper.updateInfo(messageManager);
        this.pathPlanning.updateInfo(messageManager);
        this.actionExtMove.updateInfo(messageManager);
//        this.clustering.updateInfo(messageManager);

        return this;
    }

    @Override
    public ExtAction setTarget(EntityID target) {
        this.target = null;
        if (target != null) {
            StandardEntity entity = this.worldInfo.getEntity(target);
            if (entity instanceof Building) {
                this.target = target;
            }
        }
        return this;
    }

    @Override
    public ExtAction calc() {
        this.result = null;
        FireBrigade agent = (FireBrigade) this.agentInfo.me();

        Action action = moveToRefugeIfDamagedOrTankIsEmpty();
        if (action != null) {
            this.result = action;
            return this;
        }

        if (this.target == null) {

            return this;
        }
        this.result = this.calcExtinguish(agent, this.pathPlanning, this.target);
        return this;
    }

    private void fillProperties() {
        for (MrlBuilding mrlBuilding : worldHelper.getMrlBuildings()) {
            Set<EntityID> extinguishableFromAreas = FireBrigadeUtilities.findAreaIDsInExtinguishRange(worldInfo, scenarioInfo, mrlBuilding.getID());
            List<MrlBuilding> buildingsInExtinguishRange = new ArrayList<MrlBuilding>();
            for (EntityID next : extinguishableFromAreas) {
                if (worldInfo.getEntity(next) instanceof Building) {
                    buildingsInExtinguishRange.add(worldHelper.getMrlBuilding(next));
                }
            }
            mrlBuilding.setExtinguishableFromAreas(extinguishableFromAreas);
//            extinguishableFromAreasMap.put(mrlBuilding.getID(), extinguishableFromAreas);
            mrlBuilding.setBuildingsInExtinguishRange(buildingsInExtinguishRange);
//            buildingsInExtinguishRangeMap.put(mrlBuilding.getID(), buildingsInExtinguishRange);
        }
        for (MrlRoad mrlRoad : worldHelper.getMrlRoads()) {
            List<MrlBuilding> buildingsInExtinguishRange = FireBrigadeUtilities.findBuildingsInExtinguishRangeOf(worldHelper, worldInfo, scenarioInfo, mrlRoad.getID());
            mrlRoad.setBuildingsInExtinguishRange(buildingsInExtinguishRange);
//            buildingsInExtinguishRangeMap.put(mrlRoad.getID(), buildingsInExtinguishRange);
        }

//        MrlPersonalData.VIEWER_DATA.setExtinguishData(extinguishableFromAreasMap, buildingsInExtinguishRangeMap);
    }


    public Action calcExtinguish(FireBrigade agent, PathPlanning pathPlanning, EntityID target) {
        EntityID agentPosition = agent.getPosition();
        StandardEntity positionEntity = Objects.requireNonNull(this.worldInfo.getPosition(agent));
//        if (StandardEntityURN.REFUGE == positionEntity.getStandardURN()) {
//            Action action = this.getMoveAction(pathPlanning, agentPosition, target);
//            if (action != null) {
//                return action;
//            }
//        }

        List<StandardEntity> neighbourBuilding = new ArrayList<>();
        StandardEntity entity = this.worldInfo.getEntity(target);
        if (entity instanceof Building) {
            if (this.worldInfo.getDistance(positionEntity, entity) < this.maxExtinguishDistance) {
                neighbourBuilding.add(entity);
            }
        }

        if (neighbourBuilding.size() > 0) {
            neighbourBuilding.sort(new DistanceSorter(this.worldInfo, agent));

            int waterPower;
            if (worldHelper != null) {
                MrlBuilding targetMrlBuilding = worldHelper.getMrlBuilding(target);
                waterPower = FireBrigadeUtilities.calculateWaterPower(agentInfo.getWater(), scenarioInfo.getFireExtinguishMaxSum(), targetMrlBuilding);
                targetMrlBuilding.increaseWaterQuantity(waterPower);
            } else {
                waterPower = FireBrigadeUtilities.calculateWaterPowerNotEstimated(agentInfo.getWater(), scenarioInfo.getFireExtinguishMaxSum(), (Building) worldInfo.getEntity(target));
            }

//            if (messageManager != null) {
//                sendWaterMessage(target, waterPower);
//            }

            return new ActionExtinguish(neighbourBuilding.get(0).getID(), waterPower);
        }

        StandardEntity bestLocation = chooseBestLocationToStandForExtinguishingFire(target);
        if (bestLocation == null) {
            return null;
        }
        List<EntityID> movePlan = pathPlanning.setFrom(agentPosition).setDestination(bestLocation.getID()).calc().getResult();
        if (movePlan == null || movePlan.isEmpty()) {
            Collection<StandardEntity> inRange = worldInfo.getObjectsInRange(bestLocation, maxExtinguishDistance / 3);
            int counter = 0;
            for (StandardEntity e : inRange) {
                if (e instanceof Area && worldInfo.getDistance(target, e.getID()) < maxExtinguishDistance) {

                    movePlan = pathPlanning.setFrom(agentPosition).setDestination(e.getID()).calc().getResult();
                    counter++;
                    if (movePlan != null && !movePlan.isEmpty()) {
                        return getMoveAction(movePlan);
                    }
                    if (counter > 3) {
                        lastTryToExtinguish();
                    }
                }
            }
        }
        return this.getMoveAction(pathPlanning, agentPosition, bestLocation.getID());
    }

    private void sendWaterMessage(EntityID target, int waterPower) {
        if (waterPower == scenarioInfo.getFireExtinguishMaxSum()) {
            messageManager.addMessage(new MessageFireBrigade(true, (FireBrigade) agentInfo.me(), 23, target));
        } else {
            messageManager.addMessage(new MessageFireBrigade(true, (FireBrigade) agentInfo.me(), 24, target));
        }
    }


    private Action lastTryToExtinguish() {

        if (MrlPersonalData.DEBUG_MODE) {
//            System.out.println(agentInfo.me() + "in lastTryToExtinguish");
        }
        Set<StandardEntity> buildingsInMyExtinguishRange = getBuildingsInMyExtinguishRange();
        List<StandardEntity> fieryBuildingsInMyExtinguishRange = new ArrayList<>();
        for (StandardEntity entity : buildingsInMyExtinguishRange) {
            Building building = (Building) entity;
            if (building.isOnFire()) {
                fieryBuildingsInMyExtinguishRange.add(entity);
            }
        }
        StandardEntity tempTarget = findNearest(fieryBuildingsInMyExtinguishRange, agentInfo.getPositionArea());
        if (tempTarget != null) {
//            int waterPower = FireBrigadeUtilities.calculateWaterPower(world, tempTarget);
//            ((MrlFireBrigade) platoonAgent).getFireBrigadeMessageHelper().sendWaterMessage(tempTarget.getID(), waterPower);
//            tempTarget.increaseWaterQuantity(waterPower);
//            platoonAgent.sendExtinguishAct(world.getTime(), tempTarget.getID(), waterPower);
            if (MrlPersonalData.DEBUG_MODE) {
                System.out.println(agentInfo.me() + "target in lastTryToExtinguish " + tempTarget.getID());
            }


            int waterPower;
            if (worldHelper != null) {
                MrlBuilding targetMrlBuilding = worldHelper.getMrlBuilding(tempTarget.getID());
                waterPower = FireBrigadeUtilities.calculateWaterPower(agentInfo.getWater(), scenarioInfo.getFireExtinguishMaxSum(), targetMrlBuilding);
                targetMrlBuilding.increaseWaterQuantity(waterPower);
            } else {
                waterPower = FireBrigadeUtilities.calculateWaterPowerNotEstimated(agentInfo.getWater(), scenarioInfo.getFireExtinguishMaxSum(), (Building) worldInfo.getEntity(target));
            }


//            if (messageManager != null) {
//                sendWaterMessage(target, waterPower);
//            }

            return new ActionExtinguish(tempTarget.getID(), waterPower);
        }
        return null;
    }

    public Set<StandardEntity> getBuildingsInMyExtinguishRange() {
        Set<StandardEntity> result = new HashSet<>();
        int maxExtinguishDistance = this.maxExtinguishDistance - EXTINGUISH_DISTANCE_THRESHOLD;
        for (StandardEntity next : worldInfo.getObjectsInRange(agentInfo.getID(), (int) (maxExtinguishDistance * 1.5))) {
            if (next instanceof Building) {
                if (worldInfo.getDistance(next.getID(), agentInfo.getID()) < maxExtinguishDistance) {
                    result.add(next);
                }
            }
        }
        return result;
    }

    public StandardEntity findNearest(List<StandardEntity> buildings, StandardEntity basePosition) {
        StandardEntity result = null;
        double minDistance = Double.POSITIVE_INFINITY;
        for (StandardEntity next : buildings) {
            double dist = worldInfo.getDistance(next.getID(), basePosition.getID());
            if (dist < minDistance) {
                result = next;
                minDistance = dist;
            }
        }
        return result;

    }

//    private List<EntityID> getForbiddenLocations(MrlFireBrigadeWorld world, FireBrigadeTarget target) {
//        List<EntityID> forbiddenLocations = new ArrayList<EntityID>();
//
//        if (target.getCluster() != null) {
//            forbiddenLocations.addAll(objectsToIDs(target.getCluster().getAllEntities()));
//        }
//        forbiddenLocations.addAll(world.getBurningBuildings());
//        //if i am nth smallest FB, i should move over there
//        //to force FBs to create a ring around fire
//        int n = 3;
//        if (world.isMapMedium()) n = 10;
//        if (world.isMapHuge()) n = 15;
//        for (FireBrigade next : world.getFireBrigadeList()) {
//            if (world.getSelf().getID().getValue() < next.getID().getValue() && world.getDistance(world.getSelf().getID(), next.getID()) < world.getViewDistance() && --n <= 0) {
//                MrlRoad roadOfNearFB = world.getMrlRoad(next.getPosition());
//                MrlBuilding buildingOfNearFB = world.getMrlBuilding(next.getPosition());
//                if (roadOfNearFB != null) {
//                    forbiddenLocations.addAll(roadOfNearFB.getObservableAreas());
//                }
//                if (buildingOfNearFB != null) {
//                    forbiddenLocations.addAll(buildingOfNearFB.getObservableAreas());
//                }
//            }
//        }
//
//        MrlPersonalData.VIEWER_DATA.setForbiddenLocations(world.getSelf().getID(), forbiddenLocations);
//        return forbiddenLocations;
//    }

    private StandardEntity chooseBestLocationToStandForExtinguishingFire(EntityID target) {
        double minDistance = Integer.MAX_VALUE;
        Set<EntityID> forbiddenLocationIDs = getForbiddenLocations();
        List<StandardEntity> possibleAreas = new ArrayList<StandardEntity>();
        StandardEntity targetToExtinguish = null;
        double dis;
        Set<EntityID> extinguishableFromAreas = worldHelper.getMrlBuilding(target).getExtinguishableFromAreas();
//        visibleFromAreas.get(worldInfo.getEntity(target));
//        List<EntityID> extinguishableFromAreas = worldHelper.getMrlBuilding(target).getExtinguishableFromAreas();
//        Collection<EntityID> extinguishableFromAreas = worldInfo.getObjectIDsInRange(target, scenarioInfo.getFireExtinguishMaxDistance());
        for (EntityID next : extinguishableFromAreas) {
            StandardEntity entity = worldInfo.getEntity(next);
            possibleAreas.add(entity);
        }
//        MrlPersonalData.VIEWER_DATA.setBestPlaceToStand(agentInfo.getID(), worldInfo.getEntities(extinguishableFromAreas));

        List<StandardEntity> forbiddenLocations = worldHelper.getEntities(forbiddenLocationIDs);
        possibleAreas.removeAll(forbiddenLocations);
        if (possibleAreas.isEmpty()) {
            for (EntityID next : extinguishableFromAreas) {
                possibleAreas.add(worldInfo.getEntity(next));
            }
        }


        //fist search for a road to stand there
        for (StandardEntity entity : possibleAreas) {
            if (entity instanceof Road) {
                dis = worldInfo.getDistance(agentInfo.me(), entity);
                if (dis < minDistance) {
                    minDistance = dis;
                    targetToExtinguish = entity;
                }
            }
        }
        //if there is no road to stand, search for a no fiery building to go
        if (targetToExtinguish == null) {
            for (StandardEntity entity : possibleAreas) {
                if (entity instanceof Building) {
                    Building building = (Building) entity;
                    dis = worldInfo.getDistance(agentInfo.me(), entity);
                    if (dis < minDistance && (!building.isFierynessDefined() || (building.isFierynessDefined() && (building.getFieryness() >= 4 || building.getFieryness() <= 1)))) {
                        minDistance = dis;
                        targetToExtinguish = entity;
                    }
                }
            }
        }
        return targetToExtinguish;
    }


    private Set<EntityID> getForbiddenLocations() {
        Set<EntityID> forbiddenLocations = new HashSet<>();

        Collection<StandardEntity> clusterEntities = this.worldHelper.getFireClustering().getClusterEntities(this.worldHelper.getFireClustering().getMyClusterIndex());

        if (clusterEntities != null) {
            forbiddenLocations.addAll(objectsToIDs(clusterEntities));
        }

        //TODO: @MRL change this line to add cluster on-fire buildings
        forbiddenLocations.addAll(worldInfo.getFireBuildingIDs());
        //if i am nth smallest FB, i should move over there
        //to force FBs to create a ring around fire
        int n = 3;
        if (worldHelper.isMapMedium()) n = 10;
        if (worldHelper.isMapHuge()) n = 15;
        for (StandardEntity next : worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE)) {
            if (agentInfo.getID().getValue() < next.getID().getValue() && worldInfo.getDistance(agentInfo.getID(), next.getID()) < scenarioInfo.getPerceptionLosMaxDistance() && --n <= 0) {
                StandardEntity position = worldInfo.getPosition(next.getID());
                if (position != null) {
                    MrlBuilding buildingOfNearFB = worldHelper.getMrlBuilding(position.getID());
                    MrlRoad roadOfNearFB = worldHelper.getMrlRoad(position.getID());
                    if (roadOfNearFB != null) {
                        forbiddenLocations.addAll(roadOfNearFB.getObservableAreas());
                    }
                    if (buildingOfNearFB != null) {
                        forbiddenLocations.addAll(buildingOfNearFB.getObservableAreas());
                    }
                }
            }
        }


        if (MrlPersonalData.DEBUG_MODE) {
            List<Integer> elementList = Util.fetchIdValueFormElementIds(forbiddenLocations);
            VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlForbiddenAreaLayer", (Serializable) elementList);
        }

//        MrlPersonalData.VIEWER_DATA.setForbiddenLocations(world.getSelf().getID(), forbiddenLocations);
        return forbiddenLocations;
    }


    private Action getMoveAction(PathPlanning pathPlanning, EntityID from, EntityID target) {
        pathPlanning.setFrom(from);
        pathPlanning.setDestination(target);
        List<EntityID> path = pathPlanning.calc().getResult();
        return getMoveAction(path);
    }

    private Action getMoveAction(List<EntityID> path) {
        if (path != null && path.size() > 0) {
            StandardEntity entity = this.worldInfo.getEntity(path.get(path.size() - 1));
            if (entity instanceof Building) {
                if (entity.getStandardURN() != StandardEntityURN.REFUGE) {
                    path.remove(path.size() - 1);
                }
            }
            if (!path.isEmpty()) {
                ActionMove moveAction = (ActionMove) actionExtMove.setTarget(path.get(path.size() - 1)).calc().getAction();
                if (moveAction != null) {
                    return moveAction;
                }
            }
            return null;
        }
        return null;
    }

    private class DistanceSorter implements Comparator<StandardEntity> {
        private StandardEntity reference;
        private WorldInfo worldInfo;

        DistanceSorter(WorldInfo wi, StandardEntity reference) {
            this.reference = reference;
            this.worldInfo = wi;
        }

        public int compare(StandardEntity a, StandardEntity b) {
            int d1 = this.worldInfo.getDistance(this.reference, a);
            int d2 = this.worldInfo.getDistance(this.reference, b);
            return d1 - d2;
        }
    }


    //region Refill And Rest
    private boolean moveToHydrant = false;
    private boolean moveToHydrantFail = false;
    private int failTime = 0;
    protected int timeInThisHydrant = 0;
    protected EntityID lastHydrant = null;
    protected int stayInHydrant = 15;
    protected int currentWater = 0;
    protected int prevWater = 0;

    protected Action moveToRefugeIfDamagedOrTankIsEmpty() {
        int requiredWater;
        if (worldHelper.getRefuges().isEmpty() && worldHelper.getHydrants().isEmpty()) {
            return null;
        }
        FireBrigade self = (FireBrigade) worldInfo.getEntity(agentInfo.getID());

        StandardEntityURN positionURN = agentInfo.getPositionArea().getStandardURN();


        if (lastHydrant == null || !worldHelper.getSelfPosition().getID().equals(lastHydrant)) {
            timeInThisHydrant = 0;
            lastHydrant = null;
        }
        prevWater = currentWater;
        currentWater = self.getWater();

        if (worldHelper.getTime() - failTime > MRLConstants.AVAILABLE_HYDRANTS_UPDATE_TIME) {
            moveToHydrantFail = false;
        }


//        int fbCount = 0;
        if (positionURN == REFUGE) {
//            for (StandardEntity standardEntity : worldHelper.getFireBrigades()) {
//                FireBrigade fb = (FireBrigade) standardEntity;
//                if (fb.getPosition().equals(self.getPosition()) && worldHelper.isVisible(fb)) {
//                    fbCount++;
//                }
//            }

            requiredWater = worldHelper.isWaterRefillRateInRefugeSet ?
                    worldHelper.getWaterRefillRate() * 10 :     //10 Cycle in refuge
                    worldHelper.getMaxWater() / 3 * 2;          // 2/3 tank
            requiredWater = Util.min(worldHelper.getMaxWater(), requiredWater);
            if (self.isWaterDefined() && self.getWater() < requiredWater) {
                if (!worldHelper.isWaterRefillRateInRefugeSet) {
//                    if (!ifFirstTimeInRefuge) {
                    int refillRate = Math.abs(currentWater - prevWater);
//                        world.printData("refill rate can't have negative or zero value");
                    if (refillRate > 0 && !(lastAction instanceof ActionExtinguish)) {
                        worldHelper.setWaterRefillRate(refillRate);
                        worldHelper.isWaterRefillRateInRefugeSet = true;
                    }
                }
                return new ActionRefill();
            }
        } else if (positionURN == HYDRANT) {
            if (!worldHelper.isWaterRefillRateInHydrantSet) {
                int refillRate = Math.abs(currentWater - prevWater);
                if (refillRate <= 0) {
                    //world.printData("refill rate in hydrant is negative or zero value");
                } else if (!(lastAction instanceof ActionExtinguish)) {
                    worldHelper.setWaterRefillRateInHydrant(refillRate);
                    //world.printData("refillRate in hydrant is " + refillRate);
                    worldHelper.isWaterRefillRateInHydrantSet = true;
                }
            }
            boolean havePermission;//show whether current agent has permission to use current hydrant or not
            if (worldHelper.getAvailableHydrants().contains(worldHelper.getSelfPosition())) {
                havePermission = true;
            } else {
                havePermission = false;
            }


            if (havePermission) {
//                Hydrant hydrant = (Hydrant) self.getPosition();
                lastHydrant = self.getPosition();
                requiredWater = Math.min(worldHelper.getMaxPower() * 3, worldHelper.getMaxWater() * 2 / 3);
                if (self.isWaterDefined() && self.getWater() < requiredWater) {
                    if (timeInThisHydrant < stayInHydrant) {
                        timeInThisHydrant++;
                        List<EntityID> path = new ArrayList<EntityID>();
                        path.add(self.getPosition());
                        new ActionMove(path);
//                    self.sendRestAct(world.getTime());
                    } else {
                        failTime = worldHelper.getTime();
                        moveToHydrantFail = true;

                    }
                }
                moveToHydrant = false;
            }
        }

        if (worldHelper.getRefuges().size() > 0 && (self.getDamage() > 10 && self.getHP() < 5000) || self.getWater() == 0) {
            Action action = moveToRefuge();//todo
            if (action != null) {
                return action;
            }
        }

        if (worldHelper.getHydrants().size() > 0 && self.getWater() <= 0 && !moveToHydrantFail) {
            moveToHydrant = true;
            Action action = moveToHydrant();//todo
            if (action != null) {
                return action;
            }
            moveToHydrant = false;
        }

        return null;

    }

    private Action moveToRefuge() {
        Area selfPositionArea = (Area) worldInfo.getEntity(agentInfo.getPosition());
        List<EntityID> refugePath = getRefugePath(selfPositionArea, false);
        if (refugePath == null || refugePath.isEmpty()) {
            return null;
        }
        return new ActionMove(refugePath);
    }

    private Action moveToHydrant() {
        Area selfPositionArea = (Area) worldInfo.getEntity(agentInfo.getPosition());
        List<EntityID> hydrantPath = getHydrantPath(selfPositionArea, false);
        if (hydrantPath == null || hydrantPath.isEmpty()) {
            return null;
        }
        return new ActionMove(hydrantPath);
    }

    private List<EntityID> getRefugePath(Area sourceArea, boolean force) {
        return getAreaTypePath(sourceArea, Math.min(10, worldHelper.getRefuges().size()), Refuge.class, worldHelper.getRefuges(), force);
    }

    private List<EntityID> getHydrantPath(Area sourceArea, boolean force) {
        return getAreaTypePath(sourceArea, Math.min(10, worldHelper.getHydrants().size()), Hydrant.class, worldHelper.getAvailableHydrants(), force);
    }

    @Override
    public Action getAction() {
        return lastAction = result;
    }


    private List<EntityID> getAreaTypePath(Area sourceArea, int numberToGetNearest, Class<? extends StandardEntity> areaType, Collection<? extends StandardEntity> validAreas, boolean force) {
        /**
         * aval chand ta az nazdiktarin area haro peyda mikonim.
         * ba'd ba hame oon ha path mizanim.
         * pathe nakdiktarin ro barmigardunim.
         */
        List<EntityID> finalPath = new ArrayList<EntityID>();

        if (validAreas.isEmpty() || areaType.isInstance(sourceArea)) {
            // age area nadashte bashim ke hichi.
            return finalPath;
        }

        List<EntityID> bestAreaPath = new ArrayList<EntityID>();
        Area bestArea = null;
        double minimumCost = Double.MAX_VALUE;
        List<EntityID> path;
        double cost;

        List<Area> nearestAreas;
        // te'dadi az area haye nazdik (az nazare fasele oghlidosi) ro migire.
        nearestAreas = getSomeNearAreaType(sourceArea.getX(), sourceArea.getY(), numberToGetNearest, validAreas);

        for (Area area : nearestAreas) {
            if (area == null) {
                continue;
            }

            if (sourceArea.equals(area)) {
                return null;
            }

            // be hameye area haye nazdik path mizane va nazdiktarin ro entekhab mikone.
//                path = a_star.getShortestGraphPath(sourceArea, area, force);
            pathPlanning.setFrom(agentInfo.getPosition()).setDestination(area.getID()).calc();
            path = pathPlanning.getResult();

            // kamtarin cost ro peyda mikonim.
            if (path != null && !path.isEmpty()) {
                cost = pathPlanning.getDistance();

                if (cost < minimumCost) {
                    minimumCost = cost;
                    bestArea = area;
                    bestAreaPath = new ArrayList<>(path);
                }
            } else {
                worldHelper.flagUnreachable(area);
//                setUnreachableTarget(area);
            }
        }

        if (bestArea == null && !nearestAreas.isEmpty()) {
            // age area khubi peyda nakard avalin area ke az nazare oghlidosi nazdike ro entekhab mmikone.
            if (MRLConstants.DEBUG_PLANNER) {
                MrlPersonalData.VIEWER_DATA.print(" true move....");
            }
            bestArea = nearestAreas.get(0);
//                bestAreaPath = a_star.getShortestGraphPath(sourceArea, bestArea, true);


            bestAreaPath = pathPlanning.setFrom(agentInfo.getPosition()).setDestination(bestArea.getID()).calc().getResult();

        }
//        if (bestArea != null) {
//            if (MRLConstants.DEBUG_PLANNER) {
//                world.printData("  best:" + bestArea + " -------- nearest area = " + nearestAreas);
//            }
//            previousTarget = bestArea;
//            nearestAreaPathCost = minimumCost;
//            previousPath = bestAreaPath;//a_star.getAreaPath(sourceArea.getID(), bestArea.getID(), bestAreaPath);
//
//        }


        finalPath = bestAreaPath;
        return finalPath;
    }

//    private Pair<List<EntityID>, Integer> planMove(Area area) {
//        pathPlanning.setFrom(agentInfo.getPosition()).setDestination(area.getID()).calc().ge
//
//    }

    private List<Area> getSomeNearAreaType(int selfX, int selfY, int numberToGetNearest, Collection<? extends StandardEntity> areas) {
        // peyda kardane chand refuge-e nakdiz az nazare fasele oghlidosi.

        List<Area> nearAreas = new ArrayList<Area>();
        List<Area> allRefuges = new ArrayList<Area>();
        for (StandardEntity standardEntity : areas) {
            Area refuge = (Area) standardEntity;
            allRefuges.add(refuge);
        }
        Area selected;
        Area absoluteNearestArea = null;
        int absoluteMinDist = Integer.MAX_VALUE;
        int minDistance, distance, maxDistance = -1;

        StandardEntity self = worldInfo.getEntity(agentInfo.getID());

        while (nearAreas.size() < numberToGetNearest) {

            minDistance = Integer.MAX_VALUE;
            selected = null;

            for (Area area : allRefuges) {
                distance = Util.distance(selfX, selfY, area.getX(), area.getY());
                if (distance < minDistance && (area instanceof Building && worldHelper.getMrlBuilding(area.getID()).isOneEntranceOpen(worldHelper))) {
                    minDistance = distance;
                    selected = area;
                    absoluteNearestArea = area;
                }
                if (absoluteMinDist > distance) {
                    absoluteMinDist = distance;
                    absoluteNearestArea = area;
                }
            }
            if (self instanceof PoliceForce && absoluteNearestArea != null) {
                break;
            }
            if (maxDistance == -1) {
                maxDistance = minDistance * 2;
            } else if (maxDistance < minDistance) {
                // age distance kheili ziad beshe dige edame nemidim.
                break;
            }
            if (selected != null) {
                allRefuges.remove(selected);
                nearAreas.add(selected);
            }
        }
        if (nearAreas.isEmpty()) {
            nearAreas.add(absoluteNearestArea);
            if (MRLConstants.DEBUG_PLANNER) {
                System.out.println("absoluteNearestArea: " + absoluteNearestArea);
            }
        }
        return nearAreas;
    }
    //endregion

    public ExtAction actionExtinguish(){
        this.result = null;
        FireBrigade agent = (FireBrigade) this.agentInfo.me();

        Action action = moveToRefugeIfDamagedOrTankIsEmpty();
        if (action != null) {
            this.result = action;
            return this;
        }

        if (this.target == null) {
            return this;
        }
        if (this.target!=null){
            PreExtinguish preExtinguish = new PreExtinguish();
            this.result=preExtinguish.calc(agent, this.pathPlanning, this.target);
        }
        this.result = this.calcExtinguish(agent, this.pathPlanning, this.target);
        return this;

    }

}

