package AUR.util.knd;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class AURPerceptibleAndExtinguishablePolygon {

	public static Polygon get(AURBuilding building) {
		double maxExtinguishDistance = building.wsg.si.getFireExtinguishMaxDistance() - AURConstants.Agent.RADIUS;
		double maxViewDistance = building.wsg.si.getPerceptionLosMaxDistance() - AURConstants.Agent.RADIUS * 2;
		
		Polygon result = new Polygon();
		Polygon bp = building.ag.polygon;
		Rectangle bounds = bp.getBounds();
		
		bounds = new Rectangle(
				(int) (bounds.getMinX() - maxViewDistance),
				(int) (bounds.getMinY() - maxViewDistance),
				(int) (bounds.getWidth() + 2 * maxViewDistance),
				(int) (bounds.getHeight() + 2 * maxViewDistance)
		);
		
		
		Collection<StandardEntity> cands = building.wsg.wi.getObjectsInRectangle(
			(int) bounds.getMinX(),
			(int) bounds.getMinY(),
			(int) bounds.getMaxX(),
			(int) bounds.getMaxY()
		);
		int r_ = (int) Math.max(bounds.getHeight(), bounds.getWidth()) / 2;
		
		cands.remove(building.ag.area);
		
		ArrayList<Building> q1 = new ArrayList<>();
		ArrayList<Building> q2 = new ArrayList<>();
		ArrayList<Building> q3 = new ArrayList<>();
		ArrayList<Building> q4 = new ArrayList<>();
		
		
		double cx = building.ag.area.getX();
		double cy = building.ag.area.getY();
		
		Rectangle bounds1 = new Rectangle((int) cx, (int) cy, r_, r_);
		Rectangle bounds2 = new Rectangle((int) cx - r_, (int) cy, r_, r_);
		Rectangle bounds3 = new Rectangle((int) cx - r_, (int) cy - r_, r_, r_);
		Rectangle bounds4 = new Rectangle((int) cx, (int) cy - r_, r_, r_);
		
		for(StandardEntity sent : cands) {
		
			if(AURUtil.isBuilding(sent) == false) {
				continue;
			}
			Polygon p = (Polygon) ((Building) sent).getShape();
			Rectangle2D pBounds = p.getBounds();
			if(pBounds.intersects(bounds1)) {
				q1.add((Building) sent);
			}
			if(pBounds.intersects(bounds2)) {
				q2.add((Building) sent);
			}
			if(pBounds.intersects(bounds3)) {
				q3.add((Building) sent);
			}
			if(pBounds.intersects(bounds4)) {
				q4.add((Building) sent);
			}
		}

		double rx = 0;
		double ry = 0;

		double r = 0;
		double dr = (2 * Math.PI) / 80;
		
		double smallINF = 1e6;
		
		double p[] = new double[2];
		double ray[] = new double[4];

		while(r < Math.PI * 2) {
			double max_ = 0;
			double d = 0;
			double fx = 0;
			double fy = 0;
			rx = cx + Math.cos(r) * smallINF;
			ry = cy + Math.sin(r) * smallINF;
			boolean commonW = false;
			for(int i = 0; i < bp.npoints; i++) {
				boolean b = AURGeoUtil.getIntersection(
						cx,
						cy,
						rx,
						ry,
						bp.xpoints[i],
						bp.ypoints[i],
						bp.xpoints[(i + 1) % bp.npoints],
						bp.ypoints[(i + 1) % bp.npoints],
						p
				);
				if(b) {
					d = AURGeoUtil.dist(cx, cy, p[0], p[1]);
					if(d >= max_) {
						max_ = d;
						fx = p[0];
						fy = p[1];
						//commonW = building.commonWall[i];
					}
				}
			}
			
			if(commonW && false) {
				rx = fx;
				ry = fy;
			} else {
				
				double dist = AURGeoUtil.dist(cx, cy, fx, fy);
				
				dist = Math.min(dist + maxViewDistance, maxExtinguishDistance);
				
				rx = cx + Math.cos(r) * (dist);
				ry = cy + Math.sin(r) * (dist);
//				rx = fx;
//				ry = fy;
				ArrayList<Building> candi = null;
				if(r >= 0 && r <= Math.PI / 2) {
					candi = q1;
				} else if(r >= Math.PI / 2 && r <= Math.PI / 1) {
					candi = q2;
				} else if(r >= Math.PI / 1 && r <= 3 * Math.PI / 2) {
					candi = q3;
				} else if(r >= 3 * Math.PI / 2 && r <= 2 * Math.PI / 1) {
					candi = q4;
				}
				
				for(Building b : candi) {
					ray[0] = fx;
					ray[1] = fy;
					ray[2] = rx;
					ray[3] = ry;
					
					if(AURGeoUtil.hitRayWalls(b, ray)) {
						rx = ray[2];
						ry = ray[3];
						if(Math.abs(rx - fx) < 1 && Math.abs(ry - fy) < 1) {
							continue;
						}
					}
				}
			}
			
			result.addPoint((int) rx, (int) ry);
			r += dr;
		}
//		return result;
		return AURGeoUtil.getSimplifiedPolygon(result, 0.2);
	}

}
