package adf.sample.module.algorithm;

import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.misc.collections.LazyMap;
import rescuecore2.standard.entities.Area;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

public class SamplePathPlanning extends PathPlanning {

    private Map<EntityID, Set<EntityID>> graph;

    //出发点
    private EntityID from;
    //将要到达地方的entityid
    private Collection<EntityID> targets;
    //算出的路径,从from到goal
    private List<EntityID> result;

    public SamplePathPlanning(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.init();
    }

    private void init() {
        //保存worldinfo中entity和其neighbours
        Map<EntityID, Set<EntityID>> neighbours = new LazyMap<EntityID, Set<EntityID>>() {
            @Override
            public Set<EntityID> createValue() {
                return new HashSet<>();
            }
        };
        for (Entity next : this.worldInfo) {
            if (next instanceof Area) {
                //next的所有neighbors
                Collection<EntityID> areaNeighbours = ((Area) next).getNeighbours();
                //添加next的所有neighbors
                neighbours.get(next.getID()).addAll(areaNeighbours);
            }
        }
        //worldinfo中的entities和其neighbours
        this.graph = neighbours;
    }

    @Override
    public List<EntityID> getResult() {
        return this.result;
    }

    @Override
    public PathPlanning setFrom(EntityID id) {
        this.from = id;
        return this;
    }

    @Override
    public PathPlanning setDestination(Collection<EntityID> targets) {
        this.targets = targets;
        return this;
    }

    @Override
    public PathPlanning updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        return this;
    }

    //只进行了计数操作,默认未实现其他功能
    @Override
    public PathPlanning precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        return this;
    }

    @Override
    public PathPlanning resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        return this;
    }

    @Override
    public PathPlanning preparate() {
        super.preparate();
        return this;
    }

    //寻路算法,获取从from到某一个target的一条路径
    @Override
    public PathPlanning calc() {
        //顺序保存将要走的路径
        List<EntityID> open = new LinkedList<>();
        //保存前一个entity
        Map<EntityID, EntityID> ancestors = new HashMap<>();
        //由于是do-while循环,初始化open
        open.add(this.from);
        EntityID next;
        boolean found = false;
        ancestors.put(this.from, this.from);
        do {
            //获取要走的下一个id
            next = open.remove(0);
            //如果next在targets里,跳出循环
            if (isGoal(next, targets)) {
                found = true;
                break;
            }
            Collection<EntityID> neighbours = graph.get(next);
            if (neighbours.isEmpty()) {
                continue;
            }
            for (EntityID neighbour : neighbours) {
                if (isGoal(neighbour, targets)) {
                    ancestors.put(neighbour, next);
                    //更新next
                    next = neighbour;
                    found = true;
                    break;
                }
                else {
                    if (!ancestors.containsKey(neighbour)) {
                        open.add(neighbour);
                        ancestors.put(neighbour, next);
                    }
                }
            }
        } while (!found && !open.isEmpty());
        if (!found) {
            // No path
            this.result = null;
        }
        // Walk back from goal to this.from
        EntityID current = next;
        //存储从this.from到goal的路径
        LinkedList<EntityID> path = new LinkedList<>();
        do {
            path.add(0, current);
            current = ancestors.get(current);
            if (current == null) {
                throw new RuntimeException("Found a node with no ancestor! Something is broken.");
            }
        } while (current != this.from);
        this.result = path;
        return this;
    }

    //判断是否包含在目的地
    private boolean isGoal(EntityID e, Collection<EntityID> test) {
        return test.contains(e);
    }
}
