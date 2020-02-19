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

public class K_PassedAreas extends K_ViewerLayer {
	
	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		g2.setColor(Color.yellow);
		for (AURAreaGraph ag : wsg.areas.values()) {
			if (ag.isPassed()) {
				Polygon polygon = kst.getTransformedPolygon(ag.area.getShape());
				g2.fillPolygon(polygon);
			}
		}
	}

}
