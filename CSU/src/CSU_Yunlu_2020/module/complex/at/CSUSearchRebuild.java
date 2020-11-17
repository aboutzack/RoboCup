package CSU_Yunlu_2020.module.complex.at;

import CSU_Yunlu_2020.debugger.DebugHelper;
import CSU_Yunlu_2020.util.Util;
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
    private Collection<EntityID> unvisitedBuildingIDs;
    public CSUSearchRecorder recorder;
    private CSUSearchUtil util;

    public CSUSearchRebuild(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);

        this.unvisitedBuildingIDs = new HashSet<>();

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

        this.util = new CSUSearchUtil(worldInfo, agentInfo, scenarioInfo);
        this.recorder = new CSUSearchRecorder(agentInfo,worldInfo,scenarioInfo,clustering,pathPlanning,util);
        unvisitedBuildingIDs = this.recorder.getATBS().getUnvisitedBuildings();
    }

    @Override
    public Search updateInfo(MessageManager messageManager) {
//        CSUSearchUtil.debugOverall("当前目标为:" + this.result);\
        super.updateInfo(messageManager);
        util.debugSpecific("======================updateInfo Start======================");
        util.debugSpecific("当前位置为:"+agentInfo.getPosition());
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        super.updateInfo(messageManager);
        this.clustering.updateInfo(messageManager);
        this.pathPlanning.updateInfo(messageManager);
        this.recorder.updateInfo(messageManager, this.clustering);
        this.unvisitedBuildingIDs = this.recorder.getATBS().getUnvisitedBuildings();
        CSUSearchRecorder.registerRecorder(recorder);


        util.debugSpecific(this.agentInfo.getTime()+"回合，allCivilian的大小:"+util.getCivilianIDs().size());
        util.debugSpecific("======================updateInfo End========================");
        return this;
    }

    @Override
    public Search calc() {
        boolean completeCalc = false;
        util.debugSpecific("=========================calc Start=========================");
        util.debugSpecific("上一目标为:"+recorder.getTarget()
                +",优先级为:"+CSUSearchUtil.getNameByPriority(recorder.getNowPriority()));
        if (util.creatingScene()) {
            util.debugSpecific("正在创建地图，跳过calc()");
            completeCalc = true;
        }
        if(!completeCalc && recorder.needToChangeTarget()){
            util.debugSpecific("有必要换目标");
            this.result = recorder.decideBest()? recorder.getTarget() : recorder.quickDecide();
            util.debugSpecific("换目标成功："
                    +CSUSearchUtil.getNameByPriority(recorder.getNowPriority())+"("+result+")");
            completeCalc = true;
        }
        //如果一切正常，尝试抢占。
        if(!completeCalc){
            if(recorder.tryFindPriorTarget()){
                this.result = recorder.getTarget();
            }
        }


        // 动态确定距离最近的建筑，用来改进decideBest，changeTarget。
//        this.pathPlanning.setFrom(this.agentInfo.getPosition());
//        this.pathPlanning.setDestination(this.unsearchedBuildingIDs);
//        List<EntityID> path = this.pathPlanning.calc().getResult();
//        if (path != null && path.size() > 0) {
//            this.result = path.get(path.size() - 1);
//        }
        visualDebug();
        if(this.result == null && !(agentInfo.getTime() < scenarioInfo.getKernelAgentsIgnoreuntil())){
            util.debugOverall("当前目标为空(impossible).");
        }
        util.debugSpecific("当前目标为"+this.result+",优先级为:"+CSUSearchUtil.getNameByPriority(recorder.getNowPriority()));
        if(this.result != null){
            util.debugSpecific(recorder.getATBS().getByID(result).getStatusString());
        }
        util.debugSpecific("=========================calc  End =========================\n");
//        util.flush();
        return this;
    }

//    private void reset() {
//        this.unsearchedBuildingIDs.clear();
//
//        Collection<StandardEntity> clusterEntities = null;
//        if (this.clustering != null)
//        {
//            int clusterIndex = this.clustering.getClusterIndex(this.agentInfo.getID());
//            clusterEntities = this.clustering.getClusterEntities(clusterIndex);
//
//        }
//        if (clusterEntities != null && clusterEntities.size() > 0)
//        {
//            for (StandardEntity entity : clusterEntities)
//            {
//                if (entity instanceof Building && entity.getStandardURN() != REFUGE)
//                {
//                    this.unsearchedBuildingIDs.add(entity.getID());
//                }
//            }
//        }
//        else
//        {
//            this.unsearchedBuildingIDs.addAll(this.worldInfo.getEntityIDsOfType(
//                    BUILDING,
//                    GAS_STATION,
//                    AMBULANCE_CENTRE,
//                    FIRE_STATION,
//                    POLICE_OFFICE
//            ));
//        }
//    }

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
                List<Integer> elementList = Util.fetchIdValueFromElementIds(unvisitedBuildingIDs);
                DebugHelper.VD_CLIENT.drawAsync(agentInfo.getID().getValue(), "UnsearchedBuildings", (Serializable) elementList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
