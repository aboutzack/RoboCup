package viewer.layers.AmboLayers;


import AUR.util.ambulance.Information.BuildingInfo;
import AUR.util.ambulance.Information.RescueInfo;
import AUR.util.ambulance.ProbabilityDeterminant.BuildingRateDeterminer;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntityURN;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;

/**
 * Created by armanaxh on 2018.
 */

public class BuildingInfoLayer extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {


    }


    public String getString(AURWorldGraph wsg, AURAreaGraph selected_ag){
        String s = "";

        if(selected_ag != null) {
            Area area = selected_ag.area;
            if(wsg.rescueInfo != null) {
                if (wsg.rescueInfo.buildingsInfo != null) {
                    for (BuildingInfo b : wsg.rescueInfo.buildingsInfo.values()) {
                        if (b.me.getID().equals(area.getID())) {
                            s += calc(wsg, b);

                            s += "\n==============================\n";
                        }
                    }
                }
            }
        }
        return s;
    }

    public String calc(AURWorldGraph wsg, BuildingInfo building){
        String rate = "";

        RescueInfo rescueInfo = wsg.rescueInfo;
        double clusterEffect = BuildingRateDeterminer.clusterEffect(wsg, rescueInfo, building, 1);
        double travelTime =  BuildingRateDeterminer.TravelCostToBuildingEffect(wsg, rescueInfo, building, 0.55);
        double distanceFromFire =  BuildingRateDeterminer.distanceFromFireEffect(wsg, rescueInfo, building, 0.2);
        double brokness =   BuildingRateDeterminer.broknessEffect(wsg, rescueInfo, building, 0.35);
        double teperature =  BuildingRateDeterminer.buildingTemperatureEffect(wsg, rescueInfo, building, 0.2);
        double disFormRefuge = BuildingRateDeterminer.distanceFromRefugeEffect(wsg, rescueInfo, building, 0.15);
        double otherAgent = BuildingRateDeterminer.otherAgentPossionEffect(wsg, rescueInfo, building, 1.4);

        double travelTime2 =  BuildingRateDeterminer.TravelCostToBuildingEffect(wsg, rescueInfo, building, 0.7);
        double disFormRefugeSearch = BuildingRateDeterminer.distanceFromRefugeInSearchEffect(wsg, rescueInfo, building, 0.2);

        rate += "\nclusterEffect :"+clusterEffect;
        rate += "\nTravelTime :"+travelTime+"  > " +building.travelCostTobulding ;
        rate += "\ndistanceFromFire :"+distanceFromFire + "  > "  ;
        rate += "\nbrokness :"+brokness + " > " + (building.me.isBrokennessDefined() ? building.me.getBrokenness() : 0);
        rate += "\nteperature :"+teperature + " > "+ (building.me.isTemperatureDefined() ? building.me.getTemperature() : 0 );
        rate += "\ndistance form Refuge : " + disFormRefuge + " > " + building.distanceFromRefuge;
        rate += "\nother Agent : " + otherAgent;

        rate += "\nTravelTime 2 " + travelTime2 ;
        rate += "\ndistance form Refuge : " + disFormRefugeSearch + " > " ;

        rate += " \n Rate : " + building.rate;

        rate += ignoreBulding( wsg, rescueInfo, building);
        rate += "[[[[";

        return rate;
    }
    public static String ignoreBulding(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building){
        String rate = " ";
        if(rescueInfo == null || wsg == null || building == null){
            return rate;
        }
        rate += "1";
        if(building.me.getURN().equals(StandardEntityURN.REFUGE)
                || building.me.isOnFire()
                || (building.me.isFierynessDefined() && building.me.getFieryness() == 8)
                ||  (building.me.isBrokennessDefined() && building.me.getBrokenness() == 0)){
            return rate;
        }
        rate += "2";
        if(rescueInfo.visitedList.contains(building)){
            return rate;
        }
        rate += "2";
        AURAreaGraph areaB = wsg.getAreaGraph(building.me.getID());
        if(areaB.isBuilding()){
            if(areaB.getBuilding().fireSimBuilding.isOnFire() || areaB.getBuilding().fireSimBuilding.getEstimatedFieryness() == 8 ){
                return rate;
            }
        }
        rate += "3";
        if(building.me.isOnFire() || (building.me.isFierynessDefined() && building.me.getFieryness() == 8) ){
            return rate;
        }
        rate += "4";
        if(BuildingRateDeterminer.TravelCostToBuildingEffect(wsg, rescueInfo, building, 1) < 0 ){
            return rate;
        }
        rate += "5\n";

        return rate;
    }
}
