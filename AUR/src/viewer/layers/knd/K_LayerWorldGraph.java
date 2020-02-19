package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURBorder;
import AUR.util.knd.AUREdge;
import AUR.util.knd.AURNode;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import javax.swing.border.Border;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2017
 */

public class K_LayerWorldGraph extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        
        g2.setColor(Color.BLUE);
//        g2.setStroke(new BasicStroke(2));
        
        wsg.KStar(wsg.ai.getPosition());
        
        
        for(AURAreaGraph ag : wsg.areas.values()) {
            for(AURBorder border : ag.borders) {
//                g2.drawLine(
//                    kst.xToScreen(border.Ax), kst.yToScreen(border.Ay),
//                    kst.xToScreen(border.Bx), kst.yToScreen(border.By)
//                );
                
                for(AURNode node : border.nodes) {
                    g2.fillOval(kst.xToScreen(node.x) - 3, kst.yToScreen(node.y) - 3, 6, 6);
                }
                
            }
        }
		
        g2.setStroke(new BasicStroke(1));
        g2.setColor(Color.green);
        
        
        
        for(AURAreaGraph ag : wsg.areas.values()) {
            for(AURBorder border : ag.borders) {
                
                for(AURNode node : border.nodes) {
                    for(AUREdge edge : node.edges) {
                        g2.drawLine(
                            kst.xToScreen(edge.A.x), kst.yToScreen(edge.A.y),
                            kst.xToScreen(edge.B.x), kst.yToScreen(edge.B.y)
                        );
                    }
                }
            }
        }
        
        ArrayList<AURNode> nodes = wsg.getAreaGraph(wsg.ai.getPosition()).getReachabeEdgeNodes(wsg.ai.getX(), wsg.ai.getY());
        
        for(AURNode node : nodes) {
            g2.drawLine(
                kst.xToScreen(wsg.ai.getX()), kst.yToScreen(wsg.ai.getY()),
                kst.xToScreen(node.x), kst.yToScreen(node.y)
            );
        }
        
        g2.setStroke(new BasicStroke(1));
    }
    
}