package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURBuilding;
import AUR.util.knd.AURConstants;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class K_CloseBuildings extends K_ViewerLayer{
	
	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		if (selected_ag == null) {
			return;
		}
		
		g2.setColor(new Color(255, 120, 50, 128));
		
		for(AURBuilding cb : selected_ag.getCloseBuildings()) {
			g2.fill(kst.getTransformedPolygon(cb.ag.polygon));
		}
		
		g2.setColor(Color.green);
		g2.setStroke(new BasicStroke(1));
		
		Rectangle2D bounds = selected_ag.polygon.getBounds();
		int a = AURConstants.Misc.CLOSE_BUILDING_THRESHOLD;
		
		bounds = new Rectangle(
			(int) bounds.getMinX() - a,
			(int) bounds.getMinY() - a,
			(int) bounds.getWidth() + 2 * a,
			(int) bounds.getHeight() + 2 * a
		);
		
		g2.draw(kst.getTransformedRectangle(bounds));
		
		
		
	}

}