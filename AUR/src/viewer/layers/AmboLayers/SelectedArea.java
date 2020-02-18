package viewer.layers.AmboLayers;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import rescuecore2.standard.entities.*;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;

/**
 * Created by armanaxh on 12/20/17.
 */

public class SelectedArea extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        g2.setStroke(new BasicStroke(2));
        g2.setColor(Color.pink);
        if(selected_ag != null){
            Area area = selected_ag.area;
            Polygon polygon = kst.getTransformedPolygon(area.getShape());
            g2.drawPolygon(polygon);
        }
        g2.setStroke(new BasicStroke(1));
    }

}
