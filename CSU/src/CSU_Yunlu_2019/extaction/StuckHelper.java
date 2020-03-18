package CSU_Yunlu_2019.extaction;

import CSU_Yunlu_2019.CSUConstants;
import CSU_Yunlu_2019.module.algorithm.fb.CompositeConvexHull;
import CSU_Yunlu_2019.module.complex.fb.tools.FbUtilities;
import CSU_Yunlu_2019.standard.Ruler;
import CSU_Yunlu_2019.util.Util;
import CSU_Yunlu_2019.world.CSUWorldHelper;
import CSU_Yunlu_2019.world.object.CSUEdge;
import CSU_Yunlu_2019.world.object.CSULineOfSightPerception;
import CSU_Yunlu_2019.world.object.CSURoad;
import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author: Guanyu-Cai
 * @Date: 03/10/2020
 */
public class StuckHelper {
    private CSUWorldHelper world;
    private CSULineOfSightPerception lineOfSightPerception;


    public StuckHelper(CSUWorldHelper world) {
        this.world = world;
        this.lineOfSightPerception = new CSULineOfSightPerception(world);
    }

    public Action calc(List<EntityID> path) {
        if (path == null || path.size() < 2) {
            return null;
        }

        List<EntityID> selfAndNeighborBlockades = getSelfAndNeighborBlockades();
        EntityID nearestBlockade = getNearestBlockade();
        //获取所有距离自己紧挨着的blockade小于AGENT_PASSING_THRESHOLD的blockades,包括此blockade
        List<StandardEntity> nearBlockades = getBlockadesInRange(nearestBlockade, selfAndNeighborBlockades, CSUConstants.AGENT_PASSING_THRESHOLD);
        if (nearBlockades.isEmpty()) {
            return null;
        }
        //获取nearBlockades的凸包
        CompositeConvexHull blockadesConvexHull = getConvexByBlockades(nearBlockades);

        StandardEntity selfPosition = world.getSelfPosition();
        Pair<Integer, Integer> selfLocation = world.getSelfLocation();
        Point2D locationPoint = new Point2D(selfLocation.first(), selfLocation.second());
        Point2D openPartCenter = null;
        Point2D edgeStart = null;
        Point2D edgeEnd = null;
        CSUEdge targetEdge = null;
        if (selfPosition instanceof Road) {
            CSURoad csuRoad = world.getCsuRoad(selfPosition);
            for (CSUEdge csuEdge : csuRoad.getCsuEdgesTo(path.get(1))) {
                if (!csuEdge.isBlocked()) {
                    //和目标地点相连的没有阻塞的edge的开放部分的中点
                    openPartCenter = csuEdge.getOpenPartCenter();
                    edgeStart = csuEdge.getStart();
                    edgeEnd = csuEdge.getEnd();
                    targetEdge = csuEdge;
                    break;
                }
            }
        }

        if (openPartCenter == null) {//如果已经没有可以通过的openPart
            return null;
        }

        Line2D guideLine = new Line2D(locationPoint, openPartCenter);
        //尝试直接走到openPartCenter下一个身位的位置
        if (!Util.hasIntersectLine(blockadesConvexHull.getConvexPolygon(), guideLine)) {//由于使用了凸包,会漏判一些实际能走的情况
//            //延长AGENT_SIZE的长度
//            Point2D endPoint = Util.improveLine(guideLine, CSUConstants.AGENT_SIZE).getEndPoint();
            //延长至视线末端
            Point2D endPoint = Util.clipLine(guideLine, world.getConfig().maxRayDistance).getEndPoint();
            if (CSUConstants.DEBUG_STUCK_HELPER) {
                System.out.println(world.getSelfHuman().getID() + " stuckHelper succeed to point " + endPoint + " planPath(0): " + path.get(0));
            }
            return moveToPoint(endPoint);
        } else {//guideLine被挡住
            //逃脱点有效的位置,包括当前道路和openPart相对的道路
            Collection<CSURoad> targetValidRoads = new HashSet<>();
            CSURoad csuRoad = world.getCsuRoad(selfPosition);
            targetValidRoads.add(csuRoad);
            CSURoad oppositePassableEdgeRoad = csuRoad.getOppositePassableEdgeRoad(targetEdge);
            if (oppositePassableEdgeRoad != null) {
                targetValidRoads.add(oppositePassableEdgeRoad);
            }

            //a large number
            int distance = 300000;
            Set<CSULineOfSightPerception.CsuRay> raysNotHit1 = lineOfSightPerception.findRaysNotHit(locationPoint, nearBlockades, distance);
            Set<CSULineOfSightPerception.CsuRay> raysNotHit2 = lineOfSightPerception.findRaysNotHit(openPartCenter, nearBlockades, distance);
            Set<CSULineOfSightPerception.CsuRay> raysNotHit3 = lineOfSightPerception.findRaysNotHit(edgeStart, nearBlockades, distance);
            Set<CSULineOfSightPerception.CsuRay> raysNotHit4 = lineOfSightPerception.findRaysNotHit(edgeEnd, nearBlockades, distance);
            Set<Set<CSULineOfSightPerception.CsuRay>> raysNotHits = new HashSet<>();
            raysNotHits.add(raysNotHit2);
            raysNotHits.add(raysNotHit3);
            raysNotHits.add(raysNotHit4);
            for (Set<CSULineOfSightPerception.CsuRay> next : raysNotHits) {
                List<Point2D> validIntersections = getValidIntersections(raysNotHit1, next, targetValidRoads);
                if (!validIntersections.isEmpty()) {
                    validIntersections.sort(new DistanceComparator(locationPoint));
                    if (CSUConstants.DEBUG_STUCK_HELPER) {
                        System.out.println(world.getSelfHuman().getID() + " stuckHelper succeed to point " + validIntersections.get(0) + " planPath(0): " + path.get(0));
                    }
                    return moveToPoint(validIntersections.get(0));
                }
            }
        }
        return null;
    }

    /**
    * @Description: 获取a和b中rays相交在targetRoads范围内的交点
    * @Author: Guanyu-Cai
    * @Date: 3/13/20
    */
    private List<Point2D> getValidIntersections(Set<CSULineOfSightPerception.CsuRay> a, Set<CSULineOfSightPerception.CsuRay> b,
                                                Collection<CSURoad> targetValidRoads) {
        List<Point2D> intersections = Util.getSegmentIntersections(a, b);
        if (!intersections.isEmpty()) {
            filterIntersectionsNotInRoads(intersections, targetValidRoads);
        }
        return intersections;
    }

    /**
    * @Description: 获取自己所在road和最近的road
    * @Author: Guanyu-Cai
    * @Date: 3/13/20
    */
    private Collection<CSURoad> getSelfAndNearestRoad() {
        CSURoad csuRoad = world.getCsuRoad(world.getSelfPosition());
        Collection<CSURoad> result = new HashSet<>();
        result.add(csuRoad);
        result.add(world.getNearestNeighborRoad());
        return result;
    }

    /**
    * @Description: 去除不在selfRoad和neighbourRoad上的交点
    * @Author: Guanyu-Cai
    * @Date: 3/13/20
    */
    private void filterIntersectionsNotInRoads(Collection<Point2D> points , Collection<CSURoad> roads) {
        Collection<Point2D> toRemove = new HashSet<>();
        for (Point2D point : points) {
            boolean contain = false;
            for (CSURoad road : roads) {
                Polygon polygon = road.getPolygon();
                if (polygon.contains(point.getX(), point.getY())) {
                    contain = true;
                    break;
                }
            }
            if (!contain) {
                toRemove.add(point);
            }
        }
        points.removeAll(toRemove);
    }

    private Action moveToPoint(Point2D target) {
        List<EntityID> path = new ArrayList<>();
        path.add(world.getSelfPosition().getID());
        return new ActionMove(path, (int) target.getX(), (int) target.getY());
    }

    /**
     * @param blockades 所有障碍物
     * @return 所有障碍物组成的凸包
     */
    private CompositeConvexHull getConvexByBlockades(List<StandardEntity> blockades) {
        CompositeConvexHull convexHull = new CompositeConvexHull();
        for (StandardEntity entity : blockades) {
            Blockade blockade;
            if (entity instanceof Blockade) {
                blockade = (Blockade) entity;
                for (int i = 0; i < blockade.getApexes().length; i += 2) {
                    convexHull.addPoint(blockade.getApexes()[i], blockade.getApexes()[i + 1]);
                }
            }
        }
        return convexHull;
    }

    /**
     * @return 获取当前道路和相邻道路的所有blockades
     */
    private List<EntityID> getSelfAndNeighborBlockades() {
        StandardEntity selfPosition = world.getSelfPosition();
        List<EntityID> blockadeIDs = new ArrayList<>();

        if (selfPosition instanceof Road) {
            Road road = (Road) selfPosition;
            for (StandardEntity next : world.getEntities(road.getNeighbours())) {
                if (next instanceof Road) {
                    Road neighbour = (Road) next;
                    if (neighbour.isBlockadesDefined()) {
                        blockadeIDs.addAll(neighbour.getBlockades());
                    }
                }
            }
            //邻居和自己路上的blockades
            blockadeIDs.addAll(road.getBlockades());
            return blockadeIDs;
        }
        return null;
    }

    /**
     * @return 距离自己当前坐标最近的blockade, 基本上等同于直接接触到的那个blockade
     */
    private EntityID getNearestBlockade() {
        List<EntityID> selfAndNeighborBlockades = getSelfAndNeighborBlockades();
        EntityID nearest = null;
        if (selfAndNeighborBlockades != null) {
            nearest = FbUtilities.getNearest(world, selfAndNeighborBlockades, world.getSelfHuman().getID());
        }
        return nearest;
    }

    /**
     * @param entity   目标blockade
     * @param entities 供选择的blockades
     * @param range    范围
     * @return entities中距离entity在range范围内的全体
     */
    private List<StandardEntity> getBlockadesInRange(EntityID entity, List<EntityID> entities, int range) {
        List<StandardEntity> inRangeBlockades = new ArrayList<>();
        if (entities == null || entities.isEmpty()) {
            return inRangeBlockades;
        }
        Blockade anchorBlockade = (Blockade) world.getEntity(entity);
        for (EntityID blockadeID : entities) {
            StandardEntity standardEntity = world.getEntity(blockadeID);
            if (!(standardEntity instanceof Blockade)) {
                continue;
            }
            Blockade blockade = (Blockade) standardEntity;
            double dist = Ruler.getDistance(Util.getPolygon(blockade.getApexes()), Util.getPolygon(anchorBlockade.getApexes()));
            if (dist < range) {
                inRangeBlockades.add(blockade);
            }
        }
        return inRangeBlockades;
    }

    private static class DistanceComparator implements Comparator<Point2D> {
        private Point2D reference;
        public DistanceComparator(Point2D reference) {
            this.reference = reference;
        }
        @Override
        public int compare(Point2D a, Point2D b) {
            int d1 = (int) Ruler.getDistance(reference, a);
            int d2 = (int) Ruler.getDistance(reference, b);
            return d1 - d2;
        }
    }
}
