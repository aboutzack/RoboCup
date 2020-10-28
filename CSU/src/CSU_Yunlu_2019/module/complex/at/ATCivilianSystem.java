package CSU_Yunlu_2019.module.complex.at;

import adf.agent.info.AgentInfo;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ATCivilianSystem {
    private Map<EntityID, ATCivilian> allCivilian;
    private Set<ATCivilian> savedCivilian;
    private Set<ATCivilian> inSightCivilian;


//    public Set<EntityID> all;
    private CSUSearchUtil util;
    private ATTimerContainer container;


    private boolean initialised = false;

    private ATCivilianSystem(){
        allCivilian = new HashMap<>();
        savedCivilian = new HashSet<>();
        inSightCivilian = new HashSet<>();
    }

    public ATCivilianSystem(CSUSearchUtil util){
        allCivilian = new HashMap<>();
        savedCivilian = new HashSet<>();
        inSightCivilian = new HashSet<>();
        this.util = util;
        this.container = new ATTimerContainer();
    }

//    public static ATCivilianSystem create(CSUSearchUtil util){
//        return new ATCivilianSystem(util);
//    }

    public void initialised(Set<EntityID> ids){
        initialised = true;
        for(EntityID id : ids){
            ATCivilian atc = new ATCivilian(id, util, container);
            allCivilian.put(id, atc);
        }
    }

    public void updateSingleCivilian(StandardEntity entity){
        Civilian civilian = (Civilian) entity;
        if(!civilian.isPositionDefined()){
            util.debugOverall("视线内的平民不知道position(impossible)");
        }
        ATCivilian atCivilian = getByID(civilian.getID());
        inSightCivilian.add(atCivilian);
    }

    public void updateHeard(EntityID id){
        ATCivilian atCivilian = getByID(id);
        atCivilian.setHeard();
    }

//    public void updateInSight(StandardEntity entity){
//        Civilian civilian = (Civilian) entity;
//        if(!allCivilian.containsKey(civilian.getID())){
//            allCivilian.put()
//        }
////        if(civilian == null){
////            CSUSearchUtil.debugOverall("视野内的Civilian为null(ATCivilian:impossible)");
////        }
//        ATCivilian atCivilian = getByID(civilian.getID());
//        inSightCivilian.add(atCivilian);
//    }

    public ATCivilian getByID(EntityID id){
        ATCivilian atCivilian = allCivilian.get(id);
        if(atCivilian == null) {
            atCivilian = new ATCivilian(id, util, container);
            allCivilian.put(id, atCivilian);
//            util.debugOverall(id+"不在allCivilian里(ATCivilian:impossible)");
        }
        return atCivilian;
    }

    public Set<ATCivilian> getInSightCivilian() {
        return inSightCivilian;
    }

    public void setHeard(EntityID id){
        getByID(id).setHeard();
    }

    public boolean isHeardEmpty(){
        for(ATCivilian atc : allCivilian.values()){
            if(atc.isHeardButNotFound()){
                return false;
            }
        }
        return true;
    }

    public Set<ATCivilian> getHeardATCivilian(){
        Set<ATCivilian> set = new HashSet<>();
        for(ATCivilian atc : allCivilian.values()){
            if(atc.isHeardButNotFound()){
                set.add(atc);
            }
        }
        return set;
    }

    public Set<EntityID> getHeardEntityID(){
        Set<EntityID> set = new HashSet<>();
        for(ATCivilian atc : allCivilian.values()){
            if(atc.isHeardButNotFound()){
                set.add(atc.getId());
            }
        }
        return set;
    }

    public void passTime(){
        container.passTime();
    }
}
