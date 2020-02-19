package AUR.util.knd;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;

import rescuecore2.standard.entities.Edge;

public class AURAreaInSightChecker {

//	public AURAreaGraph targetAg = null;
//	public AURWorldGraph wsg = null;
//	public ArrayList<AURWall> targetAreaWalls = new ArrayList<>();
//	public ArrayList<AURWall> targetAroundAreaWalls = new ArrayList<>();
//	public Rectangle targetBound = null;
//	public double maxViewDistance = 10000;
//	public int rayCount = 72;
//
//	public AURRange boundRange = null;
//
//	public AURRaySet raysInstance = null;
//
//	public AURAreaInSightChecker(AURWorldGraph wsg, AURAreaGraph targetAg) {
//		this.wsg = wsg;
//		this.targetAg = targetAg;
//		this.maxViewDistance = wsg.si.getPerceptionLosMaxDistance();
//		this.maxViewDistance -= 500 * 3;
//		targetBound = targetAg.area.getShape().getBounds();
//		targetBound = new Rectangle((int) (targetBound.getMinX() - maxViewDistance),
//				(int) (targetBound.getMinY() - maxViewDistance), (int) (targetBound.getWidth() + 2 * maxViewDistance),
//				(int) (targetBound.getHeight() + 2 * maxViewDistance));
//		double r = Math.max(targetBound.getWidth() / 2, targetBound.getHeight() / 2);
//		;
//		boundRange = new AURRange(targetBound.getCenterX(), targetBound.getCenterY(), r);
//		this.targetPolygon = (Polygon) (targetAg.area.getShape());
//		this.inited = false;
//		raysInstance = new AURRaySet(rayCount, maxViewDistance);
//	}
//
//	public Polygon targetPolygon = null;
//
//	private boolean inited = false;
//
//	public boolean hasChance(AURAreaGraph ag) {
//		Rectangle bound = ag.area.getShape().getBounds();
//		if (targetBound.intersects(bound)) {
//			if (ag.area.getShape().intersects(targetBound)) {
//				return true;
//			}
//		}
//		if (targetBound.contains(bound)) {
//			return true;
//		}
//		return false;
//	}
//
//	public boolean query(double x, double y) {
//		if (inited == false) {
//			getTargetBlockerWalls();
//			inited = true;
//		}
//		if (boundRange.contains(x, y) == false) {
//			return false;
//		}
//		if (targetBound.contains(x, y) == false) {
//			return false;
//		}
//		boolean b = false;
//		b = raysInstance.initAndCheck(this, x, y, rayCount, maxViewDistance, targetPolygon, targetAroundAreaWalls);
//		return b;
//	}
//
//	private void getTargetBlockerWalls() {
//		targetAroundAreaWalls.clear();
//		for (AURWall wall : wsg.walls) {
//			if (wall.owner != targetAg && wall.inBoundOrIntersectWith(targetBound)) {
//				targetAroundAreaWalls.add(wall);
//			}
//		}
//	}
//
//	public void draw(Graphics2D g) {
//
//		g.setColor(Color.BLUE);
//		g.draw(targetBound);
//
//		boundRange.draw(g);
//
//		g.setColor(Color.YELLOW);
//		for (AURWall wall : targetAreaWalls) {
//			wall.draw(g);
//		}
//
//		g.setColor(Color.RED);
//		for (AURWall wall : targetAroundAreaWalls) {
//			wall.draw(g);
//		}
//	}
}
