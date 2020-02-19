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

public class K_PerceptibleAreaPolygon extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		if(selected_ag == null || selected_ag.isBuilding() == false) {
			return;
		}
		Polygon polygon = selected_ag.getBuilding().getPerceptibleAreaPolygon();
		
		g2.setColor(new Color(0, 200, 0, 75));
		g2.fill(kst.getTransformedPolygon(polygon));
		g2.setColor(new Color(0, 200, 0, 100));
		g2.setStroke(new BasicStroke(2));
		g2.draw(kst.getTransformedPolygon(polygon));
		
		g2.setColor(Color.black);
		for(int i = 0; i < polygon.npoints; i++) {
			g2.fillRect(
				kst.xToScreen(polygon.xpoints[i] - 3),
				kst.yToScreen(polygon.ypoints[i] - 3),
				6,
				6
			);
		}
	}

}