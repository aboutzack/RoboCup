package AUR.util.knd;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import adf.agent.info.AgentInfo;
import adf.agent.info.WorldInfo;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Alireza Kandeh - 2017
 */

public class AURRandomDirectSelector {

	private final int AGENT_POSITION_HISTORY_SIZE = 4;
	private ArrayList<Point2D> agentPositionHistory = new ArrayList<>(AGENT_POSITION_HISTORY_SIZE + 1);

	private WorldInfo wi = null;
	private AgentInfo ai = null;

	public AURRandomDirectSelector(AgentInfo ai, WorldInfo wi) {
		this.wi = wi;
		this.ai = ai;
	}

	class RandomPoint implements Comparable<RandomPoint> {
		double x;
		double y;
		int value;
		double r;
		double cx, cy;

		public RandomPoint(double cx, double cy, double maxR) {
			this.cx = cx;
			this.cy = cy;
			this.x = cx + Math.random() * maxR * 2 - maxR;
			this.y = cy + Math.random() * maxR * 2 - maxR;
			this.r = AURGeoUtil.dist(x, y, cx, cy);
			this.value = (int) r;
			addValue__recentPoints();
		}

		@Override
		public int compareTo(RandomPoint o) {
			return 0 - (int) (this.value - o.value);
		}

		public void addValue__recentPoints() {
			for (Point2D point : agentPositionHistory) {
				this.value += AURGeoUtil.dist(x, y, point.getX(), point.getY());
			}
		}

		public void addValue__areaInside(LinkedList<Polygon> allAroundAreaPolygons,
				LinkedList<Polygon> allAroundBuildingOnFirePolygons) {
			boolean insideAnArea = false;
			for (Polygon areaPolygon : allAroundAreaPolygons) {
				if (areaPolygon.contains(x, y)) {
					insideAnArea = true;
					break;
				}
			}
			if (insideAnArea == false) {
				value = -1;
				return;
			}
			for (Polygon buldingPolygon : allAroundBuildingOnFirePolygons) {
				if (buldingPolygon.intersects(x - 2000, y - 2000, 4000, 4000) || buldingPolygon.contains(x, y)) {
					value = -3;
					break;
				}
			}
		}

		public void addValue__blockade(LinkedList<Polygon> AllBlockades) {
			for (Polygon blockadePolygon : AllBlockades) {
				if (blockadePolygon.intersects(x - 300, y - 300, 600, 600) || blockadePolygon.contains(x, y)
						|| intersects(blockadePolygon, cx, cy, x, y)) {
					value = -2;
					return;
				}
			}
		}
	}

	public boolean intersects(Polygon p, double x0, double y0, double x1, double y1) {
		int ni;
		boolean b;
		double ip[] = new double[2];
		for (int i = 0; i < p.npoints; i++) {
			ni = (i + 1) % p.npoints;
			b = AURGeoUtil.getIntersection(x0, y0, x1, y1, p.xpoints[i], p.ypoints[i], p.xpoints[ni], p.ypoints[ni],
					ip);
			if (b) {
				return true;
			}
		}
		return false;
	}

	public LinkedList<Area> getAroundAreas() {
		LinkedList<Area> result = new LinkedList<>();
		LinkedList<EntityID> IDs = new LinkedList<>();

		Area agentArea = ai.getPositionArea();
		for (EntityID neiID : agentArea.getNeighbours()) {
			if (IDs.contains(neiID) == false) {
				IDs.add(neiID);
				result.add((Area) (wi.getEntity(neiID)));
			}
		}
		LinkedList<Area> resultPlus = new LinkedList<>();
		for (Area area : result) {
			for (EntityID neiID : area.getNeighbours()) {
				if (IDs.contains(neiID) == false) {
					IDs.add(neiID);
					resultPlus.add((Area) (wi.getEntity(neiID)));
				}
			}
		}
		result.addAll(resultPlus);
		return result;
	}

	public LinkedList<Polygon> getBuildingOnFirePolygons(LinkedList<Area> allAreas) {
		LinkedList<Polygon> result = new LinkedList<>();
		for (Area area : allAreas) {
			StandardEntityURN urn = area.getStandardURN();
			if (false || urn.equals(StandardEntityURN.BUILDING) || urn.equals(StandardEntityURN.GAS_STATION)
					|| urn.equals(StandardEntityURN.AMBULANCE_CENTRE) || urn.equals(StandardEntityURN.FIRE_STATION)
					|| urn.equals(StandardEntityURN.POLICE_OFFICE)) {
				if (((Building) (area)).isOnFire()) {
					result.add((Polygon) (area.getShape()));
				}
			}
		}
		return result;
	}

	public LinkedList<Polygon> getPolygons(LinkedList<Area> allAreas) {
		LinkedList<Polygon> result = new LinkedList<>();
		for (Area area : allAreas) {
			result.add((Polygon) (area.getShape()));
		}
		return result;
	}

	public LinkedList<Polygon> getBlockadePolygons(LinkedList<Area> allAreas) {
		LinkedList<Polygon> result = new LinkedList<>();
		for (Area area : allAreas) {
			Collection<Blockade> blockades = wi.getBlockades(area);
			for (Blockade b : blockades) {
				result.add((Polygon) (b.getShape()));
			}

		}
		return result;
	}

	public void update() {
		if (agentPositionHistory.size() > AGENT_POSITION_HISTORY_SIZE) {
			agentPositionHistory.remove(0);
		}
		agentPositionHistory.add(new Point2D(ai.getX(), ai.getY()));
	}

	public void generate() {
		double x = ai.getX();
		double y = ai.getY();
		LinkedList<RandomPoint> list = new LinkedList<>();
		for (int i = 0; i < 500; i++) {
			list.add(new RandomPoint(x, y, 9000));
		}

		LinkedList<Area> allAroundAreas = getAroundAreas();
		LinkedList<Polygon> allAroundAreaPolygons = getPolygons(allAroundAreas);
		LinkedList<Polygon> allAroundBuildingOnFirePolygons = getBuildingOnFirePolygons(allAroundAreas);
		LinkedList<Polygon> allAroundBlockadePolygons = getBlockadePolygons(allAroundAreas);

		for (RandomPoint point : list) {
			point.addValue__areaInside(allAroundAreaPolygons, allAroundBuildingOnFirePolygons);
			point.addValue__blockade(allAroundBlockadePolygons);
		}

		Collections.sort(list);
		generatedPoint = new Point2D(list.get(0).x, list.get(0).y);
	}

	public Point2D generatedPoint = null;
}
