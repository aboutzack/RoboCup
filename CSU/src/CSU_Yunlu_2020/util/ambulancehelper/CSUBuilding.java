package CSU_Yunlu_2020.util.ambulancehelper;

import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

public class CSUBuilding {

    private EntityID entityID;
    private int fireyness;
    private int temperature;
    private double value;

    public CSUBuilding(EntityID entityID, int fireyness, int temperature) {
        this.entityID = entityID;
        this.fireyness = fireyness;
        this.temperature = temperature;
    }

    public CSUBuilding(Building building) {
        this.entityID = building.getID();
        this.fireyness = building.getFieryness();
    }

    public EntityID getEntityID() {
        return entityID;
    }

    public void setEntityID(EntityID entityID) {
        this.entityID = entityID;
    }

    public int getFireyness() {
        return fireyness;
    }

    public void setFireyness(int fireyness) {
        this.fireyness = fireyness;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CSUBuilding that = (CSUBuilding) o;

        return fireyness == that.fireyness && entityID.equals(that.entityID);

    }

    @Override
    public int hashCode() {
        int result = entityID.hashCode();
        result = 31 * result + fireyness;
        return result;
    }
}
