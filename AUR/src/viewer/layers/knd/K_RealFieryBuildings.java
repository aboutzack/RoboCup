package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.Color;
import java.awt.Graphics2D;
import rescuecore2.standard.entities.Building;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2017
 */

public class K_RealFieryBuildings extends K_ViewerLayer {
	
	private final static Color colors[] = new Color[] {
		new Color(176, 176,  56, 128),	// HEATING
		new Color(204, 122,  50, 128),	// BURNING
		new Color(160,  52,  52, 128),	// INFERNO
		new Color(50, 120, 130, 128),	// WATER_DAMAGE
		new Color(100, 140, 210, 128),	// MINOR_DAMAGE
		new Color(100, 70, 190, 128),	// MODERATE_DAMAGE
		new Color(80, 60, 140, 128),	// SEVERE_DAMAGE
		new Color(0, 0, 0, 255)		// BURNT_OUT
	};
	
	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		for(AURAreaGraph ag : wsg.areas.values()) {
			if(ag.isBuilding() == true) {
				Building b = (Building) ag.area;
				if(b.isFierynessDefined() == false) {
					continue;
				}
				int f = b.getFieryness();
				if(f > 0) {
					g2.setColor(colors[f - 1]);
					g2.fill(kst.getTransformedPolygon(ag.polygon));
				}
			}
		}
	}
	
}
