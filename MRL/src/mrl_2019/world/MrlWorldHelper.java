package mrl_2019.world;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.information.MessageBuilding;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.communication.CommunicationMessage;
import adf.component.module.AbstractModule;
import mrl_2019.MRLConstants;
import mrl_2019.SampleModuleKey;
import mrl_2019.complex.firebrigade.BorderEntities;
import mrl_2019.complex.firebrigade.BuildingProperty;
import mrl_2019.complex.firebrigade.MrlFireBrigadeWorld;
import mrl_2019.util.Util;
import mrl_2019.world.entity.*;
import mrl_2019.world.helper.*;
import mrl_2019.world.routing.graph.GraphModule;
import rescuecore2.messages.Command;
import rescuecore2.misc.Handy;
import rescuecore2.misc.Pair;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.*;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * @author Mahdi
 */
public class MrlWorldHelper extends AbstractModule {

    private int lastUpdateTime = -1;

    protected Set<Road> roadsSeen;
    protected Set<Building> buildingsSeen;
    protected Set<Civilian> civiliansSeen;
    protected Set<FireBrigade> fireBrigadesSeen;
    protected Set<Blockade> blockadesSeen;
    protected Set<StandardEntity> roads;
    protected Collection<StandardEntity> civilians;
    protected Set<StandardEntity> buildings;
    protected Set<StandardEntity> areas;
    protected Set<StandardEntity> humans;
    protected Set<StandardEntity> agents;
    protected List<StandardEntity> fireBrigades;
    protected Set<StandardEntity> platoonAgents;
    protected Set<StandardEntity> hydrants;
    protected Set<StandardEntity> gasStations;
    protected Set<StandardEntity> refuges;
    protected Set<MrlRoad> mrlRoads;
    protected Map<EntityID, MrlRoad> mrlRoadIdMap;
    protected Map<EntityID, MrlBuilding> mrlBuildingIdMap;
    protected GraphModule graph;
    protected Paths paths;
    protected List<IHelper> helpers = new ArrayList<>();
    protected Map<EntityID, EntityID> entranceRoads;

    public float rayRate = 0.0025f;
    protected Set<MrlBuilding> mrlBuildings;
    protected Map<EntityID, MrlBuilding> tempBuildingsMap;
    protected Map<String, Building> buildingXYMap;
    protected Map<String, Road> roadXYMap;

    protected Set<EntityID> unvisitedBuildings = new HashSet<>();
    protected Set<EntityID> visitedBuildings = new HashSet<>();
    protected int worldTotalArea;
    protected List<MrlBuilding> shouldCheckInsideBuildings = new ArrayList<>();
    protected Human selfHuman;
    protected StandardAgent self;
    protected Building selfBuilding;
    protected PropertyHelper propertyHelper;
    protected int lastAfterShockTime = 0;
    protected int aftershockCount = 0;
    protected Set<EntityID> burningBuildings;
    protected Set<EntityID> emptyBuildings;
    protected Set<EntityID> thisCycleEmptyBuildings;

    protected boolean CommunicationLess = false;
    protected boolean isCommunicationLow = false;
    protected boolean isCommunicationMedium = false;
    protected boolean isCommunicationHigh = false;
    public boolean isWaterRefillRateInHydrantSet;
    public boolean isWaterRefillRateInRefugeSet;

    protected Set<MrlBuilding> estimatedBurningBuildings = new HashSet<>();
    private int lastUpdateHydrants = -1;
    private Set<StandardEntity> availableHydrants = new HashSet<>();
    protected Long uniqueMapNumber;
    protected Set<EntityID> borderBuildings;
    protected ScenarioInfo scenarioInfo;
    protected AgentInfo agentInfo;
    protected WorldInfo worldInfo;
    protected ModuleManager moduleManager;
    //    public boolean shouldPrecompute = false;
    public BorderEntities borderFinder;
    protected int minX, minY, maxX, maxY;
    protected boolean isMapHuge = false;
    protected boolean isMapMedium = false;
    protected boolean isMapSmall = false;
    private Map<EntityID, BuildingProperty> sentBuildingMap;
    private Set<EntityID> possibleBurningBuildings;
    private Set<EntityID> allCivilians;

    private Set<EntityID> heardCivilians;
    private EntityID exploreTarget;

    public MrlWorldHelper(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.worldInfo = wi;
        this.agentInfo = ai;
        this.scenarioInfo = si;
        this.moduleManager = moduleManager;
        this.developData = developData;
        this.roadsSeen = new HashSet<>();
        buildingsSeen = new HashSet<>();
        civiliansSeen = new HashSet<>();
        blockadesSeen = new HashSet<>();
        fireBrigadesSeen = new HashSet<>();
        mrlRoads = new HashSet<>();
        burningBuildings = new HashSet<>();
        emptyBuildings = new HashSet<>();
        thisCycleEmptyBuildings = new HashSet<>();
        civilians = new HashSet<>();
        allCivilians = new HashSet<>();

        buildings = new HashSet<>(getBuildingsWithURN());
        roads = new HashSet<>(getRoadsWithURN());
        areas = new HashSet<>(getAreasWithURN());
        humans = new HashSet<>(getHumansWithURN());
        agents = new HashSet<>(getAgentsWithURN());
        fireBrigades = new ArrayList<>(getFireBrigades());
        platoonAgents = new HashSet<>(getPlatoonAgentsWithURN());
        hydrants = new HashSet<>(getHydrantsWithURN());
        gasStations = new HashSet<>(getGasStationsWithUrn());
        refuges = new HashSet<>(getRefugesWithUrn());
        graph = new GraphModule(ai, wi, si, moduleManager, developData);
        mrlRoadIdMap = new HashMap<>();
        mrlBuildingIdMap = new HashMap<>();
        entranceRoads = new HashMap<>();
        buildingXYMap = new HashMap<>();
        roadXYMap = new HashMap<>();
        this.sentBuildingMap = new HashMap<>();
        possibleBurningBuildings = new HashSet<>();
        heardCivilians = new HashSet<>();

        createUniqueMapNumber();

        setWorldCommunicationCondition();


        helpers.add(new PropertyHelper(this));
        helpers.add(new BuildingHelper(this, scenarioInfo, agentInfo, worldInfo));
//        helpers.add(new AreaHelper(this));
        helpers.add(new RoadHelper(this, ai, wi, si, moduleManager, developData));
//        helpers.add(new EdgeHelper());
//        helpers.add(new HumanHelper(this));
        helpers.add(new CivilianHelper(this, si, ai, wi));
        helpers.add(new VisibilityHelper(this, si, ai, wi));

        if (worldInfo.getEntity(agentInfo.getID()) instanceof Building) {
            selfBuilding = (Building) worldInfo.getEntity(agentInfo.getID());
//            this.centre = (MrlCentre) self;
        } else {
//            this.platoonAgent = (MrlPlatoonAgent) self;
            selfHuman = (Human) worldInfo.getEntity(agentInfo.getID());
        }

        propertyHelper = this.getHelper(PropertyHelper.class);

        calculateMapDimensions();


        StandardEntity entity = worldInfo.getEntity(agentInfo.getID());
        if (entity instanceof FireBrigade || entity instanceof PoliceForce || entity instanceof AmbulanceTeam) {
//            System.err.println("calling createMrlBuildings .......");
            createMrlBuildings();
        }


        paths = new Paths(this, worldInfo, agentInfo, scenarioInfo);

        createMrlRoads();

        verifyMap();

        borderBuildings = new HashSet<>();
        borderFinder = new BorderEntities(this);

        helpers.forEach(IHelper::init);

    }

    private void verifyMap() {

        double mapDimension = Math.hypot(getMapWidth(), getMapHeight());

        double rate = mapDimension / MRLConstants.MEAN_VELOCITY_OF_MOVING;

        if (rate > 60) {
            isMapHuge = true;
        } else if (rate > 30) {
            isMapMedium = true;
        } else {
            isMapSmall = true;
        }


    }

    private void calculateMapDimensions() {
        this.minX = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.minY = Integer.MAX_VALUE;
        this.maxY = Integer.MIN_VALUE;
        Pair<Integer, Integer> pos;
        List<StandardEntity> invalidEntities = new ArrayList<>();
        for (StandardEntity standardEntity : worldInfo.getAllEntities()) {
            pos = worldInfo.getLocation(standardEntity);
            if (pos.first() == Integer.MIN_VALUE || pos.first() == Integer.MAX_VALUE || pos.second() == Integer.MIN_VALUE || pos.second() == Integer.MAX_VALUE) {
                invalidEntities.add(standardEntity);
                continue;
            }
            if (pos.first() < this.minX)
                this.minX = pos.first();
            if (pos.second() < this.minY)
                this.minY = pos.second();
            if (pos.first() > this.maxX)
                this.maxX = pos.first();
            if (pos.second() > this.maxY)
                this.maxY = pos.second();
        }
        if (!invalidEntities.isEmpty()) {
            System.out.println("##### WARNING: There is some invalid entities ====> " + invalidEntities.size());
        }
    }

    private void createUniqueMapNumber() {
        long sum = 0;
        for (StandardEntity building : getBuildings()) {
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

//        System.out.println("Unique Map Number=" + uniqueMapNumber);
    }

    public synchronized static MrlWorldHelper load(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, DevelopData developData) {
        MrlWorldHelper worldHelper = null;
        try {
//            System.err.println(" getting MrlWorldHelper .......");
            if (worldInfo.getEntity(agentInfo.getID()) instanceof FireBrigade) {
                worldHelper = moduleManager.getModule(SampleModuleKey.AGENT_WORLD, "mrl_2019.complex.firebrigade.MrlFireBrigadeWorld");
            } else {
                worldHelper = moduleManager.getModule(SampleModuleKey.AGENT_WORLD, "mrl_2019.world.MrlWorldHelper");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            worldHelper = null;
        }
        if (worldHelper == null) {
//            System.err.println(" creating MrlWorldHelper .......");
            if (worldInfo.getEntity(agentInfo.getID()) instanceof FireBrigade) {
                worldHelper = new MrlFireBrigadeWorld(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
            } else {
                worldHelper = new MrlWorldHelper(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
            }
        }
        return worldHelper;
    }

    protected Area centerOfMap;
    protected double pole = 0;

    public Area getCenterOfMap() {
        if (centerOfMap != null) {
            return centerOfMap;
        }

        double ret;
        int min_x = Integer.MAX_VALUE;
        int max_x = Integer.MIN_VALUE;
        int min_y = Integer.MAX_VALUE;
        int max_y = Integer.MIN_VALUE;

        Collection<StandardEntity> areas = getAreas();

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
            if (Util.distance((int) x, (int) y, result.getX(), result.getY()) > Util.distance((int) x, (int) y, temp.getX(), temp.getY())) {
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
        ret = (Math.pow((min_x - max_x), 2) +
                Math.pow((min_y - max_y), 2));
        ret = Math.sqrt(ret);
        pole = ret;
        centerOfMap = result;

        return result;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public Paths getPaths() {
        return paths;
    }

    @Override
    public MrlWorldHelper updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        if (lastUpdateTime == agentInfo.getTime()) {
            return this;
        }
        lastUpdateTime = agentInfo.getTime();
        reflectMessage(messageManager);

        roadsSeen.clear();
        buildingsSeen.clear();
        blockadesSeen.clear();
        civiliansSeen.clear();
        fireBrigadesSeen.clear();
        civilians = worldInfo.getEntitiesOfType(StandardEntityURN.CIVILIAN);

        civilians.forEach(civEntity -> allCivilians.add(civEntity.getID()));

        Collection<Command> heard = agentInfo.getHeard();
        heardCivilians.clear();
        if (heard != null) {
            heard.forEach(next -> {
                if (next instanceof AKSpeak && ((AKSpeak) next).getChannel() == 0 && !next.getAgentID().equals(agentInfo.getID())) {// say messages
                    AKSpeak speak = (AKSpeak) next;
                    Collection<EntityID> platoonIDs = Handy.objectsToIDs(getAgents());
                    if (!platoonIDs.contains(speak.getAgentID())) {//Civilian message
                        processCivilianCommand(speak);
                        allCivilians.add(speak.getAgentID());
                    }
                }
            });
        }

        worldInfo.getChanged().getChangedEntities().forEach(changedId -> {
            StandardEntity entity = worldInfo.getEntity(changedId);
            if (entity instanceof Civilian) {
                Civilian civilian = (Civilian) entity;
                civiliansSeen.add(civilian);
            } else if (entity instanceof Building) {
                Building building = (Building) entity;
                //Checking for AFTER SHOCK occurrence
                Property brokennessProperty = building.getProperty(StandardPropertyURN.BROKENNESS.toString());
                if (brokennessProperty.isDefined()) {
                    int newBrokennessValue = -1;
                    for (Property p : worldInfo.getChanged().getChangedProperties(building.getID())) {
                        if (p.getURN().endsWith(brokennessProperty.getURN())) {
                            newBrokennessValue = (Integer) p.getValue();
                        }
                    }
                    if (building.getBrokenness() < newBrokennessValue) {
                        //after shock is occurred
                        if (propertyHelper.getPropertyTime(brokennessProperty) > getLastAfterShockTime()) {
                            setAftershockProperties(agentInfo.getTime(), agentInfo.getTime());
                        }
                    }
                }

                //Update seen building properties
                for (Property p : worldInfo.getChanged().getChangedProperties(building.getID())) {
                    building.getProperty(p.getURN()).takeValue(p);
                    propertyHelper.setPropertyTime(building.getProperty(p.getURN()), agentInfo.getTime());
                }

                MrlBuilding mrlBuilding = getMrlBuilding(building.getID());
                if (agentInfo.me() instanceof FireBrigade) {
                    if (building.isFierynessDefined() && building.isTemperatureDefined()) {
                        mrlBuilding.setEnergy(building.getTemperature() * mrlBuilding.getCapacity());
                        mrlBuilding.updateValues(building);
                    }
                }
//                if (getEntity(building.getID()) == null) {
//                    addEntityImpl(building);
                propertyHelper.addEntityProperty(building, agentInfo.getTime());
//                }

                //updating burning buildings set
                if (building.getFieryness() > 0 && building.getFieryness() < 4) {
                    burningBuildings.add(building.getID());
                } else {
                    burningBuildings.remove(building.getID());
                }

                buildingsSeen.add(building);
                mrlBuilding.setSensed(agentInfo.getTime());
                if (building.isOnFire()) {
                    mrlBuilding.setIgnitionTime(agentInfo.getTime());
                }

            } else if (entity instanceof Road) {
                Road road = (Road) entity;
                roadsSeen.add(road);

                MrlRoad mrlRoad = getMrlRoad(entity.getID());
                if (mrlRoad.isNeedUpdate()) {
                    mrlRoad.update();
                }
                mrlRoad.setLastSeenTime(agentInfo.getTime());
                mrlRoad.setSeen(true);

//                if (road.isBlockadesDefined()) {
//                    for (EntityID blockadeId : road.getBlockades()) {
//                        blockadesSeen.add((Blockade) worldInfo.getEntity(blockadeId));
//                    }
//                }
            } else if (entity instanceof Blockade) {
                blockadesSeen.add((Blockade) entity);
            } else if (entity instanceof FireBrigade) {
                fireBrigadesSeen.add((FireBrigade) entity);
            }
        });

        helpers.forEach(IHelper::update);


        return this;
    }

    public void processCivilianCommand(AKSpeak speak) {
        Civilian civilian = (Civilian) getEntity(speak.getAgentID());
        if (civilian == null) {
            civilian = new Civilian(speak.getAgentID());
            addNewCivilian(civilian);
        }
        if (!civilian.isPositionDefined()) {
            addHeardCivilian(civilian.getID());
        }
    }

    public void addNewCivilian(Civilian civilian) {
//        worldInfo.getRawWorld().addEntityImpl(civilian);//todo or should be worldInfo.addEntity(civilian);
        getHelper(PropertyHelper.class).addEntityProperty(civilian, getTime());
        getHelper(CivilianHelper.class).setInfoMap(civilian.getID());
    }


    private void reflectMessage(MessageManager messageManager) {
        Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
        changedEntities.add(this.agentInfo.getID());
        int time = this.agentInfo.getTime();
        int receivedTime = -1;
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageBuilding.class)) {
            MessageBuilding mb = (MessageBuilding) message;
            if (!changedEntities.contains(mb.getBuildingID())) {
                MessageUtil.reflectMessage(this.worldInfo, mb);
                if (mb.isRadio()) {
                    receivedTime = time - 1;
                } else {
                    receivedTime = time - 5;
                }
                if (agentInfo.me() instanceof FireBrigade) {
                    processBurningBuilding(mb, receivedTime);
                }
            }
//            this.sentTimeMap.put(mb.getBuildingID(), time + this.sendingAvoidTimeReceived);
        }

//        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageFireBrigade.class)) {
//            MessageFireBrigade mb = (MessageFireBrigade) message;
//            MessageUtil.reflectMessage(this.worldInfo, mb);
//
//            processWaterMessage(mb.getAction(), mb.getTargetID());
//        }
    }


    private void processWaterMessage(int action, EntityID targetId) {
        MrlBuilding mrlBuilding = getMrlBuilding(targetId);
        if (mrlBuilding != null) {
            int waterQuantity = 0;
            if (action == 23) {
                waterQuantity = scenarioInfo.getFireExtinguishMaxSum();
            } else if (action == 24) {
                waterQuantity = scenarioInfo.getFireExtinguishMaxSum() >> 1;
            }
            mrlBuilding.increaseWaterQuantity(waterQuantity);
        }
    }

    private void processBurningBuilding(MessageBuilding burningBuildingMessage, int receivedTime) {
        Building building;

        building = (Building) this.getEntity(burningBuildingMessage.getBuildingID());
        if (propertyHelper.getPropertyTime(building.getFierynessProperty()) < receivedTime) {
//            if (building.isFierynessDefined() && building.getFieryness() == 8 && burningBuilding.getFieryness() != 8) {
//                System.out.println("aaaa");
//            }
//            if (building.getID().getValue() == 25393) {
//                world.printData("BurningBuilding\tSender=" + burningBuilding.getSender().getValue() + " Real Fire=" + (building.isFierynessDefined() ? building.getFieryness() : 0) + " message fire: " + burningBuilding.getFieryness());
//            }
            building.setFieryness(burningBuildingMessage.getFieryness());
            propertyHelper.setPropertyTime(building.getFierynessProperty(), receivedTime);
            building.setTemperature(burningBuildingMessage.getTemperature());
            propertyHelper.setPropertyTime(building.getTemperatureProperty(), receivedTime);
//                if ((platoonAgent instanceof MrlFireBrigade)) {
//                    MrlFireBrigadeWorld w = (MrlFireBrigadeWorld) world;
            MrlBuilding mrlBuilding = this.getMrlBuilding(building.getID());
            switch (building.getFieryness()) {
                case 0:
                    mrlBuilding.setFuel(mrlBuilding.getInitialFuel());
                    break;
                case 1:
                    if (mrlBuilding.getFuel() < mrlBuilding.getInitialFuel() * 0.66) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.75));
                    } else if (mrlBuilding.getFuel() == mrlBuilding.getInitialFuel()) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.90));
                    }
                    break;

                case 2:
                    if (mrlBuilding.getFuel() < mrlBuilding.getInitialFuel() * 0.33
                            || mrlBuilding.getFuel() > mrlBuilding.getInitialFuel() * 0.66) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.50));
                    }
                    break;

                case 3:
                    if (mrlBuilding.getFuel() < mrlBuilding.getInitialFuel() * 0.01
                            || mrlBuilding.getFuel() > mrlBuilding.getInitialFuel() * 0.33) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.15));
                    }
                    break;
                case 4:
                case 5:
                case 6:
                case 7:
                    mrlBuilding.setWasEverWatered(true);
                    mrlBuilding.setEnergy(0);
                    break;

                case 8:
                    mrlBuilding.setFuel(0);
                    break;
            }
            mrlBuilding.setEnergy(building.getTemperature() * mrlBuilding.getCapacity());
//                    world.printData("burningBuilding:" + building+" f:"+burningBuildingMessage.getFieriness()+" temp:"+burningBuildingMessage.getTemperature());
//                }
            //updating burning buildings set
            if (building.getFieryness() > 0 && building.getFieryness() < 4) {
                this.getBurningBuildings().add(building.getID());
                mrlBuilding.setIgnitionTime(this.getTime());
            } else {
                this.getBurningBuildings().remove(building.getID());
            }
        }
    }


//    /**
//     * Returns a list of {@link rescuecore2.standard.entities.Road} containing roads that ends to {@code building}
//     *
//     * @param building building to find entrance roads
//     * @return List of entrance roads
//     * @author Siavash
//     */
//    public List<Road> getEntranceRoads(Building building) {
//        ArrayList<Road> entranceRoads = new ArrayList<Road>();
//        for (Entrance entrance : mrlBuilding.getEntrances()) {
//            entranceRoads.add(entrance.getNeighbour());
//        }
//        return entranceRoads;
//
//
//        // throw new UnsupportedOperationException();
//    }


    public Map<EntityID, EntityID> getEntranceRoads() {
        return entranceRoads;
    }

    public Map<EntityID, MrlRoad> getMrlRoadIdMap() {
        return mrlRoadIdMap;
    }

    public Map<EntityID, MrlBuilding> getMrlBuildingIdMap() {
        return mrlBuildingIdMap;
    }

    @Override
    public AbstractModule calc() {
        return this;
    }

    public <T extends IHelper> T getHelper(Class<T> c) {
        for (IHelper helper : helpers) {
            if (c.isInstance(helper)) {
                return c.cast(helper);
            }
        }
        throw new RuntimeException("Helper not available for:" + c);
    }

    public Set<MrlRoad> getMrlRoads() {
        return mrlRoads;
    }

    public MrlRoad getMrlRoad(EntityID id) {
        return mrlRoadIdMap.get(id);
    }

    public MrlBuilding getMrlBuilding(EntityID id) {
        return mrlBuildingIdMap.get(id);
    }

    public Set<MrlBuilding> getMrlBuildings() {
        return mrlBuildings;
    }

    public Set<Civilian> getCiviliansSeen() {
        return civiliansSeen;
    }

    private Collection<StandardEntity> getBuildingsWithURN() {
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.REFUGE,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.GAS_STATION);
    }

    private Collection<StandardEntity> getHydrantsWithURN() {
        return worldInfo.getEntitiesOfType(StandardEntityURN.HYDRANT);
    }

    private Collection<StandardEntity> getGasStationsWithUrn() {
        return worldInfo.getEntitiesOfType(StandardEntityURN.GAS_STATION);
    }

    private Collection<StandardEntity> getRefugesWithUrn() {
        return worldInfo.getEntitiesOfType(StandardEntityURN.REFUGE);
    }

    private Collection<StandardEntity> getAreasWithURN() {
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

    private Collection<StandardEntity> getHumansWithURN() {
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.CIVILIAN,
                StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE,
                StandardEntityURN.AMBULANCE_TEAM);
    }

    private Collection<StandardEntity> getAgentsWithURN() {
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE,
                StandardEntityURN.AMBULANCE_TEAM,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.AMBULANCE_CENTRE);
    }

    private Collection<StandardEntity> getPlatoonAgentsWithURN() {
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE,
                StandardEntityURN.AMBULANCE_TEAM);
    }

    private Collection<StandardEntity> getRoadsWithURN() {
        return worldInfo.getEntitiesOfType(StandardEntityURN.ROAD, StandardEntityURN.HYDRANT);
    }

    public Set<StandardEntity> getRefuges() {
        return refuges;
    }

    private void createMrlRoads() {
        mrlRoads = new HashSet<>();
        mrlRoadIdMap = new HashMap<>();
        for (StandardEntity rEntity : getRoads()) {
            Road road = (Road) rEntity;
            MrlRoad mrlRoad = new MrlRoad(agentInfo, worldInfo, scenarioInfo, moduleManager, road, this);
            mrlRoads.add(mrlRoad);
            mrlRoadIdMap.put(road.getID(), mrlRoad);
            String xy = road.getX() + "," + road.getY();
            roadXYMap.put(xy, road);
        }


//        MrlPersonalData.VIEWER_DATA.setViewRoadsMap(self.getID(), mrlRoads);
    }

    private void createMrlBuildings() {

        tempBuildingsMap = new HashMap<>(buildings.size());
        mrlBuildings = new HashSet<>();

        getBuildings().forEach(standardEntity -> {
            Building building = (Building) standardEntity;
            String xy = building.getX() + "," + building.getY();
            buildingXYMap.put(xy, building);

            MrlBuilding mrlBuilding = new MrlBuilding(standardEntity, this, worldInfo, agentInfo);

            if ((standardEntity instanceof Refuge)
                    || (standardEntity instanceof FireStation)
                    || (standardEntity instanceof PoliceOffice)
                    || (standardEntity instanceof AmbulanceCentre)) {  //todo all of these buildings may be flammable..............
                mrlBuilding.setFlammable(false);
            }
            mrlBuildings.add(mrlBuilding);
            tempBuildingsMap.put(standardEntity.getID(), mrlBuilding);
            mrlBuildingIdMap.put(building.getID(), mrlBuilding);

            // ina bejaye building helper umade.
            unvisitedBuildings.add(standardEntity.getID());
            worldTotalArea += mrlBuilding.getSelfBuilding().getTotalArea();
        });
        shouldCheckInsideBuildings.clear();

        //related to FBLegacyStrategy and Zone operations
        if (getSelfHuman() instanceof FireBrigade) {


            mrlBuildings.parallelStream().forEach(b -> {
                Collection<StandardEntity> neighbour = worldInfo.getObjectsInRange(b.getSelfBuilding(), Wall.MAX_SAMPLE_DISTANCE);
//            Collection<StandardEntity> fireNeighbour = getObjectsInRange(b.getSelfBuilding(), Wall.MAX_FIRE_DISTANCE);
                List<EntityID> neighbourBuildings = new ArrayList<EntityID>();
                for (StandardEntity entity : neighbour) {
                    if (entity instanceof Building) {
                        neighbourBuildings.add(entity.getID());
                        b.addMrlBuildingNeighbour(tempBuildingsMap.get(entity.getID()));
                    }
                }
                b.setNeighbourIdBuildings(neighbourBuildings);


            });

//            for (MrlBuilding b : mrlBuildings) {
//                Collection<StandardEntity> neighbour = worldInfo.getObjectsInRange(b.getSelfBuilding(), Wall.MAX_ SAMPLE_DISTANCE);
////            Collection<StandardEntity> fireNeighbour = getObjectsInRange(b.getSelfBuilding(), Wall.MAX_FIRE_DISTANCE);
//                List<EntityID> neighbourBuildings = new ArrayList<EntityID>();
//                for (StandardEntity entity : neighbour) {
//                    if (entity instanceof Building) {
//                        neighbourBuildings.add(entity.getID());
//                        b.addMrlBuildingNeighbour(tempBuildingsMap.get(entity.getID()));
//                    }
//                }
//                b.setNeighbourIdBuildings(neighbourBuildings);
//            }
        }


        for (MrlBuilding b : mrlBuildings) {
            //MTN
            if (b.getEntrances() != null) {
                Building building = b.getSelfBuilding();
                List<Road> rEntrances = BuildingHelper.getEntranceRoads(this, building);
                for (Road road : rEntrances) {
                    entranceRoads.put(road.getID(), b.getID());
                }

/*

                boolean shouldCheck = true;
//                if (rEntrances != null) {
//                    if (rEntrances.size() == 0)
//                        shouldCheck = false;
                VisibilityHelper visibilityHelper = getHelper(VisibilityHelper.class);
                for (Road road : rEntrances) {
                    boolean shouldCheckTemp = !visibilityHelper.isInsideVisible(new Point(road.getX(), road.getY()), new Point(building.getX(), building.getY()), building.getEdgeTo(road.getID()), scenarioInfo.getPerceptionLosMaxDistance());
                    if (!shouldCheckTemp) {
                        shouldCheck = false;
                        break;
//                    }
                    }
                }
                b.setShouldCheckInside(shouldCheck);
                if (shouldCheck) {
                    shouldCheckInsideBuildings.add(b);
                }
*/


            }
//            b.setNeighbourFireBuildings(fireNeighbours);
//            MrlPersonalData.VIEWER_DATA.setMrlBuildingsMap(b);

        }

//        MrlPersonalData.VIEWER_DATA.setViewerBuildingsMap(self.getID(), mrlBuildings);
    }

    public List<MrlBuilding> getShouldCheckInsideBuildings() {
        return shouldCheckInsideBuildings;
    }

    public Human getSelfHuman() {
        return selfHuman;
    }

    public Building getSelfBuilding() {
        return selfBuilding;
    }

    public StandardEntity getSelfPosition() {
        if (worldInfo.getEntity(agentInfo.getID()) instanceof Building) {
            return selfBuilding;
        } else {
            return agentInfo.getPositionArea();
        }
    }

    public Set<Road> getRoadsSeen() {
        return roadsSeen;
    }

    public Set<Building> getBuildingsSeen() {
        return buildingsSeen;
    }

    public Set<Blockade> getBlockadesSeen() {
        return blockadesSeen;
    }

    public Set<FireBrigade> getFireBrigadesSeen() {
        return fireBrigadesSeen;
    }

    public Set<StandardEntity> getRoads() {
        return roads;
    }

    public Set<StandardEntity> getBuildings() {
        return buildings;
    }

    public Set<StandardEntity> getAreas() {
        return areas;
    }

    public Set<StandardEntity> getHumans() {
        return humans;
    }

    public Set<StandardEntity> getAgents() {
        return agents;
    }

    public Set<StandardEntity> getPlatoonAgents() {
        return platoonAgents;
    }

    public Set<StandardEntity> getHydrants() {
        return hydrants;
    }

    public Set<StandardEntity> getGasStations() {
        return gasStations;
    }

    public GraphModule getGraph() {
        return graph;
    }

    public void printData(String s) {
        System.out.println("Time:" + agentInfo.getTime() + " Me:" + agentInfo.me() + " \t- " + s);
    }

    public void putEntrance(EntityID buildingId, Entrance entrance) {
        entranceRoads.put(entrance.getID(), buildingId);
    }

    public <T extends StandardEntity> T getEntity(EntityID id, Class<T> c) {
        StandardEntity entity;

        entity = worldInfo.getEntity(id);
        if (c.isInstance(entity)) {
            T castedEntity;

            castedEntity = c.cast(entity);
            return castedEntity;
        } else {
            return null;
        }
    }

    public StandardEntity getEntity(EntityID id) {
        return worldInfo.getEntity(id);
    }


    public int getLastAfterShockTime() {
        return lastAfterShockTime;
    }

    public int getAftershockCount() {
        return aftershockCount;
    }

    public void setAftershockProperties(int lastAfterShockTime, int aftershockCount) {
        if (this.aftershockCount < aftershockCount) {
            this.aftershockCount = aftershockCount;
            if (selfHuman != null) {
                postAftershockAction();
            }
        }
    }

    public void postAftershockAction() {
        this.printData("New aftershock occurred! Time: " + agentInfo.getTime() + " Total: " + this.getAftershockCount());

        for (MrlRoad mrlRoad : this.getMrlRoads()) {
            mrlRoad.getParent().undefineBlockades();
        }
    }

    public Set<EntityID> getVisitedBuildings() {
        return visitedBuildings;
    }

    public Set<EntityID> getUnvisitedBuildings() {
        return unvisitedBuildings;
    }

    public Set<EntityID> getBurningBuildings() {
        return burningBuildings;
    }


    /**
     * this method remove input building from {@code visitedBuildings}, add it in the {@code unvisitedBuilding} and prepare
     * message that should be send.<br/><br/>
     * <font color="red"><b>Note: </b></font> this method is calling automatically in  agent {@code act} in {}
     *
     * @param buildingID  {@code EntityID} of building that visited!
     * @param sendMessage {@code boolean} to sent visited building message
     */
    public void setBuildingVisited(EntityID buildingID, boolean sendMessage) {
        MrlBuilding mrlBuilding = getMrlBuilding(buildingID);
        if (selfHuman == null) {
            return;
        }
        if (!mrlBuilding.isVisited()) {
            mrlBuilding.setVisited();
            visitedBuildings.add(buildingID);
            unvisitedBuildings.remove(buildingID);
        }
        updateEmptyBuildingState(mrlBuilding, sendMessage);
    }

    public void updateEmptyBuildingState(MrlBuilding mrlBuilding, boolean sendMessage) {
        if (!mrlBuilding.isVisited()) {
            return;
        }

        if (!emptyBuildings.contains(mrlBuilding.getID()) && mrlBuilding.getCivilians().isEmpty()) {
            if (sendMessage) {
                thisCycleEmptyBuildings.add(mrlBuilding.getID());
            }
            emptyBuildings.add(mrlBuilding.getID());
        }

        if (emptyBuildings.contains(mrlBuilding.getID()) && !mrlBuilding.getCivilians().isEmpty()) {
            emptyBuildings.remove(mrlBuilding.getID());
        }
    }

    public Set<EntityID> getThisCycleEmptyBuildings() {
        return thisCycleEmptyBuildings;
    }

    public Rectangle2D getBounds() {
        return worldInfo.getBounds();
    }

    protected void setWorldCommunicationCondition() {


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

    public void setCommunicationLess(boolean CL) {
        this.CommunicationLess = CL;
    }

    public boolean isCommunicationLess() {
        return CommunicationLess;
    }

    public boolean isCommunicationLow() {
        return isCommunicationLow;
    }

    public void setCommunicationLow(boolean communicationLow) {
        isCommunicationLow = communicationLow;
    }

    public boolean isCommunicationMedium() {
        return isCommunicationMedium;
    }

    public void setCommunicationMedium(boolean communicationMedium) {
        isCommunicationMedium = communicationMedium;
    }

    public void setCommunicationHigh(boolean communicationHigh) {
        isCommunicationHigh = communicationHigh;
    }

    public boolean isCommunicationHigh() {
        return isCommunicationHigh;
    }

    public Set<MrlBuilding> getEstimatedBurningBuildings() {
        return estimatedBurningBuildings;
    }

    public void setEstimatedBurningBuildings(Set<MrlBuilding> estimatedBurningBuildings) {
        this.estimatedBurningBuildings = estimatedBurningBuildings;
    }

    protected void updateAvailableHydrants() {
      /*  if (lastUpdateHydrants < agentInfo.getTime() && selfHuman != null && selfHuman instanceof FireBrigade && !getHydrants().isEmpty()) {
            lastUpdateHydrants = agentInfo.getTime();
            availableHydrants.clear();
            availableHydrants.addAll(getHydrants());
            StandardEntity position;
            MrlRoad hydrantMrlRoad;
            PropertyHelper propertyHelper = getHelper(PropertyHelper.class);
            Set<StandardEntity> toRemoves = new HashSet<>();
            for (StandardEntity entity : availableHydrants) {
                if (toRemoves.contains(entity) || platoonAgent.getPathPlanner().getPassableAreas().contains(entity.getID())) {
                    continue;
                }

                if (hydrantsSeen.contains(entity)) {
                    //Ignore Hydrants which located in view range and is reachable
                    List<EntityID> planMove =
                            platoonAgent.getPathPlanner().planMove((Area) getSelfPosition(), (Hydrant) entity, MRLConstants.IN_TARGET, false);
                    if (planMove != null && !planMove.isEmpty()) {
                        platoonAgent.getPathPlanner().getPassableAreas().addAll(planMove);
                    } else {
                        toRemoves.add(entity);
                    }
                } else {
                    toRemoves.add(entity);
                }
            }
            for (FireBrigade fireBrigade : getFireBrigadeList()) {
                if (fireBrigade.getID().equals(selfHuman.getID())) {
                    continue;
                }
                if (fireBrigade.isPositionDefined()) {
                    position = fireBrigade.getPosition(this);
                    if (toRemoves.contains(position)) {
                        continue;
                    }
                    if (position instanceof Hydrant) {
                        hydrantMrlRoad = getMrlRoad(position.getID());
                        int agentDataTime = propertyHelper.getEntityLastUpdateTime(fireBrigade);
                        int hydrantSeenTime = hydrantMrlRoad.getLastSeenTime();
                        if (agentInfo.getTime() - agentDataTime < MRLConstants.AVAILABLE_HYDRANTS_UPDATE_TIME
                                || agentInfo.getTime() - hydrantSeenTime < MRLConstants.AVAILABLE_HYDRANTS_UPDATE_TIME) {
//                                printData("my data from " + fireBrigade + " is out of date... my data time is : " + agentDataTime + " and hydrant seen time is: " + hydrantSeenTime);
                            toRemoves.add(position);
                        }
                    }
                }
            }


            availableHydrants.removeAll(toRemoves);

//            MrlPersonalData.VIEWER_DATA.setAvailableHydrants(getPlatoonAgent(), availableHydrants);
        }*/
    }

    public String getMapName() {
        return getUniqueMapNumber().toString();
    }

    public Long getUniqueMapNumber() {
        return uniqueMapNumber;
    }

    public Building getBuildingInPoint(int x, int y) {
        String xy = x + "," + y;
        return buildingXYMap.get(xy);
    }

    public Road getRoadInPoint(Point point) {
        String xy = point.getX() + "," + point.getY();
        Road road = roadXYMap.get(xy);
        if (road == null) {
            for (StandardEntity entity : getRoads()) {
                Road r = (Road) entity;
                if (r.getShape().contains(point)) {
                    return r;
                }
            }
        }
        return road;
    }

    public ScenarioInfo getScenarioInfo() {
        return scenarioInfo;
    }

    public void setScenarioInfo(ScenarioInfo scenarioInfo) {
        this.scenarioInfo = scenarioInfo;
    }

    public AgentInfo getAgentInfo() {
        return agentInfo;
    }

    public void setAgentInfo(AgentInfo agentInfo) {
        this.agentInfo = agentInfo;
    }

    public WorldInfo getWorldInfo() {
        return worldInfo;
    }

    public void setWorldInfo(WorldInfo worldInfo) {
        this.worldInfo = worldInfo;
    }

    public Set<EntityID> getBuildingIDs() {
        Set<EntityID> buildingIDs = new HashSet<>();
        Collection<StandardEntity> buildings = getBuildings();
        for (StandardEntity entity : buildings) {
            buildingIDs.add(entity.getID());
        }

        return buildingIDs;
    }

    public Set<EntityID> getBorderBuildings() {
        return borderBuildings;
    }

    public int getMinX() {
        return this.minX;
    }

    public int getMinY() {
        return this.minY;
    }

    public int getMaxX() {
        return this.maxX;
    }

    public int getMaxY() {
        return this.maxY;
    }

    public int getMapWidth() {
        return maxX - minX;
    }

    public int getMapHeight() {
        return maxY - minY;
    }

    public int getTime() {
        return agentInfo.getTime();
    }

    public int getMaxExtinguishDistance() {
        return scenarioInfo.getFireExtinguishMaxDistance();

    }

    public int getDistance(EntityID first, EntityID second) {
        return worldInfo.getDistance(first, second);
    }

    public int getDistance(StandardEntity first, StandardEntity second) {
        return worldInfo.getDistance(first, second);
    }


    public Collection<StandardEntity> getFireBrigades() {
        return worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE);
    }

    public List<StandardEntity> getFireBrigadeList() {
        return fireBrigades;
    }

    public Collection<StandardEntity> getObjectsInRange(EntityID entityID, int distance) {
        return worldInfo.getObjectsInRange(entityID, distance);
    }

    public Collection<StandardEntity> getObjectsInRange(int x, int y, int range) {
        int newRange = (int) (0.64 * range);
        return worldInfo.getObjectsInRange(x, y, newRange);
    }

    public Polygon getWorldPolygon() {
        Polygon worldPolygon;

        double[] point = new double[4];
        int xs[] = new int[4];
        int ys[] = new int[4];

        point[0] = this.getMinX() - 1;
        point[1] = this.getMinY() - 1;
        point[2] = this.getMaxX() + 1;
        point[3] = this.getMaxY() + 1;

        xs[0] = (int) point[0];
        ys[0] = (int) point[1];

        xs[1] = (int) point[2];
        ys[1] = (int) point[1];

        xs[2] = (int) point[2];
        ys[2] = (int) point[3];

        xs[3] = (int) point[0];
        ys[3] = (int) point[3];

        worldPolygon = new Polygon(xs, ys, 4);

        return worldPolygon;
    }

    public boolean isMapHuge() {
        return isMapHuge;
    }

    public boolean isMapMedium() {
        return isMapMedium;
    }

    public boolean isMapSmall() {
        return isMapSmall;
    }


    public List<StandardEntity> getBuildingsInShape(Shape shape) {
        List<StandardEntity> result = new ArrayList<StandardEntity>();
        for (StandardEntity next : getBuildings()) {
            Area area = (Area) next;
            if (shape.contains(area.getShape().getBounds2D()))
                result.add(next);
        }
        return result;
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

    public Set<EntityID> getPossibleBurningBuildings() {
        return possibleBurningBuildings;
    }

    public void setPossibleBurningBuildings(Set<EntityID> possibleBurningBuildings) {
        this.possibleBurningBuildings = possibleBurningBuildings;
    }

    public Collection<StandardEntity> getCivilians() {
        return civilians;
    }


    /**
     * All civilian defined in world model or their voice were heard.
     *
     * @return
     */
    public Set<EntityID> getAllCivilians() {
        return allCivilians;
    }

    public int getVoiceRange() {
        return scenarioInfo.getRawConfig().getIntValue(MRLConstants.VOICE_RANGE_KEY);
    }

    /**
     * Gets heard civilians at current cycle;<br/>
     * <br/>
     * <b>Note: </b> At each cycle the list will be cleared
     *
     * @return EntityIDs of heard civilians
     */
    public Set<EntityID> getHeardCivilians() {
        return heardCivilians;
    }

    /**
     * add civilian who speak of it was heard in current cycle!
     *
     * @param civID EntityID of civilian
     */
    public void addHeardCivilian(EntityID civID) {
//        MrlPersonalData.VIEWER_DATA.setHeardPositions(civID, getSelfLocation());

        if (!heardCivilians.contains(civID)) {
            heardCivilians.add(civID);
        }
    }

    public Pair<Integer, Integer> getSelfLocation() {
        return worldInfo.getLocation(agentInfo.getID());
    }

    public boolean isBuildingBurnt(Building building) {
        if (building == null || !building.isFierynessDefined()) {
            return false;
        }
        int fieriness = building.getFieryness();

        return fieriness != 0 && fieriness != 4 && fieriness != 5;
    }

    public int getViewDistance() {
        return scenarioInfo.getPerceptionLosMaxDistance();
    }

    public void setExploreTarget(EntityID exploreTarget) {
        this.exploreTarget = exploreTarget;
    }

    public EntityID getExploreTarget() {
        return exploreTarget;
    }

    public void flagUnreachable(Area area) {
        if (area instanceof Building) {
            MrlBuilding mrlBuilding = getMrlBuilding(area.getID());
            mrlBuilding.setReachable(false);
        } else if (area instanceof Road) {
            MrlRoad mrlRoad = getMrlRoad(area.getID());
            mrlRoad.setReachable(false);
        }
    }
}
