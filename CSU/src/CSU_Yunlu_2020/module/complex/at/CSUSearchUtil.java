package CSU_Yunlu_2020.module.complex.at;

import CSU_Yunlu_2020.CSUConstants;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @author Yiji Gao
 */
public class CSUSearchUtil {
    //建筑类型
    public final static int FIRST_CLASS = 6; //确定有人的建筑
    public final static int SECOND_CLASS = 5; //听到可能有人的建筑
    public final static int THIRD_CLASS = 4; //聚类内没搜过的建筑
    public final static int FORTH_CLASS = 3; //全图没搜过的建筑
    public final static int FIFTH_CLASS = 2; //搜过的建筑
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
    //世界信息
    private WorldInfo worldInfo;
    private AgentInfo agentInfo;
    private ScenarioInfo scenarioInfo;

    //debug
    public final static boolean debug = false;
    public final static boolean logMode = false;
    public boolean logCreated = false;

    public final int monitorID = 0;
    private BufferedWriter output;

    public CSUSearchUtil(WorldInfo wi, AgentInfo ai, ScenarioInfo si){
        this.worldInfo = wi;
        this.agentInfo = ai;
        this.scenarioInfo = si;
        if(logMode){
            try{
                createLog();
            }catch (IOException e){
                logCreated = false;
            }
        }
    }

    public Collection<StandardEntity> getAllAgents(){
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE,
                StandardEntityURN.AMBULANCE_TEAM,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.AMBULANCE_CENTRE);
    }

    public Collection<StandardEntity> getAgentsByURN(StandardEntityURN urn){
        return worldInfo.getEntitiesOfType(urn);
    }

    public Collection<StandardEntity> getBuildings() {
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.REFUGE,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.GAS_STATION);
    }

    public Collection<StandardEntity> getCivilians(){
        return worldInfo.getEntitiesOfType(StandardEntityURN.CIVILIAN);
    }

    public Collection<EntityID> getCivilianIDs(){
        return worldInfo.getEntityIDsOfType(StandardEntityURN.CIVILIAN);
    }

    public Collection<EntityID> getHumanIDs(){
        return worldInfo.getEntityIDsOfType(StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE, StandardEntityURN.AMBULANCE_TEAM);
    }

    public Collection<EntityID> getBuildingIDs(){
        return worldInfo.getEntityIDsOfType(StandardEntityURN.BUILDING,
                StandardEntityURN.REFUGE,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.GAS_STATION);
    }

    public boolean isBuildingBurning(EntityID entityID){
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

    public boolean isBuildingReachable(PathPlanning pathPlanning, EntityID destination){
        EntityID from = agentInfo.getPosition();
        if(from.equals(destination)){
            return true;
        }
        List<EntityID> result = pathPlanning.setFrom(from).setDestination(destination).calc().getResult();
        return result != null && !result.isEmpty();
    }

    public boolean creatingScene(){
        return agentInfo.getTime() < scenarioInfo.getKernelAgentsIgnoreuntil();
    }

    public boolean safeHangUpScope(){
        return agentInfo.getTime() < scenarioInfo.getKernelAgentsIgnoreuntil()+5;
    }

    public Building getBuilding(EntityID id){
        return (Building)worldInfo.getEntity(id);
    }

    public Civilian getCivilian(EntityID id){
        return (Civilian)worldInfo.getEntity(id);
    }

    public static String getNameByPriority(int priority){
        switch (priority){
            case FIRST_CLASS:{
                return "FIRST_CLASS";
            }
            case SECOND_CLASS:{
                return "SECOND_CLASS";
            }
            case THIRD_CLASS:{
                return "THIRD_CLASS";
            }
            case FORTH_CLASS:{
                return "FORTH_CLASS";
            }
            case FIFTH_CLASS:{
                return "FIFTH_CLASS";
            }
            default:{
                return "UNKNOWN";
            }
        }
    }

    //fileWrite
    public void createLog() throws IOException {
        String fileName = "at_log/search";
        File file = new File(fileName);
        if(!file.exists()){
            file.mkdirs();
        }
        fileName = "at_log/search/"+agentInfo.getID().toString()+".txt";
        file = new File(fileName);
        if(file.exists()){
            file.delete();
        }
        file.createNewFile();
        FileWriter writer = new FileWriter(file,true);
        this.output = new BufferedWriter(writer);
        logCreated = true;
    }

    private void write(String msg){
        if (logCreated) {
            try{
                output.write(msg+"\n");
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void flush(){
        if(logCreated){
            try {
                output.flush();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void writeAndFlush(String msg){
        try {
            output.write(msg+"\n");
            output.flush();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    //11.7
    public Human getHuman(EntityID id){
            return (Human) worldInfo.getEntity(id);
    }

    //debug
    public void debugOverall(String message){
        if(CSUConstants.DEBUG_AT_SEARCH && debug){
            System.out.println("[第"+agentInfo.getTime()+"回合]Search "+agentInfo.getID()+":"+message);
        }
    }

    public void debugSpecific(String message){
        if(CSUConstants.DEBUG_AT_SEARCH && logMode){
            writeAndFlush("[第"+agentInfo.getTime()+"回合]Search "+agentInfo.getID()+":"+message);
        }
        if(CSUConstants.DEBUG_AT_SEARCH && debug){
            if(agentInfo.getID().getValue() == monitorID){
                System.out.println("[第"+agentInfo.getTime()+"回合] "+agentInfo.getID()+":"+message);
            }
        }
    }
}
