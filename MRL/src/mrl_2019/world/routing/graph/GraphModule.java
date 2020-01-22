package mrl_2019.world.routing.graph;

import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.module.AbstractModule;
import javolution.util.FastMap;
import mrl_2019.util.Util;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.geom.Line2D;
import java.util.*;

/**
 * Created by Mostafa Shabani.
 * Date: Sep 22, 2010
 * Time: 5:03:52 PM
 */
public class GraphModule extends AbstractModule {

    private Map<EntityID, Node> nodes = new FastMap<EntityID, Node>();
    private Map<EntityID, ArrayList<Node>> allAreasNodes = new FastMap<EntityID, ArrayList<Node>>();
    private Map<Pair<Integer, Integer>, Node> pairNodeMap = new FastMap<Pair<Integer, Integer>, Node>();
    //    private Map<EntityID, MyEdge> myEdges = new FastMap<EntityID, MyEdge>();
    private Map<EntityID, List<MyEdge>> allAreaMyEdges = new FastMap<EntityID, List<MyEdge>>();
    private int graphSize;
    int nodeIdGenerator = 0;
    int edgeIdGenerator = 0;
    private Collection<StandardEntity> entrances;
    private Set<StandardEntity> buildings, roads, areas;


    public GraphModule(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager,developData);
        areas = new HashSet<>(getAreasWithURN());
        roads = new HashSet<>(getRoadsWithURN());
        buildings = new HashSet<>(getBuildingsWithURN());
        entrances = new HashSet<>();
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
        createGraph();



    }

    @Override
    public AbstractModule calc() {
        return this;
    }

//    public Collection<Node> getNodes() {
//        return nodes.values();
//    }

    public Node getNode(EntityID id) {
        return nodes.get(id);
    }

    public Node getNode(Pair<Integer, Integer> pair) {
        return pairNodeMap.get(pair);
    }

    public Node getNode(Point2D point) {
        return getNode(new Pair<Integer, Integer>((int) point.getX(), (int) point.getY()));
    }


    public Map<EntityID, List<MyEdge>> getAllAreaMyEdges() {
        return allAreaMyEdges;
    }

    public MyEdge getMyEdge(EntityID areaId, Pair<Node, Node> pair) {

        List<MyEdge> thisAreaMyEdges = allAreaMyEdges.get(areaId);
        Pair<Node, Node> reversePair = new Pair<Node, Node>(pair.second(), pair.first());

        for (MyEdge myEdge : thisAreaMyEdges) {
//            if (pair.first().equals(myEdge.getNodes().first().getId()) && pair.second().equals(myEdge.getNodes().second().getId())
//                    || reversePair.first().equals(myEdge.getNodes().first().getId()) && reversePair.second().equals(myEdge.getNodes().second().getId())) {

            if (pair.equals(myEdge.getNodes()) || reversePair.equals(myEdge.getNodes())) {
                return myEdge;
            }
        }

        throw new RuntimeException("ERROR: getMyEdge(): not found myEdge in area:" + areaId +
                "  nodes:<" + pair.first().getId().getValue() + ", " + pair.second().getId().getValue() + ">");
    }

    public List<MyEdge> getMyEdgesBetween(Area from, Area to) {
        List<MyEdge> myEdgesBetween = new ArrayList<MyEdge>();
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

        Pair<Node, Node> reversePair = new Pair<Node, Node>(pair.second(), pair.first());

        for (MyEdge myEdge : thisAreaMyEdges) {
            if (pair.equals(myEdge.getNodes()) || reversePair.equals(myEdge.getNodes())) {
                return myEdge;
            }
        }
        return null;
    }

    public List<MyEdge> getMyEdgesInArea(EntityID areaId) {

        return allAreaMyEdges.get(areaId);
    }

    public List<Node> getAreaNodes(EntityID areaId) {
        /**
         * in method tamame node haye yek area ro barmigardoone.
         */
        return allAreasNodes.get(areaId);
    }

    public Node getNodeBetweenAreas(EntityID areaId1, EntityID areaId2, Edge edge) {

        List<Node> nodes = new ArrayList<Node>(allAreasNodes.get(areaId1));

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

    private void createGraph() {
        /**
         * method asli baraye sakhtane graph.
         * be ezaye har area edge haye passable ra gerefte va rooye harkodam yek node dorost mikonad.
         */
        ArrayList<Pair<Integer, Integer>> thisAreaVertices;

        for (StandardEntity entity : areas) {
            Area area = (Area) entity;
            thisAreaVertices = new ArrayList<Pair<Integer, Integer>>();
            thisAreaVertices.addAll(findVertexes(area));
            createAreNodes(area, thisAreaVertices);
        }

        List<Node> thisAreaNodes;
        for (StandardEntity entity : areas) {
            Area area = (Area) entity;
            thisAreaNodes = allAreasNodes.get(entity.getID());
            createAreaGraph(area, thisAreaNodes);
        }
        graphSize = nodes.size();
    }


    public Collection<StandardEntity> getAreasWithURN() {
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.REFUGE,
                StandardEntityURN.ROAD,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.HYDRANT,
                StandardEntityURN.GAS_STATION);
    }

    public Collection<StandardEntity> getBuildingsWithURN() {
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.REFUGE,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.GAS_STATION);
    }

    public Collection<StandardEntity> getRoadsWithURN() {
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.ROAD,
                StandardEntityURN.HYDRANT);
    }

    private ArrayList<Pair<Integer, Integer>> findVertexes(Area area) {
        /**
         * bedast avardane middle edge haye passable.
         * created by P.D.G
         */

        ArrayList<Pair<Integer, Integer>> nodes = new ArrayList<Pair<Integer, Integer>>();
        Pair<Integer, Integer> edgeMiddle;
//        int size = (int) (MRLConstants.AGENT_SIZE * 1.5d);

        for (Edge edge : area.getEdges()) {
            if (edge.isPassable() /*&& EdgeHelper.getEdgeLength(edge) >= size*/) { //todo: Mostafa merge edges

                edgeMiddle = getEdgeMiddle(edge);
                nodes.add(edgeMiddle);
            }
        }

        return nodes;
    }

    private void createAreNodes(Area area, ArrayList<Pair<Integer, Integer>> pairs) {
        ArrayList<Node> thisAreaNodes = new ArrayList<Node>();
        boolean isEntrance = getEntrances().contains(area);

        // create area Nodes
        for (Pair<Integer, Integer> pair1 : pairs) {

            Node node1 = getNode(pair1);
            if (node1 == null) {
                EntityID id = new EntityID(++nodeIdGenerator);
                node1 = new Node(id, pair1);
                nodes.put(id, node1);
                pairNodeMap.put(pair1, node1);
            }
            //set all nodes on edges of building flag
            if (isEntrance && !node1.isOnBuilding()) {
                node1.setOnBuilding(true);
            }
            if (isOnTooSmallEdge(area, node1)) {
                node1.setOnTooSmallEdge(true);
            }


            // add nodes neighbours
            for (Pair<Integer, Integer> pair2 : pairs) {

                Node node2 = getNode(pair2);
                if (node2 == null) {
                    EntityID id = new EntityID(++nodeIdGenerator);
                    node2 = new Node(id, pair2);
                    nodes.put(id, node2);
                    pairNodeMap.put(pair2, node2);
                }
                if (isEntrance && !node2.isOnBuilding()) {
                    node2.setOnBuilding(true);
                }
                if (isOnTooSmallEdge(area, node2)) {
                    node2.setOnTooSmallEdge(true);
                }

            }
            thisAreaNodes.add(node1);
            node1.addNeighbourAreaIds(area.getID());


        }

        allAreasNodes.put(area.getID(), thisAreaNodes);
    }

    private boolean isOnTooSmallEdge(Area area, Node node) {

        Line2D line2D;
        for (Edge edge : area.getEdges()) {
            if (edge.isPassable()) {
                line2D = new Line2D.Double(edge.getStartX(), edge.getStartY(), edge.getEndX(), edge.getEndY());
                if (Util.distance(edge.getStartX(), edge.getStartY(), edge.getEndX(), edge.getEndY()) < 600
                        && line2D.contains(node.getPosition().first(), node.getPosition().second())) {
                    return true;
                }
            }
        }
        return false;

    }

    private void createAreaGraph(Area area, List<Node> nodes) {
        /**
         * yek area va point ha ra gerefte va ba anha baraye area node dorost mikonad.
         */
        ArrayList<MyEdge> thisAreaMyEdges = new ArrayList<MyEdge>();
        int weight;

        // create area Nodes
        for (Node node1 : nodes) {

            // add nodes neighbours
            for (Node node2 : nodes) {

                if (!node1.equals(node2)) {
                    MyEdge myEdge = getThisAreaMyEdge(new Pair<Node, Node>(node1, node2), thisAreaMyEdges);

                    if (myEdge == null) {
                        EntityID edgeId = new EntityID(edgeIdGenerator++);
                        weight = Util.distance(node1.getPosition(), node2.getPosition());

                        myEdge = new MyEdge(edgeId, new Pair<Node, Node>(node1, node2), area.getID(), weight, false/*world.getHighways().isContains(area.getID())*/);
                        setMyEdgeNeighbours(area, myEdge);
//                        myEdges.put(edgeId, myEdge);
                        thisAreaMyEdges.add(myEdge);


                        //set edge weight for on building
                        if (node1.isOnBuilding() || node2.isOnBuilding()) {
                            myEdge.setEntranceEdgeWeight();
                        }
                        if (node1.isOnTooSmallEdge() || node2.isOnTooSmallEdge()) {
                            weight = (myEdge.getWeight() * 20);
                            myEdge.setWeight(weight);
                        }
                    }
                    node1.addNeighbourNode(node2.getId(), myEdge);


                }
            }
        }

        allAreaMyEdges.put(area.getID(), thisAreaMyEdges);



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

    public int getGraphSize() {
        return graphSize;
    }

    public void setHighWayStrategy() {
        for (List<MyEdge> edges : allAreaMyEdges.values()) {
            for (MyEdge edge : edges) {
                edge.setHighWayStrategy();
            }
        }
    }

    public static int getEdgeLength(Edge edge) {
        return Util.distance(edge.getStartX(), edge.getStartY(), edge.getEndX(), edge.getEndY());
    }

    public static Pair<Integer, Integer> getEdgeMiddle(Edge edge) {
        int x = (int) ((edge.getStartX() + edge.getEndX()) / 2.0);
        int y = (int) ((edge.getStartY() + edge.getEndY()) / 2.0);
        return new Pair<Integer, Integer>(x, y);
    }

    public Collection<StandardEntity> getEntrances() {
        return entrances;
    }
}