package mrl_2019.complex.firebrigade;

import javolution.util.FastSet;
import mrl_2019.algorithm.clustering.Cluster;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 11/20/13
 * Time: 6:12 PM
 *
 * @Author: Mostafa Shabani
 */
public class IdBasedExploreLeaderSelector implements IExploreLeaderSelector {
    public IdBasedExploreLeaderSelector(MrlFireBrigadeWorld world) {
        this.world = world;
    }

    private MrlFireBrigadeWorld world;

    @Override
    public EntityID findLeader(FireBrigadeTarget fireBrigadeTarget) {
        EntityID smallestId = null;

        if (world.getDistance(world.getAgentInfo().getID(),fireBrigadeTarget.getMrlBuilding().getID()) > world.getMaxExtinguishDistance()) {
            return smallestId;
        }

        for (FireBrigade next : world.getFireBrigadesSeen()) {
            if (smallestId == null || smallestId.getValue() < next.getID().getValue()) {
                smallestId = next.getID();
            }
        }

//        world.printData("cluster : " + cluster.getId() + "  selected:" + smallestId + "  from agents=" + world.getFireBrigadesSeen().toString());
        return smallestId;
    }

    private Set<EntityID> findAgentInThisCluster(Cluster cluster) {
        Set<EntityID> fbInThisCluster = new FastSet<EntityID>();
        Polygon bigPolyForAgents = getClusterBigPolygon(cluster);

        for (StandardEntity next : world.getFireBrigadeList()) {
            if (agentInThisClusterPolygon((FireBrigade) next, bigPolyForAgents)) {
                fbInThisCluster.add(next.getID());
            }
        }
        return fbInThisCluster;
    }

    private Polygon getClusterBigPolygon(Cluster cluster) {
        Polygon pp = cluster.getConvexHullObject().getConvexPolygon();
        double x = Math.min(pp.getBounds2D().getWidth(), pp.getBounds2D().getHeight());
        double scale = (x + world.getMaxExtinguishDistance() + (world.getMaxExtinguishDistance() / 3)) / x;
        Polygon bigPolyForAgents = Cluster.scalePolygon(pp, scale);

        return bigPolyForAgents;
    }

    private boolean agentInThisClusterPolygon(FireBrigade fireBrigade, Polygon polygon) {
        return polygon.contains(fireBrigade.getX(), fireBrigade.getY());
    }
}
