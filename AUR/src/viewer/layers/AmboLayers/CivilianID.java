package viewer.layers.AmboLayers;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import adf.agent.info.WorldInfo;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;

/**
 * Created by armanaxh on 12/20/17.
 *
 */

public class CivilianID extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {

        WorldInfo wi = wsg.wi;
        g2.setStroke(new BasicStroke(2));
        g2.setColor(new Color(255, 17, 167, 255));
        for (StandardEntity e : wi.getAllEntities()) {
            if (e.getStandardURN().equals(StandardEntityURN.CIVILIAN)) {
                Civilian c = (Civilian) e;
                if (c.isXDefined() == false || c.isYDefined() == false) {
                    continue;
                }
                g2.drawString(c.getID()+"", kst.xToScreen(c.getX() - 5), kst.yToScreen(c.getY()) + 5 );

            }
        }
    }

}
