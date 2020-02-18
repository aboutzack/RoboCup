package viewer.layers.AmboLayers;

import AUR.util.ambulance.Information.BuildingInfo;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import rescuecore2.standard.entities.Building;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;

/**
 *
 * @author armanaxh - 2018
 */

public class VisetedBuiliding extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        g2.setStroke(new BasicStroke(2));
        g2.setColor(new Color(36, 255, 45, 80));
        if(wsg.rescueInfo != null) {
            for (BuildingInfo building : wsg.rescueInfo.visitedList) {
                Building builing = building.me;
                Polygon polygon = kst.getTransformedPolygon(builing.getShape());
                g2.fillPolygon(polygon);
            }
        }
        g2.setStroke(new BasicStroke(1));
    }

}
