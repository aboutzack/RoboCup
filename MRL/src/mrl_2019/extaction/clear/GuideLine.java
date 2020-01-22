package mrl_2019.extaction.clear;


import mrl_2019.util.Util;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mahdi
 */
public class GuideLine extends Line2D {
    List<EntityID> areas = new ArrayList<EntityID>();
    private boolean isMinor = true;


    public GuideLine(Point2D origin, Vector2D direction) {
        super(origin, direction);
    }

    public GuideLine(Point2D origin, Point2D end) {
        super(origin, end);
    }

    public GuideLine(double x1, double y1, double x2, double y2) {
        super(new Point2D(x1, y1), new Point2D(x2, y2));
    }


    public GuideLine(Line2D line) {
        super(line.getOrigin(),line.getEndPoint());
    }

    public void setAreas(List<EntityID> areas) {
        this.areas = areas;
    }

    public List<EntityID> getAreas() {
        return areas;
    }

    public double getLength() {
        return Util.lineLength(this);
    }

    public double distanceFromSegment(Point2D point2D) {
        return Util.lineSegmentAndPointDistance(this, point2D);
    }

    public double distanceFromLine(Point2D point2D) {
        return java.awt.geom.Line2D.ptLineDist(this.getOrigin().getX(), getOrigin().getY(), getEndPoint().getX(), getEndPoint().getY(), point2D.getX(), point2D.getY());
    }


    public boolean isMinor() {
        return isMinor;
    }

    public void setMinor(boolean isMinor) {
        this.isMinor = isMinor;
    }
}
