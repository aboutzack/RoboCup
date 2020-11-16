package CSU_Yunlu_2020.world.object;

import CSU_Yunlu_2020.CSUConstants;
import CSU_Yunlu_2020.debugger.DebugHelper;
import CSU_Yunlu_2020.geom.ExpandApexes;
import CSU_Yunlu_2020.standard.Ruler;
import CSU_Yunlu_2020.util.Util;
import CSU_Yunlu_2020.world.CSUWorldHelper;
import CSU_Yunlu_2020.world.graph.GraphHelper;
import CSU_Yunlu_2020.world.graph.MyEdge;
import CSU_Yunlu_2020.world.graph.Node;
import adf.agent.info.AgentInfo;
import adf.launcher.ConfigKey;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.awt.geom.Area;
import java.io.Serializable;
import java.util.List;
import java.util.*;

/**
 * Mainly for blockades.
 *
 * Date: May 31, 2014  Time: 7:50pm
 *
 * @author appreciation-csu
 *
 */
public class CSURoad {
	private double CLEAR_WIDTH; // 3m = 3000mm

	private Road selfRoad;
	private EntityID selfId;
	private CSUWorldHelper world;
	private GraphHelper graph;
	private AgentInfo agentInfo;

	private CSULineOfSightPerception lineOfSightPerception;
	private List<EntityID> observableAreas;

	private List<CSUEdge> csuEdges;
	private List<CSUBlockade> csuBlockades = new ArrayList<>();

	private Pair<Line2D, Line2D> pfClearLines = null;
	private Area pfClearArea = null;

	private int lastUpdateTime = 0;
	private Polygon polygon;
	private int passablyLastResetTime = 0;
	private List<CSULineOfSightPerception.CsuRay> lineOfSight;
	private Set<EntityID> visibleFrom;

	/**
	 * When {@link CSURoad#pfClearLines} is null, the roadCenterLine is null, too.
	 */
	private Line2D roadCenterLine = null;

	private boolean isEntrance = false;
	private boolean isRoadCenterBlocked = false;
	private static final double COLLINEAR_THRESHOLD = 1.0E-3D;

	// constructor
	public CSURoad(Road road, CSUWorldHelper world) {
		this.world = world;
		this.graph = world.getGraph();
		this.agentInfo = world.getAgentInfo();
		this.selfRoad = road;
		this.selfId = road.getID();
		this.lineOfSightPerception = new CSULineOfSightPerception(world);
		this.csuEdges = createCsuEdges();

		this.CLEAR_WIDTH = world.getConfig().repairRad;
		this.lineOfSight = new ArrayList<>();
		this.visibleFrom = new HashSet<>();
		createPolygon();
	}

	// constructor, only for test
	public CSURoad(EntityID roadId, List<CSUEdge> edges) {
		this.selfId = roadId;
		this.csuEdges = edges;
	}

	/**
	 * Update the blockade inform.
	 */
	public void update() {
		//reset
		lastUpdateTime = world.getTime();
		if (selfRoad.isBlockadesDefined()) {
			for (CSUEdge next : csuEdges) {
				next.setOpenPart(next.getLine());
				next.setBlocked(false);
			}
			for (MyEdge myEdge : graph.getMyEdgesInArea(selfId)) {
				myEdge.setPassable(true);
			}
			//update
			this.csuBlockades = createCsuBlockade();
			if (selfRoad.isBlockadesDefined()) {
				for (CSUEdge csuEdge : csuEdges) {
					if (csuEdge.isPassable()) {
						csuEdge.setOpenPart(csuEdge.getLine());
						List<CSUBlockade> blockedStart = new ArrayList<>();
						List<CSUBlockade> blockedEnd = new ArrayList<>();
						for (CSUBlockade csuBlockade : csuBlockades) {

							if (Ruler.getDistance(csuBlockade.getPolygon(), csuEdge.getStart()) < CSUConstants.AGENT_PASSING_THRESHOLD_SMALL) {
								blockedStart.add(csuBlockade);
							}
							if (Ruler.getDistance(csuBlockade.getPolygon(), csuEdge.getEnd()) < CSUConstants.AGENT_PASSING_THRESHOLD_SMALL) {
								blockedEnd.add(csuBlockade);
							}
						}
						setCsuEdgeOpenPart(csuEdge);
						if (csuBlockades.size() == 1) {
							if (Util.containsEach(blockedEnd, blockedStart)) {
								csuBlockades.get(0).addBlockedEdges(csuEdge);
								csuEdge.setBlocked(true);
							}
						} else {
							for (CSUBlockade block1 : blockedStart) {
								for (CSUBlockade block2 : blockedEnd) {
									if (Util.isPassable(block1.getPolygon(), block2.getPolygon(), CSUConstants.AGENT_PASSING_THRESHOLD_SMALL)) {
										csuEdge.setBlocked(true);
										block1.addBlockedEdges(csuEdge);
										block2.addBlockedEdges(csuEdge);
									}

								}
							}
						}
					} else {
						for (CSUBlockade csuBlockade : csuBlockades) {
							double distance = Ruler.getDistance(csuEdge.getLine(), csuBlockade.getPolygon());

							if (distance < CSUConstants.AGENT_PASSING_THRESHOLD_SMALL) {
								csuEdge.setBlocked(true);
								csuBlockade.addBlockedEdges(csuEdge);
							}

						}
					}
				}
			}
			updateNodePassably();
			updateMyEdgePassably();
		}
	}

	private boolean isTimeToResetPassably() {
		int resetTime = CSUConstants.ROAD_PASSABLY_RESET_TIME_IN_MEDIUM_MAP;
		return passablyLastResetTime <= lastUpdateTime && agentInfo.getTime() - passablyLastResetTime > resetTime &&
				agentInfo.getTime() - lastUpdateTime > resetTime;
	}
	
	public void resetPassably() {
		boolean isSeen = world.getRoadsSeen().contains(selfId);
//		//在有radio通讯情况下，会收到其他agent关于某MyEdge不可通的信息，所以即使看不见road也进行reset
//		if (!isSeen && world.isCommunicationLess()) {
//			return;
//		}
		//最近的重置时间在最近更新时间之前,保证每次更新之后至多进行一次重置
		if (agentInfo.me() instanceof PoliceForce || passablyLastResetTime > lastUpdateTime) {
			return;
		}
		if (isTimeToResetPassably()) {
			reset();
		}
	}

	private void reset() {
		if (!(agentInfo.me() instanceof Human)) {
			return;
		}
		for (CSUEdge csuEdge : csuEdges) {
			csuEdge.setBlocked(false);
			CSUEdge otherEdge = csuEdge.getOtherSideEdge();
			csuEdge.setOpenPart(csuEdge.getLine());
			if (otherEdge != null) {
				CSURoad csuRoad = world.getCsuRoad(csuEdge.getNeighbours().second());
				if (csuRoad.getLastUpdateTime() < lastUpdateTime) {
					otherEdge.setOpenPart(otherEdge.getLine());
				}
			}
			rescuecore2.standard.entities.Area neighbour = (rescuecore2.standard.entities.Area) world.getEntity(csuEdge.getNeighbours().second());
			if (csuEdge.isPassable()) {
				Node node = graph.getNode((csuEdge.getMiddlePoint()));
				if (node == null) {
					System.out.println("node == null in " + selfId);
					continue;
				}
				if (neighbour instanceof Road) {
					CSURoad csuRoad = world.getCsuRoad(neighbour.getID());
					CSUEdge neighbourEdge = csuRoad.getCsuEdgeInPoint(csuEdge.getMiddlePoint());
					if (neighbourEdge != null && !neighbourEdge.isBlocked()) {
						node.setPassable(true, agentInfo.getTime());
					}
				} else {
					node.setPassable(true, agentInfo.getTime());
				}
			}
		}
		for (MyEdge myEdge : graph.getMyEdgesInArea(selfId)) {
			myEdge.setPassable(true);
		}
		passablyLastResetTime = agentInfo.getTime();
	}

	/**
	 * 更新node是否可通
	 */
	private void updateNodePassably() {
		for (CSUEdge csuEdge : csuEdges) {
			if (csuEdge.isPassable()) {
				Node node = graph.getNode(csuEdge.getMiddlePoint());
				if (node == null) {
					continue;
				}
				if (csuEdge.isBlocked() || csuEdge.getOtherSideEdge().isBlocked()) {
					node.setPassable(false, agentInfo.getTime());
				} else {
					node.setPassable(true, agentInfo.getTime());
				}
			}
		}
	}

	/**
	 * 更新myEdge是否可通过
	 */
	private void updateMyEdgePassably() {
		for (int i = 0; i < csuEdges.size() - 1; i++) {
			CSUEdge edge1 = csuEdges.get(i);
			if (!edge1.isPassable()) {
				continue;
			}
			//考虑passable的csuEdge
			for (int j = i + 1; j < csuEdges.size(); j++) {
				CSUEdge edge2 = csuEdges.get(j);
				if (!edge2.isPassable()) {
					continue;
				}
				setMyEdgePassably(edge1, edge2, isPassable(edge1, edge2));
			}
		}
	}

	private void setMyEdgePassably(CSUEdge edge1, CSUEdge edge2, boolean passably) {
		if (!(agentInfo.me() instanceof Human) || !edge1.getNeighbours().second().equals(edge2.getNeighbours().second())) {
			return;
		}
		Node node1 = graph.getNode(edge1.getMiddlePoint());
		Node node2 = graph.getNode(edge2.getMiddlePoint());
		MyEdge myEdge = graph.getMyEdge(selfId, new Pair<>(node1, node2));
		if (myEdge != null) {
			myEdge.setPassable(passably);
		}
	}

	/**
	 * @return 两edge之间是否可通
	 */
	public boolean isPassable(CSUEdge from, CSUEdge to) {
		if (!from.getNeighbours().second().equals(to.getNeighbours().second())) {
			System.err.println("this 2 edge is not in a same area!!!");
			return false;
		}
		//如果有一端block,则返回false
		if (from.isBlocked() || to.isBlocked())
			return false;
		//这两条edge之间的所有edge
		Pair<List<CSUEdge>, List<CSUEdge>> edgesBetween = getEdgesBetween(from, to, false);
	
		int count = csuBlockades.size();
		List<CSUEdge> blockedEdges = new ArrayList<>();
		if (count == 1) {
			//添加进blockEdge
			blockedEdges.addAll(csuBlockades.get(0).getBlockedEdges());
		} else if (count > 1) {
			for (int i = 0; i < count - 1; i++) {
				CSUBlockade block1 = csuBlockades.get(i);
				for (int j = i + 1; j < count; j++) {
					CSUBlockade block2 = csuBlockades.get(j);
					if (isBlockedTwoSides(block1, edgesBetween)) {
						return false;
					}
					if (isBlockedTwoSides(block2, edgesBetween)) {
						return false;
					}
					if (isInSameSide(block1, block2, edgesBetween)) {
						continue;
					}
					if (Util.isPassable(block1.getPolygon(), block2.getPolygon(), CSUConstants.AGENT_PASSING_THRESHOLD)) {
						blockedEdges.removeAll(block1.getBlockedEdges());
						blockedEdges.addAll(block1.getBlockedEdges());
						blockedEdges.removeAll(block2.getBlockedEdges());
						blockedEdges.addAll(block2.getBlockedEdges());
					}
				}
			}
		} else if (count == 0) {
			return !(from.isBlocked() || to.isBlocked());
		}
		return !(Util.containsEach(blockedEdges, edgesBetween.first()) && Util.containsEach(blockedEdges, edgesBetween.second()));
	}

	private Pair<List<CSUEdge>, List<CSUEdge>> getEdgesBetween(CSUEdge edge1, CSUEdge edge2, boolean justImPassable) {
		List<CSUEdge> leftSideEdges = new ArrayList<>();
		List<CSUEdge> rightSideEdges = new ArrayList<>();
		rescuecore2.misc.geometry.Point2D startPoint1 = edge1.getStart();
		rescuecore2.misc.geometry.Point2D endPoint1 = edge1.getEnd();
		rescuecore2.misc.geometry.Point2D startPoint2 = edge2.getStart();
		rescuecore2.misc.geometry.Point2D endPoint2 = edge2.getEnd();

		boolean finishedLeft = false;
		boolean finishedRight = false;
		for (CSUEdge edge : csuEdges) {
			if (finishedLeft && finishedRight)
				break;
			for (CSUEdge ed : csuEdges) {
				if (finishedLeft && finishedRight)
					break;
				if (ed.equals(edge1) || ed.equals(edge2)) {
					continue;
				}
				if (startPoint1.equals(startPoint2) || startPoint1.equals(endPoint2)) {
					finishedLeft = true;
				}
				if (endPoint1.equals(startPoint2) || endPoint1.equals(endPoint2)) {
					finishedRight = true;
				}

				if (ed.getStart().equals(startPoint1) && !finishedLeft && !leftSideEdges.contains(ed)) {
					startPoint1 = ed.getEnd();
					if (!justImPassable || !ed.isPassable())
						leftSideEdges.add(ed);
					continue;
				}
				if (ed.getEnd().equals(startPoint1) && !finishedLeft && !leftSideEdges.contains(ed)) {
					startPoint1 = ed.getStart();
					if (!justImPassable || !ed.isPassable())
						leftSideEdges.add(ed);
					continue;
				}
				if (ed.getStart().equals(endPoint1) && !finishedRight && !rightSideEdges.contains(ed)) {
					endPoint1 = ed.getEnd();
					if (!justImPassable || !ed.isPassable())
						rightSideEdges.add(ed);
					continue;
				}
				if (ed.getEnd().equals(endPoint1) && !finishedRight && !rightSideEdges.contains(ed)) {
					endPoint1 = ed.getStart();
					if (!justImPassable || !ed.isPassable())
						rightSideEdges.add(ed);
					continue;
				}
			}
		}
		return new Pair<>(leftSideEdges, rightSideEdges);
	}

	private boolean isInSameSide(CSUBlockade block1, CSUBlockade block2, Pair<List<CSUEdge>, List<CSUEdge>> edgesBetween) {
		return edgesBetween.first().containsAll(block1.getBlockedEdges()) &&
				edgesBetween.first().containsAll(block2.getBlockedEdges()) ||
				edgesBetween.second().containsAll(block1.getBlockedEdges()) &&
						edgesBetween.second().containsAll(block2.getBlockedEdges());
	}

	private boolean isBlockedTwoSides(CSUBlockade block1, Pair<List<CSUEdge>, List<CSUEdge>> edgesBetween) {
		return Util.containsEach(edgesBetween.first(), block1.getBlockedEdges()) &&
				Util.containsEach(edgesBetween.second(), block1.getBlockedEdges());
	}

	private void createPolygon() {
		int[] apexList = selfRoad.getApexList();
		polygon = Util.getPolygon(apexList);
	}

	/**
	 * Create the CSUEdge objects for this road
	 *
	 * @return a list of CSUEdges
	 */
	private List<CSUEdge> createCsuEdges() {
		List<CSUEdge> result = new ArrayList<>();

		for (Edge next : selfRoad.getEdges()) {
			result.add(new CSUEdge(world, next, selfRoad.getID()));
		}

		return result;
	}

	/**
	 * Create the CSUBlockade objects for this road.
	 *
	 * @return a list of CSUBlockades
	 */
	private List<CSUBlockade> createCsuBlockade() {
		List<CSUBlockade> result = new ArrayList<>();
		if (!selfRoad.isBlockadesDefined())
			return result;
		for (EntityID next : selfRoad.getBlockades()) {
			StandardEntity entity = world.getEntity(next, StandardEntity.class);
			if (entity == null)
				continue;
			if (!(entity instanceof Blockade))
				continue;
			Blockade bloc = (Blockade) entity;
			if (!bloc.isApexesDefined())
				continue;
			if (bloc.getApexes().length < 6)
				continue;
			result.add(new CSUBlockade(next, world));
		}

		return result;
	}

	/**
	* @Description: 精确设置openPart和isBlocked
	* @Author: Guanyu-Cai
	* @Date: 3/12/20
	*/
	private void setCsuEdgeOpenPart(CSUEdge edge) {
		//first: startPoint second: endPoint
		List<Pair<Point2D, Point2D>> blockadePartPoints = new ArrayList<>();
		Point2D edgeStart = edge.getStart();
		Point2D edgeEnd = edge.getEnd();
		//初始化为整条line
		boolean isBlocked = false;
		List<CSUBlockade> totalBlockades = new ArrayList<>(csuBlockades);
		//获取edge连接的道路的所有blockades
		CSURoad neighborRoad = world.getCsuRoad(edge.getNeighbours().first());
		if (neighborRoad != null) {
			List<CSUBlockade> neighborBlockades = neighborRoad.getCsuBlockades();
			if (neighborBlockades != null) {
				totalBlockades.addAll(neighborBlockades);
			}
		}
		for (CSUBlockade blockade : totalBlockades) {
			boolean isStartBlocked = false;
			boolean isEndBlocked = false;
			if (blockade.getPolygon().contains(selfRoad.getX(), selfRoad.getY())) {
				isRoadCenterBlocked = true;
			}
			//blockade所在多边形扩大10
//			Polygon expand = ExpandApexes.expandApexes(blockade.getSelfBlockade(), 10);
			Polygon expand = Util.scaleBySize(blockade.getPolygon(), 10);
			if (expand.contains(edgeStart.getX(), edgeStart.getY())) {
				isStartBlocked = true;
			}
			if (expand.contains(edgeEnd.getX(), edgeEnd.getY())) {
				isEndBlocked = true;
			}

			Set<Point2D> intersections = Util.getIntersections(expand, edge.getLine());

			if (isStartBlocked && isEndBlocked) {//判定为整条edge堵住
				//确定堵住,不需要再判断
				isBlocked = true;
				blockadePartPoints.add(new Pair<>(edgeStart, edgeEnd));
				break;
			}else if (isStartBlocked) {
				double maxDistance = Double.MIN_VALUE, distance;
				Point2D blockadePartEnd = null;
				for (Point2D point : intersections) {
					distance = distance(point, edgeStart);
					if (distance > maxDistance) {
						maxDistance = distance;
						blockadePartEnd = point;
					}
				}
				blockadePartPoints.add(new Pair<>(edgeStart, blockadePartEnd));
			} else if (isEndBlocked) {
				double maxDistance = Double.MIN_VALUE, distance;
				Point2D blockadePartStart = null;
				for (Point2D point : intersections) {
					distance = distance(point, edgeEnd);
					if (distance > maxDistance) {
						maxDistance = distance;
						blockadePartStart = point;
					}
				}
				blockadePartPoints.add(new Pair<>(blockadePartStart, edgeEnd));
			} else {//可能是在中间堵住edge或者和edge没有接触,至少需要两个接触点
				if (!intersections.isEmpty() && intersections.size() > 1) {
					//两点之间是不能通过的区域
					Pair<Point2D, Point2D> twoFarthestPoints = getTwoFarthestPoints(intersections);
					double distanceToFirst = Ruler.getDistance(edgeStart, twoFarthestPoints.first());
					double distanceToSecond = Ruler.getDistance(edgeStart, twoFarthestPoints.second());
					if (distanceToFirst < distanceToSecond) {
						blockadePartPoints.add(new Pair<>(twoFarthestPoints.first(), twoFarthestPoints.second()));
					} else {
						blockadePartPoints.add(new Pair<>(twoFarthestPoints.second(), twoFarthestPoints.first()));
					}
				}
			}
		}

		if (isBlocked) {
			edge.setBlocked(true);
			edge.setOpenPart(null);
		} else {
			List<Line2D> openPartLines = calcOpenPart(blockadePartPoints, edgeStart, edgeEnd);
			if (!openPartLines.isEmpty()) {
				//将最长的设置为openPart
				edge.setOpenPart(openPartLines.get(openPartLines.size() - 1));
				if (Ruler.getLength(edge.getOpenPart()) <= CSUConstants.AGENT_MINIMUM_PASSING_THRESHOLD) {
					edge.setBlocked(true);
				}else {
					edge.setBlocked(false);
				}
			}
		}
	}

	/**
	* @Description: 获取距离最远的两个点,暴力算法复杂度高
	* @Author: Guanyu-Cai
	* @Date: 3/12/20
	*/
	private Pair<Point2D, Point2D> getTwoFarthestPoints(Set<Point2D> points) {
		double maxDistance = Double.MIN_VALUE;
		Point2D p1 = null;
		Point2D p2 = null;
		for (Point2D p3 : points) {
			for (Point2D p4 : points) {
				double distance = Ruler.getDistance(p3, p4);
				if (distance > maxDistance) {
					maxDistance = distance;
					p1 = p3;
					p2 = p4;
				}
			}
		}
		return new Pair<>(p1, p2);
	}

	/**
	 * @Description: 根据所有的blockadePart计算openPart, 显然blockadePart都是不想交的
	 * @Author: Guanyu-Cai
	 * @Date: 3/12/20
	 */
	private List<Line2D> calcOpenPart(List<Pair<Point2D, Point2D>> blockadePartPoints, Point2D edgeStart, Point2D edgeEnd) {
		//按照和edgeStart距离从小到达排序
		blockadePartPoints.sort(new Util.DistanceComparator(edgeStart));
		//将edgeStart和edgeEnd视为blockade的end和start方便计算
		blockadePartPoints.add(0, new Pair<>(null, edgeStart));
		blockadePartPoints.add(blockadePartPoints.size(), new Pair<>(edgeEnd, null));
		List<Line2D> openPartLines = new ArrayList<>();
		for (int i = 0; i < blockadePartPoints.size() - 1; i++) {
			if (Ruler.getDistance(blockadePartPoints.get(i).second(), edgeStart) < Ruler.getDistance(blockadePartPoints.get(i + 1).first(), edgeStart)) {
				//只有当line方向是从edgeStart到edgeEnd才视为openPart
				openPartLines.add(new Line2D(blockadePartPoints.get(i).second(), blockadePartPoints.get(i + 1).first()));
			}

		}
		//按照openPart长度从小到大排序
		openPartLines.sort(new Util.LengthComparator());

		return openPartLines;
	}

	public Road getSelfRoad() {
		return selfRoad;
	}

	public EntityID getId() {
		return this.selfId;
	}

	public List<EntityID> getObservableAreas() {
		if (observableAreas == null || observableAreas.isEmpty()) {
			observableAreas = lineOfSightPerception.getVisibleAreas(getId());
		}
		return observableAreas;
	}

	public CSUEdge getCsuEdgeInPoint(Point2D middlePoint) {
		for (CSUEdge next : csuEdges) {
			if (contains(next.getLine(), middlePoint, 1.0))
				return next;
		}

		return null;
	}

	/**
	 * For entrance only.
	 *
	 * @return true when this entrance is need to clear.
	 * 140824 true -- dont need ,false -- need
	 */
	public boolean isNeedlessToClear() {
		double buildingEntranceLength = 0.0;
		double maxUnpassableEdgeLength = Double.MIN_VALUE;
		double length;

		Edge buildingEntrance = null;

		//building的边
		for (Edge next : selfRoad.getEdges()) {
			//可以通过的边
			if (next.isPassable()) {
				StandardEntity entity = world.getEntity(next.getNeighbour(), StandardEntity.class);
				if (entity instanceof Building) {
					buildingEntranceLength = distance(next.getStart(), next.getEnd());
					buildingEntrance = next;
				}
			} else {
				length = distance(next.getStart(), next.getEnd());
				if (length > maxUnpassableEdgeLength) {
					maxUnpassableEdgeLength = length;
				}
			}
		}

		if (buildingEntrance == null)
			return true;
		double rad = buildingEntranceLength + maxUnpassableEdgeLength;
		Area entranceArea = entranceArea(buildingEntrance.getLine(), rad);

		Set<EntityID> blockadeIds = new HashSet<>();

		if (selfRoad.isBlockadesDefined()) {
			blockadeIds.addAll(selfRoad.getBlockades());
		}

		for (EntityID next : selfRoad.getNeighbours()) {
			StandardEntity entity = world.getEntity(next, StandardEntity.class);
			if (entity instanceof Road) {
				Road road = (Road) entity;
				if (road.isBlockadesDefined())
					blockadeIds.addAll(road.getBlockades());
			}
		}

		for (EntityID next : blockadeIds) {
			StandardEntity entity = world.getEntity(next, StandardEntity.class);
			if (entity == null)
				continue;
			if (!(entity instanceof Blockade))
				continue;
			Blockade blockade = (Blockade)entity;
			if (!blockade.isApexesDefined())
				continue;
			//？？
			if (blockade.getApexes().length < 6)
				continue;
			Polygon po = Util.getPolygon(blockade.getApexes());
			Area blocArea = new Area(po);
			blocArea.intersect(entranceArea);
			//??
			if (!blocArea.getPathIterator(null).isDone())
				return false;
		}
		return true;
	}

	private Area entranceArea(Line2D line, double rad) {
		double theta = Math.atan2(line.getEndPoint().getY() - line.getOrigin().getY(),
				line.getEndPoint().getX() - line.getOrigin().getX());
		theta = theta - Math.PI / 2;
		while (theta > Math.PI || theta < -Math.PI) {
			if (theta > Math.PI)
				theta -= 2 * Math.PI;
			else
				theta += 2 * Math.PI;
		}
		int x = (int)(rad * Math.cos(theta)), y = (int)(rad * Math.sin(theta));

		Polygon polygon = new Polygon();
		polygon.addPoint((int)(line.getOrigin().getX() + x), (int)(line.getOrigin().getY() + y));
		polygon.addPoint((int)(line.getEndPoint().getX() + x), (int)(line.getEndPoint().getY() + y));
		polygon.addPoint((int)(line.getEndPoint().getX() - x), (int)(line.getEndPoint().getY() - y));
		polygon.addPoint((int)(line.getOrigin().getX() - x), (int)(line.getOrigin().getY() - y));

		return new Area(polygon);
	}

	public List<CSUEdge> getCsuEdgesTo(EntityID neighbourId) {
		List<CSUEdge> result = new ArrayList<>();

		for (CSUEdge next : csuEdges) {
			if (next.isPassable() && next.getNeighbours().first().equals(neighbourId)) {
				result.add(next);
			}
		}

		return result;
	}

	// TODO July 9, 2014  Time: 2:59pm
	/**
	 * Get all passable edge od this road. If all edge are impassable, then you
	 * are stucked.
	 *
	 * @return a set of passable edge.
	 */
	public Set<CSUEdge> getPassableEdges() {
		Set<CSUEdge> result = new HashSet<>();

		for (CSUEdge next : csuEdges) {
			if (next.isPassable() && !next.isBlocked()) {
				result.add(next);
			}
		}

		return result;
	}

	/**
	 * Determines whether this road's center point is covered by blockades.
	 *
	 * @return true when this road's center point is covered by blockade.
	 *         Otherwise, false.
	 */
	public boolean isRoadCenterBlocked() {
		return this.isRoadCenterBlocked;
	}

	/**
	 * Determines whether this road is passable or not. When this road is
	 * totally blocked by a blockade, then this road is impassable.
	 *
	 * @return true when this road is passable. Otherwise, false.
	 */
	public boolean isPassable() {
		if (isAllEdgePassable() || isOneEdgeUnpassable()) {
			
			/*for (CSUBlockade next : getCsuBlockades()) {
				if (next.getPolygon().contains(selfRoad.getX(), selfRoad.getY()))
					return false;
			}*/
			// return true;
			// TODO July 9, 2014  Time: 2:58pm
			return getPassableEdges().size() > 1;      ///why >
		} else {
			List<CSUBlockade> blockades = new LinkedList<>(getCsuBlockades());
			
			for (CSUEscapePoint next : getEscapePoint(this, 500)) {
				blockades.removeAll(next.getRelateBlockade());
			}
			
			if (blockades.isEmpty())
				return true;
			return false;
		}
	}

	/**
	* @Description: 判断这条路需不需要清理
	* @Author: Guanyu-Cai
	* @Date: 3/17/20
	*/
	public boolean isPassableForPF() {
		if (isAllEdgePassable() || isOneEdgeUnpassable()) {
			return getPassableEdges().size() > 1;      ///why >
		}
		boolean isPassable = true;
		//判断blockade是否会和两条相对的edge相交,如果会则不可通过
		List<Polygon> blockadePolygons = getBlockadePolygons(10);
		for (Polygon polygon : blockadePolygons) {
			for (CSUEdge edge : csuEdges) {
				CSUEdge oppositeEdge = getOppositeEdge(edge);
				if (Util.hasIntersectLine(polygon, Util.improveLineBothSides(edge.getLine(), 300000)) &&
						Util.hasIntersectLine(polygon, Util.improveLineBothSides(oppositeEdge.getLine(), 300000))) {
					isPassable = false;
					break;
				}
			}
			if (!isPassable) {
				break;
			}
		}
		List<CSUBlockade> blockades = new LinkedList<>(getCsuBlockades());

		for (CSUEscapePoint next : getEscapePoint(this, 500)) {
			blockades.removeAll(next.getRelateBlockade());
		}

		return blockades.isEmpty();
	}

	/**
	 * Determines the passability of entrances.
	 * @return true when entrance is passable. Otherwise, false.
	 */
	public boolean isEntrancePassable() {
		return false;
	}

	public boolean isAllEdgePassable() {
		for (CSUEdge next : csuEdges) {
			if (!next.isPassable())
				return false;
		}
		return true;
	}

	public boolean isOneEdgeUnpassable() {
		int count = 0;
		for (CSUEdge next : csuEdges) {
			if (!next.isPassable())
				count++;
		}

		if (count == 1)
			return true;
		else
			return false;
	}

	public boolean isEntrance() {
		return this.isEntrance;
	}

	public boolean isEntranceNeighbour() {
//		for (EntityID next : selfRoad.getNeighbours()) {
//			StandardEntity neig = worldHelper.getEntity(next, StandardEntity.class);
//			if (neig instanceof Road && worldHelper.getEntrance().containsKey((Road)neig))
//				return true;
//		}
		return false;
	}

	public void setEntrance(boolean entrance) {
		this.isEntrance = entrance;
	}

	/**
	 * We only consider the case when there are four edges, excluding entrances.
	 * <p>
	 * Anti-clockwise of verters.
	 */
	public Pair<Line2D, Line2D> getPfClearLine(CSURoad road) {

		if (this.pfClearLines != null)
			return this.pfClearLines;

		if (road.getCsuEdges().size() != 4)
			return null;
		if (road.isAllEdgePassable())
			return null;

		CSUEdge edge_1 = road.getCsuEdges().get(0);
		CSUEdge edge_2 = road.getCsuEdges().get(1);
		CSUEdge edge_3 = road.getCsuEdges().get(2);
		CSUEdge edge_4 = road.getCsuEdges().get(3);

		Line2D line_1 = null, line_2 = null, line_3 = null, line_4 = null;

		if (edge_1.isPassable() && edge_3.isPassable()) {
			roadCenterLine = new Line2D(edge_1.getMiddlePoint(), edge_3.getMiddlePoint());

			Point2D perpendicular_1, perpendicular_2;

			Pair<Double, Boolean> dis = ptSegDistSq(edge_2.getLine(), edge_1.getStart());
			if (dis.second().booleanValue()) { // the point is out the range of this line
				perpendicular_1 = GeometryTools2D.getClosestPoint(edge_4.getLine(), edge_1.getEnd());
				line_1 = new Line2D(perpendicular_1, edge_1.getEnd());
			} else { // the point is within the range of this line
				perpendicular_1 = GeometryTools2D.getClosestPoint(edge_2.getLine(), edge_1.getStart());
				line_1 = new Line2D(edge_1.getStart(), perpendicular_1);
			}

			dis = ptSegDistSq(edge_4.getLine(), edge_3.getStart());
			if (dis.second().booleanValue()) {
				perpendicular_2 = GeometryTools2D.getClosestPoint(edge_2.getLine(), edge_3.getEnd());
				line_2 = new Line2D(edge_3.getEnd(), perpendicular_2);
			} else {
				perpendicular_2 = GeometryTools2D.getClosestPoint(edge_4.getLine(), edge_3.getStart());
				line_2 = new Line2D(perpendicular_2, edge_3.getStart());
			}
		} else if (edge_2.isPassable() && edge_4.isPassable()) {
			roadCenterLine = new Line2D(edge_2.getMiddlePoint(), edge_4.getMiddlePoint());

			Point2D perpendicular_1, perpendicular_2;

			Pair<Double, Boolean> dis = ptSegDistSq(edge_3.getLine(), edge_2.getStart());
			if (dis.second().booleanValue()) {
				perpendicular_1 = GeometryTools2D.getClosestPoint(edge_1.getLine(), edge_2.getEnd());
				line_1 = new Line2D(perpendicular_1, edge_2.getEnd());
			} else {
				perpendicular_1 = GeometryTools2D.getClosestPoint(edge_3.getLine(), edge_2.getStart());
				line_1 = new Line2D(edge_2.getStart(), perpendicular_1);
			}

			dis = ptSegDistSq(edge_1.getLine(), edge_4.getStart());
			if (dis.second().booleanValue()) {
				perpendicular_2 = GeometryTools2D.getClosestPoint(edge_3.getLine(), edge_4.getEnd());
				line_2 = new Line2D(edge_4.getEnd(), perpendicular_2);
			} else {
				perpendicular_2 = GeometryTools2D.getClosestPoint(edge_1.getLine(), edge_4.getStart());
				line_2 = new Line2D(perpendicular_2, edge_4.getStart());
			}
		}

		double rate_1 = CLEAR_WIDTH / getLength(line_1);
		double rate_2 = CLEAR_WIDTH / getLength(line_2);
		Point2D mid_1 = getMiddle(line_1), mid_2 = getMiddle(line_2);

		Point2D end_1 = (new Line2D(mid_1, line_1.getOrigin())).getPoint(rate_1);
		Point2D end_2 = (new Line2D(mid_2, line_2.getOrigin())).getPoint(rate_2);
		line_3 = new Line2D(end_1, end_2);

		end_1 = (new Line2D(mid_1, line_1.getEndPoint())).getPoint(rate_1);
		end_2 = (new Line2D(mid_2, line_2.getEndPoint())).getPoint(rate_2);
		line_4 = new Line2D(end_1, end_2);

		this.pfClearLines = new Pair<Line2D, Line2D>(line_3, line_4);
		return this.pfClearLines;
	}

	public Area getPfClearArea(CSURoad road) {

		if (this.pfClearArea != null)
			return pfClearArea;

		if (road.getCsuEdges().size() != 4)
			return null;
		if (road.isAllEdgePassable())
			return null;

		CSUEdge edge_1 = road.getCsuEdges().get(0);
		CSUEdge edge_2 = road.getCsuEdges().get(1);
		CSUEdge edge_3 = road.getCsuEdges().get(2);
		CSUEdge edge_4 = road.getCsuEdges().get(3);

		Polygon area = new Polygon();

		Line2D line_1 = null, line_2 = null;

		if (edge_1.isPassable() && edge_3.isPassable()) {
			roadCenterLine = new Line2D(edge_1.getMiddlePoint(), edge_3.getMiddlePoint());
			Point2D perpendicular_1, perpendicular_2;

			Pair<Double, Boolean> dis = ptSegDistSq(edge_2.getLine(), edge_1.getStart());
			if (!dis.second().booleanValue()) { // the point is out the range of this line
				perpendicular_1 = GeometryTools2D.getClosestPoint(edge_4.getLine(), edge_1.getEnd());
				line_1 = new Line2D(perpendicular_1, edge_1.getEnd());
			} else { // the point is within the range of this line
				perpendicular_1 = GeometryTools2D.getClosestPoint(edge_2.getLine(), edge_1.getStart());
				line_1 = new Line2D(edge_1.getStart(), perpendicular_1);
			}

			dis = ptSegDistSq(edge_4.getLine(), edge_3.getStart());
			if (!dis.second().booleanValue()) {
				perpendicular_2 = GeometryTools2D.getClosestPoint(edge_2.getLine(), edge_3.getEnd());
				line_2 = new Line2D(edge_3.getEnd(), perpendicular_2);
			} else {
				perpendicular_2 = GeometryTools2D.getClosestPoint(edge_4.getLine(), edge_3.getStart());
				line_2 = new Line2D(perpendicular_2, edge_3.getStart());
			}
		} else if (edge_2.isPassable() && edge_4.isPassable()) {
			roadCenterLine = new Line2D(edge_2.getMiddlePoint(), edge_4.getMiddlePoint());
			Point2D perpendicular_1, perpendicular_2;

			Pair<Double, Boolean> dis = ptSegDistSq(edge_3.getLine(), edge_2.getStart());
			if (!dis.second().booleanValue()) {
				perpendicular_1 = GeometryTools2D.getClosestPoint(edge_1.getLine(), edge_2.getEnd());
				line_1 = new Line2D(perpendicular_1, edge_2.getEnd());
			} else {
				perpendicular_1 = GeometryTools2D.getClosestPoint(edge_3.getLine(), edge_2.getStart());
				line_1 = new Line2D(edge_2.getStart(), perpendicular_1);
			}

			dis = ptSegDistSq(edge_1.getLine(), edge_4.getStart());
			if (!dis.second().booleanValue()) {
				perpendicular_2 = GeometryTools2D.getClosestPoint(edge_3.getLine(), edge_4.getEnd());
				line_2 = new Line2D(edge_4.getEnd(), perpendicular_2);
			} else {
				perpendicular_2 = GeometryTools2D.getClosestPoint(edge_1.getLine(), edge_4.getStart());
				line_2 = new Line2D(perpendicular_2, edge_4.getStart());
			}
		}

		double rate_1 = CLEAR_WIDTH / getLength(line_1);
		double rate_2 = CLEAR_WIDTH / getLength(line_2);
		Point2D mid_1 = getMiddle(line_1), mid_2 = getMiddle(line_2);

		Point2D end_1 = (new Line2D(mid_1, line_1.getOrigin())).getPoint(rate_1);
		Point2D end_2 = (new Line2D(mid_2, line_2.getOrigin())).getPoint(rate_2);
		area.addPoint((int)end_1.getX(), (int)end_1.getY());
		area.addPoint((int)end_2.getX(), (int)end_2.getY());

		end_1 = (new Line2D(mid_1, line_1.getEndPoint())).getPoint(rate_1);
		end_2 = (new Line2D(mid_2, line_2.getEndPoint())).getPoint(rate_2);

		// the order of the following two lines should not be change
		area.addPoint((int)end_2.getX(), (int)end_2.getY());
		area.addPoint((int)end_1.getX(), (int)end_1.getY());

		this.pfClearArea = new Area(area);
		return this.pfClearArea;
	}

	/**
	 * The method {@link CSURoad#getPfClearLine(CSURoad)} should be invoked
	 * somewhere before using of this method.
	 *
	 * @return the center line of this road. Null when
	 *         {@link CSURoad#getPfClearLine(CSURoad)} has not been invoked
	 *         somewhere before this metthod or the return value of
	 *         {@link CSURoad#getPfClearLine(CSURoad)} is null.
	 */
	public Line2D getRoadCenterLine() {
		return this.roadCenterLine;
	}

	private boolean contains(Line2D line, Point2D point, double threshold) {

		double pos = java.awt.geom.Line2D.ptSegDist(line.getOrigin().getX(), line.getOrigin().getY(),
				line.getEndPoint().getX(), line.getEndPoint().getY(), point.getX(), point.getY());
		if (pos <= threshold)
			return true;

		return false;
	}

	private double distance(Point2D first, Point2D second) {
		return Math.hypot(first.getX() - second.getX(), first.getY() - second.getY());
	}

	public List<CSUEscapePoint> getEscapePoint(CSURoad road, int threshold) {
		List<CSUEscapePoint> m_p_points = new ArrayList<>();

		for (CSUBlockade next : road.getCsuBlockades()) {
			if (next == null)
				continue;
			Polygon expan = next.getPolygon();

			for(CSUEdge csuEdge : road.getCsuEdges()) {
				CSUEscapePoint p = findPoints(csuEdge, expan, next);
				if (p == null) {
					continue;
				} else {
					m_p_points.add(p);
				}
			}
		}

		filter(road, m_p_points, threshold);
		return m_p_points;
	}

	private CSUEscapePoint findPoints(CSUEdge csuEdge, Polygon expan, CSUBlockade next) {
		if (csuEdge.isPassable()) {
			// do nothing
		} else {
			if (hasIntersection(expan, csuEdge.getLine())) {
				return null;
			}
			double minDistance = Double.MAX_VALUE, distance;
			Pair<Integer, Integer> minDistanceVertex = null;

			for (Pair<Integer, Integer> vertex : next.getVertexesList()) {

				Pair<Double, Boolean> dis = ptSegDistSq(csuEdge.getStart().getX(),
						csuEdge.getStart().getY(), csuEdge.getEnd().getX(),
						csuEdge.getEnd().getY(), vertex.first(), vertex.second());

				if (dis.second().booleanValue())
					continue;
				distance = dis.first().doubleValue();

				if (distance < minDistance) {
					minDistance = distance;
					minDistanceVertex = vertex;
				}
			}

			if (minDistanceVertex == null)
				return null;

			Point2D perpendicular = GeometryTools2D.getClosestPoint(csuEdge.getLine(),
					new Point2D(minDistanceVertex.first(), minDistanceVertex.second()));

			Point middlePoint = getMiddle(minDistanceVertex, perpendicular);

			Point2D vertex = new Point2D(minDistanceVertex.first(), minDistanceVertex.second());
			Point2D perpenPoint = new Point2D(perpendicular.getX(), perpendicular.getY());

			Line2D lin = new Line2D(vertex, perpenPoint);

			return new CSUEscapePoint(middlePoint, lin, next);
		}

		return null;
	}

	private void filter(CSURoad road, List<CSUEscapePoint> m_p_points, int threshold) {
		Mark:for (Iterator<CSUEscapePoint> itor = m_p_points.iterator(); itor.hasNext(); ) {

			CSUEscapePoint m_p = itor.next();
			for (CSUEdge edge : road.getCsuEdges()) {
				if (edge.isPassable())
					continue;
				if (contains(edge.getLine(), m_p.getUnderlyingPoint(), threshold / 2)) {
					itor.remove();
					continue Mark;
				}
			}

			for (CSUBlockade blockade : road.getCsuBlockades()) {
				if (blockade == null)
					continue;
				Polygon polygon = blockade.getPolygon();
				Polygon po = ExpandApexes.expandApexes(blockade.getSelfBlockade(), 200);


				if (po.contains(m_p.getLine().getEndPoint().getX(), m_p.getLine().getEndPoint().getY())) {

					Set<Point2D> intersections = Util.getIntersections(polygon, m_p.getLine());

					double minDistance = Double.MAX_VALUE, distance;
					Point2D closest = null;
					boolean shouldRemove = false;
					for (Point2D inter : intersections) {
						distance = Ruler.getDistance(m_p.getLine().getOrigin(), inter);

						if (distance > threshold && distance < minDistance) {
							minDistance = distance;
							closest = inter;
						}
						shouldRemove = true;
					}

					if (closest != null) {
						Point p = getMiddle(m_p.getLine().getOrigin(), closest);
						m_p.getUnderlyingPoint().setLocation(p);
						m_p.addCsuBlockade(blockade);
					} else if (shouldRemove){
						itor.remove();
						continue Mark;
					}
				}

				if (po.contains(m_p.getUnderlyingPoint())) {
					itor.remove();
					continue Mark;
				}
			}
		}
	}

	private boolean contains(Line2D line, Point point, double threshold) {

		double pos = java.awt.geom.Line2D.ptSegDist(line.getOrigin().getX(),
				line.getOrigin().getY(), line.getEndPoint().getX(), line
						.getEndPoint().getY(), point.getX(), point.getY());
		if (pos <= threshold)
			return true;

		return false;
	}

	private Pair<Double, Boolean> ptSegDistSq(Line2D line, Point2D point) {
		return ptSegDistSq((int)line.getOrigin().getX(), (int)line.getOrigin().getY(),
				(int)line.getEndPoint().getX(), (int)line.getEndPoint().getY(),
				(int)point.getX(), (int)point.getY());
	}

	private Pair<Double, Boolean> ptSegDistSq(double x1, double y1, double x2,
                                              double y2, double px, double py) {

		x2 -= x1;
		y2 -= y1;

		px -= x1;
		py -= y1;

		double dotprod = px * x2 + py * y2;

		double projlenSq;

		if (dotprod <= 0) {
			projlenSq = 0;
		} else {
			px = x2 - px;
			py = y2 - py;
			dotprod = px * x2 + py * y2;

			if (dotprod <= 0.0) {
				projlenSq = 0.0;
			} else {
				projlenSq = dotprod * dotprod / (x2 * x2 + y2 * y2);
			}
		}

		double lenSq = px * px + py * py - projlenSq;

		if (lenSq < 0)
			lenSq = 0;

		if (projlenSq == 0) {
			// the target point out of this line
			return new Pair<Double, Boolean>(Math.sqrt(lenSq), true);
		} else {
			// the target point within this line
			return new Pair<Double, Boolean>(Math.sqrt(lenSq), false);
		}
	}
	
	/*public boolean hasIntersection(Polygon polygon, rescuecore2.misc.geometry.Line2D line) {
		List<rescuecore2.misc.geometry.Line2D> polyLines = getLines(polygon);
		for (rescuecore2.misc.geometry.Line2D ln : polyLines) {
			Point2D p = GeometryTools2D.getSegmentIntersectionPoint(ln, line);
			if (p != null)
				return true;
		}
		return false;
	}*/

	public boolean hasIntersection(Polygon polygon, rescuecore2.misc.geometry.Line2D line) {
		List<Line2D> polyLines = getLines(polygon);
		for (rescuecore2.misc.geometry.Line2D ln : polyLines) {

			math.geom2d.line.Line2D line_1 = new math.geom2d.line.Line2D(
					line.getOrigin().getX(), line.getOrigin().getY(),
					line.getEndPoint().getX(), line.getEndPoint().getY());

			math.geom2d.line.Line2D line_2 = new math.geom2d.line.Line2D(
					ln.getOrigin().getX(), ln.getOrigin().getY(),
					ln.getOrigin().getX(), ln.getOrigin().getY());

			if (math.geom2d.line.Line2D.intersects(line_1, line_2)) {

				return true;
			}
		}
		return false;
	}

	private List<Line2D> getLines(Polygon polygon) {
		List<Line2D> lines = new ArrayList<>();
		int count = polygon.npoints;
		for (int i = 0; i < count; i++) {
			int j = (i + 1) % count;
			rescuecore2.misc.geometry.Point2D p1 = new rescuecore2.misc.geometry.Point2D(polygon.xpoints[i], polygon.ypoints[i]);
			rescuecore2.misc.geometry.Point2D p2 = new rescuecore2.misc.geometry.Point2D(polygon.xpoints[j], polygon.ypoints[j]);
			rescuecore2.misc.geometry.Line2D line = new rescuecore2.misc.geometry.Line2D(p1, p2);
			lines.add(line);
		}
		return lines;
	}
	
	private Point getMiddle(Pair<Integer, Integer> first, Point2D second) {
		int x = first.first() + (int)second.getX();
		int y = first.second() + (int)second.getY();

		return new Point(x / 2, y / 2);
	}

	private Point getMiddle(Point2D first, Point2D second) {
		int x = (int)(first.getX() + second.getX());
		int y = (int)(first.getY() + second.getY());

		return new Point(x / 2, y / 2);
	}

	private Point2D getMiddle(Line2D line) {
		double x = line.getOrigin().getX() + line.getEndPoint().getX();
		double y = line.getOrigin().getY() + line.getEndPoint().getY();

		return new Point2D(x / 2, y / 2);
	}

	private int getLength(Line2D line) {
		return (int) Ruler.getDistance(line.getOrigin(), line.getEndPoint());
	}

	public int getLastUpdateTime() {
		return lastUpdateTime;
	}

	public Polygon getPolygon() {
		return polygon;
	}

	/**
	* @Description: 根据角度获取和edge相对的一条edge
	* @Author: Guanyu-Cai
	* @Date: 3/16/20
	*/
	public CSUEdge getOppositeEdge(CSUEdge edge) {
		if (!csuEdges.contains(edge)) {
			return null;
		}
		List<Pair<CSUEdge, Line2D>> edgeLinesExcept = getEdgeLinesExcept(edge);
		edgeLinesExcept.sort(new Util.AngleComparator(edge.getLine()));
		return !edgeLinesExcept.isEmpty() ? edgeLinesExcept.get(0).first() : null;
	}

	/**
	* @Description: 根据角度获取和edge相对的一条passable edge
	* @Author: Guanyu-Cai
	* @Date: 3/13/20
	*/
	public CSUEdge getOppositePassableEdge(CSUEdge edge) {
		if (!csuEdges.contains(edge)) {
			return null;
		}
		List<Pair<CSUEdge, Line2D>> passableEdgeLinesExcept = getPassableEdgeLinesExcept(edge);
		passableEdgeLinesExcept.sort(new Util.AngleComparator(edge.getLine()));
		return !passableEdgeLinesExcept.isEmpty() ? passableEdgeLinesExcept.get(0).first() : null;
	}

	/**
	* @Description: 获取所有passableEdge-line2D
	* @Author: Guanyu-Cai
	* @Date: 3/16/20
	*/
	public List<Pair<CSUEdge, Line2D>> getPassableEdgeLines() {
		List<Pair<CSUEdge, Line2D>> result = new ArrayList<>();
		for (CSUEdge edge : csuEdges) {
			if (edge.isPassable()) {
				result.add(new Pair<>(edge, edge.getLine()));
			}
		}
		return result;
	}

	/**
	 * @Description: 获取所有passableEdge-line2d,除了和exceptEdge在同一条直线上的
	 * @Author: Guanyu-Cai
	 * @Date: 3/16/20
	 */
	public List<Pair<CSUEdge, Line2D>> getPassableEdgeLinesExcept(CSUEdge exceptEdge) {
		List<Pair<CSUEdge, Line2D>> result = new ArrayList<>();
		math.geom2d.line.Line2D exceptLine = Util.convertLine(exceptEdge.getLine());
		for (CSUEdge edge : csuEdges) {
			math.geom2d.line.Line2D line = Util.convertLine(edge.getLine());
			if (edge.isPassable() && Util.isCollinear(exceptLine, line, COLLINEAR_THRESHOLD)) {
				result.add(new Pair<>(edge, edge.getLine()));
			}
		}
		return result;
	}

	/**
	* @Description: 获取所有passableEdge-line2d,除了和exceptEdge在同一条直线上的
	* @Author: Guanyu-Cai
	* @Date: 3/16/20
	*/
	public List<Pair<CSUEdge, Line2D>> getEdgeLinesExcept(CSUEdge exceptEdge) {
		List<Pair<CSUEdge, Line2D>> result = new ArrayList<>();
		math.geom2d.line.Line2D exceptLine = Util.convertLine(exceptEdge.getLine());
		for (CSUEdge edge : csuEdges) {
			math.geom2d.line.Line2D line = Util.convertLine(edge.getLine());
			if (!Util.isCollinear(exceptLine, line, COLLINEAR_THRESHOLD)) {
				result.add(new Pair<>(edge, edge.getLine()));
			}
		}
		return result;
	}

	/**
	 * @Description: 获取和edge相对的passable edge的neighbour road
	 * @Author: Guanyu-Cai
	 * @Date: 3/13/20
	 */
	public CSURoad getOppositePassableEdgeRoad(CSUEdge edge) {
		CSUEdge oppositeEdge = getOppositePassableEdge(edge);
		EntityID id = oppositeEdge.getNeighbours().first();
		return world.getCsuRoad(id);
	}

	/**
	* @Description: 获取所有blockade的polygon
	* @Author: Guanyu-Cai
	* @Date: 3/16/20
	*/
	public List<Polygon> getBlockadePolygons() {
		List<Polygon> result = new ArrayList<>();
		for (CSUBlockade blockade : csuBlockades) {
			result.add(blockade.getPolygon());
		}
		return result;
	}

	/**
	 * @Description: 获取所有blockade的放大scale大小的polygon
	 * @Author: Guanyu-Cai
	 * @Date: 3/16/20
	 */
	public List<Polygon> getBlockadePolygons(int scale) {
		List<Polygon> result = new ArrayList<>();
		for (CSUBlockade blockade : csuBlockades) {
			Polygon polygon = Util.scaleBySize(blockade.getPolygon(), scale);
			result.add(polygon);
		}
		return result;
	}

	/* --------------------------------- the following method is only for test --------------------------------- */

	public void setCsuBlockades(List<CSUBlockade> blockades) {
		this.csuBlockades.clear();
		this.csuBlockades.addAll(blockades);
	}

	public List<CSUEdge> getCsuEdges() {
		return this.csuEdges;
	}

	public List<CSUBlockade> getCsuBlockades() {
		return this.csuBlockades;
	}

	public void setObservableAreas(List<EntityID> observableAreas) {
		this.observableAreas = observableAreas;
		if (DebugHelper.DEBUG_MODE && !world.getScenarioInfo().getRawConfig().getBooleanValue(ConfigKey.KEY_PRECOMPUTE, false)) {
			List<Integer> elementIds = Util.fetchIdValueFromElementIds(observableAreas);
			DebugHelper.VD_CLIENT.drawAsync(this.getId().getValue(), "ObservableAreas", (Serializable) elementIds);
		}
	}

	public void setLineOfSight(List<CSULineOfSightPerception.CsuRay> rays) {
		this.lineOfSight = rays;
	}

	public List<CSULineOfSightPerception.CsuRay> getLineOfSight() {
		return lineOfSight;
	}

	public Set<EntityID> getVisibleFrom() {
		return visibleFrom;
	}

	public void setVisibleFrom(Set<EntityID> visibleFrom) {
		this.visibleFrom = visibleFrom;
		if (DebugHelper.DEBUG_MODE && !world.getScenarioInfo().getRawConfig().getBooleanValue(ConfigKey.KEY_PRECOMPUTE, false)) {
			List<Integer> elementIds = Util.fetchIdValueFromElementIds(visibleFrom);
			DebugHelper.VD_CLIENT.drawAsync(this.getId().getValue(), "VisibleFromAreas", (Serializable) elementIds);
		}
	}

}
