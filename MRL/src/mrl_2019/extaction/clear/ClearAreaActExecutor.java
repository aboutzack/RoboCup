package mrl_2019.extaction.clear;


import adf.agent.action.Action;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.PathPlanning;
import mrl_2019.MRLConstants;
import mrl_2019.util.Util;
import mrl_2019.viewer.MrlPersonalData;
import mrl_2019.world.MrlWorldHelper;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * @author Mahdi
 */
public class ClearAreaActExecutor extends ClearActExecutor {

    private double clearRange;
    private static final int SECURE_RANGE = 1000;
    private Line2D lastClearLine;
    private boolean wasOnBlockade = false;
    private boolean clearBlockadesOnWay = false;
//    private List<EntityID> lastPath;

    public ClearAreaActExecutor(MrlWorldHelper worldHelper, WorldInfo worldInfo, AgentInfo agentInfo, ScenarioInfo scenarioInfo, PathPlanning pathPlanning) {
        super(worldHelper, worldInfo, agentInfo, scenarioInfo,pathPlanning);

        clearRange = scenarioInfo.getClearRepairDistance();
    }

    @Override
    public Action clearWay(List<EntityID> path, EntityID targetID) {
        Line2D clearLine;
        //Debug PF loop when reach to building entrance and no path was returned.
        StandardEntity targetEntity = worldInfo.getEntity(targetID);
        if ((targetEntity instanceof Area) && path.size() <= 1 && !path.contains(targetID)) {
            List<EntityID> newPath = pathPlanning.setFrom(getSelfPosition().getID()).setDestination(targetEntity.getID()).calc().getResult();
//            List<EntityID> newPath = world.getPlatoonAgent().getPathPlanner().planMove(
//                    (Area) getSelfPosition(),
//                    (Area) targetEntity, MRLConstants.IN_TARGET, true
//            );
            if (path.size() < newPath.size()) {
                path = newPath;
            }
        }

        GuideLine guideLine = getTargetGuideline(path, clearRange, targetID);
        MrlPersonalData.VIEWER_DATA.setPFGuideline(agentInfo.getID(), guideLine);

        List<Area> areasSeenInPath = getAreasSeenInPath(path);
        if (guideLine != null) {
            Action action = moveToGuideLine(areasSeenInPath, guideLine);
            if (action != null) {
                return action;
            }
//            if (positionActState.equals(PositionActState.KEEP_MOVING)) {
//                return ActResult.SUCCESSFULLY_COMPLETE;
//            }
        }

        clearLine = getTargetClearLine(areasSeenInPath, clearRange, guideLine);

        Pair<Line2D, Line2D> clearSecureLines = null;
        Point2D agentPosition = Util.getPoint(getSelfLocation());
        if (clearLine != null) {
            Pair<Line2D, Line2D> clearLengthLines = Util.clearLengthLines(Util.clearAreaRectangle(agentPosition, clearLine.getEndPoint(), getClearRadius()), getClearRadius());

            double distance = (getClearRadius() - MRLConstants.AGENT_SIZE * 0.5) / 2;
            clearSecureLines = clearTools.getClearSecureLines(clearLengthLines.first(), clearLengthLines.second(), getClearRadius(), distance);
            MrlPersonalData.VIEWER_DATA.setPFClearAreaLines(agentInfo.getID(), clearLine, clearSecureLines.first(), clearSecureLines.second());
        }

        List<EntityID> thisRoadPath = new ArrayList<EntityID>();
        thisRoadPath.add(getSelfPosition().getID());

        if (clearLine != null &&
                (anyBlockadeIntersection(areasSeenInPath, clearLine, true) || anyBlockadeIntersection(areasSeenInPath, clearSecureLines.first(), true) || anyBlockadeIntersection(areasSeenInPath, clearSecureLines.second(), true))) {
            lastClearLine = clearLine;
            wasOnBlockade = clearTools.isOnBlockade(clearLine.getEndPoint(), areasSeenInPath);
            return sendClearAct(agentInfo.getTime(), (int) clearLine.getEndPoint().getX(), (int) clearLine.getEndPoint().getY());
        } else {
            if (wasOnBlockade && lastClearLine != null) {//move to point to the end of lastClearLine if was on the blockade!
                int x = (int) lastClearLine.getEndPoint().getX();
                int y = (int) lastClearLine.getEndPoint().getY();
                lastClearLine = null;
                return moveAction(agentInfo.getTime(), thisRoadPath, x, y);
            } else if (clearBlockadesOnWay && guideLine != null) {
                //looking for blockades on way....
                Point2D nearestIntersect = anyBlockadeIntersection(guideLine);
                if (nearestIntersect != null) {
                    int dist = Util.distance(agentPosition, nearestIntersect);
                    clearLine = new Line2D(agentPosition, nearestIntersect);
                    clearLine = Util.clipLine(clearLine, clearRange - getClearRadius() - SECURE_RANGE);
                    lastClearLine = clearLine;
                    wasOnBlockade = clearTools.isOnBlockade(clearLine.getEndPoint(), areasSeenInPath);
                    if (dist < clearRange - getClearRadius() - SECURE_RANGE) {
//                        world.printData("I found blockades which intersects with guideline and near me!!!!!");
                        return sendClearAct(agentInfo.getTime(), (int) clearLine.getEndPoint().getX(), (int) clearLine.getEndPoint().getY());
                    } else {
//                        world.printData("Move to point to clear blockades in way....");
                        return moveAction(agentInfo.getTime(), thisRoadPath, (int) nearestIntersect.getX(), (int) nearestIntersect.getY());
                    }
                }
            }

            lastClearLine = null;
        }

        return null;
    }

    @Override
    public Action clearAroundTarget(Pair<Integer, Integer> targetLocation) {
        throw new UnsupportedOperationException();
    }

    private Action moveToGuideLine(List<Area> areasSeenInPath, GuideLine guideLine) {

        int distanceThreshold = getClearRadius() / 3;
        Point2D betterPosition = getBetterPosition(guideLine, distanceThreshold);
        Point2D agentPosition = Util.getPoint(getSelfLocation());
        if (betterPosition == null) {
            return null;
        }


        Line2D line2D = new Line2D(agentPosition, betterPosition);

        Pair<Line2D, Line2D> clearLengthLines = Util.clearLengthLines(Util.clearAreaRectangle(agentPosition, line2D.getEndPoint(), getClearRadius()), getClearRadius());
        double distance = (getClearRadius() - MRLConstants.AGENT_SIZE * 0.5);
        Pair<Line2D, Line2D> clearSecureLines = clearTools.getClearSecureLines(clearLengthLines.first(), clearLengthLines.second(), getClearRadius(), distance);
        boolean shouldClear = anyBlockadeIntersection(areasSeenInPath, line2D, false) ||
                anyBlockadeIntersection(areasSeenInPath, clearSecureLines.first(), false) ||
                anyBlockadeIntersection(areasSeenInPath, clearSecureLines.second(), false);

        if (Util.lineLength(line2D) > clearRange) {
//            MrlPersonalData.VIEWER_DATA.print("I'm too far from guideline.");

            //todo should implement
            return clearToPoint(agentPosition, line2D.getEndPoint());
        }
        if (shouldClear) {
            Line2D clearLine = Util.improveLine(line2D, getClearRadius());
            return sendClearAct(agentInfo.getTime(), (int) clearLine.getEndPoint().getX(), (int) clearLine.getEndPoint().getY());
        }

        if (!isNeedToClear(agentPosition, areasSeenInPath, getClearLine(agentPosition, guideLine, clearRange))) {
            return null;
        }


        List<EntityID> path = new ArrayList<EntityID>(1);
        path.add(getSelfPosition().getID());
        return moveAction(agentInfo.getTime(), path, (int) line2D.getEndPoint().getX(), (int) line2D.getEndPoint().getY());
//        return PositionActState.MOVE_TO_GUIDELINE;
    }

    private Action clearToPoint(Point2D agentPosition, Point2D point) {
        double distance = Util.distance(agentPosition, point);
        Line2D clearLine = null, targetClearLine;

        distance = Math.min(distance, clearRange);
        distance -= SECURE_RANGE;
        clearLine = Util.clipLine(new Line2D(agentPosition, point), distance);
        targetClearLine = Util.clipLine(clearLine, distance - getClearRadius());

        List<Area> areasSeenInPath = new ArrayList<>();

        Area selfPosition = getSelfPosition();
        if (selfPosition.getShape().contains(point.getX(), point.getY())) {
            areasSeenInPath.add(selfPosition);
        } else {
            areasSeenInPath.addAll(getRoadsSeen());
        }
        if (anyBlockadeIntersection(areasSeenInPath, targetClearLine, true)) {
            Point2D endPoint = targetClearLine.getEndPoint();
            return sendClearAct(agentInfo.getTime(), (int) endPoint.getX(), (int) endPoint.getY());
        } else {
            ArrayList<EntityID> path = new ArrayList<>();
            path.add(getSelfPosition().getID());
            return moveAction(agentInfo.getTime(), path, (int) point.getX(), (int) point.getY());
        }

    }

    private Point2D getBetterPosition(Line2D guideline, double distanceThreshold) {
        Point2D agentLocation = Util.getPoint(getSelfLocation());
        Point2D betterPosition = null;
        Point2D pointOnGuideline = Util.closestPoint(guideline, agentLocation);

        Area selfPosition = (Area) getSelfPosition();
        if (!selfPosition.getShape().contains(pointOnGuideline.getX(), pointOnGuideline.getY())) {
            List<Point> pointList = Util.getPointList(selfPosition.getApexList());
            Line2D line;
            Point2D p1, p2, nearestPoint = null;
            Point point;
            int minDistance = Integer.MAX_VALUE;
            for (int i = 0; i < pointList.size(); i++) {
                point = pointList.get(i);
                p1 = new Point2D(point.getX(), point.getY());
                point = pointList.get((i + 1) % pointList.size());
                p2 = new Point2D(point.getX(), point.getY());
                line = new Line2D(p1, p2);
                Point2D intersection = Util.getIntersection(line, guideline);
                int distance = Util.distance(agentLocation, intersection);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestPoint = intersection;
                }
            }

            betterPosition = nearestPoint;
        } else {
            betterPosition = pointOnGuideline;
        }

        if (betterPosition == null || Util.distance(betterPosition, agentLocation) < distanceThreshold) {
            //it means guideline is too close to me. so no need to move on it
            return null;
        }

        return betterPosition;
    }

    private GuidelineProvider guidelineProvider = new GuidelineProvider(worldHelper, worldInfo, agentInfo, scenarioInfo, clearRange);

    private GuideLine getTargetGuideline(List<EntityID> path, double range, EntityID targetID) {
        return guidelineProvider.findTargetGuideline(path, range, targetID);
    }

    private Line2D getTargetClearLine(List<Area> areasSeenInPath, double range, GuideLine guideLine) {
        Point2D agentPosition = Util.getPoint(getSelfLocation());

        if (!isNeedToClear(agentPosition, areasSeenInPath, guideLine)) {
            return null;
        }
        return getClearLine(agentPosition, guideLine, range);
    }

    private boolean isNeedToClear(Point2D agentLocation, List<Area> areasSeenInPath, Line2D guideLine) {

        if (guideLine == null) {
            return false;
        }

        if (Util.distance(guideLine, agentLocation) > MRLConstants.AGENT_SIZE) {
            return true;
        }
//        List<Area> areasSeenInPath = getAreasSeenInPath(path);
        for (Area area : areasSeenInPath) {
            if (area.isBlockadesDefined()) {
                for (EntityID blockID : area.getBlockades()) {
                    Blockade blockade = (Blockade) worldInfo.getEntity(blockID);
                    if (blockade != null) {
                        Polygon blockadePoly = Util.getPolygon(blockade.getApexes());
                        if (!Util.isPassable(blockadePoly, guideLine, MRLConstants.AGENT_PASSING_THRESHOLD)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Line2D getClearLine(Point2D agentPosition, GuideLine guideLine, double range) {
        Line2D targetLine = new Line2D(agentPosition, guideLine.getDirection());

        ////////////////////////////////////////////////////////////////////
        //rotate target line for containing traffic simulator move points //
//        Polygon clearRectangle = Util.clearAreaRectangle(targetLine.getOrigin(), targetLine.getEndPoint(), getClearRadius());
        ////////////////////////////////////////////////////////////////////


        return Util.clipLine(targetLine, range - SECURE_RANGE);
    }


    /**
     * @param path
     * @return
     */
    private List<Area> getAreasSeenInPath(List<EntityID> path) {
        List<Area> areasSeenInPath = new ArrayList<Area>();
        Area area;
        for (EntityID id : path) {
            area = (Area) worldInfo.getEntity(id);
            if (worldInfo.getChanged().getChangedEntities().contains(id)) {
                areasSeenInPath.add(area);
            } else {
                break;
            }
        }
        return areasSeenInPath;
    }


    private Point2D getTargetClearPoint(List<EntityID> path, double range) {
        if (path == null || range < 0) {
            return null;
        }
//        final double minimumRangeThreshold = range * clearRangeYieldCoefficient;

        Area area;
        Point2D targetPoint = null;
        Point2D positionPoint = Util.getPoint(getSelfLocation());
        if (path.size() <= 1) {
            area = (Area) worldInfo.getEntity(getSelfPosition().getID());
            Point2D areaCenterPoint = Util.getPoint(worldInfo.getLocation(area));
            targetPoint = Util.clipLine(new Line2D(positionPoint, areaCenterPoint), range).getEndPoint();
        } else if (path.size() > 1) {
            area = (Area) worldInfo.getEntity(path.get(0));
            Edge edge = area.getEdgeTo(path.get(1));
            if (edge == null) {
                return null;
            }
            Point2D areaCenterPoint = Util.getPoint(worldInfo.getLocation(area));
            Point2D edgeCenterPoint = Util.getMiddle(edge.getLine());
            Point2D targetPoint2D = Util.clipLine(new Line2D(areaCenterPoint, edgeCenterPoint), range).getEndPoint();
//            targetPoint = Util.clipLine(new Line2D(positionPoint, edgeCenterPoint), range).getEndPoint();
            //guideline is the line from agent location area center toward edge to next area
            //to avoid bad shape clearing, guideline help pfs to clear in one direction in path
            double deltaX = positionPoint.getX() - areaCenterPoint.getX();
            double deltaY = positionPoint.getY() - areaCenterPoint.getY();
//            if(Util.contains())
            targetPoint2D = new Point2D(targetPoint2D.getX() + deltaX, targetPoint2D.getY() + deltaY);//rotate line to set it as parallel of guideline
            Polygon clearPoly = Util.clearAreaRectangle(positionPoint, targetPoint2D, getClearRadius());
            if (clearPoly.contains(edgeCenterPoint.getX(), edgeCenterPoint.getY())) {
                targetPoint = Util.clipLine(new Line2D(positionPoint, targetPoint2D), range).getEndPoint();
            } else {
                targetPoint = Util.clipLine(new Line2D(positionPoint, edgeCenterPoint), range).getEndPoint();
            }
        }

        List<Area> areasSeenInPath = getAreasSeenInPath(path);
        //target point is point that agent want to clear up to it.
//        List<EntityID> checkedAreas = new ArrayList<EntityID>();
//        Polygon polygon = null;
        if (targetPoint != null) {
            Line2D targetLine = new Line2D(positionPoint, targetPoint);
//            polygon = Util.clearAreaRectangle(positionPoint.getX(), positionPoint.getY(), targetPoint.getX(), targetPoint.getY(), getClearRadius());
//            Area neighbour;
//            cleaningBefore = false;
//            for (Road road : roadsSeenInPath) {//3 loop
//                for (EntityID id : road.getNeighboursByEdge()) {
//                    if (checkedAreas.contains(id) || path.contains(id)) {
//                        //this area checked before...
//                        continue;
//                    }
//                    checkedAreas.add(id);
//                    neighbour = worldInfo.getEntity(id, Area.class);
//                    if (!(neighbour instanceof Road)) {
//                        //this area is not road! so no blockades is in it.
//                        continue;
//                    }
//                    targetLine = normalizeClearLine(neighbour, targetLine, polygon, minimumRangeThreshold);
//                    if (targetLine == null) {
//                        return null;
//                    }
//                }
//            }
            Pair<Line2D, Line2D> clearLengthLines = Util.clearLengthLines(Util.clearAreaRectangle(Util.getPoint(getSelfLocation()), targetLine.getEndPoint(), getClearRadius()), getClearRadius());
            double distance = (getClearRadius() - MRLConstants.AGENT_SIZE * 0.5) / 2;

            Pair<Line2D, Line2D> clearSecureLines = clearTools.getClearSecureLines(clearLengthLines.first(), clearLengthLines.second(), getClearRadius(), distance);
            MrlPersonalData.VIEWER_DATA.setPFClearAreaLines(agentInfo.getID(), targetLine, clearSecureLines.first(), clearSecureLines.second());
            if (anyBlockadeIntersection(areasSeenInPath, targetLine, true) ||
                    anyBlockadeIntersection(areasSeenInPath, clearSecureLines.first(), false) ||
                    anyBlockadeIntersection(areasSeenInPath, clearSecureLines.second(), false)) {
                return targetLine.getEndPoint();
            } else {
//                return beforeClearPoint(targetLine);
            }
        }
        return null;
    }


    private int getClearRadius() {

        return scenarioInfo.getClearRepairRad();
    }


    private Point2D anyBlockadeIntersection(GuideLine guideLine) {
        List<Area> areas = new ArrayList<Area>();
        for (StandardEntity entity : getEntities(guideLine.getAreas())) {
            areas.add((Area) entity);
        }
        Point2D nearestPoint = null;
        Point2D agentLocation = Util.getPoint(getSelfLocation());
        int minDist = Integer.MAX_VALUE;
        for (Area area : areas) {

            List<Point2D> intersects = clearTools.blockadesIntersections(area, guideLine);
            for (Point2D point2D : intersects) {
                int dist = Util.distance(point2D, agentLocation);
                if (dist < minDist) {
                    minDist = dist;
                    nearestPoint = point2D;
                }
            }
        }
        return nearestPoint;
    }


    private boolean anyBlockadeIntersection(Collection<Area> areasSeenInPath, Line2D targetLine, boolean secure) {
        Line2D line;
        if (secure) {
            double length = Util.lineLength(targetLine);
            double secureSize = 510 + SECURE_RANGE;
            if (length - secureSize <= 0) {
                printData("The clear line is too short.....");
                return false;
            }
            line = Util.improveLine(targetLine, -secureSize);
        } else {
            line = targetLine;
        }
        for (Area area : areasSeenInPath) {
            if (clearTools.anyBlockadeIntersection(area, line)) {
                return true;
            }
        }
        return false;
    }


}
