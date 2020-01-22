package mrl_2019.complex.search;

import javolution.util.FastSet;
import mrl_2019.complex.firebrigade.FireBrigadeTarget;
import mrl_2019.world.MrlWorldHelper;

import mrl_2019.world.entity.MrlBuilding;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 11/26/13
 * Time: 3:21 PM
 *
 * @Author: Mostafa Shabani
 */
public class ExploreAroundFireDecisionMaker /*extends SearchDecisionMaker*/ {
    protected MrlWorldHelper world;
    public ExploreAroundFireDecisionMaker(MrlWorldHelper world) {
        this.world = (world);
        maximalCovering = new MaximalCovering(world);
    }

    private MaximalCovering maximalCovering;
    private FireBrigadeTarget fireBrigadeTarget;
    private Set<EntityID> exploreTargets;
    private EntityID target;

    public void update() {
//        super.update();
        exploreTargets.remove(world.getSelfPosition().getID());
        if (world.getSelfPosition().getID().equals(target)) {
            target = null;
            //MrlPersonalData.VIEWER_DATA.removeExplorePosition(world.getSelf().getID(), world.getSelfPosition());
        }
    }

    public void setTargetFire(FireBrigadeTarget fireBrigadeTarget, boolean exploreAll) {
        this.fireBrigadeTarget = fireBrigadeTarget;
        fillExplorePlaces(exploreAll);
    }

    /**
     * this method find all areas for explore in this cluster.
     * we now use a simple area selector for this method. but M.Amin try to find best solution. ;)
     *
     * @param exploreAll for explore all of fire cluster
     */
    private void fillExplorePlaces(boolean exploreAll) {
        Set<MrlBuilding> borderBuildings = new FastSet<MrlBuilding>();
        Set<MrlBuilding> allBuildings = new FastSet<MrlBuilding>();

        borderBuildings.add(fireBrigadeTarget.getMrlBuilding());
        allBuildings.add(fireBrigadeTarget.getMrlBuilding());

        if (exploreAll) {
            Set<StandardEntity> entitySet = new FastSet<StandardEntity>(fireBrigadeTarget.getCluster().getBorderEntities());
            entitySet.removeAll(fireBrigadeTarget.getCluster().getIgnoredBorderEntities());

            for (StandardEntity entity : entitySet) {
                borderBuildings.add(world.getMrlBuilding(entity.getID()));
                allBuildings.add(world.getMrlBuilding(entity.getID()));
            }
        }
        for (MrlBuilding neighbour : borderBuildings) {
            for (MrlBuilding b : neighbour.getConnectedBuilding()) {

                if (world.getDistance(b.getSelfBuilding(), neighbour.getSelfBuilding()) < world.getViewDistance()) {
                    allBuildings.add(b);
                }
            }
        }

        exploreTargets = maximalCovering.findMaximalCovering(allBuildings);
        //MrlPersonalData.VIEWER_DATA.setExplorePositions(world.getSelf().getID(), exploreTargets, world);
        target = null;
    }

    public void removeUnreachableArea(EntityID areaId) {
        exploreTargets.remove(areaId);
        target = null;

        //MrlPersonalData.VIEWER_DATA.removeExplorePosition(world.getSelf().getID(), world.getEntity(areaId));
    }

    public Set<EntityID> getExploreTargets() {
        return exploreTargets;
    }

    public void initialize() {
        exploreTargets = new HashSet<EntityID>();
        update();
    }

    public Area getNextArea() {
        if (exploreTargets != null) {
            update();
            if (target == null) {
                int distance;
                int minDistance = Integer.MAX_VALUE;
                for (EntityID areaId : exploreTargets) {
                    distance = world.getDistance(world.getAgentInfo().getID(), areaId);
                    if (distance < minDistance) {
                        minDistance = distance;
                        target = areaId;
                    }
                }
            }
        }
        if (target != null) {
            Area area = (Area) world.getEntity(target);
//            //MrlPersonalData.VIEWER_DATA.setExploreTarget(world.getSelf().getID(), area);
            return area;
        }
        return null;
    }
}
