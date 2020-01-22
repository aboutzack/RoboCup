package mrl_2019.world.entity;

import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl_2019.util.Util;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.helper.RoadHelper;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by P.D.G. and M.Shabani
 * Date: Apr 29, 2010
 * Time: 4:34:49 PM
 */
public class Paths extends ArrayList<Path> {

    private MrlWorldHelper worldHelper;
    private RoadHelper roadHelper;
    private List<Road> roads = new ArrayList<Road>();
    private List<Road> headRoads = new ArrayList<Road>();
    private List<Road> orphanRoads = new ArrayList<Road>();
    private List<Road> entrances = new ArrayList<Road>();
    private Map<EntityID, Entrance> entrancesMap = new FastMap<EntityID, Entrance>();
    private List<Road> added = new ArrayList<Road>();
    private boolean end;
    private Map<EntityID, Path> roadPathMap = new FastMap<EntityID, Path>();
    protected WorldInfo worldInfo;
    protected AgentInfo agentInfo;
    protected ScenarioInfo scenarioInfo;


    public Paths(MrlWorldHelper worldHelper, WorldInfo worldInfo, AgentInfo agentInfo, ScenarioInfo scenarioInfo) {

        this.agentInfo = agentInfo;
        this.scenarioInfo = scenarioInfo;
        this.worldHelper = worldHelper;
        this.worldInfo = worldInfo;
        roadHelper = worldHelper.getHelper(RoadHelper.class);
        this.addAll(createPaths());

        // test if we have any orphan road yet or not
        createMappingFromRoadToPath();


        //TODO: General Test - Pooya      It is just used in Federated coordination Strategy
        createPathsNeighbours();

    }

    private void createPathsNeighbours() {
        Set<Path> neighbours;
        for (Path path : this) {
            neighbours = new FastSet<Path>();
            for (Road road : path) {
                for (EntityID entityID : road.getNeighbours()) {
                    if (worldInfo.getEntity(entityID) instanceof Road) {
                        for (Path p : this) {
                            if (!path.equals(p) && p.contains((Road) worldInfo.getEntity(entityID))) {
                                if (!neighbours.contains(p)) {
                                    neighbours.add(p);
                                }
                            }
                        }

                    }
                }

            }
            path.setNeighbours(neighbours);
        }


    }

    public List<Path> createPaths() {

        List<Path> paths = new ArrayList<Path>();

        getRoadsAndEntrances();
        getHeadRoads();
        createPathsByMeansOfFoundHeadRoads(paths);

        //set ID for each Path
        setIDForPaths(paths);

        setPathIDForRoads(paths);

        validateEndOfPath(paths);

        // because we first don't consider entrance Roads for creating paths

        attachOrphanRoadsToNearestPath(paths);

        return paths;
    }

    private void validateEndOfPath(List<Path> paths) {
        for (Path path : paths) {
            if (path.getEndOfPath() == null || path.getEndOfPath().equals(path.getHeadOfPath())) {
                double dist, maxDist = 0;
                Road farthestRoad = path.getHeadOfPath();
                for (Road road : path) {
                    dist = Util.distance(road, path.getHeadOfPath());
                    if (path.isOrphanRoad(road)) {
                        dist = -1;//for ignoring orphan roads
                    }
                    if (entrances.contains(road)) {
                        dist *= 0.5;//for decrease chance of selecting entrances
                    }
                    if (dist > maxDist) {
                        maxDist = dist;
                        farthestRoad = road;
                    }
                }
                path.setEndOfPath(farthestRoad);
            }
        }
    }

    private void getRoadsAndEntrances() {


        for (StandardEntity standardEntity : worldHelper.getRoads()) {
            Road road = (Road) standardEntity;
            if (isEntrance(road)) {
                if (!entrances.contains(road)) {
                    createEntrance(road);
                    entrances.add(road);
                }
                ArrayList<Road> connectedRoads = roadHelper.getConnectedRoads(road.getID());
                if (connectedRoads.size() > 1) {
                    Road neighbour = connectedRoads.get(0);
                    Road otherNeighbour = connectedRoads.get(1);
                    if (!roadHelper.getConnectedRoads(neighbour.getID()).contains(otherNeighbour)) {
                        roads.add(road);
                        continue;
                    }
                }
//                exclusiveEntrances.add(road);


            } else {
                roads.add(road);
            }
        }
    }

    private void createEntrance(Road road) {
        Entrance entrance = entrancesMap.get(road.getID());
        if (entrance == null) {
            List<Building> buildings = worldHelper.getHelper(RoadHelper.class).getBuildingsOfThisEntrance(road.getID());
            entrance = new Entrance(road, buildings);
            entrancesMap.put(road.getID(), entrance);
            if (worldHelper.getMrlBuildings() != null && !worldHelper.getMrlBuildings().isEmpty()) {
                for (Area area : buildings) {
                    worldHelper.getMrlBuilding(area.getID()).addEntrance(entrance);
                }
            }
        }
    }

    private boolean isEntrance(Road road) {
        if (RoadHelper.getConnectedBuildings(worldInfo, road).size() > 0) {
            return true;
        }
        return false;
    }

    private void getHeadRoads() {

        for (Road road : roads) {
            int numberOfNeighbours = 0;  // which are not entrance of a building

            for (Road r : roadHelper.getConnectedRoads(road.getID())) {
                if (!entrances.contains(r)) {
                    numberOfNeighbours++;
                }
            }
            if (numberOfNeighbours > 2 /*&& numberOfNeighbours < 5*/) {
                headRoads.add(road);
            }
        }

        // remove headRoads that are neighbour to the other headRoad and are larger than it
        removeNeighbours(headRoads);
    }

    private void removeNeighbours(List<Road> headRoads) {
        Set<Road> toRemove = new FastSet<Road>();
        Rectangle rec1;
        Rectangle rec2;
        for (Road road : headRoads) {
            for (Road r : roadHelper.getConnectedRoads(road.getID())) {

                if (headRoads.contains(r) && !toRemove.contains(r)) {
                    rec1 = r.getShape().getBounds();
                    rec2 = road.getShape().getBounds();

                    if (rec1.getHeight() * rec1.getWidth() > rec2.getHeight() * rec2.getWidth()) {
                        toRemove.add(r);
                    } else {
                        toRemove.add(road);
                    }

                }
            }
        }
        headRoads.removeAll(toRemove);
    }

    private void createPathsByMeansOfFoundHeadRoads(List<Path> paths) {

        for (Road headRoad : headRoads) {
            paths.addAll(createThisHeadRoadPaths(headRoad));
        }
    }

    private List<Path> createThisHeadRoadPaths(Road head) {
        List<Path> paths = new ArrayList<Path>();
        List<Road> headRoadEntrances = new ArrayList<Road>();
        for (Road neighbourRoad : roadHelper.getConnectedRoads(head.getID())) {

            if (added.contains(neighbourRoad)) {
                continue;
            } else if (entrances.contains(neighbourRoad)) {
                added.add(neighbourRoad);
                headRoadEntrances.add(neighbourRoad);
                continue;
            }
            end = false;
            Path path = new Path(worldHelper, worldInfo, agentInfo, scenarioInfo);

            path.add(head);
            added.add(head);

            path.setHeadOfPath(head);
            createAPath(neighbourRoad, path);

            paths.add(path);
            added.remove(head);
        }
        if (!headRoadEntrances.isEmpty() && paths.size() > 0) {
            List<Road> temp = new ArrayList<Road>();
            temp.addAll(paths.get(0));
            paths.get(0).clear();
            for (Road road : headRoadEntrances) {
                paths.get(0).add(road);
                paths.get(0).addEntrance(getEntrance(road));
            }
            paths.get(0).addAll(temp);
        }

        return paths;
    }

    private void createAPath(Road neighbourRoad, Path path) {
        if (!path.contains(neighbourRoad)) {
            path.add(neighbourRoad);
            added.add(neighbourRoad);
        }

        addEntrances(path, neighbourRoad);

        for (Road road : roadHelper.getConnectedRoads(neighbourRoad.getID())) {
            if (entrances.contains(road) || added.contains(road)) {
                continue;
            }
            if (end || roadHelper.getConnectedRoads(neighbourRoad.getID()).size() == 1) {
                return;
            }
            if (headRoads.contains(road)) {
                if (!end) {
                    end = true;
                    if (!path.contains(road)) {
                        path.add(road);
                    }
                    path.setEndOfPath(road);
                }
            } else if (!end) {
                createAPath(road, path);
            }
        }
        if (!end) {
            for (Road neighbourR : roadHelper.getConnectedRoads(neighbourRoad.getID())) {
                if (headRoads.contains(neighbourR)) {
                    if (!path.contains(neighbourR)) {
                        path.add(neighbourR);
                    }
                    path.setEndOfPath(neighbourR);
                    end = true;
                }
            }
        }
    }

    private void setIDForPaths(List<Path> paths) {
        for (Path path : paths) {
            path.setId();
        }
    }

    private void addEntrances(Path path, Road road) {
        Entrance entrance;
        for (Road n : roadHelper.getConnectedRoads(road.getID())) {
            if (entrances.contains(n)) {
                roadHelper.setPathId(n.getID(), path.getId());
                if (!path.contains(n) && !added.contains(n)) {
                    path.add(n);
                    added.add(n);
                }
                entrance = getEntrance(n);
                if (!path.getEntrances().contains(entrance)) {
                    path.addEntrance(entrance);
                }
            }
        }
    }

    private Entrance getEntrance(Road road) {
        Entrance entrance = entrancesMap.get(road.getID());
        if (entrance == null) {
            List<Building> buildings = worldHelper.getHelper(RoadHelper.class).getBuildingsOfThisEntrance(road.getID());
            entrance = new Entrance(road, buildings);
            entrancesMap.put(road.getID(), entrance);
            for (Area area : buildings) {
                worldHelper.putEntrance(area.getID(), entrance);
                worldHelper.getMrlBuilding(area.getID()).addEntrance(entrance);
            }
        }
        return entrance;
    }

    private void attachOrphanRoadsToNearestPath(List<Path> paths) {

        findOrphanRoads();

        ArrayList<Road> orRoads = new ArrayList<Road>();

        orRoads.addAll(orphanRoads);

        while (!orphanRoads.isEmpty()) {

            for (Road road : orRoads) {
                boolean isAdded = false;
                for (Road connectedRoad : roadHelper.getConnectedRoads(road.getID())) {
                    EntityID rPathId = roadHelper.getPathId(connectedRoad.getID());
                    Path path = getPath(rPathId, paths);
                    if (rPathId != null) {
                        isAdded = true;
                        adding(road, path);
                        path.addOrphanRoad(road);
                        orphanRoads.remove(road);
                        break;
                    }
                }
                if (!isAdded) {
                    Path nearestPath = getNearestPath(road, paths);
                    if (nearestPath != null) {
                        adding(road, nearestPath);
                        orphanRoads.remove(road);
                    }
                }
            }

            if (orphanRoads.size() == orRoads.size())
                break;
            orRoads.clear();
            orRoads.addAll(orphanRoads);
        }
    }

    private Path getPath(EntityID pathId, List<Path> paths) {
        for (Path path : paths) {
            if (path.getId().equals(pathId)) {
                return path;
            }
        }
        return null;
    }

    private void findOrphanRoads() {
        for (StandardEntity road : worldHelper.getRoads()) {
            if (roadHelper.getPathId(road.getID()) == null) {
                orphanRoads.add((Road) road);
            }
        }
    }

//    private void addToPath(Road road, EntityID pathId, List<Path> paths) {
//        for (Path path : paths) {
//            if (path.getId().equals(pathId)) {
//                adding(road, path);
//                break;
//            }
//        }
//    }

    private void adding(Road road, Path path) {
        roadHelper.setPathId(road.getID(), path.getId());
        if (!path.contains(road)) {
            path.add(road);
            if (entrances.contains(road)) {
                path.addEntrance(getEntrance(road));
            }
        }
    }

    private Path getNearestPath(Road road, List<Path> paths) {
        int range = 10000;
        int minDistance = Integer.MAX_VALUE;
        EntityID nearestPath = null;

        while (nearestPath == null) {
            Collection<StandardEntity> entities = worldInfo.getObjectsInRange(road, range);

            for (StandardEntity entity : entities) {
                if (entity instanceof Road) {
                    EntityID pathId = roadHelper.getPathId(entity.getID());

                    if (pathId != null) {
                        int distance = Util.distance((Area) entity, road);
                        if (distance < minDistance) {
                            nearestPath = pathId;
                            minDistance = distance;
                        }
                    }
                }
            }
            range += 10000;
        }

        for (Path path : paths) {
            if (path.getId().equals(nearestPath)) {
                return path;
            }
        }
        return null;
    }

    private void setPathIDForRoads(List<Path> paths) {
        for (Path path : paths) {
            for (Road road : path) {
                roadHelper.setPathId(road.getID(), path.getId());
            }
        }
    }

    private void createMappingFromRoadToPath() {

        for (Path path : this) {
            for (Road road : path) {
                roadPathMap.put(road.getID(), path);
            }
        }
    }

    public Path getRoadPath(EntityID roadId) {
        return roadPathMap.get(roadId);
    }

    public List<Road> getEntrances() {
        return entrances;
    }
}
