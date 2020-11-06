package CSU_Yunlu_2019.module.complex.fb.targetSelection;

import CSU_Yunlu_2019.CSUConstants;
import CSU_Yunlu_2019.module.algorithm.fb.Cluster;
import CSU_Yunlu_2019.module.algorithm.fb.CompositeConvexHull;
import CSU_Yunlu_2019.module.algorithm.fb.FireCluster;
import CSU_Yunlu_2019.standard.Ruler;
import CSU_Yunlu_2019.world.CSUFireBrigadeWorld;
import CSU_Yunlu_2019.world.object.CSUBuilding;
import javolution.util.FastSet;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.stream.Collectors;

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
 * @edit Guanyu-Cai 2020
 */
public class DirectionBasedTargetSelector extends TargetSelector {

    public DirectionBasedTargetSelector(CSUFireBrigadeWorld world) {
        super(world);
    }

    @Override
    public FireBrigadeTarget selectTarget(Cluster targetCluster) {
        FireBrigadeTarget targetBuilding;
        this.lastTarget = this.target;
        if (targetCluster != null) {
            targetBuilding = gasStationHandler((FireCluster) targetCluster);
            if (targetBuilding != null) {
                return targetBuilding;
            }

            SortedSet<Pair<Pair<EntityID, Double>, Double>> sortedBuildings;
            sortedBuildings = this.calculateValue((FireCluster) targetCluster);
            sortedBuildings = fbUtilities.reRankBuildings(sortedBuildings, (FireBrigade) selfHuman);

            //去除所有认为未着火的
            Set<EntityID> changedEntities = world.getWorldInfo().getChanged().getChangedEntities();
            sortedBuildings = sortedBuildings.stream().filter(e -> {
                EntityID id = e.first().first();
                return world.getEntity(id, Building.class).isTemperatureDefined() &&
                        world.getEntity(id, Building.class).getTemperature() > 40 &&
                        world.getEntity(id, Building.class).isFierynessDefined() &&
                        world.getEntity(id, Building.class).getFieryness() != 8;
            }).collect(Collectors.toCollection(() -> new TreeSet<>(fbUtilities.pairComparator_new)));

            if (sortedBuildings != null && !sortedBuildings.isEmpty()) {
                this.target = world.getCsuBuilding(sortedBuildings.first().first().first());
                targetBuilding = new FireBrigadeTarget(targetCluster, this.target);
                if (CSUConstants.DEBUG_DIRECTION_BASED_TARGET_SELECTOR) {
                    System.out.println("clusterSize: " + targetCluster.getBuildings().size());
                    System.out.println("allBuildings: " + targetCluster.getBuildings());
                    System.out.println("sortedBuildings: " + sortedBuildings);
                }
            } else {//任意取一个building
                if (CSUConstants.DEBUG_DIRECTION_BASED_TARGET_SELECTOR) {
                    Point directionPoint = directionManager.findFarthestPointOfMap((FireCluster) targetCluster, (FireBrigade) selfHuman);
                    System.out.println("empty sorted buildings in cluster's buildings: " + targetCluster.getBuildings() + "entities: " +
                            targetCluster.getEntities());
                    System.out.println("borders: " + targetCluster.getBorderEntities());
                    System.out.println("inDirections: " + ((FireCluster) targetCluster).findBuildingInDirection(directionPoint));
                    System.out.println("direction point: " + directionPoint);
                }
                Building building = targetCluster.getBuildings().iterator().next();
                targetBuilding = new FireBrigadeTarget(targetCluster, world.getCsuBuilding(building.getID()));
            }
        } else {
            return null;
        }
        return targetBuilding;
    }

    /**
     * 优先灭gasstation
     * choose gas stations whose fieriness == 0 but temperature >= 25 and water quantity < 3000
     * and distance to the fire cluster <= 5000
     * if including lastTarget, return it;
     * otherwise, return the closet(Euler) to the fire brigade
     *
     * @param cluster
     * @return
     */
    private FireBrigadeTarget gasStationHandler(FireCluster cluster) {
        Collection<StandardEntity> gasStas = world.getWorldInfo().getEntitiesOfType(StandardEntityURN.GAS_STATION);
        Set<CSUBuilding> targetGasStas = new HashSet<>();

        Polygon clusterPolygon = cluster.getConvexHull().getConvexPolygon();

        for (StandardEntity next : gasStas) {
            GasStation sta = (GasStation) next;

            if (sta.isFierynessDefined() && sta.getFieryness() >= 1)
                continue;

            if (sta.isTemperatureDefined() && sta.getTemperature() < 25)      ///temperature
                continue;

            CSUBuilding csuBu = world.getCsuBuilding(next.getID());
            if (csuBu.getWaterQuantity() >= 3000)  // two cycles   ///water
                continue;

            int[] apexes = sta.getApexList();
            CompositeConvexHull convexHull = new CompositeConvexHull();

            for (int i = 0; i < apexes.length; i += 2) {
                convexHull.addPoint(apexes[i], apexes[i + 1]);
            }

            Polygon gasPolygon = convexHull.getConvexPolygon();

            int dis = (int) Ruler.getDistance(clusterPolygon, gasPolygon);
            if (dis <= 5000)     ///distance
                targetGasStas.add(world.getCsuBuilding(next.getID()));
        }

        if (targetGasStas.contains(lastTarget))
            return new FireBrigadeTarget(cluster, lastTarget);

        CSUBuilding targetBuilding = null;
        double minDistance = Double.MAX_VALUE;
        for (CSUBuilding next : targetGasStas) {
            double dis = world.getDistance(next.getId(), selfHuman.getID());
            if (dis < minDistance) {
                minDistance = dis;
                targetBuilding = next;
            }
        }

        if (targetBuilding != null) {
            this.target = targetBuilding;
            return new FireBrigadeTarget(cluster, targetBuilding);
        } else
            return null;
    }

    private void printSortedBuilding(SortedSet<Pair<Pair<EntityID, Double>, Double>> sortedBuildings) {
        String str = null;
        for (Pair<Pair<EntityID, Double>, Double> next : sortedBuildings) {
            if (str == null) {
                str = "time = " + world.getTime() + ", " + selfHuman + ", sortedBuilding: [";
            } else {
                str = str + ", ";
            }
            str = str + "(" + next.first().first().getValue() + ", " + next.second() + ")";
        }
        str = str + "]";
        System.out.println(str);
    }

    //计算borderBuildings和inDirectionBuildings的value选择
    private SortedSet<Pair<Pair<EntityID, Double>, Double>> calculateValue(FireCluster fireCluster) {
        SortedSet<Pair<Pair<EntityID, Double>, Double>> sortedBuildings =
                new TreeSet<Pair<Pair<EntityID, Double>, Double>>(fbUtilities.pairComparator_new);

//        Set<StandardEntity> borderBuildings = new FastSet<>(fireCluster.getBorderEntities());
//        ///System.out.println(world.getTime() + ", "+ world.me + ",fireCluster: " + fireCluster.getFireCondition());
//        ///System.out.println(world.getTime() + ", "+ world.me + ",borderBuildings  "+borderBuildings);///
//        Set<CSUBuilding> inDirectionBuildings;
//        Point directionPoint = directionManager.findFarthestPointOfMap(fireCluster, (FireBrigade) selfHuman);
//        inDirectionBuildings = fireCluster.findBuildingInDirection(directionPoint);
        ///System.out.println(world.getTime() + ", "+world.me +", inDirectionBuildings  " + inDirectionBuildings);///
//        borderBuildings.removeAll(csuBuildingToEntity(inDirectionBuildings));

        this.calculateValueOfInDirectionBuildings(getInDirectionBuildings(fireCluster), sortedBuildings);
        this.calculateValueOfBorderBuildings(getBorderBuildings((fireCluster)), sortedBuildings);

        return sortedBuildings;
    }

    private void calculateValueOfInDirectionBuildings(Set<CSUBuilding> inDirectionBuildings,
                                                      SortedSet<Pair<Pair<EntityID, Double>, Double>> sortedBuildings) {
        for (CSUBuilding next : inDirectionBuildings) {


            next.BUILDING_VALUE += 500;///

            if (lastTarget != null && next.getId() == lastTarget.getId()) {
                next.BUILDING_VALUE += 250;///
            }

            Pair<Integer, Integer> selfLocation = world.getSelfLocation();
            Pair<Integer, Integer> buildingLocation = world.getLocation(next.getSelfBuilding());
            double distance = Ruler.getDistance(selfLocation, buildingLocation);

            Pair<EntityID, Double> pair = new Pair<>(next.getSelfBuilding().getID(), distance);
            sortedBuildings.add(new Pair<Pair<EntityID, Double>, Double>(pair, next.BUILDING_VALUE));
        }
    }

    private void calculateValueOfBorderBuildings(Set<StandardEntity> borderBuildings,
                                                 SortedSet<Pair<Pair<EntityID, Double>, Double>> sortedBuildings) {
        CSUBuilding csuBuilding;
        for (StandardEntity next : borderBuildings) {
            csuBuilding = world.getCsuBuilding(next.getID());
            csuBuilding.BUILDING_VALUE -= 250;///

            if (lastTarget != null) {
                double distance = world.getDistance(lastTarget.getId(), csuBuilding.getId());
                int temp = (int) (distance / CSUConstants.MEAN_VELOCITY_DISTANCE);
                csuBuilding.BUILDING_VALUE -= temp * 50;
            }

            if (lastTarget != null && csuBuilding.getId() == lastTarget.getId()) {
                csuBuilding.BUILDING_VALUE += 250;///

            }


            Pair<Integer, Integer> selfLocation = world.getSelfLocation();
            Pair<Integer, Integer> buildingLocation = world.getLocation(next);
            double distance = Ruler.getDistance(selfLocation, buildingLocation);

            Pair<EntityID, Double> pair = new Pair<>(next.getID(), distance);
            sortedBuildings.add(new Pair<Pair<EntityID, Double>, Double>(pair, csuBuilding.BUILDING_VALUE));
        }
    }

    /**
     * Translate a collection of CSUBuilding into a collection of
     * StandardEntity.
     *
     * @param csuBuildings a collection of CSUBuilding will be translated
     * @return a collection of StandardEntity
     */
    private Collection<StandardEntity> csuBuildingToEntity(Collection<CSUBuilding> csuBuildings) {
        Collection<StandardEntity> result = new FastSet<>();
        for (CSUBuilding next : csuBuildings)
            result.add(next.getSelfBuilding());
        return result;
    }

    public Set<CSUBuilding> getInDirectionBuildings(FireCluster fireCluster) {
        Set<CSUBuilding> inDirectionBuildings;
        Point directionPoint = directionManager.findFarthestPointOfMap(fireCluster, (FireBrigade) selfHuman);
        inDirectionBuildings = fireCluster.findBuildingInDirection(directionPoint);
        return inDirectionBuildings;
    }


    public Set<StandardEntity> getBorderBuildings(FireCluster fireCluster) {
        Set<StandardEntity> borderBuildings = new FastSet<>(fireCluster.getBorderEntities());
        borderBuildings.removeAll(csuBuildingToEntity(getInDirectionBuildings(fireCluster)));
        return borderBuildings;
    }
}
