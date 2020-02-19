package AUR.util.ambulance.sightAreaLegacy;

import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.StandardEntity;

import java.util.*;

/**
 * Created by armanaxh on 12/23/17.
 */
public class RayIntersection {

    private Collection<LineInfo> entityLines;
    private WorldInfo worldInfo;
    private final int perceptionMax;
    private static final IntersectionSorter INTERSECTION_SORTER = new IntersectionSorter();

    public RayIntersection(WorldInfo wi, ScenarioInfo si) {
        this.worldInfo = wi;
        this.perceptionMax = si.getPerceptionLosMaxDistance();
    }

    public Line2D Calc(Line2D ray) {

        List<Pair<LineInfo, Double>> intersections = new ArrayList<Pair<LineInfo, Double>>();
        // Find intersections with other lines
        for (LineInfo other : this.entityLines) {
            double d1 = ray.getIntersection(other.getLine());
            double d2 = other.getLine().getIntersection(ray);
            if (d2 >= 0 && d2 <= 1 && d1 > 0 && d1 <= 1) {
                intersections.add(new Pair<LineInfo, Double>(other, d1));
            }
        }
        if(intersections.isEmpty()){
            return ray;
        }
        Collections.sort(intersections, INTERSECTION_SORTER);
        double length = intersections.get(0).second();
        if (length >= 1) {
            return ray;
        }
        return new Line2D(ray.getOrigin(),ray.getDirection().scale(length));
    }


    public RayIntersection findEntityLineInPerseptionRange(Point2D p){
        Collection<StandardEntity> entity = worldInfo.getObjectsInRange((int)p.getX(),(int)p.getY(),perceptionMax);
        this.calcEntityLine(entity);
        return this;
    }
    public RayIntersection calcEntityLine(Collection<StandardEntity> entities) {
        Collection<LineInfo> result = new HashSet<LineInfo>();
        for (StandardEntity next : entities) {
            if (next instanceof Building) {
                for (Edge edge : ((Building)next).getEdges()) {
                    if (!edge.isPassable()) {
                        Line2D line = edge.getLine();
                        result.add(new LineInfo(line, next, !edge.isPassable()));
                    }
                }
            }
        }
        this.entityLines = result;
        return this;
    }
    private static class LineInfo {
        private Line2D line;
        private StandardEntity entity;
        private boolean blocking;

        public LineInfo(Line2D line, StandardEntity entity, boolean blocking) {
            this.line = line;
            this.entity = entity;
            this.blocking = blocking;
        }

        public Line2D getLine() {
            return line;
        }

        public StandardEntity getEntity() {
            return entity;
        }

        public boolean isBlocking() {
            return blocking;
        }
    }

    private static class IntersectionSorter implements Comparator<Pair<LineInfo, Double>>, java.io.Serializable {
        @Override
        public int compare(Pair<LineInfo, Double> a, Pair<LineInfo, Double> b) {
            double d1 = a.second();
            double d2 = b.second();
            if (d1 < d2) {
                return -1;
            }
            if (d1 > d2) {
                return 1;
            }
            return 0;
        }
    }

}
