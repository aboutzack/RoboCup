package mrl_2019.extaction.clear;


import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import mrl_2019.util.Util;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.entity.MrlRoad;
import mrl_2019.world.entity.Path;
import mrl_2019.world.helper.RoadHelper;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Mahdi
 */
public class GuidelineProvider {

    protected WorldInfo worldInfo;
    protected MrlWorldHelper worldHelper;
    protected AgentInfo agentInfo;
    protected ScenarioInfo scenarioInfo;
    private ClearTools clearTools;
    private double clearRange;
    private GuideLine lastGuideline;
    private EntityID lastTarget;
    private List<EntityID> lastPlan;

    public GuidelineProvider(MrlWorldHelper worldHelper, WorldInfo worldInfo, AgentInfo agentInfo, ScenarioInfo scenarioInfo, double clearRange) {
        this.worldHelper = worldHelper;
        this.worldInfo = worldInfo;
        this.agentInfo = agentInfo;
        this.scenarioInfo = scenarioInfo;
        this.clearTools = new ClearTools(worldHelper, worldInfo, agentInfo, scenarioInfo);
        this.clearRange = clearRange;
        this.lastPlan = new ArrayList<>();

    }

    public GuideLine findTargetGuideline(List<EntityID> movePlan, double range, EntityID targetID) {
        if (movePlan == null || range <= 0 || targetID == null) {
            lastGuideline = null;
            lastPlan = movePlan;
            lastTarget = targetID;
            return null;
        }

        GuideLine guideLine;
        //new guideline method
//        if(lastGuideline!=null && lastGuideline.isMinor() && lastGuideline.getAreas().contains(world.getSelfPosition().getID())){
//            //for minor guidelines which connects Major guideline to entrance
//            guideLine = lastGuideline;
//        }else {
        guideLine = findByPath(movePlan);
//        }

//        if (guideLine != null) world.printData("GUIDELINE : new = " + guideLine);

        if (guideLine != null && isNeedToInvert(guideLine, movePlan)) {
//            guideLine = new GuideLine(guideLine.getEndPoint(), guideLine.getOrigin());
            Point2D origin = guideLine.getOrigin();
            Point2D end = guideLine.getEndPoint();
            guideLine.setEnd(origin);
            guideLine.setOrigin(end);

        }

        if (guideLine == null) {
            guideLine = findByMovePlan(movePlan, range, targetID);
        }


        lastGuideline = guideLine;
        lastPlan = movePlan;
        lastTarget = targetID;
        return guideLine;
    }

    private boolean isNeedToInvert(GuideLine guideLine, List<EntityID> movePlan) {
        if (guideLine == null || movePlan.size() <= 1 || guideLine.isMinor()) {
            return false;
        }
        Area currentPosition = (Area) worldInfo.getEntity(movePlan.get(0));
        Area nextPosition = (Area) worldInfo.getEntity(movePlan.get(1));
//        Util.getMiddle(currentPosition.getEdgeTo(nextPosition.getID()).getLine());
        Line2D ln = new Line2D(
//                Util.getPoint(currentPosition.getLocation(world)),
                Util.getPoint(worldInfo.getLocation(currentPosition)),
                Util.getMiddle(currentPosition.getEdgeTo(nextPosition.getID()).getLine())
        );
        double angle = Util.angleBetween2Lines(ln, guideLine);

//        if(angle>60 && angle <120){
//            world.printData("#######Suspicious GUIDELINE ANGLE is " + angle);
//        }

        if (angle <= 90 && angle >= -90) {
            return false;
        } else {
            return true;
        }


    }


    private GuideLine findByPath(List<EntityID> movePlan) {
        GuideLine guideLine = null;

        if (movePlan.size() >= 2) {
            EntityID currentPosition = movePlan.get(0);
            EntityID nextPosition = movePlan.get(1);

            Path path = null;
            MrlRoad mrlRoad = worldHelper.getMrlRoad(currentPosition);
            if (mrlRoad != null) {
                //new guideline method

                StandardEntity nextEntity = worldInfo.getEntity(nextPosition);
                if (nextEntity instanceof Road) {
                    for (Path p : mrlRoad.getPaths()) {
                        if (p.contains(nextEntity)) {
                            path = p;
                            break;
                        }
                    }
                }


                if (path != null) {
                    for (GuideLine gl : path.getGuideLines()) {
                        if (gl.getAreas().contains(mrlRoad.getID())) {
                            guideLine = gl;
                            break;
                        }
                    }
                    if (guideLine != null && !path.getHeadToEndRoads().contains(nextPosition)) {
                        Edge edge = mrlRoad.getParent().getEdgeTo(nextPosition);
                        Point2D middle = Util.getMiddle(edge.getLine());
                        Point2D closestPoint = Util.closestPoint(guideLine, middle);
                        guideLine = new GuideLine(closestPoint, middle);
                        List<EntityID> areas = new ArrayList<>();
                        areas.add(currentPosition);
//                        areas.add(nextPosition);
                        guideLine.setAreas(areas);
                        guideLine.setMinor(true);
                        printData("GUIDELINE TO ENTRANCE....");

                    }
                }
            }
        }


        return guideLine;
    }


    /**
     * Old method for find guideline by move plan
     *
     * @param path     move plan
     * @param range    clear range
     * @param targetID target
     * @return guideline
     */
    private GuideLine findByMovePlan(List<EntityID> path, double range, EntityID targetID) {
        if (path == null || range <= 0 || targetID == null) {
            return null;
        }


        Point2D agentPosition = Util.getPoint(getSelfLocation());
        StandardEntity target = worldInfo.getEntity(targetID);

        GuideLine guideLine = null;
        if (path.size() == 1) {

            if (target instanceof Building) {
                Set<Edge> edges = RoadHelper.getEdgesBetween((Area) worldInfo.getEntity(path.get(0)), (Area) target);
                if (edges != null && !edges.isEmpty()) {
                    Edge edge = edges.iterator().next();
                    int middleX = (edge.getStartX() + edge.getEndX()) / 2;
                    int middleY = (edge.getStartY() + edge.getEndY()) / 2;

                    Point2D targetPoint = new Point2D(middleX, middleY);
                    List<GuideLine> pathGuidelines = clearTools.getPathGuidelines(path, targetPoint);
                    if (pathGuidelines.isEmpty()) {
                        guideLine = new GuideLine(agentPosition, targetPoint);
                    } else {
                        guideLine = clearTools.getTargetGuideLine(pathGuidelines, path, targetID, clearRange);
                    }
                }
            }
        } else if (!targetID.equals(lastTarget) ||
                lastGuideline == null ||
                !lastGuideline.getAreas().contains(getSelfPosition().getID()) ||
                (lastPlan == null || !lastPlan.containsAll(path))) {
            Point2D targetPoint = Util.getPoint(worldInfo.getLocation(target));
            List<GuideLine> pathGuidelines = clearTools.getPathGuidelines(path, targetPoint);
            if (pathGuidelines.isEmpty()) {
                guideLine = new GuideLine(agentPosition, targetPoint);
            } else {
                guideLine = clearTools.getTargetGuideLine(pathGuidelines, path, targetID, clearRange);

            }
        } else {
            guideLine = lastGuideline;
        }


        return guideLine;
    }

    private Pair<Integer, Integer> getSelfLocation() {
        return worldInfo.getLocation(agentInfo.getID());
    }

    private Area getSelfPosition() {
        return agentInfo.getPositionArea();
    }

    private void printData(String s) {
        System.out.println(s);
    }


}
