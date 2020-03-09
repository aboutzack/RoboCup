package CSU_Yunlu_2019.module.complex.fb.targetSelection;

import CSU_Yunlu_2019.CSUConstants;
import CSU_Yunlu_2019.module.algorithm.fb.Cluster;
import CSU_Yunlu_2019.module.algorithm.fb.CompositeConvexHull;
import CSU_Yunlu_2019.module.algorithm.fb.FireCluster;
import CSU_Yunlu_2019.standard.Ruler;
import CSU_Yunlu_2019.world.CSUFireBrigadeWorld;
import CSU_Yunlu_2019.world.object.CSUBuilding;
import CSU_Yunlu_2019.world.object.CSURoad;
import javolution.util.FastSet;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * First we choose the closest fire cluster from this Agent. Then we find the
 * expand direction of this fire cluster. We get buildings in that direction,
 * and with the border buildings of this fire cluster, we calculate their
 * buildingValues. For inDirectionBuildings and borderBuildings, we choose
 * differnet standard to calculate buildingValues. Very obvious,
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

            if (sortedBuildings != null && !sortedBuildings.isEmpty()) {
                this.target = world.getCsuBuilding(sortedBuildings.first().first().first());
                targetBuilding = new FireBrigadeTarget(targetCluster, this.target);
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

        Polygon cluster_po = cluster.getConvexHull().getConvexPolygon();

        for (StandardEntity next : gasStas) {
            GasStation sta = (GasStation) next;

            if (sta.isFierynessDefined() && sta.getFieryness() >= 1)
                continue;

            if (sta.isTemperatureDefined() && sta.getTemperature() < 25)      ///temperature
                continue;

            CSUBuilding csuBu = world.getCsuBuilding(next.getID());
            if (csuBu.getWaterQuantity() >= 3000)  // two cycles   ///water
                continue;

            int[] apexs = sta.getApexList();
            CompositeConvexHull convexHull = new CompositeConvexHull();

            for (int i = 0; i < apexs.length; i += 2) {
                convexHull.addPoint(apexs[i], apexs[i + 1]);
            }

            Polygon po = convexHull.getConvexPolygon();

            int dis = (int) Ruler.getDistance(cluster_po, po);
            if (dis <= 5000)     ///distance
                targetGasStas.add(world.getCsuBuilding(next.getID()));
        }

        if (targetGasStas.contains(lastTarget))
            return new FireBrigadeTarget(cluster, lastTarget);

        CSUBuilding tar = null;
        double minDistance = Double.MAX_VALUE;
        for (CSUBuilding next : targetGasStas) {
            double dis = world.getDistance(next.getId(), selfHuman.getID());
            if (dis < minDistance) {
                minDistance = dis;
                tar = next;
            }
        }

        if (tar != null) {
            this.target = tar;
            return new FireBrigadeTarget(cluster, tar);
        } else
            return null;
    }

    /**
     * Remove all buildings that this agent has seen. And also, all buildings
     * other agent might see.
     * first, the underlyingAgent, add buildings from getChanged()
     * second, other agents, if position is defined, if in building add it, then add observableAreas
     *
     * @return a list of looked buildings
     */
    private List<EntityID> lookedBuildings() {
        List<EntityID> result = new ArrayList<>();

        Human agent;
        for (StandardEntity next : world.getPlatoonAgentsWithURN()) {
            agent = (Human) next;

            if (agent.getID().getValue() == selfHuman.getID().getValue()) {
                for (EntityID visible : world.getAgentInfo().getChanged().getChangedEntities()) {
                    StandardEntity v_entity = world.getEntity(visible);
                    if (v_entity instanceof Building) {
                        result.add(visible);
                    }
                }
            }

            if (agent.isPositionDefined()) {              ///world.getSelfPosition() && agent.getPosition()
                StandardEntity position = world.getSelfPosition();
                List<EntityID> visible = null;
                if (position instanceof Building) {
                    CSUBuilding csu_b = world.getCsuBuilding(position.getID());
                    visible = csu_b.getObservableAreas();
                    result.add(agent.getPosition());

                }
                if (position instanceof Road) {
                    CSURoad csu_r = world.getCsuRoad(position.getID());
                    visible = csu_r.getObservableAreas();
                }

                if (visible != null) {
                    for (EntityID visible_id : visible) {
                        StandardEntity v_entity = world.getEntity(visible_id);
                        if (v_entity instanceof Building) {
                            result.add(visible_id);
                        }
                    }
                }
            }
        }

        return result;
    }

//    private Set<EntityID> unreachables(Area area) {
//        Set<EntityID> result = new FastSet<>();
//
//        if (area instanceof Building) {
//            for (Road entrance : world.getEntrance().getEntrance((Building) area)) {
//                for (Building next : world.getEntrance().getBuilding(entrance)) {
//                    result.add(next.getID());
//                }
//            }
//        } else {
//            List<Building> bu_s = world.getEntrance().getBuilding((Road) area);
//            if (bu_s != null) {
//                for (Building next : bu_s) {
//                    result.add(next.getID());
//                }
//            }
//        }
//
//        return result;
//    }

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

        Set<StandardEntity> borderBuildings = new FastSet<>(fireCluster.getBorderEntities());
        ///System.out.println(world.getTime() + ", "+ world.me + ",fireCluster: " + fireCluster.getFireCondition());
        ///System.out.println(world.getTime() + ", "+ world.me + ",borderBuildings  "+borderBuildings);///
        Set<CSUBuilding> inDirectionBuildings;
        Point directionPoint = directionManager.findFarthestPointOfMap(fireCluster, (FireBrigade) selfHuman);
        inDirectionBuildings = fireCluster.findBuildingInDirection(directionPoint);
        ///System.out.println(world.getTime() + ", "+world.me +", inDirectionBuildings  " + inDirectionBuildings);///
        borderBuildings.removeAll(csuBuildingToEntity(inDirectionBuildings));

        this.calculateValueOfInDirectionBuildings(inDirectionBuildings, sortedBuildings);
        this.calculateValueOfBorderBuildings(borderBuildings, sortedBuildings);

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
     * Classify buildings.
     *
     * <pre>
     * 1.lessValueBuildings: the border buildings of dying FireCLuster
     *
     * 2.mapBorderBuildings: the border buildings of map border FireCLuster
     *
     * 3.highValueBuildings: buildings in the expand direction of its FireCluster
     *
     * 4.otherBuildings: the remaining border buildings of FireCluster
     * </pre>
     *
     * @param lessValue a set to store less value buildings
     * @param mapBorder a set to store map border buildings
     * @return a set of high value buildings
     */
//    private Set<CSUBuilding> buildingClassifier(Set<CSUBuilding> lessValue, Set<CSUBuilding> mapBorder) {
//        Set<CSUBuilding> highValueBuildings = new HashSet<>();
//        List<FireCluster> fireClusters = world.getFireClustering().getClusters();
//
//        ConvexObject convexObject;
//        Polygon polygon;
//        CSUBuilding csuBuilding;
//
//        for (FireCluster cluster : fireClusters) {
//            if (cluster == null)
//                continue;
//            if (cluster.isDying()) {
//                for (StandardEntity next : cluster.getBorderEntities())
//                    lessValue.add(this.world.getCsuBuilding(next.getID()));
//                continue;
//            }
//            if (cluster.isBorder()) {
//                for (StandardEntity next : cluster.getBorderEntities())
//                    mapBorder.add(this.world.getCsuBuilding(next.getID()));
//                continue;
//            }
//
//            directionManager.findFarthestPointOfMap(cluster, controlledEntity);
//            convexObject = cluster.getConvexObject();
//            if (convexObject == null || convexObject.CENTER_POINT == null
//                    || convexObject.CONVEX_POINT == null || convexObject.getConvexHullPolygon() == null)
//                continue;
//
//            if (cluster.isOverCenter()) {
//                polygon = convexObject.getDirectionRectangle();
//            } else {
//                polygon = convexObject.getTriangle();
//            }
//
//            for (StandardEntity next : cluster.getBorderEntities()) {
//                csuBuilding = world.getCsuBuilding(next.getID());
//                int[] vertices = csuBuilding.getSelfBuilding().getApexList();
//                for (int i = 0; i < vertices.length; i += 2) {
//                    if (polygon.contains(vertices[i], vertices[i + 1])) {
//                        highValueBuildings.add(csuBuilding);
//                        break;
//                    }
//                }
//            }
//        }
//
//        return highValueBuildings;
//    }

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
}
