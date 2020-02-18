package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class K_InflammableBuildings extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		Color redColor = new Color(255, 0, 0, 128);
		Color greenColor = new Color(0, 255, 0, 128);
		for (AURAreaGraph ag : wsg.areas.values()) {
			if (ag.isBuilding() == true) {
				if(ag.getBuilding().fireSimBuilding.inflammable() == true) {
					g2.setColor(redColor);
				} else {
					g2.setColor(greenColor);
				}
				Polygon polygon = kst.getTransformedPolygon(ag.area.getShape());
				g2.fillPolygon(polygon);
			}
		}
	}

}
