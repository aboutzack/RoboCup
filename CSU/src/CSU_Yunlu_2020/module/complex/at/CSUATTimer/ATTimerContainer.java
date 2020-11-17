package CSU_Yunlu_2020.module.complex.at.CSUATTimer;

import rescuecore2.worldmodel.EntityID;

import java.util.HashSet;
import java.util.Set;

public class ATTimerContainer {

    private Set<ATTimer> container;

    public ATTimerContainer(){
        container = new HashSet<>();
    }

    public void register(ATTimer atTimer){
        container.add(atTimer);
    }

    public void release(ATTimer atTimer){
        container.remove(atTimer);
    }

    public void passTime(){
        for(ATTimer timer : container){
            timer.pass();
        }
    }

    public EntityID getOldest(){
        int maxTime = 0;
        EntityID id = null;
        for (ATTimer timer : container){
            int hangUpTime = timer.getHangUpTime();
            if(hangUpTime >= maxTime){
                maxTime = hangUpTime;
                id = timer.getBoundID();
            }
        }
        return id;
    }

    public EntityID getYoungest(){
        int minTime = 999999;
        EntityID id = null;
        for (ATTimer timer : container){
            int hangUpTime = timer.getHangUpTime();
            if(hangUpTime <= minTime){
                minTime = hangUpTime;
                id = timer.getBoundID();
            }
        }
        return id;
    }

//    public EntityID getOldest(Set<ATBuilding> atBuildings){
//        for(ATBuilding atb : atBuildings){
//
//        }
//    }
}
