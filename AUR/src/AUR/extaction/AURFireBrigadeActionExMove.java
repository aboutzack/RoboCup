package AUR.extaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import AUR.util.knd.AURWalkWatcher;
import AUR.util.knd.AURWorldGraph;
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
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

public class AURFireBrigadeActionExMove extends ExtAction {

	private int kernelTime;

	private EntityID target;

	public AURWorldGraph wsg = null;
	public AgentInfo ai = null;
	AURWalkWatcher walkWatcher = null;

	public AURFireBrigadeActionExMove(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo,
			ModuleManager moduleManager, DevelopData developData) {
		super(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
		this.target = null;
		this.wsg = moduleManager.getModule("knd.AuraWorldGraph");
		this.ai = agentInfo;
		this.walkWatcher = moduleManager.getModule("knd.AuraWalkWatcher");
	}

	public boolean dangerDest() {
		StandardEntity stdEnt = worldInfo.getEntity(target);
		if (stdEnt.getStandardURN().equals(StandardEntityURN.BUILDING) == true) {
			return true;
		}
		return false;
	}

	@Override
	public ExtAction precompute(PrecomputeData precomputeData) {
		super.precompute(precomputeData);
		if (this.getCountPrecompute() >= 2) {
			return this;
		}
		this.wsg.precompute(precomputeData);
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
		this.wsg.resume(precomputeData);
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
		this.wsg.preparate();
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
		wsg.updateInfo(messageManager);
		if (this.getCountUpdateInfo() >= 2) {
			return this;
		}
		return this;
	}

	@Override
	public ExtAction setTarget(EntityID target) {
		this.target = target;
		return this;
	}

	@Override
	public ExtAction calc() {
		ActionMove actMove = wsg.getMoveActionToPercept(ai.getPosition(), target);
//		Collection<EntityID> targets = new ArrayList<>();
//		targets.add(target);
//		ActionMove actMove = new ActionMove(wsg.getPathToClosest(ai.getPosition(), targets));
		
		if (actMove == null || actMove.getPath() == null || actMove.getPath().size() <= 0) {
			actMove = wsg.getNoBlockadeMoveAction(ai.getPosition(), target);
		}
		this.result = walkWatcher.check(actMove);
		return this;
	}
}