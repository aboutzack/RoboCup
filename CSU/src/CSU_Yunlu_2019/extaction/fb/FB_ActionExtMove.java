
package CSU_Yunlu_2019.extaction.fb;

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
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

import CSU_Yunlu_2019.module.algorithm.DebugLog;
import CSU_Yunlu_2019.module.algorithm.StuckDetector;
import CSU_Yunlu_2019.standard.EntranceHelper;

public class FB_ActionExtMove extends ExtAction {

	    private PathPlanning pathPlanning;

	    private int thresholdRest;
	    private int kernelTime;

		private EntityID target;
		
		private final int MAXTRIAL = 200;
		private final int MAXRANGE = 50000;
		private StuckDetector cirDetecor;
		private DebugLog logger;
		private Random random = new Random();
		private EntranceHelper entrances;

	    public FB_ActionExtMove(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, DevelopData developData) {
	        super(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
	        this.target = null;
	        this.thresholdRest = developData.getInteger("ActionExtMove.rest", 100);

	        switch  (scenarioInfo.getMode()) {
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
			
			cirDetecor = new StuckDetector(agentInfo);
			logger = new DebugLog(agentInfo);

			entrances = new EntranceHelper(worldInfo);
	    }

	    @Override
	    public ExtAction precompute(PrecomputeData precomputeData) {
	        super.precompute(precomputeData);
	        if(this.getCountPrecompute() >= 2) {
	            return this;
	        }
	        this.pathPlanning.precompute(precomputeData);
	        try {
	            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
	        }catch (NoSuchConfigOptionException e) {
	            this.kernelTime = -1;
	        }
	        return this;
	    }

	    @Override
	    public ExtAction resume(PrecomputeData precomputeData) {
	        super.resume(precomputeData);
	        if(this.getCountResume() >= 2) {
	            return this;
	        }
	        this.pathPlanning.resume(precomputeData);
	        try {
	            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
	        }catch (NoSuchConfigOptionException e) {
	            this.kernelTime = -1;
	        }
	        return this;
	    }

	    @Override
	    public ExtAction preparate() {
	        super.preparate();
	        if(this.getCountPreparate() >= 2) {
	            return this;
	        }
	        this.pathPlanning.preparate();
	        try {
	            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
	        }catch (NoSuchConfigOptionException e) {
	            this.kernelTime = -1;
	        }
	        return this;
	    }

	    @Override
	    public ExtAction updateInfo(MessageManager messageManager){
	        super.updateInfo(messageManager);
	        if(this.getCountUpdateInfo() >= 2) {
	            return this;
	        }
	        this.pathPlanning.updateInfo(messageManager);
	        return this;
	    }

	    @Override
	    public ExtAction setTarget(EntityID target) {
	        this.target = null;
	        StandardEntity entity = this.worldInfo.getEntity(target);
	        if(entity != null) {
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
			Human agent = (Human)this.agentInfo.me();
			
			cirDetecor.update(agent.getPosition());
			cirDetecor.warnStuck();

			if (cirDetecor.isStucked()) {
				this.result = avoidStuck();
				if (this.result != null)
					return this;
			}

	        if(this.needRest(agent)) {
	            this.result = this.calcRest(agent, this.pathPlanning, this.target);
	            if(this.result != null) {
	                return this;
	            }
	        }
	        if(this.target == null) {
	            return this;
			}
			
			this.pathPlanning.setFrom(agent.getPosition());
			this.pathPlanning.setDestination(this.target);
			List<EntityID> path = this.pathPlanning.calc().getResult();
			if (path != null && path.size() > 0) {
				this.result = new ActionMove(path);
			}
			
			return this;
		}
		
		public ActionMove avoidStuck() {
			EntityID position = agentInfo.getPosition();

			Collection<StandardEntity> neighbors = worldInfo.getObjectsInRange(agentInfo.getID(), MAXRANGE);
			Object[] array = neighbors.toArray();

			if (array.length > 0) {
				StandardEntity ne = (StandardEntity) array[random.nextInt(array.length)];
				int cnt = 0;
				while (cnt < MAXTRIAL) {
					++cnt;
					if ((ne instanceof Building && !((Building)ne).isOnFire()) ||
						(ne instanceof Blockade) ||
						(ne instanceof Human) ||
						(ne.getID().getValue() == agentInfo.getID().getValue()) ||
						(position.getValue() == ne.getID().getValue()))
					{
						ne = (StandardEntity) array[random.nextInt(array.length)];
						continue;
					}

					this.pathPlanning.setFrom(position);
					if (ne instanceof Building) {
						Collection<Road> entr = entrances.getEntrance((Building)ne);
						Collection<EntityID> entrids = new LinkedList<>();
						for (Road e : entr) {
							entrids.add(e.getID());
						}
						this.pathPlanning.setDestination(entrids);
					} else {
						this.pathPlanning.setDestination(ne.getID());
					}

					List<EntityID> path = this.pathPlanning.calc().getResult();
					if (path != null && path.size() > 0) {
						logger.log("[AvoidStuck]: neighbors: " + array.length);
						logger.log("[AvoidStuck] Succeed");
						return new ActionMove(path);
					}
				}
			}

			logger.log("[AvoidStuck]: neighbors: " + array.length);
			logger.log("[AvoidStuck] Failed.");
			return null;
		}

	    private boolean needRest(Human agent) {
	        int hp = agent.getHP();
	        int damage = agent.getDamage();
	        if(hp == 0 || damage == 0) {
	            return false;
	        }
	        int activeTime = (hp / damage) + ((hp % damage) != 0 ? 1 : 0);
	        if(this.kernelTime == -1) {
	            try {
	                this.kernelTime = this.scenarioInfo.getKernelTimesteps();
	            }catch (NoSuchConfigOptionException e) {
	                this.kernelTime = -1;
	            }
	        }
	        return damage >= this.thresholdRest || (activeTime + this.agentInfo.getTime()) < this.kernelTime;
	    }

	    private Action calcRest(Human human, PathPlanning pathPlanning, EntityID target) {
	        EntityID position = human.getPosition();
	        Collection<EntityID> refuges = this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE);
	        int currentSize = refuges.size();
	        if(refuges.contains(position)) {
	            return new ActionRest();
	        }
	        List<EntityID> firstResult = null;
	        while(refuges.size() > 0) {
	            pathPlanning.setFrom(position);
	            pathPlanning.setDestination(refuges);
	            List<EntityID> path = pathPlanning.calc().getResult();
	            if (path != null && path.size() > 0) {
	                if (firstResult == null) {
	                    firstResult = new ArrayList<>(path);
	                    if(target == null) {
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
	

}
