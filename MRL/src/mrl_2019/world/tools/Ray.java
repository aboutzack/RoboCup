package mrl_2019.world.tools;

import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Line2D;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 2/13/13
 * Time: 6:18 PM
 */
public class Ray implements Serializable {
    /**
     * The ray itself.
     */
    private Line2D ray;
    /**
     * The visible length of the ray.
     */
    private double length;
    /**
     * List of lines hit in order.
     */
    private List<LineInfo> hit;


    public Ray(Line2D ray, Collection<LineInfo> otherLines) {
        this.ray = ray;
        List<Pair<LineInfo, Double>> intersections = new ArrayList<Pair<LineInfo, Double>>();
        // Find intersections with other lines
        for (LineInfo other : otherLines) {
            double d1 = ray.getIntersection(other.getLine());
            double d2 = other.getLine().getIntersection(ray);
            if (d2 >= 0 && d2 <= 1 && d1 > 0 && d1 <= 1) {
                intersections.add(new Pair<LineInfo, Double>(other, d1));
            }
        }
        IntersectionSorter intersectionSorter = new IntersectionSorter();

        Collections.sort(intersections, intersectionSorter);
        hit = new ArrayList<LineInfo>();
        length = 1;
        for (Pair<LineInfo, Double> next : intersections) {
            LineInfo l = next.first();
            hit.add(l);
            if (l.isBlocking()) {
                length = next.second();
                break;
            }
        }
    }

    public Line2D getRay() {
        return ray;
    }

    public double getVisibleLength() {
        return length;
    }

    public List<LineInfo> getLinesHit() {
        return Collections.unmodifiableList(hit);
    }

}
