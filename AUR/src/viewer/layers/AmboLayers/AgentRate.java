package viewer.layers.AmboLayers;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import rescuecore2.standard.entities.Human;
import rescuecore2.worldmodel.EntityID;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;
import java.util.Collection;

/**
 *
 * @author armanaxh  - 2018
 */

public class AgentRate extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        g2.setStroke(new BasicStroke(4));
        g2.setFont(new Font("Arial", 0, 13));
        g2.setColor(Color.GREEN);
        if(wsg.rescueInfo != null) {
            Collection<EntityID> rateS = wsg.rescueInfo.agentsRate.keySet();
            for (EntityID id : rateS) {
                String rate = "" + (((int) (wsg.rescueInfo.agentsRate.get(id) * 100))) / 100D;
                Human h = (Human) wsg.wi.getEntity(id);
                if (h.isXDefined() && h.isYDefined()) {
                    if (h.isXDefined() && h.isYDefined()) {
                        g2.drawString(rate, kst.xToScreen(h.getX() - 5), kst.yToScreen(h.getY()) - 5);
                    }
                }
            }

        }
        g2.setStroke(new BasicStroke(1));
    }

}
