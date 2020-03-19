package CSU_Yunlu_2019.module.complex.pf;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.centralized.CommandAmbulance;
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.agent.communication.standard.bundle.centralized.MessageReport;
import adf.agent.communication.standard.bundle.information.MessageAmbulanceTeam;
import adf.agent.communication.standard.bundle.information.MessageFireBrigade;
import adf.agent.communication.standard.bundle.information.MessagePoliceForce;
import adf.agent.communication.standard.bundle.information.MessageRoad;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.RoadDetector;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;
import java.util.stream.Collectors;

import CSU_Yunlu_2019.extaction.pf.guidelineHelper;

public class CSURoadDetector extends RoadDetector {
	
	public static final String KEY_JUDGE_ROAD = "RoadDetector.judge_road";
	public static final String KEY_START_X = "RoadDetector.start_x";
	public static final String KEY_START_Y = "RoadDetector.start_y";
	public static final String KEY_END_X = "RoadDetector.end_x";
	public static final String KEY_END_Y = "RoadDetector.end_y";
	public static final String KEY_ROAD_SIZE = "RoadDetector.road_size";

    private List<guidelineHelper> judgeRoad = new ArrayList<>();
	private int roadsize;

	//targets
	private Set<EntityID> targetAreas = new HashSet<>();
	private Set<EntityID> priorityRoads = new HashSet<>();
	private Set<EntityID> presetTargets = new HashSet<>();

	//history
	private Set<EntityID> clearedAreas = new HashSet<>();
	private int lastRequestTime = -5;

	//observable entities
    private Set<EntityID> stuckAgentRoads = new HashSet<>();
    private EntityID nearRefuge = null;
    private Set<EntityID> blockedRoads = new HashSet<>();

	//centralize
	private MessageManager messageManager = null;
	private boolean needReport = true;

	//constant & flag
	private boolean haveOffice = false;
	private final int stuckRange = 600;
	private final int sendingAvoidTimeRequest = 5;

	//precompute keys
	private final String KEY_PRESET_TARGETS = "CSU.roadDetector.presetTargets";
	private final String KEY_CLEARED_AREAS = "CSU.roadDetector.clearedAreas";

	//tools
	private Clustering clustering;
	private PathPlanning pathPlanning;

	//debug & log
	//private Logger logger;

	//result
	private EntityID result = null;

	public CSURoadDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
		 super(ai, wi, si, moduleManager, developData);
        switch (scenarioInfo.getMode())
        {
            case PRECOMPUTATION_PHASE:
            	this.pathPlanning = moduleManager.getModule("RoadDetector.PathPlanning",
    					"CSU_Yunlu_2019.module.algorithm.AStarPathPlanning");
                 this.clustering = moduleManager.getModule("SampleRoadDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case PRECOMPUTED:
            	this.pathPlanning = moduleManager.getModule("RoadDetector.PathPlanning",
    					"CSU_Yunlu_2019.module.algorithm.AStarPathPlanning");
                this.clustering = moduleManager.getModule("SampleRoadDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case NON_PRECOMPUTE:
            	this.pathPlanning = moduleManager.getModule("RoadDetector.PathPlanning",
    					"CSU_Yunlu_2019.module.algorithm.AStarPathPlanning");
               this.clustering = moduleManager.getModule("SampleRoadDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
        }
        registerModule(this.pathPlanning);
        this.haveOffice = !wi.getEntityIDsOfType(StandardEntityURN.POLICE_OFFICE).isEmpty();
        this.result = null;
	}

	@Override
    public RoadDetector precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if(this.getCountPrecompute() >= 2) {
            return this;
        }
        this.clustering.precompute(precomputeData);
        this.pathPlanning.precompute(precomputeData);
        
        this.create_guideline();
        this.roadsize = this.judgeRoad.size();
        precomputeData.setInteger(KEY_ROAD_SIZE, this.roadsize);
        
        for(int i=0 ; i<this.roadsize ; ++i) {
        	precomputeData.setDouble(KEY_START_X + i, this.judgeRoad.get(i).getStartPoint().getX());
        	precomputeData.setDouble(KEY_START_Y + i, this.judgeRoad.get(i).getStartPoint().getY());
        	precomputeData.setDouble(KEY_END_X + i, this.judgeRoad.get(i).getEndPoint().getX());
        	precomputeData.setDouble(KEY_END_Y + i, this.judgeRoad.get(i).getEndPoint().getY());
        	precomputeData.setEntityID(KEY_JUDGE_ROAD + i, this.judgeRoad.get(i).getSelfID());
        }

        //entrances of building
        Set<EntityID> entrances = new HashSet<>();
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
            for (EntityID id : ((Road)entity).getNeighbours()) {
                if (this.worldInfo.getEntity(id) instanceof Building) {
                    entrances.add(entity.getID());
                    break;
                }
            }
        }

        //crosses
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
            int count = 0;
            for (EntityID id : ((Road)entity).getNeighbours()) {
                if (this.worldInfo.getEntity(id) instanceof Road && !entrances.contains(id)) {
                    ++count;
                }
            }
            if (count > 2) {
                this.presetTargets.add(entity.getID());
            }
        }
        List<EntityID> removeList = new ArrayList<>(); //these crosses are sort of square, they are not as important as other crosses
        for (EntityID id : this.presetTargets) { //there are many this kind of road in Kobe
            int count = 0;
            Road road = (Road)this.worldInfo.getEntity(id);
            for (EntityID neighbor : road.getNeighbours()) {
                if (this.presetTargets.contains(neighbor)) {
                    ++count;
                }
            }
            if (count == this.getPassableEdge(road).size()) {
                removeList.add(id);
            }
        }
        this.presetTargets.removeAll(removeList);
        removeList.clear();

        //entrances of refuge
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.REFUGE)) {
            this.presetTargets.addAll(this.getAllEntrancesOfBuilding((Building)entity));
        }

        //another type of cross, all of their neighbors are buildings, in fact, they are always shortcuts.
        for (EntityID id : entrances) {
            boolean flag = false;
            for (EntityID neighbor : ((Area)this.worldInfo.getEntity(id)).getNeighbours()) {
                if (this.worldInfo.getEntity(neighbor) instanceof Road) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                this.presetTargets.add(id);
            }
        }

        precomputeData.setEntityIDList(KEY_PRESET_TARGETS, new ArrayList<>(this.presetTargets));

        //dead ends, clear these roads is a waste of time
        Queue<EntityID> clearedQueue = new ArrayDeque<>();
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
            if (((Area)entity).getNeighbours().size() <= 1) {
                clearedQueue.offer(entity.getID());
            }
        }
        while (!clearedQueue.isEmpty()) {
            EntityID clearedID = clearedQueue.poll();
            this.clearedAreas.add(clearedID);
            for (EntityID id : ((Area)this.worldInfo.getEntity(clearedID)).getNeighbours()) {
                if (clearedQueue.contains(id) || this.clearedAreas.contains(id)) {
                    continue;
                }
                Area area = (Area)this.worldInfo.getEntity(id);
                int count = 0;
                for (EntityID neighbor : area.getNeighbours()) {
                    if (this.clearedAreas.contains(neighbor) || clearedQueue.contains(neighbor)) {
                        ++count;
                    }
                }
                if (area.getNeighbours().size() - count <= 1) {
                    clearedQueue.offer(id);
                }
            }
        }
        //roads which are far away from building are also clean-areas, but I don't know how to read and use the config file in code file yet
        //in roborescue-v1.2/modules/collapse/src/collapse/CollapseSimulator.java can find how blockades were created
        precomputeData.setEntityIDList(KEY_CLEARED_AREAS, new ArrayList<>(this.clearedAreas));

        return this;
    }

    @Override
    public RoadDetector resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if(this.getCountResume() >= 2) {
            return this;
        }
        
        
        this.roadsize = precomputeData.getInteger(KEY_ROAD_SIZE);
        for(int i=0 ; i<this.roadsize ; ++i) {
        	double startx = precomputeData.getDouble(KEY_START_X + i);
        	double starty = precomputeData.getDouble(KEY_START_Y + i);
        	Point2D start = new Point2D(startx,starty);
        	double endx = precomputeData.getDouble(KEY_END_X + i);
        	double endy = precomputeData.getDouble(KEY_END_Y + i);
        	Point2D end = new Point2D(endx,endy);
        	Road road = (Road) this.worldInfo.getEntity(precomputeData.getEntityID(KEY_JUDGE_ROAD + i));
        	guidelineHelper line = new guidelineHelper(road,start,end);
        	if(! this.judgeRoad.contains(line)) this.judgeRoad.add(line);
        }
        
        
        this.clustering.resume(precomputeData);
        this.pathPlanning.resume(precomputeData);

        this.presetTargets.addAll(precomputeData.getEntityIDList(this.KEY_PRESET_TARGETS));
        this.clearedAreas.addAll(precomputeData.getEntityIDList(this.KEY_CLEARED_AREAS));

        int clusterIndex = this.clustering.getClusterIndex(this.agentInfo.getID());
        this.result = (new ArrayList<>(this.clustering.getClusterEntityIDs(clusterIndex))).get(0);
        this.targetAreas.add(this.result);


	    return this;
    }

    @Override
    public RoadDetector preparate() {
        super.preparate();
        if(this.getCountPreparate() >= 2) {
            return this;
        }
        this.pathPlanning.preparate();
        this.clustering.preparate();

        //entrances
        Set<EntityID> entrances = new HashSet<>();
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.BUILDING)) {
            for (EntityID id : ((Building)entity).getNeighbours()) {
                if (this.worldInfo.getEntity(id) instanceof Road) {
                    entrances.add(id);
                }
            }
        }

        //crosses
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
            int count = 0;
            for (EntityID id : ((Road)entity).getNeighbours()) {
                if (this.worldInfo.getEntity(id) instanceof Road && !entrances.contains(id)) {
                    ++count;
                }
            }
            if (count > 2) {
                this.presetTargets.add(entity.getID());
            }
        }
        List<EntityID> removeList = new ArrayList<>();//these crosses are sort of square, they are not as important as other cross
        for (EntityID id : this.presetTargets) { //there are many this kind of road in Kobe
            int count = 0;
            Road road = (Road)this.worldInfo.getEntity(id);
            for (EntityID neighbor : road.getNeighbours()) {
                if (this.presetTargets.contains(neighbor)) {
                    ++count;
                }
            }
            if (count == this.getPassableEdge(road).size()) {
                removeList.add(id);
            }
        }
        this.presetTargets.removeAll(removeList);
        removeList.clear();

        //entracnes of refuge
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.REFUGE)) {
            this.presetTargets.addAll(this.getAllEntrancesOfBuilding((Building)entity));
        }

        //another type of cross, all of their neighbors are buildings, in fact, they are always shortcuts.
        for (EntityID id : entrances) {
            boolean flag = false;
            for (EntityID neighbor : ((Area)this.worldInfo.getEntity(id)).getNeighbours()) {
                if (this.worldInfo.getEntity(neighbor) instanceof Road) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                this.presetTargets.add(id);
            }
        }

        //first mission for police is going to his workplace
        this.clustering.calc();
        List<EntityID> sortList = new ArrayList<>();
        for (EntityID id : this.clustering.getClusterEntityIDs(
                this.clustering.getClusterIndex(this.agentInfo.getID()))) {
            if (this.worldInfo.getEntity(id) instanceof Road) {
                sortList.add(id);
            }
        }
        sortList.sort(new DistanceIDSorter(this.worldInfo, this.agentInfo.getID()));
        this.result = sortList.get(sortList.size() / 2);
        this.targetAreas.add(this.result);

        //dead ends
        Queue<EntityID> clearedQueue = new ArrayDeque<>();
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
            if (((Area)entity).getNeighbours().size() <= 1) {
                clearedQueue.offer(entity.getID());
            }
        }
        while (!clearedQueue.isEmpty()) {
            EntityID clearedID = clearedQueue.poll();
            this.clearedAreas.add(clearedID);
            for (EntityID id : ((Area)this.worldInfo.getEntity(clearedID)).getNeighbours()) {
                if (clearedQueue.contains(id) || this.clearedAreas.contains(id)) {
                    continue;
                }
                Area area = (Area)this.worldInfo.getEntity(id);
                int count = 0;
                for (EntityID neighbor : area.getNeighbours()) {
                    if (this.clearedAreas.contains(neighbor) || clearedQueue.contains(neighbor)) {
                        ++count;
                    }
                }
                if (area.getNeighbours().size() - count <= 1) {
                    clearedQueue.offer(id);
                }
            }
        }



	    return this;
    }

	@Override
	public RoadDetector updateInfo(MessageManager messageManager) {
		//System.out.println("Selected Target: " + this.result);
		super.updateInfo(messageManager);
		this.pathPlanning.updateInfo(messageManager);
		this.clustering.updateInfo(messageManager);

		this.sendRequestCommand(messageManager);

		this.update_roads();

        this.reflectMessage(messageManager);

        this.updateResult();

        if (this.needReport && this.haveOffice && this.result == null) {
            messageManager.addMessage(new MessageReport(true, true, true, this.agentInfo.getID()));
            this.needReport = false;
        }

        this.messageManager = messageManager;

		return this;
	}
	
	@Override
	public RoadDetector calc() {
        Human me = (Human)this.agentInfo.me();
        if (me.isDamageDefined() && me.getDamage() > 0) {
            this.result = this.getClosestEntityID(this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE), this.agentInfo.getID());
            //System.out.println("Selected Target: " + this.result);
            return this;
        }

        if (this.result == null) {
            EntityID positionID = this.agentInfo.getPosition();
            if (this.targetAreas.contains(positionID) &&
                    (!this.clearedAreas.contains(positionID) || this.stuckAgentRoads.contains(positionID))) {
                this.result = positionID;
             
                return this;
            }

            if (this.nearRefuge != null) {
                boolean flag = true;
                for (EntityID id : this.getAllEntrancesOfBuilding((Building)this.worldInfo.getEntity(nearRefuge))) {
                    if (this.clearedAreas.contains(id)) {
                        flag = false;
                        break;
                    }
                }
                if (flag) {
                    this.result = this.getClosestEntityID(this.getAllEntrancesOfBuilding((Building)this.worldInfo.getEntity(nearRefuge)),
                            this.agentInfo.getID());
                    if (this.result != null) {
                        this.targetAreas.add(this.result);
                        if (this.haveOffice) {
                            this.messageManager.addMessage(new MessageReport(true, false, true, this.agentInfo.getID()));
                            this.needReport = true;
                        }
                      
                        return this;
                    }
                }
            }

            if (!this.stuckAgentRoads.isEmpty()) {
                List<EntityID> sortList = new ArrayList<>(this.stuckAgentRoads);
                sortList.sort(new DistanceIDSorter(this.worldInfo, this.agentInfo.getID()));
                List<EntityID> nearPolice = new ArrayList<>();
                for (EntityID id : this.worldInfo.getChanged().getChangedEntities()) {
                    if (this.worldInfo.getEntity(id) instanceof PoliceForce) {
                        nearPolice.add(id);
                    }
                }
                nearPolice.add(this.agentInfo.getID());
                for (int i = 0;i < sortList.size();++i) {
                    EntityID id = this.getClosestEntityID(nearPolice, sortList.get(i));
                    if (!id.equals(this.agentInfo.getID())) {
                        nearPolice.remove(id);
                    } else {
                        this.result = sortList.get(i);
                        this.targetAreas.add(this.result);
                        if (this.haveOffice) {
                            this.messageManager.addMessage(new MessageReport(true, false, true, this.agentInfo.getID()));
                            this.needReport = true;
                        }
                       // System.out.println("Selected Target: " + this.result);
                        return this;
                    }
                }
            }

            if (!this.targetAreas.isEmpty()) {
                this.pathPlanning.setFrom(positionID);
                this.pathPlanning.setDestination(this.getClosestEntityID(this.targetAreas, this.agentInfo.getID()));
                List<EntityID> path = this.pathPlanning.calc().getResult();
                if (path != null && path.size() > 0) {
                    this.result = path.get(path.size() - 1);
                }
              //System.out.println("Selected Target: " + this.result);
                return this;
            }

            if (!this.priorityRoads.isEmpty()) {
                for (EntityID id : this.priorityRoads) {
                    if (this.worldInfo.getDistance(this.agentInfo.getID(), id) < 50000) {
                        this.targetAreas.add(id);
                    }
                }
                if (!this.targetAreas.isEmpty()) {
                    this.pathPlanning.setFrom(positionID);
                    this.pathPlanning.setDestination(this.getClosestEntityID(this.targetAreas, this.agentInfo.getID()));
                    List<EntityID> path = this.pathPlanning.calc().getResult();
                    if (path != null && path.size() > 0) {
                        this.result = path.get(path.size() - 1);
                    }
                    //System.out.println("Selected Target: " + this.result);
                    return this;
                }
            }

            List<EntityID> removeList = new ArrayList<>();
            for (EntityID id : this.blockedRoads) {
                if (this.clearedAreas.contains(id)) {
                    removeList.add(id);
                }
            }
            this.blockedRoads.removeAll(removeList);
            removeList.clear();
            if (!this.blockedRoads.isEmpty()) {
                List<EntityID> sortList = new ArrayList<>(this.blockedRoads);
                sortList.sort(new DistanceIDSorter(this.worldInfo, this.agentInfo.getID()));
                List<EntityID> nearPolice = new ArrayList<>();
                for (EntityID id : this.worldInfo.getChanged().getChangedEntities()) {
                    if (this.worldInfo.getEntity(id) instanceof PoliceForce) {
                        nearPolice.add(id);
                    }
                }
                nearPolice.add(this.agentInfo.getID());
                for (int i = 0;i < sortList.size();++i) {
                    EntityID id = this.getClosestEntityID(nearPolice, sortList.get(i));
                    if (!id.equals(this.agentInfo.getID())) {
                        nearPolice.remove(id);
                    } else {
                        this.result = sortList.get(i);
                        this.targetAreas.add(this.result);
                        //System.out.println("Selected Target: " + this.result);
                        return this;
                    }
                }
            }

            for (EntityID id : this.clustering.getClusterEntityIDs(
                    this.clustering.getClusterIndex(this.agentInfo.getID()))) {
                if ((this.worldInfo.getEntity(id) instanceof Road) && !this.clearedAreas.contains(id) && this.presetTargets.contains(id)) {
                    this.targetAreas.add(id);
                }
            }
            if (!this.targetAreas.isEmpty()) {
                this.pathPlanning.setFrom(positionID);
                this.pathPlanning.setDestination(this.targetAreas);
                List<EntityID> path = this.pathPlanning.calc().getResult();
                if (path != null && path.size() > 0) {
                    this.result = path.get(path.size() - 1);
                }
              
                return this;
            }

            List<EntityID> sortList = new ArrayList<>(this.presetTargets);
            sortList.sort(new DistanceIDSorter(this.worldInfo, this.agentInfo.getID()));
            for (int i = 0;i < sortList.size() && i < 3; ++i) {
                this.targetAreas.add(sortList.get(i));
            }
            if (!this.targetAreas.isEmpty()) {
                this.pathPlanning.setFrom(positionID);
                this.pathPlanning.setDestination(this.targetAreas);
                List<EntityID> path = this.pathPlanning.calc().getResult();
                if (path != null && path.size() > 0) {
                    this.result = path.get(path.size() - 1);
                }
                //System.out.println("Selected Target: " + this.result);
                return this;
            }
        }
		return this;
	}

    @Override
    public EntityID getTarget() {
        return this.result;
    }

    //UpdateInfo
    private void sendRequestCommand(MessageManager messageManager) {
	    if (this.agentInfo.getTime() < this.lastRequestTime + this.sendingAvoidTimeRequest) {
	        return;
        }
        Human me = (Human)this.agentInfo.me();
        if (me.isBuriednessDefined() && me.getBuriedness() > 0) {
            messageManager.addMessage(new CommandAmbulance(true, null, this.agentInfo.getPosition(), CommandAmbulance.ACTION_MOVE));
            this.lastRequestTime = this.agentInfo.getTime();
            Area position = (Area)this.worldInfo.getPosition(this.agentInfo.getID());
            if (position instanceof Building) {
                Building building = (Building)position;
                EntityID entrance = this.getClosestEntityID(this.getAllEntrancesOfBuilding(building), this.agentInfo.getID());
                if (!this.isPassable((Road)this.worldInfo.getEntity(entrance))) {
                    messageManager.addMessage(new CommandPolice(true, null, entrance, CommandPolice.ACTION_CLEAR));
                }
            }
        }
    }

    private void update_roads() {
        this.stuckAgentRoads.clear();
        this.nearRefuge = null;
        this.blockedRoads.clear();
        for (EntityID id : this.worldInfo.getChanged().getChangedEntities()) {
            StandardEntity entity = this.worldInfo.getEntity(id);
            if ((entity instanceof FireBrigade) || (entity instanceof AmbulanceTeam) || (entity instanceof Civilian)) {
                Human human = (Human)entity;
                EntityID position = human.getPosition();
                StandardEntity positionEntity = this.worldInfo.getEntity(position);
                if (positionEntity instanceof Road) {
                    if (this.isHumanStucked(human, (Road)positionEntity)) {
                        this.stuckAgentRoads.add(position);
                    }
                } else if (positionEntity instanceof Building) {
                    boolean flag = false;
                    for (EntityID entrance : this.getAllEntrancesOfBuilding((Building)positionEntity)) {
                        if (this.isPassable((Road)this.worldInfo.getEntity(entrance))) {
                            flag = true;
                            break;
                        }
                    }
                    if (!flag) {
                        this.stuckAgentRoads.addAll(this.getAllEntrancesOfBuilding((Building)positionEntity));
                    }
                }
            } else if (entity instanceof Road) {
                if (this.isPassable((Road)entity)) {
                    this.clearedAreas.add(id);
                    this.priorityRoads.remove(id);
                    this.presetTargets.remove(id);
                } else {
                    this.blockedRoads.add(id);
                }
            } else if (entity instanceof Refuge) {
                this.nearRefuge = id;
            }
        }
        List<EntityID> removeList = new ArrayList<>();
        for (EntityID id : this.targetAreas) {
            if (this.clearedAreas.contains(id) && !stuckAgentRoads.contains(id)) {
                removeList.add(id);
            }
        }
        this.targetAreas.removeAll(removeList);
    }

	
	
	
	
	
	
	
	
    private void reflectMessage(MessageManager messageManager) {
	    Collection<EntityID> change = this.worldInfo.getChanged().getChangedEntities();
	    //reflect MessageRoad
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageRoad.class)) {
            MessageRoad messageRoad = (MessageRoad)message;
            EntityID roadID = messageRoad.getRoadID();
            if (!change.contains(roadID) && messageRoad.isPassable()) {
                this.clearedAreas.add(roadID);
                this.targetAreas.remove(roadID);
                this.priorityRoads.remove(roadID);
            }
        }
	    //reflect MessagePolice
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessagePoliceForce.class)) {
            MessagePoliceForce messagePoliceForce = (MessagePoliceForce)message;
            if (messagePoliceForce.getAction() != MessagePoliceForce.ACTION_REST) {
                if (messagePoliceForce.isPositionDefined() && !change.contains(messagePoliceForce.getPosition())) {
                    this.clearedAreas.add(messagePoliceForce.getPosition());
                }
                if (messagePoliceForce.isTargetDefined() && !change.contains(messagePoliceForce.getTargetID())) {
                    EntityID targetID = messagePoliceForce.getTargetID();
                    if (this.targetAreas.contains(targetID)) {
                        if (messagePoliceForce.isPositionDefined()) {
                            if (this.worldInfo.getDistance(this.agentInfo.getID(), targetID)
                                    > this.worldInfo.getDistance(messagePoliceForce.getAgentID(), targetID)) {
                                this.targetAreas.remove(targetID);
                                this.clearedAreas.add(targetID);
                            }
                        }
                    } else {
                        this.clearedAreas.add(messagePoliceForce.getTargetID());
                    }
                }
            }
        }
        //reflect CommandPolice
        for (CommunicationMessage message : messageManager.getReceivedMessageList(CommandPolice.class)) {
            CommandPolice commandPolice = (CommandPolice)message;
            if (this.haveOffice) {
                if (commandPolice.isToIDDefined() && commandPolice.getToID().equals(this.agentInfo.getID())) {
                    if (this.worldInfo.getDistance(this.agentInfo.getID(), commandPolice.getTargetID()) > 80000) {
                        this.result = commandPolice.getTargetID();
                        this.targetAreas.add(this.result);
                    }
                }
            } else {
                if (commandPolice.isToIDDefined()) {
                    if (commandPolice.getToID().equals(this.agentInfo.getID())) {
                        StandardEntity entity = this.worldInfo.getEntity(commandPolice.getTargetID());
                        if (entity instanceof Area) {
                            this.result = entity.getID();
                            this.targetAreas.add(this.result);
                        } else if (entity instanceof Human) {
                            Human human = (Human)entity;
                            if (human.isPositionDefined()) {
                                this.result = human.getPosition();
                                this.targetAreas.add(this.result);
                            }
                        } else if (entity instanceof Blockade) {
                            Blockade blockade = (Blockade)entity;
                            if (blockade.isPositionDefined()) {
                                this.result = blockade.getPosition();
                                this.targetAreas.add(this.result);
                            }
                        }
                    }
                } else {
                    StandardEntity target = this.worldInfo.getEntity(commandPolice.getTargetID());
                    if (target instanceof Human) {
                        target = this.worldInfo.getPosition((Human)target);
                    } else if (target instanceof Blockade) {
                        target = this.worldInfo.getPosition((Blockade)target);
                    }
                    if (this.worldInfo.getDistance(target, this.agentInfo.me()) < 50000) {
                        List<EntityID> nearPolice = new ArrayList<>();
                        for (EntityID id : change) {
                            if (this.worldInfo.getEntity(id) instanceof PoliceForce) {
                                nearPolice.add(id);
                            }
                        }
                        nearPolice.add(this.agentInfo.getID());
                        if (this.getClosestEntityID(nearPolice, target.getID()).equals(this.agentInfo.getID())) {
                            this.result = target.getID();
                            this.targetAreas.add(this.result);
                        }
                    }
                }
            }
        }
        //reflect MessageFireBrigade
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageFireBrigade.class)) {
            MessageFireBrigade messageFireBrigade = (MessageFireBrigade)message;
            if (messageFireBrigade.isPositionDefined()) {
                if (this.worldInfo.getDistance(this.agentInfo.getID(), messageFireBrigade.getPosition()) < 50000) {
                    if(messageFireBrigade.getAction() == MessageFireBrigade.ACTION_REFILL) {
                        StandardEntity target = this.worldInfo.getEntity(messageFireBrigade.getTargetID());
                        if(target instanceof Building) {
                            this.priorityRoads.addAll(this.getAllEntrancesOfBuilding((Building)target));
                        } else if(target.getStandardURN() == StandardEntityURN.HYDRANT) {
                            this.priorityRoads.add(target.getID());
                        }
                    }
                } else if (messageFireBrigade.getAction() == MessageFireBrigade.ACTION_EXTINGUISH) {
                    EntityID target = messageFireBrigade.getTargetID();
                    if (this.worldInfo.getDistance(messageFireBrigade.getPosition(), target)
                            > this.scenarioInfo.getFireExtinguishMaxDistance()) {
                        this.priorityRoads.add(this.getClosestEntityID(
                                this.worldInfo.getObjectIDsInRange(target, this.scenarioInfo.getFireExtinguishMaxDistance()),
                                this.agentInfo.getID()));
                    }
                }
            }
        }
        //reflect MessageAmbulanceTeam
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageAmbulanceTeam.class)) {
            MessageAmbulanceTeam messageAmbulanceTeam = (MessageAmbulanceTeam)message;
            if (messageAmbulanceTeam.isPositionDefined()) {
                if (this.worldInfo.getDistance(this.agentInfo.getID(), messageAmbulanceTeam.getPosition()) < 50000) {
                    if (messageAmbulanceTeam.getAction() == MessageAmbulanceTeam.ACTION_MOVE) {
                        StandardEntity target = this.worldInfo.getEntity(messageAmbulanceTeam.getTargetID());
                        if (target instanceof Building) {
                            this.priorityRoads.add(this.getClosestEntityID(
                                    this.getAllEntrancesOfBuilding((Building)target), this.agentInfo.getID()));
                        } else if (target instanceof Human) {
                            Human human = (Human)target;
                            if (human.isPositionDefined()) {
                                Area area = (Area)this.worldInfo.getPosition(human);
                                if (area instanceof Building) {
                                    this.priorityRoads.add(this.getClosestEntityID(
                                            this.getAllEntrancesOfBuilding((Building)area), this.agentInfo.getID()));
                                }
                            }
                        }
                    }
                }
            }
        }
        this.priorityRoads.removeAll(this.clearedAreas);
    }

    private void updateResult() {
        if (this.result != null) {
            if (!this.targetAreas.contains(this.result)) {
                this.result = null;
                return;
            }
            if (this.agentInfo.getPosition().equals(this.result) ||
                    this.worldInfo.getChanged().getChangedEntities().contains(this.result)) {
                StandardEntity entity = this.worldInfo.getEntity(this.result);
                if (entity instanceof Refuge) {
                    if (((Human)this.agentInfo.me()).getDamage() <= 0) {
                        this.targetAreas.remove(this.result);
                        this.priorityRoads.remove(this.result);
                        this.presetTargets.remove(this.result);
                        this.clearedAreas.add(this.result);
                        this.result = null;
                    }
                } else if (entity instanceof Building) {
                    this.targetAreas.remove(this.result);
                    this.priorityRoads.remove(this.result);
                    this.presetTargets.remove(this.result);
                    this.clearedAreas.add(this.result);
                    this.result = null;
                } else if (entity instanceof Road) {
                    Road road = (Road)entity;
                    if (this.isPassable(road)) {
                        this.clearedAreas.add(this.result);
                        this.priorityRoads.remove(this.result);
                        this.presetTargets.remove(this.result);
                        if (!this.stuckAgentRoads.contains(this.result)) {
                            this.targetAreas.remove(this.result);
                            this.result = null;
                        }
                    }
                }
            }
        }
    }

    //Entity Utils
    private Set<EntityID> getAllEntrancesOfBuilding(Building building) {
        EntityID buildingID = building.getID();
        Set<EntityID> entrances = new HashSet<>();
        Set<EntityID> visited = new HashSet<>();
        Stack<EntityID> stack = new Stack<>();
        stack.push(buildingID);
        visited.add(buildingID);
        while (!stack.isEmpty()) {
            EntityID id = stack.pop();
            visited.add(id);
            Area area = (Area)this.worldInfo.getEntity(id);
            if (area instanceof Road) {
                entrances.add(id);
            } else {
                for (EntityID neighbor : area.getNeighbours()) {
                    if (visited.contains(neighbor)) {
                        continue;
                    }
                    stack.push(neighbor);
                }
            }
        }
        return entrances;
    }

    private Set<Edge> getPassableEdge(Area area) {
        Set<Edge> passableEdges = new HashSet<>();
        for (Edge edge : area.getEdges()) {
            if (edge.isPassable()) {
                passableEdges.add(edge);
            }
        }
        return passableEdges;
    }

    private boolean isPassable(Road road) {
    	if (!road.isBlockadesDefined() || road.getBlockades().isEmpty()) {
			return true;
		}
		Collection<Blockade> blockades = this.worldInfo.getBlockades(road).stream().filter(Blockade::isApexesDefined)
				.collect(Collectors.toSet());	
			Line2D guideline = null;
			for(guidelineHelper r : this.judgeRoad) {
				if(r.getSelfID().equals(road.getID())) {
					guideline = r.getGuideline();
				}
			}
			if (guideline != null) {
				for (Blockade blockade : blockades) {
					List<Point2D> Points = GeometryTools2D.vertexArrayToPoints(blockade.getApexes());
					for(int i =0;i<Points.size();++i) {
						if(i!=Points.size()-1) {
							double crossProduct1 = this.getCrossProduct(guideline, Points.get(i));
							double crossProduct2 = this.getCrossProduct(guideline, Points.get(i+1));
							if(crossProduct1<0&&crossProduct2>0 || crossProduct1>0&&crossProduct2<0) {
								Line2D line = new Line2D(Points.get(i),Points.get(i+1));
								Point2D intersect = GeometryTools2D.getIntersectionPoint(line, guideline);
								if(intersect!=null) {
									return false;
								}
							}
						}
						else {
							double crossProduct1 = this.getCrossProduct(guideline, Points.get(i));
							double crossProduct2 = this.getCrossProduct(guideline, Points.get(0));
							if(crossProduct1<0&&crossProduct2>0 || crossProduct1>0&&crossProduct2<0) {
								Line2D line = new Line2D(Points.get(i),Points.get(0));
								Point2D intersect = GeometryTools2D.getIntersectionPoint(line, guideline);
								if(intersect!=null) {
									return false;
								}
							}
						}
					}
				}
				return true;
	        }	
		return false;
	}
    
    private Line2D get_longest_line(Road road) {
		List<Edge> edges = road.getEdges();
		List<Point2D> Points = new ArrayList<>();
		for(Edge edge : edges) {
			Point2D point = new Point2D((edge.getStartX()+edge.getEndX())/2,(edge.getStartY()+edge.getEndY())/2);
			Points.add(point);
		}
		Point2D start = null;
		Point2D end = null;
		double max = Double.MIN_VALUE;
		
		for(int i = 0; i < Points.size(); ++i) {
			for(int j = 0; j < Points.size(); ++j) {
				if(j==i) continue;
				else {
					double dist = this.getDistance(Points.get(i).getX(), Points.get(i).getY(), Points.get(j).getX(),Points.get(j).getY());
					if(dist > max) {
						max = dist;
						start = Points.get(i);
						end = Points.get(j);
					}
				}
			}
		}
		if(start!=null && end!=null) {
			Line2D guideline = new Line2D(start,end);
			return guideline;
		}
		else return null;
    }

    private double getCrossProduct(Line2D line , Point2D point) {
    	
    	double X = point.getX();
    	double Y = point.getY();
    	double X1 = line.getOrigin().getX();
    	double Y1 = line.getOrigin().getY();
    	double X2 = line.getEndPoint().getX();
    	double Y2 = line.getEndPoint().getY();
    	
    	return ((X2 - X1)*(Y - Y1) - (X - X1)*(Y2 - Y1));
    }
    
    private void create_guideline() {
    	
		for(StandardEntity se : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
			Road road = (Road) se;
    		for(EntityID neighbour : road.getNeighbours()) {
    			if(this.worldInfo.getEntity(neighbour) instanceof Building || this.worldInfo.getEntity(neighbour) instanceof Refuge) {
    				guidelineHelper entrance = new guidelineHelper(this.get_longest_line(road),road);
    				if(!this.judgeRoad.contains(entrance))	this.judgeRoad.add(entrance);
    				for(EntityID id : road.getNeighbours()) {
    	    			StandardEntity entity = this.worldInfo.getEntity(id);
    	    			if(entity instanceof Road) {
    	    				Road next = (Road) entity;
    	    				guidelineHelper line = new guidelineHelper(this.get_longest_line(next),next);
    	    				if(!this.judgeRoad.contains(line)) this.judgeRoad.add(line);
    	    			}
    				}
    			}
    		}
		}
    	    	
    	StandardEntity positionEntity = this.worldInfo.getEntity(this.agentInfo.getPosition());
    	
    	if(positionEntity instanceof Road) {
    		Road position = (Road) positionEntity;
    		guidelineHelper guideline = new guidelineHelper(this.get_longest_line(position),position);
    		if(!this.judgeRoad.contains(guideline))	this.judgeRoad.add(guideline);
    		
    		for(EntityID neighbour : position.getNeighbours()) {
    			if(this.worldInfo.getEntity(neighbour) instanceof Road) {
    				Road road = (Road) this.worldInfo.getEntity(neighbour);
    				Edge edge = position.getEdgeTo(neighbour);
    				Point2D start = new Point2D((edge.getStartX()+edge.getEndX())/2,(edge.getStartY()+edge.getEndY())/2);
    				Point2D mid = new Point2D(road.getX(),road.getY());
    				guidelineHelper line = new guidelineHelper(road,start,mid);
    				if(!this.judgeRoad.contains(line))	this.judgeRoad.add(line);
    			}
    		}
    		
    		for(StandardEntity se : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
				boolean flag = false;
				Road otherRoads = (Road) se;

    			this.pathPlanning.setFrom(position.getID());
    			this.pathPlanning.setDestination(se.getID());
    			List<EntityID> path = this.pathPlanning.calc().getResult();
    			if(path!=null && path.size()>2) {
    				for(int i = 1 ; i < path.size() - 1 ; ++i ) {
    					StandardEntity entity = this.worldInfo.getEntity(path.get(i));
    					if(!(entity instanceof Road)) continue;
    					Road road = (Road) entity;
    					Area before = (Area) this.worldInfo.getEntity(path.get(i-1));
    					Area next = (Area) this.worldInfo.getEntity(path.get(i+1));
    					Edge edge1 = before.getEdgeTo(road.getID());
    					Edge edge2 = road.getEdgeTo(next.getID());
    					Point2D start = new Point2D((edge1.getStartX()+edge1.getEndX())/2,(edge1.getStartY()+edge1.getEndY())/2);
    					Point2D end = new Point2D((edge2.getStartX()+edge2.getEndX())/2,(edge2.getStartY()+edge2.getEndY())/2);
        				guidelineHelper line = new guidelineHelper(road,start,end);
        				if(!this.judgeRoad.contains(line))	this.judgeRoad.add(line);
    				}
    			}
    		}
    	}
    	
    	else if(positionEntity instanceof Building) {
    		Building building = (Building) positionEntity;
    		
    		for(StandardEntity se : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
    			this.pathPlanning.setFrom(building.getID());
    			this.pathPlanning.setDestination(se.getID());
    			List<EntityID> path = this.pathPlanning.calc().getResult();
    			if(path!=null && path.size()>2) {
    				for(int i = 1 ; i < path.size() - 1 ; ++i ) {
    					StandardEntity entity = this.worldInfo.getEntity(path.get(i));
    					if(!(entity instanceof Road)) continue;
    					Road road = (Road) entity;
    					Area before = (Area) this.worldInfo.getEntity(path.get(i-1));
    					Area next = (Area) this.worldInfo.getEntity(path.get(i+1));
    					Edge edge1 = before.getEdgeTo(road.getID());
    					Edge edge2 = road.getEdgeTo(next.getID());
    					Point2D start = new Point2D((edge1.getStartX()+edge1.getEndX())/2,(edge1.getStartY()+edge1.getEndY())/2);
    					Point2D end = new Point2D((edge2.getStartX()+edge2.getEndX())/2,(edge2.getStartY()+edge2.getEndY())/2);
        				guidelineHelper line = new guidelineHelper(road,start,end);
        				if(!this.judgeRoad.contains(line))	this.judgeRoad.add(line);
    				}
    			}
    		}
    	}
    	
		for(StandardEntity se : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
			Road road = (Road) se;
			guidelineHelper line = new guidelineHelper(this.get_longest_line(road),road);
			if(!this.judgeRoad.contains(line)) this.judgeRoad.add(line);
		}
    }
    
    private double getDistance(double fromX, double fromY, double toX, double toY) {
    	double dx = fromX - toX;
    	double dy = fromY - toY;
    	return Math.hypot(dx, dy);
    }


    private boolean isHumanStucked(Human human, Road road) {
        if (!road.isBlockadesDefined() || road.getBlockades().isEmpty()) {
            return false;
        }
        for (Blockade blockade : this.worldInfo.getBlockades(road)) {
            int hX = human.getX();
            int hY = human.getY();
            if (this.isInside(hX, hY, blockade.getApexes()) ||
                    this.isNearBlockade(hX, hY, blockade, this.stuckRange)) {
                return true;
            }
        }
        return false;
    }

    //Geom Utils
    private boolean isNearBlockade(double pX, double pY, Blockade blockade, double range) {
        int[] apex = blockade.getApexes();
        for (int i = 0; i < apex.length - 4; i += 2) {
            if(java.awt.geom.Line2D.ptLineDist(apex[i], apex[i + 1], apex[i + 2], apex[i + 3], pX, pY) < range) {
                return true;
            }
        }
        if (java.awt.geom.Line2D.ptLineDist(apex[0], apex[1], apex[apex.length - 2], apex[apex.length - 1], pX, pY) < range) {
            return true;
        }
        return false;
    }

    private boolean isInside(double pX, double pY, int[] apex) {
        Point2D p = new Point2D(pX, pY);
        Vector2D v1 = (new Point2D(apex[apex.length - 2], apex[apex.length - 1])).minus(p);
        Vector2D v2 = (new Point2D(apex[0], apex[1])).minus(p);
        double theta = this.getAngle(v1, v2);

        for(int i = 0; i < apex.length - 2; i += 2) {
            v1 = (new Point2D(apex[i], apex[i + 1])).minus(p);
            v2 = (new Point2D(apex[i + 2], apex[i + 3])).minus(p);
            theta += this.getAngle(v1, v2);
        }
        return Math.round(Math.abs((theta / 2) / Math.PI)) >= 1;
    }

    private double getAngle(Vector2D v1, Vector2D v2) {
        double flag = (v1.getX() * v2.getY()) - (v1.getY() * v2.getX());
        double angle = Math.acos(((v1.getX() * v2.getX()) + (v1.getY() * v2.getY())) / (v1.getLength() * v2.getLength()));
        if(flag > 0) {
            return angle;
        }
        if(flag < 0) {
            return -1 * angle;
        }
        return 0.0D;
    }

    private boolean intersect(double agentX, double agentY, double pointX, double pointY, Blockade blockade) {
        List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(blockade.getApexes()), true);
        for(Line2D line : lines) {
            Point2D start = line.getOrigin();
            Point2D end = line.getEndPoint();
            double startX = start.getX();
            double startY = start.getY();
            double endX = end.getX();
            double endY = end.getY();
            if(java.awt.geom.Line2D.linesIntersect(
                    agentX, agentY, pointX, pointY,
                    startX, startY, endX, endY
            )) {
                return true;
            }
        }
        return false;
    }

    private Point2D getMidPoint(Line2D line) {
        return new Point2D((line.getOrigin().getX() + line.getEndPoint().getX()) / 2,
                (line.getOrigin().getY() + line.getEndPoint().getY()) / 2);
    }

    //Sorter & Selector
    private EntityID getClosestEntityID(Collection<EntityID> IDs, EntityID reference) {
        if (IDs.isEmpty()) {
            return null;
        }
        double minDistance = Double.MAX_VALUE;
        EntityID closestID = null;
        for (EntityID id : IDs) {
            double distance = this.worldInfo.getDistance(id, reference);
            if (distance < minDistance) {
                minDistance = distance;
                closestID = id;
            }
        }
        return closestID;
    }

    private class DistanceIDSorter implements Comparator<EntityID> {
        private WorldInfo worldInfo;
        private EntityID reference;

        DistanceIDSorter(WorldInfo worldInfo, EntityID reference) {
            this.worldInfo = worldInfo;
            this.reference = reference;
        }

        DistanceIDSorter(WorldInfo worldInfo, StandardEntity reference) {
            this.worldInfo = worldInfo;
            this.reference = reference.getID();
        }

        public int compare(EntityID a, EntityID b) {
            int d1 = this.worldInfo.getDistance(this.reference, a);
            int d2 = this.worldInfo.getDistance(this.reference, b);
            return d1 - d2;
        }
    }
}

