package CSU_Yunlu_2019.extaction.at;

import adf.agent.action.Action;
import adf.agent.action.ambulance.ActionLoad;
import adf.agent.action.ambulance.ActionRescue;
import adf.agent.action.ambulance.ActionUnload;
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
import com.google.common.collect.Lists;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static rescuecore2.standard.entities.StandardEntityURN.*;

import CSU_Yunlu_2019.module.algorithm.DebugLog;

/**
 * 修改calc calcRescue calcUnload策略
 */
public class ActionTransport extends ExtAction {

	private PathPlanning pathPlanning;
	private int thresholdRest;
	private int kernelTime;
	private EntityID target;
	private StuckAvoid stuckAvoid;
	//----------------------该类的作用就是确定一个最优的行动存进result-----------------------------
	//----------------------------------------------------------------------------------------
	//(父变量)Action result;//目标行动
	//----------------------------------------------------------------------------------------
	//private DebugLog logger;


	public ActionTransport(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo,
						   ModuleManager moduleManager, DevelopData developData) {
		super(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
		this.target = null;
		this.thresholdRest = developData.getInteger("ActionTransport.rest", 100);
		switch (scenarioInfo.getMode()) {
		case PRECOMPUTATION_PHASE:
			this.pathPlanning = moduleManager.getModule("ActionTransport.PathPlanning",
					"adf.sample.module.algorithm.SamplePathPlanning");
			break;
		case PRECOMPUTED:
			this.pathPlanning = moduleManager.getModule("ActionTransport.PathPlanning",
					"adf.sample.module.algorithm.SamplePathPlanning");
			break;
		case NON_PRECOMPUTE:
			this.pathPlanning = moduleManager.getModule("ActionTransport.PathPlanning",
					"adf.sample.module.algorithm.SamplePathPlanning");
			break;
		}

		//logger = new DebugLog(agentInfo);
		stuckAvoid = new StuckAvoid(agentInfo, worldInfo);
	}

	//预计算执行的方法
	public ExtAction precompute(PrecomputeData precomputeData) {
		super.precompute(precomputeData);
		if (this.getCountPrecompute() >= 2) {
			return this;
		}
		this.pathPlanning.precompute(precomputeData);
		try {
			this.kernelTime = this.scenarioInfo.getKernelTimesteps();
		} catch (NoSuchConfigOptionException e) {
			this.kernelTime = -1;
		}
		return this;
	}

	public ExtAction resume(PrecomputeData precomputeData) {
		super.resume(precomputeData);
		if (this.getCountResume() >= 2) {
			return this;
		}
		this.pathPlanning.resume(precomputeData);
		try {
			this.kernelTime = this.scenarioInfo.getKernelTimesteps();
		} catch (NoSuchConfigOptionException e) {
			this.kernelTime = -1;
		}
		return this;
	}

	public ExtAction preparate() {
		super.preparate();
		if (this.getCountPreparate() >= 2) {
			return this;
		}
		this.pathPlanning.preparate();
		try {
			this.kernelTime = this.scenarioInfo.getKernelTimesteps();
		} catch (NoSuchConfigOptionException e) {
			this.kernelTime = -1;
		}
		return this;
	}

	public ExtAction updateInfo(MessageManager messageManager) {
		super.updateInfo(messageManager);
		if (this.getCountUpdateInfo() >= 2) {
			return this;
		}
		this.pathPlanning.updateInfo(messageManager);
		return this;
	}

	@Override// 设置目标，通过HumanDetector传入的target来进行设置目标
	public ExtAction setTarget(EntityID target) {
		this.target = null;//？？？
		if (target != null) {
			StandardEntity entity = this.worldInfo.getEntity(target);
			if (entity instanceof Human || entity instanceof Area) {
				this.target = target;
				return this;
			}
		}
		return this;
	}

	@Override//todo:没读完
	public ExtAction calc() {// 整个执行策略
		this.result = null;
		AmbulanceTeam me = (AmbulanceTeam) this.agentInfo.me();
		Human transportHuman = this.agentInfo.someoneOnBoard();

		//如果背着人
		if (transportHuman != null) {
			//计算是否要放下市民
			this.result = this.calcUnload(me, this.pathPlanning, transportHuman, this.target);
		}
		if ((this.result == null) && this.needRest(me)) {
			EntityID areaID = this.convertArea(this.target);
			ArrayList<EntityID> targets = new ArrayList<>();
			if (areaID != null) {
				targets.add(areaID);
			}
			//在新target里找refuge
			this.result = this.calcRefugeAction(me, this.pathPlanning, targets, false);
		}
		//targets里没找到refuge||target不为空
		if ((this.result == null) && this.target != null) {
			this.result = this.calcRescue(me, this.pathPlanning, this.target);
		}
		
		if (((this.result instanceof ActionMove) || (transportHuman != null) ) &&
			stuckAvoid.check(me.getPosition())) {
			this.result = stuckAvoid.avoidStuck(this.pathPlanning);
			if (this.result != null)
				return this;
		}

		return this;
	}

	//todo:没读完
	// 这边是是否会在途中死亡的救援-----------------可以用的-------------------------------
	protected boolean willDiedWhenRscued(Human human) {
		//System.out.println("human.isBuriednessDefined():" + human.isBuriednessDefined());
		if (human.isBuriednessDefined() && human.getBuriedness() == 0)
			return false;

		int deadtime = estimatedDeathTime(human.getHP(), human.getDamage(), agentInfo.getTime());
		int resuceTime = 10000;
		//System.out.println("deadtime:" + deadtime);
		if (deadtime > resuceTime)
			return true;

		return false;
	}

	//todo:估计死亡时间(没读完)
	public int estimatedDeathTime(int hp, double dmg, int updatetime) {
		int agenttime = 1000;
		int count = agenttime - updatetime;
		if ((count <= 0) || (dmg == 0.0D)) {
			return hp;
		}
		double kbury = 3.5E-05D;
		double kcollapse = 0.00025D;
		double darsadbury = -0.0014D * updatetime + 0.64D;
		double burydamage = dmg * darsadbury;
		double collapsedamage = dmg - burydamage;

		while (count > 0) {
			int time = agenttime - count;

			burydamage += kbury * burydamage * burydamage + 0.11D;
			collapsedamage += kcollapse * collapsedamage * collapsedamage + 0.11D;
			dmg = burydamage + collapsedamage;
			count--;
			hp = (int) (hp - dmg);

			if (hp <= 0)
				return time;
		}
		return 1000;
	}

	//根据目标位置类型和目标状态确定下一步行动是挖掘废墟，背人走还是只移动
	private Action calcRescue(AmbulanceTeam agent, PathPlanning pathPlanning, EntityID target) {
		StandardEntity targetEntity = this.worldInfo.getEntity(target);
		if (targetEntity == null) {
			return null;
		}
		//获取agent位置
		EntityID agentPosition = agent.getPosition();
		//如果target是人
		if (targetEntity instanceof Human) {
			Human human = (Human) targetEntity;
			//如果位置未知
			if (!human.isPositionDefined()) {
				return null;
			}
			// 如果在路中就会死亡
			if (human.isHPDefined() && human.getHP() == 0 ) {
				return null;
			}
			//获取人的未知
			EntityID targetPosition = human.getPosition();
			//如果agent走到了目标未位置
			if (agentPosition.getValue() == targetPosition.getValue()) {
				//如果human还被埋着，先把它挖出来
				if (human.isBuriednessDefined() && human.getBuriedness() > 0) {
					return new ActionRescue(human);
					//已经挖出来了，如果是civilian，背他去refuge
				} else if (human.getStandardURN() == CIVILIAN) {
					return new ActionLoad(human.getID());
				}
				//如果还没走到目标位置，先走过去
			} else {
				List<EntityID> path = pathPlanning.getResult(agentPosition, targetPosition);
				if (path != null && path.size() > 0) {
					return new ActionMove(path);
				}
			}
			return null;
		}
		//如果target是路障，把位置赋值给targetEntity
		if (targetEntity.getStandardURN() == BLOCKADE) {
			Blockade blockade = (Blockade) targetEntity;
			if (blockade.isPositionDefined()) {//如果已知位置，赋给targetEntity
				targetEntity = this.worldInfo.getEntity(blockade.getPosition());
			}
		}
		//如果target是area，直接寻路
		if (targetEntity instanceof Area) {
			List<EntityID> path = pathPlanning.getResult(agentPosition, targetEntity.getID());
			if (path != null && path.size() > 0) {
				this.result = new ActionMove(path);
			}
		}
		return null;
	}

	//考虑把人放下 todo 写的巨乱无比
	private Action calcUnload(AmbulanceTeam agent, PathPlanning pathPlanning, Human transportHuman, EntityID targetID) {
		//如果背上没人，直接退出
		if (transportHuman == null) {
			return null;
		}
		//如果背上的人hp已经掉到了0
		if (transportHuman.isHPDefined() && transportHuman.getHP() == 0) {
			return new ActionUnload();// 放下，会NULL，这边是否Defined问一问学长
		}
		EntityID agentPosition = agent.getPosition();
		//获取当前位置的地形
		StandardEntity position = this.worldInfo.getEntity(agentPosition);

		//如果掉血速度为0
		if (transportHuman.isDamageDefined() &&
			position.getStandardURN() == ROAD &&
			transportHuman.getDamage() == 0) {
			EntityID human= transportHuman.getID();
			Human onloadhuman=(Human)this.worldInfo.getEntity(human);;
			//System.out.println("Damage****new:"+onloadhuman.getDamage()+"DamegePor"+onloadhuman.getDamageProperty().getValue());
			//System.out.println("Damage====old"+transportHuman.getDamage());
			return new ActionUnload();//放下
		}

		//target不为空或者把人背到了目的地
		if (targetID == null || transportHuman.getID().getValue() == targetID.getValue()) {
			//如果在refuge里
			if (position != null && position.getStandardURN() == REFUGE) {
				return new ActionUnload();//放下
			} else {//否则寻路
				pathPlanning.setFrom(agentPosition);
				pathPlanning.setDestination(this.worldInfo.getEntityIDsOfType(REFUGE));
				List<EntityID> path = pathPlanning.calc().getResult();
				if (path != null && path.size() > 0) {
					return new ActionMove(path);
				}
			}
		}

		//???
		if (targetID == null) {
			return null;
		}
		//获取目标地形
		StandardEntity targetEntity = this.worldInfo.getEntity(targetID);
		//如果是路障
		if (targetEntity != null && targetEntity.getStandardURN() == BLOCKADE) {
			Blockade blockade = (Blockade) targetEntity;
			if (blockade.isPositionDefined()) {
				targetEntity = this.worldInfo.getEntity(blockade.getPosition());
			}
		}
		if (targetEntity instanceof Area) {
			if (agentPosition.getValue() == targetID.getValue()) {
				return new ActionUnload();
			} else {
				pathPlanning.setFrom(agentPosition);
				pathPlanning.setDestination(targetID);
				List<EntityID> path = pathPlanning.calc().getResult();
				if (path != null && path.size() > 0) {
					return new ActionMove(path);
				}
			}
			//？？？
		} else if (targetEntity instanceof Human) {
			Human human = (Human) targetEntity;
			if (human.isPositionDefined()) {
				return calcRefugeAction(agent, pathPlanning, Lists.newArrayList(human.getPosition()), true);
			}
			pathPlanning.setFrom(agentPosition);
			pathPlanning.setDestination(this.worldInfo.getEntityIDsOfType(REFUGE));
			List<EntityID> path = pathPlanning.calc().getResult();
			if (path != null && path.size() > 0) {
				return new ActionMove(path);
			}
		}
		return null;
	}
	//是否需要救援 --------只能提示否需要救援，回家,救援的是在燃烧建筑物的人，困的不会死
	private boolean needRest(Human agent) {
		int hp = agent.getHP();
		int damage = agent.getDamage();
		if (hp == 0 || damage == 0) {
			return false;
		}
		//计算掉血到0的时间，计算用的hp向上取整
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

	//是human且在area，返回ID的位置。是area，返回原ID。是blockade，返回blockade的位置。
	private EntityID convertArea(EntityID targetID) {// 返回ID的位置，position
		StandardEntity entity = this.worldInfo.getEntity(targetID);
		if (entity == null) {
			return null;
		}
		if (entity instanceof Human) {
			Human human = (Human) entity;
			if (human.isPositionDefined()) {
				EntityID position = human.getPosition();
				if (this.worldInfo.getEntity(position) instanceof Area) {
					return position;
				}
			}
		} else if (entity instanceof Area) {
			return targetID;
		} else if (entity.getStandardURN() == BLOCKADE) {
			Blockade blockade = (Blockade) entity;
			if (blockade.isPositionDefined()) {
				return blockade.getPosition();
			}
		}
		return null;
	}

	//找到一个refuge，agent先去它还能找到路继续去target，并返回move去该refuge（adf代码）
	private Action calcRefugeAction(Human human, PathPlanning pathPlanning, Collection<EntityID> targets,
			boolean isUnload) {
		EntityID position = human.getPosition();
		Collection<EntityID> refuges = this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE);
		int size = refuges.size();//refuge的数量
		if (refuges.contains(position)) {//如果当前agent在refuge里，有人放人，没人休息。
			return isUnload ? new ActionUnload() : new ActionRest();
		}
		//不在refuge里，找路去refuge
		List<EntityID> firstResult = null;
		while (refuges.size() > 0) {
			pathPlanning.setFrom(position);
			pathPlanning.setDestination(refuges);
			List<EntityID> path = pathPlanning.calc().getResult();
			//找到了去refuge的路
			if (path != null && path.size() > 0) {
				if (firstResult == null) {//？？？
					firstResult = new ArrayList<>(path);
					if (targets == null || targets.isEmpty()) {
						break;
					}
				}
				//路线终点refuge
				EntityID refugeID = path.get(path.size() - 1);
				pathPlanning.setFrom(refugeID);
				pathPlanning.setDestination(targets);
				List<EntityID> fromRefugeToTarget = pathPlanning.calc().getResult();//计算从refuge到传入的target的路径
				//如果存在从该refuge到当前目标的路，放心大胆地回refuge
				if (fromRefugeToTarget != null && fromRefugeToTarget.size() > 0) {
					return new ActionMove(path);
				}
				refuges.remove(refugeID);//防止下次循环选中算过的refuge
				//remove失败，跳出循环
				if (size == refuges.size()) {
					break;
				}
				size = refuges.size();
			} else {//？？？没找到路直接跳出循环？？？？todo：判断写反了？
				break;
			}
		}
		//
		return firstResult != null ? new ActionMove(firstResult) : null;
	}
}
