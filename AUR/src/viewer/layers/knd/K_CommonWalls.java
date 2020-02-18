package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class K_CommonWalls extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		g2.setStroke(new BasicStroke(3));
		g2.setColor(new Color(250, 0, 0, 150));
		for(AURAreaGraph ag : wsg.areas.values()) {
			if(ag.isBuilding() == true) {
				Polygon polygon = kst.getTransformedPolygon(ag.area.getShape());
				for(int i = 0; i < polygon.npoints; i++) {
					if(ag.getBuilding().commonWall[i]) {
						g2.drawLine(
							polygon.xpoints[i],
							polygon.ypoints[i],
							polygon.xpoints[(i + 1) % polygon.npoints],
							polygon.ypoints[(i + 1) % polygon.npoints]
						);
					}
				}
			}
		}
	}
	
}
