package CSU_Yunlu_2020.module.complex.fb;

import CSU_Yunlu_2020.CSUConstants;
import CSU_Yunlu_2020.debugger.DebugHelper;
import CSU_Yunlu_2020.util.Util;
import CSU_Yunlu_2020.world.CSUWorldHelper;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.information.*;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.communication.CommunicationMessage;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.Search;
import adf.debug.TestLogger;
import org.apache.log4j.Logger;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class CSUSearchForFire extends Search {
	private PathPlanning pathPlanning;
	private Clustering clustering;

	private List<EntityID> unsearchedRoadIDs;
	private int threshold=5;
	private int SearchTime=threshold;
	private int SearchclusterIndex=0;
	private boolean BurnOut = false;

	private boolean fireInCluster = false;

	private boolean Distributed=false;

	private EntityID result;
	private Logger logger;
	private CSUWorldHelper world;

	public CSUSearchForFire(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
		super(ai, wi, si, moduleManager, developData);
		logger = TestLogger.getLogger(agentInfo.me());

		this.unsearchedRoadIDs=new ArrayList<>();
		//System.out.println("************\n\nSearchForFireInit\n\n");
		StandardEntityURN agentURN = ai.me().getStandardURN();
		if (agentURN == AMBULANCE_TEAM) {
				this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Ambulance",
						"adf.sample.module.algorithm.AStarPathPlanning");
				this.clustering = moduleManager.getModule("SampleSearch.Clustering.Ambulance",
						"adf.sample.module.algorithm.SampleKMeans");
				world = moduleManager.getModule("WorldHelper.Default", CSUConstants.WORLD_HELPER_DEFAULT);

		} else if (agentURN == FIRE_BRIGADE) {
				this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire",
						"adf.sample.module.algorithm.AStarPathPlanning");
				this.clustering = moduleManager.getModule("SampleSearch.Clustering.Fire",
						"adf.sample.module.algorithm.SampleKMeans");
				world = moduleManager.getModule("WorldHelper.FireBrigade", CSUConstants.WORLD_HELPER_FIRE_BRIGADE);
		} else if (agentURN == POLICE_FORCE) {
				this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Police",
						"adf.sample.module.algorithm.AStarPathPlanning");
				this.clustering = moduleManager.getModule("SampleSearch.Clustering.Police",
						"adf.sample.module.algorithm.SampleKMeans");
				world = moduleManager.getModule("WorldHelper.FireBrigade", CSUConstants.WORLD_HELPER_FIRE_BRIGADE);
		}
		registerModule(this.clustering);
		registerModule(this.pathPlanning);
	}

	@Override
	public Search updateInfo(MessageManager messageManager) {
		logger.debug("Time:" + agentInfo.getTime());
		super.updateInfo(messageManager);
		this.reflectMessage(messageManager);

		this.unsearchedRoadIDs.removeAll(this.worldInfo.getChanged().getChangedEntities());
		fireInCluster = this.updateFire();//判断搜索分区内有没有着火的建筑
//		this.BurnOut = this.Burnout();//如果分区内建筑全烧毁了就为TRUE，有没烧毁的为FALSE
		if(this.fireInCluster)//有着火的
			this.SearchTime = threshold;
		if(BurnOut)//全烧毁了
		{
			this.SearchTime=0;
			this.reset();
		}
		if(this.unsearchedRoadIDs.isEmpty()) {
			this.reset();
		}
		this.unsearchedRoadIDs.removeAll(this.worldInfo.getChanged().getChangedEntities());
		if(this.unsearchedRoadIDs.isEmpty()) {
			this.reset();
			unsearchedRoadIDs.removeAll(this.worldInfo.getChanged().getChangedEntities());
		}
		return this;
	}

	@Override
	public Search calc() {
		EntityID exploreTarget = world.getSearchTarget();
		if (exploreTarget != null) {
			this.result = exploreTarget;
			return this;
		}

		this.result = null;
		if (unsearchedRoadIDs.isEmpty()){
			reset();
		}
		this.pathPlanning.setFrom(this.agentInfo.getPosition());
		this.pathPlanning.setDestination(this.unsearchedRoadIDs);
		List<EntityID> path = this.pathPlanning.calc().getResult();
		if (path != null) {
			this.result = path.get(path.size() - 1);
		}
		if (result == null) {
			//没找到一条路去搜索
			StandardEntity selfPosition = world.getSelfPosition();
			boolean allMyEdgesImpassableInArea = world.getGraph().allMyEdgesImpassableInArea(selfPosition.getID());
			if (allMyEdgesImpassableInArea) {
				//自己所在的路被判定为不能走
				this.result = unsearchedRoadIDs.get(agentInfo.getTime() % unsearchedRoadIDs.size());
			}else {
				//所有未搜索的地方均不能到达
				reset();
				this.pathPlanning.setFrom(this.agentInfo.getPosition());
				this.pathPlanning.setDestination(this.unsearchedRoadIDs);
				path = this.pathPlanning.calc().getResult();
				if (path != null) {
					this.result = path.get(path.size() - 1);
				}
			}
		}
		visualDebug();
		return this;

	}

	private void reset() {
		fireInCluster = updateFire();
		if(this.Distributed==false)
		{
			this.SearchclusterIndex=this.clustering.getClusterIndex(this.agentInfo.getID());
			this.Distributed=true;
		}
		if(this.agentInfo.me() instanceof FireBrigade)
		{
			if(this.fireInCluster == true)
				this.SearchTime = threshold;
			else
				this.SearchTime -= 1;

			if(this.SearchTime > 0)
			{
				this.unsearchedRoadIDs.clear();
				Collection<StandardEntity> clusterEntities = null;
				if(this.clustering != null) {
					clusterEntities = this.clustering.getClusterEntities(this.SearchclusterIndex);
				}
				if(clusterEntities != null && clusterEntities.size() > 0)
				{
					for(StandardEntity entity : clusterEntities)
					{
						if(entity instanceof Road)
						{	Road road=(Road) entity;
							boolean ifadd=true;
							for(EntityID entityid:road.getNeighbours())
							{
								if(this.worldInfo.getEntity(entityid) instanceof Building)
								{
									ifadd=false;
									break;
								}
							}
							if(ifadd)
								this.unsearchedRoadIDs.add(entity.getID());
						}
					}
				}
				else
				{
					this.unsearchedRoadIDs.addAll(this.worldInfo.getEntityIDsOfType(
							StandardEntityURN.ROAD,
							StandardEntityURN.GAS_STATION,
							StandardEntityURN.AMBULANCE_CENTRE,
							StandardEntityURN.FIRE_STATION,
							StandardEntityURN.POLICE_OFFICE
					));
				}
			}
			else
			{
				this.SearchTime=threshold;
				this.SearchclusterIndex=(this.SearchclusterIndex+1)%this.clustering.getClusterNumber();
				this.unsearchedRoadIDs.clear();
				Collection<StandardEntity> clusterEntities = null;
				if(this.clustering != null) {
					clusterEntities = this.clustering.getClusterEntities(this.SearchclusterIndex);
				}
				if(clusterEntities != null && clusterEntities.size() > 0)
				{
					for(StandardEntity entity : clusterEntities)
					{
						if(entity instanceof Road)
						{this.unsearchedRoadIDs.add(entity.getID());}
					}
				}
				else
				{
					this.unsearchedRoadIDs.addAll(this.worldInfo.getEntityIDsOfType(
							StandardEntityURN.ROAD,
							StandardEntityURN.GAS_STATION,
							StandardEntityURN.AMBULANCE_CENTRE,
							StandardEntityURN.FIRE_STATION,
							StandardEntityURN.POLICE_OFFICE
					));
				}
			}
		}
	}

	@Override
	public EntityID getTarget() {
		return this.result;
	}

	private boolean updateFire(){
		for(StandardEntity entity:this.clustering.getClusterEntities(this.SearchclusterIndex))
		{
			if(entity instanceof Building && ((Building) entity).isOnFire())
				return true;
		}
		return false;
	}

	private boolean Burnout()
	{
	
		for(EntityID entityid:this.worldInfo.getChanged().getChangedEntities())
		{
			if(entityid != null)
			{
				StandardEntity se = this.worldInfo.getEntity(entityid);
				if(se instanceof Building)
				{
					Building b = (Building) se;
					if(b.isFierynessDefined() && b.getFieryness() < 8)
						return false;
				}
			}
		}
		return true;
	}

	private void reflectMessage(MessageManager messageManager) {
		
		Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
		changedEntities.add(this.agentInfo.getID());
		int time = this.agentInfo.getTime();

		for(CommunicationMessage message : messageManager.getReceivedMessageList()) {
			Class<? extends CommunicationMessage> messageClass = message.getClass();
			if(messageClass == MessageBuilding.class) {
				MessageBuilding mb = (MessageBuilding)message;
				if(!changedEntities.contains(mb.getBuildingID())) {
					MessageUtil.reflectMessage(this.worldInfo, mb);
				}
			} else if(messageClass == MessageRoad.class) {
				MessageRoad mr = (MessageRoad)message;
				if(mr.isBlockadeDefined() && !changedEntities.contains(mr.getBlockadeID())) {
					MessageUtil.reflectMessage(this.worldInfo, mr);
				}

			} else if(messageClass == MessageCivilian.class) {
				MessageCivilian mc = (MessageCivilian) message;
				if(!changedEntities.contains(mc.getAgentID())){
					MessageUtil.reflectMessage(this.worldInfo, mc);
				}

			} else if(messageClass == MessageAmbulanceTeam.class) {
				MessageAmbulanceTeam mat = (MessageAmbulanceTeam)message;
				if(!changedEntities.contains(mat.getAgentID())) {
					MessageUtil.reflectMessage(this.worldInfo, mat);
				}

			} else if(messageClass == MessageFireBrigade.class) {
				MessageFireBrigade mfb = (MessageFireBrigade) message;
				if(!changedEntities.contains(mfb.getAgentID())) {
					MessageUtil.reflectMessage(this.worldInfo, mfb);
				}

			} else if(messageClass == MessagePoliceForce.class) {
				MessagePoliceForce mpf = (MessagePoliceForce) message;
				if(!changedEntities.contains(mpf.getAgentID())) {
					MessageUtil.reflectMessage(this.worldInfo, mpf);
				}

			}
		}
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

}
