package mrl_2019.algorithm.clustering;

import com.poths.rna.data.Point;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ConvexHull {

    public Point FIRST_POINT;
    public Point SECOND_POINT;
    public Point CONVEX_POINT;
    public Point OTHER_POINT1;
    public Point OTHER_POINT2;
    public Set<Point2D> CONVEX_INTERSECT_POINTS;
    public Set<Line2D> CONVEX_INTERSECT_LINES;
    public Polygon DIRECTION_POLYGON;


    com.poths.rna.data.ConvexHull convexHull = new com.poths.rna.data.ConvexHull();


    public void addPoint(int x, int y) {
        convexHull.addPoint(new com.poths.rna.data.Point(x, y));
    }

    public void addPoint(Point point) {
        convexHull.addPoint(point);

    }


    public Polygon convex() {
        List<Point> pointList = convexHull.getPointList();
        int xPoints2[] = new int[pointList.size()];
        int yPoints2[] = new int[pointList.size()];

        for (int i = 0; i < pointList.size(); i++) {
            xPoints2[i] = (int) pointList.get(i).getX();
            yPoints2[i] = (int) pointList.get(i).getY();
        }

        return new Polygon(xPoints2, yPoints2, pointList.size());
    }

    public List<Point> getPoints() {
        return convexHull.getPointList();
    }

    public List<Point> getAwtPoints() {
        List<Point> points = new ArrayList<Point>();
        for (Point point : convexHull.getPointList()) {
            points.add(new Point((int) point.getX(), (int) point.getY()));
        }
        return points;
    }

}
