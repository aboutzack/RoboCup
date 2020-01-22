package mrl_2019.util;

import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.properties.IntProperty;

import java.util.*;
import java.util.List;

/**
 * Line of sight perception.
 */
public class MrlLineOfSightPerception {
    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(MrlLineOfSightPerception.class);

    private static final int DEFAULT_VIEW_DISTANCE = 30000;
    private static final int DEFAULT_HP_PRECISION = 1000;
    private static final int DEFAULT_DAMAGE_PRECISION = 100;
    private static final int DEFAULT_RAY_COUNT = 72;

    private static final String VIEW_DISTANCE_KEY = "perception.los.max-distance";
    private static final String RAY_COUNT_KEY = "perception.los.ray-count";
    private static final String HP_PRECISION_KEY = "perception.los.precision.hp";
    private static final String DAMAGE_PRECISION_KEY = "perception.los.precision.damage";

//    private static final IntersectionSorter INTERSECTION_SORTER = new IntersectionSorter();

    private int rayCount;
    private List<MrlRay> rays = new ArrayList<>();

//    private Config config;

    private WorldInfo world;
    private ScenarioInfo seScenarioInfo;


    /**
     * Create a LineOfSightPerception object.
     */
    public MrlLineOfSightPerception(WorldInfo worldInfo, ScenarioInfo scenarioInfo) {
        this.world = worldInfo;
        this.seScenarioInfo=scenarioInfo;
        this.rayCount = 36;

    }


    @Override
    public String toString() {
        return "Line of sight perception";
    }

    public Set<EntityID> getVisibleAreas(EntityID areaID) {
        Area area = (Area) world.getEntity(areaID);
        Set<EntityID> result = new HashSet<>();
        // Look for objects within range
        Pair<Integer, Integer> location = world.getLocation(area.getID());
        if (location != null) {
            Point2D point = new Point2D(location.first(), location.second());
            Collection<StandardEntity> nearby = world.getObjectsInRange(location.first(), location.second(), seScenarioInfo.getPerceptionLosMaxDistance());
            Collection<StandardEntity> visible = findVisibleAreas(area, point, nearby);
            for (StandardEntity next : visible) {
                if (next instanceof Area) {
                    result.add(next.getID());
                }
            }
        }
        return result;
    }

    public List<MrlRay> getRays() {
        return rays;
    }

    private void roundProperty(IntProperty p, int precision) {
        if (precision != 1 && p.isDefined()) {
            p.setValue(round(p.getValue(), precision));
        }
    }

    private int round(int value, int precision) {
        int remainder = value % precision;
        value -= remainder;
        if (remainder >= precision / 2) {
            value += precision;
        }
        return value;
    }

    private Collection<StandardEntity> findVisibleAreas(Area area, Point2D location, Collection<StandardEntity> nearby) {
        Collection<LineInfo> lines = getAllLines(nearby);
        // Cast rays
        // CHECKSTYLE:OFF:MagicNumber
        double dAngle = Math.PI * 2 / rayCount;
        // CHECKSTYLE:ON:MagicNumber
        Collection<StandardEntity> result = new HashSet<StandardEntity>();
        rays.clear();
        for (int i = 0; i < rayCount; ++i) {
            double angle = i * dAngle;
            Vector2D vector = new Vector2D(Math.sin(angle), Math.cos(angle)).scale(seScenarioInfo.getPerceptionLosMaxDistance());
            Ray ray = new Ray(new Line2D(location, vector), lines);
            rays.add(new MrlRay(ray.getRay()));
            for (LineInfo hit : ray.getLinesHit()) {
                StandardEntity e = hit.getEntity();
                result.add(e);
            }
        }
        return result;
    }

    private Collection<LineInfo> getAllLines(Collection<StandardEntity> entities) {
        Collection<LineInfo> result = new HashSet<LineInfo>();
        for (StandardEntity next : entities) {
            if (next instanceof Building) {
                for (Edge edge : ((Building) next).getEdges()) {
                    Line2D line = edge.getLine();
                    result.add(new LineInfo(line, next, !edge.isPassable()));
                }
            }
            if (next instanceof Road) {
                for (Edge edge : ((Road) next).getEdges()) {
                    Line2D line = edge.getLine();
                    result.add(new LineInfo(line, next, false));
                }
            } else if (next instanceof Blockade) {
                int[] apexes = ((Blockade) next).getApexes();
                List<Point2D> points = GeometryTools2D.vertexArrayToPoints(apexes);
                List<Line2D> lines = GeometryTools2D.pointsToLines(points, true);
                for (Line2D line : lines) {
                    result.add(new LineInfo(line, next, false));
                }
            } else {
                continue;
            }
        }
        return result;
    }
}