package com.mrl.debugger.remote.dto;

import math.geom2d.conic.Circle2D;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.Collection;

/**
 * @author CSU-zack
 */
public class StuckDto implements StandardDto{

    private Integer agentId;

    private Polygon blockadesConvexHull;

    private Point openPartCenter;

    private Line2D guideLine;

    private Point target;

    private Collection<Integer> targetValidRoads;

    private Collection<Line2D> raysNotHits;

    private Collection<Line2D> selfRaysNotHits;

    private Circle2D circle2D;

    public Integer getAgentId() {
        return agentId;
    }

    public void setAgentId(Integer agentId) {
        this.agentId = agentId;
    }

    public Polygon getBlockadesConvexHull() {
        return blockadesConvexHull;
    }

    public void setBlockadesConvexHull(Polygon blockadesConvexHull) {
        this.blockadesConvexHull = blockadesConvexHull;
    }

    public Point getOpenPartCenter() {
        return openPartCenter;
    }

    public void setOpenPartCenter(Point openPartCenter) {
        this.openPartCenter = openPartCenter;
    }

    public Line2D getGuideLine() {
        return guideLine;
    }

    public void setGuideLine(Line2D guideLine) {
        this.guideLine = guideLine;
    }

    public Point getTarget() {
        return target;
    }

    public void setTarget(Point target) {
        this.target = target;
    }

    public Collection<Integer> getTargetValidRoads() {
        return targetValidRoads;
    }

    public void setTargetValidRoads(Collection<Integer> targetValidRoads) {
        this.targetValidRoads = targetValidRoads;
    }

    public Collection<Line2D> getRaysNotHits() {
        return raysNotHits;
    }

    public void setRaysNotHits(Collection<Line2D> raysNotHits) {
        this.raysNotHits = raysNotHits;
    }

    public Circle2D getCircle2D() {
        return circle2D;
    }

    public void setCircle2D(Circle2D circle2D) {
        this.circle2D = circle2D;
    }

    public Collection<Line2D> getSelfRaysNotHits() {
        return selfRaysNotHits;
    }

    public void setSelfRaysNotHits(Collection<Line2D> selfRaysNotHits) {
        this.selfRaysNotHits = selfRaysNotHits;
    }
}
