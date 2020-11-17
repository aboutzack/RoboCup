package CSU_Yunlu_2020.extaction.at;

import CSU_Yunlu_2020.util.ambulancehelper.CSUSelectorTargetByDis;
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
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
* @Description: ambulances' extMove
* @Author: Guanyu-Cai
* @Date: 2/15/20
*/
public class ActionExtMove extends ExtAction {
	private PathPlanning pathPlanning;

	private int thresholdRest;
	private int kernelTime;

	private EntityID target;

	private Pair<Integer, Integer> selfLocation;
	private Point lastPosition=null;

	private int lastMoveTime;
	private List<EntityID> lastmovePath;
	private final int STUCK_THRESHOLD = 2000;//threshold of stuck


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

		if (agentInfo.getTime() >= scenarioInfo.getKernelAgentsIgnoreuntil() && isStuck(path)){
//			System.out.println(this.agentInfo.getID()+" stuck!");
			// TODO: 2/16/20 比randomWalk更好的解决stuck的方法
			action = randomWalk();
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
	* @Description: 通过计算移动距离来判断是否stuck
	* @Author: Guanyu-Cai
	* @Date: 2/16/20
	*/
	private boolean isStuck(List<EntityID> path){
		if (lastMoveTime < scenarioInfo.getKernelAgentsIgnoreuntil()) {
			return false;
		}
		if (path.size() > 0) {
			EntityID target = path.get(path.size() - 1);
			if (target.equals(agentInfo.getPosition())) {
				return false;
			}
		}
		Point position = new Point(selfLocation.first(), selfLocation.second());
		int moveDistance = CSUSelectorTargetByDis.getDistance.distance(position, lastPosition);
		lastPosition = position;
		if (moveDistance <= STUCK_THRESHOLD) {
			return true;
		}else{
			return false;
		}
	}

	/**
	* @Description: 处理stuck
	* @Author: Guanyu-Cai
	* @Date: 2/18/20
	*/
	public Action randomWalk() {
		Random random = new Random();

		Collection<StandardEntity> inRnage = worldInfo.getObjectsInRange(agentInfo.getID(), 50000);
		EntityID position = agentInfo.getPosition();
		Object[] array = inRnage.toArray();

		Action action = null;
		List<EntityID> path = null;
		//找不到完全无障碍的area时，随机找一个有障碍的area，尝试走过去
		Area blockedArea = null;
		if (array.length > 0) {
			int start = random.nextInt(array.length);
			for (int i = 0; i < array.length; i++) {
				StandardEntity entity = (StandardEntity) array[(start + i) % array.length];
				if (!(entity instanceof Area)) {
					continue;
				}
				if (entity instanceof Building && ((Building) entity).isOnFire()) {
					continue;
				}
				if (entity.getID().getValue() == agentInfo.getID().getValue()) {
					continue;
				}
				if (position.getValue() == entity.getID().getValue()) {
					continue;
				}
				Area nearArea = (Area) entity;
				if (nearArea.isBlockadesDefined()) {
					blockedArea = nearArea;
					continue;
				}
				pathPlanning.setFrom(position);
				pathPlanning.setDestination(entity.getID());
				path = pathPlanning.calc().getResult();
				if (path != null && !path.isEmpty()) {
					action = new ActionMove(path);
					lastmovePath.clear();
					lastmovePath.addAll(path);
				}
				break;
			}
		}
		if (action == null && blockedArea != null) {
			pathPlanning.setFrom(position);
			pathPlanning.setDestination(blockedArea.getID());
			path = pathPlanning.calc().getResult();
			if (path != null && !path.isEmpty()) {
				action = new ActionMove(path);
				lastmovePath.clear();
				lastmovePath.addAll(path);
			}
		}
		return action;
	}
}

