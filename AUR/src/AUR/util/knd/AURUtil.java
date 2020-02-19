package AUR.util.knd;

import adf.agent.info.WorldInfo;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collection;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class AURUtil {
	
	public static boolean isBuilding(StandardEntity sent) {
		StandardEntityURN urn = sent.getStandardURN();
		return (false
			|| urn.equals(StandardEntityURN.BUILDING)
			|| urn.equals(StandardEntityURN.GAS_STATION)
			|| urn.equals(StandardEntityURN.REFUGE)
			|| urn.equals(StandardEntityURN.POLICE_OFFICE)
			|| urn.equals(StandardEntityURN.AMBULANCE_CENTRE)
			|| urn.equals(StandardEntityURN.FIRE_STATION));
	}
	
	public static ArrayList<StandardEntity> getTravelAreas(WorldInfo wi, Human h) {
		ArrayList<StandardEntity> result = new ArrayList<>();
		if (h.isPositionHistoryDefined() == false) {
			return result;
		}
		int travelHistory[] = h.getPositionHistory();
		if (travelHistory == null || travelHistory.length <= 2) {
			return result;
		}
		int minX = AURConstants.Math.INT_INF;
		int minY = AURConstants.Math.INT_INF;
		int maxX = 0;
		int maxY = 0;
		
		for (int i = 0; i < travelHistory.length; i++) {
			minX = Math.min(minX, travelHistory[i]);
			minY = Math.min(minY, travelHistory[i + 1]);
			maxX = Math.max(maxX, travelHistory[i]);
			maxY = Math.max(maxY, travelHistory[i + 1]);
			i++;
		}
		
		Collection<StandardEntity> cands = wi.getObjectsInRectangle(minX, minY, maxX, maxY);
		

		int lastX = travelHistory[0];
		int lastY = travelHistory[1];

		for (int i = 2; i < travelHistory.length; i++) {
			for(StandardEntity sent : cands) {
				if(sent instanceof Area) {
					Polygon p = (Polygon) ((Area) sent).getShape();
					double segmentLine[] = new double[] {lastX, lastY, travelHistory[i], travelHistory[i + 1]};
					if(AURGeoUtil.intersectsOrContains(p, segmentLine) == true) {
						result.add(sent);
					}
				}
			}
			lastX = travelHistory[i];
			lastY = travelHistory[i + 1];
			i++;
		}
		return result;
	}
	
//	public int getAroundAliveLowerFireBrigades(WorldInfo wi) {
//		for (StandardEntity sent : wi.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE)) {
//			FireBrigade fb = (FireBrigade) sent;
//			if(fb.isHPDefined() && fb.getHP() > 0) {
//				
//			}
//			if (fb.isXDefined() && fb.isYDefined() && fb.isPositionDefined()) {
//				if (fb.getID().getValue() < ai.me().getID().getValue()) {
//
//					double dist = AURGeoUtil.dist(ai.getX(), ai.getY(), fb.getX(), fb.getY());
//
//					if (fb.getPosition().equals(ai.getPosition())) { // 
//						return this;
//					}
//				}
//			}
//		}
//	}
	
}
