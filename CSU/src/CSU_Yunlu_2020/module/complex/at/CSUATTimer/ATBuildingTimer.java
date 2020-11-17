package CSU_Yunlu_2020.module.complex.at.CSUATTimer;

import rescuecore2.worldmodel.EntityID;

public class ATBuildingTimer extends ATTimer{
    private boolean burning = false;
    private boolean unReachable = false;
    private boolean occupied  = false;
    private boolean wayBurning = false;
    private int burningTime = 0;
    private int unReachableTime = 0;
    private int occupiedTime = 0;
    private int wayBurningTime = 0;

    public ATBuildingTimer(EntityID id) {
        super(id);
    }

    public void reasonBurning(){
        burning = true;
    }

    public void reasonUnReachable(){
        unReachable = true;
    }

    public void reasonOccupied(){
        occupied = true;
    }

    public void reasonWayBurning(){
        wayBurning = true;
    }

    public void removeBurningReason(){
        burning = false;
        burningTime = 0;
    }

    public void removeUnReachableReason(){
        unReachable = false;
        unReachableTime = 0;
    }

    public void removeOccupiedReason(){
        occupied = false;
        occupiedTime = 0;
    }

    public void removeWayBurning(){
        wayBurning = false;
        wayBurningTime = 0;
    }

    public int getBurningTime(){
        return burningTime;
    }

    public int getUnReachableTime(){
        return unReachableTime;
    }

    public int getOccupiedTime(){
        return occupiedTime;
    }

    @Override
    public void pass(){
        super.pass();
        if(burning) burningTime++;
        if(unReachable) unReachableTime++;
        if(occupied) occupiedTime++;
        if(wayBurning) wayBurningTime++;
    }
}
