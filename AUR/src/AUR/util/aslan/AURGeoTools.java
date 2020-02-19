package AUR.util.aslan;

import AUR.util.knd.AURConstants;
import AUR.util.knd.AURGeoUtil;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Edge;

/**
 *
 * @author Amir Aslan Aslani - 2017 & 2018
 */
public class AURGeoTools {
        public static final double FIVE_DEGREES_RADIAN = 0.0872665;

        public static Point2D getClosestPointOnSegment(Point2D ss, Point2D se, Point2D p) {
                return getClosestPointOnSegment(ss.getX(), ss.getY(), se.getX(), se.getY(), p.getX(), p.getY());
        }

        public static Point2D getClosestPointOnSegment(Line2D l, Point2D p) {
                return getClosestPointOnSegment(l.getOrigin(), l.getEndPoint(), p);
        }

        public static Point2D getClosestPointOnSegment(double sx1, double sy1, double sx2, double sy2, double px, double py) {
                double xDelta = sx2 - sx1;
                double yDelta = sy2 - sy1;

                if ((xDelta == 0) && (yDelta == 0)) {
                        throw new IllegalArgumentException("Segment start equals segment end");
                }

                double u = ((px - sx1) * xDelta + (py - sy1) * yDelta) / (xDelta * xDelta + yDelta * yDelta);

                final Point2D closestPoint;
                if (u < 0) {
                        closestPoint = new Point2D(sx1, sy1);
                } else if (u > 1) {
                        closestPoint = new Point2D(sx2, sy2);
                } else {
                        closestPoint = new Point2D((int) Math.round(sx1 + u * xDelta), (int) Math.round(sy1 + u * yDelta));
                }

                return closestPoint;
        }

        public static double getPointToLineDistance(Point2D A, Point2D B, Point2D P) {
                Point2D mid = new Point2D((A.getX() + B.getX()) / 2, (A.getY() + B.getY()) / 2);
                return mid.minus(P).getLength();
        }

        public static double getPointToLineDistance(Line2D l, Point2D p) {
                return getPointToLineDistance(l.getOrigin(), l.getEndPoint(), p);
        }

        public static Point2D getEdgeMid(Edge e) {
                return new Point2D(
                        (e.getEndX() + e.getStartX()) / 2.0,
                        (e.getEndY() + e.getStartY()) / 2.0
                );
        }

        public static Vector2D getUnitPerpendicularVector(Vector2D v) {
                Vector2D v2 = new Vector2D(-v.getY(), v.getX());
                return v2.normalised();
        }

        public static int[][] getLinesOfPolygon(Polygon p) {
                int[][] lines = new int[p.npoints][4];

                for (int i = 0; i < p.npoints; i++) {
                        lines[i][0] = p.xpoints[i];
                        lines[i][1] = p.ypoints[i];
                        lines[i][2] = p.xpoints[(i + 1) % p.npoints];
                        lines[i][3] = p.ypoints[(i + 1) % p.npoints];
                        
                }
                return lines;
        }

        public static boolean getIntersection(Polygon p, Line2D l) {
                int[][] lines = getLinesOfPolygon(p);
                double r[] = new double[2];
                for (int[] line : lines) {
                        if (AURGeoUtil.getIntersection(
                                l.getOrigin().getX(),
                                l.getOrigin().getY(),
                                l.getEndPoint().getX(),
                                l.getEndPoint().getY(),
                                line[0],
                                line[1],
                                line[2],
                                line[3],
                                r
                        )) {
                                return true;
                        }
                }
                return false;
        }
        
        public static boolean intersect(Polygon polygon, Polygon another) {
                if(polygon.npoints != 0 && another.npoints != 0){
                        int[] apexes0 = new int[2 * another.npoints];
                        int[] apexes1 = new int[2 * polygon.npoints];
                        for (int i = 0; i < polygon.npoints; i++) {
                                apexes1[i * 2] = polygon.xpoints[i];
                                apexes1[i * 2 + 1] = polygon.ypoints[i];
                        }
                        for (int i = 0; i < another.npoints; i++) {
                                apexes0[i * 2] = another.xpoints[i];
                                apexes0[i * 2 + 1] = another.ypoints[i];
                        }

                        for (int i = 0; i < (apexes0.length - 2); i += 2) {
                                for (int j = 0; j < (apexes1.length - 2); j += 2) {
                                        if (java.awt.geom.Line2D.linesIntersect(apexes0[i], apexes0[i + 1], apexes0[i + 2], apexes0[i + 3],
                                                apexes1[j], apexes1[j + 1], apexes1[j + 2], apexes1[j + 3])) {
                                                return true;
                                        }
                                }
                        }
                        for (int i = 0; i < (apexes0.length - 2); i += 2) {
                                if (java.awt.geom.Line2D.linesIntersect(apexes0[i], apexes0[i + 1], apexes0[i + 2], apexes0[i + 3],
                                        apexes1[apexes1.length - 2], apexes1[apexes1.length - 1], apexes1[0], apexes1[1])) {
                                        return true;
                                }
                        }
                        for (int j = 0; j < (apexes1.length - 2); j += 2) {
                                if (java.awt.geom.Line2D.linesIntersect(apexes0[apexes0.length - 2], apexes0[apexes0.length - 1],
                                        apexes0[0], apexes0[1], apexes1[j], apexes1[j + 1], apexes1[j + 2], apexes1[j + 3])) {
                                        return true;
                                }
                        }
                }
                return false;
        }
        
        public static boolean intersect(Polygon polygon, Area area) {
                for(Edge e : area.getEdges())
                        if(
                                (! e.isPassable()) &&
                                (
                                        AURGeoTools.getIntersection(polygon, e.getLine()) || 
                                        polygon.contains(e.getStartX(),e.getStartY()) ||
                                        polygon.contains(e.getEndX(),e.getEndY())
                                )
                        )
                                return true;
                return false;
        }
        
        public static boolean intersect(double[] line, Area area) {
                double[] tmp = new double[2];
                for(Edge e : area.getEdges())
                        if(
                                (! e.isPassable()) &&
                                (
                                        AURGeoUtil.getIntersection(
                                                line[0],
                                                line[1],
                                                line[2],
                                                line[3],
                                                e.getEndX(),
                                                e.getEndY(),
                                                e.getStartX(),
                                                e.getStartY(),
                                                tmp
                                        )
                                )
                        )
                                return true;
                return false;
        }
        
        public static boolean intersect(Polygon polygon, Collection<Area> areas) {
                for(Area area : areas)
                        if( intersect ( polygon, area ) ) {
                                return true;
                        }
                                
                return false;
        }
        
        public static boolean intersectOrContains(Polygon polygon, Collection<Edge> edges) {
                for(Edge edge : edges)
                        if( AURGeoUtil.intersectsOrContains(polygon, new double[]{edge.getStartX(), edge.getStartY(), edge.getEndX(), edge.getEndY()} )) {
                                return true;
                        }
                                
                return false;
        }
        
        public static Polygon getClearPolygon(Point2D p1, Point2D p2, double width, boolean useHead) {
                Vector2D v = p2.minus(p1).normalised().scale(AURConstants.Agent.RADIUS * 2 / 3);
                Vector2D vp = AURGeoTools.getUnitPerpendicularVector(v);
                Polygon p = new Polygon();
                Point2D head = useHead ? p1.plus(v) : p1;
                p.addPoint(
                        (int) (head.getX() + vp.getX() * width / 2),
                        (int) (head.getY() + vp.getY() * width / 2)
                );
                p.addPoint(
                        (int) (head.getX() - vp.getX() * width / 2),
                        (int) (head.getY() - vp.getY() * width / 2)
                );
                p.addPoint(
                        (int) (p2.getX() - vp.getX() * width / 2),
                        (int) (p2.getY() - vp.getY() * width / 2)
                );
                p.addPoint(
                        (int) (p2.getX() + vp.getX() * width / 2),
                        (int) (p2.getY() + vp.getY() * width / 2)
                );
                return p;
        }
        
        public static Polygon getCircle(int center[], int radius){
                Polygon circle = new Polygon();
                double currentAngle = 0;
                while(currentAngle < 2 * Math.PI){
                        double normal[] = getNormalVectorWithRadian(currentAngle);
                        circle.addPoint(
                                (int)(center[0] + normal[0] * radius),
                                (int)(center[1] + normal[1] * radius)
                        );
                        currentAngle += AURGeoTools.FIVE_DEGREES_RADIAN;
                }
                return circle;
        }
        
        public static double[] getNormalVectorWithRadian(double rad){
                return new double[]{
                        Math.sin(rad),
                        Math.cos(rad)
                };
        }
        
        public static double[] getNormalVectorWithDegree(int deg){
                return getNormalVectorWithRadian(
                        Math.toRadians(deg)
                );
        }
        
        public static Rectangle getShapeOfLineSegment(double line[]){
                return new Rectangle(
                        (int) Math.min(line[0], line[2]),
                        (int) Math.min(line[1], line[3]),
                        (int) Math.abs(line[0] - line[2]),
                        (int) Math.abs(line[1] - line[3])
                );
        }
        
        public static Polygon getScaledPolygon(Polygon polygon, double p,int[] center){
                Polygon result = new Polygon();
                for(int i = 0;i < polygon.npoints;i ++){
                        double[] v = new double[] {polygon.xpoints[i] - center[0], polygon.ypoints[i] - center[1]};
                        double[] r = AURGeoMetrics.getVectorScaled(v, p);
                        result.addPoint(
                                (int) r[0] + center[0],
                                (int) r[1] + center[1]
                        );
                }
                return result;
        }
        
        public static Polygon getAddedPolygon(Polygon polygon, double p,int[] center){
                Polygon result = new Polygon();
                for(int i = 0;i < polygon.npoints;i ++){
                        double[] v = new double[] {polygon.xpoints[i] - center[0], polygon.ypoints[i] - center[1]};
                        double vLen = AURGeoMetrics.getVectorLen(v);
                        double[] r = AURGeoMetrics.getVectorScaled(
                                AURGeoMetrics.getVectorNormal(v),
                                vLen + p
                        );
                        result.addPoint(
                                (int) r[0] + center[0],
                                (int) r[1] + center[1]
                        );
                }
                return result;
        }
        
        public static double getEdgeDistanceToOppositeSideEdge(Polygon p, double[] e){
                double[] result = new double[2];
                double rP[] = new double[]{
                        (e[0] + e[2]) / 2,
                        (e[1] + e[3]) / 2,
                };
                
                double v[] = AURGeoMetrics.getVectorNormal(new double[]{
                        e[0] - e[2],
                        e[1] - e[3]
                });
                Rectangle bounds = p.getBounds();
                double sqrt = Math.sqrt(bounds.height * bounds.height + bounds.width * bounds.width);
                double[] p1 = AURGeoMetrics.getPointsPlus(
                        rP,
                        AURGeoMetrics.getVectorScaled(v, sqrt)
                );
                double[] p2 = AURGeoMetrics.getPointsPlus(
                        rP,
                        AURGeoMetrics.getVectorScaled(v, - sqrt)
                );
                for(int i = 1;i < p.npoints;i ++){
                        if( ! (AURGeoUtil.equals(p.xpoints[i],p.ypoints[i],e[0],e[1]) && AURGeoUtil.equals(p.xpoints[i - 1],p.ypoints[i - 1],e[2],e[3])) &&
                            ! (AURGeoUtil.equals(p.xpoints[i - 1],p.ypoints[i - 1],e[0],e[1]) && AURGeoUtil.equals(p.xpoints[i],p.ypoints[i],e[2],e[3]))){
                                if(AURGeoUtil.getIntersection(
                                        p.xpoints[i],
                                        p.ypoints[i], 
                                        p.xpoints[i - 1],
                                        p.ypoints[i - 1], 
                                        p1[0],
                                        p1[1],
                                        p2[0],
                                        p2[1],
                                        result)
                                ){
                                        return Math.hypot(result[0] - rP[0], result[1] - rP[1]);
                                }
                        }
                }
                
                return -1;
        }

        public static boolean intersect(Blockade blockade, Blockade another) {
                if (blockade.isApexesDefined() && another.isApexesDefined()) {
                        int[] apexes0 = blockade.getApexes();
                        int[] apexes1 = another.getApexes();
                        for (int i = 0; i < (apexes0.length - 2); i += 2) {
                                for (int j = 0; j < (apexes1.length - 2); j += 2) {
                                        if (java.awt.geom.Line2D.linesIntersect(apexes0[i], apexes0[i + 1], apexes0[i + 2], apexes0[i + 3],
                                                apexes1[j], apexes1[j + 1], apexes1[j + 2], apexes1[j + 3])) {
                                                return true;
                                        }
                                }
                        }
                        for (int i = 0; i < (apexes0.length - 2); i += 2) {
                                if (java.awt.geom.Line2D.linesIntersect(apexes0[i], apexes0[i + 1], apexes0[i + 2], apexes0[i + 3],
                                        apexes1[apexes1.length - 2], apexes1[apexes1.length - 1], apexes1[0], apexes1[1])) {
                                        return true;
                                }
                        }
                        for (int j = 0; j < (apexes1.length - 2); j += 2) {
                                if (java.awt.geom.Line2D.linesIntersect(apexes0[apexes0.length - 2], apexes0[apexes0.length - 1],
                                        apexes0[0], apexes0[1], apexes1[j], apexes1[j + 1], apexes1[j + 2], apexes1[j + 3])) {
                                        return true;
                                }
                        }
                }
                return false;
        }

        public static boolean intersect(Blockade blockade, Polygon polygon) {

                if (blockade.isApexesDefined()) {
                        int[] apexes0 = blockade.getApexes();
                        int[] apexes1 = new int[2 * polygon.npoints];
                        for (int i = 0; i < polygon.npoints; i++) {
                                apexes1[i * 2] = polygon.xpoints[i];
                                apexes1[i * 2 + 1] = polygon.ypoints[i];
                        }

                        for (int i = 0; i < (apexes0.length - 2); i += 2) {
                                for (int j = 0; j < (apexes1.length - 2); j += 2) {
                                        if (java.awt.geom.Line2D.linesIntersect(apexes0[i], apexes0[i + 1], apexes0[i + 2], apexes0[i + 3],
                                                apexes1[j], apexes1[j + 1], apexes1[j + 2], apexes1[j + 3])) {
                                                return true;
                                        }
                                }
                        }
                        for (int i = 0; i < (apexes0.length - 2); i += 2) {
                                if (java.awt.geom.Line2D.linesIntersect(apexes0[i], apexes0[i + 1], apexes0[i + 2], apexes0[i + 3],
                                        apexes1[apexes1.length - 2], apexes1[apexes1.length - 1], apexes1[0], apexes1[1])) {
                                        return true;
                                }
                        }
                        for (int j = 0; j < (apexes1.length - 2); j += 2) {
                                if (java.awt.geom.Line2D.linesIntersect(apexes0[apexes0.length - 2], apexes0[apexes0.length - 1],
                                        apexes0[0], apexes0[1], apexes1[j], apexes1[j + 1], apexes1[j + 2], apexes1[j + 3])) {
                                        return true;
                                }
                        }
                }
                return false;
        }

        public static boolean intersect(double agentX, double agentY, double pointX, double pointY, Blockade blockade) {
                List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(blockade.getApexes()),
                        true);
                for (Line2D line : lines) {
                        Point2D start = line.getOrigin();
                        Point2D end = line.getEndPoint();
                        double startX = start.getX();
                        double startY = start.getY();
                        double endX = end.getX();
                        double endY = end.getY();
                        if (java.awt.geom.Line2D.linesIntersect(agentX, agentY, pointX, pointY, startX, startY, endX, endY)) {
                                return true;
                        }
                }
                return false;
        }
}
