package CSU_Yunlu_2019.module.complex.fb.targetSelection;

import CSU_Yunlu_2019.module.algorithm.fb.Cluster;
import CSU_Yunlu_2019.module.algorithm.fb.FireCluster;
import CSU_Yunlu_2019.module.complex.fb.tools.ZJUBaseBuildingCostComputer;
import CSU_Yunlu_2019.util.ConstantComparators;
import CSU_Yunlu_2019.world.CSUFireBrigadeWorld;
import CSU_Yunlu_2019.world.object.CSUBuilding;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

import java.util.*;
import java.util.stream.Collectors;


public class GreedyTargetSelector extends TargetSelector {

    public GreedyTargetSelector(CSUFireBrigadeWorld world) {
        super(world);
        this.buildingCostComputer = new ZJUBaseBuildingCostComputer(world);
    }

    private ZJUBaseBuildingCostComputer buildingCostComputer;

    @Override
    public FireBrigadeTarget selectTarget(Cluster targetCluster) {

        FireBrigadeTarget fireBrigadeTarget = null;

        if (targetCluster != null) {
            target = calculateValueZJUBase((FireCluster) targetCluster);
            if (target != null) {
                lastTarget = target;
                fireBrigadeTarget = new FireBrigadeTarget(targetCluster, target);
            }
        }

        return fireBrigadeTarget;

    }

    private CSUBuilding calculateValueZJUBase(FireCluster fireCluster) {
        Set<Building> buildings = fireCluster.getBuildings();
        Set<CSUBuilding> csuBuildings = buildings.stream().map(world::getCsuBuilding).collect(Collectors.toSet());
        Map<EntityID,Double> buildingCostMap=new HashMap<>();
        CSUBuilding targetBuilding = null;
        SortedSet<Pair<EntityID, Double>> sortedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        buildingCostComputer.updateFor(fireCluster, lastTarget);

        for (CSUBuilding building : csuBuildings) {
            if (building.isBurning()) {
                int cost = buildingCostComputer.getCost(building);
                building.BUILDING_VALUE = cost;
                buildingCostMap.put(building.getId(), (double) cost);
                sortedBuildings.add(new Pair<EntityID, Double>(building.getId(), building.BUILDING_VALUE));
            }
        }


        if (sortedBuildings != null && !sortedBuildings.isEmpty()) {
            lastTarget = target;
            target = world.getCsuBuilding(sortedBuildings.first().first());
            targetBuilding = target;
        }

        return targetBuilding;
    }

}
