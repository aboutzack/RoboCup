package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURSightAreaPolygon;
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

public class K_BuildingSightAreaPolygon extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		if(selected_ag == null || selected_ag.isBuilding() == false) {
			return;
		}
		Polygon sightAreaPolygon = AURSightAreaPolygon.getAndPaint(selected_ag.getBuilding(), g2, kst);
		g2.setColor(new Color(0, 255, 255, 100));
		g2.fillPolygon(kst.getTransformedPolygon(sightAreaPolygon));
		
		g2.setColor(Color.black);
		for(int i = 0; i < sightAreaPolygon.npoints; i++) {
			g2.fillRect(
				kst.xToScreen(sightAreaPolygon.xpoints[i] - 3),
				kst.yToScreen(sightAreaPolygon.ypoints[i] - 3),
				6,
				6
			);
		}
		
	}
    
}