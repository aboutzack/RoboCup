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
 * @author Alireza Kandeh - 2017
 */

public class K_LayerAliveBlockades extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		g2.setColor(Color.black);
		g2.setStroke(new BasicStroke(3));
		for(AURAreaGraph ag : wsg.areas.values()) {
			for(Polygon p : ag.getAliveBlockades()) {
				Polygon polygon = kst.getTransformedPolygon(p);
				g2.draw(polygon);
				g2.fillPolygon(polygon);
			}
		}
		g2.setStroke(new BasicStroke(1));
	}

}