package AUR.util.knd;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.StandardEntity;
import viewer.K_ScreenTransform;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class AURSightAreaPolygon {
	
	private static final double RAY_RATE = 0.005;
	private static final int R = 1500;
	
	public static Polygon get(AURBuilding building) {
		return getAndPaint(building, null, null);
	}
	
	public static Polygon getAndPaint(AURBuilding building, Graphics2D g2, K_ScreenTransform kst) {
		
		boolean paint = (g2 != null && kst != null);
		
		if(paint) {
			g2.setStroke(new BasicStroke(1));
		}
		
		double maxViewDistance = building.wsg.si.getPerceptionLosMaxDistance() - AURConstants.Agent.RADIUS - R;

		Polygon result = new Polygon();
		Polygon bp = building.ag.polygon;
		Rectangle bounds = new Rectangle(
				(int) (building.ag.getX() - maxViewDistance),
				(int) (building.ag.getY() - maxViewDistance),
				(int) (2 * maxViewDistance),
				(int) (2 * maxViewDistance)
		);
		
		if(paint) {
			g2.setColor(Color.blue);
			g2.draw(kst.getTransformedRectangle(bounds));
		}
		
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
			if(sent instanceof Building == false) {
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
		
		ArrayList<Double> rs = new ArrayList<>();
		ArrayList<Edge> blockingEdges = new ArrayList<>();
		
		for(Edge edge : ((Building) building.ag.area).getEdges()) {
			double len = AURGeoUtil.dist(edge.getStartX(), edge.getStartY(), edge.getEndX(), edge.getEndY());
			int rays = (int) (len * RAY_RATE);
			if(edge.isPassable() == true) {
				
				double dx = edge.getEndX() - edge.getStartX();
				double dy = edge.getEndY() - edge.getStartY();
				
				double l = Math.hypot(dx, dy);
				if(l < 1e-3) {
					continue;
				}
				dx /= l;
				dy /= l;

				double ecdx = 0;
				double ecdy = 0;
				double l2 = 0;
				
				if(paint) {
					g2.setColor(Color.green);
					kst.drawTransformedLine(g2, cx - dx * R, cy - dy * R, cx + dx * R, cy + dy * R);
				}
				
				ecdx = edge.getStartX() - (cx - dx * R);
				ecdy = edge.getStartY() - (cy - dy * R);
				l2 = Math.hypot(ecdx, ecdy);
				ecdx /= l2;
				ecdy /= l2;
				
				blockingEdges.add(new Edge(
					(int) (edge.getStartX() - ecdx * 10),
					(int) (edge.getStartY() - ecdy * 10),
					(int) (edge.getStartX() + maxViewDistance * 1 * ecdx),
					(int) (edge.getStartY() + maxViewDistance * 1 * ecdy)
				));

				
				ecdx = edge.getEndX() - (cx + dx * R);
				ecdy = edge.getEndY() - (cy + dy * R);
				l2 = Math.hypot(ecdx, ecdy);
				ecdx /= l2;
				ecdy /= l2;
				
				blockingEdges.add(new Edge(
					(int) (edge.getEndX() - ecdx * 10),
					(int) (edge.getEndY() - ecdy * 10),
					(int) (edge.getEndX() + maxViewDistance * 1 * ecdx),
					(int) (edge.getEndY() + maxViewDistance * 1 * ecdy)
				));
				
				
				dx *= (l / (rays - 1));
				dy *= (l / (rays - 1));
				
				for(int i = 0; i < rays; i++) {
					double px = edge.getStartX() + i * dx;
					double py = edge.getStartY() + i * dy;
					double rdx = px - cx;
					double rdy = py - cy;
					double r = Math.atan2(rdy, rdx);
					if(r < 0) {
						r += Math.PI * 2;
					}
					if(r > Math.PI * 2) {
						r -= Math.PI * 2;
					}
					if(rs.size() <= 0 || Math.abs(r - rs.get(rs.size() - 1)) > 1e-8) {
						rs.add(r);
					}
				}
			}
		}
		
		ArrayList<Edge> dels = new ArrayList<>();
		for(int i = 0; i < blockingEdges.size(); i++) {
			Edge iEdge = blockingEdges.get(i);
			if(dels.contains(iEdge)) {
				continue;
			}
			for(int j = i + 1; j < blockingEdges.size(); j++) {
				Edge jEdge = blockingEdges.get(j);
				double dx = iEdge.getStartX() - jEdge.getStartX();
				double dy = iEdge.getStartY() - jEdge.getStartY();
				if(Math.hypot(dx, dy) < 30) {
					dels.add(iEdge);
					dels.add(jEdge);
					continue;
				}
			}
		}
		
		blockingEdges.removeAll(dels);
		
		if(paint) {
			g2.setBackground(Color.green);
			for(Edge edge : blockingEdges) {
				kst.drawTransformedLine(g2, edge.getStartX(), edge.getStartY(), edge.getEndX(), edge.getEndY());
			}
		}

		double rx = 0;
		double ry = 0;

		double p[] = new double[2];
		double ray[] = new double[4];

		result.addPoint((int) cx, (int) cy);
		
		//double 
		
		for(Double r : rs) {
			
			rx = cx + Math.cos(r) * maxViewDistance;
			ry = cy + Math.sin(r) * maxViewDistance;

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

			if(candi == null) {
				continue;
			}
			
			ray[0] = cx;
			ray[1] = cy;
			ray[2] = rx;
			ray[3] = ry;
			
			boolean b = AURGeoUtil.hitRayWalls((Building) building.ag.area, ray);
			if(b == true) {
				rx = ray[2];
				ry = ray[3];
			}
			
			for(Edge edge : blockingEdges) {
				
				ray[0] = cx;
				ray[1] = cy;
				ray[2] = rx;
				ray[3] = ry;
			
				boolean b_ = AURGeoUtil.getIntersection(
					ray[0],
					ray[1],
					ray[2],
					ray[3],
					edge.getStartX(),
					edge.getStartY(),
					edge.getEndX(),
					edge.getEndY(),
					p
				);
				
				if(b_ == true) {
					rx = p[0];
					ry = p[1];
				}
				
			}
			

			for(Building bu : candi) {
				ray[0] = cx;
				ray[1] = cy;
				ray[2] = rx;
				ray[3] = ry;

				if(AURGeoUtil.hitRayWalls(bu, ray)) {
					rx = ray[2];
					ry = ray[3];
					if(Math.abs(rx - cx) < 1 && Math.abs(ry - cy) < 1) {
						continue;
					}
				}
			}
			
			result.addPoint((int) rx, (int) ry);
		}

		return AURGeoUtil.getSimplifiedPolygon(result, 0.1);
	}
}
