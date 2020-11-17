package CSU_Yunlu_2020.module.complex.at;

import CSU_Yunlu_2020.module.complex.at.CSUATTimer.ATTimerContainer;
import adf.agent.info.AgentInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author kyrieg
 */
public class ATBuildingSystem {
    //todo 用map效率更高
    //所有见到的建筑
    private Map<EntityID, ATBuilding> allBuildingMap;
    //视线之内的建筑
    private Set<ATBuilding> inSightBuildings;
    private Set<ATBuilding> searchedBuildings;

    private final PathPlanning pathPlanning;
    private final AgentInfo agentInfo;
    private WorldInfo worldInfo;
    private CSUSearchUtil util;
    private ATTimerContainer container;

    private boolean initialized = false;

    //需要间接更新的建筑
    private Set<ATBuilding> wayBurningBuilding;
    private Set<ATBuilding> unReachableBuilding;

    private ATBuildingSystem(PathPlanning pathPlanning, AgentInfo agentInfo){
        this.allBuildingMap = new HashMap<>();
        this.pathPlanning = pathPlanning;
        this.agentInfo = agentInfo;
        this.searchedBuildings = new HashSet<>();
        this.inSightBuildings = new HashSet<>();
    }

    public ATBuildingSystem(PathPlanning pathPlanning, AgentInfo agentInfo, WorldInfo worldInfo, CSUSearchUtil util){
        this.allBuildingMap = new HashMap<>();
        this.pathPlanning = pathPlanning;
        this.agentInfo = agentInfo;
        this.searchedBuildings = new HashSet<>();
        this.inSightBuildings = new HashSet<>();
        this.util = util;
        this.container = new ATTimerContainer();
        this.worldInfo = worldInfo;
        this.wayBurningBuilding = new HashSet<>();
        this.unReachableBuilding = new HashSet<>();
    }


    public void initialized(Set<EntityID> allBuildings){
        for(EntityID id: allBuildings){
            ATBuilding atb = new ATBuilding(id, util, container);
            this.allBuildingMap.put(id, atb);
        }
        initialized = true;
    }

    public void updateInfo(){
//        for (Map.Entry<EntityID, ATBuilding> entry : allBuildingMap.entrySet()) {
//            EntityID entityID = entry.getKey();
//            ATBuilding atb = entry.getValue();
//            atb.setBurning(util.isBuildingBurning(entityID));
////            if(!util.isBuildingReachable(pathPlanning,atb.getId())) util.debugSpecific("urika");
////            atb.setReachable(util.isBuildingReachable(pathPlanning,atb.getId()));
//        }

        //每回合检查Unsearchable能不能到
        Set<ATBuilding> toRemove = new HashSet<>();
        for(ATBuilding atb : wayBurningBuilding){
            if(atb.isWayBurning()){
                EntityID id = atb.getWayBurningBuilding();
                if (id == null) {
                    util.debugOverall("wayBurning但是wayBurningBuilding为null(impossible)");
                }
                if (!getByID(id).isBurning()) {
                    atb.setWayBurning(false, null);
                    toRemove.add(atb);
                } else {
                    continue;
                }
            }else {
                util.debugOverall(atb.getId() + "的wayBurning被异常取消(impossiblee)");
                continue;
            }
        }
        wayBurningBuilding.removeAll(toRemove);
        toRemove.clear();
//        wayBurningBuilding = wayBurningBuilding.stream().filter(atb -> {
//            if (atb.isWayBurning()) {
//                EntityID id = atb.getWayBurningBuilding();
//                if (id == null) {
//                    util.debugOverall("wayBurning但是wayBurningBuilding为null(impossible)");
//                }
//                if (!getByID(id).isBurning()) {
//                    atb.setWayBurning(false, null);
//                    wayBurningBuilding.remove(atb);
//                    return false;
//                } else {
//                    return true;
//                }
//            } else {
//                util.debugOverall(atb.getId() + "的wayBurning被异常取消(impossiblee)");
//                return false;
//            }
//        }).collect(Collectors.toSet());

        //判定为不能到达的建筑20秒后释放
        for(ATBuilding atb : unReachableBuilding){
            if(!atb.isReachable()){
                if(atb.getUnReachableHangUpTime() >= 20){
                    atb.setReachable(true);
                    toRemove.add(atb);
                }else{
                    continue;
                }
            }else{
                util.debugOverall(atb.getId()+"的unReachable被异常取消(impossible)");
               continue;
            }
        }
        unReachableBuilding.removeAll(toRemove);
        toRemove.clear();
//        unReachableBuilding = unReachableBuilding.stream().filter(atb -> {
//            if(!atb.isReachable()){
//                if(atb.getUnReachableHangUpTime() >= 20){
//                    atb.setReachable(true);
//                    return false;
//                }else{
//                    return true;
//                }
//            }else{
//                util.debugOverall(atb.getId()+"的unReachable被异常取消(impossible)");
//                return false;
//            }
//        }).collect(Collectors.toSet());
//        for (ATBuilding atBuilding : getUnsearchableBuildings()){
//            if(util.isBuildingReachable(pathPlanning,atBuilding.getId())){
//                atBuilding.setReachable(true);
//            }
//        }
    }

    public void updateSingleBuilding(StandardEntity entity){
        Building building = (Building)entity;
        EntityID id = building.getID();
        inSightBuildings.add(getByID(building.getID()));
        ATBuilding atb = getByID(id);
        if(building.isFierynessDefined()){
            atb.setFieriness(building.getFieryness());
        }
        if (building.isBrokennessDefined()){
            atb.setBrokenness(building.getBrokenness());
        }
//        if (building.isFierynessDefined() && building.getFieryness() > 0 && building.getFieryness() < 4) {
//            getByID(building.getID()).setBurning(true);
////            setBurning(building.getID());
//        } else {
//            getByID(building.getID()).setBurning(false);
////            removeBurning(building.getID());
//        }

    }

    public void addWayBurningBuilding(EntityID id){
        if(id == null){
            util.debugOverall("通往null的道路上有建筑着火(impossible)");
        }else{
            ATBuilding atb = getByID(id);
            wayBurningBuilding.add(atb);
        }
    }

    public void addWayBurningBuilding(ATBuilding atb){
        if(atb == null){
            util.debugOverall("通往null的道路上有建筑着火(impossible)");
        }else{
            wayBurningBuilding.add(atb);
        }
    }

    public void addUnreachableBuilding(EntityID id){
        if(id == null){
            util.debugOverall("建筑null没有路可以到达(impossible)");
        }else{
            ATBuilding atb = getByID(id);
            unReachableBuilding.add(atb);
        }
    }

    public void addUnreachableBuilding(ATBuilding atb){
        if(atb == null){
            util.debugOverall("建筑null没有路可以到达(impossible)");
        }else{
            unReachableBuilding.add(atb);
        }
    }

    public ATBuilding getByID(EntityID id){
        ATBuilding atb = allBuildingMap.get(id);
        if(atb == null) util.debugOverall(id+"不在allBuilding里(impossible)");
        return atb;
    }

    public Set<ATBuilding> getInSightBuildings() {
        return inSightBuildings;
    }

    public void setVisited(EntityID id){
        ATBuilding atb = getByID(id);
        if(atb.getPriority() == CSUSearchUtil.FIFTH_CLASS){
            // 可能原因:1、at在房子里救人 2、at已经把整个地图的建筑搜完，开始搜索搜过的建筑避免at不动。
            // 3、at被卡在房子里
            util.debugOverall("搜索过的建筑("+atb.getId()+")被再次设置为Visited");
        }
        atb.setVisited();
        if(!searchedBuildings.contains(atb)){
            searchedBuildings.add(atb);
        }else {
//            util.debugOverall("未搜索过的建筑("+atb.getId()+")提前被放进searchedBuildings(impossible)2222");
        }
    }

//    public void setBurning(EntityID id){
//        ATBuilding atb = getByID(id);
//        atb.setBurning(true);
//    }
//
//    public void removeBurning(EntityID id){
//        ATBuilding atb = getByID(id);
//        atb.setBurning(false);
//    }

//    public void passHangUp(){
//        for(ATBuilding atBuilding : allBuildings){
//            if(atBuilding.isHangUp()){
//                atBuilding.passTime();
//            }
//        }
//    }

    public static Set<EntityID> toIDSet(Set<ATBuilding> buildings){
        Set<EntityID> ids = new HashSet<>();
        buildings.forEach(atb -> ids.add(atb.getId()));
        return ids;
    }

    public Set<ATBuilding> getAllBuildingMap() {
        return (Set<ATBuilding>)allBuildingMap.values();
    }

    public Set<ATBuilding> getClassBuildingsByPriority(int priority) {
        Set<ATBuilding> set = new HashSet<>();
        for(ATBuilding atb : allBuildingMap.values()){
            if(atb.getPriority() == priority){
                set.add(atb);
            }
        }
        return set;
    }

    public Set<EntityID> getUnvisitedBuildings(){
        Set<ATBuilding> atbs = new HashSet<>();
        atbs.addAll(getClassBuildingsByPriority(CSUSearchUtil.FIRST_CLASS));
        atbs.addAll(getClassBuildingsByPriority(CSUSearchUtil.SECOND_CLASS));
        atbs.addAll(getClassBuildingsByPriority(CSUSearchUtil.THIRD_CLASS));
        atbs.addAll(getClassBuildingsByPriority(CSUSearchUtil.FORTH_CLASS));
        return ATBuildingSystem.toIDSet(atbs);
    }

//    public Set<ATBuilding> getUnsearchableBuildings(){
//        Set<ATBuilding> set = new HashSet<>();
//        for(ATBuilding atb : allBuildingMap.values()){
//            if(!atb.isReachable()){
//                set.add(atb);
//            }
//        }
//        return set;
//    }

    public void passTime(){
        container.passTime();
    }

    public boolean isHangUp(EntityID id){
        return allBuildingMap.get(id).isHangUp();
    }

    public Set<ATBuilding> getAllHangUp(){
        Set<ATBuilding> hangUp = new HashSet<>();
        for(ATBuilding atBuilding : allBuildingMap.values()){
            if(atBuilding.isHangUp()){
                hangUp.add(atBuilding);
            }
        }
        return hangUp;
    }

    public Set<ATBuilding> getHangUpByPriority(int priority){
        Set<ATBuilding> hangUp = new HashSet<>();
        for(ATBuilding atBuilding : allBuildingMap.values()){
            if(atBuilding.isHangUp() && atBuilding.getPriority() == priority){
                hangUp.add(atBuilding);
            }
        }
        return hangUp;
    }

    public Set<ATBuilding> getAllOccupied(){
        Set<ATBuilding> set = new HashSet<>();
        for(ATBuilding atBuilding : allBuildingMap.values()){
            if(atBuilding.isOccupied()){
                set.add(atBuilding);
            }
        }
        return set;
    }

    public Set<ATBuilding> getAllOccupiedByPriority(int priority){
        Set<ATBuilding> set = new HashSet<>();
        for(ATBuilding atBuilding : allBuildingMap.values()){
            if(atBuilding.isOccupied() && atBuilding.getPriority() == priority){
                set.add(atBuilding);
            }
        }
        return set;
    }

//    //当前为随机选择//todo
//    public ATBuilding decideBestOccupied(int priority){
//        int maxTime = 0;
//        ATBuilding best = null;
//        Set<ATBuilding> occupiedBuildings = getAllOccupiedByPriority(priority);
//        for(ATBuilding atBuilding : occupiedBuildings){
//            if(atBuilding.isBurning()){
//                atBuilding.setOccupied(false);
//                atBuilding.setBurning(true);
//                continue;
//            }
//            if(!atBuilding.isSearchable()){
//                continue;
//            }
//            int hangUpTime = atBuilding.getHangUptime();
//            if(hangUpTime > maxTime){
//                maxTime = hangUpTime;
//                best = atBuilding;
//            }
//        }
//        if(best != null) best.setOccupied(false);
//        return best;
//    }


    //11.7
    public Set<EntityID> removeBad(Set<EntityID> set){
        return set.stream().filter(id ->{
            ATBuilding atb = getByID(id);
            if(atb.isBurning()){
                return false;
            }
            if(!atb.isReachable()){
                return false;
            }
            if(atb.isOccupied()){
                return false;
            }
            return atb.isNeedToSearch();
        }).collect(Collectors.toSet());
    }


    //debug
    public void printNowCluster(){
        Set<ATBuilding> set = new HashSet<>();
        for (ATBuilding atBuilding : allBuildingMap.values()){
            if(atBuilding.getPriority() == CSUSearchUtil.THIRD_CLASS){
                set.add(atBuilding);
            }
        }
        util.debugSpecific("当前聚类成员有:"+set);
    }


}
