package AUR.util.ambulance.sightAreaLegacy;


import java.awt.*;
import java.util.ArrayList;

/**
 * Created by armanaxh on 12/21/17.
 */
public class SightPolygon {

    private ArrayList<Polygon> element;

    public SightPolygon()
    {
        element = new ArrayList<>();
    }
    public void addPolygon(int xPoint[] ,int yPoint[] , int nPoint){
        element.add(new Polygon(xPoint,yPoint,nPoint));
    }
    public void addPolygon(Polygon polygon){
        element.add(polygon);
    }
    public ArrayList<Polygon> getPolygons(){
        return element;
    }

}
