package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import rescuecore2.standard.entities.Edge;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2017 & 2018
 */

public class K_AreaVertices extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		if (selected_ag == null) {
			return;
		}
		Polygon polygon = (Polygon) selected_ag.area.getShape();
		for (int i = 0; i < polygon.npoints; i++) {
			double x = polygon.xpoints[i];
			double y = polygon.ypoints[i];

			g2.setColor(Color.BLACK);
			g2.fillOval(kst.xToScreen(x) - 3, kst.yToScreen(y) - 3, 6, 6);
		}
	}
	
	@Override
	public String getString(AURWorldGraph wsg, AURAreaGraph selected_ag) {
		if(selected_ag == null) {
			return null;
		}
		String result = "";
//		for(Edge edge : selected_ag.area.getEdges()) {
//			result += edge.getStartX() + ", " + edge.getStartY();
//			result += "\n";
//		}
		for(int i = 0; i < selected_ag.polygon.npoints; i++) {
			result += selected_ag.polygon.xpoints[i] + ", " + selected_ag.polygon.ypoints[i];
			result += "\n";
		}
		return result;
	}
	
}
