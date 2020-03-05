package CSU_Yunlu_2019.module.complex.fb;

import CSU_Yunlu_2019.module.algorithm.fb.CSUFireClustering;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.Clustering;
import rescuecore2.worldmodel.EntityID;

import java.util.Collection;

/**
 * @description: 寻找灭火效益最大的建筑
 * @author: Guanyu-Cai
 * @Date: 03/05/2020
 */
public class OptimalTargetSelector implements IFireBrigadeTargetSelector {
    private AgentInfo agentInfo;
    private WorldInfo worldInfo;
    private ScenarioInfo scenarioInfo;
    private Clustering clustering;

    public OptimalTargetSelector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, Clustering clustering) {
        this.agentInfo = ai;
        this.worldInfo = wi;
        this.scenarioInfo = si;
        this.clustering = clustering;
    }

    @Override
    public EntityID calc() {
        EntityID result = null;
        CSUFireClustering fireClustering = (CSUFireClustering) clustering;
        // TODO: 3/4/20 精确的选择着火建筑的算法
        int clusterIndex = fireClustering.getMyNearestClusterIndex();
        if (clusterIndex != -1) {
            Collection<EntityID> clusterEntityIDs = fireClustering.getClusterEntityIDs(clusterIndex);
            result = (EntityID) clusterEntityIDs.toArray()[(int) (Math.random() * clusterEntityIDs.size())];
        }
        return result;
    }
}
