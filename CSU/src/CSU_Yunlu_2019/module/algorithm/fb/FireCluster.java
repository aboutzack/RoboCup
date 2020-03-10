package CSU_Yunlu_2019.module.algorithm.fb;

import CSU_Yunlu_2019.geom.PolygonScaler;
import CSU_Yunlu_2019.standard.Ruler;
import CSU_Yunlu_2019.world.CSUWorldHelper;
import CSU_Yunlu_2019.world.object.CSUBuilding;
import javolution.util.FastSet;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Set;

/**
 * @description: cluster for buildings on fire
 * @author: Guanyu-Cai
 * @Date: 03/03/2020
 */
public class FireCluster extends Cluster {
    //扑灭这个cluster需要的总水量
    private int waterNeeded;
    // TODO: 3/4/20 计算
    private double clusterEnergy;
    //这个cluster的火有没有可能被控制住
    private boolean controllable;
    private CSUWorldHelper world;
    private boolean isOverCenter;

    public FireCluster(CSUWorldHelper worldHelper) {
        super();
        this.world = worldHelper;
        this.waterNeeded = 0;
        this.clusterEnergy = 0;
        this.isOverCenter = false;
    }

    @Override
    public void updateConvexHull() {
        // TODO: 3/4/20 直接重新算快还是一个个算快
        //根据本回合删除的entities更新convexHull
        for (StandardEntity entity : removedEntities) {
            if (entity instanceof Building) {
                Building building = (Building) entity;
                for (int i = 0; i < building.getApexList().length; i += 2) {
                    convexHull.removePoint(building.getApexList()[i], building.getApexList()[i + 1]);
                }
            }
        }
        //根据本回合新增的entities更新convexHull
        for (StandardEntity entity : newEntities) {
            if (entity instanceof Building) {
                Building building = (Building) entity;
                for (int i = 0; i < building.getApexList().length; i += 2) {
                    convexHull.addPoint(building.getApexList()[i], building.getApexList()[i + 1]);
                }
            }
        }
        this.convexObject.setConvexHullPolygon(convexHull.getConvexPolygon());
        resetRemovedAndNew();
        updateCenter();
        updateBorderEntities();
    }

    private void updateCenter() {
        if (convexObject.getConvexHullPolygon().npoints > 0) {
            int sumX = 0;
            int sumY = 0;
            for (int x : convexObject.getConvexHullPolygon().xpoints) {
                sumX += x;
            }
            for (int y : convexObject.getConvexHullPolygon().ypoints) {
                sumY += y;
            }
            center = new Point(sumX / convexObject.getConvexHullPolygon().npoints, sumY / convexObject.getConvexHullPolygon().npoints);
        } else {
            center = new Point(0, 0);
        }
    }

    /**
     * Get all candidate buildings of this FireCluster in the given direction.
     *
     * @param center         the point marks the direction
     * @param limitDirection a flag used to determine the size of the direction triangle
     * @return
     */
    public Set<CSUBuilding> findBuildingInDirection(Point center) {
        Set<CSUBuilding> targetBuildins = new FastSet<>();

        if (!isOverCenter)
            this.checkForOverCenter(center);
        this.setTriangle(isOverCenter);

        if (this.isDying() || this.getConvexObject() == null)
            return targetBuildins;
        if (convexObject.CENTER_POINT == null || convexObject.CONVEX_POINT == null)
            return targetBuildins;

        Building building;
        CSUBuilding csuBuilding;
        Polygon polygon;
        if (isOverCenter) {
            polygon = this.convexObject.getDirectionRectangle();
        } else {
            polygon = this.convexObject.getTriangle();
        }

        for (StandardEntity entity : this.borderEntities) {
            building = (Building) entity;
            csuBuilding = world.getCsuBuilding(entity);
            if (!isCandidate(csuBuilding))
                continue;
            if (!isOldCandidate(csuBuilding))
                continue;
            int[] vertices = building.getApexList();
            for (int i = 0; i < vertices.length; i += 2) {
                if (polygon.contains(vertices[i], vertices[i + 1])) {
                    targetBuildins.add(csuBuilding);
                    break;
                }
            }
        }

        return targetBuildins;
    }

    /**
     * Determines whether there are candidate buildings in the given direction.
     *
     * @param center
     *            the point marks the direction
     * @param limitDirection
     *            a flag used to determines the size of the direction triangle
     * @return true if there is building in this direction. Otherwise, false.
     */
    public boolean haveBuildingInDirectionOf(Point center) {
        if (!isOverCenter)
            this.checkForOverCenter(center);
        this.setTriangle(isOverCenter);

        if (this.isDying() || this.getConvexObject() == null)
            return false;
        if (convexObject.CENTER_POINT == null || convexObject.CONVEX_POINT == null)
            return false;

        Building building;
        CSUBuilding csuBuilding;
        Polygon polygon;

        if (isOverCenter) {
            polygon = this.convexObject.getDirectionRectangle();
        } else {
            polygon = this.convexObject.getTriangle();
        }

        for (StandardEntity entity : this.borderEntities) {
            building = (Building) entity;
            csuBuilding = world.getCsuBuilding(entity);
            if (!isCandidate(csuBuilding))
                continue;
            if (!isOldCandidate(csuBuilding))
                continue;
            int[] vertices = building.getApexList();
            for (int i = 0; i < vertices.length; i += 2) {
                if (polygon.contains(vertices[i], vertices[i + 1]))
                    return true;
            }
        }

        return false;
    }

    /**
     * We should find the expand direction of fire cluster. And in the expand
     * direction, there always are buildings not burn or with slight burn rate.
     */
    private boolean isCandidate(CSUBuilding building) {
        return !(building.getEstimatedFieryness() == 2
                || building.getEstimatedFieryness() == 3
                || building.getEstimatedFieryness() == 8);

    }

    /**
     * For those building that is the candidate in last one or two cycle will
     * have a higher fieryness in current cycle. So we simply think that
     * buildings with estimate fieryness == 3 and estimate temperature < 150 are
     * the candidate in last one or two cycle.
     */
    private boolean isOldCandidate(CSUBuilding building) {
        return !(building.getEstimatedFieryness() == 3 && building.getEstimatedTemperature() < 150);
    }

    /**
     * Set border buildings of this FireCluster. All buildings except Refuge
     * within {@link Cluster#smallBorderPolygon smallBorderPolygon} and
     * {@link Cluster#bigBorderPolygon bigBorderPolygon} are border buildings.
     */
    private void updateBorderEntities() {
        Building building;
        this.borderEntities.clear();

        if (convexObject.getConvexHullPolygon().npoints == 0)
            return;

        this.smallBorderPolygon = PolygonScaler.scalePolygon(convexObject.getConvexHullPolygon(), 0.9);
        this.bigBorderPolygon = PolygonScaler.scalePolygon(convexObject.getConvexHullPolygon(), 1.1);

        for (StandardEntity entity : buildings) {
            if (entity instanceof Refuge) {
                continue;
            }
            if (!(entity instanceof Building)) {
                continue;
            }
            building = (Building) entity;
            int[] vertices = building.getApexList();
            for (int i = 0; i < vertices.length; i += 2) {
                boolean flag_1 = this.bigBorderPolygon.contains(vertices[i], vertices[i + 1]);
                boolean flag_2 = this.smallBorderPolygon.contains(vertices[i], vertices[i + 1]);
                if (flag_1 && !flag_2) {
                    this.borderEntities.add(entity);
                    break;
                }
            }
        }
    }

    /**
     * Set the direction triangle. Please run class {@link TestForSetTriangle}
     * to see what this method can do.
     */
    private void setTriangle(boolean isOverCenter) {
        Polygon convexPolygon = this.convexHull.getConvexPolygon();
        Rectangle convexPolygonBound = convexPolygon.getBounds();
        double polygonBoundWidth = convexPolygonBound.getWidth();
        double polygonBoundHeight = convexPolygonBound.getHeight();
        double radiusLength = Math.hypot(polygonBoundWidth, polygonBoundHeight);

        Point targetPoint = this.convexObject.CENTER_POINT;
        Point convexCenterPoint = this.convexObject.CONVEX_POINT;

        if (isOverCenter) {
            radiusLength /= 2.0;
        } else {
            rescuecore2.misc.geometry.Point2D point =
                    new rescuecore2.misc.geometry.Point2D(targetPoint.getX(), targetPoint.getY());
            double distance = Ruler.getDistance(convexPolygon, point);

            if (distance > radiusLength)
                radiusLength = distance;

			/*if (distance < radiusLength / 2.0)
				radiusLength /= 2.0;
			else
				radiusLength = distance;*/
        }

        Point[] points = getPerpendicularPoints(targetPoint, convexCenterPoint, radiusLength);
        Point point1 = points[0], point2 = points[1];

        this.convexObject.FIRST_POINT = points[0];
        this.convexObject.SECOND_POINT = points[1];

        Polygon trianglePolygon = new Polygon();
        trianglePolygon.addPoint(convexCenterPoint.x, convexCenterPoint.y);
        trianglePolygon.addPoint(point1.x, point1.y);
        trianglePolygon.addPoint(point2.x, point2.y);
        this.convexObject.setTriangle(trianglePolygon);

        if (isOverCenter) {
            double distance = point1.distance(point2) / 2.0;

            Polygon directionPolygon = new Polygon();
            directionPolygon.addPoint(point1.x, point1.y);
            directionPolygon.addPoint(point2.x, point2.y);

            points = getPerpendicularPoints(point1, point2, distance);
            if (convexCenterPoint.distance(points[0]) > convexCenterPoint.distance(points[1])) {
                directionPolygon.addPoint(points[0].x, points[0].y);
                this.convexObject.OTHER_POINT_1 = points[0];
            } else {
                directionPolygon.addPoint(points[1].x, points[1].y);
                this.convexObject.OTHER_POINT_1 = points[1];
            }
            points = getPerpendicularPoints(point2, point1, distance);
            if (convexCenterPoint.distance(points[0]) > convexCenterPoint.distance(points[1])) {
                directionPolygon.addPoint(points[0].x, points[0].y);
                this.convexObject.OTHER_POINT_2 = points[0];
            } else {
                directionPolygon.addPoint(points[1].x, points[1].y);
                this.convexObject.OTHER_POINT_2 = points[1];
            }
        }
    }

    /**
     * We have a test class {@link TestForGetPerpendicularPoints} for this
     * method. Please run this test class to see what this method can do.
     */
    public Point[] getPerpendicularPoints(Point2D P_1, Point2D P_2, double radiusLength) {
        double x1 = P_1.getX();
        double y1 = P_1.getY();
        double x2 = P_2.getX();
        double y2 = P_2.getY();

        double x3, x4, y3, y4;

        if (y1 == y2) {
            x3 = x1;
            x4 = x1;
            y3 = y1 + radiusLength;
            y4 = y1 - radiusLength;
        } else {
            /* a * X^2 + b * X + c = 0 */
            double m1 = (y1 - y2) / (x1 - x2);   ///infinity
            double m2 = (-1 / m1);                    ///0

//            double a = Math.pow(m2, 2) + 1;
//            double b = (-2 * x1) - (2 * Math.pow(m2, 2) * x1);
//            double c = (Math.pow(x1, 2) * (Math.pow(m2, 2) + 1)) - Math.pow(radiusLength, 2);
//            x3 = ((-1 * b) + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
//            x4 = ((-1 * b) - Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);

            double x = Math.sqrt(Math.pow(radiusLength, 2) / (Math.pow(m2, 2) + 1));
            x3 = x1 + x;
            x4 = x1 - x;

            y3 = (m2 * x3) - (m2 * x1) + y1;
            y4 = (m2 * x4) - (m2 * x1) + y1;
        }

        Point perpendicular1 = new Point((int) x3, (int) y3);
        Point perpendicular2 = new Point((int) x4, (int) y4);
        return new Point[]{perpendicular1, perpendicular2};
    }

    public int getWaterNeeded() {
        return waterNeeded;
    }

    public void setWaterNeeded(int waterNeeded) {
        this.waterNeeded = waterNeeded;
    }

    public double getClusterEnergy() {
        return clusterEnergy;
    }

    public void setClusterEnergy(double clusterEnergy) {
        this.clusterEnergy = clusterEnergy;
    }

    public boolean isControllable() {
        return controllable;
    }

    public void setControllable(boolean controllable) {
        this.controllable = controllable;
    }
}
