package CSU_Yunlu_2019.module.complex.fb.targetSelection;

import CSU_Yunlu_2019.CSUConstants;
import CSU_Yunlu_2019.module.algorithm.fb.Cluster;
import CSU_Yunlu_2019.module.algorithm.fb.CompositeConvexHull;
import CSU_Yunlu_2019.module.algorithm.fb.FireCluster;
import CSU_Yunlu_2019.module.complex.fb.tools.ZJUBaseBuildingCostComputer;
import CSU_Yunlu_2019.standard.Ruler;
import CSU_Yunlu_2019.util.ConstantComparators;
import CSU_Yunlu_2019.world.CSUFireBrigadeWorld;
import CSU_Yunlu_2019.world.object.CSUBuilding;
import javolution.util.FastSet;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;
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
//            sortedBuildings = fireBrigadeUtilities.reRankBuildings(sortedBuildings);

//            MrlPersonalData.VIEWER_DATA.setBuildingValues(world.getSelf().getID(), world.getMrlBuildings());

            if (sortedBuildings != null && !sortedBuildings.isEmpty()) {
                lastTarget = target;
                target = world.getCsuBuilding(sortedBuildings.first().first());
                fireBrigadeTarget = new FireBrigadeTarget(targetCluster ,target);
            }
        }

        return fireBrigadeTarget;
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
    private SortedSet<Pair<EntityID, Double>> calculateValue(FireCluster fireCluster) {
        // data preparation [sortedBuilding, borderbuilding, ]
        SortedSet<Pair<EntityID, Double>> sortedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        Set<StandardEntity> borderEntities = fireCluster.getBorderEntities();

        // get in direction building
        List<CSUBuilding> inDirectionBuildings = fireCluster.getBuildingsInDirection();
        // update
        calculateBuildingCost.updateFor(fireCluster, lastTarget);

        if (inDirectionBuildings.isEmpty()) {
            calculateValueForOtherBuildings(sortedBuildings, borderEntities);
        }
        calculateValueForInDirectionBuildings(sortedBuildings, inDirectionBuildings);
        return sortedBuildings;
//        Set<StandardEntity> borderBuildings = new FastSet<>(fireCluster.getBorderEntities());
//        ///System.out.println(world.getTime() + ", "+ world.me + ",fireCluster: " + fireCluster.getFireCondition());
//        ///System.out.println(world.getTime() + ", "+ world.me + ",borderBuildings  "+borderBuildings);///
//        Set<CSUBuilding> inDirectionBuildings;
//        Point directionPoint = directionManager.findFarthestPointOfMap(fireCluster, (FireBrigade) selfHuman);
//        inDirectionBuildings = fireCluster.findBuildingInDirection(directionPoint);
        ///System.out.println(world.getTime() + ", "+world.me +", inDirectionBuildings  " + inDirectionBuildings);///
//        borderBuildings.removeAll(csuBuildingToEntity(inDirectionBuildings));

//        this.calculateValueOfInDirectionBuildings(getInDirectionBuildings(fireCluster), sortedBuildings);
//        this.calculateValueOfBorderBuildings(getBorderBuildings((fireCluster)), sortedBuildings);

//        return this.calculateValueOfDistance(sortedBuildings);
    }
    private void calculateValueForOtherBuildings(SortedSet<Pair<EntityID, Double>> sortedBuildings, Set<StandardEntity> otherBuildings) {
        for (StandardEntity entity : otherBuildings) {
            CSUBuilding csuBuilding = world.getCsuBuilding(entity.getID());
            csuBuilding.BUILDING_VALUE = calculateBuildingCost.getCost(csuBuilding);
            sortedBuildings.add(new Pair<EntityID, Double>(csuBuilding.getId(), csuBuilding.BUILDING_VALUE));
        }
    }

    private void calculateValueForInDirectionBuildings(SortedSet<Pair<EntityID, Double>> sortedBuildings, List<CSUBuilding> highValueBuildings) {
        for (CSUBuilding csuBuilding : highValueBuildings) {
            csuBuilding.BUILDING_VALUE = calculateBuildingCost.getCost(csuBuilding);
            sortedBuildings.add(new Pair<EntityID, Double>(csuBuilding.getId(), csuBuilding.BUILDING_VALUE));
        }
    }

    private void calculateValueOfInDirectionBuildings(Set<CSUBuilding> inDirectionBuildings,
                                                      SortedSet<Pair<Pair<EntityID, Double>, Double>> sortedBuildings) {
        for (CSUBuilding next : inDirectionBuildings) {


            next.BUILDING_VALUE += 500;///

            if (lastTarget != null && next.getId() == lastTarget.getId()) {
                next.BUILDING_VALUE += 1000;///
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

    // 计算当前的sortedset中的所有建筑物与当前agent之间的距离作为权重并加入
    private SortedSet<Pair<Pair<EntityID, Double>, Double>> calculateValueOfDistance(SortedSet<Pair<Pair<EntityID, Double>, Double>> sortedBuildings) {
        double distanceWeight = 0.2;
        SortedSet<Pair<Pair<EntityID, Double>, Double>> sortedBuildingsWithDistance =
                new TreeSet<Pair<Pair<EntityID, Double>, Double>>(fbUtilities.pairComparator_new);
        Iterator interator = sortedBuildings.iterator();
        while(interator.hasNext()){
            Pair<Pair<EntityID, Double>, Double> pair = (Pair<Pair<EntityID, Double>, Double>)interator.next();

//            System.out.println("  +++++++  "+ pair.first().first()+"  "+pair.second() + "  "+world.getDistance(pair.first().first(), world.getAgentInfo().getID()));
            sortedBuildingsWithDistance.add(new Pair<Pair<EntityID, Double>, Double>(pair.first(), pair.second()+distanceWeight*world.getDistance(pair.first().first(), world.getAgentInfo().getID())));
//            System.out.println("  *******  "+ pair.first().first()+"  "+pair.second() + "  "+world.getDistance(pair.first().first(), world.getAgentInfo().getID()));

        }
        return sortedBuildingsWithDistance;
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
