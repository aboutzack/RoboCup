package AUR.util.ambulance.ProbabilityDeterminant;


import AUR.util.ambulance.AmbulanceUtil;
import AUR.util.ambulance.Information.CivilianInfo;
import AUR.util.ambulance.Information.RescueInfo;
import AUR.util.knd.AURWorldGraph;
import adf.agent.action.ambulance.ActionRescue;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;

/**
 *
 * @author armanaxh - 2018
 */


public class HumanRateDeterminer {

    private CivilianInfo civilian;
    private AURWorldGraph wsg;
    private EntityID lastPosCi = null;

    public HumanRateDeterminer(AURWorldGraph wsg, CivilianInfo ci){
        this.civilian = ci;
        this.wsg = wsg;

    }

    //TODO static
    public void calc(){
        double rate = 0;

        if(ignoreCivilian()){
            civilian.rate = rate;
            return;
        }

        if(test()){
            civilian.rate = 0.25;
            return;
        }
        //Base Rate
        rate += effectAreaType(0.01);
        rate += effectReverseSaveTime(2.3);
        rate += effectDamage(0.5);

        if(rate > 1) {
            //chose rate
            rate += effectHp(0.5);
            rate += effectBuriedness(0.5);
            rate += effectTravelTime(0.25);
            rate += effectTravelTimeToRefuge(0.25);
            rate += clusterEffect(0.25);
            // MY cluster effect
            // Civilian Rally
            // distance of Fire // kamtar bashe behtare chon mohem tare vali bayad Save Time daghig bege key mimire
            //age Raido bege civilian hast chimishe ?

        }


        civilian.rate = rate;
    }
    public boolean test(){
        StandardEntity pos = wsg.wi.getEntity(civilian.getPosition());
        if(pos instanceof Road) {
            ArrayList<EntityID> path = wsg.getPathToClosest(civilian.getPosition(), wsg.getAllRefuges());
            if (path == null || path.size() == 0) {
                return true;
            }
        }
        return false;
    }
    public boolean ignoreCivilian(){

        if(civilian.saveTime <= 0){
            return true;
        }
        if(!civilian.me.isHPDefined() || civilian.me.getHP() == 0 ){
            return true;
        }

        if(lastPosCi != null){
            if(civilian.me.isPositionDefined()){
                if(!lastPosCi.equals(civilian.me.getPosition())){
                    return true;
                }
            }
        }
        lastPosCi = civilian.me.getPosition();

        if(wsg.wi.getEntity(civilian.getPosition()) instanceof AmbulanceTeam){
            return true;
        }

        Civilian ci = civilian.me;
        if(ci.isHPDefined() && ci.isBuriednessDefined() && ci.isDamageDefined() ){
            if(ci.getHP() == RescueInfo.maxHp && ci.getBuriedness() == 0 && ci.getDamage() == 0){
                return true;
            }
        }



        if(otherAgentResuce()){
            return true;
        }
        StandardEntity posEntity = wsg.wi.getEntity(civilian.getPosition());
        if(posEntity.getStandardURN().equals(StandardEntityURN.REFUGE)){
            return true;
        }

        if(posEntity instanceof Building) {
            Building position = (Building)posEntity;
            if (position.isOnFire()) {
                return true;
            }
            if(position.isTemperatureDefined() && position.getTemperature() > 44){
                return true;
            }
        }

        return false;
    }

    public boolean otherAgentsRescueEffects(){


        for(StandardEntity entity: wsg.wi.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM)){
            if(entity instanceof AmbulanceTeam){
                AmbulanceTeam at = (AmbulanceTeam)entity;
                if(at.getID().equals(wsg.ai.getID())) {
                    continue;
                }
                if(at.isBuriednessDefined() && at.getBuriedness() == 0) {
                    if (at.isPositionDefined() && at.getPosition().equals(civilian.getPosition())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean otherAgentResuce(){
        int numberOfAmbulance = 0;
        int numberOfCivilian = 0;
        if(wsg.ai.getExecutedAction(wsg.ai.getTime()-1) instanceof ActionRescue){
            return false;
        }
        for(StandardEntity entity : wsg.wi.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM)){
            if(entity instanceof AmbulanceTeam) {
                AmbulanceTeam at = (AmbulanceTeam) entity;
                if (at.getID().equals(wsg.ai.getID())) {
                    continue;
                }
                if(at.isBuriednessDefined() && at.getBuriedness() == 0) {
                    if (at.isPositionDefined() && at.getPosition().equals(civilian.getPosition())) {
                        numberOfAmbulance++;
                    }
                }
            }
        }

        for(StandardEntity entity : wsg.wi.getEntitiesOfType(StandardEntityURN.CIVILIAN)){
            if(entity instanceof Civilian) {
                Civilian ci = (Civilian)entity;
                if (ci.isPositionDefined() && ci.getPosition().equals(civilian.getPosition())) {
                    numberOfCivilian++;
                }
            }
        }


        if(numberOfAmbulance >= numberOfCivilian ){
            return true;
        }
        return false;
    }
    public double effectAreaType(double coefficient){
        double tempRate = 0;
        if(AmbulanceUtil.inSideBulding(wsg,civilian.me)){
            tempRate += 1;
        }else if(AmbulanceUtil.onTheRoad(wsg, civilian.me)){
            tempRate += 0.3;
        }

        return tempRate * coefficient;
    }


    public double effectReverseSaveTime(double coefficient){
        double tempRate = RescueInfo.simulationTime - (civilian.saveTime);
        return (tempRate/ RescueInfo.simulationTime)*coefficient;
    }

    public double effectBuriedness(double coefficient){
        if(!civilian.me.isBuriednessDefined()){
            return 0;
        }
        if(civilian.getBuriedness() == 0){
            return 0;
        }
        double tempRate = RescueInfo.maxBuriedness - civilian.getBuriedness();
        return (tempRate/ RescueInfo.maxBuriedness)*coefficient;

    }

    public double effectTravelTime(double coefficient){
        double temprate = RescueInfo.maxTravelTime - civilian.travelTimeToMe;
        return ((temprate*1D)/ RescueInfo.maxTravelTime)*coefficient;
    }

    public double effectTravelTimeToRefuge(double coefficient){
        double temprate = RescueInfo.maxTravelTime - civilian.travelTimeToRefuge;
        return ((temprate*1D)/ RescueInfo.maxTravelTime)*coefficient;
    }

    public double effectDamage(double coefficient){
        double tempRate = civilian.getDmg();
        if(tempRate > 500 ){
            return 0;
        }
        return (tempRate/ RescueInfo.maxDamage)*coefficient;
    }

    public double effectHp(double coefficient){
        double tempRate = RescueInfo.maxHp - civilian.getHp();
        return (tempRate/ RescueInfo.maxHp)*coefficient;
    }

    public double clusterEffect(double coefficient){

        for(StandardEntity entity : wsg.rescueInfo.clusterEntity){
            if(civilian.me.isPositionDefined()) {
                if (entity.getID().equals(civilian.me.getPosition())) {
                    return 1 * coefficient;
                }
            }
        }
        return 0;
    }

}
