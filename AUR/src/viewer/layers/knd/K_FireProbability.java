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

public class K_FireProbability extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		g2.setColor(Color.RED);
		for (AURAreaGraph ag : wsg.areas.values()) {
			if (ag.isBuilding() == true) {
				
				g2.setColor(new Color(255, 0, 0, (int) (Math.min(255, ag.getBuilding().fireSimBuilding.fireProbability * 255))));
				
				Polygon polygon = kst.getTransformedPolygon(ag.area.getShape());
				g2.fillPolygon(polygon);
			}

		}
	}

	@Override
	public String getString(AURWorldGraph wsg, AURAreaGraph selected_ag) {
		if(selected_ag == null || selected_ag.isBuilding() == false) {
			return null;
		}
		
		return selected_ag.getBuilding().fireSimBuilding.fireProbability + "\n";
	}
	
	

}