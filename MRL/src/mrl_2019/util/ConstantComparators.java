package mrl_2019.util;

import mrl_2019.algorithm.clustering.FireCluster;
import mrl_2019.world.entity.Path;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.Comparator;

/**
 * User: mrl_2019
 * Date: May 6, 2010
 * Time: 7:05:42 PM
 */
public class ConstantComparators {

//    public static java.util.Comparator<FireCluster> FIRE_CLUSTER_VALUE_COMPARATOR = new java.util.Comparator<FireCluster>() {
//        public int compare(FireCluster f1, FireCluster f2) {
//            if (f1.getValue() > f2.getValue())
//                return 1;
//            if (f1.getValue() == f2.getValue())
//                return 0;
//
//            return -1;
//        }
//    };


    public static Comparator<StandardEntity> ID_COMPARATOR = new Comparator<StandardEntity>() {
        public int compare(StandardEntity r1, StandardEntity r2) {

            if (r1.getID().getValue() > r2.getID().getValue())
                return 1;
            if (r1.getID().getValue() == r2.getID().getValue())
                return 0;

            return -1;
        }
    };
    public static Comparator<EntityID> EntityID_COMPARATOR = new Comparator<EntityID>() {
        public int compare(EntityID r1, EntityID r2) {

            if (r1.getValue() > r2.getValue())
                return 1;
            if (r1.getValue() == r2.getValue())
                return 0;

            return -1;
        }
    };
    public static Comparator<Path> PATH_ID_COMPARATOR = new Comparator<Path>() {
        public int compare(Path r1, Path r2) {

            if (r1.getId().getValue() > r2.getId().getValue())
                return 1;
            if (r1.getId().getValue() == r2.getId().getValue())
                return 0;

            return -1;
        }
    };
    public static Comparator<Integer> VALUE_COMPARATOR = new Comparator<Integer>() {
        public int compare(Integer r1, Integer r2) {

            if (r1 < r2)
                return 1;
            if (r1.equals(r2))
                return 0;

            return -1;
        }
    };


    public static Comparator<FireCluster> FIRE_CLUSTER_VALUE_COMPARATOR = new Comparator<FireCluster>() {
        public int compare(FireCluster f1, FireCluster f2) {

            if (f1.getValue() > f2.getValue())
                return 1;
            if (f1.getValue() == f2.getValue())
                return 0;

            return -1;
        }
    };

    public static Comparator<Pair<EntityID, Integer>> BID_VALUE_COMPARATOR = new Comparator<Pair<EntityID, Integer>>() {
        public int compare(Pair<EntityID, Integer> o1, Pair<EntityID, Integer> o2) {
            int l1 = o1.second();
            int l2 = o2.second();
            if (l1 < l2)       //decrease
                return 1;
            if (l1 == l2)
                return 0;

            return -1;
        }
    };

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
    public static Comparator<Pair<Point, Double>> TARGET_POINT_VALUE_COMPARATOR = new Comparator<Pair<Point, Double>>() {
        @Override
        public int compare(Pair<Point, Double> o1, Pair<Point, Double> o2) {
            double l1 = o1.second();
            double l2 = o2.second();
            if (l1 < l2) //DESC
                return 1;
            if (l1 == l2)
                return 0;

            return -1;
        }
    };

    public static Comparator<Pair<Road, Integer>> DISTANCE_ENTRANCE_VALUE_COMPARATOR = new Comparator<Pair<Road, Integer>>() {
        public int compare(Pair<Road, Integer> o1, Pair<Road, Integer> o2) {
            int l1 = o1.second();
            int l2 = o2.second();
            if (l1 > l2) //Increase
                return 1;
            if (l1 == l2)
                return 0;

            return -1;
        }
    };


    public static Comparator<int[]> COST_VALUE_COMPARATOR = new Comparator<int[]>() {
        public int compare(int[] o1, int[] o2) {
            int l1 = o1[1];
            int l2 = o2[1];
            if (l1 > l2) //Increase
                return 1;
            if (l1 == l2)
                return 0;

            return -1;
        }
    };

    public static Comparator<StandardEntity> VICTIM_BURIENDNESS_COMPARATOR = new Comparator<StandardEntity>() {
        public int compare(StandardEntity r1, StandardEntity r2) {

            Human h1 = (Human) r1;
            Human h2 = (Human) r2;

            if (h1.getBuriedness() > h2.getBuriedness())
                return 1;
            if (h1.getBuriedness() == h2.getBuriedness())
                return 0;

            return -1;
        }
    };


}
