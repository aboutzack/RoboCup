package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURUtil;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class K_TravelHistoryExt extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		
		g2.setStroke(new BasicStroke(2));
		Human h = (Human) wsg.ai.me();
		if(h.isPositionHistoryDefined() == false) {
			return;
		}
		
		ArrayList<StandardEntity> areas = AURUtil.getTravelAreas(wsg.wi, h);
		
		g2.setColor(Color.ORANGE);
		
		for(StandardEntity sent : areas) {
			AURAreaGraph ag = wsg.getAreaGraph(sent.getID());
			if(ag != null) {
				g2.fill(kst.getTransformedPolygon(ag.polygon));
			}
		}
		
		g2.setColor(Color.red);
		
		int travelHistory[] = h.getPositionHistory();
		if(travelHistory == null || travelHistory.length <= 2) {
			return;
		}
		int lastX = travelHistory[0];
		int lastY = travelHistory[1];
		
		kst.fillTransformedOvalFixedRadius(g2, lastX, lastY, 5);
		
		for(int i = 2; i < travelHistory.length; i++) {
			kst.drawTransformedLine(g2, lastX, lastY, travelHistory[i], travelHistory[i + 1]);
			lastX = travelHistory[i];
			lastY = travelHistory[i + 1];
			kst.fillTransformedOvalFixedRadius(g2, lastX, lastY, 4);
			i++;
		}
		
		g2.setStroke(new BasicStroke(1));
	}

}