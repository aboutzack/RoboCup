package viewer;

import java.util.HashMap;

/**
 *
 * @author Alireza Kandeh - 2017
 */

public class K_ViewerLayerFactory {
    
    private static K_ViewerLayerFactory _instance = null;
    
    private HashMap<String, K_ViewerLayer> layers = new HashMap<String, K_ViewerLayer>();
    
    public static K_ViewerLayerFactory getInstance() {
     if(_instance == null) {
            _instance = new K_ViewerLayerFactory();
        }
        return _instance;
    }
    
    public void addLayer(String name, Class c) {
        try {
            layers.put(name, (K_ViewerLayer) c.newInstance());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public K_ViewerLayer getLayer(String name) {
        return layers.get(name);
    }
    
}