package mrl_2019.algorithm.clustering;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Set;

/**
 * @author Pooya Deldar
 *         Date: 3/11/12
 *         Time: 8:38 PM
 */
public class ConvexObject {

    private Polygon convexPolygon;
    private Polygon triangle;

    public ConvexObject(Polygon convexPolygon) {
        this.convexPolygon = convexPolygon;
    }

    public ConvexObject() {
    }

    public Point CENTER_POINT;
    public Point FIRST_POINT;
    public Point SECOND_POINT;
    public Point CONVEX_POINT;
    //-------------
    public Point OTHER_POINT1;
    public Point OTHER_POINT2;
    public Set<Point2D> CONVEX_INTERSECT_POINTS;
    public Set<Line2D> CONVEX_INTERSECT_LINES;
    public Polygon DIRECTION_POLYGON;


    public Polygon getConvexPolygon() {
        return convexPolygon;
    }

    public void setConvexPolygon(Polygon convexPolygon) {
        this.convexPolygon = convexPolygon;
    }

    public void setTrianglePolygon(Polygon shape) {
        int xs[] = new int[shape.npoints];
        int ys[] = new int[shape.npoints];
        for (int i = 0; i < shape.npoints; i++) {
            xs[i] = shape.xpoints[i];
            ys[i] = shape.ypoints[i];
        }
        triangle = new Polygon(xs, ys, shape.npoints);
    }

    public Polygon getTriangle() {
        return triangle;
    }

}
