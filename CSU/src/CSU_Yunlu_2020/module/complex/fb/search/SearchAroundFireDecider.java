package CSU_Yunlu_2020.module.complex.fb.search;

import CSU_Yunlu_2020.CSUConstants;
import CSU_Yunlu_2020.module.complex.fb.targetSelection.FireBrigadeTarget;
import CSU_Yunlu_2020.module.complex.fb.tools.FbUtilities;
import CSU_Yunlu_2020.world.CSUWorldHelper;
import CSU_Yunlu_2020.world.object.CSUBuilding;
import javolution.util.FastSet;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 提供fireBrigadeTarget作为目标,决定search哪些地方
 */
public class SearchAroundFireDecider {

    protected CSUWorldHelper world;
    private FireBrigadeTarget fireBrigadeTarget;
    private Set<EntityID> searchTargets;//到这些地点进行search
    private final FbUtilities fbUtilities;

    public SearchAroundFireDecider(CSUWorldHelper world) {
        this.world = (world);
        fbUtilities = new FbUtilities(world);
        this.searchTargets = new HashSet<>();
    }

    public void calc(boolean exploreAll) {
        update();
        calcSearchTargets(exploreAll);
    }

    private void update() {
        searchTargets.remove(world.getSelfPosition().getID());
//        if (world.getSelfPosition().getID().equals(target)) {
//            target = null;
//        }
    }

    public void setTargetFire(FireBrigadeTarget fireBrigadeTarget) {
        this.fireBrigadeTarget = fireBrigadeTarget;
    }

    /**
     * this method find all areas for search in this cluster.
     *
     * @param searchAll for explore all of fire cluster
     */
    public void calcSearchTargets(boolean searchAll) {
        Set<CSUBuilding> borderBuildings = new FastSet<>();
        Set<CSUBuilding> allBuildings = new FastSet<>();

        borderBuildings.add(fireBrigadeTarget.getCsuBuilding());
        allBuildings.add(fireBrigadeTarget.getCsuBuilding());

        //如果searchAll,将cluster的borderBuildings纳入考虑范围
        if (searchAll) {
            Set<StandardEntity> clusterBorderEntities = new FastSet<>(fireBrigadeTarget.getCluster().getBorderEntities());

            for (StandardEntity entity : clusterBorderEntities) {
                borderBuildings.add(world.getCsuBuilding(entity.getID()));
                allBuildings.add(world.getCsuBuilding(entity.getID()));
            }
        }

        //添加与borderBuildings相连的区域
        for (CSUBuilding neighbour : borderBuildings) {
            for (CSUBuilding b : neighbour.getConnectedBuildings()) {
                if (world.getDistance(b.getSelfBuilding(), neighbour.getSelfBuilding()) < world.getConfig().viewDistance) {
                    allBuildings.add(b);
                }
            }
        }
        //去除MAX_SEARCH_INTERVAL_BETWEEN_LAST_SEEN之内看见过的buidlings
        allBuildings = allBuildings.stream().filter(e -> {
            return world.getTime() - e.getLastSeenTime() > CSUConstants.MAX_SEARCH_INTERVAL_BETWEEN_LAST_SEEN;
        }).collect(Collectors.toSet());

        searchTargets = fbUtilities.findMaximalCovering(allBuildings);
    }

    public Set<EntityID> getSearchTargets() {
        return searchTargets;
    }

//    public Area getNextArea() {
//        if (searchTargets != null) {
//            update();
//            if (target == null) {   //切换target为exploreTargets里最近的
//                int distance;
//                int minDistance = Integer.MAX_VALUE;
//                for (EntityID areaId : searchTargets) {
//                    distance = world.getDistance(world.getAgentInfo().getID(), areaId);
//                    if (distance < minDistance) {
//                        minDistance = distance;
//                        target = areaId;
//                    }
//                }
//            }
//        }
//        if (target != null) {
//            return (Area) world.getEntity(target);
//        }
//        return null;
//    }
}
