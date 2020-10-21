package CSU_Yunlu_2019.module.complex.at;

import CSU_Yunlu_2019.debugger.DebugHelper;
import CSU_Yunlu_2019.util.Util;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.Search;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class CSUSearchRebuild extends Search{
    private boolean debug = false;
    private PathPlanning pathPlanning;
    private Clustering clustering;
    private EntityID result;
    private Collection<EntityID> unsearchedBuildingIDs;
    private CSUSearchRecorder recorder;

    public CSUSearchRebuild(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);

        this.unsearchedBuildingIDs = new HashSet<>();

        StandardEntityURN agentURN = ai.me().getStandardURN();
        switch (si.getMode()) {
            case PRECOMPUTATION_PHASE:
                if (agentURN == AMBULANCE_TEAM) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Ambulance", "adf.sample.module.algorithm.SamplePathPlanning");
                    this.clustering = moduleManager.getModule("SampleSearch.Clustering.Ambulance", "adf.sample.module.algorithm.SampleKMeans");
                }
                else if (agentURN == FIRE_BRIGADE) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire", "adf.sample.module.algorithm.SamplePathPlanning");
                    this.clustering = moduleManager.getModule("SampleSearch.Clustering.Fire", "adf.sample.module.algorithm.SampleKMeans");
                }
                else if (agentURN == POLICE_FORCE) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Police", "adf.sample.module.algorithm.SamplePathPlanning");
                    this.clustering = moduleManager.getModule("SampleSearch.Clustering.Police", "adf.sample.module.algorithm.SampleKMeans");
                }
                break;
            case PRECOMPUTED:
                if (agentURN == AMBULANCE_TEAM) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Ambulance", "adf.sample.module.algorithm.SamplePathPlanning");
                    this.clustering = moduleManager.getModule("SampleSearch.Clustering.Ambulance", "adf.sample.module.algorithm.SampleKMeans");
                }
                else if (agentURN == FIRE_BRIGADE) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire", "adf.sample.module.algorithm.SamplePathPlanning");
                    this.clustering = moduleManager.getModule("SampleSearch.Clustering.Fire", "adf.sample.module.algorithm.SampleKMeans");
                }
                else if (agentURN == POLICE_FORCE) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Police", "adf.sample.module.algorithm.SamplePathPlanning");
                    this.clustering = moduleManager.getModule("SampleSearch.Clustering.Police", "adf.sample.module.algorithm.SampleKMeans");
                }
                break;
            case NON_PRECOMPUTE:
                if (agentURN == AMBULANCE_TEAM) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Ambulance", "adf.sample.module.algorithm.SamplePathPlanning");
                    this.clustering = moduleManager.getModule("SampleSearch.Clustering.Ambulance", "adf.sample.module.algorithm.SampleKMeans");
                }
                else if (agentURN == FIRE_BRIGADE) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire", "adf.sample.module.algorithm.SamplePathPlanning");
                    this.clustering = moduleManager.getModule("SampleSearch.Clustering.Fire", "adf.sample.module.algorithm.SampleKMeans");
                }
                else if (agentURN == POLICE_FORCE) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Police", "adf.sample.module.algorithm.SamplePathPlanning");
                    this.clustering = moduleManager.getModule("SampleSearch.Clustering.Police", "adf.sample.module.algorithm.SampleKMeans");
                }
                break;
        }

        registerModule(this.pathPlanning);
        registerModule(this.clustering);

        this.recorder = new CSUSearchRecorder(agentInfo,worldInfo,scenarioInfo,this.clustering,this.pathPlanning);
        CSUSearchUtil.register(worldInfo, agentInfo);
    }

    @Override
    public Search updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        this.clustering.updateInfo(messageManager);
        this.pathPlanning.updateInfo(messageManager);
        recorder.updateInfo(messageManager, this.clustering);
        CSUSearchRecorder.registerRecorder(recorder);//??

        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }

        this.unsearchedBuildingIDs.removeAll(this.worldInfo.getChanged().getChangedEntities());

        if (this.unsearchedBuildingIDs.isEmpty()) {
            this.reset();
            this.unsearchedBuildingIDs.removeAll(this.worldInfo.getChanged().getChangedEntities());
        }
        return this;
    }

    @Override
    public Search calc() {
        boolean completeCalc = false;

        if (agentInfo.getTime() < scenarioInfo.getKernelAgentsIgnoreuntil()) {
            completeCalc = true;
        }
        if(!completeCalc && this.result == null){
            this.result = recorder.decideBest()? recorder.getTarget() : recorder.quickDecide();
            completeCalc = true;
        }
        if(!completeCalc){
            int needToChangeMessage = recorder.needToChangeTarget();
            if(needToChangeMessage != 0){
                this.result = recorder.ChangeTarget(needToChangeMessage)? recorder.getTarget() : recorder.quickDecide();
                completeCalc = true;
            }
        }
        //如果一切正常，尝试抢占。
        if(!completeCalc){
            recorder.changeTarget();
        }

        // 动态确定距离最近的建筑，用来改进decideBest，changeTarget。
//        this.pathPlanning.setFrom(this.agentInfo.getPosition());
//        this.pathPlanning.setDestination(this.unsearchedBuildingIDs);
//        List<EntityID> path = this.pathPlanning.calc().getResult();
//        if (path != null && path.size() > 0) {
//            this.result = path.get(path.size() - 1);
//        }
        visualDebug();
        return this;
    }

    private void reset() {
        this.unsearchedBuildingIDs.clear();

        Collection<StandardEntity> clusterEntities = null;
        if (this.clustering != null)
        {
            int clusterIndex = this.clustering.getClusterIndex(this.agentInfo.getID());
            clusterEntities = this.clustering.getClusterEntities(clusterIndex);

        }
        if (clusterEntities != null && clusterEntities.size() > 0)
        {
            for (StandardEntity entity : clusterEntities)
            {
                if (entity instanceof Building && entity.getStandardURN() != REFUGE)
                {
                    this.unsearchedBuildingIDs.add(entity.getID());
                }
            }
        }
        else
        {
            this.unsearchedBuildingIDs.addAll(this.worldInfo.getEntityIDsOfType(
                    BUILDING,
                    GAS_STATION,
                    AMBULANCE_CENTRE,
                    FIRE_STATION,
                    POLICE_OFFICE
            ));
        }
    }

    @Override
    public EntityID getTarget()
    {
        return this.result;
    }

    @Override
    public Search precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        return this;
    }

    @Override
    public Search resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2)
        {
            return this;
        }
        this.worldInfo.requestRollback();
        return this;
    }

    @Override
    public Search preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2)
        {
            return this;
        }
        this.worldInfo.requestRollback();
        return this;
    }

    private void visualDebug() {
        if (DebugHelper.DEBUG_MODE) {
            try {
                DebugHelper.drawSearchTarget(worldInfo, agentInfo.getID(), result);
                List<Integer> elementList = Util.fetchIdValueFromElementIds(unsearchedBuildingIDs);
                DebugHelper.VD_CLIENT.drawAsync(agentInfo.getID().getValue(), "UnsearchedBuildings", (Serializable) elementList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
