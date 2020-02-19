/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURBorder;
import AUR.util.knd.AUREdge;
import AUR.util.knd.AUREdgeToStand;
import AUR.util.knd.AURNode;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import rescuecore2.standard.entities.Blockade;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2017 & 2018
 */

public class K_AreaGraph extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		if(selected_ag == null) {
			return;
		}
		
        g2.setColor(Color.CYAN);
        wsg.KStar(wsg.ai.getPosition());
        
        
		for(AURBorder border : selected_ag.borders) {
			for(AURNode node : border.nodes) {
				g2.fillOval(kst.xToScreen(node.x) - 3, kst.yToScreen(node.y) - 3, 6, 6);
			}
		}
		
		g2.setStroke(new BasicStroke(2));
		g2.setColor(Color.green);
		
		for(AURBorder border : selected_ag.borders) {
			for(AURNode node : border.nodes) {
				for(AUREdge edge : node.edges) {
					if(edge.areaGraph == selected_ag) {
						g2.drawLine(
							kst.xToScreen(edge.A.x), kst.yToScreen(edge.A.y),
							kst.xToScreen(edge.B.x), kst.yToScreen(edge.B.y)
						);
					}
				}
				
				
			}
		}
		
//		g2.setColor(Color.red);
//		AUREdgeToStand edge = selected_ag.getBuilding().edgeToPereceptAndExtinguish;
//			
//		if(edge != null) {
//			System.out.println(edge.standX + ", " + edge.standY);
//			g2.fillOval(kst.xToScreen(edge.standX) - 3, kst.yToScreen(edge.standY) - 3, 6, 6);
//		}
		
//		for(AURBorder border : selected_ag.borders) {
//			for(AURNode node : border.nodes) {
//				if(node.edgesToPerceptAndExtinguish == null) {
//					continue;
//				}
//				for(AUREdgeToStand edge : node.edgesToPerceptAndExtinguish) {
//					g2.fillOval(kst.xToScreen(edge.standX) - 3, kst.yToScreen(edge.standY) - 3, 6, 6);
//					
//					System.out.println(edge.standX + ", " + edge.standY);
//					
//					g2.drawLine(
//						kst.xToScreen(node.x), kst.yToScreen(node.y),
//						kst.xToScreen(edge.standX), kst.yToScreen(edge.standY)
//					);
//				}
//				
//				
//			}
//		}
		
    }

}