package com.mrl.debugger.layers.custom;

import com.mrl.debugger.ViewLayer;
import com.mrl.debugger.layers.standard.MrlStandardHumanLayer;
import math.geom2d.conic.Circle2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Human;

import java.awt.*;

/**
 * @author CSU-zack
 */
@ViewLayer(visible = false, caption = "Need Refill FB", tag = "NeedRefillFB", drawAllData = true)
public class CSUNeedRefillFB extends MrlStandardHumanLayer {

    @Override
    protected void paintData(Human h, Shape shape, Graphics2D g, ScreenTransform t) {
        g.setColor(new Color(0, 255, 0));
        Circle2D circle2D = new Circle2D(t.xToScreen(h.getX()), t.yToScreen(h.getY()), 18d, true);
        circle2D.draw(g);
    }
}