package mrl_2019.world.routing.graph;

import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Sep 22, 2010
 * Time: 5:03:30 PM
 */
public class Node implements Comparable<Node> {
    private EntityID id;
    // markaze edge
    private Pair<Integer, Integer> position;
    // area haei ke in node rooye edge anha gharar darad.
    private List<EntityID> neighbourAreaIds;
    // entityId = neighbour id; Integer = distance node to this neighbour.
    private List<Pair<EntityID, MyEdge>> neighbourNodes = new ArrayList<Pair<EntityID, MyEdge>>();
    // moshakhas mikone ke in nod rooye edge-i gharar dare ke voroodie building-e.
    private boolean isPassable;
    private boolean isOnBuilding;
    private int lastUpdate = 0;
    // for A* ...
    private EntityID parent;
    private int cost;
    private int depth;
    private int heuristic;
    private int g;
    private boolean isOnTooSmallEdge = false;


    public Node(EntityID id, Pair<Integer, Integer> position) {
        this.id = id;
        this.position = position;
        this.isPassable = true;
        this.isOnBuilding = false;
        this.neighbourNodes = new ArrayList<Pair<EntityID, MyEdge>>();
        this.neighbourAreaIds = new ArrayList<EntityID>();
    }

    public void addNeighbourAreaIds(EntityID neighbourAreaIds) {
        if (!this.neighbourAreaIds.contains(neighbourAreaIds)) {
            this.neighbourAreaIds.add(neighbourAreaIds);
        }
    }

    public void addNeighbourNode(EntityID id, MyEdge a_edge) {
        this.neighbourNodes.add(new Pair<EntityID, MyEdge>(id, a_edge));
    }

    public void setPassable(boolean passable, int time) {
        if (lastUpdate < time || (lastUpdate == time && isPassable)) {
            isPassable = passable;
            this.lastUpdate = time;
        }
    }

    public void setParent(EntityID parent) {
        this.parent = parent;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setHeuristic(int heuristic) {
        this.heuristic = heuristic;
    }

    public void setG(int g) {
        this.g = g;
    }

    public EntityID getId() {
        return id;
    }

    public Pair<Integer, Integer> getPosition() {
        return position;
    }

    public List<EntityID> getNeighbourAreaIds() {
        return neighbourAreaIds;
    }

    public List<Pair<EntityID, MyEdge>> getNeighbourNodes() {
        return neighbourNodes;
    }

//    public MyEdge getNeighbourEdge(EntityID id) {
//
//        for (Pair<EntityID, MyEdge> pair : neighbourNodes) {
//            if (pair.first().equals(id)) {
//                return pair.second();
//            }
//        }
//        return null;
//    }

    public boolean isPassable() {
        return isPassable;
    }

    public boolean isOnBuilding() {
        return isOnBuilding;
    }

    public boolean isOnTooSmallEdge() {
        return isOnTooSmallEdge;
    }

    public void setOnBuilding(boolean onBuilding) {
        isOnBuilding = onBuilding;
    }

    public void setOnTooSmallEdge(boolean onSmallEdge) {
        isOnTooSmallEdge = onSmallEdge;
    }

    public int getLastUpdate() {
        return lastUpdate;
    }

    public EntityID getParent() {
        return parent;
    }

    public int getCost() {
        return cost;
    }

    public int getDepth() {
        return depth;
    }

    public int getHeuristic() {
        return heuristic;
    }

    public int getG() {
        return g;
    }

    @Override
    public int compareTo(Node o) {
        int c = o.getCost();

        if (this.cost > c) //increase
            return 1;
        if (this.cost == c)
            return 0;

        return -1;

    }
}