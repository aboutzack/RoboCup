package CSU_Yunlu_2019.module.algorithm.fb;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
* @Description: cluster
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
    private Set<Building> buildings;
    //cluster的边界上的entities
    protected Set<StandardEntity> borderEntities;
    //凸包
    protected IConvexHull convexHull;
    //cluster的中心点
    protected Point center;

    public Cluster() {
        id = -1;
        entities = new HashSet<>();
        newEntities = new HashSet<>();
        removedEntities = new HashSet<>();
        borderEntities = new HashSet<>();
        buildings = new HashSet<>();
        convexHull = new CompositeConvexHull();
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
    }
}
