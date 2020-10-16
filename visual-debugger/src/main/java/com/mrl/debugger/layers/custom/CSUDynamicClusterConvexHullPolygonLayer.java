package com.mrl.debugger.layers.custom;

import com.mrl.debugger.ViewLayer;
import com.mrl.debugger.layers.base.MrlBasePolygonLayer;

import java.awt.*;

/**
 * @author csu-zack
 */
@ViewLayer(visible = false, caption = "Dynamic Cluster ConvexHull",tag = "DynamicClusterConvexPolygon", drawAllData = false)
public class CSUDynamicClusterConvexHullPolygonLayer extends MrlBasePolygonLayer {

    @Override
    protected void paintShape(Polygon p, Graphics2D g) {
        g.setColor(Color.red);
        g.draw(p);
    }
}
