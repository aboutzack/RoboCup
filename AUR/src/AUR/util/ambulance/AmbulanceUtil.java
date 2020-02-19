package AUR.util.ambulance;


import AUR.util.ambulance.Information.BuildingInfo;
import AUR.util.ambulance.Information.CivilianInfo;
import AUR.util.knd.AURWorldGraph;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntityURN;

import java.util.Comparator;

/**
 *
 * @author armanaxh - 2018
 */

public class AmbulanceUtil {


    public static boolean inSideBulding(AURWorldGraph wsg, Civilian civilian){
        if(!civilian.isPositionDefined()){
            return false;
        }
        if( wsg.wi.getEntity(civilian.getPosition()).getStandardURN().equals(StandardEntityURN.BUILDING)
                || wsg.wi.getEntity(civilian.getPosition()).getStandardURN().equals(StandardEntityURN.REFUGE)
                || wsg.wi.getEntity(civilian.getPosition()).getStandardURN().equals(StandardEntityURN.FIRE_STATION)
                || wsg.wi.getEntity(civilian.getPosition()).getStandardURN().equals(StandardEntityURN.AMBULANCE_CENTRE)
                || wsg.wi.getEntity(civilian.getPosition()).getStandardURN().equals(StandardEntityURN.POLICE_OFFICE)
                || wsg.wi.getEntity(civilian.getPosition()).getStandardURN().equals(StandardEntityURN.GAS_STATION)){
            return true;
        }
        return false;
    }

    public static boolean onTheRoad(AURWorldGraph wsg, Civilian civilian){
        if(!civilian.isPositionDefined()){
            return false;
        }
        if( wsg.wi.getEntity(civilian.getPosition()).getStandardURN().equals(StandardEntityURN.ROAD)
                || wsg.wi.getEntity(civilian.getPosition()).getStandardURN().equals(StandardEntityURN.HYDRANT) ){
            return true;
        }
        return false;
    }

    public static boolean inSideRefuge(AURWorldGraph wsg, Civilian civilian){
        if(!civilian.isPositionDefined()){
            return false;
        }
        if( wsg.wi.getEntity(civilian.getPosition()).getStandardURN().equals(StandardEntityURN.REFUGE)){
            return true;
        }
        return false;
    }

    public static Comparator<CivilianInfo> CivilianRateSorter = new Comparator<CivilianInfo>() {

        public int compare(CivilianInfo a, CivilianInfo b) {
            return Double.compare(b.rate ,a.rate);// a > b
        }

    };
    public static Comparator<BuildingInfo> BuilidingRateSorter = new Comparator<BuildingInfo>() {

        public int compare(BuildingInfo a, BuildingInfo b) {
            return Double.compare(b.rate ,a.rate);// a > b
        }

    };

}
