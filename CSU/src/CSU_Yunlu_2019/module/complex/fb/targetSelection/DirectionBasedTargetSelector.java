package CSU_Yunlu_2019.module.complex.fb.targetSelection;

import CSU_Yunlu_2019.module.algorithm.fb.Cluster;
import CSU_Yunlu_2019.module.algorithm.fb.FireCluster;
import CSU_Yunlu_2019.module.complex.fb.tools.ZJUBaseBuildingCostComputer;
import CSU_Yunlu_2019.util.ConstantComparators;
import CSU_Yunlu_2019.world.CSUFireBrigadeWorld;
import CSU_Yunlu_2019.world.object.CSUBuilding;
import javolution.util.FastSet;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * First we find the expand direction of the specified fire cluster. Then We get
 * buildings in that direction, and with the border buildings of this fire cluster,
 * we calculate their buildingValues. For inDirectionBuildings and borderBuildings
 * , we choose different standard to calculate buildingValues. Very obvious,
 * inDirectionBuildings are more important. Finally, we sorted
 * inDirectionBuildings and borderBuildings together according to their
 * buildingValues and the first in this sorted set is the target building to
 * extinguish.
 *
 * @author appreciation-csu
 * @edit CSU-zack CSU-crf 2020
 */
public class DirectionBasedTargetSelector extends TargetSelector {

    /**
     * first get a triangle and then add them to
     * then calculate cost
     * then select a specific entity as target
     */
    private ZJUBaseBuildingCostComputer calculateBuildingCost;

    public DirectionBasedTargetSelector(CSUFireBrigadeWorld world) {
        super(world);
        this.calculateBuildingCost = new ZJUBaseBuildingCostComputer(world);
    }

    @Override
    public FireBrigadeTarget selectTarget(Cluster targetCluster) {
        FireBrigadeTarget fireBrigadeTarget = null;
        if (targetCluster != null) {
            SortedSet<Pair<EntityID, Double>> sortedBuildings;
            sortedBuildings = calculateValue((FireCluster) targetCluster);
            if (!sortedBuildings.isEmpty()) {
                lastTarget = target;
                target = world.getCsuBuilding(sortedBuildings.first().first());
                fireBrigadeTarget = new FireBrigadeTarget(targetCluster ,target);
            }
        }

        return fireBrigadeTarget;
    }

    //计算borderBuildings和inDirectionBuildings的value选择
    private SortedSet<Pair<EntityID, Double>> calculateValue(FireCluster fireCluster) {
        // data preparation [sortedBuilding, borderbuilding, ]
        SortedSet<Pair<EntityID, Double>> sortedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        Set<StandardEntity> borderEntities = fireCluster.getBorderEntities();

        // get in direction building
//        List<CSUBuilding> inDirectionBuildings = fireCluster.getBuildingsInDirection();
        Collection<CSUBuilding> inDirectionBuildings = getInDirectionBuildings(fireCluster);
        // update
        calculateBuildingCost.updateFor(fireCluster, lastTarget);

        if (inDirectionBuildings.isEmpty()) {
            calculateValueForOtherBuildings(sortedBuildings, borderEntities);
        }
        calculateValueForInDirectionBuildings(sortedBuildings, inDirectionBuildings);
        return sortedBuildings;
    }

    private void calculateValueForOtherBuildings(SortedSet<Pair<EntityID, Double>> sortedBuildings, Collection<StandardEntity> otherBuildings) {
        for (StandardEntity entity : otherBuildings) {
            CSUBuilding csuBuilding = world.getCsuBuilding(entity.getID());
            csuBuilding.BUILDING_VALUE = calculateBuildingCost.getCost(csuBuilding);
            sortedBuildings.add(new Pair<EntityID, Double>(csuBuilding.getId(), csuBuilding.BUILDING_VALUE));
        }
    }

    private void calculateValueForInDirectionBuildings(SortedSet<Pair<EntityID, Double>> sortedBuildings, Collection<CSUBuilding> highValueBuildings) {
        for (CSUBuilding csuBuilding : highValueBuildings) {
            csuBuilding.BUILDING_VALUE = calculateBuildingCost.getCost(csuBuilding);
            sortedBuildings.add(new Pair<EntityID, Double>(csuBuilding.getId(), csuBuilding.BUILDING_VALUE));
        }
    }

//    public Collection<CSUBuilding> getInDirectionBuildings(FireCluster fireCluster) {
//        return fireCluster.getBuildingsInDirection();
//    }
//
//    public Set<StandardEntity> getBorderBuildings(FireCluster fireCluster) {
//        return new HashSet<>(fireCluster.getBorderEntities());
//    }

    public Collection<CSUBuilding> getInDirectionBuildings(FireCluster fireCluster) {
        Set<CSUBuilding> inDirectionBuildings;
        Point directionPoint = directionManager.findFarthestPointOfMap(fireCluster, (FireBrigade) selfHuman);
        inDirectionBuildings = fireCluster.findBuildingInDirection(directionPoint);
        return inDirectionBuildings;
    }


    public Collection<StandardEntity> getBorderBuildings(FireCluster fireCluster) {
        return new FastSet<>(fireCluster.getBorderEntities());
    }

}
