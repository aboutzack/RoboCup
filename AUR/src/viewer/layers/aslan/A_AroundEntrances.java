package viewer.layers.aslan;

import AUR.util.aslan.AURGeoTools;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURConstants;
import AUR.util.knd.AURWorldGraph;
import adf.agent.info.WorldInfo;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Collection;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Amir Aslan Aslan - Mar 2018
 */
public class A_AroundEntrances extends K_ViewerLayer {
        
        @Override
        public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
                g2.setStroke(new BasicStroke(2));
                
        
                g2.setColor(new Color(0, 0 , 100 , 50));
                Polygon agentPolygon = AURGeoTools.getCircle(
                        new int[]{(int) wsg.ai.getX(), (int) wsg.ai.getY()},
                        wsg.si.getClearRepairDistance() - AURConstants.Agent.RADIUS
                );
                g2.fill(kst.getTransformedPolygon(agentPolygon));
                
                g2.setColor(Color.RED);
                Collection<StandardEntity> objectsInRange = wsg.wi.getObjectsInRange((int) wsg.ai.getX(), (int) wsg.ai.getY(), wsg.si.getClearRepairDistance());
                for (StandardEntity se : objectsInRange) {
                        if (se instanceof Building) {
                                Building b = (Building) se;
                                for (Edge e : b.getEdges()) {
                                        if (e.isPassable()
                                            && AURGeoTools.getEdgeMid(e).minus(new Point2D(wsg.ai.getX(), wsg.ai.getY())).getLength() < wsg.si.getClearRepairDistance() - 50
                                            && isPolygonOnBlockades(wsg.wi, g2, kst, AURGeoTools.getClearPolygon(new Point2D(wsg.ai.getX(), wsg.ai.getY()), AURGeoTools.getEdgeMid(e), AURConstants.Agent.RADIUS * 2 + 100, true))) {
                                                
                                                g2.setColor(Color.RED);
                                                g2.drawLine(
                                                        kst.xToScreen(e.getStartX()),
                                                        kst.yToScreen(e.getStartY()),
                                                        kst.xToScreen(e.getEndX()),
                                                        kst.yToScreen(e.getEndY())
                                                );
                                        }
                                }
                        }
                }
        }

        private boolean isPolygonOnBlockades(WorldInfo wi, Graphics2D g2, K_ScreenTransform kst , Polygon clearPolygon) {
                Rectangle bounds = clearPolygon.getBounds();
                
                Collection<StandardEntity> objectsInRectangle = wi.getObjectsInRectangle(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height);
                for (StandardEntity se : objectsInRectangle) {
                        if (se instanceof Road) {
                                Road r = (Road) se;
                                
                                g2.setColor(Color.CYAN);
                                g2.draw(kst.getTransformedPolygon(r.getShape()));
                                
                                if (r.isBlockadesDefined()) {
                                        for (EntityID eid : r.getBlockades()) {
                                                Blockade entity = (Blockade) wi.getEntity(eid);
                                                if (AURGeoTools.intersect(entity, clearPolygon)) {
                                                        return true;
                                                }
                                        }
                                }
                        }
                }
                return false;
        }
    
}
