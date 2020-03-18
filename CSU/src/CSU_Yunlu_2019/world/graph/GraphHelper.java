package CSU_Yunlu_2019.world.graph;

import CSU_Yunlu_2019.CSUConstants;
import CSU_Yunlu_2019.standard.Ruler;
import CSU_Yunlu_2019.world.CSUWorldHelper;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.module.AbstractModule;
import javolution.util.FastMap;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.geom.Line2D;
import java.util.*;


/**
 * @Description: 基于mrl的GraphModule
 * @Date: 3/18/20
 */
public class GraphHelper extends AbstractModule {
    private Map<EntityID, Node> idNodeMap = new FastMap<>();
    private Map<EntityID, List<Node>> areaNodesMap = new FastMap<>();
    private Map<Pair<Integer, Integer>, Node> locationNodeMap = new FastMap<>();
    private Map<EntityID, List<MyEdge>> areaMyEdgesMap = new FastMap<>();

    private int nodeSize;
    private int nodeIdGenerator = 0;
    private int edgeIdGenerator = 0;

    private Set<StandardEntity> buildings;
    private Set<StandardEntity> roads;
    private Set<StandardEntity> entrances;
    private Set<StandardEntity> areas;


    public GraphHelper(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        areas = new HashSet<>(CSUWorldHelper.getAreasWithURN(wi));
        roads = new HashSet<>(CSUWorldHelper.getRoadsWithURN(wi));
        buildings = new HashSet<>(CSUWorldHelper.getBuildingsWithURN(wi));
        entrances = new HashSet<>();

        initEntrances();
        initGraph();
    }

    private void initEntrances() {
        for (StandardEntity roadEntity : roads) {
            Road road = (Road) roadEntity;
            for (Edge edge : road.getEdges()) {
                if (edge.isPassable()) {
                    if (buildings.contains(worldInfo.getEntity(edge.getNeighbour()))) {
                        entrances.add(roadEntity);
                        break;
                    }
                }
            }
        }
    }

    private void initGraph() {
        //area上所有nodes的坐标
        ArrayList<Pair<Integer, Integer>> nodesLocations;
        for (StandardEntity entity : areas) {
            Area area = (Area) entity;
            nodesLocations = getNodesLocations(area);
            initNodes(area, nodesLocations);
        }

        List<Node> thisAreaNodes;
        for (StandardEntity entity : areas) {
            Area area = (Area) entity;
            thisAreaNodes = areaNodesMap.get(entity.getID());
            initEdges(area, thisAreaNodes);
        }
        nodeSize = idNodeMap.size();
    }

    /**
     * 初始化nodes
     */
    private void initNodes(Area area, ArrayList<Pair<Integer, Integer>> pairs) {
        ArrayList<Node> thisAreaNodes = new ArrayList<>();
        boolean isEntrance = getEntrances().contains(area);

        for (Pair<Integer, Integer> pair1 : pairs) {
            //不重复添加,因为有多个area共享edge
            Node node1 = getNode(pair1);
            if (node1 == null) {
                EntityID id = new EntityID(++nodeIdGenerator);
                node1 = new Node(id, pair1);
                idNodeMap.put(id, node1);
                locationNodeMap.put(pair1, node1);
            }
            //set all nodes on edges of building flag
            if (isEntrance && !node1.isOnBuilding()) {
                node1.setOnBuilding(true);
            }
            if (isOnTooSmallEdge(area, node1)) {
                node1.setOnTooSmallEdge(true);
            }
            thisAreaNodes.add(node1);
            //添加此area为neighbour,一个node可以有多个neighbour area
            node1.addNeighbourAreaIds(area.getID());
        }
        areaNodesMap.put(area.getID(), thisAreaNodes);
    }

    /**
     * 初始化nodes连接成的edges
     */
    private void initEdges(Area area, List<Node> nodes) {
        ArrayList<MyEdge> thisAreaMyEdges = new ArrayList<>();
        int weight;
        // create area Nodes
        for (Node node1 : nodes) {
            // add nodes neighbours
            for (Node node2 : nodes) {
                if (!node1.equals(node2)) {
                    MyEdge myEdge = getThisAreaMyEdge(new Pair<>(node1, node2), thisAreaMyEdges);

                    if (myEdge == null) {
                        EntityID edgeId = new EntityID(edgeIdGenerator++);
                        weight = (int) Ruler.getDistance(node1.getPosition(), node2.getPosition());
                        myEdge = new MyEdge(edgeId, new Pair<>(node1, node2), area.getID(), weight);
                        //设置edge连接的area(除此area)
                        setMyEdgeNeighbours(area, myEdge);
                        thisAreaMyEdges.add(myEdge);
                        //set edge weight for on building
                        if (node1.isOnBuilding() || node2.isOnBuilding()) {
                            myEdge.setEntranceEdgeWeight();
                        }
                        if (node1.isOnTooSmallEdge() || node2.isOnTooSmallEdge()) {
                            myEdge.setOnTooSmallEdgeWeight();
                        }
                    }
                    //在同一个area的node都视为相邻
                    node1.addNeighbourNode(node2.getId(), myEdge);
                }
            }
        }
        areaMyEdgesMap.put(area.getID(), thisAreaMyEdges);
    }

    @Override
    public AbstractModule calc() {
        return this;
    }

    public Node getNode(EntityID id) {
        return idNodeMap.get(id);
    }

    public Node getNode(Pair<Integer, Integer> pair) {
        return locationNodeMap.get(pair);
    }

    public Node getNode(Point2D point) {
        return getNode(new Pair<>((int) point.getX(), (int) point.getY()));
    }

    public Map<EntityID, List<MyEdge>> getAreaMyEdgesMap() {
        return areaMyEdgesMap;
    }

    public MyEdge getMyEdge(EntityID areaId, Pair<Node, Node> pair) {
        List<MyEdge> thisAreaMyEdges = areaMyEdgesMap.get(areaId);
        Pair<Node, Node> reversePair = new Pair<>(pair.second(), pair.first());

        for (MyEdge myEdge : thisAreaMyEdges) {
            if (pair.equals(myEdge.getNodes()) || reversePair.equals(myEdge.getNodes())) {
                return myEdge;
            }
        }
        throw new RuntimeException("ERROR: getMyEdge(): not found myEdge in area:" + areaId +
                "  nodes:<" + pair.first().getId().getValue() + ", " + pair.second().getId().getValue() + ">");
    }

    public List<MyEdge> getMyEdgesBetween(Area from, Area to) {
        List<MyEdge> myEdgesBetween = new ArrayList<>();
        List<MyEdge> thisAreaMyEdge = getMyEdgesInArea(from.getID());
        for (MyEdge myEdge : thisAreaMyEdge) {
            if (!myEdgesBetween.contains(myEdge) &&
                    (myEdge.getNodes().first().getNeighbourAreaIds().contains(to.getID()) || myEdge.getNodes().second().getNeighbourAreaIds().contains(to.getID()))) {
                myEdgesBetween.add(myEdge);
            }
        }
        return myEdgesBetween;
    }

    private MyEdge getThisAreaMyEdge(Pair<Node, Node> pair, List<MyEdge> thisAreaMyEdges) {
        Pair<Node, Node> reversePair = new Pair<>(pair.second(), pair.first());

        for (MyEdge myEdge : thisAreaMyEdges) {
            if (pair.equals(myEdge.getNodes()) || reversePair.equals(myEdge.getNodes())) {
                return myEdge;
            }
        }
        return null;
    }

    public List<MyEdge> getMyEdgesInArea(EntityID areaId) {
        return areaMyEdgesMap.get(areaId);
    }

    public List<Node> getAreaNodes(EntityID areaId) {
        return areaNodesMap.get(areaId);
    }

    public Node getNodeBetweenAreas(EntityID areaId1, EntityID areaId2, Edge edge) {
        List<Node> nodes = new ArrayList<Node>(areaNodesMap.get(areaId1));
        if (edge == null) {
            for (Node node : nodes) {
                if (node.getNeighbourAreaIds().contains(areaId2)) {
                    return node;
                }
            }
        } else {
            for (Node node : nodes) {
                if (node.getNeighbourAreaIds().contains(areaId2) && getEdgeMiddle(edge).equals(node.getPosition())) {
                    return node;
                }
            }
        }
        return null;
    }

    /**
     * @return area上所有passableEdge中点的坐标
     */
    private ArrayList<Pair<Integer, Integer>> getNodesLocations(Area area) {
        ArrayList<Pair<Integer, Integer>> nodes = new ArrayList<>();
        Pair<Integer, Integer> edgeMiddle;
        for (Edge edge : area.getEdges()) {
            if (edge.isPassable()) {
                edgeMiddle = getEdgeMiddle(edge);
                nodes.add(edgeMiddle);
            }
        }
        return nodes;
    }

    private boolean isOnTooSmallEdge(Area area, Node node) {
        Line2D line2D;
        for (Edge edge : area.getEdges()) {
            if (edge.isPassable()) {
                line2D = new Line2D.Double(edge.getStartX(), edge.getStartY(), edge.getEndX(), edge.getEndY());
                if (Ruler.getDistance(edge.getStartX(), edge.getStartY(), edge.getEndX(), edge.getEndY()) < CSUConstants.TOO_SMALL_EDGE_THRESHOLD
                        && line2D.contains(node.getPosition().first(), node.getPosition().second())) {
                    return true;
                }
            }
        }
        return false;

    }

    private void setMyEdgeNeighbours(Area area, MyEdge myEdge) {
        EntityID n1 = null;
        EntityID n2 = null;
        for (EntityID neighbour : myEdge.getNodes().first().getNeighbourAreaIds()) {
            if (neighbour.equals(area.getID()))
                continue;
            n1 = neighbour;
            break;
        }
        for (EntityID neighbour : myEdge.getNodes().second().getNeighbourAreaIds()) {
            if (neighbour.equals(area.getID()))
                continue;
            n2 = neighbour;
            break;
        }
        myEdge.setNeighbours(n1, n2);
    }

    public int getNodeSize() {
        return nodeSize;
    }

    public static int getEdgeLength(Edge edge) {
        return Ruler.getDistance(edge.getStartX(), edge.getStartY(), edge.getEndX(), edge.getEndY());
    }

    public static Pair<Integer, Integer> getEdgeMiddle(Edge edge) {
        int x = (int) ((edge.getStartX() + edge.getEndX()) / 2.0);
        int y = (int) ((edge.getStartY() + edge.getEndY()) / 2.0);
        return new Pair<>(x, y);
    }

    public Collection<StandardEntity> getEntrances() {
        return entrances;
    }
}
