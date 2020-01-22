package mrl_2019.algorithm.clustering;

import mrl_2019.world.geometry.CompositeConvexHull;
import mrl_2019.world.geometry.IConvexHull;
import rescuecore2.standard.entities.StandardEntity;

import java.awt.*;
//import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Siavash
 * Date: 2/26/12
 * Time: 1:27 PM
 */
public abstract class
Cluster {

    protected int id;
    protected Set<StandardEntity> entities;
    protected List<StandardEntity> newEntities;
    protected Set<StandardEntity> borderEntities;
    protected Set<StandardEntity> ignoredBorderEntities;
    protected List<StandardEntity> allEntities; //Roads and Buildings

    protected Point center;
    protected ConvexObject convexObject;
    protected IConvexHull convexHull;

    protected Polygon smallBorderPolygon;
    protected Polygon bigBorderPolygon;

    protected boolean isOverCenter;
    protected boolean isEdge;
    protected boolean isDying;
    protected boolean controllable;

    protected double value;

    protected Cluster() {
        entities = new HashSet<>();
        newEntities = new ArrayList<>();
        borderEntities = new HashSet<>();
        allEntities = new ArrayList<>();
        convexHull = new CompositeConvexHull();
        ignoredBorderEntities = new HashSet<>();
        bigBorderPolygon = new Polygon();
        smallBorderPolygon = new Polygon();
        convexObject = new ConvexObject();
        isOverCenter = false;
        isDying = true;
        controllable = true;
        isEdge = true;
    }

    public void addAll(List<StandardEntity> entities) {
        this.entities.addAll(entities);
    }

    public void add(StandardEntity entity) {
        entities.add(entity);
    }

    public void removeAll(List<StandardEntity> entities) {
        entities.removeAll(entities);
    }

    public void remove(StandardEntity entity) {
        entities.remove(entity);
    }


    public void eat(Cluster cluster) {
        //TODO if there exist any properties, then merge them too
        if (!cluster.isDying()) {
            this.isDying = cluster.isDying();
        }
        if (!cluster.isEdge()) {
            this.isEdge = cluster.isEdge();
        }
        entities.addAll(cluster.getEntities());
    }


    public Set<StandardEntity> getEntities() {
        return entities;
    }

    public Point getCenter() {
        return center;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ConvexObject getConvexHullObject() {
        return convexObject;
    }

    public Set<StandardEntity> getBorderEntities() {
        return borderEntities;
    }

    public List<StandardEntity> getNewEntities() {
        return newEntities;
    }

    public Polygon getSmallBorderPolygon() {
        return smallBorderPolygon;
    }

    public Polygon getBigBorderPolygon() {
        return bigBorderPolygon;
    }

    public double getBoundingBoxArea() {
        Dimension clusterDimension = convexHull.getConvexPolygon().getBounds().getSize();
        return (clusterDimension.getHeight() / 1000d) * (clusterDimension.getWidth() / 1000d);
    }

    public abstract void updateConvexHull();

    public abstract void updateValue();


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
        java.awt.geom.Point2D p, p1;
        int sumX = 0;
        int sumY = 0;

        for (int i = 0; i < sourcePolygon.npoints; i++) {
            p = new math.geom2d.Point2D(sourcePolygon.xpoints[i], sourcePolygon.ypoints[i]) {
                @Override
                public double getX() {
                    return 0;
                }

                @Override
                public double getY() {
                    return 0;
                }

                @Override
                public void setLocation(double x, double y) {

                }
            };
            p1 = ((math.geom2d.Point2D) p).scale(scale);
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

    public void setOverCenter() {
        isOverCenter = true;
    }

    public boolean IsOverCenter() {
        return isOverCenter;
    }

    public void checkForOverCenter() {
        //new code
        Point p1 = convexObject.CONVEX_POINT;
        Point pc = convexObject.CENTER_POINT;

        int x1, x2, y1, y2, total1, total2;

        int[] xArray = convexHull.getConvexPolygon().xpoints;
        int[] yArray = convexHull.getConvexPolygon().ypoints;

        List<Point> points = new ArrayList<Point>();
        for (int i = 0; i < xArray.length; i++) {
            points.add(new Point(xArray[i], yArray[i]));
        }

        for (Point point : points) {
            x1 = (p1.x - pc.x) / 1000;
            x2 = (int) ((point.getX() - pc.getX()) / 1000);
            y1 = (p1.y - pc.y) / 1000;
            y2 = (int) ((point.getY() - pc.y) / 1000);

            total1 = x1 * x2;
            total2 = y1 * y2;

            if (total1 <= 0 && total2 <= 0) {
                setOverCenter();
                break;
            }
        }

        //old code
        /*for (StandardEntity entity : borderEntities)
        {
            Building b = (Building) entity;

            x1 = (p1.x-pc.x) / 1000;
            x2 = (b.getX() - pc.x) / 1000;
            y1 = (p1.y - pc.y) / 1000;
            y2 = (b.getY() - pc.y) / 1000;
            total1 = x1*x2;
            total2 = y1*y2;
            if (total1 <= 0 && total2 <= 0)
            {
                setOverCenter();
                break;
            }
        }*/
    }

    /**
     * This function changes the position of the polygon which is scaled by the "scalePolygon" function. If we don't use this function the scaled polygon does not appear in the right place.
     *
     * @param scaled: is the scaled polygon of our source (notice that it is not in the right place)
     * @param source: is the source polygon, (that is not scaled) we want it to determine the exact position of our scaled polygon
     * @return: returns the new polygon that is in the right place (its center is exactly on the old center)
     */
    protected static Polygon reAllocatePolygon(Polygon scaled, Polygon source) {
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

    public boolean isDying() {
        return isDying;
    }

    public void setDying(boolean dying) {
        isDying = dying;
    }

    public boolean isControllable() {
        return controllable;
    }

    public void setControllable(boolean controllable) {
        this.controllable = controllable;
    }

    public boolean isEdge() {
        return isEdge;
    }

    public void setEdge(boolean edge) {
        isEdge = edge;
    }

    public List<StandardEntity> getAllEntities() {
        return allEntities;
    }

    public void setAllEntities(List<StandardEntity> allEntities) {
        this.allEntities = allEntities;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Set<StandardEntity> getIgnoredBorderEntities() {
        return ignoredBorderEntities;
    }

    public void setIgnoredBorderEntities(Set<StandardEntity> ignoredBorderEntities) {
        this.ignoredBorderEntities = ignoredBorderEntities;
    }
}
