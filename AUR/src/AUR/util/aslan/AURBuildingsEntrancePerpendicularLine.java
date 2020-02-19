package AUR.util.aslan;

import AUR.util.knd.AURConstants;
import AUR.util.knd.AURGeoUtil;
import AUR.util.knd.AURWorldGraph;
import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.info.AgentInfo;
import adf.agent.info.WorldInfo;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Amir Aslan Aslani - Mar 2018
 */
public class AURBuildingsEntrancePerpendicularLine {
        private AgentInfo ai;
        private WorldInfo wi;
        private AURClearWatcher cw;
        private AURWorldGraph wsg;
        
        public final int NO_POINT_SELECTED = 0,
                         GOING_TO_POINT = 1,
                         GOING_FROM_POINT_TO_BUILDING = 2,
                         GOING_FROM_BUILDING_TO_POINT = 3;
        
        private Point2D lastHomeComing = null;
        private EntityID lastHomeComingEntityID = null;
        private int lastHomeComingStatus = this.NO_POINT_SELECTED;

        public AURBuildingsEntrancePerpendicularLine(AgentInfo ai, WorldInfo wi, AURClearWatcher cw, AURWorldGraph wsg) {
                this.ai = ai;
                this.wi = wi;
                this.cw = cw;
                this.wsg = wsg;
        }
        
        public Action setChangesOfBuildingsEntrancePerpendicularLine(EntityID target) {
                if(! AURConstants.PoliceExtClear.USE_BUILDINGS_ENTRANCE_PERPENDICULAR_LINE){
                        return null;
                }
                
                if(lastHomeComing != null &&
                   this.lastHomeComingStatus == GOING_TO_POINT &&
                   AURConstants.Agent.RADIUS > Math.hypot(ai.getX() - lastHomeComing.getX(), ai.getY() - lastHomeComing.getY())
                ){
                        lastHomeComingStatus = this.GOING_FROM_POINT_TO_BUILDING;
                }
                if(lastHomeComingStatus == GOING_FROM_BUILDING_TO_POINT &&
                   AURConstants.Agent.RADIUS > Math.hypot(ai.getX() - lastHomeComing.getX(), ai.getY() - lastHomeComing.getY())
                ){
                        lastHomeComing = null;
                        lastHomeComingEntityID = null;
                        lastHomeComingStatus = this.NO_POINT_SELECTED;
                }
                else if(lastHomeComingStatus == GOING_FROM_BUILDING_TO_POINT){
                        System.out.println("Back to point...");
                        return this.cw.getAction(
                                new ActionMove(
                                        wsg.getPathToClosest(ai.getPosition(), Lists.newArrayList(lastHomeComingEntityID)),
                                        (int) lastHomeComing.getX(),
                                        (int) lastHomeComing.getY()
                                )
                        );
                }
                if(lastHomeComingStatus == GOING_FROM_POINT_TO_BUILDING &&
                   (wi.getEntity(ai.getPosition()) instanceof Building ||
                    this.wsg.getNoBlockadePathToClosest(ai.getPosition(), Lists.newArrayList(target)).size() == this.wsg.getPathToClosest(ai.getPosition(), Lists.newArrayList(target)).size())
                ){
                        lastHomeComingStatus = this.GOING_FROM_BUILDING_TO_POINT;
                }
                return null;
        }
        
        public double[] getBuildingEntranceLine(ArrayList<EntityID> path){
                if(AURConstants.PoliceExtClear.USE_BUILDINGS_ENTRANCE_PERPENDICULAR_LINE && 
                   path.size() > 1 &&
                   wi.getEntity(path.get( path.size() - 1 ) ) instanceof Building  &&
                   (lastHomeComingStatus == this.NO_POINT_SELECTED || this.lastHomeComingStatus == this.GOING_TO_POINT)
                ){
                        Area targetBuilding = (Area) wi.getEntity(path.get( path.size() - 1 ) );
                        Edge targetBuildingEntrance = targetBuilding.getEdgeTo(path.get( path.size() - 2 ));
                        System.out.println("Building detected as target " + targetBuilding.getID()); 
                        return getBuildingEntranceLine(targetBuilding, targetBuildingEntrance);
                }
                return null;
        }
        
        public Pair<Point2D, EntityID> isGoingToPoint(double[] buildingEntranceLine){
                Pair<Point2D, EntityID> decidedLine = null;
                double[] intersect = new double[]{-1,-1};
                
                if(AURConstants.PoliceExtClear.USE_BUILDINGS_ENTRANCE_PERPENDICULAR_LINE &&
                   (lastHomeComingStatus == NO_POINT_SELECTED || lastHomeComingStatus == GOING_TO_POINT) &&
                   ! (cw.lastMoveVector[0] == 0 && cw.lastMoveVector[1] == 0) &&
                   buildingEntranceLine != null &&
                   isCurrentLineIntersect(buildingEntranceLine,intersect) &&
                   intersect[0] != -1 &&
                   intersect[1] != -1 &&
                   (lastHomeComing == null ||
                   (lastHomeComing != null &&
                   AURConstants.Agent.RADIUS > Math.hypot(ai.getX() - lastHomeComing.getX(), ai.getY() - lastHomeComing.getY())))
                ){
                        decidedLine = new Pair(
                                new Point2D(intersect[0], intersect[1]),
                                ai.getPosition()
                        );
                        lastHomeComing = decidedLine.first();
                        lastHomeComingEntityID = decidedLine.second();
                        lastHomeComingStatus = GOING_TO_POINT;
                }
                return decidedLine;
        }
        
        private double[] getBuildingEntranceLine(Area a,Edge e){
                double[] pME = AURGeoMetrics.getPointFromPoint2D(
                        AURGeoTools.getEdgeMid(e)
                );
                
                double vE[] = new double[]{
                        e.getEndX() - e.getStartX(),
                        e.getEndY() - e.getStartY()
                };
                double[] perpendicularVector = AURGeoMetrics.getPerpendicularVector(vE);
                perpendicularVector = AURGeoMetrics.getVectorNormal(perpendicularVector);
                vE = AURGeoMetrics.getVectorNormal(vE);
                
                int linesNumber = 5;
                int linesLen = 10000;
                double[][][] lines = new double[linesNumber][2][2];
                double mids[][] = new double[linesNumber][2];
                for(int i = 0;i < linesNumber;i ++){
                        mids[i] = AURGeoMetrics.getPointsPlus(
                                pME,
                                AURGeoMetrics.getVectorScaled(vE,3 * AURConstants.Agent.RADIUS * (i - linesNumber / 2) / linesNumber)
                        );
                        lines[i][0] = AURGeoMetrics.getPointsPlus(mids[i], AURGeoMetrics.getVectorScaled(perpendicularVector, linesLen));
                        lines[i][1] = AURGeoMetrics.getPointsPlus(mids[i], AURGeoMetrics.getVectorScaled(perpendicularVector, -linesLen));
                }
                
                double p1[] = AURGeoMetrics.getPointsPlus(pME, AURGeoMetrics.getVectorScaled(perpendicularVector, linesLen)),
                       p2[] = AURGeoMetrics.getPointsPlus(pME, AURGeoMetrics.getVectorScaled(perpendicularVector, -linesLen));
                
                for(EntityID aid : wi.getObjectIDsInRectangle((int) lines[1][0][0], (int) lines[1][0][1], (int) lines[1][1][0], (int) lines[1][1][1])){
                        if(! (wi.getEntity(aid) instanceof Area))
                                continue;
                        
                        Area area = (Area) wi.getEntity(aid);
                        
                        if(area.isEdgesDefined()){
                                for(Edge edge : area.getEdges()){
                                        if(edge.isPassable())
                                                continue;
                                        
                                        for(int i = 0;i < lines.length;i ++){
                                                double[] intersect = new double[]{-1,-1};
                                                AURGeoUtil.getIntersection(
                                                        edge.getLine().getOrigin().getX(),
                                                        edge.getLine().getOrigin().getY(),
                                                        edge.getLine().getEndPoint().getX(),
                                                        edge.getLine().getEndPoint().getY(),
                                                        lines[i][0][0],
                                                        lines[i][0][1],
                                                        lines[i][1][0],
                                                        lines[i][1][1],
                                                        intersect
                                                );

                                                boolean linesIntersect = java.awt.geom.Line2D.linesIntersect(
                                                        edge.getLine().getOrigin().getX(),
                                                        edge.getLine().getOrigin().getY(),
                                                        edge.getLine().getEndPoint().getX(),
                                                        edge.getLine().getEndPoint().getY(),
                                                        lines[i][0][0],
                                                        lines[i][0][1],
                                                        lines[i][1][0],
                                                        lines[i][1][1]
                                                );

                                                if(intersect[0] != -1 && linesIntersect){
                                                        if(AURGeoUtil.length(intersect[0], intersect[1], p1[0], p1[1]) < AURGeoUtil.length(intersect[0], intersect[1], p2[0], p2[1])){
                                                                double hypot = Math.hypot(intersect[0] - mids[i][0],intersect[1] - mids[i][1]);
                                                                for(int j = 0;j < lines.length;j ++){
                                                                        lines[j][0] = AURGeoMetrics.getPointsPlus(
                                                                                mids[j],
                                                                                AURGeoMetrics.getVectorScaled(
                                                                                        perpendicularVector,
                                                                                        hypot - AURConstants.Agent.RADIUS
                                                                                )
                                                                        );
                                                                }
                                                        }
                                                        else{
                                                                double hypot = - Math.hypot(intersect[0] - mids[i][0],intersect[1] - mids[i][1]);
                                                                for(int j = 0;j < lines.length;j ++){
                                                                        lines[j][1] = AURGeoMetrics.getPointsPlus(
                                                                                mids[j],
                                                                                AURGeoMetrics.getVectorScaled(
                                                                                        perpendicularVector,
                                                                                        hypot - AURConstants.Agent.RADIUS
                                                                                )
                                                                        );
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
                
                double[] result = new double[4];
                result[0] = lines[linesNumber / 2][0][0];
                result[1] = lines[linesNumber / 2][0][1];
                result[2] = lines[linesNumber / 2][1][0];
                result[3] = lines[linesNumber / 2][1][1];
                return result;
        }

        private boolean isCurrentLineIntersect(double[] buildingEntranceLine, double[] intersect) {
                double v[] = AURGeoMetrics.getVectorNormal(cw.lastMoveVector);
                double[] vectorScaled = AURGeoMetrics.getVectorScaled(
                        v,
                        Math.max(
                                Math.hypot(buildingEntranceLine[0] - ai.getX(),buildingEntranceLine[1] - ai.getY()),
                                Math.hypot(buildingEntranceLine[2] - ai.getX(),buildingEntranceLine[3] - ai.getY())
                        )
                );
                double[] agentLine = new double[]{
                        ai.getX(),
                        ai.getY(),
                        ai.getX() + vectorScaled[0],
                        ai.getY() + vectorScaled[1]
                };
                if(AURGeoUtil.getIntersection(
                        agentLine[0],
                        agentLine[1],
                        agentLine[2],
                        agentLine[3],
                        buildingEntranceLine[0],
                        buildingEntranceLine[1],
                        buildingEntranceLine[2],
                        buildingEntranceLine[3],
                        intersect
                )){
                        double[] vectorScaledForAgentSpace = AURGeoMetrics.getVectorScaled(
                                v,
                                AURConstants.Agent.RADIUS
                        );
                        agentLine[2] = intersect[0] + vectorScaledForAgentSpace[0];
                        agentLine[3] = intersect[1] + vectorScaledForAgentSpace[1];
                        
                        for(EntityID eid : wi.getObjectIDsInRectangle((int) agentLine[0], (int) agentLine[1], (int) agentLine[2], (int) agentLine[3])){
                                if(wi.getEntity(eid) instanceof Area){
                                        if(AURGeoTools.intersect(agentLine, (Area) wi.getEntity(eid))){
                                                return false;
                                        }
                                }
                        }
                        return true;
                }
                return false;
        }
}
