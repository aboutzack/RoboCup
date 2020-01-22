package CSU_Yunlu_2019.standard.simplePartition;

import rescuecore2.standard.entities.StandardEntityURN;

public enum GroupingType {
    TotalBuildingArea,
    BuildingCount,
    RoadCount;

    public static GroupingType getGroupingType(StandardEntityURN urn){
        switch(urn){
            case FIRE_BRIGADE:
                return TotalBuildingArea;
            case POLICE_FORCE:
                return RoadCount;
            case AMBULANCE_TEAM:
            default:
                return BuildingCount;
        }
    }
}
