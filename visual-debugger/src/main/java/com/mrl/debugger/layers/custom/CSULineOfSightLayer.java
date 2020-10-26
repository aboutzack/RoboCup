package com.mrl.debugger.layers.custom;

import com.mrl.debugger.ViewLayer;
import com.mrl.debugger.layers.base.MrlBaselineLayer;

import java.awt.*;
import java.awt.geom.Line2D;

/**
 * @author CSU-zack
 */
@ViewLayer(visible = false, tag = "LineOfSightLayer", caption = "Line Of Sight", drawAllData = false)
public class CSULineOfSightLayer extends MrlBaselineLayer {

    @Override
    protected void paintShape(Line2D p, Graphics2D g) {
        g.setColor(new Color(0, 255, 233, 87));
        g.draw(p);
    }
}
