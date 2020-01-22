package mrl_2019.viewer;

import adf.agent.info.WorldInfo;
import mrl_2019.extaction.clear.GuideLine;
import mrl_2019.world.routing.graph.GraphModule;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class EmptyViewerData implements IViewerData {
    @Override
    public void print(String s) {
        System.out.println(s);
    }

    @Override
    public void setUnreachableBuildings(EntityID id, HashSet<EntityID> entityIDs) {

    }

    @Override
    public void setPFClearAreaLines(EntityID id, rescuecore2.misc.geometry.Line2D targetLine, rescuecore2.misc.geometry.Line2D first, rescuecore2.misc.geometry.Line2D second) {

    }

    @Override
    public void setScaledBlockadeData(EntityID id, List<StandardEntity> obstacles, Polygon scaledBlockades, Map<rescuecore2.misc.geometry.Line2D, List<Point2D>> freePoints, List<Point2D> targetPoint) {

    }

    @Override
    public void setObstacleBounds(EntityID id, List<rescuecore2.misc.geometry.Line2D> boundLines) {

    }

    @Override
    public void setGraphEdges(EntityID id, GraphModule graph) {

    }

    @Override
    public void setPFGuideline(EntityID id, GuideLine guideLine) {

    }

    @Override
    public void drawBuildingDetectorTarget(WorldInfo worldInfo, EntityID agentID, EntityID target) {

    }

    @Override
    public void drawSearchTarget(WorldInfo worldInfo, EntityID agentID, EntityID target) {

    }
}
