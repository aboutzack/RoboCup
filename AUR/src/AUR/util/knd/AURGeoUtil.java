package AUR.util.knd;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;

import rescuecore2.misc.geometry.Line2D;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;

/**
 *
 * @author Alireza Kandeh - 2017 & 2018
 */

public class AURGeoUtil {

	public static final int COLLINEAR = 0;
	public static final int CLOCKWISE = 1;
	public static final int COUNTER_CLOCKWISE = -1;

	public final static double INF = 1e50;
	public final static double EPS = 1e-8;
        
	public static double dist(double Ax, double Ay, double Bx, double By) {
		return Math.hypot(Ax - Bx, Ay - By);
	}

	public static Rectangle getOffsetRect(Rectangle rect, double off) {
		Rectangle result = new Rectangle(
			(int) (rect.getMinX() - off),
			(int) (rect.getMinY() - off),
			(int) (rect.getWidth() + 2 * off),
			(int) (rect.getHeight() + 2 * off)
		);
		return result;
	}
	
	public static ArrayList<double[]> getRandomPointsOnSegmentLine(double Ax, double Ay, double Bx, double By, double rate) {
		ArrayList<double[]> result = new ArrayList<>();
		double dx = Bx - Ax;
		double dy = By - Ay;
		double l = Math.hypot(dx, dy);
		double n = (int) (rate * l);
		if(l <= 1e-5) {
			for(int i = 0; i < n; i++) {
				result.add(new double[]{Ax, By});
			}
			return result;
		}
		dx /= l;
		dy /= l;
		for(int i = 0; i < n; i++) {
			double rand = Math.random();
			result.add(new double[] {Ax + dx * l * rand, Ay + dy * l * rand});
		}
		return result;
	}
	
	public static void getRandomUnitVector(double result[]) {
		double r = Math.random() * Math.PI * 2;
		result[0] = Math.cos(r);
		result[1] = Math.sin(r);
	}
	
	private static double __temp__[] = new double[2];
	
	public static boolean intersectsOrContains(Polygon p1, Polygon p2) {
		
		if(p1.getBounds2D().intersects(p2.getBounds()) == false) {
			return false;
		}
		
		for(int i = 0; i < p1.npoints; i++) {
			for(int j = 0; j < p2.npoints; j++) {
				boolean b = AURGeoUtil.getIntersection(
						p1.xpoints[i],
						p1.ypoints[i],
						p1.xpoints[(i + 1) % p1.npoints],
						p1.ypoints[(i + 1) % p1.npoints],
						p2.xpoints[j],
						p2.ypoints[j],
						p2.xpoints[(j + 1) % p2.npoints],
						p2.ypoints[(j + 1) % p2.npoints],
						__temp__
				);
				if(b) {
					return true;
				}
			}
		}
		
		for(int i = 0; i < p1.npoints; i++) {
			if(p2.contains(p1.xpoints[i], p1.ypoints[i])) {
				return true;
			}
		}
		for(int j = 0; j < p2.npoints; j++) {
			if(p1.contains(p2.xpoints[j], p2.ypoints[j])) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean intersects(Polygon p1, Polygon p2) {
		
		if(p1.getBounds2D().intersects(p2.getBounds()) == false) {
			return false;
		}
		
		for(int i = 0; i < p1.npoints; i++) {
			for(int j = 0; j < p2.npoints; j++) {
				boolean b = AURGeoUtil.getIntersection(
						p1.xpoints[i],
						p1.ypoints[i],
						p1.xpoints[(i + 1) % p1.npoints],
						p1.ypoints[(i + 1) % p1.npoints],
						p2.xpoints[j],
						p2.ypoints[j],
						p2.xpoints[(j + 1) % p2.npoints],
						p2.ypoints[(j + 1) % p2.npoints],
						__temp__
				);
				if(b) {
					return true;
				}
			}
		}

		return false;
	}
	
	public static boolean intersectsOrContains(Polygon p, double[] segmentLine) {
		
		for(int i = 0; i < p.npoints; i++) {
			boolean b = AURGeoUtil.getIntersection(
					p.xpoints[i],
					p.ypoints[i],
					p.xpoints[(i + 1) % p.npoints],
					p.ypoints[(i + 1) % p.npoints],
					segmentLine[0],
					segmentLine[1],
					segmentLine[2],
					segmentLine[3],
					__temp__
			);
			if(b) {
				return true;
			}
		}

		if(p.contains(segmentLine[0], segmentLine[1])) {
			return true;
		}
		
		if(p.contains(segmentLine[2], segmentLine[3])) {
			return true;
		}
		
		return false;
	}

	
	public static double getArea(Polygon p) {
		double sum = 0;
		for (int i = 0; i < p.npoints; i++) {
			sum += (
				((double) (p.xpoints[i]) * (p.ypoints[(i + 1) % p.npoints])) -
				((double) (p.ypoints[i]) * (p.xpoints[(i + 1) % p.npoints]))
			);
		}
		return Math.abs(sum / 2);
	}
	
	public static boolean isAlmostConvex(Polygon p) {
		if(p.npoints <= 3) {
			return true;
		}
		p = AURGeoUtil.getSimplifiedPolygon(p, 0.1);
		if(p.npoints <= 3) {
			return true;
		}
		int ori = AURGeoUtil.COLLINEAR;
		for (int i = 0; i < p.npoints; i++) {
			if(false&& p.xpoints[i] == p.xpoints[(i + 1) % p.npoints]
				&& p.ypoints[i] == p.ypoints[(i + 1) % p.npoints]
			) {
				continue;
			}
			if(false&& p.xpoints[(i + 1) % p.npoints] == p.xpoints[(i + 2) % p.npoints]
				&& p.ypoints[(i + 1) % p.npoints] == p.ypoints[(i + 2) % p.npoints]
			) {
				continue;
			}
			int ori_ = AURGeoUtil.getOrientation(
				p.xpoints[i],
				p.ypoints[i],
				p.xpoints[(i + 1) % p.npoints],
				p.ypoints[(i + 1) % p.npoints],
				p.xpoints[(i + 2) % p.npoints],
				p.ypoints[(i + 2) % p.npoints]
			);
			if(ori == AURGeoUtil.COLLINEAR) {
				ori = ori_;
			}
			if(ori_ != ori && ori != AURGeoUtil.COLLINEAR && ori_ != AURGeoUtil.COLLINEAR) {
				return false;
			}
		}
		return true;
	}
		
	
	public static double getPerimeter(Polygon p) {
		double sum = 0;
		for (int i = 0; i < p.npoints; i++) {
			sum += AURGeoUtil.dist(p.xpoints[i], p.ypoints[i], p.xpoints[(i + 1) % p.npoints], p.ypoints[(i + 1) % p.npoints]);
		}
		return sum;
	}
	
	public static boolean hitRayAllEdges(Polygon p, double ray[]) {
		double ip[] = new double[2];
		boolean result = false;
		for(int i = 0; i < p.npoints; i++) {
			boolean b = AURGeoUtil.getIntersection(
					p.xpoints[i],
					p.ypoints[i],
					p.xpoints[(i + 1) % p.npoints],
					p.ypoints[(i + 1) % p.npoints],
					ray[0],
					ray[1],
					ray[2],
					ray[3],
					ip
			);
			if(b) {
				ray[2] = ip[0];
				ray[3] = ip[1];
				result = true;
			}
		}
		return result;
	}
	
	public static Polygon getSimplifiedPolygon(Polygon p, double d) {
		Polygon result = new Polygon();
		int lastX = p.xpoints[0];
		int lastY = p.ypoints[0];
		result.addPoint(lastX, lastY);
		for(int i = 1; i < p.npoints; i++) {
			double v1x = p.xpoints[i] - lastX;
			double v1y = p.ypoints[i] - lastY;
			double v2x = p.xpoints[(i + 1) % p.npoints] - p.xpoints[i];
			double v2y = p.ypoints[(i + 1) % p.npoints] - p.ypoints[i];
			double l1 = Math.hypot(v1x, v1y);
			double l2 = Math.hypot(v2x, v2y);
			if (Math.abs(l1) < AURGeoUtil.EPS || Math.abs(l2) < AURGeoUtil.EPS) {
				continue;
			}
			v1x /= l1;
			v1y /= l1;
			v2x /= l2;
			v2y /= l2;
			double v3x = v2x - v1x;
			double v3y = v2y - v1y;
			double l3 = Math.hypot(v3x, v3y);
			if (l3 < d) {
				continue;
			}
			lastX = p.xpoints[i];
			lastY = p.ypoints[i];
			result.addPoint(lastX, lastY);
		}
		return result;
	}
	
//	public static boolean hitRayAllEdges(Polygon p, double ray[]) {
//		double ip[] = new double[2];
//		boolean result = false;
//		for(int i = 0; i < p.npoints; i++) {
//			boolean b = AURGeoUtil.getIntersection(
//					p.xpoints[i],
//					p.ypoints[i],
//					p.xpoints[(i + 1) % p.npoints],
//					p.ypoints[(i + 1) % p.npoints],
//					ray[0],
//					ray[1],
//					ray[2],
//					ray[3],
//					ip
//			);
//			if(b) {
//				ray[2] = ip[0];
//				ray[3] = ip[1];
//				result = true;
//			}
//		}
//		return result;
//	}
	
	public static boolean hitRayWalls(Building building, double ray[]) {
		double ip[] = new double[2];
		boolean result = false;
		for(Edge edge : building.getEdges()) {
			if(edge.isPassable() == true) {
				continue;
			}
			boolean b = AURGeoUtil.getIntersection(
					edge.getStartX(),
					edge.getStartY(),
					edge.getEndX(),
					edge.getEndY(),
					ray[0],
					ray[1],
					ray[2],
					ray[3],
					ip
			);
			if(b) {
				ray[2] = ip[0];
				ray[3] = ip[1];
				result = true;
			}
		}
		return result;
	}
	
	public static int getOrientation(double Ax1, double Ay1, double Ax2, double Ay2, double Bx1, double By1) {
		double v = (Ay2 - Ay1) * (Bx1 - Ax2) - (Ax2 - Ax1) * (By1 - Ay2);
		if (Math.abs(v) < EPS) {
			return AURGeoUtil.COLLINEAR;
		}
		return v > 0 ? AURGeoUtil.CLOCKWISE : AURGeoUtil.COUNTER_CLOCKWISE;
	}

	public static boolean getIntersection(Line2D a, Line2D b, double[] result) {
		return getIntersection(
			a.getOrigin().getX(),
			a.getOrigin().getY(),
			a.getEndPoint().getX(),
			a.getEndPoint().getY(),
			b.getOrigin().getX(),
			b.getOrigin().getY(),
			b.getEndPoint().getX(),
			b.getEndPoint().getY(),
			result
		);
	}

	public static boolean equals(Edge e1, Edge e2) {
		if (true&& e1.getStartX() == e2.getStartX()
				&& e1.getEndX() == e2.getEndX()
				&& e1.getStartY() == e2.getStartY()
				&& e1.getEndY() == e2.getEndY()) {
			return true;
		}
		if (true&& e1.getStartX() == e2.getEndX()
				&& e1.getEndX() == e2.getStartX()
				&& e1.getStartY() == e2.getEndY()
				&& e1.getEndY() == e2.getStartY()) {
			return true;
		}
		return false;
	}

	public static boolean getIntersection(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3, double[] intersection) {
		double dx1 = x1 - x0;
		double dy1 = y1 - y0;
		double dx2 = x3 - x2;
		double dy2 = y3 - y2;
		double s = (dx1 * (y0 - y2) - dy1 * (x0 - x2)) / (dx1 * dy2 - dx2 * dy1);
		double t = (dx2 * (y0 - y2) - dy2 * (x0 - x2)) / (dx1 * dy2 - dx2 * dy1);
		if (s >= 0 - EPS && s <= 1 + EPS && t >= 0 - EPS && t <= 1 + EPS) {
			intersection[0] = x0 + (t * dx1);
			intersection[1] = y0 + (t * dy1);
			return true;
		}
		return false;
	}

	public static double length(double x0, double y0, double x1, double y1) {
		double dx = x1 - x0;
		double dy = y1 - y0;
		return Math.sqrt(dx * dx + dy * dy);
	}

	public static boolean equals(double Ax, double Ay, double Bx, double By) {
		return _equalsFast(Ax, Ay, Bx, By);
	}
	
	private static boolean _equalsFast(double Ax, double Ay, double Bx, double By) {
		return Math.abs(Ax - Bx) < EPS && Math.abs(Ay - By) < EPS;
	}
	
	private static boolean _equals(double Ax, double Ay, double Bx, double By) {
		return dist(Ax, Ay, Bx, By) < EPS;
	}

}