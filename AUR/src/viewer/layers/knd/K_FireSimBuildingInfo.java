package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURFireSimBuilding;
import AUR.util.knd.AURWorldGraph;
import java.awt.Graphics2D;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class K_FireSimBuildingInfo extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
	}

	@Override
	public String getString(AURWorldGraph wsg, AURAreaGraph selected_ag) {
		if(selected_ag == null) {
			return null;
		}
		String result = null;
		if(selected_ag.isBuilding()) {
			result = "";
			AURFireSimBuilding b = selected_ag.getBuilding().fireSimBuilding;
			result += "Estimated Temperature:\t" + b.getEstimatedTemperature();
			result += "\n";
			
			result += "Estimated Energy:\t" + b.getEstimatedEnergy();
			result += "\n";
			
			result += "Estimated Fieryness:\t" + b.getEstimatedFieryness();
			result += "\n";

			result += "ThermoCapacity:\t" + b.getThermoCapacity();
			result += "\n";
			
			result += "Capacity:\t\t" + b.getCapacity();
			result += "\n";
			
			result += "InitialFuel:\t\t" + b.getInitialFuel();
			result += "\n";
			
			result += "EstimatedFuel:\t\t" + b.getEstimatedFuel();
			result += "\n";
			
			result += "Consume:\t\t" + b.getConsum();
			result += "\n";
			
			result += "RadiationEnergy:\t" + b.getRadiationEnergy();
			result += "\n";
			
			result += "WaterNeeded:\t\t" + b.getWaterNeeded();
			result += "\n";
			
			result += "WasEverWatered:\t" + b.wasEverWatered();
			result += "\n";
			
			result += "WaterQuantity:\t\t" + b.getWaterQuantity();
			result += "\n";
			
			result += "Inflammable:\t\t" + b.inflammable();
			result += "\n";
			
			result += "Perimeter:\t\t" + b.getPerimeter();
			result += "\n";
			
			result += "GroundArea:\t\t" + b.getGroundArea();
			result += "\n";
			
			result += "Volume:\t\t" + b.getVolume();
			result += "\n";
			
			result += "TotalWallArea:\t\t" + b.getTotalWallArea();
			result += "\n";
			
			result += "LastRealTemperature:\t" + b.lastRealTemperature;
			result += "\n";
			
			result += "LastRealFieryness:\t" + b.lastRealFieryness;
			result += "\n";
			
			result += "EffectiveRadiation:\t" + b.getEffectiveRadiation();
			result += "\n";
			
//			result += "EffectiveRadiationDT:\t" + b.getEffectiveRadiationDT();
//			result += "\n";
		
			
			if(b.fireZone != null) {
				result += "g:\t" + b.fireZone.g();
				result += "\n";
				
				result += "need:\t" + b.fireZone.getAgentsNeeded();
				result += "\n";
				
				result += "percept time:\t" + b.fireZone.getPerceptTime();
				result += "\n";
			}
		}
		return result;
	}
	
}