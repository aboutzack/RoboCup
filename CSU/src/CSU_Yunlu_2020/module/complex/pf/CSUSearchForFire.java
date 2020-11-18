package CSU_Yunlu_2020.module.complex.pf;

import CSU_Yunlu_2020.CSUConstants;
import CSU_Yunlu_2020.debugger.DebugHelper;
import CSU_Yunlu_2020.util.Util;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.information.MessageRoad;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.Search;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class CSUSearchForFire extends Search {
	private PathPlanning pathPlanning;
	private Clustering clustering;

	private EntityID result;
	private Collection<EntityID> unsearchedRoadIDs;
	private Collection<EntityID> searchedRoadIDs;
	private int searchedClusterIndexes[] ;
	private int clusterCount = 0;
	private List<EntityID> blockedRoad = new ArrayList<>();
	private List<EntityID> clearedRoad = new ArrayList<>();
	private MessageManager messageManager =null;

	public CSUSearchForFire(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager,
							DevelopData developData) {
		super(ai, wi, si, moduleManager, developData);

		this.unsearchedRoadIDs = new HashSet<>();

		StandardEntityURN agentURN = ai.me().getStandardURN();
		switch (si.getMode()) {
			case PRECOMPUTATION_PHASE:
				if (agentURN == AMBULANCE_TEAM) {
					this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Ambulance",
							"adf.sample.module.algorithm.SamplePathPlanning");
					this.clustering = moduleManager.getModule("SampleSearch.Clustering.Ambulance",
							"adf.sample.module.algorithm.SampleKMeans");
				} else if (agentURN == FIRE_BRIGADE) {
					this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire",
							"adf.sample.module.algorithm.SamplePathPlanning");
					this.clustering = moduleManager.getModule("SampleSearch.Clustering.Fire",
							"adf.sample.module.algorithm.SampleKMeans");
				} else if (agentURN == POLICE_FORCE) {
					this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Police",
							"adf.sample.module.algorithm.SamplePathPlanning");
					this.clustering = moduleManager.getModule("SampleSearch.Clustering.Police",
							"adf.sample.module.algorithm.SampleKMeans");
				}
				break;
			case PRECOMPUTED:
				if (agentURN == AMBULANCE_TEAM) {
					this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Ambulance",
							"adf.sample.module.algorithm.SamplePathPlanning");
					this.clustering = moduleManager.getModule("SampleSearch.Clustering.Ambulance",
							"adf.sample.module.algorithm.SampleKMeans");
				} else if (agentURN == FIRE_BRIGADE) {
					this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire",
							"adf.sample.module.algorithm.SamplePathPlanning");
					this.clustering = moduleManager.getModule("SampleSearch.Clustering.Fire",
							"adf.sample.module.algorithm.SampleKMeans");
				} else if (agentURN == POLICE_FORCE) {
					this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Police",
							"adf.sample.module.algorithm.SamplePathPlanning");
					this.clustering = moduleManager.getModule("SampleSearch.Clustering.Police",
							"adf.sample.module.algorithm.SampleKMeans");
				}
				break;
			case NON_PRECOMPUTE:
				if (agentURN == AMBULANCE_TEAM) {
					this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Ambulance",
							"adf.sample.module.algorithm.SamplePathPlanning");
					this.clustering = moduleManager.getModule("SampleSearch.Clustering.Ambulance",
							"adf.sample.module.algorithm.SampleKMeans");
				} else if (agentURN == FIRE_BRIGADE) {
					this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire",
							"adf.sample.module.algorithm.SamplePathPlanning");
					this.clustering = moduleManager.getModule("SampleSearch.Clustering.Fire",
							"adf.sample.module.algorithm.SampleKMeans");
				} else if (agentURN == POLICE_FORCE) {
					this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Police",
							"adf.sample.module.algorithm.SamplePathPlanning");
					this.clustering = moduleManager.getModule("SampleSearch.Clustering.Police",
							"adf.sample.module.algorithm.SampleKMeans");
				}
				break;
		}

		registerModule(this.pathPlanning);
		registerModule(this.clustering);
	}

	@Override
	public Search updateInfo(MessageManager messageManager) {
		super.updateInfo(messageManager);
		if (this.getCountUpdateInfo() >= 2) {
			return this;
		}

		this.unsearchedRoadIDs.removeAll(this.worldInfo.getChanged().getChangedEntities());

		if (this.unsearchedRoadIDs.isEmpty()) {
			this.reset();
			this.unsearchedRoadIDs.removeAll(this.worldInfo.getChanged().getChangedEntities());
		}
		this.messageManager = messageManager;
		this.pathPlanning.updateInfo(messageManager);
		this.clustering.updateInfo(messageManager);
		return this;
	}

	@Override
	public Search calc() {
		if (agentInfo.getTime() < scenarioInfo.getKernelAgentsIgnoreuntil()) {
			return this;
		}
		this.result = null;
		this.pathPlanning.setFrom(this.agentInfo.getPosition());
		this.pathPlanning.setDestination(this.unsearchedRoadIDs);
		List<EntityID> path = this.pathPlanning.calc().getResult();
		if (path != null && path.size() > 1) {
			this.result = path.get(path.size() - 1);
		}
		visualDebug();
		return this;
	}

	private void visualDebug() {
		if (DebugHelper.DEBUG_MODE) {
			try {
				DebugHelper.drawSearchTarget(worldInfo, agentInfo.getID(), result);
				List<Integer> elementList = Util.fetchIdValueFromElementIds(unsearchedRoadIDs);
				DebugHelper.VD_CLIENT.drawAsync(agentInfo.getID().getValue(), "UnsearchedRoads", (Serializable) elementList);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void reset() {
		this.unsearchedRoadIDs.clear();
		Collection<StandardEntity> clusterEntities = null;
		if (this.clustering != null) {

			int clusterIndex = this.findNextCluster();
			clusterEntities = this.clustering.getClusterEntities(clusterIndex);
		}
		if (clusterEntities != null && clusterEntities.size() > 0) {
			for (StandardEntity entity : clusterEntities) {
				if (entity instanceof Road || entity instanceof Hydrant) {
					this.unsearchedRoadIDs.add(entity.getID());
				}
			}
		} else {
			this.unsearchedRoadIDs.addAll(this.worldInfo.getEntityIDsOfType(BUILDING, GAS_STATION, AMBULANCE_CENTRE,
					FIRE_STATION, POLICE_OFFICE));
		}
	}

	private int findNextCluster() {
		boolean flag = false;
		int clusterIndex ;
		clusterIndex = this.clustering.getClusterIndex(this.agentInfo.getID());
		for(int i = 0 ; i < this.clusterCount ; ++i) {
			if(this.searchedClusterIndexes[i] == clusterIndex) {
				flag = true;
				break;
			}
		}
		if(!flag) {
			this.searchedClusterIndexes[this.clusterCount] = clusterIndex;
			++this.clusterCount;
			return clusterIndex;
		}else {
			if(this.scenarioInfo.getCommsChannelsCount() < 2) {
				double min = Double.MAX_VALUE;
				for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)) {
					Boolean alreadyflag = false;
					int index = this.clustering.getClusterIndex(entity.getID());
					for (int i = 0; i < this.clusterCount; ++i) {
						if (this.searchedClusterIndexes[i] == index) {
							alreadyflag = true;
							break;
						}
					}
					if (!alreadyflag) {
						Collection<StandardEntity> clutserEntities = this.clustering.getClusterEntities(index);
						int sumx = 0, sumy = 0, count = 0;
						for (StandardEntity se : clutserEntities) {
							sumx += this.worldInfo.getLocation(se).first();
							sumy += this.worldInfo.getLocation(se).second();
							++count;
						}
						double dist = this.getDistance(sumx / count, sumy / count, this.agentInfo.getX(), this.agentInfo.getY());
						if (dist < min) {
							clusterIndex = index;
							min = dist;
						}
					}
				}
				this.searchedClusterIndexes[this.clusterCount] = clusterIndex;
				++this.clusterCount;

			}else {
				this.addMessageRoad(this.blockedRoad);
				this.blockedRoad.removeAll(this.clearedRoad);
				clusterIndex = this.clustering.getClusterIndex(this.agentInfo.getID());
				if (this.blockedRoad != null && !this.blockedRoad.isEmpty()) {
					double bestRate = Double.MAX_VALUE;
					for (StandardEntity se : this.worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)) {
						double rate = 0;
						double allRoad = 0;
						double blocked = 0;
						int index = this.clustering.getClusterIndex(se.getID());
						Collection<StandardEntity> clusterEntities = this.clustering.getClusterEntities(index);
						for (StandardEntity entity : clusterEntities) {
							if (entity instanceof Road || entity instanceof Hydrant) {
								allRoad++;
								if (this.blockedRoad.contains(entity.getID())) {
									blocked++;
								}
							}
						}
						if (blocked == 0) {
							continue;
						} else {
							rate = allRoad / blocked;
							if (rate < bestRate) {
								bestRate = rate;
								clusterIndex = index;
							}
						}
					}
				}
				if (this.clusterCount > this.clustering.getClusterNumber() / 2) {
					if (CSUConstants.DEBUG_BACK_TO_MY_CLUSTER) {
						System.out.println("back to my cluster");
					}
					return this.clustering.getClusterIndex(this.agentInfo.getID());
				}

				this.searchedClusterIndexes[this.clusterCount] = clusterIndex;
				++this.clusterCount;
			}
			return clusterIndex;
		}
	}
	void addMessageRoad(List<EntityID> Roadlist){
		if(messageManager.getReceivedMessageList() != null
				&& !messageManager.getReceivedMessageList().isEmpty()) {
			for (CommunicationMessage message : messageManager.getReceivedMessageList()) {
				Class<? extends CommunicationMessage> messageClass = message.getClass();
				if (messageClass == MessageRoad.class) {
					MessageRoad messageRoad = (MessageRoad) message;
					if(!messageRoad.isPassable()) {
						Roadlist.add(messageRoad.getRoadID());
					}else{
						this.clearedRoad.add(messageRoad.getRoadID());
					}
				}
			}
		}
	}


	private double getDistance(double fromX, double fromY, double toX, double toY) {
		double dx = fromX - toX;
		double dy = fromY - toY;
		return Math.hypot(dx, dy);
	}

	@Override
	public EntityID getTarget() {
		return this.result;
	}

	@Override
	public Search precompute(PrecomputeData precomputeData) {
		super.precompute(precomputeData);
		if (this.getCountPrecompute() >= 2) {
			return this;
		}
		return this;
	}

	@Override
	public Search resume(PrecomputeData precomputeData) {
		super.resume(precomputeData);
		if (this.getCountResume() >= 2) {
			return this;
		}
		this.worldInfo.requestRollback();
		this.searchedClusterIndexes = new int[this.clustering.getClusterNumber()];
		return this;
	}

	@Override
	public Search preparate() {
		super.preparate();
		if (this.getCountPreparate() >= 2) {
			return this;
		}
		this.worldInfo.requestRollback();
		this.searchedClusterIndexes = new int[this.clustering.getClusterNumber()];
		return this;
	}
}
