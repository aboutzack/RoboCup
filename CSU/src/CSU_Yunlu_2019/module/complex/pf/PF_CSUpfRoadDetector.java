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
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Entity;

//import PF_CSUpfRoadDetector.sorter;

public class PF_CSUpfRoadDetector extends RoadDetector {
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
    
	private boolean is_no_move(){

	double currentx = this.agentInfo.getX();
	double currenty = this.agentInfo.getY();
	int currentTime = this.agentInfo.getTime();

	if(currentx==lastx &&currenty == lasty&&(currentTime-this.lastTime>5)){

	//System.out.println("***************************************don't move for a long 5 seconds*********");




	return true;

	
	}


	else{
	this.lastx = currentx;
	this.lasty = currenty;
	this.lastTime = currentTime;
	


}	
return false;
}
	
	
	
	
	
	private void P_information(){
		
		
		System.out.println("********************************************");
		System.out.println("the no_need_clear areas"+this.no_need_to_clear.size());
		System.out.println("entrances' sizes:"+entrances.size());
		System.out.println("imaportant_roads' size:"+imaportant_roads.size());
		System.out.println("entrance_of_Refuge_and_Hydrant' size:"+entrance_of_Refuge_and_Hydrant.size());
		System.out.println("this.blockedRoads' size:"+this.blockedRoads.size());
		System.out.println("this.StuckedAgent_BlockedRoad' size:"+this.StuckedAgent_BlockedRoad.size());
		System.out.println("********************************************");
		
		
	}
	
	
    private void getEntrance() {
    	
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
    
    private void get_important_cross(EntityID id) {
		for (EntityID neighbour : ((Area) this.worldInfo.getEntity(id)).getNeighbours()) {
			if (this.worldInfo.getEntity(neighbour) instanceof Road) {
				this.imaportant_roads.add(id);
				break;
			}
		}
	}

    private void get_original(){
    	
    	//this.entrances = new HashSet<>();
    	
    	//this.imaportant_roads = new HashSet<>();
    	this.blockedRoads  = new HashSet<>();
    	
    	for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
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
		
		this.imaportant_roads.removeAll(this.entrances);
		//this.blockedRoads.removeAll(this.entrances);
		this.blockedRoads.removeAll(this.no_need_to_clear);
    }
    
    public PF_CSUpfRoadDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData)
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

    
    private void get_near_police(List<EntityID> nearPolice) {
		for (EntityID id : this.worldInfo.getChanged().getChangedEntities()) {
			if (this.worldInfo.getEntity(id) instanceof PoliceForce)
				nearPolice.add(id);
		}
	}
    
    
    private boolean is_area_blocked(EntityID id){
		Area area = (Area) this.worldInfo.getEntity(id);
		if(area.isBlockadesDefined()&&!area.getBlockades().isEmpty())  return true;
		return false;
	}
	
    
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
	
    



	private void clear_Blocked_Roads(Set<EntityID> ori_road){

	Set<EntityID> roads = new HashSet<>();
	for (EntityID i : ori_road) {
			StandardEntity entity = (StandardEntity)this.worldInfo.getEntity(i);
			if(entity instanceof Road){
				Road road = (Road) entity;
				if (road.isBlockadesDefined() && !road.getBlockades().isEmpty()){
						//blockedRoads.add(entity.getID());
						
				}
				else{
				roads.add(i);
				//this.StuckedAgent_BlockedRoad.remove(i);
				}
					
				
			
			}
		}
		ori_road.removeAll(roads);
		}



    @Override
    public RoadDetector calc()
    {
    	
    	//System.out.println("###################clca Function:############################");
		//this.P_information();
    	
    	List<EntityID> nearPolice = new ArrayList<>();
		// nearPolice.add(this.agentInfo.getID());
		//get_near_police(nearPolice);
    	

		EntityID id ;


	//int time = this.agentInfo.getTime();

	//if(time%100 == 0){
	//this.clustering.clac();
	
	//Set<EntityID> center = new HashSet<>(this.clustering.centerIDs());

	//id  = get_result_from_set(center, nearPolice);
	//while(center.size() != 1){
	//	center.remove(id);
	//	id = get_result_from_set(center, nearPolice);
	//	}
	///this.result = id;
	//System.out.println("choose another center");
	//return this;

//}

		
	

//		if(this.is_no_move()){
//		id = get_result_from_set(this.blockedRoads, nearPolice);
//		while(id == this.result){
//		this.blockedRoads.remove(this.result);
//		id = get_result_from_set(this.blockedRoads, nearPolice);
//		}
//		this.result = id;
//		
//		System.out.println("***************chosse another target***************");
//		}



		//this.StuckedAgent_BlockedRoad.removeAll(this.no_need_to_clear );
		if(this.result == null || !this.StuckedAgent_BlockedRoad.contains(this.result) ){
		//this.StuckedAgent_BlockedRoad.removeAll(this.no_need_to_clear);
		this.clear_Blocked_Roads(this.StuckedAgent_BlockedRoad);
		id = get_result_from_set(this.StuckedAgent_BlockedRoad, nearPolice);
		if (id != null)
			{
			//System.out.println("_______________________choose the Blocked_agent Roads");
			this.result=id; 
			return this;
			
			}

		}
		//if (this.result != null)
			//return this;
		
		
		//the first step
       // Set<EntityID> Areas1 = new HashSet<>();
		//Areas1.addAll(get_center_of_Set(this.StuckedAgent_BlockedRoad));
		//Areas1.addAll(get_center_of_Set(this.BuriedAgent_BlockedRoad));
		//Areas1.addAll();
		//Areas1.addAll(this.BuriedAgent_BlockedRoad);
		
		
		
		
		//the second step
		if(this.result == null || this.entrance_of_Refuge_and_Hydrant.contains(this.result)){
		this.entrance_of_Refuge_and_Hydrant .removeAll(this.no_need_to_clear);
		//this.clear_Blocked_Roads();
		id = get_result_from_set(this.entrance_of_Refuge_and_Hydrant, nearPolice);
		if (id != null)
			{
			this.result = id;
			//System.out.println("________________________choose entrance_of_Refuge_and_Hydrant");
			return this;
			
			}
    	
		}
		if (this.result == null)
			
        { 
			 
			
			
			//this.result = this.get_new_result();
			//this.clear_Blocked_Roads(this.blockedRoads);
			this.result = get_result_from_set(this.blockedRoads, nearPolice);
			if (this.result != null)
				{
				//System.out.println("_____________________-choose the new result");
				return this;
				
				}
			this.get_original();
			 this.result = get_result_from_set(this.imaportant_roads, nearPolice);
				if (this.result != null)
					{
					//System.out.println("______________________choose imaportant_roads");
					return this;
					
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
    
    
    private EntityID get_new_result(){
    	

    	List<EntityID> nearPolice = new ArrayList<>();
		// nearPolice.add(this.agentInfo.getID());
		//get_near_police(nearPolice);
    	EntityID next_result = null;
    	
    	
//    	//the first step
//        Set<EntityID> Areas1 = new HashSet<>();
//		//Areas1.addAll(get_center_of_Set(this.StuckedAgent_BlockedRoad));
//		//Areas1.addAll(get_center_of_Set(this.BuriedAgent_BlockedRoad));
//		Areas1.addAll(this.StuckedAgent_BlockedRoad);
//		//Areas1.addAll(this.BuriedAgent_BlockedRoad);
//		next_result = get_result_from_set(Areas1, nearPolice);
//		
//		if (next_result != null)
//			return next_result;
//		
//		//the second step
//		next_result = get_result_from_set(this.entrance_of_Refuge_and_Hydrant, nearPolice);
//		if (next_result != null)
//			return next_result;
    	
    	
    	
    	//this.getEntrance();
    	this.get_original();
    	next_result = get_result_from_set(this.blockedRoads, nearPolice);
			return next_result;
    }
    
    @Override
    public RoadDetector updateInfo(MessageManager messageManager)
    {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2)
        {
            return this;
        }
        
        this.update_roads();
        
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
        
       
        
        
        /*
        if (this.result != null)
        {
            if (this.agentInfo.getPosition().equals(this.result))
            {
                StandardEntity entity = this.worldInfo.getEntity(this.result);
                if (entity instanceof Building)
                {
                    this.result = null;
                }
                else if (entity instanceof Road)
                {
                    Road road = (Road) entity;
                    if (!road.isBlockadesDefined() || road.getBlockades().isEmpty())
                    {
                        this.targetAreas.remove(this.result);
                        this.result = null;
                    }
                }
            }
        }
        
        for (EntityID id : this.worldInfo.getChanged().getChangedEntities())
        {
            StandardEntity entity = this.worldInfo.getEntity(id);
            if (entity instanceof Road)
            {
                Road road = (Road) entity;
                if (!road.isBlockadesDefined() || road.getBlockades().isEmpty())
                {
                    this.targetAreas.remove(id);
                }
            }
        }*/
        
       // System.out.println("###################updateInfo Function:############################");
		//this.P_information();
        return this;
    }

    private Set<EntityID> StuckedAgent_BlockedRoad = new HashSet<>();
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

    
    private void update_roads() {
    	
    	for (EntityID id : this.worldInfo.getChanged().getChangedEntities()) {
    		StandardEntity entity = (StandardEntity)this.worldInfo.getEntity(id);
			if (entity != null && entity instanceof Refuge)
				this.entrance_of_Refuge_and_Hydrant
						.addAll(this.get_all_Bloacked_Entrance_of_Building((Building) entity));
		}
		for (EntityID id: this.worldInfo.getChanged().getChangedEntities()) {
			StandardEntity entity = (StandardEntity)this.worldInfo.getEntity(id);
			if (entity != null  &&  !this.no_need_to_clear.contains(entity.getID()) && entity instanceof Hydrant)
				this.entrance_of_Refuge_and_Hydrant.add(entity.getID());
		}
    	
		for (EntityID id : this.worldInfo.getChanged().getChangedEntities()) {
			StandardEntity entity = this.worldInfo.getEntity(id);
			if ((entity instanceof FireBrigade) || (entity instanceof AmbulanceTeam) || (entity instanceof Civilian)) {
				Human human = (Human) entity;
				EntityID position = human.getPosition();
				StandardEntity positionEntity = this.worldInfo.getEntity(position);
				if (positionEntity instanceof Road) {
					if (this.is_agent_stucked(human, (Road) positionEntity)) {
						this.StuckedAgent_BlockedRoad.add(position);
					}
					if (this.is_agent_Buried(human, (Road) positionEntity)) {
						this.StuckedAgent_BlockedRoad.add(position);
					}
				} else if (positionEntity instanceof Building) {
					Set<EntityID> entrances = this.get_all_Bloacked_Entrance_of_Building((Building) positionEntity);
					// if(entrances.size() == (Area)positionEntity.getEdge())
					this.StuckedAgent_BlockedRoad.addAll(entrances);
				}
			} else if (entity instanceof Road) {
				Road road = (Road) entity;
				if (!road.isBlockadesDefined() || road.getBlockades().isEmpty()) {
					this.no_need_to_clear.add(id);
					this.blockedRoads.remove(id);
				} else
					this.blockedRoads.add(id);
			}
			
		}
	}

    private double getAngle(Vector2D v1, Vector2D v2) {
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

	private boolean is_inside_blocks(Human human, Blockade blockade) {
		int x = human.getX();
		int y = human.getY();
		Point2D p = new Point2D(x, y);
		int apex[] = blockade.getApexes();
		Vector2D v1 = (new Point2D(apex[apex.length - 2], apex[apex.length - 1])).minus(p);
		Vector2D v2 = (new Point2D(apex[0], apex[1])).minus(p);
		double t = this.getAngle(v1, v2);

		for (int i = 0; i < apex.length - 2; i += 2) {
			v1 = (new Point2D(apex[i], apex[i + 1])).minus(p);
			v2 = (new Point2D(apex[i + 2], apex[i + 3])).minus(p);
			t += this.getAngle(v1, v2);
		}
		return Math.round(Math.abs((t / 2) / Math.PI)) >= 1;
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
			return true;// this.BuriedAgent_BlockedRoad.add((Entity)road.getEntityID());
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
						this.StuckedAgent_BlockedRoad.add(id);
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
								this.StuckedAgent_BlockedRoad.add(id);
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
						this.StuckedAgent_BlockedRoad.add(id);
					}
				}
			} else if (target.getStandardURN() == StandardEntityURN.HYDRANT) {
				// for(EntityID id
				// :this.get_all_Bloacked_Entrance_of_Building((Building)
				// target))
				this.entrance_of_Refuge_and_Hydrant.add(target.getID());
				// this.targetAreas.add(target.getID());
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
						this.blockedRoads.remove(targetID);
						this.no_need_to_clear.add(targetID);
						if (this.result != null && this.result.getValue() == targetID.getValue()) {
							if (this.agentInfo.getID().getValue() < messagePoliceForce.getAgentID().getValue()) {
								this.result = null;
								//this.result = this.get_new_result();
							}
						}
					} else if (entity.getStandardURN() == StandardEntityURN.BLOCKADE) {
						EntityID position = ((Blockade) entity).getPosition();
						this.blockedRoads.remove(targetID);
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
				this.StuckedAgent_BlockedRoad.add(target.getID());
			} else if (target.getStandardURN() == StandardEntityURN.BLOCKADE) {
				Blockade blockade = (Blockade) target;
				if (blockade.isPositionDefined()) {
					// this.priorityRoads.add(blockade.getPosition());
					this.StuckedAgent_BlockedRoad.add(blockade.getPosition());
				}
			}
		}
	}

	
	
	
}
