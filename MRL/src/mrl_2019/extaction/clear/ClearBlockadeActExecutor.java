package mrl_2019.extaction.clear;

import adf.agent.action.Action;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.PathPlanning;
import javolution.util.FastSet;

import mrl_2019.MRLConstants;
import mrl_2019.util.Util;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.entity.MrlBlockade;
import mrl_2019.world.entity.MrlEdge;
import mrl_2019.world.entity.MrlRoad;
import mrl_2019.world.helper.RoadHelper;
import rescuecore2.log.Logger;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * @author Mahdi
 */
public class ClearBlockadeActExecutor extends ClearActExecutor {

    private int maxClearDistance;
    Set<Blockade> blockadesToClear;
    private Blockade previousBlockade;
    private Blockade blockadeToMove;
    private int tryCount;

    public ClearBlockadeActExecutor(MrlWorldHelper worldHelper, WorldInfo worldInfo, AgentInfo agentInfo, ScenarioInfo scenarioInfo, PathPlanning pathPlanning) {
        super(worldHelper, worldInfo, agentInfo, scenarioInfo, pathPlanning);
        this.maxClearDistance = scenarioInfo.getClearRepairDistance();
        this.tryCount = 0;
    }

    @Override
    public Action clearWay(List<EntityID> pathToGo, EntityID targetID) {
        Blockade targetBlockade = getTargetBlockade(pathToGo, targetID);
        previousBlockade = targetBlockade;
        if (targetBlockade != null) {
            if (clearTools.isInClearRange(targetBlockade)) {
                Logger.info("Clearing blockade " + targetBlockade);
//                getMrlRoad(targetBlockade.getPosition()).setNeedUpdate(true);
                return sendClearAct(agentInfo.getTime(), targetBlockade.getID());
            } else {
                if (blockadeToMove != null && blockadeToMove.equals(previousBlockade)) {
                    tryCount++;
                } else {
                    tryCount = 0;
                }
                blockadeToMove = previousBlockade;

                if (targetBlockade.getPosition().equals(agentInfo.getPosition())) {
                    return moveToPoint(targetBlockade.getPosition(), targetBlockade.getX(), targetBlockade.getY());
                } else if (pathToGo == null || pathToGo.isEmpty()) {
                    return moveAction((Area) worldInfo.getEntity(targetBlockade.getPosition()), true);
                } else {
                    return moveAction(pathToGo);

                }

            }
        }

        return null;
    }


    @Override
    public Action clearAroundTarget(Pair<Integer, Integer> targetLocation) {
        Set<Blockade> blockadesAround = clearTools.getBlockadesInRange(targetLocation, getBlockadeSeen(), 1000);
        if (!blockadesAround.isEmpty()) {
            for (Blockade blockade : blockadesAround) {
                if (clearTools.isInClearRange(blockade)) {
                    return sendClearAct(agentInfo.getTime(), blockade.getID());
                }
            }
        }
        return null;
    }

    private Blockade getTargetBlockade(List<EntityID> pathToGo, EntityID target) {

        Blockade blockadeToClear = null;

        // Find first blockade that is in range.
        if (blockadesToClear == null) {
            blockadesToClear = new FastSet<Blockade>();
        } else if (!blockadesToClear.isEmpty()) {
            Set<Blockade> tempSet;// = new FastSet<Blockade>();

            tempSet = refreshFoundBlockades(pathToGo, blockadesToClear);
            blockadesToClear.clear();
            blockadesToClear.addAll(tempSet);
        }


        if (!blockadesToClear.isEmpty()) {
            if (previousBlockade != null) {
                Road road = (Road) worldInfo.getEntity(previousBlockade.getPosition());
                if (!road.isBlockadesDefined() || road.getBlockades().contains(previousBlockade.getID())) {
                    previousBlockade = null;
                } else {
                    blockadesToClear.remove(previousBlockade);
                }
            }
        }


        if (blockadesToClear.isEmpty()) {
            if (pathToGo == null || pathToGo.isEmpty()) {
                //TODO: @Pooya check bellow line and change it with better one
//                blockadesToClear.addAll(getBlockadesOnWay_ImportanceBased(world.getPlatoonAgent().getPathPlanner().getNextPlan(), getRoadsSeen()));
//                blockadesToClear.addAll(findBlockadesInRange(getSelfLocation(), maxClearDistance));
                if (target != null && agentInfo.getPositionArea() instanceof Road) {
                    blockadesToClear.addAll(clearTools.getTargetRoadBlockades((Road) agentInfo.getPositionArea()));
                }
                if (blockadesToClear.isEmpty()) {
                    //TODO: @Pooya check bellow line and check if it needs to be changed
//                    blockadeToClear = getTargetBlockade(pathToGo, target);

                    //do nothing
                } else {
                    Pair<Blockade, Integer> nearestBlockadePair = clearTools.findNearestBlockade(blockadesToClear);
                    if (nearestBlockadePair != null) {
                        blockadeToClear = nearestBlockadePair.first();
                    }
                }
            } else {
                //TODO: @Pooya check bellow line and change it with better one(the one uses target based clearing(findBlockadesOnWay()))
//                blockadesToClear.addAll(getBlockadesOnWay_ImportanceBased(pathToGo, getRoadsSeen()));
                if (target == null) {
                    blockadesToClear.addAll(findBlockadesInRange(getSelfLocation(), 1000));
                } else {
                    blockadesToClear.addAll(findBlockadesOnWay(pathToGo, getRoadsSeen(), target));
                }
                //add blockades around too small edges.
                MrlRoad mrlRoad;
                Pair<Integer, Integer> edgeMiddle;
                for (int i = 0; i < pathToGo.size() - 1; i++) {
                    mrlRoad = getMrlRoad(pathToGo.get(i));
                    if (mrlRoad == null || !worldInfo.getChanged().getChangedEntities().contains(pathToGo.get(i))) {
                        continue;
                    }
                    for (MrlEdge mrlEdge : mrlRoad.getMrlEdgesTo(pathToGo.get(i + 1))) {
                        if (mrlEdge.isTooSmall() /*&& mrlEdge.isBlocked() && world.getChanges().contains(pathToGo.get(i + 1))*/) {
                            edgeMiddle = new Pair<Integer, Integer>((int) mrlEdge.getMiddle().getX(), (int) mrlEdge.getMiddle().getY());
                            Set<Blockade> blockades = clearTools.getBlockadesInRange(edgeMiddle, getBlockadeSeen(), MRLConstants.AGENT_SIZE);
//                            world.printData(blockades.size() + " blockade(s) added into blockades To Clear. these are around too small edge of " + mrlRoad.getParent());
                            blockadesToClear.addAll(blockades);
                        }
                    }
                }

                Pair<Blockade, Integer> nearestBlockadePair = clearTools.findNearestBlockade(blockadesToClear);
                if (nearestBlockadePair != null) {
                    blockadeToClear = nearestBlockadePair.first();
                }

            }

        } else {

            if (pathToGo == null || pathToGo.isEmpty()) {
                blockadesToClear.addAll(findBlockadesInRange(getSelfLocation(), 1000));
            } else {
                blockadesToClear.addAll(findBlockadesOnWay(pathToGo, getRoadsSeen(), target));
            }

            Pair<Blockade, Integer> nearestBlockadePair = clearTools.findNearestBlockade(blockadesToClear);

            if (nearestBlockadePair != null) {
                blockadeToClear = nearestBlockadePair.first();
            }
        }

        return blockadeToClear;
    }


    private Set<Blockade> refreshFoundBlockades(List<EntityID> pathToGo, Set<Blockade> blockadesToClear) {

        Set<Blockade> blockadesToRemove = new FastSet<Blockade>();
        Set<Blockade> blockades = new FastSet<Blockade>(blockadesToClear);
        Road road;
        for (Blockade blockade : blockades) {
            road = (Road) worldInfo.getEntity(blockade.getPosition());
            //if this blockade is not in a road that I can see it
            if (!getRoadsSeen().contains(road) || (road.isBlockadesDefined() && !road.getBlockades().contains(blockade.getID())) || (pathToGo != null && !pathToGo.contains(road.getID()))) {
                blockadesToRemove.add(blockade);
            }
        }

        blockades.removeAll(blockadesToRemove);
        return blockades;

    }

    public Set<Blockade> findBlockadesOnWay(List<EntityID> pathToGo, Set<Road> roadsSeen, EntityID target) {

        Set<MrlBlockade> obstacles_MrlBlockades = new HashSet<MrlBlockade>();
        Set<Blockade> blockades = new FastSet<Blockade>();


        MrlEdge sourceEdge = null;
        Pair<Integer, Integer> nextMiddlePoint;
        Pair<Integer, Integer> sourceMiddlePoint;


        //find entityID sequence of which can be seen
        List<EntityID> seenPath = new ArrayList<EntityID>();
        int count = 0;
        for (EntityID entityID : pathToGo) {
            if (worldInfo.getEntity(entityID) instanceof Road) {
                if (!roadsSeen.contains(worldInfo.getEntity(entityID))) {
                    if (pathToGo.size() > count) {
                        seenPath.add(pathToGo.get(count));
                    }
                    break;
                }
            }
            count++;
            seenPath.add(entityID);
        }

        if (seenPath.isEmpty()) {
            //TODO: @Pooya What should be done?
        } else if (seenPath.size() == 1) {
            if (worldInfo.getEntity(seenPath.get(0)) instanceof Road) {
                if (target != null) {
                    blockades = clearTools.getTargetRoadBlockades((Road) worldInfo.getEntity(seenPath.get(0)));
                } else {//if there is no target

                    //clear blockades in range
                    blockades = findBlockadesInRange(getSelfLocation(), 500 * 2);
                }
            }
        } else {// if seenPath contains more than one entity
            int j;
            EntityID sourceAreaID;
            EntityID nextAreaID;
            Area sourceArea;
            Area nextArea = null;
            Set<Edge> edgeSet;
            MrlRoad mrlRoad;
            MrlEdge nextEdge = null;
            List<EntityID> neighbours;
            for (int i = 0; i < seenPath.size() - 1; i++) {
                j = i + 1;
                sourceAreaID = seenPath.get(i);
                nextAreaID = seenPath.get(j);
                sourceArea = (Area) worldInfo.getEntity(sourceAreaID);
                nextArea = (Area) worldInfo.getEntity(nextAreaID);
                if (sourceArea instanceof Road) {
                    mrlRoad = getMrlRoad(sourceAreaID);
//                    neighbours = sourceArea.getNeighboursByEdge();
//                    for (EntityID neighbourID : neighbours) {
//                        // the neighbour of this area is also in my way, so it should be cleared
//                        if (pathToGo.contains(neighbourID) && pathToGo.indexOf(neighbourID)>pathToGo.indexOf(sourceAreaID)) {
                    edgeSet = RoadHelper.getEdgesBetween(sourceArea, nextArea);
                    for (Edge edge : edgeSet) {
                        nextEdge = mrlRoad.getMrlEdge(edge);
                        nextMiddlePoint = new Pair<Integer, Integer>((int) nextEdge.getMiddle().getX(), (int) nextEdge.getMiddle().getY());
                        if (sourceEdge == null) {

                            if (agentInfo.getPositionArea().equals(sourceArea)) {
                                obstacles_MrlBlockades.addAll(clearTools.getRoadObstacles(mrlRoad, getSelfLocation(), nextMiddlePoint));
                            } else {
                                obstacles_MrlBlockades.addAll(clearTools.getRoadObstacles(mrlRoad, worldInfo.getLocation(sourceArea), nextMiddlePoint));
                            }
                        } else {
                            sourceMiddlePoint = new Pair<Integer, Integer>((int) sourceEdge.getMiddle().getX(), (int) sourceEdge.getMiddle().getY());
                            obstacles_MrlBlockades.addAll(clearTools.getRoadObstacles(mrlRoad, sourceMiddlePoint, nextMiddlePoint));
                        }
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
                                            obstacles_MrlBlockades.addAll(clearTools.getRoadObstacles(mrlRoad, getSelfLocation(), nextMiddlePoint));
                                        } else {
                                            obstacles_MrlBlockades.addAll(clearTools.getRoadObstacles(mrlRoad, worldInfo.getLocation(sourceArea), nextMiddlePoint));
                                        }
                                    } else {
                                        sourceMiddlePoint = new Pair<Integer, Integer>((int) sourceEdge.getMiddle().getX(), (int) sourceEdge.getMiddle().getY());
                                        obstacles_MrlBlockades.addAll(clearTools.getRoadObstacles(mrlRoad, sourceMiddlePoint, nextMiddlePoint));
                                    }
                                }
                            }
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

            //find blockades of last Entity in the sequence
            if (nextArea instanceof Road) {
                blockades = clearTools.getTargetRoadBlockades((Road) nextArea);
            }
        }

//        // adding blockades of the entrances near the specified path
//        blockades.addAll(findEntrancesNearPath(seenPath));


        for (MrlBlockade blockade : obstacles_MrlBlockades) {
            blockades.add(blockade.getParent());
        }


        return blockades;

    }


    /**
     * This method finds blockades in a specified range
     *
     * @param position XY position to find blockades to it
     * @param range    the range to find blockades in it
     * @return blockades in the specified range
     */
    private Set<Blockade> findBlockadesInRange(Pair<Integer, Integer> position, int range) {
        Set<Blockade> blockadeSet = new FastSet<Blockade>();
        for (Blockade blockade : getBlockadeSeen()) {

            if (blockade.getShape().contains(position.first(), position.second()) || Util.findDistanceTo(blockade, position.first(), position.second()) < range) {
                blockadeSet.add(blockade);
            }
        }
        return blockadeSet;
    }


    private Pair<Blockade, Integer> getTargetBlockade(Area area, int maxDistance) {
        //        Logger.debug("Looking for nearest blockade in " + area);
        if (area == null || !area.isBlockadesDefined()) {
            //            Logger.debug("Blockades undefined");
            return null;
        }
        List<EntityID> ids = area.getBlockades();
        // Find the first blockade that is in range.
        Set<Blockade> blockadesToClear = new FastSet<Blockade>();

        for (EntityID next : ids) {
            Blockade b = (Blockade) worldInfo.getEntity(next);
            blockadesToClear.add(b);
        }

        Pair<Blockade, Integer> nearestBlockadePair = clearTools.findNearestBlockade(blockadesToClear);


        if (nearestBlockadePair != null) {
            return nearestBlockadePair;
        }

        //        Logger.debug("No blockades in range");
        return null;
    }

    private Action moveToBlockadeIfIsAway(Pair<Blockade, Integer> blockadePair) {
        if (blockadePair == null) {
            return null;
        }
        if (blockadePair.second() < maxClearDistance) {
            //do nothing
            return null;
        } else {
            return moveToPoint(blockadePair.first().getPosition(), blockadePair.first().getX(), blockadePair.first().getY());
        }
    }

    /**
     * This Method clears blockades which are in a specific range of a specific position
     *
     * @param position the specified position to clear blockades based on distance to it
     * @param range    the range to find and clear blockades from specified position
     */
    public Action clearBlockadesInRange(Pair<Integer, Integer> position, int range) {

        Set<Blockade> blockades = findBlockadesInRange(position, range);
        return clearNearestBlockade(blockades);

    }

    private Action clearNearestBlockade(Set<Blockade> blockades) {

        Blockade blockadeToClear;

        Pair<Blockade, Integer> nearestBlockadePair = clearTools.findNearestBlockade(blockades);
        if (nearestBlockadePair != null) {
            blockadeToClear = nearestBlockadePair.first();
            Logger.info("Clearing blockade " + blockadeToClear);
//            getMrlRoad(blockadeToClear.getPosition()).setNeedUpdate(true);
            return sendClearAct(agentInfo.getTime(), blockadeToClear.getID());

        }
        return null;
    }


    private Set<Blockade> findEntrancesNearPath(List<EntityID> seenPath) {
        Set<EntityID> blockades = new FastSet<EntityID>();
        Set<Blockade> blockadeEntities = new FastSet<Blockade>();

        Road road;
        Area area;
        for (EntityID entityID : seenPath) {
            //if this entity itself is an entrance
            area = (Area) worldInfo.getEntity(entityID);
            if (area instanceof Road) {
                road = (Road) area;
                if (getEntranceRoads().keySet().contains(entityID)) {
                    if (road.isBlockadesDefined()) {
                        blockades.addAll(road.getBlockades());
                    }
                }

            }

            //if neighbours of the path is also entrance
            for (EntityID neighbourID : area.getNeighbours()) {
                area = (Area) worldInfo.getEntity(neighbourID);
                if (area instanceof Road) {
                    road = (Road) area;
                    if (getEntranceRoads().keySet().contains(entityID)) {
                        if (road.isBlockadesDefined()) {
                            blockades.addAll(road.getBlockades());
                        }
                    }

                }
            }

        }


        for (EntityID blockadeID : blockades) {
            blockadeEntities.add((Blockade) worldInfo.getEntity(blockadeID));
        }

        return blockadeEntities;
    }


//    private Blockade getTargetBlockade(List<EntityID> pathToGo) {
//        Logger.debug("Looking for target blockade");
//        Area location = (Area) agentInfo.getPositionArea();
//        Logger.debug("Looking in current location");
//        Blockade result =  getTargetBlockade(location, maxClearDistance);
//        if (result != null) {
//            return result;
//        }
//        Logger.debug("Looking in neighbouring locations");
//        for (EntityID next : location.getNeighbours()) {
//            location = (Area) worldInfo.getEntity(next);
//            result = getTargetBlockade(location, maxClearDistance);
//            if (result != null) {
//                return result;
//            }
//        }
//        return null;
//    }

    private Set<Blockade> getImportantBlockades(Set<Road> roadsSeen) {
        Set<Blockade> blockadeSet = new FastSet<Blockade>();
        Blockade blockade;
        for (Road road : roadsSeen) {
            if (!road.isBlockadesDefined()) {
                continue;
            }
            //if is on entrance road
            if (getEntranceRoads().containsKey(road.getID())) {
                for (EntityID entityID : road.getBlockades()) {
                    blockade = (Blockade) (worldInfo.getEntity(entityID));
                    if (getBlockadeSeen().contains(blockade)) {
                        blockadeSet.add(blockade);
                    }
                }
            }

            //if is on a passable edge
            for (Edge edge : road.getEdges()) {
                if (!edge.isPassable()) {
                    continue;
                }
                for (EntityID blockadeID : road.getBlockades()) {
                    blockade = (Blockade) worldInfo.getEntity(blockadeID);
                    rescuecore2.misc.geometry.Point2D middlePoint = Util.getMiddle(edge.getStart(), edge.getEnd());
                    if (Util.findDistanceTo(blockade, (int) middlePoint.getX(), (int) middlePoint.getY()) < 600) {
                        blockadeSet.add(blockade);
                    }

                }
            }


        }
        return blockadeSet;
    }

    private Set<Blockade> getBlockadesOnWay(List<EntityID> pathToGo, Set<Road> roadsSeen) {


        Set<Blockade> blockadeSet = new FastSet<Blockade>();

        if (pathToGo == null || pathToGo.isEmpty()) {
            return blockadeSet;
        }

        Blockade blockade;

        Set<Road> onWayEntrances = getOnWayEntrances(pathToGo);


        for (Road road : roadsSeen) {
            if (!road.isBlockadesDefined()) {
                continue;
            }

            //if is on way road
            if (!pathToGo.contains(road.getID())) {
                //do nothing
            } else {
                if (road.isBlockadesDefined()) {
                    for (EntityID blockadeID : road.getBlockades()) {
                        blockadeSet.add((Blockade) worldInfo.getEntity(blockadeID));
                    }
                }
            }

            //if is on entrance road
            if (onWayEntrances.contains(road)) {
                if (road.isBlockadesDefined() && !isEntranceOfBurningBuilding(road)) {
                    for (EntityID blockadeID : road.getBlockades()) {
                        blockadeSet.add((Blockade) worldInfo.getEntity(blockadeID));
                    }
                }

                blockadeSet.addAll(clearTools.getBlockadesInRange(road, getBlockadeSeen()));

            }
//            if (getEntranceRoads().containsKey(road)) {
//                for (EntityID entityID : road.getBlockades()) {
//                    blockade = (Blockade) (worldInfo.getEntity(entityID));
//                    if (getBlockadeSeen().contains(blockade)) {
//                        blockadeSet.add(blockade);
//                    }
//                }
//            }


        }
        return blockadeSet;
    }

    public Set<Blockade> getBlockadesOnWay_ImportanceBased(List<EntityID> pathToGo, Set<Road> roadsSeen) {
        Set<MrlBlockade> obstacles_MrlBlockades = new HashSet<MrlBlockade>();
        Set<Blockade> blockades = new FastSet<Blockade>();


        MrlEdge sourceEdge = null;


        //find entityID sequence of which can be seen
        List<EntityID> seenPath = new ArrayList<EntityID>();
        for (EntityID entityID : pathToGo) {
            if (worldInfo.getEntity(entityID) instanceof Road) {
                if (!roadsSeen.contains((Road) worldInfo.getEntity(entityID))) {
                    break;
                }
            }
            seenPath.add(entityID);
        }

        for (int i = 0; i < seenPath.size() - 1; i++) {
            int j = i + 1;
            EntityID entityID = seenPath.get(i);
            EntityID nextAreaID = seenPath.get(j);
            Area sourceArea = (Area) worldInfo.getEntity(entityID);
            Area nextArea = (Area) worldInfo.getEntity(nextAreaID);
            if (sourceArea instanceof Road) {
                Road road = (Road) sourceArea;
//                if (!roadsSeen.contains(road)) {
//                    break;
//                }
                MrlRoad mrlRoad = getMrlRoad(entityID);
                Set<Edge> edgeSet = RoadHelper.getEdgesBetween(sourceArea, nextArea);
                if (edgeSet.iterator().hasNext()) {
                    MrlEdge nextEdge = mrlRoad.getMrlEdge(edgeSet.iterator().next());
                    if (sourceEdge == null) {
                        for (MrlEdge mrlEdge : mrlRoad.getMrlEdges()) {
                            if (mrlEdge.isPassable() && !mrlEdge.equals(nextEdge)) {
                                sourceEdge = mrlEdge;
                                break;
                            }
                        }
                    }
                    obstacles_MrlBlockades.addAll(mrlRoad.getObstacles(sourceEdge, nextEdge));
                    sourceEdge = nextEdge;
                }
            } else {
                if (nextArea instanceof Road) {
                    Road road = (Road) nextArea;
//                    if (!roadsSeen.contains(road)) {
//                        break;
//                    }
                    MrlRoad mrlRoad = getMrlRoad(nextAreaID);
                    Set<Edge> edgeSet = RoadHelper.getEdgesBetween(sourceArea, nextArea);
                    if (edgeSet.iterator().hasNext()) {
                        MrlEdge nextEdge = mrlRoad.getMrlEdge(edgeSet.iterator().next());
                        if (sourceEdge == null) {
                            for (MrlEdge mrlEdge : mrlRoad.getMrlEdges()) {
                                if (mrlEdge.isPassable() && !mrlEdge.equals(nextEdge)) {
                                    sourceEdge = mrlEdge;
                                    break;
                                }
                            }
                        }
                        obstacles_MrlBlockades.addAll(mrlRoad.getObstacles(sourceEdge, nextEdge));
                        sourceEdge = nextEdge;
                    }
                } else {
                    sourceEdge = null;
                }
            }
        }


        for (MrlBlockade blockade : obstacles_MrlBlockades) {
            blockades.add(blockade.getParent());
        }


        return blockades;
    }

    private boolean isEntranceOfBurningBuilding(Road road) {
        Building building = (Building) worldInfo.getEntity(getEntranceRoads().get(road.getID()));
        boolean isFierynessDefined = building.isFierynessDefined();
        int fieryness = -1;
        if (isFierynessDefined) {
            fieryness = building.getFieryness();
        }
        return isFierynessDefined && (fieryness == 1 || fieryness == 2 || fieryness == 3 || fieryness == 7 || fieryness == 8);
    }

    private Set<Road> getOnWayEntrances(List<EntityID> pathToGo) {

        Set<Road> onWayEntrances = new FastSet<Road>();
        Road road;
        for (EntityID entityID : pathToGo) {
            if (worldInfo.getEntity(entityID) instanceof Road) {
                road = (Road) worldInfo.getEntity(entityID);
                //if it is an entrance
                if (getEntranceRoads().containsKey(entityID)) {
                    onWayEntrances.add(road);
                } else {// if its neighbours are entrance
                    for (EntityID neighbourID : road.getNeighbours()) {
                        if (worldInfo.getEntity(neighbourID) instanceof Road) {
                            road = (Road) worldInfo.getEntity(neighbourID);
                            if (getEntranceRoads().keySet().contains(neighbourID)) {
                                if (!isEntranceOfBurningBuilding(road)) {
                                    onWayEntrances.add(road);
                                }
                            }
                        }
                    }

                }
            }

        }

        return onWayEntrances;
    }


}
