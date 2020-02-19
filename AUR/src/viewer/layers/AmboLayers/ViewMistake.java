package viewer.layers.AmboLayers;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.standard.entities.Edge;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;
import java.util.ArrayList;

/**
 * Created by armanaxh on 2018.
 */

public class ViewMistake extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        g2.setStroke(new BasicStroke(3));
        g2.setColor(new Color(255, 32, 156, 230));
        if(wsg.rescueInfo != null) {
            int r = 5;
            if (wsg.rescueInfo.testLine != null) {
                ArrayList<Line2D> lines = wsg.rescueInfo.testLine;
                for (Line2D line : lines) {
                    g2.drawLine(kst.xToScreen(line.getOrigin().getX()), kst.yToScreen(line.getOrigin().getY()), kst.xToScreen(line.getEndPoint().getX()), kst.yToScreen(line.getEndPoint().getY()));
                }
            }
            g2.setColor(new Color(255, 46, 46, 230));
            if (wsg.rescueInfo.areasInter != null) {
                ArrayList<Edge> lines = wsg.rescueInfo.areasInter;
                for (Edge line : lines) {
                    g2.drawLine(kst.xToScreen(line.getStartX()), kst.yToScreen(line.getStartY()), kst.xToScreen(line.getEnd().getX()), kst.yToScreen(line.getEnd().getY()));
                }
            }
            g2.setStroke(new BasicStroke(1));
        }
    }

}