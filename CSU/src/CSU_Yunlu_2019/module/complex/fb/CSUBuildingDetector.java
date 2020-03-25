package CSU_Yunlu_2019.module.complex.fb;

import CSU_Yunlu_2019.CSUConstants;
import CSU_Yunlu_2019.debugger.DebugHelper;
import CSU_Yunlu_2019.module.algorithm.fb.CSUFireClustering;
import CSU_Yunlu_2019.module.algorithm.fb.FireCluster;
import CSU_Yunlu_2019.module.complex.fb.clusterSelection.ClusterSelectorType;
import CSU_Yunlu_2019.module.complex.fb.clusterSelection.DistanceBasedClusterSelector;
import CSU_Yunlu_2019.module.complex.fb.clusterSelection.IFireBrigadeClusterSelector;
import CSU_Yunlu_2019.module.complex.fb.targetSelection.DirectionBasedTargetSelector;
import CSU_Yunlu_2019.module.complex.fb.targetSelection.FireBrigadeTarget;
import CSU_Yunlu_2019.module.complex.fb.targetSelection.IFireBrigadeTargetSelector;
import CSU_Yunlu_2019.module.complex.fb.targetSelection.TargetSelectorType;
import CSU_Yunlu_2019.util.Util;
import CSU_Yunlu_2019.util.ambulancehelper.CSUBuilding;
import CSU_Yunlu_2019.world.CSUFireBrigadeWorld;
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

import java.io.Serializable;
import java.util.*;

public class CSUBuildingDetector extends BuildingDetector {
    private EntityID result;

    private Clustering clustering;
    private CSUFireBrigadeWorld world;

    private Map<EntityID, CSUBuilding> sentBuildingMap;
    private ClusterSelectorType clusterSelectorType = ClusterSelectorType.DISTANCE_BASED;
    private IFireBrigadeClusterSelector clusterSelector;
    private TargetSelectorType targetSelectorType = TargetSelectorType.DIRECTION_BASED;
    private IFireBrigadeTargetSelector targetSelector;

    public CSUBuildingDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        switch (si.getMode()) {
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
        this.world = moduleManager.getModule("WorldHelper.FireBrigade", CSUConstants.WORLD_HELPER_FIRE_BRIGADE);
        world.setFireClustering((CSUFireClustering) clustering);
        registerModule(this.clustering);
        registerModule(world);
        this.sentBuildingMap = new HashMap<>();
    }


    @Override
    public BuildingDetector updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
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
     *
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
    public BuildingDetector calc() {
        setClusterSelector();
        setTargetSelector();
        FireCluster targetCluster = clusterSelector.selectCluster();
        if (targetCluster != null) {
            FireBrigadeTarget fireBrigadeTarget = targetSelector.selectTarget(targetCluster);
            this.result = fireBrigadeTarget.getCsuBuilding().getId();
        } else {
            // TODO: 3/9/20 没有着火房屋时，search重新根据距离分配cluster，防止search时大范围移动?
            this.result = null;
        }
        visualDebug();
        return this;
    }

    private void visualDebug() {
        if (DebugHelper.DEBUG_MODE) {
            try {
                Collection<StandardEntity> buildings = new ArrayList<>();
                buildings.add(world.getEntity(result));
                List<Integer> elementList = Util.fetchIdValueFromElements(buildings);
                DebugHelper.VD_CLIENT.drawAsync(agentInfo.getID().getValue(), "SampleBuildings", (Serializable) elementList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setClusterSelector() {
        switch (clusterSelectorType) {
            case DISTANCE_BASED:
                clusterSelector = new DistanceBasedClusterSelector(world);
        }
    }

    private void setTargetSelector() {
        switch (targetSelectorType) {
            case DIRECTION_BASED:
                targetSelector = new DirectionBasedTargetSelector(world);
                break;
        }
    }

    @Override
    public EntityID getTarget() {
        return this.result;
    }

    @Override
    public BuildingDetector precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        return this;
    }

    @Override
    public BuildingDetector resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        return this;
    }

    @Override
    public BuildingDetector preparate() {
        super.preparate();
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        return this;
    }


}

