/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AUREdgeToStand;
import AUR.util.knd.AURNode;
import AUR.util.knd.AURWorldGraph;
import adf.agent.action.common.ActionMove;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import rescuecore2.worldmodel.EntityID;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class K_PathToSeeInside extends K_ViewerLayer {
	
	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {

		if (selected_ag == null || selected_ag.isBuilding() == false) {
			return;
		}

		wsg.KStar(wsg.ai.getPosition());

		AUREdgeToStand etp = selected_ag.getBuilding().edgeToSeeInside;
		if (etp == null) {			
			return;
		}
		int lastX = kst.xToScreen(etp.standX);
		int lastY = kst.yToScreen(etp.standY);
		
		AURNode node = new AURNode(lastX, lastY, selected_ag, selected_ag);
		node.pre = etp.fromNode;
		
		g2.setColor(new Color(0, 128, 128));
		g2.setStroke(new BasicStroke(3));
		
		while (node.pre != wsg.startNullNode) {

			node = node.pre;

			if (node == null) {
				return;
			}

			int X = kst.xToScreen(node.x);
			int Y = kst.yToScreen(node.y);

			g2.drawLine(
				lastX, lastY,
				X, Y
			);

			lastX = X;
			lastY = Y;
		}

		g2.drawLine(
			lastX, lastY,
			kst.xToScreen(wsg.ai.getX()), kst.yToScreen(wsg.ai.getY())
		);

		g2.setStroke(new BasicStroke(1));
		
	}
	
	@Override
	public String getString(AURWorldGraph wsg, AURAreaGraph selected_ag) {
		if(selected_ag == null) {
		    return null;
		}
		String result = "";
		Collection<EntityID> targets = new ArrayList<>();
		targets.add(selected_ag.area.getID());
		ActionMove am = wsg.getMoveActionToSeeInside(wsg.ai.getPosition(), selected_ag.area.getID());
		if(am == null) {
				result += "null";
				result += "\n";
				return result;
		}
		List<EntityID> path = am.getPath();
		if(path != null) {
			for(int i = 0; i < path.size(); i++) {
				result += path.get(i).getValue();
				result += "\n";
			}
		}
		return result;
	}
	
}