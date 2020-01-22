package mrl_2019.complex;


import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.agent.communication.standard.bundle.information.*;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.RoadDetector;
import adf.launcher.ConsoleOutput;
import com.mrl.debugger.remote.VDClient;
import mrl_2019.algorithm.clustering.ConvexHull;
import mrl_2019.complex.firebrigade.BuildingProperty;
import mrl_2019.util.Util;
import mrl_2019.viewer.MrlPersonalData;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.entity.Entrance;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.io.Serializable;
import java.util.*;
import java.util.List;

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class MrlPFRoadDetector extends RoadDetector {
    private Set<EntityID> targetAreas;
    private Set<EntityID> doneTasks;
    private Map<StandardEntityURN, Set<EntityID>> targetAreasMap;
    private Set<EntityID> priorityRoads;
    private Set<EntityID> coincidentalRoads;

    private PathPlanning pathPlanning;
    private Clustering clustering;

    private EntityID result;
    private Polygon clusterConvexPolygon;
    private Map<EntityID, BuildingProperty> sentBuildingMap;
    private MrlWorldHelper worldHelper;
    private Set<StandardEntity> targetsToClear;


    public MrlPFRoadDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        switch (scenarioInfo.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("SampleRoadDetector.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("MrlSimpleFireSearch.Clustering.Police", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("SampleRoadDetector.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("MrlSimpleFireSearch.Clustering.Police", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("SampleRoadDetector.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("MrlSimpleFireSearch.Clustering.Police", "adf.sample.module.algorithm.SampleKMeans");
                break;
        }

        this.worldHelper = MrlWorldHelper.load(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);


        targetsToClear = new HashSet<>();
        this.result = null;
        this.sentBuildingMap = new HashMap<>();
        this.coincidentalRoads = new HashSet<>();

    }

    @Override
    public RoadDetector calc() {

        EntityID positionID = this.agentInfo.getPosition();


        //find coincidental tasks and do them first, such as trapped FBs!!!
        addBlockedAgentsToTargets();
        addBlockedRefugesToTargets();
        addInBuildingHumansToTargets();


        this.coincidentalRoads.removeAll(doneTasks);
        if (this.result != null && this.coincidentalRoads.contains(this.result)) {
            return this;
        }

        if (!coincidentalRoads.isEmpty()) {
            return getRoadDetector(positionID, coincidentalRoads);
        }

        if (this.result == null) {
            List<EntityID> removeList = new ArrayList<>(this.priorityRoads.size());
            for (EntityID id : this.priorityRoads) {
                if (!this.targetAreas.contains(id)) {
                    removeList.add(id);
                }
            }
            this.priorityRoads.removeAll(removeList);
            this.priorityRoads.removeAll(this.doneTasks);
            this.targetAreasMap.values().forEach(entityIDSet -> {
                entityIDSet.removeAll(removeList);
                entityIDSet.removeAll(this.doneTasks);
            });
            if (this.priorityRoads.contains(positionID)) {
                this.result = positionID;
                return this;
            } else if (this.targetAreas.contains(positionID)) {
                this.result = positionID;
                return this;
            }

            Set<EntityID> fireBrigadeEntityIDSet = this.targetAreasMap.get(StandardEntityURN.FIRE_BRIGADE);
            Set<EntityID> refugeEntityIDSet = this.targetAreasMap.get(StandardEntityURN.REFUGE);
            Set<EntityID> ambulanceEntityIDSet = this.targetAreasMap.get(StandardEntityURN.AMBULANCE_TEAM);
            if (fireBrigadeEntityIDSet != null && !fireBrigadeEntityIDSet.isEmpty()) {
                return getRoadDetector(positionID, fireBrigadeEntityIDSet);
            }
            if (refugeEntityIDSet != null && !refugeEntityIDSet.isEmpty()) {
                return getRoadDetector(positionID, refugeEntityIDSet);
            }
            if (ambulanceEntityIDSet != null && !ambulanceEntityIDSet.isEmpty()) {
                return getRoadDetector(positionID, ambulanceEntityIDSet);
            }

            if (this.priorityRoads.size() > 0) {
                return getRoadDetector(positionID, this.priorityRoads);
            }


            this.pathPlanning.setFrom(positionID);
            this.pathPlanning.setDestination(this.targetAreas);
            List<EntityID> path = this.pathPlanning.calc().getResult();
            if (path != null && path.size() > 0) {
                this.result = path.get(path.size() - 1);
            }
        }
        return this;
    }

    private void addInBuildingHumansToTargets() {


        if (MrlPersonalData.DEBUG_MODE) {
            Collection<StandardEntity> civilians = worldInfo.getEntitiesOfType(StandardEntityURN.CIVILIAN);
            if (!civilians.isEmpty()) {
                List<Integer> elementList = Util.fetchIdValueFormElements(civilians);
                VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlKnownVictimsLayer", (Serializable) elementList);
            }
        }


        // get Blocked Civilians
        Civilian civilian;
        for (StandardEntity civilianEntity : worldInfo.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
            civilian = (Civilian) civilianEntity;
            if (!civilian.isPositionDefined() || !civilian.isHPDefined() || civilian.getHP() < 1000) {
                targetsToClear.remove(civilianEntity);
                continue;
            }
//            if (!targetsToClear.contains(civilianEntity)) {
            StandardEntity positionEntity = worldInfo.getPosition(civilian);
            if (!(positionEntity instanceof Refuge) && (positionEntity instanceof Building)) {
                Building building = (Building) positionEntity;

                if (worldInfo.getChanged().getChangedEntities().contains(civilian.getID())) {
                    for (EntityID neighbourId : building.getNeighbours()) {
                        StandardEntity neighbour = this.worldInfo.getEntity(neighbourId);
                        if (neighbour instanceof Road) {
                            if (!this.doneTasks.contains(neighbour.getID())) {
                                this.coincidentalRoads.add(neighbourId);
                            }
                        }
                    }
                } else {

                    if (clusterConvexPolygon != null && (clusterConvexPolygon.contains(building.getX(), building.getY())
                            || Util.distance(clusterConvexPolygon, worldInfo.getLocation(building.getID())) < scenarioInfo.getPerceptionLosMaxDistance()))
                        for (EntityID neighbourId : building.getNeighbours()) {
                            StandardEntity neighbour = this.worldInfo.getEntity(neighbourId);
                            if (neighbour instanceof Road) {
                                if (!this.doneTasks.contains(neighbour.getID())) {
                                    this.priorityRoads.add(neighbourId);
                                    this.targetAreas.add(neighbourId);
                                }
                            }
                        }
                }

//                }
            }
        }
    }


    private void addBlockedAgentsToTargets() {
        List<StandardEntity> agents = new ArrayList<>(
                worldInfo.getEntitiesOfType(
                        StandardEntityURN.FIRE_BRIGADE,
                        StandardEntityURN.AMBULANCE_TEAM
                ));
        for (StandardEntity agent : agents) {
            if (worldInfo.getChanged().getChangedEntities().contains(agent.getID())) {
                for (EntityID changedEntityId : worldInfo.getChanged().getChangedEntities()) {
                    StandardEntity changedEntity = worldInfo.getEntity(changedEntityId);
                    if (changedEntity instanceof Blockade) {
                        Pair<Integer, Integer> agentLocation = worldInfo.getLocation(agent.getID());
                        if (isInsideBlockage(agent, (Blockade) changedEntity) || isBlockadeClose((Blockade) changedEntity, agentLocation, 1000)) {
                            if (amIResponsibleFor(agent)) {
                                StandardEntity blockageRoadPosition = worldInfo.getPosition(changedEntityId);
                                if (blockageRoadPosition != null && !this.doneTasks.contains(blockageRoadPosition.getID())) {
                                    this.coincidentalRoads.add(blockageRoadPosition.getID());
//                                this.targetAreas.add(blockageRoadPosition.getID());
                                }
                            }
                        }
                    }
                }

            }
        }

    }

    private boolean amIResponsibleFor(StandardEntity agent) {
        PoliceForce policeForce;
        List<StandardEntity> visiblePoliceForces = new ArrayList<>();
        worldInfo.getChanged().getChangedEntities().forEach(entityId -> {
            StandardEntity entity = worldInfo.getEntity(entityId);
            if (entity instanceof PoliceForce) {
                visiblePoliceForces.add(entity);
            }
        });
        for (StandardEntity entity : visiblePoliceForces) {
            policeForce = (PoliceForce) entity;
            if (policeForce.isBuriednessDefined() && policeForce.getBuriedness() == 0) {
                if (worldInfo.getDistance(policeForce.getID(), agent.getID()) < scenarioInfo.getPerceptionLosMaxDistance()) {
                    if (policeForce.getID().getValue() > agentInfo.getID().getValue()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void addBlockedRefugesToTargets() {
        List<StandardEntity> refuges = new ArrayList<>(
                worldInfo.getEntitiesOfType(
                        StandardEntityURN.REFUGE
                ));
        for (StandardEntity refuge : refuges) {

            Building refugeBuilding = (Building) refuge;
            if (worldInfo.getChanged().getChangedEntities().contains(refuge.getID())) {

                for (EntityID neighbourId : refugeBuilding.getNeighbours()) {
                    StandardEntity neighbour = this.worldInfo.getEntity(neighbourId);
                    if (neighbour instanceof Road) {
                        if (worldInfo.getDistance(agentInfo.getPosition(), neighbour.getID()) < 20000) {
                            if (!this.doneTasks.contains(neighbour.getID())) {
                                this.coincidentalRoads.add(neighbour.getID());
//                                this.targetAreas.add(neighbour.getID());
                            }
                        }
                    }
                }
            }
        }
    }

    private RoadDetector getRoadDetector(EntityID positionID, Set<EntityID> entityIDSet) {

        this.pathPlanning.setFrom(positionID);
        this.pathPlanning.setDestination(entityIDSet);
        List<EntityID> path = this.pathPlanning.calc().getResult();
        if (path != null && path.size() > 0) {
            this.result = path.get(path.size() - 1);
        }
        return this;
    }

    private boolean isBlockadeClose(Blockade changedEntity, Pair<Integer, Integer> agentLocation, int range) {
        return Util.findDistanceTo(changedEntity, agentLocation.first(), agentLocation.second()) < range;
    }

    private boolean isInsideBlockage(StandardEntity agent, Blockade blockade) {
        Pair<Integer, Integer> agentLocation = worldInfo.getLocation(agent.getID());

        if (blockade.isApexesDefined() && agentLocation != null) {
            if (blockade.getShape().contains(agentLocation.first(), agentLocation.second())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public EntityID getTarget() {
        return this.result;
    }

    @Override
    public RoadDetector precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        this.pathPlanning.precompute(precomputeData);
        return this;
    }

    @Override
    public RoadDetector resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
        this.pathPlanning.resume(precomputeData);
        fillTargets();
        return this;
    }

    private void fillTargets() {

        this.targetAreas = new HashSet<>();
        this.doneTasks = new HashSet<>();
        this.targetAreasMap = new HashMap<>();
        this.priorityRoads = new HashSet<>();

        Collection<StandardEntity> clusterEntities = null;
        if (this.clustering != null) {
            int clusterIndex = this.clustering.getClusterIndex(this.agentInfo.getID());
            if (clusterIndex != -1) {
                clusterEntities = this.clustering.getClusterEntities(clusterIndex);
                createConvexHull(clusterEntities);
            }

        }

        if (this.clusterConvexPolygon != null && clusterEntities != null) {
            for (StandardEntity entity : worldInfo.getEntitiesOfType(REFUGE, FIRE_BRIGADE)) {
                if (Util.distance(this.clusterConvexPolygon, worldInfo.getLocation(entity), true) < scenarioInfo.getPerceptionLosMaxDistance()) {
                    clusterEntities.add(entity);
                }
            }
        }


        StandardEntityURN entityURN = null;
        if (clusterEntities != null) {
            for (StandardEntity e : clusterEntities) {
                if (e instanceof FireBrigade || e instanceof AmbulanceTeam) {
                    StandardEntity position = worldInfo.getPosition(e.getID());
                    if (position != null) {
                        if (position instanceof Road) {
                            this.priorityRoads.add(position.getID());
                            this.targetAreas.add(position.getID());
                            EntityID id = position.getID();
                            if (e instanceof FireBrigade) {
                                entityURN = StandardEntityURN.FIRE_BRIGADE;

                            } else if (e instanceof AmbulanceTeam) {
                                entityURN = StandardEntityURN.AMBULANCE_TEAM;
                            }
                            addToTargetMap(entityURN, id);


                        } else {
                            for (EntityID id : ((Building) position).getNeighbours()) {
                                StandardEntity neighbour = this.worldInfo.getEntity(id);
                                if (neighbour instanceof Road) {
                                    this.priorityRoads.add(id);
                                    this.targetAreas.add(id);
                                    if (e instanceof FireBrigade) {
                                        entityURN = StandardEntityURN.FIRE_BRIGADE;

                                    } else if (e instanceof AmbulanceTeam) {
                                        entityURN = StandardEntityURN.AMBULANCE_TEAM;
                                    }
                                    addToTargetMap(entityURN, id);
                                }
                            }
                        }
                    }
                } else if (e instanceof Refuge) {
                    for (EntityID id : ((Building) e).getNeighbours()) {
                        StandardEntity neighbour = this.worldInfo.getEntity(id);
                        if (neighbour instanceof Road) {
                            this.priorityRoads.add(id);
                            this.targetAreas.add(id);
                            addToTargetMap(StandardEntityURN.REFUGE, id);
                        }
                    }
                } else if (e instanceof Building) {
                    for (EntityID id : ((Building) e).getNeighbours()) {
                        StandardEntity neighbour = this.worldInfo.getEntity(id);
                        if (neighbour instanceof Road) {
                            this.targetAreas.add(id);
                        }
                    }
                } else if (e instanceof Road) {
                    this.targetAreas.add(e.getID());
                }
            }
        } else {
            //in case of null cluster
            for (StandardEntity e : this.worldInfo.getEntitiesOfType(REFUGE, BUILDING, GAS_STATION)) {
                for (EntityID id : ((Building) e).getNeighbours()) {
                    StandardEntity neighbour = this.worldInfo.getEntity(id);
                    if (neighbour instanceof Road) {
                        this.targetAreas.add(id);
                        if (e instanceof Refuge) {
                            this.priorityRoads.add(id);
                        }
                    }
                }
            }
        }
    }


    private void createConvexHull(Collection<StandardEntity> clusterEntities) {
        ConvexHull convexHull = new ConvexHull();


        for (StandardEntity entity : clusterEntities) {

            if (entity instanceof Building) {
                Building building = (Building) entity;
                for (int i = 0; i < building.getApexList().length; i += 2) {
                    convexHull.addPoint(building.getApexList()[i],
                            building.getApexList()[i + 1]);
                }
            }
        }
//        for (Road road : roads) {
//            for (int i = 0; i < road.getApexList().length; i += 2) {
//                convexHull.addPoint(road.getApexList()[i], road.getApexList()[i + 1]);
//            }
//        }

        this.clusterConvexPolygon = convexHull.convex();

    }


    private void addToTargetMap(StandardEntityURN entityURN, EntityID id) {
        Set<EntityID> idSet = this.targetAreasMap.get(entityURN);
        if (idSet == null) {
            idSet = new HashSet<>();
        }
        idSet.add(id);
        this.targetAreasMap.put(entityURN, idSet);
    }

    @Override
    public RoadDetector preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }
        this.pathPlanning.preparate();
        fillTargets();
        return this;
    }

    @Override
    public RoadDetector updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);


        if (MrlPersonalData.DEBUG_MODE) {
            ArrayList<Polygon> data = new ArrayList<>();
            data.add(this.clusterConvexPolygon);
            VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "ClusterConvexPolygon", data);
        }

        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.pathPlanning.updateInfo(messageManager);
        if (this.result != null) {
            if (worldInfo.getChanged().getChangedEntities().contains(this.result)) {
                StandardEntity entity = this.worldInfo.getEntity(this.result);
                if (entity instanceof Building) {
                    this.result = null;
                } else if (entity instanceof Road) {
                    Road road = (Road) entity;
                    if ((!road.isBlockadesDefined() || road.getBlockades().isEmpty()) && agentInfo.getPosition().equals(road.getID())) {
                        this.doneTasks.add(this.result);
                        this.targetAreas.remove(this.result);
                        this.result = null;
                    }
                }
            }
        }

        worldInfo.getChanged().getChangedEntities().forEach(changedId -> {
            StandardEntity entity = worldInfo.getEntity(changedId);
            if (entity instanceof Building) {
                Building building = (Building) worldInfo.getEntity(changedId);
                if (building.isFierynessDefined() && building.getFieryness() > 0 /*|| building.isTemperatureDefined() && building.getTemperature() > 0*/) {
                    BuildingProperty buildingProperty = sentBuildingMap.get(changedId);
                    if (buildingProperty == null || buildingProperty.getFieryness() != building.getFieryness() || buildingProperty.getFieryness() == 1) {
//                        printDebugMessage("burningBuilding: " + building.getID());
                        messageManager.addMessage(new MessageBuilding(true, building));
                        messageManager.addMessage(new MessageBuilding(false, building));
                        sentBuildingMap.put(changedId, new BuildingProperty(building));
                    }
                }
            } else if (entity instanceof Civilian) {
                Civilian civilian = (Civilian) entity;
                if ((civilian.isHPDefined() && civilian.getHP() > 1000 && civilian.isDamageDefined() && civilian.getDamage() > 0)
                        || ((civilian.isPositionDefined() && !(worldInfo.getEntity(civilian.getPosition()) instanceof Refuge))
                        && (worldInfo.getEntity(civilian.getPosition()) instanceof Building))) {
                    messageManager.addMessage(new MessageCivilian(true, civilian));
                    messageManager.addMessage(new MessageCivilian(false, civilian));
//                    System.out.println("Saw civilian: " + agentInfo.getTime() + " " + agentInfo.getID() + " --> " + civilian.getID());
                    StandardEntity target = this.worldInfo.getPosition(civilian.getID());
                    if (target instanceof Building) {
                        if (isInMyTerritoryOrCloseToIt(target)) {
                            for (EntityID id : ((Building) target).getNeighbours()) {
                                StandardEntity neighbour = this.worldInfo.getEntity(id);
                                if (neighbour instanceof Road) {
                                   System.out.println("SAW----Roads to Clear for civilian: " + agentInfo.getTime() + " agentId: " + agentInfo.getID() + " civId: " + civilian.getID() + " roadId: " + neighbour.getID());
                                    this.priorityRoads.add(id);
                                    this.targetAreas.add(id);
                                }
                            }
                            List<Entrance> entrances = worldHelper.getMrlBuilding(target.getID()).getEntrances();
                            if (entrances != null && !entrances.isEmpty()) {
                                entrances.forEach(entrance -> {
                                    this.priorityRoads.add(entrance.getID());
                                    this.targetAreas.add(entrance.getID());
                                });
                            }
                        }
                    }
                }

            }
        });


        Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
        for (CommunicationMessage message : messageManager.getReceivedMessageList()) {
            Class<? extends CommunicationMessage> messageClass = message.getClass();
            if (messageClass == MessageAmbulanceTeam.class) {
                this.reflectMessage((MessageAmbulanceTeam) message);
            } else if (messageClass == MessageFireBrigade.class) {
                this.reflectMessage((MessageFireBrigade) message);
            } else if (messageClass == MessageRoad.class) {
                this.reflectMessage((MessageRoad) message, changedEntities);
            } else if (messageClass == MessagePoliceForce.class) {
                this.reflectMessage((MessagePoliceForce) message);
            } else if (messageClass == CommandPolice.class) {
                this.reflectMessage((CommandPolice) message);
            } else if (messageClass == MessageCivilian.class) {
                MessageCivilian mc = (MessageCivilian) message;
                if (!changedEntities.contains(mc.getAgentID())) {
                    MessageUtil.reflectMessage(this.worldInfo, mc);
//                    System.out.println("received Civilian message: " + agentInfo.getTime() + " agentId: " + agentInfo.getID() + " civId: " + mc.getAgentID());
                    StandardEntity target = this.worldInfo.getPosition(mc.getAgentID());
                    if (isInMyTerritoryOrCloseToIt(target)) {
                        if (target instanceof Building) {
                            for (EntityID id : ((Building) target).getNeighbours()) {
                                StandardEntity neighbour = this.worldInfo.getEntity(id);
                                if (neighbour instanceof Road) {
//                                    System.out.println("Roads to Clear for civilian: " + agentInfo.getTime() + " agentId: " + agentInfo.getID() + " civId: " + mc.getAgentID() + " roadId: " + neighbour.getID());
                                    this.priorityRoads.add(id);
                                    this.targetAreas.add(id);
                                }
                            }
                            List<Entrance> entrances = worldHelper.getMrlBuilding(target.getID()).getEntrances();
                            if (entrances != null && !entrances.isEmpty()) {
                                entrances.forEach(entrance -> {
                                    this.priorityRoads.add(entrance.getID());
                                    this.targetAreas.add(entrance.getID());
                                });
                            }

                        }
                    }


                }
//                this.sentTimeMap.put(mc.getAgentID(), time + this.sendingAvoidTimeReceived);
            }
        }
        for (EntityID id : this.worldInfo.getChanged().getChangedEntities()) {
            StandardEntity entity = this.worldInfo.getEntity(id);
            if (entity instanceof Road) {
                Road road = (Road) entity;
                if (!road.isBlockadesDefined() || road.getBlockades().isEmpty()) {
                    this.doneTasks.add(id);
                    this.targetAreas.remove(id);
                }
            }
        }
        return this;
    }

    private boolean isInMyTerritoryOrCloseToIt(StandardEntity positionEntity) {

        if (this.clusterConvexPolygon != null) {
            if (Util.distance(this.clusterConvexPolygon, worldInfo.getLocation(positionEntity), true) < scenarioInfo.getPerceptionLosMaxDistance()) {
                return true;
            }
        }
        return false;
    }

    private void reflectMessage(MessageRoad messageRoad, Collection<EntityID> changedEntities) {
        if (messageRoad.isBlockadeDefined() && !changedEntities.contains(messageRoad.getBlockadeID())) {
            MessageUtil.reflectMessage(this.worldInfo, messageRoad);
        }
        if (messageRoad.isPassable()) {
            this.doneTasks.add(messageRoad.getRoadID());
            this.targetAreas.remove(messageRoad.getRoadID());
        }
    }

    private void reflectMessage(MessageAmbulanceTeam messageAmbulanceTeam) {
        if (messageAmbulanceTeam.getPosition() == null) {
            return;
        }
        if (messageAmbulanceTeam.getAction() == MessageAmbulanceTeam.ACTION_RESCUE) {
            StandardEntity position = this.worldInfo.getEntity(messageAmbulanceTeam.getPosition());
            if (position != null && position instanceof Building) {
                this.doneTasks.addAll(((Building) position).getNeighbours());
                this.targetAreas.removeAll(((Building) position).getNeighbours());
            }
        } else if (messageAmbulanceTeam.getAction() == MessageAmbulanceTeam.ACTION_LOAD) {
            StandardEntity position = this.worldInfo.getEntity(messageAmbulanceTeam.getPosition());
            if (position != null && position instanceof Building) {
                this.doneTasks.addAll(((Building) position).getNeighbours());
                this.targetAreas.removeAll(((Building) position).getNeighbours());
            }
        } else if (messageAmbulanceTeam.getAction() == MessageAmbulanceTeam.ACTION_MOVE) {
            if (messageAmbulanceTeam.getTargetID() == null) {
                return;
            }
            StandardEntity target = this.worldInfo.getEntity(messageAmbulanceTeam.getTargetID());
//            if (target instanceof Building) {
//                for (EntityID id : ((Building) target).getNeighbours()) {
//                    StandardEntity neighbour = this.worldInfo.getEntity(id);
//                    if (neighbour instanceof Road) {
//                        this.priorityRoads.add(id);
//                    }
//                }
//            } else if (target instanceof Human) {
//                Human human = (Human) target;
//                if (human.isPositionDefined()) {
//                    StandardEntity position = this.worldInfo.getPosition(human);
//                    if (position instanceof Building) {
//                        for (EntityID id : ((Building) position).getNeighbours()) {
//                            StandardEntity neighbour = this.worldInfo.getEntity(id);
//                            if (neighbour instanceof Road) {
//                                this.priorityRoads.add(id);
//                            }
//                        }
//                    }
//                }
//            }
        }
    }

    private void reflectMessage(MessageFireBrigade messageFireBrigade) {
        if (messageFireBrigade.getTargetID() == null) {
            return;
        }
//        if (messageFireBrigade.getAction() == MessageFireBrigade.ACTION_REFILL) {
//            StandardEntity target = this.worldInfo.getEntity(messageFireBrigade.getTargetID());
//            if (target instanceof Building) {
//                for (EntityID id : ((Building) target).getNeighbours()) {
//                    StandardEntity neighbour = this.worldInfo.getEntity(id);
//                    if (neighbour instanceof Road) {
//                        this.priorityRoads.add(id);
//                    }
//                }
//            } else if (target.getStandardURN() == HYDRANT) {
//                this.priorityRoads.add(target.getID());
//                this.targetAreas.add(target.getID());
//            }
//        }
    }

    private void reflectMessage(MessagePoliceForce messagePoliceForce) {
        if (messagePoliceForce.getAction() == MessagePoliceForce.ACTION_CLEAR) {
            if (messagePoliceForce.getAgentID().getValue() != this.agentInfo.getID().getValue()) {
                if (messagePoliceForce.isTargetDefined()) {
                    EntityID targetID = messagePoliceForce.getTargetID();
                    if (targetID == null) {
                        return;
                    }
                    StandardEntity entity = this.worldInfo.getEntity(targetID);
                    if (entity == null) {
                        return;
                    }

                    if (entity instanceof Area) {
                        this.targetAreas.remove(targetID);
                        this.doneTasks.add(targetID);
                        if (this.result != null && this.result.getValue() == targetID.getValue()) {
                            if (this.agentInfo.getID().getValue() < messagePoliceForce.getAgentID().getValue()) {
                                this.result = null;
                            }
                        }
                    } else if (entity.getStandardURN() == BLOCKADE) {
                        EntityID position = ((Blockade) entity).getPosition();
                        this.targetAreas.remove(position);
                        this.doneTasks.add(position);
                        if (this.result != null && this.result.getValue() == position.getValue()) {
                            if (this.agentInfo.getID().getValue() < messagePoliceForce.getAgentID().getValue()) {
                                this.result = null;
                            }
                        }
                    }

                }
            }
        }
    }

    private void reflectMessage(CommandPolice commandPolice) {
        boolean flag = false;
        if (commandPolice.isToIDDefined() && this.agentInfo.getID().getValue() == commandPolice.getToID().getValue()) {
            flag = true;
        } else if (commandPolice.isBroadcast()) {
            flag = true;
        }
        if (flag && commandPolice.getAction() == CommandPolice.ACTION_CLEAR) {
            if (commandPolice.getTargetID() == null) {
                return;
            }

            //TODO: @MRL Check if this target is in my cluster

            StandardEntity target = this.worldInfo.getEntity(commandPolice.getTargetID());
//            if (target instanceof Area) {
//                this.priorityRoads.add(target.getID());
//                this.targetAreas.add(target.getID());
//            } else if (target.getStandardURN() == BLOCKADE) {
//                Blockade blockade = (Blockade) target;
//                if (blockade.isPositionDefined()) {
//                    this.priorityRoads.add(blockade.getPosition());
//                    this.targetAreas.add(blockade.getPosition());
//                }
//            }
        }
    }

    protected void printDebugMessage(String msg) {
        ConsoleOutput.error("Agent:" + agentInfo.getID() + " Time:" + agentInfo.getTime() + " " + msg);
    }

}
