package CSU_Yunlu_2019.module.complex.at;

import CSU_Yunlu_2019.CSUConstants;
import CSU_Yunlu_2019.util.Util;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.messages.Command;
import rescuecore2.misc.Handy;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;
import static rescuecore2.standard.entities.StandardEntityURN.BLOCKADE;

/**
 * @author kyrieg
 */
public class CSUSearchRecorder {
    //所有建筑
    private Set<EntityID> allBuildings;
    //燃烧的建筑
    private Set<EntityID> burningBuildings;
    //发现的平民(不知道位置)
    private Set<EntityID> allCivilians;
    //听到声音的人
    private Set<EntityID> heardCivilians;
    //通讯类
    private MessageManager messageManager;
    //视线范围
    private Set<Civilian> visionCivilian;
    private Set<Building> visionBuilding;
    private Set<Blockade> visionBlockade;
    //优先级建筑
//    //有人的建筑
//    private Set<EntityID> firstClassBuildings;
//    //听到可能有人的建筑 todo 可以添加可能性排序
//    private Set<EntityID> secondClassBuildings;
//    //其他没搜过的建筑(聚类)
//    private Set<EntityID> thirdClassBuildings;
//    //其他没搜过的建筑（全局） //todo 尚未排除聚类
//    private Set<EntityID> forthClassBuildings;
//    //搜过的建筑
//    private Set<EntityID> fifthClassBuildings;

    private EntityID lastPosition;
    private EntityID nowPosition;
    private EntityID target;
    private int nowPriority;
    private int strategyType;
    private WorldInfo worldInfo;
    private AgentInfo agentInfo;
    private ScenarioInfo scenarioInfo;
    private int voiceRange;
    private int lastClusterIndex = -1;

    private Clustering clustering;
    private PathPlanning pathPlanning;
//    private HangUpSystem hangUpSystem;

    private ATBuildingSystem ATBS;
    private static CSUSearchRecorder me;
//todo 防止过多的人以同一建筑为目标

    public CSUSearchRecorder(AgentInfo ai, WorldInfo wi, ScenarioInfo si, Clustering clustering, PathPlanning pathPlanning){
        initializeBuilding();
        this.agentInfo = ai;
        this.worldInfo = wi;
        this.scenarioInfo = si;
        allBuildings = new HashSet<>();
        burningBuildings = new HashSet<>();
        allCivilians = new HashSet<>();
        heardCivilians = new HashSet<>();
        visionCivilian = new HashSet<>();
        visionBuilding = new HashSet<>();
        visionBlockade = new HashSet<>();
//        firstClassBuildings = new HashSet<>();
//        secondClassBuildings = new HashSet<>();
//        thirdClassBuildings = new HashSet<>();
//        forthClassBuildings = new HashSet<>();
//        fifthClassBuildings = new HashSet<>();
        this.voiceRange = scenarioInfo.getRawConfig().getIntValue("comms.channels.0.range");
        this.clustering = clustering;
        this.pathPlanning = pathPlanning;
        this.nowPriority = 0;
//        hangUpSystem = new HangUpSystem();
        ATBS = ATBuildingSystem.load(pathPlanning, agentInfo);
        ATBS.initialized(allBuildings);
    }

    public static void registerRecorder(CSUSearchRecorder searchRecorder){
        CSUSearchRecorder.me = searchRecorder;
    }

    public static CSUSearchRecorder load(){
        return me;
    }

    //modified
    private void initializeBuilding(){
        allBuildings.addAll(CSUSearchUtil.getBuildingIDs());
    }

    public int needToChangeTarget(){
        if(isStuck()){
            return CSUSearchUtil.STUCK;
        }
        if(isStationary()){
            return CSUSearchUtil.STATIONARY;
        }
        return CSUSearchUtil.NORMAL;
    }

    public boolean ChangeTarget(int message) {
        //todo 当前目标移出从前的队列，同时移入unreachableBuildings
        switch (message){
            case CSUSearchUtil.STUCK:{
                //todo 抄MRL
                //todo toID：用null不知道会怎么样，如果可能，补充为getNearby的PF。
                messageManager.addMessage(new CommandPolice(
                        true,
                        null,
                        agentInfo.getPosition(),
                        CommandPolice.ACTION_CLEAR));
                break;
            }
            case CSUSearchUtil.STATIONARY:{
                return decideBest();
            }
            default:{
                return changeTarget();
            }
        }
        CSUSearchUtil.debugOverall("ChangeTarget() go into unknown logic.(impossible)");
        return false;
    }

    //最优决策(忽略当前优先级)
    public boolean decideBest(){
        if(calcFirstClassTarget()){
            return true;
        }else if(calcSecondClassTarget()){
            return true;
        }else if(calcThirdClassTarget()){
            return true;
        }else {
            return calcForthClassTarget();
        }
    }

    public EntityID getTarget(){
        return this.target;
    }

    //todo
    public EntityID quickDecide(){
        return null;
    }

    //最优抢占(考虑优先级)
    public boolean changeTarget(){
        if(nowPriority <= CSUSearchUtil.FIRST_CLASS){
            if(calcFirstClassTarget()) {
                this.nowPriority = 6;
                return true;
            }
        }
        if(nowPriority <= CSUSearchUtil.SECOND_CLASS){
            if(calcSecondClassTarget()){
                this.nowPriority = 5;
                return true;
            }
        }
        if(nowPriority <= CSUSearchUtil.THIRD_CLASS){
            if(calcThirdClassTarget()){
                this.nowPriority = 4;
                return true;
            }
        }
        if(nowPriority <= CSUSearchUtil.FORTH_CLASS){
            if(calcForthClassTarget()){
                this.nowPriority = 3;
                return true;
            }
        }
        if(nowPriority <= CSUSearchUtil.FIFTH_CLASS){
            if(calcFifthClassTarget()){
                this.nowPriority = 2;
                return true;
            }
        }
        return false;
    }

    private boolean calcFirstClassTarget(){
        this.pathPlanning.setFrom(this.agentInfo.getPosition());
        Set<EntityID> first = ATBuildingSystem.toIDSet(ATBS.getClassBuildings(CSUSearchUtil.FIRST_CLASS));
        this.pathPlanning.setDestination(first);
        if(!first.isEmpty()){
            List<EntityID> path = this.pathPlanning.calc().getResult();
            if (path != null && path.size() > 0) {
                this.target = path.get(path.size() - 1);//获取终点
                ATBuilding atb = ATBS.getByID(target);
                if(atb.isBurning() || !atb.isReachable()){
                    atb.hangUp();
                    return calcFirstClassTarget();
                }
                if(this.target == null){
                    CSUSearchUtil.debugOverall("到"+this.target+"的路径终点为null(impossible).");
                    return false;
                }
                return true;
            }
            //功能冗余
//            else {//building都不可到达,去除
//                //todo:叫附近的警察来帮忙清障
////			Collection<EntityID> toRemove = new HashSet<>(optimalBuildings);
////			this.reset();
////			unsearchedBuildings.removeAll(toRemove);
//                for (EntityID id : optimalBuildings){
//                    if(worldInfo.getEntity(id) instanceof Building){
//                        Building building = (Building) worldInfo.getEntity(id);
//                        messageManager.addMessage(new MessageBuilding(false,building));
//                    }
//                }
//                return false;
//            }
        }
        return false;
    }

    private boolean calcSecondClassTarget(){
        this.pathPlanning.setFrom(this.agentInfo.getPosition());
        Set<EntityID> second = ATBuildingSystem.toIDSet(ATBS.getClassBuildings(CSUSearchUtil.SECOND_CLASS));
        this.pathPlanning.setDestination(second);
        if(!second.isEmpty()){
            List<EntityID> path = this.pathPlanning.calc().getResult();
            if (path != null && path.size() > 0) {
                this.target = path.get(path.size() - 1);//获取终点
                ATBuilding atb = ATBS.getByID(target);
                if(atb.isBurning() || !atb.isReachable()){
                    atb.hangUp();
                    return calcSecondClassTarget();
                }
                if(this.target == null){
                    CSUSearchUtil.debugOverall("到"+this.target+"的路径终点为null(impossible).");
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    private boolean calcThirdClassTarget(){
        this.pathPlanning.setFrom(this.agentInfo.getPosition());
        Set<EntityID> third = ATBuildingSystem.toIDSet(ATBS.getClassBuildings(CSUSearchUtil.THIRD_CLASS));
        this.pathPlanning.setDestination(third);
        if(!third.isEmpty()){
            List<EntityID> path = this.pathPlanning.calc().getResult();
            if (path != null && path.size() > 0) {
                this.target = path.get(path.size() - 1);//获取终点
                ATBuilding atb = ATBS.getByID(target);
                if(atb.isBurning() || !atb.isReachable()){
                    atb.hangUp();
                    return calcSecondClassTarget();
                }
                if(this.target == null){
                    CSUSearchUtil.debugOverall("到"+this.target+"的路径终点为null(impossible).");
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    private boolean calcForthClassTarget(){
        this.pathPlanning.setFrom(this.agentInfo.getPosition());
        Set<EntityID> forth = ATBuildingSystem.toIDSet(ATBS.getClassBuildings(CSUSearchUtil.FORTH_CLASS));
        this.pathPlanning.setDestination(forth);
        if(!forth.isEmpty()){
            List<EntityID> path = this.pathPlanning.calc().getResult();
            if (path != null && path.size() > 0) {
                this.target = path.get(path.size() - 1);//获取终点
                ATBuilding atb = ATBS.getByID(target);
                if(atb.isBurning() || !atb.isReachable()){
                    atb.hangUp();
                    return calcSecondClassTarget();
                }
                if(this.target == null){
                    CSUSearchUtil.debugOverall("到"+this.target+"的路径终点为null(impossible).");
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    //todo
    private boolean calcFifthClassTarget(){
        return false;
    }

    //todo
    public void updateInfo(MessageManager messageManager, Clustering clustering){
        this.messageManager = messageManager;
        this.clustering = clustering;
        //更新实现内的实体
        visionCivilian.clear();
        visionBuilding.clear();
        visionBlockade.clear();
        Set<EntityID> changedEntities = new HashSet<>(worldInfo.getChanged().getChangedEntities());
        for (EntityID changedId: changedEntities) {
            StandardEntity entity = worldInfo.getEntity(changedId);
            if(entity == null){
                CSUSearchUtil.debugOverall("changedEntity:"+changedId+"is null(impossible)");
                continue;
            }
            if(entity instanceof Building){
                UpdateVisionBuilding(entity);
                continue;
            }
            if(entity instanceof Civilian){
                UpdateVisionCivilian(entity);
                continue;
            }
            if(entity instanceof Blockade){
                visionBlockade.add((Blockade)entity);
            }
        }

        lastPosition = nowPosition;
        nowPosition = agentInfo.getPosition();
//        if(thirdClassBuildings.isEmpty()){
//            updateThirdClassBuildings();
//        }
        updateHeardCivilian();
        updateFirstClassBuildings();
        if(!heardCivilians.isEmpty()) updateSecondClassBuildings();
        updateThirdClassBuildings();
        updateForthClassBuildings();
        ATBS.updateInfo();
    }

    private void UpdateVisionCivilian(StandardEntity entity){

        Civilian civilian = (Civilian) entity;
        visionCivilian.add(civilian);
    }

    private void UpdateVisionBuilding(StandardEntity entity){

        Building building = (Building)entity;
        visionBuilding.add(building);
        if (building.isFierynessDefined() && building.getFieryness() > 0 && building.getFieryness() < 4) {
            burningBuildings.add(building.getID());
        } else {
            burningBuildings.remove(building.getID());
        }
    }

    //modified
    private void updateHeardCivilian(){
        Collection<Command> heard = agentInfo.getHeard();
        if (heard != null) {
            heard.forEach(sound -> {
                if (sound instanceof AKSpeak && ((AKSpeak) sound).getChannel() == 0 && !sound.getAgentID().equals(agentInfo.getID())) {// say messages
                    AKSpeak speak = (AKSpeak) sound;
                    Collection<EntityID> platoonIDs = Handy.objectsToIDs(CSUSearchUtil.getAllAgents());
                    if (!platoonIDs.contains(speak.getAgentID())) {//Civilian message
//                        processCivilianCommand(speak);
                        allCivilians.add(speak.getAgentID());
                        heardCivilians.add(speak.getAgentID());
                    }
                }
            });
        }
    }

    //todo 每回合去除获救的人
    private void updateFirstClassBuildings(){
        for (Civilian civ : visionCivilian) {
            EntityID civID = civ.getID();
            if (worldInfo.getEntity(civID) != null &&
                    worldInfo.getPosition((Human) worldInfo.getEntity(civID))
                            .getStandardURN() == BUILDING){
                Building building = (Building) worldInfo.getPosition((Human) worldInfo.getEntity(civID));
                ATBuilding atb = ATBS.getByID(building.getID());
                atb.setPriority(CSUSearchUtil.FIRST_CLASS);
                atb.addCivilianMaybe(civID);
            }
        }
        CSUSearchUtil.debugSpecific(agentInfo.getID()+":\'有"+ATBS.getClassBuildings(CSUSearchUtil.FIRST_CLASS).size()+"栋建筑里有人\'");
    }

    //todo 每回合去除获救的人
    private void updateSecondClassBuildings(){
        Pair<Integer,Integer> location = worldInfo.getLocation(agentInfo.getID());
        Collection<StandardEntity> ens = worldInfo.getObjectsInRange(location.first(), location.second(), voiceRange);
        for (StandardEntity entity : ens) {
            if (entity instanceof Building) {
                ATBuilding atb = ATBS.getByID(entity.getID());
                atb.setPriority(CSUSearchUtil.SECOND_CLASS);
            }
        }
    }

    private void updateThirdClassBuildings(){
        Collection<StandardEntity> clusterEntities = null;
        if(LastClusterHasFinished()){
            int clusterIndex = -1;
            if (this.clustering != null) {
                clusterIndex = this.clustering.getClusterIndex(this.agentInfo.getID());
                clusterEntities = this.clustering.getClusterEntities(clusterIndex);
                lastClusterIndex = clusterIndex;
            }
            if (clusterEntities != null && clusterEntities.size() > 0) {
                for (StandardEntity entity : clusterEntities) {
                    if(entity.getStandardURN() == BUILDING){
                        EntityID entityID = entity.getID();
                        ATBS.getByID(entityID).setClusterIndex(clusterIndex);
                        ATBuilding atb = ATBS.getByID(entityID);
                        atb.setPriority(CSUSearchUtil.THIRD_CLASS);
                    }
                }
            }
        }
    }

    private void updateForthClassBuildings(){
        for(EntityID entityID: allBuildings){
            ATBuilding atb = ATBS.getByID(entityID);
            atb.setPriority(CSUSearchUtil.FORTH_CLASS);
        }
    }

    //todo 要改
    public boolean lastTargetHasFinished(){
        if (worldInfo.getEntity(agentInfo.getPosition()) instanceof Building) {
            Building building = (Building) worldInfo.getEntity(agentInfo.getPosition());
            int distance = Util.getdistance(worldInfo.getLocation(agentInfo.getID()), worldInfo.getLocation(building));
            if (distance < scenarioInfo.getPerceptionLosMaxDistance()) {
                ATBuilding atb = ATBS.getByID(building.getID());
                atb.setVisited();
                return true;
            }
        }
        return false;
    }

    //todo
    private void handleHangUp(){}

    //todo
    private void findClosestElseCluster(){}

    private boolean isStationary(){
        CSUSearchUtil.debugSpecific("--------判断是否静止--------");
        if(lastPosition != null){
            CSUSearchUtil.debugSpecific(agentInfo.getID()+": 走了"+
                    Util.getdistance(worldInfo.getLocation(lastPosition),worldInfo.getLocation(nowPosition)));
            if(Util.getdistance(worldInfo.getLocation(lastPosition),worldInfo.getLocation(nowPosition)) < 2000){
                CSUSearchUtil.debugSpecific(agentInfo.getID()+":静止不动");
                return true;
            }
        }
        CSUSearchUtil.debugSpecific(agentInfo.getID()+":动了");
        return false;
    }

    private boolean isStuck(){
        CSUSearchUtil.debugSpecific("--------判断是否卡住--------");
        if(isStationary() && worldInfo.getEntity(nowPosition).getStandardURN() == BLOCKADE){
            CSUSearchUtil.debugSpecific(agentInfo.getID()+":卡住了");
            return true;
        }
        CSUSearchUtil.debugSpecific(agentInfo.getID()+":没卡住");
        return false;
    }

    //如果上一个聚类还没有排查完毕，不移动到下一个聚类
    private boolean LastClusterHasFinished(){
        if(lastClusterIndex == -1) return true;
        Collection<StandardEntity> clusterEntities = this.clustering.getClusterEntities(lastClusterIndex);
        for(StandardEntity entity : clusterEntities){
            if(entity.getStandardURN() == BUILDING){
                EntityID entityID = entity.getID();
                if(!(ATBS.getByID(entityID).isHangUp() || ATBS.getByID(entityID).isVisited())){
                    return false;
                }
            }
        }
        return true;
    }

    //todo
    private Set<EntityID> getNearbyPF(){
        return null;
    }

    //todo
    private Set<EntityID> getNearbyFB(){
        return null;
    }

}
