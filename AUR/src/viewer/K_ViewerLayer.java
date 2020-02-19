package viewer;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.Graphics2D;

/**
 *
 * @author Alireza Kandeh - 2017
 */

public abstract class K_ViewerLayer {
    
    abstract public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag);
    
    public String getString(AURWorldGraph wsg, AURAreaGraph selected_ag) {
        return null;
    }
    
}
