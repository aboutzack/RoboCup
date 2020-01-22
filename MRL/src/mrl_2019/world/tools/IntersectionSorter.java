package mrl_2019.world.tools;

import rescuecore2.misc.Pair;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 2/13/13
 * Time: 6:20 PM
 */
public class IntersectionSorter implements Comparator<Pair<LineInfo, Double>>, java.io.Serializable {
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