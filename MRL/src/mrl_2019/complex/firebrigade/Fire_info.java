package mrl_2019.complex.firebrigade;


import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.PathPlanning;
import adf.sample.module.complex.SampleFireTargetAllocator;
import mrl_2019.algorithm.clustering.Cluster;
import mrl_2019.algorithm.clustering.MrlFireClustering;
import mrl_2019.world.MrlWorldHelper;
import adf.agent.module.ModuleManager;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import adf.sample.module.algorithm.SampleKMeans;

import static rescuecore2.standard.entities.StandardEntityURN.*;


/**
 * Date: May 10, 2019
 * Time: 3:40 PM create
 * Mahdi Chatri
 */

public class Fire_info {
    protected SampleFireTargetAllocator sfa;
    protected MrlFireClustering mfc;
    protected SampleKMeans sk;
    private MrlWorldHelper worldHelper;
    private PathPlanning pathPlanning;
    private WorldInfo worldInfo;
    private ScenarioInfo scenarioInfo;
    private AgentInfo agentInfo;
    private ModuleManager moduleManager;
    private DevelopData developData;
    //private MrlFireClustering mfc = new MrlFireClustering(agentInfo,worldInfo,scenarioInfo,moduleManager,developData);
    private StandardEntity sa;
    private EntityID entityID;
    List<StandardEntity> standardEntityList = new ArrayList<>();
    public Building selfBuilding;
    private Map<EntityID, Cluster> entityClusterMap;


        private Cluster getCluster(EntityID id) {
            return entityClusterMap.get(id);

        }
public Cluster cluster_building(){
            Cluster cluster0 = null;
    for (StandardEntity entity : worldInfo.getEntitiesOfType(BUILDING)) {
        Building building = (Building) entity;
        if (building.isFierynessDefined() && building.getFieryness() != 8
                && building.isTemperatureDefined() && building.getTemperature() > 25) {

            getCluster(building.getID());

            cluster0 = getCluster(building.getID());
            
            //cluster0.add(building);
//            if (cluster0 == null) {
////                    cluster = new FireCluster(world, fireClusterMembershipChecker); //old
//                cluster0 = new FireCluster(worldHelper);
//                cluster0.add(building);
//            }
        }
    }

return cluster0;

}


    public int cluster_num(){

        //MrlFireClustering mfc = new MrlFireClustering(agentInfo,worldInfo,scenarioInfo,moduleManager,developData);
        mfc = new MrlFireClustering(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);


        return mfc.getClusterNumber();

    }


    public int get_Temp(Cluster b){

        for (StandardEntity entity : worldInfo.getEntitiesOfType(BUILDING)) {
            Building building = (Building) entity;
            if (building.isFierynessDefined() && building.getFieryness() != 8
                    && building.isTemperatureDefined() && building.getTemperature() > 25) {

                int BuildingTemp = building.getTemperature();

                return BuildingTemp;

            }

        }

        return 0;
    }


    public int cluster_index(){

        return this.mfc.getClusterIndex(worldInfo.getEntity(entityID));


    }


    public boolean Burning_building(Building building) {

        // How to use : set building in Parameter  when call method

        if (building.isFierynessDefined()) {
            switch (building.getFieryness()) {
                case 1:
                case 2:
                case 3:
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }


    public int get_building(){
        int buiC = 0;
     for (StandardEntity entity : worldInfo.getEntitiesOfType(BUILDING)) {
         Building building = (Building) entity;

         buiC =building.getBuildingCode();
         return buiC;

     }


        return buiC;
 }


    public int get_Dis(StandardEntity first, StandardEntity second) {
        return worldInfo.getDistance(first, second);
        //How to use : Fire_info.getDistance(StandardEntity mordnazar A,StandardEntity mordnazar A )

    }

    public int get_Dis_id(EntityID first,EntityID second){

        return worldInfo.getDistance(first,second);
        //How to use : Fire_info.getDistance(EntityID mordnazar A,EntityID mordnazar A )
    }




    public void Fire_1(){

        List<StandardEntity> firebrigadeList = new ArrayList<>(this.worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE));
//       this.sk.assignAgents(this.worldInfo, firebrigadeList);
//
//        firebrigadeList.get(5);
//        return firebrigadeList.get(5);



    }

    public Building getSelfBuilding() {
        return selfBuilding;
    }


    public int fire_Tank(){
        StandardEntity entity = null;
        selfBuilding = (Building) entity;

        int Fire_Tank_Max = 0;

        Fire_Tank_Max=scenarioInfo.getFireTankMaximum();
        return Fire_Tank_Max;


    }


    public void fire_BrigadeAgent() {
    }
}
