/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package viewer.layers.knd;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class K_BuildingNeighbourAreas extends K_ViewerLayer {

	@Override
	public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
		for (AURAreaGraph ag : wsg.areas.values()) {
			if (ag.isBuildingNeighbour()) {
				Polygon polygon = kst.getTransformedPolygon(ag.area.getShape());
				g2.setColor(Color.green);
				g2.fillPolygon(polygon);
				g2.setColor(Color.black);
				g2.drawPolygon(polygon);
			}

		}
	}

}
