package viewer.layers.AmboLayers;


import AUR.util.ambulance.Information.RescueInfo;
import AUR.util.ambulance.ProbabilityDeterminant.AgentRateDeterminer;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;

/**
 * Created by armanaxh on 2018.
 */

public class AgentInSideArea extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {


    }




    public String getString(AURWorldGraph wsg, AURAreaGraph selected_ag){
        String s = "";
        if(selected_ag != null) {
            Area area = selected_ag.area;
            if (wsg.rescueInfo != null) {
                for (EntityID id : wsg.rescueInfo.agentsRate.keySet()) {
                    StandardEntity st = wsg.wi.getEntity(id);
                    if(st instanceof Human) {
                        Human human = (Human) st;
                        if (human.getPosition().equals(area.getID())) {
                            s += calc(wsg, human);

                            s += "\n==============================\n";
                        }
                    }
                }
            }
        }
        return s;
    }

    public String calc(AURWorldGraph wsg, Human human){
        String rate = "";

        RescueInfo rescueInfo = wsg.rescueInfo;
        double clusterEffect = AgentRateDeterminer.clusterEffect(wsg, rescueInfo, human, 1);
        double TravelTime =  AgentRateDeterminer.travelTimeEffect(wsg, rescueInfo, human, 1.2);
        double distanceFromFire =  AgentRateDeterminer.distanceFromFireEffect(wsg, rescueInfo, human, 0.3);
        double brokness =   AgentRateDeterminer.buriednessEffect(wsg, rescueInfo, human, 0.35);


        rate += "\nclusterEffect :"+clusterEffect;
        rate += "\nTravelTime :"+TravelTime+"  > " +wsg.getAreaGraph(human.getPosition()).getTravelTime() ;
        rate += "\ndistanceFromFire :"+distanceFromFire + "  > "  ;
        rate += "\nbrokness :"+brokness + " > " + (human.isBuriednessDefined() ? human.getBuriedness() : 0);

        rate += " \n Rate :: " + rescueInfo.agentsRate.get(human.getID());

        return rate;
    }

}
