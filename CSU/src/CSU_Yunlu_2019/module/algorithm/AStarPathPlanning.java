
package CSU_Yunlu_2019.module.algorithm;

import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.misc.collections.LazyMap;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.*;

import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class AStarPathPlanning  extends PathPlanning {

    private final boolean DEBUGLOG = false;
    private final boolean LESS = false;

    private Map<EntityID, Set<EntityID>> graph;

    private EntityID from;
    private Collection<EntityID> targets;
    private List<EntityID> result;

    private StuckDetector cDetector;
    
    private Random rander = new Random();

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

        cDetector = new StuckDetector(this.agentInfo);        
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
        
        // this.result=RandomWork();
        this.result=null;
        // Collection<EntityID> blockades = worldInfo.getEntityIDsOfType(BLOCKADE);
        // Collection<Integer> blockadeids = new HashSet<Integer>();
        // for (EntityID b : blockades) {
        //     blockadeids.add(b.getValue());
        // }
        // if (!blockades.isEmpty()) System.out.println(this.agentInfo.getID() + " Not Empty Blockades !!!");
        
        // Map<Double,LinkedList<EntityID>> thePathResult= new HashMap<>();
        // EntityID nearestTarget = null;
        // Double minDist = Double.MAX_VALUE;
        // for (EntityID tid : targets) {
        //     Double evalDist = (double)this.worldInfo.getDistance(from, tid);
        //     if (minDist > evalDist) {
        //         minDist = evalDist;
        //         nearestTarget = tid;
        //     }
        // }
        
        // if (nearestTarget == null) {
        //     debugLog("Targets list empty !!!");
        //     return this;
        // }
        debugLog(targets.size() + " targets.");

        cDetector.update(from);
        cDetector.warnStuck();

        Iterator<EntityID> it = targets.iterator();
        Double mincost=Double.MAX_VALUE;
        LinkedList<EntityID> minPath = null; // new LinkedList<>();
        int maxLength = 0;
        boolean bFound = false;
        while (it.hasNext()) {
            EntityID target = it.next();
            
            StandardEntity te = this.worldInfo.getEntity(target);
            StandardEntity me = this.worldInfo.getEntity(this.agentInfo.getID());
            debugLog(me.getURN());
            debugLog(te.getURN());

            // EntityID target = nearestTarget; //it.next();
            debugLog("Now is cal the target:"+target);//zheibain keyi jia shang xin dongxi
            // Node node=GetPathEndNode(target, blockadeids);
            Node node=GetPathEndNode(target);
            debugLog("Path node estimate " + node.estimate());
            boolean bReach = (node.getID().getValue()==target.getValue());
            if (LESS && !bReach) continue;
            if(node!=null && node.estimate()<mincost) {
                LinkedList<EntityID> path = new LinkedList<>();
                Node current = node;
                String route = "";
                do {
                    path.add(0, current.getID());
                    route += "<=" + current.getID();
                    current = current.getParentNode();
                    if (current == null) {
                        debugLog("Found a node with no ancestor! Something is broken.");
                        break;
                        // throw new RuntimeException("Found a node with no ancestor! Something is broken.");
                    }
                } while (current.getID() != this.from);
                if(bReach) {
                    if  (node.getCost()<mincost) {
                        mincost=node.getCost();
                        minPath=path;
                        this.result=minPath;
                        bFound=true;
                        debugLog("Path: " + route);
                    }
                } else {
                    double luckyNumber = (double)(rander.nextInt()%40+70)/100;
                    if (!bFound && path.size()>1 && node.getLength()+luckyNumber>maxLength) {
                        minPath=path;
                        this.result=minPath;
                        maxLength=node.getLength();
                        debugLog("Path: " + route);
                    }
                }
             //   thePathResult.add(path,(Double)super.getDistance());
            }
        }
        //排序，将distance进行，然后返回path
        this.result=minPath;//min cost
        debugLog("Done.");

        return this;
    }

    public Node GetPathEndNode(EntityID target){
        PriorityQueue<Node> open = new PriorityQueue<>();
        HashSet<EntityID> closed = new HashSet<>();
        Map<EntityID, Node> nodeMap = new HashMap<>();

        Node current = new Node(null, from, target);
        Node nearest = current;
        open.add(current);
        nodeMap.put(from, current);
        int cnt = 0;
        while (!open.isEmpty()) {
            current = open.poll();
            EntityID cid = current.getID();
            if (cid.getValue() == target.getValue()) {
                debugLog("Solved at iteration " + cnt);
                return current;
            }
            if (closed.contains(cid)) {
                continue;
            }
            closed.add(cid);
            if (current.getHeuristic() < nearest.getHeuristic())
                nearest = current;
			Collection<EntityID> neighbours = this.graph.get(cid);
            for (EntityID nid : neighbours) {
                if (!cDetector.isStucked() && agentInfo.me().getStandardURN() != POLICE_FORCE) {
                    Collection<Blockade>  blocks = worldInfo.getBlockades(nid);
                    if (!blocks.isEmpty()) {
//                        System.out.println(this.agentInfo.getID() + " " + nid + " BLOCKADE AVOIDED !!!!!!");
                        continue;
                    }
                }
                // if (blockadeids.contains(nid.getValue())) {
                //     System.out.println(this.agentInfo.getID() + " BLOCKADE AVOIDED !!!!!!");
                //     continue;
                // }
                // if (en instanceof Area && ((Area)en).isBlockadesDefined()) continue;
                // if (en instanceof Blockade) continue;

                if (closed.contains(nid)) {
                    continue;
                }
                Node neighbor = new Node(current, nid, target);
                if (nodeMap.containsKey(nid) && nodeMap.get(nid).estimate()<neighbor.estimate()) {
                    continue;
                }
                // tentative_gScore := gScore[current] + dist_between(current, neighbor)
                // if tentative_gScore >= gScore[neighbor]
                //     continue
                open.add(neighbor);
                nodeMap.put(nid, neighbor);
            }
            ++cnt;
            // debugLog("iteration " + cnt);
        }
        debugLog("path not found.");
        return nearest;
    }

    private class Node implements Comparable<Node> {
        EntityID id;
        EntityID target;

        Node parentNode;

        double cost;
        double heuristic;

        int length;

        public Node(Node parentNode, EntityID id, EntityID target) {
            this.id = id;
            this.target=target;
            this.parentNode=parentNode;

            if (this.parentNode == null) {
                if(this.getID() != from)
                    debugLog("this.parentNode==null && this.getID()!=from");
                this.cost = 0;
                this.length = 0;
            } else {
                this.cost = parentNode.getCost() + worldInfo.getDistance(parentNode.getID(), id);
                this.length = parentNode.length + 1;
            }
            this.heuristic = worldInfo.getDistance(id, target);
        }

        @Override
        public int compareTo(Node o) {
            double diff = (this.estimate()) - (o.estimate());
            if (diff > 0) return 1;
            if (diff < 0) return -1;
            return 0;
        }

        public EntityID getID() {
            return id;
        }

        public double getCost() {
            return cost;
        }

        public double getHeuristic() {
            return heuristic;
        }

        public double estimate() {
            return cost + heuristic;
        }

        public int getLength() {
            return length;
        }

        public EntityID getParent() {
            return this.parentNode.getID();
        }

        public void setParent(EntityID parent) { this.parentNode.id =parent; }
        public Node getParentNode(){return this.parentNode;}
    }
}

