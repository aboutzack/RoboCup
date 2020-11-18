package CSU_Yunlu_2020.extaction.pf;

import CSU_Yunlu_2020.CSUConstants;
import CSU_Yunlu_2020.module.complex.pf.GuidelineCreator;
import CSU_Yunlu_2020.standard.CSURoadHelper;
import CSU_Yunlu_2020.standard.Ruler;
import CSU_Yunlu_2020.util.CircleQueue;
import CSU_Yunlu_2020.util.CircleStack;
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
import adf.component.communication.CommunicationMessage;
import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import com.google.common.collect.Lists;
import javolution.util.FastMap;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;


public class ActionExtClear extends ExtAction {

	protected double x, y, time;
	protected double lastx = 0;
	protected double lasty = 0;
	protected double repairDistance;
	private List<guidelineHelper> judgeRoad = new ArrayList<>();
	HashSet<Blockade> already_clear_blocked = new HashSet<>();
	private List<Road> targetblockedRoad = new ArrayList<>();
	private List<EntityID> clearedRoad = new ArrayList<>();
	protected Map<EntityID, CSURoadHelper> csuRoadMap = new FastMap<>();
	private Action lastAction = null;
	private int lasttime = 0;
	private int lastNoMoveTime = 0;
	private Boolean noMoveFlag = false;
	MessageManager messageManager = null;
	private GuidelineCreator guidelineCreator;


	protected int count = 0, lock = 4;


	private PathPlanning pathPlanning;
	private Clustering clustering;
	//清理距离
	private int clearDistance;
	//需要休息时的阈值
	private int thresholdRest;
	//时间
	private int kernelTime;
	private Action mayRepeat = null;
	private boolean needCheckForRepeat = false;

	private final int actionStoreNumber = 10;
	private CircleQueue<Action> actionHistory = new CircleQueue<>(actionStoreNumber);
	private CircleStack<Action> actionStack = new CircleStack<>(actionStoreNumber);

	//目标
	private EntityID target;

	protected List<EntityID> lastCyclePath;

	private Blockade lastClearTarget = null;

	public ActionExtClear(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager,
						  DevelopData developData) {
		super(ai, wi, si, moduleManager, developData);
		this.clearDistance = si.getClearRepairDistance();
		this.thresholdRest = developData.getInteger("ActionExtClear.rest", 100);

		this.target = null;
		this.count = 0;

		this.time = ai.getTime();
		this.x = agentInfo.getX();
		this.y = agentInfo.getY();
		this.repairDistance = scenarioInfo.getClearRepairDistance();


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
		this.guidelineCreator = moduleManager.getModule("GuidelineCreator.Default", CSUConstants.A_STAR_PATH_PLANNING);

	}

	@Override
	public ExtAction precompute(PrecomputeData precomputeData) {
		super.precompute(precomputeData);
		if (this.getCountPrecompute() >= 2) {
			return this;
		}
		this.pathPlanning.precompute(precomputeData);
		this.clustering.precompute(precomputeData);
		this.guidelineCreator.precompute(precomputeData);
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
		this.guidelineCreator.resume(precomputeData);
		this.judgeRoad = guidelineCreator.getJudgeRoad();
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
		this.guidelineCreator.preparate();
		this.judgeRoad = guidelineCreator.getJudgeRoad();
		return this;
	}


	@Override
	public ExtAction updateInfo(MessageManager messageManager) {
		super.updateInfo(messageManager);
		this.messageManager = messageManager;
		if (this.getCountUpdateInfo() >= 2) {
			return this;
		}


		this.time = agentInfo.getTime();
		this.x = agentInfo.getX();
		this.y = agentInfo.getY();

		this.pathPlanning.updateInfo(messageManager);
		this.clustering.updateInfo(messageManager);
		this.guidelineCreator.updateInfo(messageManager);
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


	private boolean is_no_move() {
		double currentx = this.agentInfo.getX();
		double currenty = this.agentInfo.getY();
		int currentTime = this.agentInfo.getTime();
		if (currentx > lastx - 200 && currentx < lastx + 200 && currenty > lasty - 200 && currenty < lasty + 200) {
			if (this.noMoveFlag == false) {
				this.lastNoMoveTime = this.agentInfo.getTime();
			}
			noMoveFlag = true;
			if (this.agentInfo.getTime() - this.lastNoMoveTime > 5) {
				return true;
			}
		} else {
			this.noMoveFlag = false;
			this.lastx = currentx;
			this.lasty = currenty;
			return false;
		}
		return false;
	}


	protected Action randomWalk() {

		if (this.target == null && this.lastClearTarget != null) this.target = this.lastClearTarget.getID();
		if (this.target != null) {

			if (this.agentInfo.getPosition().equals(this.target)) {
				Collection<Blockade> blockades = null;
				if (this.worldInfo.getEntity(target).getStandardURN() == StandardEntityURN.ROAD)
					blockades = this.worldInfo.getBlockades(this.target);
				if (blockades != null) {
					if (!blockades.isEmpty()) {

						Set<Blockade> covers = new HashSet<>();

						if (blockades != null)
							for (Blockade blockade : blockades) {
								covers.add(blockade);
							}
						if (!covers.isEmpty()) { //if police is covered by blockade, clear it first
							Blockade block = (Blockade) this.getClosestEntity(covers, this.agentInfo.me());
							while (!covers.isEmpty() && this.already_clear_blocked.contains(block)) {
								covers.remove(block);
								block = (Blockade) this.getClosestEntity(covers, this.agentInfo.me());
							}
							this.already_clear_blocked.add(block);
							if (block != null) return new ActionClear(block);//(int)block.getX(),(int)block.getY());
						}

					}
				}
				StandardEntity entity = this.worldInfo.getEntity(this.target);
				Pair<Integer, Integer> location = getSelfLocation();
				this.pathPlanning.setDestination(entity.getID());
				this.pathPlanning.setFrom(this.agentInfo.getPosition());
				List<EntityID> path = this.pathPlanning.calc().getResult();
				if (path != null && path.size() > 1) return new ActionMove(path);
			}
		}
		Set<EntityID> blockedRoads = new HashSet<>();


		for (EntityID i : this.worldInfo.getChanged().getChangedEntities()) {
			StandardEntity entity = (StandardEntity) this.worldInfo.getEntity(i);
			if (entity instanceof Road) {
				Road road = (Road) entity;
				if (!this.isRoadPassable(road)) {
					blockedRoads.add(entity.getID());
				}
			}
		}

		for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
			if (entity instanceof Road) {
				Road road = (Road) entity;
				if (!this.isRoadPassable(road)) {
					blockedRoads.add(entity.getID());
				}
			}
		}

		this.target = get_result_from_set(blockedRoads);


		if (this.target == null) {
			this.target = this.agentInfo.getPosition();
		} else {
			while (!blockedRoads.isEmpty()) {
				Collection<Blockade> blockades = null;
				if (this.worldInfo.getEntity(target).getStandardURN() == StandardEntityURN.ROAD)
					blockades = this.worldInfo.getBlockades(this.target);

				Set<Blockade> covers = new HashSet<>();

				if (blockades != null)
					for (Blockade blockade : blockades) {
						covers.add(blockade);
					}
				if (!covers.isEmpty()) {

					Blockade block = (Blockade) this.getClosestEntity(covers, this.agentInfo.me());
					while (!covers.isEmpty() && this.already_clear_blocked.contains(block)) {
						covers.remove(block);
						block = (Blockade) this.getClosestEntity(covers, this.agentInfo.me());
					}
					this.already_clear_blocked.add(block);
					if (block != null) return new ActionClear((int) block.getX(), (int) block.getY());

					//return new ActionClear((Blockade) this.getClosestEntity(covers, this.agentInfo.me()));
				}
				blockedRoads.remove(this.target);
				this.target = get_result_from_set(blockedRoads);
			}
		}

		if (this.agentInfo.getPosition().equals(this.target)) {
			Collection<Blockade> blockades = null;
			if (this.worldInfo.getEntity(target).getStandardURN() == StandardEntityURN.ROAD)
				blockades = this.worldInfo.getBlockades(this.target);

			Set<Blockade> covers = new HashSet<>();

			if (blockades != null)
				for (Blockade blockade : blockades) {
					covers.add(blockade);
				}
			if (!covers.isEmpty()) {

				Blockade block = (Blockade) this.getClosestEntity(covers, this.agentInfo.me());
				while (!covers.isEmpty() && this.already_clear_blocked.contains(block)) {
					covers.remove(block);
					block = (Blockade) this.getClosestEntity(covers, this.agentInfo.me());
				}
				this.already_clear_blocked.add(block);
				if (block != null) return new ActionClear(block);//(int)block.getX(),(int)block.getY());

				//return new ActionClear((Blockade) this.getClosestEntity(covers, this.agentInfo.me()));
			}
		}
		StandardEntity entity = this.worldInfo.getEntity(this.target);
		if(entity != null) {
			this.pathPlanning.setFrom(this.agentInfo.getPosition());
			this.pathPlanning.setDestination(entity.getID());
			List<EntityID> path = pathPlanning.calc().getResult();

			if (path != null && path.size() > 1) return new ActionMove(path);
		}

		int clusterIndex = this.get_cluster_Index();
		this.target = get_clustering(clusterIndex);

		entity = this.worldInfo.getEntity(this.target);
		if(entity != null) {
			this.pathPlanning.setFrom(this.agentInfo.getPosition());
			this.pathPlanning.setDestination(entity.getID());
			List<EntityID> path = pathPlanning.calc().getResult();

			if (path != null && path.size() > 1) return new ActionMove(path);
		}

		return null;
	}


	Set<Integer> last_cluster_number = new HashSet<Integer>();

	private int get_cluster_Index() {
		int clusterIndex = this.clustering.getClusterIndex(this.agentInfo.getID());
		int cluster_number = this.clustering.getClusterNumber();
		if (!last_cluster_number.contains(clusterIndex)) {
			last_cluster_number.add(clusterIndex);
		} else {
			Random r = new Random(cluster_number);
			while (last_cluster_number.contains(clusterIndex)) {
				if (last_cluster_number.size() >= cluster_number) this.last_cluster_number = new HashSet<Integer>();
				clusterIndex = r.nextInt(cluster_number);
			}
			last_cluster_number.add(clusterIndex);
		}
		return clusterIndex;
	}

	private EntityID get_clustering(int clusterIndex) {

		List<EntityID> sortList = new ArrayList<>();
		for (EntityID id : this.clustering.getClusterEntityIDs(clusterIndex)) {
			if (this.worldInfo.getEntity(id) instanceof Road) sortList.add(id);
		}
		sortList.sort(new sorter(this.worldInfo, this.agentInfo.getID()));
		return sortList.get(sortList.size() / 2);


	}


	private EntityID get_result_from_set(Set<EntityID> set) {

		if (set == null) return null;

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


	public Pair<Integer, Integer> getSelfLocation() {
		return worldInfo.getLocation(agentInfo.getID());
	}

	public StandardEntity getSelfPosition() {
		return worldInfo.getPosition(agentInfo.getID());
	}


	// TODO to finish it
	@Override
	public ExtAction calc() {

		if(isStucked((Human) (agentInfo.me()))){
			result = clearWhenStuck();
			if(result!= null){
				return this;
			}
		}

		if (this.is_no_move()) {
			this.result = this.noMoveAction();
			if (result != null) {
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
		}
		if (result == null) {
			result = this.clear();
		}
		if (result != null) {
			actionHistory.add(result);
			actionStack.push(result);
		}
		//if(result instanceof ActionMove) result = randomWalk();
		this.check_same_action();
		return this;
	}


	/**
	 * @Description: 发送信息，改为以guideline是否被覆盖为判断标准
	 */
	private void send_message() {

		for (EntityID id : this.worldInfo.getChanged().getChangedEntities()) {
			StandardEntity entity = this.worldInfo.getEntity(id);
			if (entity instanceof Road || entity instanceof Hydrant) {
				Road road = (Road) entity;
				boolean passable = this.isRoadPassable(road);
				if (passable) {
					this.messageManager.addMessage(new MessageRoad(true, road, null, true, true));
					this.messageManager.addMessage(new MessageRoad(false, road, null, true, true));

				} else {    //add all blockades to the message
					Collection<Blockade> blockades = worldInfo.getBlockades(road);
					for (Blockade block : blockades) {
						this.messageManager.addMessage(new MessageRoad(true, road, block, false, true)); //不可以通过，选择最近的障碍物加入
						this.messageManager.addMessage(new MessageRoad(false, road, block, false, true)); //不可以通过，选择最近的障碍物加入
					}
				}
			}
		}

	}

	/**
	 * @Description: 道路可否通过，以guideline是否被覆盖为判断标准
	 * @Author: Bochun-Yue
	 * @Date: 3/7/20
	 */
	private boolean isRoadPassable(Road road) {
		if(this.agentInfo.getTime() <= this.scenarioInfo.getKernelAgentsIgnoreuntil()){
			return false;
		}
		if (!road.isBlockadesDefined() || road.getBlockades().isEmpty()) {
			return true;
		}
		Collection<Blockade> blockades = this.worldInfo.getBlockades(road).stream().filter(Blockade::isApexesDefined)
				.collect(Collectors.toSet());
		Line2D guideline = null;
		for (guidelineHelper r : this.judgeRoad) {
			if (r.getSelfID().equals(road.getID())) {
				guideline = r.getGuideline();
			}
		}
		if (guideline != null) {
			for (Blockade blockade : blockades) {
				List<Point2D> Points = GeometryTools2D.vertexArrayToPoints(blockade.getApexes());
				for (int i = 0; i < Points.size(); ++i) {
					if (i != Points.size() - 1) {
						double crossProduct1 = this.getCrossProduct(guideline, Points.get(i));
						double crossProduct2 = this.getCrossProduct(guideline, Points.get(i + 1));
						if (crossProduct1 < 0 && crossProduct2 > 0 || crossProduct1 > 0 && crossProduct2 < 0) {
							Line2D line = new Line2D(Points.get(i), Points.get(i + 1));
							Point2D intersect = GeometryTools2D.getIntersectionPoint(line, guideline);
							if (intersect != null) {
								return false;
							}
						}
					} else {
						double crossProduct1 = this.getCrossProduct(guideline, Points.get(i));
						double crossProduct2 = this.getCrossProduct(guideline, Points.get(0));
						if (crossProduct1 < 0 && crossProduct2 > 0 || crossProduct1 > 0 && crossProduct2 < 0) {
							Line2D line = new Line2D(Points.get(i), Points.get(0));
							Point2D intersect = GeometryTools2D.getIntersectionPoint(line, guideline);
							if (intersect != null) {
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

	private void check_same_action() {
		if (!(result instanceof ActionMove && this.lastAction instanceof ActionMove)
				&& !(result instanceof ActionClear && this.lastAction instanceof ActionClear)) {
			this.lastAction = result;
			lasttime = this.agentInfo.getTime();
		} else {
			int time = this.agentInfo.getTime();
			if (time - lasttime > 15) {
				result = this.randomWalk();
				lasttime = time;
			}
			this.lastAction = result;
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
		StandardEntity se = this.worldInfo.getEntity(this.agentInfo.getPosition());
		if (se instanceof Building) {
			Building building = (Building) se;
			for (EntityID neighbourID : building.getNeighbours()) {
				StandardEntity neighbour = this.worldInfo.getEntity(neighbourID);
				if (neighbour instanceof Road || neighbour instanceof Hydrant) {
					Road road = (Road) neighbour;
					Collection<Blockade> blockades = this.worldInfo.getBlockades(road).stream().filter(Blockade::isApexesDefined)
							.collect(Collectors.toSet());
					if (blockades != null) {
						if (!blockades.isEmpty()) {
							Blockade block = (Blockade) this.getClosestEntity(blockades, this.agentInfo.me());
							if (block != null && this.isNearBlockade(this.agentInfo.getX(), this.agentInfo.getY(), block)) {
//						System.out.println("------------------被挡没动------------");
								return new ActionClear(block);//(int)block.getX(),(int)block.getY());
							} else {
								return this.clear();
							}
						}
					}
				}
			}
		}
		if (se instanceof Road) {
			Road position = (Road) se;
			Collection<Blockade> blockades = this.worldInfo.getBlockades(position).stream().filter(Blockade::isApexesDefined)
					.collect(Collectors.toSet());
			if (blockades != null) {
				if (!blockades.isEmpty()) {
					Blockade clearBlockade = null;

					Line2D guideline = null;
					for(guidelineHelper r : this.judgeRoad) {
						if(r.getSelfID().equals(position.getID())) {
							guideline = r.getGuideline();
						}
					}

					if (guideline != null) {
//						Action action = moveToGuideLine(guideline,position);
//						if (action != null) {
//							return action;
//						}
						double agentX = this.agentInfo.getX();
						double agentY = this.agentInfo.getY();
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
							return new ActionMove(Lists.newArrayList(this.agentInfo.getPosition()), (int)intersection.getX() - dX,(int) intersection.getY() - dY);
						}
					}
				}
			}else {
				if (this.scenarioInfo.getCommsChannelsCount() > 1) {
					this.addMessageRoad();
					Road road = (Road) this.getClosestEntity(this.targetblockedRoad, this.agentInfo.me());
					blockades = this.worldInfo.getBlockades(road).stream().filter(Blockade::isApexesDefined)
							.collect(Collectors.toSet());
					if (blockades != null) {
						if (!blockades.isEmpty()) {
							Blockade block = (Blockade) this.getClosestEntity(blockades, this.agentInfo.me());
							if (block != null && this.isNearBlockade(this.agentInfo.getX(), this.agentInfo.getY(), block)) {
//						System.out.println("------------------被挡没动------------");
								return new ActionClear(block);//(int)block.getX(),(int)block.getY());
							} else if (block != null) {
								List<Point2D> Points = GeometryTools2D.vertexArrayToPoints(block.getApexes());
								double min = Double.MAX_VALUE;
								Point2D targetPoint = Points.get(0);
								for (Point2D point : Points) {
									double dist = this.getDistance(this.agentInfo.getX(), this.agentInfo.getY(), point.getX(), point.getY());
									if (dist < min) {
										min = dist;
										targetPoint = point;
									}
								}
								Point2D agentPosition = new Point2D(this.agentInfo.getX(), this.agentInfo.getY());
								Action action = this.clearToPoint(blockades, agentPosition, targetPoint);
								if(action != null){
									return action;
								}
//								return this.clearToPoint(blockades, agentPosition, targetPoint);
							}
						}
					}
				} else {
					for (EntityID id : this.worldInfo.getChanged().getChangedEntities()) {
						StandardEntity entity = this.worldInfo.getEntity(id);
						if (entity instanceof Road || entity instanceof Hydrant) {
							blockades = this.worldInfo.getBlockades((Road) entity).stream().filter(Blockade::isApexesDefined)
									.collect(Collectors.toSet());
							if (blockades != null) {
								if (!blockades.isEmpty()) {
									Blockade block = (Blockade) this.getClosestEntity(blockades, this.agentInfo.me());
									if (block != null && this.isNearBlockade(this.agentInfo.getX(), this.agentInfo.getY(), block)) {
//						System.out.println("------------------被挡没动------------");
										return new ActionClear(block);//(int)block.getX(),(int)block.getY());
									} else if (block != null) {
										List<Point2D> Points = GeometryTools2D.vertexArrayToPoints(block.getApexes());
										double min = Double.MAX_VALUE;
										Point2D targetPoint = Points.get(0);
										for (Point2D point : Points) {
											double dist = this.getDistance(this.agentInfo.getX(), this.agentInfo.getY(), point.getX(), point.getY());
											if (dist < min) {
												min = dist;
												targetPoint = point;
											}
										}
										Point2D agentPosition = new Point2D(this.agentInfo.getX(), this.agentInfo.getY());
										Action action = this.clearToPoint(blockades, agentPosition, targetPoint);
										if(action != null){
											return action;
										}
//										return this.clearToPoint(blockades, agentPosition, targetPoint);
									}
								}
							}
						}
					}
				}
			}
		}
		return null;
	}



	void addMessageRoad(){
		if(messageManager.getReceivedMessageList() != null
				&& !messageManager.getReceivedMessageList().isEmpty()) {
			for (CommunicationMessage message : messageManager.getReceivedMessageList()) {
				Class<? extends CommunicationMessage> messageClass = message.getClass();
				if (messageClass == MessageRoad.class) {
					MessageRoad messageRoad = (MessageRoad) message;
					if(!messageRoad.isPassable()) {
						this.targetblockedRoad.add((Road) this.worldInfo.getEntity(messageRoad.getRoadID()));
					}else{
						this.clearedRoad.add(messageRoad.getRoadID());
					}
				}
			}
			List<Road> roads = new ArrayList<>();
			for(EntityID id : this.clearedRoad){
				roads.add((Road)this.worldInfo.getEntity(id));
			}
			this.targetblockedRoad.removeAll(roads);
		}
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
			return null;
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
		return null;
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
			return null;
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
			this.pathPlanning.setFrom(agentPosition);
			this.pathPlanning.setDestination(this.target);
			List<EntityID> path = this.pathPlanning.calc().getResult();
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
		List<StandardEntity > agents = new ArrayList<>();
		for(EntityID ID : this.worldInfo.getChanged().getChangedEntities()){
			StandardEntity entity = this.worldInfo.getEntity(ID);
			if(entity instanceof AmbulanceTeam
					|| entity instanceof FireBrigade
					|| entity instanceof Civilian){
				agents.add(entity);
			}
		}

		double policeX = police.getX();
		double policeY = police.getY();
		Action moveAction = null;
		for (StandardEntity entity : agents) {
			Human human = (Human) entity;
			if (!human.isPositionDefined() || human.getPosition().getValue() != road.getID().getValue()) {
				continue;
			}
			double humanX = human.getX();
			double humanY = human.getY();
			for (Blockade blockade : blockades) {
				if(entity instanceof Civilian && !this.isInside(humanX, humanY, blockade.getApexes())){
					continue;
				}
				if (entity instanceof FireBrigade && !this.isInside(humanX, humanY, blockade.getApexes()) && !this.isNearBlockade(humanX, humanY, blockade)
						||entity instanceof AmbulanceTeam && !this.isInside(humanX, humanY, blockade.getApexes()) && !this.isNearBlockade(humanX, humanY, blockade)) {
					continue;
				}
				Point2D agent = new Point2D(humanX, humanY);
				Point2D Police = new Point2D(policeX, policeY);
				moveAction = this.clearToPoint(blockades, Police, agent);
				if(moveAction != null){
					return moveAction;
				}
			}
		}
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
		StandardEntity position = this.worldInfo.getEntity(this.agentInfo.getPosition());
		Edge edge = target.getEdgeTo(position.getID());
		if (edge == null) {
			return null;
		}
		if (position instanceof Road || position instanceof Hydrant) {
			if(edge.isPassable()) {
				Road road = (Road) position;
				Collection<Blockade> blockades = this.worldInfo.getBlockades(road).stream().filter(Blockade::isApexesDefined)
						.collect(Collectors.toSet());
				Point2D Police = new Point2D(agentX,agentY);
				Point2D mid = new Point2D((edge.getStartX() + edge.getEndX()) / 2,(edge.getStartY() + edge.getEndY()) / 2);
//				return this.clearToPoint(blockades, Police, mid);
				Action clearAction = this.clearPassEdge(blockades,Police,mid);
				if(clearAction != null){
					return clearAction;
				}
//				return this.clearPassEdge(blockades,Police,mid,target);
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
					if (minPointDistance < this.clearDistance/ 2) {
						Vector2D vector = this.scaleClear(this.getVector(agentX, agentY, clearX, clearY));
						clearX = (int) (agentX + vector.getX());
						clearY = (int) (agentY + vector.getY());
						lastClearTarget = null;
						double dist = this.getDistance(agentX, agentY, clearX, clearY);
						return new ActionClear(clearX, clearY, clearBlockade);
					}
					return new ActionMove(Lists.newArrayList(police.getPosition()), (int)intersection.getX(),(int) intersection.getY());
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
		if(distance < 500 || distance > 3000) {
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
			return new ActionMove(Lists.newArrayList(police.getPosition()), (int)intersection.getX(), (int)intersection.getY());
		}
		else {
			return new ActionMove(Lists.newArrayList(police.getPosition()),(int) closest.getX(),(int) closest.getY());
		}
	}

	/**
	 * @Description: Applying in the function getNeighbourPositionAction,different to function clearToPoint
	 * @Author: Bochun-Yue
	 * @Date: 11/17/20
	 */
	private Action clearPassEdge(Collection<Blockade> blockades,Point2D agent,Point2D edgeMidPoint) {
		PoliceForce police = (PoliceForce) this.agentInfo.me();
		double agentX = agent.getX();
		double agentY = agent.getY();
		Point2D intersection = null;
		Double minPointDistance = Double.MAX_VALUE;
		int clearX = 0;
		int clearY = 0;
		Line2D ToGuideLine = new Line2D(agent,edgeMidPoint);
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
			return new ActionMove(Lists.newArrayList(police.getPosition()), (int)intersection.getX(), (int)intersection.getY());
		}
		else {
			return null;
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

			Set<Road> entrances = new HashSet<>();
			for(EntityID id : loc.getNeighbours()){
				StandardEntity neighbour = this.worldInfo.getEntity(id);
				if(neighbour instanceof Road || neighbour instanceof Hydrant){
					Road road = (Road) neighbour;
					entrances.add(road);
				}
			}
			int size = entrances.size();
			int count = 0;
			for (Road next : entrances) {
				if (this.isRoadPassable(next))
					continue;
				count++;
			}

			if (count == size)
				return true;
		}
		return false;
	}
}
