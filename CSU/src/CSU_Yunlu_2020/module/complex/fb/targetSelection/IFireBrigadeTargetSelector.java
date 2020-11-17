package CSU_Yunlu_2020.module.complex.fb.targetSelection;

import CSU_Yunlu_2020.module.algorithm.fb.Cluster;

/**
 * Interface for FB target selector. When you want to write a new target
 * selector, you should extends this interface.
 *
 * @author appreciation-csu
 *
 */
public interface IFireBrigadeTargetSelector {
    /**
     * @return a FireBrigadeTarget represent the target building
     */
    FireBrigadeTarget selectTarget(Cluster targetCluster);
}
