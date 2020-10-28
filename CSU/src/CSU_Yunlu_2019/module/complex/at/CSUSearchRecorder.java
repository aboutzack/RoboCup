package CSU_Yunlu_2019.module.complex.at;

import CSU_Yunlu_2019.util.Util;
import adf.agent.communication.MessageManager;
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
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;

/**
 * @author kyrieg
 */
public class CSUSearchRecorder {
    //所有建筑
    private Set<EntityID> allBuildings;
    //所有平民(不知道位置)
    private Set<EntityID> allCivilians;
//    //听到声音的人
//    private Set<EntityID> heardCivilians;
//    //救过的人
//    private Set<EntityID> savedCivilians;
    //通讯类
    private MessageManager messageManager;
    //视线范围
//    private Set<Civilian> visionCivilian;
//    private Set<Building> visionBuilding;
//    private Set<Blockade> visionBlockade;
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

    private ATBuildingSystem ATBS;
    private ATCivilianSystem ATCS;
    private static CSUSearchRecorder me;
    private CSUSearchUtil util;

    public CSUSearchRecorder(AgentInfo ai, WorldInfo wi, ScenarioInfo si, Clustering clustering, PathPlanning pathPlanning, CSUSearchUtil util){
        this.agentInfo = ai;
        this.worldInfo = wi;
        this.scenarioInfo = si;
        allBuildings = new HashSet<>();
        allCivilians = new HashSet<>();
//        heardCivilians = new HashSet<>();
//        visionBlockade = new HashSet<>();
//        firstClassBuildings = new HashSet<>();
//        secondClassBuildings = new HashSet<>();
//        thirdClassBuildings = new HashSet<>();
//        forthClassBuildings = new HashSet<>();
//        fifthClassBuildings = new HashSet<>();
        this.voiceRange = scenarioInfo.getRawConfig().getIntValue("comms.channels.0.range");
        this.clustering = clustering;
        this.pathPlanning = pathPlanning;
        this.nowPriority = 0;
        this.util = util;

        initialize();
    }

    public static void registerRecorder(CSUSearchRecorder searchRecorder){
        CSUSearchRecorder.me = searchRecorder;
    }

    public static CSUSearchRecorder load(){
        return me;
    }

    public ATBuildingSystem getATBS(){
        return ATBS;
    }

    //modified
    private void initialize(){
        ATBS = new ATBuildingSystem(pathPlanning, agentInfo, util);
        ATCS = new ATCivilianSystem(util);
        allBuildings.addAll(util.getBuildingIDs());
        allCivilians.addAll(util.getCivilianIDs());
        ATBS.initialized(allBuildings);
        ATCS.initialised(allCivilians);
//        ATCS.all = allCivilians;
    }

    public void updateInfo(MessageManager messageManager, Clustering clustering){

        this.messageManager = messageManager;
        this.clustering = clustering;
        //更新视野内的实体
        ATBS.getInSightBuildings().clear();
        ATCS.getInSightCivilian().clear();
        ATBS.passTime();
        ATCS.passTime();

        Set<EntityID> changedEntities = new HashSet<>(worldInfo.getChanged().getChangedEntities());
        for (EntityID changedId: changedEntities) {
            StandardEntity entity = worldInfo.getEntity(changedId);
            if(entity == null){
                util.debugOverall("changedEntity:"+changedId+"is null(impossible)");
                continue;
            }
            if(entity instanceof Building){
                ATBS.updateSingleBuilding(entity);
                continue;
            }
            if(entity instanceof Civilian){
                ATCS.updateSingleCivilian(entity);
                continue;
            }
//            if(entity instanceof Blockade){
//                visionBlockade.add((Blockade)entity);
//            }
        }

        lastPosition = nowPosition;
        nowPosition = agentInfo.getPosition();
        updateHeardCivilian();
        updateFirstClassBuildings();
        if(!ATCS.isHeardEmpty()) updateSecondClassBuildings();
        updateThirdClassBuildings();
        updateForthClassBuildings();
        ATBS.updateInfo();
        if(!isCurrentTargetReachable() && target != null) ATBS.getByID(target).setReachable(false);
//        ATBuilding currentATBuilding = ATBS.getByID(target);
        util.debugSpecific("听到的Civilian:"+ATCS.getHeardATCivilian());
        util.debugSpecific("视线内的Civilian:"+ATCS.getInSightCivilian());
    }

    public int motionState(){
//        if(isStuck()){
//            return CSUSearchUtil.STUCK;
//        }
        if(isStationary()){
            return CSUSearchUtil.STATIONARY;
        }
        return CSUSearchUtil.NORMAL;
    }

    public boolean isForcedToChangeTarget(int message){
        //todo 当前目标移出从前的队列，同时移入unreachableBuildings
        switch (message){
//            case CSUSearchUtil.STUCK:{
//                //todo 抄MRL
//                //todo toID：用null不知道会怎么样，如果可能，补充为getNearby的PF。
//                messageManager.addMessage(new CommandPolice(
//                        true,
//                        null,
//                        agentInfo.getPosition(),
//                        CommandPolice.ACTION_CLEAR));
//                break;
//            }
            case CSUSearchUtil.STATIONARY:{
                if(this.target == null) {
                    return decideBest();
                } else{
                    return changeTarget();
                }
            }
            default:{
                return false;
            }
        }
//        util.debugOverall("ChangeTarget() go into unknown logic.(impossible)");
//        return false;
    }

    //calcFifthClassTarget
    //最优决策(忽略当前优先级)
    public boolean decideBest(){
        boolean success = false;
        ATBuilding hangUpBuilding = null;
        util.debugSpecific("开始最优决策");

        if(decideBest(CSUSearchUtil.FIRST_CLASS)){
            return true;
        }else if(decideBest(CSUSearchUtil.SECOND_CLASS)){
            return true;
        }else if(decideBest(CSUSearchUtil.THIRD_CLASS)){
            return true;
        }else return decideBest(CSUSearchUtil.FORTH_CLASS);
//        if(handleHangUp(CSUSearchUtil.FIRST_CLASS)){
//            success = true;
//            nowPriority = CSUSearchUtil.FIRST_CLASS;
//            util.debugSpecific("从挂起队列中确定FirstClass");
//        }else if(findTargetLoop(CSUSearchUtil.FIRST_CLASS)){
//            success = true;
//            nowPriority = CSUSearchUtil.FIRST_CLASS;
//            util.debugSpecific("确定FirstClass");
//        }
//        if(!success && handleHangUp(CSUSearchUtil.SECOND_CLASS)){
//            success = true;
//            nowPriority = CSUSearchUtil.SECOND_CLASS;
//            util.debugSpecific("从挂起队列中确定SecondClass");
//        }else if(!success && findTargetLoop(CSUSearchUtil.SECOND_CLASS)){
//            success = true;
//            nowPriority = CSUSearchUtil.SECOND_CLASS;
//            util.debugSpecific("确定SecondClass");
//        }
//        if(!success && handleHangUp(CSUSearchUtil.THIRD_CLASS)){
//            success = true;
//            nowPriority = CSUSearchUtil.THIRD_CLASS;
//            util.debugSpecific("从挂起队列中确定ThirdClass");
//        }else if(!success && findTargetLoop(CSUSearchUtil.THIRD_CLASS)){
//            success = true;
//            nowPriority = CSUSearchUtil.THIRD_CLASS;
//            util.debugSpecific("确定ThirdClass");
//        }
//        if(!success && handleHangUp(CSUSearchUtil.FORTH_CLASS)){
//            success = true;
//            nowPriority = CSUSearchUtil.FORTH_CLASS;
//            util.debugSpecific("从挂起队列中确定ForthClass");
//        }else if(!success && findTargetLoop(CSUSearchUtil.FORTH_CLASS)){
//            success = true;
//            nowPriority = CSUSearchUtil.FORTH_CLASS;
//            util.debugSpecific("确定ForthClass");
//        }
//        if(success){
//            util.debugSpecific("最优决策成功");
//        }else{
//            util.debugSpecific("最优决策失败");
//        }
//        return success;

//        if(findTargetLoop(CSUSearchUtil.FIRST_CLASS)){
//            success = true;
//            util.debugSpecific("确定FirstClass");
//        }
//        if(!success && findTargetLoop(CSUSearchUtil.SECOND_CLASS)){
//            success = true;
//            util.debugSpecific("确定SecondClass");
//        }
//        if(!success && findTargetLoop(CSUSearchUtil.THIRD_CLASS)){
//            success = true;
//            util.debugSpecific("确定ThirdClass");
//        }
//        if(!success && findTargetLoop(CSUSearchUtil.FORTH_CLASS)){
//            success = true;
//            util.debugSpecific("确定ForthClass");
//        }
//        if(success){
//            util.debugSpecific("最优决策成功");
//        }else{
//            util.debugSpecific("最优决策失败");
//        }
//        return success;
    }

    private boolean decideBest(int priority){
        boolean success = false;
        if(handleHangUp(priority)){
            nowPriority = priority;
            util.debugSpecific("从挂起队列中确定"+CSUSearchUtil.getNameByPriority(priority));
            success = true;
        }else if(findTargetLoop(priority)){
            nowPriority = priority;
            util.debugSpecific("确定"+CSUSearchUtil.getNameByPriority(priority));
            success = true;
        }
        return success;
    }

    //简单直接决策
    public EntityID quickDecide(){
        findTargetLoop(CSUSearchUtil.FIFTH_CLASS);
        return this.target;
    }

    //最优抢占(考虑优先级)
    public boolean changeTarget(){
        util.debugSpecific("尝试抢占");
        EntityID lastTarget = target;
        boolean success = false;
        boolean occupied = false;
        if(nowPriority <= CSUSearchUtil.FIRST_CLASS){
            if(handleHangUp(CSUSearchUtil.FIRST_CLASS)){
                this.nowPriority = CSUSearchUtil.FIRST_CLASS;
                occupied = true;
                success = true;
            }else if(findTargetLoop(CSUSearchUtil.FIRST_CLASS)) {
                this.nowPriority = CSUSearchUtil.FIRST_CLASS;
                success = true;
            }
        }
        if(nowPriority <= CSUSearchUtil.SECOND_CLASS){
            if(handleHangUp(CSUSearchUtil.SECOND_CLASS)){
                this.nowPriority = CSUSearchUtil.SECOND_CLASS;
                occupied = true;
                success = true;
            }else if(findTargetLoop(CSUSearchUtil.SECOND_CLASS)){
                this.nowPriority = CSUSearchUtil.SECOND_CLASS;
                success = true;
            }
        }
        if(nowPriority <= CSUSearchUtil.THIRD_CLASS){
            if(handleHangUp(CSUSearchUtil.THIRD_CLASS)){
                this.nowPriority = CSUSearchUtil.THIRD_CLASS;
                occupied = true;
                success = true;
            }else if(findTargetLoop(CSUSearchUtil.THIRD_CLASS)){
                this.nowPriority = CSUSearchUtil.THIRD_CLASS;
                success = true;
            }
        }
        if(nowPriority <= CSUSearchUtil.FORTH_CLASS){
            if(handleHangUp(CSUSearchUtil.FORTH_CLASS)){
                this.nowPriority = CSUSearchUtil.FORTH_CLASS;
                occupied = true;
                success = true;
            }else if(findTargetLoop(CSUSearchUtil.FORTH_CLASS)){
                this.nowPriority = CSUSearchUtil.FORTH_CLASS;
                success = true;
            }
        }
        if(nowPriority <= CSUSearchUtil.FIFTH_CLASS){
            if(decideBest()){
                this.nowPriority = 2;
                success = true;
            }
        }
        if(occupied) {
            ATBS.getByID(lastTarget).setOccupied(true);
        }
        if(success){
            util.debugSpecific("抢占成功");
        }else {
            util.debugSpecific("抢占失败");
        }
        return success;
//        util.debugSpecific("尝试抢占");
//        if(nowPriority <= CSUSearchUtil.FIRST_CLASS){
//            if(findTargetLoop(CSUSearchUtil.FIRST_CLASS)) {
//                this.nowPriority = 6;
//                return true;
//            }
//        }
//        if(nowPriority <= CSUSearchUtil.SECOND_CLASS){
//            if(findTargetLoop(CSUSearchUtil.SECOND_CLASS)){
//                this.nowPriority = 5;
//                return true;
//            }
//        }
//        if(nowPriority <= CSUSearchUtil.THIRD_CLASS){
//            if(findTargetLoop(CSUSearchUtil.THIRD_CLASS)){
//                this.nowPriority = 4;
//                return true;
//            }
//        }
//        if(nowPriority <= CSUSearchUtil.FORTH_CLASS){
//            if(findTargetLoop(CSUSearchUtil.FORTH_CLASS)){
//                this.nowPriority = 3;
//                return true;
//            }
//        }
//        if(nowPriority <= CSUSearchUtil.FIFTH_CLASS){
//            if(decideBest()){
//                this.nowPriority = 2;
//                return true;
//            }
//        }
//        util.debugSpecific("抢占失败");
//        return false;
    }

    private boolean findTargetLoop(int priority){
        util.debugSpecific("计算"+CSUSearchUtil.getNameByPriority(priority)+"");
        this.pathPlanning.setFrom(this.agentInfo.getPosition());
        Set<EntityID> set = ATBuildingSystem.toIDSet(ATBS.getClassBuildingsByPriority(priority));
        util.debugSpecific("set有:"+set);
        set.removeIf(entityID -> ATBS.isHangUp(entityID));
        util.debugSpecific(CSUSearchUtil.getNameByPriority(priority)+"有:"+set);
        EntityID target = null;
        while(!set.isEmpty()){
            this.pathPlanning.setDestination(set);
            List<EntityID> path = this.pathPlanning.calc().getResult();
            if(path != null && path.size() > 0){
                target = path.get(path.size() - 1);//获取终点
                if(target == null){
                    util.debugOverall("到"+target+"的路径终点为null.(impossible)");
                    return false;
                }
                ATBuilding atb = ATBS.getByID(target);
                if(atb.isBurning() || !atb.isReachable()){
                    if(atb.isBurning()) util.debugSpecific("该建筑("+atb.getId()+")正在燃烧，去除");
                    if(!atb.isReachable()) util.debugSpecific("该建筑("+atb.getId()+")无法到达，去除");
                    util.debugSpecific("重新计算");
                    set.remove(atb.getId());
                    continue;
                }
                util.debugSpecific("计算"+CSUSearchUtil.getNameByPriority(priority)+"成功");
                this.target = target;
                return true;
            }
            util.debugSpecific(CSUSearchUtil.getNameByPriority(priority)+"没有可行建筑");
            break;
        }
//        if(!set.isEmpty()){
//            List<EntityID> path = this.pathPlanning.calc().getResult();
//            if (path != null && path.size() > 0) {
//                this.target = path.get(path.size() - 1);//获取终点
//                ATBuilding atb = ATBS.getByID(target);
//                if(atb.isBurning() || !atb.isReachable()){
//                    if(atb.isBurning()) util.debugSpecific("该建筑("+atb.getId()+")正在燃烧，重新计算");
//                    if(atb.isReachable()) util.debugSpecific("找到该建筑("+atb.getId()+")无法到达，重新计算");
//                    atb.hangUp();
//                    return calcSecondClassTarget();
//                }
//                if(this.target == null){
//                    util.debugOverall("到"+this.target+"的路径终点为null(impossible).");
//                    return false;
//                }
//                util.debugSpecific("计算"+CSUSearchUtil.getNameByPriority(priority)+"成功");
//                return true;
//            }
//        }
        util.debugSpecific("计算"+CSUSearchUtil.getNameByPriority(priority)+"失败，进入下一级");
        return false;
    }

    //modified
    private void updateHeardCivilian(){
        Collection<Command> heard = agentInfo.getHeard();
        if (heard != null) {
            heard.forEach(sound -> {
                if (sound instanceof AKSpeak &&
                        ((AKSpeak) sound).getChannel() == 0 &&
                        !sound.getAgentID().equals(agentInfo.getID())) {// say messages
                    AKSpeak speak = (AKSpeak) sound;
                    Collection<EntityID> platoonIDs = Handy.objectsToIDs(util.getAllAgents());
                    if (!platoonIDs.contains(speak.getAgentID())) {//Civilian message
//                        processCivilianCommand(speak);
                        ATCS.updateHeard(speak.getAgentID());
                    }
                }
            });
        }
    }

    //todo 每回合去除获救的人
    private void updateFirstClassBuildings(){
        for (ATCivilian atCivilian : ATCS.getInSightCivilian()) {
            EntityID civID = atCivilian.getId();
            if (worldInfo.getEntity(civID) != null &&
                    worldInfo.getPosition((Human) worldInfo.getEntity(civID))
                            .getStandardURN() == BUILDING){
                Building building = (Building) worldInfo.getPosition((Human) worldInfo.getEntity(civID));
                ATBuilding atb = ATBS.getByID(building.getID());
                atb.setPriority(CSUSearchUtil.FIRST_CLASS);
                atb.addCivilianConfirmed(civID);
            }
        }
        util.debugSpecific("有"+ATBS.getClassBuildingsByPriority(CSUSearchUtil.FIRST_CLASS).size()+"栋建筑里有人");
    }

    //todo 每回合去除获救的人,听到声音的人数没有利用
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
            util.debugSpecific("刷新聚类");
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
                        ATBuilding atb = ATBS.getByID(entityID);
                        atb.setClusterIndex(clusterIndex);
                        atb.setPriority(CSUSearchUtil.THIRD_CLASS);
                    }
                }
                //debug
                ATBS.printNowCluster();
            }
        }
    }

    private void updateForthClassBuildings(){
        for(EntityID entityID: allBuildings){
            ATBuilding atb = ATBS.getByID(entityID);
            atb.setPriority(CSUSearchUtil.FORTH_CLASS);
        }
    }

    public EntityID getTarget(){
        return this.target;
    }

//    private boolean calcFirstClassTarget(){
//        util.debugSpecific("计算FirstClass");
//        this.pathPlanning.setFrom(this.agentInfo.getPosition());
//        Set<EntityID> first = ATBuildingSystem.toIDSet(ATBS.getClassBuildingsByPriority(CSUSearchUtil.FIRST_CLASS));
//        this.pathPlanning.setDestination(first);
//        if(!first.isEmpty()){
//            List<EntityID> path = this.pathPlanning.calc().getResult();
//            if (path != null && path.size() > 0) {
//                this.target = path.get(path.size() - 1);//获取终点
//                ATBuilding atb = ATBS.getByID(target);
//                if(atb.isBurning() || !atb.isReachable()){
//                    util.debugSpecific("该建筑("+atb.getId()+")不行，重新计算");
//                    atb.hangUp();
//                    return calcFirstClassTarget();
//                }
//                if(this.target == null){
//                    util.debugOverall("到"+this.target+"的路径终点为null(impossible).");
//                    return false;
//                }
//                util.debugSpecific("计算FirstClass成功");
//                return true;
//            }
//            //功能冗余
////            else {//building都不可到达,去除
////                //todo:叫附近的警察来帮忙清障
//////			Collection<EntityID> toRemove = new HashSet<>(optimalBuildings);
//////			this.reset();
//////			unsearchedBuildings.removeAll(toRemove);
////                for (EntityID id : optimalBuildings){
////                    if(worldInfo.getEntity(id) instanceof Building){
////                        Building building = (Building) worldInfo.getEntity(id);
////                        messageManager.addMessage(new MessageBuilding(false,building));
////                    }
////                }
////                return false;
////            }
//        }
//        util.debugSpecific("计算FirstClass失败，进入下一级");
//        return false;
//    }
//
//    private boolean calcSecondClassTarget(){
//        util.debugSpecific("计算SecondClass");
//        this.pathPlanning.setFrom(this.agentInfo.getPosition());
//        Set<EntityID> second = ATBuildingSystem.toIDSet(ATBS.getClassBuildingsByPriority(CSUSearchUtil.SECOND_CLASS));
//        this.pathPlanning.setDestination(second);
//        if(!second.isEmpty()){
//            List<EntityID> path = this.pathPlanning.calc().getResult();
//            if (path != null && path.size() > 0) {
//                this.target = path.get(path.size() - 1);//获取终点
//                ATBuilding atb = ATBS.getByID(target);
//                if(atb.isBurning() || !atb.isReachable()){
//                    if(atb.isBurning()) util.debugSpecific("该建筑("+atb.getId()+")正在燃烧，重新计算");
//                    if(atb.isReachable()) util.debugSpecific("找到该建筑("+atb.getId()+")无法到达，重新计算");
//                    atb.hangUp();
//                    return calcSecondClassTarget();
//                }
//                if(this.target == null){
//                    util.debugOverall("到"+this.target+"的路径终点为null(impossible).");
//                    return false;
//                }
//                util.debugSpecific("计算SecondClass成功");
//                return true;
//            }
//        }
//        util.debugSpecific("计算SecondClass失败，进入下一级");
//        return false;
//    }
//
//    private boolean calcThirdClassTarget(){
//        util.debugSpecific("计算ThirdClass");
//        this.pathPlanning.setFrom(this.agentInfo.getPosition());
//        Set<EntityID> third = ATBuildingSystem.toIDSet(ATBS.getClassBuildingsByPriority(CSUSearchUtil.THIRD_CLASS));
//        this.pathPlanning.setDestination(third);
//        if(!third.isEmpty()){
//            List<EntityID> path = this.pathPlanning.calc().getResult();
//            if (path != null && path.size() > 0) {
//                this.target = path.get(path.size() - 1);//获取终点
//                ATBuilding atb = ATBS.getByID(target);
//                if(atb.isBurning() || !atb.isReachable()){
//                    util.debugSpecific("该建筑("+atb.getId()+")不行，重新计算");
//                    atb.hangUp();
//                    return calcSecondClassTarget();
//                }
//                if(this.target == null){
//                    util.debugOverall("到"+this.target+"的路径终点为null(impossible).");
//                    return false;
//                }
//                util.debugSpecific("计算ThirdClass成功");
//                return true;
//            }
//        }
//        util.debugSpecific("计算ThirdClass失败，进入下一级");
//        return false;
//    }
//
//    private boolean calcForthClassTarget(){
//        util.debugSpecific("计算ForthClass");
//        this.pathPlanning.setFrom(this.agentInfo.getPosition());
//        Set<EntityID> forth = ATBuildingSystem.toIDSet(ATBS.getClassBuildingsByPriority(CSUSearchUtil.FORTH_CLASS));
//        this.pathPlanning.setDestination(forth);
//        if(!forth.isEmpty()){
//            List<EntityID> path = this.pathPlanning.calc().getResult();
//            if (path != null && path.size() > 0) {
//                this.target = path.get(path.size() - 1);//获取终点
//                ATBuilding atb = ATBS.getByID(target);
//                if(atb.isBurning() || !atb.isReachable()){
//                    util.debugSpecific("该建筑("+atb.getId()+")不行，重新计算");
//                    atb.hangUp();
//                    return calcSecondClassTarget();
//                }
//                if(this.target == null){
//                    util.debugOverall("到"+this.target+"的路径终点为null(impossible).");
//                    return false;
//                }
//                util.debugSpecific("计算ForthClass成功");
//                return true;
//            }
//        }
//        util.debugSpecific("计算ForthClass失败，进入下一级");
//        return false;
//    }

    //todo
//    private boolean calcFifthClassTarget(){
//        return false;
//    }

    //todo 要考虑燃烧
    private boolean handleHangUp(int priority){
        util.debugSpecific("看看挂起队列");
        ATBuilding hangUpBuilding = ATBS.decideBestOccupied(priority);
        if(hangUpBuilding != null){
            this.target = hangUpBuilding.getId();
            util.debugSpecific("取出挂起成功");
            return true;
        }else{
            util.debugSpecific("取出挂起失败");
            return false;
        }
    }

    private void findClosestElseCluster(){}

    private boolean isStationary(){
        util.debugSpecific("判断是否静止");
        if(lastPosition != null){
            util.debugSpecific("走了 "+
                    Util.getdistance(worldInfo.getLocation(lastPosition),worldInfo.getLocation(nowPosition)));
            if(Util.getdistance(worldInfo.getLocation(lastPosition),worldInfo.getLocation(nowPosition)) < 2000){
//                if(target != null){
//                    ATBS.setVisited(target);
//                }
                util.debugSpecific(agentInfo.getID()+":静止不动");
                return true;
            }
        }
        util.debugSpecific("动了");
        return false;
    }

    private boolean isCurrentTargetReachable(){
        return util.isBuildingReachable(pathPlanning, target);
    }

    private boolean lastTargetHasFinished(){
        if(worldInfo.getEntity(this.target) instanceof Building && worldInfo.getLocation(this.target) != null){
            Building building = (Building) worldInfo.getEntity(this.target);
            int distance = Util.getdistance(worldInfo.getLocation(agentInfo.getID()),worldInfo.getLocation(building));
            if (agentInfo.getPositionArea() instanceof Building &&
                    distance < scenarioInfo.getPerceptionLosMaxDistance()){
                ATBS.setVisited(this.target);
                util.debugSpecific("上一目标建筑("+this.target+")搜完了");
//                this.target = null;
                return true;
            }
        }
//        if (worldInfo.getEntity(agentInfo.getPosition()) instanceof Building) {
//            Building building = (Building) worldInfo.getEntity(agentInfo.getPosition());
//            int distance = Util.getdistance(worldInfo.getLocation(agentInfo.getID()), worldInfo.getLocation(building));
//            if (distance < scenarioInfo.getPerceptionLosMaxDistance()) {
//                ATBS.setVisited(building.getID());
//                this.target = null;
//                util.debugSpecific("上一目标建筑("+this.target+")搜完了");
//                return true;
//            }
//        }
        return false;
    }

//    //添加掉血速度计算（感觉不用，因为at越多救人越快）
//    private boolean isATInBuildingEnough(){
//        int buildingATNum = 0;
//        Building building = (Building)worldInfo.getEntity(this.target);
//        if(building == null){
//            util.debugSpecific("目标building为null(impossible)");
//            return false;
//        }
//        Collection<Human> buriedHumans = worldInfo.getBuriedHumans(building);
//        int buriedHumanNum = buriedHumans.size();
//        Set<Entity> entities = new HashSet<Entity>(worldInfo.getEntitiesOfType(AMBULANCE_TEAM));
//        for(Entity e : entities){
//            AmbulanceTeam at = (AmbulanceTeam)e;
//            if(worldInfo.getEntity(at.getPosition()) == worldInfo.getEntity(this.target)) buildingATNum++;
//        }
//        if(buildingATNum != 0 && buildingATNum >= buriedHumanNum){
//            ATBS.setVisited(target);
//            return true;
//        }else{
//            return false;
//        }
//    }

//    private boolean isStuck(){
//        util.debugSpecific("判断是否卡住");
//        if(isStationary() && worldInfo.getEntity(nowPosition).getStandardURN() == BLOCKADE){
//            util.debugSpecific(agentInfo.getID()+":卡住了");
//            return true;
//        }
//        util.debugSpecific(agentInfo.getID()+":没卡住");
//        return false;
//    }

    public boolean needToChangeTarget(){
        return this.target == null || lastTargetHasFinished()
                || !ATBS.getByID(target).isReachable() || ATBS.getByID(target).isBurning();
//        return this.target == null || isATInBuildingEnough()
//                || lastTargetHasFinished() || !ATBS.getByID(target).isReachable() || ATBS.getByID(target).isBurning();
    }

    //如果上一个聚类还没有排查完毕，不移动到下一个聚类
    private boolean LastClusterHasFinished(){
        if(lastClusterIndex == -1){
            return true;
        }
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

    private Set<EntityID> getNearbyPF(){
        return null;
    }

    private Set<EntityID> getNearbyFB(){
        return null;
    }

}
