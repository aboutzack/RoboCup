package mrl_2019.world.entity;

import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import javolution.util.FastMap;
import mrl_2019.extaction.clear.GuideLine;
import mrl_2019.util.Util;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.helper.RoadHelper;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * Created by P.D.G. and M.Eghbali
 * edited by Mostafa Shabani.
 * Date: Apr 29, 2010
 * Time: 2:47:08 PM
 */
public class Path extends ArrayList<Road> implements Comparable {

    private MrlWorldHelper worldHelper;
    private List<Entrance> entrances;
    private EntityID id;
    private Road endOfPath = null;
    private Road headOfPath = null;
    private double value = 0;
    private int searched = 0;
    //private boolean hasBeenSeen = false;
    //private static final int AT_IMP_TARGET = 2;
    //private static final int FB_IMP_TARGET = 1;
    private Set<Road> importantRoads = new HashSet<Road>();
    private List<Area> buildingsOfThisPath;
    private int totalBuildingArea = 0;
    protected Map<EntityID, Integer> agentDistanceMap = new FastMap<EntityID, Integer>();
    private List<Edge> commonEdge = new ArrayList<Edge>();
    private Set<Path> neighbours;
    private Set<Road> orphanRoads;

    private Set<GuideLine> guideLines;

    private int targetToGoValue;
    private int lenght;
    private List<EntityID> headToEndRoads;
    private WorldInfo worldInfo;
    private AgentInfo agentInfo;
    private ScenarioInfo scenarioInfo;


    public Path(MrlWorldHelper worldHelper, WorldInfo worldInfo, AgentInfo agentInfo, ScenarioInfo scenarioInfo) {
        this.worldHelper = worldHelper;
        this.worldInfo = worldInfo;
        this.agentInfo = agentInfo;
        this.scenarioInfo = scenarioInfo;
        entrances = new ArrayList<Entrance>();
        buildingsOfThisPath = new ArrayList<Area>();
        guideLines = new HashSet<>();
        orphanRoads = new HashSet<>();
//        initThisPathBuildings();
    }

    public int getTargetToGoValue() {
        return targetToGoValue;
    }

    public void setTargetToGoValue(int targetToGoValue) {
        this.targetToGoValue = targetToGoValue;
    }

    public void resetValue() {
        value = 0;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public void setId() {
        this.id = getMiddleArea();
    }

    public EntityID getId() {
        return id;
    }

    public Road getEndOfPath() {
        return endOfPath;
    }

    public void setEndOfPath(Road endOfPath) {
        this.endOfPath = endOfPath;
    }

    public Road getHeadOfPath() {
        return headOfPath;
    }

    public void setHeadOfPath(Road headOfPath) {
        this.headOfPath = headOfPath;
    }

    public void addEntrance(Entrance entrance) {
        this.entrances.add(entrance);
        List<Building> buildings = entrance.getBuildings();
        buildingsOfThisPath.addAll(buildings);
    }

    public List<Entrance> getEntrances() {
        return entrances;
    }

    public List<Area> getBuildings() {
        return buildingsOfThisPath;
    }

    public int getTotalBuildingArea() {
        return totalBuildingArea;
    }

    public int getBurningBuildingsTotalArea() {
        int totalBurningBuildingArea = 0;

        for (Area entity : buildingsOfThisPath) {
            Building building = (Building) entity;
            if (building.isFierynessDefined() && building.getFieryness() > 1 && building.getFieryness() < 3)
                totalBurningBuildingArea += building.getTotalArea();
        }
        return totalBurningBuildingArea;
    }

    public Set<Path> getNeighbours() {
        return neighbours;
    }

    public void setNeighbours(Set<Path> neighbours) {
        this.neighbours = neighbours;
    }

    public EntityID getMiddleArea() {
        return this.get((this.size() / 2)).getID();
    }

    public Road getMiddleRoad() {
        return this.get((this.size() / 2));
    }

    public Pair<Integer, Integer> getMiddleAreaLocation() {
        return worldInfo.getLocation(getMiddleArea());
    }

    public int getReportedBlockadesDif() {
        int numberOfBlockades = 0;
        for (StandardEntity standardEntity : worldHelper.getRoads()) {
            Road road = (Road) standardEntity;
            if (road.isBlockadesDefined())
                numberOfBlockades += road.getBlockades().size();
        }
        return numberOfBlockades;
    }


    public boolean isContaining(EntityID entityID) {
        for (Road road : this) {
            if (road.getID() == entityID)
                return true;
            for (Building building : RoadHelper.getConnectedBuildings(worldInfo, road)) {
                if (building.getID() == entityID)
                    return true;
            }
        }
        return false;
    }

    public void updateAgentDistance(Collection<StandardEntity> freeAgents) {
        int pathX = getMiddleRoad().getX();
        int pathY = getMiddleRoad().getY();
        agentDistanceMap.clear();

        for (StandardEntity standardEntity : freeAgents) {
            PoliceForce policeForce = (PoliceForce) standardEntity;
            Integer fX, fY;
            if (policeForce.isPositionDefined()) {
                StandardEntity entity = worldInfo.getEntity(policeForce.getPosition());
                if (entity instanceof Area) {
                    fX = ((Area) entity).getX();
                    fY = ((Area) entity).getY();
                } else {
                    System.err.println(agentInfo.me() + " Time:" + agentInfo.getTime() + " Agent:" + policeForce + " position:" + entity);
                    agentDistanceMap.remove(policeForce.getID());
                    continue;
                }
            } else {
                System.err.println(agentInfo.me() + " Time:" + agentInfo.getTime() + " Agent:" + policeForce + " position: UNKNOWN");
                agentDistanceMap.remove(policeForce.getID());
                continue;
            }
            int distance = Util.distance(pathX, pathY, fX, fY);
            agentDistanceMap.put(policeForce.getID(), distance);
        }
    }

    public Boolean isItOpened() {
        RoadHelper roadHelper = worldHelper.getHelper(RoadHelper.class);
        for (Road road : this) {
            if (!roadHelper.isPassable(road.getID())) {
                return false;
            }
        }
        return true;
    }

    public boolean containsId(EntityID id) {
        for (Road r : this) {
            if (r.getID().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public boolean equals(Path other) {
        return getId().equals(other.getId());
    }

    public List<EntityID> toEntityId() {
        List<EntityID> ids = new ArrayList<EntityID>();
        for (Road r : this)
            ids.add(r.getID());
        return ids;
    }

    public void addImportantRoad(Road road) {
        //if(!importantRoads.contains(road))
        importantRoads.add(road);
    }

    public Collection<Road> getImportantRoads() {
        return importantRoads;
    }

    public int computeTotalRepairCost() {
        int totalRepairCost = 0;
        for (Road road : this) {
            totalRepairCost += RoadHelper.totalRepairCost(worldInfo, road);
        }

        return totalRepairCost;
    }

    @Override
    public int compareTo(Object o) {
        if (value < ((Path) o).getValue())
            return 1;
        if (value == ((Path) o).getValue())
            return 0;

        return -1;
    }

    public Map<EntityID, Integer> getAgentDistanceMap() {
        return agentDistanceMap;
    }

    @Override
    public String toString() {
        return "Path[id:" + getMiddleRoad().getID() + " v:" + value + "]";
    }

    public int getNeededAgentsToSearch() {
        return (size() > 8 ? 2 : 1);
    }

    public boolean isSearched() {
        return searched == -1;
    }

    /**
     * @param searched -1 for finished, 0 for not searched, others(world.time) repeat check this path if new time - pre time = 7.
     */
    public void setSearched(int searched) {
        this.searched = searched;
    }

    public boolean checkCanSearch(int time) {
        if (searched != 0 && searched + 7 <= time) {
            searched = 0;
        }
        return searched == 0;
    }

//    public int getUnvisitedBuildingCount() {
//        int c = 0;
//        for (Area area : buildingsOfThisPath) {
//            if (!worldHelper.getMrlBuilding(area.getID()).isVisited()) {
//                c++;
//            }
//        }
//        return c;
//    }

    public void setLenght(int lenght) {
        this.lenght = lenght;
    }

    public int getLenght() {
        return lenght;
    }

    public List<Edge> getCommonEdge() {
        return commonEdge;
    }

    public Set<GuideLine> getGuideLines() {
        return guideLines;
    }

    public void setGuideLines(Set<GuideLine> guideLines) {
        this.guideLines = guideLines;
    }

    public void addOrphanRoad(Road road) {
        orphanRoads.add(road);
    }

    public Set<Road> getOrphanRoads() {
        return orphanRoads;
    }

    public boolean isOrphanRoad(Road road){
        return orphanRoads.contains(road);
    }

    public List<EntityID> getHeadToEndRoads() {
        return headToEndRoads;
    }

    public void setHeadToEndRoads(List<EntityID> headToEndRoads) {
        this.headToEndRoads = new ArrayList<>(headToEndRoads);
    }
}

