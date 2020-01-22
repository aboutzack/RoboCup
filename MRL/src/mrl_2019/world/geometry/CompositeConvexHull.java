package mrl_2019.world.geometry;

import math.geom2d.Point2D;
import math.geom2d.line.Line2D;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.polygon.convhull.JarvisMarch2D;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Implementation of Convex Hull using Graham Scan, Jarvis March and On-line CH algorithm.
 * <p/>
 * This class is in conformance with late-computation technique. Whenever {@link #getConvexPolygon()} is called, the actual convex hull
 * is created and kept until addition or removal of a point (possibly) invalidates it.
 *
 * @version 1.0
 */
public class CompositeConvexHull implements IConvexHull {

    /**
     * Note: We keep points in a List because the underlying algorithm requires sorting the input
     */
    private List<Point> points;

    /**
     * Temporary storage for points which are added but not yet taken into account for calculation
     */
    private Set<Point> addedPoints;

    /**
     * Temporary storage for points which are removed but not yet taken into account for calculation
     */
    private Set<Point> removedPoints;

    /**
     * Cached convex hull polygon. In this implementation it's updated whenever {@link #getConvexPolygon()} is called and {@link #isDataUpdated()} returns true.
     */
    private Polygon convexHullPolygon;

    //TODO @BrainX Make sure the delegate algorithm is efficient.
    /**
     * Delegate algorithm
     */
    private JarvisMarch2D jarvisMarchCalculator;

    /**
     * Unique id of this convex hull. This ID must be calculated similarly among different agents (threads or VM's)
     */
    @SuppressWarnings("unused")
    private Long guid;

    public CompositeConvexHull() {
        points = new ArrayList<Point>();
        jarvisMarchCalculator = new JarvisMarch2D();

        addedPoints = new HashSet<Point>();
        removedPoints = new HashSet<Point>();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unused")
    @Override
    public Long getGuid() {
        return guid;
    }

    /**
     * Gets or calculates the output Convex hull based on internal point data. If dataUpdated flag is set, calculation is
     * performed, otherwise the cahced {@link #convexHullPolygon} is returned.
     *
     * @return A Polygon object representing the convex hull
     */
    @Override
    public Polygon getConvexPolygon() {
        if (isDataUpdated()) {
            convexHullPolygon = updateConvexPolygon();
        }
        return convexHullPolygon;
    }

    /**
     * Takes {@link #convexHullPolygon} as initial state and updates it with the newly-altered points
     *
     * @return The updated convex polygon
     */
    public Polygon updateConvexPolygon() {
        Polygon returnValue;
        if (convexHullPolygon != null) {
            Set<Point> onVerticesRemovedPoints = new HashSet<Point>();
            for (Point removedPoint : removedPoints) {
                for (int i = 0; i < convexHullPolygon.npoints; i++) {
                    if (removedPoint.x == convexHullPolygon.xpoints[i]
                            && removedPoint.y == convexHullPolygon.ypoints[i]) {
                        onVerticesRemovedPoints.add(removedPoint);
                        break;
                    }
                }
            }

            removedPoints.removeAll(onVerticesRemovedPoints);
            points.removeAll(removedPoints);
            removedPoints = onVerticesRemovedPoints;

            Set<Point> onVerticesAddedPoints = new HashSet<Point>();
            Set<Point> outerAddedPoints = new HashSet<Point>();
            Set<Point> innerAddedPoints = new HashSet<Point>();

            for (Point addedPoint : addedPoints) {
                for (int i = 0; i < convexHullPolygon.npoints; i++) {
                    if (addedPoint.x == convexHullPolygon.xpoints[i]
                            && addedPoint.y == convexHullPolygon.ypoints[i]) {
                        onVerticesAddedPoints.add(addedPoint);
                    } else if (convexHullPolygon.contains(addedPoint)) {
                        innerAddedPoints.add(addedPoint);
                    } else {
                        outerAddedPoints.add(addedPoint);
                    }
                }
            }

            addedPoints.removeAll(innerAddedPoints);
            addedPoints.removeAll(onVerticesAddedPoints);

            points.addAll(innerAddedPoints);
            points.addAll(onVerticesAddedPoints);

            addedPoints = outerAddedPoints;
        }


        if (removedPoints.isEmpty() && addedPoints.isEmpty()) {
            returnValue = convexHullPolygon;
        } else if (convexHullPolygon != null && removedPoints.isEmpty() && !addedPoints.isEmpty()) {
            addPointsToConvexHull(addedPoints);
            addedPoints.clear();
            returnValue = convexHullPolygon;
        } else {
            points.removeAll(removedPoints);
            points.addAll(addedPoints);

            addedPoints.clear();
            removedPoints.clear();

            List<Point2D> point2ds = convertPoints(points);
            Polygon2D polygon2d = jarvisMarchCalculator.convexHull(point2ds);
            returnValue = convertPolygon2d(polygon2d);
        }

        return returnValue;
    }

    /**
     * Adds the specified points to the convex hull without using the delegate.
     *
     * @param addedPoints Points to be added to the convex hull
     */
    private void addPointsToConvexHull(Set<Point> addedPoints) {
        for (Point addedPoint : addedPoints) {
            Point centroid = new Point();
            for (int i = 0; i < convexHullPolygon.npoints; i++) {
                centroid.x += convexHullPolygon.xpoints[i];
                centroid.y += convexHullPolygon.ypoints[i];
            }
            centroid.x /= convexHullPolygon.npoints;
            centroid.y /= convexHullPolygon.npoints;


            Line2D anchor = new Line2D(centroid.x, centroid.y, addedPoint.x, addedPoint.y);
            double anchorAngle = anchor.getHorizontalAngle();

            double minAngle = 2 * Math.PI;
            double maxAngle = -2 * Math.PI;
            int minAngleVertexIndex = -1;
            int maxAngleVertexIndex = -1;
            for (int i = 0; i < convexHullPolygon.npoints; i++) {
                double angle = anchorAngle - new Line2D(convexHullPolygon.xpoints[i], convexHullPolygon.ypoints[i], addedPoint.x, addedPoint.y).getHorizontalAngle();
                if (angle > 2 * Math.PI) {
                    angle -= (2 * Math.PI);
                }
                if (angle < 0) {
                    angle += (2 * Math.PI);
                }

                if (angle > Math.PI / 2 && angle < 1.5 * Math.PI) {
                    angle = angle - Math.PI;
                } else if (angle >= 1.5 * Math.PI && angle <= 2 * Math.PI) {
                    angle = angle - 2 * Math.PI;
                }

                if (angle > maxAngle) {
                    maxAngle = angle;
                    maxAngleVertexIndex = i;
                }
                if (angle < minAngle) {
                    minAngle = angle;
                    minAngleVertexIndex = i;
                }
            }

            int[] xpoints = new int[convexHullPolygon.npoints + 2 - Math.abs(maxAngleVertexIndex - minAngleVertexIndex)];
            int[] ypoints = new int[convexHullPolygon.npoints + 2 - Math.abs(maxAngleVertexIndex - minAngleVertexIndex)];

            int newPointsIndex = 0;

            points.clear();

            for (int i = 0; i <= Math.min(minAngleVertexIndex, maxAngleVertexIndex); i++) {
                xpoints[newPointsIndex] = convexHullPolygon.xpoints[i];
                ypoints[newPointsIndex] = convexHullPolygon.ypoints[i];
                points.add(new Point(xpoints[newPointsIndex], ypoints[newPointsIndex]));
                newPointsIndex++;
            }

            xpoints[newPointsIndex] = addedPoint.x;
            ypoints[newPointsIndex] = addedPoint.y;
            points.add(new Point(xpoints[newPointsIndex], ypoints[newPointsIndex]));
            newPointsIndex++;

            for (int i = Math.max(minAngleVertexIndex, maxAngleVertexIndex);
                 i < convexHullPolygon.npoints;
                 i++) {
                xpoints[newPointsIndex] = convexHullPolygon.xpoints[i];
                ypoints[newPointsIndex] = convexHullPolygon.ypoints[i];
                points.add(new Point(xpoints[newPointsIndex], ypoints[newPointsIndex]));
                newPointsIndex++;
            }

            convexHullPolygon = new Polygon(xpoints, ypoints, newPointsIndex);
        }
    }

    /**
     * Adds the point to the internal point-list for future calculation. This method does not trigger Convex Hull calculation algorithm.
     *
     * @param x X Coordinate of the added point
     * @param y Y Coordinate of the added point
     */
    @Override
    public void addPoint(int x, int y) {
        addPoint(new Point(x, y));
    }

    /**
     * Adds the point to the internal point-list for future calculation. This method does not trigger Convex Hull calculation algorithm.
     *
     * @param point Coordinates of the added point
     */
    @Override
    public void addPoint(Point point) {
        if (removedPoints.contains(point)) {
            removedPoints.remove(point);
        } else {
            addedPoints.add(point);
        }
    }

    /**
     * Removes the point from the internal point-list for future calculation. This method does not trigger Convex Hull calculation algorithm.
     *
     * @param x X Coordinate of the removed point
     * @param y Y Coordinate of the removed point
     */
    @Override
    public void removePoint(int x, int y) {
        removePoint(new Point(x, y));
    }

    /**
     * Removes the point from the internal point-list for future calculation. This method does not trigger Convex Hull calculation algorithm.
     *
     * @param point Coordinates of the removed point
     */
    @Override
    public void removePoint(Point point) {
        if (addedPoints.contains(point)) {
            addedPoints.remove(point);
        } else {
            removedPoints.add(point);
        }
    }

    /**
     * Boolean flag accessor showing if the cached convex hull polygon kept in {@link #convexHullPolygon} is outdated.
     *
     * @return true if points data has been changed since last time convexHullPolygon is created, false otherwise.
     */
    private boolean isDataUpdated() {
        return !addedPoints.isEmpty() || !removedPoints.isEmpty();
    }

    //TODO @BrainX Optimize this method whenever it's used.

    /**
     * Simply delegates to {@link #addPoint(Point)} and {@link #removePoint(Point)}.
     *
     * @param addedPoints   Coordinates of the added points
     * @param removedPoints Coordinates of the removed points
     */
    @Override
    public void updatePoints(Collection<Point> addedPoints, Collection<Point> removedPoints) {
        if (addedPoints != null) {
            for (Point addedPoint : addedPoints) {
                addPoint(addedPoint);
            }
        }

        if (removedPoints != null) {
            for (Point removedPoint : removedPoints) {
                removePoint(removedPoint);
            }
        }
    }

    /**
     * Cross-library conversion method
     *
     * @param points List of points to be converted
     * @return Converted points
     */
    private static List<Point2D> convertPoints(List<Point> points) {
        List<Point2D> point2ds = new ArrayList<Point2D>();
        for (Point point : points) {
            point2ds.add(new Point2D(point.x, point.y));
        }
        return point2ds;
    }

    /**
     * Cross-library conversion method
     *
     * @param polygon2d Polygon2D to be converted
     * @return Converted polygon
     */
    private static Polygon convertPolygon2d(Polygon2D polygon2d) {
        Collection<Point2D> vertices = polygon2d.getVertices();
        int[] xPoints = new int[vertices.size()];
        int[] yPoints = new int[vertices.size()];
        int i = 0;
        for (Point2D point2d : vertices) {
            xPoints[i] = (int) point2d.x;
            yPoints[i] = (int) point2d.y;
            i++;
        }
        return new Polygon(xPoints, yPoints, vertices.size());
    }

}
