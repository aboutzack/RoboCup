package mrl_2019.algorithm.clustering;

import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.StaticClustering;
import javolution.util.FastMap;
import mrl_2019.util.Util;
import mrl_2019.viewer.MrlPersonalData;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created on 6/7/2017.
 *
 * @author Pooya Deldar Gohardani
 */
public class MrlAmbulanceKMeans extends StaticClustering {

    private Collection<StandardEntity> entities;

    private List<StandardEntity> centerList;
    private List<EntityID> centerIDs;
    private Map<Integer, List<StandardEntity>> clusterEntitiesList;
    private Map<Integer, Set<StandardEntity>> clusterEntitiesMap;
    private Map<Integer, Set<EntityID>> clusterEntityIdsMap;
    private int clusterSize;
    private boolean assignAgentsFlag;


    public MrlAmbulanceKMeans(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);

        // developData.getInteger("sample.module.SampleKMeans.clusterSize", 10);
//        this.assignAgentsFlag = developData.getBoolean("sample.module.SampleKMeans.assignAgentsFlag", true);
        this.centerIDs = new ArrayList<>();
        this.clusterEntitiesList = new HashMap<>();
        this.clusterEntitiesMap = new HashMap<>();
        this.clusterEntityIdsMap = new HashMap<>();
        this.centerList = new ArrayList<>();
        this.entities = wi.getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.REFUGE
        );

    }

    @Override
    public Clustering resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
        this.calcStandard();
        return this;
    }

    @Override
    public Clustering preparate() {
        super.preparate();
        if (this.getCountResume() >= 2) {
            return this;
        }
        this.calcStandard();
        return this;
    }

    private List<Point> clusterCenterPoints;

    private void calcStandard() {

        Collection<StandardEntity> allAgents = worldInfo.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM);
        List<StandardEntity> healthyAgents = new ArrayList<>();
        List<StandardEntity> unhealthyAgents = new ArrayList<>();
        allAgents.forEach(agent -> {
            StandardEntity position = worldInfo.getPosition(agent.getID());
            if (position instanceof Road) {
                healthyAgents.add(agent);
            } else {
                unhealthyAgents.add(agent);
            }
        });

        this.clusterSize = healthyAgents.size();

        Double[][] data = new Double[entities.size()][2];
        Map<Double[], StandardEntity> entityMap = new FastMap<Double[], StandardEntity>();
        Pair<Integer, Integer> location;
        int i = 0;
        for (StandardEntity entity : entities) {
            location = worldInfo.getLocation(entity.getID());
            data[i][0] = location.first().doubleValue();
            data[i][1] = location.second().doubleValue();
            entityMap.put(data[i], entity);
            i++;
        }

        int tryCount = 10;


        Kmeans_Modified kmeans = new Kmeans_Modified(data, clusterSize, tryCount);
        kmeans.calculateClusters();
        Double[][] centers = kmeans.getClusterCenters();
//        for (Double[] center : centers) {
//            clusterCenterPoints.add(new Point(center[0].intValue(), center[1].intValue()));
//        }
        ArrayList<Double[]>[] clusters = kmeans.getClusters();

        Set<EntityID> entityIdSet;

//        List<EntityCluster> entityClusters = new ArrayList<EntityCluster>();
//        List<StandardEntity> entities;
        Set<StandardEntity> entities;
        int index = 0;
        for (ArrayList<Double[]> cluster : clusters) {
//            entities = new ArrayList<StandardEntity>();
            entities = new HashSet<>();
            entityIdSet = new HashSet<>();
            if (cluster.isEmpty()) {
                continue;
            }
            for (Double[] clusterData : cluster) {
                StandardEntity entity = entityMap.get(clusterData);
                entities.add(entity);
                entityIdSet.add(entity.getID());
            }
            this.clusterEntityIdsMap.put(index, entityIdSet);
            this.clusterEntitiesMap.put(index, entities);
//            entityClusters.add(new EntityCluster(world, entities));
            index++;
        }

        int n = clusterSize;
        int m = clusterSize;
        double[][] costMatrix = new double[n][m];
//        List<StandardEntity> agents = new ArrayList<>(worldInfo.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM));

        Pair<Integer, Integer> center;
        for (int k = 0; k < n; k++) {
            for (int j = 0; j < m; j++) {
                center = new Pair<>(centers[j][0].intValue(), centers[j][1].intValue());
                costMatrix[k][j] = Util.distance(worldInfo.getLocation(healthyAgents.get(k)), center);
            }
        }

        int[] assignment = computeVectorAssignments(costMatrix);

        for (int assignmentIndex = 0; assignmentIndex < assignment.length; assignmentIndex++) {
            this.clusterEntitiesMap.get(assignmentIndex).add(healthyAgents.get(assignment[assignmentIndex]));
            this.clusterEntityIdsMap.get(assignmentIndex).add(healthyAgents.get(assignment[assignmentIndex]).getID());
        }

        int nearestCenterIndex;
        for (StandardEntity agent : unhealthyAgents) {

            nearestCenterIndex = findNearestCenter(agent, centers);
            this.clusterEntitiesMap.get(nearestCenterIndex).add(agent);
            this.clusterEntityIdsMap.get(nearestCenterIndex).add(agent.getID());
        }

        if (MrlPersonalData.DEBUG_MODE) {
            System.out.println("[" + this.getClass().getSimpleName() + "] Cluster : " + this.clusterSize);
        }

    }

    private int findNearestCenter(StandardEntity agent, Double[][] centers) {

        Pair<Integer, Integer> pair;
        int minDistance = Integer.MAX_VALUE;
        int minIndex = 0;
        int distance;
        for (int i = 0; i < centers.length; i++) {
            pair = new Pair<>(centers[i][0].intValue(), centers[i][1].intValue());
            distance = Util.distance(worldInfo.getLocation(agent.getID()), pair);
            if (distance < minDistance) {
                minDistance = distance;
                minIndex = i;
            }
        }
        return minIndex;

    }

    public int[] computeVectorAssignments(double[][] costMatrix) {
        HungarianAssignment hungarianAssignment = new HungarianAssignment(costMatrix);
        int[] temp = hungarianAssignment.execute();
        int[] result = null;
        List<Integer> tempList = new ArrayList<Integer>();

        if (temp != null) {
            for (int i : temp) {
                if (i != -1) {
                    tempList.add(i);
                }
            }

            result = new int[tempList.size()];

            for (int i = 0; i < temp.length; i++) {
                if (temp[i] != -1) {
                    result[temp[i]] = i;
                }
            }

        }

        return result;

    }


    @Override
    public int getClusterNumber() {
        return this.clusterSize;
    }

    @Override
    public int getClusterIndex(StandardEntity standardEntity) {
        return this.getClusterIndex(standardEntity.getID());
    }

    @Override
    public int getClusterIndex(EntityID entityID) {
        //TODO @MRL change this code to a better one in case of performance issues!
        for (int i = 0; i < this.clusterSize; i++) {
            if (this.clusterEntityIdsMap.get(i).contains(entityID)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Collection<StandardEntity> getClusterEntities(int i) {
        return this.clusterEntitiesMap.get(i);
    }

    @Override
    public Collection<EntityID> getClusterEntityIDs(int i) {
        return this.clusterEntityIdsMap.get(i);
    }

    @Override
    public Clustering calc() {
        return this;
    }
}
