package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURConstants;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class K_FireZoneBorderThreshold extends K_ViewerLayer {
	
	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		if (selected_ag == null) {
			return;
		}
		g2.setStroke(new BasicStroke(2));
		g2.setColor(new Color(255, 50, 150, 128));
		g2.draw(kst.getTransformedRectangle(selected_ag.getOffsettedBounds(AURConstants.Misc.FIRE_ZONE_BORDER_INTERSECT_THRESHOLD)));
	}

}