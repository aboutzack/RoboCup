package viewer.layers.AmboLayers;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;

/**
 * Created by armanaxh on 12/22/17.
 */
public class SightPolygonLayer extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
//        g2.setStroke(new BasicStroke(1));
//        g2.setColor(Color.cyan);
//        if (selected_ag != null) {
//            if (selected_ag.isBuilding()) {
//                SightPolygon vPoly = wsg.sightPolygonAllocator.getBuldingSightPolygon((Building) selected_ag.area);
//                if(vPoly != null) {
//                    for (Polygon poly : vPoly.getPolygons()) {
//                        g2.setColor(Color.DARK_GRAY);
//                        g2.drawPolygon(kst.getTransformedPolygon(poly));
//                        g2.setColor(new Color(50, 255, 170, 100));
//                        g2.fillPolygon(kst.getTransformedPolygon(poly));
//                    }
//                }
//            }
//            g2.setColor(Color.black);
//
//        }
    }
}
