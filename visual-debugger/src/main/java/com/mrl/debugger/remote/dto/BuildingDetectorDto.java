package com.mrl.debugger.remote.dto;

import java.awt.*;
import java.util.Collection;

public class BuildingDetectorDto implements StandardDto {

    private Collection<Polygon> dynamicClusterConvexHulls;

    private Collection<Integer> inDirectionBuildings;

    private Collection<Integer> borderBuildings;

    private Integer targetBuilding;

    public Collection<Polygon> getDynamicClusterConvexHulls() {
        return dynamicClusterConvexHulls;
    }

    public void setDynamicClusterConvexHulls(Collection<Polygon> dynamicClusterConvexHulls) {
        this.dynamicClusterConvexHulls = dynamicClusterConvexHulls;
    }

    public Collection<Integer> getInDirectionBuildings() {
        return inDirectionBuildings;
    }

    public void setInDirectionBuildings(Collection<Integer> inDirectionBuildings) {
        this.inDirectionBuildings = inDirectionBuildings;
    }

    public Collection<Integer> getBorderBuildings() {
        return borderBuildings;
    }

    public void setBorderBuildings(Collection<Integer> borderBuildings) {
        this.borderBuildings = borderBuildings;
    }

    public Integer getTargetBuilding() {
        return targetBuilding;
    }

    public void setTargetBuilding(Integer targetBuilding) {
        this.targetBuilding = targetBuilding;
    }
}
