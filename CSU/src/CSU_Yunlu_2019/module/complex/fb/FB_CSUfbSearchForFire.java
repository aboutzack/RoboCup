package CSU_Yunlu_2019.module.complex.fb;

import static rescuecore2.standard.entities.StandardEntityURN.AMBULANCE_TEAM;
import static rescuecore2.standard.entities.StandardEntityURN.FIRE_BRIGADE;
import static rescuecore2.standard.entities.StandardEntityURN.POLICE_FORCE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import adf.debug.TestLogger;
import org.apache.log4j.Logger;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.information.MessageAmbulanceTeam;
import adf.agent.communication.standard.bundle.information.MessageBuilding;
import adf.agent.communication.standard.bundle.information.MessageCivilian;
import adf.agent.communication.standard.bundle.information.MessageFireBrigade;
import adf.agent.communication.standard.bundle.information.MessagePoliceForce;
import adf.agent.communication.standard.bundle.information.MessageRoad;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.communication.CommunicationMessage;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.Search;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

public class FB_CSUfbSearchForFire extends Search {
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

	public FB_CSUfbSearchForFire(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
		super(ai, wi, si, moduleManager, developData);
		logger = TestLogger.getLogger(agentInfo.me());

		this.unsearchedRoadIDs=new ArrayList<>();
		//System.out.println("************\n\nSearchForFireInit\n\n");
		StandardEntityURN agentURN = ai.me().getStandardURN();
		if (agentURN == AMBULANCE_TEAM) {
				this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Ambulance",
						"adf.sample.module.algorithm.AStarPathPlanning");
				this.clustering = moduleManager.getModule("TestSearch.Clustering.Ambulance",
						"adf.sample.module.algorithm.at.AT_SampleKMeans");
			} else if (agentURN == FIRE_BRIGADE) {
				this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire",
						"adf.sample.module.algorithm.AStarPathPlanning");
				this.clustering = moduleManager.getModule("TestSearch.Clustering.Fire",
						"adf.sample.module.algorithm.fb.FB_SampleKMeans");
			} else if (agentURN == POLICE_FORCE) {
				this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Police",
						"adf.sample.module.algorithm.AStarPathPlanning");
				this.clustering = moduleManager.getModule("TestSearch.Clustering.Police",
						"adf.sample.module.algorithm.pf.PF_SampleKMeans");
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
		this.BurnOut = this.Burnout();//如果分区内建筑全烧毁了就为TRUE，有没烧毁的为FALSE
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
		this.result = null;
		if (unsearchedRoadIDs.isEmpty())
			return this;
		this.pathPlanning.setFrom(this.agentInfo.getPosition());
		this.pathPlanning.setDestination(this.unsearchedRoadIDs);
		List<EntityID> path = this.pathPlanning.calc().getResult();
		if (path != null) {
			this.result = path.get(path.size() - 1);
		}
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

}
