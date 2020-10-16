package com.mrl.debugger.layers.custom;

import com.mrl.debugger.ViewLayer;
import com.mrl.debugger.layers.standard.MrlStandardBuildingLayer;
import rescuecore2.standard.entities.Building;

import java.awt.*;

/**
 * @author CSU-zack
 */
@ViewLayer(visible = false, caption = "Unsearched buildings", tag = "UnsearchedBuildings")
public class CSUUnsearchedBuildingsLayer extends MrlStandardBuildingLayer {

    @Override
    protected void paintData(Building area, Polygon p, Graphics2D g) {
        g.setColor(Color.CYAN);
        g.fill(p);
    }
}
