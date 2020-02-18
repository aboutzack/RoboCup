package viewer.layers.AmboLayers;

import AUR.util.ambulance.Information.CivilianInfo;
import AUR.util.ambulance.Information.RescueInfo;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;
import java.util.Collection;

/**
 * Created by armanaxh on 2018.
 */

public class WorstCaseDeathTime extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        g2.setStroke(new BasicStroke(4));
        g2.setFont(new Font("Arial", 0, 13));
        g2.setColor(Color.blue);
        if(wsg.rescueInfo != null) {
            Collection<CivilianInfo> civilianInfos = wsg.rescueInfo.civiliansInfo.values();
            for (CivilianInfo ciInfo : civilianInfos) {
                String deathTime = "" + ciInfo.getWorstCaseDeathTime();

                if (ciInfo.me.isXDefined() && ciInfo.me.isYDefined()) {
                    g2.drawString(deathTime, kst.xToScreen(ciInfo.me.getX() - 5), kst.yToScreen(ciInfo.me.getY()) - 5);
                }
            }
        }
        g2.setStroke(new BasicStroke(1));
    }


    public static String getDeathTimeInWorstCase(RescueInfo rescueInfo, int hp, int dmg) {

        int worstDmg = dmg - (rescueInfo.losDamge/2);
        int worstHp = hp + (rescueInfo.losHp/2);


        return rescueInfo.wsg.ai.getTime()  + " " + worstHp + " /" + worstDmg;

    }

}