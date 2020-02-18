package viewer.layers.AmboLayers;

import AUR.util.ambulance.Information.CivilianInfo;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;
import java.util.Collection;

/**
 * Created by armanaxh on 2018.
 */

public class RepresentCanNotRescueCivilian extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        g2.setStroke(new BasicStroke(4));
        g2.setColor(new Color(255, 0, 0 , 90 ));

        int r = 5;
        if(wsg.rescueInfo != null) {
            Collection<CivilianInfo> canNotRescues = wsg.rescueInfo.canNotRescueCivilian;
            for (CivilianInfo ciInfo : canNotRescues) {

                if (ciInfo.me.isXDefined() && ciInfo.me.isYDefined()) {
                    g2.drawRect(kst.xToScreen(ciInfo.me.getX()) - r, kst.yToScreen(ciInfo.me.getY()) - r, 2 * r, 2 * r);
                    g2.fillRect(kst.xToScreen(ciInfo.me.getX()) - r, kst.yToScreen(ciInfo.me.getY()) - r, 2 * r, 2 * r);
                }

            }
        }
        g2.setStroke(new BasicStroke(1));
    }

}