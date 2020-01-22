package mrl_2019.complex.firebrigade;

import adf.agent.action.fire.ActionExtinguish;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.Search;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

import java.util.List;

import static rescuecore2.standard.entities.StandardEntityURN.AMBULANCE_TEAM;
import static rescuecore2.standard.entities.StandardEntityURN.FIRE_BRIGADE;
import static rescuecore2.standard.entities.StandardEntityURN.POLICE_FORCE;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 11/26/13
 * Time: 7:55 PM
 *
 * @Author: Mostafa Shabani
 */
public class TargetBaseExploreManager extends ExploreManager {
    private PathPlanning pathPlanning;

    public TargetBaseExploreManager(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);

        StandardEntityURN agentURN = ai.me().getStandardURN();
        switch (si.getMode()) {
            case PRECOMPUTATION_PHASE:
                if (agentURN == AMBULANCE_TEAM) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Ambulance", "adf.sample.module.algorithm.SamplePathPlanning");
                } else if (agentURN == FIRE_BRIGADE) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire", "adf.sample.module.algorithm.SamplePathPlanning");
                } else if (agentURN == POLICE_FORCE) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Police", "adf.sample.module.algorithm.SamplePathPlanning");
                }
                break;
            case PRECOMPUTED:
                if (agentURN == AMBULANCE_TEAM) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Ambulance", "adf.sample.module.algorithm.SamplePathPlanning");
                } else if (agentURN == FIRE_BRIGADE) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire", "adf.sample.module.algorithm.SamplePathPlanning");
                } else if (agentURN == POLICE_FORCE) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Police", "adf.sample.module.algorithm.SamplePathPlanning");
                }
                break;
            case NON_PRECOMPUTE:
                if (agentURN == AMBULANCE_TEAM) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Ambulance", "adf.sample.module.algorithm.SamplePathPlanning");
                } else if (agentURN == FIRE_BRIGADE) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire", "adf.sample.module.algorithm.SamplePathPlanning");
                } else if (agentURN == POLICE_FORCE) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Police", "adf.sample.module.algorithm.SamplePathPlanning");
                }
                break;
        }
    }

    protected FireBrigadeTarget exploringTarget = null;
    protected Area preFBTargetArea = null;
    private int timeToExtinguish = MAX_TIME_TO_EXTINGUISH;
    private static final int MAX_TIME_TO_EXTINGUISH = 4;
    private static final int MAX_TIME_TO_EXTINGUISH_CL = 4;
    private boolean reachedToFirstTarget = true;

    /**
     * In this strategy fireBrigade "MAX_TIME_TO_EXTINGUISH" to extinguish only.
     * if time to extinguish finished and target is not extinguished FB should be goto explore for all explore targets and back to extinguish work.
     * zamani ke map CL bashad har 4 cycle explore mikonad. va dar gheire pas az 6 cycle extinguish momtad.
     *
     * @param fireBrigadeTarget target of this agent
     * @return boolean is true equals now goto explore
     */
    public boolean isTimeToExplore(FireBrigadeTarget fireBrigadeTarget) {
        boolean returnVal = false;
        Area area = exploreDecisionMaker.getNextArea();

        if (lastFireBrigadeTarget != null) {
            Integer lastClusterId = lastFireBrigadeTarget.getCluster().getId();
            Integer exploringClusterId = (exploringTarget == null ? null : exploringTarget.getCluster().getId());
            Integer newClusterId = (fireBrigadeTarget == null ? null : fireBrigadeTarget.getCluster().getId());
            boolean targetChanged = !lastClusterId.equals(newClusterId) || (exploringClusterId != null && !exploringClusterId.equals(newClusterId));
            boolean fireIsNear = fireBrigadeTarget != null && world.getDistance(fireBrigadeTarget.getMrlBuilding().getID(), world.getSelfPosition().getID()) <= world.getViewDistance();


            if (!reachedToFirstTarget && area != null && !fireIsNear) {
                if (preFBTargetArea != null && preFBTargetArea.getID().equals(world.getSelfPosition().getID())) {
                    reachedToFirstTarget = true;
//                    world.printData("reachedToFirstTarget " + preFBTargetArea);
                } else {
                    lastFireBrigadeTarget = fireBrigadeTarget;
                    preFBTargetArea = area;
                    return true;
                }
            }

            if (targetChanged && area != null && !fireIsNear) {
                lastFireBrigadeTarget = fireBrigadeTarget;
//                world.printData("continue explore goto " + area);
                return true;
            }

            EntityID clusterLeaderId = leaderSelector.findLeader(lastFireBrigadeTarget);
            if (clusterLeaderId != null && world.getAgentInfo().getID().equals(clusterLeaderId)) {

                if (fireBrigadeTarget == null || targetChanged || timeToExtinguish == 0) {
                    timeToExtinguish = MAX_TIME_TO_EXTINGUISH;
                    if (world.isCommunicationLess()) {
                        timeToExtinguish = MAX_TIME_TO_EXTINGUISH_CL;
                    }
                    exploreDecisionMaker.setTargetFire(lastFireBrigadeTarget, fireBrigadeTarget == null);
                    preFBTargetArea = exploreDecisionMaker.getNextArea();
                    exploringTarget = lastFireBrigadeTarget;
                    reachedToFirstTarget = false;
                    returnVal = true;
//                    LOGGER.debug("now I'm goto exploring cluster " + lastFireBrigadeTarget.getCluster().getId() + " places:" + exploreTargets);
//                    world.printData("now I'm goto exploring cluster " + lastFireBrigadeTarget.getCluster().getId() + "  places: " + exploreDecisionMaker.getExploreTargets());
                } else {
                    ExtAction extAction = world.getModuleManager().getExtAction("TacticsFireBrigade.ActionFireFighting", "adf.sample.extaction.ActionFireFighting");
                    boolean wasExtinguish = extAction != null && extAction.getAction() != null && (extAction.getAction() instanceof ActionExtinguish);
                    if (wasExtinguish) {
                        timeToExtinguish--;
                    } else {
                        timeToExtinguish = MAX_TIME_TO_EXTINGUISH;
                    }
                }
            }
        } else if (area != null) {
            returnVal = true;
        }
        lastFireBrigadeTarget = fireBrigadeTarget;
//        MrlPersonalData.VIEWER_DATA.setExploreBuildings(world.getSelf().getID(), new FastSet<MrlBuilding>());

        return returnVal;
    }

    private void initialize() {
        exploreDecisionMaker.initialize();
    }

    @Override
    public Search updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (getCountUpdateInfo() >= 2) {
            return this;
        }

        this.world.updateInfo(messageManager);
        this.pathPlanning.updateInfo(messageManager);
        return null;
    }

//    @Override
//    public void execute() {
//        exploreDecisionMaker.getNextArea()
//    }

    static int tryCount = 0;
    static int lastTime = 0;
    protected Area targetArea = null;
    protected EntityID target = null;


    @Override
    public Search calc() {
        execute();
        return this;
    }

    @Override
    public EntityID getTarget() {
        return target;
    }

    @Override
    public Search precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (getCountPrecompute() >= 2) {
            return this;
        }
        this.world.precompute(precomputeData);
        this.pathPlanning.precompute(precomputeData);
        this.initialize();
        return this;
    }

    @Override
    public Search resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (getCountResume() >= 2) {
            return this;
        }
        this.world.resume(precomputeData);
        this.pathPlanning.resume(precomputeData);

        this.initialize();
        return this;
    }

    @Override
    public Search preparate() {
        super.preparate();
        if (getCountPreparate() >= 2) {
            return this;
        }
        this.world.preparate();
        this.pathPlanning.preparate();

        this.initialize();
        return this;
    }


    private void execute() {
        if (lastTime != world.getTime()) {
            tryCount = 0;
            lastTime = world.getTime();
        }
        tryCount++;
        if (tryCount > 5) {
            tryCount = 0;
            return;
        }
        exploreDecisionMaker.update();

        targetArea = exploreDecisionMaker.getNextArea();
        if (targetArea == null) {
//            LOGGER.debug("targetArea is null");
            target = null;
            return;
        }

        target = targetArea.getID();

        //TODO @MRL Check black list targets.
        List<EntityID> path = pathPlanning.setFrom(agentInfo.getPosition()).setDestination(target).calc().getResult();
        if (path == null) {
            exploreDecisionMaker.removeUnreachableArea(target);
            target = null;
            execute();
        }

//        SearchStatus status = searchStrategy.manualMoveToArea(targetArea);

//        if (status == CANCELED) {
//            exploreDecisionMaker.removeUnreachableArea(targetArea.getID());
//            execute();
////            LOGGER.debug("unreachable area for search: " + targetArea);
//        }
//
//        if (status == FINISHED) {
////            LOGGER.debug("explored area = " + targetArea);
//            world.printData("explored area = " + targetArea);
//            execute();
//        } else if (status == SEARCHING) {
//            //Do Nothing
//        }
    }
}
