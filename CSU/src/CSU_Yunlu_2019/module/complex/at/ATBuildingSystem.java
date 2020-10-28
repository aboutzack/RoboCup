package CSU_Yunlu_2019.module.complex.at;

import adf.agent.info.AgentInfo;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * @author kyrieg
 */
public class ATBuildingSystem {
    //todo 用map效率更高
    //所有见到的建筑
    private Map<EntityID, ATBuilding> allBuildingMap;
    //已知有人的建筑
    private Set<ATBuilding> firstClassBuildings;
    //听到可能有人的建筑 todo 可以添加可能性排序
    private Set<ATBuilding> secondClassBuildings;
    //其他没搜过的建筑(聚类)
    private Set<ATBuilding> thirdClassBuildings;
    //其他没搜过的建筑（全局）
    private Set<ATBuilding> forthClassBuildings;
    //搜过的建筑
    private Set<ATBuilding> fifthClassBuildings;
    //视线之内的建筑
    private Set<ATBuilding> inSightBuildings;
    private Set<ATBuilding> searchedBuildings;

    private PathPlanning pathPlanning;
    private AgentInfo agentInfo;
    private CSUSearchUtil util;
    private ATTimerContainer container;

    private boolean initialized = false;

    private ATBuildingSystem(PathPlanning pathPlanning, AgentInfo agentInfo){
        this.allBuildingMap = new HashMap<>();
        this.pathPlanning = pathPlanning;
        this.agentInfo = agentInfo;
        this.searchedBuildings = new HashSet<>();
        this.inSightBuildings = new HashSet<>();
    }

    public ATBuildingSystem(PathPlanning pathPlanning, AgentInfo agentInfo, CSUSearchUtil util){
        this.allBuildingMap = new HashMap<>();
        this.pathPlanning = pathPlanning;
        this.agentInfo = agentInfo;
        this.searchedBuildings = new HashSet<>();
        this.inSightBuildings = new HashSet<>();
        this.util = util;
        this.container = new ATTimerContainer();
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
        for (ATBuilding atBuilding : getUnsearchableBuildings()){
            if(util.isBuildingReachable(pathPlanning,atBuilding.getId())){
                atBuilding.setReachable(true);
            }
        }
    }

    public void updateSingleBuilding(StandardEntity entity){
        Building building = (Building)entity;
        inSightBuildings.add(getByID(building.getID()));
        if (building.isFierynessDefined() && building.getFieryness() > 0 && building.getFieryness() < 4) {
            getByID(building.getID()).setBurning(true);
//            setBurning(building.getID());
        } else {
            getByID(building.getID()).setBurning(false);
//            removeBurning(building.getID());
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
            util.debugOverall("未搜索过的建筑("+atb.getId()+")提前被放进searchedBuildings(impossible)1111");
        }
        atb.setVisited();
        if(!searchedBuildings.contains(atb)){
            searchedBuildings.add(atb);
        }else {
            util.debugOverall("未搜索过的建筑("+atb.getId()+")提前被放进searchedBuildings(impossible)2222");
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

    public Set<ATBuilding> getUnsearchableBuildings(){
        Set<ATBuilding> set = new HashSet<>();
        for(ATBuilding atb : allBuildingMap.values()){
            if(!atb.isReachable()){
                set.add(atb);
            }
        }
        return set;
    }

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

//    public ATBuilding decideBestHangUp(int priority){
//        Set<ATBuilding> set = getHangUpByPriority(priority);
//        for(ATBuilding atb : set){
//            if(atb.isSearchable()){
//                return atb;
//            }
//        }
//        return null;
//    }

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

    //当前为随机选择
    public ATBuilding decideBestOccupied(int priority){
        int maxTime = 0;
        ATBuilding best = null;
        Set<ATBuilding> occupiedBuildings = getAllOccupiedByPriority(priority);
        for(ATBuilding atBuilding : occupiedBuildings){
            if(atBuilding.isBurning()){
                atBuilding.setFromOccupiedToBurning();
                continue;
            }
            if(!atBuilding.isSearchable()){
                continue;
            }
            int hangUpTime = atBuilding.getHangUptime();
            if(hangUpTime > maxTime){
                maxTime = hangUpTime;
                best = atBuilding;
            }
        }
        if(best != null) best.setOccupied(false);
        return best;
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
