package viewer.layers.AmboLayers;

import AUR.util.ambulance.Information.BuildingInfo;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;
import java.util.Collection;

/**
 *
 * @author armanaxh  - 2017
 */

public class BuildingRate extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        g2.setStroke(new BasicStroke(3));
        g2.setFont(new Font("Arial", 0, 13));
        g2.setColor(Color.black);
        if(wsg.rescueInfo != null) {
            Collection<BuildingInfo> ZJUdeathTime = wsg.rescueInfo.buildingsInfo.values();
            for (BuildingInfo bInfo : ZJUdeathTime) {
                String rate = "" + (((int) (bInfo.rate * 100))) / 100D;

                if (bInfo.me.isXDefined() && bInfo.me.isYDefined()) {
                    if (bInfo.me.isXDefined() && bInfo.me.isYDefined()) {
                        g2.drawString(rate, kst.xToScreen(bInfo.me.getX() - 5), kst.yToScreen(bInfo.me.getY()) - 5);
                    }
                }
            }

            g2.setStroke(new BasicStroke(1));
        }
    }

}
