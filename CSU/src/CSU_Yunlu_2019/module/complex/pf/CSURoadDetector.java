package CSU_Yunlu_2019.module.complex.pf;

import java.util.*;

import adf.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.agent.communication.standard.bundle.centralized.MessageReport;
import adf.agent.communication.standard.bundle.information.MessageAmbulanceTeam;
import adf.agent.communication.standard.bundle.information.MessageFireBrigade;
import adf.agent.communication.standard.bundle.information.MessagePoliceForce;
import adf.agent.communication.standard.bundle.information.MessageRoad;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import rescuecore2.misc.Pair;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.RoadDetector;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Entity;
import CSU_Yunlu_2019.standard.Ruler;
//import PF_CSUpfRoadDetector.sorter;

public class CSURoadDetector extends RoadDetector {
    private Set<EntityID> targetAreas;
    private Set<EntityID> priorityRoads;

    private PathPlanning pathPlanning;
    private Clustering clustering;

    private EntityID result = null;
    private boolean have_police_office = false;


	int lastTime = 0;
	double lastx  =0 ;
	double lasty = 0;
    
    private Set<EntityID> entrances = new HashSet<>();
	private Set<EntityID> imaportant_roads = new HashSet<>();
	private Set<EntityID> entrance_of_Refuge_and_Hydrant = new HashSet<>();
	private MessageManager messageManager =new MessageManager();
    
/*	private boolean is_no_move(){

		double currentx = this.agentInfo.getX();
		double currenty = this.agentInfo.getY();
		int currentTime = this.agentInfo.getTime();

		if(currentx==lastx &&currenty == lasty&&(currentTime-this.lastTime>3)){
			return true;
		}
	
		else{
			this.lastx = currentx;
			this.lasty = currenty;
			this.lastTime = currentTime;
		}	
		return false;
	}
*/	
/*	private boolean is_no_move(Human human){

		double currentx = human.getX();
		double currenty = human.getY();
		int currentTime = this.agentInfo.getTime();

		if(currentx==lastx &&currenty == lasty&&(currentTime-this.lastTime>3)){
			return true;
		}
	
		else{
			this.lastx = currentx;
			this.lasty = currenty;
			this.lastTime = currentTime;
		}	
		return false;
	}
*/	
	
	
	
/*	private void P_information(){
		
		
		System.out.println("********************************************");
		System.out.println("the no_need_clear areas"+this.no_need_to_clear.size());
		System.out.println("entrances' sizes:"+entrances.size());
		System.out.println("imaportant_roads' size:"+imaportant_roads.size());
		System.out.println("entrance_of_Refuge_and_Hydrant' size:"+entrance_of_Refuge_and_Hydrant.size());
		System.out.println("this.blockedRoads' size:"+this.blockedRoads.size());
		System.out.println("this.StuckedAgentOrRefuge_BlockedRoad' size:"+this.StuckedAgentOrRefuge_BlockedRoad.size());
		System.out.println("********************************************");
		
		
	}
	
*/	
/*    private void getEntrance() {
    	
    	this.entrances = new HashSet<>();
    	
    	this.imaportant_roads = new HashSet<>();
    	this.blockedRoads  = new HashSet<>();
    	
		for (EntityID i : this.worldInfo.getChanged().getChangedEntities()) {
			StandardEntity entity = (StandardEntity)this.worldInfo.getEntity(i);
			if(entity instanceof Road){
				Road road = (Road) entity;
				if (road.isBlockadesDefined() && !road.getBlockades().isEmpty()){
						blockedRoads.add(entity.getID());
				}
					
				for (EntityID id : road.getNeighbours()) {
					Entity e = this.worldInfo.getEntity(id);
					if (e instanceof Building && road.isBlockadesDefined()&&!road.getBlockades().isEmpty()) {
							entrances.add(entity.getID());
						get_important_cross(entity.getID());
						break;
					}
				}
			
			}
		}
		
		this.imaportant_roads.removeAll(this.entrances);
		//this.blockedRoads.removeAll(this.entrances);
	
	}
*/    
/*    private void get_important_cross(EntityID id) {
		for (EntityID neighbour : ((Area) this.worldInfo.getEntity(id)).getNeighbours()) {
			if (this.worldInfo.getEntity(neighbour) instanceof Road) {
				this.imaportant_roads.add(id);
				break;
			}
		}
	}
*/
    private void get_original(){
    	
    	this.priorityRoads = new HashSet<>();
    	
    	for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
			Road road = (Road) entity;
			if (road.isBlockadesDefined() && !road.getBlockades().isEmpty()){
					this.priorityRoads.add(entity.getID());
			}
		}
		
		this.imaportant_roads.removeAll(this.entrances);
		this.priorityRoads.removeAll(this.no_need_to_clear);
    }
    
    public CSURoadDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData)
    {
        super(ai, wi, si, moduleManager, developData);
        switch (scenarioInfo.getMode())
        {
            case PRECOMPUTATION_PHASE:
            	this.pathPlanning = moduleManager.getModule("RoadDetector.PathPlanning",
    					"CSU_Yunlu_2019.module.algorithm.AStarPathPlanning");
                break;
            case PRECOMPUTED:
            	this.pathPlanning = moduleManager.getModule("RoadDetector.PathPlanning",
    					"CSU_Yunlu_2019.module.algorithm.AStarPathPlanning");
                break;
            case NON_PRECOMPUTE:
            	this.pathPlanning = moduleManager.getModule("RoadDetector.PathPlanning",
    					"CSU_Yunlu_2019.module.algorithm.AStarPathPlanning");
                break;
        }
	//this.clustering = moduleManager.getModule("PF_CSUpfRoadDetector.Clustering",
    					//"CSU.module.algorithm.pf.PF_SampleKMeans");
        registerModule(this.pathPlanning);
	//registerModule(this.clustering);
        this.have_police_office = !wi.getEntityIDsOfType(StandardEntityURN.POLICE_OFFICE).isEmpty();
        this.get_original();
        this.result = null;
    }

    
/*    private void get_near_police(List<EntityID> nearPolice) {
		for (EntityID id : this.worldInfo.getChanged().getChangedEntities()) {
			if (this.worldInfo.getEntity(id) instanceof PoliceForce)
				nearPolice.add(id);
		}
	}
 */   
    
    private boolean is_area_blocked(EntityID id){
		Area area = (Area) this.worldInfo.getEntity(id);
		if(area.isBlockadesDefined()&&!area.getBlockades().isEmpty())  return true;
		return false;
	}
	
 /*   
    private boolean is_nearsest_to_me(List<EntityID> nearPolice, EntityID id) {

		if(nearPolice == null || nearPolice.isEmpty()) return true;
		
		double me_distance = getDistance(this.agentInfo.getPosition(), id);

		for (EntityID nearplice_id : nearPolice) {
			double d = getDistance(nearplice_id, id);
			if (d < me_distance)
				return false;
		}
		return true;
	}

    
private EntityID get_result_from_set(Set<EntityID> set, List<EntityID> nearPolice) {
		
		if(set == null) return null;
		
		EntityID positionID = this.agentInfo.getPosition();
		if (set.contains(positionID)) {
			if ( is_area_blocked(positionID) ) {//  &&  !this.no_need_to_clear.contains(positionID)
				return positionID;
			} else
				set.remove(positionID);
		}
		if (!set.isEmpty()) {
			List<EntityID> sortList = new ArrayList<>(set);
			sortList.sort(new sorter(this.worldInfo, this.agentInfo.getID()));
			for (EntityID id : sortList) {// int i = 0;i < sortList.size();++i)
											// {
				if (is_area_blocked(id)  ) {// && !this.no_need_to_clear.contains(id)
					if (is_nearsest_to_me(nearPolice, id)) {
						if (this.have_police_office)
							this.messageManager
									.addMessage(new MessageReport(true, false, true, this.agentInfo.getID()));
						
						try{
						//return id;
						this.pathPlanning.setFrom(positionID);
		                this.pathPlanning.setDestination(id);
		                List<EntityID> path = this.pathPlanning.calc().getResult();
		                if (path != null && path.size() > 0)
		                {
		                	return path.get(path.size() - 1);
		                }
		                }
						catch (Exception e){
							return id;
						}
						
					}
				} else
					set.remove(id);
			}
		}
		return null;
	}
*/	
private class sorter implements Comparator<EntityID> {
	private WorldInfo worldInfo;
	private EntityID id;

	public sorter(WorldInfo worldInfo, EntityID id) {
		this.worldInfo = worldInfo;
		this.id = id;
	}

	public int compare(EntityID a, EntityID b) {
		int d1 = this.worldInfo.getDistance(this.id, a);
		int d2 = this.worldInfo.getDistance(this.id, b);
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
	
private double getDistance(Human human,Road road) {
	Collection<Blockade> blockades = this.worldInfo.getBlockades(road);
	Point2D basePoint = new Point2D(human.getX(),human.getY());
	double nearest = Double.MAX_VALUE;
	
	for (Blockade blockade : blockades) {
		if(blockade.getShape().contains(human.getX(), human.getY())) {
			return 0;
		}
		List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(blockade.getApexes()), true);
		for(Line2D line : lines) {
			rescuecore2.misc.geometry.Point2D closest = GeometryTools2D.getClosestPointOnSegment(line, basePoint);
	        double d = GeometryTools2D.getDistance(basePoint, closest);
	        if (d < nearest) {
	            nearest = d;
	        }
		}
	}
	return nearest;
}


/*
	private void clear_Blocked_Roads(Set<EntityID> ori_road){

		Set<EntityID> roads = new HashSet<>();
		for (EntityID i : ori_road) {
			StandardEntity entity = (StandardEntity)this.worldInfo.getEntity(i);
			if(entity instanceof Road){
				Road road = (Road) entity;
				if (road.isBlockadesDefined() && !road.getBlockades().isEmpty()){						
				}
				else{
				roads.add(i);
				}			
			}
		}
		ori_road.removeAll(roads);
		ori_road.removeAll(no_need_to_clear);
	}
*/	
	//摘于MRL
	private RoadDetector getRoadDetector(EntityID positionID, Set<EntityID> entityIDSet) {
      //智能体自己的位置和
        this.pathPlanning.setFrom(positionID);
        this.pathPlanning.setDestination(entityIDSet);
        List<EntityID> path = this.pathPlanning.calc().getResult();
        if (path != null && path.size() > 0) {
            this.result = path.get(path.size() - 1);
        }
        return this;
    }
	//新加
	private RoadDetector getPathTo(EntityID positionIDfrom, EntityID positionIDto) {
		this.pathPlanning.setFrom(positionIDfrom);
        this.pathPlanning.setDestination(positionIDto);
        List<EntityID> path = this.pathPlanning.calc().getResult();
        if (path != null && path.size() > 0) {
            this.result = path.get(path.size() - 1);
        }
        return this;
	}

    @Override
    public RoadDetector calc()
    {

    	//this.result是机器人要去的地方
    	//目标是返回下一个要去的地方的id
    	List<EntityID> nearPolice = new ArrayList<>();    
	EntityID id ;
		
//		if(this.result == null || !this.StuckedAgent_BlockedRoad.contains(this.result) ){   //目的地为空的话或者
//		this.clear_Blocked_Roads(this.StuckedAgent_BlockedRoad);
//		id = get_result_from_set(this.StuckedAgent_BlockedRoad, nearPolice);//id是下一个要去的目的地，自己是最近的就会去清理最近的障碍物
//		if (id != null)
//			{
//			this.result=id;
//			return this;    //寻路返回下一一个要去的地方
//
//			}
//
//		}
//		//如果机器人有地方要去的话
//		//the second step
//		if(this.result == null || this.entrance_of_Refuge_and_Hydrant.contains(this.result)){
//		this.entrance_of_Refuge_and_Hydrant .removeAll(this.no_need_to_clear);
//		//this.clear_Blocked_Roads();
//		id = get_result_from_set(this.entrance_of_Refuge_and_Hydrant, nearPolice);
//		if (id != null)
//			{
//			this.result = id;
//			//System.out.println("________________________choose entrance_of_Refuge_and_Hydrant");
    	EntityID positionID = this.agentInfo.getPosition();
		this.StuckedAgentOrRefuge_BlockedRoad.removeAll(no_need_to_clear);
		this.priorityRoads.removeAll(no_need_to_clear);
		//update_roads()函数本来在updateInfo()中，更新StuckedAgentOrRefuge_Blocked，加入避难所，消防栓，消防等所在的街区
		this.update_roads();
		//先清理StuckedAgentOrRefuge_Blocked
		if(!this.StuckedAgentOrRefuge_BlockedRoad.isEmpty()) {
			return getRoadDetector(positionID,this.StuckedAgentOrRefuge_BlockedRoad);//this.result在此处被赋值主要是前往对应的街道
		}
		//result在这些里边就仍然不变，下次运行执行
		if(this.result != null && this.StuckedAgentOrRefuge_BlockedRoad.contains(this.result) ||this.result != null && this.priorityRoads .contains(this.result)){
			return this;
		}
		if (this.result == null)
			
        { 	//找最近的火警或者医疗队			
            double min=Double.MAX_VALUE;
            StandardEntity target = null;
            for(StandardEntity SE:worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE,StandardEntityURN.AMBULANCE_TEAM)) {
            	double minDistance = this.getDistance(this.agentInfo.getX(), this.agentInfo.getY(),worldInfo.getLocation(SE).first() ,worldInfo.getLocation(SE).second());
            	if(minDistance < min) {
            		min = minDistance;
            		target = SE;
            	}
            }
            if(target!=null) {	
            	return this.getPathTo(positionID, target.getID());
            }           
            //没找到就去priorityRoads
            if(!this.priorityRoads.isEmpty()){    				
    			return getRoadDetector(positionID,this.priorityRoads);
    		}
			
        }
        return this;
    }

    @Override
    public EntityID getTarget()
    {
        return this.result;
    }

    @Override
    public RoadDetector precompute(PrecomputeData precomputeData)
    {
        super.precompute(precomputeData);
        this.pathPlanning.precompute(precomputeData);
		//this.clustering.precompute(precomputeData);
	
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        return this;
    }

    @Override
    public RoadDetector resume(PrecomputeData precomputeData)
    {
        super.resume(precomputeData);
        this.pathPlanning.resume(precomputeData);
        //this.clustering.resume(precomputeData);
        if (this.getCountResume() >= 2)
        {
            return this;
        }
        
        return this;
    }

    @Override
    public RoadDetector preparate()
    {
        super.preparate();
        this.pathPlanning.preparate();
        //this.clustering.preparate();
        if (this.getCountPreparate() >= 2)
        {
            return this;
        }
       
        return this;
    }
    @Override
    public RoadDetector updateInfo(MessageManager messageManager)
    {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2)
        {
            return this;
        }
        
        
        this.Reflect_Message();
        if (this.result != null) {
			if (this.agentInfo.getPosition().equals(this.result)
					|| this.worldInfo.getChanged().getChangedEntities().contains(this.result)) {
				StandardEntity entity = this.worldInfo.getEntity(this.result);
				if (entity instanceof Building) {
					this.no_need_to_clear.add(this.result);
					this.blockedRoads.remove(this.result);
					this.result = null;
					//this.result = this.get_new_result();
				} else if (entity instanceof Road) {
					Road road = (Road) entity;
					if (!road.isBlockadesDefined() || road.getBlockades().isEmpty()) {
						this.no_need_to_clear.add(this.result);
						this.blockedRoads.remove(this.result);
						this.result = null;
						//this.result = this.get_new_result();
					}
				}
			}
		}
        return this;
    }

    private Set<EntityID> StuckedAgentOrRefuge_BlockedRoad = new HashSet<>();
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
			if (area instanceof Road && area.isBlockadesDefined()&&!area.getBlockades().isEmpty())
				all_Bloacked_Entrance.add(id);
		}
	}

    //更新路况
    private void update_roads() {
		  //将避难所附近的障碍物全部加入 StuckedAgentOrRefuge_BlockedRoad附近
		for (EntityID id : this.worldInfo.getChanged().getChangedEntities()) {
    		StandardEntity entity = (StandardEntity)this.worldInfo.getEntity(id);
    		//refuge
			if (entity != null && entity instanceof Refuge){		
				this.StuckedAgentOrRefuge_BlockedRoad.addAll(this.get_all_Bloacked_Entrance_of_Building((Building) entity));	
			}
		}
		
		 //消防栓优先清理通过no_need_to_clear判断需不需要清理，加入priorityRoad数组
		for (EntityID id: this.worldInfo.getChanged().getChangedEntities()) {
			StandardEntity entity = (StandardEntity)this.worldInfo.getEntity(id);
			//hydrant
			if (entity != null  &&  !this.no_need_to_clear.contains(entity.getID()) && entity instanceof Hydrant)
				this.priorityRoads.add(entity.getID());
		}
    	
		//接下来集中清理火警和救护车队	，建筑物的出入口，路段
		for (EntityID id : this.worldInfo.getChanged().getChangedEntities()) {
			StandardEntity entity = this.worldInfo.getEntity(id);
			//firebrigade和ambulanceteam
			if (entity instanceof FireBrigade||entity instanceof AmbulanceTeam) {
				Human human = (Human) entity;
				EntityID position = human.getPosition();
				StandardEntity positionEntity = this.worldInfo.getEntity(position);
				if (positionEntity instanceof Road) {
					//堵了埋了或者离blockade距离小于1000
					if (this.is_agent_stucked(human, (Road) positionEntity)||this.is_agent_Buried(human, (Road) positionEntity)||getDistance(human,(Road)positionEntity)<1000) {
						this.StuckedAgentOrRefuge_BlockedRoad.add(position);
					}
				} else if (positionEntity instanceof Building) {
					Set<EntityID> entrances = this.get_all_Bloacked_Entrance_of_Building((Building) positionEntity);
					// if(entrances.size() == (Area)positionEntity.getEdge())
					this.StuckedAgentOrRefuge_BlockedRoad.addAll(entrances);
				}
			}
			//civilian，
			else if (entity instanceof Civilian) {
				Human human = (Human) entity;
				if (!human.isPositionDefined() || !human.isHPDefined()||human.getHP()<1000) {
	                continue;
	            }
				EntityID position = human.getPosition();
				StandardEntity positionEntity = this.worldInfo.getEntity(position);
				if (positionEntity instanceof Road) {
					//堵了埋了或者离blockade距离小于1000
					if (this.is_agent_stucked(human, (Road) positionEntity)||this.is_agent_Buried(human, (Road) positionEntity)||getDistance(human,(Road)positionEntity)<1000) {
						this.StuckedAgentOrRefuge_BlockedRoad.add(position);
					}
				} else if (!(positionEntity instanceof Refuge) && positionEntity instanceof Building) {
					Set<EntityID> entrances = this.get_all_Bloacked_Entrance_of_Building((Building) positionEntity);
					this.priorityRoads.addAll(entrances);
				}
			}
			//road 
			else if (entity instanceof Road) {
				Road road = (Road) entity;
				if (!road.isBlockadesDefined() || road.getBlockades().isEmpty()) {
					this.no_need_to_clear.add(id);   
				}
				//我想的是这条路上没有被困的智能体就从set里去掉
				if(this.StuckedAgentOrRefuge_BlockedRoad.contains(entity)) {
					boolean flag = false;
					for (EntityID eid : this.worldInfo.getChanged().getChangedEntities()) {
						StandardEntity et = this.worldInfo.getEntity(eid);
						if ((et instanceof FireBrigade) || (et instanceof AmbulanceTeam) || (et instanceof Civilian)) {
							Human human = (Human) et;
							//堵了埋了或者离blockade距离小于1000
							if(this.is_agent_stucked(human, (Road)entity)|| this.is_agent_Buried(human,(Road) entity)) {
								flag=true;
							}
						}
					}
					if(!flag) {
						this.StuckedAgentOrRefuge_BlockedRoad.remove(entity);
						this.no_need_to_clear.add(id);
					}
				}else this.priorityRoads.add(id);
			}
			
		}
    }

			
		
	

/*    private double getAngle(Vector2D v1, Vector2D v2) {
		double flag = (v1.getX() * v2.getY()) - (v1.getY() * v2.getX());
		double angle = Math
				.acos(((v1.getX() * v2.getX()) + (v1.getY() * v2.getY())) / (v1.getLength() * v2.getLength()));
		if (flag > 0) {
			return angle;
		}
		if (flag < 0) {
			return -1 * angle;
		}
		return 0.0D;
	}
*/
    private boolean is_inside_blocks(Human human, Blockade blockade) {
    	//改动
    	
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
					if (neighbour instanceof Road && this.is_area_blocked(id)) {
//						this.StuckedAgentOrRefuge_BlockedRoad.add(id);
						this.priorityRoads.add(id);
					}
				}
			} else if (target instanceof Human) {
				Human human = (Human) target;
				if (human.isPositionDefined()) {
					StandardEntity position = this.worldInfo.getPosition(human);
					if (position instanceof Building) {
						for (EntityID id : ((Building) position).getNeighbours()) {
							StandardEntity neighbour = this.worldInfo.getEntity(id);
							if (neighbour instanceof Road &&  this.is_area_blocked(id)) {
								this.priorityRoads.add(id);
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
					if (neighbour instanceof Road  && this.is_area_blocked(id)) {
//						this.StuckedAgentOrRefuge_BlockedRoad.add(id);
						this.priorityRoads.add(id);
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
				// this.priorityRoads.add(target.getID());
				this.StuckedAgentOrRefuge_BlockedRoad.add(target.getID());
			} else if (target.getStandardURN() == StandardEntityURN.BLOCKADE) {
				Blockade blockade = (Blockade) target;
				if (blockade.isPositionDefined()) {
					// this.priorityRoads.add(blockade.getPosition());
					this.StuckedAgentOrRefuge_BlockedRoad.add(blockade.getPosition());
				}
			}
		}
	}

	
	
	
}
