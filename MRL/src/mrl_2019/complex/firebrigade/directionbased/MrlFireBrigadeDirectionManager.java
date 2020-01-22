package mrl_2019.complex.firebrigade.directionbased;

import adf.agent.info.AgentInfo;
import javolution.util.FastList;
import javolution.util.FastSet;

import mrl_2019.algorithm.clustering.ConvexObject;
import mrl_2019.algorithm.clustering.FireCluster;
import mrl_2019.complex.firebrigade.MrlFireBrigadeWorld;
import mrl_2019.util.Util;
import mrl_2019.world.entity.MrlBuilding;
import rescuecore2.standard.entities.GasStation;
import rescuecore2.standard.entities.Refuge;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: 1/26/12
 * Time: 3:27 PM
 */
public class MrlFireBrigadeDirectionManager {
//    private static final double TARGET_DISTANCE_THRESHOLD = ClusterManager.CLUSTER_RANGE_THRESHOLD;
    MrlFireBrigadeWorld world;
    Point mapCenter;
    int DIRECTION_THRESHOLD = 10000;
    double REFUGE_COEFFICIENT = 0.001;
    double GAS_STATION_COEFFICIENT = 0.4;
    double lastDegree = 0;
    private int theta = 20;//30;
    math.geom2d.Point2D targetPoint;
    math.geom2d.Point2D lastTargetPoint;
    private static final int SWITCH_THRESHOLD_VALUE = 3;
    int switchThreshold = SWITCH_THRESHOLD_VALUE;

    List<MrlBuilding> highValueBuildings;//added by Sajjad
    AgentInfo agentInfo;

    public MrlFireBrigadeDirectionManager(MrlFireBrigadeWorld world, AgentInfo agentInfo) {
        this.world = world;
        this.agentInfo = agentInfo;
        mapCenter = new Point(world.getCenterOfMap().getX(), world.getCenterOfMap().getY());
        //highValueBuildings = new ArrayList<MrlBuilding>();
    }

    public Point findBestDirectionPoint(FireCluster fireCluster, List<FireCluster> fireClusters) {
        math.geom2d.Point2D fireClusterCenter = new math.geom2d.Point2D(fireCluster.getCenter());
        math.geom2d.Point2D[] points = new math.geom2d.Point2D[4];
        points[0] = new math.geom2d.Point2D(0, 0);
        points[1] = new math.geom2d.Point2D(0, world.getMapHeight());
        points[2] = new math.geom2d.Point2D(world.getMapWidth(), 0);
        points[3] = new math.geom2d.Point2D(world.getMapWidth(), world.getMapHeight());
        double maxDistance = Double.MIN_VALUE;
        math.geom2d.Point2D tempTargetPoint = null;
        for (math.geom2d.Point2D point : points) {
            double distance = Util.distance(point, fireClusterCenter);
            if (distance > maxDistance) {
                maxDistance = distance;
                tempTargetPoint = point;
            }
        }

        targetPoint = new math.geom2d.Point2D(tempTargetPoint.getX(), tempTargetPoint.getY());

        List<FireCluster> nearClusters = new ArrayList<FireCluster>();
        for (FireCluster next : fireClusters) {
            if (next.equals(fireCluster)) {
                continue;
            }

            if (Util.distance(next.getConvexHullObject().getConvexPolygon(), fireCluster.getConvexHullObject().getConvexPolygon()) < world.getFireClustering().getClusterRangeThreshold() * 3) {
                nearClusters.add(next);
            }
        }
        double degree = 0;
        while (degree < 360) {
            if (!isThereClusterInDirectionOf(targetPoint, fireCluster, nearClusters)) {
                if (fireCluster.hasBuildingInDirection(new Point((int) targetPoint.getX(), (int) targetPoint.getY()), true, false)) {
                    break;
                }
            }
            targetPoint = targetPoint.rotate(fireClusterCenter, Math.toRadians((agentInfo.getID().getValue() % 2) == 0 ? theta : -theta));
            degree += theta;
        }
        /*if (degree >= 360) {
            return null;
        }*/
        return new Point((int) targetPoint.getX(), (int) targetPoint.getY());

    }

    private boolean isThereClusterInDirectionOf(math.geom2d.Point2D targetPoint, FireCluster sourceCluster, List<FireCluster> nearClusters) {
        rescuecore2.misc.geometry.Line2D line = new rescuecore2.misc.geometry.Line2D(targetPoint.getX(), targetPoint.getY(), sourceCluster.getCenter().getX(), sourceCluster.getCenter().getY());
        for (FireCluster fireCluster : nearClusters) {
            if (Util.hasIntersectionLines(fireCluster.getConvexHullObject().getConvexPolygon(), line)) {
                return true;
            }
        }
        return false;
    }

    public Point findFarthestPointOfMap(FireCluster fireCluster) {
        math.geom2d.Point2D fireClusterCenter = new math.geom2d.Point2D(fireCluster.getCenter());
        List<rescuecore2.misc.geometry.Point2D> points = new ArrayList<rescuecore2.misc.geometry.Point2D>();
        points.add(new rescuecore2.misc.geometry.Point2D(0, 0));
        points.add(new rescuecore2.misc.geometry.Point2D(0, world.getMapHeight()));
        points.add(new rescuecore2.misc.geometry.Point2D(world.getMapWidth(), 0));
        points.add(new rescuecore2.misc.geometry.Point2D(world.getMapWidth(), world.getMapHeight()));


        rescuecore2.misc.geometry.Point2D farthestPointOfMap = Util.findFarthestPoint(fireCluster.getConvexHullObject().getConvexPolygon(), points);

        targetPoint = new math.geom2d.Point2D(farthestPointOfMap.getX(), farthestPointOfMap.getY());

        double degree = 0;
        while (degree < 360) {
            if (fireCluster.hasBuildingInDirection(new Point((int) targetPoint.getX(), (int) targetPoint.getY()), true, false)) {
                break;
            }
            targetPoint = targetPoint.rotate(fireClusterCenter, Math.toRadians((agentInfo.getID().getValue() % 2) == 0 ? theta : -theta));
            degree += theta;
        }
        return new Point((int) targetPoint.getX(), (int) targetPoint.getY());

    }

    /**
     * create max 8 and min 3 cube with fire cluster bound2D and calculate buildings area in each polygon.
     * <p/>
     * ____ ____ ____max map x,y
     * |    |    |    |
     * |____|____|____|
     * |    |####|    |
     * |____|####|____|
     * |    |    |    |
     * y |____|____|____|
     * x
     *
     * @param fireCluster target cluster
     * @param clusters    all fire clusters
     * @return point for direction target
     */
    public Point findDirectionPointInMap(FireCluster fireCluster, List<FireCluster> clusters) {
        List<DirectionObject> polygons = new FastList<DirectionObject>();
        Rectangle2D fireClusterShape = fireCluster.getConvexHullObject().getConvexPolygon().getBounds2D();
        math.geom2d.Point2D fireClusterCenter = new math.geom2d.Point2D(fireCluster.getCenter());
        int mapMaxX = (int) world.getBounds().getWidth();
        int mapMaxY = (int) world.getBounds().getHeight();
        int clusterMinX = (int) fireClusterShape.getMinX();
        int clusterMaxX = (int) fireClusterShape.getMaxX();
        int clusterMinY = (int) fireClusterShape.getMinY();
        int clusterMaxY = (int) fireClusterShape.getMaxY();
//        boolean IAmOddIndex = (agentInfo.getID().getValue() % 2) == 0;
        boolean IAmOddIndex = (world.getFireBrigadeList().indexOf(world.getSelfHuman()) % 2) == 0;
        Polygon polygon;
        if (clusterMinY > 0) {
            if (clusterMinX > 0) {
                polygon = new Polygon();
                polygon.addPoint(0, 0);
                polygon.addPoint(clusterMinX, 0);
                polygon.addPoint(clusterMinX, clusterMinY);
                polygon.addPoint(0, clusterMinY);
                polygons.add(new DirectionObject(polygon, DirectionSide.Southwest));
            }

            polygon = new Polygon();
            polygon.addPoint(clusterMinX, 0);
            polygon.addPoint(clusterMaxX, 0);
            polygon.addPoint(clusterMaxX, clusterMinY);
            polygon.addPoint(clusterMinX, clusterMinY);
            polygons.add(new DirectionObject(polygon, DirectionSide.South));

            if (clusterMaxX < mapMaxX) {
                polygon = new Polygon();
                polygon.addPoint(clusterMaxX, 0);
                polygon.addPoint(mapMaxX, 0);
                polygon.addPoint(mapMaxX, clusterMinY);
                polygon.addPoint(clusterMaxX, clusterMinY);
                polygons.add(new DirectionObject(polygon, DirectionSide.Southeast));
            }
        }
        if (clusterMinX > 0) {
            polygon = new Polygon();
            polygon.addPoint(0, clusterMinY);
            polygon.addPoint(clusterMinX, clusterMinY);
            polygon.addPoint(clusterMinX, clusterMaxY);
            polygon.addPoint(0, clusterMaxY);
            polygons.add(new DirectionObject(polygon, DirectionSide.West));
        }
        if (clusterMaxX < mapMaxX) {
            polygon = new Polygon();
            polygon.addPoint(clusterMaxX, clusterMinY);
            polygon.addPoint(mapMaxX, clusterMinY);
            polygon.addPoint(mapMaxX, clusterMaxY);
            polygon.addPoint(clusterMaxX, clusterMaxY);
            polygons.add(new DirectionObject(polygon, DirectionSide.East));
        }
        if (clusterMaxY < mapMaxY) {
            if (clusterMinX > 0) {
                polygon = new Polygon();
                polygon.addPoint(0, clusterMaxY);
                polygon.addPoint(clusterMinX, clusterMaxY);
                polygon.addPoint(clusterMinX, mapMaxY);
                polygon.addPoint(0, mapMaxY);
                polygons.add(new DirectionObject(polygon, DirectionSide.Northwest));
            }

            polygon = new Polygon();
            polygon.addPoint(clusterMinX, clusterMaxY);
            polygon.addPoint(clusterMaxX, clusterMaxY);
            polygon.addPoint(clusterMaxX, mapMaxY);
            polygon.addPoint(clusterMinX, mapMaxY);
            polygons.add(new DirectionObject(polygon, DirectionSide.North));

            if (clusterMaxX < mapMaxX) {
                polygon = new Polygon();
                polygon.addPoint(clusterMaxX, clusterMaxY);
                polygon.addPoint(mapMaxX, clusterMaxY);
                polygon.addPoint(mapMaxX, mapMaxY);
                polygon.addPoint(clusterMaxX, mapMaxY);
                polygons.add(new DirectionObject(polygon, DirectionSide.Northeast));
            }
        }
        double maxValue = Double.MIN_VALUE;
        DirectionObject selectedDirection = null;
        Set<DirectionSide> ignoredSides = new FastSet<DirectionSide>();
        for (DirectionObject direction : polygons) {
            Polygon poly = direction.getPolygon();
            boolean hasFire = false;
            //int minWidth = (int) Math.min(poly.getBounds2D().getWidth(), poly.getBounds2D().getHeight()) / 5;

            for (FireCluster c : clusters) {
                if (!c.isDying() && poly.contains(c.getCenter())) {
                    //int clusterMaxWidth = (int) Math.max(poly.getBounds2D().getWidth(), poly.getBounds2D().getHeight());
                    if (!c.isControllable()) {
//                    if (clusterMaxWidth > minWidth) {
                        hasFire = true;
                        break;
                    }
                }
            }
            if (hasFire) {
                ignoredSides.add(direction.getSide());
                direction.setValue(0.0);
                continue;
            }
            double value = 0;
            int refugeNo = 0;
            int gasStationNo = 0;
            for (MrlBuilding building : world.getMrlBuildings()) {
                if (building.getEstimatedFieryness() == 0 && poly.contains(building.getSelfBuilding().getX(), building.getSelfBuilding().getY()) && !world.getBorderBuildings().contains(building.getID())) {
                    if (building.getSelfBuilding() instanceof Refuge) {
//                        value += 1000;
                        refugeNo++;
                    } else if (building.getSelfBuilding() instanceof GasStation) {
//                        value += 1000;
                        gasStationNo++;
                    } else {
                        value += building.getSelfBuilding().getTotalArea();
                    }
                }
            }

            if (value == 0) {
                ignoredSides.add(direction.getSide());
            } else {
                value += (refugeNo * REFUGE_COEFFICIENT * value);
                value += (gasStationNo * GAS_STATION_COEFFICIENT * value);
            }
            direction.setValue(value);
            direction.setRefugeNo(refugeNo);
            direction.setGasStationNo(gasStationNo);
            if (maxValue < value) {
                maxValue = value;
                selectedDirection = direction;
            }
        }

        if (selectedDirection != null) {
            Polygon selectedPolygon = selectedDirection.getPolygon();
            targetPoint = new math.geom2d.Point2D(selectedPolygon.getBounds2D().getCenterX(), selectedPolygon.getBounds2D().getCenterY());
            double degree = 0;
            while (degree < 360) {
                if (isIgnoredDirection(selectedDirection.getSide(), (int) degree, !IAmOddIndex, ignoredSides)) {
                    targetPoint = targetPoint.rotate(fireClusterCenter, Math.toRadians(IAmOddIndex ? theta : -theta));
                    degree += theta;
                    continue;
                }
                if (fireCluster.hasBuildingInDirection(new Point((int) targetPoint.getX(), (int) targetPoint.getY()), true, false)) {
                    break;
                }
                targetPoint = targetPoint.rotate(fireClusterCenter, Math.toRadians(IAmOddIndex ? theta : -theta));
                degree += theta;
            }
//            debounceDirectionSwitch(targetPoint);
        } else {
//            world.printData("I can't find the best direction. but goto(findFarthestPointOfMap)");
            findFarthestPointOfMap(fireCluster);
        }
        //MrlPersonalData.VIEWER_DATA.setDirectionPolygons(agentInfo.getID(), polygons);
        return new Point((int) targetPoint.getX(), (int) targetPoint.getY());
    }

    private boolean isIgnoredDirection(DirectionSide side, int degree, boolean clockwise, Set<DirectionSide> ignoredSides) {
        int ord = side.ordinal();
        if (!clockwise) {
            degree = 360 - degree;
        }

        int nowOrd;

        switch (degree) {
            case 0:
                nowOrd = ord;
                break;
            case 20:
                nowOrd = ord;
                break;
            case 40:
                nowOrd = 1 + ord;
                break;
            case 60:
                nowOrd = 1 + ord;
                break;
            case 80:
                nowOrd = 2 + ord;
                break;
            case 100:
                nowOrd = 2 + ord;
                break;
            case 120:
                nowOrd = 3 + ord;
                break;
            case 140:
                nowOrd = 3 + ord;
                break;
            case 160:
                nowOrd = 4 + ord;
                break;
            case 180:
                nowOrd = 4 + ord;
                break;
            case 200:
                nowOrd = 4 + ord;
                break;
            case 220:
                nowOrd = 5 + ord;
                break;
            case 240:
                nowOrd = 5 + ord;
                break;
            case 260:
                nowOrd = 6 + ord;
                break;
            case 280:
                nowOrd = 6 + ord;
                break;
            case 300:
                nowOrd = 7 + ord;
                break;
            case 320:
                nowOrd = 7 + ord;
                break;
            case 340:
                nowOrd = ord;
                break;
            default:
                nowOrd = ord;
        }
        nowOrd = nowOrd % DirectionSide.values().length;
        DirectionSide nowDir = DirectionSide.values()[nowOrd];

        return ignoredSides.contains(nowDir);
    }

    public void setCenterBasedDirection(ConvexObject convexObject) {
        getDirectionBorder(mapCenter, convexObject);
    }

    /**
     * be dast avordane khate direction be samte markaze chegali map
     *
     * @param densityCenter map density center
     * @param convexHull    fire convex hull
     * @return direction lines
     */
    private Set<Line2D> getDirectionBorder(Point densityCenter, ConvexObject convexHull) {
        Polygon convexPoly = convexHull.getConvexPolygon();
        double radiusLength = Math.sqrt(Math.pow(convexPoly.getBounds().getHeight(), 2) + Math.pow(convexPoly.getBounds().getWidth(), 2));

        Point convexPoint = new Point((int) convexPoly.getBounds().getCenterX(), (int) convexPoly.getBounds().getCenterY());
        Point[] points = getPerpendicularPoints(densityCenter, convexPoint, radiusLength);
        Point point1 = points[0];
        Point point2 = points[1];
        //MrlPersonalData.VIEWER_DATA.setConvexCenterPoint(convexHull, densityCenter);
        //MrlPersonalData.VIEWER_DATA.setCenterPoint(agentInfo.getID(), new Pair<Point, ConvexObject>(densityCenter, convexHull));
        convexHull.FIRST_POINT = point1;
        convexHull.SECOND_POINT = point2;
        convexHull.CONVEX_POINT = convexPoint;

        Polygon trianglePoly = new Polygon();
        trianglePoly.addPoint(point1.x, point1.y);
        trianglePoly.addPoint(convexPoint.x, convexPoint.y);
        trianglePoly.addPoint(point2.x, point2.y);

        convexHull.setTrianglePolygon(trianglePoly);
        {//get other side of triangle
            double distance = point1.distance(point2) / 3; //mostafa commented this line
            points = getPerpendicularPoints(point2, point1, distance);
            if (convexPoint.distance(points[0]) >= convexPoint.distance(points[1])) {
                trianglePoly.addPoint(points[0].x, points[0].y);
                convexHull.OTHER_POINT2 = new Point(points[0].x, points[0].y);
            } else {
                trianglePoly.addPoint(points[1].x, points[1].y);
                convexHull.OTHER_POINT2 = new Point(points[1].x, points[1].y);
            }

            points = getPerpendicularPoints(point1, point2, distance);
            if (convexPoint.distance(points[0]) >= convexPoint.distance(points[1])) {
                trianglePoly.addPoint(points[0].x, points[0].y);
                convexHull.OTHER_POINT1 = new Point(points[0].x, points[0].y);
            } else {
                trianglePoly.addPoint(points[1].x, points[1].y);
                convexHull.OTHER_POINT1 = new Point(points[1].x, points[1].y);
            }
        }

        try {
            return getIntersections(convexPoly, new Line2D.Double(point1, convexPoint), new Line2D.Double(point2, convexPoint), trianglePoly);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Set<Line2D> getLimitDirectionBorder(Point densityCenter, ConvexObject convexHull) {
        Polygon convexPoly = convexHull.getConvexPolygon();
        //--------------Mostafa-------------
        double distance; //distance of fire convex hull to map center density
        distance = Util.distance(convexHull.getConvexPolygon(), new rescuecore2.misc.geometry.Point2D(densityCenter.getX(), densityCenter.getY()));
        double radiusLength = distance;
        //----------------------------------
//        double radiusLength = Math.sqrt(Math.pow(convexPoly.getBounds().getHeight(), 2) + Math.pow(convexPoly.getBounds().getWidth(), 2));//mostafa commented this line

        Point convexPoint = new Point((int) convexPoly.getBounds().getCenterX(), (int) convexPoly.getBounds().getCenterY());
        Point[] points = getPerpendicularPoints(densityCenter, convexPoint, radiusLength);
        Point point1 = points[0];
        Point point2 = points[1];

//        MrlPersonalData.VIEWER_DATA.setConvexCenterPoint(convexHull, densityCenter);
//        MrlPersonalData.VIEWER_DATA.setCenterPoint(agentInfo.getID(), new Pair<Point, ConvexObject>(densityCenter, convexHull));
        convexHull.FIRST_POINT = point1;
        convexHull.SECOND_POINT = point2;
        convexHull.CONVEX_POINT = convexPoint;

        Polygon trianglePoly = new Polygon();
        trianglePoly.addPoint(point1.x, point1.y);
        trianglePoly.addPoint(convexPoint.x, convexPoint.y);
        trianglePoly.addPoint(point2.x, point2.y);

        convexHull.setTrianglePolygon(trianglePoly);
        {//get other side of triangle
            //double distance = point1.distance(point2) / 3; //mostafa commented this line
            points = getPerpendicularPoints(point2, point1, distance);
            if (convexPoint.distance(points[0]) >= convexPoint.distance(points[1])) {
                trianglePoly.addPoint(points[0].x, points[0].y);
                convexHull.OTHER_POINT2 = new Point(points[0].x, points[0].y);
            } else {
                trianglePoly.addPoint(points[1].x, points[1].y);
                convexHull.OTHER_POINT2 = new Point(points[1].x, points[1].y);
            }

            points = getPerpendicularPoints(point1, point2, distance);
            if (convexPoint.distance(points[0]) >= convexPoint.distance(points[1])) {
                trianglePoly.addPoint(points[0].x, points[0].y);
                convexHull.OTHER_POINT1 = new Point(points[0].x, points[0].y);
            } else {
                trianglePoly.addPoint(points[1].x, points[1].y);
                convexHull.OTHER_POINT1 = new Point(points[1].x, points[1].y);
            }
        }

        try {
            return getIntersections(convexPoly, new Line2D.Double(point1, convexPoint), new Line2D.Double(point2, convexPoint), trianglePoly);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * bedat avordane do noghte amood bar khate hasele az tdo noghte dade shode be faseleye khaste shode.
     *
     * @param point1       main point
     * @param point2       second point for create line
     * @param radiusLength points distance to line
     * @return two points
     */
    public static Point[] getPerpendicularPoints(Point2D point1, Point2D point2, double radiusLength) {
        double x1 = point1.getX();
        double y1 = point1.getY();
        double x2 = point2.getX();
        double y2 = point2.getY();

        double m1 = (y1 - y2) / (x1 - x2);
        double m2 = (-1 / m1);
        double a = Math.pow(m2, 2) + 1;
        double b = (-2 * x1) - (2 * Math.pow(m2, 2) * x1);
        double c = (Math.pow(x1, 2) * (Math.pow(m2, 2) + 1)) - Math.pow(radiusLength, 2);

        double x3 = ((-1 * b) + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
        double x4 = ((-1 * b) - Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
        double y3 = (m2 * x3) - (m2 * x1) + y1;
        double y4 = (m2 * x4) - (m2 * x1) + y1;

        Point perpendicular1 = new Point((int) x3, (int) y3);
        Point perpendicular2 = new Point((int) x4, (int) y4);
        return new Point[]{perpendicular1, perpendicular2};
    }

    public static Set<Line2D> getIntersections(Polygon poly, Line2D line1, Line2D line2, Polygon trianglePoly) throws Exception {

        PathIterator polyIt = poly.getPathIterator(null); //Getting an iterator along the polygon path
        double[] coords = new double[6]; //Double array with length 6 needed by iterator
        double[] firstCoords = new double[2]; //First point (needed for closing polygon path)
        double[] lastCoords = new double[2]; //Previously visited point
        Set<Line2D> intersections = new FastSet<Line2D>(); //List to hold found intersections

        polyIt.currentSegment(firstCoords); //Getting the first coordinate pair
        lastCoords[0] = firstCoords[0]; //Priming the previous coordinate pair
        lastCoords[1] = firstCoords[1];
        polyIt.next();

        while (!polyIt.isDone()) {
            int type = polyIt.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_LINETO: {
                    Line2D.Double currentLine = new Line2D.Double(lastCoords[0], lastCoords[1], coords[0], coords[1]);
                    if (trianglePoly.contains(lastCoords[0], lastCoords[1]) || trianglePoly.contains(coords[0], coords[1])) {
                        intersections.add(currentLine);
                    }
                    if (currentLine.intersectsLine(line1)) {
                        intersections.add(currentLine);
                    }
                    if (currentLine.intersectsLine(line2)) {
                        intersections.add(currentLine);
                    }
                    lastCoords[0] = coords[0];
                    lastCoords[1] = coords[1];
                    break;
                }
                case PathIterator.SEG_CLOSE: {
                    Line2D.Double currentLine = new Line2D.Double(coords[0], coords[1], firstCoords[0], firstCoords[1]);
                    if (trianglePoly.contains(coords[0], coords[1]) || trianglePoly.contains(firstCoords[0], firstCoords[1])) {
                        intersections.add(currentLine);
                    }
                    if (currentLine.intersectsLine(line1)) {
                        intersections.add(currentLine);
                    }
                    if (currentLine.intersectsLine(line2)) {
                        intersections.add(currentLine);
                    }
                    break;
                }
                default: {
                    throw new Exception("Unsupported PathIterator segment type.");
                }
            }
            polyIt.next();
        }
        return intersections;

    }

    public static Point2D getIntersection(final Line2D.Double line1, final Line2D.Double line2) {
        double x1, y1, x2, y2, x3, y3, x4, y4;
        x1 = line1.x1;
        y1 = line1.y1;
        x2 = line1.x2;
        y2 = line1.y2;
        x3 = line2.x1;
        y3 = line2.y1;
        x4 = line2.x2;
        y4 = line2.y2;
        double x = ((x2 - x1) * (x3 * y4 - x4 * y3) - (x4 - x3) * (x1 * y2 - x2 * y1))
                / ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));
        double y = ((y3 - y4) * (x1 * y2 - x2 * y1) - (y1 - y2) * (x3 * y4 - x4 * y3))
                / ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));
        return new Point2D.Double(x, y);
    }

    public List<MrlBuilding> getHighValueBuildings() {
        return highValueBuildings;
    }

    public int getTheta() {
        return theta;
    }

    public void setTheta(int theta) {
        this.theta = theta;
    }

    private void debounceDirectionSwitch(math.geom2d.Point2D targetPoint) {
        if (lastTargetPoint == null) {
            lastTargetPoint = targetPoint;
            return;
        }
        if (!lastTargetPoint.equals(targetPoint)) {
            if (switchThreshold == 0) {
                switchThreshold = SWITCH_THRESHOLD_VALUE;
                lastTargetPoint = targetPoint;
            } else {
                switchThreshold--;
                this.targetPoint = lastTargetPoint;
            }
        }
    }
}
