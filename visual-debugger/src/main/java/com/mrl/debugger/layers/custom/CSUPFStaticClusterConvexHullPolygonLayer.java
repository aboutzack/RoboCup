package com.mrl.debugger.layers.custom;

import com.mrl.debugger.ViewLayer;
import com.mrl.debugger.layers.base.MrlBasePolygonLayer;

import java.awt.*;

/**
 * @author csu-zack
 */
@ViewLayer(visible = false, caption = "PF Cluster ConvexHull",tag = "PFClusterConvexPolygon", drawAllData = true)
public class CSUPFStaticClusterConvexHullPolygonLayer extends MrlBasePolygonLayer {

    @Override
    protected void paintShape(Polygon p, Graphics2D g) {
        g.setColor(Color.BLUE);
        g.draw(p);
    }
}