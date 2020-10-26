package com.mrl.debugger.layers.custom;

import com.mrl.debugger.ViewLayer;
import com.mrl.debugger.layers.base.MrlBaseAreaLayer;
import rescuecore2.standard.entities.Area;

import java.awt.*;

/**
 * @author CSU-zack
 */
@ViewLayer(visible = false, caption = "Observable Areas", tag = "ObservableAreas")
public class CSUObservableAreas extends MrlBaseAreaLayer<Area> {

    public CSUObservableAreas() {
        super(Area.class);
    }

    @Override
    protected void paintData(Area area, Polygon p, Graphics2D g) {
        g.setColor(Color.WHITE);
        g.fill(p);
    }

}
