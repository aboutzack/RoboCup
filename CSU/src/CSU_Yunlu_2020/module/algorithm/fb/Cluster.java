package CSU_Yunlu_2020.module.algorithm.fb;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Description: 改进自csu_2016
 * @Author: Guanyu-Cai
 * @Date: 3/3/20
 */
public abstract class Cluster {
    //cluster的唯一标识
    protected int id;
    //cluster内所有的entities,包括Buildings和Roads
    protected Set<StandardEntity> entities;
    //暂时存储的新加入的entities
    protected Set<StandardEntity> newEntities;
    //暂时存储的删除的entities
    protected Set<StandardEntity> removedEntities;
    //cluster内所有房屋
    protected Set<Building> buildings;
    //cluster的边界上的entities
    protected Set<StandardEntity> borderEntities;
    //凸包
    protected IConvexHull convexHull;
    //用于判断火势蔓延方向
    protected ConvexObject convexObject;
    //cluster的中心点
    protected Point center;
    //Enlarged polygon of this cluster's convex hull polygon(scale is 1.1).
    protected Polygon bigBorderPolygon;

    //Narrowed polygon of this cluster's convex hull polygon(scale is 0.9).
    protected Polygon smallBorderPolygon;
    //cluster的价值
    protected double value;

    //中心是否在多边形之外
    protected boolean isOverCenter;
    //是否在地图边缘
    protected boolean isEdge;
    //value是否正在变小
    protected boolean isDying;
    //是否可控
    protected boolean controllable;
    //边缘建筑物
//    protected Set<StandardEntity> ignoredBorderEntities;

    public Cluster() {
        id = -1;
        entities = new HashSet<>();
        newEntities = new HashSet<>();
        removedEntities = new HashSet<>();
        buildings = new HashSet<>();
        borderEntities = new HashSet<>();
        convexHull = new CompositeConvexHull();
        convexObject = new ConvexObject();
        center = new Point();
        bigBorderPolygon = new Polygon();
        smallBorderPolygon = new Polygon();
        value = 0;
        isOverCenter = false;
        isEdge = false;
        isDying = false;
        controllable = true;
    }

    public void addAll(Set<StandardEntity> entities) {
        this.entities.addAll(entities);
        for (StandardEntity entity : entities) {
            if (entity instanceof Building) {
                buildings.add((Building) entity);
            }
        }
        newEntities.addAll(entities);
    }

    public void add(StandardEntity entity) {
        this.entities.add(entity);
        if (entity instanceof Building) {
            buildings.add((Building) entity);
        }
        newEntities.add(entity);
    }

    public void removeAll(List<StandardEntity> entities) {
        this.entities.removeAll(entities);
        for (StandardEntity entity : entities) {
            if (entity instanceof Building) {
                buildings.remove((Building) entity);
            }
        }
        removedEntities.addAll(entities);
    }

    public void remove(StandardEntity entity) {
        this.entities.remove(entity);
        if (entity instanceof Building) {
            buildings.remove((Building) entity);
        }
        removedEntities.add(entity);
    }

    /**
     * Check whether this cluster's convex hull polygon contains the
     * ConvexObject's CENTER_POINT.
     */
    public void checkForOverCenter(Point targetPoint) {
        Polygon convexPolygon = this.convexHull.getConvexPolygon();
        Rectangle convexPolygonBound = convexPolygon.getBounds();
        int convexCenterPoint_x = (int) convexPolygonBound.getCenterX();
        int convexCenterPoint_y = (int) convexPolygonBound.getCenterY();
        Point convexCenterPoint = new Point(convexCenterPoint_x, convexCenterPoint_y);

        this.convexObject.CENTER_POINT = targetPoint;
        this.convexObject.CONVEX_POINT = convexCenterPoint;

        int[] xs = this.convexHull.getConvexPolygon().xpoints;
        int[] ys = this.convexHull.getConvexPolygon().ypoints;

        double x1, y1, x2, y2, total_1, total_2;

        for (int i = 0; i < ys.length; i++) {
            Point point = new Point(xs[i], ys[i]);
            x1 = (convexCenterPoint.getX() - targetPoint.getX()) / 1000;
            y1 = (convexCenterPoint.getY() - targetPoint.getY()) / 1000;

            x2 = (point.getX() - targetPoint.getX()) / 1000;
            y2 = (point.getY() - targetPoint.getY()) / 1000;

            total_1 = x1 * x2;
            total_2 = y1 * y2;
            if (total_1 <= 0 && total_2 <= 0 /*or total_1 + total_2 <= 0*/) {
                this.isOverCenter = true;
                break;
            }
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Set<StandardEntity> getEntities() {
        return entities;
    }

    public void setEntities(Set<StandardEntity> entities) {
        this.entities = entities;
    }

    public Set<StandardEntity> getNewEntities() {
        return newEntities;
    }

    public void setNewEntities(Set<StandardEntity> newEntities) {
        this.newEntities = newEntities;
    }

    public Set<StandardEntity> getRemovedEntities() {
        return removedEntities;
    }

    public void setRemovedEntities(Set<StandardEntity> removedEntities) {
        this.removedEntities = removedEntities;
    }

    public Set<StandardEntity> getBorderEntities() {
        return borderEntities;
    }

    public void setBorderEntities(Set<StandardEntity> borderEntities) {
        this.borderEntities = borderEntities;
    }

    public IConvexHull getConvexHull() {
        return convexHull;
    }

    public void setConvexHull(IConvexHull convexHull) {
        this.convexHull = convexHull;
    }

    public Set<Building> getBuildings() {
        return buildings;
    }

    public void setBuildings(Set<Building> buildings) {
        this.buildings = buildings;
    }

    public Point getCenter() {
        return center;
    }

    public void setCenter(Point center) {
        this.center = center;
    }

    /**
     * @return 获取ConvexPolygon的外包矩形的面积
     */
    public double getBoundingBoxArea() {
        Dimension clusterDimension = convexHull.getConvexPolygon().getBounds().getSize();
        return (clusterDimension.getHeight() / 1000d) * (clusterDimension.getWidth() / 1000d);
    }

    public abstract void updateConvexHull();

    /**
     * 清空newEntities和removedEntities
     */
    public void resetRemovedAndNew() {
        newEntities.clear();
        removedEntities.clear();
    }

    public void eat(Cluster cluster) {
        entities.addAll(cluster.getEntities());
        buildings.addAll(cluster.getBuildings());
        removedEntities.addAll(cluster.getRemovedEntities());
        newEntities.addAll(cluster.getNewEntities());
    }

    public ConvexObject getConvexObject() {
        return convexObject;
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
}
