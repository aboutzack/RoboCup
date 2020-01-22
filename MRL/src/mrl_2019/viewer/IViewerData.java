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

public interface IViewerData {

    void print(String s);

    void setUnreachableBuildings(EntityID id, HashSet<EntityID> entityIDs);

    void setPFGuideline(EntityID id, GuideLine guideLine);

    void setPFClearAreaLines(EntityID id, rescuecore2.misc.geometry.Line2D targetLine, rescuecore2.misc.geometry.Line2D first, rescuecore2.misc.geometry.Line2D second);

    void setScaledBlockadeData(EntityID id, List<StandardEntity> obstacles, Polygon scaledBlockades, Map<rescuecore2.misc.geometry.Line2D, List<Point2D>> freePoints, List<Point2D> targetPoint);

    void setObstacleBounds(EntityID id, List<rescuecore2.misc.geometry.Line2D> boundLines);

    void setGraphEdges(EntityID id, GraphModule graph);

    void drawBuildingDetectorTarget(WorldInfo worldInfo, EntityID agentID, EntityID target);

    void drawSearchTarget(WorldInfo worldInfo, EntityID agentID, EntityID target);
}
