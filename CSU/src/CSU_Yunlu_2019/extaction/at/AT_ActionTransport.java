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

public class AT_ActionTransport extends ExtAction {

	private PathPlanning pathPlanning;
	private int thresholdRest;
	private int kernelTime;
	private EntityID target;

	private DebugLog logger;
	private AT_StuckAvoid stuckAvoid;

	public AT_ActionTransport(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo,
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

		logger = new DebugLog(agentInfo);
		stuckAvoid = new AT_StuckAvoid(agentInfo, worldInfo);
	}

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

	@Override
	public ExtAction setTarget(EntityID target) {// 设置目标，通过传入的target来进行设置目标
		this.target = null;
		if (target != null) {
			StandardEntity entity = this.worldInfo.getEntity(target);
			if (entity instanceof Human || entity instanceof Area) {
				this.target = target;
				return this;
			}
		}
		return this;
	}

	@Override
	public ExtAction calc() {// 整个执行策略
		this.result = null;
		AmbulanceTeam agent = (AmbulanceTeam) this.agentInfo.me();
		Human transportHuman = this.agentInfo.someoneOnBoard();

		if (transportHuman != null) {
			this.result = this.calcUnload(agent, this.pathPlanning, transportHuman, this.target);// -------放下市民
		}

		if ((this.result == null) && this.needRest(agent)) {
			EntityID areaID = this.convertArea(this.target);
			ArrayList<EntityID> targets = new ArrayList<>();
			if (areaID != null) {
				targets.add(areaID);
			}
			this.result = this.calcRefugeAction(agent, this.pathPlanning, targets, false);
		}

		if ((this.result == null) && this.target != null) {
			this.result = this.calcRescue(agent, this.pathPlanning, this.target);
		}
		
		if (((this.result instanceof ActionMove) || (transportHuman != null) ) &&
			stuckAvoid.check(agent.getPosition())) {
			this.result = stuckAvoid.avoidStuck(this.pathPlanning);
			if (this.result != null)
				return this;
		}

		return this;
	}

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

	// cywEnd
	// -------------------------这边是可以采用的
	// ---------------------------------------------------------------------------------
	private Action calcRescue(AmbulanceTeam agent, PathPlanning pathPlanning, EntityID targetID) {// ------------救援挖
		StandardEntity targetEntity = this.worldInfo.getEntity(targetID);
		if (targetEntity == null) {
			return null;
		}
		EntityID agentPosition = agent.getPosition();
		if (targetEntity instanceof Human) {
			Human human = (Human) targetEntity;
			if (!human.isPositionDefined()) {
				return null;
			}
			if (human.isHPDefined() && human.getHP() == 0 ) {// 如果在路中就会死亡
				return null;
			}
			EntityID targetPosition = human.getPosition();
			if (agentPosition.getValue() == targetPosition.getValue()) {
				if (human.isBuriednessDefined() && human.getBuriedness() > 0) {
					return new ActionRescue(human);
				} else if (human.getStandardURN() == CIVILIAN) {
					return new ActionLoad(human.getID());
				}
			} else {
				List<EntityID> path = pathPlanning.getResult(agentPosition, targetPosition);
				if (path != null && path.size() > 0) {
					return new ActionMove(path);
				}
			}
			return null;
		}
		if (targetEntity.getStandardURN() == BLOCKADE) {
			Blockade blockade = (Blockade) targetEntity;
			if (blockade.isPositionDefined()) {
				targetEntity = this.worldInfo.getEntity(blockade.getPosition());
			}
		}
		if (targetEntity instanceof Area) {
			List<EntityID> path = pathPlanning.getResult(agentPosition, targetEntity.getID());
			if (path != null && path.size() > 0) {
				this.result = new ActionMove(path);
			}
		}
		return null;
	}

	private Action calcUnload(AmbulanceTeam agent, PathPlanning pathPlanning, Human transportHuman, EntityID targetID) {// 这个函数的作用就是用来
		if (transportHuman == null) {
			return null;
		}
		if (transportHuman.isHPDefined() && transportHuman.getHP() == 0) {
			return new ActionUnload();// 会NULL，这边是否Defined问一问学长
		}

		EntityID agentPosition = agent.getPosition();
		StandardEntity position = this.worldInfo.getEntity(agentPosition);


		if (transportHuman.isDamageDefined() &&
			position.getStandardURN() == ROAD &&
			transportHuman.getDamage() == 0) {
			EntityID human= transportHuman.getID();
			Human onloadhuman=(Human)this.worldInfo.getEntity(human);;
			//System.out.println("Damage****new:"+onloadhuman.getDamage()+"DamegePor"+onloadhuman.getDamageProperty().getValue());
			//System.out.println("Damage====old"+transportHuman.getDamage());
			return new ActionUnload();
		}

		if (targetID == null || transportHuman.getID().getValue() == targetID.getValue()) {
			if (position != null && position.getStandardURN() == REFUGE) {
				return new ActionUnload();// 在救助站里面的话也继续和没血条一样
			} else {
				pathPlanning.setFrom(agentPosition);
				pathPlanning.setDestination(this.worldInfo.getEntityIDsOfType(REFUGE));
				List<EntityID> path = pathPlanning.calc().getResult();
				if (path != null && path.size() > 0) {
					return new ActionMove(path);
				}
			}
		}

		if (targetID == null) {
			return null;
		}

		StandardEntity targetEntity = this.worldInfo.getEntity(targetID);

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

	private boolean needRest(Human agent) {// --------只能题是否需要救援，回家,救援的是在燃烧建筑物的人，困的不会死
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

	private Action calcRefugeAction(Human human, PathPlanning pathPlanning, Collection<EntityID> targets,
			boolean isUnload) {
		EntityID position = human.getPosition();
		Collection<EntityID> refuges = this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE);
		int size = refuges.size();
		if (refuges.contains(position)) {
			return isUnload ? new ActionUnload() : new ActionRest();
		}
		List<EntityID> firstResult = null;
		while (refuges.size() > 0) {
			pathPlanning.setFrom(position);
			pathPlanning.setDestination(refuges);
			List<EntityID> path = pathPlanning.calc().getResult();
			if (path != null && path.size() > 0) {
				if (firstResult == null) {
					firstResult = new ArrayList<>(path);
					if (targets == null || targets.isEmpty()) {
						break;
					}
				}
				EntityID refugeID = path.get(path.size() - 1);
				pathPlanning.setFrom(refugeID);
				pathPlanning.setDestination(targets);
				List<EntityID> fromRefugeToTarget = pathPlanning.calc().getResult();
				if (fromRefugeToTarget != null && fromRefugeToTarget.size() > 0) {
					return new ActionMove(path);
				}
				refuges.remove(refugeID);
				// remove failed
				if (size == refuges.size()) {
					break;
				}
				size = refuges.size();
			} else {
				break;
			}
		}
		return firstResult != null ? new ActionMove(firstResult) : null;
	}
}
