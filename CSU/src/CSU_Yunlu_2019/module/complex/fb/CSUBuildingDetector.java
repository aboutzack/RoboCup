package CSU_Yunlu_2019.module.complex.fb;

import CSU_Yunlu_2019.module.algorithm.fb.CSUFireClustering;
import CSU_Yunlu_2019.util.ambulancehelper.CSUBuilding;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.StandardMessage;
import adf.agent.communication.standard.bundle.information.MessageBuilding;
import adf.agent.communication.standard.bundle.information.MessageCivilian;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.algorithm.Clustering;
import adf.component.module.complex.BuildingDetector;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CSUBuildingDetector extends BuildingDetector{
    private EntityID result;

    private Clustering clustering;

    private Map<EntityID, CSUBuilding> sentBuildingMap;
    private ClusteringType clusteringType;

    public CSUBuildingDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData)
    {
        super(ai, wi, si, moduleManager, developData);
        switch (si.getMode())
        {
            case PRECOMPUTATION_PHASE:
                this.clustering = moduleManager.getModule("SampleBuildingDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case PRECOMPUTED:
                this.clustering = moduleManager.getModule("SampleBuildingDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case NON_PRECOMPUTE:
                this.clustering = moduleManager.getModule("SampleBuildingDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
        }
        registerModule(this.clustering);
        this.sentBuildingMap = new HashMap<>();
        if (clustering instanceof CSUFireClustering) {
            clusteringType = ClusteringType.BURNING_BUILDINGS_BASED;
        } else {
            clusteringType = ClusteringType.ALL_ENTITIES_BASED;
        }
    }


    @Override
    public BuildingDetector updateInfo(MessageManager messageManager)
    {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2)
        {
            return this;
        }

        this.reflectMessage(messageManager);
        this.clustering.updateInfo(messageManager);
        preProcessChangedEntities(messageManager);
        return this;
    }

    /**
    * @Description: 根据changedEntities的信息进行通讯等操作
    * @Date: 2/28/20
    */
    private void preProcessChangedEntities(MessageManager messageManager) {
        worldInfo.getChanged().getChangedEntities().forEach(changedId -> {
            StandardEntity entity = worldInfo.getEntity(changedId);
            if (entity instanceof Building) {
                Building building = (Building) worldInfo.getEntity(changedId);
                if (building.isFierynessDefined() && building.getFieryness() > 0) {
                    CSUBuilding csuBuilding = sentBuildingMap.get(changedId);
                    if (csuBuilding == null || csuBuilding.getFireyness() != building.getFieryness() || csuBuilding.getFireyness() == 1) {
                        messageManager.addMessage(new MessageBuilding(true, building));
                        messageManager.addMessage(new MessageBuilding(false, building));
                        sentBuildingMap.put(changedId, new CSUBuilding(building));
                    }
                }
            } else if (entity instanceof Civilian) {
                Civilian civilian = (Civilian) entity;
                if ((civilian.isHPDefined() && civilian.getHP() > 1000 && civilian.isDamageDefined() && civilian.getDamage() > 0)
                        || ((civilian.isPositionDefined() && !(worldInfo.getEntity(civilian.getPosition()) instanceof Refuge))
                        && (worldInfo.getEntity(civilian.getPosition()) instanceof Building))) {
                    messageManager.addMessage(new MessageCivilian(true, civilian));
                    messageManager.addMessage(new MessageCivilian(false, civilian));
                }
            }
        });
    }

    /**
     * 更新信息
     * @param messageManager
     */
    private void reflectMessage(MessageManager messageManager) {
        Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
        changedEntities.add(this.agentInfo.getID());

        for (CommunicationMessage message : messageManager.getReceivedMessageList()) {
            if (message instanceof StandardMessage) {
                MessageUtil.reflectMessage(this.worldInfo, (StandardMessage) message);
            }
        }
    }

    @Override
    public BuildingDetector calc()
    {
        IFireBrigadeTargetSelector targetSelector;
        if (clusteringType == ClusteringType.ALL_ENTITIES_BASED) {
            targetSelector = new DefaultTargetSelector(agentInfo, worldInfo, scenarioInfo, clustering);
            this.result = targetSelector.calc();
        } else if (clusteringType == ClusteringType.BURNING_BUILDINGS_BASED) {
            targetSelector = new OptimalTargetSelector(agentInfo, worldInfo, scenarioInfo, clustering);
            this.result = targetSelector.calc();
        }
        return this;
    }



    @Override
    public EntityID getTarget()
    {
        return this.result;
    }

    @Override
    public BuildingDetector precompute(PrecomputeData precomputeData)
    {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        return this;
    }

    @Override
    public BuildingDetector resume(PrecomputeData precomputeData)
    {
        super.resume(precomputeData);
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        return this;
    }

    @Override
    public BuildingDetector preparate()
    {
        super.preparate();
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        return this;
    }


}

