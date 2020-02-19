package viewer.layers.aslan;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import rescuecore2.standard.entities.Area;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Amir Aslan Aslani - Mar 2018
 */
public class A_SelectedArea extends K_ViewerLayer {

        @Override
        public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
                if (selected_ag == null || !(selected_ag.area instanceof Area)) {
                        return;
                }

                g2.setStroke(new BasicStroke(2));
                g2.setFont(new Font("Arial", 0, 9));
                g2.setColor(new Color(0, 255, 0, 50));

                g2.fill(kst.getTransformedPolygon(selected_ag.polygon));
        }
}
