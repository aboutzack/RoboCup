package AUR.module.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

import AUR.util.HungarianAlgorithm;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.StaticClustering;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

public class AURKMeans extends StaticClustering {

	private Collection<StandardEntity> 			entities;
	private Map<Integer, List<StandardEntity>> 	clusterEntitiesList;
	private Map<DoublePoint, StandardEntity> 	entitiesList;
	private List<List<EntityID>> 				clusterEntityIDsList;
	
	private Collection<StandardEntity> 			roads;
	private List<RoadNode> 						listRoadNodes;
	
	private int 								clusterSize;
	private int 								repeat;
	private int 								randomSeed;
	private double								costs[][];
	
	public AURKMeans(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager,
			DevelopData developData) {
		super(ai, wi, si, moduleManager, developData);

		this.clusterEntityIDsList 	= new ArrayList<>();
		this.clusterEntitiesList 	= new HashMap<>();
		this.listRoadNodes 			= new ArrayList<>();
		// int clusterS         		= si.getScenarioAgentsPf();
		int clusterS         		= worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE).size();
		this.clusterSize 			= developData.getInteger("sample.module.SampleKMeans.clusterSize",	(clusterS == 0) ? 10 : clusterS);
		this.repeat      			= developData.getInteger("AuraKMeans.repeat", 30);
		this.randomSeed  			= developData.getInteger("AuraKMeans.randomSeed", 50);
		this.entities    			= wi.getAllEntities();
		this.roads 	   				= wi.getAllEntities();
		this.costs					= new double[clusterSize][clusterSize];
		
		for (StandardEntity se : roads)
		{
			if (se instanceof Road)
			{
				RoadNode roadNode = new RoadNode();
				roadNode.setEntityID(se.getID().getValue());
				
				for (EntityID eid : ((Area) se).getNeighbours())
				{
					if (this.worldInfo.getEntity(eid) instanceof Building)
						continue;
					roadNode.setNeighborEntityID(eid.getValue());
				}
				roadNode.setX((Integer)se.getProperty("urn:rescuecore2.standard:property:x").getValue());
				roadNode.setY((Integer)se.getProperty("urn:rescuecore2.standard:property:y").getValue());
				
				listRoadNodes.add(roadNode);
			}
		}
	}

	@Override
	public Clustering calc() 
	{
		return this;
	}

	@Override
	public Collection<StandardEntity> getClusterEntities(int i) {
		return clusterEntitiesList.get(i);
	}

	@Override
	public Collection<EntityID> getClusterEntityIDs(int i) {
		return clusterEntityIDsList.get(i);
	}

	@Override
	public int getClusterIndex(StandardEntity seTemp) {
		for (int i = 0; i < clusterEntitiesList.size(); i++) {
			List<StandardEntity> lse = clusterEntitiesList.get(i);
			for (StandardEntity se : lse) {
				if (se.equals(seTemp))
					return i;
			}
		}
		return -1;
	}

	@Override
	public int getClusterIndex(EntityID eidTemp) {
		for (int i = 0; i < clusterEntityIDsList.size(); i++) {
			List<EntityID> leid = clusterEntityIDsList.get(i);
			for (EntityID eid : leid) {
				if (eid.equals(eidTemp))
					return i;
			}
		}
		return -1;
	}

	@Override
	public int getClusterNumber() {
		return clusterEntitiesList.size();
	}

	@Override
	public Clustering preparate() {
		super.preparate();
		if (this.getCountPreparate() >= 2) {
			return this;
		}
		
		while (edgeRemoved().size() != 0)
		{}

		List<StandardEntity> listStandardEntityRoadNode = new ArrayList<>();
		for (RoadNode roadNode : listRoadNodes) 
		{
			StandardEntity standardEntityRoadNode = this.worldInfo.getEntity(new EntityID(roadNode.getEntityID()));
			if (roadNode.listNeighborEntityIDs.size() <= 2 ) continue;
			listStandardEntityRoadNode.add(standardEntityRoadNode);
		}
		
		entitiesList = new HashMap<>();

		Collection<DoublePoint> cd = new ArrayList<>();
		for (StandardEntity se : entities) 
		{
			if (se instanceof Building)
			{
				double x = (int) se.getProperty("urn:rescuecore2.standard:property:x").getValue();
				double y = (int) se.getProperty("urn:rescuecore2.standard:property:y").getValue();
				DoublePoint dp = new DoublePoint(new double[] { x, y });
				entitiesList.put(dp, se);
				cd.add(dp);
			}
		}
		
		for (StandardEntity se : listStandardEntityRoadNode) 
		{
			double x = (int) se.getProperty("urn:rescuecore2.standard:property:x").getValue();
			double y = (int) se.getProperty("urn:rescuecore2.standard:property:y").getValue();
			DoublePoint dp = new DoublePoint(new double[] { x, y });
			entitiesList.put(dp, se);
			cd.add(dp);
		}
		
		KMeansPlusPlusClusterer<DoublePoint> dbscan = new KMeansPlusPlusClusterer<DoublePoint>(this.clusterSize, this.repeat);
		dbscan.getRandomGenerator().setSeed(this.randomSeed);
		List<CentroidCluster<DoublePoint>> lcd = dbscan.cluster(cd);

		for (int i = 0; i < lcd.size(); i++) {
			List<StandardEntity> lse = new ArrayList<>();
			List<EntityID> leid = new ArrayList<>();
			for (DoublePoint dp : lcd.get(i).getPoints()) {
				lse.add(entitiesList.get(dp));
				leid.add(entitiesList.get(dp).getID());
			}
			clusterEntitiesList.put(i, lse);
			clusterEntityIDsList.add(leid);
		}

		List<StandardEntity> policeforceList = new ArrayList<>(this.worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE));
		policeforceList.sort(new Comparator<StandardEntity>()
		{
			@Override
			public int compare(StandardEntity o1, StandardEntity o2)
			{
				if (o1.getID().getValue() > o2.getID().getValue()) return 1;
				else return -1;
			}
		});
		
		int indexAgent = 0;
		while(indexAgent < lcd.size())
		{
			PoliceForce pf = (PoliceForce) policeforceList.get(indexAgent);
			EntityID posEI = pf.getPosition();
			Area aa = (Area) this.worldInfo.getEntity(posEI);
		
			for (int i =0 ;i < lcd.size(); i++)
			{
				List<StandardEntity> listSECluster = clusterEntitiesList.get(i);
				double ave = 0;
				double x =0, y=0;
	
				for (StandardEntity se : listSECluster)
				{
					Area area = (Area) se;
					x += area.getX();
					y += area.getY();
				}
				x/= listSECluster.size();
				y/= listSECluster.size();
				
				ave = GeometryTools2D.getDistance(new Point2D(aa.getX(), aa.getY()), new Point2D(x, y));
				
				costs[indexAgent][i] = (int)ave;
			}
			indexAgent++;
		}
		
		HungarianAlgorithm ha = new HungarianAlgorithm(costs);
		int[] assigns = ha.execute();
		
		this.assignAgents(assigns, policeforceList);
		return this;
	}
	
	private List<Integer> edgeRemoved() 
	{
		List<Integer> removedEntity = new ArrayList<>();
		for (int i = 0; i < listRoadNodes.size(); i++)
		{
			RoadNode roadNode = listRoadNodes.get(i);
			if (roadNode.getNeighborEntityIDs().size() != 0)
			{
				if (roadNode.getNeighborEntityIDs().size() == 1)
				{
					List<Integer> leidRoadNeighbour = roadNode.getNeighborEntityIDs();
					for (Integer EntityIDValue : leidRoadNeighbour)
					{
						for (int j = 0 ; j < listRoadNodes.size() ; j++)
						{
							RoadNode TRoad = listRoadNodes.get(j);
							if (TRoad.getEntityID() == EntityIDValue)
							{
								TRoad.getNeighborEntityIDs().remove(new Integer(roadNode.getEntityID()));
								break;
							}
						}
					}
					listRoadNodes.remove(roadNode);
					removedEntity.add(roadNode.getEntityID());
				}
			}
			else
			{
				listRoadNodes.remove(roadNode);
				removedEntity.add(roadNode.getEntityID());
			}
		}
		return removedEntity;
	}

	private void assignAgents(int[] assigns, List<StandardEntity> agentList) 
	{
		int clusterIndex = 0;
		for (StandardEntity agent : agentList) {
			this.clusterEntitiesList.get(assigns[clusterIndex]).add(agent);
			this.clusterEntityIDsList.get(assigns[clusterIndex]).add(agent.getID());
			clusterIndex++;
			if (clusterIndex >= this.getClusterNumber()) {
				clusterIndex = 0;
			}
		}
	}
	
	private class RoadNode
	{
		private int entityID;
		private List<Integer> listNeighborEntityIDs = new ArrayList<>();
		
		private int x,y;
		
		public int getEntityID() {
			return entityID;
		}
		public void setEntityID(int entityID) {
			this.entityID = entityID;
		}
		public List<Integer> getNeighborEntityIDs() {
			return listNeighborEntityIDs;
		}
		public void setNeighborEntityID(Integer neighborEntityID) {
			this.listNeighborEntityIDs.add(neighborEntityID);
		}
		public int getX() {
			return x;
		}
		public void setX(int x) {
			this.x = x;
		}
		public int getY() {
			return y;
		}
		public void setY(int y) {
			this.y = y;
		}
		@Override
		public String toString() {
			return "RoadNode [entityID=" + entityID + ", listNeighborEntityIDs=" + listNeighborEntityIDs + "]";
		}
	}
}
