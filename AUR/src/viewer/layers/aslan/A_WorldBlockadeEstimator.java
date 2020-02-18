package viewer.layers.aslan;

import AUR.util.aslan.AURBuildingCollapseEstimator;
import AUR.util.aslan.AURWorldCollapseEstimator;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Amir Aslan Aslani
 */
public class A_WorldBlockadeEstimator extends K_ViewerLayer {
        
        @Override
        public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
                g2.setFont(new Font("Arial", 0, 9));
                g2.setColor(Color.white);
                AURWorldCollapseEstimator wce = new AURWorldCollapseEstimator(wsg.wi.getEntitiesOfType(StandardEntityURN.BUILDING));
                for(StandardEntity se : wsg.wi.getEntitiesOfType(StandardEntityURN.BUILDING)){
                        if(wce.map.containsKey(se.getID())){
                                AURBuildingCollapseEstimator get = wce.map.get(se.getID());
                                g2.drawString(
                                        String.valueOf(get.estimatedDamage),
                                        kst.xToScreen(
                                                ((Building)se).getX()
                                        ),
                                        kst.yToScreen(
                                                ((Building)se).getY()
                                        )
                                );
                        }
                }
        }
        
}
