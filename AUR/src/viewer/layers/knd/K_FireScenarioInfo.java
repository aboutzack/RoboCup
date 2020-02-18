package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.Graphics2D;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class K_FireScenarioInfo extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
	}

	@Override
	public String getString(AURWorldGraph wsg, AURAreaGraph selected_ag) {
		String result = "";

		result += "ExtinguishMaxDistance:\t" + wsg.si.getFireExtinguishMaxDistance();
		result += "\n";

		result += "TankMaximum:\t\t" + wsg.si.getFireTankMaximum();
		result += "\n";

		result += "ExtinguishMaxSum:\t" + wsg.si.getFireExtinguishMaxSum();
		result += "\n";

		result += "TankRefillRate:\t\t" + wsg.si.getFireTankRefillRate();
		result += "\n";

		result += "TankRefillHydrantRate\t" + wsg.si.getFireTankRefillHydrantRate();
		result += "\n";
			
		return result;
	}
	
}