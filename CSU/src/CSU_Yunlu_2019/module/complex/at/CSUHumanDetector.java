package CSU_Yunlu_2019.module.complex.at;

import CSU_Yunlu_2019.CSUConstants;
import CSU_Yunlu_2019.debugger.DebugHelper;
import CSU_Yunlu_2019.util.ambulancehelper.CSUBuilding;
import CSU_Yunlu_2019.util.ambulancehelper.CSUDistanceSorter;
import CSU_Yunlu_2019.util.ambulancehelper.CSUHurtHumanClassifier;
import CSU_Yunlu_2019.util.ambulancehelper.CSUSelectorTargetByDis;
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
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;
import java.util.stream.Collectors;

import static rescuecore2.standard.entities.StandardEntityURN.BUILDING;


public class CSUHumanDetector extends HumanDetector {

    private EntityID result;
    private int clusterIndex;

    private CSUSelectorTargetByDis targetSelector;
    private CSUHurtHumanClassifier CSU_HurtHumanClassifier;

    private Clustering clustering;

    private Map<EntityID, CSUBuilding> sentBuildingMap;
    private Map<EntityID, Integer> sentTimeMap;

    private Map<StandardEntity, Integer> blockedVictims;
    private PathPlanning pathPlanning;

    private Random rnd = new Random(System.currentTimeMillis());

//    private AgentInfo agentInfo;
//    private WorldInfo worldInfo;
//    private ScenarioInfo scenarioInfo;

    private EntityID lastPosition;
    private EntityID nowPosition;
    private List<EntityID> way;

    private Map<EntityID,Integer> hangUpMap;
    private Set<EntityID> visited;
    private int savedTime = 0;

//    private Set<EntityID> availableTarget;

    public CSUHumanDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);

        switch (scenarioInfo.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("RoadDetector.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("SampleHumanDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("RoadDetector.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("SampleHumanDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("RoadDetector.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("SampleHumanDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
        }

        this.blockedVictims = new HashMap<>();
        this.clusterIndex = -1;
        this.sentBuildingMap = new HashMap<>();
        this.sentTimeMap = new HashMap<>();
        this.agentInfo = ai;
        this.worldInfo = wi;
        this.scenarioInfo = si;
        this.hangUpMap = new HashMap<>();
        this.visited = new HashSet<>();
//        this.availableTarget = new HashSet<>();
//        targetSelector = this.moduleManager.getModule(SampleModuleKey.AMBULANCE_MODULE_TARGET_SELECTOR, "CSU_SelectorTargetByDis");
        targetSelector = new CSUSelectorTargetByDis(ai, wi, si, moduleManager, developData);
        initClassifier();//init CSU_HurtHumanClassifier
    }

    @Override
    public HumanDetector calc() {
        //11.10
        //如果有result，坚持。如果不能到或者着火，挂起result。10s释放
        if(this.result != null){
            if(isReachable(this.result)){
                if(needToChange(result)){
                    hangUp(this.result);
                }else{
                    return this;
                }
            }else{
                hangUp(this.result);
            }
        }
        //11.9
//        if(this.result != null && isReachable(this.result)){
//            return this;
//        }else if(this.result != null){
//            hangUp(this.result);
//        }
        //11.9
        passHangUp();
        this.result = findHumanToRescue();
        visualDebug();
        return this;

//        if (clustering == null) {
//            this.result = this.failedClusteringCalc();
//            if(noNeed()) this.result = null;
//            visualDebug();
//            return this;
//        }
//        if (this.clusterIndex == -1) {
//            this.clusterIndex = clustering.getClusterIndex(this.agentInfo.getID());
//        }
//        Collection<StandardEntity> elements = clustering.getClusterEntities(this.clusterIndex);

        //todo send data to Buildings layer


//        updateBlockedVictims();
//        passHangUp();
//
//        CSU_HurtHumanClassifier.updateGoodHumanList(worldInfo.getEntitiesOfType(
//                StandardEntityURN.CIVILIAN));
//
//
//        if (CSU_HurtHumanClassifier.getMyGoodHumans().isEmpty()) {
//            CSU_HurtHumanClassifier.updateGoodHumanList(worldInfo.getEntitiesOfType(
//                    StandardEntityURN.FIRE_BRIGADE,
//                    StandardEntityURN.POLICE_FORCE,
//                    StandardEntityURN.AMBULANCE_TEAM));
//        }
//
//        for (int i = 0; i < 6; i++) {
//            CSU_HurtHumanClassifier.getMyGoodHumans().removeAll(blockedVictims.keySet());
//            CSU_HurtHumanClassifier.getMyGoodHumans().removeIf(se -> hangUpMap.keySet().contains(se.getID()));
//
//            result = targetSelector.nextTarget(CSU_HurtHumanClassifier.getMyGoodHumans());
//            if (result != null) {
//                StandardEntity position = worldInfo.getPosition(result);
//                if (position != null) {
//                    List<EntityID> path = pathPlanning.getResult(agentInfo.getPosition(), position.getID());
//                    if (agentInfo.getPosition().equals(position.getID()) || path != null && !path.isEmpty()) {
//                        if(noNeed()) this.result = null;
//                        visualDebug();
//                        return this;
//                    }
//                    int postponeTime = rnd.nextInt(6) + 5;
//                    blockedVictims.put(worldInfo.getEntity(result), postponeTime);
////                    System.out.println("BLOCKED VICTIM: " + agentInfo.getTime() + " agent: " + agentInfo.getID() + " victim: " + result + " postpone: " + postponeTime);
//                }
//            }
//        }
//        if(noNeed()) this.result = null;
//        visualDebug();
//        return this;
    }

    //新增
    private boolean noNeed(){
        StandardEntity entity = worldInfo.getEntity(result);
        if(entity instanceof Area) return true;
        if(entity instanceof Human){
            Human human = (Human) entity;
            if(worldInfo.getEntity(human.getPosition()) instanceof Refuge){
                return true;
            }
            return false;
        }
        return  false;
    }

    private boolean noNeed(EntityID result){
        StandardEntity entity = worldInfo.getEntity(result);
        if(entity instanceof Area) return true;
        if(entity instanceof Human){
            Human human = (Human) entity;
            if(worldInfo.getEntity(human.getPosition()) instanceof Refuge){
                return true;
            }
            return false;
        }
        return  false;
    }

    private boolean needToChange(EntityID result){
        StandardEntity area = worldInfo.getPosition(result);
        if(area instanceof Refuge) {
            if (CSUConstants.DEBUG_AT_SEARCH) {
                System.out.println("[第"+agentInfo.getTime()+"回合]   "+agentInfo.getID()+":当前目标在refugee里，换目标");
            }
            return true;
        }

        if(area instanceof Building){
            Building building = (Building) area;
            if(building.isFierynessDefined() && building.getFieryness()>0 && building.getFieryness()<4){
                if(CSUConstants.DEBUG_AT_SEARCH){
                    System.out.println("[第"+agentInfo.getTime()+"回合]   "+agentInfo.getID()+":当前目标在着火建筑里，换目标");
                }
                return true;
            }
        }
        return false;
    }

    //
    private EntityID findHumanToRescue(){
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
//            if (h.isHPDefined()
//                    && h.isBuriednessDefined()
//                    && h.isDamageDefined()
//                    && h.isPositionDefined()
//                    && h.getHP() > 0
//                    && (h.getBuriedness() > 0 || h.getDamage() > 0)) {
//                targets.add(h);
//            }
            if (h.isHPDefined()
                    && h.isBuriednessDefined()
                    && h.isPositionDefined()
                    && h.getHP() > 0
                    && h.getBuriedness() > 0) {
                targets.add(h);
            }
        }
        targets.removeIf(human -> hangUpMap.keySet().contains(human.getID()));
        targets.removeIf(human -> noNeed(human.getID()));
        targets.removeIf(human -> visited.contains(human.getID()));
        targets.removeIf(human -> {
            StandardEntity area = worldInfo.getPosition(human);
            if(area.getStandardURN() == BUILDING){
                Building building = (Building) area;

                return building.isFierynessDefined()
                        && ((building.getFieryness() > 0 && building.getFieryness() < 4)
                        || building.getFieryness() == 8);
            }else{
                //只要有被埋，就去救
                return false;
            }
        });
        targets.sort(new CSUDistanceSorter(this.worldInfo, this.agentInfo.getPositionArea()));
        return targets.isEmpty() ? null : targets.get(0).getID();
    }

    private void visualDebug() {
        if (DebugHelper.DEBUG_MODE) {
            try {
                DebugHelper.drawDetectorTarget(worldInfo, agentInfo.getID(), result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        targets.removeIf(human -> hangUpMap.keySet().contains(human.getID()));
        targets.sort(new CSUDistanceSorter(this.worldInfo, this.agentInfo.getPositionArea()));
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
        lastPosition = nowPosition;
        nowPosition = agentInfo.getPosition();
        this.clustering.updateInfo(messageManager);
        this.reflectMessage(messageManager);
        preProcessChangedEntities(messageManager);
        return this;
    }

    /**
    * @Description: 根据changedEntities的信息进行通讯等操作
    * @Date: 2/28/20
    */
    private void preProcessChangedEntities(MessageManager messageManager) {
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
            } else if (entity instanceof Building) {
                Building building = (Building) worldInfo.getEntity(id);
                if (building.isFierynessDefined() && building.getFieryness() > 0 /*|| building.isTemperatureDefined() && building.getTemperature() > 0*/) {
                    CSUBuilding CSU_Building = sentBuildingMap.get(id);
                    if (CSU_Building == null || CSU_Building.getFireyness() != building.getFieryness()) {
                        messageManager.addMessage(new MessageBuilding(true, building));
                        messageManager.addMessage(new MessageBuilding(false, building));
                        sentBuildingMap.put(id, new CSUBuilding(building));
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
                }

            }
        });
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
        CSU_HurtHumanClassifier = new CSUHurtHumanClassifier(worldInfo, agentInfo);
    }

    private boolean isReachable(EntityID destination){
        EntityID from = agentInfo.getPosition();
        if(from.equals(destination)){
            return true;
        }
        List<EntityID> result = pathPlanning.setFrom(from).setDestination(destination).getResult();
        return result != null && !result.isEmpty();
    }

    private List<EntityID> calcWay(EntityID destination){
        EntityID from = agentInfo.getPosition();
        if(from.equals(destination)){
            return new ArrayList<>();
        }
        return pathPlanning.setFrom(from).setDestination(destination).getResult();
    }

    private void passHangUp(){
        Set<EntityID> toRemove = new HashSet<>();
        for(EntityID id : hangUpMap.keySet()){
            int i = hangUpMap.get(id)-1;
            if(i <= 0){
                toRemove.add(id);
            }else{
                hangUpMap.put(id, i);
            }
        }
        hangUpMap.keySet().removeAll(toRemove);
    }

    private void hangUp(EntityID id){
        int postponeTime = rnd.nextInt(6) + 15;
        hangUpMap.put(id, postponeTime);
    }

    protected void printDebugMessage(String msg) {
        ConsoleOutput.error("Agent:" + agentInfo.getID() + " Time:" + agentInfo.getTime() + " " + msg);
    }

}
