package com.mrl.debugger.layers.custom;

import com.mrl.debugger.ViewLayer;
import com.mrl.debugger.layers.base.MrlBaseAreaLayer;
import rescuecore2.standard.entities.Area;

import java.awt.*;

/**
 * @author CSU-zack
 */
@ViewLayer(visible = false, caption = "VisibleFrom Areas", tag = "VisibleFromAreas")
public class CSUVisibleFromAreas extends MrlBaseAreaLayer<Area> {

    public CSUVisibleFromAreas() {
        super(Area.class);
    }

    @Override
    protected void paintData(Area area, Polygon p, Graphics2D g) {
        g.setColor(Color.PINK);
        g.fill(p);
    }

}
