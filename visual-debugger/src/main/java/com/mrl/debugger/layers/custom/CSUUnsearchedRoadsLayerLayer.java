package com.mrl.debugger.layers.custom;

import com.mrl.debugger.ViewLayer;
import com.mrl.debugger.layers.standard.MrlStandardRoadLayer;
import rescuecore2.standard.entities.Road;

import java.awt.*;

/**
 * @author CSU-zack
 */
@ViewLayer(visible = false, caption = "Unsearched roads", tag = "UnsearchedRoads")
public class CSUUnsearchedRoadsLayerLayer extends MrlStandardRoadLayer {

    @Override
    protected void paintData(Road area, Polygon p, Graphics2D g) {
        g.setColor(Color.YELLOW);
        g.fill(p);
    }
}
