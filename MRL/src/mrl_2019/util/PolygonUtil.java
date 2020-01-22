package mrl_2019.util;

import math.geom2d.Point2D;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.polygon.Polygon2DUtils;
import math.geom2d.polygon.SimplePolygon2D;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;

import java.awt.*;
import java.awt.geom.Line2D;

/**
 * User: Pooya
 * Date: 3/30/12
 * Time: 11:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class PolygonUtil {
    /**
     * This function scales a polygon by the scale coefficient
     *
     * @param sourcePolygon : Is the Polygon that we want to scale
     * @param scale         : Is the scale coefficient, It actually multiplies to the points and makes the new shape
     * @return : returns the scaled polygon which, its center is on the center of the last polygon
     */
    public static Polygon scalePolygon(Polygon sourcePolygon, double scale) {
        Polygon scaledPolygon;

        int xs[] = new int[sourcePolygon.npoints];
        int ys[] = new int[sourcePolygon.npoints];
        Point2D p, p1;
        int sumX = 0;
        int sumY = 0;

        for (int i = 0; i < sourcePolygon.npoints; i++) {
            p = new Point2D(sourcePolygon.xpoints[i], sourcePolygon.ypoints[i]);
            p1 = p.scale(scale);
            sumX += p1.getX();
            sumY += p1.getY();
            xs[i] = (int) p1.getX();
            ys[i] = (int) p1.getY();
            p.clone();
        }

        Polygon preScaledPolygon = new Polygon(xs, ys, sourcePolygon.npoints);
        scaledPolygon = reAllocatePolygon(preScaledPolygon, sourcePolygon);
        if (scaledPolygon == null)
            scaledPolygon = preScaledPolygon;
        return scaledPolygon;
    }

    /**
     * This function changes the position of the polygon which is scaled by the "scalePolygon" function. If we don't use this function the scaled polygon does not appear in the right place.
     *
     * @param scaled: is the scaled polygon of our source (notice that it is not in the right place)
     * @param source: is the source polygon, (that is not scaled) we want it to determine the exact position of our scaled polygon
     * @return: returns the new polygon that is in the right place (its center is exactly on the old center)
     */
    private static Polygon reAllocatePolygon(Polygon scaled, Polygon source) {
        if (source == null || scaled == null || source.npoints == 0 || scaled.npoints == 0)
            return null;
        Polygon reAllocated;
        int xs[] = new int[scaled.npoints];
        int ys[] = new int[scaled.npoints];

        int sourceCenterX = 0;
        int sourceCenterY = 0;

        int scaledCenterX = 0;
        int scaledCenterY = 0;

        for (int i = 0; i < scaled.npoints; i++) {
            sourceCenterX += source.xpoints[i];
            sourceCenterY += source.ypoints[i];

            scaledCenterX += scaled.xpoints[i];
            scaledCenterY += scaled.ypoints[i];
        }

        sourceCenterX = sourceCenterX / source.npoints;
        sourceCenterY = sourceCenterY / source.npoints;

        scaledCenterX = scaledCenterX / scaled.npoints;
        scaledCenterY = scaledCenterY / scaled.npoints;

        int xDistance = sourceCenterX - scaledCenterX;
        int yDistance = sourceCenterY - scaledCenterY;

        for (int i = 0; i < scaled.npoints; i++) {
            xs[i] = scaled.xpoints[i] + xDistance;
            ys[i] = scaled.ypoints[i] + yDistance;
        }
        reAllocated = new Polygon(xs, ys, scaled.npoints);

        return reAllocated;
    }

    //MTN==>

    /**
     * split polygon by a line2D to to polygon
     *
     * @param polygon
     * @param line
     * @return
     */
    public static Pair<Polygon, Polygon> split(Polygon polygon, rescuecore2.misc.geometry.Line2D line) {
        int index1 = -1, index2 = -1, count = polygon.npoints;
        int newXs[] = new int[count + 2];
        int newYs[] = new int[count + 2];
        int k = 0;
        for (int i = 0; i < count; i++, k++) {
            int j = (i + 1) % count;
            rescuecore2.misc.geometry.Point2D p1 = new rescuecore2.misc.geometry.Point2D(polygon.xpoints[i], polygon.ypoints[i]),
                    p2 = new rescuecore2.misc.geometry.Point2D(polygon.xpoints[j], polygon.ypoints[j]);
            rescuecore2.misc.geometry.Line2D line2D = new rescuecore2.misc.geometry.Line2D(p1, p2);
            rescuecore2.misc.geometry.Point2D intersectPoint = GeometryTools2D.getIntersectionPoint(line, line2D);
            newXs[k] = (int) p1.getX();
            newYs[k] = (int) p1.getY();
            if (line == null || intersectPoint == null) {
                continue;
            }

            //bara inke motmaen shim in noghte rooye pare khat hast na khat!!!
            if (Util.contains(line, intersectPoint)) {
                if (index1 == -1) {
                    k++;
                    newXs[k] = (int) intersectPoint.getX();
                    newYs[k] = (int) intersectPoint.getY();
                    index1 = k;
                } else if (index2 == -1) {
                    k++;
                    newXs[k] = (int) intersectPoint.getX();
                    newYs[k] = (int) intersectPoint.getY();
                    index2 = k;
                }
            }
        }

        Polygon poly1 = new Polygon();
        Polygon poly2 = new Polygon();
        for (int i = 0; i < count + 2; i++) {
            if (i == index1 || i == index2) {
                poly1.addPoint(newXs[i], newYs[i]);
                poly2.addPoint(newXs[i], newYs[i]);
            } else if (i < index1 || i > index2) {
                poly1.addPoint(newXs[i], newYs[i]);
            } else {
                poly2.addPoint(newXs[i], newYs[i]);
            }

        }
        return new Pair<Polygon, Polygon>(poly1, poly2);
    }

    public static Pair<Polygon, Polygon> split(int polygonApexes[], rescuecore2.misc.geometry.Line2D line) {
        return split(Util.getPolygon(polygonApexes), line);
    }

    public static Polygon retainPolygon(Polygon polygon1, Polygon polygon2) {
        double xs1[] = new double[polygon1.npoints];
        double ys1[] = new double[polygon1.npoints];
        double xs2[] = new double[polygon2.npoints];
        double ys2[] = new double[polygon2.npoints];
        for (int i = 0; i < polygon1.npoints; i++) {
            xs1[i] = polygon1.xpoints[i];
            ys1[i] = polygon1.ypoints[i];
        }
        for (int i = 0; i < polygon2.npoints; i++) {
            xs2[i] = polygon2.xpoints[i];
            ys2[i] = polygon2.ypoints[i];
        }
        math.geom2d.polygon.SimplePolygon2D polygon2D1 = new SimplePolygon2D(xs1, ys1);
        math.geom2d.polygon.SimplePolygon2D polygon2D2 = new SimplePolygon2D(xs2, ys2);

        Polygon2D exclusive = Polygon2DUtils.intersection(polygon2D1, polygon2D2);


        int exclusiveXs[] = new int[exclusive.getVertexNumber()];
        int exclusiveYs[] = new int[exclusive.getVertexNumber()];
        int count = exclusive.getVertexNumber();
        for (int i = 0; i < count; i++) {
            exclusiveXs[i] = (int) exclusive.getVertex(i).getX();
            exclusiveYs[i] = (int) exclusive.getVertex(i).getY();
        }
        return new Polygon(exclusiveXs, exclusiveYs, count);
    }

    public static Polygon union(Polygon polygon1, Polygon polygon2) {
        double xs1[] = new double[polygon1.npoints];
        double ys1[] = new double[polygon1.npoints];
        double xs2[] = new double[polygon2.npoints];
        double ys2[] = new double[polygon2.npoints];
        for (int i = 0; i < polygon1.npoints; i++) {
            xs1[i] = polygon1.xpoints[i];
            ys1[i] = polygon1.ypoints[i];
        }
        for (int i = 0; i < polygon2.npoints; i++) {
            xs2[i] = polygon2.xpoints[i];
            ys2[i] = polygon2.ypoints[i];
        }
        math.geom2d.polygon.SimplePolygon2D polygon2D1 = new SimplePolygon2D(xs1, ys1);
        math.geom2d.polygon.SimplePolygon2D polygon2D2 = new SimplePolygon2D(xs2, ys2);

        Polygon2D union = Polygon2DUtils.union(polygon2D1, polygon2D2);


        int unionXs[] = new int[union.getVertexNumber()];
        int unionYs[] = new int[union.getVertexNumber()];
        int count = union.getVertexNumber();
        for (int i = 0; i < count; i++) {
            unionXs[i] = (int) union.getVertex(i).getX();
            unionYs[i] = (int) union.getVertex(i).getY();
        }
        return new Polygon(unionXs, unionYs, count);
    }

    public static double distanceBetween(Polygon polygon1, Polygon polygon2) {
        return distanceBetween(polygon1, polygon2, Double.MAX_VALUE);
    }

    private static double distanceBetween(Polygon polygon1, Polygon polygon2, double minDistance) {
        int count = polygon1.npoints;
        double minDist = minDistance;

        for (int i = 0; i < count; i++) {
            int j = (i + 1) % count;
            Point point1 = new Point(polygon1.xpoints[i], polygon1.ypoints[i]);
            Point point2 = new Point(polygon1.xpoints[j], polygon1.ypoints[j]);
            Line2D line = new Line2D.Double(point1, point2);

            for (int k = 0; k < polygon2.npoints; k++) {
                Point point = new Point(polygon1.xpoints[i], polygon1.ypoints[i]);
                double dist = Util.lineSegmentAndPointDistance(line, point);
                minDist = Math.min(minDist, dist);
            }
        }
        if (minDistance == Double.MAX_VALUE) {
            minDistance = minDist;
            distanceBetween(polygon1, polygon2, minDistance);

        }
        return minDist;
    }
}
