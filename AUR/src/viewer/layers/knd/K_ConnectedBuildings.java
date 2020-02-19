package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURBuildingConnection;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import rescuecore2.worldmodel.EntityID;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class K_ConnectedBuildings extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		
		if(wsg.fireSimulator.isPrecomputedConnections == false) {
			g2.setFont(new Font("Arial", Font.BOLD, 20));
			g2.setColor(Color.red);
			g2.drawString("precompute required!", 5, 50);
			//return;
		}
		
		if(selected_ag == null || selected_ag.isBuilding() == false) {
			return;
		}
		
//		selected_ag.getBuilding().fireSimBuilding.calcConnectionsAndPaint(g2, kst);
		
		if(selected_ag.getBuilding().fireSimBuilding.connections == null) {
			return;
		}
		
		g2.setColor(Color.red);
		g2.setStroke(new BasicStroke(3));
		
		for(AURBuildingConnection c : selected_ag.getBuilding().fireSimBuilding.connections) {
			AURAreaGraph toAg = wsg.getAreaGraph(new EntityID(c.toID));
			double w = c.weight;
			w = Math.pow(w, 0.5);
			g2.setColor(new Color(255, 0, 255, (int) (w * 255)));
			kst.drawTransformedLine(g2, selected_ag.getX(), selected_ag.getY(), toAg.getX(), toAg.getY());
		}
	}
    
}
