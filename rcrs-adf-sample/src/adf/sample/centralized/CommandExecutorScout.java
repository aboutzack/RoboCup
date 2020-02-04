package adf.sample.centralized;


import adf.agent.action.common.ActionMove;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.centralized.CommandScout;
import adf.agent.communication.standard.bundle.centralized.MessageReport;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.centralized.CommandExecutor;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.standard.entities.Area;
import rescuecore2.worldmodel.AbstractEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static rescuecore2.standard.entities.StandardEntityURN.REFUGE;

//调用pathplanning的calc,获取actionmode保存在result
public class CommandExecutorScout extends CommandExecutor<CommandScout> {
    private static final int ACTION_UNKNOWN = -1;
    private static final int ACTION_SCOUT = 1;

    private PathPlanning pathPlanning;

    //执行动作对应的int值
    private int type;
    //侦查目标的entityid,为road和building
    private Collection<EntityID> scoutTargets;
    //sender的entityid
    private EntityID commanderID;

    public CommandExecutorScout(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.type = ACTION_UNKNOWN;
        //反射加载SamplePathPlanning
        switch  (scenarioInfo.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("CommandExecutorScout.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("CommandExecutorScout.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("CommandExecutorScout.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
        }
    }

    @Override
    public CommandExecutor setCommand(CommandScout command) {
        //接收command的agent对应的id
        EntityID agentID = this.agentInfo.getID();
        //如果toid有效且不是broadcast且agentid和toid相同
        if(command.isToIDDefined() && (Objects.requireNonNull(command.getToID()).getValue() == agentID.getValue())) {
            EntityID target = command.getTargetID();
            if(target == null) {
                //接收command的agent所在位置的entityid
                target = this.agentInfo.getPosition();
            }
            this.type = ACTION_SCOUT;
            this.commanderID = command.getSenderID();
            this.scoutTargets = new HashSet<>();
            //agent所在entity为圆心,range半径的圆内所有entity为侦查物
            this.scoutTargets.addAll(
                    worldInfo.getObjectsInRange(target, command.getRange())
                            .stream()
                            //过滤,只侦查road(hydrant)和building(ambulanceCenter,fireStation,gasStation,policeForce)
                            .filter(e -> e instanceof Area && e.getStandardURN() != REFUGE)
                            .map(AbstractEntity::getID)
                            .collect(Collectors.toList())
            );
        }
        return this;
    }

    @Override
    public CommandExecutor updateInfo(MessageManager messageManager){
        super.updateInfo(messageManager);
        if(this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.pathPlanning.updateInfo(messageManager);

        //command执行完
        if(this.isCommandCompleted()) {
            if(this.type != ACTION_UNKNOWN) {
                messageManager.addMessage(new MessageReport(true, true, false, this.commanderID));
                //设为默认值
                this.type = ACTION_UNKNOWN;
                this.scoutTargets = null;
                this.commanderID = null;
            }
        }
        return this;
    }

    @Override
    public CommandExecutor precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if(this.getCountPrecompute() >= 2) {
            return this;
        }
        this.pathPlanning.precompute(precomputeData);
        return this;
    }

    @Override
    public CommandExecutor resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if(this.getCountResume() >= 2) {
            return this;
        }
        this.pathPlanning.resume(precomputeData);
        return this;
    }

    @Override
    public CommandExecutor preparate() {
        super.preparate();
        if(this.getCountPreparate() >= 2) {
            return this;
        }
        this.pathPlanning.preparate();
        return this;
    }

    //每次调用calc,获取到某一个target的一条路径
    @Override
    public CommandExecutor calc() {
        this.result = null;
        if(this.type == ACTION_SCOUT) {
            if(this.scoutTargets == null || this.scoutTargets.isEmpty()) {
                return this;
            }
            //设置from为agent所在当前位置的entityid
            this.pathPlanning.setFrom(this.agentInfo.getPosition());
            //设置destination为scoutTargets
            this.pathPlanning.setDestination(this.scoutTargets);
            //获取计算所得路径
            List<EntityID> path = this.pathPlanning.calc().getResult();
            if(path != null) {
                //result即路径
                this.result = new ActionMove(path);
            }
        }
        return this;
    }

    //所有targets都侦察过或command无效,每次调用会删除scoutTargets中侦察过的点
    private boolean isCommandCompleted() {
        if(this.type ==  ACTION_SCOUT) {
            if(this.scoutTargets != null) {
                this.scoutTargets.removeAll(this.worldInfo.getChanged().getChangedEntities());
            }
            return (this.scoutTargets == null || this.scoutTargets.isEmpty());
        }
        return true;
    }
}
