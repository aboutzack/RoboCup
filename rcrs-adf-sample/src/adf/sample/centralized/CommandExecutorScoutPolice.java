package adf.sample.centralized;
//搜索侦查，目标为building和road
import adf.agent.action.Action;
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
import adf.component.extaction.ExtAction;
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

public class CommandExecutorScoutPolice extends CommandExecutor<CommandScout> {

    private static final int ACTION_UNKNOWN = -1;
    private static final int ACTION_SCOUT = 1;

    private PathPlanning pathPlanning;

    private ExtAction actionExtClear;
    //执行动作对于int值
    private int commandType;	
    //侦查目标的ID
    private Collection<EntityID> scoutTargets;	
    //发指令者的ID
    private EntityID commanderID;	

    public CommandExecutorScoutPolice(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {    //构造函数
        super(ai, wi, si, moduleManager, developData);
        this.commandType = ACTION_UNKNOWN;
        this.scoutTargets = new HashSet<>();
        this.commanderID = null;
      //反射加载SamplePathPlanning
        switch  (scenarioInfo.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("CommandExecutorScoutPolice.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.actionExtClear = moduleManager.getExtAction("CommandExecutorScoutPolice.ActionExtClear", "adf.sample.extaction.ActionExtClear");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("CommandExecutorScoutPolice.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.actionExtClear = moduleManager.getExtAction("CommandExecutorScoutPolice.ActionExtClear", "adf.sample.extaction.ActionExtClear");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("CommandExecutorScoutPolice.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.actionExtClear = moduleManager.getExtAction("CommandExecutorScoutPolice.ActionExtClear", "adf.sample.extaction.ActionExtClear");
                break;
        }
    }

    @Override  //下令搜索侦查
    public CommandExecutor<CommandScout> setCommand(CommandScout command) {
    	//警察ID
        EntityID agentID = this.agentInfo.getID();	
        if(command.isToIDDefined() && (Objects.requireNonNull(command.getToID()).getValue() == agentID.getValue())) {
        	//command有定义且不是broadcast且command的ToId与agentID相同时获取target
            EntityID target = command.getTargetID();	
            if(target == null) {
                target = this.agentInfo.getPosition();
            }
            //搜索命令
            this.commandType = ACTION_SCOUT;	
            //commanderID
            this.commanderID = command.getSenderID();	
            this.scoutTargets = new HashSet<>();
            //agent所在entity为圆心,range半径的圆内所有road和building（refuge除外）为侦查物
            this.scoutTargets.addAll(	
                    worldInfo.getObjectsInRange(target, command.getRange())
                            .stream()
                            .filter(e -> e instanceof Area && e.getStandardURN() != REFUGE)
                            .map(AbstractEntity::getID)
                            .collect(Collectors.toList())
            );
        }
        return this;
    }
    //数据预处理
    public CommandExecutor precompute(PrecomputeData precomputeData) {    
        super.precompute(precomputeData);
        if(this.getCountPrecompute() >= 2) {
            return this;
        }
        this.pathPlanning.precompute(precomputeData);
        this.actionExtClear.precompute(precomputeData);
        return this;
    }
    //重新开始
    public CommandExecutor resume(PrecomputeData precomputeData) {   
        super.resume(precomputeData);
        if(this.getCountResume() >= 2) {
            return this;
        }
        this.pathPlanning.resume(precomputeData);
        this.actionExtClear.resume(precomputeData);
        return this;
    }
    //预备
    public CommandExecutor preparate() {   
        super.preparate();
        if(this.getCountPreparate() >= 2) {
            return this;
        }
        this.pathPlanning.preparate();
        this.actionExtClear.preparate();
        return this;
    }
    //更新这个time的内容
    public CommandExecutor updateInfo(MessageManager messageManager){  
        super.updateInfo(messageManager);
        if(this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.pathPlanning.updateInfo(messageManager);
        this.actionExtClear.updateInfo(messageManager);
        //command完成
        if(this.isCommandCompleted()) {   
            if(this.commandType != ACTION_UNKNOWN) {
                messageManager.addMessage(new MessageReport(true, true, false, this.commanderID));
                this.commandType = ACTION_UNKNOWN;
                this.scoutTargets = new HashSet<>();
                this.commanderID = null;
            }
        }
        return this;
    }

    @Override  
    //侦查算法
    public CommandExecutor calc() {   
        this.result = null;
        EntityID position = this.agentInfo.getPosition();
        if(this.commandType == ACTION_SCOUT) {
        	 //搜索目标空
            if (this.scoutTargets == null || this.scoutTargets.isEmpty()) {   
                return this;
            }
            //设定初始position与scoutTarget
            this.pathPlanning.setFrom(position);    
            this.pathPlanning.setDestination(this.scoutTargets);
            //路径
            List<EntityID> path = this.pathPlanning.calc().getResult();		
            if (path != null) {
                EntityID target = path.size() > 0 ? path.get(path.size() - 1) : position;
                //根据路上是否需要清障选择动作
                Action action = this.actionExtClear.setTarget(target).calc().getAction();  
                if (action == null) {
                    action = new ActionMove(path);
                }
              //找到的路
                this.result = action;	
            }
        }
        return this;
    }
    //指令完成
    private boolean isCommandCompleted() {    
        if(this.commandType == ACTION_SCOUT) {
        	//所有target都侦察过
            if(this.scoutTargets != null) {		
                this.scoutTargets.removeAll(this.worldInfo.getChanged().getChangedEntities());
            }
            return (this.scoutTargets == null || this.scoutTargets.isEmpty());
        }
        return true;
    }
}
