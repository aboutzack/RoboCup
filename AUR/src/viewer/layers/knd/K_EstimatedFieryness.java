package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.Color;
import java.awt.Graphics2D;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2017
 */

public class K_EstimatedFieryness extends K_ViewerLayer {
	
	private final static Color colors[] = new Color[] {
		new Color(176, 176,  56, 80),	// HEATING
		new Color(204, 122,  50, 80),	// BURNING
		new Color(160,  52,  52, 80),	// INFERNO
		new Color(50, 120, 130, 80),	// WATER_DAMAGE
		new Color(100, 140, 210, 80),	// MINOR_DAMAGE
		new Color(100, 70, 190, 80),	// MODERATE_DAMAGE
		new Color(80, 60, 140, 80),	// SEVERE_DAMAGE
		new Color(0, 0, 0, 200)		// BURNT_OUT
	};
	
	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		for(AURAreaGraph ag : wsg.areas.values()) {
			if(ag.isBuilding() == true) {
				int f = ag.getBuilding().fireSimBuilding.getEstimatedFieryness();
				if(f > 0) {
					g2.setColor(colors[f - 1]);
					g2.fill(kst.getTransformedPolygon(ag.polygon));
				}
			}
		}
	}
	
}
