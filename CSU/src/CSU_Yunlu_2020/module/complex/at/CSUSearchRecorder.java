package CSU_Yunlu_2020.module.complex.at;

import CSU_Yunlu_2020.LogHelper;
import CSU_Yunlu_2020.module.algorithm.AStarPathPlanning;
import CSU_Yunlu_2020.util.Util;
import adf.agent.communication.MessageManager;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.messages.Command;
import rescuecore2.misc.Handy;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.BUILDING;

/**
 * @author kyrieg
 */
public class CSUSearchRecorder {
    //所有建筑
    private Set<EntityID> allBuildings;
    //所有平民(不知道位置)
    private Set<EntityID> allCivilians;
    //通讯类
    private MessageManager messageManager;

    private EntityID lastPosition;
    private EntityID nowPosition;
    private EntityID target;
    private int nowPriority;
    private int strategyType;
    private WorldInfo worldInfo;
    public AgentInfo agentInfo;
    private ScenarioInfo scenarioInfo;
    private int voiceRange;
    private int lastClusterIndex = -1;

    private Clustering clustering;
    private PathPlanning pathPlanning;

    private ATBuildingSystem ATBS;
    private ATHumanSystem ATCS;
    private static CSUSearchRecorder me;
    private CSUSearchUtil util;

    private List<EntityID> myWay;

    private LogHelper logHelper;

    public CSUSearchRecorder(AgentInfo ai, WorldInfo wi, ScenarioInfo si, Clustering clustering, PathPlanning pathPlanning, CSUSearchUtil util){
        this.agentInfo = ai;
        this.worldInfo = wi;
        this.scenarioInfo = si;
        allBuildings = new HashSet<>();
        allCivilians = new HashSet<>();

        this.voiceRange = scenarioInfo.getRawConfig().getIntValue("comms.channels.0.range");
        this.clustering = clustering;
        this.pathPlanning = pathPlanning;
        this.nowPriority = 0;
        this.util = util;
        this.myWay = new ArrayList<>();

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
        ATBS = new ATBuildingSystem(pathPlanning, agentInfo, worldInfo, util);
        ATCS = new ATHumanSystem(util, agentInfo);
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
        ATCS.getInSightHuman().clear();
        //计时器计时
        ATBS.passTime();
        ATCS.passTime();
        //更新位置信息
        lastPosition = nowPosition;
        nowPosition = agentInfo.getPosition();

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
            if(entity instanceof Human){
                ATCS.updateSingleHuman(entity);
                continue;
            }
        }

        //11.15
        //追加判断视线内的智能体逻辑（尝试）
        Set<EntityID> inSightEntityIDs = new HashSet<>(worldInfo.getObjectIDsInRange(agentInfo.getID(), scenarioInfo.getPerceptionLosMaxDistance()));
        for (EntityID changedId: inSightEntityIDs) {
            StandardEntity entity = worldInfo.getEntity(changedId);
            if(entity == null){
                util.debugOverall("changedEntity:"+changedId+"is null(impossible)");
                continue;
            }
            if(entity instanceof Building){
                ATBS.updateSingleBuilding(entity);
                continue;
            }
            if(entity instanceof Human){
                ATCS.updateSingleHuman(entity);
                continue;
            }
        }
        //11.15

        //11.16
        //update In Case decision by search
        StandardEntity standardEntity = worldInfo.getEntity(nowPosition);
        if(standardEntity instanceof Building){
            ATBS.getByID(standardEntity.getID()).setVisited();
        }
        //11.16

        updateHeardCivilian();

        updateFirstClassBuildings();

        updateSecondClassBuildings();

        updateThirdClassBuildings();
        updateForthClassBuildings();
        ATBS.updateInfo();
        //更新当前的路线
        this.myWay = this.pathPlanning.setFrom(agentInfo.getPosition()).setDestination(target).calc().getResult();
        if(!isCurrentTargetReachable() && target != null){
            ATBS.getByID(target).setReachable(false);
            ATBS.addUnreachableBuilding(target);
        }
//        ATBuilding currentATBuilding = ATBS.getByID(target);
        util.debugSpecific("听到的Civilian:"+ATCS.getHeardATHuman());
        util.debugSpecific("视线内的Civilian:"+ATCS.getInSightHuman());
    }

    //最优决策(忽略当前优先级) 没有必要考虑挂起当前目标，因为进入这个方法之前，当前目标早已经被挂起了
    public boolean decideBest(){
        long start = Calendar.getInstance().getTimeInMillis();
        boolean success = false;
        ATBuilding hangUpBuilding = null;
//        EntityID lastTarget = this.target;
        util.debugSpecific("开始最优决策");
        if(decideBestByPriority(CSUSearchUtil.FIRST_CLASS)){
            success = true;
            util.debugSpecific("最优决策结果:FIRST_CLASS");
        }else if(decideBestByPriority(CSUSearchUtil.SECOND_CLASS)){
            success = true;
            util.debugSpecific("最优决策结果:SECOND_CLASS");
        }else if(decideBestByPriority(CSUSearchUtil.THIRD_CLASS)){
            success = true;
            util.debugSpecific("最优决策结果:THIRD_CLASS");
        }else if(decideBestByPriority(CSUSearchUtil.FORTH_CLASS)){
            success = true;
            util.debugSpecific("最优决策结果:FORTH_CLASS");
        }
        long end = Calendar.getInstance().getTimeInMillis();
        if(success){
            util.debugSpecific("最优决策成功,花费时间:"+(end-start));
        }else{
            util.debugSpecific("最优决策失败,花费时间:"+(end-start));
        }
        return success;
    }

    private boolean decideBestByPriority(int priority){
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

    private boolean findTargetLoop(int priority){
        util.debugSpecific("从非挂起中计算"+CSUSearchUtil.getNameByPriority(priority)+"");
        this.pathPlanning.setFrom(this.agentInfo.getPosition());
        Set<EntityID> set = ATBuildingSystem.toIDSet(ATBS.getClassBuildingsByPriority(priority));
//        util.debugSpecific("set有:"+set);
        set.removeIf(entityID -> ATBS.isHangUp(entityID));
        //燃烧中，不能到达的，被抢占，没必要搜的(Burnt,unbroken，visited)都要去掉
//        set = ATBS.removeBad(set);
        util.debugSpecific(CSUSearchUtil.getNameByPriority(priority)+"有:"+set);
        EntityID target = null;
        while(!set.isEmpty()){
            this.pathPlanning.setDestination(set);
            List<EntityID> path = this.pathPlanning.calc().getResult();
            if(path != null && path.size() > 0){
                if(pathPlanning instanceof AStarPathPlanning){
                    AStarPathPlanning aspp = (AStarPathPlanning) pathPlanning;
                    target = aspp.getResultTarget();
                }else{
                    target = path.get(path.size() - 1);//获取终点
                }
                if(target == null){
                    util.debugOverall("到null的路径终点为null.(impossible)");
                    return false;
                }
                ATBuilding atb = ATBS.getByID(target);
                if(atb.isBurning()){
                    set.remove(target);
                    continue;
                }
                if(!atb.isReachable()){
                    set.remove(target);
                    continue;
                }
                if(atb.isOccupied()){
                    set.remove(target);
                   continue;
                }
                //-----------多这一句话就会不动-----------
//                if(!atb.isNeedToSearch()){
//                      continue;
//                }
                //-----------多这一句话就会不动-----------
                if(atb.isVisited() || atb.isBurnt()){
                    set.remove(target);
                    continue;
                }
                //---------------修改尝试----------------(成功)
                if(atb.isWayBurning()){
                    set.remove(target);
                    continue;
                }
//                if(atb.isBurning() || !atb.isReachable()){
//                    if(atb.isBurning()) util.debugSpecific("该建筑("+atb.getId()+")正在燃烧，去除");
//                    if(!atb.isReachable()) util.debugSpecific("该建筑("+atb.getId()+")无法到达，去除");
//                    util.debugSpecific("重新计算");
//                    set.remove(atb.getId());
//                    continue;
//                }
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


    public boolean tryFindPriorTarget(){
        long start = Calendar.getInstance().getTimeInMillis();
        util.debugSpecific("尝试抢占");
        EntityID lastTarget = target;
        int lastPriority = ATBS.getByID(target).getPriority();
        boolean success = false;
        if(nowPriority < CSUSearchUtil.FIRST_CLASS){
            util.debugSpecific("尝试用FIRST_CLASS抢占");
            if(handleHangUp(CSUSearchUtil.FIRST_CLASS)){
                this.nowPriority = CSUSearchUtil.FIRST_CLASS;
                success = true;
            }else if(findTargetLoop(CSUSearchUtil.FIRST_CLASS)) {
                this.nowPriority = CSUSearchUtil.FIRST_CLASS;
                success = true;
            }
        }
        if(!success && nowPriority < CSUSearchUtil.SECOND_CLASS){
            util.debugSpecific("尝试用SECOND_CLASS抢占");
            if(handleHangUp(CSUSearchUtil.SECOND_CLASS)){
                this.nowPriority = CSUSearchUtil.SECOND_CLASS;
//                occupied = true;
                success = true;
            }else if(findTargetLoop(CSUSearchUtil.SECOND_CLASS)){
                this.nowPriority = CSUSearchUtil.SECOND_CLASS;
                success = true;
            }
        }
        if(!success && nowPriority < CSUSearchUtil.THIRD_CLASS){
            util.debugSpecific("尝试用THIRD_CLASS抢占");
            if(handleHangUp(CSUSearchUtil.THIRD_CLASS)){
                this.nowPriority = CSUSearchUtil.THIRD_CLASS;
//                occupied = true;
                success = true;
            }else if(findTargetLoop(CSUSearchUtil.THIRD_CLASS)){
                this.nowPriority = CSUSearchUtil.THIRD_CLASS;
                success = true;
            }
        }
        if(!success && nowPriority < CSUSearchUtil.FORTH_CLASS){
            util.debugSpecific("尝试用FORTH_CLASS抢占");
            if(handleHangUp(CSUSearchUtil.FORTH_CLASS)){
                this.nowPriority = CSUSearchUtil.FORTH_CLASS;
//                occupied = true;
                success = true;
            }else if(findTargetLoop(CSUSearchUtil.FORTH_CLASS)){
                this.nowPriority = CSUSearchUtil.FORTH_CLASS;
                success = true;
            }
        }
        if(!success && nowPriority < CSUSearchUtil.FIFTH_CLASS){
            util.debugSpecific("尝试用Unknown building(impossible)抢占");
            util.debugOverall("Unknown building(impossible)");
//            if(decideBest()){
//                this.nowPriority = 2;
//                success = true;
//            }
        }
        long end = Calendar.getInstance().getTimeInMillis();
        if(success){
            util.debugSpecific("抢占成功,由"+CSUSearchUtil.getNameByPriority(lastPriority)+"变为"+CSUSearchUtil.getNameByPriority(nowPriority)+",花费时间:"+(end-start));
            ATBS.getByID(lastTarget).setOccupied(true);
        }else {
            util.debugSpecific("抢占失败,花费时间"+(end-start));
        }
        return success;
    }

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
        for (ATHuman atHuman : ATCS.getInSightHuman()) {
            EntityID civID = atHuman.getId();
            if(worldInfo.getEntity(civID) == null){
                util.debugSpecific("看到的civ("+civID+")却无法获得Entity(impossible)");
            }
            util.debugSpecific("看到的civ("+civID+")当前位置类型为:"+worldInfo.getPosition((Human) worldInfo.getEntity(civID)).getStandardURN());
            if (worldInfo.getEntity(civID) != null &&
                    worldInfo.getPosition((Human) worldInfo.getEntity(civID))
                            .getStandardURN() == BUILDING){
                //只有buriness大于零的human要救，以及只有damage的平民要背
                Building building = (Building) worldInfo.getPosition((Human) worldInfo.getEntity(civID));
                ATBuilding atb = ATBS.getByID(building.getID());
                atb.setPriority(CSUSearchUtil.FIRST_CLASS);
                atb.addHumanConfirmed(civID);
            }
        }
        util.debugSpecific("有"+ATBS.getClassBuildingsByPriority(CSUSearchUtil.FIRST_CLASS).size()+"栋建筑里有人");
    }

    //todo 每回合去除获救的人,听到声音的人数没有利用
    private void updateSecondClassBuildings(){
        if(!ATCS.isHeardEmpty()){
            Pair<Integer,Integer> location = worldInfo.getLocation(agentInfo.getID());
            Collection<StandardEntity> ens = worldInfo.getObjectsInRange(location.first(), location.second(), voiceRange);
            for (StandardEntity entity : ens) {
                if (entity instanceof Building) {
                    ATBuilding atb = ATBS.getByID(entity.getID());
                    atb.setPriority(CSUSearchUtil.SECOND_CLASS);
                }
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
    //------以上11.7-------


    //-------11.8--------
    private boolean handleHangUp(int priority){
        util.debugSpecific("先查看"+CSUSearchUtil.getNameByPriority(priority)+"挂起队列");
        ATBuilding hangUpBuilding = decideBestOccupied(priority);//suspect
        if(hangUpBuilding != null){
            if(target != null) ATBS.getByID(target).setOccupied(true);
            this.target = hangUpBuilding.getId();
            util.debugSpecific("取出挂起成功");
            return true;
        }else{
            util.debugSpecific("取出挂起失败");
            return false;
        }
    }

    //当前为随机选择
    public ATBuilding decideBestOccupied(int priority){
        int maxTime = 0;
        ATBuilding best = null;
        Set<ATBuilding> occupiedBuildings = ATBS.getAllOccupiedByPriority(priority);
        //燃烧中，不能到达的没必要搜的(Burnt,unbroken，visited)都要去掉
        for(ATBuilding atBuilding : occupiedBuildings){
            if(atBuilding.isBurning()){
                continue;
            }
            if(!atBuilding.isReachable()){
                continue;
            }
            if(!atBuilding.isOccupied()){
                util.debugOverall(atBuilding+"没被抢占(impossible)");
                continue;
            }
            //-----------多这一句话就会不动-----------
//            if(!atBuilding.isNeedToSearch()){
//                continue;
//            }
            //-----------多这一句话就会不动-----------
            if(atBuilding.isWayBurning()){
                continue;
            }

            if(atBuilding.isVisited() || atBuilding.isBurnt()){
                continue;
            }
            //---------------修改尝试----------------(成功)
            int hangUpTime = atBuilding.getHangUptime();
            if(hangUpTime > maxTime){
                maxTime = hangUpTime;
                best = atBuilding;
            }
        }
        if(best != null) best.setOccupied(false);
        return best;
    }

    //当前为随机选择(suspect) //不能用pathplanning，不然算不过来
//    public ATBuilding decideBestOccupied(int priority, EntityID target) {
//        util.debugSpecific("-----------进行最优抢占计算-----------");
//        int maxTime = 0;//修改策略了
//        EntityID best = null;
//        EntityID id = null;
//        Set<ATBuilding> occupiedBuildings = ATBS.getAllOccupiedByPriority(priority);
//        Set<EntityID> set = ATBuildingSystem.toIDSet(occupiedBuildings);
//        //set中移除当前目标
//        set.remove(target);
//        this.pathPlanning.setFrom(this.agentInfo.getPosition());
//        set.removeIf(entityID -> worldInfo.getEntity(entityID).getStandardURN() == REFUGE);
//        while (!occupiedBuildings.isEmpty()) {
//            this.pathPlanning.setDestination(set);
//            List<EntityID> path = this.pathPlanning.calc().getResult();
//            if (path != null && path.size() > 0) {
//                id = path.get(path.size() - 1);//获取终点
//                if (id == null) {
//                    util.debugOverall("到" + id + "的路径终点为null.(impossible)");
//                    continue;
//                }
//                ATBuilding atb = ATBS.getByID(id);
//                if (atb.isBurning() || !atb.isReachable()) {
//                    //debug
//                    if (atb.isBurning()) util.debugSpecific("该建筑(" + atb.getId() + ")正在燃烧，去除");
//                    if (!atb.isReachable()) util.debugSpecific("该建筑(" + atb.getId() + ")无法到达，去除");
//                    util.debugSpecific("重新计算");
//                    set.remove(atb.getId());
//                    continue;
//                }
//                if (!atb.isNeedToSearch()) {
//                    util.debugSpecific("该建筑(" + atb.getId() + ")已经没必要搜了，取出");
//                    set.remove(atb.getId());
//                    continue;
//                }
//                this.myWay = path;
//                best = id;
//                break;
//            }
//        }
//        if (best != null) {
//            //解挂
//            ATBS.getByID(best).setOccupied(false);
//            util.debugSpecific("最优抢占成功，确定目标为:" + best);
//            return ATBS.getByID(best);
//        } else {
//            util.debugSpecific("最优抢占失败。");
//            return null;
//        }
//    }

//    private boolean isStationary(){
//        util.debugSpecific("判断是否静止");
//        if(lastPosition != null){
//            util.debugSpecific("走了 "+
//                    Util.getdistance(worldInfo.getLocation(lastPosition),worldInfo.getLocation(nowPosition)));
//            if(Util.getdistance(worldInfo.getLocation(lastPosition),worldInfo.getLocation(nowPosition)) < 2000){
////                if(target != null){
////                    ATBS.setVisited(target);
////                }
//                util.debugSpecific(agentInfo.getID()+":静止不动");
//                return true;
//            }
//        }
//        util.debugSpecific("动了");
//        return false;
//    }
    private boolean isCurrentTargetReachable(){
        return util.isBuildingReachable(pathPlanning, target);
    }

    //innocent
    private boolean isWayAvailable(){
        if(myWay == null || myWay.size() <= 0) return true;
        Collections.reverse(myWay);
        for (EntityID wayAreaID : myWay){
            StandardEntity entity = worldInfo.getEntity(wayAreaID);
            if(entity instanceof Building){
                ATBuilding atb = ATBS.getByID(wayAreaID);
                if(atb.isBurning()){
                    ATBuilding targetATB = ATBS.getByID(target);
                    if(wayAreaID == null){
                        util.debugSpecific("当前道路上有null(impossible)");
                    }
                    targetATB.setWayBurning(true, wayAreaID);
                    ATBS.addWayBurningBuilding(targetATB);
                    return false;
                }
            }else{
                continue;
            }
        }
        return true;
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

    //添加
    private boolean remainToBeSaved(){
//        worldInfo.getBuriedHumans()
        return false;
    }

    public boolean needToChangeTarget(){
//        return this.target == null || lastTargetHasFinished()
//                || !ATBS.getByID(target).isReachable() || ATBS.getByID(target).isBurning();
        String msg = "";
        boolean need = false;
        if (this.target == null) {
            need = true;
            msg = "target为null";
        } else if (lastTargetHasFinished()) {
            need = true;
            msg = "上一个目标已经搜完(" + target + ")";
        } else if (!ATBS.getByID(target).isReachable()) {
            need = true;
            msg = "上一个目标无法到达";
        } else if (ATBS.getByID(target).isBurning()) {
            need = true;
            msg = "上一个目标正在燃烧";
        } else if (!ATBS.getByID(target).isNeedToSearch()) {
            need = true;
            msg = "上一个目标已经没必要搜";
        } else if(!isWayAvailable()){
            need = true;
            msg = "上一个目标的路上有建筑着火";
        }
        if(need){
            util.debugSpecific("因为:" + msg + "换目标");
            if(this.target != null && ATBS.getByID(target).getPriority() == CSUSearchUtil.FIRST_CLASS){
                util.debugOverall("FirstClass("+this.target+")里面有人,但是因为"+msg+"换目标");
            }else{
                util.debugSpecific("因为:" + msg + "换目标");
            }
        }
        return need;
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

    public int getNowPriority(){
        return  nowPriority;
    }

    public EntityID getID(){
        return agentInfo.getID();
    }

    public static void main(String[] args) {
        long start = Calendar.getInstance().getTimeInMillis();
//        long start = System.currentTimeMillis();
        int a = 1;
        for(int i=1;i<100000;i++){
            a++;
        }
        long end = Calendar.getInstance().getTimeInMillis();
//        long end = System.currentTimeMillis();
        System.out.println(end-start);
    }

}
