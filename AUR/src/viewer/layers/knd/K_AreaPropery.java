package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.Graphics2D;
import rescuecore2.standard.entities.Building;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class K_AreaPropery extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
	}

	@Override
	public String getString(AURWorldGraph wsg, AURAreaGraph selected_ag) {
		if(selected_ag == null) {
		    return null;
		}
		String result = "";

		result += "ID:\t" + selected_ag.area.getID().getValue();
		result += "\n";

		if(selected_ag.isBuilding()) {
			Building b = (Building ) (selected_ag.area);

			result += "Temperature:\t" + (b.isTemperatureDefined() ? b.getTemperature() : "undefined");
			result += "\n";

			result += "Fieryness:\t" + (b.isFierynessDefined() ? b.getFieryness() : "undefined");
			result += "\n";

			result += "Floors:\t" + (b.isFloorsDefined() ? b.getFloors() : "undefined");
			result += "\n";

			result += "BuildingCode:\t" + (b.isBuildingCodeDefined() ? b.getBuildingCode() : "undefined");
			result += "\n";

			result += "Brokenness:\t" + (b.isBrokennessDefined() ? b.getBrokenness() : "undefined");
			result += "\n";
		}

		result += "X:\t" + (selected_ag.area.isXDefined() ? selected_ag.area.getX() : "undefined");
		result += "\n";

		result += "Y:\t" + (selected_ag.area.isYDefined() ? selected_ag.area.getY() : "undefined");
		result += "\n";

		result += "Perimeter:\t" + selected_ag.perimeter;
		result += "\n";
		
		result += "GroundArea:\t" + selected_ag.goundArea;
		result += "\n";

		return result;
	}
    
}
