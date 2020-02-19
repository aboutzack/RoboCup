package AUR.util.ambulance.ProbabilityDeterminant;

import AUR.util.ambulance.Information.BuildingInfo;
import AUR.util.ambulance.Information.RescueInfo;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;

import java.util.Collection;

/**
 * Created by armanaxh on 3/17/18.
 */
public class BuildingRateDeterminer {

    public static double baseCalc(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building){
        double rate = 0;
        rate += clusterEffect(wsg, rescueInfo, building, 1.3);
        rate += neaberClusterEffect(wsg, rescueInfo, building, 0.6);
        rate += otherAgentPossionEffect(wsg, rescueInfo, building, 0.5);

        return rate;
    }

    public static double calc(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building){
        double rate = building.baseRate;


        if( ignoreBulding(wsg, rescueInfo, building)){
            return 0;
        }


        rate += TravelCostToBuildingEffect(wsg, rescueInfo, building, 0.6);
        rate += distanceFromFireEffect(wsg, rescueInfo, building, 0.2);
        rate += broknessEffect(wsg, rescueInfo, building, 0.3);
        rate += buildingTemperatureEffect(wsg, rescueInfo, building, 0.2);
        rate += distanceFromRefugeEffect(wsg, rescueInfo, building, 0.15);

        if(rate >= 1){
            rate += TravelCostToBuildingEffect(wsg, rescueInfo, building, 1.3);
            rate += distanceFromRefugeInSearchEffect(wsg, rescueInfo, building, 0.2);
            rate += otherAgentPossionEffect(wsg, rescueInfo, building, 0.5);

        }

        // more effectess
        // distance from Cluster without
        // distance form Gas Station
        // Civilian Rally


        return rate;
    }

    public static boolean ignoreBulding(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building){

        if(rescueInfo == null || wsg == null || building == null){
            return true;
        }
//        if(building.me instanceof Refuge){
//            return true;
//        }
        if(building.me.getStandardURN().equals(StandardEntityURN.REFUGE)){
            return true;
        }
        if(          building.me.isOnFire()
                ||  (building.me.isFierynessDefined() && building.me.getFieryness() == 8)
                ||  (building.me.isBrokennessDefined() && building.me.getBrokenness() == 0) ){
            return true;
        }
        if(rescueInfo.visitedList.contains(building)){
            return true;
        }
        AURAreaGraph areaB = wsg.getAreaGraph(building.me.getID());
        if(areaB != null) {
            if (areaB.isBuilding()) {
                if (areaB.getBuilding().fireSimBuilding.isOnFire() || areaB.getBuilding().fireSimBuilding.getEstimatedFieryness() == 8) {
                    return true;
                }
            }
        }
        if(building.me.isOnFire() || (building.me.isFierynessDefined() && building.me.getFieryness() == 8) ){
            return true;
        }
        if(TravelCostToBuildingEffect(wsg, rescueInfo, building, 1) < 0 ){
            return true;
        }

        if(building.me.isTemperatureDefined() && building.me.getTemperature() > 44){
            return true;
        }

        return false;
    }
    public static double clusterEffect(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building , double coefficient){

        for(StandardEntity entity : rescueInfo.clusterEntity){
            if(entity.getID().equals(building.me.getID())){
                return 1* coefficient;
            }
        }
        return 0;
    }
    public static double neaberClusterEffect(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building , double coefficient){
        for(StandardEntity entity : rescueInfo.neaberClusterEntity){
            if(entity.getID().equals(building.me.getID())){
                return 1* coefficient;
            }
        }
        return 0;
    }

    public static double TravelCostToBuildingEffect(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building , double coefficient){
        double tempRate = 0;
        if(rescueInfo.maxTravelCost > building.travelCostTobulding ){
            tempRate = Math.pow(rescueInfo.maxTravelCost - building.travelCostTobulding , 2);
        }
        if(rescueInfo.maxTravelCost == 0){
            return 0;
        }
        return (tempRate / Math.pow(rescueInfo.maxTravelCost, 2) )*coefficient;
    }

    public static double distanceFromFireEffect(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building , double coefficient){
        double tempRate = rescueInfo.maxTravelCost;
        return (tempRate/ rescueInfo.maxTravelCost)*coefficient;
    }

    public static double broknessEffect(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building , double coefficient){
        if(building.me.isBrokennessDefined()) {
            double tempRate = Math.pow(RescueInfo.maxBrokness - building.me.getBrokenness() , 2);
            return (tempRate / Math.pow(RescueInfo.maxBrokness, 2) ) * coefficient;
        }
        return 0.5 * coefficient;
    }

    public static double buildingTemperatureEffect(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building , double coefficient){
        double tempRate =0;

        if(building.me.isTemperatureDefined()) {
            if(RescueInfo.maxTemperature > building.me.getTemperature()) {
                tempRate = Math.pow(RescueInfo.maxTemperature - building.me.getTemperature(), 2);
                return (tempRate / Math.pow(RescueInfo.maxTemperature, 2)) * coefficient;
            }
        }
        return 0.6 * coefficient;
    }
    public static double distanceFromRefugeEffect(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building , double coefficient){
        double tempRate = rescueInfo.maxDistance - building.distanceFromRefuge ;
        return (tempRate/ rescueInfo.maxDistance)*coefficient;
    }
    public static double distanceFromRefugeInSearchEffect(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building , double coefficient){
        int maxD = 0;
        for(BuildingInfo b : rescueInfo.searchList){
            if(b.distanceFromRefuge > maxD){
                maxD = b.distanceFromRefuge;
            }
        }
        if(maxD == 0){
            return 0;
        }
        double tempRate = maxD - building.distanceFromRefuge ;
        return (tempRate/ maxD)*coefficient;
    }
    //TODO
    public static double otherAgentPossionEffect(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building , double coefficient){

        int counter = 0;
        Collection<StandardEntity> agentList = wsg.wi.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE);
        for(StandardEntity entity : agentList){
            if(entity instanceof Human){
                Human agent = (Human)entity;
                if(agent.isPositionDefined() && agent.getPosition().equals(building.me.getID())){
//                    for(StandardEntity st : wsg.wi.getObjectsInRange(building.me.getX() , building.me.getY() , rescueInfo.maxDistance/15) ){
//                        if(st instanceof AmbulanceTeam){
//                            return 0.6*coefficient;
//                        }
//                    }
                    counter++;
                }
            }
        }
        if(counter == 1){
            return 1D*coefficient;
        }else if(counter == 2){
            return 1.2*coefficient;
        }else if(counter > 2){
            return 0.4*counter*coefficient;
        }

        return 0;
    }
}
