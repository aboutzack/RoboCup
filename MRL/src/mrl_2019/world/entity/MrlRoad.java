package mrl_2019.world.entity;

import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import mrl_2019.MRLConstants;
import mrl_2019.util.MrlRay;
import mrl_2019.util.Util;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.routing.graph.GraphModule;
import mrl_2019.world.routing.graph.MyEdge;
import mrl_2019.world.routing.graph.Node;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by: Mahdi Taherian
 * User: mrl_2019
 * Date: 5/17/12
 * Time: 8:25 PM
 */
public class MrlRoad {

    private Map<EntityID, List<Polygon>> buildingVisitableParts;
    private Map<MrlEdge, HashSet<MrlEdge>> reachableEdges;
    private Set<MrlBlockade> veryImportantBlockades;
    private Set<MrlBlockade> importantBlockades;
    private List<MrlBlockade> mrlBlockades;
    private List<MrlEdge> passableMrlEdges;
    private Set<MrlEdge> blockedEdges;
    private Polygon transformedPolygon;
    private HashSet<MrlEdge> openEdges;
    private List<Point2D> apexPoints;
    private List<MrlRoad> childRoads;
    private List<MrlEdge> mrlEdges;
    private Set<MrlBlockade> farNeighbourBlockades;
    private boolean isReachable;
    private int totalRepairCost;
    private boolean isPassable;
    private int lastSeenTime;
    private int lastUpdateTime;
    private int lastResetTime;
    private List<Edge> edges;
    private List<Path> paths;

    private Polygon polygon;
    private int groundArea;
    private boolean highway;
    private boolean freeway;

    private boolean isSeen;
    private int repairTime;
    private Road parent;
    private Set<EntityID> visibleFrom;
    private Set<EntityID> observableAreas;
    //    private RoadHelper roadHelper;
    private List<MrlBuilding> buildingsInExtinguishRange;


    private WorldInfo worldInfo;
    private AgentInfo agentInfo;
    private ScenarioInfo scenarioInfo;
    MrlWorldHelper worldHelper;
    private List<MrlRay> lineOfSight;


    public MrlRoad(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, Road parent, MrlWorldHelper worldHelper) {
        this.worldInfo = worldInfo;
        this.agentInfo = agentInfo;
        this.scenarioInfo = scenarioInfo;

        this.worldHelper = worldHelper;

//        this.roadHelper = roadHelper;
        this.parent = parent;
        initialize(parent, createMrlEdges(parent.getEdges()));
    }


//    public MrlRoad(Road road, MrlWorld world, List<MrlEdge> mrlEdges) {
//        initialize(road, world, mrlEdges);
//    }
//
//    public MrlRoad(Road road, MrlWorld world) {
//        this.parent = road;
//        initialize(road, world, createMrlEdges(road.getEdges()));
//    }

    private void initialize(Road road, List<MrlEdge> mrlEdges) {
        setVisibleFrom(new HashSet<>());
        setObservableAreas(new HashSet<>());
        this.highway = false;
        this.freeway = false;
        this.parent = road;


        this.edges = new ArrayList<>(road.getEdges());
        this.apexPoints = new ArrayList<>();
        this.mrlBlockades = new ArrayList<>();
        this.childRoads = new ArrayList<>();
        this.blockedEdges = new HashSet<>();
        this.reachableEdges = new HashMap<>();
        paths = new ArrayList<>();
        this.isPassable = true;
        this.isReachable = true;
        this.lastSeenTime = 0;
        this.totalRepairCost = 0;
        this.repairTime = 0;
        lastResetTime = 0;
        farNeighbourBlockades = new HashSet<MrlBlockade>();
        lastUpdateTime = 0;
        this.importantBlockades = new HashSet<MrlBlockade>();
        this.veryImportantBlockades = new HashSet<MrlBlockade>();
        passableMrlEdges = new ArrayList<MrlEdge>();
        this.buildingVisitableParts = new HashMap<EntityID, List<Polygon>>();
        for (MrlEdge mrlEdge : mrlEdges) {
            if (mrlEdge.isPassable()) {
                passableMrlEdges.add(mrlEdge);
            }
        }

        for (Path p : worldHelper.getPaths()) {
            if (p.contains(road)) {
                paths.add(p);
            }
        }
        setSeen(false);

        setMrlEdges(mrlEdges);
        this.openEdges = new HashSet<MrlEdge>(mrlEdges);
        resetReachableEdges();

//        MrlPersonalData.VIEWER_DATA.setMrlRoadMap(world.getPlatoonAgent(), road.getID(), this);
    }

    public void update() {
        reset();
        setMrlBlockades();
//        setBlockadesFromViewer();
        mrlEdges.forEach(mrlEdge -> {
            if (mrlEdge.isPassable()) {
//                if (mrlEdge.isTooSmall()) {
//                    mrlEdge.setBlocked(true);
//                }
                mrlEdge.setOpenPart(mrlEdge.getLine());
                List<MrlBlockade> blockedStart = new ArrayList<MrlBlockade>();
                List<MrlBlockade> blockedEnd = new ArrayList<MrlBlockade>();
                for (MrlBlockade mrlBlockade : mrlBlockades) {

                    if (Util.distance(mrlBlockade.getPolygon(), mrlEdge.getStart()) < MRLConstants.AGENT_PASSING_THRESHOLD) {
                        blockedStart.add(mrlBlockade);
                    }
                    if (Util.distance(mrlBlockade.getPolygon(), mrlEdge.getEnd()) < MRLConstants.AGENT_PASSING_THRESHOLD) {
                        blockedEnd.add(mrlBlockade);
                    }
//                    setMrlEdgeOpenPart(mrlEdge, mrlBlockade);
                }
                setMrlEdgeOpenPart(mrlEdge);
                if (mrlBlockades.size() == 1) {
                    if (Util.containsEach(blockedEnd, blockedStart)) {
                        mrlBlockades.get(0).addBlockedEdges(mrlEdge);
                        mrlEdge.setBlocked(true);
                        mrlEdge.setAbsolutelyBlocked(true);
                    }
                } else {
                    for (MrlBlockade block1 : blockedStart) {
                        for (MrlBlockade block2 : blockedEnd) {
//                            double distance = Util.distance(block1.getPolygon(), block2.getPolygon());

                            if (Util.isPassable(block1.getPolygon(), block2.getPolygon(), MRLConstants.AGENT_PASSING_THRESHOLD)) {
                                mrlEdge.setBlocked(true);
                                block1.addBlockedEdges(mrlEdge);
                                block2.addBlockedEdges(mrlEdge);
                            }

                        }
                    }
                }
                if (mrlEdge.isBlocked()) {
                    blockedEdges.add(mrlEdge);
                }
                isPassable = getReachableEdges(mrlEdge) != null && !getReachableEdges(mrlEdge).isEmpty();
            } else {
                for (MrlBlockade mrlBlockade : mrlBlockades) {
                    double distance = Util.distance(mrlEdge.getLine(), mrlBlockade.getPolygon());

                    if (distance < MRLConstants.AGENT_PASSING_THRESHOLD) {
                        mrlEdge.setBlocked(true);
                        mrlBlockade.addBlockedEdges(mrlEdge);
                    }

                }
            }
//            boolean isOtherSideBlocked = mrlEdge.isOtherSideBlocked(world);
//            if (isOtherSideBlocked) {
//                mrlEdge.setBlocked(true);
//            }
        });

        //check too small edge passably
        checkTooSmallEdgesPassably();
        if (agentInfo.me() instanceof Human) {
            for (MrlEdge mrlEdge : passableMrlEdges) {
                //for edges that not blocked each side separately but each side blocked other one.
                if (!mrlEdge.isBlocked() && !mrlEdge.isTooSmall()) {
                    if (Util.lineLength(mrlEdge.getOpenPart()) < (MRLConstants.AGENT_PASSING_THRESHOLD)) {
                        blockedEdges.add(mrlEdge);
                        mrlEdge.setBlocked(true);
                    }
                }
            }
        }

        updateRepairCost();
        openEdges.removeAll(blockedEdges);
        if (agentInfo.me() instanceof PoliceForce) {
//            updateBlockadesValue();
        } else {
            updateNodesPassably();
        }
        lastUpdateTime = agentInfo.getTime();
    }

    private void checkTooSmallEdgesPassably() {

        for (MrlEdge mrlEdge : passableMrlEdges) {
            if (mrlEdge.isTooSmall()) {
                Set<EntityID> neighbours = new HashSet<EntityID>(parent.getNeighbours());
                neighbours.addAll(((Area) worldInfo.getEntity(mrlEdge.getNeighbours().second())).getNeighbours());
                MrlRoad mrlRoad;
                FOR1:
                for (EntityID neighbourID : neighbours) {
                    mrlRoad = getMrlRoad(neighbourID);
                    if (mrlRoad != null) {
                        for (MrlBlockade mrlBlockade : mrlRoad.getMrlBlockades()) {
                            if (Util.distance(mrlBlockade.getPolygon(), mrlEdge.getMiddle()) < MRLConstants.AGENT_PASSING_THRESHOLD) {
                                blockedEdges.add(mrlEdge);
                                mrlEdge.setBlocked(true);
                                break FOR1;
                            }
                        }
                    }
                }
            }
        }
    }

    private MrlRoad getMrlRoad(EntityID id) {
        return worldHelper.getMrlRoad(id);
    }

    private Set<MrlEdge> getConnectedEdges(MrlEdge mrlEdge) {
        MrlRoad ownerRoad = getMrlRoad(mrlEdge.getNeighbours().first());
        MrlRoad neighbourRoad = getMrlRoad(mrlEdge.getNeighbours().second());
        Set<MrlEdge> connectedEdges = new HashSet<MrlEdge>();
        for (MrlEdge edge : ownerRoad.getMrlEdges()) {
            if (mrlEdge.getStart().equals(edge.getStart()) ||
                    mrlEdge.getStart().equals(edge.getEnd()) ||
                    mrlEdge.getEnd().equals(edge.getStart()) ||
                    mrlEdge.getEnd().equals(edge.getEnd())) {
                connectedEdges.add(edge);
            }
        }

        if (neighbourRoad != null) {//if neighbour instance of building...
            for (MrlEdge edge : neighbourRoad.getMrlEdges()) {
                if (mrlEdge.getStart().equals(edge.getStart()) ||
                        mrlEdge.getStart().equals(edge.getEnd()) ||
                        mrlEdge.getEnd().equals(edge.getStart()) ||
                        mrlEdge.getEnd().equals(edge.getEnd())) {
                    connectedEdges.add(edge);
                }
            }
        }

        //todo add other neighbours connected edges..........
        return connectedEdges;
    }


    private void updateNodesPassably() {
        if (!(agentInfo.me() instanceof Human)) {
            return;
        }
        GraphModule graph = getGraph();
        for (MrlEdge mrlEdge : passableMrlEdges) {
            Node node = graph.getNode(mrlEdge.getMiddle());
            if (node == null) {
                continue;
            }

            if (mrlEdge.isBlocked() || mrlEdge.isOtherSideBlocked(worldInfo)) {
                node.setPassable(false, agentInfo.getTime());
            } else {
                node.setPassable(true, agentInfo.getTime());
            }
        }
    }

    private GraphModule getGraph() {
        return worldHelper.getGraph();
    }

    public void addBuildingVisitableParts(EntityID buildingID, Polygon visitablePartsPolygon) {
        if (!buildingVisitableParts.containsKey(buildingID)) {
            buildingVisitableParts.put(buildingID, new ArrayList<Polygon>());
        }
        buildingVisitableParts.get(buildingID).add(visitablePartsPolygon);
    }

    private void setMrlEdgeOpenPart(MrlEdge mrlEdge) {
        Point2D p1 = null, p2 = null;
        int d1 = 0, d2 = 0;
        for (MrlBlockade mrlBlockade : mrlBlockades) {
            List<Point2D> pointList = Util.getPoint2DList(mrlBlockade.getPolygon().xpoints, mrlBlockade.getPolygon().ypoints);
            List<Point2D> centerPoints = new ArrayList<Point2D>();
            boolean isBlockedStart = false, isBlockedEnd = false;
            for (Point2D point : pointList) {
                if (Util.contains(mrlEdge.getLine(), point, 100)) {
                    if (Util.distance(point, mrlEdge.getLine().getOrigin()) <= 10/*point.equals(mrlEdge.getLine().getOrigin())*/) {
                        isBlockedStart = true;
                    } else if (Util.distance(point, mrlEdge.getLine().getEndPoint()) <= 10/*point.equals(mrlEdge.getLine().getEndPoint())*/) {
                        isBlockedEnd = true;
                    } else {
                        centerPoints.add(point);
                    }
                }
            }

            for (Point2D centerPoint : centerPoints) {
                if (isBlockedEnd && isBlockedStart) {
                    p1 = mrlEdge.getMiddle();
                    p2 = mrlEdge.getMiddle();
                    break;
                } else if (isBlockedEnd) {
                    int dist = Util.distance(centerPoint, mrlEdge.getLine().getEndPoint());
                    if (dist > d2) {
                        p2 = centerPoint;
                        d2 = dist;
                    }
                } else if (isBlockedStart) {
                    int dist = Util.distance(centerPoint, mrlEdge.getLine().getOrigin());
                    if (dist > d1) {
                        p1 = centerPoint;
                        d1 = dist;
                    }
                }
            }
        }
        if (p1 == null) {
            p1 = mrlEdge.getStart();
        }
        if (p2 == null) {
            p2 = mrlEdge.getEnd();
        }
        MrlEdge otherSide = mrlEdge.getOtherSideEdge(worldInfo);
        Line2D openPart = new Line2D(p1, p2);
        if (otherSide != null) {
            MrlRoad neighbour = getMrlRoad(otherSide.getNeighbours().first());
            if (neighbour.getLastUpdateTime() >= this.lastUpdateTime) {
                Line2D otherSideOpenPart = otherSide.getOpenPart();
                if (Util.lineLength(openPart) < Util.lineLength(otherSideOpenPart)) {
                    mrlEdge.setOpenPart(openPart);
                    otherSide.setOpenPart(openPart);
                } else {
                    mrlEdge.setOpenPart(otherSideOpenPart);
                    otherSide.setOpenPart(otherSideOpenPart);
                }
            } else {
                mrlEdge.setOpenPart(openPart);
            }
        } else {
            mrlEdge.setOpenPart(openPart);
        }
    }

    public List<Path> getPaths() {
        return paths;
    }

    public HashSet<MrlEdge> getReachableEdges(MrlEdge from) {
        return reachableEdges.get(from);
    }

    public void addReachableEdges(MrlEdge from, MrlEdge to) {
        reachableEdges.get(from).add(to);
        reachableEdges.get(to).add(from);
    }

    public void removeReachableEdges(MrlEdge from, MrlEdge to) {
        reachableEdges.get(from).remove(to);
        reachableEdges.get(to).remove(from);
    }

    private void setApexPoint() {
        apexPoints.clear();
        for (MrlEdge mrlEdge : mrlEdges) {
            if (mrlEdge == null) {
                System.out.println("(MrlRoad.class ==> mrlEdge == null)");
                continue;
            }
            if (!apexPoints.contains(mrlEdge.getStart()))
                apexPoints.add(mrlEdge.getStart());
            else if (!apexPoints.contains(mrlEdge.getEnd()))
                apexPoints.add(mrlEdge.getEnd());
        }
        createPolygon();
        computeGroundArea();
    }

    public List<MrlEdge> getMrlEdgesTo(EntityID neighbourID) {
        List<MrlEdge> mrlEdgeList = new ArrayList<MrlEdge>();
        for (MrlEdge mrlEdge : mrlEdges) {
            if (mrlEdge.isPassable() && mrlEdge.getNeighbours().second().equals(neighbourID)) {
                mrlEdgeList.add(mrlEdge);
            }
        }
        return mrlEdgeList;
    }

    private void createPolygon() {
        int count = apexPoints.size();
        int xs[] = new int[count];
        int ys[] = new int[count];
        for (int i = 0; i < count; i++) {
            xs[i] = (int) apexPoints.get(i).getX();
            ys[i] = (int) apexPoints.get(i).getY();
        }
        polygon = new Polygon(xs, ys, count);
    }

    private void computeGroundArea() {
        double area = GeometryTools2D.computeArea(apexPoints) * MRLConstants.SQ_MM_TO_SQ_M;
        groundArea = (int) Math.abs(area);
    }

    private void resetReachableEdges() {
        HashSet<MrlEdge> edgesInstead;
        for (MrlEdge mrlEdge : mrlEdges) {
            if (mrlEdge.isPassable()) {
                edgesInstead = new HashSet<MrlEdge>(passableMrlEdges);
                edgesInstead.remove(mrlEdge);
                reachableEdges.put(mrlEdge, edgesInstead);
            }
        }
    }

    /**
     * yek point migirim va beine edge haye roademoon iterate mikonim ta bebinim kodoom MrlEdge in noghte ro dare
     * age hich kodoom in noghte ro nadashtand null bar migardoonim
     *
     * @param point point that we want found edge on it
     * @return MrlEdge which point on it.
     */
    public MrlEdge getEdgeInPoint(Point2D point) {
        for (MrlEdge mrlEdge : mrlEdges) {
            if (Util.contains(mrlEdge.getLine(), point, 1.0)) {
                return mrlEdge;
            }
        }
        return null;
    }

    public MrlEdge getMrlEdge(Edge edge) {
        Point2D middle = Util.getPoint(getEdgeMiddle(edge));
        return getEdgeInPoint(middle);
    }


    private void setMrlEdges(List<MrlEdge> edges) {
        mrlEdges = new ArrayList<MrlEdge>();
        mrlEdges.addAll(edges);
        setApexPoint();
    }

    private List<MrlEdge> createMrlEdges(List<Edge> edges) {
        List<MrlEdge> mrlEdges = new ArrayList<MrlEdge>();
        for (Edge edge : edges) {
            mrlEdges.add(new MrlEdge(worldHelper, edge, parent.getID()));
        }
        return mrlEdges;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public int getGroundArea() {
        return groundArea;
    }

    private void addBlockade(MrlBlockade blockade) {
        mrlBlockades.add(blockade);
    }


    private void setMrlBlockades() {
        totalRepairCost = 0;
        mrlBlockades.clear();
        if (!parent.isBlockadesDefined()) {
            return;
        }
        try {
            parent.getBlockades().forEach(blockID -> {
                StandardEntity entity = worldInfo.getEntity(blockID);
                if (entity instanceof Blockade) {
                    Blockade blockade = (Blockade) entity;
                    Polygon blockPolygon = Util.retainPolygon(getPolygon(), Util.getPolygon(blockade.getApexes()));
                    MrlBlockade newBlockade = new MrlBlockade(this, blockade, blockPolygon);
                    addBlockade(newBlockade);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateRepairCost() {
        totalRepairCost = 0;
        for (MrlBlockade mrlBlockade : mrlBlockades) {
            totalRepairCost += mrlBlockade.getRepairCost();
        }

        int repairRate = scenarioInfo.getRawConfig().getIntValue("clear.repair.rate");
        repairTime = (int) Math.ceil(totalRepairCost / repairRate);
    }

    private void updateBlockadesValue() {
        if (this.getMrlBlockades().size() == 0) {
            return;
        }
        importantBlockades.clear();
        for (int e1 = 0; e1 < passableMrlEdges.size() - 1; e1++) {
            MrlEdge from = getMrlEdges().get(e1);
            for (int e2 = e1; e2 < passableMrlEdges.size(); e2++) {
                MrlEdge to = getMrlEdges().get(e2);
                updateBlockadesValue(from, to);
            }
        }

    }

    private void updateBlockadesValue(MrlEdge from, MrlEdge to) {
        Pair<List<MrlEdge>, List<MrlEdge>> edgesBetween = getEdgesBetween(this, from, to, false);
        for (MrlBlockade blockade : this.getMrlBlockades()) {
            blockade.setValue(BlockadeValue.WORTHLESS);
            if (blockade.getBlockedEdges().contains(from) || blockade.getBlockedEdges().contains(to)) {
                blockade.setValue(BlockadeValue.VERY_IMPORTANT);
                importantBlockades.add(blockade);
                veryImportantBlockades.add(blockade);
                continue;
            }

            if (Util.containsEach(blockade.getBlockedEdges(), edgesBetween.first()) &&
                    Util.containsEach(blockade.getBlockedEdges(), edgesBetween.second())) {
                blockade.setValue(BlockadeValue.VERY_IMPORTANT);
                veryImportantBlockades.add(blockade);
                importantBlockades.add(blockade);
            }
        }

        for (int i = 0; i < this.getMrlBlockades().size() - 1; i++) {
            List<MrlEdge> blockedEdges = new ArrayList<MrlEdge>();
            MrlBlockade blockade1 = this.getMrlBlockades().get(i);
            if (blockade1.getValue().equals(BlockadeValue.VERY_IMPORTANT))
                continue;
            blockedEdges.addAll(blockade1.getBlockedEdges());
            for (int j = i + 1; j < this.getMrlBlockades().size(); j++) {
                MrlBlockade blockade2 = this.getMrlBlockades().get(j);
                if (blockade2.getValue().equals(BlockadeValue.VERY_IMPORTANT))
                    continue;
                blockedEdges.addAll(blockade2.getBlockedEdges());
//                if (Util.distance(blockade1.getPolygon(), blockade2.getPolygon()) < MRLConstants.AGENT_PASSING_THRESHOLD) {
                if (Util.isPassable(blockade1.getPolygon(), blockade2.getPolygon(), MRLConstants.AGENT_PASSING_THRESHOLD)) {
                    if (Util.containsEach(blockedEdges, edgesBetween.first()) &&
                            Util.containsEach(blockedEdges, edgesBetween.second())) {
                        if (blockade1.getRepairCost() > blockade2.getRepairCost()) {
                            importantBlockades.add(blockade2);
                            blockade1.setValue(BlockadeValue.IMPORTANT_WITH_HIGH_REPAIR_COST);
                            blockade2.setValue(BlockadeValue.IMPORTANT_WITH_LOW_REPAIR_COST);
                        } else {
                            importantBlockades.add(blockade1);
                            blockade1.setValue(BlockadeValue.IMPORTANT_WITH_LOW_REPAIR_COST);
                            blockade2.setValue(BlockadeValue.IMPORTANT_WITH_HIGH_REPAIR_COST);
                        }
                    }
                }
            }
        }

        rescuecore2.misc.geometry.Line2D myEdgeLine = new rescuecore2.misc.geometry.Line2D(from.getMiddle(), to.getMiddle());
        for (MrlBlockade blockade : this.getMrlBlockades()) {
            if (blockade.getValue().equals(BlockadeValue.WORTHLESS)) {
                if (Util.intersections(blockade.getPolygon(), myEdgeLine).size() > 0) {
                    blockade.setValue(BlockadeValue.ORNERY);
                }
            }
        }
    }

    public Set<MrlBlockade> getObstacles(MrlEdge from, MrlEdge to) {
        Set<MrlBlockade> obstacles = new HashSet<MrlBlockade>();
        Pair<List<MrlEdge>, List<MrlEdge>> edgesBetween = getEdgesBetween(this, from, to, false);
        for (MrlBlockade blockade : this.getMrlBlockades()) {
            if (blockade.getBlockedEdges().contains(from) || blockade.getBlockedEdges().contains(to)) {
                obstacles.add(blockade);
                continue;
            }

            if (Util.containsEach(blockade.getBlockedEdges(), edgesBetween.first()) &&
                    Util.containsEach(blockade.getBlockedEdges(), edgesBetween.second())) {
                obstacles.add(blockade);
            }
        }

        for (int i = 0; i < this.getMrlBlockades().size() - 1; i++) {
            List<MrlEdge> blockedEdges = new ArrayList<MrlEdge>();
            MrlBlockade blockade1 = this.getMrlBlockades().get(i);
            blockedEdges.addAll(blockade1.getBlockedEdges());
            for (int j = i + 1; j < this.getMrlBlockades().size(); j++) {
                MrlBlockade blockade2 = this.getMrlBlockades().get(j);
                blockedEdges.addAll(blockade2.getBlockedEdges());
//                if (Util.distance(blockade1.getPolygon(), blockade2.getPolygon()) < MRLConstants.AGENT_PASSING_THRESHOLD) {

                if (Util.isPassable(blockade1.getPolygon(), blockade2.getPolygon(), MRLConstants.AGENT_PASSING_THRESHOLD)) {
                    if (Util.containsEach(blockedEdges, edgesBetween.first()) &&
                            Util.containsEach(blockedEdges, edgesBetween.second())) {
                        if (blockade1.getRepairCost() > blockade2.getRepairCost()) {
                            obstacles.add(blockade2);
                        } else {
                            obstacles.add(blockade1);
                        }
                    }
                }
            }
        }
        return obstacles;
    }

    /**
     * agar 1 road 1 modat zamane khassi 2bare dide nashod ya payami dar ertebat ba un naresid reset mishe
     * be in soorat ke tamame yalhaye dakhele un + node haye un passable mishand va
     * edge haye un azx halate block kharej mishand
     * <p/>
     * zamane reset shodan bayad az meghdare repairCost/repair_rate (meghdar zamini ke bara pak kardane road lazeme) +
     * yek meghdare threshold baraye etminan be dast miad
     */
    public void resetOldPassably() {
        if (!isSeen() || agentInfo.me() instanceof PoliceForce || lastResetTime > lastUpdateTime) {
            return;
        }
        if (isTimeToReset()) {
            reset();
        }
    }

    private boolean isTimeToReset() {
        int resetTime = getRepairTime();
//        if (world.isMapHuge()) {
//            resetTime += MRLConstants.ROAD_PASSABLY_RESET_TIME_IN_HUGE_MAP;
//        } else if (world.isMapMedium()) {
        resetTime += MRLConstants.ROAD_PASSABLY_RESET_TIME_IN_MEDIUM_MAP;
//        } else if (world.isMapSmall()) {
//            resetTime += MRLConstants.ROAD_PASSABLY_RESET_TIME_IN_SMALL_MAP;
//
//        }
        return lastResetTime <= lastUpdateTime + resetTime && agentInfo.getTime() - lastSeenTime > resetTime;
    }

    public void reset() {
        blockedEdges.clear();
        openEdges.addAll(mrlEdges);
        mrlBlockades.clear();
        isPassable = true;
        isReachable = true;
        farNeighbourBlockades.clear();
        if (!(agentInfo.me() instanceof Human)) {
            return;
        }
        GraphModule graph = getGraph();
        for (MrlEdge mrlEdge : mrlEdges) {
            mrlEdge.setBlocked(false);
            mrlEdge.setAbsolutelyBlocked(false);
            MrlEdge otherEdge = mrlEdge.getOtherSideEdge(worldInfo);
            mrlEdge.setOpenPart(mrlEdge.getLine());
            if (otherEdge != null) {
                MrlRoad mrlRoad = getMrlRoad(mrlEdge.getNeighbours().second());
                if (mrlRoad.getLastUpdateTime() < lastUpdateTime) {
                    //mrlRoad.update();
                    otherEdge.setOpenPart(otherEdge.getLine());
                }
            }
            Area neighbour = (Area) worldInfo.getEntity(mrlEdge.getNeighbours().second());
            if (mrlEdge.isPassable()) {
                Node node = graph.getNode((mrlEdge.getMiddle()));
                if (node == null) {
                    System.out.println("node == null in " + this.getID());
                    continue;
                }
                if (neighbour instanceof Road) {
                    MrlRoad mrlRoad = getMrlRoad(neighbour.getID());
                    MrlEdge neighbourEdge = mrlRoad.getEdgeInPoint(mrlEdge.getMiddle());
                    if (neighbourEdge != null && !neighbourEdge.isBlocked()) {
                        node.setPassable(true, agentInfo.getTime());
                    }
                } else {
                    node.setPassable(true, agentInfo.getTime());
                }
            }
        }
        resetReachableEdges();
        for (MyEdge myEdge : graph.getMyEdgesInArea(getID())) {
            myEdge.setPassable(true);
        }
        lastResetTime = agentInfo.getTime();
    }

    public List<MrlBlockade> getMrlBlockades() {
        return mrlBlockades;
    }

    public boolean isNeedUpdate() {
        if (!(agentInfo.me() instanceof Human)) {
            return false;
        }
        if (!parent.isBlockadesDefined()) {
            return true;
        }
        if (agentInfo.me() instanceof PoliceForce ||
                (parent.getBlockades().size() != getMrlBlockades().size()) ||
                lastSeenTime == 0 ||
                lastSeenTime < agentInfo.getTime() - 10) {
            return true;
        }
        Blockade blockade;
        for (MrlBlockade mrlBlockade : getMrlBlockades()) {
            blockade = mrlBlockade.getParent();
            if (blockade == null || !parent.getBlockades().contains(mrlBlockade.getParent().getID()) || blockade.getRepairCost() != mrlBlockade.getRepairCost()) {
                return true;
            }
        }
        return false;
    }

    public boolean isHighway() {
        return highway;
    }

    public void setHighway(boolean highway) {
        this.highway = highway;
    }

    /**
     * free way is a kind of road which had a very long passable edge (more than 95% )
     *
     * @return if this road is a freeway return true , otherwise return false
     */
    public boolean isFreeway() {
        return freeway;
    }

    public void setFreeway(boolean isFreeway) {
        freeway = isFreeway;
    }

    public List<MrlEdge> getMrlEdges() {
        return mrlEdges;
    }

    public void addNeighboursBlockades() {
        MrlRoad neighbour;
        for (EntityID nID : parent.getNeighbours()) {
            neighbour = getMrlRoad(nID);
            if (neighbour != null) {
                for (MrlBlockade mrlBlockade : neighbour.getMrlBlockades()) {
                    if (!farNeighbourBlockades.contains(mrlBlockade)) {
                        if (Util.isPassable(mrlBlockade.getPolygon(), this.getPolygon(), MRLConstants.AGENT_SIZE)) {
                            addBlockade(mrlBlockade);
                        } else {
                            farNeighbourBlockades.add(mrlBlockade);
                        }
                    }
                }
            }
        }
    }

    public Road getParent() {
        return parent;
    }

    public int getLastUpdateTime() {
        return lastUpdateTime;
    }

    public int getLastResetTime() {
        return lastResetTime;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public List<MrlRoad> getChildRoads() {
        return childRoads;
    }

    public boolean isPassable() {
        return isPassable;
    }

    public Set<MrlEdge> getBlockedEdges() {
        return blockedEdges;
    }

    public HashSet<MrlEdge> getOpenEdges() {
        return openEdges;
    }

    public EntityID getID() {
        return parent.getID();
    }

    public boolean isReachable() {
        return isReachable;
    }

    public void setReachable(boolean reachable) {
        isReachable = reachable;
    }

    public void setSeen(boolean seen) {
        this.isSeen = seen;
    }

    public boolean isSeen() {
        return isSeen;
    }

    public void setLastSeenTime(int lastSeenTime) {
        this.lastSeenTime = lastSeenTime;
    }

    public int getLastSeenTime() {
        return lastSeenTime;
    }

    public int getRepairTime() {
        return repairTime;
    }

    public Set<MrlBlockade> getImportantBlockades() {
        return importantBlockades;
    }

    public Set<MrlBlockade> getVeryImportantBlockades() {
        return veryImportantBlockades;
    }

    /**
     * @return transformed polygon just for viewer
     */
    public Polygon getTransformedPolygon() {
        return transformedPolygon;
    }

    /**
     * transform polygon for viewer
     *
     * @param t viewer screen transform
     */
    public void createTransformedPolygon(ScreenTransform t) {

        int count = apexPoints.size();
        int xs[] = new int[count];
        int ys[] = new int[count];
        int i = 0;
        for (Point2D point2D : apexPoints) {
            xs[i] = t.xToScreen(point2D.getX());
            ys[i] = t.yToScreen(point2D.getY());
            i++;
        }
        transformedPolygon = new Polygon(xs, ys, count);
    }

    public Set<EntityID> getVisibleFrom() {
        return visibleFrom;
    }

    public void setVisibleFrom(Set<EntityID> visibleFrom) {
        this.visibleFrom = visibleFrom;
    }

    public Set<EntityID> getObservableAreas() {
        return observableAreas;
    }

    public void setObservableAreas(Set<EntityID> observableAreas) {
        this.observableAreas = observableAreas;
    }


    public List<MrlBuilding> getBuildingsInExtinguishRange() {
        return buildingsInExtinguishRange;
    }

    public void setBuildingsInExtinguishRange(List<MrlBuilding> buildingsInExtinguishRange) {
        this.buildingsInExtinguishRange = buildingsInExtinguishRange;
    }

    public List<MrlRay> getLineOfSight() {
        return lineOfSight;
    }

    public void setLineOfSight(List<MrlRay> lineOfSight) {
        this.lineOfSight = lineOfSight;
    }

    public static Pair<Integer, Integer> getEdgeMiddle(Edge edge) {
        int x = (int) ((edge.getStartX() + edge.getEndX()) / 2.0);
        int y = (int) ((edge.getStartY() + edge.getEndY()) / 2.0);
        return new Pair<Integer, Integer>(x, y);
    }

    /**
     * 1road va 2ta edge ke dakhele un hastand ro migire va
     * tamame edgehaei ke bein un 2ta edge va dakhele un road hastand ro be shekle 1 Pair bar migardoone.
     *
     * @param road           roade morede barresi
     * @param edge1          edge avali
     * @param edge2          edge 2vomi
     * @param justImPassable in parameter bara ine ke faghat edgehaye impassable ezafe she ya na.
     * @return 1pair az edge haye ye samte un 2edge va edge haye samte digeshoon
     */
    public Pair<List<MrlEdge>, List<MrlEdge>> getEdgesBetween(MrlRoad road, MrlEdge edge1, MrlEdge edge2, boolean justImPassable) {
        List<MrlEdge> leftSideEdges = new ArrayList<MrlEdge>();
        List<MrlEdge> rightSideEdges = new ArrayList<MrlEdge>();
        rescuecore2.misc.geometry.Point2D startPoint1 = edge1.getStart();
        rescuecore2.misc.geometry.Point2D endPoint1 = edge1.getEnd();
        rescuecore2.misc.geometry.Point2D startPoint2 = edge2.getStart();
        rescuecore2.misc.geometry.Point2D endPoint2 = edge2.getEnd();

        boolean finishedLeft = false;
        boolean finishedRight = false;
        for (MrlEdge edge : road.getMrlEdges()) {
            if (finishedLeft && finishedRight)
                break;
            for (MrlEdge ed : road.getMrlEdges()) {
                if (finishedLeft && finishedRight)
                    break;
                if (ed.equals(edge1) || ed.equals(edge2)) {
                    continue;
                }
                if (startPoint1.equals(startPoint2) || startPoint1.equals(endPoint2)) {
                    finishedLeft = true;
                }
                if (endPoint1.equals(startPoint2) || endPoint1.equals(endPoint2)) {
                    finishedRight = true;
                }

                if (ed.getStart().equals(startPoint1) && !finishedLeft && !leftSideEdges.contains(ed)) {
                    startPoint1 = ed.getEnd();
                    if (!justImPassable || !ed.isPassable())
                        leftSideEdges.add(ed);
                    continue;
                }
                if (ed.getEnd().equals(startPoint1) && !finishedLeft && !leftSideEdges.contains(ed)) {
                    startPoint1 = ed.getStart();
                    if (!justImPassable || !ed.isPassable())
                        leftSideEdges.add(ed);
                    continue;
                }
                if (ed.getStart().equals(endPoint1) && !finishedRight && !rightSideEdges.contains(ed)) {
                    endPoint1 = ed.getEnd();
                    if (!justImPassable || !ed.isPassable())
                        rightSideEdges.add(ed);
                    continue;
                }
                if (ed.getEnd().equals(endPoint1) && !finishedRight && !rightSideEdges.contains(ed)) {
                    endPoint1 = ed.getStart();
                    if (!justImPassable || !ed.isPassable())
                        rightSideEdges.add(ed);
                    continue;
                }
            }
        }
        return new Pair<List<MrlEdge>, List<MrlEdge>>(leftSideEdges, rightSideEdges);
    }
}
