package mrl_2019.extaction.clear;

import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import javolution.util.FastSet;
import mrl_2019.MRLConstants;
import mrl_2019.util.Util;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.entity.MrlBlockade;
import mrl_2019.world.entity.MrlEdge;
import mrl_2019.world.entity.MrlRoad;
import mrl_2019.world.helper.RoadHelper;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: MRL_RSL
 */
public class ClearTools {
    private WorldInfo worldInfo;
    private AgentInfo agentInfo;
    private ScenarioInfo scenarioInfo;
    int maxClearDistance;
    private MrlWorldHelper worldHelper;

    public ClearTools(MrlWorldHelper worldHelper, WorldInfo worldInfo, AgentInfo agentInfo, ScenarioInfo scenarioInfo) {
        this.worldInfo = worldInfo;
        this.agentInfo = agentInfo;
        this.scenarioInfo = scenarioInfo;
        this.worldHelper = worldHelper;
        maxClearDistance = scenarioInfo.getClearRepairDistance();
    }

    /**
     * this method create a list of guide line used by pf to clearing area
     *
     * @param path             list of area id calculated by path planning algorithms(such as AStar). this list must be started by
     *                         current agent position id and should not be null or empty.
     * @param destinationPoint
     * @return list of guideline indexed like path indexes
     */
    public List<GuideLine> getPathGuidelines(List<EntityID> path, Point2D destinationPoint) {
        List<GuideLine> guideLineList = new ArrayList<GuideLine>();

        if (path == null) {
            return null;
        }


        Area sourceArea;
        if (path.isEmpty()) {
            sourceArea = (Area) worldInfo.getEntity(agentInfo.getPosition());
        } else {
            sourceArea = (Area) worldInfo.getEntity(path.get(0));
        }
        Point2D firstPoint = Util.getPoint(worldInfo.getLocation(sourceArea));
        if (path.size() > 1) {
            Edge edgeTo = agentInfo.getPositionArea().getEdgeTo(path.get(1));
            if (edgeTo != null) {
                Point2D mid1 = Util.getMiddle(edgeTo.getLine());
                Point2D agentPosition = Util.getPoint(worldInfo.getLocation(agentInfo.getID()));//current agent location
                Line2D agentEdgeLine = new Line2D(agentPosition, mid1);
                Line2D agentAreaLine = new Line2D(agentPosition, firstPoint);
                Line2D areaEdgeLine = new Line2D(firstPoint, mid1);
                double theta = Util.angleBetween2Lines(agentAreaLine, areaEdgeLine);
                double alpha = Util.angleBetween2Lines(agentAreaLine, agentEdgeLine);
                if (alpha < 80 && theta > 80) {
//                    world.printData("theta = " + theta + " alpha = " + alpha);
                    firstPoint = agentPosition;
                }
            }
        }
        //guideLineList.add(new GuideLine(sourcePoint, firstPoint));

        Area area;
        Edge edge;
        GuideLine guideLine;
        for (int i = 0; i < path.size() - 1; i++) {
            EntityID id = path.get(i);
            area = (Area) worldInfo.getEntity(id);
            edge = area.getEdgeTo(path.get(i + 1));
            if (edge == null) {
                break;
            }
            Point2D middle = Util.getMiddle(edge.getLine());
            guideLine = new GuideLine(firstPoint, middle);
            guideLineList.add(guideLine);
            firstPoint = middle;
        }

        //add last path area guideline
        Point2D lastPoint;
        if (destinationPoint == null) {
            Area lastArea = (Area) worldInfo.getEntity(path.get(path.size() - 1));
            lastPoint = Util.getPoint(worldInfo.getLocation(lastArea));
        } else {
            lastPoint = destinationPoint;
        }

        if (firstPoint.equals(lastPoint)) {
            System.out.println("first point and last point are equal");
        } else {
            guideLine = new GuideLine(firstPoint, lastPoint);
            guideLineList.add(guideLine);
        }

        return guideLineList;
    }


    /**
     * this function merge similar guidelines
     * merge criteria is similarity of current guide line "Slope" and "angle" than next one.
     *
     * @param guideLines
     * @param path
     * @param target
     * @return
     */
    public GuideLine getTargetGuideLine(List<GuideLine> guideLines, List<EntityID> path, EntityID target, double clearDistance) {
        if (guideLines.isEmpty()) {
            return null;
        }
        double secureAngle = 30d;
        GuideLine guideLine = guideLines.get(0);

        GuideLine targetGuideLine;
        Point2D selfPosition = Util.getPoint(worldInfo.getLocation(agentInfo.getID()));
//        if (Util.distance(guideLine.getEndPoint(), selfPosition) > Util.distance(guideLine.getOrigin(), selfPosition)) {//this condition prevents problem of too long area.
//            targetGuideLine = guideLine;
//        } else {
        targetGuideLine = guideLine;
//        targetGuideLine.setAreas(guideLine.getAreas());
//        }
        List<EntityID> areas = new ArrayList<EntityID>();
        areas.add(path.get(0));
        double angle;
        List<GuideLine> tempGuidelines = new ArrayList<GuideLine>();
        boolean firsLine = true;
        for (int i = 1; i < guideLines.size(); i++) {
            guideLine = guideLines.get(i);
            angle = Util.angleBetween2Lines(targetGuideLine, guideLine);
            double distance = (targetGuideLine.getLength() + guideLine.getLength());
            double distanceCost = 1 - (guideLine.getLength() / distance);//area distance cost (cost < 1)
            if ((firsLine && distance < clearDistance / 5) || angle < (secureAngle * distanceCost)) {
                Vector2D newDirection = targetGuideLine.getDirection().add(guideLine.getDirection());
                targetGuideLine = new GuideLine(targetGuideLine.getOrigin(), newDirection);
                tempGuidelines.add(targetGuideLine);

                if (path.size() > i) {
                    areas.add(path.get(i));
                }
                targetGuideLine.getAreas().addAll(areas);
            } else {
                break;
            }
            firsLine = false;
        }
        targetGuideLine.setAreas(areas);

        for (int j = 0; j < tempGuidelines.size(); j++) {
            //check through list to prevent distance fault
            GuideLine line1 = tempGuidelines.get(j);
            double a = Util.angleBetween2Lines(line1, targetGuideLine);
            double dist = line1.getLength() * Math.sin(Math.toRadians(a));
            if (dist > scenarioInfo.getClearRepairRad()) {
                targetGuideLine = line1;
                break;
            }
        }
        return targetGuideLine;
    }

    /**
     * determine whether @point2D in the any blockade of the {@code road} or not;
     *
     * @param point2D the point we want know is it in the blockade or not
     * @param road    the road which we want check
     * @return
     */
    public boolean isInAnyBlockade(Point2D point2D, Road road) {
        if (!road.isBlockadesDefined() || road.getBlockades().isEmpty()) {
            return false;
        }

        Blockade blockade;
        for (EntityID blockadeID : road.getBlockades()) {
            blockade = (Blockade) worldInfo.getEntity(blockadeID);
            Polygon polygon = Util.getPolygon(blockade.getApexes());
            if (polygon.contains(point2D.getX(), point2D.getY())) {
                return true;
            }
        }
        return false;
    }

    public boolean isOnBlockade(Point2D point2D, List<Area> areas) {
        Blockade blockade;
        Polygon polygon;
        for (Area area : areas) {
            if (!area.isBlockadesDefined()) {
                continue;
            }
            for (EntityID blockadeID : area.getBlockades()) {
                blockade = (Blockade) worldInfo.getEntity(blockadeID);
                polygon = Util.getPolygon(blockade.getApexes());
                if (polygon.contains(point2D.getX(), point2D.getY())) {
                    return true;
                }

            }
        }
        return false;
    }


    public boolean anyBlockadeIntersection(Area area, Line2D targetLine) {
        if (!(area instanceof Road) || targetLine == null) {
            return false;
        }
        MrlRoad mrlRoad = getMrlRoad(area.getID());
        for (MrlBlockade mrlBlockade : mrlRoad.getMrlBlockades()) {
            if (Util.hasIntersection(mrlBlockade.getPolygon(), targetLine)) {
                return true;
            }
        }
        return false;
    }


    public List<Point2D> blockadesIntersections(Area area, Line2D targetLine) {
        if (!(area instanceof Road) || targetLine == null) {
            return new ArrayList<Point2D>();
        }
        List<Point2D> points = new ArrayList<Point2D>();
        MrlRoad mrlRoad = getMrlRoad(area.getID());
        for (MrlBlockade mrlBlockade : mrlRoad.getMrlBlockades()) {
            points.addAll(Util.intersections(mrlBlockade.getPolygon(), targetLine));
        }
        return points;
    }

    /**
     * This method returns blockades which block the middle point of a road or two passing edges of way roads
     *
     * @param pathToGo entityID list of path entities to a target
     * @param target   target which police want to reach
     * @return blockades which might make troubles for path planning of any agents
     */
    public boolean shouldCheckForBlockadesOnWay(List<EntityID> pathToGo, EntityID target) {

        boolean shouldCheck = false;
        Set<MrlBlockade> obstacles_MrlBlockades = new HashSet<MrlBlockade>();
        Set<Blockade> blockades = new FastSet<Blockade>();


        MrlEdge sourceEdge = null;
        Pair<Integer, Integer> nextMiddlePoint = null;
        Pair<Integer, Integer> sourceMiddlePoint = null;


        if (pathToGo.isEmpty()) {
            shouldCheck = false;
        } else if (pathToGo.size() == 1) {
            if (worldInfo.getEntity(pathToGo.get(0)) instanceof Road) {
                Road road = (Road) worldInfo.getEntity(pathToGo.get(0));
                if (road.isBlockadesDefined()) {
                    if (worldInfo.getEntity(target) instanceof Human) {
                        Human human = (Human) worldInfo.getEntity(target);
                        if (Util.isOnBlockade(worldInfo, agentInfo, human) || Util.isNearBlockade(worldInfo, human)) {
                            shouldCheck = true;
                        } else {
                            shouldCheck = false;
                        }

                    } else {
                        Building building = (Building) worldInfo.getEntity(target);
                        Set<Edge> edges = RoadHelper.getEdgesBetween((Area) worldInfo.getEntity(pathToGo.get(0)), building);
                        if (edges != null && !edges.isEmpty()) {
                            Edge edge = edges.iterator().next();
                            int middleX = (edge.getStartX() + edge.getEndX()) / 2;
                            int middleY = (edge.getStartY() + edge.getEndY()) / 2;

                            for (EntityID blockadeEntityID : road.getBlockades()) {
                                if (Util.findDistanceTo((Blockade) worldInfo.getEntity(blockadeEntityID), middleX, middleY) < MRLConstants.AGENT_SIZE) {
                                    shouldCheck = true;
                                } else {
                                    shouldCheck = false;
                                }
                            }

                        }
                    }


                } else {
                    shouldCheck = true;
                }

            }
        } else {// if seenPath contains more than one entity
            int j;
            EntityID sourceAreaID = null;
            EntityID nextAreaID = null;
            Area sourceArea = null;
            Area nextArea = null;
            Set<Edge> edgeSet;
            MrlRoad mrlRoad = null;
            MrlEdge nextEdge = null;
            List<EntityID> neighbours;
            for (int i = 0; i < pathToGo.size() - 1; i++) {
                j = i + 1;
                sourceAreaID = pathToGo.get(i);
                nextAreaID = pathToGo.get(j);
                sourceArea = (Area) worldInfo.getEntity(sourceAreaID);
                nextArea = (Area) worldInfo.getEntity(nextAreaID);
                if (sourceArea instanceof Road) {
                    mrlRoad = getMrlRoad(sourceAreaID);
//                    neighbours = sourceArea.getNeighboursByEdge();
//                    for (EntityID neighbourID : neighbours) {
//                        // the neighbour of this area is also in my way, so it should be cleared
//                        if (pathToGo.contains(neighbourID) && pathToGo.indexOf(neighbourID)>pathToGo.indexOf(sourceAreaID)) {
//                    edgeSet = RoadHelper.getEdgesBetween(sourceArea, nextArea);
                    Edge edge = sourceArea.getEdgeTo(nextArea.getID());
                    if (edge != null) {
                        nextEdge = mrlRoad.getMrlEdge(edge);
                        nextMiddlePoint = new Pair<Integer, Integer>((int) nextEdge.getMiddle().getX(), (int) nextEdge.getMiddle().getY());
                        if (sourceEdge == null) {

                            if (agentInfo.getPositionArea().equals(sourceArea)) {
                                obstacles_MrlBlockades.addAll(getRoadObstacles(mrlRoad, worldInfo.getLocation(agentInfo.getID()), nextMiddlePoint));
                            } else {
                                obstacles_MrlBlockades.addAll(getRoadObstacles(mrlRoad, worldInfo.getLocation(sourceArea), nextMiddlePoint));
                            }
                        } else {
                            sourceMiddlePoint = new Pair<Integer, Integer>((int) sourceEdge.getMiddle().getX(), (int) sourceEdge.getMiddle().getY());
                            obstacles_MrlBlockades.addAll(getRoadObstacles(mrlRoad, sourceMiddlePoint, nextMiddlePoint));
                        }

                        if (!obstacles_MrlBlockades.isEmpty()) {
                            shouldCheck = true;
                        }
                    }


                    if (shouldCheck) {
                        break;
                    }
//                        }
//                    }
//                    edgeSet = RoadHelper.getEdgesBetween(sourceArea, nextArea);
//                    nextEdge=null;
//                    if(edgeSet.iterator().hasNext()){
//                        nextEdge=mrlRoad.getMrlEdge(edgeSet.iterator().next());
//                    }
                    sourceEdge = nextEdge;


                } else {
                    if (nextArea instanceof Road) {
                        mrlRoad = getMrlRoad(nextAreaID);
                        neighbours = sourceArea.getNeighbours();
                        for (EntityID neighbourID : neighbours) {
                            // the neighbour of this area is also in my way, so it should be cleared
                            if (pathToGo.contains(neighbourID) && pathToGo.indexOf(neighbourID) > pathToGo.indexOf(sourceAreaID)) {
                                edgeSet = RoadHelper.getEdgesBetween(sourceArea, nextArea);
                                for (Edge edge : edgeSet) {
                                    nextEdge = mrlRoad.getMrlEdge(edge);
                                    nextMiddlePoint = new Pair<Integer, Integer>((int) nextEdge.getMiddle().getX(), (int) nextEdge.getMiddle().getY());
                                    if (sourceEdge == null) {

                                        if (agentInfo.getPositionArea().equals(sourceArea)) {
                                            obstacles_MrlBlockades.addAll(getRoadObstacles(mrlRoad, worldInfo.getLocation(agentInfo.getID()), nextMiddlePoint));
                                        } else {
                                            obstacles_MrlBlockades.addAll(getRoadObstacles(mrlRoad, worldInfo.getLocation(sourceArea), nextMiddlePoint));
                                        }
                                    } else {
                                        sourceMiddlePoint = new Pair<Integer, Integer>((int) sourceEdge.getMiddle().getX(), (int) sourceEdge.getMiddle().getY());
                                        obstacles_MrlBlockades.addAll(getRoadObstacles(mrlRoad, sourceMiddlePoint, nextMiddlePoint));
                                    }

                                    if (!obstacles_MrlBlockades.isEmpty()) {
                                        shouldCheck = true;
                                    }
                                }

                                if (shouldCheck) {
                                    break;
                                }

                            }
                        }

                        if (shouldCheck) {
                            break;
                        }

                        edgeSet = RoadHelper.getEdgesBetween(sourceArea, nextArea);
                        nextEdge = null;
                        if (edgeSet.iterator().hasNext()) {
                            nextEdge = mrlRoad.getMrlEdge(edgeSet.iterator().next());
                        }
                        sourceEdge = nextEdge;
                    } else {
                        sourceEdge = null;
                    }
                }
            }


            if (!shouldCheck) {


                //find blockades of last Entity in the sequence
                if (nextArea instanceof Road) {
                    //target is an agent which is on the road
                    StandardEntity entity = worldInfo.getEntity(target);
                    if (sourceEdge != null) {
                        Set<MrlBlockade> mrlBlockades = getRoadObstacles(getMrlRoad(nextArea.getID()), worldInfo.getLocation(entity), new Pair<Integer, Integer>((int) sourceEdge.getMiddle().getX(), (int) sourceEdge.getMiddle().getY()));
                        if (mrlBlockades != null && !mrlBlockades.isEmpty()) {
                            shouldCheck = true;
                        }
                    }
                } else {

                    //do nothing
                }

            }
        }


        return shouldCheck;
    }

    public boolean isInClearRange(Blockade target) {
        Pair<Integer, Integer> location = worldInfo.getLocation(agentInfo.getID());
        return Util.findDistanceTo(target, location.first(), location.second()) < maxClearDistance;
    }

    public Pair<Blockade, Integer> findNearestBlockade(Set<Blockade> blockades) {

        if (blockades == null || blockades.isEmpty()) {
            return null;
        }

        Pair<Integer, Integer> location = worldInfo.getLocation(agentInfo.getID());
        int x = location.first();
        int y = location.second();


        int minDistance = Integer.MAX_VALUE;
        Pair<Blockade, Integer> nearestBlockadePair = new Pair<Blockade, Integer>(blockades.iterator().next(), 0);
        int tempDistance;
        for (Blockade blockade : blockades) {
//            tempDistance = Util.distance(world.getSelfHuman().getX(), world.getSelfHuman().getY(), blockade.getX(), blockade.getY());
            tempDistance = Util.findDistanceTo(blockade, x, y);
            if (tempDistance < minDistance) {
                minDistance = tempDistance;
                nearestBlockadePair = new Pair<Blockade, Integer>(blockade, minDistance);
            }
        }

        return nearestBlockadePair;
    }

    /**
     * THis method gets obstacles which has intersect/s with a line from {@code firstPoint} to {@code secondPoint}
     *
     * @param mrlRoad     the road we want found obstacles of
     * @param firstPoint  head point of the expressed line
     * @param secondPoint end point of the expressed line
     * @return set of {@code MrlBlockade} which has intersect to the expressed line
     */
    public Set<MrlBlockade> getRoadObstacles(MrlRoad mrlRoad, Pair<Integer, Integer> firstPoint, Pair<Integer, Integer> secondPoint) {
        Set<MrlBlockade> obstacles = new HashSet<MrlBlockade>();
        for (MrlBlockade blockade : mrlRoad.getMrlBlockades()) {
            Point2D sourcePoint = Util.getPoint(firstPoint);
            Point2D endPoint = Util.getPoint(secondPoint);
            if (blockade.getPolygon().contains(firstPoint.first(), firstPoint.second()) || Util.intersections(blockade.getPolygon(), new Line2D(sourcePoint, endPoint)).size() > 0
                    || Util.findDistanceTo(blockade.getParent(), firstPoint.first(), firstPoint.second()) < 500
                    || Util.findDistanceTo(blockade.getParent(), secondPoint.first(), secondPoint.second()) < 500) {
                obstacles.add(blockade);
            }
        }

        return obstacles;
    }


    public Set<Blockade> getBlockadesInRange(Pair<Integer, Integer> targetLocation, Set<Blockade> blockadeSeen, int range) {
        Set<Blockade> blockades = new HashSet<Blockade>();
        for (Blockade blockade : blockadeSeen) {
            if (Util.findDistanceTo(blockade, targetLocation.first(), targetLocation.second()) < range) {
                blockades.add(blockade);
            }
        }
        return blockades;
    }

    public Set<Blockade> getBlockadesInRange(Road road, Set<Blockade> blockadeSeen) {

        Set<Blockade> blockadeSet = new FastSet<Blockade>();
        for (Blockade blockade : blockadeSeen) {
            for (Edge edge : road.getEdges()) {
                if (edge.isPassable() && Util.findDistanceTo(blockade, road.getX(), road.getY()) < 1200) {
                    blockadeSet.add(blockade);
                }

            }
        }

        return blockadeSet;

    }

    public Set<Blockade> getTargetRoadBlockades(Road road) {
        Set<Blockade> blockades = new FastSet<Blockade>();
        if (worldInfo.getChanged().getChangedEntities().contains(road.getID()) && road.isBlockadesDefined()) {
            for (EntityID entityID : road.getBlockades()) {
                blockades.add((Blockade) worldInfo.getEntity(entityID));
            }
        }
        return blockades;
    }

    /**
     * this method calculate and return 2 parallel line around clear length lines at the specific distance from each of them to avoid rounding fault
     *
     * @param clearLine1  clear length line1
     * @param clearLine2  clear length line2
     * @param clearRadius clear radius
     * @param distance    distance of wanted lines from each length lines
     * @return pair of parallel line in the specific distance from entry lines {@code clearLine1} and {@code clearLine2}
     */
    public Pair<Line2D, Line2D> getClearSecureLines(Line2D clearLine1, Line2D clearLine2, double clearRadius, double distance) {
        Line2D l1, l2;
        double x1, y1, x2, y2;
        Point2D origin1, endPoint1;
        Point2D origin2, endPoint2;
        double ratio = distance / (clearRadius * 2);
        double minX1, maxX1, minY1, maxY1;
        double minX2, maxX2, minY2, maxY2;

        origin1 = clearLine1.getOrigin();
        endPoint1 = clearLine1.getEndPoint();
        origin2 = clearLine2.getOrigin();
        endPoint2 = clearLine2.getEndPoint();

        minX1 = Math.min(origin1.getX(), origin2.getX());
        maxX1 = Math.max(origin1.getX(), origin2.getX());
        minY1 = Math.min(origin1.getY(), origin2.getY());
        maxY1 = Math.max(origin1.getY(), origin2.getY());
        minX2 = Math.min(endPoint1.getX(), endPoint2.getX());
        maxX2 = Math.max(endPoint1.getX(), endPoint2.getX());
        minY2 = Math.min(endPoint1.getY(), endPoint2.getY());
        maxY2 = Math.max(endPoint1.getY(), endPoint2.getY());

        x1 = ratio * (maxX1 - minX1);
        y1 = ratio * (maxY1 - minY1);
        x2 = ratio * (maxX2 - minX2);
        y2 = ratio * (maxY2 - minY2);

        Point2D p1 = new Point2D(
                origin1.getX() == maxX1 ? maxX1 - x1 : minX1 + x1,
                origin1.getY() == maxY1 ? maxY1 - y1 : minY1 + y1);
        Point2D p2 = new Point2D(
                endPoint1.getX() == maxX2 ? maxX2 - x2 : minX2 + x2,
                endPoint1.getY() == maxY2 ? maxY2 - y2 : minY2 + y2);
        l1 = new Line2D(p1, p2);


        p1 = new Point2D(
                origin2.getX() == maxX1 ? maxX1 - x1 : minX1 + x1,
                origin2.getY() == maxY1 ? maxY1 - y1 : minY1 + y1);
        p2 = new Point2D(
                endPoint2.getX() == maxX2 ? maxX2 - x2 : minX2 + x2,
                endPoint2.getY() == maxY2 ? maxY2 - y2 : minY2 + y2);
        l2 = new Line2D(p1, p2);

        l1 = Util.clipLine(l1, Util.lineLength(l1) - scenarioInfo.getClearRepairRad() / 2 + 100, true);
        l2 = Util.clipLine(l2, Util.lineLength(l2) - scenarioInfo.getClearRepairRad() / 2 + 100, true);
        return new Pair<Line2D, Line2D>(l1, l2);
    }


    private MrlRoad getMrlRoad(EntityID roadId) {
        return worldHelper.getMrlRoad(roadId);
    }
}
