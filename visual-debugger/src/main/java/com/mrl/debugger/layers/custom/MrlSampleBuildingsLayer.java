package com.mrl.debugger.layers.custom;

import com.mrl.debugger.ViewLayer;
import com.mrl.debugger.layers.standard.MrlStandardBuildingLayer;
import rescuecore2.standard.entities.Building;

import java.awt.*;

/**
 * @author Mahdi
 */
@ViewLayer(visible = false, caption = "Sample buildings", tag = "SampleBuildings")
public class MrlSampleBuildingsLayer extends MrlStandardBuildingLayer {

    @Override
    protected void paintData(Building area, Polygon p, Graphics2D g) {
        g.setColor(Color.CYAN);
        g.fill(p);
    }
}
