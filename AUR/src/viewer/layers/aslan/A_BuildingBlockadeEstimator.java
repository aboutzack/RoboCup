package viewer.layers.aslan;

import AUR.util.aslan.AURBuildingCollapseEstimator;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;
import org.uncommons.maths.random.MersenneTwisterRNG;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Amir Aslan Aslani - Mar 2018
 */
public class A_BuildingBlockadeEstimator extends K_ViewerLayer {
        
        Random rnd = new MersenneTwisterRNG();

        @Override
        public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
                if(selected_ag != null && selected_ag.isBuilding()){
                        AURBuildingCollapseEstimator ce = new AURBuildingCollapseEstimator((Building) selected_ag.area, rnd);
                        double d = ce.d();
                        g2.setColor(new Color(0, 0, 150, 70));
                        for(Edge e : selected_ag.area.getEdges()){
                                g2.fillOval(
                                        kst.xToScreen(e.getEndX() - d),
                                        kst.yToScreen(e.getEndY() + d),
                                        (int) (kst.zoom * d*2),
                                        (int) (kst.zoom * d*2)
                                );
                                g2.fillOval(
                                        kst.xToScreen(e.getStartX() - d),
                                        kst.yToScreen(e.getStartY() + d),
                                        (int) (kst.zoom * d * 2),
                                        (int) (kst.zoom * d * 2)
                                );
                        }
                }
        }
        
        @Override
	public String getString(AURWorldGraph wsg, AURAreaGraph selected_ag) {
                String result = "\n";
		if(selected_ag == null || ! selected_ag.isBuilding()) {
		    return "";
		}
                AURBuildingCollapseEstimator ce = new AURBuildingCollapseEstimator((Building) selected_ag.area, rnd);
                result += "D: " + ce.d() + "\n";
                result += "getRemainingFloors: " + ce.getRemainingFloors() + "\n";
                result += "getDamage: " + ce.getDamage() + "\n";
                result += "getCollapsedRatio: " + ce.getCollapsedRatio() + "\n";
                result += "extent: " + ce.extent.nextValue() + " - " + ce.extent.nextValue() + "\n";
                return result;
	}
}
