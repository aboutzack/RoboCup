package viewer.layers.AmboLayers;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import rescuecore2.standard.entities.Human;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;

/**
 * Created by armanaxh on 2018.
 */

public class Transport extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        g2.setStroke(new BasicStroke(4));
        g2.setColor(new Color(255, 255, 255, 250));

        int r = 5;
        if(wsg.ai.someoneOnBoard() != null) {
            Human m = (Human)wsg.ai.me();
            if (m.isXDefined() && m.isYDefined()) {
                g2.fillOval(kst.xToScreen(m.getX()) - r, kst.yToScreen(m.getY()) - r, 2 * r + 1 , 2 * r + 1);
                g2.setColor(new Color(36, 255, 45, 200));
                g2.drawOval(kst.xToScreen(m.getX()) - r, kst.yToScreen(m.getY()) - r, 2 * r -1, 2 * r -1);


            }

        }


        g2.setStroke(new BasicStroke(1));
    }

}