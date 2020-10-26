package com.mrl.debugger.layers.custom;

import com.mrl.debugger.Util;
import com.mrl.debugger.ViewLayer;
import com.mrl.debugger.layers.base.MrlBaseDtoLayer;
import com.mrl.debugger.remote.dto.StuckDto;
import math.geom2d.conic.Circle2D;
import rescuecore2.misc.Pair;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * @author CSU-zack
 */
@ViewLayer(caption = "Stuck Dto", tag = "StuckDtoLayer", drawAllData = true, visible = false)
public class CSUStuckDtoLayer extends MrlBaseDtoLayer<StuckDto> {

    private static final Stroke STROKE = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

    @Override
    protected StuckDto transform(StuckDto p, ScreenTransform t) {
        StuckDto stuckDto = new StuckDto();
        if (p.getAgentId() != null) {
            stuckDto.setAgentId(p.getAgentId());
            stuckDto.setBlockadesConvexHull(Util.transform(p.getBlockadesConvexHull(), t));
            stuckDto.setOpenPartCenter(Util.transform(p.getOpenPartCenter(), t));
            stuckDto.setTarget(Util.transform(p.getTarget(), t));
            stuckDto.setRaysNotHits(p.getRaysNotHits().stream().map(e -> Util.transform(e, t)).collect(Collectors.toSet()));
            stuckDto.setSelfRaysNotHits(p.getSelfRaysNotHits().stream().map(e -> Util.transform(e, t)).collect(Collectors.toSet()));
            stuckDto.setGuideLine(Util.transform(p.getGuideLine(), t));
            stuckDto.setGuideLine(Util.transform(p.getGuideLine(), t));
            Pair<Integer, Integer> location = world.getEntity(new EntityID(p.getAgentId())).getLocation(world);
            stuckDto.setCircle2D(new Circle2D(t.xToScreen(location.first()), t.yToScreen(location.second()), 18d, true));
        }else {
            stuckDto.setBlockadesConvexHull(new Polygon());
            stuckDto.setCircle2D(new Circle2D());
            stuckDto.setTarget(new Point());
            stuckDto.setOpenPartCenter(new Point());
            stuckDto.setRaysNotHits(new HashSet<>());
            stuckDto.setGuideLine(new Line2D.Double());
        }
        return stuckDto;
    }

    @Override
    protected void paintDto(StuckDto p, Graphics2D g) {
        if (p.getAgentId() != null) {
            g.setStroke(STROKE);
            g.setColor(Color.yellow);
            g.drawPolygon(p.getBlockadesConvexHull());
            g.setColor(Color.CYAN);
            p.getRaysNotHits().forEach(g::draw);
            p.getSelfRaysNotHits().forEach(g::draw);
            g.setColor(Color.BLUE);
            p.getCircle2D().draw(g);
            g.setColor(Color.ORANGE);
            g.draw(p.getGuideLine());
            g.setColor(Color.magenta);
            g.fillOval((int) p.getOpenPartCenter().getX(), (int) p.getOpenPartCenter().getY(), 8, 8);
            g.setColor(Color.RED);
            g.fillOval((int) p.getTarget().getX(), (int) p.getTarget().getY(), 8, 8);
        }
    }
}
