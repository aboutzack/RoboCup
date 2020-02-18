package AUR.util.ambulance;

import adf.agent.info.AgentInfo;
import adf.agent.info.WorldInfo;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * Created by armanaxh in 2017
 */
public class RunPoint {

    private AgentInfo agentInfo;
    private WorldInfo worldInfo;
    private List<EntityID> result;
    private List<EntityID> lastTarget;

    public RunPoint(AgentInfo ai, WorldInfo wi) {
        this.agentInfo = ai;
        this.worldInfo = wi;
        this.result = new LinkedList<>();
        this.lastTarget = new LinkedList<>();
    }

    public RunPoint find() {
        List<EntityID> open = new LinkedList<>();
        Map<EntityID, EntityID> parent = new HashMap<>();
        Map<EntityID, Integer> distance = new HashMap<>();
        EntityID from = agentInfo.getPosition();
        open.add(from);
        EntityID next;
        boolean found = false;
        parent.put(from, null);
        distance.put(from, 0);
        do {
            next = open.remove(0);
            if (isGoal(next, distance)) {
                found = true;
                break;
            }
            StandardEntity entity = worldInfo.getEntity(next);
            if (entity == null) continue;
            if (!(entity instanceof Area)) continue;
            Area area = (Area) entity;
            Collection<EntityID> neighbours = area.getNeighbours();
            if (neighbours.isEmpty()) {
                continue;
            }
            for (EntityID neighbour : neighbours) {
                if (isGoal(neighbour, distance)) {
                    parent.put(neighbour, next);
                    distance.put(neighbour, distance.get(next) + 1);
                    next = neighbour;
                    found = true;
                    break;
                } else {
                    if (!parent.containsKey(neighbour)) {
                        open.add(neighbour);
                        parent.put(neighbour, next);
                        distance.put(neighbour, distance.get(next) + 1);
                    }
                }
            }
        } while (!found && !open.isEmpty());

        EntityID current = next;
        LinkedList<EntityID> path = new LinkedList<>();
        do {
            path.add(0, current);
            current = parent.get(current);
        } while (current != from && current != null);
        this.result = path;
        if (path != null && path.size() > 0)
            this.lastTarget.add(path.get(path.size() - 1));
        return this;
    }

    public boolean isGoal(EntityID next, Map<EntityID, Integer> parent) {
        if (!lastTarget.contains(next)) {
            if (parent.get(next) != null && parent.get(next) > 3) {
                StandardEntity entity = worldInfo.getEntity(next);
                if (entity instanceof Road) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<EntityID> getPath() {
        return this.result;
    }
}
