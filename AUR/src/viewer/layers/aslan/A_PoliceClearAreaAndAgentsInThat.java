package viewer.layers.aslan;

import AUR.util.aslan.AURGeoTools;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURConstants;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Collection;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Amir Aslan Aslani - Mar 2018
 */
public class A_PoliceClearAreaAndAgentsInThat extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        
        g2.setStroke(new BasicStroke(2));
        
        
        int[] agentPosition = new int[]{(int)wsg.ai.getX(), (int)wsg.ai.getY()};
        
        g2.setColor(new Color(255, 0, 0, 100));
        Polygon agentPolygon = AURGeoTools.getCircle(
                agentPosition,
                wsg.si.getClearRepairDistance() - AURConstants.Agent.RADIUS
        );

        g2.fill(kst.getTransformedPolygon(agentPolygon));
                        
        
        Collection<EntityID> objectIDsInRange = wsg.wi.getObjectIDsInRange(agentPosition[0], agentPosition[1], wsg.si.getClearRepairDistance() - AURConstants.Agent.RADIUS);
        for(EntityID o : objectIDsInRange){
                StandardEntity se = wsg.wi.getEntity(o);
                
                if(se instanceof Human && ! se.getID().equals(wsg.ai.getID())){
                        Human h = (Human) se;
                        int[] humanPosition = new int[]{(int)h.getX(), (int)h.getY()};
                        Polygon agentSPolygon = AURGeoTools.getCircle(
                                humanPosition,
                                (int) AURConstants.Agent.RADIUS + 10
                        );
                        
                        boolean x = true;
                        Rectangle humanPositionBounds = ((Area) wsg.wi.getEntity(((Human) se).getPosition())).getShape().getBounds();
//                        System.out.println("Max: " + Math.max(humanPositionBounds.width, humanPositionBounds.height));
                        g2.setColor(new Color(255, 255, 0, 150));
                        System.out.println(humanPositionBounds.width + " : " + humanPositionBounds.height);
                        int wid = Math.max(humanPositionBounds.width, humanPositionBounds.height);
                        g2.draw(kst.getTransformedPolygon(AURGeoTools.getCircle(
                                humanPosition,
                                wid
                        )));

                        if(wsg.wi.getEntity(h.getPosition()) instanceof Road){
                                if(((Road) wsg.wi.getEntity(h.getPosition())).getBlockades() != null)
                                        for(EntityID blc: ((Road) wsg.wi.getEntity(h.getPosition())).getBlockades()){
                                                Blockade blockade = (Blockade) wsg.wi.getEntity(blc);
//                                                        System.out.println("Blockade Drawen");
                                                g2.setColor(new Color(255, 255, 255, 255));
                                                g2.drawOval(
                                                        kst.xToScreen( blockade.getX() ) - 10 ,
                                                        kst.yToScreen( blockade.getY() ) - 10,
                                                        20,
                                                        20
                                                );
                                                if(/*intersect(blockade, agentSPolygon) || */blockade.getShape().contains(humanPosition[0],humanPosition[1])){
                                                        g2.setColor(new Color(0, 255, 0, 100));
                                                        g2.fill(kst.getTransformedPolygon(agentSPolygon));
                                                        x = false;
                //                                        break;
                                                }
                                        }
                        }
                        
                        if(x){
                                g2.setColor(new Color(0, 0, 255, 100));
                                g2.fill(kst.getTransformedPolygon(agentSPolygon));
                        }
//                        g2.drawOval(
//                                kst.xToScreen( h.getX() ) - 50 ,
//                                kst.yToScreen( h.getY() ) - 50,
//                                100,
//                                100
//                        );
                }
        }
    }
    

        private boolean intersect(Blockade blockade, Polygon polygon) {

                if (blockade.isApexesDefined()) {
                        int[] apexes0 = blockade.getApexes();
                        int[] apexes1 = new int[2 * polygon.npoints];
                        for (int i = 0; i < polygon.npoints; i++) {
                                apexes1[i * 2] = polygon.xpoints[i];
                                apexes1[i * 2 + 1] = polygon.ypoints[i];
                        }

                        for (int i = 0; i < (apexes0.length - 2); i += 2) {
                                for (int j = 0; j < (apexes1.length - 2); j += 2) {
                                        if (java.awt.geom.Line2D.linesIntersect(apexes0[i], apexes0[i + 1], apexes0[i + 2], apexes0[i + 3],
                                                apexes1[j], apexes1[j + 1], apexes1[j + 2], apexes1[j + 3])) {
                                                return true;
                                        }
                                }
                        }
                        for (int i = 0; i < (apexes0.length - 2); i += 2) {
                                if (java.awt.geom.Line2D.linesIntersect(apexes0[i], apexes0[i + 1], apexes0[i + 2], apexes0[i + 3],
                                        apexes1[apexes1.length - 2], apexes1[apexes1.length - 1], apexes1[0], apexes1[1])) {
                                        return true;
                                }
                        }
                        for (int j = 0; j < (apexes1.length - 2); j += 2) {
                                if (java.awt.geom.Line2D.linesIntersect(apexes0[apexes0.length - 2], apexes0[apexes0.length - 1],
                                        apexes0[0], apexes0[1], apexes1[j], apexes1[j + 1], apexes1[j + 2], apexes1[j + 3])) {
                                        return true;
                                }
                        }
                }
                return false;
        }

}


