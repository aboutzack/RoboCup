package CSU_Yunlu_2019.debugger;

import CSU_Yunlu_2019.world.graph.GraphHelper;
import CSU_Yunlu_2019.world.graph.MyEdge;
import CSU_Yunlu_2019.world.graph.Node;
import com.mrl.debugger.remote.VDClient;
import com.mrl.debugger.remote.dto.EdgeDto;
import com.mrl.debugger.remote.dto.GraphDto;
import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.EntityID;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Guanyu-Cai
 * @Date: 03/21/2020
 */
public class DebugHelper {
    public static final boolean DEBUG_MODE = true;
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
}
