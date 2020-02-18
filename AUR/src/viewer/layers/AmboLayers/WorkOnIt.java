package viewer.layers.AmboLayers;

import AUR.util.ambulance.Information.BuildingInfo;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import rescuecore2.standard.entities.Human;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;

/**
 * Created by armanaxh on 2018.
 */

public class WorkOnIt extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        g2.setStroke(new BasicStroke(4));
        g2.setColor(new Color(255, 251, 13, 230));

        int r = 5;
        if(wsg.rescueInfo != null) {
            if (wsg.rescueInfo.ambo != null) {

                Human workOnIt = wsg.rescueInfo.ambo.workOnIt;
                BuildingInfo searchTarget = wsg.rescueInfo.ambo.searchTarget;
                if (workOnIt != null) {
                    if (workOnIt.isXDefined() && workOnIt.isYDefined()) {
                        g2.drawOval(kst.xToScreen(workOnIt.getX()) - r, kst.yToScreen(workOnIt.getY()) - r, 2 * r + 1, 2 * r + 1);
                        g2.setColor(new Color(255, 251, 13, 163));
                        g2.fillOval(kst.xToScreen(workOnIt.getX()) - r, kst.yToScreen(workOnIt.getY()) - r, 2 * r, 2 * r);
                    }
                }
                if (searchTarget != null){
                    if (workOnIt == null || (workOnIt != null && !workOnIt.getPosition().equals(searchTarget.me.getID()) ) ) {
                        Polygon polygon = kst.getTransformedPolygon(searchTarget.me.getShape());
                        g2.setColor(new Color(255, 251, 13, 230));
                        g2.drawPolygon(polygon);
                        g2.setColor(new Color(255, 251, 13, 163));
                        g2.fill(polygon);
                    }
                 }
            }
        }
        g2.setStroke(new BasicStroke(1));
    }

}