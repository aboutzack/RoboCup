package mrl_2019.complex.firebrigade;


import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.agent.communication.standard.bundle.information.MessageBuilding;
import adf.agent.communication.standard.bundle.information.MessageCivilian;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.communication.CommunicationMessage;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.BuildingDetector;
import adf.launcher.ConsoleOutput;
import com.mrl.debugger.remote.VDClient;
import com.mrl.debugger.remote.dto.BuildingDto;
import mrl_2019.algorithm.clustering.Cluster;
import mrl_2019.algorithm.clustering.MrlFireClustering;
import mrl_2019.complex.firebrigade.directionbased.DirectionBasedTargetSelector14;
import mrl_2019.util.Util;
import mrl_2019.viewer.MrlPersonalData;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.entity.MrlBuilding;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.io.Serializable;
import java.util.*;
import java.util.List;

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class MrlBuildingDetector extends BuildingDetector {
    private EntityID result;

    protected MrlFireClustering mrlFireClustering;
    protected MrlWorldHelper worldHelperb;
    protected ClusterTemperature clusterTemperature;

    private int sendTime;
    private int sendingAvoidTimeClearRequest;

    private Collection<EntityID> agentPositions;
    private Map<EntityID, Integer> sentTimeMap;
    private int sendingAvoidTimeReceived;
    private int sendingAvoidTimeSent;

    private int moveDistance;
    private EntityID lastPosition;
    private int positionCount;
    private Map<EntityID, BuildingProperty> sentBuildingMap;
    private TargetSelectorType targetSelectorType = TargetSelectorType.FULLY_GREEDY;
    private IFireBrigadeTargetSelector targetSelector;
    private MrlFireBrigadeWorld worldHelper;
    private PathPlanning pathPlanning;
    private MrlBuilding lastSelectedBuilding;
    private ExploreManager exploreManager;

    public MrlBuildingDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        switch (si.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("ActionExtClear.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("ActionExtClear.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("ActionExtClear.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
        }

        exploreManager = moduleManager.getModule("FireBrigadeSearch.ExploreManager", "mrl_2019.complex.firebrigade.TargetBaseExploreManager");


        this.worldHelper = (MrlFireBrigadeWorld) MrlWorldHelper.load(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
        this.sendTime = 0;
        this.sendingAvoidTimeClearRequest = developData.getInteger("SampleBuildingDetector.sendingAvoidTimeClearRequest", 5);


        this.agentPositions = new HashSet<>();
        this.sentTimeMap = new HashMap<>();
        this.sentBuildingMap = new HashMap<>();
        this.sendingAvoidTimeReceived = developData.getInteger("SampleBuildingDetector.sendingAvoidTimeReceived", 3);
        this.sendingAvoidTimeSent = developData.getInteger("SampleBuildingDetector.sendingAvoidTimeSent", 5);

        this.moveDistance = developData.getInteger("SampleBuildingDetector.moveDistance", 40000);

        registerModule(worldHelper);
        registerModule(pathPlanning);
        registerModule(exploreManager);

        setTargetSelectorApproach();
    }

    @Override
    public BuildingDetector updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);

        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
//        this.clustering.updateInfo(messageManager);

        this.reflectMessage(messageManager);
        this.sendEntityInfo(messageManager);

//        if (this.result != null) {
//            Building building = (Building) this.worldInfo.getEntity(this.result);
//            if (building.getFieryness() >= 4) {
//                messageManager.addMessage(new MessageBuilding(true, building));
//            }
//        }

        this.worldHelper.getPossibleBurningBuildings().removeAll(worldInfo.getChanged().getChangedEntities());

        worldInfo.getChanged().getChangedEntities().forEach(id -> {
            StandardEntity entity = worldInfo.getEntity(id);
            if (entity instanceof Building) {
                Building building = (Building) worldInfo.getEntity(id);
                if (building.isFierynessDefined() && building.getFieryness() > 0 /*|| building.isTemperatureDefined() && building.getTemperature() > 0*/) {
                    BuildingProperty buildingProperty = sentBuildingMap.get(id);
                    if (buildingProperty == null || buildingProperty.getFieryness() != building.getFieryness() || buildingProperty.getFieryness() == 1) {
//                        printDebugMessage("burningBuilding: " + building.getID());
                        messageManager.addMessage(new MessageBuilding(true, building));
                        messageManager.addMessage(new MessageBuilding(false, building));
                        sentBuildingMap.put(id, new BuildingProperty(building));
                    }
                }
            } else if (entity instanceof Civilian) {
                Civilian civilian = (Civilian) entity;
                if ((civilian.isHPDefined() && civilian.getHP() > 1000 && civilian.isDamageDefined() && civilian.getDamage() > 0)
                        || ((civilian.isPositionDefined() && !(worldInfo.getEntity(civilian.getPosition()) instanceof Refuge))
                        && (worldInfo.getEntity(civilian.getPosition()) instanceof Building))) {
                    messageManager.addMessage(new MessageCivilian(true, civilian));
                    messageManager.addMessage(new MessageCivilian(false, civilian));
//                    System.out.println(" CIVILIAN_MESSAGE: " + agentInfo.getTime() + " " + agentInfo.getID() + " --> " + civilian.getID());
                }

            }
        });






        int currentTime = this.agentInfo.getTime();
        Human agent = (Human) this.agentInfo.me();
        int agentX = agent.getX();
        int agentY = agent.getY();
        StandardEntity positionEntity = this.worldInfo.getPosition(agent);
        if (positionEntity instanceof Road) {
            Road road = (Road) positionEntity;
            if (road.isBlockadesDefined() && road.getBlockades().size() > 0) {
                for (Blockade blockade : this.worldInfo.getBlockades(road)) {
                    if (blockade == null || !blockade.isApexesDefined()) {
                        continue;
                    }
                    if (Util.isInside(agentX, agentY, blockade.getApexes())) {
                        if ((this.sendTime + this.sendingAvoidTimeClearRequest) <= currentTime) {
                            this.sendTime = currentTime;
                            messageManager.addMessage(
                                    new CommandPolice(
                                            true,
                                            null,
                                            agent.getPosition(),
                                            CommandPolice.ACTION_CLEAR
                                    )
                            );
                            break;
                        }
                    }
                }
            }
            if (this.lastPosition != null && this.lastPosition.getValue() == road.getID().getValue()) {
                this.positionCount++;
                if (this.positionCount > this.getMaxTravelTime(road)) {
                    if ((this.sendTime + this.sendingAvoidTimeClearRequest) <= currentTime) {
                        this.sendTime = currentTime;
                        messageManager.addMessage(
                                new CommandPolice(
                                        true,
                                        null,
                                        agent.getPosition(),
                                        CommandPolice.ACTION_CLEAR
                                )
                        );
                    }
                }
            } else {
                this.lastPosition = road.getID();
                this.positionCount = 0;
            }
        }
        return this;
    }

    @Override
    public BuildingDetector calc() {
        try {


            //TODO @MRL extinguishNearbyWhenStuck();

            Cluster targetCluster;

            //TODO @MRL migrate find my best cluster function
//            if (worldHelper.isCommunicationLess() || worldHelper.isCommunicationLow()|| worldHelper.getFireClustering().getClusters().size() >= 3) {
            targetCluster = worldHelper.getFireClustering().findNearestCluster((worldHelper.getSelfLocation()));
//            }else {
//                targetCluster = worldHelper.getFireClustering().findMyBestCluster(lastSelectedBuilding);
//            }


//          TODO @MRL
            if (targetCluster != null) {
                if (!targetCluster.isControllable() && !targetSelectorType.equals(TargetSelectorType.DIRECTION_BASED14)) {
                    targetSelectorType = TargetSelectorType.DIRECTION_BASED14;
                    setTargetSelectorApproach();
                    MrlPersonalData.VIEWER_DATA.print(agentInfo.getID() + " change strategy to " + targetSelectorType);
                } else if (targetCluster.isControllable() && !targetSelectorType.equals(TargetSelectorType.FULLY_GREEDY)) {
                    targetSelectorType = TargetSelectorType.FULLY_GREEDY;
                    setTargetSelectorApproach();
                    MrlPersonalData.VIEWER_DATA.print(agentInfo.getID() + " change strategy to " + targetSelectorType);
                }
            }
            Collection<StandardEntity> entities = this.worldInfo.getEntitiesOfType(
                    StandardEntityURN.BUILDING,
                    StandardEntityURN.GAS_STATION,
                    StandardEntityURN.AMBULANCE_CENTRE,
                    StandardEntityURN.FIRE_STATION,
                    StandardEntityURN.POLICE_OFFICE
            );

            Set<StandardEntity> fireBuildings = new HashSet<>();
            for (StandardEntity entity : entities) {
                if (((Building) entity).isOnFire()) {
                    fireBuildings.add(entity);
                }
            }

            if (MrlPersonalData.DEBUG_MODE) {
                List<Integer> elementList = Util.fetchIdValueFormElements(fireBuildings);
                //todo send data to Buildings layer
                VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlBurningBuildingsLayer", (Serializable) elementList);
            }

            if (MrlPersonalData.DEBUG_MODE) {
                List<BuildingDto> estimated = new ArrayList<>();
                worldHelper.getMrlBuildings().forEach(mrlBuilding -> {
                    if (mrlBuilding.getEstimatedFieryness() > 0) {
                        estimated.add(new BuildingDto(mrlBuilding.getID().getValue(), mrlBuilding.getEstimatedFieryness()));
                    }
                });

//                List<Integer> elementList = Util.fetchIdValueFormElementIds(estimated);
                //todo send data to Buildings layer
                VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlEstimatedBurningBuildingsLayer", (Serializable) estimated);
            }


            if (this.worldHelper.getFireClustering().getClusterNumber() > 0) {
//                for (int i = 0; i < this.clustering.getClusterNumber(); i++) {
//                    clusterConvexPolygons.add(i, createConvexHull(this.clustering.getClusterEntities(i)));
//                }


                double minDistance = Double.MAX_VALUE;
                int nearestClusterIndex = 0;
                for (int i = 0; i < this.worldHelper.getFireClustering().getClusterConvexPolygons().size(); i++) {
                    double distance = Util.distance(this.worldHelper.getFireClustering().getClusterConvexPolygons().get(i), worldInfo.getLocation(agentInfo.getID()), false);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestClusterIndex = i;
                    }
                }

//                System.out.println(agentInfo.getTime() + " " + agentInfo.getID() + " clusterIndex: " +
//                        nearestClusterIndex + " clusterSize: " + clustering.getClusterEntities(nearestClusterIndex).size());

//            for (int i = 0; i < this.clustering.getClusterNumber(); i++) {
//                System.out.println(agentInfo.getID() + " first cluster : " + this.clustering.getClusterEntities(i).size());
                if (MrlPersonalData.DEBUG_MODE) {
                    Collection<StandardEntity> clusterEntities = this.worldHelper.getFireClustering().getClusterEntities(nearestClusterIndex);
                    if (clusterEntities != null) {
                        List<Integer> elementList = Util.fetchIdValueFormElements(clusterEntities);
                        VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlSampleBuildingsLayer", (Serializable) elementList);
                    }
                }
                if (MrlPersonalData.DEBUG_MODE) {
                    ArrayList<Polygon> data = new ArrayList<>();
                    data.add(worldHelper.getFireClustering().getClusterConvexPolygons().get(nearestClusterIndex));
                    VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "ClusterConvexPolygon", data);
                }
//            }

//                Cluster targetCluster;
//
//                targetCluster = worldHelper.getFireClustering().findNearestCluster((worldInfo.getLocation(agentInfo.getID())));


                FireBrigadeTarget fireBrigadeTarget = targetSelector.selectTarget(targetCluster);

                if (fireBrigadeTarget != null) {
                    lastSelectedBuilding = fireBrigadeTarget.getMrlBuilding();
                    findPossibleBurningBuildings(lastSelectedBuilding);
                } else {
                    lastSelectedBuilding = null;
                }


//    TODO @MRL

                // explore around last target
                if (exploreManager.isTimeToExplore(fireBrigadeTarget)) {
                    EntityID exploreTarget = null;
                    exploreManager.calc();
                    exploreTarget = exploreManager.getTarget();
                    worldHelper.setExploreTarget(exploreTarget);
                    if (exploreTarget != null) {
                        lastSelectedBuilding = null;
                    }
                } else {
                    worldHelper.setExploreTarget(null);
                }
                if (lastSelectedBuilding != null) {
                    this.result = lastSelectedBuilding.getID();
//                    worldHelper.printData(" YESSSSSS ... target is : " + result);
                } else {
//                    worldHelper.printData(" no target found in targetCluster so look for target in sample cluster ...");
//                    this.result = this.calcTargetInCluster(nearestClusterIndex);
                    this.result = null;
                }

            } else {
                this.result = null;
            }
            if (this.result == null) {
//                worldHelper.printData(" my cluster target is null so look for target in the world ...");
//                this.result = this.calcTargetInWorld(fireBuildings);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    private void findPossibleBurningBuildings(MrlBuilding lastSelectedBuilding) {

        if (lastSelectedBuilding != null) {
//            Collection<StandardEntity> objectsInRange = worldInfo.getObjectsInRange(lastSelectedBuilding.getSelfBuilding(), range);
            List<MrlBuilding> objectsInRange = lastSelectedBuilding.getConnectedBuilding();
            for (MrlBuilding mrlBuilding : objectsInRange) {
                if (!mrlBuilding.getSelfBuilding().isOnFire() && worldInfo.getDistance(lastSelectedBuilding.getID(), mrlBuilding.getID()) < scenarioInfo.getPerceptionLosMaxDistance()) {
//                    MrlBuilding mrlBuilding = worldHelper.getMrlBuilding(entity.getID());
//                    if (agentInfo.getTime() - mrlBuilding.getSensedTime() > resetTime) {
                    worldHelper.getPossibleBurningBuildings().add(mrlBuilding.getID());
                }
//                    }
            }
        }

        if (MrlPersonalData.DEBUG_MODE) {
            if (worldHelper.getPossibleBurningBuildings() != null) {
                List<Integer> elementList = Util.fetchIdValueFormElementIds(worldHelper.getPossibleBurningBuildings());
                VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlPossibleBurningBuildingsLayer", (Serializable) elementList);
            }
        }


    }


    private void setTargetSelectorApproach() {

        switch (targetSelectorType) {
            case FULLY_GREEDY:
                targetSelector = new FullyGreedyTargetSelector(worldHelper, agentInfo);
                break;
            case DIRECTION_BASED14:
                targetSelector = new DirectionBasedTargetSelector14(worldHelper, agentInfo);
                break;

        }
    }

    private EntityID calcTargetInCluster(int nearestClusterIndex) {
        StandardEntity targetBuilding = null;
        Cluster b;
        if (nearestClusterIndex != -1) {
            Collection<StandardEntity> elements = this.worldHelper.getFireClustering().getClusterEntities(nearestClusterIndex);

            if (MrlPersonalData.DEBUG_MODE) {
                List<Integer> elementList = Util.fetchIdValueFormElements(elements);
                VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlSampleBuildingsLayer", (Serializable) elementList);
            }

            if (elements == null || elements.isEmpty()) {
                return null;
            }

            Set<StandardEntity> borderBuildings = findBorderElements(elements, worldHelper.getFireClustering().getClusterConvexPolygons().get(nearestClusterIndex));

            targetBuilding = findBestBuilding(borderBuildings, worldHelper.getFireClustering().getClusterConvexPolygons().get(nearestClusterIndex));


            b=this.mrlFireClustering.findBestCluster();
            if (b !=null){
                this.clusterTemperature.findTempOfCluster(worldHelperb.getBuildingIDs());
            }


        }
        return targetBuilding != null ? targetBuilding.getID() : null;
    }

    private StandardEntity findBestBuilding(Set<StandardEntity> borderBuildings, Polygon polygon) {
        StandardEntity bestBuilding = null;
        int minDistance = Integer.MAX_VALUE;

        List<BuildingProperty> buildingPropertyList = new ArrayList<>();
        for (StandardEntity borderEntity : borderBuildings) {
            Building building = (Building) borderEntity;
            int fieryness = building.isFierynessDefined() ? building.getFieryness() : 0;
            int temperature = building.isTemperatureDefined() ? building.getTemperature() : 0;
            BuildingProperty buildingProperty = new BuildingProperty(borderEntity.getID(), fieryness, temperature);
            buildingProperty.setValue(calculateValue(building, polygon));
            buildingPropertyList.add(buildingProperty);
        }

        double maxValue = Double.MIN_VALUE;
        BuildingProperty selectedBuildingProperty = null;
        for (BuildingProperty buildingProperty : buildingPropertyList) {
            if (buildingProperty.getValue() > maxValue) {
                maxValue = buildingProperty.getValue();
                selectedBuildingProperty = buildingProperty;
            }
        }

        if (selectedBuildingProperty == null) {
            StandardEntity nearestBuilding = findNearestBuilding(borderBuildings);
            System.out.println("Nearest ... " + agentInfo.getID() + "  -->  " + nearestBuilding.getID());
            return nearestBuilding;
        } else {
            System.out.println("BestValue ... " + agentInfo.getID() + "  -->  " + selectedBuildingProperty.getEntityID() + " value: " + selectedBuildingProperty.getValue());
            return worldInfo.getEntity(selectedBuildingProperty.getEntityID());
        }
    }

    private StandardEntity findNearestBuilding(Set<StandardEntity> borderBuildings) {
        StandardEntity bestBuilding = null;
        int minDistance = Integer.MAX_VALUE;
        for (StandardEntity borderEntity : borderBuildings) {
            int distance = worldInfo.getDistance(borderEntity.getID(), agentInfo.getID());
            if (distance < minDistance) {
                minDistance = distance;
                bestBuilding = borderEntity;
            }
        }
        return bestBuilding;
    }

    private static final double INITIAL_COST = 500;
    private static final double AGENT_SPEED = 32000;
    private static final double BASE_PER_MOVE_COST = 30;
    private static final double MAX_DISTANCE_COST = BASE_PER_MOVE_COST * 10;
    private static final double SHOULD_MOVE_COST = BASE_PER_MOVE_COST * 2.2;
    private static final double NOT_IN_CHANGESET_COST = BASE_PER_MOVE_COST * 1.2;


    private double perMoveCost;
    private double shouldMoveCost;
    private double notInChangeSetCost;

    private double calculateCost(Building building, Polygon clusterPolygon) {

        double cost = INITIAL_COST;

        double clusterSize = Math.max(clusterPolygon.getBounds2D().getWidth(), clusterPolygon.getBounds2D().getHeight());
        double mapSize = Math.max(worldInfo.getBounds().getWidth(), worldInfo.getBounds().getHeight());
        double worldFireBuildingSituation = clusterSize / mapSize;
        double coefficient = worldFireBuildingSituation;
        this.perMoveCost = BASE_PER_MOVE_COST * coefficient;
        this.shouldMoveCost = SHOULD_MOVE_COST * coefficient;
        this.notInChangeSetCost = NOT_IN_CHANGESET_COST * coefficient;

        // distance and should move    //todo: change with pathPlaner mostafas
        double distance = Util.distance(agentInfo.getX(), agentInfo.getY(), building.getX(), building.getY());
        if (distance > scenarioInfo.getFireExtinguishMaxDistance()) {
            double timeToMove = (distance - scenarioInfo.getFireExtinguishMaxDistance()) / AGENT_SPEED;
            double distanceCost;
            if (timeToMove <= 0.5) {
                distanceCost = timeToMove * perMoveCost * 1.3;//0.4;
            } else if (timeToMove <= 2) {
                distanceCost = timeToMove * perMoveCost * 1.9;//0.6;
            } else if (timeToMove <= 4) {
                distanceCost = timeToMove * perMoveCost * 2.6;//0.8;
            } else {
                distanceCost = timeToMove * perMoveCost * 3.1;//1.0;
            }
            if (distanceCost > MAX_DISTANCE_COST) {
                distanceCost = MAX_DISTANCE_COST;
            }
            cost += distanceCost + shouldMoveCost;
        }


        return cost;
    }


    private double calculateValue(Building building, Polygon clusterPolygon) {


        double value = 0;

        double clusterSize = Math.max(clusterPolygon.getBounds2D().getWidth(), clusterPolygon.getBounds2D().getHeight());
        double mapSize = Math.max(worldInfo.getBounds().getWidth(), worldInfo.getBounds().getHeight());
        double worldFireBuildingSituation = clusterSize / mapSize;
        double coefficient = worldFireBuildingSituation;
        this.perMoveCost = BASE_PER_MOVE_COST * coefficient;
        this.shouldMoveCost = SHOULD_MOVE_COST * coefficient;
        this.notInChangeSetCost = NOT_IN_CHANGESET_COST * coefficient;

        if (building.isFierynessDefined()) {
            switch (building.getFieryness()) {
                case 1:
                    value = 1000;
                    break;
                case 2:
                    value = 300;
                    break;
                case 3:
                    value = 100;
                    break;
            }
        }


        // distance and should move    //todo: change with pathPlaner mostafas
        double distance = Util.distance(agentInfo.getX(), agentInfo.getY(), building.getX(), building.getY());
        if (distance > scenarioInfo.getFireExtinguishMaxDistance()) {
            double timeToMove = (distance - scenarioInfo.getFireExtinguishMaxDistance()) / AGENT_SPEED;
            value = (value + 1) / (timeToMove + 0.01);
        } else {
            value = value + 200;
        }


        return value;
    }


    private Set<StandardEntity> findBorderElements(Collection<StandardEntity> elements, Polygon nearestClusterPolygon) {
        Set<StandardEntity> borderElements = new HashSet<>();

        Collection<StandardEntity> entities = worldInfo.getEntitiesOfType(StandardEntityURN.BUILDING);
        elements = entities;
        if (elements != null) {
            elements.forEach(entity -> {
                Building building = (Building) entity;
                int vertexes[] = building.getApexList();
                for (int i = 0; i < vertexes.length; i += 2) {
                    double distance = Util.distance(nearestClusterPolygon, new Pair<>(vertexes[i], vertexes[i + 1]), false);
                    if (distance < scenarioInfo.getPerceptionLosMaxDistance() / 4) {
                        borderElements.add(entity);
                        break;
                    }
//                    if ((bigBorderPolygon.contains(vertexes[i], vertexes[i + 1])) && !(smallBorderPolygon.contains(vertexes[i], vertexes[i + 1]))) {
//                        borderEntities.add(building);
//                        break;
//                    }
                }


//                double distance = Util.distance(nearestClusterPolygon, worldInfo.getLocation(entity.getID()), false);
//                if (distance < scenarioInfo.getPerceptionLosMaxDistance() / 2) {
//                    borderElements.add(entity);
//                }
            });
        }

        if (MrlPersonalData.DEBUG_MODE) {
            List<Integer> elementList = Util.fetchIdValueFormElements(borderElements);
            VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlBorderBuildingsLayer", (Serializable) elementList);
        }


        return borderElements;
    }

    private EntityID calcTargetInWorld(Set<StandardEntity> fireBuildings) {

        if (!fireBuildings.isEmpty()) {
            List<StandardEntity> fireBuildingsArray = new ArrayList<>(fireBuildings);
            fireBuildingsArray.sort(new DistanceSorter(this.worldInfo, agentInfo.me()));
            EntityID targetId = fireBuildingsArray.get(0).getID();

            if (MrlPersonalData.DEBUG_MODE) {

//                List<Line2D> lines = new ArrayList<>();
//                Pair<Integer, Integer> targetLocation = worldInfo.getLocation(targetId);
//                lines.add(new Line2D(agentInfo.getX(), agentInfo.getY(), targetLocation.first(), targetLocation.second()));
//                VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "FireBrigadeTargetLine", (Serializable) lines);
            }
            return targetId;
        }

        return null;
    }

    @Override
    public EntityID getTarget() {
        return this.result;
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

    private void reflectMessage(MessageManager messageManager) {
        Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
        changedEntities.add(this.agentInfo.getID());
        int time = this.agentInfo.getTime();
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageBuilding.class)) {
            MessageBuilding mb = (MessageBuilding) message;
            if (!changedEntities.contains(mb.getBuildingID())) {
                MessageUtil.reflectMessage(this.worldInfo, mb);
            }
            this.sentTimeMap.put(mb.getBuildingID(), time + this.sendingAvoidTimeReceived);
        }

        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageCivilian.class)) {
            MessageCivilian mc = (MessageCivilian) message;
            if (!changedEntities.contains(mc.getAgentID())) {
                MessageUtil.reflectMessage(this.worldInfo, mc);
            }
//                this.sentTimeMap.put(mc.getAgentID(), time + this.sendingAvoidTimeReceived);
        }
    }

    private boolean checkSendFlags() {
        boolean isSendBuildingMessage = true;

        StandardEntity me = this.agentInfo.me();
        if (!(me instanceof Human)) {
            return false;
        }
        Human agent = (Human) me;
        EntityID agentID = agent.getID();
        EntityID position = agent.getPosition();
        StandardEntityURN agentURN = agent.getStandardURN();
        EnumSet<StandardEntityURN> agentTypes = EnumSet.of(AMBULANCE_TEAM, FIRE_BRIGADE, POLICE_FORCE);
        agentTypes.remove(agentURN);

        this.agentPositions.clear();
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(agentURN)) {
            Human other = (Human) entity;
            if (isSendBuildingMessage) {
                if (other.getPosition().getValue() == position.getValue()) {
                    if (other.getID().getValue() > agentID.getValue()) {
                        isSendBuildingMessage = false;
                    }
                }
            }
            this.agentPositions.add(other.getPosition());
        }
        for (StandardEntityURN urn : agentTypes) {
            for (StandardEntity entity : this.worldInfo.getEntitiesOfType(urn)) {
                Human other = (Human) entity;
                if (isSendBuildingMessage) {
                    if (other.getPosition().getValue() == position.getValue()) {
                        if (urn == FIRE_BRIGADE) {
                            isSendBuildingMessage = false;
                        } else if (agentURN != FIRE_BRIGADE && other.getID().getValue() > agentID.getValue()) {
                            isSendBuildingMessage = false;
                        }
                    }
                }
                this.agentPositions.add(other.getPosition());
            }
        }
        return isSendBuildingMessage;
    }

    private void sendEntityInfo(MessageManager messageManager) {
        if (this.checkSendFlags()) {
            Building building = null;
            int currentTime = this.agentInfo.getTime();
            Human agent = (Human) this.agentInfo.me();
            for (EntityID id : this.worldInfo.getChanged().getChangedEntities()) {
                StandardEntity entity = this.worldInfo.getEntity(id);
                if (entity instanceof Building) {
                    Integer time = this.sentTimeMap.get(id);
                    if (time != null && time > currentTime) {
                        continue;
                    }
                    Building target = (Building) entity;
                    if (!this.agentPositions.contains(target.getID())) {
                        building = this.selectBuilding(building, target);
                    } else if (target.getID().getValue() == agent.getPosition().getValue()) {
                        building = this.selectBuilding(building, target);
                    }
                }
            }
            if (building != null) {
                messageManager.addMessage(new MessageBuilding(true, building));
                this.sentTimeMap.put(building.getID(), currentTime + this.sendingAvoidTimeSent);
            }
        }
    }

    private Building selectBuilding(Building building1, Building building2) {
        if (building1 != null) {
            if (building2 != null) {
                if (building1.isOnFire() && building2.isOnFire()) {
                    if (building1.getFieryness() < building2.getFieryness()) {
                        return building2;
                    } else if (building1.getFieryness() > building2.getFieryness()) {
                        return building1;
                    }
                    if (building1.isTemperatureDefined() && building2.isTemperatureDefined()) {
                        return building1.getTemperature() < building2.getTemperature() ? building2 : building1;
                    }
                } else if (!building1.isOnFire() && building2.isOnFire()) {
                    return building2;
                }
            }
            return building1;
        }
        return building2 != null ? building2 : null;
    }

    private int getMaxTravelTime(Area area) {
        int distance = 0;
        List<Edge> edges = new ArrayList<>();
        for (Edge edge : area.getEdges()) {
            if (edge.isPassable()) {
                edges.add(edge);
            }
        }
        if (edges.size() <= 1) {
            return Integer.MAX_VALUE;
        }
        for (int i = 0; i < edges.size(); i++) {
            for (int j = 0; j < edges.size(); j++) {
                if (i != j) {
                    Edge edge1 = edges.get(i);
                    double midX1 = (edge1.getStartX() + edge1.getEndX()) / 2;
                    double midY1 = (edge1.getStartY() + edge1.getEndY()) / 2;
                    Edge edge2 = edges.get(j);
                    double midX2 = (edge2.getStartX() + edge2.getEndX()) / 2;
                    double midY2 = (edge2.getStartY() + edge2.getEndY()) / 2;
                    int d = this.getDistance(midX1, midY1, midX2, midY2);
                    if (distance < d) {
                        distance = d;
                    }
                }
            }
        }
        if (distance > 0) {
            return (distance / this.moveDistance) + ((distance % this.moveDistance) > 0 ? 1 : 0) + 1;
        }
        return Integer.MAX_VALUE;
    }

    private int getDistance(double fromX, double fromY, double toX, double toY) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        return (int) Math.hypot(dx, dy);
    }

    protected void printDebugMessage(String msg) {
        ConsoleOutput.error("Agent:" + agentInfo.getID() + " Time:" + agentInfo.getTime() + " " + msg);
    }

}
