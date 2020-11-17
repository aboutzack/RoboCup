package CSU_Yunlu_2020.debugger;

import CSU_Yunlu_2020.world.graph.GraphHelper;
import CSU_Yunlu_2020.world.graph.MyEdge;
import CSU_Yunlu_2020.world.graph.Node;
import adf.agent.info.WorldInfo;
import com.mrl.debugger.remote.VDClient;
import com.mrl.debugger.remote.dto.EdgeDto;
import com.mrl.debugger.remote.dto.GraphDto;
import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.EntityID;

import java.awt.geom.Line2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: CSU-zack
 * @Date: 03/21/2020
 */
public class DebugHelper {
    public static final boolean DEBUG_MODE = false;
    public static final VDClient VD_CLIENT = DEBUG_MODE ? VDClient.getInstance() : null;

    static {
        if (DEBUG_MODE) {
            VD_CLIENT.init();
        }
    }

    public static void setGraphEdges(EntityID id, GraphHelper graph) {
        if (DEBUG_MODE) {
            GraphDto graphDto = new GraphDto();
            EdgeDto edgeDto;
            List<EdgeDto> edgeDtoList = new ArrayList<>();
            for (List<MyEdge> edges : graph.getAreaMyEdgesMap().values()) {
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
    }

    public static void drawDetectorTarget(WorldInfo worldInfo, EntityID agentID, EntityID target) {
        if (DEBUG_MODE) {
            List<Line2D> elementList = new ArrayList<>();
            if (target != null) {
                Pair<Integer, Integer> l1 = worldInfo.getLocation(agentID);
                Pair<Integer, Integer> l2 = worldInfo.getLocation(target);
                if (l1 == null || l2 == null) {
                    return;
                }
                elementList.add(new Line2D.Double(l1.first(), l1.second(), l2.first(), l2.second()));
            }
            VDClient.getInstance().drawAsync(agentID.getValue(), "DetectorTargetLayer", (Serializable) elementList);
        }
    }

    public static void drawSearchTarget(WorldInfo worldInfo, EntityID agentID, EntityID target) {
        if (DEBUG_MODE) {
            List<Line2D> elementList = new ArrayList<>();
            if (target != null) {
                Pair<Integer, Integer> l1 = worldInfo.getLocation(agentID);
                Pair<Integer, Integer> l2 = worldInfo.getLocation(target);
                if (l1 == null || l2 == null) {
                    return;
                }
                elementList.add(new Line2D.Double(l1.first(), l1.second(), l2.first(), l2.second()));
            }
            VDClient.getInstance().drawAsync(agentID.getValue(), "SearchTargetLayer", (Serializable) elementList);
        }
    }
}
