package viewer.layers.AmboLayers;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import rescuecore2.standard.entities.*;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

import java.awt.*;

/**
 * Created by armanaxh on 2018.
 *
 */

public class AgentBig extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {


        Human agent = (Human)wsg.ai.me();
        g2.setStroke(new BasicStroke(1));
        if(agent instanceof AmbulanceTeam){
            g2.setColor(Color.white);
        }else if(agent instanceof PoliceForce){
            g2.setColor(Color.blue);
        }else{
            g2.setColor(Color.red);
        }

        int r = 6;
        if(agent.isXDefined() && agent.isYDefined()){
            g2.fillOval(
                    kst.xToScreen(agent.getX()) - r ,
                    kst.yToScreen(agent.getY()) - r ,
                    (int) (2 * r ),//* kst.zoom
                    (int) (2 * r )//* kst.zoom
            );
        }

        
    }

    @Override
    public String getString(AURWorldGraph wsg, AURAreaGraph selected_ag){
        return "action " + wsg.rescueInfo.temptest;
    }

}
