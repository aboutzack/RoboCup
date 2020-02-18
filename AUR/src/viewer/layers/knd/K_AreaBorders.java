package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURAreaGrid;
import AUR.util.knd.AURBorder;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import rescuecore2.standard.entities.Blockade;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2017 & 2018
 */

public class K_AreaBorders extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		if (selected_ag == null) {
			return;
		}
		g2.setColor(Color.CYAN);
		g2.setStroke(new BasicStroke(2));
		for (AURBorder border : selected_ag.borders) {
			kst.drawTransformedLine(g2, border.Ax, border.Ay, border.Bx, border.By);
		}
	}
	
	@Override
	public String getString(AURWorldGraph wsg, AURAreaGraph selected_ag) {
		if (selected_ag == null) {
			return null;
		}
		String result = "";
		for (AURBorder border : selected_ag.borders) {
			result += "(" + border.Ax + ", " + border.Ay + ") to (" + border.Bx + ", " + border.By + ")";
			result += "\n";
		}
		return result;
	}
	
}