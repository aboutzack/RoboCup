package AUR.util.knd;

import AUR.util.ConcaveHull;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import viewer.K_ScreenTransform;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class AURFireZone {
	
	public ArrayList<AURBuilding> buildings = null;

	private Polygon polygon = null;
	public AURWorldGraph wsg = null;
	
	public AURFireZone(AURWorldGraph wsg) {
		this.wsg = wsg;
		this.buildings = new ArrayList<>();
	}
	
	public boolean contains(AURBuilding b) {
		return this.buildings.contains(b);
	}
	
	public void add(AURBuilding b) {
		if(contains(b) == false) {
			this.buildings.add(b);
		}
	}
	
	public int getAgentsNeeded() {
		
		double sum = 0;
		double coe = 1;
		for(AURBuilding b : buildings) {
//			coe = 1;
			if(b.fireSimBuilding.ignoreFire() == true) {
				//continue;
			}
			if(b.ag.isSmall()) {
				sum += 0.75;
			} else {
				if(b.ag.isBig()) {
					sum += 1.3;
				} else {
					sum += 1.0;
				}
			}
		}
		return Math.max(1, (int) sum);
	}
	
	public int getPerceptTime() {
		int time = AURConstants.Math.INT_INF;
		
		if(buildings.size() <= 0) {
			return time;
		}
		
		for(AURBuilding b : buildings) {
			if(b.fireSimBuilding.ignoreFire() == true) {
				continue;
			}
			if( b.ag.isInExtinguishRange()) { // b.ag.isRecentlyReportedFire() &&
				time = 0;
				break;
			}
			time = Math.min(time, b.getPerceptTime());
		}
		return time;
	}
	
	public boolean ok() {
		
		
//		if(wsg.si.getCommsChannelsCount() <= 1) {
//			return true;
//		}
		
		int pt = getPerceptTime();
		
		if(pt <= 4) {
			return true;
		}
		
		for(AURBuilding b : buildings) {
			if(b.ag.isInExtinguishRange()) {
				return true;
			}
			if(b.ag.clusterIndex == b.wsg.agentCluster) {
				return true;
			}
			if(b.wsg.neighbourClusters.contains(b.ag.clusterIndex)) {
				return true;
			}
		}
		
//		if(g() >= 0.8)  {
//			return true;
//		}
		
		return false;
	}
	
	public double g() {
		int need = getAgentsNeeded();
		
		if(need <= 0) {
			return 0;
		}
		
		int time = getPerceptTime();
		
		
		if(time <= 0) {
			return 1000;
		}
		
		double g = 0;
		
		if(time > 0) {
			g = (double) need / time;
		}
		
		

		
		return g;
		
	}
	
	public boolean ignore() {
		return false;
	}
	
//	public Polygon getPolygon() {
//		if(polygon != null) {
//			return polygon;
//		}
////		ArrayList<int[]> points = new ArrayList<>();
//		ArrayList<ConcaveHull.Point> points = new ArrayList<>();
//		for(AURBuilding b : buildings) {
////			for(int i = 0; i < b.ag.polygon.npoints; i++) {
////				points.add(new ConcaveHull.Point((double) b.ag.polygon.xpoints[i], (double) b.ag.polygon.ypoints[i]));
////			} 
//			points.add(new ConcaveHull.Point((double) b.ag.getX(), (double) b.ag.getY()));
//		}
//		ConcaveHull ch = new ConcaveHull();
//		ArrayList<ConcaveHull.Point> rps = ch.calculateConcaveHull(points, 3);
//		polygon = new Polygon();
//		for(ConcaveHull.Point p : rps) {
//			polygon.addPoint((int) ((double) p.getX()), (int) ((double) p.getY()));
//		}
//		return polygon;
//		//return AURConvexHull.calc(points);
//	}
	
	
	public Polygon getPolygon() {
		if (polygon != null) {
			return polygon;
		}
		ArrayList<int[]> points = new ArrayList<>();
		for (AURBuilding b : buildings) {
//			for(int i = 0; i < b.ag.polygon.npoints; i++) {
//				points.add(new int[] {b.ag.polygon.xpoints[i], b.ag.polygon.ypoints[i]});
//			} 
			points.add(new int[] {b.ag.getX(), b.ag.getY()});
		}
		return AURConvexHull.calc(points);
	}
	
	public void paint(Graphics2D g2, K_ScreenTransform kst) {
		g2.setColor(new Color(200, 0, 0, 70));
		for(AURBuilding b : this.buildings) {
			g2.fill(kst.getTransformedPolygon(b.ag.polygon));
		}
		g2.setStroke(new BasicStroke(3));
		g2.setColor(new Color(150, 0, 0, 255));
		g2.draw(kst.getTransformedPolygon(getPolygon()));
//		g2.setColor(Color.CYAN);
//		for(AURBuilding b : this.buildings) {
//			g2.draw(kst.getTransformedRectangle(b.ag.getOffsettedBounds(AURConstants.Misc.FIRE_ZONE_BORDER_INTERSECT_THRESHOLD)));
//		}
	}
	
}
