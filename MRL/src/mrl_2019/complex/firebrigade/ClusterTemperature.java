package mrl_2019.complex.firebrigade;

import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.module.algorithm.PathPlanning;
import mrl_2019.algorithm.clustering.Cluster;
import mrl_2019.algorithm.clustering.FireCluster;
import mrl_2019.algorithm.clustering.MrlFireClustering;
import mrl_2019.world.MrlWorldHelper;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

import static rescuecore2.standard.entities.StandardEntityURN.*;

/**
 * @author shima
 * 18/05/2019
 */


/**this class can calculation of cluster temprature &
 * get distance Calculate the distance between the agent and the building
 */

public class ClusterTemperature {

    private ClusterTemperature ct;
    public MrlFireClustering mrlFireClustering;


    private MrlWorldHelper worldHelper;

    private Map<EntityID, Cluster> entityClusterMap;

    private PathPlanning pathPlanning;
    private WorldInfo worldInfo;
    private ScenarioInfo scenarioInfo;
    private AgentInfo agentInfo;
    protected MrlFireClustering mfc;
    private EntityID entityID;
    private List<EntityID> clusterBuildingIDList;
    private List<EntityID> buildingIDList;
    private List<Integer> temprBuilding;
    private ArrayList<Integer> listOftempOfBuildingInOneCluster = new ArrayList<>();
    private List<Integer> totalClustersTemp;
    private List<Integer> lastInfoTempCluster;
    private List<Integer> totalClusterBuildingTemp;
    protected Point center;
    Building building;
    protected Set<EntityID> entities;
    Cluster buildingsOfCluster = null;
    //EntityID  buildingsOfCluster;
    Set<Cluster> tempratureOfCluster = new HashSet<>();
    int oneIndexOfListOfTotalCluster=0;
    int otherIndexOfListOfTotalCluster=0;
    int tempBuilding = 0;
    int totalBuildingsTemp = 0;
    int indexKomaki = 0;
    private List<Integer> clustersTempList;
    private ArrayList<Integer> listOfTemperaturesEachCluster = new ArrayList<Integer>();

    private ExploreManager exploreManager;

    protected Set<EntityID> burningBuildings;

    Cluster tempCluster;
    //Integer getClusterIndex;
    Fire_info numberOfCluster = new Fire_info();
    private List<Cluster> clusters;

    public ClusterTemperature(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        // this.groupingDistance = developData.getInteger("adf.sample.module.algorithm.SampleFireClustering.groupingDistance", 30);
        worldInfo = wi;
        scenarioInfo = si;
        agentInfo = ai;
        entityClusterMap = new HashMap<>();
        clusters = new ArrayList<>();
        // clusterConvexPolygons = new ArrayList<>();
        //TODO @MRL consider map scale
        // CLUSTER_RANGE_THRESHOLD = scenarioInfo.getPerceptionLosMaxDistance();

        switch (si.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("Clustering.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("Clustering.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("Clustering.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
        }


        this.worldHelper = MrlWorldHelper.load(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);

    }


    /**
     * Calculation of cluster temperature
     */

    public List<Integer> calc() {

        upTodateInfo();

        for (int i = 0; i < numberOfCluster.cluster_num(); i++) {

            getClusterIndex();

            for (StandardEntity entity : worldInfo.getEntitiesOfType(BUILDING, AMBULANCE_CENTRE, POLICE_OFFICE, FIRE_STATION, GAS_STATION)) {
                Building building = (Building) entity;
                if (entity instanceof Building) {
                    this.clusterBuildingIDList.add(findBuildingsId());//id hameye building haro mirize to in list
                }
                if (building.isFierynessDefined() && building.getFieryness() != 8 //yani building haii ke kamel nasookhte boodan
                        && building.isTemperatureDefined() && building.getTemperature() > 25) {//yani damashoon rooye damaye cluster taasir mizare
                    buildingIDList.add(building.getID());
                    temprBuilding.add(building.getTemperature());//listi az temp building haye yek clu ster

                    clustersTempList.add(getSumList());//har kodum az khune haye in list shamel damaye koli yek cluster ast

                    //hala dar har cycle in list bayad ba list cycle qabl moqayese beshe ta moteveje bshim damaye kudum cluster darhale taqiir ast

                    buildingsOfCluster = getCluster(building.getID()); // alan hameye building haye daraye sharayet yek buildingsOfCluster(cluster) ke mikhaym
                    // tooye in buildingsOfCluster ast
                    if (buildingsOfCluster == null) {
                        buildingsOfCluster = new FireCluster(worldHelper); // gereftan cluster jadid bad az tamoom shodan cluster
                        //qabli
                        buildingsOfCluster.add(building);
                    }
                }

                // tempBuilding(); //temp building morede nazaro migirim

                //  totalBuildingsTemp = tempBuilding + totalBuildingsTemp; //majmooe damaye building haye yek cluster
            }

            //totalClustersTemp.add(totalBuildingsTemp);// ye list az majmooe damaye building hast
        }
        return clustersTempList;
    }


    /**
     * Compare the temperature with the previous cycle
     */

    public void upTodateInfo() {
        lastInfoTempCluster = totalClustersTemp;
        totalClustersTemp.clear(); //list ii ke shamel damaye cluster ast ro mirize to ye list dg baraye zakhire kardan
        // va baad list qabliro clear mikone baraye in ke etelate jadid zakhire kone
    }


    public int tempBuilding() {

        Fire_info fireInfo = new Fire_info();
        tempBuilding = fireInfo.get_Temp(buildingsOfCluster);
        return tempBuilding;

    }


    private Cluster getCluster(EntityID id) {

        return entityClusterMap.get(id);

    }

    public int getClusterIndex() {

        return this.mfc.getClusterIndex(worldInfo.getEntity(entityID));

    }


    public Point getCenter() {
        return center;
    }


    /**
     * baad az moqayese damaye cluster haye mokhtalef ba estefade az in method faseye
     * agent ta oon building dakhel cluster bdast miad
     */


    public int findBestDistance() {

        Fire_info findBestDistance = new Fire_info();
        return findBestDistance.get_Dis_id(findBuildingsId(), findFireBrigadeId());
    }


    private void fireBrigade_getID() {

        Fire_info fireBrigade_getID = new Fire_info();
        fireBrigade_getID.fire_BrigadeAgent();


    }


    public EntityID findFireBrigadeId() {
        Collection<StandardEntity> fireBrigades = worldInfo.getEntitiesOfType(FIRE_BRIGADE);
        EntityID fireBrigadeID = null;

        for (StandardEntity fireEntity : fireBrigades) {
            fireBrigadeID = fireEntity.getID();
        }
        return fireBrigadeID;
    }


    public EntityID findBuildingsId() {
        Collection<StandardEntity> buildings = worldInfo.getEntitiesOfType(BUILDING);
        EntityID buildingID = null;

        for (StandardEntity buildingEntity : buildings) {
            buildingID = buildingEntity.getID();
        }
        return buildingID;
    }

    /**
     * total temp buildings in one cluster
     *
     * @return sum
     */

    public int getSumList() {
        int sum = 0;
        for (int x = 0; x <= temprBuilding.size(); x++) {
            sum += temprBuilding.get(x);
        }
        return sum;

    }


    public int findNearestClusterTemp(Cluster targetCluster) {           ///   ///EntityID building1
        Cluster cluster;
        targetCluster = mrlFireClustering.findNearestCluster((worldHelper.getSelfLocation()));


        if (building.getFieryness() > 0 && building.getFieryness() < 4) {
            burningBuildings.add(building.getID());
        } else {
            burningBuildings.remove(building.getID());
        }


        int b = 0;
        Collection<StandardEntity> entities = this.worldInfo.getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.GAS_STATION,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.POLICE_OFFICE
        );

        // Building building = (Building) entities;

        Set<StandardEntity> fireBuildings = new HashSet<>();
        for (StandardEntity entity : entities) {
            if (((Building) entity).isOnFire()) {
                fireBuildings.add(entity);
                ((Building) entity).getTemperature();
            }
            b = ((Building) entity).getTemperature();
            targetCluster = getCluster(building.getID());
//            if (targetCluster == null) {
//                targetCluster = new FireCluster(worldHelper); // gereftan cluster jadid bad az tamoom shodan cluster
//                //qabli
//                targetCluster.add(building);
//            }
            targetCluster.add(building);
        }
        totalClusterBuildingTemp.add(b);


        return getSumList1();
    }


    public int getSumList1() {
        int sum = 0;
        for (int x = 0; x <= totalClusterBuildingTemp.size(); x++) {
            sum += totalClusterBuildingTemp.get(x);
        }
        return sum;

    }

//
//    public int getSumList2() {
//        int sum = 0;
//        for (int x = 0; x <= listOftempOfBuildingInOneCluster.size(); x++) {
//            sum += listOftempOfBuildingInOneCluster.get(x);
//        }
//        return sum;
//
//    }

    public int getSumList2() {
        int sum = 0;
        for (int x : listOftempOfBuildingInOneCluster) {
            sum += listOftempOfBuildingInOneCluster.get(x);
        }
        return sum;

    }


    public List calcTemp() {
        Integer b = 0;

        if (clusters == null || clusters.isEmpty()) {
            return null;
        }
        Set<Cluster> availableClusters = new HashSet<>();
        for (Cluster cluster : clusters) {
            if (cluster.isDying() || (cluster instanceof FireCluster && !((FireCluster) cluster).isExpandableToCenterOfMap())) {
                availableClusters.add(cluster);
                continue;
            }


            for (StandardEntity entity : worldInfo.getEntitiesOfType(BUILDING, AMBULANCE_CENTRE, POLICE_OFFICE, FIRE_STATION, GAS_STATION)) {
                Building building = (Building) entity;
                if (entity instanceof Building) {
                    // this.clusterBuildingIDList.add(findBuildingsId());//id hameye building haro mirize to in list
                }
                if (building.isFierynessDefined() && building.getFieryness() != 8 //yani building haii ke kamel nasookhte boodan
                        && building.isTemperatureDefined() && building.getTemperature() > 25) {//yani damashoon rooye damaye cluster taasir mizare
                    //buildingIDList.add(building.getID());
                    // temprBuilding.add(building.getTemperature());//listi az temp building haye yek cluster

                    Set<StandardEntity> fireBuildings = new HashSet<>();
                    // for (StandardEntity entity : entities) {
                    if (((Building) entity).isOnFire()) {
                        fireBuildings.add(entity);
                        cluster = getCluster(entity.getID());
                        b = ((Building) entity).getTemperature(); //damaye yek sakhteman
                        if (cluster == null) {
                            cluster = new FireCluster(worldHelper); // gereftan cluster jadid bad az tamoom shodan cluster
                            //qabli
                            cluster.add(building);
                        }
                    }
                }
            }
            listOftempOfBuildingInOneCluster.add(b); //listi az damaye sakhteman haye tek cluster yani har
            // khune in list damaye yek sakhteman ast
            listOfTemperaturesEachCluster.add(getSumList2());


        }
        return listOfTemperaturesEachCluster;
    }


    public int findTempOfCluster(Set<EntityID> buiIds) {
        int tempOfEachCluster = 0;
        //int v = 0;
        Integer b = 0;

        if (clusters == null || clusters.isEmpty()) {
            return Integer.parseInt(null);
        }
        Set<Cluster> availableClusters = new HashSet<>();
        for (Cluster cluster : clusters) {
            if (cluster.isDying() || (cluster instanceof FireCluster && !((FireCluster) cluster).isExpandableToCenterOfMap())) {
                availableClusters.add(cluster);
                continue;
            }//end if


            Collection<StandardEntity> buildings = worldInfo.getEntitiesOfType(BUILDING);
            for (StandardEntity buildingEntity : buildings) {
                buiIds.add(buildingEntity.getID()); //buiIds ye set az id haye building ast

                Building building = (Building) buildingEntity;
                if (buildingEntity instanceof Building) {
                    // this.clusterBuildingIDList.add(findBuildingsId());//id hameye building haro mirize to in list
                }
                if (building.isFierynessDefined() && building.getFieryness() != 8 //yani building haii ke kamel nasookhte boodan
                        && building.isTemperatureDefined() && building.getTemperature() > 25) {//yani damashoon rooye damaye cluster taasir mizare
                    //buildingIDList.add(building.getID());
                    // temprBuilding.add(building.getTemperature());//listi az temp building haye yek cluster

                    Set<StandardEntity> fireBuildings = new HashSet<>();
                    // for (StandardEntity entity : entities) {
                    if (((Building) buildingEntity).isOnFire()) {
                        fireBuildings.add(buildingEntity);

                        cluster = getCluster(buildingEntity.getID());
                        b = ((Building) buildingEntity).getTemperature(); //damaye yek sakhteman
                        if (cluster == null) {
                            cluster = new FireCluster(worldHelper); // gereftan cluster jadid bad az tamoom shodan cluster
                            //qabli
                            cluster.add(buildingEntity);
                        }//end if cluster==null

                    }//end if building.isOnFire

                }//end if temp>25 bood

            }//end for find buildings
            listOftempOfBuildingInOneCluster.add(b);
            // getSumList2();

            for (int x : listOftempOfBuildingInOneCluster) {
                tempOfEachCluster += listOftempOfBuildingInOneCluster.get(x);
            }


        }//end for get cluster


        //  v = ct.getSumList2(); //v=majmooe damaye building haye yek cluster ke mishe damaye cluster


        return tempOfEachCluster;
    }


    public Cluster findBestCluster() {
        Cluster c;

       c= mrlFireClustering.findNearestCluster(worldHelper.getSelfLocation());

        for (int x : listOfTemperaturesEachCluster) {
             oneIndexOfListOfTotalCluster = listOfTemperaturesEachCluster.get(x);
             otherIndexOfListOfTotalCluster=listOfTemperaturesEachCluster.get(x+1);


            if (oneIndexOfListOfTotalCluster>otherIndexOfListOfTotalCluster) {
                c= mrlFireClustering.findNearestCluster(worldHelper.getSelfLocation());
               // c=getCluster()

            }
           else if (oneIndexOfListOfTotalCluster<otherIndexOfListOfTotalCluster){

                c= mrlFireClustering.findNearestCluster(worldHelper.getSelfLocation());
            }
           else{
                c= mrlFireClustering.findNearestCluster(worldHelper.getSelfLocation());
            }

        }
      return c;
    }
}


//                public int getIndexlistOfTemperaturesEachCluster(){
////
//                    int clusterIndexWithRisingTemp = 0;
//                    int getIndex = 0;
//                    //
//                    //for (int x = 2; x <=listOfTemperaturesEachCluster.size(); x++) {
//                    for (Integer number : listOfTemperaturesEachCluster) {
//                        getIndex = listOfTemperaturesEachCluster.get(number);
//
//                        if (getIndex > 28) {
//                            clusterIndexWithRisingTemp = getIndex;
//
//                        }
//                    }
//                    return clusterIndexWithRisingTemp;
//                }








//khorooji in classo dorost konim baad b onvane vorodi too mrlBuildingDetector worldhelper.getBuildingIDs azash estefade konim













//    public EntityID findId(){
//
//        int distance;
//        Collection<StandardEntity> fireBrigades = worldInfo.getEntitiesOfType(FIRE_BRIGADE);
//        for (StandardEntity fireEntity : fireBrigades){
//            distance = worldInfo.getDistance(fireEntity.getID(),buildingsOfCluster.getId());
//           fireEntity.getID();
//        }
//
//        return findId();
//
//    }







//    public EntityID getDistance(EntityID building,EntityID fireBrigade){
//
//        return ;
//    }

//    private Cluster findBestCluster(Cluster r){
//        Collection<StandardEntity> refuges = worldInfo.getEntitiesOfType(FIRE_BRIGADE);
//        EntityID nearestID = null;
//
//        return r;
//    }


  //  protected Cluster() {
       // entities = new HashSet<>();
    //}



    // getClusterIndex = cluster_index();

    //this.clusterBuildingIDList.add(buildingsOfCluster.getId());
    // tempBuilding=buildingsOfCluster;


