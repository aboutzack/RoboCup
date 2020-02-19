package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURNode;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import rescuecore2.worldmodel.EntityID;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class K_MapInfo extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
	}

	@Override
	public String getString(AURWorldGraph wsg, AURAreaGraph selected_ag) {
		String result = "";
		
		result += "MapDiemeter: " + wsg.mapDiameter;
		result += "\n";
		
		result += "MapSize: ";
		
		if(wsg.isSmallMap()) {
			result += "small";
		}
		if(wsg.isMediumMap()) {
			result += "medium";
		}
		if(wsg.isBigMap()) {
			result += "big";
		}
		
		result += "\n";
		
		return result;
	}

}
