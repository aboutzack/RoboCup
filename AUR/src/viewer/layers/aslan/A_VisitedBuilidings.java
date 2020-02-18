package viewer.layers.aslan;

import AUR.util.ambulance.Information.BuildingInfo;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURGeoUtil;
import AUR.util.knd.AURWorldGraph;
import rescuecore2.standard.entities.Building;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

/**
 * Implementation From Arman's Idea.
 * @author Amir Aslan Aslani - Apr 2018
 */

public class A_VisitedBuilidings extends K_ViewerLayer {
        public HashSet<EntityID> visitedBuildings = new HashSet<>();
        
        @Override
        public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
                if(wsg.ai.getTime() < 2)
                        return;
                boolean intersect = false;
                Set<EntityID> changedEntities = wsg.ai.getChanged().getChangedEntities();
                if(changedEntities == null)
                        return;
                
                
                for (EntityID id : changedEntities) {
                        StandardEntity se = wsg.wi.getEntity(id);
                
                        if (se instanceof Civilian){
                                AURAreaGraph ag = wsg.getAreaGraph(((Civilian) se).getPosition());
                                if(ag.isBuilding() && ! ag.isRefuge()){
                                        visitedBuildings.add(((Civilian) se).getPosition());
                                }
                        }
                
                        if (se instanceof Building
                            && wsg.wi.getDistance(wsg.ai.getID(), id) < wsg.si.getPerceptionLosMaxDistance()) {
                            Building building = (Building) se;
                            intersect = false;
                            
                                for (StandardEntity entity : wsg.wi.getObjectsInRange(wsg.ai.getID(), wsg.si.getPerceptionLosMaxDistance())) {
                                        
                                        if (entity instanceof Area) {
                                                Area area = (Area) entity;
                                                
                                                if (entity instanceof Road) {
                                                        continue;
                                                }
                                                
                                                for (Edge edge : area.getEdges()) {
                                                        double[] d = new double[2];
                                                        if (edge.isPassable()) {
                                                                continue;
                                                        }
                                                        
                                                        if (AURGeoUtil.getIntersection(
                                                            edge.getStartX(), edge.getStartY(),
                                                            edge.getEndX(), edge.getEndY(),
                                                            wsg.ai.getX(), wsg.ai.getY(),
                                                            building.getX(), building.getY(),
                                                            d)) {
                                                                
                                                                intersect = true;
                                                                break;
                                                        }
                                                }
                                                if (intersect == true) {
                                                        break;
                                                }
                                        }

                                }
                                AURAreaGraph b = wsg.getAreaGraph(building.getID());
                                if(b != null && b.isBuilding()){
                                        Polygon sightAreaPolygon = wsg.getAreaGraph(building.getID()).getBuilding().getSightAreaPolygon();
                                        if (intersect == false && sightAreaPolygon != null && sightAreaPolygon.contains(wsg.ai.getX(), wsg.ai.getY())) {
                                                visitedBuildings.add(building.getID());
                                        }
                                }
                        }
                }
                
                
                g2.setStroke(new BasicStroke(2));
                g2.setColor(new Color(36, 255, 45, 80));
                for (EntityID building : visitedBuildings) {
                        AURAreaGraph b = wsg.getAreaGraph(building);
                        Polygon polygon = b.polygon;
                        g2.fillPolygon(kst.getTransformedPolygon(polygon));
                }
        }

        @Override
        public String getString(AURWorldGraph wsg, AURAreaGraph selected_ag) {
                return visitedBuildings.toString();
        }

        
}
