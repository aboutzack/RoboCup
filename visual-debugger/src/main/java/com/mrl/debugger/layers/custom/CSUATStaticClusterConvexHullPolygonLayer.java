package com.mrl.debugger.layers.custom;

import com.mrl.debugger.ViewLayer;
import com.mrl.debugger.layers.base.MrlBasePolygonLayer;

import java.awt.*;

/**
 * @author csu-zack
 */
@ViewLayer(visible = false, caption = "AT Cluster ConvexHull",tag = "ATClusterConvexPolygon", drawAllData = true)
public class CSUATStaticClusterConvexHullPolygonLayer extends MrlBasePolygonLayer {

    @Override
    protected void paintShape(Polygon p, Graphics2D g) {
        g.setColor(Color.BLUE);
        g.draw(p);
    }
}