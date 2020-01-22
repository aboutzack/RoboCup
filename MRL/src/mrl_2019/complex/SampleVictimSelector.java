package mrl_2019.complex;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.information.MessageBuilding;
import adf.agent.communication.standard.bundle.information.MessageCivilian;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.HumanDetector;
import adf.launcher.ConsoleOutput;
import com.mrl.debugger.remote.VDClient;
import mrl_2019.complex.firebrigade.BuildingProperty;
import mrl_2019.complex.firebrigade.DistanceBasedTargetSelector;
import mrl_2019.util.DistanceSorter;
import mrl_2019.util.Util;
import mrl_2019.viewer.MrlPersonalData;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class SampleVictimSelector extends HumanDetector {

    private EntityID result;
    private int clusterIndex;
    private DistanceBasedTargetSelector targetSelector;
    private VictimClassifier victimClassifier;
    private Clustering clustering;
    private Map<EntityID, BuildingProperty> sentBuildingMap;
    private Map<EntityID, Integer> sentTimeMap;
    private PathPlanning pathPlanning;
    private Map<StandardEntity, Integer> blockedVictims;
    private Random rnd = new Random(System.currentTimeMillis());


    public SampleVictimSelector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);

        switch (scenarioInfo.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("SampleRoadDetector.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("SampleVictimSelector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("SampleRoadDetector.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("SampleVictimSelector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("SampleRoadDetector.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("SampleVictimSelector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
        }

        this.blockedVictims = new HashMap<>();
        this.clusterIndex = -1;
        this.sentBuildingMap = new HashMap<>();
        this.sentTimeMap = new HashMap<>();
//        targetSelector = this.moduleManager.getModule(SampleModuleKey.AMBULANCE_MODULE_TARGET_SELECTOR, "DistanceBasedTargetSelector");
        targetSelector = new DistanceBasedTargetSelector(ai, wi, si, moduleManager, developData);
        initClassifier();
    }

    @Override
    public HumanDetector calc() {
//        Clustering clustering = this.moduleManager.getModule(SampleModuleKey.AMBULANCE_MODULE_CLUSTERING);
        if (clustering == null) {
            this.result = this.failedClusteringCalc();
            return this;
        }
        if (this.clusterIndex == -1) {
            this.clusterIndex = clustering.getClusterIndex(this.agentInfo.getID());
        }
        Collection<StandardEntity> elements = clustering.getClusterEntities(this.clusterIndex);

        //todo send data to Buildings layer
        if (MrlPersonalData.DEBUG_MODE) {
            if (elements != null) {
                List<Integer> elementList = findElements(elements);
                VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlSampleBuildingsLayer", (Serializable) elementList);
            }
        }

        updateBlockedVictims();

        victimClassifier.updateGoodHumanList(worldInfo.getEntitiesOfType(
                StandardEntityURN.CIVILIAN));


        if (victimClassifier.getMyGoodHumans().isEmpty()) {
            victimClassifier.updateGoodHumanList(worldInfo.getEntitiesOfType(
                    StandardEntityURN.FIRE_BRIGADE,
                    StandardEntityURN.POLICE_FORCE,
                    StandardEntityURN.AMBULANCE_TEAM));
        }


        if (MrlPersonalData.DEBUG_MODE) {
            if (victimClassifier.getMyGoodHumans() != null) {
                List<Integer> elementList = Util.fetchIdValueFormElements(victimClassifier.getMyGoodHumans());
                VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlKnownVictimsLayer", (Serializable) elementList);
            }
        }

        for (int i = 0; i < 6; i++) {
            victimClassifier.getMyGoodHumans().removeAll(blockedVictims.keySet());
            result = targetSelector.nextTarget(victimClassifier.getMyGoodHumans());
            if (result != null) {
                StandardEntity position = worldInfo.getPosition(result);
                if (position != null) {
                    List<EntityID> path = pathPlanning.getResult(agentInfo.getPosition(), position.getID());
                    if (agentInfo.getPosition().equals(position.getID()) || path != null && !path.isEmpty()) {
                        return this;
                    }
                    int postponeTime = rnd.nextInt(6) + 5;
                    blockedVictims.put(worldInfo.getEntity(result), postponeTime);
//                    System.out.println("BLOCKED VICTIM: " + agentInfo.getTime() + " agent: " + agentInfo.getID() + " victim: " + result + " postpone: " + postponeTime);
                }
            }
        }


        return this;
    }

    private void updateBlockedVictims() {
        ArrayList<StandardEntity> toRemove = new ArrayList<StandardEntity>();
        int postponeTime;
        for (StandardEntity standardEntity : blockedVictims.keySet()) {
            postponeTime = blockedVictims.get(standardEntity);
            postponeTime--;
            if (postponeTime <= 0) {
                toRemove.add(standardEntity);
            } else {
                blockedVictims.put(standardEntity, postponeTime);
            }

        }
        blockedVictims.keySet().removeAll(toRemove);
    }

    private List<Integer> findElements(Collection<StandardEntity> elements) {
        return elements.stream().map(entity -> entity.getID().getValue()).collect(Collectors.toList());
    }

    private EntityID failedClusteringCalc() {
        List<Human> targets = new ArrayList<>();
        for (StandardEntity next : worldInfo.getEntitiesOfType(
                StandardEntityURN.CIVILIAN,
                StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE,
                StandardEntityURN.AMBULANCE_TEAM)
                ) {
            Human h = (Human) next;
            if (agentInfo.getID() == h.getID()) {
                continue;
            }
            if (h.isHPDefined()
                    && h.isBuriednessDefined()
                    && h.isDamageDefined()
                    && h.isPositionDefined()
                    && h.getHP() > 0
                    && (h.getBuriedness() > 0 || h.getDamage() > 0)) {
                targets.add(h);
            }
        }
        targets.sort(new DistanceSorter(this.worldInfo, this.agentInfo.getPositionArea()));
        return targets.isEmpty() ? null : targets.get(0).getID();
    }

    @Override
    public EntityID getTarget() {
        return this.result;
    }

    @Override
    public HumanDetector precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        this.clustering.precompute(precomputeData);
        return this;
    }

    @Override
    public HumanDetector resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        this.clustering.resume(precomputeData);
        return this;
    }

    @Override
    public HumanDetector preparate() {
        super.preparate();
        this.clustering.preparate();
        return this;
    }


    @Override
    public HumanDetector updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.clustering.updateInfo(messageManager);
        this.reflectMessage(messageManager);

        Set<StandardEntity> inBuildingAmbulances = new HashSet<>();
        worldInfo.getChanged().getChangedEntities().forEach(id -> {
            StandardEntity entity = worldInfo.getEntity(id);
            if (entity instanceof AmbulanceTeam) {
                StandardEntity position = worldInfo.getPosition(entity.getID());
                if (!entity.getID().equals(agentInfo.getID()) && position != null
                        && position instanceof Building
                        && position.getID().equals(agentInfo.getPosition())) {
                    inBuildingAmbulances.add(entity);
                }
            }
        });


        worldInfo.getChanged().getChangedEntities().forEach(id -> {
            StandardEntity entity = worldInfo.getEntity(id);
            if (entity instanceof Building) {
                Building building = (Building) worldInfo.getEntity(id);
                if (building.isFierynessDefined() && building.getFieryness() > 0 /*|| building.isTemperatureDefined() && building.getTemperature() > 0*/) {
                    BuildingProperty buildingProperty = sentBuildingMap.get(id);
                    if (buildingProperty == null || buildingProperty.getFieryness() != building.getFieryness()) {
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

                    if (inBuildingAmbulances.size() < 3) {
                        messageManager.addMessage(new MessageCivilian(true, civilian));
                        messageManager.addMessage(new MessageCivilian(false, civilian));
                    }
//                    System.out.println(" CIVILIAN_MESSAGE: " + agentInfo.getTime() + " " + agentInfo.getID() + " --> " + civilian.getID());
                }

            }
        });


        return this;


    }


    private void reflectMessage(MessageManager messageManager) {
        Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
        changedEntities.add(this.agentInfo.getID());
        int time = this.agentInfo.getTime();
        for (CommunicationMessage message : messageManager.getReceivedMessageList()) {
            Class<? extends CommunicationMessage> messageClass = message.getClass();
            if (messageClass == MessageCivilian.class) {
                MessageCivilian mc = (MessageCivilian) message;
                if (!changedEntities.contains(mc.getAgentID())) {
                    MessageUtil.reflectMessage(this.worldInfo, mc);
                }
//                this.sentTimeMap.put(mc.getAgentID(), time + this.sendingAvoidTimeReceived);
            }
        }
    }


    private void initClassifier() {
        victimClassifier = new VictimClassifier(worldInfo, agentInfo);
    }


    protected void printDebugMessage(String msg) {
        ConsoleOutput.error("Agent:" + agentInfo.getID() + " Time:" + agentInfo.getTime() + " " + msg);
    }

}
