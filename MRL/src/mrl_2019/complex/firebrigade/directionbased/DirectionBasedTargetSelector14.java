package mrl_2019.complex.firebrigade.directionbased;

import adf.agent.info.AgentInfo;
import mrl_2019.MRLConstants;
import mrl_2019.algorithm.clustering.Cluster;
import mrl_2019.algorithm.clustering.FireCluster;
import mrl_2019.complex.firebrigade.DefaultFireBrigadeTargetSelector;
import mrl_2019.complex.firebrigade.FireBrigadeTarget;
import mrl_2019.complex.firebrigade.MrlFireBrigadeWorld;
import mrl_2019.complex.firebrigade.ZJUBaseBuildingCostComputer;
import mrl_2019.util.ConstantComparators;
import mrl_2019.world.entity.MrlBuilding;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * <p>
 * dar in strategy faghat building-haye hashie-i ke fieriness-e 1 ya 2 entekhab mishavand. <i>albate in mozu dar {@code hasBuildingInDirection} dar {@link FireCluster} anjam mishavad</i><br/>
 * nokteye digar inke baraye building value-ha az raveshe zju estefade mishavad.<br/>
 * </p>
 * <p>
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 12/13/13
 * Time: 4:45 PM
 * </p>
 *
 * @Author: Mostafa Shabani
 */
public class DirectionBasedTargetSelector14 extends DefaultFireBrigadeTargetSelector {
    public DirectionBasedTargetSelector14(MrlFireBrigadeWorld world, AgentInfo agentInfo) {
        super(world, agentInfo);
        buildingCostComputer = new ZJUBaseBuildingCostComputer(world);
        distanceNormalizer = MRLConstants.MEAN_VELOCITY_OF_MOVING;
    }

    private ZJUBaseBuildingCostComputer buildingCostComputer;
    private double distanceNormalizer;


    @Override
    public FireBrigadeTarget selectTarget(Cluster targetCluster) {
        FireBrigadeTarget fireBrigadeTarget = null;

        if (targetCluster != null) {
            SortedSet<Pair<EntityID, Double>> sortedBuildings;
            sortedBuildings = calculateValue((FireCluster) targetCluster);
//            sortedBuildings = fireBrigadeUtilities.reRankBuildings(sortedBuildings);

//            MrlPersonalData.VIEWER_DATA.setBuildingValues(world.getSelf().getID(), world.getMrlBuildings());

            if (sortedBuildings != null && !sortedBuildings.isEmpty()) {
                lastTarget = target;
                target = world.getMrlBuilding(sortedBuildings.first().first());
                fireBrigadeTarget = new FireBrigadeTarget(target, targetCluster);
            }
        }

        return fireBrigadeTarget;
    }


    private SortedSet<Pair<EntityID, Double>> calculateValue(FireCluster fireCluster) {

        Set<StandardEntity> borderEntities = fireCluster.getBorderEntities();
        SortedSet<Pair<EntityID, Double>> sortedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        List<MrlBuilding> inDirectionBuildings;
        //2014
//        Point targetPoint = directionManager.findDirectionPointInMap(fireCluster, world.getFireClustering().getClusters());
        inDirectionBuildings = fireCluster.getBuildingsInDirection();
        buildingCostComputer.updateFor(fireCluster, lastTarget);

        if (inDirectionBuildings.isEmpty()) {
            calculateValueForOtherBuildings(sortedBuildings, borderEntities);
        }
        calculateValueForInDirectionBuildings(sortedBuildings, inDirectionBuildings);
        return sortedBuildings;
    }

    private void calculateValueForOtherBuildings(SortedSet<Pair<EntityID, Double>> sortedBuildings, Set<StandardEntity> otherBuildings) {
        for (StandardEntity entity : otherBuildings) {
            MrlBuilding b = world.getMrlBuilding(entity.getID());
            b.BUILDING_VALUE = buildingCostComputer.getCost(b);
            sortedBuildings.add(new Pair<EntityID, Double>(b.getID(), b.BUILDING_VALUE));
        }
    }

    private void calculateValueForInDirectionBuildings(SortedSet<Pair<EntityID, Double>> sortedBuildings, List<MrlBuilding> highValueBuildings) {
        for (MrlBuilding b : highValueBuildings) {
            b.BUILDING_VALUE = buildingCostComputer.getCost(b);
            sortedBuildings.add(new Pair<EntityID, Double>(b.getID(), b.BUILDING_VALUE));
        }
    }

}