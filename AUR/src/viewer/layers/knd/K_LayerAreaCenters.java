package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;
import viewer.K_Viewer;

/**
 *
 * @author Alireza Kandeh - 2017
 */

public class K_LayerAreaCenters extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        for(AURAreaGraph ag : wsg.areas.values()) {
            if(true) {
                double x = ag.getX();
                double y = ag.getY();

                g2.setColor(Color.BLACK);
                
                g2.fillOval(kst.xToScreen(x) - 2, kst.yToScreen(y) - 2, 4, 4);
            }

        }
    }
    
}