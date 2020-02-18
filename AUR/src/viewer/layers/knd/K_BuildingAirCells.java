package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph; 
import java.awt.Color;
import java.awt.Graphics2D;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class K_BuildingAirCells extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		if (selected_ag == null || selected_ag.isBuilding() == false) {
			return;
		}
		for (int cell[] : selected_ag.getBuilding().fireSimBuilding.getAirCells()) {
			int xy[] = wsg.fireSimulator.airCells.getCell_xy(cell[0], cell[1]);
			g2.setColor(new Color(255, 255, 0, 10 + cell[2] * 2));
			g2.fill(kst.getTransformedRectangle(xy[0], xy[1], wsg.fireSimulator.airCells.getCellSize(), wsg.fireSimulator.airCells.getCellSize()));
		}
	}

}
