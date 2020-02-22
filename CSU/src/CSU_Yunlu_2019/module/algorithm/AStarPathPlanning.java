
package CSU_Yunlu_2019.module.algorithm;

import CSU_Yunlu_2019.standard.CSURoadHelper;
import CSU_Yunlu_2019.standard.Ruler;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.misc.Pair;
import rescuecore2.misc.collections.LazyMap;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
* @Description: 考虑了blockade的A*算法
* @Author: Guanyu-Cai
* @Date: 2/20/20
*/
public class AStarPathPlanning  extends PathPlanning {

    private final boolean DEBUGLOG = false;
    private final boolean LESS = false;

    private Map<EntityID, Set<EntityID>> graph;

    private EntityID from;
    private List<EntityID> targets;
    private List<EntityID> result;

    private StuckDetector stuckDetector;
    private Random random = new Random();
    private HashSet<EntityID> passableRoads;
    private List<EntityID> previousPath = new ArrayList<>();
    private Area previousTarget = null;
    private HashSet<EntityID> impassableRoads;
    private EntityID lastNearestTarget = null;
    private boolean amIPoliceForce = false;
    //持续像同一个目标移动的次数
    private int repeatMovingTime = 0;
    private static final double PASSABLE = 1;
    private static final double UNKNOWN = 1.2;
    // TODO: 2/20/20 确定权值或函数
    private static final double IMPASSABLE = 100;
    // TODO: 2/22/20 不进入着火的房屋?

    public AStarPathPlanning(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.init();
    }

    private void init() {
        Map<EntityID, Set<EntityID>> neighbours = new LazyMap<EntityID, Set<EntityID>>() {
            @Override
            public Set<EntityID> createValue() {
                return new HashSet<>();
            }
        };
        for (Entity next : this.worldInfo) {
            if (next instanceof Area) {
                Collection<EntityID> areaNeighbours = ((Area) next).getNeighbours();
                neighbours.get(next.getID()).addAll(areaNeighbours);
            }
        }
        this.graph = neighbours;

        stuckDetector = new StuckDetector(this.agentInfo);
        this.passableRoads = new HashSet<>();
        this.impassableRoads = new HashSet<>();
        if (agentInfo.me().getStandardURN() == StandardEntityURN.POLICE_FORCE) {
            this.amIPoliceForce = true;
        }
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
        this.targets = new ArrayList<>(targets);
        List<EntityID> toRemoves = new ArrayList<>();
        List<EntityID> toAdd = new ArrayList<>();
        for (EntityID id : targets) {
            StandardEntity entity = worldInfo.getEntity(id);
            //将所有的human换成所在位置
            if (!(entity instanceof Area)) {
                if (entity instanceof Human) {
                    toAdd.add(((Human) entity).getPosition());
                    toRemoves.add(entity.getID());
                }
            }
        }
        this.targets.removeAll(toRemoves);
        this.targets.addAll(toAdd);
        return this;
    }

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

    public void debugLog(String info) {
        if (DEBUGLOG) System.out.println(this.agentInfo.getID() + ": " + info);
    }

    @Override
    public PathPlanning calc() {
        this.result=null;
        List<EntityID> planPath = null;
        Area sourceArea = (Area) worldInfo.getEntity(from);
        debugLog(targets.size() + " targets.");
        stuckDetector.update(from);
        stuckDetector.warnStuck();

        //上次计算出的最近target还未到达
        if (lastNearestTarget != null && targets.contains(lastNearestTarget)) {
            Area target = (Area) worldInfo.getEntity(lastNearestTarget);
            planPath = new ArrayList<>(getPath(sourceArea, target));
            result = planPath;
        }

        if (result == null || result.isEmpty()) {
            targets.sort(new DistanceComparator(worldInfo, agentInfo));
            for (EntityID target1 : targets) {
                Area target = (Area) worldInfo.getEntity(target1);
                lastNearestTarget = target1;
                planPath = new ArrayList<>(getPath(sourceArea, target));
                if (!planPath.isEmpty()) {
                    result = planPath;
                    break;
                }
            }
        }
        return this;
    }

    /**
    * @Description: 根据target获取path
    * @Author: Guanyu-Cai
    * @Date: 2/21/20
    */
    private List<EntityID> getPath(Area sourceArea, Area target) {
        List<EntityID> path = new ArrayList<>();
        if (target == null) {
            return path;
        }
        if (sourceArea.equals(target)) {
            return path;
        }
        //是否需要再寻路
        boolean repeatPlanning = repeatPlanning(target);
        //是否需要重新进行a*寻路
        boolean repeatAStar = !isPositionOnPreviousPath(sourceArea.getID());
        if (repeatAStar || repeatPlanning) {
            Area nearestTarget = null;
            previousPath.clear();
            Node node = getPathEndNode(target.getID());
            path = getPathByEndNode(node);
            previousTarget = nearestTarget;
            previousPath = path;
        } else if (previousTarget.equals(target)) {
            //截取之前的路即可
            ArrayList<EntityID> temp = new ArrayList<>();
            for (EntityID aPreviousPath : previousPath) {
                if (!sourceArea.getID().equals(aPreviousPath)) {
                    temp.add(aPreviousPath);
                } else {
                    break;
                }
            }
            //删点已经走过的路
            previousPath.removeAll(temp);
            path = previousPath;
        }
        return path;
    }

    /**
    * @Description: 根据a*的最后一个节点获取路径
    * @Author: Guanyu-Cai
    * @Date: 2/21/20
    */
    private LinkedList<EntityID> getPathByEndNode(Node node) {
        LinkedList<EntityID> path = new LinkedList<>();
        do {
            path.add(0, node.getID());
            String route = "";
            route += "<=" + node.getID();
            node = node.getParent();
            if (node == null) {
                debugLog("Found a node with no ancestor! Something is broken.");
                break;
            }
        } while (node.getID() != this.from);
        return path;
    }

    /**
    * @Description: a*寻路算法核心
    * @Author: Guanyu-Cai
    * @Date: 2/21/20
    */
    private Node getPathEndNode(EntityID target){
        PriorityQueue<Node> open = new PriorityQueue<>();
        HashSet<EntityID> closed = new HashSet<>();
        Map<EntityID, Node> nodeMap = new HashMap<>();

        Node current = new Node(null, from, target);
        //能找到路的的距离target最近的点
        Node nearest = current;
        open.add(current);
        nodeMap.put(from, current);
        int cnt = 0;
        double count = 0;
        while (!open.isEmpty()) {
            //获取open里f最小的node
            current = open.poll();
            if (current.getHeuristic() < nearest.getHeuristic()) {
                nearest = current;
            }
            EntityID cid = current.getID();
            closed.add(cid);
			Collection<EntityID> neighbours = this.graph.get(cid);

            for (EntityID nid : neighbours) {
                Node neighbor = new Node(current, nid, target);
                if (closed.contains(nid)) {
                    open.remove(cid);
                    continue;
                } else if (!open.contains(nid)) {//不在open
                    if (!neighbor.isImpassable() && isInProperRange(worldInfo.getLocation(from), worldInfo.getLocation(target), worldInfo.getLocation(nid))) {
                        open.add(neighbor);
                        nodeMap.put(nid, neighbor);
                    }else {
                        closed.add(nid);
                        nodeMap.put(nid, neighbor);
                    }
                } else if (nodeMap.containsKey(nid) && nodeMap.get(nid).estimate() > neighbor.estimate()) {//在open,更新g值
                    open.remove(nodeMap.get(nid));
                    open.add(neighbor);
                    nodeMap.put(nid, neighbor);
                }
            }
            if (cid.getValue() == target.getValue()) {
                debugLog("Solved at iteration " + cnt);
//                System.out.println("solved at"+cnt);
                return current;
            }
            ++cnt;
            // debugLog("iteration " + cnt);
        }
        debugLog("path not found.");
        return current;
    }

    /**
    * @Description:判断某个点是否在合适的距离内,提高a*速度,防止找不到时搜索整个地图
    * @param sourceArea 当前所在位置
    * @param target 寻路终点
    * @param checkedArea 被检查是否在某个范围内的点
    * @Author: Guanyu-Cai
    * @Date: 2/21/20
    */
    private boolean isInProperRange(Pair<Integer, Integer> sourceArea,Pair<Integer, Integer> target,Pair<Integer, Integer> checkedArea) {
        return Ruler.getDistance(sourceArea, checkedArea) < 3 * Ruler.getDistance(sourceArea, target);
    }

    private boolean repeatPlanning(Area target) {
        //没有之前的目标,或者重复之前的目标次数超过了3次
        if (previousTarget == null || !previousTarget.equals(target) || repeatMovingTime > 1) {
            repeatMovingTime = 0;
            return true;
        } else {
            repeatMovingTime++;
            return false;
        }
    }

    private boolean isPositionOnPreviousPath(EntityID position) {
        return previousPath.contains(position);
    }

    /**
    * @Description: A*算法的节点
    * @Author: Guanyu-Cai
    * @Date: 2/20/20
    */
    private class Node implements Comparable<Node> {
        private EntityID id;
        private Node parent;
        private double cost;
        private double heuristic;
        private int length;
        private boolean impassable;

        public Node(Node parent, EntityID id, EntityID target) {
            this.id = id;
            this.parent = parent;
            this.impassable = false;

            if (this.parent == null) {
                if(this.getID() != from) {
                    debugLog("this.parentNode==null && this.getID()!=from");
                }
                this.cost = 0;
                this.length = 0;
            } else {
                StandardEntity entity = worldInfo.getEntity(id);
                StandardEntity positionEntity = worldInfo.getPosition(agentInfo.getID());
                //下面还要乘上一个常数,代表路是否能通过的权值
                this.cost = parent.getCost() + Ruler.getManhattanDistance(worldInfo.getLocation(id), worldInfo.getLocation(parent.getID()));
                if (!amIPoliceForce) {
                    if (positionEntity.getStandardURN() != StandardEntityURN.ROAD) {//当前在房子里,不需要考虑可见的Road
                        if (entity.getStandardURN() == StandardEntityURN.ROAD) {
                            //如果可通过
                            if (passableRoads.contains(id)) {
                                cost *= PASSABLE;
                            }else if (impassableRoads.contains(id)) {
                                cost *= IMPASSABLE;
                                impassable = true;
                            }else {
                                cost *= UNKNOWN;
                            }
                        }else {//当前entity是building
                            cost *= PASSABLE;
                        }
                    }else {//当前在Road上,考虑可见的Road是否可以通过
                        //如果当前entity是road
                        if (entity.getStandardURN() == StandardEntityURN.ROAD) {
                            //如果可通过
                            if (passableRoads.contains(id)) {
                                cost *= PASSABLE;
                            }else if(((Road)entity).isBlockadesDefined()) {
                                CSURoadHelper roadHelper = new CSURoadHelper((Road) entity, worldInfo, scenarioInfo);
                                roadHelper.update();
                                if (roadHelper.isPassable()) {//可通过
                                    // TODO: 2/22/20 判断是否能通过的算法表现并不好
                                    passableRoads.add(id);
                                    impassableRoads.remove(id);
                                    cost *= PASSABLE;
                                } else {//不可通过
                                    impassableRoads.add(id);
                                    passableRoads.remove(id);
                                    cost *= IMPASSABLE;
                                    impassable = true;
                                }
                            } else if (impassableRoads.contains(id)){//看不到此road且之前发现路是不通的
                                cost *= IMPASSABLE;
                                impassable = true;
                            }else {
                                cost *= UNKNOWN;
                            }
                        }
                        else {//当前entity是building
                            cost *= PASSABLE;
                        }
                    }
                }
                this.length = parent.getLength() + 1;
            }
            this.heuristic = Ruler.getManhattanDistance(worldInfo.getLocation(id), worldInfo.getLocation(target));
        }

        @Override
        public int compareTo(Node o) {
            double diff = (this.estimate()) - (o.estimate());
            if (diff > 0) return 1;
            if (diff < 0) return -1;
            return 0;
        }

        public EntityID getID() {
            return this.id;
        }

        public double getCost() {
            return this.cost;
        }

        public double getHeuristic() {
            return this.heuristic;
        }

        public double estimate() {
            return this.cost + this.heuristic;
        }

        public Node getParent() {
            return this.parent;
        }

        public EntityID getId() {
            return id;
        }

        public int getLength() {
            return length;
        }

        public boolean isImpassable() {
            return impassable;
        }

    }

    public static class DistanceComparator implements Comparator<EntityID> {
        private WorldInfo worldInfo;
        private AgentInfo agentInfo;

        public DistanceComparator(WorldInfo worldInfo, AgentInfo agentInfo) {
            this.worldInfo = worldInfo;
            this.agentInfo = agentInfo;
        }

        @Override
        public int compare(EntityID o1, EntityID o2) {
            int d1 = worldInfo.getDistance(worldInfo.getEntity(o1), agentInfo.me());
            int d2 = worldInfo.getDistance(worldInfo.getEntity(o2), agentInfo.me());
            return d1 > d2 ? 1 : d1 < d2 ? -1 : 0;
        }
    }
}

