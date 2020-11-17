
package CSU_Yunlu_2020.module.algorithm;

import CSU_Yunlu_2020.CSUConstants;
import CSU_Yunlu_2020.standard.Ruler;
import CSU_Yunlu_2020.world.CSUWorldHelper;
import CSU_Yunlu_2020.world.object.CSUEdge;
import CSU_Yunlu_2020.world.object.CSURoad;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.information.MessageRoad;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.misc.Pair;
import rescuecore2.misc.collections.LazyMap;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
* @Description: 将每条road作为Node的寻路算法
* @Author: Guanyu-Cai
* @Date: 2/20/20
*/
@Deprecated
public class AStarPathPlanningOld extends PathPlanning {

    private final boolean DEBUGLOG = false;

    private Map<EntityID, Set<EntityID>> graph;

    private EntityID from;
    private List<EntityID> targets;
    private List<EntityID> result;

//    private StuckDetector stuckDetector;
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
    private static final double IMPASSABLE = 100;
    private static final double BURNING = 100;

    private CSUWorldHelper world;

    public AStarPathPlanningOld(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        if (agentInfo.me() instanceof FireBrigade) {
            world = moduleManager.getModule("WorldHelper.FireBrigade", CSUConstants.WORLD_HELPER_FIRE_BRIGADE);
        } else {
            world = moduleManager.getModule("WorldHelper.Default", CSUConstants.WORLD_HELPER_DEFAULT);
        }
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

//        stuckDetector = new StuckDetector(this.agentInfo);
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
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        this.world.precompute(precomputeData);
        return this;
    }

    @Override
    public PathPlanning resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
        this.world.resume(precomputeData);
        return this;
    }

    @Override
    public PathPlanning preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }
        this.world.preparate();
        return this;
    }

    @Override
    public PathPlanning updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.world.updateInfo(messageManager);
        for (CommunicationMessage message : messageManager.getReceivedMessageList()) {
            Class<? extends CommunicationMessage> messageClass = message.getClass();
            if (messageClass == MessageRoad.class) {
                MessageRoad messageRoad = (MessageRoad) message;
                if (messageRoad.isPassable()) {
                    impassableRoads.remove(messageRoad.getRoadID());
                    passableRoads.add(messageRoad.getRoadID());
                }
            }
        }
        return this;
    }

    public void debugLog(String info) {
        if (DEBUGLOG) System.out.println(this.agentInfo.getID() + ": " + info);
    }

    @Override
    public PathPlanning calc() {
        long a = System.currentTimeMillis();
        this.result=null;
        List<EntityID> planPath = null;
        Area sourceArea = (Area) worldInfo.getEntity(from);
        debugLog(targets.size() + " targets.");
        long b = System.currentTimeMillis();
        //判断当前道路是否可通
        //上次计算出的最近target还未到达
        if (lastNearestTarget != null && targets.contains(lastNearestTarget)) {
            Area target = (Area) worldInfo.getEntity(lastNearestTarget);
            planPath = new ArrayList<>(getPath(sourceArea, target));
            //检测第第一条edge是否可以通过
            if (planPath != null && !planPath.isEmpty()) {
                CSURoad selfRoad = world.getCsuRoad(agentInfo.getPosition());
                if (selfRoad != null) {
                    EntityID firstToRoad = previousPath.get(0);
                    List<CSUEdge> toEdges = selfRoad.getCsuEdgesTo(firstToRoad);
                    if (toEdges != null && !toEdges.isEmpty() && !toEdges.get(0).isBlocked()) {
                        result = planPath;
                    }
                } else {
                    result = planPath;
                }
            }
        }
        if (CSUConstants.DEBUG_PATH_PLANNING) {
            System.out.println("\r<br> lastNearestTarget执行耗时 : "+(System.currentTimeMillis()-b)/1000f+" 秒 ");
        }
        if (result == null || result.isEmpty()) {
            long c = System.currentTimeMillis();
            targets.sort(new DistanceComparator(worldInfo, agentInfo));
            if (CSUConstants.DEBUG_PATH_PLANNING) {
                System.out.println("\r<br> targets排序耗时 : "+(System.currentTimeMillis()-c)/1000f+" 秒 ");
            }
            long d = System.currentTimeMillis();
            for (EntityID target1 : targets) {
                Area target = (Area) worldInfo.getEntity(target1);
                lastNearestTarget = target1;
                planPath = new ArrayList<>(getPath(sourceArea, target));
                if (!planPath.isEmpty()) {
                    //由于road的isPassable只是粗略判断,还需要判断路径的第一条edge是否可通
                    CSURoad selfRoad = world.getCsuRoad(agentInfo.getPosition());
                    if (selfRoad != null) {
                        EntityID firstToRoad = planPath.get(0);
                        List<CSUEdge> toEdges = selfRoad.getCsuEdgesTo(firstToRoad);
                        if (toEdges != null && !toEdges.isEmpty() && !toEdges.get(0).isBlocked()) {
                            result = planPath;
                            break;
                        }
                    } else {
                        result = planPath;
                        break;
                    }
                }
            }
            if (CSUConstants.DEBUG_PATH_PLANNING) {
                System.out.println("\r<br> 寻找可到达target耗时 : "+(System.currentTimeMillis()-d)/1000f+" 秒 ");
            }
        }
        if (result != null && result.isEmpty()) {
            result = null;
        }
        if (CSUConstants.DEBUG_PATH_PLANNING) {
            System.out.println(agentInfo.getID() +" 总执行耗时 : "+(System.currentTimeMillis()-a)/1000f+" 秒 ");
            System.out.println("==================================");
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
            if (node != null) {
                path = getPathByEndNode(node);
                previousTarget = nearestTarget;
                previousPath = path;
            }
        } else if (previousTarget.equals(target)) {
            //截取之前的路,但要重新判断是否可以通过
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
            boolean stillPassable = true;
            //粗略检测路点是否可通过
            for (int i = 0; i < Math.min(3, previousPath.size()); i++) {
                EntityID id = previousPath.get(i);
                Area area = (Area)worldInfo.getEntity(id);
                if (area instanceof Road && area.isBlockadesDefined()){
                    CSURoad csuRoad = world.getCsuRoad(id);
                    if (!csuRoad.isPassable()) {
                        stillPassable = false;
                        passableRoads.remove(id);
                        impassableRoads.add(id);
                        break;
                    }
                }
            }
            if (stillPassable) {
                path = previousPath;
            }else {
                Area nearestTarget = null;
                previousPath.clear();
                Node node = getPathEndNode(target.getID());
                if (node != null) {
                    path = getPathByEndNode(node);
                    previousTarget = nearestTarget;
                    previousPath = path;
                }
            }
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
        if (!current.isImpassable()) {
            open.add(current);
            nodeMap.put(from, current);
        }
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
                if (closed.contains(nid)) {
                    continue;
                }
                Node neighbor = new Node(current, nid, target);
                if (neighbor.isImpassable()) {
                    closed.add(nid);
                    continue;
                }
                Node node = nodeMap.get(nid);
                if (!open.contains(node)) {//不在open
                    if (!neighbor.isImpassable() && isInProperRange(worldInfo.getLocation(from), worldInfo.getLocation(target), worldInfo.getLocation(nid))) {
                        open.add(neighbor);
                        nodeMap.put(nid, neighbor);
                    } else {
                        closed.add(nid);
                        nodeMap.put(nid, neighbor);
                    }
                } else if (node != null && node.estimate() > neighbor.estimate()) {//在open,更新g值
                    open.remove(node);
                    open.add(neighbor);
                    nodeMap.put(nid, neighbor);
                }
            }
            if (cid.getValue() == target.getValue()) {
                debugLog("Solved at iteration " + cnt);
                if (CSUConstants.DEBUG_PATH_PLANNING) {
                    System.out.println("solved at"+cnt);
                }
                return current;
            }
            ++cnt;
            // debugLog("iteration " + cnt);
        }
        debugLog("path not found.");
        if (CSUConstants.DEBUG_PATH_PLANNING) {
            System.out.println("unSolved at"+cnt);
        }
        return null;
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
                Area area = (Area) worldInfo.getEntity(entity.getID());
                if (area instanceof Building) {
                    if (((Building) area).isFierynessDefined()) {//防止进入着火的屋子
                        int fieriness = ((Building) area).getFieryness();
                        if (fieriness > 0 && fieriness < 4) {
                            cost *= BURNING;
                            impassable = true;
                        }
                    }
                    CSURoad entrance = world.getCsuRoad(parent.getID());
                    if (entrance != null) {
                        List<CSUEdge> toEdges = entrance.getCsuEdgesTo(id);
                        if (toEdges != null && !toEdges.isEmpty() && toEdges.get(0).isBlocked() && !amIPoliceForce) {//排除进不去的房屋
                            cost *= IMPASSABLE;
                            impassable = true;
                        }
                    }
                } else if (!amIPoliceForce) {//at或者fb
                    //如果当前entity是road
                    if (area instanceof Road) {
                        //如果可通过
                        if (passableRoads.contains(id)) {
                            cost *= PASSABLE;
                        } else if (area.isBlockadesDefined()) {
                            CSURoad csuRoad = world.getCsuRoad(id);
                            List<CSUEdge> toEdges = csuRoad.getCsuEdgesTo(parent.getID());
                            if (toEdges != null && !toEdges.isEmpty()) {//edge已知,需要加强条件
                                if (csuRoad.isPassable() && !toEdges.get(0).isBlocked()) {
                                    passableRoads.add(id);
                                    impassableRoads.remove(id);
                                    cost *= PASSABLE;
                                } else {
                                    impassableRoads.add(id);
                                    passableRoads.remove(id);
                                    cost *= IMPASSABLE;
                                    impassable = true;
                                }
                            }else {//edge未知,只判断road是否可通
                                if (csuRoad.isPassable()) {
                                    passableRoads.add(id);
                                    impassableRoads.remove(id);
                                    cost *= PASSABLE;
                                } else {
                                    impassableRoads.add(id);
                                    passableRoads.remove(id);
                                    cost *= IMPASSABLE;
                                    impassable = true;
                                }
                            }
                        } else if (impassableRoads.contains(id)) {//看不到此road且之前发现路是不通的
                            cost *= IMPASSABLE;
                            impassable = true;
                        } else {
                            cost *= UNKNOWN;
                        }
                    } else {//当前entity是building
                        cost *= PASSABLE;
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

