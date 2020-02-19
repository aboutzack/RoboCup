package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Alireza Kandeh - 2017
 */

public class K_LayerAllBlockades extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        
        g2.setColor(new Color(0, 0, 0, 150));
        g2.setStroke(new BasicStroke(3));
		
        for(AURAreaGraph ag : wsg.areas.values()) {
            
            for(Blockade b : wsg.wi.getBlockades(ag.area)) {
                Polygon polygon = kst.getTransformedPolygon(b.getShape());
                g2.drawPolygon(polygon);
                g2.fillPolygon(polygon);
            }
            

        }
    }
    
}