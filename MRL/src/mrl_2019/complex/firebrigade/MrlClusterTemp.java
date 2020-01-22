package mrl_2019.complex.firebrigade;


import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import mrl_2019.algorithm.clustering.MrlFireClustering;
import mrl_2019.world.MrlWorldHelper;

/**
 * @author shima
 * 9/June/2019
 */

public class MrlClusterTemp extends MrlWorldHelper {
    private ClusterTemperature tempCluster1;


    public MrlClusterTemp(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
    }


    @Override
    public MrlWorldHelper precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }

        tempCluster1 = new ClusterTemperature(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);

        return this;
    }

    @Override
    public MrlWorldHelper resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
        tempCluster1 = new ClusterTemperature(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);

        return this;
    }

    @Override
    public MrlWorldHelper preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }
        tempCluster1  = new ClusterTemperature(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);

        return this;
    }


    public ClusterTemperature getClusterTemp1() {
        return tempCluster1;
    }

    public void setTempCluster1(MrlFireClustering fireClustering) {
        this.tempCluster1 = tempCluster1;
    }

}
