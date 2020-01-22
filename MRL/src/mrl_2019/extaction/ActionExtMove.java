package mrl_2019.extaction;

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
import mrl_2019.algorithm.SamplePathPlanning;
import mrl_2019.extaction.move.RayMoveActExecutor;
import mrl_2019.util.Util;
import mrl_2019.world.MrlWorldHelper;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ActionExtMove extends ExtAction {
    private final int STUCK_THRESHOLD = 2000;
    private PathPlanning pathPlanning;

    private int thresholdRest;
    private int kernelTime;
    private List<EntityID> lastMovePlan = new ArrayList<>();

    private EntityID target;

    //    private MrlWorldHelper worldHelper;
    private boolean thisCycleMoveToPoint;
    //    private int lastMoveTime;
    private Pair<Integer, Integer> selfLocation;
    private Point lastPositionCoordinate;
    private int moveDistance;
    private final RayMoveActExecutor rayMoveActExecutor;
    private MrlWorldHelper worldHelper;
    private int lastMoveTime;

    public ActionExtMove(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, DevelopData developData) {
        super(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
        this.target = null;
        this.thresholdRest = developData.getInteger("ActionExtMove.rest", 100);

        switch (scenarioInfo.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("ActionExtMove.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("ActionExtMove.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("ActionExtMove.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
        }

        this.worldHelper = MrlWorldHelper.load(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);


        selfLocation = worldInfo.getLocation(agentInfo.getID());
        lastPositionCoordinate = new Point(selfLocation.first(), selfLocation.second());
        rayMoveActExecutor = new RayMoveActExecutor(worldHelper, worldInfo, scenarioInfo, agentInfo);
    }

    @Override
    public ExtAction precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        worldHelper.precompute(precomputeData);
        this.pathPlanning.precompute(precomputeData);
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
        this.worldHelper.resume(precomputeData);
        this.pathPlanning.resume(precomputeData);
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
        worldHelper.preparate();
        this.pathPlanning.preparate();
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
        this.worldHelper.updateInfo(messageManager);
        this.pathPlanning.updateInfo(messageManager);
        selfLocation = worldInfo.getLocation(agentInfo.getID());
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
        List<EntityID> plan = this.pathPlanning.calc().getResult();


        try {


            this.result = moveOnPlan(plan);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return this;
    }

    public Action moveOnPlan(List<EntityID> plan) {
        if (plan == null) {
            return null;
        }
        lastMovePlan.clear();
        lastMovePlan.addAll(plan);

//        if (plan.isEmpty()) {
//            return;
//        }
//        Area target = null;
//        if (!lastMovePlan.isEmpty()) {
//            target = (Area) worldInfo.getEntity(lastMovePlan.get(lastMovePlan.size() - 1));
//        } else {
//            target = (Area) worldInfo.getEntity(this.target);
//        }

        if (!(pathPlanning instanceof SamplePathPlanning)) {
            Area currentPosition = agentInfo.getPositionArea();
            if (!plan.isEmpty() && !plan.contains(currentPosition.getID())) {
                plan.add(0, currentPosition.getID());
            }
        }


        Action action = null;
        if (agentInfo.getTime() >= scenarioInfo.getKernelAgentsIgnoreuntil() && amIMotionLess(plan)) {

            action = rayMoveActExecutor.execute(plan);

        }


        lastMoveTime = agentInfo.getTime();
        if (action == null) {
            setThisCycleMoveToPoint(false);

            if (lastMovePlan != null && !lastMovePlan.isEmpty()) {
                action = new ActionMove(lastMovePlan);
            }
        }

        return action;


    }

    private boolean amIMotionLess(List<EntityID> plan) {
        return !plan.isEmpty() && !plan.contains(agentInfo.getPosition())
                || stuckCondition(plan);
    }

    private boolean stuckCondition(List<EntityID> plan) {
        if (lastMoveTime < scenarioInfo.getKernelAgentsIgnoreuntil())
            return false;
        if (isThisCycleMoveToPoint()) {
            return false;
        }
        if (plan.size() > 0) {
            EntityID target = plan.get(plan.size() - 1);
            if (target.equals(agentInfo.getPosition())) {
                return false;
            }
        }


        Point positionCoordinate = new Point(selfLocation.first(), selfLocation.second());
        moveDistance = Util.distance(lastPositionCoordinate, positionCoordinate);
        lastPositionCoordinate = positionCoordinate;
        if (moveDistance <= STUCK_THRESHOLD) {
            return true;
        }
        lastPositionCoordinate = positionCoordinate;
        return false;
    }

    @Override
    public Action getAction() {
        return result;
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
                //remove failed
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

    public void setThisCycleMoveToPoint(boolean thisCycleMoveToPoint) {
        this.thisCycleMoveToPoint = thisCycleMoveToPoint;
    }

    public boolean isThisCycleMoveToPoint() {
        return thisCycleMoveToPoint;
    }
}
