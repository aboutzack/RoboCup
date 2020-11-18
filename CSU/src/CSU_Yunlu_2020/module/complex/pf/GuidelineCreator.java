package CSU_Yunlu_2020.module.complex.pf;

import CSU_Yunlu_2020.CSUConstants;
import CSU_Yunlu_2020.debugger.DebugHelper;
import CSU_Yunlu_2020.extaction.pf.guidelineHelper;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.AbstractModule;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class GuidelineCreator extends AbstractModule {


    private PathPlanning pathPlanning;
    private Clustering clustering;

    private List<guidelineHelper> judgeRoad = new ArrayList<>();
    private List<EntityID> countedRoad = new ArrayList<>();
    private List<EntityID> countedEntrance = new ArrayList<>();
    private int roadsize;

    public static final String KEY_JUDGE_ROAD = "guidelineCreator.judge_road";
    public static final String KEY_START_X = "guidelineCreator.start_x";
    public static final String KEY_START_Y = "guidelineCreator.start_y";
    public static final String KEY_END_X = "guidelineCreator.end_x";
    public static final String KEY_END_Y = "guidelineCreator.end_y";
    public static final String KEY_ROAD_SIZE = "guidelineCreator.road_size";
    public static final String KEY_ISENTRANCE = "guidelineCreator.is_entrance";

    public GuidelineCreator(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);

        switch (si.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("RoadDetector.PathPlanning",
                        CSUConstants.A_STAR_PATH_PLANNING);
                this.clustering = moduleManager.getModule("SampleRoadDetector.Clustering",
                        "adf.sample.module.algorithm.SampleKMeans");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("RoadDetector.PathPlanning",
                        CSUConstants.A_STAR_PATH_PLANNING);
                this.clustering = moduleManager.getModule("SampleRoadDetector.Clustering",
                        "adf.sample.module.algorithm.SampleKMeans");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("RoadDetector.PathPlanning",
                        CSUConstants.A_STAR_PATH_PLANNING);
                this.clustering = moduleManager.getModule("SampleRoadDetector.Clustering",
                        "adf.sample.module.algorithm.SampleKMeans");
                break;
        }
        this.clustering = moduleManager.getModule("ActionExtClear.Clustering",
                "adf.sample.module.algorithm.SampleKMeans");
    }


    //d1 - d2 is nearest //d2 -d1 is farest
    private class DistanceIDSorter implements Comparator<EntityID> {
        private WorldInfo worldInfo;
        private EntityID reference;

        DistanceIDSorter(WorldInfo worldInfo, EntityID reference) {
            this.worldInfo = worldInfo;
            this.reference = reference;
        }

        DistanceIDSorter(WorldInfo worldInfo, StandardEntity reference) {
            this.worldInfo = worldInfo;
            this.reference = reference.getID();
        }

        public int compare(EntityID a, EntityID b) {
            int d1 = this.worldInfo.getDistance(this.reference, a);
            int d2 = this.worldInfo.getDistance(this.reference, b);
            return d2 - d1;
        }
    }

    @Override
    public AbstractModule calc() {

        return this;
    }

    @Override
    public AbstractModule updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (getCountUpdateInfo() >= 2) {
            return this;
        }
        this.clustering.updateInfo(messageManager);
        this.pathPlanning.updateInfo(messageManager);
        return null;
    }

    @Override
    public AbstractModule precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (getCountPrecompute() >= 2) {
            return this;
        }

        this.createGuideline();
        this.roadsize = this.judgeRoad.size();
        precomputeData.setInteger(KEY_ROAD_SIZE,this.roadsize);
        ArrayList<java.awt.geom.Line2D> line2DS = new ArrayList<>();
        for (int i = 0; i < this.roadsize; ++i) {
            line2DS.add(new java.awt.geom.Line2D.Double(this.judgeRoad.get(i).getStartPoint().getX(), this.judgeRoad.get(i).getStartPoint().getY(), this.judgeRoad.get(i).getEndPoint().getX(), this.judgeRoad.get(i).getEndPoint().getY()));
			precomputeData.setDouble(KEY_START_X + i, this.judgeRoad.get(i).getStartPoint().getX());
			precomputeData.setDouble(KEY_START_Y + i, this.judgeRoad.get(i).getStartPoint().getY());
			precomputeData.setDouble(KEY_END_X + i, this.judgeRoad.get(i).getEndPoint().getX());
			precomputeData.setDouble(KEY_END_Y + i, this.judgeRoad.get(i).getEndPoint().getY());
			precomputeData.setEntityID(KEY_JUDGE_ROAD + i, this.judgeRoad.get(i).getSelfID());
			precomputeData.setBoolean(KEY_ISENTRANCE + i, this.judgeRoad.get(i).getEntranceState());
        }
        if (DebugHelper.DEBUG_MODE){
            DebugHelper.VD_CLIENT.drawAsync(agentInfo.getID().getValue(), "GuideLine", line2DS);
        }

        this.clustering.precompute(precomputeData);
        this.pathPlanning.precompute(precomputeData);
        return this;
    }

    @Override
    public AbstractModule resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (getCountResume() >= 2) {
            return this;
        }

        this.roadsize = precomputeData.getInteger(KEY_ROAD_SIZE);
        ArrayList<java.awt.geom.Line2D> line2DS = new ArrayList<>();
        for (int i = 0; i < this.roadsize; ++i) {
			double startx = precomputeData.getDouble(KEY_START_X + i);
			double starty = precomputeData.getDouble(KEY_START_Y + i);
			Point2D start = new Point2D(startx, starty);
			double endx = precomputeData.getDouble(KEY_END_X + i);
			double endy = precomputeData.getDouble(KEY_END_Y + i);
			Boolean is_entrance = precomputeData.getBoolean(KEY_ISENTRANCE + i);
			Point2D end = new Point2D(endx, endy);
			Road road = (Road) this.worldInfo.getEntity(precomputeData.getEntityID(KEY_JUDGE_ROAD + i));
			guidelineHelper line = new guidelineHelper(road, start, end, is_entrance);
			if (!this.judgeRoad.contains(line)) {
				this.judgeRoad.add(line);
                line2DS.add(new java.awt.geom.Line2D.Double(startx, starty, endx, endy));
			}
        }

        if (DebugHelper.DEBUG_MODE){
            DebugHelper.VD_CLIENT.drawAsync(agentInfo.getID().getValue(), "GuideLine", line2DS);
        }

        this.clustering.resume(precomputeData);
        this.pathPlanning.resume(precomputeData);
        return this;
    }

    @Override
    public AbstractModule preparate() {
        super.preparate();
        if (getCountPreparate() >= 2) {
            return this;
        }
        this.createGuideline();
        this.clustering.preparate();
        this.pathPlanning.preparate();
        return this;
    }


    public List<guidelineHelper> getJudgeRoad(){
        return this.judgeRoad;
    }

    /**
     * @Description: 获取合适的线作为guideline
     * @Author: Bochun-Yue
     * @Date: 10/27/20
     */
    private Line2D getProperLine(Road road) {
        List<Edge> edges = new ArrayList<>();
        List<Edge> reservedEdges = new ArrayList<>();
        for (Edge edge : road.getEdges()) {
            Boolean edgeToEntrance = false;
            for(EntityID neighbour : road.getNeighbours()){
                Edge neighbourEdge = road.getEdgeTo(neighbour);
                if(this.countedEntrance.contains(neighbour) && this.getMidPoint(edge).equals(this.getMidPoint(neighbourEdge))){
                    edgeToEntrance = true;
                    break;
                }
            }
            if(edge.isPassable()){
                reservedEdges.add(edge);
                if(!edgeToEntrance){
                    edges.add(edge);
                }
            }
        }

        Point2D start = null;
        Point2D end = null;
        double max = Double.MIN_VALUE;
        if(edges != null && edges.size() > 1) {
            for (Edge edge : edges) {
                Edge opposite = this.getOppositeEdge(road, edge);
                if(opposite != null) {
                    Point2D p1 = this.getMidPoint(edge);
                    Point2D p2 = this.getMidPoint(opposite);
                    double dist = this.getDistance(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                    if (dist > max) {
                        max = dist;
                        start = p1;
                        end = p2;
                    }
                }
            }
            if (start != null && end != null) {
                Line2D guideline = new Line2D(start, end);
                return guideline;
            }
        }else if(edges != null && edges.size() == 1){
            Edge only = edges.get(0);
            Point2D onlyMid = this.getMidPoint(only);
            Point2D roadCenter = new Point2D(road.getX(),road.getY());
            List<Edge> allOtherEdges = new ArrayList<>();
            for(Edge e : road.getEdges()){
                if(!this.getMidPoint(e).equals(this.getMidPoint(only))){
                    allOtherEdges.add(e);
                }
            }
            Vector2D standardDirection = new Vector2D(roadCenter.getX() - onlyMid.getX(), roadCenter.getY() - onlyMid.getY());
            Point2D answerPoint = null;
            double minAngle = Double.MAX_VALUE;
            for (int i = 0; i < allOtherEdges.size(); ++i) {
                Point2D mid = this.getMidPoint(allOtherEdges.get(i));
                Vector2D testDirection = new Vector2D(mid.getX() - onlyMid.getX(), mid.getY() - onlyMid.getY());
                double angle = GeometryTools2D.getAngleBetweenVectors(standardDirection, testDirection);
                if (angle < minAngle) {
                    minAngle = angle;
                    answerPoint = mid;
                }
            }
            Line2D guideline = new Line2D(onlyMid,answerPoint);
            return guideline;
        }
        //in case of all the neighbours are entrance
        else {
            if(reservedEdges != null && reservedEdges.size() > 1) {
                for (Edge edge : reservedEdges) {
                    Edge opposite = this.getOppositeEdge(road, edge);
                    Point2D p1 = this.getMidPoint(edge);
                    Point2D p2 = this.getMidPoint(opposite);
                    double dist = this.getDistance(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                    if (dist > max) {
                        max = dist;
                        start = p1;
                        end = p2;
                    }
                }
                if (start != null && end != null) {
                    Line2D guideline = new Line2D(start, end);
                    return guideline;
                }
            }else if(reservedEdges != null && reservedEdges.size() == 1){
                Edge only = reservedEdges.get(0);
                Point2D onlyMid = this.getMidPoint(only);
                Point2D roadCenter = new Point2D(road.getX(),road.getY());
                List<Edge> allOtherEdges = new ArrayList<>();
                for(Edge e : road.getEdges()){
                    if(!this.getMidPoint(e).equals(this.getMidPoint(only))){
                        allOtherEdges.add(e);
                    }
                }
                allOtherEdges.remove(only);
                Vector2D standardDirection = new Vector2D(roadCenter.getX() - onlyMid.getX(), roadCenter.getY() - onlyMid.getY());
                Point2D answerPoint = null;
                double minAngle = Double.MAX_VALUE;
                for (int i = 0; i < allOtherEdges.size(); ++i) {
                    Point2D mid = this.getMidPoint(allOtherEdges.get(i));
                    Vector2D testDirection = new Vector2D(mid.getX() - onlyMid.getX(), mid.getY() - onlyMid.getY());
                    double angle = GeometryTools2D.getAngleBetweenVectors(standardDirection, testDirection);
                    if (angle < minAngle) {
                        minAngle = angle;
                        answerPoint = mid;
                    }
                }
                Line2D guideline = new Line2D(onlyMid,roadCenter);
                return guideline;
            }
        }
        System.out.println("null properLine");
        System.out.println("RoadID:"+road.getID().getValue());
        return null;
    }

    private void createGuideline() {
        //建筑入口
        for (StandardEntity se : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD, StandardEntityURN.HYDRANT)) {
            Road road = (Road) se;
            for (EntityID neighbour : road.getNeighbours()) {
                if (this.worldInfo.getEntity(neighbour) instanceof Building
                        || this.worldInfo.getEntity(neighbour) instanceof Refuge) {
                    Edge edgeToBuilding = road.getEdgeTo(neighbour);
                    Point2D startPoint = this.getMidPoint(edgeToBuilding);
                    Edge oppositeEdge = this.getOppositeEdge(road, edgeToBuilding);
                    if(oppositeEdge != null) {
                        Point2D endPoint = this.getMidPoint(oppositeEdge);
                        guidelineHelper guideline = new guidelineHelper(road, startPoint, endPoint, true);
                        if (!this.judgeRoad.contains(guideline)) {
                            this.judgeRoad.add(guideline);
                            this.countedRoad.add(road.getID());
                            this.countedEntrance.add(road.getID());
                        }
                    }
                }
            }
        }

        //对剩余道路进行由远到近的距离排序
        List<EntityID> worldRoadID = new ArrayList<>();
        List<StandardEntity> worldRoad = new ArrayList<>();
        for (EntityID id : this.worldInfo.getEntityIDsOfType(StandardEntityURN.ROAD, StandardEntityURN.HYDRANT)) {
            if (!this.countedRoad.contains(id)) {
                worldRoadID.add(id);
            }
        }
        worldRoadID.sort(new DistanceIDSorter(this.worldInfo, this.agentInfo.getID()));
        for (EntityID id : worldRoadID) {
            worldRoad.add(this.worldInfo.getEntity(id));
        }
        StandardEntity positionEntity = this.worldInfo.getEntity(this.agentInfo.getPosition());

        if (positionEntity instanceof Road || positionEntity instanceof Hydrant) {

            //预计算时PF所处的road，由于只有一块，故可忽略直接取最长线段作为guideline
            Road position = (Road) positionEntity;
            guidelineHelper guideline = new guidelineHelper(this.getProperLine(position), position, false);
            if (!this.judgeRoad.contains(guideline)) {
                this.judgeRoad.add(guideline);
                this.countedRoad.add(position.getID());
            }
            //PF位置的neighbours
            for (EntityID neighbour : position.getNeighbours()) {
                if (this.worldInfo.getEntity(neighbour) instanceof Road
                        || this.worldInfo.getEntity(neighbour) instanceof Hydrant) {
                    Road road = (Road) this.worldInfo.getEntity(neighbour);
                    Edge edge = position.getEdgeTo(neighbour);
                    Point2D startPoint = this.getMidPoint(edge);
                    Edge oppositeEdge = this.getPFNeighbourOppositeEdge(road, edge);
                    Point2D endPoint = this.getMidPoint(oppositeEdge);
                    guidelineHelper line = new guidelineHelper(road, startPoint, endPoint, false);
                    if (!this.judgeRoad.contains(line)) {
                        this.judgeRoad.add(line);
                        this.countedRoad.add(road.getID());
                    }
                }
            }
            //根据路线计算guideline,此举可得绝大部分guideline
            for (int cnt = 0 ; cnt < worldRoad.size() ; ++ cnt) {
                
                StandardEntity se = worldRoad.get(cnt);
                if (this.countedRoad.contains(se.getID())) {
                    continue;
                }
                boolean flag = false;
                Road otherRoads = (Road) se;
                this.pathPlanning.setFrom(position.getID());
                this.pathPlanning.setDestination(se.getID());
                List<EntityID> path = this.pathPlanning.calc().getResult();
                if (path != null && path.size() > 2) {
                    for (int i = 1; i < path.size() - 1; ++i) {
                        StandardEntity entity = this.worldInfo.getEntity(path.get(i));
                        if (!(entity instanceof Road) && !(entity instanceof Hydrant)) continue;
                        Road road = (Road) entity;
                        Area before = (Area) this.worldInfo.getEntity(path.get(i - 1));
                        Area next = (Area) this.worldInfo.getEntity(path.get(i + 1));
                        if(i > 2 && i<path.size()-2){
                            if(this.countedEntrance.contains(path.get(i-1))){
                                StandardEntity theRoadBefore = this.worldInfo.getEntity(path.get(i-2));
                                if(theRoadBefore instanceof Road && road.getEdgeTo(theRoadBefore.getID())!=null){
                                    before = (Area) theRoadBefore;
                                }
                            }
                            if(this.countedEntrance.contains(path.get(i+1))){
                                StandardEntity third = this.worldInfo.getEntity(path.get(i+2));
                                if(third instanceof Road && road.getEdgeTo(third.getID())!=null){
                                    next = (Area) third;
                                }
                            }
                        }
                        Edge edge1 = before.getEdgeTo(road.getID());
                        Edge edge2 = road.getEdgeTo(next.getID());
                        Point2D start = this.getMidPoint(edge1);
                        Point2D end = this.getMidPoint(edge2);
                        guidelineHelper line = new guidelineHelper(road, start, end,false);
                        if (!this.judgeRoad.contains(line)) {
                            this.judgeRoad.add(line);
                            this.countedRoad.add(entity.getID());
                        }
                    }
                }
            }
        }
        //防止用来进行计算guideline的PF在建筑内
        else if (positionEntity instanceof Building) {
            Building building = (Building) positionEntity;

            for (int cnt = 0 ; cnt < worldRoad.size() ; ++ cnt) {
                StandardEntity se = worldRoad.get(cnt);
                if (this.countedRoad.contains(se.getID())) {
                    continue;
                }
                this.pathPlanning.setFrom(building.getID());
                this.pathPlanning.setDestination(se.getID());
                List<EntityID> path = this.pathPlanning.calc().getResult();
                if (path != null && path.size() > 2) {
                    for (int i = 1; i < path.size() - 1; ++i) {
                        StandardEntity entity = this.worldInfo.getEntity(path.get(i));
                        if (!(entity instanceof Road) && !(entity instanceof Hydrant)) continue;
                        Road road = (Road) entity;
                        Area before = (Area) this.worldInfo.getEntity(path.get(i - 1));
                        Area next = (Area) this.worldInfo.getEntity(path.get(i + 1));
                        if(i > 2 && i<path.size()-2){
                            if(this.countedEntrance.contains(path.get(i-1))){
                                StandardEntity theRoadBefore = this.worldInfo.getEntity(path.get(i-2));
                                if(theRoadBefore instanceof Road && road.getEdgeTo(theRoadBefore.getID())!=null){
                                    before = (Area) theRoadBefore;
                                }
                            }
                            if(this.countedEntrance.contains(path.get(i+1))){
                                StandardEntity third = this.worldInfo.getEntity(path.get(i+2));
                                if(third instanceof Road && road.getEdgeTo(third.getID())!=null){
                                    next = (Area) third;
                                }
                            }
                        }
                        Edge edge1 = before.getEdgeTo(road.getID());
                        Edge edge2 = road.getEdgeTo(next.getID());
                        Point2D start = this.getMidPoint(edge1);
                        Point2D end = this.getMidPoint(edge2);
                        guidelineHelper line = new guidelineHelper(road, start, end,false);
                        if (!this.judgeRoad.contains(line)) {
                            this.judgeRoad.add(line);
                            this.countedRoad.add(entity.getID());
                        }
                    }
                }
            }
        }

        //地图外圈的guideline
        Collection<StandardEntity> remoteRoad = this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD, StandardEntityURN.HYDRANT);
        List<EntityID> remoteRoadID = new ArrayList<>();
        for (StandardEntity se : remoteRoad) {
            remoteRoadID.add(se.getID());
        }
        remoteRoadID.removeAll(countedRoad);
        for (StandardEntity se : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD, StandardEntityURN.HYDRANT)) {
            Road road = (Road) se;
            Line2D createLine = this.getProperLine(road);
            if(createLine != null) {
                guidelineHelper line = new guidelineHelper(createLine, road, false);
                if (!this.judgeRoad.contains(line)){
                    this.judgeRoad.add(line);
                }
            }
        }
    }

    //应对某个道路对面的edge
    private Edge getOppositeEdge(Road road,Edge original) {
        List<Edge> edges = new ArrayList<>();
        for(Edge edge : road.getEdges()){
            if(edge.isPassable() && !this.getMidPoint(edge).equals(this.getMidPoint(original))){
                edges.add(edge);
            }
        }
        if(edges.size() > 0) {
            Point2D roadCenter = new Point2D(road.getX(), road.getY());
            Point2D originalMid = this.getMidPoint(original);
            Vector2D standardDirection = new Vector2D(roadCenter.getX() - originalMid.getX(), roadCenter.getY() - originalMid.getY());
            Edge answerEdge = null;
            double minAngle = Double.MAX_VALUE;
            for (int i = 0; i < edges.size(); ++i) {
                Point2D mid = this.getMidPoint(edges.get(i));
                Vector2D testDirection = new Vector2D(mid.getX() - originalMid.getX(), mid.getY() - originalMid.getY());
                double angle = GeometryTools2D.getAngleBetweenVectors(standardDirection, testDirection);
                if (angle < minAngle) {
                    minAngle = angle;
                    answerEdge = edges.get(i);
                }
            }
            return answerEdge;
        }
        return null;
    }
    //PF的neighbour,只用到一次
    private Edge getPFNeighbourOppositeEdge(Road road,Edge original){
        List<Edge> edges = new ArrayList<>();
        List<Edge> reverseEdges = new ArrayList<>();
        for(Edge edge : road.getEdges()){
            if(!this.getMidPoint(edge).equals(this.getMidPoint(original))){
                reverseEdges.add(edge);
            }
            if(edge.isPassable() && !this.getMidPoint(edge).equals(this.getMidPoint(original))){
                edges.add(edge);
            }
        }
        if(edges.size() > 0) {
            Point2D roadCenter = new Point2D(road.getX(), road.getY());
            Point2D originalMid = this.getMidPoint(original);
            Vector2D standardDirection = new Vector2D(roadCenter.getX() - originalMid.getX(), roadCenter.getY() - originalMid.getY());
            Edge answerEdge = null;
            double minAngle = Double.MAX_VALUE;
            for (int i = 0; i < edges.size(); ++i) {
                Point2D mid = this.getMidPoint(edges.get(i));
                Vector2D testDirection = new Vector2D(mid.getX() - originalMid.getX(), mid.getY() - originalMid.getY());
                double angle = GeometryTools2D.getAngleBetweenVectors(standardDirection, testDirection);
                if (angle < minAngle) {
                    minAngle = angle;
                    answerEdge = edges.get(i);
                }
            }
            return answerEdge;
        }
        if(reverseEdges.size() > 0) {
            Point2D roadCenter = new Point2D(road.getX(), road.getY());
            Point2D originalMid = this.getMidPoint(original);
            Vector2D standardDirection = new Vector2D(roadCenter.getX() - originalMid.getX(), roadCenter.getY() - originalMid.getY());
            Edge answerEdge = null;
            double minAngle = Double.MAX_VALUE;
            for (int i = 0; i < reverseEdges.size(); ++i) {
                Point2D mid = this.getMidPoint(reverseEdges.get(i));
                Vector2D testDirection = new Vector2D(mid.getX() - originalMid.getX(), mid.getY() - originalMid.getY());
                double angle = GeometryTools2D.getAngleBetweenVectors(standardDirection, testDirection);
                if (angle < minAngle) {
                    minAngle = angle;
                    answerEdge = reverseEdges.get(i);
                }
            }
            return answerEdge;
        }
        return null;
    }

    private Point2D getMidPoint(Edge edge) {
        if(edge != null) {
            double midX = (edge.getStartX() + edge.getEndX()) / 2;
            double midY = (edge.getStartY() + edge.getEndY()) / 2;
            Point2D point = new Point2D(midX, midY);
            return point;
        }
        return null;
    }


    private double getDistance(double fromX, double fromY, double toX, double toY) {
        double dx = fromX - toX;
        double dy = fromY - toY;
        return Math.hypot(dx, dy);
    }


}
