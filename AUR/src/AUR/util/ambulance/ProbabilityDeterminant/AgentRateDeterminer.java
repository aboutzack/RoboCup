package AUR.util.ambulance.ProbabilityDeterminant;

import AUR.util.ambulance.Information.RescueInfo;
import AUR.util.knd.AURWorldGraph;
import adf.agent.action.ambulance.ActionRescue;
import rescuecore2.standard.entities.*;

/**
 * Created by armanaxh on 3/17/18.
 */
public class AgentRateDeterminer {

    public static double calc(AURWorldGraph wsg, RescueInfo rescueInfo, Human human){
        double rate = 0;


        if( ignoreAgent(wsg, rescueInfo, human)){
            return rate;
        }


        rate += clusterEffect(wsg, rescueInfo, human, 1);
        rate += travelTimeEffect(wsg, rescueInfo, human, 1);
        rate += distanceFromFireEffect(wsg, rescueInfo, human, 0.2);
        rate += buriednessEffect(wsg, rescueInfo, human, 0.35);




        return rate;
    }

    public static boolean ignoreAgent(AURWorldGraph wsg, RescueInfo rescueInfo, Human human){

        //TODO
        if(human.isHPDefined() && human.getHP() < 1000){
            return true;
        }
        if(human.isBuriednessDefined() && human.getBuriedness() == 0){
            return true;
        }
        if(wsg.wi.getEntity(human.getPosition()) instanceof AmbulanceTeam){
            return true;
        }

        StandardEntity posEntity = wsg.wi.getEntity(human.getPosition());
        if(posEntity.getStandardURN().equals(StandardEntityURN.REFUGE)){
            return true;
        }

        if(posEntity instanceof Building) {
            Building position = (Building)posEntity;
            if (position.isOnFire()) {
                return true;
            }
        }
        if(otherAgentResuce(wsg, human)){
            return true;
        }

        return false;
    }

    public static boolean otherAgentsRescueEffects(AURWorldGraph wsg, Human human){


        for(StandardEntity entity: wsg.wi.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM)){
            if(entity instanceof AmbulanceTeam){
                AmbulanceTeam at = (AmbulanceTeam)entity;
                if(at.getID().equals(wsg.ai.getID())) {
                    continue;
                }
                if(at.isBuriednessDefined() && at.getBuriedness() == 0) {
                    if (at.isPositionDefined() && at.getPosition().equals(human.getPosition())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean otherAgentResuce(AURWorldGraph wsg, Human human){
        int numberOfAmbulance = 0;
        int numberOfBuridAgent = 0;
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
                    if (at.isPositionDefined() && at.getPosition().equals(human.getPosition())) {
                        numberOfAmbulance++;
                    }
                }
            }
        }

        for(StandardEntity entity : wsg.wi.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM,StandardEntityURN.FIRE_BRIGADE,StandardEntityURN.POLICE_FORCE)){
            if(entity instanceof AmbulanceTeam || entity instanceof FireBrigade || entity instanceof PoliceForce) {
                Human hu = (Human)entity;
                if(hu.isBuriednessDefined() && hu.getBuriedness() > 0) {
                    if (hu.isPositionDefined() && hu.getPosition().equals(human.getPosition())) {
                        numberOfBuridAgent++;
                    }
                }
            }
        }
//        if(human.getPosition().getValue() == 937)
//            System.out.println(numberOfAmbulance + "  >> " + numberOfBuridAgent);
        if(numberOfAmbulance >= numberOfBuridAgent ){
            return true;
        }
        return false;
    }
    public static double clusterEffect(AURWorldGraph wsg, RescueInfo rescueInfo, Human human , double coefficient){

        for(StandardEntity entity : rescueInfo.clusterEntity){
            if(human.isPositionDefined()) {
                if (entity.getID().equals(human.getPosition())) {
                    return 1 * coefficient;
                }
            }
        }
        return 0;
    }


    public static double travelTimeEffect(AURWorldGraph wsg, RescueInfo rescueInfo, Human human , double coefficient){
        if(!human.isPositionDefined()){
            return 0;
        }
        double temprate = RescueInfo.maxTravelTime - wsg.getAreaGraph(human.getPosition()).getTravelTime();
        return ((temprate*1D)/ RescueInfo.maxTravelTime)*coefficient;
    }

    public static double distanceFromFireEffect(AURWorldGraph wsg, RescueInfo rescueInfo, Human human , double coefficient){
        double tempRate = rescueInfo.maxTravelCost;
        if(rescueInfo.maxDistance == 0){
            return 0;
        }
        return (tempRate/ rescueInfo.maxTravelCost)*coefficient;
    }
    public static double buriednessEffect(AURWorldGraph wsg, RescueInfo rescueInfo, Human human , double coefficient){
        if(!human.isBuriednessDefined()){
            return 0;
        }
        if(human.getBuriedness() == 0){
            return 0;
        }
        double tempRate = RescueInfo.maxBuriedness - human.getBuriedness();
        return (tempRate/ RescueInfo.maxBuriedness)*coefficient;

    }


}
