package viewer.layers.aslan;

import AUR.util.aslan.AURPoliceScoreGraph;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURConstants;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Amir Aslan Aslani - Mar 2018
 */
public class A_AreasRoadDetectorScore extends K_ViewerLayer {

        @Override
        public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
                g2.setStroke(new BasicStroke(2));
                g2.setFont(new Font("Arial", 0, 9));
                g2.setColor(Color.BLACK);
                
                AURAreaGraph max = null;
                double maxN = 0;
                for (AURAreaGraph ag : wsg.areas.values()) {
                        if(ag.getFinalScore() > maxN){
                                maxN = ag.getFinalScore();
                                max = ag;
                        }
                        String score = String.valueOf(((int)(ag.getFinalScore() * 10000))/ 10000.0);
                        g2.drawString(score, kst.xToScreen(ag.getX()), kst.yToScreen(ag.getY()));
                }
                g2.setStroke(new BasicStroke(1));
                
                g2.setColor(new Color(100,100,0,100));
                if(max != null)
                        g2.fill(kst.getTransformedPolygon(max.polygon));
        }

        @Override
        public String getString(AURWorldGraph wsg, AURAreaGraph selected_ag) {
                String result = "\n";
                if (selected_ag != null) {
                        result += " Base Score: " + selected_ag.baseScore;
                        result += " \n Secondary Score: " + selected_ag.secondaryScore;
                        result += " \n Dist Score: " + selected_ag.distanceScore;
//                        result += " \n Refuge Score: " + AURPoliceScoreGraph.addRefugeScore(selected_ag, AURConstants.RoadDetector.BaseScore.REFUGE);
//                        result += " \n Gas Station Score: " + AURPoliceScoreGraph.addGasStationScore(selected_ag, AURConstants.RoadDetector.BaseScore.REFUGE);
//                        result += " \n Hydrant Score: " + AURPoliceScoreGraph.addHydrandScore(selected_ag, AURConstants.RoadDetector.BaseScore.REFUGE);
//                        result += " \n Entrance Number Score: " + AURPoliceScoreGraph.addEntrancesNumberScore(selected_ag, AURConstants.RoadDetector.BaseScore.REFUGE);
//                        result += " \n WSG Road Score: " + AURPoliceScoreGraph.addWSGRoadScores(selected_ag, AURConstants.RoadDetector.BaseScore.WSG_ROAD);
                        result += " \n Target Score: " + selected_ag.targetScore + " \n ";
                }
                return result;
        }

}
