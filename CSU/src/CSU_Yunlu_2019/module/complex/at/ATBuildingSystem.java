package CSU_Yunlu_2019.module.complex.at;

import adf.agent.info.AgentInfo;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.worldmodel.EntityID;
import sun.management.Agent;

import java.util.HashSet;
import java.util.Set;

/**
 * @author kyrieg
 */
public class ATBuildingSystem {
    //所有见到的建筑
    private Set<ATBuilding> allBuildings;
    //已知有人的建筑
    private Set<ATBuilding> firstClassBuildings;
    //听到可能有人的建筑 todo 可以添加可能性排序
    private Set<ATBuilding> secondClassBuildings;
    //其他没搜过的建筑(聚类)
    private Set<ATBuilding> thirdClassBuildings;
    //其他没搜过的建筑（全局） //todo 尚未排除聚类
    private Set<ATBuilding> forthClassBuildings;
    //搜过的建筑
    private Set<ATBuilding> fifthClassBuildings;

    private PathPlanning pathPlanning;
    private AgentInfo agentInfo;

    private boolean initialized = false;

    private ATBuildingSystem(PathPlanning pathPlanning, AgentInfo agentInfo){
        allBuildings = new HashSet<>();
        this.pathPlanning = pathPlanning;
        this.agentInfo = agentInfo;
//        firstClassBuildings = new HashSet<>();
//        secondClassBuildings = new HashSet<>();
//        thirdClassBuildings = new HashSet<>();
//        forthClassBuildings = new HashSet<>();
//        fifthClassBuildings = new HashSet<>();
    }

    public synchronized static ATBuildingSystem load(PathPlanning pathPlanning,AgentInfo agentInfo){
        return new ATBuildingSystem(pathPlanning, agentInfo);
    }

    public void initialized(Set<EntityID> allBuildings){
        for(EntityID id: allBuildings){
            ATBuilding atb = ATBuilding.getSample(id);
            this.allBuildings.add(atb);
        }
        initialized = true;
    }

    public void updateInfo(){
        passHangUp();
        for(ATBuilding atb : allBuildings){
            if(CSUSearchUtil.isBuildingBurning(atb.getId())){
                atb.setBurning(true);
            }
            if(CSUSearchUtil.isBuildingReachable(pathPlanning, atb.getId())){
                atb.setReachable(true);
            }
        }
    }

    public boolean isKnow(EntityID entityID){
        ATBuilding atb = ATBuilding.getSample(entityID);
        return allBuildings.contains(atb);
    }

    public ATBuilding getByID(EntityID entityID){
        for(ATBuilding atb : allBuildings){
            if(atb.getId().equals(entityID)){
                return atb;
            }
        }
        return null;
    }

//    public int getPriority(EntityID entityID){
//        if(isKnow(entityID)){
//            ATBuilding atb = ATBuilding.getSample(entityID);
//            if(firstClassBuildings.contains(atb)){
//                return CSUSearchUtil.FIRST_CLASS;
//            }else if(secondClassBuildings.contains(atb)){
//                return CSUSearchUtil.SECOND_CLASS;
//            }else if(thirdClassBuildings.contains(atb)){
//                return CSUSearchUtil.THIRD_CLASS;
//            }else if(forthClassBuildings.contains(atb)){
//                return CSUSearchUtil.FORTH_CLASS;
//            }else if(fifthClassBuildings.contains(atb)){
//                return CSUSearchUtil.FIFTH_CLASS;
//            }else{
//                CSUSearchUtil.debugOverall("unknown priority of ATBuilding (impossible)");
//                return -1;
//            }
//        }else{
//            return CSUSearchUtil.UNKNOWN_CLASS;
//        }
//    }

//    public boolean checkPriority(EntityID entityID,int priority){
//        return getPriority(entityID) == priority;
//    }

//    public void tidy(){
//        tidyFirstBuildings();
//        tidySecondBuildings();
//        tidyThirdBuildings();
//        tidyForthBuildings();
//    }
//
//    private void tidyFirstBuildings(){
//        firstClassBuildings.removeIf(atb -> fifthClassBuildings.contains(atb));
//    }
//
//    private void tidySecondBuildings(){
//        secondClassBuildings.removeIf(atb -> fifthClassBuildings.contains(atb));
//        secondClassBuildings.removeIf(atb -> firstClassBuildings.contains(atb));
//    }
//
//    private void tidyThirdBuildings(){
//        thirdClassBuildings.removeIf(atb -> fifthClassBuildings.contains(atb));
//        thirdClassBuildings.removeIf(atb -> firstClassBuildings.contains(atb));
//        thirdClassBuildings.removeIf(atb -> secondClassBuildings.contains(atb));
//    }
//
//    private void tidyForthBuildings(){
//        Set<ATBuilding> all = new HashSet<>();
//        all.addAll(allBuildings);
//        all.removeIf(atb -> fifthClassBuildings.contains(atb));
//        all.removeIf(atb -> firstClassBuildings.contains(atb));
//        all.removeIf(atb -> secondClassBuildings.contains(atb));
//        all.removeIf(atb -> thirdClassBuildings.contains(atb));
//    }

//    //检查函数
//    private void checkAllOverlap(){
//        CSUSearchUtil.debugOverall("firstClassBuildings-"+checkSpecificOverlap(firstClassBuildings));
//        CSUSearchUtil.debugOverall("secondClassBuildings-"+checkSpecificOverlap(secondClassBuildings));
//        CSUSearchUtil.debugOverall("thirdClassBuildings-"+checkSpecificOverlap(thirdClassBuildings));
//        CSUSearchUtil.debugOverall("forthClassBuildings-"+checkSpecificOverlap(forthClassBuildings));
//        CSUSearchUtil.debugOverall("fifthClassBuildings-"+checkSpecificOverlap(fifthClassBuildings));
//    }

//    private String checkSpecificOverlap(Set<ATBuilding> buildings){
//        StringBuilder message = new StringBuilder("overlap:");
//        if(buildings.size() <= 0){
//            return "empty";
//        }
//        boolean first = false;
//        boolean second = false;
//        boolean third = false;
//        boolean forth = false;
//        boolean fifth = false;
//        boolean overlap = false;
//        int priority = CSUSearchUtil.UNKNOWN_CLASS;
//        for(ATBuilding atb : buildings){
//            priority = atb.getPriority();
//            break;
//        }
//        switch (priority){
//            case CSUSearchUtil.FIRST_CLASS:{
//                first = true;
//                break;
//            }
//            case CSUSearchUtil.SECOND_CLASS:{
//                second = true;
//                break;
//            }
//            case CSUSearchUtil.THIRD_CLASS:{
//                third = true;
//                break;
//            }
//            case CSUSearchUtil.FORTH_CLASS:{
//                forth = true;
//                break;
//            }
//            case CSUSearchUtil.FIFTH_CLASS:{
//                fifth = true;
//                break;
//            }
//            default:{
//                message.append("unknown type of set(impossible)");
//                return message.toString();
//            }
//        }
//        for(ATBuilding atBuilding: buildings){
//            if(!first && firstClassBuildings.contains(atBuilding)){
//                first = true;
//                overlap = true;
//                message.append("first,");
//            } else if(!second && secondClassBuildings.contains(atBuilding)){
//                second = true;
//                overlap = true;
//                message.append("second,");
//            } else if(!third && thirdClassBuildings.contains(atBuilding)){
//                third = true;
//                overlap = true;
//                message.append("third,");
//            } else if(!forth && forthClassBuildings.contains(atBuilding)){
//                forth = true;
//                overlap = true;
//                message.append("forth,");
//            } else if(!fifth && fifthClassBuildings.contains(atBuilding)){
//                fifth = true;
//                overlap = true;
//                message.append("fifth,");
//            }
//            if(first && second && third && forth && fifth){
//                break;
//            }
//        }
//        if(overlap){
//            message.append("None");
//        }
//        return message.toString();
//    }

    public void passHangUp(){
        for(ATBuilding atBuilding : allBuildings){
            if(atBuilding.isHangUp()){
                atBuilding.passTime();
            }
        }
    }

    public static Set<EntityID> toIDSet(Set<ATBuilding> buildings){
        Set<EntityID> ids = new HashSet<>();
        for(ATBuilding atb : buildings){
            ids.add(atb.getId());
        }
        return ids;
    }

    public Set<ATBuilding> getAllBuildings() {
        return allBuildings;
    }

    public Set<ATBuilding> getClassBuildings(int priority) {
        Set<ATBuilding> set = new HashSet<>();
        for(ATBuilding atb : allBuildings){
            if(atb.getPriority() == priority){
                set.add(atb);
            }
        }
        return set;
    }
}
