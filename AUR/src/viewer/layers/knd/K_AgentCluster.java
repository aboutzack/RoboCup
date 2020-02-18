package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import viewer.K_ScreenTransform;
import viewer.K_Viewer;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class K_AgentCluster extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		for (AURAreaGraph ag : wsg.areas.values()) {
			
			if(ag.clusterIndex != wsg.agentCluster) {
				continue;
			}
			
			if (ag.isBuilding() == true) {
				g2.setColor(K_Viewer.colors_list.get(wsg.agentCluster));
				Polygon polygon = kst.getTransformedPolygon(ag.area.getShape());
				g2.fillPolygon(polygon);
				g2.setColor(Color.darkGray);
				g2.drawPolygon(polygon);
			}

		}
		g2.setColor(K_Viewer.colors_list.get(wsg.agentCluster));
		g2.setStroke(new BasicStroke(2));
		g2.draw(kst.getTransformedRectangle(wsg.myClusterBounds));
		g2.setStroke(new BasicStroke(1));
	}

}
