package viewer.layers.aslan;

import AUR.util.aslan.AURGeoTools;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURGeoUtil;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Collection;
import rescuecore2.worldmodel.EntityID;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Amir Aslan Aslani - Apr 2018
 */
public class A_AroundRoadsOfGasStations extends K_ViewerLayer {

        @Override
        public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
                if (selected_ag != null && selected_ag.isGasStation()) {
                        g2.setColor(Color.red);
                        int fireExtinguishMaxDistance = wsg.si.getFireExtinguishMaxDistance();
                        
                        Polygon circle = AURGeoTools.getCircle(new int[]{selected_ag.getX(), selected_ag.getY()}, fireExtinguishMaxDistance);
                        
                        g2.draw(kst.getTransformedPolygon(circle));
                        
                        Collection<EntityID> objectIDsInRange = wsg.wi.getObjectIDsInRange(
                                selected_ag.getX(),
                                selected_ag.getY(),
                                wsg.si.getFireExtinguishMaxDistance()
                        );
                        
                        for(AURAreaGraph ag : wsg.getAreaGraph(objectIDsInRange)){
                                if(ag.isRoad() && AURGeoUtil.intersectsOrContains(circle, ag.polygon)){
                                        g2.draw(kst.getTransformedPolygon(ag.polygon));
                                }
                        }
                }
        }
}
