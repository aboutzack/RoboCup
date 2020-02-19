package viewer.layers.aslan;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import rescuecore2.standard.entities.Blockade;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Amir Aslan Aslani - Feb 2018
 */
public class A_BlockadesEntityID extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        
        g2.setStroke(new BasicStroke(2));
        g2.setFont(new Font("Arial", 0, 9));
        g2.setColor(Color.orange);
        
        for(AURAreaGraph ag : wsg.areas.values()) {
            for(Blockade b : wsg.wi.getBlockades(ag.area)) {
                String id = "-1";
                id = b.getID().toString();
                g2.drawString(id, kst.xToScreen(b.getX()),  kst.yToScreen(b.getY()));
            }
        }
            
        
    }
}


