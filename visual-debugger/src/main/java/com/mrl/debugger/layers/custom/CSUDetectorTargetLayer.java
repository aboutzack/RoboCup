package com.mrl.debugger.layers.custom;

import com.mrl.debugger.ViewLayer;
import com.mrl.debugger.layers.base.MrlBaselineLayer;

import java.awt.*;
import java.awt.geom.Line2D;

/**
 * @author CSU-zack
 */
@ViewLayer(visible = false, tag = "DetectorTargetLayer", caption = "DetectorTarget", drawAllData = true)
public class CSUDetectorTargetLayer extends MrlBaselineLayer {

    @Override
    protected void paintShape(Line2D p, Graphics2D g) {
        g.setColor(Color.BLUE);
        g.draw(p);
    }
}
