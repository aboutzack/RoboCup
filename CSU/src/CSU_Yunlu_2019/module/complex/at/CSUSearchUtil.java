package CSU_Yunlu_2019.module.complex.at;

import CSU_Yunlu_2019.CSUConstants;
import CSU_Yunlu_2019.world.object.CSUBuilding;
import adf.agent.info.AgentInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * @author Yiji Gao
 */
//todo:1、想办法求出路程最短建筑，而不是欧几里得最短。
public class CSUSearchUtil {
    //建筑类型
    public final static int FIRST_CLASS = 6;
    public final static int SECOND_CLASS = 5;
    public final static int THIRD_CLASS = 4;
    public final static int FORTH_CLASS = 3;
    public final static int FIFTH_CLASS = 2;
    public final static int UNKNOWN_CLASS = 0;
    //智能体状态
    public final static int NORMAL = 0;
    public final static int STUCK = 1;
    public final static int STATIONARY = 2;
    //建筑状态
    public final static int ACCESSIBLE = 0;
    public final static int BURNING = 1;
    public final static int BLOCKED = 2;
    public final static int BLOCKED_AND_BURNING = 3;
    public final static int UNKNOWN = 4;
    //debug
    public final static boolean debug = false;
    public final static int monitorID = 0;
    public static WorldInfo worldInfo;
    public static AgentInfo agentInfo;

    public static void register(WorldInfo wi, AgentInfo ai){
        CSUSearchUtil.worldInfo = wi;
        CSUSearchUtil.agentInfo = ai;
        //todo 测一下有没有共享内存。
    }

    public static Collection<StandardEntity> getAllAgents(){
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE,
                StandardEntityURN.AMBULANCE_TEAM,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.AMBULANCE_CENTRE);
    }

    public static Collection<StandardEntity> getAgentsByURN(StandardEntityURN urn){
        return worldInfo.getEntitiesOfType(urn);
    }

    public static Set<EntityID> getBuildingIDs(){
        Set<EntityID> buildingIDs = new HashSet<>();
        Collection<StandardEntity> buildings = getBuildingsWithURN();
        for (StandardEntity entity : buildings) {
            buildingIDs.add(entity.getID());
        }
        return buildingIDs;
    }

    public static Collection<StandardEntity> getBuildingsWithURN() {
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.REFUGE,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.GAS_STATION);
    }

    public static boolean isBuildingBurning(EntityID entityID){
        StandardEntity entity = worldInfo.getEntity(entityID);
        if(entity instanceof Building){
            Building building = (Building) entity;
            if(building.isFierynessDefined() &&
                    building.getFieryness() > 0 && building.getFieryness() < 4){
                return true;
            }
        }
        return false;
    }

    public static boolean isBuildingReachable(PathPlanning pathPlanning, EntityID destination){
        EntityID from = agentInfo.getPosition();
        List<EntityID> result = pathPlanning.setFrom(from).setDestination(destination).getResult();
        return result != null && !result.isEmpty();
    }

    //debug
    public static void debugOverall(String message){
        if(CSUConstants.DEBUG_AT_SEARCH && debug){
            System.out.println(agentInfo.getID()+":"+message);
        }
    }

    public static void debugSpecific(String message){
        if(CSUConstants.DEBUG_AT_SEARCH && debug){
            if(agentInfo.getID().getValue() == monitorID){
                System.out.println(agentInfo.getID()+":"+message);
            }
        }
    }

}
