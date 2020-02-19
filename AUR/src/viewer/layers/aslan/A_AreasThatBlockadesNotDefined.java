package viewer.layers.aslan;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.Color;
import java.awt.Graphics2D;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Amir Aslan Aslani - Mar 2018
 */
public class A_AreasThatBlockadesNotDefined extends K_ViewerLayer {

        @Override
        public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
                g2.setColor(new Color(100, 0, 0, 100));

                for (AURAreaGraph ag : wsg.areas.values()) {
                        if(ag.isRoad() && ! ag.area.isBlockadesDefined()){
                                g2.fill(kst.getTransformedPolygon(ag.polygon));
                        }
                }
        }
}
