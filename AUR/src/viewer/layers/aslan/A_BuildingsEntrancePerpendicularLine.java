package viewer.layers.aslan;

import AUR.util.aslan.AURGeoMetrics;
import AUR.util.aslan.AURGeoTools;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURConstants;
import AUR.util.knd.AURGeoUtil;
import AUR.util.knd.AURWorldGraph;
import adf.agent.info.WorldInfo;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.worldmodel.EntityID;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Amir Aslan Aslani - Mar 2018
 */
public class A_BuildingsEntrancePerpendicularLine extends K_ViewerLayer {

        WorldInfo wi;
        Graphics2D g2;
        K_ScreenTransform kst;
        
        @Override
        public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
                this.wi = wsg.wi;
                this.g2 = g2;
                this.kst = kst;
                
                g2.setStroke(new BasicStroke(2));
                g2.setFont(new Font("Arial", 0, 9));

                if(selected_ag == null || ! selected_ag.area.isEdgesDefined() || ! (selected_ag.area instanceof Building))
                        return;
                
                for(Edge e : selected_ag.area.getEdges()){
                        if(! e.isPassable())
                                continue;
                        double[] line = getLine(selected_ag.area, e);
                        
                        g2.setColor(Color.orange);
                        g2.drawLine(
                                kst.xToScreen(line[0]),
                                kst.yToScreen(line[1]),
                                kst.xToScreen(line[2]),
                                kst.yToScreen(line[3])
                        );
                }
        }

        public double[] getLine(Area a,Edge e){
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
                                                boolean linesIntersect = AURGeoUtil.getIntersection(
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
                
                g2.setColor(Color.BLUE);
                for(int i = 0;i < linesNumber;i ++){
                        g2.drawLine(
                                kst.xToScreen(lines[i][0][0]),
                                kst.yToScreen(lines[i][0][1]),
                                kst.xToScreen(lines[i][1][0]),
                                kst.yToScreen(lines[i][1][1])
                        );
                }
                
                double[] result = new double[4];
                result[0] = lines[linesNumber / 2][0][0];
                result[1] = lines[linesNumber / 2][0][1];
                result[2] = lines[linesNumber / 2][1][0];
                result[3] = lines[linesNumber / 2][1][1];
                return result;
        }
}


