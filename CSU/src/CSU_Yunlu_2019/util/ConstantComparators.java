package CSU_Yunlu_2019.util;

import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.EntityID;

import java.util.Comparator;

public class ConstantComparators {
    public static Comparator<Pair<EntityID, Integer>> DISTANCE_VALUE_COMPARATOR = new Comparator<Pair<EntityID, Integer>>() {
        @Override
        public int compare(Pair<EntityID, Integer> o1, Pair<EntityID, Integer> o2) {
            int l1 = o1.second();
            int l2 = o2.second();
            if (l1 > l2) //Increase
                return 1;
            if (l1 == l2)
                return 0;

            return -1;
        }
    };

    public static Comparator<Pair<EntityID, Double>> DISTANCE_VALUE_COMPARATOR_DOUBLE = new Comparator<Pair<EntityID, Double>>() {
        @Override
        public int compare(Pair<EntityID, Double> o1, Pair<EntityID, Double> o2) {
            double l1 = o1.second();
            double l2 = o2.second();
            if (l1 > l2) //Increase
                return 1;
            if (l1 == l2)
                return 0;

            return -1;
        }
    };
}
