package viewer.layers.AmboLayers;

import AUR.util.ambulance.Information.CivilianInfo;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import rescuecore2.standard.entities.*;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;

/**
 * Created by armanaxh on 12/20/17.
 */

public class CivilianInSideBuldingInfo extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {


    }


    public String getString(AURWorldGraph wsg, AURAreaGraph selected_ag){
        String s = "";
        if(selected_ag != null) {
            Area area = selected_ag.area;
            if (wsg.rescueInfo != null) {
                for (CivilianInfo ci : wsg.rescueInfo.civiliansInfo.values()) {
                    if (ci.getPosition().equals(area.getID())) {
                        s += calc(wsg, ci);

                        s += "\n==============================\n";
                    }
                }
            }
        }
        return s;
    }

    public String calc(AURWorldGraph wsg, CivilianInfo civilian){
        String rate = "";


        double areaType = civilian.rateDeterminer.effectAreaType(0.01);
        double saveTime = civilian.rateDeterminer.effectReverseSaveTime(2.3);
        double damage = civilian.rateDeterminer.effectDamage(0.5);

        double hp = civilian.rateDeterminer.effectHp(0.5);
        double burid = civilian.rateDeterminer.effectBuriedness(0.5);
        double travelTime = civilian.rateDeterminer.effectTravelTime(0.25);
        double travelTimetoRefuge = civilian.rateDeterminer.effectTravelTimeToRefuge(0.25);
//        double otherAgent = civilian.rateDeterminer.otherAgentsRescueEffects();

        rate += "\nareaType :"+areaType;
        rate += "\nsaveTime :"+saveTime+"  > " + (double)(civilian.saveTime) ;
        rate += "\nburid :"+burid + "  > " + civilian.getBuriedness();
        rate += "\ntravelTime :"+travelTime + " > "+civilian.travelTimeToMe;
        rate += "\ntravelTimetoRefuge :"+travelTimetoRefuge + " > "+civilian.travelTimeToRefuge;
        rate += "\ndamage :"+damage + "  > "+ civilian.getDmg();
        rate += "\nhp :"+hp + "  > "+ civilian.getHp() ;
//        rate += "\nother agent : "+ otherAgent +" > "+ civilian.rateDeterminer.otherAgentsRescueEffects(1);

        double ratea = 0;
        if(civilian.rateDeterminer.ignoreCivilian()){
            rate += " \n Rate :: ignore  " + ratea + ignoreCivilian(wsg, civilian);
            return rate;
        }
        ratea += areaType;
        ratea += saveTime;
        ratea += damage;

        if(ratea > 1 ) {
            ratea += burid;
            ratea += travelTime;
            ratea += travelTimetoRefuge;
            ratea += hp;
        }

//
//        rate += " \n Rate 1:: " + ratea ;
//        if(hp == 0
//                || AmbulanceUtil.inSideRefuge(wsg, civilian.me)
//                ) {
//            ratea = 0;
//        }

        rate += " \n Rate :: " + ratea;

        return rate;
    }

    public String  ignoreCivilian(AURWorldGraph wsg, CivilianInfo civilian){
        String rate = " ";
        if(civilian.saveTime <= 0){
            return rate;
        }
        rate +="1";
        if(wsg.wi.getEntity(civilian.getPosition()) instanceof AmbulanceTeam){
            return rate;
        }
        rate +="2";
        if(otherAgentsRescueEffects(wsg, civilian)){
            return rate;
        }
        rate +="3";
        StandardEntity posEntity = wsg.wi.getEntity(civilian.getPosition());
        if(posEntity.getStandardURN().equals(StandardEntityURN.REFUGE)){
            return rate;
        }

        rate +="4";
        if(posEntity instanceof Building) {
            Building position = (Building)posEntity;
            if (position.isOnFire()) {
                return rate;
            }
        }
        rate +="5";
        return rate;
    }
    public boolean otherAgentsRescueEffects(AURWorldGraph wsg , CivilianInfo civilian){


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

}
