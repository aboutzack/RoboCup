package mrl_2019.complex.firebrigade;

import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

/**
 * Created by pooya on 6/21/2017.
 */
public class BuildingProperty {

    private EntityID entityID;
    private int fieryness;
    private int temperature;
    private double value;

    public BuildingProperty(EntityID entityID, int fieryness, int temperature) {
        this.entityID = entityID;
        this.fieryness = fieryness;
        this.temperature = temperature;
    }

    public BuildingProperty(Building building) {
        this.entityID = building.getID();
        this.fieryness = building.getFieryness();
    }

    public EntityID getEntityID() {
        return entityID;
    }

    public void setEntityID(EntityID entityID) {
        this.entityID = entityID;
    }

    public int getFieryness() {
        return fieryness;
    }

    public void setFieryness(int fieryness) {
        this.fieryness = fieryness;
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

        BuildingProperty that = (BuildingProperty) o;

        return fieryness == that.fieryness && entityID.equals(that.entityID);

    }

    @Override
    public int hashCode() {
        int result = entityID.hashCode();
        result = 31 * result + fieryness;
        return result;
    }
}
