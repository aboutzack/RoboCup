package viewer.layers.AmboLayers;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;

/**
 *
 * @author armanaxh - 2018
 */

public class AgentClusterLayer extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        g2.setStroke(new BasicStroke(2));
        g2.setColor(new Color(10, 10, 255, 80));
        if(wsg.rescueInfo != null) {
            for (StandardEntity entity : wsg.rescueInfo.clusterEntity) {
                if (entity instanceof Building) {
                    Building builing = (Building) entity;
                    Polygon polygon = kst.getTransformedPolygon(builing.getShape());
                    g2.fillPolygon(polygon);
                }
            }
        }
        g2.setStroke(new BasicStroke(1));
    }

}
