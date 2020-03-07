package CSU_Yunlu_2019.module.algorithm.fb;

import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.StaticClustering;
import rescuecore2.misc.Pair;
import rescuecore2.misc.collections.LazyMap;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

public class SampleKMeans extends StaticClustering{


	//import static java.util.Comparator.comparing;
	//import static java.util.Comparator.reverseOrder;


	    private static final String KEY_CLUSTER_SIZE = "sample.clustering.size";
	    private static final String KEY_CLUSTER_CENTER = "sample.clustering.centers";
	    private static final String KEY_CLUSTER_ENTITY = "sample.clustering.entities.";
	    private static final String KEY_ASSIGN_AGENT = "sample.clustering.assign";

	    private int repeatPrecompute;
	    private int repeatPreparate;

	    private Collection<StandardEntity> entities;

	    private List<StandardEntity> centerList;
	    private List<EntityID> centerIDs;
	    private Map<Integer, List<StandardEntity>> clusterEntitiesList;
	    private List<List<EntityID>> clusterEntityIDsList;

	    private int clusterSize;
	    private int agentSize;	//only firebridge

	    private boolean assignAgentsFlag;

	    private Map<EntityID, Set<EntityID>> shortestPathGraph;

	    /** crf add */
	    List<node> allDistanceGraph= new ArrayList<>();
	    private void sortListByDistance(List<node> list){
	    	Collections.sort(list,new Comparator<node>(){
	    		@Override
	    		public int compare(node o1,node o2){
	    			if (o1.distance >o2.distance) return 1;
	    			else if(o1.distance <o2.distance) return -1;
	    			else return 0;
	    		}
	    	});
	    }
	    // private Map<Integer,List<StandardEntity>> distanceGraph;
	    // public <Integer extends Comparable<? super Integer>,List<StandardEntity>> Map<Integer,List<StandardEntity>> sortByKey(Map<Integer,List<StandardEntity>> map){
	    // 	Map<Integer,List<StandardEntity>> result = new LinkedHashMap<>();
	    // 	// Map<Integer,List<StandardEntity>> result = Maps.newLinkedHashMap();
	    // 	map.entrySet().stream().sorted(Map.Entry.<Integer,List<StandardEntity>>comparingByKey().reversed()).forEachOrdered(e->result.put(e.getKey(),e.getValue()));
	    // 	return result;
	    // }


	    public SampleKMeans(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {

	        super(ai, wi, si, moduleManager, developData);
	        this.repeatPrecompute = developData.getInteger("sample.module.SampleKMeans.repeatPrecompute", 7);
	        this.repeatPreparate = developData.getInteger("sample.module.SampleKMeans.repeatPreparate", 30);
	        this.clusterSize = developData.getInteger("sample.module.SampleKMeans.clusterSize", 10);
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
	        
	        
	        if(agentInfo.me().getStandardURN().equals(StandardEntityURN.AMBULANCE_TEAM)){
	        	agentSize = scenarioInfo.getScenarioAgentsAt();
	        } else if(agentInfo.me().getStandardURN().equals(StandardEntityURN.FIRE_BRIGADE)){
	        	agentSize = scenarioInfo.getScenarioAgentsFb();
	        } else if ( agentInfo.me().getStandardURN().equals( StandardEntityURN.POLICE_FORCE ) ) {
	        	agentSize = scenarioInfo.getScenarioAgentsPf();
	        }
	        
	        clusterSize = Math.min(30, agentSize);
	        
	        //System.out.println("\n *** 簇的数量 " + clusterSize + " *** \n");
	    }

	    @Override
	    public Clustering updateInfo(MessageManager messageManager) {
	    	System.out.println("updateinfo check connect");
	    	// System.out.println(messageManager);
	        super.updateInfo(messageManager);
	        if(this.getCountUpdateInfo() >= 2) {
	            return this;
	        }
	        
	        this.updateKmeans();
	        this.centerList.clear();
	        this.clusterEntitiesList.clear();
	        return this;
	    }

		public void updateKmeans(){
	    	// System.out.println("############## before update kmeans=================");
	    	// System.out.println(centerList.size());
	    	// for (StandardEntity center:centerList) System.out.println(center);
	    	// // for (int i = 0; i < clusterEntitiesList.size(); i++) if(clusterEntitiesList.get(i).size()>0)System.out.println(i+"  "+centerList.get(i)+"  "+clusterEntitiesList.get(i).size());

	    	// /** the process of change kmeans  */
	    	// System.out.println("-----after change -------");
	    	// System.out.println(centerList.size());
	    	// for (StandardEntity center:centerList) System.out.println(center);
	    	// for (int i = 0; i < clusterEntitiesList.size(); i++) if(clusterEntitiesList.get(i).size()>0) System.out.println(i+"  "+centerList.get(i)+"  "+clusterEntitiesList.get(i).size());

	    }
	    

	    @Override
	    public Clustering precompute(PrecomputeData precomputeData) {
	        super.precompute(precomputeData);
	        if(this.getCountPrecompute() >= 2) {
	        	
	            return this;
	        }
	        this.calcPathBased(this.repeatPrecompute);
	        this.entities = null;
	        // write
	        precomputeData.setInteger(KEY_CLUSTER_SIZE, this.clusterSize);
	        precomputeData.setEntityIDList(KEY_CLUSTER_CENTER, this.centerIDs);
	        for(int i = 0; i < this.clusterSize; i++) {
	            precomputeData.setEntityIDList(KEY_CLUSTER_ENTITY + i, this.clusterEntityIDsList.get(i));
	        }
	        precomputeData.setBoolean(KEY_ASSIGN_AGENT, this.assignAgentsFlag);
		       	
	        return this;
	    }

	    @Override
	    public Clustering resume(PrecomputeData precomputeData) {
	    	System.out.println("resume  connnect");
	        super.resume(precomputeData);
	        if(this.getCountResume() >= 2) {
	            return this;
	        }
	        this.entities = null;
	        // read
	        this.clusterSize = precomputeData.getInteger(KEY_CLUSTER_SIZE);
	        this.centerIDs = new ArrayList<>(precomputeData.getEntityIDList(KEY_CLUSTER_CENTER));
	        this.clusterEntityIDsList = new ArrayList<>(this.clusterSize);
	        for(int i = 0; i < this.clusterSize; i++) {
	            this.clusterEntityIDsList.add(i, precomputeData.getEntityIDList(KEY_CLUSTER_ENTITY + i));
	        }
	        this.assignAgentsFlag = precomputeData.getBoolean(KEY_ASSIGN_AGENT);
	        return this;
	    }

	    @Override
	    public Clustering preparate() {
	        super.preparate();
	        if(this.getCountPreparate() >= 2) {

	            return this;
	        }
	        this.calcStandard(this.repeatPreparate);
	        this.entities = null;
	        return this;
	    }

	    public boolean checkMerge(StandardEntity e1,StandardEntity e2){
			return true;
	    }

	    /**
	     * crf
	     * Main param: clusterEntityList\centerList\centerID
	     * Method:首先根据邻里关系小范围聚类(整体中心数降至1/3)
	     *        再根据凝聚层次聚类进行指定距离范围(mergeRange为范围大小)的聚类合并。		  	
	     *
	     */
	    private void calcStandard(int repeat) {
	        
	        this.initShortestPath(this.worldInfo);
	        
	        /** init */
	        this.allDistanceGraph.clear();

	        for(StandardEntity tementity:entities){
	        	// System.out.println(tementity);
	        	Pair<Integer,Integer> firstLocation = this.worldInfo.getLocation(tementity);
	        	for (EntityID id :this.shortestPathGraph.get(tementity.getID())) {
	        		Pair<Integer,Integer> secondLocation = this.worldInfo.getLocation(this.worldInfo.getEntity(id));
	        		// System.out.println(id+"  "+this.getDistance(firstLocation.first(),firstLocation.second(),secondLocation.first(),secondLocation.second()));
	        		this.allDistanceGraph.add(new node(tementity,this.worldInfo.getEntity(id),this.getDistance(firstLocation.first(),firstLocation.second(),secondLocation.first(),secondLocation.second())));
	        	}
	        }

	        /** sort and preprocess */
	        // this.sortListByDistance(this.allDistanceGraph);
	        for (int i = allDistanceGraph.size()-1; i > 0; i-=2) this.allDistanceGraph.remove(i);
	        

	        while(this.allDistanceGraph.size()>0){
	        	this.clusterEntitiesList.put(this.centerList.size(),new ArrayList<>());
	        	this.clusterEntitiesList.get(this.centerList.size()).add(this.allDistanceGraph.get(0).e2);
	        	
	        	this.centerList.add(this.allDistanceGraph.get(0).e1);
	        	this.centerIDs.add(this.allDistanceGraph.get(0).e1.getID());

	        	EntityID e1ID = this.allDistanceGraph.get(0).e1.getID();
	        	EntityID e2ID = this.allDistanceGraph.get(0).e2.getID();
	        	this.allDistanceGraph.remove(0);
	        	
	        	/** first merge */
	        	//check point
	        	for (int i = 0;i < this.allDistanceGraph.size() ;i++ ) {
	        		if (this.allDistanceGraph.get(i).e1.getID().equals(e1ID) || this.allDistanceGraph.get(i).e2.getID().equals(e1ID) ||this.allDistanceGraph.get(i).e1.getID().equals(e2ID) || this.allDistanceGraph.get(i).e2.getID().equals(e2ID) ) {
	        			//check is already exist
	        			if (!this.clusterEntitiesList.get(this.centerList.size()-1).contains(this.allDistanceGraph.get(i).e1)) 
	        				this.clusterEntitiesList.get(this.centerList.size()-1).add(this.allDistanceGraph.get(i).e1);
	        			if (!this.clusterEntitiesList.get(this.centerList.size()-1).contains(this.allDistanceGraph.get(i).e2)) 
	        				this.clusterEntitiesList.get(this.centerList.size()-1).add(this.allDistanceGraph.get(i).e2);
	        				// if (this.allDistanceGraph.get(i).e1.getID().equals(e1ID) || this.allDistanceGraph.get(i).e1.getID().equals(e2ID)) 
	        				// 	this.centerIDs.remove(centerIDs.indexOf(this.allDistanceGraph.get(i).e1.getID()));
	        				this.allDistanceGraph.remove(i);
	        		}
	        	}
	        }

	        /** 检验第一阶段merge输出点 check and print cluster groups*/
	        // System.out.println("----------------- "+this.shortestPathGraph.size()+" buildings in total and after kmeans here are "+centerList.size()+" "+centerIDs.size()+" cluster groups and  "+ agentSize +" firebridges-------------------------");
	        // for (int i = 0;i <clusterEntitiesList.size() ; i++) {
	        // 	System.out.println("cluster ID is "+i+"    and the center is "+centerList.get(i)+"      and size of this cluster is "+clusterEntitiesList.get(i).size());
	        // 	for (StandardEntity teme :clusterEntitiesList.get(i) ) System.out.println(teme);
	        // }



	        /**
	        `* crf:
	         * iterator merge   select 0 as begin point
	         * mergeRange:一次merge的节点数=>越小精度越大，当num选取1时为凝聚层次聚类。
	         * pace:遍历步伐数=>越大速度越快，当pace为1时为遍历全部点集。
	         * stdCenter:temp center 
	         */
	        int mergeRange = 8,pace = 3;
	        StandardEntity stdCenter = centerList.get(0);
	        List<node> singleDistance = new ArrayList<>();
	        while(centerList.size() > this.agentSize){
	        	singleDistance.clear();
	        	Pair<Integer,Integer> firstLocation = this.worldInfo.getLocation(centerList.get(0));
	    	    for (int i = 0; i < centerList.size(); i+=pace) {
	    	    	if (i != centerList.indexOf(stdCenter)) {
	    	    		Pair<Integer,Integer> secondLocation = this.worldInfo.getLocation(centerList.get(i));
	       		 		singleDistance.add(new node(stdCenter,centerList.get(i),this.getDistance(firstLocation.first(),firstLocation.second(),secondLocation.first(),secondLocation.second())));	
	        		}
	        	}
	       	 	
	       	 	this.sortListByDistance(singleDistance);
	       	 	
	       	 	for (int i = 0;i < Math.min(mergeRange,singleDistance.size()) ;i++ ) {
	        		//merge  second merge  
	        		this.clusterEntitiesList.get(this.centerList.indexOf(stdCenter)).add(singleDistance.get(0).e2);
	        		if (this.centerList.indexOf(singleDistance.get(0).e2) >= 0) {
	        			for (StandardEntity teme : this.clusterEntitiesList.get(this.centerList.indexOf(singleDistance.get(0).e2))) {
	        				if (!this.clusterEntitiesList.get(this.centerList.indexOf(stdCenter)).contains(teme)) {
	        					this.clusterEntitiesList.get(this.centerList.indexOf(stdCenter)).add(teme);
	        				}
	        			}
	        			// clusterEntitiesList.get(centerList.indexOf(stdCenter)).add(teme);
	        			
	        			//remove => 从clusterEntityList、centerList、centerID 
	        			this.clusterEntitiesList.remove(singleDistance.get(0).e2);
	        			this.centerIDs.remove(this.centerList.indexOf(singleDistance.get(0).e2));
	        			this.centerList.remove(this.centerList.indexOf(singleDistance.get(0).e2));

	        			singleDistance.remove(0);
	        		}
	        	}
	        	//transform stdCenter=>the farthest building
	        	stdCenter = singleDistance.get(singleDistance.size()-1).e2;
	        }
	        /** 检验第二阶段merge输出点  */
			// System.out.println("----------------- "+this.shortestPathGraph.size()+" buildings in total and after kmeans here are "+centerList.size()+" "+centerIDs.size()+" cluster groups and  "+ agentSize +" firebridges-------------------------");
	    	// for (int i = 0; i < centerList.size();i++ ) {
	    	// 	System.out.println(i+"  "+centerList.get(i)+"  "+clusterEntitiesList.get(i).size());
	    	// 	for (StandardEntity teme:clusterEntitiesList.get(i)) System.out.println(teme);
	    	// }

	    }

	    @Override
	    public int getClusterNumber() {
	        //The number of clusters
	        return this.clusterSize;
	    }

	    @Override
	    public int getClusterIndex(StandardEntity entity) {
	        return this.getClusterIndex(entity.getID());
	    }

	    @Override
	    public int getClusterIndex(EntityID id) {
	        for(int i = 0; i < this.clusterSize; i++) {
	            if(this.clusterEntityIDsList.get(i).contains(id)) {
	                return i;
	            }
	        }
	        return -1;
	    }

	    @Override
	    public Collection<StandardEntity> getClusterEntities(int index) {
	        List<StandardEntity> result = this.clusterEntitiesList.get(index);
	        if(result == null || result.isEmpty()) {
	            List<EntityID> list = this.clusterEntityIDsList.get(index);
	            result = new ArrayList<>(list.size());
	            for(int i = 0; i < list.size(); i++) {
	                result.add(i, this.worldInfo.getEntity(list.get(i)));
	            }
	            this.clusterEntitiesList.put(index, result);
	        }
	        return result;
	    }

	    @Override
	    public Collection<EntityID> getClusterEntityIDs(int index) {
	        return this.clusterEntityIDsList.get(index);
	    }

	    @Override
	    public Clustering calc() {
		
	        return this;
	    }

	    

	    private void calcPathBased(int repeat) {
	    	this.initShortestPath(this.worldInfo);
	        
	        /** init */
	        this.allDistanceGraph.clear();

	        for(StandardEntity tementity:entities){
	        	// System.out.println(tementity);
	        	Pair<Integer,Integer> firstLocation = this.worldInfo.getLocation(tementity);
	        	for (EntityID id :this.shortestPathGraph.get(tementity.getID())) {
	        		Pair<Integer,Integer> secondLocation = this.worldInfo.getLocation(this.worldInfo.getEntity(id));
	        		// System.out.println(id+"  "+this.getDistance(firstLocation.first(),firstLocation.second(),secondLocation.first(),secondLocation.second()));
	        		this.allDistanceGraph.add(new node(tementity,this.worldInfo.getEntity(id),this.getDistance(firstLocation.first(),firstLocation.second(),secondLocation.first(),secondLocation.second())));
	        	}
	        }

	        /** sort and preprocess */
	        // this.sortListByDistance(this.allDistanceGraph);
	        for (int i = allDistanceGraph.size()-1; i > 0; i-=2) this.allDistanceGraph.remove(i);
	        

	        while(this.allDistanceGraph.size()>0){
	        	this.clusterEntitiesList.put(this.centerList.size(),new ArrayList<>());
	        	this.clusterEntitiesList.get(this.centerList.size()).add(this.allDistanceGraph.get(0).e2);
	        	
	        	this.centerList.add(this.allDistanceGraph.get(0).e1);
	        	this.centerIDs.add(this.allDistanceGraph.get(0).e1.getID());

	        	EntityID e1ID = this.allDistanceGraph.get(0).e1.getID();
	        	EntityID e2ID = this.allDistanceGraph.get(0).e2.getID();
	        	this.allDistanceGraph.remove(0);
	        	
	        	/** first merge */
	        	//check point
	        	for (int i = 0;i < this.allDistanceGraph.size() ;i++ ) {
	        		if (this.allDistanceGraph.get(i).e1.getID().equals(e1ID) || this.allDistanceGraph.get(i).e2.getID().equals(e1ID) ||this.allDistanceGraph.get(i).e1.getID().equals(e2ID) || this.allDistanceGraph.get(i).e2.getID().equals(e2ID) ) {
	        			//check is already exist
	        			if (!this.clusterEntitiesList.get(this.centerList.size()-1).contains(this.allDistanceGraph.get(i).e1)) 
	        				this.clusterEntitiesList.get(this.centerList.size()-1).add(this.allDistanceGraph.get(i).e1);
	        			if (!this.clusterEntitiesList.get(this.centerList.size()-1).contains(this.allDistanceGraph.get(i).e2)) 
	        				this.clusterEntitiesList.get(this.centerList.size()-1).add(this.allDistanceGraph.get(i).e2);
	        				// if (this.allDistanceGraph.get(i).e1.getID().equals(e1ID) || this.allDistanceGraph.get(i).e1.getID().equals(e2ID)) 
	        				// 	this.centerIDs.remove(centerIDs.indexOf(this.allDistanceGraph.get(i).e1.getID()));
	        				this.allDistanceGraph.remove(i);
	        		}
	        	}
	        }

	        /** 检验第一阶段merge输出点 check and print cluster groups*/
	        // System.out.println("----------------- "+this.shortestPathGraph.size()+" buildings in total and after kmeans here are "+centerList.size()+" "+centerIDs.size()+" cluster groups and  "+ agentSize +" firebridges-------------------------");
	        // for (int i = 0;i <clusterEntitiesList.size() ; i++) {
	        // 	System.out.println("cluster ID is "+i+"    and the center is "+centerList.get(i)+"      and size of this cluster is "+clusterEntitiesList.get(i).size());
	        // 	for (StandardEntity teme :clusterEntitiesList.get(i) ) System.out.println(teme);
	        // }


	        int mergeRange = 8,pace = 3;
	        StandardEntity stdCenter = centerList.get(0);
	        List<node> singleDistance = new ArrayList<>();
	        while(centerList.size() > this.agentSize){
	        	singleDistance.clear();
	        	Pair<Integer,Integer> firstLocation = this.worldInfo.getLocation(centerList.get(0));
	    	    for (int i = 0; i < centerList.size(); i+=pace) {
	    	    	if (i != centerList.indexOf(stdCenter)) {
	    	    		Pair<Integer,Integer> secondLocation = this.worldInfo.getLocation(centerList.get(i));
	       		 		singleDistance.add(new node(stdCenter,centerList.get(i),this.getDistance(firstLocation.first(),firstLocation.second(),secondLocation.first(),secondLocation.second())));	
	        		}
	        	}
	       	 	
	       	 	this.sortListByDistance(singleDistance);
	       	 	
	       	 	for (int i = 0;i < Math.min(mergeRange,singleDistance.size()) ;i++ ) {
	        		//merge  second merge  
	        		this.clusterEntitiesList.get(this.centerList.indexOf(stdCenter)).add(singleDistance.get(0).e2);
	        		if (this.centerList.indexOf(singleDistance.get(0).e2) >= 0) {
	        			for (StandardEntity teme : this.clusterEntitiesList.get(this.centerList.indexOf(singleDistance.get(0).e2))) {
	        				if (!this.clusterEntitiesList.get(this.centerList.indexOf(stdCenter)).contains(teme)) {
	        					this.clusterEntitiesList.get(this.centerList.indexOf(stdCenter)).add(teme);
	        				}
	        			}
	        			// clusterEntitiesList.get(centerList.indexOf(stdCenter)).add(teme);
	        			
	        			//remove => 从clusterEntityList、centerList、centerID 
	        			this.clusterEntitiesList.remove(singleDistance.get(0).e2);
	        			this.centerIDs.remove(this.centerList.indexOf(singleDistance.get(0).e2));
	        			this.centerList.remove(this.centerList.indexOf(singleDistance.get(0).e2));

	        			singleDistance.remove(0);
	        		}
	        	}
	        	//transform stdCenter=>the farthest building
	        	stdCenter = singleDistance.get(singleDistance.size()-1).e2;
	        }
	        /** 检验第二阶段merge输出点  */
			// System.out.println("----------------- "+this.shortestPathGraph.size()+" buildings in total and after kmeans here are "+centerList.size()+" "+centerIDs.size()+" cluster groups and  "+ agentSize +" firebridges-------------------------");
	    	// for (int i = 0; i < centerList.size();i++ ) {
	    	// 	System.out.println(i+"  "+centerList.get(i)+"  "+clusterEntitiesList.get(i).size());
	    	// 	for (StandardEntity teme:clusterEntitiesList.get(i)) System.out.println(teme);
	    	// }
	    }

	    
	    		

	    private void assignAgents(WorldInfo world, List<StandardEntity> agentList) {
	    	System.out.println("assing agent connect------");
	        int clusterIndex = 0;
	        while (agentList.size() > 0) {
	            StandardEntity center = this.centerList.get(clusterIndex);
	            StandardEntity agent = this.getNearAgent(world, agentList, center);
	            this.clusterEntitiesList.get(clusterIndex).add(agent);
	            agentList.remove(agent);
	            clusterIndex++;
	            if (clusterIndex >= this.clusterSize) {
	                clusterIndex = 0;
	            }
	        }
	    }

	    private StandardEntity getNearEntityByLine(WorldInfo world, List<StandardEntity> srcEntityList, StandardEntity targetEntity) {
	        Pair<Integer, Integer> location = world.getLocation(targetEntity);
	        return this.getNearEntityByLine(world, srcEntityList, location.first(), location.second());
	    }

	    private StandardEntity getNearEntityByLine(WorldInfo world, List<StandardEntity> srcEntityList, int targetX, int targetY) {
	        StandardEntity result = null;
	        for(StandardEntity entity : srcEntityList) {
	            result = ((result != null) ? this.compareLineDistance(world, targetX, targetY, result, entity) : entity);
	        }
	        return result;
	    }

	    private StandardEntity getNearAgent(WorldInfo worldInfo, List<StandardEntity> srcAgentList, StandardEntity targetEntity) {
	        StandardEntity result = null;
	        for (StandardEntity agent : srcAgentList) {
	            Human human = (Human)agent;
	            if (result == null) {
	                result = agent;
	            }
	            else {
	                if (this.comparePathDistance(worldInfo, targetEntity, result, worldInfo.getPosition(human)).equals(worldInfo.getPosition(human))) {
	                    result = agent;
	                }
	            }
	        }
	        return result;
	    }

	    private StandardEntity getNearEntity(WorldInfo worldInfo, List<StandardEntity> srcEntityList, int targetX, int targetY) {
	        StandardEntity result = null;
	        for (StandardEntity entity : srcEntityList) {
	            result = (result != null) ? this.compareLineDistance(worldInfo, targetX, targetY, result, entity) : entity;
	        }
	        return result;
	    }

	    private Point2D getEdgePoint(Edge edge) {
	        Point2D start = edge.getStart();
	        Point2D end = edge.getEnd();
	        return new Point2D(((start.getX() + end.getX()) / 2.0D), ((start.getY() + end.getY()) / 2.0D));
	    }


	    private double getDistance(double fromX, double fromY, double toX, double toY) {
	        double dx = fromX - toX;
	        double dy = fromY - toY;
	        return Math.hypot(dx, dy);
	    }

	    private double getDistance(Pair<Integer, Integer> from, Point2D to) {
	        return getDistance(from.first(), from.second(), to.getX(), to.getY());
	    }

	    private double getDistance(Pair<Integer, Integer> from, Edge to) {
	        return getDistance(from, getEdgePoint(to));
	    }

	    private double getDistance(Point2D from, Point2D to) {
	        return getDistance(from.getX(), from.getY(), to.getX(), to.getY());
	    }

	    private double getDistance(Edge from, Edge to) {
	        return getDistance(getEdgePoint(from), getEdgePoint(to));
	    }

	    private StandardEntity compareLineDistance(WorldInfo worldInfo, int targetX, int targetY, StandardEntity first, StandardEntity second) {
	        Pair<Integer, Integer> firstLocation = worldInfo.getLocation(first);
	        Pair<Integer, Integer> secondLocation = worldInfo.getLocation(second);
	        double firstDistance = getDistance(firstLocation.first(), firstLocation.second(), targetX, targetY);
	        double secondDistance = getDistance(secondLocation.first(), secondLocation.second(), targetX, targetY);
	        return (firstDistance < secondDistance ? first : second);
	    }

	    private StandardEntity getNearEntity(WorldInfo worldInfo, List<StandardEntity> srcEntityList, StandardEntity targetEntity) {
	        StandardEntity result = null;
	        for (StandardEntity entity : srcEntityList) {
	            result = (result != null) ? this.comparePathDistance(worldInfo, targetEntity, result, entity) : entity;
	        }
	        return result;
	    }

	    private StandardEntity comparePathDistance(WorldInfo worldInfo, StandardEntity target, StandardEntity first, StandardEntity second) {
	        double firstDistance = getPathDistance(worldInfo, shortestPath(target.getID(), first.getID()));
	        double secondDistance = getPathDistance(worldInfo, shortestPath(target.getID(), second.getID()));
	        return (firstDistance < secondDistance ? first : second);
	    }

	    private double getPathDistance(WorldInfo worldInfo, List<EntityID> path) {
	        if (path == null) return Double.MAX_VALUE;
	        if (path.size() <= 1) return 0.0D;

	        double distance = 0.0D;
	        int limit = path.size() - 1;

	        Area area = (Area)worldInfo.getEntity(path.get(0));
	        distance += getDistance(worldInfo.getLocation(area), area.getEdgeTo(path.get(1)));
	        area = (Area)worldInfo.getEntity(path.get(limit));
	        distance += getDistance(worldInfo.getLocation(area), area.getEdgeTo(path.get(limit - 1)));

	        for(int i = 1; i < limit; i++) {
	            area = (Area)worldInfo.getEntity(path.get(i));
	            distance += getDistance(area.getEdgeTo(path.get(i - 1)), area.getEdgeTo(path.get(i + 1)));
	        }
	        return distance;
	    }

	    private void initShortestPath(WorldInfo worldInfo) {
	        Map<EntityID, Set<EntityID>> neighbours = new LazyMap<EntityID, Set<EntityID>>() {
	            @Override
	            public Set<EntityID> createValue() {
	                return new HashSet<>();
	            }
	        };
	        for (Entity next : worldInfo) {
	            if (next instanceof Area) {
	                Collection<EntityID> areaNeighbours = ((Area) next).getNeighbours();
	                neighbours.get(next.getID()).addAll(areaNeighbours);
	            }
	        }
	        for (Map.Entry<EntityID, Set<EntityID>> graph : neighbours.entrySet()) {// fix graph
	            for (EntityID entityID : graph.getValue()) {
	                neighbours.get(entityID).add(graph.getKey());
	            }
	        }
	        this.shortestPathGraph = neighbours;
	    }

	    private List<EntityID> shortestPath(EntityID start, EntityID... goals) {
	        return shortestPath(start, Arrays.asList(goals));
	    }

	    private List<EntityID> shortestPath(EntityID start, Collection<EntityID> goals) {
	        List<EntityID> open = new LinkedList<>();
	        Map<EntityID, EntityID> ancestors = new HashMap<>();
	        open.add(start);
	        EntityID next;
	        boolean found = false;
	        ancestors.put(start, start);
	        do {
	            next = open.remove(0);
	            if (isGoal(next, goals)) {
	                found = true;
	                break;
	            }
	            Collection<EntityID> neighbours = shortestPathGraph.get(next);
	            if (neighbours.isEmpty()) continue;

	            for (EntityID neighbour : neighbours) {
	                if (isGoal(neighbour, goals)) {
	                    ancestors.put(neighbour, next);
	                    next = neighbour;
	                    found = true;
	                    break;
	                }
	                else if (!ancestors.containsKey(neighbour)) {
	                    open.add(neighbour);
	                    ancestors.put(neighbour, next);
	                }
	            }
	        } while (!found && !open.isEmpty());
	        if (!found) {
	            // No path
	            return null;
	        }
	        // Walk back from goal to start
	        EntityID current = next;
	        List<EntityID> path = new LinkedList<>();
	        do {
	            path.add(0, current);
	            current = ancestors.get(current);
	            if (current == null) throw new RuntimeException("Found a node with no ancestor! Something is broken.");
	        } while (current != start);
	        return path;
	    }

	    private boolean isGoal(EntityID e, Collection<EntityID> test) {
	        return test.contains(e);
	    }
	

}

class node{
	StandardEntity e1,e2;
	double distance;
	public node(){}
	public node(StandardEntity e1,StandardEntity e2,double distance){
		this.e1 = e1;
		this.e2 = e2;
		this.distance = distance;
	}
}
