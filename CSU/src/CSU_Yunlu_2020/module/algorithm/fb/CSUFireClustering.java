package CSU_Yunlu_2020.module.algorithm.fb;

import CSU_Yunlu_2020.CSUConstants;
import CSU_Yunlu_2020.standard.Ruler;
import CSU_Yunlu_2020.util.Util;
import CSU_Yunlu_2020.world.CSUWorldHelper;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.DynamicClustering;
import adf.component.module.algorithm.PathPlanning;
import math.geom2d.polygon.SimplePolygon2D;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.List;
import java.util.*;

public class CSUFireClustering extends DynamicClustering {
    private int groupingDistance;
    private Map<EntityID, Cluster> entityClusterMap;
    private static int CLUSTER_RANGE_THRESHOLD;
    //生成的cluster的计数器
    private int idCounter = 1;
    private List<Cluster> clusters;
    private List<Polygon> clusterConvexPolygons;
    private int myNearestClusterIndex = -1;
    private PathPlanning pathPlanning;
    private WorldInfo worldInfo;
    private ScenarioInfo scenarioInfo;
    private AgentInfo agentInfo;
    private CSUWorldHelper world;
    private ExtAction actionFireFighting;

    public CSUFireClustering(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.groupingDistance = developData.getInteger("adf.sample.module.algorithm.SampleFireClustering.groupingDistance", 30);
        worldInfo = wi;
        scenarioInfo = si;
        agentInfo = ai;
        entityClusterMap = new HashMap<>();
        clusters = new ArrayList<>();
        clusterConvexPolygons = new ArrayList<>();
        CLUSTER_RANGE_THRESHOLD = scenarioInfo.getPerceptionLosMaxDistance();

        switch (si.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("PathPlanning.Default", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("PathPlanning.Default", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("PathPlanning.Default", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
        }
        world = moduleManager.getModule("WorldHelper.FireBrigade", CSUConstants.WORLD_HELPER_FIRE_BRIGADE);
        actionFireFighting = moduleManager.getExtAction("TacticsFireBrigade.ActionFireFighting", "adf.sample.extaction.ActionFireFighting");
    }

    @Override
    public Clustering calc() {
        calcCluster();
        this.clusterConvexPolygons.clear();
        if (getClusterNumber() > 0) {
            Map<FireCluster, Set<FireCluster>> eat = new HashMap<>();
            List<FireCluster> eatenFireClusters = new ArrayList<>();
            //计算所有cluster可以合并的所有cluster
            for (Cluster cluster1 : clusters) {
                if (eatenFireClusters.contains((FireCluster) cluster1)) continue;
                Set<FireCluster> feed = new HashSet<>();
                for (Cluster cluster2 : clusters) {
                    if (eatenFireClusters.contains((FireCluster) cluster2)) continue;
                    if (cluster1.equals(cluster2)) continue;
                    if (canEat((FireCluster) cluster1, (FireCluster) cluster2)) {
                        feed.add((FireCluster) cluster2);
                        eatenFireClusters.add((FireCluster) cluster2);
                    }
                }
                eat.put((FireCluster) cluster1, feed);
            }
            for (FireCluster nextCluster : eat.keySet()) {
                for (FireCluster c : eat.get(nextCluster)) {
                    //合并另一个cluster
                    nextCluster.eat(c);
                    // refreshing EntityClusterMap
                    for (StandardEntity entity : c.entities) {
                        entityClusterMap.remove(entity.getID());
                        //更新cluster
                        entityClusterMap.put(entity.getID(), nextCluster);
                    }
                    clusters.remove(c);
                }
            }

            //更新凸包
            for (int i = 0; i < getClusterNumber(); i++) {
                clusterConvexPolygons.add(i, clusters.get(i).getConvexHull().getConvexPolygon());
            }

            //更新myNearestClusterIndex
            double minDistance = Double.MAX_VALUE;
            int nearestClusterIndex = 0;
            for (int i = 0; i < this.clusterConvexPolygons.size(); i++) {
                double distance = Ruler.getDistance(this.clusterConvexPolygons.get(i), worldInfo.getLocation(agentInfo.getID()), false);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestClusterIndex = i;
                }
            }
            //直接选择距离最近的凸包
            myNearestClusterIndex = nearestClusterIndex;
        }else {//当前没有发现着火建筑
            myNearestClusterIndex = -1;
        }
        visualDebug();
        return this;
    }

    private void visualDebug() {
//        if (DebugHelper.DEBUG_MODE) {
//            try {
//                DebugHelper.VD_CLIENT.drawAsync(agentInfo.getID().getValue(), "DynamicClusterConvexPolygon", (Serializable) clusterConvexPolygons);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    }

    private void calcCluster() {
        Cluster cluster;
        Cluster tempCluster;
        Set<Cluster> adjacentClusters = new HashSet<>();
        Collection<StandardEntity> consideredBuildings = CSUWorldHelper.getBuildingsWithURN(worldInfo);
        EntityID withoutConsiderBuilding = null; //不考虑划入cluster的房子
//        if (actionFireFighting instanceof ActionFireFighting && agentInfo.getTime() > 1) {
//            Action lastTimeAction = agentInfo.getExecutedAction(agentInfo.getTime() - 1);
//            int lastTimeWater = ((ActionFireFighting) actionFireFighting).getWaterHistory(agentInfo.getTime() - 1);
//            if (lastTimeAction instanceof ActionExtinguish && lastTimeWater == agentInfo.getWater()) {
//                //上次灭火失败了,不考虑上次的target了
//                withoutConsiderBuilding = ((ActionExtinguish) lastTimeAction).getTarget();
////                //因为求凸包后可能会再将这个target包含进cluster,所以设置这个房子的fireness为8
//                Building building = (Building) worldInfo.getEntity(withoutConsiderBuilding);
//                CSUBuilding csuBuilding = world.getCsuBuilding(withoutConsiderBuilding);
//                building.setFieryness(8);
//                building.setTemperature(0);
//                csuBuilding.updateValues(building);
//            }
//        }
        for (StandardEntity entity : consideredBuildings) {
            Building building = (Building) entity;
            //生成新的cluster或者合并进已有的cluster
            if (building.isFierynessDefined() && building.getFieryness() != 8
                    && building.isTemperatureDefined() && building.getTemperature() > 50
                    && ! (building instanceof Refuge)
                    && !(building.getID().equals(withoutConsiderBuilding))) {
                cluster = getCluster(building.getID());
                //还未分配cluster
                if (cluster == null) {
                    cluster = new FireCluster(world);
                    cluster.add(building);
                    cluster.updateConvexHull();
                    Polygon polygon = cluster.getConvexHull().getConvexPolygon();
                    Collection<Building> buildingsInRange = getBuildingsInRange(polygon, CLUSTER_RANGE_THRESHOLD);
                    //checking neighbour clusters
                    for (StandardEntity neighbourEntity : buildingsInRange) {
                        if (!(neighbourEntity instanceof Building)) {
                            continue;
                        }
                        tempCluster = getCluster(neighbourEntity.getID());
                        if (tempCluster != null) {
                            //获取到所有相邻房屋的cluster
                            adjacentClusters.add(tempCluster);
                        }
                    }

                    if (adjacentClusters.isEmpty()) {
                        //设置cluster的id
                        cluster.setId(idCounter++);
                        //添加到cluster里保存
                        addToClusterSet(cluster, building.getID());
                    } else {
                        //将两个clustermerge,只合并了adjacentClusters的第一个
                        merge(adjacentClusters, cluster, building.getID());
                    }
                }
            } else { // remove this building if it was in a cluster
                // Was it previously in any cluster?
                cluster = getCluster(building.getID());
                if (cluster != null) {
                    cluster.remove(building);
                    entityClusterMap.remove(building.getID());//edited by sajjad, 2 lines shifted up
                    if (cluster.getBuildings().isEmpty()) {
                        clusters.remove(cluster);
                    }
                }
            }
            adjacentClusters.clear();

        }
        for (Cluster c : clusters) {
            //更新凸包
            c.updateConvexHull();
            //并将凸包内的所有房屋设置为这个cluster
            Set<StandardEntity> entitiesInShape = getEntitiesInShape(c.getConvexHull().getConvexPolygon());
            Set<Building> buildingsInShape = getBuildingsInShape(c.getConvexHull().getConvexPolygon());
            if (entitiesInShape != null && !entitiesInShape.isEmpty()) {
                c.setEntities(entitiesInShape);
                for (StandardEntity entity : entitiesInShape) {
                    entityClusterMap.put(entity.getID(), c);
                }
            }
            if (buildingsInShape != null && !buildingsInShape.isEmpty()) {
                c.setBuildings(buildingsInShape);
            }
            c.updateConvexHull();
        }
    }

    @Override
    public Clustering updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() > 1) {
            return this;
        }
        this.calc(); // invoke calc()
        this.debugStdOut("Cluster : " + clusters.size());
        return this;
    }

    @Override
    public Clustering precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() > 1) {
            return this;
        }
        pathPlanning.precompute(precomputeData);
        return this;
    }

    @Override
    public Clustering resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() > 1) {
            return this;
        }
        pathPlanning.resume(precomputeData);
        return this;
    }

    @Override
    public Clustering preparate() {
        super.preparate();
        if (this.getCountPreparate() > 1) {
            return this;
        }
        pathPlanning.preparate();
        return this;
    }

    @Override
    public int getClusterNumber() {
        return clusters.size();
    }

    @Override
    public int getClusterIndex(StandardEntity standardEntity) {
        for (int index = 0; index < clusters.size(); index++) {
            if (clusters.get(index).getEntities().contains(standardEntity)) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public int getClusterIndex(EntityID entityID) {
        return getClusterIndex(worldInfo.getEntity(entityID));
    }

    @Override
    public Collection<StandardEntity> getClusterEntities(int i) {
        if (i < clusters.size() && i > -1) {
            return clusters.get(i).getEntities();
        } else {
            return null;
        }
    }

    @Override
    public Collection<EntityID> getClusterEntityIDs(int i) {
        ArrayList<EntityID> list = new ArrayList<>();
        for (StandardEntity entity : getClusterEntities(i)) {
            list.add(entity.getID());
        }
        return list;
    }

    private boolean isBurning(Building building) {
        if (building.isFierynessDefined()) {
            switch (building.getFieryness()) {
                case 1: case 2: case 3:
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

    private void debugStdOut(String text) {
        if (scenarioInfo.isDebugMode()) {
            System.out.println("[" + this.getClass().getSimpleName() + "] " + text);
        }
    }

    private Cluster getCluster(EntityID id) {
        return entityClusterMap.get(id);
    }

    public int getMyNearestClusterIndex() {
        return myNearestClusterIndex;
    }

    public void setMyNearestClusterIndex(int myNearestClusterIndex) {
        this.myNearestClusterIndex = myNearestClusterIndex;
    }

    private void addToClusterSet(Cluster cluster, EntityID entityID) {
        entityClusterMap.put(entityID, cluster);
        clusters.add(cluster);
    }

    /**
     * @Description: 获取距离polygon在range内的所有buildings
     * @Author: Guanyu-Cai
     * @Date: 3/25/20
     */
    private Collection<Building> getBuildingsInRange(Polygon polygon, int range) {
        Polygon scaledPolygon = Util.scaleBySize(polygon, range);
        return getBuildingsInShape(scaledPolygon);
    }

    /**
     * merge new cluster to others and replace the result with all others
     *
     * @param adjacentClusters adjacent clusters to the new cluster
     * @param cluster          new constructed cluster
     * @param entityID
     */
    private void merge(Set<Cluster> adjacentClusters, Cluster cluster, EntityID entityID) {
        //原来的最大的cluster的id
        int maxCId = 0;
        for (Cluster c : adjacentClusters) {
            if (maxCId < c.getId()) {
                maxCId = c.getId();
            }
            cluster.eat(c);
            //更新cluster为最新的
            for (StandardEntity entity : c.getEntities()) {
                entityClusterMap.remove(entity.getID()); //added 25 khordad! by sajjad & peyman
                entityClusterMap.put(entity.getID(), cluster);
            }
            clusters.remove(c);
            //只合并一个cluster?
            break;//todo: remove this line to merge all possible clusters
        }
        cluster.setId(maxCId);
        addToClusterSet(cluster, entityID);
    }

    /**
    * @Description: 获取范围内的buildings和roads
    */
    public Set<StandardEntity> getEntitiesInShape(Shape shape) {
        Set<StandardEntity> result = new HashSet<>();
        for (StandardEntity next : CSUWorldHelper.getAreasWithURN(worldInfo)) {
            Area area = (Area) next;
            Pair<Integer, Integer> location = worldInfo.getLocation(area);
            if (shape.contains(location.first(), location.second()))
                result.add(next);
        }
        return result;
    }

    /**
    * @Description: 获取范围内的buildings
     * @return
    */
    public Set<Building> getBuildingsInShape(Shape shape) {
        Set<Building> result = new HashSet<>();
        for (StandardEntity next : CSUWorldHelper.getBuildingsWithURN(worldInfo)) {
            Building building = (Building) next;
            Pair<Integer, Integer> location = worldInfo.getLocation(building);
            if (shape.contains(location.first(), location.second()))
                result.add(building);
        }
        return result;
    }

    private boolean canEat(FireCluster cluster1, FireCluster cluster2) {
        int nPointsCluster1 = cluster1.getConvexHull().getConvexPolygon().npoints;
        int nPointsCluster2 = cluster2.getConvexHull().getConvexPolygon().npoints;
        double[] xPointsCluster2 = new double[nPointsCluster2];
        double[] yPointsCluster2 = new double[nPointsCluster2];
        for (int i = 0; i < nPointsCluster2; i++) {
            xPointsCluster2[i] = cluster2.getConvexHull().getConvexPolygon().xpoints[i];
            yPointsCluster2[i] = cluster2.getConvexHull().getConvexPolygon().ypoints[i];
        }

        SimplePolygon2D cluster2Polygon = new SimplePolygon2D(xPointsCluster2, yPointsCluster2);


        // TODO: 3/4/20 cluster2的大小超过了一定的范围做额外判断

        //多边形1包围了多边形2
        if (cluster1.getConvexHull().getConvexPolygon().contains(cluster2.getCenter())) {
            return true;
        }

        rescuecore2.misc.geometry.Point2D clusterCenter = new rescuecore2.misc.geometry.Point2D(cluster2.getCenter().getX(), cluster2.getCenter().getY());
        Polygon convexPolygon = cluster1.getConvexHull().getConvexPolygon();
        //如果多边形1的任一条边距离多边形2的中点距离小于30000
        for (int i = 0; i < nPointsCluster1; i++) {
            rescuecore2.misc.geometry.Point2D point1 = new rescuecore2.misc.geometry.Point2D(convexPolygon.xpoints[i], convexPolygon.ypoints[i]);
            rescuecore2.misc.geometry.Point2D point2 = new rescuecore2.misc.geometry.Point2D(convexPolygon.xpoints[(i + 1) % nPointsCluster1], convexPolygon.ypoints[(i + 1) % nPointsCluster1]);
            if (Ruler.getDistance(new rescuecore2.misc.geometry.Line2D(point1, point2), clusterCenter) < 30000) {
                return true;
            }
        }
        return false;
    }

    public List<Cluster> getClusters() {
        return clusters;
    }

    public void setClusters(List<Cluster> clusters) {
        this.clusters = clusters;
    }

    public List<Polygon> getClusterConvexPolygons() {
        return clusterConvexPolygons;
    }

    public void setClusterConvexPolygons(List<Polygon> clusterConvexPolygons) {
        this.clusterConvexPolygons = clusterConvexPolygons;
    }
}