package viewer.layers.AmboLayers;

import AUR.util.ambulance.Information.CivilianInfo;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;
import java.util.Collection;

/**
 *
 * @author armanaxh  - 2018
 */

public class CivilianSaveTimeLayer extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        g2.setStroke(new BasicStroke(4));
        g2.setFont(new Font("Arial", 0, 13));
        g2.setColor(new Color(170, 50, 200));
        if(wsg.rescueInfo != null) {
            Collection<CivilianInfo> ZJUdeathTime = wsg.rescueInfo.civiliansInfo.values();
            for (CivilianInfo ciInfo : ZJUdeathTime) {
                String saveTime = "" + ciInfo.saveTime;

                if (ciInfo.me.isXDefined() && ciInfo.me.isYDefined()) {
                    if (ciInfo.me.isXDefined() && ciInfo.me.isYDefined()) {
                        g2.drawString(saveTime, kst.xToScreen(ciInfo.me.getX() - 5), kst.yToScreen(ciInfo.me.getY()) + 14);
                    }
                }
            }
        }
        g2.setStroke(new BasicStroke(1));
    }
}
