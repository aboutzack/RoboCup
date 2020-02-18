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

public class K_BuildingSightableAreas extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		if (selected_ag == null || selected_ag.isBuilding() == false) {
			return;
		}
		g2.setColor(new Color(200, 255, 150, 75));
		for (AURAreaGraph ag : selected_ag.getBuilding().getSightableAreas()) {
			if (true) {
				Polygon polygon = kst.getTransformedPolygon(ag.area.getShape());
				g2.fillPolygon(polygon);
			}

		}
	}

}
