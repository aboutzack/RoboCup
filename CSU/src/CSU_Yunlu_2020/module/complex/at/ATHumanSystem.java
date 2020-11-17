package CSU_Yunlu_2020.module.complex.at;

import CSU_Yunlu_2020.module.complex.at.CSUATTimer.ATTimerContainer;
import adf.agent.info.AgentInfo;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ATHumanSystem {
    private Map<EntityID, ATHuman> allHuman;
    private Set<ATHuman> savedHuman;
    private Set<ATHuman> inSightHuman;
    private Set<ATHuman> nowHeard;


    private AgentInfo agentInfo;
//    public Set<EntityID> all;
    private CSUSearchUtil util;
    private ATTimerContainer container;


    private boolean initialised = false;

    private ATHumanSystem(){
        allHuman = new HashMap<>();
        savedHuman = new HashSet<>();
        inSightHuman = new HashSet<>();
    }

    public ATHumanSystem(CSUSearchUtil util, AgentInfo agentInfo){
        allHuman = new HashMap<>();
        savedHuman = new HashSet<>();
        inSightHuman = new HashSet<>();
        nowHeard = new HashSet<>();
        this.agentInfo = agentInfo;
        this.util = util;
        this.container = new ATTimerContainer();
    }

//    public static ATCivilianSystem create(CSUSearchUtil util){
//        return new ATCivilianSystem(util);
//    }

    public void initialised(Set<EntityID> ids){
        initialised = true;
        for(EntityID id : ids){
            ATHuman ath = new ATHuman(id, util, container);
            allHuman.put(id, ath);
        }
    }

    public void updateSingleHuman(StandardEntity entity){
        Human human = (Human) entity;
        if(!human.isPositionDefined()){
            util.debugOverall("视线内的平民不知道position(impossible)");
        }
        if(!human.getID().equals(agentInfo.getID())){
            ATHuman atHuman = getByID(human.getID());
            inSightHuman.add(atHuman);
        }
    }

    public void updateHeard(EntityID id){
        ATHuman atHuman = getByID(id);
        atHuman.setHeard();
    }

    public void addNowHeard(EntityID id){
        ATHuman atHuman = getByID(id);
        if(!atHuman.isHeardButNotFound()){
            atHuman.setHeard();
        }
        nowHeard.add(atHuman);
    }

    public void clearNowHeard(){
        nowHeard.clear();
    }

    public ATHuman getByID(EntityID id){
        ATHuman atHuman = allHuman.get(id);
        if(atHuman == null) {
            atHuman = new ATHuman(id, util, container);
            allHuman.put(id, atHuman);
//            util.debugOverall(id+"不在allCivilian里(ATCivilian:impossible)");
        }
        return atHuman;
    }

    public Set<ATHuman> getInSightHuman() {
        return inSightHuman;
    }

    public void setHeard(EntityID id){
        getByID(id).setHeard();
    }

    public boolean isHeardEmpty(){
        for(ATHuman atc : allHuman.values()){
            if(atc.isHeardButNotFound()){
                return false;
            }
        }
        return true;
    }

    public Set<ATHuman> getHeardATHuman(){
        Set<ATHuman> set = new HashSet<>();
        for(ATHuman ath : allHuman.values()){
            if(ath.isHeardButNotFound()){
                set.add(ath);
            }
        }
        return set;
    }

    public Set<EntityID> getHeardEntityID(){
        Set<EntityID> set = new HashSet<>();
        for(ATHuman ath : allHuman.values()){
            if(ath.isHeardButNotFound()){
                set.add(ath.getId());
            }
        }
        return set;
    }

    public void passTime(){
        container.passTime();
    }
}
