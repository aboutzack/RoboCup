package CSU_Yunlu_2019.module.complex.fb;

import CSU_Yunlu_2019.CSUConstants;
import CSU_Yunlu_2019.debugger.DebugHelper;
import CSU_Yunlu_2019.module.algorithm.fb.CSUFireClustering;
import CSU_Yunlu_2019.module.algorithm.fb.Cluster;
import CSU_Yunlu_2019.module.algorithm.fb.FireCluster;
import CSU_Yunlu_2019.module.complex.fb.clusterSelection.ClusterSelectorType;
import CSU_Yunlu_2019.module.complex.fb.clusterSelection.DistanceBasedClusterSelector;
import CSU_Yunlu_2019.module.complex.fb.clusterSelection.IFireBrigadeClusterSelector;
import CSU_Yunlu_2019.module.complex.fb.search.SearchHelper;
import CSU_Yunlu_2019.module.complex.fb.targetSelection.DirectionBasedTargetSelector;
import CSU_Yunlu_2019.module.complex.fb.targetSelection.FireBrigadeTarget;
import CSU_Yunlu_2019.module.complex.fb.targetSelection.IFireBrigadeTargetSelector;
import CSU_Yunlu_2019.module.complex.fb.targetSelection.TargetSelectorType;
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
import com.mrl.debugger.remote.dto.BuildingDetectorDto;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.io.Serializable;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class CSUBuildingDetector extends BuildingDetector {
    private EntityID result;

    private Clustering clustering;
    private CSUFireBrigadeWorld world;

    private Map<EntityID, CSUBuilding> sentBuildingMap;
    private ClusterSelectorType clusterSelectorType = ClusterSelectorType.DISTANCE_BASED;
    private IFireBrigadeClusterSelector clusterSelector;
    private TargetSelectorType targetSelectorType = TargetSelectorType.DIRECTION_BASED;
    private IFireBrigadeTargetSelector targetSelector;
    private SearchHelper searchHelper;

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
        searchHelper = moduleManager.getModule("SearchHelper.Default", "CSU_Yunlu_2019.module.complex.fb.search.SearchHelper");
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
        FireBrigadeTarget fireBrigadeTarget = targetSelector.selectTarget(targetCluster);
        if (DebugHelper.DEBUG_MODE) {
            if (fireBrigadeTarget != null) {
                BuildingDetectorDto dto = new BuildingDetectorDto();
                dto.setTargetBuilding(fireBrigadeTarget.getCsuBuilding().getId().getValue());
                dto.setDynamicClusterConvexHulls(((CSUFireClustering) clustering).getClusterConvexPolygons());
                HashMap<Polygon, Boolean> polygonControllableMap = new HashMap<>();
                for (Cluster cluster : ((CSUFireClustering) clustering).getClusters()) {
                    polygonControllableMap.put(cluster.getConvexHull().getConvexPolygon(), cluster.isControllable());
                }
                dto.setPolygonControllableMap(polygonControllableMap);
                if (targetSelector instanceof DirectionBasedTargetSelector) {
                    dto.setBorderBuildings(((DirectionBasedTargetSelector) targetSelector)
                            .getBorderBuildings(targetCluster).stream()
                            .map(e-> e.getID().getValue()).collect(Collectors.toSet()));
                    dto.setInDirectionBuildings(((DirectionBasedTargetSelector) targetSelector)
                            .getInDirectionBuildings(targetCluster)
                            .stream().map(e-> e.getId().getValue()).collect(Collectors.toSet()));
                }
                DebugHelper.VD_CLIENT.drawAsync(agentInfo.getID().getValue(), "CSUBuildingDetectorLayer", dto);

            }
        }
        if (fireBrigadeTarget != null) {
            this.result = fireBrigadeTarget.getCsuBuilding().getId();
        } else {
            this.result = null;
            world.setSearchTarget(null);
            return this;
        }
        boolean isTimeToSearch = searchHelper.isTimeToSearch(fireBrigadeTarget);
        if (isTimeToSearch) {
            searchHelper.setTarget(fireBrigadeTarget);
            searchHelper.calc();
            EntityID searchTarget = searchHelper.getResult();
            world.setSearchTarget(searchTarget);
            if (searchTarget != null) {
                this.result = null;
            }
        } else {
            world.setSearchTarget(null);
        }
        if (DebugHelper.DEBUG_MODE) {
            List<Integer> elements = new ArrayList<>();
            if (isTimeToSearch) {
                elements.add(agentInfo.getID().getValue());
            }
            DebugHelper.VD_CLIENT.drawAsync(agentInfo.getID().getValue(), "TimeToSearchFB", (Serializable) elements);
        }
        visualDebug();
        return this;
    }

    private void visualDebug() {
        if (DebugHelper.DEBUG_MODE) {
            try {
                DebugHelper.drawDetectorTarget(worldInfo, agentInfo.getID(), result);
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

