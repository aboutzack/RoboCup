package AUR.util.knd;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Stack;

/**
 *
 * @author Alireza Kandeh - 2017 & 2018
 */

public class AURConvexHull {

	public static Polygon calc(ArrayList<int[]> points_) {
		
		Polygon result = new Polygon();
		
		if (points_.size() <= 0) {
			return result;
		}
		
		Stack<int[]> stack = new Stack<int[]>();
		ArrayList<int[]> temp = new ArrayList<int[]>();
		ArrayList<int[]> points = new ArrayList<>();
		points.addAll(points_);

		int mblP[] = null;
		for (int[] p : points) {
			if (mblP == null || p[1] < mblP[1] || (p[1] == mblP[1] && p[0] < mblP[0])) {
				mblP = p;
			}
		}
		if (points.size() > 1) {
			points.remove(mblP);
			Collections.sort(points, new OrientationComparator(mblP));

			points.add(0, mblP);
			temp.add(0, mblP);
			int[] curPoint;
			int[] nexPoint;
			int ori;
			for (int i = 1; i < points.size(); i++) {
				if (i == (points.size() - 1)) {
					temp.add(points.get(i));
					continue;
				}
				curPoint = points.get(i);
				nexPoint = points.get(i + 1);
				ori = AURGeoUtil.getOrientation(mblP[0], mblP[1], curPoint[0], curPoint[1], nexPoint[0], nexPoint[1]);
				if (ori != 0) {
					temp.add(points.get(i));
				}
			}

			if (temp.size() < 3) {
				for(int p[] : temp) {
					result.addPoint(p[0], p[1]);
				}
				return result;
			}

			stack.clear();
			stack.push(temp.get(0));
			stack.push(temp.get(1));
			stack.push(temp.get(2));
			int[] p1;
			int[] p2;
			int[] p3;
			for (int i = 3; i < temp.size(); i++) {
				p1 = stack.elementAt(stack.size() - 2);
				p2 = stack.peek();
				p3 = temp.get(i);
				while (AURGeoUtil.getOrientation(p1[0], p1[1], p2[0], p2[1], p3[0], p3[1]) == 1) {
					stack.pop();
					p1 = stack.elementAt(stack.size() - 2);
					p2 = stack.peek();
				}
				stack.push(temp.get(i));
			}
			for(int p[] : stack.subList(0, stack.size())) {
				result.addPoint(p[0], p[1]);
			}
			return result;
		} else {
			result.addPoint(mblP[0], mblP[1]);
			return result;
		}
	}
	
	
	private static class OrientationComparator implements Comparator<int[]> {
		
		int base[] = null;
		
		public OrientationComparator(int base[]) {
			this.base = base;
		}

		@Override
		public int compare(int o1[], int o2[]) {
			int orination = AURGeoUtil.getOrientation(this.base[0], this.base[1], o1[0], o1[1], o2[0], o2[1]);
			if (orination == 0) {
				double d1 = AURGeoUtil.dist(this.base[0], this.base[1], o1[0], o1[1]);
				double d2 = AURGeoUtil.dist(this.base[0], this.base[1], o2[0], o2[1]);
				return Double.compare(d1, d2);
			}
			return orination;
		}
		
	}

//	public boolean isOnEdge(Rectangle rect) {
//		int size = resultPoints.size();
//		if (size == 1) {
//			return true;
//		}
//		int ni;
//		AURValuePoint pi;
//		AURValuePoint pni;
//		for (int i = 0; i < size; i++) {
//			ni = (i + 1) % size;
//			pi = resultPoints.get(i);
//			pni = resultPoints.get(ni);
//			if (rect.intersectsLine(pi.x, pi.y, pni.x, pni.y)) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	private void calcCenter() {
//		centerPoint.set(0, 0);
//		int size = resultPoints.size();
//		if (size <= 0) {
//			return;
//		}
//		for (AURValuePoint p : resultPoints) {
//			centerPoint.x += p.x;
//			centerPoint.y += p.y;
//		}
//		centerPoint.x /= size;
//		centerPoint.y /= size;
//	}
//
//	public void draw(Graphics2D g) {
//		int a = 5;
//		int ii = 0;
//		for (AURValuePoint point : points) {
//			g.setColor(Color.gray);
//			g.fillRect((int) (point.x - a), (int) (point.y - a), 2 * a, 2 * a);
//			g.setColor(Color.black);
//			// g.drawString(ii++ + "", (int) (point.getX()), (int)
//			// (point.getY()));
//		}
//		ii = 0;
//		for (AURValuePoint point : resultPoints) {
//			a = 2;
//			g.setColor(Color.GREEN);
//			g.fillRect((int) (point.x - a), (int) (point.y - a), 2 * a, 2 * a);
//			g.setColor(Color.black);
//			// g.drawString(ii++ + "", (int) (point.x), (int) (point.y));
//		}
//		g.setColor(Color.gray);
//		int ni;
//		for (int i = 0; i < resultPoints.size(); i++) {
//			ni = (i + 1) % resultPoints.size();
//			g.drawLine((int) (resultPoints.get(i).x), (int) (resultPoints.get(i).y), (int) (resultPoints.get(ni).x),
//					(int) (resultPoints.get(ni).y));
//		}
//
//		a = 4;
//		g.setColor(Color.blue);
//		g.fillRect((int) (centerPoint.x - a), (int) (centerPoint.y - a), 2 * a, 2 * a);
//	}
}