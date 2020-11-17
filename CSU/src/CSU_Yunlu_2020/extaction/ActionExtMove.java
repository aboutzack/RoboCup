package CSU_Yunlu_2020.extaction;

import CSU_Yunlu_2020.CSUConstants;
import CSU_Yunlu_2020.debugger.DebugHelper;
import CSU_Yunlu_2020.util.ambulancehelper.CSUSelectorTargetByDis;
import CSU_Yunlu_2020.world.CSUWorldHelper;
import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.action.common.ActionRest;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.PathPlanning;
import com.mrl.debugger.remote.dto.StuckDto;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @Author: Guanyu-Cai
 * @Date: 2/15/20
 */
public class ActionExtMove extends ExtAction {
    private PathPlanning pathPlanning;

    private int thresholdRest;
    private int kernelTime;

    private EntityID target;

    private Pair<Integer, Integer> selfLocation;
    private Point lastPosition = null;

    private int lastMoveTime;
    private List<EntityID> lastmovePath;
    private final int STUCK_THRESHOLD = 2000;//threshold of stuck
    private CSUWorldHelper world;
    private StuckHelper stuckHelper;
    private boolean stuck = false;//每回合只更新一次
    private int lastStuckUpdateTime = -1;


    public ActionExtMove(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo,
                         ModuleManager moduleManager, DevelopData developData) {
        super(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
        this.target = null;
        this.thresholdRest = developData.getInteger("ActionExtMove.rest", 100);

        switch (scenarioInfo.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("ActionExtMove.PathPlanning",
                        "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("ActionExtMove.PathPlanning",
                        "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("ActionExtMove.PathPlanning",
                        "adf.sample.module.algorithm.SamplePathPlanning");
                break;
        }
        this.world = moduleManager.getModule("WorldHelper.Default", CSUConstants.WORLD_HELPER_DEFAULT);
        this.stuckHelper = new StuckHelper(world);
        this.selfLocation = worldInfo.getLocation(agentInfo.getID());
        this.lastPosition = new Point(selfLocation.first(), selfLocation.second());
        this.lastmovePath = new LinkedList<>();
    }

    @Override
    public ExtAction precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        this.pathPlanning.precompute(precomputeData);
        this.world.precompute(precomputeData);

        try {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        } catch (NoSuchConfigOptionException e) {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override
    public ExtAction resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
        this.pathPlanning.resume(precomputeData);
        this.world.resume(precomputeData);
        try {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        } catch (NoSuchConfigOptionException e) {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override
    public ExtAction preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }
        this.pathPlanning.preparate();
        this.world.preparate();
        try {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        } catch (NoSuchConfigOptionException e) {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override
    public ExtAction updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.pathPlanning.updateInfo(messageManager);
        this.world.updateInfo(messageManager);
        this.selfLocation = worldInfo.getLocation(agentInfo.getID());
        return this;
    }

    @Override
    public ExtAction setTarget(EntityID target) {
        this.target = null;
        StandardEntity entity = this.worldInfo.getEntity(target);

        if (entity != null) {
            if (entity.getStandardURN().equals(StandardEntityURN.BLOCKADE)) {
                entity = this.worldInfo.getEntity(((Blockade) entity).getPosition());
            } else if (entity instanceof Human) {
                entity = this.worldInfo.getPosition((Human) entity);
            }
            if (entity != null && entity instanceof Area) {
                this.target = entity.getID();
            }
        }
        return this;
    }

    @Override
    public ExtAction calc() {
        this.result = null;
        Human agent = (Human) this.agentInfo.me();

        if (this.needRest(agent)) {
            this.result = this.calcRest(agent, this.pathPlanning, this.target);
            if (this.result != null) {
                return this;
            }
        }
        if (this.target == null) {
            return this;
        }
        this.pathPlanning.setFrom(agent.getPosition());
        this.pathPlanning.setDestination(this.target);
        List<EntityID> path = this.pathPlanning.calc().getResult();
        if (path != null && path.size() > 0) {
            this.result = moveOnPath(path);
        } else {
            this.result = null;
        }
        return this;

    }

    private Action moveOnPath(List<EntityID> path) {
        Action action = null;
        if (path == null) {
            return null;
        }
        lastmovePath.clear();
        lastmovePath.addAll(path);

        if (!path.isEmpty() && !path.contains(world.getSelfPositionId())) {
            path.add(0, world.getSelfPositionId());
        }
        boolean stuckFlag = agentInfo.getTime() >= scenarioInfo.getKernelAgentsIgnoreuntil() && isStuck(path);
        if (stuckFlag) {
            action = stuckHelper.calc(path);
            if (CSUConstants.DEBUG_STUCK_HELPER && action == null) {
                System.out.println(world.getSelfHuman().getID() + " stuckHelper fail to path(0)" + path.get(0));
            }
        } else if (DebugHelper.DEBUG_MODE) {
            DebugHelper.VD_CLIENT.drawAsync(agentInfo.getID().getValue(), "StuckDtoLayer", new StuckDto());
        }
        lastMoveTime = agentInfo.getTime();
        if (action == null) {
            if (lastmovePath != null && !lastmovePath.isEmpty()) {
                action = new ActionMove(lastmovePath);
            }
        }
        return action;
    }

    private boolean needRest(Human agent) {
        int hp = agent.getHP();
        int damage = agent.getDamage();
        if (hp == 0 || damage == 0) {
            return false;
        }
        int activeTime = (hp / damage) + ((hp % damage) != 0 ? 1 : 0);
        if (this.kernelTime == -1) {
            try {
                this.kernelTime = this.scenarioInfo.getKernelTimesteps();
            } catch (NoSuchConfigOptionException e) {
                this.kernelTime = -1;
            }
        }
        return damage >= this.thresholdRest || (activeTime + this.agentInfo.getTime()) < this.kernelTime;
    }

    private Action calcRest(Human human, PathPlanning pathPlanning, EntityID target) {
        EntityID position = human.getPosition();
        Collection<EntityID> refuges = this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE);
        int currentSize = refuges.size();
        if (refuges.contains(position)) {
            return new ActionRest();
        }
        List<EntityID> firstResult = null;
        while (refuges.size() > 0) {
            pathPlanning.setFrom(position);
            pathPlanning.setDestination(refuges);
            List<EntityID> path = pathPlanning.calc().getResult();
            if (path != null && path.size() > 0) {
                if (firstResult == null) {
                    firstResult = new ArrayList<>(path);
                    if (target == null) {
                        break;
                    }
                }
                EntityID refugeID = path.get(path.size() - 1);
                pathPlanning.setFrom(refugeID);
                pathPlanning.setDestination(target);
                List<EntityID> fromRefugeToTarget = pathPlanning.calc().getResult();
                if (fromRefugeToTarget != null && fromRefugeToTarget.size() > 0) {
                    return new ActionMove(path);
                }
                refuges.remove(refugeID);
                // remove failed
                if (currentSize == refuges.size()) {
                    break;
                }
                currentSize = refuges.size();
            } else {
                break;
            }
        }
        return firstResult != null ? new ActionMove(firstResult) : null;
    }

    /**
     * @Description: 通过计算移动距离和确定STUCK_THRESHOLD范围内是否有障碍物来判定stuck
     * @Author: Guanyu-Cai
     * @Date: 2/16/20
     */
    public boolean isStuck(List<EntityID> path) {
        if (lastStuckUpdateTime == agentInfo.getTime()) {
            return stuck;
        }else {
            lastStuckUpdateTime = agentInfo.getTime();
            if (lastMoveTime < scenarioInfo.getKernelAgentsIgnoreuntil()) {
                stuck = false;
                return false;
            }
            if (path.size() > 0) {
                EntityID target = path.get(path.size() - 1);
                if (target.equals(agentInfo.getPosition())) {
                    stuck = false;
                    return false;
                }
            }
            Point position = new Point(selfLocation.first(), selfLocation.second());
            int moveDistance = CSUSelectorTargetByDis.getDistance.distance(position, lastPosition);
            lastPosition = position;
            Collection<Blockade> blockadesInRange = world.getBlockadesInRange(STUCK_THRESHOLD);
            if (moveDistance <= STUCK_THRESHOLD && blockadesInRange != null && !blockadesInRange.isEmpty()) {
                stuck = true;
                return true;
            } else {
                stuck = false;
                return false;
            }
        }
    }
}

