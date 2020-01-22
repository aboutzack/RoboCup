package AIT_2019.module.algorithm;

import rescuecore2.standard.entities.*;
import rescuecore2.misc.geometry.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import java.awt.Polygon;
import java.util.*;

public class ConvexHull
{
    private Geometry convexhull = null;
    private Set<Area> areas = new HashSet<>();

    public void compute()
    {
        Geometry geo = (new GeometryFactory()).createPolygon();
        for (Area area : this.areas) geo = geo.union(makeGeometry(area));
        this.convexhull = geo.convexHull();
    }

    public void add(Area area)
    {
        this.convexhull = null;
        this.areas.add(area);
    }

    public Polygon get()
    {
        if (this.convexhull == null) this.compute();

        final Geometry geo = this.convexhull.convexHull();
        final Coordinate[] cs = geo.getCoordinates();
        final int n = cs.length;
        final int[] xs = new int[n];
        final int[] ys = new int[n];

        for (int i=0; i<n; ++i)
        {
            xs[i] = (int)cs[i].getX();
            ys[i] = (int)cs[i].getY();
        }
        return new Polygon(xs, ys, n);
    }

    public Point2D[] getApexes()
    {
        if (this.convexhull == null) this.compute();

        final Geometry geo = this.convexhull.convexHull();
        final Coordinate[] cs = geo.getCoordinates();
        final int n = cs.length;
        final Point2D[] ret = new Point2D[n];

        for (int i=0; i<n; ++i)
        {
            ret[i] = new Point2D(cs[i].getX(), cs[i].getY());
        }
        return ret;
    }

    private static Geometry makeGeometry(Area area)
    {
        List<Point2D> ps =
            GeometryTools2D.vertexArrayToPoints(area.getApexList());
        return makeGeometry(ps.toArray(new Point2D[0]));
    }

    private static Geometry makeGeometry(Point2D[] ps)
    {
        final int n = ps.length;
        Coordinate[] cs = new Coordinate[n+1];
        for (int i=0; i<n; ++i)
        {
            cs[i] = new Coordinate(ps[i].getX(), ps[i].getY());
        }
        cs[n] = cs[0];

        GeometryFactory fact = new GeometryFactory();
        return fact.createPolygon(cs);
    }
}
