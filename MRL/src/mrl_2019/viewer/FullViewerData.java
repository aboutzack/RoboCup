package mrl_2019.viewer;

import adf.agent.info.WorldInfo;
import com.mrl.debugger.remote.VDClient;
import com.mrl.debugger.remote.dto.EdgeDto;
import com.mrl.debugger.remote.dto.GraphDto;
import mrl_2019.extaction.clear.GuideLine;
import mrl_2019.world.routing.graph.GraphModule;
import mrl_2019.world.routing.graph.MyEdge;
import mrl_2019.world.routing.graph.Node;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by Rescue Agent on 6/20/14.
 */
public class FullViewerData implements IViewerData {

    @Override
    public void print(String s) {
//        System.out.println(s);
    }

    @Override
    public void setUnreachableBuildings(EntityID id, HashSet<EntityID> entityIDs) {

    }

    @Override
    public void setPFClearAreaLines(EntityID id, rescuecore2.misc.geometry.Line2D targetLine, rescuecore2.misc.geometry.Line2D first, rescuecore2.misc.geometry.Line2D second) {

    }

    @Override
    public void setScaledBlockadeData(EntityID id, List<StandardEntity> obstacles, Polygon scaledBlockades, Map<rescuecore2.misc.geometry.Line2D, List<rescuecore2.misc.geometry.Point2D>> freePoints, List<rescuecore2.misc.geometry.Point2D> targetPoint) {

    }

    @Override
    public void setObstacleBounds(EntityID id, List<rescuecore2.misc.geometry.Line2D> boundLines) {

    }

    @Override
    public void setGraphEdges(EntityID id, GraphModule graph) {
        GraphDto graphDto = new GraphDto();
        EdgeDto edgeDto;
        List<EdgeDto> edgeDtoList = new ArrayList<>();
        for (List<MyEdge> edges : graph.getAllAreaMyEdges().values()) {
            for (MyEdge edge : edges) {
                edgeDto = new EdgeDto();
                edgeDto.setPassable(edge.isPassable());
                Pair<Node, Node> nodes = edge.getNodes();
                int x1 = nodes.first().getPosition().first();
                int y1 = nodes.first().getPosition().second();
                int x2 = nodes.second().getPosition().first();
                int y2 = nodes.second().getPosition().second();
                edgeDto.setLine(new Line2D.Double(x1, y1, x2, y2));
                edgeDtoList.add(edgeDto);
            }
        }

        graphDto.setEdgeDtoList(edgeDtoList);
        VDClient.getInstance().drawAsync(id.getValue(), "graphLayer", graphDto);

    }

    @Override
    public void setPFGuideline(EntityID id, GuideLine guideLine) {

    }


    @Override
    public void drawBuildingDetectorTarget(WorldInfo worldInfo, EntityID agentID, EntityID target) {
        if (target != null) {
            Pair<Integer, Integer> l1 = worldInfo.getLocation(agentID);
            Pair<Integer, Integer> l2 = worldInfo.getLocation(target);
            if (l1 == null || l2 == null){
                return;
            }
            VDClient.getInstance().drawAsync(agentID.getValue(), "BuildingDetectorTargetLayer", new Line2D.Double(l1.first(), l1.second(), l2.first(), l2.second()));
        } else {
            VDClient.getInstance().drawAsync(agentID.getValue(), "BuildingDetectorTargetLayer", null);
        }
    }

    @Override
    public void drawSearchTarget(WorldInfo worldInfo, EntityID agentID, EntityID target) {
        if (target != null) {
            Pair<Integer, Integer> l1 = worldInfo.getLocation(agentID);
            Pair<Integer, Integer> l2 = worldInfo.getLocation(target);
            if (l1 == null || l2 == null){
                return;
            }
            VDClient.getInstance().drawAsync(agentID.getValue(), "SearchTargetLayer", new Line2D.Double(l1.first(), l1.second(), l2.first(), l2.second()));
        } else {
            VDClient.getInstance().drawAsync(agentID.getValue(), "SearchTargetLayer", null);
        }
    }
}
