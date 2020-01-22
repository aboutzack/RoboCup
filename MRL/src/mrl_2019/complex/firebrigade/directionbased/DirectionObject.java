package mrl_2019.complex.firebrigade.directionbased;

import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 12/7/13
 * Time: 10:35 AM
 *
 * @Author: Mostafa Shabani
 */
public class DirectionObject {
    public DirectionObject(Polygon polygon, DirectionSide side) {
        this.polygon = polygon;
        this.side = side;
    }

    Polygon polygon;
    DirectionSide side;
    Double value;
    int gasStationNo = 0;
    int refugeNo = 0;

    public Polygon getPolygon() {
        return polygon;
    }

    public void setPolygon(Polygon polygon) {
        this.polygon = polygon;
    }

    public DirectionSide getSide() {
        return side;
    }

    public void setSide(DirectionSide side) {
        this.side = side;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public int getGasStationNo() {
        return gasStationNo;
    }

    public void setGasStationNo(int gasStationNo) {
        this.gasStationNo = gasStationNo;
    }

    public int getRefugeNo() {
        return refugeNo;
    }

    public void setRefugeNo(int refugeNo) {
        this.refugeNo = refugeNo;
    }
}
