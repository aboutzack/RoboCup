package AUR.util.ambulance.sightAreaLegacy;

import AUR.util.knd.AURGeoUtil;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created by armanaxh on 12/21/17.
 */

public class SightPolygonAllocator
{
    private final double perceptionMax;
    private WorldInfo worldInfo;
    private ScenarioInfo scenarioInfo;
    private HashMap<Building, SightPolygon> visit;


    public SightPolygonAllocator(WorldInfo WorldInfo, ScenarioInfo scenarioInfo)
    {
        this.worldInfo = WorldInfo;
        this.scenarioInfo = scenarioInfo;
        this.perceptionMax = scenarioInfo.getPerceptionLosMaxDistance();
        this.visit = new HashMap();
    }

    public SightPolygonAllocator calc() {
        visit.clear();

//        Building b =(Building) worldInfo.getEntity(new EntityID(938));
//        visit.put((Building)b, this.calcForBulding((Building)b));
////
        for (StandardEntity localStandardEntity : worldInfo.getAllEntities()) {
            if ((localStandardEntity instanceof Building)) {
                visit.put((Building)localStandardEntity, this.calcForBulding((Building)localStandardEntity));
            }
        }
        return this;
    }

    public SightPolygon calcForBulding(Building paramBuilding) {
        SightPolygon result = new SightPolygon();
        Point2D center = new Point2D(paramBuilding.getX(), paramBuilding.getY());
        List<MultiEdge> edges = MultiEdge.getPassEdge(paramBuilding.getEdges());

        for (MultiEdge localEdge : edges) {
            result.addPolygon(calcMultiEdgeSight( localEdge , center));
        }
        return result;
    }
    public Polygon calcMultiEdgeSight(MultiEdge localEdge , Point2D center){
        ArrayList<Point2D> result = new ArrayList<>();
        RayIntersection rayIntersection = new RayIntersection(worldInfo,scenarioInfo);
        rayIntersection.findEntityLineInPerseptionRange( center);

        SpectatoLine spectatoEdge = new SpectatoLine(localEdge);
        spectatoEdge.reduce();
        SpectatoLine spectatoCenter = new SpectatoLine(localEdge).transfer(center);

        Vector2D localVector2D1 = spectatoEdge.getOrigin().minus(spectatoCenter.getOrigin());
        localVector2D1 = localVector2D1.normalised().scale(perceptionMax);
        Line2D localLine2D1 = rayIntersection.Calc(new Line2D(spectatoCenter.getOrigin(), localVector2D1));

        Vector2D localVector2D3 = spectatoEdge.getEndPoint().minus(spectatoCenter.getEndPoint());
        localVector2D3 = localVector2D3.normalised().scale(perceptionMax);
        Line2D localLine2D3 = rayIntersection.Calc(new Line2D(spectatoCenter.getEndPoint(), localVector2D3));

        boolean buldingHasNotWall = false;
        //vaghti bulding aslan Wall nadare va edgeStart va EdgeEnd ro ham miyoufte
        if(spectatoEdge.getOrigin() == spectatoEdge.getEndPoint()) {
            buldingHasNotWall = true;
        }
        if(buldingHasNotWall == false) {
            double[] intr = new double[2];
            if (AURGeoUtil.getIntersection(localLine2D1, localLine2D3, intr)) {
                result.add(localLine2D1.getOrigin());
                result.add(new Point2D(intr[0], intr[1]));
                result.add(localLine2D3.getOrigin());
                int[] xPoint = new int[result.size()];
                int[] yPoint = new int[result.size()];
                for (int i = 0; i < result.size(); i++) {
                    xPoint[i] = (int) result.get(i).getX();
                    yPoint[i] = (int) result.get(i).getY();
                }
                return new Polygon(xPoint, yPoint, result.size());
            }
        }
        if(buldingHasNotWall == false) {
            //add First Line
            result.add(localLine2D1.getOrigin());
            result.add(localLine2D1.getEndPoint());
        }

        //MID Point
        List<Point2D> points = localEdge.getMidPoint();
        if(points != null ) {
            for (int i = 0; i < points.size(); i++) {

                Vector2D localVector2D2 = points.get(i).minus(spectatoCenter.getmPoint());
                localVector2D2 = localVector2D2.normalised().scale(perceptionMax);
                Line2D localLine2D2 = rayIntersection.Calc(new Line2D(spectatoCenter.getmPoint(), localVector2D2));
                double inter1d1 = localLine2D2.getIntersection(localLine2D1);
                double inter1d2 = localLine2D1.getIntersection(localLine2D2);
                if (inter1d2 >= 0 && inter1d2 <= 1 && inter1d1 > 0 && inter1d1 <= 1) {
                    continue;
                }
                double inter2d1 = localLine2D2.getIntersection(localLine2D3);
                double inter2d2 = localLine2D3.getIntersection(localLine2D2);
                if (inter2d2 >= 0 && inter2d2 <= 1 && inter2d1 > 0 && inter2d1 <= 1) {
                    continue;
                }
                //else
                result.add(localLine2D2.getEndPoint());
            }
        }
        if (buldingHasNotWall == false) {
            //add End Line
            result.add(localLine2D3.getEndPoint());
            result.add(localLine2D3.getOrigin());
        }
        int[] xPoint = new int[result.size()];
        int[] yPoint = new int[result.size()];
        for (int i = 0; i < result.size(); i++) {
            xPoint[i] = (int) result.get(i).getX();
            yPoint[i] = (int) result.get(i).getY();
        }

        return new Polygon(xPoint , yPoint , result.size());
    }


    public SightPolygon getBuldingSightPolygon(Building paramBuilding) { return visit.containsKey(paramBuilding) == true ? (SightPolygon)visit.get(paramBuilding) : null; }

    public HashMap<Building, SightPolygon> getAreaPolygon()
    {
        return visit;
    }
}