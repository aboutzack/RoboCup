package CSU_Yunlu_2020.module.complex.fb.clusterSelection;

import CSU_Yunlu_2020.CSUConstants;
import CSU_Yunlu_2020.module.algorithm.fb.Cluster;
import CSU_Yunlu_2020.module.algorithm.fb.FireCluster;
import CSU_Yunlu_2020.standard.Ruler;
import CSU_Yunlu_2020.world.CSUFireBrigadeWorld;

import java.awt.*;

/**
 * @description: 根据距离选择cluster
 * @author: Guanyu-Cai
 * @Date: 03/09/2020
 */
public class DistanceBasedClusterSelector extends ClusterSelector {
    public DistanceBasedClusterSelector(CSUFireBrigadeWorld world) {
        super(world);
    }

    /**
    * @Description: 选择距离最近的一个cluster
    * @Author: Guanyu-Cai
    * @Date: 3/9/20
    */
    @Override
    public FireCluster selectCluster() {
        int myNearestClusterIndex = clustering.getMyNearestClusterIndex();
        Cluster nearestCluster;
        if (myNearestClusterIndex >= 0) {
            nearestCluster = clusters.get(myNearestClusterIndex);
        }else {
            lastClusterId = null;
            return null;
        }

        //防止在两个cluster之间来回切换
        if (lastClusterId != null) {
            Cluster lastCluster = null;
            Polygon lastPolygon = null;

            for (Cluster cluster : clusters) {
                if (cluster.getId() == lastClusterId) {
                    lastCluster = cluster;
                    lastPolygon = cluster.getConvexHull().getConvexPolygon();
                    break;
                }
            }
            if (lastCluster != null && lastPolygon != null && !(nearestCluster == lastCluster)) {
                double lastDistance = Ruler.getDistance(lastPolygon, world.getSelfLocation(), true);
                double thisDistance = Ruler.getDistance(nearestCluster.getConvexHull().getConvexPolygon(), world.getSelfLocation(), true);
                if (Math.abs(lastDistance / CSUConstants.MEAN_VELOCITY_DISTANCE - thisDistance / CSUConstants.MEAN_VELOCITY_DISTANCE) < 3) {
                    lastClusterId = lastCluster.getId();
                    if (CSUConstants.DEBUG_DISTANCE_BASED_CLUSTER_SELECTOR) {
                        System.out.println("agent: " + world.getSelfHuman().getID() + " 解决dynamic cluster徘徊问题");
                    }
                    return (FireCluster) lastCluster;
                }
            }
        }


        lastClusterId = nearestCluster.getId();
        return (FireCluster) nearestCluster;
    }
}
