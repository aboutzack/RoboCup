package mrl_2019.world.routing.graph;

import mrl_2019.MRLConstants;
import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.EntityID;

/**
 * Created by Mostafa Shabani
 * Date: Nov 28, 2010
 * Time: 11:49:48 AM
 */
public class MyEdge {

    private EntityID id;
    private Pair<Node, Node> nodes;
    private EntityID areaId;
    private int weight;
    private int entranceWeight;
    private int highWayStrategyWeight;
    private boolean isPassable;
    private Pair<EntityID, EntityID> neighbours;

    public MyEdge(EntityID id, Pair<Node, Node> nodes, EntityID areaId, int weight, boolean isInHighway) {
        this.id = id;
        this.nodes = nodes;
        this.areaId = areaId;
        this.weight = weight;
        this.highWayStrategyWeight = weight;
        if (isInHighway) {
            this.highWayStrategyWeight = weight / 2;
        }
        this.isPassable = true;
    }

    public void setPassable(boolean passable) {
        isPassable = passable;
    }

    public void setEntranceEdgeWeight() {
        this.entranceWeight = (int) (weight * MRLConstants.BUILDING_EDGE_DEV_WEIGHT);
        this.weight = entranceWeight;
    }

    public void setHighWayStrategy() {
        this.weight = highWayStrategyWeight;
    }

    public EntityID getId() {
        return id;
    }

    public Pair<Node, Node> getNodes() {
        return nodes;
    }

    public Node getOtherNode(Node id) {
        if (nodes.first().equals(id)) {
            return nodes.second();
        } else {
            return nodes.first();
        }
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getEntranceWeight() {
        return entranceWeight;
    }

    public EntityID getAreaId() {
        return areaId;
    }

    public boolean isPassable() {
        return isPassable;
    }

    public Pair<EntityID, EntityID> getNeighbours() {
        return neighbours;
    }

    public void setNeighbours(EntityID n1, EntityID n2) {
        neighbours = new Pair<EntityID, EntityID>(n1, n2);
    }
}
