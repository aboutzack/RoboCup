package mrl_2019.world.helper;

import javolution.util.FastMap;
import mrl_2019.world.MrlWorldHelper;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.Property;

import java.util.Map;

/**
 * User: mrl_2019
 * Date: Dec 3, 2010
 * Time: 10:54:57 AM
 */
public class PropertyHelper implements IHelper {

    protected MrlWorldHelper world;
    protected Map<Property, Integer> propertyTimeMap = new FastMap<Property, Integer>();

    public PropertyHelper(MrlWorldHelper world) {
        this.world = world;
    }

    public void init() {
//        Collection<StandardEntity> entities = new HashSet<StandardEntity>();
//        entities.addAll(world.getBuildings());
//        entities.addAll(world.getRoads());
//        entities.addAll(world.getHumans());

        propertyTimeMap.clear();

//        //todo : use world.getEntities
//        for (StandardEntity entity : entities) {
//            for (Property property : entity.getProperties()) {
//                propertyTimeMap.put(property, 0);
//            }
//        }
    }

    public void update() {

    }

    public int getEntityLastUpdateTime(StandardEntity entity) {
        int maxTime = Integer.MIN_VALUE;
        for (Property property : entity.getProperties()) {
            Integer value = getPropertyTime(property);
            if (value > maxTime) {
                maxTime = value;
            }
        }

        return maxTime;
    }

    public Integer getPropertyTime(Property property) {
        Integer integer = propertyTimeMap.get(property);
        if (integer == null) {
            return 0;
        }
        return integer;
    }

    public void setPropertyTime(Property property, Integer time) {
        propertyTimeMap.put(property, time);
    }

    public void addEntityProperty(StandardEntity entity, int time) {
        for (Property property : entity.getProperties()) {
            propertyTimeMap.put(property, time);
        }

    }
}
