package AUR.util.aslan;

import AUR.util.knd.AURConstants;
import AUR.util.knd.AURGeoUtil;
import adf.agent.info.AgentInfo;
import adf.agent.info.WorldInfo;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collection;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Edge;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Amir Aslan Aslani - Mar 2018
 */
public class AURPoliceUtil {
        public static ArrayList<Point2D> getAreaGuidPoints(Polygon polygon, Point2D p1, Point2D p2, Collection<Edge> passableEdges, Collection<Edge> notPassableEdges) {
                ArrayList<Point2D> points = new ArrayList<>();
                
                points.add(p1);
                points.add(p2);

                for (Edge e : passableEdges) {
                        points.add(AURGeoTools.getEdgeMid(e));
                }

                for (int i = 1; i < polygon.npoints; i ++) {
                        for (int j = 0; j < i; j ++) {
                                if (Math.abs(j - i) > 2
                                        && polygon.contains(
                                                (polygon.xpoints[i] + polygon.xpoints[j]) / 2,
                                                (polygon.ypoints[i] + polygon.ypoints[j]) / 2
                                        )) {
                                        
                                        points.add(new Point2D(
                                                (polygon.xpoints[i] + polygon.xpoints[j]) / 2,
                                                (polygon.ypoints[i] + polygon.ypoints[j]) / 2
                                        ));
                                }
                        }
                }

                int matrix[][] = new int[points.size()][points.size()];
                for (int i = 0; i < points.size(); i++) {
                        for (int j = 0; j < points.size(); j++) {
                                if (i != j
                                        && !AURGeoTools.intersectOrContains(
                                                getClearPolygon(points.get(i), points.get(j)),
                                                notPassableEdges
                                        )) {
                                        matrix[i][j] = matrix[j][i] = (int) AURGeoUtil.dist(points.get(i).getX(), points.get(i).getY(), points.get(j).getX(), points.get(j).getY());
                                } else {
                                        matrix[i][j] = 0;
                                }
                        }
                }

                AURDijkstra dijkstra = new AURDijkstra();
                dijkstra.dijkstra(matrix, 0, points.size());

                if (dijkstra.getDistanceTo(1) == Integer.MAX_VALUE) {
                        return null;
                }

                ArrayList<Integer> nodes = dijkstra.getPathTo(1);
                ArrayList<Point2D> result = new ArrayList<>();

                for (int i = 1; i < nodes.size() - 1; i++) {
                        result.add(
                                points.get(nodes.get(i))
                        );
                }

                return result;
        }
        
        public static Polygon getClearPolygon(Point2D p1, Point2D p2) {
                return AURGeoTools.getClearPolygon(p1, p2, AURConstants.PoliceExtClear.CLEAR_POLYGON_HEIGHT, true);
        }
        
        /**
         * Start of Kandeh's path planning bug ignoring
         * if kandeh fix this bug this section should remove.
         * ;)
         */
        public static ArrayList<EntityID> filterAlirezaPathBug(ArrayList<EntityID> path){
                for(int i = 0;i < path.size() - 1;i ++){
                        if(path.get(i).equals(path.get(i + 1))){
                                path.remove(i);
                                i --;
                        }
                }
                return path;
        }
        /**
         * End of Kandeh's path planning bug ignoring.
         */
        
        /*
         * This method is not tested.
         */
        public static EntityID getAreaEntityIDOfCoordinates(WorldInfo wi, int[] lastPoint) {
                for(EntityID e : wi.getObjectIDsInRange(lastPoint[0], lastPoint[1], 1))
                        return e;
                return null;
        }
        
        public static Blockade getNearestBlockadeToAgentFromList(AgentInfo ai, Collection<Blockade> list){
                Blockade selected = null;
                double dis = Double.MAX_VALUE;
                for(Blockade b : list){
                        double tdis = Math.hypot(ai.getX() - b.getX(), ai.getY() - b.getY());
                        if(tdis < dis){
                                dis = tdis;
                                selected = b;
                        }
                }
                return selected;
        }
        
        public static Blockade getNearestBlockadeToAgentFromList(AgentInfo ai, WorldInfo wi, Collection<EntityID> list){
                Blockade selected = null;
                double dis = Double.MAX_VALUE;
                for(EntityID bid : list){
                        Blockade b = (Blockade) wi.getEntity(bid);
                        double tdis = Math.hypot(ai.getX() - b.getX(), ai.getY() - b.getY());
                        if(tdis < dis){
                                dis = tdis;
                                selected = b;
                        }
                }
                return selected;
        }
}
