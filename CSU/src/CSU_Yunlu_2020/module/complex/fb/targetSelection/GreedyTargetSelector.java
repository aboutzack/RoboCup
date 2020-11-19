package CSU_Yunlu_2020.module.complex.fb.targetSelection;

import CSU_Yunlu_2020.module.algorithm.fb.Cluster;
import CSU_Yunlu_2020.module.algorithm.fb.FireCluster;
import CSU_Yunlu_2020.module.complex.fb.search.SearchHelper;
import CSU_Yunlu_2020.module.complex.fb.tools.BuildingCostComputer;
import CSU_Yunlu_2020.util.ConstantComparators;
import CSU_Yunlu_2020.world.CSUFireBrigadeWorld;
import CSU_Yunlu_2020.world.object.CSUBuilding;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

import java.util.*;
import java.util.stream.Collectors;


public class GreedyTargetSelector extends TargetSelector {

    public GreedyTargetSelector(CSUFireBrigadeWorld world) {
        super(world);
        this.buildingCostComputer = new BuildingCostComputer(world);
    }

    private BuildingCostComputer buildingCostComputer;

    @Override
    public FireBrigadeTarget selectTarget(Cluster targetCluster) {

        if (targetCluster == null) {
            return null;
        }

        FireBrigadeTarget fireBrigadeTarget = null;


        //first consider buildings in extinguish range
        Set<Building> buildingsInCluster = targetCluster.getBuildings();
        Set<Building> fireBuildingsInRange = world.getBuildingsInRange(world.getSelfHuman().getID(), world.getScenarioInfo().getFireExtinguishMaxDistance());
        fireBuildingsInRange.removeIf(e -> {
            return !(e.isOnFire() && e.isFierynessDefined() && e.getFieryness() != 8 &&
                    e.isTemperatureDefined() && e.getTemperature() > 45);
        });
        for (Building building : fireBuildingsInRange) {
            if (buildingsInCluster.contains(building) && !SearchHelper.isTimeToSearch(world, building.getID())) {
                lastTarget = target;
                target = world.getCsuBuilding(building);
                fireBrigadeTarget = new FireBrigadeTarget(targetCluster, target);
                return fireBrigadeTarget;
            }
        }

        //use ZJUBased to find best
        lastTarget = target;
        target = calculateValueZJUBase((FireCluster) targetCluster);
        if (target != null) {
            fireBrigadeTarget = new FireBrigadeTarget(targetCluster, target);
        }
        return fireBrigadeTarget;
    }

    private CSUBuilding calculateValueZJUBase(FireCluster fireCluster) {
        Set<Building> buildings = fireCluster.getBuildings();
        Set<CSUBuilding> csuBuildings = buildings.stream().map(world::getCsuBuilding).collect(Collectors.toSet());
        Map<EntityID, Double> buildingCostMap = new HashMap<>();
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
