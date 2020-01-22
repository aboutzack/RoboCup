package mrl_2019.extaction;

import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.PathPlanning;
import mrl_2019.algorithm.SamplePathPlanning;
import mrl_2019.extaction.clear.ClearAreaActExecutor;
import mrl_2019.extaction.clear.ClearBlockadeActExecutor;
import mrl_2019.util.Util;
import mrl_2019.world.MrlWorldHelper;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;

/**
 * 3/3/2017
 *
 * @author Mahdi
 */
public class ActionExtClear extends ExtAction {
    private PathPlanning pathPlanning;
    private EntityID target;
    private boolean onBlockade = true;
    private ClearAreaActExecutor clearAreaActExecutor;
    private ClearBlockadeActExecutor clearBlockadeActExecutor;

    private MrlWorldHelper worldHelper;

    public ActionExtClear(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);


        switch (si.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("ActionExtClear.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("ActionExtClear.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("ActionExtClear.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
        }

        this.worldHelper = MrlWorldHelper.load(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);

        clearAreaActExecutor = new ClearAreaActExecutor(worldHelper, wi, ai, si, pathPlanning);
        clearBlockadeActExecutor = new ClearBlockadeActExecutor(worldHelper, wi, ai, si, pathPlanning);
    }

    @Override
    public ExtAction precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        pathPlanning.precompute(precomputeData);
        worldHelper.precompute(precomputeData);
        return this;
    }

    @Override
    public ExtAction resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
        worldHelper.resume(precomputeData);
        pathPlanning.resume(precomputeData);
        return this;
    }

    @Override
    public ExtAction preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }
        worldHelper.preparate();
        pathPlanning.preparate();
        return this;
    }

    @Override
    public ExtAction updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        worldHelper.updateInfo(messageManager);
        pathPlanning.updateInfo(messageManager);
        return this;
    }

    @Override
    public ExtAction setTarget(EntityID targets) {
        this.target = targets;


        return this;
    }


    private ActionExecutorType getActionExecutorType(List<EntityID> path, EntityID target) {
        ActionExecutorType actionExecutorType;
        StandardEntity entity = worldInfo.getEntity(target);
        if (entity != null && entity instanceof Refuge && path.size() <= 1) {
            actionExecutorType = ActionExecutorType.CLEAR_BLOCKADE;
        } else if (worldInfo.getEntity(agentInfo.getID()) instanceof Building || onBlockade && Util.isOnBlockade(worldInfo, agentInfo)) {//this condition is used to prevent kernel bug at the beginning of run(if agent is on blockade, new clear act will not work)
            actionExecutorType = ActionExecutorType.CLEAR_BLOCKADE;
        } else if (agentInfo.getPosition().equals(target)) {
            actionExecutorType = ActionExecutorType.CLEAR_BLOCKADE;
        } else {
            onBlockade = false;
            actionExecutorType = ActionExecutorType.CLEAR_AREA;
        }

        return actionExecutorType;
    }

    @Override
    public ExtAction calc() {
        if (target == null || agentInfo.getTime() < scenarioInfo.getKernelAgentsIgnoreuntil()) {
            result = null;
            return this;
        }
        ActionExecutorType actionExecutorType;
        List<EntityID> path;
        if (!agentInfo.getPosition().equals(target)) {
            path = pathPlanning.setFrom(agentInfo.getPosition()).setDestination(target).calc().getResult();

            if (path == null) {
                result = null;
                return this;
            }
            actionExecutorType = getActionExecutorType(path, target);
        } else {
            path = new ArrayList<>();
            path.add(target);
            actionExecutorType = ActionExecutorType.CLEAR_BLOCKADE;
        }
        Action lastAction = null;

        if (!(pathPlanning instanceof SamplePathPlanning)) {
            Area currentPosition = agentInfo.getPositionArea();
            if (!path.isEmpty() && !path.contains(currentPosition.getID())) {
                path.add(0, currentPosition.getID());
            }
        }

        switch (actionExecutorType) {
            case CLEAR_AREA:
                lastAction = clearAreaActExecutor.clearWay(path, target);
                break;
            case CLEAR_BLOCKADE:
                lastAction = clearBlockadeActExecutor.clearWay(path, target);
                break;
        }
        if (lastAction == null) {
            lastAction = new ActionMove(path);
        }
        result = lastAction;


        return this;
    }

    @Override
    public Action getAction() {
        return result;
    }

    private enum ActionExecutorType {
        CLEAR_AREA,
        CLEAR_BLOCKADE,
    }

//    private enum ActResult {
//        SUCCESSFULLY_COMPLETE,
//        WORKING,
//        FAILED,
//    }
}
