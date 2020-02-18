package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURAreaGrid;
import AUR.util.knd.AURWorldGraph;
import java.awt.Graphics2D;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2018
 */


public class K_AreaGrid extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		if (selected_ag == null) {
			return;
		}
		AURAreaGrid areaGrid = wsg.instanceAreaGrid;
		areaGrid.init(selected_ag);

		areaGrid.paint(g2, kst);

	}

}
