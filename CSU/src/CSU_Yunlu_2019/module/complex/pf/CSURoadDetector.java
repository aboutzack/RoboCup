package CSU_Yunlu_2019.module.complex.pf;

import CSU_Yunlu_2019.util.ambulancehelper.CSUBuilding;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.agent.communication.standard.bundle.information.*;
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

import java.util.*;
//import PF_CSUpfRoadDetector.sorter;

/**
* @Description: 具有优先级的RoadDetector
* @Author: Bochun-Yue
* @Date: 2/25/20
*/

public class CSURoadDetector extends RoadDetector {
    private Set<EntityID> targetAreas;
    private PathPlanning pathPlanning;
    private Clustering clustering;

    private EntityID result = null;
    private boolean have_police_office = false;


	int lastTime = 0;
	double lastx  =0 ;
	double lasty = 0;
    

	private Set<EntityID> entrance_of_Refuge_and_Hydrant = new HashSet<>();
	private MessageManager messageManager =null;
	private Map<EntityID, CSUBuilding> sentBuildingMap;


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
        registerModule(this.pathPlanning);
        this.have_police_office = !wi.getEntityIDsOfType(StandardEntityURN.POLICE_OFFICE).isEmpty();
        this.result = null;
        this.sentBuildingMap = new HashMap<>();
    }
    private boolean is_area_blocked(EntityID id){
		Area area = (Area) this.worldInfo.getEntity(id);
		if(area.isBlockadesDefined()&&!area.getBlockades().isEmpty())  return true;
		return false;
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
	* @Description: 清除refuge旁边的障碍（如果存在），防止火警无法及时补水
	* @Author: Bochun-Yue
	* @Date: 2/25/20
	*/

	boolean initflag = true;
	private RoadDetector GetToNearestRefuge(EntityID positionID) {
		double min = Double.MAX_VALUE;
		EntityID TargetRefugeID=null;
		//去最近的refuge
		for(StandardEntity SE : worldInfo.getEntitiesOfType(StandardEntityURN.REFUGE)) {
			double minDistance = this.getDistance(this.agentInfo.getX(), this.agentInfo.getY(),worldInfo.getLocation(SE).first() ,worldInfo.getLocation(SE).second());
			if(minDistance < min) {
				min=minDistance;
				TargetRefugeID = SE.getID();
			}
		}
		if(TargetRefugeID!=null) {
			if(positionID.getValue()==TargetRefugeID.getValue()) {
				initflag=false;
				return null;
			}else {
				return getPathTo(positionID,TargetRefugeID);
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
		//如果目标存在仍然继续
		if(this.result != null){
			return this;
		}	
		//先看StuckedAgentOrRefuge_BlockedRoad,优先级最高
		if(!this.StuckedAgentOrRefuge_BlockedRoad.isEmpty()) {
				return this.getRoadDetector(positionID, this.StuckedAgentOrRefuge_BlockedRoad);
		}	
		//然后去refuge
		if(initflag) this.GetToNearestRefuge(positionID);
		//最后看priorityRoads，优先级其次
		if(!this.priorityRoads.isEmpty()){
			return getRoadDetector(positionID,this.priorityRoads);
		}
			
		if (this.result == null)	
        { 	
			//去最找最近的火警
            double min=Double.MAX_VALUE;
            StandardEntity target = null;
            for(StandardEntity SE:worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE)) {
            	double minDistance = this.getDistance(this.agentInfo.getX(), this.agentInfo.getY(),worldInfo.getLocation(SE).first() ,worldInfo.getLocation(SE).second());
            	if(minDistance > min) {
            		min = minDistance;
            		target = SE;
            	}
            }
            if( target != null) {	
            	return getPathTo(positionID,target.getID());
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
    {   this.messageManager=messageManager;
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
        preProcessChangedEntities(messageManager);
		// TODO: 2/28/20 调用getReceivedMessageList,并根据message作相应的处理
        return this;
    }

	/**
	 * @Description: 根据changedEntities的信息进行通讯等操作
	 * @Date: 2/28/20
	 */
	private void preProcessChangedEntities(MessageManager messageManager) {
		worldInfo.getChanged().getChangedEntities().forEach(changedId -> {
			StandardEntity entity = worldInfo.getEntity(changedId);
			if (entity instanceof Building) {
				Building building = (Building) worldInfo.getEntity(changedId);
				if (building.isFierynessDefined() && building.getFieryness() > 0) {
					CSUBuilding csuBuilding = sentBuildingMap.get(changedId);
					if (csuBuilding == null || csuBuilding.getFireyness() != building.getFieryness() || csuBuilding.getFireyness() == 1) {
						messageManager.addMessage(new MessageBuilding(true, building));
						messageManager.addMessage(new MessageBuilding(false, building));
						sentBuildingMap.put(changedId, new CSUBuilding(building));
					}
				}
			} else if (entity instanceof Civilian) {
				Civilian civilian = (Civilian) entity;
				if ((civilian.isHPDefined() && civilian.getHP() > 1000 && civilian.isDamageDefined() && civilian.getDamage() > 0)
						|| ((civilian.isPositionDefined() && !(worldInfo.getEntity(civilian.getPosition()) instanceof Refuge))
						&& (worldInfo.getEntity(civilian.getPosition()) instanceof Building))) {
					messageManager.addMessage(new MessageCivilian(true, civilian));
					messageManager.addMessage(new MessageCivilian(false, civilian));
					// TODO: 2/28/20 判断Civilian是否需要帮忙清障,并选择是否帮忙清障
				}
			}
		});
	}

    private Set<EntityID> StuckedAgentOrRefuge_BlockedRoad = new HashSet<>();
    private Set<EntityID> priorityRoads = new HashSet<>();
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
				if(road.isBlockadesDefined()&&!road.getBlockades().isEmpty())
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
			if (entity != null  &&  !this.no_need_to_clear.contains(entity.getID()) && entity instanceof Hydrant)
				this.priorityRoads.add(entity.getID());
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
					if (this.is_agent_stucked(human, (Road) positionEntity)||this.is_agent_Buried(human, (Road) positionEntity)||getDistance(human,(Road)positionEntity)<1000) {
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
					if (this.is_agent_stucked(human, (Road) positionEntity)||this.is_agent_Buried(human, (Road) positionEntity)||getDistance(human,(Road)positionEntity)<1000) {
						this.StuckedAgentOrRefuge_BlockedRoad.add(position);
					}
				} else if (!(positionEntity instanceof Refuge) && positionEntity instanceof Building) {
					Set<EntityID> entrances = this.get_all_Bloacked_Entrance_of_Building((Building) positionEntity);
					this.blockedRoads.addAll(entrances);
				}
			}
			//road 
			else if (entity instanceof Road) {
				Road road = (Road) entity;
					if (road.isBlockadesDefined() && road.getBlockades().isEmpty()) {
						this.StuckedAgentOrRefuge_BlockedRoad.remove(id);
						this.priorityRoads.remove(id);
						continue;
					}
					boolean BuildFlag=true;
					for(EntityID eid : road.getNeighbours()) {
						StandardEntity ent = (StandardEntity)this.worldInfo.getEntity(eid);
						if(ent instanceof Building) {
							this.blockedRoads.add(id);
							BuildFlag=false;
							break;
						}
					}
					if(BuildFlag) this.priorityRoads.add(id);
			}
			
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
