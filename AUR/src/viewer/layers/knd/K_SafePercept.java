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

public class K_SafePercept extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		g2.setStroke(new BasicStroke(2));
		wsg.KStar(wsg.ai.getPosition());
		for (AURAreaGraph ag : wsg.areas.values()) {
			if(ag.isBuilding() == false) {
				continue;
			}
			if (ag.getBuilding().isSafePerceptible() == false) {
				g2.setColor(Color.red);
			} else {
				g2.setColor(Color.GREEN);
			}

			Polygon polygon = kst.getTransformedPolygon(ag.area.getShape());
			g2.fillPolygon(polygon);

		}
		g2.setStroke(new BasicStroke(1));
	}
}