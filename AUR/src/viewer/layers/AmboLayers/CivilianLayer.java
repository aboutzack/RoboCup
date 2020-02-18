package viewer.layers.AmboLayers;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import adf.agent.info.WorldInfo;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;

/**
 * Created by armanaxh on 12/20/17.
 * 
 */

public class CivilianLayer extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {

		WorldInfo wi = wsg.wi;
		g2.setStroke(new BasicStroke(1));
		for (StandardEntity e : wi.getAllEntities()) {
			if (e.getStandardURN().equals(StandardEntityURN.CIVILIAN)) {
				Civilian c = (Civilian) e;
				if (c.isXDefined() == false || c.isYDefined() == false || c.isHPDefined() == false) {
					continue;
				}
				int r = 5;
				if(c.isHPDefined()) {
					if(c.getHP() == 0 ){
						g2.setColor(new Color(87, 34, 7));
					}else {
						g2.setColor(new Color(0, Math.min((int)(255 * ( 1D*c.getHP() / 10000)), 255), 0));
					}
				}else{
					g2.setColor(Color.green);
				}
				g2.fillOval(
						kst.xToScreen(c.getX()) - r ,
						kst.yToScreen(c.getY()) - r ,
						(int) (2 * r ),//* kst.zoom
						(int) (2 * r )//* kst.zoom
				);

			}
		}
	}

}
