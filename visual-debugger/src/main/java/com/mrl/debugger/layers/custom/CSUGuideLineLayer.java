package com.mrl.debugger.layers.custom;

import com.mrl.debugger.ViewLayer;
import com.mrl.debugger.layers.base.MrlBaselineLayer;

import java.awt.*;
import java.awt.geom.Line2D;

/**
 * @author Mahdi
 */
@ViewLayer(visible = false, tag = "GuideLine", caption = "Guide lines", drawAllData = true)
public class CSUGuideLineLayer extends MrlBaselineLayer {

    private static final Stroke STROKE = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

    @Override
    protected void paintShape(Line2D p, Graphics2D g) {
        g.setStroke(STROKE);
        g.setColor(Color.BLUE);
        g.draw(p);
    }
}
