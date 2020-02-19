package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURBuilding;
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

public class K_SightableBuildings extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		if (selected_ag == null || selected_ag.isBuilding()) {
			return;
		}
		if (selected_ag.sightableBuildings == null) {
			return;
		}
		g2.setColor(new Color(100, 0, 150, 100));
		for (AURBuilding b : selected_ag.sightableBuildings) {
			if (true) {
				Polygon polygon = kst.getTransformedPolygon(b.ag.area.getShape());
				g2.fillPolygon(polygon);
			}

		}
	}
    
}
