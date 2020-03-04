package CSU_Yunlu_2019.module.algorithm.fb;

import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;

import java.awt.*;

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
    private AgentInfo agentInfo;
    private WorldInfo worldInfo;
    private ScenarioInfo scenarioInfo;

    public FireCluster(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo) {
        super();
        this.agentInfo = agentInfo;
        this.worldInfo = worldInfo;
        this.scenarioInfo = scenarioInfo;
        this.waterNeeded = 0;
        this.clusterEnergy = 0;
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
        resetRemovedAndNew();
        updateCenter();
    }

    private void updateCenter() {
        int sumX = 0;
        int sumY = 0;
        for (int x : convexHull.getConvexPolygon().xpoints) {
            sumX += x;
        }

        for (int y : convexHull.getConvexPolygon().ypoints) {
            sumY += y;
        }

        if (convexHull.getConvexPolygon().npoints > 0) {
            center = new Point(sumX / convexHull.getConvexPolygon().npoints, sumY / convexHull.getConvexPolygon().npoints);
        } else {
            center = new Point(0, 0);
        }

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
