package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import rescuecore2.standard.entities.Edge;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2017
 */

public class K_LayerWalls extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        g2.setColor(new Color(0, 0, 0, 128));
        g2.setStroke(new BasicStroke(2));
		
		for(AURAreaGraph ag : wsg.areas.values()) {
			if(ag.isBuilding() == true) {
				for(Edge e : ag.area.getEdges()) {
					if(e.isPassable() == false) {
						g2.drawLine(
							kst.xToScreen(e.getStartX()), kst.yToScreen(e.getStartY()),
							kst.xToScreen(e.getEndX()), kst.yToScreen(e.getEndY())
						);
					}
				}
			}
		}
		
        g2.setStroke(new BasicStroke(1));
    }
    
}