package CSU_Yunlu_2020.module.complex.at.CSUATTimer;

import rescuecore2.worldmodel.EntityID;

import java.util.HashSet;
import java.util.Set;

public class ATTimer {
    private int hangUpTime = 0;
    private EntityID boundID;
    private static Set<ATTimer> allTimer = new HashSet<>();

    public ATTimer(EntityID id){
        this.boundID = id;
    }

    public EntityID getBoundID(){
        return boundID;
    }

    public int getHangUpTime(){
        return hangUpTime;
    }

    public void pass(){
        hangUpTime++;
    }

}
