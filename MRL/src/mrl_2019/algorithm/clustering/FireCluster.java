package mrl_2019.algorithm.clustering;

import mrl_2019.complex.firebrigade.WaterCoolingEstimator;
import mrl_2019.util.Util;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.entity.MrlBuilding;
import mrl_2019.world.geometry.CompositeConvexHull;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: Siavash
 * Date: 2/26/12
 * Time: 2:05 PM
 * Edited by Sajjad
 */
public class FireCluster extends Cluster {

    private static final int CLUSTER_ENERGY_COEFFICIENT = 50;//70;
    private static final int CLUSTER_ENERGY_SECOND_COEFFICIENT = 20;
    MrlWorldHelper world;
    //private double value;
    int idCounter;
    int DIRECTION_THRESHOLD = 10000;
    int waterNeeded = 0;
    //    IClusterMembershipChecker membershipChecker;
    private double coef;
    private double clusterEnergy;
    private double clusterVolume;
    private double clusterVolumeRatio;
    private List<MrlBuilding> highValueBuildings = new ArrayList<MrlBuilding>();
    private Condition condition;
    private Set<MrlBuilding> buildings;

    public FireCluster(MrlWorldHelper world, Collection<Cluster> civilianClusters) {
        super();
        this.world = world;
//        this.membershipChecker = membershipChecker;
        this.buildings = new HashSet<>();
        idCounter = 0;
    }

    public FireCluster(MrlWorldHelper world/*, IClusterMembershipChecker membershipChecker*/) {
        super();
        this.world = world;
//        this.membershipChecker = membershipChecker;
        this.buildings = new HashSet<>();
        idCounter = 0;
    }

    private static Point[] getPerpendicularPoints(Point2D point1, Point2D point2, double radiusLength) {
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

    private static Point getFinalDirectionPoints(Point2D point1, Point2D point2, double radiusLength) {
        double x1 = point1.getX();
        double y1 = point1.getY();
        double x2 = point2.getX();
        double y2 = point2.getY();

//        double m1 = (y1 - y2) / (x1 - x2);
//        double a = Math.pow(m1, 2) + 1;
//        double b = (-2 * x1) - (2 * Math.pow(m1, 2) * x1);
//        double c = (Math.pow(x1, 2) * (Math.pow(m1, 2) + 1)) - Math.pow(radiusLength, 2);
//
//        double x3 = ((-1 * b) + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
//        double y3 = (m1 * x3) - (m1 * x1) + y1;

        double d = Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
        double r = radiusLength / d;

        double x3 = r * x1 + (1 - r) * x2;
        double y3 = r * y1 + (1 - r) * y2;

        Point perpendicular = new Point((int) x3, (int) y3);
        return perpendicular;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public void setControllable(double clusterEnergy) {
        double fireBrigadeEnergy = world.getWorldInfo().getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE).size() * world.getScenarioInfo().getFireExtinguishMaxSum();
        boolean controllable = (clusterEnergy / CLUSTER_ENERGY_COEFFICIENT) < fireBrigadeEnergy;
        if (!isControllable() && controllable) {
            controllable = (clusterEnergy / CLUSTER_ENERGY_SECOND_COEFFICIENT) < fireBrigadeEnergy;
        }
        setControllable(controllable);
//        if (world.getSelf().getID().getValue() == 538173677) {
//            world.printData("cluster is controllable "+ controllable+".  clusterEnergy = " + (clusterEnergy)+"  val = " + (clusterEnergy / 70) + "  fireBrigadeEnergy = " + fireBrigadeEnergy);
//        }
    }

    /**
     * any building with temperature bigger than zero should be considered in convex hull
     */
    @Override
    public void updateConvexHull() {
        MrlBuilding mrlBuilding;
        convexHull = new CompositeConvexHull();
        convexObject.setConvexPolygon(convexHull.getConvexPolygon());
        if (convexObject != null && convexObject.getConvexPolygon() != null && convexObject.getConvexPolygon().npoints != 0) {
            for (int i = 0; i < convexObject.getConvexPolygon().npoints; i++) {
                convexHull.addPoint(convexObject.getConvexPolygon().xpoints[i],
                        convexObject.getConvexPolygon().ypoints[i]);
            }
        }

        for (StandardEntity entity : entities) {
            if (entity instanceof Building) {
                mrlBuilding = world.getMrlBuilding(entity.getID());

                /*if (isDying && mrlBuilding.getEstimatedFieryness() > 0 && mrlBuilding.getEstimatedFieryness() < 3) {
                    setDying(false);
                }*/

                if (isEdge && !world.getBorderBuildings().contains(mrlBuilding.getID())) {
                    setEdge(false);
                }

                //try {
//                    if (membershipChecker.checkMembership(mrlBuilding)) {
//                        convexHull.addPoint(mrlBuilding.getSelfBuilding().getX(),
//                                mrlBuilding.getSelfBuilding().getY());
                for (int i = 0; i < mrlBuilding.getSelfBuilding().getApexList().length; i += 2) {
                    convexHull.addPoint(mrlBuilding.getSelfBuilding().getApexList()[i], mrlBuilding.getSelfBuilding().getApexList()[i + 1]);
                }

//                    }
                /*} catch (Exception e) {
                    e.printStackTrace();
                }*/
            }
        }


//        sizeOfBuildings();  mostafas commented this
        //if (world.getTime() % 5 == 0) {
        Set<MrlBuilding> dangerBuildings = new HashSet<>();
        double clusterEnergy = 0;
        double clusterVolume = 0;
        for (StandardEntity entity : getEntities()) {
            MrlBuilding burningBuilding = world.getMrlBuilding(entity.getID());
            if (burningBuilding.getEstimatedFieryness() == 1) {
                dangerBuildings.add(burningBuilding);
                clusterEnergy += burningBuilding.getEnergy();
                clusterVolume += burningBuilding.getVolume();
            }
            if (burningBuilding.getEstimatedFieryness() == 2) {
                dangerBuildings.add(burningBuilding);
                clusterEnergy += burningBuilding.getEnergy();
                clusterVolume += burningBuilding.getVolume();
            }
            if (burningBuilding.getEstimatedFieryness() == 3 && burningBuilding.getEstimatedTemperature() > 150) {
                dangerBuildings.add(burningBuilding);
                clusterVolume += burningBuilding.getVolume();
            }
        }

        this.clusterEnergy = clusterEnergy / 4;
        this.clusterVolume = clusterVolume;
        this.clusterVolumeRatio = clusterVolume/entities.size();

        setDying(dangerBuildings.isEmpty());
        setControllable(clusterEnergy);
        buildings = dangerBuildings;
        //}
        convexObject.setConvexPolygon(convexHull.getConvexPolygon());
        setBorderEntities();
        setCentre();
//        sizeOfBuildings();
//        setTotalDistance();

    }

    public int calcNeededWaterToExtinguish() {
        int neededWater = 0;
        for (StandardEntity entity : getBorderEntities()) {
            neededWater += WaterCoolingEstimator.waterNeededToExtinguish(world.getMrlBuilding(entity.getID()));
        }
        return neededWater;
    }

    public void updateCondition() {
        double fireClusterArea = getBoundingBoxArea();
        double worldArea = (world.getMapHeight() / 1000) * (world.getMapWidth() / 1000);
        double percent = fireClusterArea / worldArea;
        if (percent > 0.80) {
            setCondition(Condition.unControllable);
            return;
        }
        if (percent > 0.15) {
            setCondition(Condition.edgeControllable);
            return;
        }
        if (percent > 0.04) {
            setCondition(Condition.largeControllable);
            return;
        }
        if (percent >= 0.00) {
            setCondition(Condition.smallControllable);
        }
    }

    @Override
    public void updateValue() {
        //updateCondition();
    }

    /**
     * this function calculate total distance between this fire cluster and All of the civilian Cluster
     */
    /*private void setTotalDistance() {
        double minDistance;
        double totalDistance = 0.0;

        for (Cluster cluster : civilianClusters) {
            minDistance = Integer.MAX_VALUE;
            int dis = 0;
            for (int i = 0; i < convexObject.getConvexPolygon().npoints; i++) {
                for (int j = 0; j < cluster.getConvexHullObject().getConvexPolygon().npoints; j++) {
                    dis = Util.distance(cluster.getConvexHullObject().getConvexPolygon().xpoints[j], cluster.getConvexHullObject().getConvexPolygon().ypoints[j], convexObject.getConvexPolygon().xpoints[i], convexObject.getConvexPolygon().ypoints[i]) / 1000;
                    if (minDistance > dis) {
                        minDistance = dis;
                    }
                }
            }
            if (cluster instanceof CivilianCluster) {
                minDistance = minDistance * cluster.getValue();
            }
            totalDistance += minDistance;
        }

        setCoef(totalDistance);

    }*/
    private void setBorderEntities() {
        Building building;
        borderEntities.clear();

        if (convexObject.getConvexPolygon().npoints == 0) // I don't know why this happens, this should be checked TODO check this if something comes wrong here
            return;

        smallBorderPolygon = scalePolygon(convexObject.getConvexPolygon(), 0.9);
        bigBorderPolygon = scalePolygon(convexObject.getConvexPolygon(), 1.1);

        for (StandardEntity entity : entities) {

            if (entity instanceof Refuge) {
                continue;
            }
            if (!(entity instanceof Building)) {
                continue;
            }
            building = (Building) entity;
            int vertexes[] = building.getApexList();
            for (int i = 0; i < vertexes.length; i += 2) {

                if ((bigBorderPolygon.contains(vertexes[i], vertexes[i + 1])) && !(smallBorderPolygon.contains(vertexes[i], vertexes[i + 1]))) {
                    borderEntities.add(building);
                    break;
                }
            }
        }
    }

    private double getTotalBuildingArea(Set<MrlBuilding> mrlBuildings) {
        double totalArea = 0;
        for (MrlBuilding building : mrlBuildings) {
//            totalArea = building.getSelfBuilding().getShape().getBounds().getWidth() * building.getSelfBuilding().getShape().getBounds().getHeight();
            totalArea = building.getSelfBuilding().getTotalArea();
        }
        return totalArea;
    }

    /**
     * this is function calculate sum of the fire building that have fireness 1, 2, 3
     */
    private void sizeOfBuildings() {
        int count = 0;
        for (StandardEntity entity : entities) {
            if (entity instanceof Building) {
                if (((Building) entity).isFierynessDefined() && ((Building) entity).getFieryness() >= 1 && ((Building) entity).getFieryness() <= 3) {
                    buildings.add(world.getMrlBuilding(entity.getID()));
                }
            }
        }

        value = coef * count;   //todo: WTF is this
    }

    public double getCoef() {
        return coef;
    }

    /**
     * this function set the filre cluster Coef
     *
     * @param total the sum of minimum distance between this fire cluster and all civilian cluster
     */
    private void setCoef(double total) {
        int minDistance = Integer.MAX_VALUE;
        int dis = 0;
        for (int i = 0; i < convexObject.getConvexPolygon().npoints; i++) {
            dis = Util.distance(convexObject.getConvexPolygon().xpoints[i], convexObject.getConvexPolygon().ypoints[i], world.getMapWidth() / 2, world.getMapHeight() / 2) / 1000;
            if (dis < minDistance) {
                minDistance = dis;
            }
        }


        coef = total + minDistance + (buildings.size() / world.getBuildings().size()) + (getTotalBuildingArea(buildings) / getTotalBuildingArea(world.getMrlBuildings()));
    }

    public Set<MrlBuilding> getBuildings() {
        return buildings;
    }

    /*public boolean hasBuildingInDirection(Point center, boolean limitDirection, boolean useAllFieryness) {
        setTriangle(center, limitDirection);
        Set<StandardEntity> entitySet = new FastSet<StandardEntity>(borderEntities);
        entitySet.removeAll(ignoredBorderEntities);
        if (IsOverCenter()) {

            Building building;
            Point p1 = convexObject.CONVEX_POINT;
            Point pc = convexObject.CENTER_POINT;

            int x1, x2, y1, y2, total1, total2;
            for (StandardEntity entity : entitySet) {
//            for (StandardEntity entity : entities) {
                building = (Building) entity;
                MrlBuilding b = world.getMrlBuilding(entity.getID());
                if (!useAllFieryness) {
                    if (!isCandidate(b)) {
                        continue;
                    }
                }
                if (!isOldCandidate(b)) {
                    continue;
                }
                x1 = (p1.x - pc.x) / 1000;
                x2 = (building.getX() - pc.x) / 1000;
                y1 = (p1.y - pc.y) / 1000;
                y2 = (building.getY() - pc.y) / 1000;
                total1 = x1 * x2;
                total2 = y1 * y2;
                if (total1 <= 0 && total2 <= 0) {
                    return true;
                }
            }
        } else {
            Polygon triangle = convexObject.getTriangle();
            MrlBuilding building;
            for (StandardEntity entity : entitySet) {
//            for (StandardEntity entity : entities) {
                building = world.getMrlBuilding(entity.getID());
                if (!useAllFieryness) {
                    if (!isCandidate(building)) {
                        continue;
                    }
                }
                if (!isOldCandidate(building)) {
                    continue;
                }
                int vertexes[] = building.getSelfBuilding().getApexList();
                for (int i = 0; i < vertexes.length; i += 2) {
                    if (triangle.contains(vertexes[i], vertexes[i + 1])) {
                        return true;
                    }
                }
            }
        }
        return false;
    }*/

    private void setCentre() {
        int sumX = 0;
        int sumY = 0;
        for (int x : convexObject.getConvexPolygon().xpoints) {
            sumX += x;
        }

        for (int y : convexObject.getConvexPolygon().ypoints) {
            sumY += y;
        }

        if (convexObject.getConvexPolygon().npoints > 0) {
            center = new Point(sumX / convexObject.getConvexPolygon().npoints, sumY / convexObject.getConvexPolygon().npoints);
        } else {
            center = new Point(0, 0);
        }

    }

    public boolean isExpandableToCenterOfMap() {
        if (isEdge()) {
            Point mapCenter = new Point(world.getMapWidth() >> 1, world.getMapHeight() >> 1);
            double distanceFireClusterToCenter = Util.distance(center, mapCenter);
            for (EntityID entityID : world.getBorderBuildings()) {
                Building building = (Building) world.getEntity(entityID);
                double distanceBuildingToCenter = Util.distance(world.getWorldInfo().getLocation(building), mapCenter);
                if (distanceBuildingToCenter <= distanceFireClusterToCenter) {
                    return true;
                }
            }
        } else {
            return true;
        }
        return false;
    }

    //edited by mostafaS
    public boolean hasBuildingInDirection(Point targetPoint, boolean limitDirection, boolean useAllFieryness) {

        highValueBuildings = new ArrayList<>();
//        List<StandardEntity> borderDirectionBuildings = new ArrayList<StandardEntity>();
        setTriangle(targetPoint, limitDirection);
        Set<MrlBuilding> entitySet = new HashSet<>();
        entitySet.addAll(buildings);
        entitySet.removeAll(ignoredBorderEntities); //todo: check this ignoredBorderEntities mostafas
        if (isDying() || getConvexHullObject() == null) {
            return !highValueBuildings.isEmpty();
        }
        if (getConvexHullObject() == null || convexObject.CONVEX_POINT == null || convexObject.CENTER_POINT == null || convexObject.getTriangle() == null) {
            return !highValueBuildings.isEmpty();
        }

        Polygon triangle = convexObject.getTriangle();

        for (MrlBuilding building : entitySet) {
            if (!isCandidate(building)) {
                if (!useAllFieryness || !isOldCandidate(building)) {
                    continue;
                }
            }

            int vertexes[] = building.getSelfBuilding().getApexList();
            for (int i = 0; i < vertexes.length; i += 2) {
                if (triangle.contains(vertexes[i], vertexes[i + 1])) {
                    highValueBuildings.add(building);
//                    borderDirectionBuildings.add(building.getSelfBuilding());
                    break;
                }
            }
        }
//        MrlPersonalData.VIEWER_DATA.setHighValueBuildings(world.getSelfHuman().getID(), highValueBuildings);
        return !highValueBuildings.isEmpty();
    }

    public List<MrlBuilding> getBuildingsInDirection() {
        return highValueBuildings;
    }

    private boolean isOldCandidate(MrlBuilding b) {
        return b.getEstimatedFieryness() == 3;
    }

    private boolean isCandidate(MrlBuilding b) {
        return (b.getEstimatedFieryness() == 1 || b.getEstimatedFieryness() == 2);
//        return !(b.getEstimatedFieryness() == 2 || b.getEstimatedFieryness() == 3 || b.getEstimatedFieryness() == 8);
    }

    private void setTriangle(Point targetPoint, boolean limitDirection) {
        Polygon convexPoly = convexObject.getConvexPolygon();
        double radiusLength;
        if (limitDirection) {
            radiusLength = Math.max(world.getBounds().getHeight(), world.getBounds().getWidth()) / 2;
//            radiusLength = Util.distance(convexHull.getConvexPolygon(), new rescuecore2.misc.geometry.Point2D(targetPoint.getX(), targetPoint.getY()));
        } else {
            radiusLength = Math.sqrt(Math.pow(convexPoly.getBounds().getHeight(), 2) + Math.pow(convexPoly.getBounds().getWidth(), 2));
        }

        Point convexPoint = new Point((int) convexPoly.getBounds().getCenterX(), (int) convexPoly.getBounds().getCenterY());
        targetPoint = getFinalDirectionPoints(targetPoint, convexPoint, Math.min(convexPoly.getBounds2D().getWidth(), convexPoly.getBounds2D().getHeight()) * 5);
        Point[] points = getPerpendicularPoints(targetPoint, convexPoint, radiusLength);
        Point point1 = points[0];
        Point point2 = points[1];

        convexObject.CENTER_POINT = targetPoint;
//        MrlPersonalData.VIEWER_DATA.setCenterPoint(world.getSelf().getID(), new Pair<Point, ConvexObject>(targetPoint, this.convexObject));
        convexObject.FIRST_POINT = point1;
        convexObject.SECOND_POINT = point2;
        convexObject.CONVEX_POINT = convexPoint;
        Polygon trianglePoly = new Polygon();
        trianglePoly.addPoint(point1.x, point1.y);
        trianglePoly.addPoint(convexPoint.x, convexPoint.y);
        trianglePoly.addPoint(point2.x, point2.y);

        convexObject.setTrianglePolygon(trianglePoly);
        {//get other side of triangle
            double distance;
            if (limitDirection) {
                distance = Math.max(world.getBounds().getHeight(), world.getBounds().getWidth()) / 2;
//                distance = Util.distance(convexHull.getConvexPolygon(), new rescuecore2.misc.geometry.Point2D(targetPoint.getX(), targetPoint.getY()));
            } else {
                distance = point1.distance(point2) / 3;
            }
            points = getPerpendicularPoints(point2, point1, distance);
            if (convexPoint.distance(points[0]) >= convexPoint.distance(points[1])) {
                trianglePoly.addPoint(points[0].x, points[0].y);
                convexObject.OTHER_POINT2 = new Point(points[0].x, points[0].y);
            } else {
                trianglePoly.addPoint(points[1].x, points[1].y);
                convexObject.OTHER_POINT2 = new Point(points[1].x, points[1].y);
            }

            points = getPerpendicularPoints(point1, point2, distance);
            if (convexPoint.distance(points[0]) >= convexPoint.distance(points[1])) {
                trianglePoly.addPoint(points[0].x, points[0].y);
                convexObject.OTHER_POINT1 = new Point(points[0].x, points[0].y);
            } else {
                trianglePoly.addPoint(points[1].x, points[1].y);
                convexObject.OTHER_POINT1 = new Point(points[1].x, points[1].y);
            }
        }
    }

    public double getClusterEnergy() {
        return clusterEnergy;
    }

    public double getClusterVolume() {
        return clusterVolume;
    }

    public double getClusterVolumeRatio() {
        return clusterVolumeRatio;
    }

    public enum Condition {smallControllable, largeControllable, edgeControllable, unControllable}
}
