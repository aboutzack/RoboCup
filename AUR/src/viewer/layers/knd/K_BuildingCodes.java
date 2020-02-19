package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import rescuecore2.standard.entities.Building;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class K_BuildingCodes extends K_ViewerLayer {

	private final static Color colors[] = new Color[] {
		new Color(168, 101, 9, 200),	// wood
		new Color(240, 240, 240, 200),	// steel
		new Color(190, 190, 190, 200)	// concrete
	};
	
	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		for(AURAreaGraph ag : wsg.areas.values()) {
			g2.setFont(new Font("Arial", 0, 9));
			if(ag.isBuilding() == true) {
				Polygon polygon = kst.getTransformedPolygon(ag.area.getShape());
				int code = ((Building) (ag.area)).getBuildingCode();
				g2.setColor(K_BuildingCodes.colors[code]);
				g2.fillPolygon(polygon);
				g2.setColor(Color.darkGray);
				g2.drawPolygon(polygon);
				g2.setColor(Color.BLACK);
				g2.drawString(code + "", kst.xToScreen(ag.getX()),  kst.yToScreen(ag.getY()));
			}
		}
	}
	
}