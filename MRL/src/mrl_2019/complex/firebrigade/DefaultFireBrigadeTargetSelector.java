package mrl_2019.complex.firebrigade;

import adf.agent.info.AgentInfo;
import mrl_2019.algorithm.clustering.FireCluster;
import mrl_2019.complex.firebrigade.directionbased.MrlFireBrigadeDirectionManager;
import mrl_2019.world.entity.MrlBuilding;
import rescuecore2.standard.entities.Human;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 3/11/13
 * Time: 6:25 PM
 * Author: Mostafa Movahedi
 */
public abstract class DefaultFireBrigadeTargetSelector implements IFireBrigadeTargetSelector {

    protected MrlFireBrigadeWorld world;
    protected Human selfHuman;
    protected FireBrigadeUtilities fireBrigadeUtilities;
    protected MrlFireBrigadeDirectionManager directionManager;

    protected MrlBuilding target;
    protected MrlBuilding lastTarget;
    protected FireCluster targetCluster;

    protected DefaultFireBrigadeTargetSelector(MrlFireBrigadeWorld world, AgentInfo agentInfo) {
        this.world = world;
        this.fireBrigadeUtilities = new FireBrigadeUtilities(world);
        this.directionManager = new MrlFireBrigadeDirectionManager(world,agentInfo);
    }


}
