package CSU_Yunlu_2019.extaction.pf;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import CSU_Yunlu_2019.exception.ActionCommandException;
import CSU_Yunlu_2019.extaction.pf.guidelineHelper;
import CSU_Yunlu_2019.module.route.pov.POVRouter;
import CSU_Yunlu_2019.standard.CSUEdgeHelper;
import CSU_Yunlu_2019.standard.CSURoadHelper;
import CSU_Yunlu_2019.standard.CriticalArea;
import CSU_Yunlu_2019.standard.EntranceHelper;
import CSU_Yunlu_2019.standard.Ruler;
import CSU_Yunlu_2019.util.CircleQueue;
import CSU_Yunlu_2019.util.CircleStack;
import CSU_Yunlu_2019.util.Util;
//import PF_CSUpfRoadDetector.sorter;
import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.action.common.ActionRest;
import adf.agent.action.police.ActionClear;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.information.MessageRoad;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.algorithm.Clustering;
import javolution.util.FastMap;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.standard.messages.StandardMessageURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

public class ActionExtClear extends ExtAction {

	protected double x, y, time;
	int lastTime = 0;
	double lastx  =0 ;
	double lasty = 0;
	protected double repairDistance;
	private List<guidelineHelper> judgeRoad = new ArrayList<>();
	protected EntranceHelper entrance;
	protected Map<EntityID, CSURoadHelper> csuRoadMap = new FastMap<>();

	protected int lastClearDest_x = -1, lastClearDest_y = -1;
	protected int count = 0, lock = 4, reverseLock = 4;

	protected CriticalArea criticalArea = null;
	protected Set<EntityID> stuckedAgentList = new HashSet<>();

	private PathPlanning pathPlanning;
	private Clustering clustering;
        //清理点
	private int oldClearX = 0, oldClearY = 0;
        //清理距离
	private int clearDistance;
        //被迫的行动
	private int forcedMove;
        //需要休息时的阈值
	private int thresholdRest;
        //时间
	private int kernelTime;
	private Action mayRepeat = null;
	private boolean needCheckForRepeat = false;

	private final int actionStoreNumber = 10;
	private CircleQueue<Action> actionHistory = new CircleQueue<>(actionStoreNumber);
	private CircleStack<Action> actionStack = new CircleStack<>(actionStoreNumber);

	private POVRouter router;
	private Random random;
        //目标
	private EntityID target;
	private Map<EntityID, Set<Point2D>> movePointCache;

	protected List<EntityID> lastCyclePath;
	protected List<EntityID> lastlastCyclePath;

	private Blockade lastClearTarget = null;
	
	
	private Action lastAction = null;
	private int lasttime = 0;
	
	MessageManager messageManager=null;
	
	public static final String KEY_JUDGE_ROAD = "ActionExtClear.judge_road";
	public static final String KEY_START_X = "ActionExtClear.start_x";
	public static final String KEY_START_Y = "ActionExtClear.start_y";
	public static final String KEY_END_X = "ActionExtClear.end_x";
	public static final String KEY_END_Y = "ActionExtClear.end_y";
	public static final String KEY_ROAD_SIZE = "ActionExtClear.road_size";

	private int roadsize;
	
	public ActionExtClear(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager,
						  DevelopData developData) {
		super(ai, wi, si, moduleManager, developData);
		this.clearDistance = si.getClearRepairDistance();
		this.forcedMove = developData.getInteger("ActionExtClear.forcedMove", 3);
		this.thresholdRest = developData.getInteger("ActionExtClear.rest", 100);

		this.target = null;
		this.movePointCache = new HashMap<>();
		this.count = 0;

		this.time = ai.getTime();
		this.x = agentInfo.getX();
		this.y = agentInfo.getY();

		this.random = new Random(agentInfo.getID().getValue());

		this.repairDistance = scenarioInfo.getClearRepairDistance();

		//System.out.print();
		
		switch (si.getMode()) {
		case PRECOMPUTATION_PHASE:
			this.pathPlanning = moduleManager.getModule("ActionExtClear.PathPlanning",
					"adf.sample.module.algorithm.SamplePathPlanning");
			break;
		case PRECOMPUTED:
			this.pathPlanning = moduleManager.getModule("ActionExtClear.PathPlanning",
					"adf.sample.module.algorithm.SamplePathPlanning");
			break;
		case NON_PRECOMPUTE:
			this.pathPlanning = moduleManager.getModule("ActionExtClear.PathPlanning",
					"adf.sample.module.algorithm.SamplePathPlanning");
			break;
		}
		this.clustering = moduleManager.getModule("ActionExtClear.Clustering",
					"adf.sample.module.algorithm.SampleKMeans");
		
	}

	@Override
	public ExtAction precompute(PrecomputeData precomputeData) {
		super.precompute(precomputeData);
		if (this.getCountPrecompute() >= 2) {
			return this;
		}
		this.pathPlanning.precompute(precomputeData);
		this.clustering.precompute(precomputeData);

		entrance = new EntranceHelper(worldInfo);
		
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
 

		Road road;
		CSURoadHelper csuRoad;
		for (StandardEntity entity : worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
			road = (Road) entity;
			csuRoad = new CSURoadHelper(road, worldInfo, scenarioInfo);
			this.csuRoadMap.put(entity.getID(), csuRoad);
		}
		router = new POVRouter(agentInfo, worldInfo, scenarioInfo, csuRoadMap);
		criticalArea = new CriticalArea(worldInfo);

		try {
			this.kernelTime = this.scenarioInfo.getKernelTimesteps();
		} catch (NoSuchConfigOptionException e) {
			this.kernelTime = -1;
		}
		return this;
	}

	@Override
	public ExtAction resume(PrecomputeData precomputeData) {
		super.resume(precomputeData);
		if (this.getCountResume() >= 2) {
			return this;
		}
		this.pathPlanning.resume(precomputeData);
		this.clustering.resume(precomputeData);
		
		entrance = new EntranceHelper(worldInfo);
		
		
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


		Road road;
		CSURoadHelper csuRoad;
		for (StandardEntity entity : worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
			road = (Road) entity;
			csuRoad = new CSURoadHelper(road, worldInfo, scenarioInfo);
			this.csuRoadMap.put(entity.getID(), csuRoad);
		}
		router = new POVRouter(agentInfo, worldInfo, scenarioInfo, csuRoadMap);
		criticalArea = new CriticalArea(worldInfo);

		try {
			this.kernelTime = this.scenarioInfo.getKernelTimesteps();
		} catch (NoSuchConfigOptionException e) {
			this.kernelTime = -1;
		}

		return this;
	}

	@Override
	public ExtAction preparate() {
		super.preparate();
		if (this.getCountPreparate() >= 2) {
			return this;
		}
		this.pathPlanning.preparate();
		this.clustering.preparate();

		entrance = new EntranceHelper(worldInfo);
		this.create_guideline();
		
		Road road;
		CSURoadHelper csuRoad;
		for (StandardEntity entity : worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
			road = (Road) entity;
			csuRoad = new CSURoadHelper(road, worldInfo, scenarioInfo);
			this.csuRoadMap.put(entity.getID(), csuRoad);
		}
		router = new POVRouter(agentInfo, worldInfo, scenarioInfo, csuRoadMap);
		criticalArea = new CriticalArea(worldInfo);

		try {
			this.kernelTime = this.scenarioInfo.getKernelTimesteps();
		} catch (NoSuchConfigOptionException e) {
			this.kernelTime = -1;
		}
		return this;
	}

	/**
	 * <pre>
	 * Those are entities can be regared as Humanoids which are 
	 *  	Ambulance Team
	 *  	Fire Brigade
	 *  	Police Force
	 *  	Civilian.
	 * </pre>
	 */
	public static final EnumSet<StandardEntityURN> HUMANOIDS = EnumSet.of(StandardEntityURN.AMBULANCE_TEAM,
			StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE, StandardEntityURN.CIVILIAN);

	public void update(Human me, ChangeSet changed) {

		for (StandardEntity se : getEntitiesOfType(worldInfo, HUMANOIDS)) {
			Human hm = (Human) se;
			if (worldInfo.getPosition(me).equals(worldInfo.getPosition(hm))
					&& !changed.getChangedEntities().contains(hm.getID())) {
				hm.undefinePosition();
			}
		}

		for (StandardEntity se : worldInfo.getEntitiesOfType(StandardEntityURN.BLOCKADE)) {
			Blockade block = (Blockade) se;
			if (me.getPosition().equals(block.getPosition()) && !changed.getChangedEntities().contains(block.getID())) {
				block.undefinePosition();
			}
		}

		this.criticalArea.update(router);

		for (EntityID next : changed.getChangedEntities()) {
			StandardEntity entity = worldInfo.getEntity(next);
			if (entity instanceof Road) {
				CSURoadHelper road = getCsuRoad(next);
				road.update();
			}

			if (entity instanceof AmbulanceTeam || entity instanceof FireBrigade) {
				if (isStucked((Human) entity))
					stuckedAgentList.add(next);
				else
					stuckedAgentList.remove(next);
			}
		}
	}

	@Override
	public ExtAction updateInfo(MessageManager messageManager) {
		super.updateInfo(messageManager);
		this.messageManager= messageManager;
		if (this.getCountUpdateInfo() >= 2) {
			return this;
		}

		update((Human) (agentInfo.me()), worldInfo.getChanged());

		this.time = agentInfo.getTime();
		this.x = agentInfo.getX();
		this.y = agentInfo.getY();

		this.pathPlanning.updateInfo(messageManager);
		this.clustering.updateInfo(messageManager);
		
		return this;
	}

	
	
	//**********************************************************try whether the target is from RoadDetector and randWalk is often used************************************************
	@Override
	public ExtAction setTarget(EntityID target) {

		int clusterIndex = this.clustering.getClusterIndex(this.agentInfo.getID());
		this.target = null;
		StandardEntity entity = this.worldInfo.getEntity(target);
		if (entity != null) {
			if (entity instanceof Road) {
				this.target = target;
			} else if (entity.getStandardURN().equals(StandardEntityURN.BLOCKADE)) {
				this.target = ((Blockade) entity).getPosition();
			} else if (entity instanceof Building) {
				this.target = target;
			}
		}
		return this;
	}

	
	 private StandardEntity getClosestEntity(Collection<? extends StandardEntity> entities, StandardEntity reference) {
	        if (entities.isEmpty()) {
	            return null;
	        }
	        double minDistance = Double.MAX_VALUE;
	        StandardEntity closestEntity = null;
	        for (StandardEntity entity : entities) {
	            double distance = this.worldInfo.getDistance(reference, entity);
	            if (distance < minDistance) {
	                minDistance = distance;
	                closestEntity = entity;
	            }
	        }
	        return closestEntity;
	    }
	
	 HashSet <Blockade> already_clear_blocked = new HashSet<>();
	 
		private boolean is_no_move(){
			double currentx = this.agentInfo.getX();
			double currenty = this.agentInfo.getY();
			int currentTime = this.agentInfo.getTime();
			if(currentx > lastx - 200 &&currentx < lastx + 200 && currenty > lasty-200 && currenty < lasty+200 && (currentTime-this.lastTime>5)){
				this.lastTime = currentTime;
				return true;
			}
			else{
				this.lastx = currentx;
				this.lasty = currenty;
				return false;
			}	
		}
	 
	protected Action randomWalk(){
		
		//System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%      use the Function : randWalk() %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		
		
		if(this.target == null && this.lastClearTarget != null) this.target = this.lastClearTarget.getID();
		if(this.target != null) {
			
			if(this.agentInfo.getPosition().equals(this.target)){
				Collection<Blockade> blockades = null;
				if(this.worldInfo.getEntity(target).getStandardURN() == StandardEntityURN.ROAD)
					 blockades= this.worldInfo.getBlockades(this.target);
				if(blockades != null) {
					if(!blockades.isEmpty()){
						
						Set<Blockade> covers = new HashSet<>();
						
						if (blockades != null)
							for (Blockade blockade : blockades) {
								covers.add(blockade);
							}
						if (!covers.isEmpty()) { //if police is covered by blockade, clear it first
							Blockade block = (Blockade) this.getClosestEntity(covers, this.agentInfo.me());
							while(!covers.isEmpty() && this.already_clear_blocked.contains(block)){
								covers.remove(block);
								block = (Blockade) this.getClosestEntity(covers, this.agentInfo.me());
							}
							this.already_clear_blocked.add(block);
						if(block != null) return new ActionClear(block);//(int)block.getX(),(int)block.getY());
						}
						
					}
				}
			StandardEntity entity = this.worldInfo.getEntity(this.target);
			Pair<Integer, Integer> location = getSelfLocation();
			List<EntityID> path = router.getAStar((Area) getSelfPosition(), (Area)entity, router.getNormalCostFunction(),
					new Point(location.first(), location.second()));
			//System.out.println("new ActionMove(path)+path.size():"+path.size());
			if(path.size() > 1) return new ActionMove(path);
		}
		}
		Set<EntityID> blockedRoads = new HashSet<>();
		
		
		
		for (EntityID i : this.worldInfo.getChanged().getChangedEntities()) {
			StandardEntity entity = (StandardEntity)this.worldInfo.getEntity(i);
			if(entity instanceof Road){
				Road road = (Road) entity;
				if (road.isBlockadesDefined() && !road.getBlockades().isEmpty()){
						blockedRoads.add(entity.getID());
				}
			}
		}
		
		for (StandardEntity entity  : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
			if(entity instanceof Road){
				Road road = (Road) entity;
				if (road.isBlockadesDefined() && !road.getBlockades().isEmpty()){
						blockedRoads.add(entity.getID());
				}
			}
		}
			
		this.target = get_result_from_set(blockedRoads);
		
		//System.out.println("this.target:"+this.target);
		
		if(this.target == null){
			this.target = this.agentInfo.getPosition();
		}else{
			while(!blockedRoads.isEmpty()){
			Collection<Blockade> blockades = null;
			if(this.worldInfo.getEntity(target).getStandardURN() == StandardEntityURN.ROAD)
				 blockades= this.worldInfo.getBlockades(this.target);
			
			 Set<Blockade> covers = new HashSet<>();
			 
			 if (blockades != null)
			 for (Blockade blockade : blockades) {
		                covers.add(blockade);
		        }
		        if (!covers.isEmpty()) { 
		        	
		        	Blockade block = (Blockade) this.getClosestEntity(covers, this.agentInfo.me());
		        	while(!covers.isEmpty() && this.already_clear_blocked.contains(block)){
		        		covers.remove(block);
		        		block = (Blockade) this.getClosestEntity(covers, this.agentInfo.me());
		        	}
		        	this.already_clear_blocked.add(block);
		        	 //System.out.println("choose the nearest blockeds");
		            if(block != null) return new ActionClear((int)block.getX(),(int)block.getY());
		        
		            //return new ActionClear((Blockade) this.getClosestEntity(covers, this.agentInfo.me()));
		        }
		        blockedRoads.remove(this.target);
		        this.target = get_result_from_set(blockedRoads);
			}
		}
		
		if(this.agentInfo.getPosition().equals(this.target)){
			Collection<Blockade> blockades = null;
			if(this.worldInfo.getEntity(target).getStandardURN() == StandardEntityURN.ROAD)
				 blockades= this.worldInfo.getBlockades(this.target);
			
			 Set<Blockade> covers = new HashSet<>();
			 
			 if (blockades != null)
			 for (Blockade blockade : blockades) {
		                covers.add(blockade);
		        }
		        if (!covers.isEmpty()) { 
		        	
		        	Blockade block = (Blockade) this.getClosestEntity(covers, this.agentInfo.me());
		        	while(!covers.isEmpty() && this.already_clear_blocked.contains(block)){
		        		covers.remove(block);
		        		block = (Blockade) this.getClosestEntity(covers, this.agentInfo.me());
		        	}
		        	this.already_clear_blocked.add(block);
		        	// System.out.println("choose the nearest blockeds");
		            if(block != null) return new ActionClear(block);//(int)block.getX(),(int)block.getY());
		        
		            //return new ActionClear((Blockade) this.getClosestEntity(covers, this.agentInfo.me()));
		        }
		}
		StandardEntity entity = this.worldInfo.getEntity(this.target);
		Pair<Integer, Integer> location = getSelfLocation();
		List<EntityID> path = router.getAStar((Area) getSelfPosition(), (Area)entity, router.getNormalCostFunction(),
				new Point(location.first(), location.second()));
		

		if (path.size() > 1) return new ActionMove(path);
		
		int clusterIndex = this.get_cluster_Index();
		this.target = get_clustering(clusterIndex);
	
		 entity = this.worldInfo.getEntity(this.target);
		 location = getSelfLocation();
		 path = router.getAStar((Area) getSelfPosition(), (Area)entity, router.getNormalCostFunction(),
				new Point(location.first(), location.second()));
		return new ActionMove(path);
	
	}


	Set<Integer> last_cluster_number = new HashSet<Integer>();
	private int get_cluster_Index(){
	int clusterIndex = this.clustering.getClusterIndex(this.agentInfo.getID());
	int cluster_number = this.clustering.getClusterNumber();
	if(!last_cluster_number.contains(clusterIndex)){
		last_cluster_number.add(clusterIndex);
	}else{
	Random r = new Random(cluster_number);
	while(last_cluster_number.contains(clusterIndex)){
	if(last_cluster_number.size() >= cluster_number) this.last_cluster_number = new HashSet<Integer>();
	clusterIndex = r.nextInt(cluster_number);
	}
	last_cluster_number.add(clusterIndex);
	}
	return clusterIndex;
	}

	private EntityID get_clustering(int clusterIndex){

		List<EntityID> sortList = new ArrayList<>();
		for(EntityID id : this.clustering.getClusterEntityIDs(clusterIndex)){
		if(this.worldInfo.getEntity(id) instanceof Road) sortList.add(id);}
		sortList.sort(new sorter(this.worldInfo,this.agentInfo.getID()));
		return sortList.get(sortList.size()/2);


	}


	
	
	 private EntityID get_result_from_set(Set<EntityID> set) {
			
			if(set == null) return null;
			
			EntityID positionID = this.agentInfo.getPosition();
			if (set.contains(positionID)) {
				return positionID;
			}
			if (!set.isEmpty()) {
				List<EntityID> sortList = new ArrayList<>(set);
				sortList.sort(new sorter(this.worldInfo, this.agentInfo.getID()));
				return sortList.get(0);
			}
			return null;
		}
		
	 private class sorter implements Comparator<EntityID> {
			private WorldInfo worldInfo;
			private EntityID id;

			public sorter(WorldInfo worldInfo, EntityID id) {
				this.worldInfo = worldInfo;
				this.id = id;
			}

			public int compare(EntityID a, EntityID b) {
				int d1 = this.worldInfo.getDistance(this.id, a);
				int d2 = this.worldInfo.getDistance(this.id, b);
				return d1 - d2;
			}
		}

	/*
	 * (non-Javadoc)
	 * 
	 * @see csu.agent.pf.clearStrategy.I_ClearStrategy#blockedClear()
	 * 返回最近的blockade
	 */
	public Blockade blockedClear() {
		Set<Blockade> blockades = new HashSet<>();
		StandardEntity entity = null;
		for (EntityID next : agentInfo.getChanged().getDeletedEntities()) {
			entity = worldInfo.getEntity(next);

			if (entity instanceof Blockade) {
				Blockade bloc = (Blockade) entity;
				// ??
				if (bloc.isApexesDefined() && bloc.getApexes().length < 6)
					continue;
				blockades.add(bloc);
			}
		}

		Blockade nearestBlockade = null;
		double minDistance = repairDistance;
		for (Blockade next : blockades) {
			double distance = findDistanceTo(next, x, y);
			if (distance < minDistance) {
				nearestBlockade = next;
				minDistance = distance;
			}
		}

		return nearestBlockade;
	}

	protected void clearf(boolean clearNeighbour) throws ActionCommandException {
		Area pfarea = (Area) worldInfo.getPosition(agentInfo.getID());
		List<EntityID> blockades = new LinkedList<EntityID>();
		if (pfarea.getBlockades() != null) {
			blockades.addAll(pfarea.getBlockades());
		}

		List<EntityID> neighbours = pfarea.getNeighbours();
		if (clearNeighbour) {
			for (EntityID next : neighbours) {
				StandardEntity entity = worldInfo.getEntity(next);
				if (entity instanceof Road) {
					Road ro = (Road) entity;
					if (ro.isBlockadesDefined())
						blockades.addAll(ro.getBlockades());
				}
			}
		}

		int minDistance = (int) (scenarioInfo.getClearRepairDistance());
		Blockade result = null;
		if (blockades != null) {
			for (EntityID entityID : blockades) {
				Blockade b = (Blockade) worldInfo.getEntity(entityID);
				double d = findDistanceTo(b, x, y);
				// double d = Ruler.getDistance(b.getX(), b.getY(), x, y);
				if (d < minDistance - 10) {
					minDistance = (int) d;
					result = b;
				}
			}
		}

		minDistance = (int) (scenarioInfo.getClearRepairDistance());
		for (EntityID entityID : neighbours) {
			Area neighbourTemp = (Area) worldInfo.getEntity(entityID);

			if (neighbourTemp != null && criticalArea.getAreas().contains(neighbourTemp)) {
				if (neighbourTemp.getBlockades() != null) {
					for (EntityID blockade : neighbourTemp.getBlockades()) {
						Blockade b = (Blockade) worldInfo.getEntity(blockade);
						// double d = findDistanceTo(b, x, y);
						double d = Ruler.getDistance(b.getX(), b.getY(), x, y);
						if (d < minDistance - 10) {
							minDistance = (int) d;
							result = b;
						}
					}
				}
			} else if (neighbourTemp instanceof Road) {
				Road neighbour = (Road) neighbourTemp;
				if (entrance.isEntrance(neighbour)) {
					if (neighbour.getBlockades() != null) {
						for (EntityID blockade : neighbour.getBlockades()) {
							Blockade b = (Blockade) worldInfo.getEntity(blockade);
							// double d = findDistanceTo(b, x, y);
							double d = Ruler.getDistance(b.getX(), b.getY(), x, y);
							if (d < minDistance - 10) {
								minDistance = (int) d;
								result = b;
							}
						}
					}
				}
			}
		}

		if (result != null) {
			// doClear(result);
			// TODO
			// agentInfo.sendClear(worldInfo.getTime(), result.getID());
			throw new ActionCommandException(StandardMessageURN.AK_CLEAR);
		}
	}

	protected Action doClear(Blockade result) {

		List<rescuecore2.misc.geometry.Line2D> lines = GeometryTools2D
				.pointsToLines(GeometryTools2D.vertexArrayToPoints(result.getApexes()), true);
		double best = Double.MAX_VALUE;
		Point2D bestPoint = null;
		Point2D origin = new Point2D(x, y);

		double d = 0;
		for (rescuecore2.misc.geometry.Line2D next : lines) {
			Point2D closest = GeometryTools2D.getClosestPointOnSegment(next, origin);
			d = GeometryTools2D.getDistance(origin, closest);
			if (d < best) {
				best = d;
				bestPoint = closest;
			}
		}
		Vector2D v = bestPoint.minus(new Point2D(x, y));
		v = v.normalised().scale(1000000);// v.dx * 1/length * 1000000

		// TODO find Clear Target
		return new ActionClear((int) (x + v.getX()), (int) (y + v.getY()));

	}

	protected double findDistanceTo(Blockade b, double x, double y) {

		Polygon bloc_pol = Util.getPolygon(b.getApexes());
		Point selfL = new Point((int) x, (int) y);

		return Ruler.getDistance(bloc_pol, selfL);
	}

	public Action noAction() {
		return null;
	}


	/**
	 * @throws ActionCommandException
	 */
	public Action mixingClear() {
		if (lastCyclePath == null) {

			return null;
		}

		boolean needClear = true;
		if (needClear == false) {
			return noAction();
		}

		Point2D selfL = new Point2D(x, y);
		int pathLength = lastCyclePath.size(); 
		StandardEntity cur_entity = worldInfo.getEntity(worldInfo.getPosition(agentInfo.getID()).getID());
		if (!(cur_entity instanceof Area))
			return noAction();
		Area currentArea = (Area) cur_entity;
		double minDistance = Double.MAX_VALUE;
		Blockade nearestBlockade = null;
		int indexInPath = findIndexInPath(lastCyclePath, cur_entity.getID());
		if (indexInPath == pathLength) {
			return noAction();
		} else if (indexInPath == (pathLength - 1)) {
			if (currentArea instanceof Road) {
				CSURoadHelper road = getCsuRoad(worldInfo.getPosition(agentInfo.getID()).getID());// 身处的道路
				if (road.getSelfRoad().isBlockadesDefined()) {
					if (!road.isEntrance() && !criticalArea.isCriticalArea(cur_entity.getID())) {
						List<EntityID> inChangeSetAT_FB = new LinkedList<>();
						List<EntityID> inChangeSetBlockades = new LinkedList<>();

						for (EntityID next : agentInfo.getChanged().getChangedEntities()) {
							StandardEntity entity = worldInfo.getEntity(next);
							if (entity instanceof AmbulanceTeam || entity instanceof FireBrigade) {
								inChangeSetAT_FB.add(next);
							} else if (entity instanceof Blockade) {
								inChangeSetBlockades.add(next);
							}
						}
						HashSet<EntityID> needClearAgent = new HashSet<>();
						for (EntityID agent_id : inChangeSetAT_FB) {
							Human agent = (Human) worldInfo.getEntity(agent_id);
							for (EntityID blockade_id : inChangeSetBlockades) {
								Blockade blockade = (Blockade) worldInfo.getEntity(blockade_id);
								double dis = Ruler.getDistanceToBlock(blockade, new Point(agent.getX(), agent.getY()));
								if (dis < 2000) {
									needClearAgent.add(agent_id);
								}
							}

						}

						if (stuckedAgentList != null)
							for (EntityID entityID : stuckedAgentList) {
								Human agent = (Human) worldInfo.getEntity(entityID);
								if (agent.getPosition().getValue() == cur_entity.getID().getValue()) {
									needClearAgent.add(entityID);
								}

							}
						EntityID closetAgentID = null;
						double mindis = Double.MAX_VALUE;
						for (EntityID entityID : needClearAgent) {
							StandardEntity entity = worldInfo.getEntity(entityID);
							double dis = Ruler.getDistance(getSelfLocation().first(), getSelfLocation().second(),
									worldInfo.getLocation(entity).first(), worldInfo.getLocation(entity).second());
							if (dis < mindis) {
								mindis = dis;
								closetAgentID = entityID;
							}

						}
						if (closetAgentID != null) {
							StandardEntity closetAgent = worldInfo.getEntity(closetAgentID);

							int xcoord = worldInfo.getLocation(closetAgent).first();
							int ycoord = worldInfo.getLocation(closetAgent).second();

							if (mindis < repairDistance - 2500) {

								Vector2D v = new Vector2D(xcoord - x, ycoord - y);
								v = v.normalised().scale(repairDistance - 500);
								int destX = (int) (x + v.getX()), destY = (int) (y + v.getY());

								if (xcoord == x && ycoord == y)
								{
									StandardEntity se = worldInfo.getPosition(agentInfo.getID());
									Area ro = (Area) se;
									for (EntityID e : ro.getBlockades()) {
										StandardEntity en = worldInfo.getEntity(e);
										Blockade nearesttBlockade = null;
										double minnDistance = Double.MAX_VALUE;
										if (en instanceof Blockade) {
											Blockade bloc = (Blockade) en;
											double dis = findDistanceTo(bloc, x, y);

											if (dis < minnDistance) {
												minnDistance = dis;
												nearesttBlockade = bloc;
											}
											if (nearesttBlockade != null) {
												destX = nearesttBlockade.getX();
												destY = nearesttBlockade.getY();
												break;
											}
										}
									}
								}

								return new ActionClear(destX, destY);

							}
						} else {
							lastlastCyclePath = lastCyclePath;
							lastCyclePath = null;
							return noAction();
						}

					}
					for (EntityID next : road.getSelfRoad().getBlockades()) {
						StandardEntity en = worldInfo.getEntity(next);
						if (!(en instanceof Blockade))
							continue;
						Blockade bloc = (Blockade) en;
						if (bloc.isApexesDefined() && bloc.getApexes().length < 6)
							continue;
						double dis = findDistanceTo(bloc, x, y);
						if (dis < minDistance) {
							minDistance = dis;
							nearestBlockade = bloc;
						}
					}

					if (nearestBlockade != null) {
						Action tmp1 = scaleClear(nearestBlockade, 2);
						if (tmp1 != null)
							return tmp1;
					}
					else {
						if (road.isEntrance()) {

							List<EntityID> neigh_bloc = new LinkedList<>();
							for (EntityID neigh : road.getSelfRoad().getNeighbours()) {
								StandardEntity entity = worldInfo.getEntity(neigh);
								if (!(entity instanceof Road))
									continue;
								Road neig_road = (Road) entity;
								if (!neig_road.isBlockadesDefined())
									continue;
								neigh_bloc.addAll(neig_road.getBlockades());
							}

							minDistance = Double.MAX_VALUE;
							nearestBlockade = null;
							for (EntityID next : neigh_bloc) {
								StandardEntity en = worldInfo.getEntity(next);
								if (!(en instanceof Blockade))
									continue;
								Blockade bloc = (Blockade) en;
								if (bloc.isApexesDefined() && bloc.getApexes().length < 6)
									continue;
								double dis = findDistanceTo(bloc, x, y);
								if (dis < minDistance) {
									minDistance = dis;
									nearestBlockade = bloc;
								}
							}

							if (minDistance < repairDistance * 0.5 && nearestBlockade != null) {
								Action tmp2 = scaleClear(nearestBlockade, 5);
								if (tmp2 != null)
									return tmp2;
							}
						}
						lastlastCyclePath = lastCyclePath;
						lastCyclePath = null;
						return noAction();
					}
				}
			}
		}

		if (indexInPath + 1 >= pathLength)
			return noAction();
		EntityID nextArea = lastCyclePath.get(indexInPath + 1);
		Area next_A = getEntity(worldInfo, nextArea, Area.class);
		Area last_A = null;
		if (indexInPath > 0) {
			last_A = getEntity(worldInfo, lastCyclePath.get(indexInPath - 1), Area.class);
		}
		Edge dirEdge = null;
		for (Edge nextEdge : currentArea.getEdges()) {
			if (!nextEdge.isPassable())
				continue;

			if (nextEdge.getNeighbour().getValue() == nextArea.getValue()) {
				dirEdge = nextEdge;
				break;
			}
		}
		if (dirEdge == null)
			return noAction();
		Point2D dirPoint = new Point2D((dirEdge.getStart().getX() + dirEdge.getEnd().getX()) / 2.0,
				(dirEdge.getStart().getY() + dirEdge.getEnd().getY()) / 2.0);

		Set<Blockade> c_a_Blockades = getBlockades(currentArea, next_A, selfL, dirPoint);
		minDistance = Double.MAX_VALUE;
		nearestBlockade = null;
		for (Blockade bloc : c_a_Blockades) {
			double dis = findDistanceTo(bloc, x, y);
			if (dis < minDistance) {
				minDistance = dis;
				nearestBlockade = bloc;
			}
		}

		if (nearestBlockade != null) {
			Action tmp3 = directionClear(nearestBlockade, dirPoint, next_A, 1);
			if (tmp3 != null)
				return tmp3;
		} else {
			Vector2D vector = selfL.minus(dirPoint);
			vector = vector.normalised().scale(repairDistance - 500);
			Line2D line = new Line2D(selfL, vector);
			Point2D r_dirPoint = line.getEndPoint();
			Set<Blockade> c_a_r_Blockades = getBlockades(currentArea, last_A, selfL, r_dirPoint);

			minDistance = Double.MAX_VALUE;
			nearestBlockade = null;
			for (Blockade bloc : c_a_r_Blockades) {
				double dis = findDistanceTo(bloc, x, y);
				if (dis < minDistance) {
					minDistance = dis;
					nearestBlockade = bloc;
				}
			}

			if (nearestBlockade != null) {
				Action tmp4 = reverseClear(nearestBlockade, r_dirPoint, last_A, 1);
				if (tmp4 != null)
					return tmp4;
			}
		}

		for (int i = indexInPath + 1; i <= indexInPath + 5; i++) {
			if (pathLength > i + 1) {
				// Point2D startPoint = dirPoint;
				StandardEntity entity_1 = worldInfo.getEntity(lastCyclePath.get(i));
				StandardEntity entity_2 = worldInfo.getEntity(lastCyclePath.get(i + 1));

				if (entity_1 instanceof Area && entity_2 instanceof Area) {
					Area next_a_1 = (Area) entity_1;
					Area next_a_2 = (Area) entity_2;

					for (Edge edge : next_a_1.getEdges()) {
						if (!edge.isPassable())
							continue;
						if (edge.getNeighbour().getValue() == next_a_2.getID().getValue()) {
							dirPoint = new Point2D((edge.getStartX() + edge.getEndX()) / 2.0,
									(edge.getStartY() + edge.getEndY()) / 2.0);
							break;
						}
					}

					Set<Blockade> n_a_blockades = getBlockades(next_a_1, next_a_2, selfL, dirPoint);

					String str = null;
					for (Blockade n_b : n_a_blockades) {
						if (str == null) {
							str = n_b.getID().getValue() + "";
						} else {
							str = str + ", " + n_b.getID().getValue();
						}
					}
					minDistance = Double.MAX_VALUE;
					nearestBlockade = null;
					for (Blockade bloc : n_a_blockades) {
						double dis = findDistanceTo(bloc, x, y);
						if (dis < minDistance) {
							minDistance = dis;
							nearestBlockade = bloc;
						}
					}

					if (nearestBlockade != null) {
						Action tmp5 = directionClear(nearestBlockade, dirPoint, next_a_2, 2);
						if (tmp5 != null)
							return tmp5;
					}
				}
			} else if (pathLength == i + 1) {

				EntityID endEntityID = lastCyclePath.get(i);
				StandardEntity endEntity = worldInfo.getEntity(endEntityID);
				if (!(endEntity instanceof Road)) {
					continue;
				}
				CSURoadHelper endRoad = getCsuRoad(lastCyclePath.get(i));
				if (!endRoad.isEntrance() && !criticalArea.isCriticalArea(endEntityID)) {

					List<EntityID> inChangeSetAT_FB = new LinkedList<>();
					List<EntityID> inChangeSetBlockades = new LinkedList<>();

					for (EntityID next : worldInfo.getChanged().getChangedEntities()) {
						StandardEntity entity = worldInfo.getEntity(next);
						if (entity instanceof AmbulanceTeam || entity instanceof FireBrigade) {
							inChangeSetAT_FB.add(next);
						} else if (entity instanceof Blockade) {
							inChangeSetBlockades.add(next);
						}
					}
					HashSet<EntityID> needClearAgent = new HashSet<>();
					for (EntityID agent_id : inChangeSetAT_FB) {
						Human agent = (Human) worldInfo.getEntity(agent_id);
						for (EntityID blockade_id : inChangeSetBlockades) {
							Blockade blockade = (Blockade) worldInfo.getEntity(blockade_id);
							double dis = Ruler.getDistanceToBlock(blockade, new Point(agent.getX(), agent.getY()));
							if (dis < 2000) {
								needClearAgent.add(agent_id);
							}
						}

					}
					for (EntityID entityID : stuckedAgentList) {
						Human agent = (Human) worldInfo.getEntity(entityID);
						if ((worldInfo.getEntity(agent.getPosition())) instanceof Building) {
							continue;
						}
						needClearAgent.add(entityID);

					}
					needClearAgent.addAll(stuckedAgentList);
					EntityID closetAgentID = null;
					double mindis = Double.MAX_VALUE;
					for (EntityID entityID : needClearAgent) {
						StandardEntity entity = worldInfo.getEntity(entityID);
						double dis = Ruler.getDistance(getSelfLocation().first(), getSelfLocation().second(),
								worldInfo.getLocation(entity).first(), worldInfo.getLocation(entity).second());
						if (dis < mindis) {
							mindis = dis;
							closetAgentID = entityID;
						}

					}
					if (closetAgentID != null) {

						StandardEntity closetAgent = worldInfo.getEntity(closetAgentID);
						int xcoord = worldInfo.getLocation(closetAgent).first();
						int ycoord = worldInfo.getLocation(closetAgent).second();

						if (mindis < repairDistance - 1500) {

							Vector2D v = new Vector2D(xcoord - x, ycoord - y);
							v = v.normalised().scale(repairDistance - 500);
							int destX = (int) (x + v.getX()), destY = (int) (y + v.getY());

							return new ActionClear(destX, destY);
						}

					}
				} else {
					//System.out.println("stuckedagent=null");///////////
					minDistance = Double.MAX_VALUE;
					nearestBlockade = null;
					StandardEntity entity = worldInfo.getEntity(lastCyclePath.get(i));
					if (!(entity instanceof Road))
						continue;
					Road destRoad = (Road) entity;
					if (!destRoad.isBlockadesDefined())
						continue;
					for (EntityID next : destRoad.getBlockades()) {
						StandardEntity en = worldInfo.getEntity(next);
						if (!(en instanceof Blockade))
							continue;
						Blockade bloc = (Blockade) worldInfo.getEntity(next);
						if (bloc.isApexesDefined() && bloc.getApexes().length < 6)
							continue;
						double dis = findDistanceTo(bloc, x, y);
						if (dis < minDistance) {
							minDistance = dis;
							nearestBlockade = bloc;
						}
					}

					if (nearestBlockade != null) {
						Action action = scaleClear(nearestBlockade, 3);
						if (action != null)
							return action;
					} else {
						break;
					}

				}
			}
		}
		return noAction();
	}

	// 1920451, 355780
	/**
	 * @param target
	 * @param marker
	 * @throws ActionCommandException
	 *             老方法清除路障target
	 */
	private Action scaleClear(Blockade target, int marker) {
		double distance = findDistanceTo(target, x, y);
		// 可清除范围内
		if (distance < repairDistance && agentInfo.getChanged().getChangedEntities().contains(target)) {
			lastClearTarget = null;
			return new ActionClear(target.getID());
		} else {
			int current_I = findIndexInPath(lastCyclePath, getSelfPosition().getID());

			List<EntityID> path = getPathToBlockade(current_I, target);

			if (path.size() > 0) {

				lastClearTarget = target;
				Action action = new ActionMove(path, target.getX(), target.getY());
				lastClearDest_x = -1;
				lastClearDest_y = -1;
				return action;
			}
		}
		return null;
	}

	private Action directionClear(Blockade target, Point2D dirPoint, Area next_A, int marker) {

		if (!agentInfo.getChanged().getChangedEntities().contains(target)) {
			int current_I = findIndexInPath(lastCyclePath, worldInfo.getPosition(agentInfo.getID()).getID());

			List<EntityID> path = getPathToBlockade(current_I, target);

			Point2D movePoint = getMovePoint(target, dirPoint);
			Action tmp;
			if (movePoint != null) {
				tmp = new ActionMove(path, (int) movePoint.getX(), (int) movePoint.getY());
			} else {
				tmp = new ActionMove(path, (int) target.getX(), (int) target.getY());
			}
			lastClearDest_x = -1;
			lastClearDest_y = -1;
			return tmp;
		}

		double dis_to_dir = Math.hypot(dirPoint.getX() - x, dirPoint.getY() - y);
		Vector2D v = new Vector2D(dirPoint.getX() - x, dirPoint.getY() - y);
		v = v.normalised().scale(Math.min(dis_to_dir, repairDistance - 500));
		Point2D t_dir_p = new Point2D(x + v.getX(), y + v.getY());

		Road road = (Road) worldInfo.getEntity(target.getPosition());

		Set<Blockade> t_bloc = getBlockades(road, next_A, new Point2D(x, y), t_dir_p);
		if (t_bloc.size() > 0) {
			if (dis_to_dir < repairDistance) {
				v = v.normalised().scale(repairDistance);
			} else {
				v = v.normalised().scale(dis_to_dir);
			}

			int destX = (int) (x + v.getX()), destY = (int) (y + v.getY());
			timeLock(destX, destY, target);

			return new ActionClear(destX, destY);
		} else {
			int current_I = findIndexInPath(lastCyclePath, getSelfPosition().getID());

			List<EntityID> path = getPathToBlockade(current_I, target);

			String str = null;
			for (EntityID pa : path) {
				if (str == null)
					str = pa.getValue() + "";
				else
					str = str + "," + pa.getValue();
			}

			Point2D movePoint = getMovePoint(target, dirPoint);
			Action tmp;
			if (movePoint != null) {
				tmp = new ActionMove(path, (int) movePoint.getX(), (int) movePoint.getY());
			} else {
				tmp = new ActionMove(path, (int) dirPoint.getX(), (int) dirPoint.getY());
			}
			lastClearDest_x = -1;
			lastClearDest_y = -1;
			return tmp;
		}
	}

	private Action reverseClear(Blockade target, Point2D dirPoint, Area last_A, int marker) {
		if (!agentInfo.getChanged().getChangedEntities().contains(target.getID()))
			return null;

		double dis_to_dir = Math.hypot(dirPoint.getX() - x, dirPoint.getY() - y);
		Vector2D v = new Vector2D(dirPoint.getX() - x, dirPoint.getY() - y);
		v = v.normalised().scale(Math.min(dis_to_dir, repairDistance - 500));
		Point2D t_dir_p = new Point2D(x + v.getX(), y + v.getY());

		Road road = (Road) worldInfo.getEntity(target.getPosition());

		Set<Blockade> t_bloc = getBlockades(road, last_A, new Point2D(x, y), t_dir_p);
		if (t_bloc.size() > 0) {
			if (dis_to_dir < repairDistance) {
				v = v.normalised().scale(repairDistance);
			}

			int destX = (int) (x + v.getX()), destY = (int) (y + v.getY());

			timeLock(destX, destY, target);
			if (reverseTimeLock(destX, destY, target))
				return null;
			return new ActionClear(destX, destY);
		}
		return null;
	}

	/**
	 * @param current_L_I
	 * @param blockade
	 * @return 重新获取路径
	 */
	private List<EntityID> getPathToBlockade(int current_L_I, Blockade blockade) {
		List<EntityID> path = new LinkedList<>();
		if (!blockade.isPositionDefined()) {

			path.add(worldInfo.getPosition(agentInfo.getID()).getID());
			return path;
		}

		EntityID blo_A = blockade.getPosition();
		int b_index = findIndexInPath(lastCyclePath, blo_A);

		if (b_index < lastCyclePath.size()) {
			for (int i = current_L_I; i <= b_index; i++)
				path.add(lastCyclePath.get(i));
		} /*
			 * else { for (int i = current_L_I; i < lastCyclePath.size(); i++)
			 * path.add(lastCyclePath.get(i)); }
			 */
		if (path.isEmpty()) {

			path.add(worldInfo.getPosition(agentInfo.getID()).getID());
		}
		return path;
	}

	public Action doClear(Road road, CSUEdgeHelper dir, Blockade target) {
		return new ActionClear(target.getID());
	}

	private int findIndexInPath(List<EntityID> path, EntityID location) {
		int index = 0;
		for (EntityID next : path) {
			if (location.getValue() == next.getValue())
				break;
			index++;
		}
		return index;
	}

	private Action timeLock(int destX, int destY, Blockade target) {
		Action action;
		if (lastClearDest_x == destX && lastClearDest_y == destY) {
			if (count >= lock) {
				int current_I = findIndexInPath(lastCyclePath, worldInfo.getPosition(agentInfo.getID()).getID());
				List<EntityID> path = getPathToBlockade(current_I, target);

				action = new ActionMove(path, destX, destY);

				lastClearDest_x = -1;
				lastClearDest_y = -1;
				count = 0;
				return action;
			} else {
				count++;
			}
		} else {
			count = 0;
			lastClearDest_x = destX;
			lastClearDest_y = destY;
		}
		return null;
	}

	private boolean reverseTimeLock(int destX, int destY, Blockade target) {
		if (lastClearDest_x == destX && lastClearDest_y == destY) {
			if (count >= reverseLock) {
				destX = -1;
				destY = -1;
				return true;
			} else {
				count++;
				return false;
			}
		} else {
			count = 0;
			lastClearDest_x = destX;
			lastClearDest_y = destY;
			return false;
		}
	}

	private Set<Blockade> getBlockades(Area current_A, Area next_A, Point2D selfL, Point2D dirPoint) {
		if (current_A instanceof Building && next_A instanceof Building)
			return new HashSet<Blockade>();
		Set<EntityID> allBlockades = new HashSet<>();

		Road currentRoad = null, nextRoad = null;
		if (current_A instanceof Road) {
			currentRoad = (Road) current_A;
		}
		if (next_A instanceof Road) {
			nextRoad = (Road) next_A;
		}

		rescuecore2.misc.geometry.Line2D line = new rescuecore2.misc.geometry.Line2D(selfL, dirPoint);
		rescuecore2.misc.geometry.Line2D[] temp = getParallelLine(line, 500);

		Polygon po_1 = new Polygon();
		po_1.addPoint((int) temp[0].getOrigin().getX(), (int) temp[0].getOrigin().getY());
		po_1.addPoint((int) temp[0].getEndPoint().getX(), (int) temp[0].getEndPoint().getY());
		po_1.addPoint((int) temp[1].getEndPoint().getX(), (int) temp[1].getEndPoint().getY());
		po_1.addPoint((int) temp[1].getOrigin().getX(), (int) temp[1].getOrigin().getY());
		java.awt.geom.Area area = new java.awt.geom.Area(po_1);

		Set<Blockade> results = new HashSet<Blockade>();

		if (currentRoad != null && currentRoad.isBlockadesDefined()) {
			allBlockades.addAll(currentRoad.getBlockades());
		}
		if (nextRoad != null && nextRoad.isBlockadesDefined()) {
			allBlockades.addAll(nextRoad.getBlockades());
		}

		for (EntityID blockade : allBlockades) {
			StandardEntity entity = worldInfo.getEntity(blockade);
			if (entity == null)
				continue;
			if (!(entity instanceof Blockade))
				continue;
			Blockade blo = (Blockade) entity;

			if (!blo.isApexesDefined())
				continue;
			if (blo.getApexes().length < 6)
				continue;
			Polygon po = Util.getPolygon(blo.getApexes());
			java.awt.geom.Area b_Area = new java.awt.geom.Area(po);
			b_Area.intersect(area);
			if (!b_Area.getPathIterator(null).isDone() || blo.getShape().contains(selfL.getX(), selfL.getY()))
				results.add(blo);
		}

		return results;
	}

	/**
	 * Get the parallel lines(both left and right sides) of the given line. The
	 * distance is specified by rad.
	 * 
	 * @param line
	 *            the given line
	 * @param rad
	 *            the distance
	 * @return the two parallel lines of the given line
	 */
	private Line2D[] getParallelLine(Line2D line, int rad) {
		float theta = (float) Math.atan2(line.getEndPoint().getY() - line.getOrigin().getY(),
				line.getEndPoint().getX() - line.getOrigin().getX());
		theta = theta - (float) Math.PI / 2;
		while (theta > Math.PI || theta < -Math.PI) {
			if (theta > Math.PI)
				theta -= 2 * Math.PI;
			else
				theta += 2 * Math.PI;
		}
		int t_x = (int) (rad * Math.cos(theta)), t_y = (int) (rad * Math.sin(theta));

		Point2D line_1_s, line_1_e, line_2_s, line_2_e;
		line_1_s = new Point2D(line.getOrigin().getX() + t_x, line.getOrigin().getY() + t_y);
		line_1_e = new Point2D(line.getEndPoint().getX() + t_x, line.getEndPoint().getY() + t_y);

		line_2_s = new Point2D(line.getOrigin().getX() - t_x, line.getOrigin().getY() - t_y);
		line_2_e = new Point2D(line.getEndPoint().getX() - t_x, line.getEndPoint().getY() - t_y);

		Line2D[] result = { new Line2D(line_1_s, line_1_e), new Line2D(line_2_s, line_2_e) };

		return result;
	}

	private Point2D getMovePoint(Blockade target, Point2D dirPoint) {
		if (target == null || dirPoint == null)
			return null;
		if (!target.isPositionDefined())
			return null;
		EntityID b_location = target.getPosition();

		StandardEntity entity = worldInfo.getEntity(b_location);
		if (!(entity instanceof Area))
			return null;
		Area b_area = (Area) entity;

		Point2D center_p = new Point2D(b_area.getX(), b_area.getY());

		Vector2D vector = center_p.minus(dirPoint);
		vector = vector.normalised().scale(100000);

		center_p = dirPoint.plus(vector);
		// dirPoint = dirPoint.plus(vector);

		Line2D line = new Line2D(dirPoint, center_p);
		// 相交的点
		Set<Point2D> intersections = Util.getIntersections(Util.getPolygon(target.getApexes()), line);

		Point2D farestPoint = null;
		double maxDistance = Double.MIN_VALUE;
		for (Point2D next : intersections) {
			double dis = Ruler.getDistance(dirPoint, next);
			if (dis > maxDistance) {
				maxDistance = dis;
				farestPoint = next;
			}
		}

		if (farestPoint != null) {
			Line2D line_2 = new Line2D(dirPoint, farestPoint);
			line_2 = Util.improveLine(line_2, 500);

			return line_2.getEndPoint();
		}

		return null;
	}

	public Pair<Integer, Integer> getSelfLocation() {
		return worldInfo.getLocation(agentInfo.getID());
	}

	public StandardEntity getSelfPosition() {
		return worldInfo.getPosition(agentInfo.getID());
	}

	public CSURoadHelper getCsuRoad(EntityID entityId) {
		CSURoadHelper road = this.csuRoadMap.get(entityId);
		if (road == null) {
			Entity entity = worldInfo.getEntity(entityId);
			if (entity instanceof Road) {
				road = new CSURoadHelper((Road) (entity), worldInfo, scenarioInfo);
				this.csuRoadMap.put(entityId, road);
			} else if (entity instanceof Building) {
				Building building = (Building) entity;
				List<Edge> edges = building.getEdges();
				for (Edge e : edges) {
					Entity another = worldInfo.getEntity(e.getNeighbour());
					if (another instanceof Road)
						return getCsuRoad(e.getNeighbour());
				}
			}
		}
		return road;
	}

	/** Get all entities of specified types stores in this world model. */
	public Collection<StandardEntity> getEntitiesOfType(WorldInfo worldInfo, EnumSet<StandardEntityURN> urns) {
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
	public static <T extends StandardEntity> T getEntity(WorldInfo world, EntityID id, Class<T> c) {
		StandardEntity entity;
		entity = world.getEntity(id);
		if (c.isInstance(entity)) {
			T castedEntity = c.cast(entity);
			return castedEntity;
		} else {
			return null;
		}
	}

	// TODO to finish it
	@Override
	public ExtAction calc() {
		
		if(this.is_no_move()) {
			 this.result = this.noMoveAction();
			 if(result!=null) {
				return this;
			 }
		}
		
		Action tmp;
		if (isStucked((Human) (agentInfo.me()))) {
			result = clearWhenStuck();
		} else {
				tmp = clear();
			if (tmp instanceof ActionMove)
				lastCyclePath = ((ActionMove) tmp).getPath();
			result = tmp;
			if (needCheckForRepeat) {

				if ( result instanceof ActionMove && mayRepeat instanceof ActionMove && lastCyclePath.size() > 1)
					if (commonMove((ActionMove) result, (ActionMove) mayRepeat)) {
						circleAlart(agentInfo.getTime());
						//result = this.randomWalk();
						if(result!=null) {
							//System.out.println("this is the 1619 return");
							this.check_same_action();
							return this;
						
						}
						if (lastClearTarget != null) {
							if (!worldInfo.getBlockades(lastClearTarget.getID()).isEmpty()) {
								result = doClear(lastClearTarget);
							} else {
								result = directClear();
								if (result == null) {
									result = this.clear();
								}
							}
							lastClearTarget = null;
						} else {
							result = directClear();
							if (result == null) {
								result = this.clear();
							}
						}
						actionHistory.add(result);
						actionStack.push(result);
						if(result  == null) {
							result = this.clear();
						}
						//System.out.println("this is the 1641 return");
						this.check_same_action();
						return this;
					}
			}

			//System.out.print("");
			if (!actionStack.isEmpty()) {
				if (goBack()) {
					if (!needCheckForRepeat) {
						circleWarning(agentInfo.getTime());
						//result = this.randomWalk();
						if(result!=null) {
							//System.out.println("this is the 1652 return");
							if(result instanceof ActionClear) {
								this.check_same_action();
								return this;
							
							}
						}
						needCheckForRepeat = true;
						actionStack.pop();
					} else {
						if (lastClearTarget != null) {
							if (!worldInfo.getBlockades(lastClearTarget.getID()).isEmpty()) {
								result = doClear(lastClearTarget);
							} else {
								result = directClear();
								if (result == null) {
									result = this.clear();
								}
							}
							lastClearTarget = null;
						} else {
							result = directClear();
							if (result == null) {
								result = this.clear();
							}
						}
					}
				} else {
					needCheckForRepeat = false;
				}

			}
			if (result instanceof ActionClear) {
				for (StandardEntity entity : worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)) {
					PoliceForce police = (PoliceForce) entity;
					StandardEntity locate1 = worldInfo.getPosition(police);
					StandardEntity locate2 = worldInfo.getPosition(agentInfo.getID());
					if (locate1 != null && locate1.equals(locate2)) {
						if (agentInfo.getID().getValue() > police.getID().getValue() && isStucked(police)) {
							result = randomWalk();
							break;
						}

					}
					Pair<Integer, Integer> location1 = worldInfo.getLocation(police);
					Pair<Integer, Integer> location2 = worldInfo.getLocation(agentInfo.getID());
					if (agentInfo.getID().getValue() > police.getID().getValue()
							&& Math.abs(location1.first() - location2.first()) < 2
							&& Math.abs(location1.second() - location2.second()) < 2 && !isStucked(police)) {
						result = randomWalk();
						break;
					}
				}
			}
		}
		if(result==null) {
			result = this.clear();
		}
		if(result!=null) {
			actionHistory.add(result);
			actionStack.push(result);
		}
		//if(result instanceof ActionMove) result = randomWalk();
		this.check_same_action();
		//System.out.println("this is the 1754 return");
		return this;
	}
	
	/**
	* @Description: 发送信息，改为以guideline是否被覆盖为判断标准
	*/
	private void send_message(){

		StandardEntity entity = this.worldInfo.getEntity(this.agentInfo.getPosition());
		if(entity instanceof Road) {// 身处的道路
			Road road = (Road) entity;
			boolean passable = this.isRoadPassable(road);
			if (passable)
			{
				this.messageManager.addMessage(new MessageRoad(true,road,null,true,true));
			}
			else
			{
				Collection<Blockade> blockades = worldInfo.getBlockades(road);         //找出最近的障碍物加入消息中
				Blockade block = (Blockade) this.getClosestEntity(blockades, this.agentInfo.me());
				this.messageManager.addMessage(new MessageRoad(true,road,block,false,true)); //不可以通过，选择最近的障碍物加入
			}
		}
	}
	/**
	* @Description: 获取guideline
	* @Author: Bochun-Yue
	* @Date: 3/7/20
	*/
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
	/**
	* @Description: 道路可否通过，以guideline是否被覆盖为判断标准
	* @Author: Bochun-Yue
	* @Date: 3/7/20
	*/
    private boolean isRoadPassable(Road road) {
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
    
	private void check_same_action(){ 
		if(!(result instanceof ActionMove && this.lastAction instanceof ActionMove) 
				&& !(result instanceof ActionClear && this.lastAction instanceof ActionClear) ) {
			this.lastAction = result;
			lasttime = this.agentInfo.getTime();
		}
		else{
			int time = this.agentInfo.getTime();
			if(time - lasttime > 15) {
				result = this.randomWalk();
				lasttime = time;
			}
			this.lastAction = result;
		}
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
			
			for(StandardEntity se : this.worldInfo.getEntitiesOfType(StandardEntityURN.HYDRANT)) {
				Road road = (Road) se;
				guidelineHelper line = new guidelineHelper(this.get_longest_line(road),road);
				if(!this.judgeRoad.contains(line)) this.judgeRoad.add(line);
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
	    

	
	private Action clearWhenStuck() {
		Blockade blockade = isLocateInBlockade((Human) agentInfo.me());
		if (blockade != null) {

			return new ActionClear(blockade.getID());
		} else {
			Action tmp = directClear();
			if (tmp == null)
				tmp = randomWalk();
			return tmp;
		}
	}

	/**
	 * 判断一个platoon agent的xy坐标是否落在一个路障的多边形内
	 * 
	 * @return 当这个plaoon agent的xy坐标落在路障的多边形内返回true。否则，false。
	 */
	protected Blockade isLocateInBlockade(Human human) {
		int x = human.getX();
		int y = human.getY();
		for (EntityID entityID : worldInfo.getChanged().getChangedEntities()) {
			StandardEntity se = worldInfo.getEntity(entityID);
			if (se instanceof Blockade) {
				Blockade blockade = (Blockade) se;
				Shape s = blockade.getShape();
				if (s != null && s.contains(x, y)) {
					return blockade;
				}
			}
		}
		return null;
	}
	
	private Action noMoveAction() {
		StandardEntity se =  this.worldInfo.getEntity(this.agentInfo.getPosition());
		if(se instanceof Road) {
			Road road = (Road) se;
			Collection<Blockade> blockades = this.worldInfo.getBlockades(road).stream().filter(Blockade::isApexesDefined)
					.collect(Collectors.toSet());
			if(blockades != null) {
				if(!blockades.isEmpty()){
					Blockade block = (Blockade) this.getClosestEntity(blockades, this.agentInfo.me());
					if(block != null && this.isNearBlockade(this.agentInfo.getX(),this.agentInfo.getY(), block)) {
//						System.out.println("------------------被挡没动------------");
						return new ActionClear(block);//(int)block.getX(),(int)block.getY());
					}else {
						return this.clear();
					}
				}
			}
					
		}
		return null;
	}
	
	/**
	* @Description: 新的directClear
	* @Author: Bochun-Yue
	* @Date: 3/7/20
	*/
	private Action directClear() {
		Collection<Blockade> blockades = worldInfo.getBlockades(worldInfo.getPosition(agentInfo.getID()).getID());
		PoliceForce police = (PoliceForce) (agentInfo.me());
		StandardEntity PositionEntity = worldInfo.getEntity(police.getPosition());
		StandardEntity targetEntity = worldInfo.getEntity(target);
		if (targetEntity instanceof Building ) {
			if (!targetEntity.equals(worldInfo.getPosition(agentInfo.getID())))
				return null;
		}
		if(PositionEntity instanceof Building ) {
			return null;
		}
		Road road = (Road) PositionEntity;
		if(!road.isBlockadesDefined() || road.getBlockades().isEmpty()) {
//			this.get_next_target(road);
			return noAction();
		}
		if (!blockades.isEmpty()) {
			Blockade clearBlockade = null;
				
			Line2D guideline = null;
			for(guidelineHelper r : this.judgeRoad) {
				if(r.getSelfID().equals(road.getID())) {
					guideline = r.getGuideline();
				}
			}
			
			if (guideline != null) {
				Action action = moveToGuideLine(guideline,road);
	            if (action != null) {
	                return action;
	            }
				double agentX = police.getX();
				double agentY = police.getY();
				clearBlockade = null;
				Point2D intersection = null;
				Double minPointDistance = Double.MAX_VALUE;
				int clearX = 0;
				int clearY = 0;
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
									double dist = this.getDistance(agentX,agentY, intersect.getX(), intersect.getY());
									if(dist<minPointDistance) {
										minPointDistance = dist;
										clearX = (int)intersect.getX();
										clearY = (int)intersect.getY();
										clearBlockade = blockade;
										intersection = intersect;
									}
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
									double dist = this.getDistance(agentX,agentY, intersect.getX(), intersect.getY());
									if(dist<minPointDistance) {
										minPointDistance = dist;
										clearX = (int)intersect.getX();
										clearY = (int)intersect.getY();
										clearBlockade = blockade;
										intersection = intersect;
									}
								}
							}
						}
					}
				}	
				if (clearBlockade != null) {
					if (minPointDistance < this.clearDistance - 1000) {
						Vector2D vector = this.scaleClear(this.getVector(agentX, agentY, clearX, clearY));
						clearX = (int) (agentX + vector.getX());
						clearY = (int) (agentY + vector.getY());
						lastClearTarget = null;
						double dist = this.getDistance(agentX, agentY, clearX, clearY);
						return new ActionClear(clearX, clearY, clearBlockade);
					}
					lastClearTarget = clearBlockade;
					int dX = (int)((intersection.getX() - agentX) / 10);
					int dY = (int)((intersection.getY() - agentY) / 10);
					return new ActionMove(Lists.newArrayList(police.getPosition()), (int)intersection.getX() - dX,(int) intersection.getY() - dY);
				}
	        }			
		}
		return noAction();
	}

	private void circleWarning(int time) {
	}

	private void circleAlart(int time) {
	}

	// TODO
	private boolean goBack() {
		Action lastAction = actionStack.peek();
		if (result != null && result.getClass().equals(ActionMove.class)) {
			if ((lastAction.getClass().equals(ActionMove.class))
					&& goBack((ActionMove) result, (ActionMove) lastAction)) {
				mayRepeat = lastAction;
				return true;
			} else {
				lastAction = actionStack.peekSecend();
				if (lastAction != null) {
					if (lastAction instanceof ActionMove) {
						if (goBack((ActionMove) result, (ActionMove) lastAction))
							mayRepeat = lastAction;
						return true;
					}
				}
				lastAction = actionStack.peekThird();
				if (lastAction != null) {
					if (lastAction instanceof ActionMove) {
						if (goBack((ActionMove) result, (ActionMove) lastAction))
							mayRepeat = lastAction;
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean commonMove(ActionMove a1, ActionMove a2) {
		List<EntityID> path1 = a1.getPath();
		List<EntityID> path2 = a2.getPath();
		// if (path1.get(0).getValue() == path2.get(0).getValue())
		if (path2.get(path2.size() - 1).getValue() == path1.get(path1.size() - 1).getValue())
			return true;
		return false;
	}

	private boolean goBack(ActionMove thisAction, ActionMove lastAction) {
		List<EntityID> path1 = thisAction.getPath();
		List<EntityID> path2 = lastAction.getPath();
		// if (path1.get(0).getValue() == path2.get(path2.size() -
		// 1).getValue())
		if (path2.get(0).getValue() == path1.get(path1.size() - 1).getValue())
			return true;
		return false;
	}

	public Action clear() {
		Action result = null;
		PoliceForce policeForce = (PoliceForce) this.agentInfo.me();

		if (this.needRest(policeForce)) {
			List<EntityID> list = new ArrayList<>();
			if (target != null) {
				list.add(this.target);
			}
			result = this.calcRest(policeForce, this.pathPlanning, list);
			if (result != null) {
				return result;
			}
		}

		if (this.target == null) {
			return noAction();
		}
		EntityID agentPosition = policeForce.getPosition();
		StandardEntity targetEntity = this.worldInfo.getEntity(this.target);
		StandardEntity positionEntity = Objects.requireNonNull(this.worldInfo.getEntity(agentPosition));
		if (targetEntity == null || !(targetEntity instanceof Area)) {
			return result;
		}
		if (positionEntity instanceof Road || positionEntity instanceof Hydrant) {
			result = this.getRescueAction(policeForce, (Road) positionEntity);
			if (result != null) {
				return result;
			}
			result = this.getAreaClearAction(policeForce, positionEntity);
			if (result != null) {
				return result;
			}
		}
		if (agentPosition.equals(this.target)) {
			result = this.getAreaClearAction(policeForce, targetEntity);
		} else if (((Area) targetEntity).getEdgeTo(agentPosition) != null) {
			result = this.getNeighbourPositionAction(policeForce, (Area) targetEntity);
		} else {
			List<EntityID> path = this.pathPlanning.getResult(agentPosition, this.target);
			if (path != null && path.size() > 1) {
				int index = path.indexOf(agentPosition);
				if (index == 0) {
					Area area = (Area) positionEntity;
					for (int i = 1; i < path.size(); i++) {
						if (area.getEdgeTo(path.get(i)) != null) {
							index = i;
							break;
						}
					}
				} else if (index >= 1) {
					index++;
				}
				if (index >= 1 && index < (path.size())) {
					StandardEntity entity = this.worldInfo.getEntity(path.get(index));
					result = this.getNeighbourPositionAction(policeForce, (Area) entity);
					// TODO NullPointer
					if (result != null && result.getClass() == ActionMove.class) {
						if (!((ActionMove) result).getUsePosition()) {
							result = null;
						}
					}
				}
				if (result == null) {
					result = new ActionMove(path);
				}
			}
		}
		return result;
	}
	/**
	* @Description: 新的getRescueAction
	* @Author: Bochun-Yue
	* @Date: 3/7/20
	*/
	private Action getRescueAction(PoliceForce police, Road road) {
//		System.out.println("---------------------rescue-----------------");
		if (!road.isBlockadesDefined()) {
			return null;
		}
		Collection<Blockade> blockades = this.worldInfo.getBlockades(road).stream().filter(Blockade::isApexesDefined)
				.collect(Collectors.toSet());
        Collection<StandardEntity> agents = this.worldInfo.getEntitiesOfType(	
                StandardEntityURN.AMBULANCE_TEAM,
                StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.CIVILIAN
        );
		
		double policeX = police.getX();
		double policeY = police.getY();
		double minDistance = Double.MAX_VALUE;
		Action moveAction = null;
		Blockade nearestBlock = null;
		for (StandardEntity entity : agents) {
			Human human = (Human) entity;
			if (!human.isPositionDefined() || human.getPosition().getValue() != road.getID().getValue()) {
				continue;
			}
			double humanX = human.getX();
			double humanY = human.getY();
			ActionClear actionClear = null;
			for (Blockade blockade : blockades) {
				if (!this.isInside(humanX, humanY, blockade.getApexes()) && !this.isNearBlockade(humanX, humanY, blockade)) {
					continue;
				}
				Point2D agent = new Point2D(humanX, humanY);
				Point2D Police = new Point2D(policeX, policeY);
				return this.clearToPoint(blockades, Police,agent);
			}
			if (actionClear != null) {
				lastClearTarget = null;
				return actionClear;
			}
		}
		if (nearestBlock != null)
			lastClearTarget = nearestBlock;
		return moveAction;
	}
	/**
	* @Description: 新的getAreaClearAction
	* @Author: Bochun-Yue
	* @Date: 3/7/20
	*/
	private Action getAreaClearAction(PoliceForce police, StandardEntity targetEntity) {
		if (targetEntity instanceof Building) {
			if (!targetEntity.equals(worldInfo.getPosition(agentInfo.getID())))
				return null;
		}
		Road road = (Road) targetEntity;
		if (!road.isBlockadesDefined() || road.getBlockades().isEmpty()) {
//			this.get_next_target(road);
			return null;
		}
		Collection<Blockade> blockades = this.worldInfo.getBlockades(road).stream().filter(Blockade::isApexesDefined)
				.collect(Collectors.toSet());
		Blockade clearBlockade = null;		
		Line2D guideline = null;
		for(guidelineHelper r : this.judgeRoad) {
			if(r.getSelfID().equals(road.getID())) {
				guideline = r.getGuideline();
			}
		}
			
			if (guideline != null) {
				Action action = moveToGuideLine(guideline,road);
	            if (action != null) {
	                return action;
	            }
				double agentX = police.getX();
				double agentY = police.getY();
				clearBlockade = null;
				Point2D intersection = null;
				Double minPointDistance = Double.MAX_VALUE;
				int clearX = 0;
				int clearY = 0;
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
									double dist = this.getDistance(agentX,agentY, intersect.getX(), intersect.getY());
									if(dist<minPointDistance) {
										minPointDistance = dist;
										clearX = (int)intersect.getX();
										clearY = (int)intersect.getY();
										clearBlockade = blockade;
										intersection = intersect;
									}
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
									double dist = this.getDistance(agentX,agentY, intersect.getX(), intersect.getY());
									if(dist<minPointDistance) {
										minPointDistance = dist;
										clearX = (int)intersect.getX();
										clearY = (int)intersect.getY();
										clearBlockade = blockade;
										intersection = intersect;
									}
								}
							}
						}
					}
				}	
				if (clearBlockade != null) {
					if (minPointDistance < this.clearDistance - 1000) {
						Vector2D vector = this.scaleClear(this.getVector(agentX, agentY, clearX, clearY));
						clearX = (int) (agentX + vector.getX());
						clearY = (int) (agentY + vector.getY());
						lastClearTarget = null;
						double dist = this.getDistance(agentX, agentY, clearX, clearY);
						return new ActionClear(clearX, clearY, clearBlockade);
					}
					lastClearTarget = clearBlockade;
					int dX = (int)((intersection.getX() - agentX) / 10);
					int dY = (int)((intersection.getY() - agentY) / 10);
					return new ActionMove(Lists.newArrayList(police.getPosition()), (int)intersection.getX() - dX,(int) intersection.getY() - dY);
				}
	        }
		return null;
	}
	/**
	* @Description: 新的getNeighbourPositionAction
	* @Author: Bochun-Yue
	* @Date: 3/7/20
	*/
	private Action getNeighbourPositionAction(PoliceForce police, Area target) {
		double agentX = police.getX();
		double agentY = police.getY();
		StandardEntity position = Objects.requireNonNull(this.worldInfo.getPosition(police));
		Edge edge = target.getEdgeTo(position.getID());
		if (edge == null) {
			return null;
		}
		if (position instanceof Road || position instanceof Hydrant) {
			if(!edge.isPassable()) {
				Road road = (Road) position;
				Collection<Blockade> blockades = this.worldInfo.getBlockades(road).stream().filter(Blockade::isApexesDefined)
						.collect(Collectors.toSet());
				Point2D Police = new Point2D(agentX,agentY);
				Point2D mid = new Point2D((edge.getStartX() + edge.getEndX()) / 2,(edge.getStartY() + edge.getEndY()) / 2);
				return this.clearToPoint(blockades, Police, mid);
			}
		}
		if (target instanceof Road || target instanceof Hydrant) {
			Road road = (Road) target;
			if (!road.isBlockadesDefined() || this.isRoadPassable(road)) {
				return new ActionMove(Lists.newArrayList(position.getID(), target.getID()));
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
				Point2D intersection = null;
				Blockade clearBlockade = null;
				Double minPointDistance = Double.MAX_VALUE;
				int clearX = 0;
				int clearY = 0;
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
									double dist = this.getDistance(agentX,agentY, intersect.getX(), intersect.getY());
									if(dist<minPointDistance) {
										minPointDistance = dist;
										clearX = (int)intersect.getX();
										clearY = (int)intersect.getY();
										intersection = intersect;
										clearBlockade = blockade;
									}
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
									double dist = this.getDistance(agentX,agentY, intersect.getX(), intersect.getY());
									if(dist<minPointDistance) {
										minPointDistance = dist;
										clearX = (int)intersect.getX();
										clearY = (int)intersect.getY();
										intersection = intersect;
										clearBlockade = blockade;
									}
								}
							}
						}
					}
				}	
				if (intersection != null) {
					if (minPointDistance < this.clearDistance /2) {
						Vector2D vector = this.scaleClear(this.getVector(agentX, agentY, clearX, clearY));
						clearX = (int) (agentX + vector.getX());
						clearY = (int) (agentY + vector.getY());
						lastClearTarget = null;
						double dist = this.getDistance(agentX, agentY, clearX, clearY);
						return new ActionClear(clearX, clearY, clearBlockade);
					}
					int dX = (int)((intersection.getX() - agentX) / 10);
					int dY = (int)((intersection.getY() - agentY) / 10);
					return new ActionMove(Lists.newArrayList(police.getPosition()), (int)intersection.getX() - dX,(int) intersection.getY() - dY);
				}
	        }

		}
		return new ActionMove(Lists.newArrayList(position.getID(), target.getID()));
	}
	/**
	* @Description: 去guideline上的点
	* @Author: Bochun-Yue
	* @Date: 3/7/20
	*/
    private Action moveToGuideLine(Line2D guideline,Road road) {
		Point2D agent = new Point2D(this.agentInfo.getX(), this.agentInfo.getY());
		Point2D closest = GeometryTools2D.getClosestPointOnSegment(guideline, agent);
    	double distance = this.getDistance(closest.getX(),closest.getY(), this.agentInfo.getX(), this.agentInfo.getY());
    	if(distance < 1000) {
    		return null;
    	}else {
    		Collection<Blockade> blockades = this.worldInfo.getBlockades(road).stream().filter(Blockade::isApexesDefined)
    				.collect(Collectors.toSet());
    		return this.clearToPoint(blockades, agent, closest);
    	}
    }
	/**
	* @Description: 到达某点
	* @Author: Bochun-Yue
	* @Date: 3/7/20
	*/		
    private Action clearToPoint(Collection<Blockade> blockades,Point2D agent,Point2D closest ) {
		PoliceForce police = (PoliceForce) this.agentInfo.me();
		double agentX = agent.getX();
		double agentY = agent.getY();
		Point2D intersection = null;
		Double minPointDistance = Double.MAX_VALUE;
		int clearX = 0;
		int clearY = 0;
		Blockade clearBlockade = null;
		Line2D ToGuideLine = new Line2D(agent,closest);
		for (Blockade blockade : blockades) {
			List<Point2D> Points = GeometryTools2D.vertexArrayToPoints(blockade.getApexes());
			for(int i =0;i<Points.size();++i) {
				if(i!=Points.size()-1) {
					double crossProduct1 = this.getCrossProduct(ToGuideLine, Points.get(i));
					double crossProduct2 = this.getCrossProduct(ToGuideLine, Points.get(i+1));
					if(crossProduct1<0&&crossProduct2>0 || crossProduct1>0&&crossProduct2<0) {
						Line2D line = new Line2D(Points.get(i),Points.get(i+1));
						Point2D intersect = GeometryTools2D.getIntersectionPoint(line, ToGuideLine);
						if(intersect!=null) {
							double dist = this.getDistance(agentX,agentY, intersect.getX(), intersect.getY());
							if(dist<minPointDistance) {
								minPointDistance = dist;
								clearX = (int)intersect.getX();
								clearY = (int)intersect.getY();
								intersection = intersect;
								clearBlockade = blockade;
							}
						}
					}
				}
				else {
					double crossProduct1 = this.getCrossProduct(ToGuideLine, Points.get(i));
					double crossProduct2 = this.getCrossProduct(ToGuideLine, Points.get(0));
					if(crossProduct1<0&&crossProduct2>0 || crossProduct1>0&&crossProduct2<0) {
						Line2D line = new Line2D(Points.get(i),Points.get(0));
						Point2D intersect = GeometryTools2D.getIntersectionPoint(line, ToGuideLine);
						if(intersect!=null) {
							double dist = this.getDistance(agentX,agentY, intersect.getX(), intersect.getY());
							if(dist<minPointDistance) {
								minPointDistance = dist;
								clearX = (int)intersect.getX();
								clearY = (int)intersect.getY();
								intersection = intersect;
								clearBlockade = blockade;
							}
						}
					}
				}
			}
		}	
		if (intersection != null) {
			if (minPointDistance < this.clearDistance - 1000) {
				Vector2D vector = this.scaleClear(this.getVector(agentX, agentY, clearX, clearY));
				clearX = (int) (agentX + vector.getX());
				clearY = (int) (agentY + vector.getY());
				double dist = this.getDistance(agentX, agentY, clearX, clearY);
				return new ActionClear(clearX, clearY);
			}
			int dX = (int)((intersection.getX() - agentX) / 10);
			int dY = (int)((intersection.getY() - agentY) / 10);
			return new ActionMove(Lists.newArrayList(police.getPosition()), (int)intersection.getX(), (int)intersection.getY());
		}
		else {
			return new ActionMove(Lists.newArrayList(police.getPosition()),(int) closest.getX(),(int) closest.getY());
		}
    }
	/**
	* @Description: 叉积
	* @Author: Bochun-Yue
	* @Date: 3/7/20
	*/
    private double getCrossProduct(Line2D line , Point2D point) {
    	
    	double X = point.getX();
    	double Y = point.getY();
    	double X1 = line.getOrigin().getX();
    	double Y1 = line.getOrigin().getY();
    	double X2 = line.getEndPoint().getX();
    	double Y2 = line.getEndPoint().getY();
    	
    	return ((X2 - X1)*(Y - Y1) - (X - X1)*(Y2 - Y1));
    }
	
    private boolean isNearBlockade(double pX, double pY, Blockade blockade) {
        int[] apex = blockade.getApexes();
        for (int i = 0; i < apex.length - 4; i += 2) {
            if(java.awt.geom.Line2D.ptLineDist(apex[i], apex[i + 1], apex[i + 2], apex[i + 3], pX, pY) < 600) {
                return true;
            }
        }
        if (java.awt.geom.Line2D.ptLineDist(apex[0], apex[1], apex[apex.length - 2], apex[apex.length - 1], pX, pY) < 600) {
            return true;
        }
        return false;
    }
	
	private Action getIntersectEdgeAction(double agentX, double agentY, Edge edge, Road road) {
		double midX = (edge.getStartX() + edge.getEndX()) / 2;
		double midY = (edge.getStartY() + edge.getEndY()) / 2;
		return this.getIntersectEdgeAction(agentX, agentY, midX, midY, road);
	}

	private Action getIntersectEdgeAction(double agentX, double agentY, double pointX, double pointY, Road road) {
		Set<Point2D> movePoints = this.getMovePoints(road);
		Point2D bestPoint = null;
		double bastDistance = Double.MAX_VALUE;
		for (Point2D p : movePoints) {
			if (!this.intersect(agentX, agentY, p.getX(), p.getY(), road)) {
				if (!this.intersect(pointX, pointY, p.getX(), p.getY(), road)) {
					double distance = this.getDistance(pointX, pointY, p.getX(), p.getY());
					if (distance < bastDistance) {
						bestPoint = p;
						bastDistance = distance;
					}
				}
			}
		}
		if (bestPoint != null) {
			double pX = bestPoint.getX();
			double pY = bestPoint.getY();
			if (!road.isBlockadesDefined()) {
				return new ActionMove(Lists.newArrayList(road.getID()), (int) pX, (int) pY);
			}
			ActionClear actionClear = null;
			ActionMove actionMove = null;
			Vector2D vector = this.scaleClear(this.getVector(agentX, agentY, pX, pY));
			int clearX = (int) (agentX + vector.getX());
			int clearY = (int) (agentY + vector.getY());
			vector = this.scaleBackClear(vector);
			int startX = (int) (agentX + vector.getX());
			int startY = (int) (agentY + vector.getY());
			for (Blockade blockade : this.worldInfo.getBlockades(road)) {
				if (this.intersect(startX, startY, pX, pY, blockade)) {
					if (this.intersect(startX, startY, clearX, clearY, blockade)) {
						if (actionClear == null) {
							actionClear = new ActionClear(clearX, clearY, blockade);
						} else {
							if (actionClear.getTarget() != null) {
								Blockade another = (Blockade) this.worldInfo.getEntity(actionClear.getTarget());
								if (another != null && this.intersect(blockade, another)) {
									return new ActionClear(another);
								}
							}
							return actionClear;
						}
					} else if (actionMove == null) {
						actionMove = new ActionMove(Lists.newArrayList(road.getID()), (int) pX, (int) pY);
					}
				}
			}
			if (actionClear != null) {
				return actionClear;
			} else if (actionMove != null) {
				return actionMove;
			}
		}
		Action action = this.getAreaClearAction((PoliceForce) this.agentInfo.me(), road);
		if (action == null) {
			action = new ActionMove(Lists.newArrayList(road.getID()), (int) pointX, (int) pointY);
		}
		return action;
	}

	private boolean equalsPoint(double p1X, double p1Y, double p2X, double p2Y) {
		return this.equalsPoint(p1X, p1Y, p2X, p2Y, 1000.0D);
	}

	private boolean equalsPoint(double p1X, double p1Y, double p2X, double p2Y, double range) {
		return (p2X - range < p1X && p1X < p2X + range) && (p2Y - range < p1Y && p1Y < p2Y + range);
	}

	private boolean isInside(double pX, double pY, int[] apex) {
		Point2D p = new Point2D(pX, pY);
		Vector2D v1 = (new Point2D(apex[apex.length - 2], apex[apex.length - 1])).minus(p);
		Vector2D v2 = (new Point2D(apex[0], apex[1])).minus(p);
		double theta = this.getAngle(v1, v2);

		for (int i = 0; i < apex.length - 2; i += 2) {
			v1 = (new Point2D(apex[i], apex[i + 1])).minus(p);
			v2 = (new Point2D(apex[i + 2], apex[i + 3])).minus(p);
			theta += this.getAngle(v1, v2);
		}
		return Math.round(Math.abs((theta / 2) / Math.PI)) >= 1;
	}

	private boolean intersect(double agentX, double agentY, double pointX, double pointY, Area area) {
		for (Edge edge : area.getEdges()) {
			double startX = edge.getStartX();
			double startY = edge.getStartY();
			double endX = edge.getEndX();
			double endY = edge.getEndY();
			if (java.awt.geom.Line2D.linesIntersect(agentX, agentY, pointX, pointY, startX, startY, endX, endY)) {
				double midX = (edge.getStartX() + edge.getEndX()) / 2;
				double midY = (edge.getStartY() + edge.getEndY()) / 2;
				if (!equalsPoint(pointX, pointY, midX, midY) && !equalsPoint(agentX, agentY, midX, midY)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean intersect(Blockade blockade, Blockade another) {
		if (blockade.isApexesDefined() && another.isApexesDefined()) {
			int[] apexes0 = blockade.getApexes();
			int[] apexes1 = another.getApexes();
			for (int i = 0; i < (apexes0.length - 2); i += 2) {
				for (int j = 0; j < (apexes1.length - 2); j += 2) {
					if (java.awt.geom.Line2D.linesIntersect(apexes0[i], apexes0[i + 1], apexes0[i + 2], apexes0[i + 3],
							apexes1[j], apexes1[j + 1], apexes1[j + 2], apexes1[j + 3])) {
						return true;
					}
				}
			}
			for (int i = 0; i < (apexes0.length - 2); i += 2) {
				if (java.awt.geom.Line2D.linesIntersect(apexes0[i], apexes0[i + 1], apexes0[i + 2], apexes0[i + 3],
						apexes1[apexes1.length - 2], apexes1[apexes1.length - 1], apexes1[0], apexes1[1])) {
					return true;
				}
			}
			for (int j = 0; j < (apexes1.length - 2); j += 2) {
				if (java.awt.geom.Line2D.linesIntersect(apexes0[apexes0.length - 2], apexes0[apexes0.length - 1],
						apexes0[0], apexes0[1], apexes1[j], apexes1[j + 1], apexes1[j + 2], apexes1[j + 3])) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean intersect(double agentX, double agentY, double pointX, double pointY, Blockade blockade) {
		List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(blockade.getApexes()),
				true);
		for (Line2D line : lines) {
			Point2D start = line.getOrigin();
			Point2D end = line.getEndPoint();
			double startX = start.getX();
			double startY = start.getY();
			double endX = end.getX();
			double endY = end.getY();
			if (java.awt.geom.Line2D.linesIntersect(agentX, agentY, pointX, pointY, startX, startY, endX, endY)) {
				return true;
			}
		}
		return false;
	}

	private double getDistance(double fromX, double fromY, double toX, double toY) {
		double dx = toX - fromX;
		double dy = toY - fromY;
		return Math.hypot(dx, dy);
	}

	private double getAngle(Vector2D v1, Vector2D v2) {
		double flag = (v1.getX() * v2.getY()) - (v1.getY() * v2.getX());
		double angle = Math
				.acos(((v1.getX() * v2.getX()) + (v1.getY() * v2.getY())) / (v1.getLength() * v2.getLength()));
		if (flag > 0) {
			return angle;
		}
		if (flag < 0) {
			return -1 * angle;
		}
		return 0.0D;
	}

	private Vector2D getVector(double fromX, double fromY, double toX, double toY) {
		return (new Point2D(toX, toY)).minus(new Point2D(fromX, fromY));
	}

	private Vector2D scaleClear(Vector2D vector) {
		return vector.normalised().scale(this.clearDistance);
	}

	private Vector2D scaleBackClear(Vector2D vector) {
		return vector.normalised().scale(-510);
	}

	private Set<Point2D> getMovePoints(Road road) {
		Set<Point2D> points = this.movePointCache.get(road.getID());
		if (points == null) {
			points = new HashSet<>();
			int[] apex = road.getApexList();
			for (int i = 0; i < apex.length; i += 2) {
				for (int j = i + 2; j < apex.length; j += 2) {
					double midX = (apex[i] + apex[j]) / 2;
					double midY = (apex[i + 1] + apex[j + 1]) / 2;
					if (this.isInside(midX, midY, apex)) {
						points.add(new Point2D(midX, midY));
					}
				}
			}
			for (Edge edge : road.getEdges()) {
				double midX = (edge.getStartX() + edge.getEndX()) / 2;
				double midY = (edge.getStartY() + edge.getEndY()) / 2;
				points.remove(new Point2D(midX, midY));
			}
			this.movePointCache.put(road.getID(), points);
		}
		return points;
	}

	private boolean needRest(Human agent) {
		int hp = agent.getHP();
		int damage = agent.getDamage();
		if (damage == 0 || hp == 0) {
			return false;
		}
		int activeTime = (hp / damage) + ((hp % damage) != 0 ? 1 : 0);
		if (this.kernelTime == -1) {
			try {
				this.kernelTime = this.scenarioInfo.getKernelTimesteps();
			} catch (NoSuchConfigOptionException e) {
				this.kernelTime = -1;
			}
		}
		return damage >= this.thresholdRest || (activeTime + this.agentInfo.getTime()) < this.kernelTime;
	}

	private Action calcRest(Human human, PathPlanning pathPlanning, Collection<EntityID> targets) {
		EntityID position = human.getPosition();
		Collection<EntityID> refuges = this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE);
		int currentSize = refuges.size();
		if (refuges.contains(position)) {
			return new ActionRest();
		}
		List<EntityID> firstResult = null;
		while (refuges.size() > 0) {
			pathPlanning.setFrom(position);
			pathPlanning.setDestination(refuges);
			List<EntityID> path = pathPlanning.calc().getResult();
			if (path != null && path.size() > 0) {
				if (firstResult == null) {
					firstResult = new ArrayList<>(path);
					if (targets == null || targets.isEmpty()) {
						break;
					}
				}
				EntityID refugeID = path.get(path.size() - 1);
				pathPlanning.setFrom(refugeID);
				pathPlanning.setDestination(targets);
				List<EntityID> fromRefugeToTarget = pathPlanning.calc().getResult();
				if (fromRefugeToTarget != null && fromRefugeToTarget.size() > 0) {
					return new ActionMove(path);
				}
				refuges.remove(refugeID);
				// remove failed
				if (currentSize == refuges.size()) {
					break;
				}
				currentSize = refuges.size();
			} else {
				break;
			}
		}
		return firstResult != null ? new ActionMove(firstResult) : null;
	}

	/**
	 * 判断一个platoon agent是否被路障围住。在这种状况下，platoon agent是不能移动的。 此处借用了服务器的判别方法。
	 * 
	 * @param human
	 *            目标agent
	 * @return 当目标agent被路障围住时，返回true。否则，false。
	 */
	public boolean isStucked(Human human) {
		Blockade blockade = isLocateInBlockade(human);
		//the new add code

		if (blockade == null)
			return false;
		double minDistance = Ruler.getDistanceToBlock(blockade, human.getX(), human.getY());

		if (minDistance > 500) {
			//System.out.println(time + ", " + human + ", " + "is stucked");
			return true;
		}

		StandardEntity position = worldInfo.getPosition(human.getID());
		if (position instanceof Building) {
			Building loc = (Building) position;

			Set<Road> entrances = entrance.getEntrance(loc);
			int size = entrances.size();
			int count = 0;
			for (Road next : entrance.getEntrance(loc)) {
				CSURoadHelper road = csuRoadMap.get(next.getID());
				if (road.isNeedlessToClear())
					continue;
				count++;
			}

			if (count == size)
				return true;
		}
		return false;
	}
}
