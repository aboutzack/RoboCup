package mrl_2019.algorithm.clustering;

import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.StaticClustering;
import mrl_2019.util.Util;
import mrl_2019.viewer.MrlPersonalData;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * Created by Peyman on 7/12/2017.
 *
 * @author Peyman
 * @author Pooya
 */
public class MrlFireBrigadeKMeans extends StaticClustering {

    private Collection<StandardEntity> entities;
    private Map<Integer, Set<StandardEntity>> clusterEntitiesMap;
    private Map<Integer, Set<EntityID>> clusterEntityIdsMap;
    private int clusterSize;

    public MrlFireBrigadeKMeans(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.clusterEntitiesMap = new HashMap<>();
        this.clusterEntityIdsMap = new HashMap<>();
        this.entities = wi.getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.REFUGE,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.GAS_STATION
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

    private void calcStandard() {
        List<StandardEntity> allAgents = new ArrayList<>(worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE));

        int numberOfAgentsPerCluster = 5;
        this.clusterSize = allAgents.size() / numberOfAgentsPerCluster;

        Double[][] data = new Double[entities.size()][2];
        Map<Double[], StandardEntity> entityMap = new HashMap<Double[], StandardEntity>();
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

        ArrayList<Double[]>[] clusters = kmeans.getClusters();
        Set<EntityID> entityIdSet;
        Set<StandardEntity> entities;

        int index = 0;
        for (ArrayList<Double[]> cluster : clusters) {
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
            index++;
        }


        List<Pair<Integer, Integer>> centerList = new ArrayList<>();
        Map<Integer, Integer> clusterIndexMap = new HashMap<>();
//        if (clusterSize < allAgents.size()) {
        int clusterIndex = 0;
        for (int agentIndex = 0; agentIndex < allAgents.size(); agentIndex++) {
            if (clusterIndex == clusterSize) {
                clusterIndex = 0;
            }
            centerList.add(new Pair<>(centers[clusterIndex][0].intValue(), centers[clusterIndex][1].intValue()));
            clusterIndexMap.put(agentIndex, clusterIndex);
            clusterIndex++;
        }


//        }


        int n = allAgents.size();
        int m = centerList.size();
        double[][] costMatrix = new double[n][m];

        Pair<Integer, Integer> center;
        for (int k = 0; k < n; k++) {
            for (int j = 0; j < m; j++) {
//                center = new Pair<>(centers[j][0].intValue(), centers[j][1].intValue());
                costMatrix[k][j] = Util.distance(worldInfo.getLocation(allAgents.get(k)), centerList.get(j));
            }
        }

        int[] assignment = computeVectorAssignments(costMatrix);

        for (int assignmentIndex = 0; assignmentIndex < assignment.length; assignmentIndex++) {
            this.clusterEntitiesMap.get(clusterIndexMap.get(assignmentIndex)).add(allAgents.get(assignment[assignmentIndex]));
            this.clusterEntityIdsMap.get(clusterIndexMap.get(assignmentIndex)).add(allAgents.get(assignment[assignmentIndex]).getID());
        }


        if (MrlPersonalData.DEBUG_MODE) {
            System.out.println("[" + this.getClass().getSimpleName() + "] Cluster : " + this.clusterSize);
        }

//        for (int k = 0; k < allAgents.size(); k++) {
//            int clusterIndex = k % clusterSize;
//            this.clusterEntitiesMap.get(clusterIndex).add(allAgents.get(k));
//            this.clusterEntityIdsMap.get(clusterIndex).add(allAgents.get(k).getID());
//        }
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
