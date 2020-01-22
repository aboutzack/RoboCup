package mrl_2019.complex.search;


import adf.agent.info.AgentInfo;
import adf.agent.info.WorldInfo;
import mrl_2019.MRLConstants;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.entity.MrlBuilding;
import mrl_2019.world.entity.MrlRoad;
import mrl_2019.world.entity.Path;
import rescuecore2.standard.entities.Road;

import java.util.*;

/**
 * @author Mahdi
 */
public class SimpleSearchDecisionMaker {
    private boolean searchInPartition = false;
    private List<Path> shouldDiscoverPaths;
    private List<Path> discoveredPaths;

    private Path pathInProgress;
    private MrlWorldHelper world;

    protected Set<Path> validPaths;


    public SimpleSearchDecisionMaker(MrlWorldHelper world, AgentInfo ai, WorldInfo wi) {
        this.world = world;
        validPaths = new HashSet<>();
    }

    //    @Override
    public void update() {
//        if (isPartitionChanged()) {
//            resetSearch();
//        }

        if (searchInPartition) {
//            validBuildings.clear();
//            validPaths.clear();
//            Partition myPartition = world.getPartitionManager().findHumanPartition(world.getSelfHuman());
//            if (myPartition == null) {
//                validBuildings.addAll(world.getBuildingIDs());
//                validPaths.addAll(world.getPaths());
//            } else {
//                Set<Partition> humanPartitionsMap = world.getPartitionManager().findHumanPartitionsMap(world.getSelfHuman());
//
//                for (Partition partition : humanPartitionsMap) {
//                    validBuildings.addAll(partition.getBuildingIDs());
//                    validPaths.addAll(partition.getPaths());
//                }
//
//
//            }
//            shouldDiscoverBuildings.retainAll(validBuildings);
        } else {
            validPaths.addAll(world.getPaths());
        }
        setShouldDiscoverPaths();
    }

    //    @Override
    public void initialize() {
        discoveredPaths = new ArrayList<>();

        shouldDiscoverPaths = new ArrayList<>();

        setPathBuildingsMap();
        pathInProgress = null;
    }

    public Path getNextPath() {
        Path nextPath = null;
        if (pathInProgress == null) {
            if (MRLConstants.DEBUG_SEARCH)
                world.printData("no path in progress... choose present path.");
            if (!shouldDiscoverPaths.isEmpty()) {
                nextPath = getMyPath();
                if (!shouldDiscoverPaths.contains(nextPath)) {
                    nextPath = shouldDiscoverPaths.get(0);
                }
            } else {
                if (MRLConstants.DEBUG_SEARCH)
                    world.printData("shouldDiscoverPath is empty!!! going to reset it.\nDiscovered paths:" + discoveredPaths.size() + "\nValid paths:" + validPaths.size());
                discoveredPaths.clear();
                setShouldDiscoverPaths();
            }
//                nextPath = shouldDiscoverPaths.get(0);
//            } else {
//                nextPath = null;
//            }
        } else {
            Set<Path> neighbours = pathInProgress.getNeighbours();
            for (Path path : neighbours) {
                if (shouldDiscoverPaths.contains(path)) {
                    nextPath = path;
                    break;
                }
            }
            if (nextPath == null) {
                if (shouldDiscoverPaths.isEmpty()) {
                    if (MRLConstants.DEBUG_SEARCH)
                        world.printData("shouldDiscoverPath is empty!!! going to reset it.\nDiscovered paths:" + discoveredPaths.size() + "\nValid paths:" + validPaths.size());
                    discoveredPaths.clear();
                    setShouldDiscoverPaths();
                } else {
                    int size = shouldDiscoverPaths.size();
                    Random random = new Random(System.currentTimeMillis());
                    int index = Math.abs(random.nextInt()) % size;
                    nextPath = shouldDiscoverPaths.get(index);
                }
            }
        }
        if (nextPath != null) {
            shouldDiscoverPaths.remove(nextPath);
            if (!discoveredPaths.contains(nextPath))
                discoveredPaths.add(nextPath);
        }
        pathInProgress = nextPath;
        return pathInProgress;
    }

    private void setPathBuildingsMap() {
//        Set<Building> buildings = new HashSet<>();
//        for (Path path : world.getPaths()) {
//            buildings.clear();
//            for (Area area : path.getBuildings()) {
//                buildings.add((Building) area);
//            }
//        }
    }

    private void setShouldDiscoverPaths() {
        shouldDiscoverPaths.clear();
        if (searchInPartition) {
            shouldDiscoverPaths.addAll(validPaths);
        } else {
            shouldDiscoverPaths.addAll(world.getPaths());
        }
        shouldDiscoverPaths.removeAll(discoveredPaths);
    }

    private Path getMyPath() {
        Path myPath;
        if (world.getSelfPosition() instanceof Road) {
            MrlRoad mrlRoad = world.getMrlRoad(world.getSelfPosition().getID());
            myPath = mrlRoad.getPaths().get(0);
        } else {
            MrlBuilding mrlBuilding = world.getMrlBuilding(world.getSelfPosition().getID());
            Road roadEntrance = mrlBuilding.getEntrances().get(0).getNeighbour();
            MrlRoad mrlRoad = world.getMrlRoad(roadEntrance.getID());
            myPath = mrlRoad.getPaths().get(0);
        }
        if (myPath == null) {
            if (MRLConstants.DEBUG_SEARCH)
                world.printData("myPath = null");
        }
        return myPath;
    }

    private void resetSearch() {
        shouldDiscoverPaths.clear();
        pathInProgress = null;
    }

//    private void setShouldDiscoverBuildings(Path path) {
//        shouldDiscoverBuildings = pathBuildingsMap.get(path);
//    }
}
