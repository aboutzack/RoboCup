package AUR.util.aslan;

import adf.agent.info.WorldInfo;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Collection;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Edge;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Amir Aslan Aslani - Mar 2018
 */
public class AURMergedPolygon {
        
        Polygon polygon = new Polygon();
        ArrayList<Edge> edges = new ArrayList<>();

        public AURMergedPolygon(Collection<rescuecore2.standard.entities.Area> areas, WorldInfo wi) {
                ArrayList<Shape> areasShape = new ArrayList<>();
                
                for(rescuecore2.standard.entities.Area area : areas){
                        areasShape.add(area.getShape());
                        for(Edge e : area.getEdges()){
                                if(! e.isPassable()){
                                        edges.add(e);
                                }
                        }
                }
                
                Area area = createAreaFromCollectionOfShapes(areasShape);
                this.polygon = createPolygonFromArea(area);
        }
        
        public Area createAreaFromCollectionOfShapes(Collection<Shape> shapes) {
                Area area = new Area();
                for(Shape shape : shapes){
                        area.add(new Area(shape));
                }
                
                return area;
        }
        
        private Polygon createPolygonFromArea(Area area) {
                Polygon polygon = new Polygon();
                
                PathIterator iterator = area.getPathIterator(null);
                float[] floats = new float[6];
                
                while (!iterator.isDone()) {
                        int type = iterator.currentSegment(floats);
                        int x = (int) floats[0];
                        int y = (int) floats[1];
                        if (type != PathIterator.SEG_CLOSE) {
                                polygon.addPoint(x, y);
                        }
                        iterator.next();
                }
                
                return polygon;
        }
        
        
}
