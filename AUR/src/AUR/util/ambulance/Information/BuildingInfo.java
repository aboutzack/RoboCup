package AUR.util.ambulance.Information;

import AUR.util.ambulance.ProbabilityDeterminant.BuildingRateDeterminer;
import AUR.util.knd.AURWorldGraph;
import rescuecore2.standard.entities.*;

/**
 *
 * @author armanaxh - 2018
 */

public class BuildingInfo {

    public double rate = 0;
    public double baseRate = 0;
    public boolean visit = false;
    public AURWorldGraph wsg;
    public RescueInfo rescueInfo;
    public final Building me;
    public int travelCostTobulding = RescueInfo.MAXDistance;
    public int distanceFromFire = RescueInfo.MAXDistance  ;
    public int distanceFromRefuge = RescueInfo.MAXDistance;

    public BuildingInfo(AURWorldGraph wsg, RescueInfo rescueInfo, Building me) {
        this.wsg = wsg;
        this.rescueInfo = rescueInfo;
        this.me = me;

        init();
    }

    // init *****************************************************************************

    private void init(){
        this.travelCostTobulding = calcTravelCostToBuilding();
        this.distanceFromFire = calcDistanceFromFire();
        this.distanceFromRefuge = calcDistanceFromRefuge();
        this.baseRate = BuildingRateDeterminer.baseCalc(wsg, rescueInfo, this);
        this.rate = BuildingRateDeterminer.calc(wsg, rescueInfo, this);

    }

    private int calcDistanceFromRefuge(){

        int minDis = rescueInfo.maxDistance;
        for(StandardEntity entity : wsg.wi.getEntitiesOfType(StandardEntityURN.REFUGE)){
            if(entity instanceof Refuge){
                Refuge refuge = (Refuge)entity;
                int refugeDis = wsg.wi.getDistance(me.getID(), refuge.getID());
                if( refugeDis < minDis){
                    minDis = refugeDis;
                }
            }
        }

        return minDis;
    }


    // update *****************************************************************************

    public void updateInformation(){
        this.travelCostTobulding = calcTravelCostToBuilding();
        this.distanceFromFire = calcDistanceFromFire();
        if(rate != 0 || wsg.ai.getTime() < 3) {
            this.rate = BuildingRateDeterminer.calc(wsg, rescueInfo, this);
        }

    }

    // Calc *****************************************************************************

    public int calcDistanceFromFire(){

        return RescueInfo.maxTravelTime;
    }

    public int calcTravelCostToBuilding(){
        if(me != null) {
            wsg.KStar(wsg.ai.getPosition());

            int tempT = RescueInfo.maxTravelTime;
            StandardEntity pos = wsg.wi.getEntity(me.getID());
            if(pos instanceof Area) {
                if(wsg.getAreaGraph(me.getID()) != null) {
                    tempT = wsg.getAreaGraph(me.getID()).getTravelCost();
                }
            } else if(pos instanceof AmbulanceTeam){
                AmbulanceTeam amtPos = (AmbulanceTeam)pos;
                if(wsg.getAreaGraph(amtPos.getPosition()) != null) {
                    tempT = wsg.getAreaGraph(amtPos.getPosition()).getTravelCost();
                }
            }

            return tempT;
        }
        return RescueInfo.maxTravelTime;
    }

}
