package adf.sample.centralized;
//警察局指令中心
import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.action.common.ActionRest;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
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
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.List;
import java.util.Objects;

import static rescuecore2.standard.entities.StandardEntityURN.BLOCKADE;
import static rescuecore2.standard.entities.StandardEntityURN.REFUGE;

public class CommandExecutorPolice extends CommandExecutor<CommandPolice> {
	//各种动作对应的int值
    private static final int ACTION_UNKNOWN = -1;
    private static final int ACTION_REST = CommandPolice.ACTION_REST;
    private static final int ACTION_MOVE = CommandPolice.ACTION_MOVE;
    private static final int ACTION_CLEAR = CommandPolice.ACTION_CLEAR;
    private static final int ACTION_AUTONOMY = CommandPolice.ACTION_AUTONOMY;
    //每种对应int值
    private int commandType;	
    //每种命令对应目标不同
    private EntityID target;	
    //下指令者ID
    private EntityID commanderID;	
    //路线规划
    private PathPlanning pathPlanning;	
	//清路动作
    private ExtAction actionExtClear;
	//移动
    private ExtAction actionExtMove;

    public CommandExecutorPolice(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {    //构造函数
        super(ai, wi, si, moduleManager, developData);
        this.commandType = ACTION_UNKNOWN;
      //反射加载SamplePathPlanning
        switch  (scenarioInfo.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("CommandExecutorPolice.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.actionExtClear = moduleManager.getExtAction("CommandExecutorPolice.ActionExtClear", "adf.sample.extaction.ActionExtClear");
                this.actionExtMove = moduleManager.getExtAction("CommandExecutorPolice.ActionExtMove", "adf.sample.extaction.ActionExtMove");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("CommandExecutorPolice.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.actionExtClear = moduleManager.getExtAction("CommandExecutorPolice.ActionExtClear", "adf.sample.extaction.ActionExtClear");
                this.actionExtMove = moduleManager.getExtAction("CommandExecutorPolice.ActionExtMove", "adf.sample.extaction.ActionExtMove");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("CommandExecutorPolice.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.actionExtClear = moduleManager.getExtAction("CommandExecutorPolice.ActionExtClear", "adf.sample.extaction.ActionExtClear");
                this.actionExtMove = moduleManager.getExtAction("CommandExecutorPolice.ActionExtMove", "adf.sample.extaction.ActionExtMove");
                break;
        }
    }

    @Override
    //警察局下令
    public CommandExecutor setCommand(CommandPolice command) {
    	//警察ID
        EntityID agentID = this.agentInfo.getID();
        if(command.isToIDDefined() && Objects.requireNonNull(command.getToID()).getValue() == agentID.getValue()) {
        	//command有定义且command的ToId与agentID相同时获取command
            this.commandType = command.getAction();
            this.target = command.getTargetID();
            this.commanderID = command.getSenderID();
        }
        return this;
    }
    //数据预计算
    public CommandExecutor precompute(PrecomputeData precomputeData) {
    	//获取这个time的precompute的count
        super.precompute(precomputeData);
        //如果count>=2,说明之前已经执行了下面的操作,直接返回
        if(this.getCountPrecompute() >= 2) {
            return this;
        }
        this.pathPlanning.precompute(precomputeData);
        this.actionExtClear.precompute(precomputeData);
        this.actionExtMove.precompute(precomputeData);
        return this;
    }
    //重新开始
    public CommandExecutor resume(PrecomputeData precomputeData) { 
    	//获取这个time的resume的count
        super.resume(precomputeData);
    	//如果count>=2,说明之前已经执行了下面的操作,直接返回
        if(this.getCountResume() >= 2) {
            return this;
        }
        this.pathPlanning.resume(precomputeData);
        this.actionExtClear.resume(precomputeData);
        this.actionExtMove.resume(precomputeData);
        return this;
    }
    //预备
    public CommandExecutor preparate() {
    	//获取这个time的prepare的count
        super.preparate();
		//如果count>=2,说明之前已经执行了下面的操作,直接返回
        if(this.getCountPreparate() >= 2) {
            return this;
        }
        this.pathPlanning.preparate();
        this.actionExtClear.preparate();
        this.actionExtMove.preparate();
        return this;
    }

    public CommandExecutor updateInfo(MessageManager messageManager){   
    	//更新这个time的updateinfo的count
        super.updateInfo(messageManager);	
   	    //如果count>=2,说明之前已经执行了下面的操作,直接返回
        if(this.getCountUpdateInfo() >= 2) {
            return this;
        }
        //更新
        this.pathPlanning.updateInfo(messageManager);
        this.actionExtClear.updateInfo(messageManager);
        this.actionExtMove.updateInfo(messageManager);
        //指令完成
        if(this.isCommandCompleted()) { 
            if(this.commandType != ACTION_UNKNOWN) {
                messageManager.addMessage(new MessageReport(true, true, false, this.commanderID));
                this.commandType = ACTION_UNKNOWN;
                this.target = null;
                this.commanderID = null;
            }
        }
        return this;
    }

    @Override   //对于每种ACTION的对应算法
    public CommandExecutor calc() {
        this.result = null;
        EntityID position = this.agentInfo.getPosition();
        switch (this.commandType) {
        	//休息
            case ACTION_REST:
            	//如果此时没有目标地点
                if(this.target == null) {
                	//去refuge
                    if(worldInfo.getEntity(position).getStandardURN() != REFUGE) {
                        this.pathPlanning.setFrom(position);
                        this.pathPlanning.setDestination(this.worldInfo.getEntityIDsOfType(REFUGE));
                        List<EntityID> path = this.pathPlanning.calc().getResult();
                        if(path != null && path.size() > 0) {
                            Action action = this.actionExtClear.setTarget(path.get(path.size() - 1)).calc().getAction();
                            if(action == null) {
                                action = new ActionMove(path);
                            }
                            this.result = action;
                            return this;
                        }
                    }
            		//此时有target就去target
                } else if (position.getValue() != this.target.getValue()) {
                    List<EntityID> path = this.pathPlanning.getResult(position, this.target);
                    if(path != null && path.size() > 0) {
                        Action action = this.actionExtClear.setTarget(path.get(path.size() - 1)).calc().getAction();
                        if(action == null) {
                            action = new ActionMove(path);
                        }
                        this.result = action;
                        return this;
                    }
                }	
        		//休息
                this.result = new ActionRest();		
                return this;
                //移动
            case ACTION_MOVE:      
                if(this.target != null) {
                    this.result = this.actionExtClear.setTarget(this.target).calc().getAction();
                }
                return this;
                //清除路障
            case ACTION_CLEAR:     
                if(this.target != null) {
                    this.result = this.actionExtClear.setTarget(this.target).calc().getAction();
                }
                return this;
                //自主选择
            case ACTION_AUTONOMY:    
                if(this.target == null) {
                    return this;
                }
                StandardEntity targetEntity = this.worldInfo.getEntity(this.target);
            	//去refuge
                if(targetEntity.getStandardURN() == REFUGE) {
                    PoliceForce agent = (PoliceForce) this.agentInfo.me();
                	//到目的地去rest
                    if(agent.getDamage() > 0) {	
                        if (position.getValue() != this.target.getValue()) {
                            List<EntityID> path = this.pathPlanning.getResult(position, this.target);
                            if(path != null && path.size() > 0) {
                                Action action = this.actionExtClear.setTarget(path.get(path.size() - 1)).calc().getAction();
                                if (action == null) {
                                    action = new ActionMove(path);
                                }
                                this.result = action;
                                return this;
                            }
                        }
                        this.result = new ActionRest();
                    } else {
                        this.result = this.actionExtClear.setTarget(this.target).calc().getAction();
                    }
            		//空地？
                } else if (targetEntity instanceof Area) {
                    this.result = this.actionExtClear.setTarget(this.target).calc().getAction();
                    return this;
                	//有平民的地方
                }else if (targetEntity instanceof Human) {	
                    Human h = (Human) targetEntity;
                	//平民挂了就不过去了
                    if((h.isHPDefined() && h.getHP() == 0)) {	
                        return this;
                    }
                	//去平民的所在地
                    if(h.isPositionDefined() && this.worldInfo.getPosition(h) instanceof Area) {	
                        this.target = h.getPosition();
                        this.result = this.actionExtClear.setTarget(this.target).calc().getAction();
                    }
                	//障碍
                } else if(targetEntity.getStandardURN() == BLOCKADE) {	
                    Blockade blockade = (Blockade)targetEntity;
                    if(blockade.isPositionDefined()) {
                        this.target = blockade.getPosition();
                        this.result = this.actionExtClear.setTarget(this.target).calc().getAction();
                    }
                }
        }
        return this;
    }
    //指令是否完成判断action
    private boolean isCommandCompleted() {    
        PoliceForce agent = (PoliceForce) this.agentInfo.me();
        switch (this.commandType) {
    	//根据damage判断是否完成了rest    
        	case ACTION_REST:
                if(this.target == null) {
                    return (agent.getDamage() == 0);
                }
                if(this.worldInfo.getEntity(this.target).getStandardURN() == REFUGE) {
                    if (agent.getPosition().getValue() == this.target.getValue()) {
                        return (agent.getDamage() == 0);
                    }
                }
                return false;
            	//没有target或者agent位置与target位置相同
            case ACTION_MOVE:
                return this.target == null || (this.agentInfo.getPosition().getValue() == this.target.getValue());
              //blockades是否empty或者是否到达target
            case ACTION_CLEAR:	
                if(this.target == null) {
                    return true;
                }
                StandardEntity entity = this.worldInfo.getEntity(this.target);
                if(entity instanceof Road) {
                    Road road = (Road)entity;
                    if(road.isBlockadesDefined()) {
                        return road.getBlockades().isEmpty();
                    }
                    if(this.agentInfo.getPosition().getValue() != this.target.getValue()) {
                        return false;
                    }
                }
                return true;
            case ACTION_AUTONOMY:
                if(this.target != null) {
                    StandardEntity targetEntity = this.worldInfo.getEntity(this.target);
                    if(targetEntity.getStandardURN() == REFUGE) {	
                    	//根据damage选择action
                        this.commandType = agent.getDamage() > 0 ? ACTION_REST : ACTION_CLEAR;	
                        return this.isCommandCompleted();
                    } else if (targetEntity instanceof Area) {
                        this.commandType = ACTION_CLEAR;
                        return this.isCommandCompleted();
                    }else if (targetEntity instanceof Human) {
                        Human h = (Human) targetEntity;
                      //平民挂了自动完成
                        if((h.isHPDefined() && h.getHP() == 0)) {	
                            return true;
                        }
                    	//清路障去平民所在地
                        if(h.isPositionDefined() && this.worldInfo.getPosition(h) instanceof Area) {
                            this.target = h.getPosition();
                            this.commandType = ACTION_CLEAR;
                            return this.isCommandCompleted();
                        }
                    	//清路障
                    } else if(targetEntity.getStandardURN() == BLOCKADE) {	
                        Blockade blockade = (Blockade)targetEntity;
                        if(blockade.isPositionDefined()) {
                            this.target = blockade.getPosition();
                            this.commandType = ACTION_CLEAR;
                            return this.isCommandCompleted();
                        }
                    }
                }
                return true;
        }
        return true;
    }
}
