package mrl_2019.complex;

import adf.agent.info.WorldInfo;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.develop.DevelopData;
import adf.agent.module.ModuleManager;
import adf.agent.communication.MessageManager;

import adf.component.module.complex.Search;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;

import adf.launcher.ConsoleOutput;
import com.mrl.debugger.remote.VDClient;
import mrl_2019.util.Util;
import mrl_2019.viewer.MrlPersonalData;
import mrl_2019.world.MrlWorldHelper;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;

import java.io.Serializable;
import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;

/**
 * Created by Peyman on 7/12/2017.
 *
 * @author Peyman
 * @author Pooya
 */
public class MrlFireBrigadeSearch extends Search {

    private PathPlanning pathPlanning;
    private Clustering clustering;

    private EntityID result;
    private Set<EntityID> unsearchedBuildingIDs;
    private List<EntityID> unsearchedBuildingIDList;


    private Map<EntityID, Integer> unreachableTargets;
    private RandomGenerator randomGenerator;
    private MrlWorldHelper worldHelper;

    public MrlFireBrigadeSearch(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.unsearchedBuildingIDs = new HashSet<>();

        this.unreachableTargets = new HashMap<>();
        this.unsearchedBuildingIDList = new ArrayList<>();
        this.randomGenerator = new MersenneTwister(System.currentTimeMillis());


        StandardEntityURN agentURN = ai.me().getStandardURN();
        switch (si.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("SampleSearch.Clustering.Fire", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("SampleSearch.Clustering.Fire", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("SampleSearch.Clustering.Fire", "adf.sample.module.algorithm.SampleKMeans");
                break;
        }

//        exploreManager = new TargetBaseExploreManager(ai, wi, si, moduleManager, developData);


        this.worldHelper = MrlWorldHelper.load(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
        registerModule(worldHelper);
        registerModule(pathPlanning);
        registerModule(clustering);


    }


    @Override
    public Search updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.worldHelper.updateInfo(messageManager);
        this.pathPlanning.updateInfo(messageManager);
        this.clustering.updateInfo(messageManager);
        this.worldHelper.getPossibleBurningBuildings().removeAll(this.worldInfo.getChanged().getChangedEntities());
        this.worldHelper.getPossibleBurningBuildings().removeAll(unreachableTargets.keySet());
        if (this.unsearchedBuildingIDs.isEmpty()) {
            this.reset();
        }
        this.unsearchedBuildingIDs.removeAll(this.worldInfo.getChanged().getChangedEntities());

        if (this.unsearchedBuildingIDs.isEmpty()) {
            this.reset();
            this.unsearchedBuildingIDs.removeAll(this.worldInfo.getChanged().getChangedEntities());
        }

        updateUnreachableTargets();


        return this;
    }

    private void reset() {
        this.unsearchedBuildingIDs.clear();

        Collection<StandardEntity> clusterEntities = null;
        if (this.clustering != null) {
            int clusterIndex = this.clustering.getClusterIndex(this.agentInfo.getID());
            clusterEntities = this.clustering.getClusterEntities(clusterIndex);

        }
        if (clusterEntities != null && clusterEntities.size() > 0) {
            for (StandardEntity entity : clusterEntities) {
                if (entity instanceof Building && entity.getStandardURN() != REFUGE) {
                    this.unsearchedBuildingIDs.add(entity.getID());
                }
            }
        } else {
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
    public Search calc() {


        if (MrlPersonalData.DEBUG_MODE) {

            int clusterIndex = clustering.getClusterIndex(this.agentInfo.getID());
            Collection<StandardEntity> elements = clustering.getClusterEntities(clusterIndex);

            if (elements != null) {
                List<Integer> elementList = Util.fetchIdValueFormElements(elements);
                VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlFireBrigadeBuildingsLayer", (Serializable) elementList);
            }


            if (worldHelper.getPossibleBurningBuildings() != null) {
                List<Integer> elementList = Util.fetchIdValueFormElementIds(worldHelper.getPossibleBurningBuildings());
                VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlPossibleBurningBuildingsLayer", (Serializable) elementList);
            }

        }

        //try to search around last target.


        EntityID exploreTarget = worldHelper.getExploreTarget();


        if (exploreTarget == null) {
            EntityID toSearchTarget;
            //try to search for possible burning buildings based on last target
            toSearchTarget = search(result, worldHelper.getPossibleBurningBuildings(), true);

            if (toSearchTarget == null) {
                //try to search for possible burning buildings in the partition
                toSearchTarget = search(result, unsearchedBuildingIDs, false);
            }
            this.result = toSearchTarget;
        } else {
            this.result = exploreTarget;
        }


        return this;

    }

    private EntityID search(EntityID lastTarget, Set<EntityID> searchableEntityIds, boolean sorted) {
        EntityID toSearchTarget;

        if (searchableEntityIds != null && !searchableEntityIds.isEmpty()) {
            if (lastTarget != null && searchableEntityIds.contains(lastTarget)) {
                if (hasPath(lastTarget)) {
                    return lastTarget;
                } else {
                    addToUnreachableTargets(lastTarget);
                }
            }

            if (sorted) {
                List<StandardEntity> unsearchedBuildings = new ArrayList<>();
                searchableEntityIds.forEach(entityID -> {
                    unsearchedBuildings.add(worldInfo.getEntity(entityID));
                });
                unsearchedBuildings.sort((o1, o2) -> {
                    int l1 = worldInfo.getDistance(agentInfo.getID(), o1.getID());
                    int l2 = worldInfo.getDistance(agentInfo.getID(), o2.getID());
                    if (l1 > l2)       //increase
                        return 1;
                    if (l1 == l2)
                        return 0;

                    return -1;
                });

                this.unsearchedBuildingIDList.clear();
                unsearchedBuildings.forEach(standardEntity -> {
                    unsearchedBuildingIDList.add(standardEntity.getID());
                });
            } else {
                this.unsearchedBuildingIDList.clear();
                this.unsearchedBuildingIDList.addAll(searchableEntityIds);
            }

//            this.result = null;
            for (int i = 0; i < 10; i++) {
                if (!this.unsearchedBuildingIDList.isEmpty()) {
                    EntityID next = this.unsearchedBuildingIDList.get(randomGenerator.nextInt(searchableEntityIds.size()));
                    if (hasPath(next)) {
                        toSearchTarget = next;
                        return toSearchTarget;
                    } else {
                        addToUnreachableTargets(next);
                    }
                }
            }
        }
        return null;
    }

    protected void printDebugMessage(String msg) {
        ConsoleOutput.error("Agent:" + agentInfo.getID() + " Time:" + agentInfo.getTime() + " " + msg);
    }

    private void updateUnreachableTargets() {
        ArrayList<EntityID> toRemove = new ArrayList<EntityID>();
        int postponeTime;
        for (EntityID standardEntity : unreachableTargets.keySet()) {
            postponeTime = unreachableTargets.get(standardEntity);
            postponeTime--;
            if (postponeTime <= 0) {
                toRemove.add(standardEntity);
            } else {
                unreachableTargets.put(standardEntity, postponeTime);
            }

        }
        unreachableTargets.keySet().removeAll(toRemove);
    }

    private void addToUnreachableTargets(EntityID result) {
        int postponeTime = randomGenerator.nextInt(6) + 5;
        unreachableTargets.put(worldInfo.getEntity(result).getID(), postponeTime);
    }

    private boolean hasPath(EntityID result) {
        List<EntityID> idList = this.pathPlanning.setFrom(agentInfo.getPosition()).setDestination(result).calc().getResult();
        return idList != null && !idList.isEmpty();

    }

    @Override
    public EntityID getTarget() {
        return this.result;
    }

}
