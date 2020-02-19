package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURBorder;
import AUR.util.knd.AUREdge;
import AUR.util.knd.AURNode;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class K_NoBlockadeWorldGraph extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {

		g2.setColor(Color.BLUE);
		
		wsg.KStarNoBlockade(wsg.ai.getPosition());

		for (AURAreaGraph ag : wsg.areas.values()) {
			for (AURBorder border : ag.borders) {
				g2.fillOval(kst.xToScreen(border.CenterNode.x) - 3, kst.yToScreen(border.CenterNode.y) - 3, 6, 6);

			}
		}

		g2.setStroke(new BasicStroke(1));
		g2.setColor(Color.green);

		for (AURAreaGraph ag : wsg.areas.values()) {
			for (AURBorder border : ag.borders) {

				AURNode node = border.CenterNode;
				for (AUREdge edge : node.edges) {
					g2.drawLine(
						kst.xToScreen(edge.A.x), kst.yToScreen(edge.A.y),
						kst.xToScreen(edge.B.x), kst.yToScreen(edge.B.y)
					);
				}
			}
		}

		g2.setStroke(new BasicStroke(1));
	}

}
