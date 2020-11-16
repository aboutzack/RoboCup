package CSU_Yunlu_2020.world;

import CSU_Yunlu_2020.CSUConstants;
import CSU_Yunlu_2020.debugger.DebugHelper;
import CSU_Yunlu_2020.geom.PolygonScaler;
import CSU_Yunlu_2020.module.algorithm.fb.CompositeConvexHull;
import CSU_Yunlu_2020.module.complex.fb.tools.FileEntityIDMap;
import CSU_Yunlu_2020.standard.Ruler;
import CSU_Yunlu_2020.standard.simplePartition.Line;
import CSU_Yunlu_2020.util.Util;
import CSU_Yunlu_2020.world.graph.GraphHelper;
import CSU_Yunlu_2020.world.graph.MyEdge;
import CSU_Yunlu_2020.world.object.*;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.information.MessageBuilding;
import adf.agent.communication.standard.bundle.information.MessageRoad;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.AbstractModule;
import javolution.util.FastSet;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

/**
 * @Description: 改进自csu_2016
 * @Date: 3/8/20
 */
public class CSUWorldHelper extends AbstractModule {
    // owner agent part
    protected Human selfHuman;                     //only for platoon agent
    protected Building selfBuilding;        //only for center agent
    protected EntityID selfId;
    protected AgentInfo agentInfo;
    protected WorldInfo worldInfo;
    protected ScenarioInfo scenarioInfo;
    protected ModuleManager moduleManager;
    protected DevelopData developData;

    //current time step
    protected Set<EntityID> roadsSeen;
    protected Set<EntityID> buildingsSeen;
    protected Set<EntityID> civiliansSeen;
    protected Set<EntityID> fireBrigadesSeen;
    protected Set<EntityID> blockadesSeen;
    protected Set<EntityID> burningBuildings;
    protected Set<EntityID> collapsedBuildings;
    protected Set<EntityID> emptyBuildings;
    protected Set<EntityID> availableHydrants;
    protected Set<EntityID> stuckAgents;

    //CSU entities map
    protected Map<EntityID, CSUBlockade> csuBlockadeMap;
    protected Map<EntityID, CSUBuilding> csuBuildingMap;
    protected Map<EntityID, CSURoad> csuRoadMap;
    protected Map<EntityID, CSUHydrant> csuHydrantMap;
    protected Map<String, Building> buildingXYMap;


    // map informs
    protected int minX, maxX, minY, maxY;
    protected Set<StandardEntity> mapBorderBuildings;
    protected Dimension mapDimension;
    private Area mapCenter;
    protected double mapWidth;
    protected double mapHeight;
    protected double mapDiameter;
    protected boolean isMapHuge = false; // TODO: 3/8/20 根据mapSize制定灭火策略
    protected boolean isMapMedium = false;
    protected boolean isMapSmall = false;
    private long uniqueMapNumber;

    // communication conditions
    protected boolean communicationLess = false;     //不能进行无线电通讯
    protected boolean communicationLow = false;
    protected boolean communicationMedium = false;
    protected boolean communicationHigh = false;

    //others
    protected ConfigConstants config;
    protected Map<Property, Integer> propertyTimeMap;

    //sub modules
    protected GraphHelper graph;

    //for search
    private EntityID searchTarget;
    private Map<EntityID, Integer> lastSendTime;

    public CSUWorldHelper(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.agentInfo = ai;
        this.worldInfo = wi;
        this.scenarioInfo = si;
        this.moduleManager = moduleManager;
        this.developData = developData;
        if (agentInfo.me() instanceof Building) {
            selfBuilding = (Building) agentInfo.me();
        } else {
            selfHuman = (Human) agentInfo.me();
        }
        selfId = agentInfo.me().getID();
        roadsSeen = new HashSet<>();
        buildingsSeen = new HashSet<>();
        civiliansSeen = new HashSet<>();
        fireBrigadesSeen = new HashSet<>();
        blockadesSeen = new HashSet<>();
        burningBuildings = new HashSet<>();
        collapsedBuildings = new HashSet<>();
        emptyBuildings = new HashSet<>();
        availableHydrants = new HashSet<>();
        stuckAgents = new HashSet<>();

        csuBlockadeMap = new HashMap<>();
        csuBuildingMap = new HashMap<>();
        csuRoadMap = new HashMap<>();
        csuHydrantMap = new HashMap<>();
        buildingXYMap = new HashMap<>();

        propertyTimeMap = new HashMap<>();
        lastSendTime = new HashMap<>();

        config = new ConfigConstants(scenarioInfo.getRawConfig(), this);
        graph = moduleManager.getModule("GraphHelper.Default", CSUConstants.GRAPH_HELPER_DEFAULT);

        registerModule(graph);
        initMapInforms();
        initMapCenter();
        initWorldCommunicationCondition();
        initCsuBuildings();
        initCsuRoads();
        initCsuHydrants();
        initCsuBlockades();
        initBorderBuildings();
    }

    @Override
    public AbstractModule precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        graph.precompute(precomputeData);
        processVisibilityData(true);
        return this;
    }

    @Override
    public CSUWorldHelper resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
        graph.resume(precomputeData);
        processVisibilityData(false);
        return this;
    }

    @Override
    public CSUWorldHelper preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }
        graph.preparate();
        processVisibilityData(false);
        return this;
    }


    /**
     * <pre>
     * First, get all entities I can see from ChangeSet. For each visible entity, if it's
     * not in the collection of entities I can see last cycle, add it. If it is in, update its
     * property.
     *
     * Second, delete those entities I can see this cycle from the collection of entities
     * I can see last cycle.
     *
     * Third, update human rectangles.
     * </pre>
     */
    @Override
    public CSUWorldHelper updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        graph.updateInfo(messageManager);
        reflectMessage(messageManager);
        roadsSeen.clear();
        buildingsSeen.clear();
        blockadesSeen.clear();
        civiliansSeen.clear();
        fireBrigadesSeen.clear();
        worldInfo.getChanged().getChangedEntities().forEach(changedId -> {
            StandardEntity entity = worldInfo.getEntity(changedId);
            if (entity instanceof Civilian) {
                civiliansSeen.add(entity.getID());
            } else if (entity instanceof Building) {
                Building building = (Building) entity;
                CSUBuilding csuBuilding = getCsuBuilding(building.getID());
                if (agentInfo.me() instanceof FireBrigade) {
                    if (building.isFierynessDefined() && building.isTemperatureDefined()) {
                        csuBuilding.setEnergy(building.getTemperature() * csuBuilding.getCapacity(), "updateInfo");
                        csuBuilding.updateValues(building);
                    }
                }
                //update burning buildings set
                if (building.getFieryness() > 0 && building.getFieryness() < 4) {
                    burningBuildings.add(building.getID());
                } else {
                    burningBuildings.remove(building.getID());
                }

                buildingsSeen.add(building.getID());
                if (building.isOnFire()) {
                    csuBuilding.setIgnitionTime(agentInfo.getTime());
                }
                csuBuilding.setLastSeenTime(agentInfo.getTime());
                csuBuilding.setLastUpdateTime(agentInfo.getTime());

                //Update seen building properties
                for (Property p : worldInfo.getChanged().getChangedProperties(building.getID())) {
                    building.getProperty(p.getURN()).takeValue(p);
                    propertyTimeMap.put(p, agentInfo.getTime());
                }

            } else if (entity instanceof Road) {
                Road road = (Road) entity;
                roadsSeen.add(road.getID());

                CSURoad csuRoad = getCsuRoad(entity.getID());
                csuRoad.update();
            } else if (entity instanceof Blockade) {
                blockadesSeen.add(entity.getID());
            } else if (entity instanceof FireBrigade) {
                fireBrigadesSeen.add(entity.getID());
            }
        });
        //reset road passably
        for (CSURoad csuRoad : csuRoadMap.values()) {
            csuRoad.resetPassably();
        }
        DebugHelper.setGraphEdges(selfId, graph);
        sendMessageRoad(messageManager);
        return this;
    }

    @Override
    public CSUWorldHelper calc() {
        return null;
    }

    /* ----------------------------------- initialize ----------------------------------------- */
    //初始化所有mapInforms
    private void initMapInforms() {
        this.minX = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.minY = Integer.MAX_VALUE;
        this.maxY = Integer.MIN_VALUE;
        Pair<Integer, Integer> pos;
        for (StandardEntity standardEntity : worldInfo.getAllEntities()) {
            pos = worldInfo.getLocation(standardEntity);
            if (pos.first() < this.minX)
                this.minX = pos.first();
            if (pos.second() < this.minY)
                this.minY = pos.second();
            if (pos.first() > this.maxX)
                this.maxX = pos.first();
            if (pos.second() > this.maxY)
                this.maxY = pos.second();
        }
        this.mapDimension = new Dimension(maxX - minX, maxY - minY);
        this.mapWidth = mapDimension.getWidth();
        this.mapHeight = mapDimension.getHeight();
        this.mapDiameter = Math.sqrt(Math.pow(this.mapWidth, 2.0) + Math.pow(this.mapHeight, 2.0));
        initMapUniqueNumber();
        initMapSize();
    }

    private void initMapUniqueNumber() {
        long sum = 0;
        for (StandardEntity building : getBuildingsWithURN(worldInfo)) {
            Building b = (Building) building;
            int[] ap = b.getApexList();
            for (int anAp : ap) {
                if (Long.MAX_VALUE - sum <= anAp) {
                    sum = 0;
                }
                sum += anAp;
            }
        }
        uniqueMapNumber = sum;
    }

    private void initMapSize() {
        double mapWidth = this.getMapDimension().getWidth();
        double mapHeight = this.getMapDimension().getHeight();
        double mapDiagonalLength = Math.hypot(mapWidth, mapHeight);
        double rate = mapDiagonalLength / CSUConstants.MEAN_VELOCITY_DISTANCE;
        if (rate > 60) {
            this.isMapHuge = true;
        } else if (rate > 30) {
            this.isMapMedium = true;
        } else {
            this.isMapSmall = true;
        }
    }

    protected void initWorldCommunicationCondition() {
        if (scenarioInfo.getCommsChannelsCount() == 1) {
            this.setCommunicationLess(true);
            return;
        }
        int size = 0;
        int maxSize = 0;
        String channelBandwidthKey = "comms.channels.NO.bandwidth";
        for (int i = 1; i < scenarioInfo.getCommsChannelsMaxPlatoon(); i++) {
            size = scenarioInfo.getRawConfig().getIntValue(channelBandwidthKey.replace("NO", String.valueOf(i)));
            maxSize = Math.max(maxSize, size);
        }

        if (size <= 256) {
            this.setCommunicationLow(true);
        } else if (size <= 1024) {
            this.setCommunicationMedium(true);
        } else {
            this.setCommunicationHigh(true);
        }
    }

    private void initCsuBuildings() {
        for (StandardEntity entity : getBuildingsWithURN(worldInfo)) {
            CSUBuilding csuBuilding;
            Building building = (Building) entity;
            String xy = building.getX() + "," + building.getY();
            buildingXYMap.put(xy, building);
            csuBuilding = new CSUBuilding(entity, this);

            if (entity instanceof Refuge || entity instanceof PoliceOffice
                    || entity instanceof FireStation || entity instanceof AmbulanceCentre)
                csuBuilding.setInflammable(false);
            this.csuBuildingMap.put(building.getID(), csuBuilding);
        }

        for (CSUBuilding next : csuBuildingMap.values()) {
            Collection<StandardEntity> neighbour = getObjectsInRange(next.getId(), CSUWall.MAX_SAMPLE_DISTANCE);

            for (StandardEntity entity : neighbour) {
                if (entity instanceof Building) {
                    next.addNeighbourBuilding(this.csuBuildingMap.get(entity.getID()));
                }
            }
        }
    }

    private void initCsuRoads() {
        CSURoad csuRoad;
        Road road;
        for (StandardEntity entity : getRoadsWithURN(worldInfo)) {
            road = (Road) entity;
            csuRoad = new CSURoad(road, this);
            this.csuRoadMap.put(entity.getID(), csuRoad);
        }
    }

    private void initCsuHydrants() {
        CSUHydrant csuHydrant;
        for (StandardEntity entity : getHydrantsWithURN(worldInfo)) {
            csuHydrant = new CSUHydrant(entity.getID());
            this.csuHydrantMap.put(entity.getID(), csuHydrant);
        }
    }

    private void initCsuBlockades() {
        CSUBlockade csuBlockade;
        for (StandardEntity entity : worldInfo.getEntitiesOfType(StandardEntityURN.BLOCKADE)) {
            csuBlockade = new CSUBlockade(entity.getID(), this);
            this.csuBlockadeMap.put(entity.getID(), csuBlockade);
        }
    }

    private void initBorderBuildings() {
        // TODO: 3/8/20 改成预计算
        CompositeConvexHull convexHull = new CompositeConvexHull();
        Set<StandardEntity> allEntities = new FastSet<StandardEntity>();
        for (StandardEntity entity : worldInfo.getAllEntities()) {
            if (entity instanceof Building) {
                allEntities.add(entity);
                Pair<Integer, Integer> location = worldInfo.getLocation(entity);
                convexHull.addPoint(location.first(), location.second());
            }
        }
        mapBorderBuildings = PolygonScaler.getMapBorderBuildings(convexHull, allEntities, 0.9);
    }


    private void initMapCenter() {
        double ret;
        int min_x = Integer.MAX_VALUE;
        int max_x = Integer.MIN_VALUE;
        int min_y = Integer.MAX_VALUE;
        int max_y = Integer.MIN_VALUE;

        Collection<StandardEntity> areas = getAreasWithURN(worldInfo);

        long x = 0, y = 0;
        Area result;

        for (StandardEntity entity : areas) {
            Area area1 = (Area) entity;
            x += area1.getX();
            y += area1.getY();
        }

        x /= areas.size();
        y /= areas.size();
        result = (Area) areas.iterator().next();
        for (StandardEntity entity : areas) {
            Area temp = (Area) entity;
            double a = Ruler.getDistance((int) x, (int) y, result.getX(), result.getY());
            double b = Ruler.getDistance((int) x, (int) y, temp.getX(), temp.getY());
            if (a > b) { ///result is the nearest actual area to calculated center
                result = temp;
            }

            if (temp.getX() < min_x) {
                min_x = temp.getX();
            } else if (temp.getX() > max_x)
                max_x = temp.getX();

            if (temp.getY() < min_y) {
                min_y = temp.getY();
            } else if (temp.getY() > max_y)
                max_y = temp.getY();
        }
        ret = (Math.pow((min_x - max_x), 2) + Math.pow((min_y - max_y), 2));
        ret = Math.sqrt(ret);
        this.mapCenter = result;
    }

    /* ----------------------------------------- update --------------------------------------------- */
    private void reflectMessage(MessageManager messageManager) {
        Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
        changedEntities.add(this.agentInfo.getID());
        for (CommunicationMessage message : messageManager.getReceivedMessageList()) {
            if (message instanceof MessageBuilding) {
                MessageBuilding mb = (MessageBuilding) message;
                if (!changedEntities.contains(mb.getBuildingID())) {
                    MessageUtil.reflectMessage(this.worldInfo, mb);
                    if (agentInfo.me() instanceof FireBrigade) {
                        updateBuildingFuelForFireBrigade(getEntity(mb.getBuildingID(), Building.class));
                        int receivedTime;
                        if (mb.isRadio()) {
                            receivedTime = agentInfo.getTime() - 1;
                        } else {
                            receivedTime = agentInfo.getTime() - 5;
                        }
                        Building building = (Building) this.getEntity(mb.getBuildingID());
                        if (getPropertyTime(building.getFierynessProperty()) < receivedTime) {
                            propertyTimeMap.put(building.getFierynessProperty(), receivedTime);
                            propertyTimeMap.put(building.getTemperatureProperty(), receivedTime);
                        }
                        CSUBuilding csuBuilding = getCsuBuilding(mb.getBuildingID());
                        csuBuilding.setLastUpdateTime(receivedTime);
                    }
                }
            } else if (message instanceof MessageRoad) {
                MessageRoad mr = (MessageRoad) message;
                MessageUtil.reflectMessage(this.worldInfo, mr);
                if (mr.isPassable()) {
                    List<MyEdge> myEdgesInArea = graph.getMyEdgesInArea(mr.getRoadID());
                    for (MyEdge edge : myEdgesInArea) {
                        edge.setPassable(true);
                    }
                }
                if (!changedEntities.contains(mr.getRoadID()) && !mr.isPassable() && !mr.isBlockadeDefined() &&
                        !mr.getSenderID().equals(agentInfo.getID())) {
                    List<MyEdge> myEdgesInArea = graph.getMyEdgesInArea(mr.getRoadID());
                    for (MyEdge myEdge : myEdgesInArea) {
                        myEdge.setPassable(false);
                    }
                }
                csuRoadMap.get(mr.getRoadID()).update();
            }
        }
        DebugHelper.setGraphEdges(selfId, graph);
    }

    /**
     * @Description: 发送所有MyEdges都不可通的road
     * @Author: Guanyu-Cai
     * @Date: 3/22/20
     */
    private void sendMessageRoad(MessageManager messageManager) {
        //发送roadSeen中每条road
        for (EntityID id : roadsSeen) {
            Road road = (Road) worldInfo.getEntity(id);
            List<MyEdge> myEdgesInArea = graph.getMyEdgesInArea(id);
            boolean passable = false;
            for (MyEdge myEdge : myEdgesInArea) {
                if (myEdge.isPassable()) {
                    passable = true;
                    break;
                }
            }
            if (!passable) {
                if (!lastSendTime.containsKey(id) || agentInfo.getTime() - lastSendTime.get(id) > 5) {
                    messageManager.addMessage(new MessageRoad(true, road, null, false, false));
                    lastSendTime.put(id, agentInfo.getTime());
                }
            }
        }
    }

    /**
     * Update the building fuel and energy for the given building for
     * FireBrigade Agent.
     *
     * @param building the target building currently within the eye shot of this
     *                 FireBrigade Agent
     */
    private void updateBuildingFuelForFireBrigade(Building building) {
        CSUBuilding csuBuilding = this.getCsuBuilding(building.getID());
        csuBuilding.setVisible(true);
        if (building.isFierynessDefined() && building.isTemperatureDefined()) {
            int temperature = building.getTemperature();
            csuBuilding.setEnergy(temperature * csuBuilding.getCapacity(), "updateBuildingFuelForFireBrigade");
            switch (building.getFieryness()) {
                case 0:
                    csuBuilding.setFuel(csuBuilding.getInitialFuel());
                    if (csuBuilding.getEstimatedTemperature() >= csuBuilding.getIgnitionPoint()) {
                        csuBuilding.setEnergy(csuBuilding.getIgnitionPoint() / 2, "updateBuildingFuelForFireBrigade");
                    }
                    break;
                case 1:
                    if (csuBuilding.getFuel() < csuBuilding.getInitialFuel() * 0.66) {
                        csuBuilding.setFuel((float) (csuBuilding.getInitialFuel() * 0.75));
                    } else if (csuBuilding.getFuel() == csuBuilding.getInitialFuel()) {
                        csuBuilding.setFuel((float) (csuBuilding.getInitialFuel() * 0.90));
                    }
                    break;
                case 2:
                    if (csuBuilding.getFuel() < csuBuilding.getInitialFuel() * 0.33
                            || csuBuilding.getFuel() > csuBuilding.getInitialFuel() * 0.66) {
                        csuBuilding.setFuel((float) (csuBuilding.getInitialFuel() * 0.50));
                    }
                    break;
                case 3:
                    if (csuBuilding.getFuel() < csuBuilding.getInitialFuel() * 0.01
                            || csuBuilding.getFuel() > csuBuilding.getInitialFuel() * 0.33) {
                        csuBuilding.setFuel((float) (csuBuilding.getInitialFuel() * 0.15));
                    }
                    break;
                case 8:
                    csuBuilding.setFuel(0);
                    break;
                default:
                    break;
            }
        }
    }

    /* ----------------------------------- getEntitiesOfType and getEntity ------------------------------------- */

    /**
     * Returns a collection of entity specified by <b>StandardEntityURN urn<b>.
     * ANd then cast those entity to the specified type <b>&lt;T extends
     * StandardEntity&gt;</b>
     */
    public <T extends StandardEntity> java.util.List<T> getEntitiesOfType(Class<T> c, StandardEntityURN urn) {
        Collection<StandardEntity> entities = worldInfo.getEntitiesOfType(urn);
        java.util.List<T> list = new ArrayList<T>();
        for (StandardEntity entity : entities) {
            if (c.isInstance(entity)) {
                list.add(c.cast(entity));
            }
        }
        return list;
    }

    /**
     * Get all entities of specified types stores in this world model.
     */
    public Collection<StandardEntity> getEntitiesOfType(EnumSet<StandardEntityURN> urns) {
        Collection<StandardEntity> res = new HashSet<StandardEntity>();
        for (StandardEntityURN urn : urns) {
            res.addAll(worldInfo.getEntitiesOfType(urn));
        }
        return res;
    }

    /**
     * Get an object of Entity according to its ID and cast this object to
     * <b>&lt;T extends StandardEntity&gt;</b>.
     */
    public <T extends StandardEntity> T getEntity(EntityID id, Class<T> c) {
        StandardEntity entity;
        entity = worldInfo.getEntity(id);
        if (c.isInstance(entity)) {
            T castedEntity = c.cast(entity);

            return castedEntity;
        } else {
            return null;
        }
    }

    public StandardEntity getEntity(EntityID id) {
        return worldInfo.getEntity(id);
    }

    public CSUBuilding getCsuBuilding(StandardEntity entity) {
        return this.csuBuildingMap.get(entity.getID());
    }

    public CSUBuilding getCsuBuilding(EntityID entityId) {
        return this.csuBuildingMap.get(entityId);
    }

    public CSURoad getCsuRoad(StandardEntity entity) {
        return this.csuRoadMap.get(entity.getID());
    }

    public CSURoad getCsuRoad(EntityID entityId) {
        return this.csuRoadMap.get(entityId);
    }

    public Collection<CSUBuilding> getCsuBuildings() {
        return this.csuBuildingMap.values();
    }

    public Map<EntityID, CSUBuilding> getCsuBuildingMap() {
        return csuBuildingMap;
    }

    public Map<EntityID, CSURoad> getCSURoadMap() {
        return csuRoadMap;
    }

    public CSUHydrant getCsuHydrant(EntityID entityid) {
        return this.csuHydrantMap.get(entityid);
    }

    /* ---------------------------------------------------------------------------------------------------------- */

    public boolean isMapHuge() {
        return this.isMapHuge;
    }

    public boolean isMapMedium() {
        return this.isMapMedium;
    }

    public boolean isMapSmall() {
        return this.isMapSmall;
    }

    ///got from entities' locations
    public Point getMapCenterPoint() {    ///should be >> 1
        return new Point((int) mapWidth >> 2, (int) mapHeight >> 2);
    }

    public java.util.List<Building> findNearBuildings(Building centerbuilding, int distance) {
        java.util.List<Building> result;
        Collection<StandardEntity> allObjects;
        int radius;

        Rectangle rect = centerbuilding.getShape().getBounds();
        radius = (int) (distance + rect.getWidth() + rect.getHeight());

        allObjects = worldInfo.getObjectsInRange(centerbuilding, radius);
        result = new ArrayList<Building>();
        for (StandardEntity next : allObjects) {
            if (next instanceof Building) {
                Building building;

                building = (Building) next;
                if (!building.equals(centerbuilding)) {
                    if (Ruler.getDistance(centerbuilding, building) < distance) {
                        result.add(building);
                    }
                }
            }
        }
        return result;
    }

    /* -------------------------------------------------------------------------------------------------------- */
    ///what occurs using Point
    public int distance(java.awt.Point p1, java.awt.Point p2) {
        return distance(p1.x, p1.y, p2.x, p2.y);
    }

    public int distance(int x1, int y1, int x2, int y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return (int) Math.hypot(dx, dy);
    }

    public Line getLine(Area node1, Area node2) {
        int x1 = node1.getX();
        int y1 = node1.getY();
        int x2 = node2.getX();
        int y2 = node2.getY();
        return new Line(x1, y1, x2, y2);
    }

    public Area getPositionFromCoordinates(int x, int y) {
        for (StandardEntity entity : getObjectsInRange(x, y, 1000)) {
            if (entity instanceof Area) {
                Area area = (Area) entity;
                if (area.getShape().contains(x, y)) {
                    return area;
                }
            }
        }
        return null;
    }

    public StandardEntity getPositionFromCoordinates(Pair<Integer, Integer> coordinate) {
        return getPositionFromCoordinates(coordinate.first(), coordinate.second());
    }

    public Set<StandardEntity> getNeighbours(StandardEntity e, int Ext) {
        Set<StandardEntity> Neighbours = new HashSet<StandardEntity>();
        if (e instanceof Building) {
            for (EntityID tmp2 : ((Building) e).getNeighbours()) {
                Neighbours.add(worldInfo.getEntity(tmp2));
            }
            if (((Building) e).isEdgesDefined()) {
                Ext *= 1000;
                java.util.List<Edge> Edges = new ArrayList<Edge>();
                Edges = ((Building) e).getEdges();
                Polygon ExtArea = new Polygon();
                Polygon baseArea = new Polygon();
                int n = 1;
                Point2D.Double tmp1 = new Point2D.Double();
                Point2D.Double tmp2 = new Point2D.Double();
                Point2D.Double tmp3 = new Point2D.Double();
                Point2D.Double tmp4 = new Point2D.Double();
                Point2D.Double tmp5 = new Point2D.Double();
                for (Edge Ee : Edges) {
                    baseArea.addPoint(Ee.getStartX(), Ee.getStartY());
                }
                for (Edge Ee : Edges) {
                    for (Edge Ee1 : Edges) {
                        if (Ee.getStart().equals(Ee1.getEnd())) {
                            tmp1.setLocation
                                    (
                                            Ee.getStartX() + (n * (Ee.getEndX() - Ee.getStartX())) / Math.hypot((double) Ee.getEndX() - (double) Ee.getStartX(), (double) Ee.getEndY() - (double) Ee.getStartY())
                                            ,
                                            Ee.getStartY() + (n * (Ee.getEndY() - Ee.getStartY())) / Math.hypot((double) Ee.getEndX() - (double) Ee.getStartX(), (double) Ee.getEndY() - (double) Ee.getStartY())
                                    );
                            tmp2.setLocation
                                    (
                                            Ee1.getEndX() + (n * (Ee1.getStartX() - Ee1.getEndX())) / Math.hypot((double) Ee1.getStartX() - (double) Ee1.getEndX(), (double) Ee1.getStartY() - (double) Ee1.getEndY())
                                            ,
                                            Ee1.getEndY() + (n * (Ee1.getStartY() - Ee1.getEndY())) / Math.hypot((double) Ee1.getStartX() - (double) Ee1.getEndX(), (double) Ee1.getStartY() - (double) Ee1.getEndY())
                                    );

                            tmp3.setLocation
                                    (
                                            (tmp1.x + tmp2.x) / 2
                                            ,
                                            (tmp1.y + tmp2.y) / 2
                                    );
                            tmp4.setLocation(Ee.getStartX(), Ee.getStartY());
                            if (tmp3.x == tmp4.x && tmp3.y == tmp4.y) {
                                continue;
                            } else if (baseArea.contains(tmp3)) {
                                tmp5.setLocation(
                                        tmp4.x + (Ext * (tmp4.x - tmp3.x) / Math.hypot(tmp4.x - tmp3.x, tmp4.y - tmp3.y))
                                        ,
                                        tmp4.y + (Ext * (tmp4.y - tmp3.y) / Math.hypot(tmp4.x - tmp3.x, tmp4.y - tmp3.y)));

                            } else if (!baseArea.contains(tmp3)) {
                                tmp5.setLocation(
                                        tmp4.x + (Ext * (tmp3.x - tmp4.x) / Math.hypot(tmp3.x - tmp4.x, tmp3.y - tmp4.y))
                                        ,
                                        tmp4.y + (Ext * (tmp3.y - tmp4.y) / Math.hypot(tmp3.x - tmp4.x, tmp3.y - tmp4.y)));
                            }
                            ExtArea.addPoint((int) tmp5.x, (int) tmp5.y);
                        }
                    }
                }
                for (StandardEntity checker : worldInfo.getObjectsInRange(e, 100 * 1000)) {
                    if (checker instanceof Building) {
                        if (((Building) checker).isEdgesDefined()) {
                            for (Edge Edger : ((Building) checker).getEdges()) {
                                if (ExtArea.contains(Edger.getStartX(), Edger.getStartY())) {
                                    Neighbours.add(checker);
                                } else if (ExtArea.contains((Edger.getStartX() + Edger.getEndX()) / 2, (Edger.getStartY() + Edger.getEndY()) / 2)) {
                                    Neighbours.add(checker);
                                }
                            }
                        }
                    }
                }
            }
        } else if (e instanceof Road) {
            for (EntityID Neighs : ((Road) e).getNeighbours()) {
                if (worldInfo.getEntity(Neighs) instanceof Road) {
                    Neighbours.add(worldInfo.getEntity(Neighs));
                } else if (worldInfo.getEntity(Neighs) instanceof Building) {
                    Neighbours.add(worldInfo.getEntity(Neighs));
                }
            }
        }
        return Neighbours;
    }

    /* ----------------------------------------- getters and setters ------------------------------------------- */

    public int getDistance(EntityID first, EntityID second) {
        return worldInfo.getDistance(first, second);
    }

    public int getDistance(StandardEntity first, StandardEntity second) {
        return worldInfo.getDistance(first, second);
    }

    public Collection<StandardEntity> getObjectsInRange(EntityID entityID, int distance) {
        return worldInfo.getObjectsInRange(entityID, distance);
    }

    public Collection<StandardEntity> getObjectsInRange(int x, int y, int range) {
        return worldInfo.getObjectsInRange(x, y, range);
    }

    public Collection<Blockade> getBlockadesInRange(int range) {
        Collection<Blockade> result = new HashSet<>();
        Collection<StandardEntity> objectsInRange = worldInfo.getObjectsInRange(agentInfo.getID(), range);
        for (StandardEntity entity : objectsInRange) {
            if (entity instanceof Road) {
                CSURoad csuRoad = csuRoadMap.get(entity.getID());
                List<CSUBlockade> csuBlockades = csuRoad.getCsuBlockades();
                for (CSUBlockade csuBlockade : csuBlockades) {
                    if (Ruler.getDistance(csuBlockade.getPolygon(), getSelfLocation()) <= range) {
                        result.add(csuBlockade.getSelfBlockade());
                    }
                }
            }
        }
        return result;
    }

    //获取所有neighbourRoad,包括自己所在的路
    public Collection<CSURoad> getNeighborRoads() {
        HashSet<CSURoad> results = new HashSet<>();
        Area selfArea = (Area) getSelfPosition();
        List<EntityID> neighbours = selfArea.getNeighbours();
        for (EntityID id : neighbours) {
            CSURoad csuRoad = csuRoadMap.get(id);
            if (csuRoad != null) {
                results.add(csuRoad);
            }
        }
        return results;
    }

    //获取最近的road
    public CSURoad getNearestNeighborRoad() {
        Collection<CSURoad> neighborRoads = getNeighborRoads();
        double minDistance = Double.MAX_VALUE;
        CSURoad nearestRoad = null;
        for (CSURoad road : neighborRoads) {
            Polygon polygon = road.getPolygon();
            double distance = Ruler.getDistance(polygon, getSelfLocation());
            if (distance < minDistance) {
                minDistance = distance;
                nearestRoad = road;
            }
        }
        return nearestRoad;
    }

    public Set<EntityID> getBurningBuildings() {
        return burningBuildings;
    }

    public Set<EntityID> getCollapsedBuildings() {
        return collapsedBuildings;
    }

    public StandardEntity getSelfPosition() {
        if (worldInfo.getEntity(agentInfo.getID()) instanceof Building) {
            return selfBuilding;
        } else {
            return agentInfo.getPositionArea();
        }
    }

    public EntityID getSelfPositionId() {
        return getSelfPosition().getID();
    }

    public Pair<Integer, Integer> getSelfLocation() {
        return worldInfo.getLocation(agentInfo.me());
    }

    public Human getSelfHuman() {
        return selfHuman;
    }

    public Building getSelfBuilding() {
        return selfBuilding;
    }

    public Pair<Integer, Integer> getLocation(StandardEntity entity) {
        return worldInfo.getLocation(entity);
    }

    public Pair<Integer, Integer> getLocation(EntityID entityID) {
        return worldInfo.getLocation(entityID);
    }

    public int getTime() {
        return agentInfo.getTime();
    }

    /**
     * Get all border building of this world.
     *
     * @return all border building of this world
     */
    public Set<StandardEntity> getBorderBuildings() {
        return this.mapBorderBuildings;
    }

    public Dimension getMapDimension() {
        return this.mapDimension;
    }

    public double getMapWidth() {
        return this.mapWidth;
    }

    public double getMapHeight() {
        return this.mapHeight;
    }

    public boolean isCommunicationLess() {
        return this.communicationLess;
    }

    public void setCommunicationLess(boolean communicationLess) {
        this.communicationLess = communicationLess;
    }

    public boolean isCommunicationLow() {
        return this.communicationLow;
    }

    public void setCommunicationLow(boolean communicationLow) {
        this.communicationLow = communicationLow;
    }

    public boolean isCommunicationMedium() {
        return this.communicationMedium;
    }

    public void setCommunicationMedium(boolean communicationMedium) {
        this.communicationMedium = communicationMedium;
    }

    public boolean isCommunicationHigh() {
        return this.communicationHigh;
    }

    public void setCommunicationHigh(boolean communicationHigh) {
        this.communicationHigh = communicationHigh;
    }

    public Set<EntityID> getStuckedAgents() {
        return this.stuckAgents;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public AgentInfo getAgentInfo() {
        return agentInfo;
    }

    public WorldInfo getWorldInfo() {
        return worldInfo;
    }

    public ScenarioInfo getScenarioInfo() {
        return scenarioInfo;
    }

    public ConfigConstants getConfig() {
        return config;
    }

    public GraphHelper getGraph() {
        return graph;
    }

    public Set<EntityID> getRoadsSeen() {
        return roadsSeen;
    }

    public Set<EntityID> getBuildingsSeen() {
        return buildingsSeen;
    }

    public Set<EntityID> getCiviliansSeen() {
        return civiliansSeen;
    }

    public Set<EntityID> getFireBrigadesSeen() {
        return fireBrigadesSeen;
    }

    public Set<EntityID> getBlockadesSeen() {
        return blockadesSeen;
    }

    public static Collection<StandardEntity> getBuildingsWithURN(WorldInfo worldInfo) {
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.REFUGE,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.GAS_STATION);
    }

    public static Collection<StandardEntity> getHydrantsWithURN(WorldInfo worldInfo) {
        return worldInfo.getEntitiesOfType(StandardEntityURN.HYDRANT);
    }

    public static Collection<StandardEntity> getGasStationsWithUrn(WorldInfo worldInfo) {
        return worldInfo.getEntitiesOfType(StandardEntityURN.GAS_STATION);
    }

    public static Collection<StandardEntity> getRefugesWithUrn(WorldInfo worldInfo) {
        return worldInfo.getEntitiesOfType(StandardEntityURN.REFUGE);
    }

    public static Collection<StandardEntity> getAreasWithURN(WorldInfo worldInfo) {
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.REFUGE,
                StandardEntityURN.ROAD,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.HYDRANT,
                StandardEntityURN.GAS_STATION);
    }

    public static Collection<StandardEntity> getHumansWithURN(WorldInfo worldInfo) {
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.CIVILIAN,
                StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE,
                StandardEntityURN.AMBULANCE_TEAM);
    }

    public static Collection<StandardEntity> getAgentsWithURN(WorldInfo worldInfo) {
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE,
                StandardEntityURN.AMBULANCE_TEAM,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.AMBULANCE_CENTRE);
    }

    public static Collection<StandardEntity> getPlatoonAgentsWithURN(WorldInfo worldInfo) {
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE,
                StandardEntityURN.AMBULANCE_TEAM);
    }

    public static Collection<StandardEntity> getRoadsWithURN(WorldInfo worldInfo) {
        return worldInfo.getEntitiesOfType(StandardEntityURN.ROAD, StandardEntityURN.HYDRANT);
    }

    public List<StandardEntity> getEntities(Set<EntityID> entityIDs) {
        List<StandardEntity> result = new ArrayList<StandardEntity>();
        for (EntityID next : entityIDs) {
            result.add(getEntity(next));
        }
        return result;
    }

    public List<StandardEntity> getEntities(List<EntityID> entityIDs) {
        List<StandardEntity> result = new ArrayList<StandardEntity>();
        for (EntityID next : entityIDs) {
            result.add(getEntity(next));
        }
        return result;
    }

    /**
     * 初始化一个area可以从哪看到和在一个area可以看到哪些area
     */
    public void processVisibilityData(boolean isPrecompute) {

        String visibleFromFileName = CSUConstants.PRECOMPUTE_DIRECTORY + this.getUniqueMapNumber() + ".vif";
        String observableAreasFileName = CSUConstants.PRECOMPUTE_DIRECTORY + this.getUniqueMapNumber() + ".oba";

        if (new File(visibleFromFileName).exists() && new File(observableAreasFileName).exists() &&
                !isPrecompute) {
            Thread loader = new Thread() {
                @Override
                public void run() {
                    loadVisibilityData(visibleFromFileName, observableAreasFileName);
                }
            };
            loader.start();
        } else {
            FileEntityIDMap visibleFrom = new FileEntityIDMap();
            FileEntityIDMap observableAreas = new FileEntityIDMap();

            CSULineOfSightPerception lineOfSightPerception = new CSULineOfSightPerception(this);


            Collection<CSURoad> allRoads = this.getCSURoads();
            Collection<CSUBuilding> allBuildings = this.getCSUBuildings();

            for (CSURoad road : allRoads) {
                road.setObservableAreas(lineOfSightPerception.getVisibleAreas(road.getId()));
                observableAreas.put(road.getId().getValue(), Util.fetchIdValueFromElementIds(road.getObservableAreas()));
            }
            for (CSUBuilding building : allBuildings) {
                building.setObservableAreas(lineOfSightPerception.getVisibleAreas(building.getId()));
                observableAreas.put(building.getId().getValue(), Util.fetchIdValueFromElementIds(building.getObservableAreas()));
            }

            for (CSURoad road1 : allRoads) {
                for (CSURoad road2 : allRoads) {
                    if (road1.equals(road2)) continue;
                    if (road2.getObservableAreas().contains(road1.getId())) {
                        road1.getVisibleFrom().add(road2.getId());

                    }
                }
                for (CSUBuilding building : allBuildings) {
                    if (building.getObservableAreas().contains(road1.getId())) {
                        road1.getVisibleFrom().add(building.getId());
                    }
                }
                visibleFrom.put(road1.getId().getValue(), Util.fetchIdValueFromElementIds(road1.getVisibleFrom()));
            }
            for (CSUBuilding building1 : allBuildings) {
                for (CSUBuilding building2 : allBuildings) {
                    if (building1.equals(building2)) continue;
                    if (building2.getObservableAreas().contains(building1.getId())) {
                        building1.getVisibleFrom().add(building2.getId());

                    }
                }
                for (CSURoad road : allRoads) {
                    if (road.getObservableAreas().contains(building1.getId())) {
                        building1.getVisibleFrom().add(road.getId());
                    }
                }
                visibleFrom.put(building1.getId().getValue(), Util.fetchIdValueFromElementIds(building1.getVisibleFrom()));
            }

            try {
                if (isPrecompute) {
                    Util.writeObject(observableAreas, observableAreasFileName);
                    Util.writeObject(visibleFrom, visibleFromFileName);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Collection<CSUBuilding> getCSUBuildings() {
        return csuBuildingMap.values();
    }

    private Collection<CSURoad> getCSURoads() {
        return csuRoadMap.values();
    }

    private void loadVisibilityData(String visibleFromFileName, String observableAreasFileName) {
        try {
            FileEntityIDMap visibleFrom = (FileEntityIDMap) Util.readObject(visibleFromFileName);
            FileEntityIDMap observableArea = (FileEntityIDMap) Util.readObject(observableAreasFileName);
            for (Integer next : visibleFrom.keySet()) {
                CSURoad road = this.getCsuRoad(new EntityID(next));
                if (road != null) {
                    road.setVisibleFrom(Util.fetchEntityIdFromIdValues(visibleFrom.get(next)));
                } else {
                    CSUBuilding building = this.getCsuBuilding(new EntityID(next));
                    building.setVisibleFrom(Util.fetchEntityIdFromIdValues(visibleFrom.get(next)));
                }
            }
            for (Integer next : observableArea.keySet()) {
                CSURoad road = this.getCsuRoad(new EntityID(next));
                if (road != null) {
                    road.setObservableAreas(new ArrayList<>(Util.fetchEntityIdFromIdValues(observableArea.get(next))));
                } else {
                    CSUBuilding building = this.getCsuBuilding(new EntityID(next));
                    building.setObservableAreas(new ArrayList<>(Util.fetchEntityIdFromIdValues(observableArea.get(next))));
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public long getUniqueMapNumber() {
        return uniqueMapNumber;
    }


    public void setSearchTarget(EntityID searchTarget) {
        this.searchTarget = searchTarget;
    }

    public EntityID getSearchTarget() {
        return searchTarget;
    }

    public Building getBuildingInPoint(int x, int y) {
        String xy = x + "," + y;
        return buildingXYMap.get(xy);
    }

    public int getEntityLastUpdateTime(StandardEntity entity) {
        int maxTime = Integer.MIN_VALUE;
        for (Property property : entity.getProperties()) {
            Integer value = getPropertyTime(property);
            if (value > maxTime) {
                maxTime = value;
            }
        }

        return maxTime;
    }

    public Integer getPropertyTime(Property property) {
        Integer integer = propertyTimeMap.get(property);
        if (integer == null) {
            return 0;
        }
        return integer;
    }

    public Set<Building> getBuildingsInRange(EntityID entityID, int distance) {
        Set<Building> result = new HashSet<>();
        for (StandardEntity e : getObjectsInRange(entityID, distance)) {
            if (e instanceof Building) {
                result.add((Building) e);
            }
        }
        return result;
    }
}