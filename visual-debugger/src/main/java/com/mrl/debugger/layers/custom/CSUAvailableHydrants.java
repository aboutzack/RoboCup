package com.mrl.debugger.layers.custom;

import com.mrl.debugger.ViewLayer;
import com.mrl.debugger.layers.standard.MrlStandardRoadLayer;
import rescuecore2.standard.entities.Road;

import java.awt.*;

/**
 * @author CSU-zack
 */
@ViewLayer(visible = false, caption = "Available Hydrants", drawAllData = true, tag = "AvailableHydrants")
public class CSUAvailableHydrants extends MrlStandardRoadLayer {

    @Override
    protected void paintData(Road area, Polygon p, Graphics2D g) {
        g.setColor(new Color(0, 232, 35));
        g.fill(p);
    }
}

