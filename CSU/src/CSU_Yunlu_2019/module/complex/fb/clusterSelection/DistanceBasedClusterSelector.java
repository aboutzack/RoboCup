package CSU_Yunlu_2019.module.complex.fb.clusterSelection;

import CSU_Yunlu_2019.module.algorithm.fb.FireCluster;
import CSU_Yunlu_2019.standard.Ruler;
import CSU_Yunlu_2019.world.CSUFireBrigadeWorld;

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
        if (polygons.size() > 0) {
            double minDistance = Double.MAX_VALUE;
            int nearestClusterIndex = 0;
            for (int i = 0; i < polygons.size(); i++) {
                double distance = Ruler.getDistance(polygons.get(i), world.getSelfLocation(), false);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestClusterIndex = i;
                }
            }
            return (FireCluster) clusters.get(nearestClusterIndex);
        } else {
            return null;
        }
    }
}
