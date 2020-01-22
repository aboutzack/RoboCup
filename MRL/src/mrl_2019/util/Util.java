package mrl_2019.util;

import adf.agent.info.AgentInfo;
import adf.agent.info.WorldInfo;
import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import math.geom2d.line.LineSegment2D;
import mrl_2019.world.MrlWorldHelper;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User: mrl_2019
 * Date: May 5, 2010
 * Time: 2:17:52 PM
 */
public class Util {


    public static boolean intersects(rescuecore2.misc.geometry.Line2D lineSegment1, rescuecore2.misc.geometry.Line2D lineSegment2) {
        lineSegment1.getIntersection(lineSegment2);
        LineSegment2D line1 = new LineSegment2D(lineSegment1.getOrigin().getX(), lineSegment1.getOrigin().getY(), lineSegment1.getEndPoint().getX(), lineSegment1.getEndPoint().getY());
        LineSegment2D line2 = new LineSegment2D(lineSegment2.getOrigin().getX(), lineSegment2.getOrigin().getY(), lineSegment2.getEndPoint().getX(), lineSegment2.getEndPoint().getY());
        return intersects(line1, line2);
    }

    public static boolean intersects(LineSegment2D lineSegment1, LineSegment2D lineSegment2) {
        return LineSegment2D.intersects(lineSegment1, lineSegment2);
    }


    public static List<Integer> fetchIdValueFormElements(Collection<StandardEntity> elements) {
        return elements.stream().map(entity -> entity.getID().getValue()).collect(Collectors.toList());
    }

    public static List<Integer> fetchIdValueFormElementIds(Collection<EntityID> elementEntityIDs) {
        return elementEntityIDs.stream().map(EntityID::getValue).collect(Collectors.toList());
    }

    public static double findDistanceToNearest(MrlWorldHelper world, Collection<StandardEntity> entities, Point targetPoint) {
        double minDistance = Double.MAX_VALUE;
        for (StandardEntity next : entities) {
            double distance = distance(world.getWorldInfo().getLocation(next), targetPoint);
            if (distance < minDistance) {
                minDistance = distance;

            }
        }
        return minDistance;
    }

    /**
     * finds farthest point of @param points from polygon
     *
     * @param polygon
     * @return
     */
    public static rescuecore2.misc.geometry.Point2D findFarthestPoint(Polygon polygon, List<rescuecore2.misc.geometry.Point2D> points) {
        rescuecore2.misc.geometry.Point2D farthestPoint = null;
        List<Pair<rescuecore2.misc.geometry.Point2D, Double>> pointsDistancesToPolygon = new ArrayList<Pair<rescuecore2.misc.geometry.Point2D, Double>>();
        for (rescuecore2.misc.geometry.Point2D point : points) {
            pointsDistancesToPolygon.add(new Pair<rescuecore2.misc.geometry.Point2D, Double>(point, distance(polygon, point, true)));
        }
        double maxDistance = Double.MIN_VALUE;
        for (Pair<rescuecore2.misc.geometry.Point2D, Double> pair : pointsDistancesToPolygon) {
            if (pair.second() > maxDistance) {
                maxDistance = pair.second();
                farthestPoint = pair.first();
            }
        }
        return farthestPoint;
    }

    public static List<Integer> EIDListToIntegerList(List<EntityID> entityIDs) {
        List<Integer> result = new ArrayList<Integer>();
        for (EntityID next : entityIDs) {
            result.add(next.getValue());
        }
        return result;
    }

    public static List<Integer> EIDListToIntegerList(Set<EntityID> entityIDs) {
        List<Integer> result = new ArrayList<Integer>();
        for (EntityID next : entityIDs) {
            result.add(next.getValue());
        }
        return result;
    }

    public static Set<EntityID> IntegerListToEIDList(List<Integer> integerIds) {
        Set<EntityID> result = new HashSet<>();
        for (Integer next : integerIds) {
            result.add(new EntityID(next));
        }
        return result;
    }

    public static Set<EntityID> IntegerListToEIDSet(List<Integer> integerIds) {
        Set<EntityID> result = new HashSet<>();
        for (Integer next : integerIds) {
            result.add(new EntityID(next));
        }
        return result;
    }

    public static Point getPoint(String value) {
        String[] values = value.split(",");
        return new Point(Integer.parseInt(values[0]), Integer.parseInt(values[1]));

    }

    public static boolean isOnBlockade(WorldInfo worldInfo, AgentInfo agentInfo) {
        if (agentInfo.me() instanceof Human) {
            return isOnBlockade(worldInfo, agentInfo, (Human) agentInfo.me());
        } else {
            return false;
        }
    }

    public static double distance(Polygon polygon, Pair<Integer, Integer> location) {
        return distance(polygon, new rescuecore2.misc.geometry.Point2D(location.first(), location.second()));
    }

    public static boolean isOnBlockade(WorldInfo worldInfo, AgentInfo agentInfo, Human human) {
        if (human != agentInfo.me() && !human.isPositionDefined()) {
            return false;
        }
        StandardEntity se = worldInfo.getEntity(human.getPosition());
        if (se instanceof Road) {
            Blockade blockade;
            Road road = (Road) se;
            if (road.isBlockadesDefined()) {
                for (EntityID id : road.getBlockades()) {
                    blockade = (Blockade) worldInfo.getEntity(id);
                    if (blockade != null && blockade.isApexesDefined()) {
                        if (blockade.getShape().contains(human.getX(), human.getY())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;

    }

    public static boolean isNearBlockade(WorldInfo world, Human human) {
        StandardEntity positionEntity = world.getPosition(human);
        if (!human.isPositionDefined() || !(positionEntity instanceof Area)) {
            return false;
        }
        int humanX, humanY;
        try {
            humanX = human.getX();
            humanY = human.getY();
        } catch (NullPointerException ex) {
//            world.printData("exception in get location");//position was set in message but location was not set.

            Pair<Integer, Integer> location = world.getLocation(human.getPosition());
            humanX = location.first();
            humanY = location.second();
        }
        Area positionArea = (Area) positionEntity;
        List<EntityID> aroundPosition = new ArrayList<EntityID>(positionArea.getNeighbours());
        aroundPosition.add(positionArea.getID());
        StandardEntity entity;
        for (EntityID neighbourID : aroundPosition) {
            StandardEntity se = world.getEntity(neighbourID);
            if (se instanceof Road) {
                Blockade blockade;
                Road road = (Road) se;
                int distance;
                if (road.isBlockadesDefined()) {
                    for (EntityID id : road.getBlockades()) {
                        entity = world.getEntity(id);
                        if (entity == null || !(entity instanceof Blockade)) {
//                            world.printData(entity + "is not instance of Blockade......");
                            continue;
                        }
                        blockade = (Blockade) entity;
                        if (blockade.isApexesDefined()) {
                            distance = findDistanceTo(blockade, humanX, humanY);
                            if (distance < 500) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;

    }

    public static int distance(int x1, int y1, int x2, int y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    public static double distance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return (double) Math.sqrt(dx * dx + dy * dy);
    }

    public static int distance(Point p1, Point p2) {
        double dx = p1.getX() - p2.getX();
        double dy = p1.getY() - p2.getY();
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    public static int distance(Point2D p1, Point2D p2) {
        double dx = p1.getX() - p2.getX();
        double dy = p1.getY() - p2.getY();
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    public static int distance(rescuecore2.misc.geometry.Point2D start, rescuecore2.misc.geometry.Point2D end) {
        double dx = start.getX() - end.getX();
        double dy = start.getY() - end.getY();
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    public static int distance(Point point, Pair<Integer, Integer> pair) {
        double dx = point.getX() - pair.first();
        double dy = point.getY() - pair.second();
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    public static int distance(Pair<Integer, Integer> pair, Point point) {
        double dx = point.getX() - pair.first();
        double dy = point.getY() - pair.second();
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    public static int max(int x, int y) {
        return x >= y ? x : y;
    }

    public static int min(int x, int y) {
        return x <= y ? x : y;
    }

    public static boolean isBetween(double a, double x, double b) {
        if (a > b) {
            double k = a;
            a = b;
            b = k;
        }
        if (x >= a && x <= b) {
            return true;
        }
        return false;
    }

    public static int distance(Area obj1, Area obj2) {
        return distance(obj1.getX(), obj1.getY(), obj2.getX(), obj2.getY());
    }

    public static int distance(Pair<Integer, Integer> obj1, Pair<Integer, Integer> obj2) {
        return distance(obj1.first(), obj1.second(), obj2.first(), obj2.second());
    }

//    public static WithCoordinates nearestAreaTo(Collection<? extends WithCoordinates> collection,WithCoordinates from){
//        int minDistance=Integer.MAX_VALUE;
//        WithCoordinates nearest=null;
//        for(WithCoordinates obj:collection){
//            if(distance(from,obj)<minDistance){
//                minDistance=distance(from,obj);
//                nearest=obj;
//            }
//        }
//        return nearest;
//    }

    public static Area nearestEntityTo(Collection<StandardEntity> collection, Pair<Integer, Integer> from) {
        int minDistance = Integer.MAX_VALUE;
        Area nearest = null;
        int tempDist;
        Area area;
        for (StandardEntity obj : collection) {
            area = (Area) obj;
            tempDist = distance(from.first(), from.second(), area.getX(), area.getY());
            if (tempDist < minDistance) {
                minDistance = tempDist;
                nearest = area;
            }
        }
        return nearest;
    }

    public static Area nearestAreaTo(Collection<Area> collection, Pair<Integer, Integer> from) {
        int minDistance = Integer.MAX_VALUE;
        Area nearest = null;
        int tempDist;
        Area area;
        for (StandardEntity obj : collection) {
            area = (Area) obj;
            tempDist = distance(from.first(), from.second(), area.getX(), area.getY());
            if (tempDist < minDistance) {
                minDistance = tempDist;
                nearest = area;
            }
        }
        return nearest;
    }

    public static <T> List<T> sortByValueInc(final Map<T, Integer> map) {
        List<T> keys = new ArrayList<T>();
        keys.addAll(map.keySet());
        Collections.sort(keys, new Comparator<T>() {
            public int compare(T o1, T o2) {
                Integer v1 = map.get(o1);
                Integer v2 = map.get(o2);
                return v1.compareTo(v2);
            }
        });
        return keys;
    }

    public static <T> List<T> sortByValueDec(final Map<T, Integer> map) {
        List<T> keys = new ArrayList<T>();
        keys.addAll(map.keySet());
        Collections.sort(keys, new Comparator<T>() {
            public int compare(T o1, T o2) {
                Integer v1 = map.get(o1);
                Integer v2 = map.get(o2);
                return v2.compareTo(v1);
            }
        });
        return keys;
    }

    public static Object readObject(String filePath) throws IOException, ClassNotFoundException {
        ObjectInputStream objInput = new ObjectInputStream(new FileInputStream(filePath));
        Object o = objInput.readObject();
        objInput.close();
        return o;
    }

    public static void writeObject(Object object, String filePath) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(filePath));
        objectOutputStream.writeObject(object);
        objectOutputStream.close();

    }

    private static double gaussmf(double x, double sig, double c) {
        return Math.exp(-((x - c) * (x - c)) / (2.0 * sig * sig));
    }

    public static double gauss2mf(double x, double sig1, double c1, double sig2, double c2) {
        if (x <= c1) {
            return gaussmf(x, sig1, c1);
        } else {
            return gaussmf(x, sig2, c2);
        }
    }


    public static List<rescuecore2.misc.geometry.Point2D> getPoint2DList(int[] xs, int[] ys) {

        List<rescuecore2.misc.geometry.Point2D> points = new ArrayList<rescuecore2.misc.geometry.Point2D>();
        for (int i = 0; i < xs.length; i++) {
            points.add(new rescuecore2.misc.geometry.Point2D(xs[i], ys[i]));
        }

        return points;
    }


    /**
     * @param poly main cluster Polygon
     * @param line sub cluster polygon lines
     * @return Set of intersect Point
     */
    public static Set<Point2D> getIntersections(final Polygon poly, final Line2D line)/* throws Exception */ {

        final PathIterator polyIt = poly.getPathIterator(null); //Getting an iterator along the polygon path
        final double[] coords = new double[6]; //Double array with length 6 needed by iterator
        final double[] firstCoords = new double[2]; //First point (needed for closing polygon path)
        final double[] lastCoords = new double[2]; //Previously visited point
        final Set<Point2D> intersections = new HashSet<Point2D>(); //List to hold found intersections
        polyIt.currentSegment(firstCoords); //Getting the first coordinate pair
        lastCoords[0] = firstCoords[0]; //Priming the previous coordinate pair
        lastCoords[1] = firstCoords[1];
        polyIt.next();
        while (!polyIt.isDone()) {
            final int type = polyIt.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_LINETO: {
                    final Line2D.Double currentLine = new Line2D.Double(lastCoords[0], lastCoords[1], coords[0], coords[1]);
                    if (currentLine.intersectsLine(line)) {
                        intersections.add(getIntersection(currentLine, line));
                    }
                    lastCoords[0] = coords[0];
                    lastCoords[1] = coords[1];
                    break;
                }
                case PathIterator.SEG_CLOSE: {
                    final Line2D.Double currentLine = new Line2D.Double(coords[0], coords[1], firstCoords[0], firstCoords[1]);
                    if (currentLine.intersectsLine(line)) {
                        intersections.add(getIntersection(currentLine, line));
                    }
                    break;
                }
                default: {
                    throw new NoSuchElementException("Unsupported PathIterator segment type.");
                }
            }
            polyIt.next();
        }
        return intersections;

    }

//    public static List<rescuecore2.misc.geometry.Point2D> getIntersections(final Polygon poly, final rescuecore2.misc.geometry.Line2D line){
//        List<rescuecore2.misc.geometry.Point2D> intersections = new ArrayList<rescuecore2.misc.geometry.Point2D>();
//        List<rescuecore2.misc.geometry.Line2D> polyLines = getLines(poly);
//        for (rescuecore2.misc.geometry.Line2D polyLine : polyLines){
//            rescuecore2.misc.geometry.Point2D intersect =  polyLine.getIntersection(line);
//        }
//    }


    /**
     * this function calculate the intersect point of polygon and line
     *
     * @param line1 main cluster polygon line
     * @param line2 sub cluster polygon line
     * @return the intersection of two lines point
     */
    public static Point2D getIntersection(final Line2D line1, final Line2D line2) {

        final double x1, y1, x2, y2, x3, y3, x4, y4;
        x1 = line1.getX1();
        y1 = line1.getY1();
        x2 = line1.getX2();
        y2 = line1.getY2();
        x3 = line2.getX1();
        y3 = line2.getY1();
        x4 = line2.getX2();
        y4 = line2.getY2();
        final double x = ((x2 - x1) * (x3 * y4 - x4 * y3) - (x4 - x3) * (x1 * y2 - x2 * y1))
                / ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));
        final double y = ((y3 - y4) * (x1 * y2 - x2 * y1) - (y1 - y2) * (x3 * y4 - x4 * y3))
                / ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));

        return new Point2D.Double(x, y);

    }

    public static rescuecore2.misc.geometry.Point2D getIntersection(final rescuecore2.misc.geometry.Line2D line1, final rescuecore2.misc.geometry.Line2D line2) {

        final double x1, y1, x2, y2, x3, y3, x4, y4;
        x1 = line1.getOrigin().getX();
        y1 = line1.getOrigin().getY();
        x2 = line1.getEndPoint().getX();
        y2 = line1.getEndPoint().getY();
        x3 = line2.getOrigin().getX();
        y3 = line2.getOrigin().getY();
        x4 = line2.getEndPoint().getX();
        y4 = line2.getEndPoint().getY();
        final double x = ((x2 - x1) * (x3 * y4 - x4 * y3) - (x4 - x3) * (x1 * y2 - x2 * y1))
                / ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));
        final double y = ((y3 - y4) * (x1 * y2 - x2 * y1) - (y1 - y2) * (x3 * y4 - x4 * y3))
                / ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));

        return new rescuecore2.misc.geometry.Point2D(x, y);

    }

    public static int findDistanceTo(Blockade b, int x, int y) {
        //        Logger.debug("Finding distance to " + b + " from " + x + ", " + y);
        if (b.getShape().contains(x, y)) {
            return 0;
        }
        List<rescuecore2.misc.geometry.Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(b.getApexes()), true);
        double best = Double.MAX_VALUE;
        rescuecore2.misc.geometry.Point2D origin = new rescuecore2.misc.geometry.Point2D(x, y);
        for (rescuecore2.misc.geometry.Line2D next : lines) {
            rescuecore2.misc.geometry.Point2D closest = GeometryTools2D.getClosestPointOnSegment(next, origin);
            double d = GeometryTools2D.getDistance(origin, closest);
            //            Logger.debug("Next line: " + next + ", closest point: " + closest + ", distance: " + d);
            if (d < best) {
                best = d;
                //                Logger.debug("New best distance");
            }

        }
        return (int) best;
    }

    public static int findDistanceTo(Area area, int x, int y) {
        //        Logger.debug("Finding distance to " + b + " from " + x + ", " + y);
        List<rescuecore2.misc.geometry.Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(area.getApexList()), true);
        double best = Double.MAX_VALUE;
        rescuecore2.misc.geometry.Point2D origin = new rescuecore2.misc.geometry.Point2D(x, y);
        for (rescuecore2.misc.geometry.Line2D next : lines) {
            rescuecore2.misc.geometry.Point2D closest = GeometryTools2D.getClosestPointOnSegment(next, origin);
            double d = GeometryTools2D.getDistance(origin, closest);
            //            Logger.debug("Next line: " + next + ", closest point: " + closest + ", distance: " + d);
            if (d < best) {
                best = d;
                //                Logger.debug("New best distance");
            }

        }
        return (int) best;
    }


    public static Polygon getPolygon(int[] apexes) {
        Polygon polygon = new Polygon();
        for (int i = 0; i < apexes.length; i += 2) {
            polygon.addPoint(apexes[i], apexes[i + 1]);
        }

        return polygon;
    }


    public static Polygon getPolygon(Rectangle2D bound) {
        PathIterator iterator = bound.getPathIterator(null);
        double[] d = new double[6];
        int[] xs = new int[4];
        int[] ys = new int[4];
        int i = 0;
        while (i < 4 && !iterator.isDone()) {
            iterator.currentSegment(d);
            iterator.next();
            xs[i] = (int) d[0];
            ys[i] = (int) d[1];
            i++;
            System.out.println("Bound Poly Points " + Arrays.toString(d));
        }
        return new Polygon(xs, ys, 4);
    }

    public static rescuecore2.misc.geometry.Point2D getMiddle(rescuecore2.misc.geometry.Point2D start, rescuecore2.misc.geometry.Point2D end) {

        double cx = ((start.getX() + end.getX()) / 2);
        double cy = ((start.getY() + end.getY()) / 2);

        return new rescuecore2.misc.geometry.Point2D(cx, cy);

    }

    public static rescuecore2.misc.geometry.Point2D getMiddle(rescuecore2.misc.geometry.Line2D line) {
        return getMiddle(line.getOrigin(), line.getEndPoint());
    }

    public static double lineSegmentAndPointDistance(Line2D line, Point2D point) {
        return Line2D.ptSegDist(line.getP1().getX(), line.getP1().getY(), line.getP2().getX(), line.getP2().getY(), point.getX(), point.getY());
    }

    public static double lineSegmentAndPointDistance(rescuecore2.misc.geometry.Line2D line, rescuecore2.misc.geometry.Point2D point) {
        return Line2D.ptSegDist(line.getOrigin().getX(), line.getOrigin().getY(), line.getEndPoint().getX(), line.getEndPoint().getY(), point.getX(), point.getY());
    }

    public static double edgeAndPointDistance(Edge edge, Point2D point) {
        return Line2D.ptSegDist(edge.getStartX(), edge.getStartY(), edge.getEndX(), edge.getEndY(), point.getX(), point.getY());
    }

    /**
     * MTN
     * get closest point of line from a point
     *
     * @param line  target point that we want get nearest point of it from another point
     * @param point a point2D
     * @return Point2D
     */
    public static rescuecore2.misc.geometry.Point2D closestPoint(rescuecore2.misc.geometry.Line2D line, rescuecore2.misc.geometry.Point2D point) {
        return GeometryTools2D.getClosestPoint(line, point);
//        return null;
//        double slope = slope(line);
//        if(Double.isInfinite(slope)){
//            slope = Double.MAX_VALUE;
//        }
//        double perpendicularSlope = -1/slope;
//        if(Double.isInfinite(perpendicularSlope)){
//            perpendicularSlope = Double.MAX_VALUE;
//        }
//
//
////        double xPer = (point.getY() - line.getY1() -perpendicularSlope*point.getX() + slope*line.getX1())/(slope-perpendicularSlope);
//        double xPer = ((perpendicularSlope*point.getX() - point.getY()) - (slope*line.getX1() - line.getY1()))/(perpendicularSlope-slope);
//        double yPer = perpendicularSlope*(xPer-point.getX()) + point.getY();
//        double yPer2 = slope*(xPer-line.getX1()) + line.getY1();
//
//        return new Point2D.Double(xPer,yPer);
    }

    /**
     * MTN
     * calculate slope of a line
     *
     * @param line line that we want calculate slope of it
     * @return line slope
     */
    public static double slope(Line2D line) {
        double x1 = line.getX1();
        double y1 = line.getY1();
        double x2 = line.getX2();
        double y2 = line.getY2();
        return ((y1 - y2) / (x1 - x2));
    }

    public static double slope(rescuecore2.misc.geometry.Line2D line) {
        double x1 = line.getOrigin().getX();
        double y1 = line.getOrigin().getY();
        double x2 = line.getEndPoint().getX();
        double y2 = line.getEndPoint().getY();
        return ((y1 - y2) / (x1 - x2));
    }


    public static double lineLength(rescuecore2.misc.geometry.Line2D line) {
//        double x1 = line.getOrigin().getX(), y1 = line.getOrigin().getY();
//        double x2 = line.getEndPoint().getX(), y2 = line.getEndPoint().getY();
        return GeometryTools2D.getDistance(line.getOrigin(), line.getEndPoint());
//        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public static boolean containsEach(Collection collection1, Collection collection2) {
        for (Object object : collection1) {
            if (collection2.contains(object)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(rescuecore2.misc.geometry.Line2D line2D, rescuecore2.misc.geometry.Point2D point) {
        return contains(line2D, point, 0);
    }

    public static boolean contains(rescuecore2.misc.geometry.Line2D line2D, rescuecore2.misc.geometry.Point2D point, double threshold) {
        return !(line2D == null || point == null) && Line2D.ptSegDist(line2D.getOrigin().getX(), line2D.getOrigin().getY(), line2D.getEndPoint().getX(), line2D.getEndPoint().getY(), point.getX(), point.getY()) <= threshold;
    }

    public static double distance(rescuecore2.misc.geometry.Line2D line1, rescuecore2.misc.geometry.Line2D line2) {
        if (Line2D.linesIntersect(line1.getOrigin().getX(), line1.getOrigin().getY(), line1.getEndPoint().getX(), line1.getEndPoint().getY(),
                line2.getOrigin().getX(), line2.getOrigin().getY(), line2.getEndPoint().getX(), line2.getEndPoint().getY())) {
            return 0d;
        }
        double dist1 = distance(line1, line2.getOrigin());
        double dist2 = distance(line1, line2.getEndPoint());
        double dist3 = distance(line2, line1.getOrigin());
        double dist4 = distance(line2, line1.getEndPoint());
        double min = Math.min(dist1, dist2);
        min = Math.min(min, dist3);
        min = Math.min(min, dist4);
        return min;
    }

    public static double distance(rescuecore2.misc.geometry.Line2D line, Polygon polygon) {
        List<rescuecore2.misc.geometry.Line2D> polyLines = getLines(polygon);
        double minDist = Double.MAX_VALUE;
        if (line.getEndPoint().equals(line.getOrigin())) {
            return distance(polygon, line.getOrigin(), true);
        }
        for (rescuecore2.misc.geometry.Line2D polyLine : polyLines) {
            minDist = Math.min(minDist, distance(line, polyLine));
            if (minDist == 0.0) {
                break;
            }
        }
        return minDist;
    }

    public static boolean isPassable(Polygon polygon, Polygon polygon1, int agentPassingThreshold) {

        int count = polygon1.npoints;
        int j;
        double tempDistance;
        boolean isPassable = false;
        for (int i = 0; i < count; i++) {
            j = (i + 1) % count;
            rescuecore2.misc.geometry.Point2D startPoint = new rescuecore2.misc.geometry.Point2D(polygon1.xpoints[i], polygon1.ypoints[i]);
            rescuecore2.misc.geometry.Point2D endPoint = new rescuecore2.misc.geometry.Point2D(polygon1.xpoints[j], polygon1.ypoints[j]);
            if (startPoint.equals(endPoint)) {
                continue;
            }
            rescuecore2.misc.geometry.Line2D poly2Line = new rescuecore2.misc.geometry.Line2D(startPoint, endPoint);
            tempDistance = Util.distance(poly2Line, polygon);
            if (tempDistance < agentPassingThreshold) {
                isPassable = true;
                break;
            }
        }
        return isPassable;

    }

    public static boolean isPassable(Polygon polygon, rescuecore2.misc.geometry.Line2D guideLine, int agentPassingThreshold) {
        boolean isPassable = false;
        double tempDistance = Util.distance(guideLine, polygon);
        if (tempDistance > agentPassingThreshold) {
            isPassable = true;
        }
        return isPassable;
    }


    public static double distance(Polygon polygon1, Polygon polygon2) {
        int count = polygon2.npoints;
        double minDistance = Double.MAX_VALUE;
        int j;
        double distance;
        for (int i = 0; i < count; i++) {
            j = (i + 1) % count;
            rescuecore2.misc.geometry.Point2D startPoint = new rescuecore2.misc.geometry.Point2D(polygon2.xpoints[i], polygon2.ypoints[i]);
            rescuecore2.misc.geometry.Point2D endPoint = new rescuecore2.misc.geometry.Point2D(polygon2.xpoints[j], polygon2.ypoints[j]);
            rescuecore2.misc.geometry.Line2D poly2Line = new rescuecore2.misc.geometry.Line2D(startPoint, endPoint);
            distance = distance(poly2Line, polygon1);
            minDistance = Math.min(minDistance, distance);
            if (minDistance == 0.0) {
                break;
            }
        }
        return minDistance;
    }

    public static double distance(rescuecore2.misc.geometry.Line2D line, rescuecore2.misc.geometry.Point2D point) {
        return Line2D.ptSegDist(line.getOrigin().getX(), line.getOrigin().getY(), line.getEndPoint().getX(), line.getEndPoint().getY(), point.getX(), point.getY());
    }

    public static double distance(rescuecore2.misc.geometry.Line2D line, Pair<Integer, Integer> pair) {
        return Line2D.ptSegDist(line.getOrigin().getX(), line.getOrigin().getY(), line.getEndPoint().getX(), line.getEndPoint().getY(), pair.first().doubleValue(), pair.second().doubleValue());
    }

    public static double distance(Polygon polygon, rescuecore2.misc.geometry.Point2D point) {
        if (polygon.contains(point.getX(), point.getY())) {
            return 0;
        }
        int count = polygon.npoints;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            int j = (i + 1) % count;
            rescuecore2.misc.geometry.Point2D stPoint = new rescuecore2.misc.geometry.Point2D(polygon.xpoints[i], polygon.ypoints[i]);
            rescuecore2.misc.geometry.Point2D endPoint = new rescuecore2.misc.geometry.Point2D(polygon.xpoints[j], polygon.ypoints[j]);
            rescuecore2.misc.geometry.Line2D poly2Line = new rescuecore2.misc.geometry.Line2D(stPoint, endPoint);
            double distance = distance(poly2Line, point);
            minDistance = Math.min(minDistance, distance);
            if (minDistance == 0.0) {
                break;
            }
        }
        return minDistance;
    }

    public static double distance(Polygon polygon, rescuecore2.misc.geometry.Point2D point, boolean ignoreInside) {
        if (ignoreInside && polygon.contains(point.getX(), point.getY())) {
            return 0;
        }
        int count = polygon.npoints;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            int j = (i + 1) % count;
            rescuecore2.misc.geometry.Point2D stPoint = new rescuecore2.misc.geometry.Point2D(polygon.xpoints[i], polygon.ypoints[i]);
            rescuecore2.misc.geometry.Point2D endPoint = new rescuecore2.misc.geometry.Point2D(polygon.xpoints[j], polygon.ypoints[j]);
            rescuecore2.misc.geometry.Line2D poly2Line = new rescuecore2.misc.geometry.Line2D(stPoint, endPoint);
            double distance = distance(poly2Line, point);
            minDistance = Math.min(minDistance, distance);
            if (minDistance == 0.0) {
                break;
            }
        }
        return minDistance;
    }

    public static boolean isInside(double pX, double pY, int[] apex) {
        rescuecore2.misc.geometry.Point2D p = new rescuecore2.misc.geometry.Point2D(pX, pY);
        Vector2D v1 = (new rescuecore2.misc.geometry.Point2D(apex[apex.length - 2], apex[apex.length - 1])).minus(p);
        Vector2D v2 = (new rescuecore2.misc.geometry.Point2D(apex[0], apex[1])).minus(p);
        double theta = getAngle(v1, v2);

        for (int i = 0; i < apex.length - 2; i += 2) {
            v1 = (new rescuecore2.misc.geometry.Point2D(apex[i], apex[i + 1])).minus(p);
            v2 = (new rescuecore2.misc.geometry.Point2D(apex[i + 2], apex[i + 3])).minus(p);
            theta += getAngle(v1, v2);
        }
        return Math.round(Math.abs((theta / 2) / Math.PI)) >= 1;
    }

    public static double getAngle(Vector2D v1, Vector2D v2) {
        double flag = (v1.getX() * v2.getY()) - (v1.getY() * v2.getX());
        double angle = Math.acos(((v1.getX() * v2.getX()) + (v1.getY() * v2.getY())) / (v1.getLength() * v2.getLength()));
        if (flag > 0) {
            return angle;
        }
        if (flag < 0) {
            return -1 * angle;
        }
        return 0.0D;
    }

        /**
         * return angle between two line in degree
         *
         * @param line1
         * @param line2
         * @return
         */
    public static double angleBetween2Lines(rescuecore2.misc.geometry.Line2D line1, rescuecore2.misc.geometry.Line2D line2) {
        double theta = Math.acos(line1.getDirection().dot(line2.getDirection()) / (Util.lineLength(line1) * Util.lineLength(line2)));
        return Math.toDegrees(theta);
//        double angle1 = getAngle(line1);
//        double angle2 = getAngle(line2);
//
//        double angle = Math.abs(angle1 - angle2);
//
//        return angle;
    }

    public static double getAngle(rescuecore2.misc.geometry.Line2D line) {
        double x1 = line.getOrigin().getX();
        double y1 = line.getOrigin().getY();
        double x2 = line.getEndPoint().getX();
        double y2 = line.getEndPoint().getX();

        return Math.atan2(y1 - y2, x1 - x2);
    }

    public static Point getPointInPolygon(Polygon polygon) {

        int index;
        double cx = polygon.getBounds().getCenterX();
        double cy = polygon.getBounds().getCenterY();
        Point cp = new Point((int) cx, (int) cy);
        if (polygon.contains(cp)) {

            return cp;
        }
        if (polygon.npoints >= 3) {
            index = 2;
        } else {
            return null;
        }
        Point p1 = new Point(polygon.xpoints[0], polygon.ypoints[0]);
        Point center;
        Point p2;
        do {
            p2 = new Point(polygon.xpoints[index], polygon.ypoints[index]);
            center = new Point((int) (p1.getX() + p2.getX()) / 2, (int) (p1.getY() + p2.getY()) / 2);
            index++;
        } while (index < polygon.npoints && !polygon.contains(center));

        return center;
    }

    public static List<rescuecore2.misc.geometry.Point2D> intersections(Polygon polygon, rescuecore2.misc.geometry.Line2D line) {
        List<rescuecore2.misc.geometry.Point2D> intersections = new ArrayList<rescuecore2.misc.geometry.Point2D>();
        List<rescuecore2.misc.geometry.Line2D> polyLines = getLines(polygon);

        for (rescuecore2.misc.geometry.Line2D ln : polyLines) {
            rescuecore2.misc.geometry.Point2D intersectPoint = GeometryTools2D.getSegmentIntersectionPoint(line, ln);
            if (/*contains(ln, intersectPoint, 5)*/ intersectPoint != null) {
                intersections.add(intersectPoint);
            }
        }
        return intersections;
    }

    public static boolean hasIntersection(Polygon polygon, rescuecore2.misc.geometry.Line2D line) {
        List<rescuecore2.misc.geometry.Line2D> polyLines = getLines(polygon);
        if (polygon.contains(line.getOrigin().getX(), line.getOrigin().getY()) ||
                polygon.contains(line.getEndPoint().getX(), line.getEndPoint().getY())) {
            return true;
        }
        for (rescuecore2.misc.geometry.Line2D ln : polyLines) {
            rescuecore2.misc.geometry.Point2D intersectPoint = GeometryTools2D.getSegmentIntersectionPoint(line, ln);
            if (/*contains(ln, intersectPoint, 5)*/ intersectPoint != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean intersection(Polygon polygon, rescuecore2.misc.geometry.Line2D line) {
        List<rescuecore2.misc.geometry.Line2D> polyLines = getLines(polygon);
        for (rescuecore2.misc.geometry.Line2D ln : polyLines) {
            rescuecore2.misc.geometry.Point2D intersectPoint = GeometryTools2D.getSegmentIntersectionPoint(line, ln);
            if (contains(ln, intersectPoint)) {
                return true;
            }
        }
        return false;
    }

    public static List<rescuecore2.misc.geometry.Line2D> getLines(Polygon polygon) {
        List<rescuecore2.misc.geometry.Line2D> lines = new ArrayList<rescuecore2.misc.geometry.Line2D>();
        int count = polygon.npoints;
        for (int i = 0; i < count; i++) {
            int j = (i + 1) % count;
            rescuecore2.misc.geometry.Point2D p1 = new rescuecore2.misc.geometry.Point2D(polygon.xpoints[i], polygon.ypoints[i]);
            rescuecore2.misc.geometry.Point2D p2 = new rescuecore2.misc.geometry.Point2D(polygon.xpoints[j], polygon.ypoints[j]);
            rescuecore2.misc.geometry.Line2D line = new rescuecore2.misc.geometry.Line2D(p1, p2);
            lines.add(line);
        }
        return lines;
    }

    public static List<rescuecore2.misc.geometry.Line2D> getIntersectionLines(Polygon polygon, rescuecore2.misc.geometry.Line2D line) {
        List<rescuecore2.misc.geometry.Line2D> polyLines = Util.getLines(polygon);
        List<rescuecore2.misc.geometry.Line2D> intersectionLines = new ArrayList<rescuecore2.misc.geometry.Line2D>();
        for (rescuecore2.misc.geometry.Line2D polyLine : polyLines) {
            rescuecore2.misc.geometry.Point2D intersect = getIntersection(line, polyLine);
            if (contains(polyLine, intersect)) {
                intersectionLines.add(polyLine);
            }
        }
        return intersectionLines;
    }

    public static boolean hasIntersectionLines(Polygon polygon, rescuecore2.misc.geometry.Line2D line) {
        List<rescuecore2.misc.geometry.Line2D> polyLines = Util.getLines(polygon);
        List<rescuecore2.misc.geometry.Line2D> intersectionLines = new ArrayList<rescuecore2.misc.geometry.Line2D>();
        for (rescuecore2.misc.geometry.Line2D polyLine : polyLines) {
            rescuecore2.misc.geometry.Point2D intersect = getIntersection(line, polyLine);
            if (intersect != null) {
                return true;
            }
        }
        return false;
    }


    public static Set<rescuecore2.misc.geometry.Point2D> getIntersectionPoints(Polygon polygon, rescuecore2.misc.geometry.Line2D line) {
        Set<rescuecore2.misc.geometry.Point2D> point2Ds = new HashSet<>();
        List<rescuecore2.misc.geometry.Line2D> polyLines = Util.getLines(polygon);
//        List<rescuecore2.misc.geometry.Point2D> point2DList = new ArrayList<>();
        for (rescuecore2.misc.geometry.Line2D polyLine : polyLines) {
            rescuecore2.misc.geometry.Point2D intersect = getIntersection(line, polyLine);
            if (intersect != null && Util.contains(polyLine, intersect, 100)) {
                point2Ds.add(intersect);
            }
        }
        return point2Ds;
    }

    public static Pair<rescuecore2.misc.geometry.Point2D, rescuecore2.misc.geometry.Point2D> get2PointsAroundCenter(Edge entrance, rescuecore2.misc.geometry.Point2D center, int distance) {
        rescuecore2.misc.geometry.Line2D edgeLine = entrance.getLine();
//        Line l = new Line((int) entrance.getOrigin().getX(), (int) entrance.getOrigin().getY(), (int) entrance.getEndPoint().getX(), (int) entrance.getEndPoint().getY());
        double slope = slope(edgeLine);
        int x1, y1, x2, y2;
        if (Double.isInfinite(slope)) {
            x1 = x2 = (int) center.getX();
            y1 = (int) (center.getY() + distance / 2);
            y2 = (int) (center.getY() - distance / 2);
        } else {
            double theta = Math.atan(slope);
            double sin = Math.sin(theta);
            double cos = Math.cos(theta);
            x1 = (int) (center.getX() + distance * cos / 2);
            y1 = (int) (center.getY() + distance * sin / 2);
            x2 = (int) (center.getX() - distance * cos / 2);
            y2 = (int) (center.getY() - distance * sin / 2);
        }
        return new Pair<rescuecore2.misc.geometry.Point2D, rescuecore2.misc.geometry.Point2D>(new rescuecore2.misc.geometry.Point2D(x1, y1), new rescuecore2.misc.geometry.Point2D(x2, y2));
    }

    public static rescuecore2.misc.geometry.Point2D getPointInDistance(Line2D line, rescuecore2.misc.geometry.Point2D from, double distance) {
        rescuecore2.misc.geometry.Point2D point;// = new rescuecore2.misc.geometry.Point2D();
        double x1, y1;
        double deltaX = line.getX1() - line.getX2(), deltaY = line.getY1() - line.getY2();
        double slope = deltaY / deltaX;
        if (Double.isInfinite(slope)) {
            x1 = from.getX();
            y1 = from.getY() + Math.signum(deltaY) * distance;
        } else {
            double theta = Math.atan(slope);
            x1 = from.getX() - Math.signum(deltaX) * distance * Math.cos(theta);
            y1 = from.getY() - Math.signum(deltaX) * distance * Math.sin(theta);
        }
        point = new rescuecore2.misc.geometry.Point2D(x1, y1);
        return point;
    }

    public static rescuecore2.misc.geometry.Line2D nearestLine(List<rescuecore2.misc.geometry.Line2D> lineList, rescuecore2.misc.geometry.Point2D point) {
        rescuecore2.misc.geometry.Line2D nearestLine = null;
        double minDistance = Double.MAX_VALUE;
        for (rescuecore2.misc.geometry.Line2D line : lineList) {
            double distance = lineSegmentAndPointDistance(line, point);
            if (distance < minDistance) {
                minDistance = distance;
                nearestLine = line;
            }
        }
        return nearestLine;
    }

    public static rescuecore2.misc.geometry.Point2D getPoint(Pair<Integer, Integer> position) {
        return new rescuecore2.misc.geometry.Point2D(position.first(), position.second());
    }

    public static rescuecore2.misc.geometry.Line2D improveLine(rescuecore2.misc.geometry.Line2D line, double size) {
        double x0 = line.getOrigin().getX(), x1 = line.getEndPoint().getX();
        double y0 = line.getOrigin().getY(), y1 = line.getEndPoint().getY();
        double deltaY = y1 - y0;
        double deltaX = x1 - x0;
        double xF, yF;
        double slope;
        if (deltaX != 0)
            slope = deltaY / deltaX;
        else {
            if (deltaY > 0)
                slope = Double.MAX_VALUE;
            else
                slope = -Double.MAX_VALUE;
        }

        double theta = Math.atan(slope);
        if (deltaX > 0) {
            xF = x1 + size * Math.abs(Math.cos(theta));
        } else {
            xF = x1 - size * Math.abs(Math.cos(theta));
        }
        if (deltaY > 0) {
            yF = y1 + size * Math.abs(Math.sin(theta));
        } else {
            yF = y1 - size * Math.abs(Math.sin(theta));
        }
        rescuecore2.misc.geometry.Point2D endPoint = new rescuecore2.misc.geometry.Point2D(xF, yF);
        return new rescuecore2.misc.geometry.Line2D(line.getOrigin(), endPoint);
    }

    public static rescuecore2.misc.geometry.Line2D clipLine(rescuecore2.misc.geometry.Line2D line, double size) {
        double length = Util.lineLength(line);
        return improveLine(line, size - length);
    }

    private static double degToRad(double degree) {
        return degree * Math.PI / 180;
    }

    public static Polygon clearAreaRectangle(double agentX, double agentY, double destinationX, double destinationY, double clearRad) {
        int clearLength = (int) Math.hypot(agentX - destinationX, agentY - destinationY);
        Vector2D agentToTarget = new Vector2D(destinationX - agentX, destinationY
                - agentY);

        if (agentToTarget.getLength() > clearLength)
            agentToTarget = agentToTarget.normalised().scale(clearLength);
        agentToTarget = agentToTarget.normalised().scale(agentToTarget.getLength() + 510);

        Vector2D backAgent = (new Vector2D(agentX, agentY))
                .add(agentToTarget.normalised().scale(-510));
        rescuecore2.misc.geometry.Line2D line = new rescuecore2.misc.geometry.Line2D(backAgent.getX(), backAgent.getY(),
                agentToTarget.getX(), agentToTarget.getY());

        Vector2D dir = agentToTarget.normalised().scale(clearRad);
        Vector2D perpend1 = new Vector2D(-dir.getY(), dir.getX());
        Vector2D perpend2 = new Vector2D(dir.getY(), -dir.getX());

        rescuecore2.misc.geometry.Point2D points[] = new rescuecore2.misc.geometry.Point2D[]{
                line.getOrigin().plus(perpend1),
                line.getEndPoint().plus(perpend1),
                line.getEndPoint().plus(perpend2),
                line.getOrigin().plus(perpend2)};
        int[] xPoints = new int[points.length];
        int[] yPoints = new int[points.length];
        for (int i = 0; i < points.length; i++) {
            xPoints[i] = (int) points[i].getX();
            yPoints[i] = (int) points[i].getY();
        }
        return new Polygon(xPoints, yPoints, points.length);
    }

    public static Polygon transformToScreen(Polygon polygon, ScreenTransform screenTransform) {
        int xs[] = new int[polygon.npoints];
        int ys[] = new int[polygon.npoints];
        for (int i = 0; i < polygon.npoints; i++) {
            xs[i] = screenTransform.xToScreen(polygon.xpoints[i]);
            ys[i] = screenTransform.yToScreen(polygon.ypoints[i]);
        }
        return new Polygon(xs, ys, polygon.npoints);
    }

    public static boolean equals(rescuecore2.misc.geometry.Line2D line1, rescuecore2.misc.geometry.Line2D line2) {
        return line1.getOrigin().getX() == line2.getOrigin().getX() && line1.getOrigin().getY() == line2.getOrigin().getY() && line1.getEndPoint().getX() == line2.getEndPoint().getX() && line1.getEndPoint().getY() == line2.getEndPoint().getY();
    }

    public static double distance(Polygon polygon, Point point, boolean igoreInside) {
        return distance(polygon, new rescuecore2.misc.geometry.Point2D(point.getX(), point.getY()), igoreInside);
    }

    public static double distance(Polygon polygon, Pair<Integer, Integer> location, boolean ignoreInside) {
        return distance(polygon, new rescuecore2.misc.geometry.Point2D(location.first(), location.second()), ignoreInside);
    }

    public static Pair<rescuecore2.misc.geometry.Line2D, rescuecore2.misc.geometry.Line2D> clearLengthLines(List<rescuecore2.misc.geometry.Point2D> polygon, double clearRad) {
        rescuecore2.misc.geometry.Line2D firstLine = null;
        rescuecore2.misc.geometry.Line2D secondLine = null;
        rescuecore2.misc.geometry.Line2D tempLine1 = new rescuecore2.misc.geometry.Line2D(polygon.get(0), polygon.get(1));
        double t1 = lineLength(tempLine1);
        if (Math.abs(t1 - clearRad) < 10) {
            firstLine = new rescuecore2.misc.geometry.Line2D(polygon.get(1), polygon.get(2));
            secondLine = new rescuecore2.misc.geometry.Line2D(polygon.get(0), polygon.get(3));
        } else {
            firstLine = tempLine1;
            secondLine = new rescuecore2.misc.geometry.Line2D(polygon.get(3), polygon.get(2));
        }

        return new Pair<rescuecore2.misc.geometry.Line2D, rescuecore2.misc.geometry.Line2D>(firstLine, secondLine);  //To change body of created methods use File | Settings | File Templates.
    }

    public static Polygon clearAreaRectangle(rescuecore2.misc.geometry.Point2D startPoint, rescuecore2.misc.geometry.Point2D endPoint, int clearRadius) {
        return clearAreaRectangle(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY(), clearRadius);
    }

    public static Pair<rescuecore2.misc.geometry.Line2D, rescuecore2.misc.geometry.Line2D> clearLengthLines(Polygon polygon, int clearRadius) {
        return clearLengthLines(Util.getPoint2DList(polygon.xpoints, polygon.ypoints), clearRadius);
    }

    public static rescuecore2.misc.geometry.Line2D clipLine(rescuecore2.misc.geometry.Line2D line, double size, boolean fromCenter) {
        double length = lineLength(line);
        if (fromCenter) {
            double clipSize = (size - length) / 2;
            rescuecore2.misc.geometry.Line2D clip = improveLine(line, clipSize);
            return reverse(improveLine(reverse(clip), clipSize));
        } else {
            return improveLine(line, size - length);
        }
    }

    public static rescuecore2.misc.geometry.Line2D reverse(rescuecore2.misc.geometry.Line2D line) {
        rescuecore2.misc.geometry.Point2D end = line.getOrigin(), origin = line.getEndPoint();
        return new rescuecore2.misc.geometry.Line2D(origin, end);
    }

    public static Polygon scaleBySize(Polygon polygon, double size) {
        Polygon result = new Polygon();
        rescuecore2.misc.geometry.Point2D center = new rescuecore2.misc.geometry.Point2D(polygon.getBounds().getCenterX(), polygon.getBounds().getCenterY());
        for (int i = 0; i < polygon.npoints; i++) {
            rescuecore2.misc.geometry.Point2D point = new rescuecore2.misc.geometry.Point2D(polygon.xpoints[i], polygon.ypoints[i]);
            rescuecore2.misc.geometry.Line2D line = new rescuecore2.misc.geometry.Line2D(center, point);
            line = Util.improveLine(line, size);
            result.addPoint((int) line.getEndPoint().getX(), (int) line.getEndPoint().getY());
        }
        return result;
    }

    public static Polygon scaleBySize2(Polygon polygon, double size) {
        Polygon result = new Polygon();
        rescuecore2.misc.geometry.Point2D center = new rescuecore2.misc.geometry.Point2D(polygon.getBounds().getCenterX(), polygon.getBounds().getCenterY());
        List<rescuecore2.misc.geometry.Line2D> polyLines = getLines(polygon);

//        com.vividsolutions.jts.algorithm.ConvexHull convexHull = convertToConvexHull(polygon);
//        Geometry convexGeom = convexHull.getConvexHull();
//        Coordinate centroid = new Coordinate()
//        Scaling.get(convexGeom, convexGeom.getCoordinate(),coef,convexGeom.getFactory());

        double coef = 1.2;
        for (rescuecore2.misc.geometry.Line2D line2D : polyLines) {
            double h = distance(line2D, center);
            double H = h + size;
            coef = Math.min(coef, H / h);
//            double a = distance(center,line2D.getOrigin());
            //double A= a*H/h;
//            rescuecore2.misc.geometry.Line2D ln = new rescuecore2.misc.geometry.Line2D(center,line2D.getOrigin());
//            ln = improveLine(ln,A-a);
//            rescuecore2.misc.geometry.Point2D pa = ln.getEndPoint();
//            result.addPoint((int) pa.getX(), (int) pa.getY());

//            double b = distance(center,line2D.getEndPoint());
//            double B = b*H/h;
//            ln = new rescuecore2.misc.geometry.Line2D(center,line2D.getEndPoint());
//            ln = improveLine(ln , B-b);
//            rescuecore2.misc.geometry.Point2D pb = ln.getEndPoint();
//            result.addPoint((int)pb.getX(), (int) pb.getY());
        }

        for (rescuecore2.misc.geometry.Line2D line2D : polyLines) {

            double a = distance(center, line2D.getOrigin());
//            double A= a*H/h;
            rescuecore2.misc.geometry.Line2D ln = new rescuecore2.misc.geometry.Line2D(center, line2D.getOrigin());
            ln = improveLine(ln, a * coef);
            rescuecore2.misc.geometry.Point2D pa = ln.getEndPoint();
            result.addPoint((int) pa.getX(), (int) pa.getY());

//            double b = distance(center,line2D.getEndPoint());
//            double B = b*H/h;
//            ln = new rescuecore2.misc.geometry.Line2D(center,line2D.getEndPoint());
//            ln = improveLine(ln , B-b);
//            rescuecore2.misc.geometry.Point2D pb = ln.getEndPoint();
//            result.addPoint((int)pb.getX(), (int) pb.getY());
        }


        return result;
    }

    public static Polygon scaleBySize3(Polygon polygon, double size) {
        Polygon result = new Polygon();
        rescuecore2.misc.geometry.Point2D center = new rescuecore2.misc.geometry.Point2D(polygon.getBounds().getCenterX(), polygon.getBounds().getCenterY());
        List<rescuecore2.misc.geometry.Line2D> polyLines = getLines(polygon);

        for (rescuecore2.misc.geometry.Line2D line2D : polyLines) {

            rescuecore2.misc.geometry.Point2D p1 = closestPoint(line2D, center);
            rescuecore2.misc.geometry.Line2D ln = new rescuecore2.misc.geometry.Line2D(center, p1);
            ln = improveLine(ln, size);
            rescuecore2.misc.geometry.Point2D p2 = ln.getEndPoint();
            double dx = p2.getX() - p1.getX();
            double dy = p2.getY() - p1.getY();

            rescuecore2.misc.geometry.Point2D origin = new rescuecore2.misc.geometry.Point2D(
                    line2D.getOrigin().getX() + dx,
                    line2D.getOrigin().getY() + dy
            );
            result.addPoint((int) origin.getX(), (int) origin.getY());

            rescuecore2.misc.geometry.Point2D end = new rescuecore2.misc.geometry.Point2D(
                    line2D.getEndPoint().getX() + dx,
                    line2D.getEndPoint().getY() + dy
            );
            result.addPoint((int) end.getX(), (int) end.getY());
        }
        return result;

    }


    public static Polygon retainPolygon(Polygon polygon1, Polygon polygon2) {
        return PolygonUtil.retainPolygon(polygon1,polygon2);
    }

    /**
     * split polygon by a line2D to to polygon
     *
     * @param polygon
     * @param line
     * @return
     */
    public static Pair<Polygon, Polygon> split(Polygon polygon, rescuecore2.misc.geometry.Line2D line) {
        return PolygonUtil.split(polygon,line);
    }

    public static Pair<Polygon, Polygon> split(int polygonApexes[], rescuecore2.misc.geometry.Line2D line) {
        return split(Util.getPolygon(polygonApexes), line);
    }

    public static List<Point> getPointList(int[] apexList) {

        List<Point> points = new ArrayList<>();
        for (int i = 0; i < apexList.length; i += 2) {
            points.add(new Point(apexList[i], apexList[i + 1]));
        }

        return points;
    }

    public static ConvexHull convertToConvexHull(Polygon polygon) {
        com.vividsolutions.jts.algorithm.ConvexHull convexHull;
        Coordinate[] coordinates = new Coordinate[polygon.npoints];
        for (int i = 0; i < polygon.npoints; i++) {
            coordinates[i] = new Coordinate(polygon.xpoints[i], polygon.ypoints[i]);
        }
        convexHull = new ConvexHull(coordinates, new GeometryFactory());

        return convexHull;
    }

    public static EntityID getNearest(WorldInfo world, Collection<EntityID> locations, EntityID base) {
        EntityID result = null;
        double minDistance = Double.POSITIVE_INFINITY;
        for (EntityID next : locations) {
            double dist = world.getDistance(base, next);
            if (dist < minDistance) {
                result = next;
                minDistance = dist;
            }
        }
        return result;
    }
}
