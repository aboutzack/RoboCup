package AUR.util.knd;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import viewer.K_ScreenTransform;

/**
 *
 * @author Alireza Kandeh - 2017 & 2018
 */

public class AURAreaGrid {

	public final static int GRID_SIZE = 450;
	private final static int DEFAULT_GRID_ROWS = 351;
	private final static int DEFAULT_GRID_COLS = 503;
	private int currentSizeM = DEFAULT_GRID_ROWS;
	private int currentSizeN = DEFAULT_GRID_COLS;
	public Area area = null;
	public final int CELL_FREE = 0;
	public final int CELL_BLOCK = 1;
	public final int CELL_AREA_EDGE = 2;
	public final int CELL_NODE = 4;
	public final int CELL_OUT = 8;
	public int gridPoints[][][] = new int[DEFAULT_GRID_ROWS][DEFAULT_GRID_COLS][2];
	public int gridIntInfo[][][] = new int[DEFAULT_GRID_ROWS][DEFAULT_GRID_COLS][2];
	public int edgePoint[][] = new int[100][3];
	public AURNode edgePointObject[] = new AURNode[100];
	private int gridM = 0;
	private int gridN = 0;
	double boundsWidth = 0;
	double boundsheight = 0;
	double boundsX0 = 0;
	double boundsY0 = 0;
	double boundsX1 = 0;
	double boundsY1 = 0;
	private int getCellResult[] = new int[2];
	public int edgePointsSize = 0;
	public static final int TYPE = 0;
	public static final int COST = 1;
	public final double lineStepSize = GRID_SIZE / 4;
	private Queue<Long> que = new LinkedList<Long>();
	private ArrayList<Polygon> blockaePolygons = new ArrayList<Polygon>();
	public AURAreaGraph areaGraph = null;
	
	private HashMap<String, ArrayList<AURNode>> nodes = new HashMap<>();
	
	public final static int dij_4[][] = {
		{-1, +0},
		{+1, +0},
		{+0, +1},
		{+0, -1}
	};
	
	public final static int dij_5[][] = {
		{+0, +0},
		{+1, +0},
		{-1, +0},
		{+0, +1},
		{+0, -1}
	};

	public final static int dij_8[][] = {
		{+0, +1},
		{-1, +0},
		{+1, +0},
		{+0, -1},
		{-1, +1},
		{+1, +1},
		{-1, -1},
		{+1, -1}
	};

	public final static int[][] dij_9 = {
		{+0, +0},
		{+0, +1},
		{-1, +0},
		{+1, +0},
		{+0, -1},
		{-1, +1},
		{+1, +1},
		{-1, -1},
		{+1, -1}
	};
	
	private void checkGridArraySize(int m, int n) {
		if (currentSizeM >= m && currentSizeN >= n) {
			return;
		}
		currentSizeM = Math.max(currentSizeM, m + 2);
		currentSizeN = Math.max(currentSizeN, n + 2);
		gridPoints = new int[currentSizeM][currentSizeN][2];
		gridIntInfo = new int[currentSizeM][currentSizeN][2];
		System.out.println("grid array size changed: " + currentSizeM + ", " + currentSizeN);
	}

//	public Point2D getPointHasSight(AURAreaGraph areaGraph, AURAreaInSightChecker checker, double fromX, double fromY) {
//		this.areaGraph = areaGraph;
//		this.areaPolygon = (Polygon) (areaGraph.area.getShape());
//		edgePointsSize = 0;
//		blockaePolygons.clear();
//
//		this.area = areaGraph.area;
//
//		initGrid();
//		addAreaBlockades(this.areaGraph);
//
//		int ij[] = getCell(fromX, fromY);
//		Point2D result = null;
//		if (ij[0] < 0) {
//			return result;
//		}
//		int i, j;
//		int ip, jp;
//		i = ij[0];
//		j = ij[1];
//		que.clear();
//
//		for (int ii = 0; ii < gridM; ii++) {
//			for (int jj = 0; jj < gridN; jj++) {
//				gridIntInfo[ii][jj][COST] = 0;
//			}
//		}
//		for (int d = 0; d < 9; d++) {
//			ip = i + dij_9[d][0];
//			jp = j + dij_9[d][1];
//
//			if (inside(ip, jp) && gridIntInfo[ip][jp][TYPE] != CELL_NODE) {
//				gridIntInfo[ip][jp][TYPE] = CELL_FREE;
//			}
//
//		}
//
//		gridIntInfo[i][j][COST] = 1;
//
//		que.add(ijToInt(i, j));
//		long heap_top = 0;
//
//		while (que.isEmpty() == false) {
//			heap_top = que.poll();
//			intToIj(heap_top, ij);
//			i = ij[0];
//			j = ij[1];
//
//			if (checker.query(gridPoints[i][j][0], gridPoints[i][j][1]) == true) {
//				result = new Point2D(gridPoints[i][j][0], gridPoints[i][j][1]);
//				return result;
//			}
//
//			for (int d = 0; d < 8; d++) {
//				ip = i + dij_8[d][0];
//				jp = j + dij_8[d][1];
//				if (false || (inside(ip, jp) == false) || gridIntInfo[ip][jp][COST] > 0
//						|| gridIntInfo[ip][jp][TYPE] == CELL_BLOCK || gridIntInfo[ip][jp][TYPE] == CELL_OUT) {
//					continue;
//				}
//				gridIntInfo[ip][jp][COST] = gridIntInfo[i][j][COST] + 1;
//				que.add(ijToInt(ip, jp));
//			}
//		}
//
//		return result;
//	}

	
//	public Point2D getPerceptiblePoint(AURBuilding building, double fromX, double fromY) {
//		this.areaGraph = building.ag;
//		this.areaPolygon = this.areaGraph.polygon;
//		edgePointsSize = 0;
//		blockaePolygons.clear();
//
//		this.area = areaGraph.area;
//
//		initGrid();
//		addAreaBlockades(this.areaGraph);
//
//		int ij[] = getCell(fromX, fromY);
//		Point2D result = null;
//		if (ij[0] < 0) {
//			return result;
//		}
//		int i, j;
//		int ip, jp;
//		i = ij[0];
//		j = ij[1];
//		que.clear();
//
//		for (int ii = 0; ii < gridM; ii++) {
//			for (int jj = 0; jj < gridN; jj++) {
//				gridIntInfo[ii][jj][COST] = 0;
//			}
//		}
//		for (int d = 0; d < 9; d++) {
//			ip = i + dij_9[d][0];
//			jp = j + dij_9[d][1];
//
//			if (inside(ip, jp) && gridIntInfo[ip][jp][TYPE] != CELL_NODE) {
//				gridIntInfo[ip][jp][TYPE] = CELL_FREE;
//			}
//
//		}
//
//		gridIntInfo[i][j][COST] = 1;
//
//		que.add(ijToInt(i, j));
//		long heap_top = 0;
//
//		while (que.isEmpty() == false) {
//			heap_top = que.poll();
//			intToIj(heap_top, ij);
//			i = ij[0];
//			j = ij[1];
//
//			if (checker.query(gridPoints[i][j][0], gridPoints[i][j][1]) == true) {
//				result = new Point2D(gridPoints[i][j][0], gridPoints[i][j][1]);
//				return result;
//			}
//
//			for (int d = 0; d < 8; d++) {
//				ip = i + dij_8[d][0];
//				jp = j + dij_8[d][1];
//				if (false || (inside(ip, jp) == false) || gridIntInfo[ip][jp][COST] > 0
//					|| gridIntInfo[ip][jp][TYPE] == CELL_BLOCK || gridIntInfo[ip][jp][TYPE] == CELL_OUT) {
//					continue;
//				}
//				gridIntInfo[ip][jp][COST] = gridIntInfo[i][j][COST] + 1;
//				que.add(ijToInt(ip, jp));
//			}
//		}
//
//		return result;
//	}

	public Point2D getPointInRange(AURAreaGraph areaGraph, double rcx, double rcy, double r, double fromX,
			double fromY) {
		this.areaGraph = areaGraph;
		edgePointsSize = 0;
		blockaePolygons.clear();
		this.area = areaGraph.area;

		Point2D result = null;
		AURRange range = new AURRange(rcx, rcy, r);

		initGrid();
		
		addAreaBlockades(this.areaGraph);

		for (int i = 0; i < gridM; i++) {
			for (int j = 0; j < gridN; j++) {
				if (gridIntInfo[i][j][TYPE] == CELL_FREE) {
					if (range.contains(gridPoints[i][j][0], gridPoints[i][j][1])) {
						gridIntInfo[i][j][TYPE] = CELL_NODE;
					}
				}
			}
		}

		int ij[] = getCell(fromX, fromY);

		if (ij[0] < 0) {
			return result;
		}
		int i, j;
		int ip, jp;
		i = ij[0];
		j = ij[1];
		que.clear();

		for (int ii = 0; ii < gridM; ii++) {
			for (int jj = 0; jj < gridN; jj++) {
				gridIntInfo[ii][jj][COST] = -1;
			}
		}
		for (int d = 0; d < 9; d++) {
			ip = i + dij_9[d][0];
			jp = j + dij_9[d][1];

			if (insideGrid(ip, jp) && gridIntInfo[ip][jp][TYPE] != CELL_NODE) {
				gridIntInfo[ip][jp][TYPE] = CELL_FREE;
			}

		}

		gridIntInfo[i][j][COST] = 0;

		que.add(IJToLong(i, j));
		long heap_top = 0;

		while (que.isEmpty() == false) {
			heap_top = que.poll();
			longToIJ(heap_top, ij);
			i = ij[0];
			j = ij[1];

			if (gridIntInfo[i][j][0] == CELL_NODE) {
				result = new Point2D(gridPoints[i][j][0], gridPoints[i][j][1]);
				return result;
			}

			for (int d = 0; d < 8; d++) {
				ip = i + dij_8[d][0];
				jp = j + dij_8[d][1];
				if (false || (insideGrid(ip, jp) == false) || gridIntInfo[ip][jp][COST] > -1
						|| gridIntInfo[ip][jp][TYPE] == CELL_BLOCK || gridIntInfo[ip][jp][TYPE] == CELL_OUT) {
					continue;
				}
				gridIntInfo[ip][jp][COST] = gridIntInfo[i][j][COST] + 1;
				que.add(IJToLong(ip, jp));
			}
		}

		return result;
	}

	public int[] getCell(double x, double y) {
		int i = (int) (Math.floor((y - gridPoints[0][0][1] + GRID_SIZE / 2) / GRID_SIZE));
		int j = (int) (Math.floor((x - gridPoints[0][0][0] + GRID_SIZE / 2) / GRID_SIZE));
		getCellResult[0] = -1;
		if ((i < 0 || i >= gridM || j < 0 || j >= gridN) == false) {
			getCellResult[0] = i;
			getCellResult[1] = j;
		}
		return getCellResult;
	}

	class AuraBound {

		double minX;
		double minY;
		double maxX;
		double maxY;

		public AuraBound(Polygon polygon) {
			minX = Double.MAX_VALUE;
			minY = Double.MAX_VALUE;
			maxX = Double.MIN_VALUE;
			maxY = Double.MIN_VALUE;
			for (int i = 0; i < polygon.npoints; i++) {
				minX = Math.min(minX, polygon.xpoints[i]);
				minY = Math.min(minY, polygon.ypoints[i]);
				maxX = Math.max(maxX, polygon.xpoints[i]);
				maxY = Math.max(maxY, polygon.ypoints[i]);
			}
		}

	}
	
	public String getKey(int i, int j) {
		return i + ", " + j;
	}

	public void initGrid() {
		AuraBound bounds = new AuraBound(this.areaGraph.polygon);

		boundsX0 = bounds.minX;
		boundsY0 = bounds.minY;
		boundsX1 = bounds.maxX;
		boundsY1 = bounds.maxY;

		int boundJ0 = (int) (Math.floor((boundsX0 - 0 + GRID_SIZE / 2) / GRID_SIZE)) - 1;
		int boundI0 = (int) (Math.floor((boundsY0 - 0 + GRID_SIZE / 2) / GRID_SIZE)) - 1;
		int boundJ1 = (int) (Math.ceil((boundsX1 - 0 + GRID_SIZE / 2) / GRID_SIZE)) + 1;
		int boundI1 = (int) (Math.ceil((boundsY1 - 0 + GRID_SIZE / 2) / GRID_SIZE)) + 1;

		gridM = boundI1 - boundI0 + 0;
		gridN = boundJ1 - boundJ0 + 0;
		checkGridArraySize(gridM, gridN);

		int oX = boundJ0 * GRID_SIZE;
		int oY = boundI0 * GRID_SIZE;

		int cx, cy;
		for (int i = 0; i < gridM; i++) {
			for (int j = 0; j < gridN; j++) {
				cx = j * GRID_SIZE + oX;
				cy = i * GRID_SIZE + oY;
				gridPoints[i][j][0] = cx;
				gridPoints[i][j][1] = cy;
				gridIntInfo[i][j][TYPE] = CELL_FREE;
//				gridIntInfo[i][j][EDGE_POINT_ID] = -1;

			}
		}
		
		nodes.clear();
		
		for (int i = 0; i < gridM; i++) {
			for (int j = 0; j < gridN; j++) {
				if (gridIntInfo[i][j][TYPE] == CELL_FREE) {
					if (this.areaGraph.polygon.contains(gridPoints[i][j][0], gridPoints[i][j][1]) == false) {
						gridIntInfo[i][j][TYPE] = CELL_OUT; // #toDo
					}
				}
			}
		}
	}

	public void addAreaBlockades(AURAreaGraph ag) {
		addBlockades(ag.getAliveBlockades());
	}

	public void init(AURAreaGraph areaGraph) {
		this.areaGraph = areaGraph;
		edgePointsSize = 0;
		blockaePolygons.clear();
		this.area = areaGraph.area;

		initGrid();
		
//		if(areaGraph.isBuilding()) {
//			System.out.println("AUR.util.knd.AURAreaGrid.init()");
//		}

		for (AURAreaGraph ag : areaGraph.neighbours) {
			for (AURBorder border : ag.borders) {
				markLine(border.Ax, border.Ay, border.Bx, border.By, CELL_AREA_EDGE);
			}
		}

//		for(Edge edge : this.areaGraph.area.getEdges()) {
//			if(edge.isPassable() == false) {
//				markLine(edge.getStartX(), edge.getStartY(), edge.getEndX(), edge.getEndY(), CELL_BLOCK);
//			}
//		}
//		
//		for (AURAreaGraph ag : areaGraph.neighbours) {
//			for(Edge edge : ag.area.getEdges()) {
//				if(edge.isPassable() == false) {
//					markLine(edge.getStartX(), edge.getStartY(), edge.getEndX(), edge.getEndY(), CELL_BLOCK);
//				}
//			}
//		}
		
		for (AURBorder border : areaGraph.borders) {
			markLine(border.Ax, border.Ay, border.Bx, border.By, CELL_AREA_EDGE);
		}

		addBlockades(areaGraph.getAliveBlockades());

		for (AURAreaGraph ag : areaGraph.neighbours) {
			addBlockades(ag.getAliveBlockades());
		}
	}

	public void addBlockades(ArrayList<Polygon> blockades) {
		blockaePolygons.addAll(blockades);
		double delta = 450;
		for (int i = 0; i < gridM; i++) {
			for (int j = 0; j < gridN; j++) {
				if (gridIntInfo[i][j][TYPE] != CELL_BLOCK) {
					for (Polygon p : blockaePolygons) {
						if (p.contains(gridPoints[i][j][0], gridPoints[i][j][1]) || p.intersects(
								gridPoints[i][j][0] - delta, gridPoints[i][j][1] - delta, delta * 2, delta * 2)) {
							gridIntInfo[i][j][TYPE] = CELL_BLOCK;
							break;
						}
					}
				}

			}
		}
	}

	public ArrayList<AURNode> getReachableEdgeNodesFrom(AURAreaGraph ag, double x, double y) {
		ArrayList<AURNode> result = new ArrayList<>();
		init(ag);
		edgePointsSize = 0;
		for (int i = 0; i < edgePoint.length; i++) {
			edgePoint[i][0] = -1;
			edgePointObject[i] = null;
		}
		for (AURBorder border : areaGraph.borders) {
			for (AURNode node : border.nodes) {
				markEdgeCenters(node, node.x, node.y, CELL_NODE);
			}
		}
		int ij[] = getCell(x, y);
		if (ij[0] < 0) {
			return result;
		}
		int i, j;
		int ip, jp;
		i = ij[0];
		j = ij[1];
		for (int d = 0; d < 9; d++) {
			ip = i + dij_9[d][0];
			jp = j + dij_9[d][1];
			if (insideGrid(ip, jp) && gridIntInfo[ip][jp][TYPE] != CELL_NODE) {
				gridIntInfo[ip][jp][TYPE] = CELL_FREE;
			}
		}
		ij = new int[2];
		que.clear();
		for (int ii = 0; ii < gridM; ii++) {
			for (int jj = 0; jj < gridN; jj++) {
				gridIntInfo[ii][jj][COST] = -1;
			}
		}
		gridIntInfo[i][j][COST] = 0;
		que.add(IJToLong(i, j));
		long heap_top = 0;

		while (que.isEmpty() == false) {
			heap_top = que.poll();
			longToIJ(heap_top, ij);
			i = ij[0];
			j = ij[1];

			if (gridIntInfo[i][j][0] == CELL_NODE) {
				
				ArrayList<AURNode> arr = nodes.get(getKey(i, j));
				for(AURNode node : arr) {
					node.cost = (int) AURGeoUtil.dist(x, y, gridPoints[i][j][0], gridPoints[i][j][1]);
					result.add(node);
				}
			}

			for (int d = 0; d < 9; d++) {
				ip = i + dij_9[d][0];
				jp = j + dij_9[d][1];
				if (false || (insideGrid(ip, jp) == false) || gridIntInfo[ip][jp][COST] > -1
						|| gridIntInfo[ip][jp][TYPE] == CELL_BLOCK || gridIntInfo[ip][jp][TYPE] == CELL_OUT) {
					continue;
				}
				gridIntInfo[ip][jp][COST] = gridIntInfo[i][j][COST] + 1;
				que.add(IJToLong(ip, jp));
			}
		}
		return result;
	}
	
	// very very bad version
	public void optimizeStandPoints(AURNode fromNode) {
		if(fromNode == null) {
			return;
		}
		ArrayList<AUREdgeToStand> edgesToPercept = fromNode.edgesToPerceptAndExtinguish;
		ArrayList<AUREdgeToStand> edgesToSeeInside = fromNode.edgesToSeeInside;
		
		if(edgesToPercept != null) {
			for (int ii = 0; ii < edgesToPercept.size(); ii++) {
				AUREdgeToStand iiE = edgesToPercept.get(ii);
				for (int jj = ii + 1; jj < edgesToPercept.size(); jj++) {
					AUREdgeToStand jjE = edgesToPercept.get(jj);
					Polygon iiP = iiE.toSeeAreaGraph.getBuilding().getPerceptibleAndExtinguishableAreaPolygon();
					if (iiE.fromNode == jjE.fromNode && iiE.ownerAg == jjE.ownerAg) {

						if (iiP.contains(jjE.standX, jjE.standY)) {
							iiE.standX = jjE.standX;
							iiE.standY = jjE.standY;
							iiE.weight = jjE.weight;
						} else {
							Polygon jjP = jjE.toSeeAreaGraph.getBuilding().getPerceptibleAndExtinguishableAreaPolygon();
							if (jjP.contains(iiE.standX, iiE.standY)) {
								jjE.standX = iiE.standX;
								jjE.standY = iiE.standY;
								jjE.weight = iiE.weight;
							}
						}
					}
				}
			}
		}
		
		if(edgesToSeeInside != null) {
			for (int ii = 0; ii < edgesToSeeInside.size(); ii++) {
				AUREdgeToStand iiE = edgesToSeeInside.get(ii);
				for (int jj = ii + 1; jj < edgesToSeeInside.size(); jj++) {
					AUREdgeToStand jjE = edgesToSeeInside.get(jj);
					Polygon iiP = iiE.toSeeAreaGraph.getBuilding().getSightAreaPolygon();
					if (iiE.fromNode == jjE.fromNode && iiE.ownerAg == jjE.ownerAg) {

						if (iiP.contains(jjE.standX, jjE.standY)) {
							iiE.standX = jjE.standX;
							iiE.standY = jjE.standY;
							iiE.weight = jjE.weight;
						} else {
							Polygon jjP = jjE.toSeeAreaGraph.getBuilding().getSightAreaPolygon();
							if (jjP.contains(iiE.standX, iiE.standY)) {
								jjE.standX = iiE.standX;
								jjE.standY = iiE.standY;
								jjE.weight = iiE.weight;
							}
						}
					}
				}
			}
		}

	}

	public ArrayList<AUREdgeToStand> getEdgesToPerceptiblePolygons(AURAreaGraph ag, int fromX, int fromY) {
		AURNode fromNode = new AURNode(fromX, fromY, ag, ag);
		ArrayList<AUREdgeToStand> result = new ArrayList<>();
		if(ag.perceptibleAndExtinguishableBuildings == null || ag.perceptibleAndExtinguishableBuildings.size() <= 0) {
			return result;
		}
		
		this.areaGraph = ag;
		edgePointsSize = 0;
		blockaePolygons.clear();

		this.area = areaGraph.area;

		initGrid();
		addAreaBlockades(this.areaGraph);

		int ij[] = getCell(fromX, fromY);
		if (ij[0] < 0) {
			return result;
		}
		int i, j;
		int ip, jp;
		i = ij[0];
		j = ij[1];
		que.clear();

		for (int ii = 0; ii < gridM; ii++) {
			for (int jj = 0; jj < gridN; jj++) {
				gridIntInfo[ii][jj][COST] = -1;
			}
		}
		for (int d = 0; d < 9; d++) {
			ip = i + dij_9[d][0];
			jp = j + dij_9[d][1];

			if (insideGrid(ip, jp) && gridIntInfo[ip][jp][TYPE] != CELL_NODE) {
				gridIntInfo[ip][jp][TYPE] = CELL_FREE;
			}

		}

		gridIntInfo[i][j][COST] = 0;

		que.add(IJToLong(i, j));
		long heap_top = 0;

		ArrayList<AURBuilding> perceptibleAreas = null;
		ArrayList<AURBuilding> remove = null;
		if(areaGraph.perceptibleAndExtinguishableBuildings != null) {
			perceptibleAreas = new ArrayList<>();
			perceptibleAreas.addAll(areaGraph.perceptibleAndExtinguishableBuildings);
			remove = new ArrayList<>();
		}
		
		while (que.isEmpty() == false) {
			heap_top = que.poll();
			longToIJ(heap_top, ij);
			i = ij[0];
			j = ij[1];


			if(areaGraph.perceptibleAndExtinguishableBuildings != null) {
				int r = 500;
				if(this.areaGraph.polygon.contains(gridPoints[i][j][0] - r, gridPoints[i][j][1] - r, 2 * r, 2 * r) == true) {
					for(AURBuilding b : perceptibleAreas) {
						if(b.getPerceptibleAndExtinguishableAreaPolygon().contains((int) gridPoints[i][j][0], (int) gridPoints[i][j][1])) {
							int cost = (int) AURGeoUtil.dist(fromX, fromY, gridPoints[i][j][0], gridPoints[i][j][1]);
							AUREdgeToStand toSeeEdge = new AUREdgeToStand(this.areaGraph, b.ag, cost, fromNode, gridPoints[i][j][0], gridPoints[i][j][1]);
							if(fromNode.edgesToPerceptAndExtinguish == null) {
								fromNode.edgesToPerceptAndExtinguish = new ArrayList<>();
							}
							remove.add(b);
							fromNode.edgesToPerceptAndExtinguish.add(toSeeEdge);
						}
					}
					perceptibleAreas.removeAll(remove);
				}
			}
			
			for (int d = 0; d < 8; d++) {
				ip = i + dij_8[d][0];
				jp = j + dij_8[d][1];
				if (false
					|| (insideGrid(ip, jp) == false)
					|| gridIntInfo[ip][jp][COST] > -1
					|| gridIntInfo[ip][jp][TYPE] == CELL_BLOCK
					|| gridIntInfo[ip][jp][TYPE] == CELL_OUT
				) {
					continue;
				}
				
				gridIntInfo[ip][jp][COST] = gridIntInfo[i][j][COST] + 1;
				que.add(IJToLong(ip, jp));
			}
		}

		
		if(fromNode.edgesToPerceptAndExtinguish != null) {
			result.addAll(fromNode.edgesToPerceptAndExtinguish);
		}
		optimizeStandPoints(fromNode);
		return result;
	}
	
	public ArrayList<AUREdgeToStand> getEdgesToSightPolygon(AURAreaGraph ag, int fromX, int fromY) {
		AURNode fromNode = new AURNode(fromX, fromY, ag, ag);
		ArrayList<AUREdgeToStand> result = new ArrayList<>();
		if(ag.sightableBuildings == null || ag.sightableBuildings.size() <= 0) {
			return result;
		}
		
		this.areaGraph = ag;
		edgePointsSize = 0;
		blockaePolygons.clear();

		this.area = areaGraph.area;

		initGrid();
		addAreaBlockades(this.areaGraph);

		int ij[] = getCell(fromX, fromY);
		if (ij[0] < 0) {
			return result;
		}
		int i, j;
		int ip, jp;
		i = ij[0];
		j = ij[1];
		que.clear();

		for (int ii = 0; ii < gridM; ii++) {
			for (int jj = 0; jj < gridN; jj++) {
				gridIntInfo[ii][jj][COST] = -1;
			}
		}
		for (int d = 0; d < 9; d++) {
			ip = i + dij_9[d][0];
			jp = j + dij_9[d][1];

			if (insideGrid(ip, jp) && gridIntInfo[ip][jp][TYPE] != CELL_NODE) {
				gridIntInfo[ip][jp][TYPE] = CELL_FREE;
			}

		}

		gridIntInfo[i][j][COST] = 0;

		que.add(IJToLong(i, j));
		long heap_top = 0;

		ArrayList<AURBuilding> sightableAreas = null;
		ArrayList<AURBuilding> remove_ = null;
		if(areaGraph.sightableBuildings != null) {
			sightableAreas = new ArrayList<>();
			sightableAreas.addAll(areaGraph.sightableBuildings);
			remove_ = new ArrayList<AURBuilding>();
		}
		
		
		while (que.isEmpty() == false) {
			heap_top = que.poll();
			longToIJ(heap_top, ij);
			i = ij[0];
			j = ij[1];

			if(areaGraph.sightableBuildings != null) {
				int r = 750;
				if(this.areaGraph.polygon.contains(gridPoints[i][j][0] - r, gridPoints[i][j][1] - r, 2* r, 2 * r) == true) {
					for(AURBuilding b : sightableAreas) {
						if(b.getSightAreaPolygon().contains((int) gridPoints[i][j][0], (int) gridPoints[i][j][1])) {
							int cost = (int) AURGeoUtil.dist(fromX, fromY, gridPoints[i][j][0], gridPoints[i][j][1]);
							AUREdgeToStand toSeeEdge = new AUREdgeToStand(this.areaGraph, b.ag, cost, fromNode, gridPoints[i][j][0], gridPoints[i][j][1]);
							if(fromNode.edgesToSeeInside == null) {
								fromNode.edgesToSeeInside = new ArrayList<>();
							}
							remove_.add(b);
							fromNode.edgesToSeeInside.add(toSeeEdge);
						}
					}
					sightableAreas.removeAll(remove_);
				}
			}
			
			for (int d = 0; d < 8; d++) {
				ip = i + dij_8[d][0];
				jp = j + dij_8[d][1];
				if (false
					|| (insideGrid(ip, jp) == false)
					|| gridIntInfo[ip][jp][COST] > -1
					|| gridIntInfo[ip][jp][TYPE] == CELL_BLOCK
					|| gridIntInfo[ip][jp][TYPE] == CELL_OUT
				) {
					continue;
				}
				gridIntInfo[ip][jp][COST] = gridIntInfo[i][j][COST] + 1;
				que.add(IJToLong(ip, jp));
			}
		}

		
		if(fromNode.edgesToSeeInside != null) {
			result.addAll(fromNode.edgesToSeeInside);
		}
		optimizeStandPoints(fromNode);
		return result;
	}
	
	public void setEdgePointsAndCreateGraph() {

		edgePointsSize = 0;
		for (int i = 0; i < edgePoint.length; i++) {
			edgePoint[i][0] = -1;
			edgePointObject[i] = null;
		}
		for (AURBorder border : areaGraph.borders) {
			if (border.calced == false) {
				markEdgeOpenCenters(border, border.Ax, border.Ay, border.Bx, border.By);
				border.calced = true;
			} else {
				for (AURNode node : border.nodes) {
					markEdgeCenters(node, node.x, node.y, CELL_NODE);
				}
				border.ready = true;
			}
		}
		ArrayList<AUREdge> delEdges = new ArrayList<>();
		for (AURBorder border : areaGraph.borders) {
			for (AURNode node : border.nodes) {
				for (AUREdge edge : node.edges) {
					if (edge.areaGraph.area.getID().equals(area.getID())) {
						delEdges.add(edge);
					}
				}
				node.edges.removeAll(delEdges);
			}
		}
		
		boolean b = (this.areaGraph.perceptibleAndExtinguishableBuildings != null && this.areaGraph.perceptibleAndExtinguishableBuildings.size() > 0);
		b = b || (this.areaGraph.sightableBuildings != null && this.areaGraph.sightableBuildings.size() > 0);
		b = b || (this.blockaePolygons != null && this.blockaePolygons.size() > 0);
		if (b) {
			
			//long t = System.currentTimeMillis();
			
			for (int i = 0; i < edgePointsSize; i++) {
				bfs(i);
			}
			
			//System.out.println(System.currentTimeMillis() - t);
			
		} else {
			int cost = 0;
			AURNode iNode;
			AURNode jNode;
			AUREdge edge = null;
			for (int i = 0; i < edgePointsSize; i++) {
				for (int j = i + 1; j < edgePointsSize; j++) {
					iNode = edgePointObject[i];
					jNode = edgePointObject[j];
					cost = (int) AURGeoUtil.dist(iNode.x, iNode.y, jNode.x, jNode.y);
					edge = new AUREdge(iNode, jNode, cost, areaGraph);
					iNode.edges.add(edge);
					jNode.edges.add(edge);
				}
			}
		}
	}

	public void bfs(int from) {
		que.clear();
		AURNode fromNode;
		fromNode = edgePointObject[from];
		int i, j;
		int ip, jp;
		int ij[] = new int[2];
		for (int ii = 0; ii < gridM; ii++) {
			for (int jj = 0; jj < gridN; jj++) {
				gridIntInfo[ii][jj][COST] = -1;
			}
		}

		i = edgePoint[from][0];
		j = edgePoint[from][1];
		gridIntInfo[i][j][COST] = 0;
		que.add(IJToLong(i, j));
		long heap_top = 0;
		AUREdge edge = null;
		
		ArrayList<AURBuilding> perceptibleAreas = null;
		ArrayList<AURBuilding> remove = null;
		if(areaGraph.perceptibleAndExtinguishableBuildings != null) {
			perceptibleAreas = new ArrayList<>();
			perceptibleAreas.addAll(areaGraph.perceptibleAndExtinguishableBuildings);
			remove = new ArrayList<AURBuilding>();
			
		}
		
		ArrayList<AURBuilding> sightableAreas = null;
		ArrayList<AURBuilding> remove_ = null;
		if(areaGraph.sightableBuildings != null) {
			sightableAreas = new ArrayList<>();
			sightableAreas.addAll(areaGraph.sightableBuildings);
			remove_ = new ArrayList<AURBuilding>();
		}
		
		while (que.isEmpty() == false) {
			heap_top = que.poll();
			longToIJ(heap_top, ij);
			i = ij[0];
			j = ij[1];
			
			
			
			
			if(false|| (perceptibleAreas != null && perceptibleAreas.size() > 0)
				|| (sightableAreas != null && sightableAreas.size() > 0)) {
				int r = 750;
				if(this.areaGraph.polygon.contains(gridPoints[i][j][0] - r, gridPoints[i][j][1] - r, 2 * r, 2 * r) == true) {
					
					if(areaGraph.perceptibleAndExtinguishableBuildings != null) {
						for(AURBuilding b : perceptibleAreas) {
							if(b.getPerceptibleAndExtinguishableAreaPolygon().contains((int) gridPoints[i][j][0], (int) gridPoints[i][j][1])) {
								int cost = (int) AURGeoUtil.dist(fromNode.x, fromNode.y, gridPoints[i][j][0], gridPoints[i][j][1]);
								AUREdgeToStand etp = new AUREdgeToStand(this.areaGraph, b.ag, cost, fromNode, gridPoints[i][j][0], gridPoints[i][j][1]);
								if(fromNode.edgesToPerceptAndExtinguish == null) {
									fromNode.edgesToPerceptAndExtinguish = new ArrayList<>();
								}
								remove.add(b);
								fromNode.edgesToPerceptAndExtinguish.add(etp);
							}
						}
						perceptibleAreas.removeAll(remove);
					}
					
					if(areaGraph.sightableBuildings != null) {
						for(AURBuilding b : sightableAreas) {
							if(b.getSightAreaPolygon().contains((int) gridPoints[i][j][0], (int) gridPoints[i][j][1])) {
								int cost = (int) AURGeoUtil.dist(fromNode.x, fromNode.y, gridPoints[i][j][0], gridPoints[i][j][1]);
								AUREdgeToStand ets = new AUREdgeToStand(this.areaGraph, b.ag, cost, fromNode, gridPoints[i][j][0], gridPoints[i][j][1]);
								if(fromNode.edgesToSeeInside == null) {
									fromNode.edgesToSeeInside = new ArrayList<>();
								}
								remove_.add(b);
								fromNode.edgesToSeeInside.add(ets);
							}
						}
						sightableAreas.removeAll(remove_);
					}
				
				}
			}
			

			if (gridIntInfo[i][j][0] == CELL_NODE) {
				
				ArrayList<AURNode> arr = nodes.get(getKey(i, j));
				
				for(AURNode toNode : arr) {
					if(toNode == fromNode) {
						continue;
					}
					if (true) {
						int cost = (int) AURGeoUtil.dist(fromNode.x, fromNode.y, gridPoints[i][j][0], gridPoints[i][j][1]);
						edge = new AUREdge(fromNode, toNode, cost, areaGraph);
						fromNode.edges.add(edge);
						toNode.edges.add(edge);						
					}				
				}
			}

			for (int d = 0; d < 9; d++) {
				ip = i + dij_9[d][0];
				jp = j + dij_9[d][1];
				if (false
					|| (insideGrid(ip, jp) == false)
					|| gridIntInfo[ip][jp][COST] > -1
					|| gridIntInfo[ip][jp][TYPE] == CELL_BLOCK
					|| gridIntInfo[ip][jp][TYPE] == CELL_OUT
				) {
					continue;
				}
				
				gridIntInfo[ip][jp][COST] = gridIntInfo[i][j][COST] + 1;
				que.add(IJToLong(ip, jp));
			}
		}
		optimizeStandPoints(fromNode);
	}

	public static long IJToLong(int i, int j) {
		return (((long) i) << 32) | (j & 0xFFFFFFFFL);
	}

	public static void longToIJ(long long_, int result[]) {
		result[0] = (int) (long_ >> 32);
		result[1] = (int) (long_);
	}

	public void markLine(double x0, double y0, double x1, double y1, int type) {
		double dx = x1 - x0;
		double dy = y1 - y0;
		double m;
		double t;
		if (dx * dx + dy * dy < 1) {
			return;
		}
		int[] res;
		res = getCell(x1, y1);
		if (res[0] >= 0) {
			x1 = res[1] * GRID_SIZE + gridPoints[0][0][0];
			y1 = res[0] * GRID_SIZE + gridPoints[0][0][1];
		}
		res = getCell(x0, y0);
		if (res[0] >= 0) {
			x0 = res[1] * GRID_SIZE + gridPoints[0][0][0];
			y0 = res[0] * GRID_SIZE + gridPoints[0][0][1];
		}		
		dx = x1 - x0;
		dy = y1 - y0;
		if (Math.abs(dx) >= Math.abs(dy)) {
			if (dx < 0) {
				t = x0;
				x0 = x1;
				x1 = t;
				t = y0;
				y0 = y1;
				y1 = t;
			}
			
			m = dy / dx;
			t = x0;
			double g = 0;
			double finish = x1;
			while (g + x0 < finish) {
				res = getCell(g + x0, y0 + g * m);
				if (res[0] != -1 && gridIntInfo[res[0]][res[1]][TYPE] != CELL_BLOCK) {
					markCell(res, type);
				}
				g += lineStepSize;
			}
		} else {
			if (dy < 0) {
				t = x0;
				x0 = x1;
				x1 = t;
				t = y0;
				y0 = y1;
				y1 = t;
			}
			
			m = dx / dy;
			t = y0;
			double g = 0;
			double finish = y1;
			while (g + y0 < finish) {
				res = getCell(x0 + g * m, y0 + g);
				if (res[0] != -1 && gridIntInfo[res[0]][res[1]][TYPE] != CELL_BLOCK) {
					markCell(res, type);
				}
				g += lineStepSize;
			}
		}
	}

	public void markEdgeCenters(AURBorder border, double x0, double y0, double x1, double y1, int type) {
		double dist = AURGeoUtil.dist(x0, y0, x1, y1);
		if (dist <= 0.5 * GRID_SIZE) {
			return;
		}
		int res[] = getCell((x0 + x1) / 2, (y0 + y1) / 2);
		if (res[0] != -1) { // && (gridIntInfo[res[0]][res[1]][TYPE] == CELL_AREA_EDGE || gridIntInfo[res[0]][res[1]][TYPE] == CELL_NODE)
			edgePoint[edgePointsSize][0] = res[0];
			edgePoint[edgePointsSize][1] = res[1];
			markCell(res, type);
			double cx = res[1] * GRID_SIZE + gridPoints[0][0][0];
			double cy = res[0] * GRID_SIZE + gridPoints[0][0][1];
			edgePointObject[edgePointsSize] = new AURNode((int) cx, (int) cy, border.area1, border.area2);
			
			ArrayList<AURNode> arr = nodes.get(getKey(res[0], res[1]));
			
			if(arr == null) {
				arr = new ArrayList<>();
				nodes.put(getKey(res[0], res[1]), arr);
			}
			
			arr.add(edgePointObject[edgePointsSize]);
			
			border.nodes.add(edgePointObject[edgePointsSize]);
			edgePointsSize++;
		}
	}

	public void markEdgeCenters(AURNode node, double x, double y, int type) {
		int res[] = getCell(x, y);
		if (res[0] != -1) {
			edgePoint[edgePointsSize][0] = res[0];
			edgePoint[edgePointsSize][1] = res[1];
			ArrayList<AURNode> arr = nodes.get(getKey(res[0], res[1]));
			
			if(arr == null) {
				arr = new ArrayList<>();
				nodes.put(getKey(res[0], res[1]), arr);
			}
			
			arr.add(node);
			markCell(res, type);
			edgePointObject[edgePointsSize] = node;
			edgePointsSize++;
		}
	}

	public void setCenter(int Ai, int Aj, int Bi, int Bj, byte type) {
		markCell((Ai + Bi) / 2, (Aj + Bj) / 2, type);
	}
	
	public void markEdgeOpenCenters(AURBorder border, double x0, double y0, double x1, double y1) {
		
		if(this.blockaePolygons == null || this.blockaePolygons.size() <= 0) {
			
			markEdgeCenters(border, x0, y0, x1, y1, CELL_NODE);
				
			return;
		}
		
		double dx = x1 - x0;
		double dy = y1 - y0;
		double m;
		double t;
		double last_x = -1;
		double last_y = -1;
		double start_y = -1;
		double start_x = -1;
		if (dx * dx + dy * dy < 1) {
			return;
		}
		int[] res;
		res = getCell(x1, y1);
		if (res[0] >= 0) {
			x1 = res[1] * GRID_SIZE + gridPoints[0][0][0];
			y1 = res[0] * GRID_SIZE + gridPoints[0][0][1];
		}
		res = getCell(x0, y0);
		if (res[0] >= 0) {
			x0 = res[1] * GRID_SIZE + gridPoints[0][0][0];
			y0 = res[0] * GRID_SIZE + gridPoints[0][0][1];
		}
		dx = x1 - x0;
		dy = y1 - y0;
		boolean last_valid = false;
		boolean cur_valid = false;
		if (Math.abs(dx) >= Math.abs(dy)) {
			if (dx < 0) {
				t = x0;
				x0 = x1;
				x1 = t;
				t = y0;
				y0 = y1;
				y1 = t;
			}
			m = dy / dx;
			t = x0;
			double g = 0;
			start_x = -1;
			start_y = -1;
			double finish = x1;
			while (g + x0 < finish) {
				res = getCell(g + x0, y0 + g * m);
				int i, j;
				i = res[0];
				j = res[1];
				cur_valid = true;
				if (i == -1 || (gridIntInfo[i][j][TYPE] != CELL_AREA_EDGE && gridIntInfo[i][j][TYPE] != CELL_NODE)) {
					cur_valid = false;
				}
				if (!last_valid && cur_valid) {
					start_x = g + x0;
					start_y = y0 + g * m;
				}
				if (last_valid && !cur_valid) {
					markEdgeCenters(border, start_x, start_y, last_x, last_y, CELL_NODE);
					start_x = -1;
					start_y = -1;
				}
				last_valid = cur_valid;
				last_x = g + x0;
				last_y = y0 + g * m;
				if (i == -1) {
					break;
				}
				g += lineStepSize;
			}
			if (start_x >= 0) {
				markEdgeCenters(border, start_x, start_y, last_x, last_y, CELL_NODE);
			}

		} else {
			if (dy < 0) {
				t = x0;
				x0 = x1;
				x1 = t;
				t = y0;
				y0 = y1;
				y1 = t;
			}
			m = dx / dy;
			t = y0;
			double g = 0;
			start_x = -1;
			start_y = -1;
			double finish = y1;
			while (g + y0 < finish) {
				res = getCell(x0 + g * m, y0 + g);
				int i, j;
				i = res[0];
				j = res[1];

				cur_valid = true;

				if (i == -1 || gridIntInfo[i][j][TYPE] != CELL_AREA_EDGE) {
					cur_valid = false;
				}

				if (!last_valid && cur_valid) {
					start_x = x0 + g * m;
					start_y = y0 + g;
				}

				if (last_valid && !cur_valid) {
					markEdgeCenters(border, start_x, start_y, last_x, last_y, CELL_NODE);
					start_x = -1;
					start_y = -1;
				}
				last_valid = cur_valid;
				last_x = x0 + g * m;
				last_y = y0 + g;
				if (i == -1) {
					break;
				}
				g += lineStepSize;
			}
			if (start_x >= 0) {
				markEdgeCenters(border, start_x, start_y, last_x, last_y, CELL_NODE);
			}
		}
	}

	public void markCell(int[] ij, int type) {
		gridIntInfo[ij[0]][ij[1]][TYPE] = type;
	}
	
	public void markCell(int i, int j, int type) {
		gridIntInfo[i][j][TYPE] = type;
	}

	public boolean insideGrid(int i, int j) {
		if (i < 0 || j < 0 || i >= gridM || j >= gridN) {
			return false;
		}
		return true;
	}

	public void paint(Graphics2D g2, K_ScreenTransform kst) {

		g2.setColor(new Color(100, 100, 100, 100));

		int r = 0;
		Color borderColor = new Color(0, 0, 0, 20);
		
		for (int i = 0; i < gridM; i++) {
			for (int j = 0; j < gridN; j++) {
				r = (int) GRID_SIZE / 2;
				
				Rectangle2D rect = kst.getTransformedRectangle(gridPoints[i][j][0] - r, gridPoints[i][j][1] - r, r * 2, r * 2);
				Color color = new Color(0, 0, 0, 10);
				
				switch(gridIntInfo[i][j][TYPE]) {
					case CELL_BLOCK: {
						color = new Color(40, 40, 40, 150);
						break;
					}
					case CELL_AREA_EDGE: {
						color = new Color(0, 0, 255, 50);
						break;
					}
					case CELL_NODE: {
						color = new Color(0, 255, 0, 200);
						break;
					}
					case CELL_OUT: {
						color = new Color(0, 0, 0, 100);
						break;
					}
				}
				
				g2.setColor(borderColor);
				g2.drawOval((int) rect.getMinX(), (int) rect.getMinY(), (int) rect.getWidth(), (int) rect.getHeight());
				
				g2.setColor(color);
				g2.fillOval((int) rect.getMinX(), (int) rect.getMinY(), (int) rect.getWidth(), (int) rect.getHeight());

			}
		}

	}
	

}