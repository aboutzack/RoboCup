package CSU_Yunlu_2020.module.algorithm;

import CSU_Yunlu_2020.debugger.DebugHelper;
import CSU_Yunlu_2020.module.algorithm.fb.CompositeConvexHull;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.StaticClustering;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.random.RandomGenerator;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @Description: dbscan => cluster and hungarian => agent assign
 * @Author: Aczy156
 * @Date: 10/18/20
 */



public class SampleKMeans extends StaticClustering {

	private int repeatPrecompute;
	private int repeatPreparate;
	private boolean calced = false;
	private boolean assignAgentsFlag;

	private int clusterNumber;
	private int agentSize;

	// data: whole map info
	private Collection<StandardEntity> entities;
	private ArrayList<ClusterNode> allNodes = new ArrayList<>();
	private ArrayList<StandardEntity> sortedTeamAgents = new ArrayList<>();

	// dbscan output to clusterEntitiesList, clusterEntityIDsList
	private int entityClusterIdx = -1;
	private int entityIdsClusterIdx = -1;
	private Collection<StandardEntity> lastClusterEntitiesQueryResult = new ArrayList<>();
	private Collection<EntityID> lastClusterEntityIDsQueryResult = new ArrayList<>();
	private List<StandardEntity> centerList;
	private List<EntityID> centerIDs;
	private Map<Integer, List<StandardEntity>> clusterEntitiesList;
	private List<List<EntityID>> clusterEntityIDsList;

	// hungarian: for assign agent
	private int allocations[] = null;


	class ClusterNode implements Clusterable {
		public Area area = null;
		public int clusterIndex = 0;
		double point[] = new double[2];

		@Override
		public double[] getPoint() {
			return point;
		}

		public ClusterNode(Area area, int clusterIndex) {
			this.area = area;
			this.clusterIndex = clusterIndex;
			this.point[0] = area.getX();
			this.point[1] = area.getY();
		}

	}

	public SampleKMeans(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
		super(ai, wi, si, moduleManager, developData);
		this.repeatPrecompute = developData.getInteger("sample.module.SampleKMeans.repeatPrecompute", 7);
		this.repeatPreparate = developData.getInteger("sample.module.SampleKMeans.repeatPreparate", 30);
		this.clusterNumber = developData.getInteger("sample.module.SampleKMeans.clusterSize", 10);
		this.assignAgentsFlag = developData.getBoolean("sample.module.SampleKMeans.assignAgentsFlag", true);
		this.clusterEntityIDsList = new ArrayList<>();
		this.centerIDs = new ArrayList<>();
		this.clusterEntitiesList = new HashMap<>();
		this.centerList = new ArrayList<>();
		this.entities = wi.getEntitiesOfType(
				StandardEntityURN.ROAD,
				StandardEntityURN.HYDRANT,
				StandardEntityURN.BUILDING,
				StandardEntityURN.REFUGE,
				StandardEntityURN.GAS_STATION,
				StandardEntityURN.AMBULANCE_CENTRE,
				StandardEntityURN.FIRE_STATION,
				StandardEntityURN.POLICE_OFFICE
		);

		if (agentInfo.me().getStandardURN().equals(StandardEntityURN.AMBULANCE_TEAM)) {
			agentSize = scenarioInfo.getScenarioAgentsAt();
		} else if (agentInfo.me().getStandardURN().equals(StandardEntityURN.FIRE_BRIGADE)) {
			agentSize = scenarioInfo.getScenarioAgentsFb();
		} else if (agentInfo.me().getStandardURN().equals(StandardEntityURN.POLICE_FORCE)) {
			agentSize = scenarioInfo.getScenarioAgentsPf();
		}

		clusterNumber = Math.min(30, agentSize);
	}

	@Override
	public Clustering updateInfo(MessageManager messageManager) {
		super.updateInfo(messageManager);
		if (this.getCountUpdateInfo() >= 2) {
			return this;
		}
		this.centerList.clear();
		this.clusterEntitiesList.clear();
		return this;
	}

	@Override
	public Clustering precompute(PrecomputeData precomputeData) {
		super.precompute(precomputeData);

		if (this.calced) {
			return this;
		}

		calClusterAndAssign();
		return this;
	}

	@Override
	public Clustering resume(PrecomputeData precomputeData) {
		super.resume(precomputeData);
		if (this.calced) {
			return this;
		}
		/**
		 * load data after precompute and run
		 */
		calClusterAndAssign();
		visualDebug();
		return this;
	}

	@Override
	public Clustering preparate() {
		super.preparate();

		if (this.calced) {
			return this;
		}

		calClusterAndAssign();
		visualDebug();
		return this;
	}


	/**
	 * assign agent to every clusters (after get all fb/at/pf agents from world info, and sorted)
	 * 	compute cost matrix with hungarian algorithm
	 * 		select a specifica agent
	 * 		add agent to corresponding cluster
	 * 		remove this agent from agentlist
	 */
	private void assignAgent() {
		allocations = new int[sortedTeamAgents.size()];
		double clusterCenters[][] = new double[clusterNumber][3];
		for (int i = 0; i < clusterNumber; i++) {
			clusterCenters[i][0] = 0;
			clusterCenters[i][1] = 0;
			clusterCenters[i][2] = 0;
		}
		for (ClusterNode clusterNode : allNodes) {
			clusterCenters[clusterNode.clusterIndex][0] += clusterNode.point[0];
			clusterCenters[clusterNode.clusterIndex][1] += clusterNode.point[1];
			clusterCenters[clusterNode.clusterIndex][2] += 1;
		}
		for (int i = 0; i < clusterNumber; i++) {
			if (clusterCenters[i][2] != 0) {
				clusterCenters[i][0] /= clusterCenters[i][2];
				clusterCenters[i][1] /= clusterCenters[i][2];
			}
		}

		// hangarian: costs matrix: for assign agent to every cluster
		double costs[][] = new double[sortedTeamAgents.size()][clusterNumber];
		for (int i = 0; i < costs.length; i++) {
			for (int j = 0; j < costs[0].length; j++) {
				StandardEntity agentStd = sortedTeamAgents.get(i);
				double ax = (int) (agentStd.getProperty("urn:rescuecore2.standard:property:x").getValue());
				double ay = (int) (agentStd.getProperty("urn:rescuecore2.standard:property:y").getValue());
				double dist = dist(ax, ay, clusterCenters[j][0], clusterCenters[j][1]);
				costs[i][j] = dist;
			}
		}
		HungarianAgentAssign hungarianAgentAssign = new HungarianAgentAssign(costs);

		allocations = hungarianAgentAssign.execute();
	}

	private void calClusterAndAssign(){

		/**
		 * get all relative entity
		 */
		allNodes.clear();
		sortedTeamAgents.clear();
		sortedTeamAgents.addAll(worldInfo.getEntitiesOfType(agentInfo.me().getStandardURN()));
		Collections.sort(sortedTeamAgents, new Comparator<StandardEntity>(){
			@Override
			public int compare(StandardEntity o1, StandardEntity o2) {
				return o1.getID().getValue() - o2.getID().getValue();
			}
		});
		this.clusterNumber = Math.max(1, sortedTeamAgents.size());
		Collection<StandardEntity> allStandardEntity =  this.worldInfo.getEntitiesOfType(
				StandardEntityURN.FIRE_STATION,
				StandardEntityURN.POLICE_OFFICE,
				StandardEntityURN.AMBULANCE_CENTRE,
				StandardEntityURN.REFUGE,
				StandardEntityURN.GAS_STATION,
				StandardEntityURN.BUILDING,
				StandardEntityURN.ROAD,
				StandardEntityURN.HYDRANT
		);
		/**
		 * data preparation
		 *      map: mapping from DoublePoint to StandardEntity
		 *      DBSCAN's input shape
		 */
		for (StandardEntity standardEntity : allStandardEntity) {
			allNodes.add(new ClusterNode((Area) standardEntity, 0));
		}

		/**
		 * use DBSCAN(opensource apache.commons.maths3.ml.clustering) to compute clusters
		 *      input:         Collection<DoublePoint>               dbscanMap
		 *      clusterer:     KMeansPlusPlusClusterer               dbscanclusterer
		 *      output:        List<CentroidCluster<DoublePoint>>    dbscanCluster
		 *
		 * descan paramlist:
		 * 		k: clusters numbers
		 * 		maxIterations: max iterations num
		 * 		DistanceMeasures map: distance between two vector
		 * 		RandomGenerator: set seed and so on
		 * 		EmptyClusterStrategy:
		 * 			FARTHEST_POINT 距离最远的创造一个聚类中心
		 * 			LARGEST_POINTS_NUMBER 分割点数最多的聚类
		 * 			LARGEST_VARIANCE 拆分掉距离方差最大的聚类(·)
		 */
//		KMeansPlusPlusClusterer<ClusterNode> dbscan = new KMeansPlusPlusClusterer<ClusterNode>(clusterNumber, 30, null, null, KMeansPlusPlusClusterer.EmptyClusterStrategy.LARGEST_VARIANCE);
		DistanceMeasure distanceMeasure = new DistanceMeasure() {
			@Override
			public double compute(double[] doubles, double[] doubles1) {
				return 0;
			}
		};
		RandomGenerator randomGenerator =  new RandomGenerator() {
			@Override
			public void setSeed(int i) {

			}

			@Override
			public void setSeed(int[] ints) {

			}

			@Override
			public void setSeed(long l) {

			}

			@Override
			public void nextBytes(byte[] bytes) {

			}

			@Override
			public int nextInt() {
				return 0;
			}

			@Override
			public int nextInt(int i) {
				return 0;
			}

			@Override
			public long nextLong() {
				return 0;
			}

			@Override
			public boolean nextBoolean() {
				return false;
			}

			@Override
			public float nextFloat() {
				return 0;
			}

			@Override
			public double nextDouble() {
				return 0;
			}

			@Override
			public double nextGaussian() {
				return 0;
			}
		};
//		KMeansPlusPlusClusterer<ClusterNode> dbscan = new KMeansPlusPlusClusterer<ClusterNode>(clusterNumber, 30, distanceMeasure, randomGenerator, KMeansPlusPlusClusterer.EmptyClusterStrategy.LARGEST_POINTS_NUMBER);
		KMeansPlusPlusClusterer<ClusterNode> dbscan = new KMeansPlusPlusClusterer<ClusterNode>(clusterNumber, 30);
		dbscan.getRandomGenerator().setSeed(agentInfo.me().getStandardURN().ordinal() + 1);
//		dbscan.getEmptyClusterStrategy().

		// then get output by use cluster()
		List<CentroidCluster<ClusterNode>> dbscanCluster = dbscan.cluster(allNodes);
		int clusterIndex = 0;
		/**
		 * paras dbscan output(a list with all cluster<ClusterNode></>) and labeling all ClusterNode(Entity) with a cluster-index
		 */
		for (CentroidCluster<ClusterNode> centroidCluster : dbscanCluster) {
			for (ClusterNode clusterNode : centroidCluster.getPoints()) {
				clusterNode.clusterIndex = clusterIndex;
			}
			clusterIndex++;
		}
		assignAgent();
		this.calced = true;
		visualDebug();
	}


	/**
	 * 获取cluster或者cluster的个数
	 * @return
	 */

	@Override
	public int getClusterNumber() {
		return clusterNumber;
	}

	@Override
	public int getClusterIndex(StandardEntity entity) {
		if (sortedTeamAgents.contains(entity)) {
			return getAgentInitialClusterIndex(entity);
		}
		return getClusterIndex(entity.getID());
	}

	@Override
	public int getClusterIndex(EntityID id) {
		StandardEntity entity = this.worldInfo.getEntity(id);
		if (entity != null && sortedTeamAgents.contains(entity)) {
			return getAgentInitialClusterIndex(entity);
		}
		for (ClusterNode clusterNode : allNodes) {
			if (clusterNode.area.getID().equals(id)) {
				return clusterNode.clusterIndex;
			}
		}
		return -1;
	}

	private int getAgentInitialClusterIndex(StandardEntity agent) {
		if (allocations == null) {
			return -1;
		}
		return allocations[sortedTeamAgents.indexOf(agent)];
	}

	/**
	 * map dbscan output to [clusterEntitiesList, clusterEntitiesIDsList]
	 *      output: [clusterEntitiesList, clusterEntityIDsList]
	 */
	@Override
	public Collection<EntityID> getClusterEntityIDs(int index) {
		Collection<EntityID> result = new ArrayList<>();
		if (entityIdsClusterIdx != index) {
			lastClusterEntityIDsQueryResult.clear();
			for (ClusterNode clusterNode : allNodes) {
				if (clusterNode.clusterIndex == index) {
					lastClusterEntityIDsQueryResult.add(clusterNode.area.getID());
				}
			}
		}
		entityIdsClusterIdx = index;
		result.addAll(lastClusterEntityIDsQueryResult);
		return result;
	}
	@Override
	public Collection<StandardEntity> getClusterEntities(int index) {
		Collection<StandardEntity> result = new ArrayList<>();
		if (entityClusterIdx != index) {
			lastClusterEntitiesQueryResult.clear();
			for (ClusterNode clusterNode : allNodes) {
				if (clusterNode.clusterIndex == index) {
					lastClusterEntitiesQueryResult.add(clusterNode.area);
				}
			}
		}
		entityClusterIdx = index;
		result.addAll(lastClusterEntitiesQueryResult);
		return result;
	}

	@Override
	public Clustering calc() {
		return this;
	}



	private void visualDebug() {
		int index = getClusterIndex(agentInfo.getID());
		CompositeConvexHull convexHull = new CompositeConvexHull();
		Collection<StandardEntity> clusterEntities = getClusterEntities(index);
		//去除自己
		clusterEntities.remove(agentInfo.me());
		for (StandardEntity entity : getClusterEntities(index)) {
			Pair<Integer, Integer> location = worldInfo.getLocation(entity);
			convexHull.addPoint(location.first(), location.second());
		}
		Polygon polygon = convexHull.getConvexPolygon();
		ArrayList<Polygon> data = new ArrayList<>();
		if (polygon != null) {
			data.add(convexHull.getConvexPolygon());
		}
		if (DebugHelper.DEBUG_MODE && scenarioInfo.getMode() != ScenarioInfo.Mode.PRECOMPUTATION_PHASE) {
			try {
				DebugHelper.VD_CLIENT.drawAsync(agentInfo.getID().getValue(), "ClusterConvexPolygon", data);
				if (agentInfo.me() instanceof FireBrigade) {
					DebugHelper.VD_CLIENT.drawAsync(agentInfo.getID().getValue(), "FBClusterConvexPolygon", data);
				}
				if (agentInfo.me() instanceof PoliceForce) {
					DebugHelper.VD_CLIENT.drawAsync(agentInfo.getID().getValue(), "PFClusterConvexPolygon", data);
				}
				if (agentInfo.me() instanceof AmbulanceTeam) {
					DebugHelper.VD_CLIENT.drawAsync(agentInfo.getID().getValue(), "ATClusterConvexPolygon", data);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static double dist(double Ax, double Ay, double Bx, double By) {
		return Math.hypot(Ax - Bx, Ay - By);
	}

}
