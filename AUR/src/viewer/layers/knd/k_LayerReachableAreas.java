package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURFireSimBuilding;
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

public class k_LayerReachableAreas extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		g2.setStroke(new BasicStroke(2));
		wsg.KStar(wsg.ai.getPosition());
		for (AURAreaGraph ag : wsg.areas.values()) {
			if (ag.lastDijkstraEntranceNode == null) {
				g2.setColor(Color.red);
			} else {
				g2.setColor(Color.GREEN);
			}

			Polygon polygon = kst.getTransformedPolygon(ag.area.getShape());
			g2.fillPolygon(polygon);

		}
		g2.setStroke(new BasicStroke(1));
	}

	@Override
	public String getString(AURWorldGraph wsg, AURAreaGraph selected_ag) {

		String result = "";
		
		wsg.KStar(wsg.ai.getPosition());
		
		int count = 0;
		
		for(AURAreaGraph ag : wsg.areas.values()) {
			if(ag.lastDijkstraEntranceNode == null) {
				count++;
			}
		}
		
		result += "unreachables: \t" + count;
		result += "\n";

		return result;
	}
    
}
