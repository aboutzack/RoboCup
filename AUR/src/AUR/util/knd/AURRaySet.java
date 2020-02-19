package AUR.util.knd;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;

public class AURRaySet {

//	public K_Ray rays[] = null;
//
//	public AURRaySet(int count, double length) {
//		double dr = (Math.PI * 2) / count;
//		rays = new K_Ray[count];
//		for (int i = 0; i < count; i++) {
//			rays[i] = new K_Ray(i * dr, length);
//		}
//	}
//
//	public boolean initAndCheck(AURAreaInSightChecker checker, double cx, double cy, int count, double length,
//			Polygon targetPolygon, ArrayList<AURWall> blockerWalls) {
//		int counter = 0;
//		for (int i = 0; i < count; i++) {
//			rays[i].init(cx, cy);
//			if (checker.boundRange.contains(rays[i].ex, rays[i].ey) == false) {
//				continue;
//			}
//			for (AURWall wall : blockerWalls) {
//				rays[i].calcWall(wall);
//			}
//			if (rays[i].hits(targetPolygon)) {
//				counter++;
//				if (counter >= 1) {
//					return true;
//				}
//			}
//
//		}
//		return false;
//	}
//
//	public void draw(Graphics2D g) {
//		g.setColor(Color.cyan);
//		for (K_Ray ray : rays) {
//			ray.draw(g);
//		}
//	}
//
//	class K_Ray {
//
//		public double sx = 0;
//		public double sy = 0;
//		public double ex = 0;
//		public double ey = 0;
//		public double edx = 0;
//		public double edy = 0;
//
//		public K_Ray(double radian, double length) {
//			this.sx = 0;
//			this.sy = 0;
//			this.edx = Math.cos(radian) * length;
//			this.edy = Math.sin(radian) * length;
//			this.ex = sx + edx;
//			this.ey = sy + edy;
//		}
//
//		public void init(double sx, double sy) {
//			this.sx = sx;
//			this.sy = sy;
//			this.ex = sx + edx;
//			this.ey = sy + edy;
//		}
//
//		double ip[] = new double[2];
//
//		public void calcWall(AURWall wall) {
//			boolean b = AURGeoUtil.getIntersection(sx, sy, ex, ey, wall.x0, wall.y0, wall.x1, wall.y1, ip);
//			if (b) {
//				ex = ip[0];
//				ey = ip[1];
//			}
//		}
//
//		public boolean hits(Polygon polygon) {
//			int ni;
//			boolean b;
//			for (int i = 0; i < polygon.npoints; i++) {
//				ni = (i + 1) % polygon.npoints;
//				b = AURGeoUtil.getIntersection(sx, sy, ex, ey, polygon.xpoints[i], polygon.ypoints[i],
//						polygon.xpoints[ni], polygon.ypoints[ni], ip);
//				if (b) {
//					return true;
//				}
//			}
//			return false;
//		}
//
//		public void draw(Graphics2D g) {
//			g.drawLine((int) sx, (int) sy, (int) ex, (int) ey);
//		}
//	}

}
