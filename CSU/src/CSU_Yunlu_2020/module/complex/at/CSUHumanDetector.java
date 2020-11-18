package CSU_Yunlu_2020.module.complex.at;

import CSU_Yunlu_2020.CSUConstants;
import CSU_Yunlu_2020.LogHelper;
import CSU_Yunlu_2020.debugger.DebugHelper;
import CSU_Yunlu_2020.util.ambulancehelper.CSUBuilding;
import CSU_Yunlu_2020.util.ambulancehelper.CSUDistanceSorter;
import CSU_Yunlu_2020.util.ambulancehelper.CSUHurtHumanClassifier;
import CSU_Yunlu_2020.util.ambulancehelper.CSUSelectorTargetByDis;
import CSU_Yunlu_2020.world.CSUWorldHelper;
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
    private Set<EntityID> deadHumanSet;
    private Set<EntityID> visited; // 暂时没更新，也不知道会不会用上
    private int savedTime = 0;
    private LogHelper logHelper;
    private Map<EntityID,StandardEntity> invalidHumanPosition;
    private CSUWorldHelper world;

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
        this.invalidHumanPosition = new HashMap<>();
        this.deadHumanSet = new HashSet<>();
//        this.availableTarget = new HashSet<>();
//        targetSelector = this.moduleManager.getModule(SampleModuleKey.AMBULANCE_MODULE_TARGET_SELECTOR, "CSU_SelectorTargetByDis");
        targetSelector = new CSUSelectorTargetByDis(ai, wi, si, moduleManager, developData);
        logHelper = new LogHelper("at_log/detector",agentInfo,"CSUHumanDetector");
        world = moduleManager.getModule("WorldHelper.Default", CSUConstants.WORLD_HELPER_DEFAULT);


        initClassifier();//init CSU_HurtHumanClassifier
    }

    @Override
    public HumanDetector calc() {
        logHelper.writeAndFlush("==============================calc start=============================");
        //11.10
        //如果有result，坚持。如果不能到或者着火，挂起result。10s释放
        if(this.result != null){
            if(isReachable(this.result)){//目标可到达
                if(needToChange(result)){//目标在避难所里或者目标位置燃烧 or human died
                    logHelper.writeAndFlush("当前目标("+result+")可到达但是需要更换,挂起");
                    hangUp(this.result);
                }else{
                    if(isTargetPositionValid()){
                        logHelper.writeAndFlush("保持当前目标:"+result);
                        logHelper.writeAndFlush("==============================calc   end=============================");
                        return this;
                    }else{
                        logHelper.writeAndFlush("当前目标:"+result+"位置信息失效");
                        logHelper.writeAndFlush("==============================calc   end=============================");
                        invalidHumanPosition.put(result,worldInfo.getPosition(result));
                    }
                }
            }else{
                logHelper.writeAndFlush("当前目标("+result+")不可到达,挂起");
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

        this.result = findHumanToRescue();
        visualDebug();
        logHelper.writeAndFlush("确定新目标:"+result);
        logHelper.writeAndFlush("==============================calc   end=============================");
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

    //去掉building（由search来确定），去掉在refuge的human
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
        StandardEntity entity = worldInfo.getEntity(result);
        Human human = (Human)entity;
        if(human.isHPDefined() && human.getHP() <= 1){
            deadHumanSet.add(human.getID());
            return true;
        }
        if(entity instanceof Civilian){
            Civilian civ = (Civilian) entity;
            logHelper.writeAndFlush("当前目标("+civ.getID()+")位置定义:"+civ.isPositionDefined()+",位置:"+civ.getPosition());
            //如果被别人背走
            if(area instanceof AmbulanceTeam){
                logHelper.writeAndFlush("当前目标("+civ.getID()+")已经被别的at("+area+")背走");
                return true;
            }
            if(area instanceof Refuge) {
                logHelper.writeAndFlush("当前目标("+civ.getID()+")已经在refuge里了");
                if (CSUConstants.DEBUG_AT_SEARCH) {
                    System.out.println("[第"+agentInfo.getTime()+"回合]   "+agentInfo.getID()+":当前目标在refugee里，换目标");
                }
                return true;
            }
            if(area instanceof Building){
                Building building = (Building) area;
                if(building.isFierynessDefined() && building.getFieryness()>0 && building.getFieryness()<4){
                    logHelper.writeAndFlush("当前目标("+civ.getID()+")所在位置"+area+"着火了");
                    if(CSUConstants.DEBUG_AT_SEARCH){
                        System.out.println("[第"+agentInfo.getTime()+"回合]   "+agentInfo.getID()+":当前目标在着火建筑里，换目标");
                    }
                    return true;
                }
            }
        }else{
            if(human.isBuriednessDefined()){
                return human.getBuriedness() <= 0;
            }
        }

        return false;
    }

    //
    private EntityID findHumanToRescue(){
        logHelper.writeAndFlush("找人救");
        List<Human> targets = new ArrayList<>();
        List<Human> exclude = new ArrayList<>();



//        //特判：如果在建筑里，里面有人就救
        EntityID agentPositionID = agentInfo.getPosition();
        StandardEntity standardEntity = worldInfo.getEntity(agentPositionID);
        if(standardEntity instanceof Building){
            Building building = (Building)standardEntity;
            if(building.isBrokennessDefined() && building.getBrokenness() > 0){
                for(EntityID id : worldInfo.getChanged().getChangedEntities()){
                    StandardEntity e = worldInfo.getEntity(id);
                    if(e instanceof Civilian){
                        if(((Civilian) e).getPosition().equals(agentPositionID)){
                            logHelper.writeAndFlush("chose because someone("+id+") in now building");
                            return id;
                        }
                    }
                }
            }
        }
//        //特判：如果在建筑里，里面有人就救
        if(world.isMapSmall()){
            //旧版 (aggregate version)
            logHelper.writeAndFlush("small map, use aggregate version.");
            for (StandardEntity next : worldInfo.getEntitiesOfType(
                    StandardEntityURN.CIVILIAN,
                    StandardEntityURN.FIRE_BRIGADE,
                    StandardEntityURN.POLICE_FORCE,
                    StandardEntityURN.AMBULANCE_TEAM)
            ) {
                Human h = (Human) next;
                if (agentInfo.getID().equals(h.getID())) {
                    continue;
                }

                //被埋的所有都要救
                if (h.isHPDefined()
                        && h.isBuriednessDefined()
                        && h.isPositionDefined()
                        && h.getHP() > 0
                        && h.getBuriedness() > 0) {
//                logHelper.writeAndFlush("targets添加"+h+"，因为被埋了且位置已知且HP大于0");
                    targets.add(h);
                    //掉血的平民要救
                }else if(h.isHPDefined() && h.getHP() > 0 && h.isDamageDefined()
                        && h.isPositionDefined() && h.getDamage() > 0 && h instanceof Civilian){
//                logHelper.writeAndFlush("targets添加"+h+"，因为damage大于0且位置已知且HP大于0");
                    targets.add(h);
                }else{
                    exclude.add(h);
                }
            }
            targets.removeIf(human -> invalidHumanPosition.keySet().contains(human.getID()));
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
            });//去掉所在位置着火的目标
            targets.removeIf(human -> deadHumanSet.contains(human.getID()));
            targets.removeIf(human -> {//给背走了的也去掉
                StandardEntity area = worldInfo.getPosition(human.getID());
                return area instanceof AmbulanceTeam;
            });
        }else{
            //尝试版11.15(separate version)
            logHelper.writeAndFlush("large or medium map, use separate version.");
            Set<EntityID> set = worldInfo.getChanged().getChangedEntities();
            logHelper.writeAndFlush("changedEntity:"+set);
            for (EntityID id : set) {
                StandardEntity entity = worldInfo.getEntity(id);
                if(entity == null){
//                    System.out.println("眼见不一定为实（abcd1234）!"); 
                    continue;
                }
                if(!(entity instanceof Human)){
                    continue;
                }
                Human h = (Human) entity;
                if(!(h instanceof Civilian) && !(h instanceof FireBrigade)
                        &&!(h instanceof PoliceForce) &&!(h instanceof AmbulanceTeam)){
                    continue;
                }
                if (agentInfo.getID().equals(h.getID())) {
                    continue;
                }

                logHelper.writeAndFlush(h+"isHP:"+h.isHPDefined()+",isBur:"+
                        h.isBuriednessDefined()+",isPos:"+h.isPositionDefined()+",HP:"
                        +h.getHP()+",Buriedness:"+h.getBuriedness()+",isDam:"+h.isDamageDefined()
                        +",damage:"+h.getDamage()+"isCiv:"+(h instanceof Civilian));
                //被埋的所有都要救
                if (h.isHPDefined()
                        && h.isBuriednessDefined()
                        && h.isPositionDefined()
                        && h.getHP() > 0
                        && h.getBuriedness() > 0) {
//                logHelper.writeAndFlush("targets添加"+h+"，因为被埋了且位置已知且HP大于0");
                    targets.add(h);
                    //掉血的平民要救
                }else if(h.isHPDefined() && h.getHP() > 0 && h.isDamageDefined()
                        && h.isPositionDefined() && h.getDamage() > 0 && h instanceof Civilian){
//                logHelper.writeAndFlush("targets添加"+h+"，因为damage大于0且位置已知且HP大于0");
                    targets.add(h);
                }else{
                    exclude.add(h);
                }
            }
            targets.removeIf(human -> invalidHumanPosition.keySet().contains(human.getID()));
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
            });//去掉所在位置着火的目标
            targets.removeIf(human -> {//给背走了的也去掉
                StandardEntity area = worldInfo.getPosition(human.getID());
                return area instanceof AmbulanceTeam;
            });
        }
        targets.sort(new CSUDistanceSorter(this.worldInfo, this.agentInfo.getPositionArea()));
        logHelper.writeAndFlush("可以选择的target有"+targets);
        logHelper.writeAndFlush("exclude target:"+exclude);
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
        passHangUp(messageManager);
        updateInvalidPositionMap();
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

    private boolean isReachable(EntityID targetID){
        EntityID from = agentInfo.getPosition();
        StandardEntity entity = worldInfo.getPosition(targetID);
        logHelper.writeAndFlush("当前位置:"+agentInfo.getPosition()+",当前目标位置:"+entity);
        if(entity != null){
            EntityID destination = entity.getID();
            if(from.equals(destination)){
                return true;
            }
            List<EntityID> result = pathPlanning.setFrom(from).setDestination(destination).calc().getResult();
            return result != null;
        }else{
            return false;
        }
//        List<EntityID> result = pathPlanning.setFrom(from).setDestination(targetID).calc().getResult();
//        return result != null;
    }

    private List<EntityID> calcWay(EntityID destination){
        EntityID from = agentInfo.getPosition();
        if(from.equals(destination)){
            return new ArrayList<>();
        }
        return pathPlanning.setFrom(from).setDestination(destination).getResult();
    }

    private void passHangUp(MessageManager messageManager){
        Set<EntityID> toRemove = new HashSet<>();
        for (EntityID id : hangUpMap.keySet()){
            int i = hangUpMap.get(id)-1;
            if(i <= 0){
                toRemove.add(id);
            }else{
                hangUpMap.put(id, i);
            }
        }
        hangUpMap.keySet().removeAll(toRemove);
        //11.17 modified
        toRemove.clear();
        if(messageManager != null){
            for (CommunicationMessage communicationMessage : messageManager.getReceivedMessageList()){
                if (communicationMessage instanceof MessageCivilian) {
                    MessageCivilian mc = (MessageCivilian) communicationMessage;
                    StandardEntity positionEntity = worldInfo.getEntity(mc.getPosition());
                    if(positionEntity instanceof Building){
                        Building building = (Building) positionEntity;
                        if(!(building.isFierynessDefined() && building.getFieryness() > 0 && building.getFieryness() < 4)){
                            toRemove.add(mc.getAgentID());
                        }
                    }

                }
            }
        }
        hangUpMap.keySet().removeAll(toRemove);
    }

    private void hangUp(EntityID id){
        int postponeTime = rnd.nextInt(6) + 20;
        hangUpMap.put(id, postponeTime);
    }

    //走进目标所在建筑时，判断changed包不包括result，不包括说明已经被救走，加入位置改变列表。
    private boolean isTargetPositionValid(){
        StandardEntity entity = worldInfo.getPosition(result);
        if (agentInfo.getPosition().equals(entity.getID())){
//            Collection<StandardEntity> inSightIDs = worldInfo.getObjectsInRange(agentInfo.getID(),scenarioInfo.getPerceptionLosMaxDistance());
//            logHelper.writeAndFlush("在视线内的ID:"+inSightIDs);
            Set<EntityID> set = worldInfo.getChanged().getChangedEntities();
            logHelper.writeAndFlush("changedEntityIDs:"+set);
            return set.contains(result);
        }else{
            return true;
        }
    }

    private void updateInvalidPositionMap(){
        Set<EntityID> toRemove = new HashSet<>();
        for (EntityID target : invalidHumanPosition.keySet()){
            StandardEntity entity = worldInfo.getPosition(target);
            if(!entity.equals(invalidHumanPosition.get(target))){
                toRemove.add(target);
            }
        }
        invalidHumanPosition.keySet().removeAll(toRemove);
    }

    protected void printDebugMessage(String msg) {
        ConsoleOutput.error("Agent:" + agentInfo.getID() + " Time:" + agentInfo.getTime() + " " + msg);
    }


    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(10);
        list.add(9);
        list.sort(new Com());
        System.out.println(list);
    }

    static class Com implements Comparator<Integer>{

        @Override
        public int compare(Integer i1, Integer i2) {
            return i1-i2;
        }
    }

}
