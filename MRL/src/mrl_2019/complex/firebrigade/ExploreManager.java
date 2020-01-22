package mrl_2019.complex.firebrigade;

import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.module.complex.Search;
import mrl_2019.complex.search.ExploreAroundFireDecisionMaker;
import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 11/26/13
 * Time: 7:55 PM
 *
 * @Author: Mostafa Shabani
 */
public abstract class ExploreManager extends Search {
    protected ExploreManager(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai,wi,si,moduleManager,developData);
        this.world = (MrlFireBrigadeWorld) MrlFireBrigadeWorld.load(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
        leaderSelector = new IdBasedExploreLeaderSelector(world);
//        exploreDecisionMaker = world.getPlatoonAgent().exploreAroundFireSearchManager.getDecisionMaker();
        exploreDecisionMaker = new ExploreAroundFireDecisionMaker(world);//todo consider on following info.
//        MrlPersonalData.VIEWER_DATA.print("Decision maker may need to be taken from Agent to prevent duplicate process.");
    }

    public Logger LOGGER = Logger.getLogger(ExploreManager.class);
    protected MrlFireBrigadeWorld world;
    protected ExploreAroundFireDecisionMaker exploreDecisionMaker;
    protected IExploreLeaderSelector leaderSelector;
    protected FireBrigadeTarget lastFireBrigadeTarget = null;


    /**
     * This method say when agent can go to exploring in fire
     *
     * @param fireBrigadeTarget target of this agent
     * @return is time to explore or not
     */
    public abstract boolean isTimeToExplore(FireBrigadeTarget fireBrigadeTarget);


}
