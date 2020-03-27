package CSU_Yunlu_2019.module.complex.pf;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.agent.communication.standard.bundle.information.MessageAmbulanceTeam;
import adf.agent.communication.standard.bundle.information.MessageFireBrigade;
import adf.agent.communication.standard.bundle.information.MessagePoliceForce;
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
import adf.component.module.complex.RoadDetector;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

import CSU_Yunlu_2019.extaction.pf.guidelineHelper;

import java.util.*;
import java.util.stream.Collectors;
//import PF_CSUpfRoadDetector.sorter;

/**
* @Description: 具有优先级的RoadDetector
* @Author: Bochun-Yue
* @Date: 2/25/20
*/

public class CSURoadDetector extends RoadDetector {
	public static final String KEY_JUDGE_ROAD = "RoadDetector.judge_road";
	public static final String KEY_START_X = "RoadDetector.start_x";
	public static final String KEY_START_Y = "RoadDetector.start_y";
	public static final String KEY_END_X = "RoadDetector.end_x";
	public static final String KEY_END_Y = "RoadDetector.end_y";
	public static final String KEY_ROAD_SIZE = "RoadDetector.road_size";
	
	private int roadsize;
	
    private Set<EntityID> targetAreas = new HashSet<>();
    private PathPlanning pathPlanning;
    private Clustering clustering;
    private EntityID first_mission = null;
    private boolean mission_flag = false;
    private boolean needFirstMission = false;
    private EntityID result = null;
    private boolean have_police_office = false;
    


	int lastTime = 0;
	double lastx  =0 ;
	double lasty = 0;
    

	private Set<EntityID> entrance_of_Refuge_and_Hydrant = new HashSet<>();
	private MessageManager messageManager =null;
    
    
    public CSURoadDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData)
    {
        super(ai, wi, si, moduleManager, developData);
        switch (scenarioInfo.getMode())
        {
            case PRECOMPUTATION_PHASE:
            	this.pathPlanning = moduleManager.getModule("RoadDetector.PathPlanning",
    					"CSU_Yunlu_2019.module.algorithm.AStarPathPlanning");
            	this.clustering = moduleManager.getModule("SampleRoadDetector.Clustering",
						"adf.sample.module.algorithm.SampleKMeans");
                break;
            case PRECOMPUTED:
            	this.pathPlanning = moduleManager.getModule("RoadDetector.PathPlanning",
    					"CSU_Yunlu_2019.module.algorithm.AStarPathPlanning");
				this.clustering = moduleManager.getModule("SampleRoadDetector.Clustering",
						"adf.sample.module.algorithm.SampleKMeans");
                break;
            case NON_PRECOMPUTE:
            	this.pathPlanning = moduleManager.getModule("RoadDetector.PathPlanning",
    					"CSU_Yunlu_2019.module.algorithm.AStarPathPlanning");
				this.clustering = moduleManager.getModule("SampleRoadDetector.Clustering",
						"adf.sample.module.algorithm.SampleKMeans");
                break;
        }
        registerModule(this.pathPlanning);
        registerModule(this.clustering);
        this.have_police_office = !wi.getEntityIDsOfType(StandardEntityURN.POLICE_OFFICE).isEmpty();
        this.result = null;
    }
    private boolean is_area_blocked(EntityID id){
		Road road = (Road) this.worldInfo.getEntity(id);
		if(!road.isBlockadesDefined()||this.isRoadPassable(road))  return false;
		return true;
	}
	
 
    private class DistanceIDSorter implements Comparator<EntityID> {
        private WorldInfo worldInfo;
        private EntityID reference;

        DistanceIDSorter(WorldInfo worldInfo, EntityID reference) {
            this.worldInfo = worldInfo;
            this.reference = reference;
        }

        DistanceIDSorter(WorldInfo worldInfo, StandardEntity reference) {
            this.worldInfo = worldInfo;
            this.reference = reference.getID();
        }

        public int compare(EntityID a, EntityID b) {
            int d1 = this.worldInfo.getDistance(this.reference, a);
            int d2 = this.worldInfo.getDistance(this.reference, b);
            return d1 - d2;
        }
    }

private StandardEntity getNearEntityByLine(WorldInfo world, List<StandardEntity> srcEntityList, int targetX,
		int targetY) {
	StandardEntity result = null;
	for (StandardEntity entity : srcEntityList) {
		result = ((result != null) ? this.compareLineDistance(world, targetX, targetY, result, entity) : entity);
	}
	return result;
}

private StandardEntity compareLineDistance(WorldInfo worldInfo, int targetX, int targetY, StandardEntity first,
		StandardEntity second) {
	Pair<Integer, Integer> firstLocation = worldInfo.getLocation(first);
	Pair<Integer, Integer> secondLocation = worldInfo.getLocation(second);
	double firstDistance = getDistance(firstLocation.first(), firstLocation.second(), targetX, targetY);
	double secondDistance = getDistance(secondLocation.first(), secondLocation.second(), targetX, targetY);
	return (firstDistance < secondDistance ? first : second);
}

private double getDistance(double fromX, double fromY, double toX, double toY) {
	double dx = fromX - toX;
	double dy = fromY - toY;
	return Math.hypot(dx, dy);
}

private double getDistance(EntityID from, EntityID to) {
	return (double) worldInfo.getDistance(from, to);
}
	
/**
* @Description: human与障碍的最短距离，方便判断civilian是否被堵
* @Author: Bochun-Yue
* @Date: 2/25/20
*/

private double getDistance(Human human,Road road) {
	Collection<Blockade> blockades = this.worldInfo.getBlockades(road);
	Point2D basePoint = new Point2D(human.getX(),human.getY());
	double nearest = Double.MAX_VALUE;
	
	for (Blockade blockade : blockades) {
		if(blockade.getShape().contains(human.getX(), human.getY())) {
			return 0;
		}
		List<Point2D> Points = GeometryTools2D.vertexArrayToPoints(blockade.getApexes());
		for(Point2D closest: Points) {
	        double d = GeometryTools2D.getDistance(basePoint, closest);
	        if (d < nearest) {
	            nearest = d;
	        }
		}
	}
	return nearest;
}

private EntityID getClosestEntityID(Collection<EntityID> IDs, EntityID reference) {
    if (IDs.isEmpty()) {
        return null;
    }
    double minDistance = Double.MAX_VALUE;
    EntityID closestID = null;
    for (EntityID id : IDs) {
        double distance = this.getDistance(reference, id);
        if (distance < minDistance) {
            minDistance = distance;
            closestID = id;
        }
    }
    return closestID;
}

	//摘于MRL
	private RoadDetector getRoadDetector(EntityID positionID, Set<EntityID> entityIDSet) {
        this.pathPlanning.setFrom(positionID);
        this.pathPlanning.setDestination(entityIDSet);
        List<EntityID> path = this.pathPlanning.calc().getResult();
        if (path != null && path.size() > 0) {
            this.result = path.get(path.size() - 1);
        }
        return this;
    }

	/**
	* @Description: 寻找警察到目标的路线
	* @Author: Bochun-Yue
	* @Date: 2/25/20
	*/

	private RoadDetector getPathTo(EntityID positionIDfrom, EntityID positionIDto) {
		this.pathPlanning.setFrom(positionIDfrom);
        this.pathPlanning.setDestination(positionIDto);
        List<EntityID> path = this.pathPlanning.calc().getResult();
        if (path != null && path.size() > 0) {
            this.result = path.get(path.size() - 1);
        }
        return this;
	}

	/**
	* @Description: 最近的警察去refuge（如果存在），防止FB无法及时补水
	* @Author: Bochun-Yue
	* @Date: 3/9/20
	*/

	boolean search_flag = false;
	boolean arrive_flag = false;
	StandardEntity nearest_refuge = null;
	private void Find_Refuge() {
		//检测离每个refuge最近的警察
		if(!this.search_flag) {
			for(StandardEntity SE : worldInfo.getEntitiesOfType(StandardEntityURN.REFUGE)) {
				boolean nearest_flag = true;
				Refuge refuge = (Refuge) SE;
				double distance = this.getDistance(this.agentInfo.getX(), this.agentInfo.getY(),refuge.getX(),refuge.getY());
				for(StandardEntity se : worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)) {
					PoliceForce police = (PoliceForce) se;
					double dist = this.getDistance(police.getX(),police.getY(),refuge.getX(),refuge.getY());
					if(dist < distance) {
						nearest_flag = false;
						break;
					}
				}
				if(nearest_flag) {
					this.nearest_refuge = SE;
					break;
				}else {
					continue;
				}
			}
		}
		this.search_flag=true;
	}
	
	private RoadDetector Get_To_Refuge(EntityID positionID) {
		if(!arrive_flag) {
			this.Find_Refuge();
			if(this.nearest_refuge!=null) {
				Refuge refuge = (Refuge) nearest_refuge;
				if(this.agentInfo.getPosition().getValue()==refuge.getID().getValue()) {
					this.arrive_flag = true;
					return null;
				}
				else {
					return this.getPathTo(positionID, refuge.getID());
				}
			}
		}
		return null;
	}
	
	/**
	* @Description: 具有优先级路线的RoadDetector
	* @Author: Bochun-Yue
	* @Date: 2/25/20
	*/
	
    @Override
    public RoadDetector calc()
    {    	
	
    	EntityID positionID = this.agentInfo.getPosition();
		this.update_roads();
		
		if(agentInfo.getTime() > scenarioInfo.getKernelAgentsIgnoreuntil() && !this.arrive_flag) this.Get_To_Refuge(positionID);

		if(nearest_refuge !=null && this.result != null && !this.arrive_flag) {
			if(nearest_refuge.getID().getValue()==this.result.getValue()) {
				return this;
			}
		}		
	
		
		if(this.first_mission!=null && !this.mission_flag && this.needFirstMission) {
			if(this.agentInfo.getPosition().equals(first_mission)) {
				this.mission_flag = true;
				this.result = null;
				return this;
			}
			return this.getPathTo(positionID, this.first_mission);
		}
		
		if(this.result!=null && this.StuckedAgentOrRefuge_BlockedRoad.contains(this.result)){    
			return this;
		}
		
		if(!this.StuckedAgentOrRefuge_BlockedRoad.isEmpty()) {
            List<EntityID> sortList = new ArrayList<>(this.StuckedAgentOrRefuge_BlockedRoad);
            sortList.sort(new DistanceIDSorter(this.worldInfo, this.agentInfo.getID()));
            List<EntityID> nearPolice = new ArrayList<>();
            for (StandardEntity se : worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)) {
            	nearPolice.add(se.getID());
            }
            for (int i = 0;i < sortList.size();++i) {
                EntityID id = this.getClosestEntityID(nearPolice, sortList.get(i));
                if (id.equals(this.agentInfo.getID())) {
                    return this.getPathTo(positionID, sortList.get(i));
                } 
            }
		}	
		
		if(this.result!=null && this.priorityRoads.contains(this.result)){   
			return this;
		}
		
		if(!this.priorityRoads.isEmpty()){
            List<EntityID> sortList = new ArrayList<>(this.priorityRoads);
            sortList.sort(new DistanceIDSorter(this.worldInfo, this.agentInfo.getID()));
            List<EntityID> nearPolice = new ArrayList<>();
            for (StandardEntity se : worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)) {
            	nearPolice.add(se.getID());
            }
            for (int i = 0;i < sortList.size();++i) {
                EntityID id = this.getClosestEntityID(nearPolice, sortList.get(i));
                if (id.equals(this.agentInfo.getID())) {
                    return this.getPathTo(positionID, sortList.get(i));
                } 
            }
		}				
		
		if(this.result!=null && this.targetAreas.contains(this.result)){   
			return this;
		}
		
		
		StandardEntity position =  this.worldInfo.getEntity(this.agentInfo.getPosition());
		if(position instanceof Road) {
			Road road = (Road) position;
			if(this.isRoadPassable(road)) {
				this.result = null;
				return this;
			}
		}else if(position instanceof Building){
			this.result = null;
			return this;
		}
		
		if (this.result == null)	
        { 	
			this.getResultWhenNull();
        }
        return this;
    }
    
    
    private RoadDetector getResultWhenNull() {
    	if(this.agentInfo.getTime() > 5) {		
    		this.targetAreas.clear();
    		for (StandardEntity entity  : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
    			Road road = (Road) entity;
    			if (!this.isRoadPassable(road)){
    				this.targetAreas.add(entity.getID());
    			}
	   		}
    		if(this.targetAreas!=null && !this.targetAreas.isEmpty()) {
    			return this.getPathTo(this.agentInfo.getPosition(),this.getClosestEntityID(targetAreas, this.agentInfo.getPosition()));
    		}  		
    	}
    	return null;
    }
    
    private boolean inRange(PoliceForce a,PoliceForce b) {
    	return (a.getX() - 3000 < b.getX())&&(a.getX() + 3000 > b.getX())&&(a.getY() - 3000 < b.getY())&&(a.getY() + 3000 > b.getY());
    }

    @Override
    public EntityID getTarget()
    {
        return this.result;
    }

    @Override
    public RoadDetector precompute(PrecomputeData precomputeData)
    {    	
    	
    	this.create_guideline();
    	
        super.precompute(precomputeData);
        this.pathPlanning.precompute(precomputeData);
        this.clustering.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        this.roadsize = this.judgeRoad.size();
        precomputeData.setInteger(KEY_ROAD_SIZE, this.roadsize);
        
        for(int i=0 ; i<this.roadsize ; ++i) {
        	precomputeData.setDouble(KEY_START_X + i, this.judgeRoad.get(i).getStartPoint().getX());
        	precomputeData.setDouble(KEY_START_Y + i, this.judgeRoad.get(i).getStartPoint().getY());
        	precomputeData.setDouble(KEY_END_X + i, this.judgeRoad.get(i).getEndPoint().getX());
        	precomputeData.setDouble(KEY_END_Y + i, this.judgeRoad.get(i).getEndPoint().getY());
        	precomputeData.setEntityID(KEY_JUDGE_ROAD + i, this.judgeRoad.get(i).getSelfID());
        }        
               
        return this;
    }

    @Override
    public RoadDetector resume(PrecomputeData precomputeData)
    {
        super.resume(precomputeData);
        this.pathPlanning.resume(precomputeData);
        this.clustering.resume(precomputeData);
        //this.clustering.resume(precomputeData);
        if (this.getCountResume() >= 2)
        {
            return this;
        }
        
        this.roadsize = precomputeData.getInteger(KEY_ROAD_SIZE);
        for(int i=0 ; i<this.roadsize ; ++i) {
        	double startx = precomputeData.getDouble(KEY_START_X + i);
        	double starty = precomputeData.getDouble(KEY_START_Y + i);
        	Point2D start = new Point2D(startx,starty);
        	double endx = precomputeData.getDouble(KEY_END_X + i);
        	double endy = precomputeData.getDouble(KEY_END_Y + i);
        	Point2D end = new Point2D(endx,endy);
        	Road road = (Road) this.worldInfo.getEntity(precomputeData.getEntityID(KEY_JUDGE_ROAD + i));
        	guidelineHelper line = new guidelineHelper(road,start,end);
        	if(! this.judgeRoad.contains(line)) this.judgeRoad.add(line);
        }
        
        this.clustering.calc();
        List<EntityID> sortList = new ArrayList<>();
        for (EntityID id : this.clustering.getClusterEntityIDs(
                this.clustering.getClusterIndex(this.agentInfo.getID()))) {
            if (this.worldInfo.getEntity(id) instanceof Road) {
            	Road road = (Road) this.worldInfo.getEntity(id);
            	boolean flag = false;
            	for(EntityID neighbour : road.getNeighbours()) {
            		StandardEntity neigh = this.worldInfo.getEntity(neighbour);
            		if(neigh instanceof Building) {
            			flag = true;
            		}
            	}
            	if(!flag) {
            		sortList.add(id);
            	}
            }
        }
        sortList.sort(new DistanceIDSorter(this.worldInfo, this.agentInfo.getID()));
        this.first_mission = sortList.get(sortList.size() / 2);        
        int count = 0;
        for(StandardEntity entity  : this.worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)) {
        	if(this.getDistance(this.agentInfo.getID(),entity.getID()) < 30000  
        			&& ! this.agentInfo.getID().equals(entity.getID())) {
        		++count;
        	}
        }
        if(count > 3) this.needFirstMission = true;
        return this;
    }

    @Override
    public RoadDetector preparate()
    {
    	
//    	this.createline();
        super.preparate();
        this.pathPlanning.preparate();
        this.clustering.preparate();
        //this.clustering.preparate();
        if (this.getCountPreparate() >= 2)
        {
            return this;
        }
        
       this.create_guideline();
       
       this.clustering.calc();
       List<EntityID> sortList = new ArrayList<>();
       for (EntityID id : this.clustering.getClusterEntityIDs(
    		   this.clustering.getClusterIndex(this.agentInfo.getID()))) {
    	   if (this.worldInfo.getEntity(id) instanceof Road) {
    		   Road road = (Road) this.worldInfo.getEntity(id);
    		   boolean flag = false;
    		   for(EntityID neighbour : road.getNeighbours()) {
    			   StandardEntity neigh = this.worldInfo.getEntity(neighbour);
    			   if(neigh instanceof Building) {
    				   flag = true;
    			   }
    		   }
    		   if(!flag) {
    			   sortList.add(id);
    		   }
    	   }
       }
       sortList.sort(new DistanceIDSorter(this.worldInfo, this.agentInfo.getID()));
       this.first_mission = sortList.get(sortList.size()/2);
       
       return this;
    }
    @Override
    public RoadDetector updateInfo(MessageManager messageManager)
    {
    	this.messageManager=messageManager;
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2)
        {
            return this;
        }
        
        
        this.Reflect_Message();
		this.pathPlanning.updateInfo(messageManager);
		this.clustering.updateInfo(messageManager);
		
        return this;
    }

    private List<guidelineHelper> judgeRoad = new ArrayList<>();
    private Set<EntityID> StuckedAgentOrRefuge_BlockedRoad = new HashSet<>();
    private Set<EntityID> priorityRoads = new HashSet<>();
    private Set<EntityID> unpassable_Road = new HashSet<>();
    private Set<EntityID> no_need_to_clear = new HashSet<>();
    private Set<EntityID> blockedRoads = new HashSet<>();
    
    
    private Set<EntityID> get_all_Bloacked_Entrance_of_Building(Building building) {
		Set<EntityID> all_Bloacked_Entrance = new HashSet<>();
		EntityID buildingID = building.getID();
		get_all_Bloacked_Entrance_result(all_Bloacked_Entrance, buildingID);
		return all_Bloacked_Entrance;

	}
    
    private void get_all_Bloacked_Entrance_result(Set<EntityID> all_Bloacked_Entrance, EntityID id) {
		Area area = (Area) this.worldInfo.getEntity(id);
		//Road road = (Road) this.worldInfo.getEntity(id);
		for (EntityID neighbor : area.getNeighbours()) {
			Entity entity = this.worldInfo.getEntity(neighbor);
			if (entity instanceof Road ) {
				Road road = (Road) entity;
				if(!this.isRoadPassable(road))
				all_Bloacked_Entrance.add(neighbor);
			}
		}
	}

	/**
	* @Description: 路况更新，对于路线优先级的判别
	* @Author: Bochun-Yue
	* @Date: 2/25/20
	*/
    
    private void update_roads() {
		for (EntityID id : this.worldInfo.getChanged().getChangedEntities()) {
    		StandardEntity entity = (StandardEntity)this.worldInfo.getEntity(id);
    		//refuge
			if (entity != null && entity instanceof Refuge){		
				this.StuckedAgentOrRefuge_BlockedRoad.addAll(this.get_all_Bloacked_Entrance_of_Building((Building) entity));	
			}
		}
		for (EntityID id: this.worldInfo.getChanged().getChangedEntities()) {
			StandardEntity entity = (StandardEntity)this.worldInfo.getEntity(id);
			//hydrant
			if (entity != null  &&  !this.no_need_to_clear.contains(entity.getID()) && entity instanceof Hydrant) {
				Road road = (Road) entity;
				if(!this.isRoadPassable(road)) {
					this.priorityRoads.add(road.getID());
				}
			}
		}
    	
		for (EntityID id : this.worldInfo.getChanged().getChangedEntities()) {
			StandardEntity entity = this.worldInfo.getEntity(id);
			//firebrigade和ambulanceteam
			if (entity instanceof FireBrigade||entity instanceof AmbulanceTeam) {
				Human human = (Human) entity;
				EntityID position = human.getPosition();
				StandardEntity positionEntity = this.worldInfo.getEntity(position);
				if (positionEntity instanceof Road) {
					//堵了埋了或者离blockade距离小于1000
					if (this.is_agent_stucked(human, (Road) positionEntity)||this.is_agent_Buried(human, (Road) positionEntity)) {
						this.StuckedAgentOrRefuge_BlockedRoad.add(position);
					}
				} else if (positionEntity instanceof Building) {
					Set<EntityID> entrances = this.get_all_Bloacked_Entrance_of_Building((Building) positionEntity);
					// if(entrances.size() == (Area)positionEntity.getEdge())
					this.StuckedAgentOrRefuge_BlockedRoad.addAll(entrances);
				}
			}
			//civilian
			else if (entity instanceof Civilian) {
				Human human = (Human) entity;
				if (!human.isPositionDefined() || !human.isHPDefined()||human.getHP()<1000) {
	                continue;
	            }
				EntityID position = human.getPosition();
				StandardEntity positionEntity = this.worldInfo.getEntity(position);
				if (positionEntity instanceof Road) {
					//堵了埋了或者离blockade距离小于1000
					if (this.is_agent_stucked(human, (Road) positionEntity)||this.is_agent_Buried(human, (Road) positionEntity)) {
						this.StuckedAgentOrRefuge_BlockedRoad.add(position);
					}
				} else if (!(positionEntity instanceof Refuge) && positionEntity instanceof Building) {
					Set<EntityID> entrances = this.get_all_Bloacked_Entrance_of_Building((Building) positionEntity);
					this.StuckedAgentOrRefuge_BlockedRoad.addAll(entrances);
				}
			}
		}
			//road 
		for (EntityID id : this.worldInfo.getChanged().getChangedEntities()) {
			StandardEntity entity = this.worldInfo.getEntity(id);
			if (entity instanceof Road) {
				Road road = (Road) entity;
					if (this.isRoadPassable(road)) {
						this.StuckedAgentOrRefuge_BlockedRoad.remove(road.getID());
						this.priorityRoads.remove(road.getID());
						continue;
					}
					boolean BuildFlag=true;
					for(EntityID eid : road.getNeighbours()) {
						StandardEntity ent = (StandardEntity)this.worldInfo.getEntity(eid);
						if(ent instanceof Building) {
							BuildFlag=false;
							break;
						}
					}
					if(BuildFlag) this.priorityRoads.add(road.getID());
			}
		}
    }
	/**
	* @Description: 道路可否通过，以guideline是否被覆盖为判断标准
	* @Author: Bochun-Yue
	* @Date: 3/7/20
	*/
    private boolean isRoadPassable(Road road) {
    	if (!road.isBlockadesDefined() || road.getBlockades().isEmpty()) {
			return true;
		}
		Collection<Blockade> blockades = this.worldInfo.getBlockades(road).stream().filter(Blockade::isApexesDefined)
				.collect(Collectors.toSet());	
			Line2D guideline = null;
			for(guidelineHelper r : this.judgeRoad) {
				if(r.getSelfID().equals(road.getID())) {
					guideline = r.getGuideline();
				}
			}
			if (guideline != null) {
				if(road.getID().getValue() == 263) {
					System.out.println("---------------------("+guideline.getOrigin().getX()+","+guideline.getOrigin().getY()+")---------------");
					System.out.println("---------------------("+guideline.getEndPoint().getX()+","+guideline.getEndPoint().getY()+")---------------");

				}
				for (Blockade blockade : blockades) {
					List<Point2D> Points = GeometryTools2D.vertexArrayToPoints(blockade.getApexes());
					for(int i =0;i<Points.size();++i) {
						if(i!=Points.size()-1) {
							double crossProduct1 = this.getCrossProduct(guideline, Points.get(i));
							double crossProduct2 = this.getCrossProduct(guideline, Points.get(i+1));
							if(crossProduct1<0&&crossProduct2>0 || crossProduct1>0&&crossProduct2<0) {
								Line2D line = new Line2D(Points.get(i),Points.get(i+1));
								Point2D intersect = GeometryTools2D.getIntersectionPoint(line, guideline);
								if(intersect!=null) {
									return false;
								}
							}
						}
						else {
							double crossProduct1 = this.getCrossProduct(guideline, Points.get(i));
							double crossProduct2 = this.getCrossProduct(guideline, Points.get(0));
							if(crossProduct1<0&&crossProduct2>0 || crossProduct1>0&&crossProduct2<0) {
								Line2D line = new Line2D(Points.get(i),Points.get(0));
								Point2D intersect = GeometryTools2D.getIntersectionPoint(line, guideline);
								if(intersect!=null) {
									return false;
								}
							}
						}
					}
				}
				return true;
	        }	
		return false;
	}
	/**
	* @Description: 获取guideline
	* @Author: Bochun-Yue
	* @Date: 3/7/20
	*/
    private Line2D get_longest_line(Road road) {
		List<Edge> edges = road.getEdges();
		List<Point2D> Points = new ArrayList<>();
		for(Edge edge : edges) {
			Point2D point = new Point2D((edge.getStartX()+edge.getEndX())/2,(edge.getStartY()+edge.getEndY())/2);
			Points.add(point);
		}
		Point2D start = null;
		Point2D end = null;
		double max = Double.MIN_VALUE;
		
		for(int i = 0; i < Points.size(); ++i) {
			for(int j = 0; j < Points.size(); ++j) {
				if(j==i) continue;
				else {
					double dist = this.getDistance(Points.get(i).getX(), Points.get(i).getY(), Points.get(j).getX(),Points.get(j).getY());
					if(dist > max) {
						max = dist;
						start = Points.get(i);
						end = Points.get(j);
					}
				}
			}
		}
		if(start!=null && end!=null) {
			Line2D guideline = new Line2D(start,end);
			return guideline;
		}
		else return null;
    }
	/**
	* @Description: 叉积
	* @Author: Bochun-Yue
	* @Date: 3/7/20
	*/
    private double getCrossProduct(Line2D line , Point2D point) {
    	
    	double X = point.getX();
    	double Y = point.getY();
    	double X1 = line.getOrigin().getX();
    	double Y1 = line.getOrigin().getY();
    	double X2 = line.getEndPoint().getX();
    	double Y2 = line.getEndPoint().getY();
    	
    	return ((X2 - X1)*(Y - Y1) - (X - X1)*(Y2 - Y1));
    }
    
    private void create_guideline() {
    	
		for(StandardEntity se : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
			Road road = (Road) se;
    		for(EntityID neighbour : road.getNeighbours()) {
    			if(this.worldInfo.getEntity(neighbour) instanceof Building || this.worldInfo.getEntity(neighbour) instanceof Refuge) {
    				guidelineHelper entrance = new guidelineHelper(this.get_longest_line(road),road);
    				if(!this.judgeRoad.contains(entrance))	this.judgeRoad.add(entrance);
    				for(EntityID id : road.getNeighbours()) {
    	    			StandardEntity entity = this.worldInfo.getEntity(id);
    	    			if(entity instanceof Road) {
    	    				Road next = (Road) entity;
    	    				guidelineHelper line = new guidelineHelper(this.get_longest_line(next),next);
    	    				if(!this.judgeRoad.contains(line)) this.judgeRoad.add(line);
    	    			}
    				}
    			}
    		}
		}
		
		for(StandardEntity se : this.worldInfo.getEntitiesOfType(StandardEntityURN.HYDRANT)) {
			Road road = (Road) se;
			guidelineHelper line = new guidelineHelper(this.get_longest_line(road),road);
			if(!this.judgeRoad.contains(line)) this.judgeRoad.add(line);
		}
    	    	
    	StandardEntity positionEntity = this.worldInfo.getEntity(this.agentInfo.getPosition());
    	
    	if(positionEntity instanceof Road) {
    		Road position = (Road) positionEntity;
    		guidelineHelper guideline = new guidelineHelper(this.get_longest_line(position),position);
    		if(!this.judgeRoad.contains(guideline))	this.judgeRoad.add(guideline);
    		
    		for(EntityID neighbour : position.getNeighbours()) {
    			if(this.worldInfo.getEntity(neighbour) instanceof Road) {
    				Road road = (Road) this.worldInfo.getEntity(neighbour);
    				Edge edge = position.getEdgeTo(neighbour);
    				Point2D start = new Point2D((edge.getStartX()+edge.getEndX())/2,(edge.getStartY()+edge.getEndY())/2);
    				Point2D mid = new Point2D(road.getX(),road.getY());
    				guidelineHelper line = new guidelineHelper(road,start,mid);
    				if(!this.judgeRoad.contains(line))	this.judgeRoad.add(line);
    			}
    		}
    		
    		for(StandardEntity se : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
				boolean flag = false;
				Road otherRoads = (Road) se;

    			this.pathPlanning.setFrom(position.getID());
    			this.pathPlanning.setDestination(se.getID());
    			List<EntityID> path = this.pathPlanning.calc().getResult();
    			if(path!=null && path.size()>2) {
    				for(int i = 1 ; i < path.size() - 1 ; ++i ) {
    					StandardEntity entity = this.worldInfo.getEntity(path.get(i));
    					if(!(entity instanceof Road)) continue;
    					Road road = (Road) entity;
    					Area before = (Area) this.worldInfo.getEntity(path.get(i-1));
    					Area next = (Area) this.worldInfo.getEntity(path.get(i+1));
    					Edge edge1 = before.getEdgeTo(road.getID());
    					Edge edge2 = road.getEdgeTo(next.getID());
    					Point2D start = new Point2D((edge1.getStartX()+edge1.getEndX())/2,(edge1.getStartY()+edge1.getEndY())/2);
    					Point2D end = new Point2D((edge2.getStartX()+edge2.getEndX())/2,(edge2.getStartY()+edge2.getEndY())/2);
        				guidelineHelper line = new guidelineHelper(road,start,end);
        				if(!this.judgeRoad.contains(line))	this.judgeRoad.add(line);
    				}
    			}
    		}
    	}
    	
    	else if(positionEntity instanceof Building) {
    		Building building = (Building) positionEntity;
    		
    		for(StandardEntity se : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
    			this.pathPlanning.setFrom(building.getID());
    			this.pathPlanning.setDestination(se.getID());
    			List<EntityID> path = this.pathPlanning.calc().getResult();
    			if(path!=null && path.size()>2) {
    				for(int i = 1 ; i < path.size() - 1 ; ++i ) {
    					StandardEntity entity = this.worldInfo.getEntity(path.get(i));
    					if(!(entity instanceof Road)) continue;
    					Road road = (Road) entity;
    					Area before = (Area) this.worldInfo.getEntity(path.get(i-1));
    					Area next = (Area) this.worldInfo.getEntity(path.get(i+1));
    					Edge edge1 = before.getEdgeTo(road.getID());
    					Edge edge2 = road.getEdgeTo(next.getID());
    					Point2D start = new Point2D((edge1.getStartX()+edge1.getEndX())/2,(edge1.getStartY()+edge1.getEndY())/2);
    					Point2D end = new Point2D((edge2.getStartX()+edge2.getEndX())/2,(edge2.getStartY()+edge2.getEndY())/2);
        				guidelineHelper line = new guidelineHelper(road,start,end);
        				if(!this.judgeRoad.contains(line))	this.judgeRoad.add(line);
    				}
    			}
    		}
    	}
    	
		for(StandardEntity se : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
			Road road = (Road) se;
			guidelineHelper line = new guidelineHelper(this.get_longest_line(road),road);
			if(!this.judgeRoad.contains(line)) this.judgeRoad.add(line);
		}
    }
    
    
    
    private boolean is_inside_blocks(Human human, Blockade blockade) {

    	if (blockade.isApexesDefined() && human.isXDefined() && human.isYDefined()) {
    		if (blockade.getShape().contains(human.getX(), human.getY())) {
    			return true;
    		}
    	}
    	return false;
    }

    private boolean is_agent_stucked(Human agent, Road road) {
    	Collection<Blockade> blockades = this.worldInfo.getBlockades(road);
    	for (Blockade blockade : blockades) {
    		if (this.is_inside_blocks(agent, blockade)) {
    			return true;
    		}
    	}
    	return false;
    }

    private boolean is_agent_Buried(Human human, Road road) {

    	if (human.isBuriednessDefined() && human.getBuriedness() > 0)
    		return true;
    	return false;
    }
	
	private void Reflect_Message() {

		// reflectMessage
		Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
		for (CommunicationMessage message : messageManager.getReceivedMessageList()) {
			Class<? extends CommunicationMessage> messageClass = message.getClass();
			if (messageClass == MessageAmbulanceTeam.class) {
				this.reflectMessage((MessageAmbulanceTeam) message);
			} else if (messageClass == MessageFireBrigade.class) {
				this.reflectMessage((MessageFireBrigade) message);
			} else if (messageClass == MessageRoad.class) {
				this.reflectMessage((MessageRoad) message, changedEntities);
			} else if (messageClass == MessagePoliceForce.class) {
				this.reflectMessage((MessagePoliceForce) message);
			} else if (messageClass == CommandPolice.class) {
				this.reflectMessage((CommandPolice) message);
			}
		}
	}

	private void reflectMessage(MessageRoad messageRoad, Collection<EntityID> changedEntities) {
		
			if (messageRoad.isBlockadeDefined() && !changedEntities.isEmpty() && !changedEntities.contains(messageRoad.getBlockadeID())) {
				MessageUtil.reflectMessage(this.worldInfo, messageRoad);
			}
			if (messageRoad.isPassable()) {
				this.blockedRoads.remove(messageRoad.getRoadID());
				this.no_need_to_clear.add(messageRoad.getRoadID());
			}
	}

	private void reflectMessage(MessageAmbulanceTeam messageAmbulanceTeam) {
		if (messageAmbulanceTeam.getPosition() == null) {
			return;
		}
		if (messageAmbulanceTeam.getAction() == MessageAmbulanceTeam.ACTION_RESCUE) {
			StandardEntity position = this.worldInfo.getEntity(messageAmbulanceTeam.getPosition());
			if (position != null && position instanceof Building) {
			this.no_need_to_clear.addAll(((Building) position).getNeighbours());				
			this.targetAreas.removeAll(((Building) position).getNeighbours());
			}
		} else if (messageAmbulanceTeam.getAction() == MessageAmbulanceTeam.ACTION_LOAD) {
			StandardEntity position = this.worldInfo.getEntity(messageAmbulanceTeam.getPosition());
			if (position != null && position instanceof Building) {
				this.targetAreas.removeAll(((Building) position).getNeighbours());
			this.no_need_to_clear.addAll(((Building) position).getNeighbours());			
}
		} else if (messageAmbulanceTeam.getAction() == MessageAmbulanceTeam.ACTION_MOVE) {
			if (messageAmbulanceTeam.getTargetID() == null) {
				return;
			}
			StandardEntity target = this.worldInfo.getEntity(messageAmbulanceTeam.getTargetID());
			if (target instanceof Building) {
				for (EntityID id : ((Building) target).getNeighbours()) {
					StandardEntity neighbour = this.worldInfo.getEntity(id);
					if (neighbour instanceof Road) {
						if(this.is_area_blocked(id)) {
//					 		this.StuckedAgentOrRefuge_BlockedRoad.add(id);
							this.priorityRoads.add(id);
						}
					}
				}
			} else if (target instanceof Human) {
				Human human = (Human) target;
				if (human.isPositionDefined()) {
					StandardEntity position = this.worldInfo.getPosition(human);
					if (position instanceof Building) {
						for (EntityID id : ((Building) position).getNeighbours()) {
							StandardEntity neighbour = this.worldInfo.getEntity(id);
							if (neighbour instanceof Road) {
								if(this.is_area_blocked(id)) {
									this.priorityRoads.add(id);
								}
							}
						}
					}
				}
			}
		}
	}

	private void reflectMessage(MessageFireBrigade messageFireBrigade) {
		if (messageFireBrigade.getTargetID() == null) {
			return;
		}
		if (messageFireBrigade.getAction() == MessageFireBrigade.ACTION_REFILL) {
			StandardEntity target = this.worldInfo.getEntity(messageFireBrigade.getTargetID());
			if (target instanceof Building) {
				for (EntityID id : ((Building) target).getNeighbours()) {
					StandardEntity neighbour = this.worldInfo.getEntity(id);
					if (neighbour instanceof Road) {
						if(this.is_area_blocked(id)) {
							this.priorityRoads.add(id);
						}
					}
				}
			} else if (target.getStandardURN() == StandardEntityURN.HYDRANT) {
				this.entrance_of_Refuge_and_Hydrant.add(target.getID());
			}
		}
	}

	private void reflectMessage(MessagePoliceForce messagePoliceForce) {
		if (messagePoliceForce.getAction() == MessagePoliceForce.ACTION_CLEAR) {
			if (messagePoliceForce.getAgentID().getValue() != this.agentInfo.getID().getValue()) {
				if (messagePoliceForce.isTargetDefined()) {
					EntityID targetID = messagePoliceForce.getTargetID();
					if (targetID == null) {
						return;
					}
					StandardEntity entity = this.worldInfo.getEntity(targetID);
					if (entity == null) {
						return;
					}

					if (entity instanceof Area) {
						this.priorityRoads.remove(targetID);
						this.no_need_to_clear.add(targetID);
						if (this.result != null && this.result.getValue() == targetID.getValue()) {
							if (this.agentInfo.getID().getValue() < messagePoliceForce.getAgentID().getValue()) {
								this.result = null;
								//this.result = this.get_new_result();
							}
						}
					} else if (entity.getStandardURN() == StandardEntityURN.BLOCKADE) {
						EntityID position = ((Blockade) entity).getPosition();
						if(position!=null) {
							this.priorityRoads.remove(targetID);
							this.no_need_to_clear.add(targetID);
							if (this.result != null && this.result.getValue() == position.getValue()) {
								if (this.agentInfo.getID().getValue() < messagePoliceForce.getAgentID().getValue()) {
									this.result = null;
									//this.result = this.get_new_result();
								}
							}
						}
					}

				}
			}
		}
	}

	private void reflectMessage(CommandPolice commandPolice) {
		boolean flag = false;
		if (commandPolice.isToIDDefined() && this.agentInfo.getID().getValue() == commandPolice.getToID().getValue()) {
			flag = true;
		} else if (commandPolice.isBroadcast()) {
			flag = true;
		}
		if (flag && commandPolice.getAction() == CommandPolice.ACTION_CLEAR) {
			if (commandPolice.getTargetID() == null) {
				return;
			}
			StandardEntity target = this.worldInfo.getEntity(commandPolice.getTargetID());
			if (target instanceof Area) {
				Road road = (Road) target;
				if(!this.isRoadPassable(road)) {
					this.StuckedAgentOrRefuge_BlockedRoad.add(target.getID());
				}
			} else if (target.getStandardURN() == StandardEntityURN.BLOCKADE) {
				Blockade blockade = (Blockade) target;
				if (blockade.isPositionDefined()) {
					StandardEntity position = worldInfo.getEntity(blockade.getPosition());
					if(position != null) {
						if(position instanceof Road) {
							Road road = (Road) position;
							if(!this.isRoadPassable(road))
								this.StuckedAgentOrRefuge_BlockedRoad.add(blockade.getPosition());
						}
					}
				}
			}
		}
	}
}
