package CSU_Yunlu_2020.world.graph;

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
    private Pair<Integer, Integer> position;
    private List<EntityID> neighbourAreaIds;
    private List<Pair<EntityID, MyEdge>> neighbourNodes;
    private boolean isPassable;
    private boolean isOnBuilding;
    private int lastUpdate = 0;
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
        this.neighbourNodes = new ArrayList<>();
        this.neighbourAreaIds = new ArrayList<>();
    }

    public void addNeighbourAreaIds(EntityID neighbourAreaIds) {
        if (!this.neighbourAreaIds.contains(neighbourAreaIds)) {
            this.neighbourAreaIds.add(neighbourAreaIds);
        }
    }

    public void addNeighbourNode(EntityID id, MyEdge myEdge) {
        this.neighbourNodes.add(new Pair<>(id, myEdge));
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