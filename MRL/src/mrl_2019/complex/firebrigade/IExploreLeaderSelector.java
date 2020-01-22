package mrl_2019.complex.firebrigade;

import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 11/23/13
 * Time: 3:50 PM
 *
 * @Author: Mostafa Shabani
 */
public interface IExploreLeaderSelector {
    /**
     * this method finds a leader for exploration around the fire.
     *
     * @param fireBrigadeTarget extinguishing target
     * @return EntityId of the cluster leader
     */
    public EntityID findLeader(FireBrigadeTarget fireBrigadeTarget);
}
