package CSU_Yunlu_2020.module.algorithm;

import CSU_Yunlu_2020.CSUConstants;
import CSU_Yunlu_2020.standard.Ruler;
import CSU_Yunlu_2020.world.CSUWorldHelper;
import CSU_Yunlu_2020.world.graph.GraphHelper;
import CSU_Yunlu_2020.world.graph.MyEdge;
import CSU_Yunlu_2020.world.graph.Node;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * @description: 将每条passable edge作为Node的寻路算法
 * @author: Guanyu-Cai
 * @Date: 03/18/2020
 */
public class AStarPathPlanning extends PathPlanning {
    private EntityID from;
    private List<EntityID> targets;
    private List<EntityID> result;
    private EntityID resultTarget;

    private List<EntityID> previousPath = new ArrayList<>();
    private Area previousTarget = null;
    private boolean amIPoliceForce = false;
    private int repeatMovingTime = 0;//持续向同一个目标移动的次数
    private int pathCost = -1;

    private static final double PASSABLE = 1;
    private static final double UNKNOWN = 1.2;
    private static final double IMPASSABLE = 100;
    private static final double BURNING = 100;

    private CSUWorldHelper world;
    private GraphHelper graph;

    public AStarPathPlanning(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        if (agentInfo.me() instanceof FireBrigade) {
            world = moduleManager.getModule("WorldHelper.FireBrigade", CSUConstants.WORLD_HELPER_FIRE_BRIGADE);
        } else {
            world = moduleManager.getModule("WorldHelper.Default", CSUConstants.WORLD_HELPER_DEFAULT);
        }
        graph = moduleManager.getModule("GraphHelper.Default", CSUConstants.GRAPH_HELPER_DEFAULT);
        if (agentInfo.me() instanceof PoliceForce) {
            amIPoliceForce = true;
        }
    }

    @Override
    public PathPlanning precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        this.world.precompute(precomputeData);
        this.graph.precompute(precomputeData);
        return this;
    }

    @Override
    public PathPlanning resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
        this.world.resume(precomputeData);
        this.graph.resume(precomputeData);
        return this;
    }

    @Override
    public PathPlanning preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }
        this.world.preparate();
        this.graph.preparate();
        return this;
    }

    @Override
    public PathPlanning updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.world.updateInfo(messageManager);
        this.graph.updateInfo(messageManager);
        return this;
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
    public double getDistance() {
        return pathCost;
    }

    @Override
    public PathPlanning calc() {
        this.result = null;
        List<EntityID> planPath;
        Area sourceArea = (Area) worldInfo.getEntity(from);
        if (previousTarget != null && targets.contains(previousTarget.getID())) {
            Area target = previousTarget;
            planPath = new ArrayList<>(getPath(sourceArea, target));
            result = planPath;
            if (!result.isEmpty()) {
                resultTarget = target.getID();
            }
        }
        if (result == null || result.isEmpty()) {
            targets.sort(new DistanceComparator(worldInfo, agentInfo));
            for (EntityID target1 : targets) {
                Area target = (Area) worldInfo.getEntity(target1);
                planPath = new ArrayList<>(getPath(sourceArea, target));
                if (!planPath.isEmpty()) {
                    result = planPath;
                    resultTarget = target1;
                    break;
                }
            }
        }
        if (result != null && result.isEmpty()) {
            result = null;
            resultTarget = null;
        }
        return this;
    }

    /**
     * @return 最终计算出的path
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
            previousPath.clear();
            path.addAll(getGraphPath(sourceArea, target));
            if (!path.isEmpty()) {
                path = getAreaPath(sourceArea, target, path);
            }
            previousTarget = target;
            previousPath = path;
        } else if (previousTarget.equals(target)) {
            //截取之前计算的路
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
     * @return 获取node组成的path
     */
    public List<EntityID> getGraphPath(Area source, Area destination) {
        if (destination == null) {
            return new ArrayList<>();
        }
        //获取距离source最近的node
        Node sourceNode = getNearestNode(source, world.getSelfLocation());
        //获取距离destination最近的node
        Node destinationNode = getNearestNode(destination, world.getLocation(destination));
        int extraPathCost = 0;
        if (sourceNode == null || destinationNode == null) {
            return new ArrayList<>();
        }
        extraPathCost += Ruler.getDistance(source.getLocation(worldInfo.getRawWorld()), sourceNode.getPosition());
        extraPathCost += Ruler.getDistance(destination.getLocation(worldInfo.getRawWorld()), destinationNode.getPosition());

        boolean findPath = false;
        Set<Node> open = new HashSet<>();
        Set<EntityID> closed = new HashSet<>();
        Node current;
        sourceNode.setG(0);
        sourceNode.setCost(0);
        sourceNode.setDepth(0);
        sourceNode.setParent(null);
        destinationNode.setParent(null);
        open.add(sourceNode);

        if (sourceNode.equals(destinationNode)) {
            pathCost = sourceNode.getCost();
            pathCost += extraPathCost;
            return getPathByEndNode(destinationNode);
        }

        int maxDepth = 0;
        pathCost = -1;

        //小于node的数量和open不为空
        while ((maxDepth < graph.getNodeSize()) && (open.size() != 0)) {

            current = Collections.min(open);
            pathCost = current.getCost();
            pathCost += extraPathCost;
            if (current.equals(destinationNode)) {
                findPath = true;
                break;
            }

            open.remove(current);
            closed.add(current.getId());

            //areaId-myEdge , 获取所有neighbour nodes
            for (Pair<EntityID, MyEdge> neighbour : current.getNeighbourNodes()) {
                MyEdge neighbourMyEdge = neighbour.second();
                Node neighbourNode = neighbourMyEdge.getOtherNode(current);

                //获取可通过的myEdge,当自己是pf时无视障碍物直接选择最短路径
                if (!closed.contains(neighbourNode.getId()) && ((neighbourMyEdge.isPassable()) || amIPoliceForce)) {
                    //edge的weight加上current的weight
                    int neighbourG = neighbourMyEdge.getWeight() + current.getG(); // neighbour weight

                    //如果房屋着火
                    Area area = (Area) worldInfo.getEntity(neighbourMyEdge.getAreaId());
                    if ((area instanceof Building) && ((Building) area).isFierynessDefined()) {
                        int fieriness = ((Building) area).getFieryness();
                        if (fieriness > 0 && fieriness < 4) {
                            neighbourG *= BURNING;
                        }
                    }

                    if (!open.contains(neighbourNode)) {

                        neighbourNode.setParent(current.getId());
                        neighbourNode.setHeuristic((int) Ruler.getDistance(neighbourNode.getPosition(), destinationNode.getPosition()));
                        neighbourNode.setG(neighbourG);
                        neighbourNode.setCost(neighbourNode.getHeuristic() + neighbourG);
                        neighbourNode.setDepth(current.getDepth() + 1);

                        open.add(neighbourNode);

                        if (neighbourNode.getDepth() > maxDepth) {
                            maxDepth = neighbourNode.getDepth();
                        }

                    } else {
                        //重新计算花费
                        if (neighbourNode.getG() > neighbourG) {

                            neighbourNode.setParent(current.getId());
                            neighbourNode.setG(neighbourG);
                            neighbourNode.setCost(neighbourNode.getHeuristic() + neighbourG);
                            neighbourNode.setDepth(current.getDepth() + 1);

                            if (neighbourNode.getDepth() > maxDepth) {
                                maxDepth = neighbourNode.getDepth();
                            }
                        }
                    }
                }
            }
        }
        if (findPath) {
            return getPathByEndNode(destinationNode);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * @return 由node path获取area path
     */
    public List<EntityID> getAreaPath(Area sourceArea, Area destinationArea, List<EntityID> path) {
        Node node;
        List<EntityID> areaPath = new ArrayList<>();
        List<EntityID> tempAreaPathList = new ArrayList<>();
        areaPath.add(sourceArea.getID());
        //遍历node的path
        for (int i = path.size() - 1; i >= 0; i--) {
            node = graph.getNode(path.get(i));
            for (EntityID areaId : node.getNeighbourAreaIds()) {
                //至少经历过路上的两个node才算作路过
                if (tempAreaPathList.contains(areaId)) {
                    if (!areaPath.contains(areaId)) {
                        areaPath.add(areaId);
                    }
                } else {
                    tempAreaPathList.add(areaId);
                }
            }
        }

        if (!areaPath.contains(destinationArea.getID())) {
            areaPath.add(destinationArea.getID());
        }

        if (!((Area) worldInfo.getEntity(destinationArea.getID())).getNeighbours().contains(sourceArea.getID())
                && areaPath.size() < 3) {
            return new ArrayList<>();
        }
        if (agentInfo.getTime() >= scenarioInfo.getKernelAgentsIgnoreuntil()) {
            areaPath = validatePath(areaPath);
        }
        return areaPath;
    }

    private List<EntityID> validatePath(List<EntityID> path) {
        Edge edge;
        Area area;
        for (int i = 0; i < path.size() - 1; i++) {
            area = (Area) worldInfo.getEntity(path.get(i));
            //获取要经过的edge
            edge = area.getEdgeTo(path.get(i + 1));
            if (edge == null) {
                System.out.println(agentInfo.getID() + " time: " + agentInfo.getTime() + " " + path.get(i) + " 到 " + path.get(i + 1) + " 路径错误!!!");
                System.out.println("原始路径: " + path);
                path = path.subList(0, i + 1);
                break;
            }
        }
        return path;
    }

    private Node getNearestNode(Area area, Pair<Integer, Integer> XYPair) {
        Node selected = null;
        int minDistance = Integer.MAX_VALUE;
        int distance;
        //获取所在位置所有edge
        List<Node> areaNodes = new ArrayList<>(graph.getAreaNodes(area.getID()));
        //获取最近的passable的node
        for (Node node : areaNodes) {
            if (node.isPassable()) {
                distance = Ruler.getDistance(XYPair.first(), XYPair.second(), node.getPosition().first(), node.getPosition().second());
                if (distance < minDistance) {
                    minDistance = distance;
                    selected = node;
                }
            }
        }
        //获取最近的node
        if (selected == null) {
            for (Node node : areaNodes) {
                distance = Ruler.getDistance(XYPair.first(), XYPair.second(), node.getPosition().first(), node.getPosition().second());
                if (distance < minDistance) {
                    minDistance = distance;
                    selected = node;
                }
            }
        }
        return selected;
    }

    private List<EntityID> getPathByEndNode(Node node) {
        List<EntityID> path = new ArrayList<>();
        Node current = node;
        path.add(current.getId());
        while (current.getParent() != null) {
            path.add(current.getParent());
            current = graph.getNode(current.getParent());
        }
        return path;
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
            return Integer.compare(d1, d2);
        }
    }

    /**
     * 返回要去的target
     */
    public EntityID getResultTarget() {
        return resultTarget;
    }
}
