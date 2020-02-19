package viewer.layers.AmboLayers;

import AUR.util.ambulance.Information.CivilianInfo;
import AUR.util.ambulance.Information.RefugeInfo;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import rescuecore2.standard.entities.Area;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;

/**
 * Created by armanaxh on 12/20/17.
 */

public class BestRefugeForCivilian extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        g2.setStroke(new BasicStroke(2));
        g2.setColor(new Color(19, 147, 255, 255));
        int ttttt = 0;
        if(selected_ag != null){
            Area area = selected_ag.area;
            if(wsg.rescueInfo != null) {
                for(CivilianInfo ci : wsg.rescueInfo.civiliansInfo.values()) {
                    if (ci.getPosition().equals(area.getID())) {
                        RefugeInfo refuge = ci.bestRefuge;
                        if (refuge != null) {
                            Polygon polygon = kst.getTransformedPolygon(refuge.refuge.getShape());
                            g2.drawString(ci.getID() + "", kst.xToScreen(refuge.refuge.getX() - 5), kst.yToScreen(refuge.refuge.getY()) + 5 + ttttt);
                            g2.drawPolygon(polygon);
                            ttttt += 10;
                        }
                    }
                }
            }
       }


        g2.setStroke(new BasicStroke(1));
//

    }

}
