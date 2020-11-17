package CSU_Yunlu_2020.module.complex.fb.clusterSelection;

import CSU_Yunlu_2020.module.algorithm.fb.CSUFireClustering;
import CSU_Yunlu_2020.module.algorithm.fb.Cluster;
import CSU_Yunlu_2020.world.CSUFireBrigadeWorld;

import java.awt.*;
import java.util.List;

/**
 * @author: Guanyu-Cai
 * @Date: 03/09/2020
 */
public abstract class ClusterSelector implements IFireBrigadeClusterSelector{
    protected CSUFireBrigadeWorld world;
    protected CSUFireClustering clustering;
    protected List<Cluster> clusters;
    protected List<Polygon> polygons;
    protected Integer lastClusterId;

    protected ClusterSelector (CSUFireBrigadeWorld world) {
        this.world = world;
        this.clustering = world.getFireClustering();
        this.clusters = clustering.getClusters();
        this.polygons = clustering.getClusterConvexPolygons();
    }
}
