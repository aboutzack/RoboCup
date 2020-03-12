package CSU_Yunlu_2019.module.complex.center;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.agent.communication.standard.bundle.centralized.MessageReport;
import adf.agent.communication.standard.bundle.information.MessagePoliceForce;
import adf.agent.communication.standard.bundle.information.MessageRoad;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.complex.PoliceTargetAllocator;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import java.util.Random;

import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;


public class TestPoliceTargetAllocator extends PoliceTargetAllocator {
	 private Collection<EntityID> priorityAreas;
	    private Collection<EntityID> targetAreas;

	    private Set<EntityID> entrance;
	    private Set<EntityID> cross;
	    private Set<EntityID> entranceOfRefuge;
	  //  private Set<EntityID> clearedAreas;
        private Map<EntityID, PoliceForceInfo> clearedAreas;
	    private Map<EntityID, PoliceForceInfo> agentInfoMap;
	    
    public TestPoliceTargetAllocator(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.priorityAreas = new HashSet<>();
        this.targetAreas = new HashSet<>();
        this.agentInfoMap = new HashMap<>();
        this.entrance = new HashSet<>();
        this.cross = new HashSet<>();
        this.entranceOfRefuge = new HashSet<>();
        this.clearedAreas = new HashMap<>();
    }

    @Override
    public PoliceTargetAllocator resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if(this.getCountResume() >= 2) {
            return this;
        }
        for(EntityID id : this.worldInfo.getEntityIDsOfType(StandardEntityURN.POLICE_FORCE)) {
            this.agentInfoMap.put(id, new PoliceForceInfo(id));
        }

        //entrance
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.BUILDING)) {
            for (EntityID id : ((Building)entity).getNeighbours()) {
                if (this.worldInfo.getEntity(id) instanceof Road) {
                    this.entrance.add(id);
                }
            }
        }
        //cross
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
            int count = 0;
            for (EntityID id : ((Road)entity).getNeighbours()) {
                if (this.worldInfo.getEntity(id) instanceof Road && !this.entrance.contains(id)) {
                    ++count;
                }
            }
            if (count > 2) {
                this.cross.add(entity.getID());
            }
        }
        List<EntityID> removeList = new ArrayList<>();
        for (EntityID id : this.cross) {
            int count = 0;
            Road road = (Road)this.worldInfo.getEntity(id);
            for (EntityID neighbor : road.getNeighbours()) {
                if (this.cross.contains(neighbor)) {
                    ++count;
                }
            }
            if (count == this.getPassableEdge(road).size()) {
                removeList.add(id);
            }
        }
        this.cross.removeAll(removeList);
        removeList.clear();
        //entranceOfRefuge
        for (EntityID id : this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE)) {
            this.entranceOfRefuge.addAll(this.getAllEntrancesOfBuilding((Building)this.worldInfo.getEntity(id)));
        }
        //another type of cross, all of their neighbors are building, in fact, they are always shortcuts.
        for (EntityID id : this.entrance) {
            boolean flag = false;
            for (EntityID neighbor : ((Area)this.worldInfo.getEntity(id)).getNeighbours()) {
                if (this.worldInfo.getEntity(neighbor) instanceof Road) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                this.targetAreas.add(id);
            }
        }
        //dead ends
        Queue<EntityID> clearedQueue = new ArrayDeque<>();
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
            if (((Area)entity).getNeighbours().size() <= 1) {
                clearedQueue.offer(entity.getID());
            }
        }
        while (!clearedQueue.isEmpty()) {
            EntityID clearedID = clearedQueue.poll();
            PoliceForceInfo info = new PoliceForceInfo(this.agentInfo.getID());
            info.commandTime = -10;
            this.clearedAreas.put(clearedID, info);
            for (EntityID id : ((Area)this.worldInfo.getEntity(clearedID)).getNeighbours()) {
                if (clearedQueue.contains(id) || this.clearedAreas.containsKey(id)) {
                    continue;
                }
                Area area = (Area)this.worldInfo.getEntity(id);
                int count = 0;
                for (EntityID neighbor : area.getNeighbours()) {
                    if (this.clearedAreas.containsKey(neighbor) || clearedQueue.contains(neighbor)) {
                        ++count;
                    }
                }
                if (area.getNeighbours().size() - count <= 1) {
                    clearedQueue.offer(id);
                }
            }
        }
        //targetAreas | priorityAreas
        this.targetAreas.addAll(this.cross);
        this.priorityAreas.addAll(this.entranceOfRefuge);

        return this;
    }

    @Override
    public PoliceTargetAllocator preparate() {
        super.preparate();
        if(this.getCountPrecompute() >= 2) {
            return this;
        }
        for(EntityID id : this.worldInfo.getEntityIDsOfType(StandardEntityURN.POLICE_FORCE)) {
            this.agentInfoMap.put(id, new PoliceForceInfo(id));
        }

        //entrance
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.BUILDING)) {
            for (EntityID id : ((Building)entity).getNeighbours()) {
                if (this.worldInfo.getEntity(id) instanceof Road) {
                    this.entrance.add(id);
                }
            }
        }
        //cross
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
            int count = 0;
            for (EntityID id : ((Road)entity).getNeighbours()) {
                if (this.worldInfo.getEntity(id) instanceof Road && !this.entrance.contains(id)) {
                    ++count;
                }
            }
            if (count > 2) {
                this.cross.add(entity.getID());
            }
        }
        List<EntityID> removeList = new ArrayList<>();
        for (EntityID id : this.cross) {
            int count = 0;
            Road road = (Road)this.worldInfo.getEntity(id);
            for (EntityID neighbor : road.getNeighbours()) {
                if (this.cross.contains(neighbor)) {
                    ++count;
                }
            }
            if (count == this.getPassableEdge(road).size()) {
                removeList.add(id);
            }
        }
        this.cross.removeAll(removeList);
        removeList.clear();
        //entranceOfRefuge
        for (EntityID id : this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE)) {
            this.entranceOfRefuge.addAll( this.getAllEntrancesOfBuilding((Building)this.worldInfo.getEntity(id)));
        }
        //another type of cross, all of their neighbors are building, in fact, they are always shortcuts.
        for (EntityID id : this.entrance) {
            boolean flag = false;
            for (EntityID neighbor : ((Area)this.worldInfo.getEntity(id)).getNeighbours()) {
                if (this.worldInfo.getEntity(neighbor) instanceof Road) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                this.targetAreas.add(id);
            }
        }
        //dead ends
        Queue<EntityID> clearedQueue = new ArrayDeque<>();
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.ROAD)) {
            if (((Area)entity).getNeighbours().size() <= 1) {
                clearedQueue.offer(entity.getID());
            }
        }
        while (!clearedQueue.isEmpty()) {
            EntityID clearedID = clearedQueue.poll();
            PoliceForceInfo info = new PoliceForceInfo(this.agentInfo.getID());
            info.commandTime = -10;
            this.clearedAreas.put(clearedID, info);
            for (EntityID id : ((Area)this.worldInfo.getEntity(clearedID)).getNeighbours()) {
                if (clearedQueue.contains(id) || this.clearedAreas.containsKey(id)) {
                    continue;
                }
                Area area = (Area)this.worldInfo.getEntity(id);
                int count = 0;
                for (EntityID neighbor : area.getNeighbours()) {
                    if (this.clearedAreas.containsKey(neighbor) || clearedQueue.contains(neighbor)) {
                        ++count;
                    }
                }
                if (area.getNeighbours().size() - count <= 1) {
                    clearedQueue.offer(id);
                }
            }
        }
        //targetAreas | priorityAreas
        this.targetAreas.addAll(this.cross);
        this.priorityAreas.addAll(this.entranceOfRefuge);

        return this;
    }

    @Override
    public Map<EntityID, EntityID> getResult() {
    	return this.convert(this.agentInfoMap);
    }

    @Override
    public PoliceTargetAllocator calc() {
        List<StandardEntity> agents = this.getActionAgents(this.agentInfoMap);
        List<StandardEntity> removeAgentList = new ArrayList<>();//allocate priority task
        if (!this.priorityAreas.isEmpty() && !agents.isEmpty()) {
            for (StandardEntity agent : agents) {
                EntityID target = this.getClosestEntityID(this.priorityAreas, agent.getID());
                PoliceForceInfo info = this.agentInfoMap.get(agent.getID());
                if (info == null) {
                    info = new PoliceForceInfo(agent.getID());
                }
                info.canNewAction = false;
                info.target = target;
                info.commandTime = this.agentInfo.getTime();
                this.agentInfoMap.put(agent.getID(), info);
               // System.out.print(agent.getID());
               // System.out.println(" work piro");
                removeAgentList.add(agent);
                this.priorityAreas.remove(target);
               this.clearedAreas.put(target,info);
              /*  if(this.clearedAreas.containsKey(target)) {
               		System.out.print(agent.getID());
               		System.out.println("priocopyaa");
               	 PoliceForceInfo infot=clearedAreas.get(target);
               	if((info.commandTime-infot.commandTime)>3) {
               		this.priorityAreas.add(target);
               		System.out.print(agent.getID());
               		System.out.println("priocopy");
               	}
               	else
               		this.clearedAreas.put(target, infot);
               }
               else 
               	this.clearedAreas.put(target,info);*/
                this.targetAreas.remove(target);
                
            }
        }
        agents.removeAll(removeAgentList);
        removeAgentList.clear();

        if (!this.targetAreas.isEmpty() && !agents.isEmpty()) {//allocate task in targetareas
            for (StandardEntity agent : agents) {
                EntityID target = this.getClosestEntityID(this.targetAreas, agent.getID());
                PoliceForceInfo info = this.agentInfoMap.get(agent.getID());
                if (info == null) {
                    info = new PoliceForceInfo(agent.getID());
                }
                info.canNewAction = false;
               info.target = target;
                info.commandTime = this.agentInfo.getTime();
                this.agentInfoMap.put(agent.getID(), info);
             //   System.out.print(agent.getID());
            //    System.out.println(" work tar");
                removeAgentList.add(agent);
                this.targetAreas.remove(target);
               this.clearedAreas.put(target,info);
               /* if(this.clearedAreas.containsKey(target)) {
                         System.out.print(agent.getID());
                         System.out.println("tarcopyaa");
                	 PoliceForceInfo infot=clearedAreas.get(target);
                	if((info.commandTime-infot.commandTime)>3) {
                		this.priorityAreas.add(target);
                		System.out.print(agent.getID());
                		System.out.println("tarcopy");
                	}
                	else
                		this.clearedAreas.put(target, infot);
                }
                else 
                	this.clearedAreas.put(target,info);*/
            }
        }
        agents.removeAll(removeAgentList);
        removeAgentList.clear();
        return this;
    }

    @Override
    public PoliceTargetAllocator updateInfo(MessageManager messageManager) {
    //	System.out.println("abc");
        super.updateInfo(messageManager);
        if(this.getCountUpdateInfo() >= 2) {
            return this;
        }
        int currentTime = this.agentInfo.getTime();
        for(CommunicationMessage message : messageManager.getReceivedMessageList(MessageRoad.class)) {
            MessageRoad mpf = (MessageRoad) message;
            MessageUtil.reflectMessage(this.worldInfo, mpf);
            if (mpf.isPassable()) {
                EntityID id = mpf.getRoadID();
                this.priorityAreas.remove(id);
                PoliceForceInfo info = new PoliceForceInfo(this.agentInfo.getID());
                info.commandTime = this.agentInfo.getTime();
                this.clearedAreas.put(id, info);
                this.targetAreas.remove(id);
            }
        }
        for(CommunicationMessage message : messageManager.getReceivedMessageList(MessagePoliceForce.class)) {
            MessagePoliceForce mpf = (MessagePoliceForce) message;
            MessageUtil.reflectMessage(this.worldInfo, mpf);
            PoliceForceInfo info = this.agentInfoMap.get(mpf.getAgentID());
            if(info == null) {
                info = new PoliceForceInfo(mpf.getAgentID());
            }
            if(currentTime >= info.commandTime + 2) {
                this.agentInfoMap.put(mpf.getAgentID(), this.update(info, mpf));
            }
        }
        for(CommunicationMessage message : messageManager.getReceivedMessageList(CommandPolice.class)) {
            CommandPolice command = (CommandPolice)message;
            if(command.getAction() == CommandPolice.ACTION_CLEAR ) {
            	if(this.worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_OFFICE).contains(command.getSenderID())) {
            		System.out.print(command.getTargetID());
            		System.out.println(" officetopolice");
            	  //  this.priorityAreas.add(command.getTargetID());
                  //  this.targetAreas.add(command.getTargetID());
            	}
            	else {
            		System.out.print(command.getTargetID());
            		System.out.println(" policetooffice");
            		if(this.clearedAreas.containsKey(command.getTargetID())){
            			System.out.print(command.getTargetID());
            			System.out.println(" alreadycopy");
            			PoliceForceInfo infot=clearedAreas.get(command.getTargetID());
            			if((currentTime-infot.commandTime)>9) {
            				this.priorityAreas.add(command.getTargetID());
            				System.out.print(command.getTargetID());
            				System.out.println(" copy");
            			}
            		}
            		else {
            			System.out.print(command.getTargetID());
            			System.out.println(" normal");
            			this.priorityAreas.add(command.getTargetID());
            			this.targetAreas.add(command.getTargetID());
            		}

            	}
            //    System.out.println("GetMessage!!!!!!!!!!!!!!!");
            }
        }

        Set<EntityID> senderIDs = new HashSet<>();
        for(CommunicationMessage message : messageManager.getReceivedMessageList(MessageReport.class)) {
            MessageReport report = (MessageReport) message;
            EntityID id = report.getSenderID();
            if (this.worldInfo.getEntity(id) instanceof PoliceForce) {
                if (report.isDone() && senderIDs.contains(id)) {
                    continue;
                }
                senderIDs.add(id);
                PoliceForceInfo info = this.agentInfoMap.get(id);
                if (info != null) {
                    info = this.update(info, report);
                } else {
                    info = this.update(new PoliceForceInfo(id), report);
                }
                this.agentInfoMap.put(id, info);
            }
        }
        return this;
    }
    private PoliceForceInfo update(PoliceForceInfo info, MessageReport report) {
        info.commandTime = this.agentInfo.getTime();
    	if(report.isDone()) {
    		info.canNewAction=true;
    		this.priorityAreas.remove(info.target);
            this.targetAreas.remove(info.target);
            info.target = null;
            this.agentInfoMap.put(info.agentID, info);
        //    System.out.print(info.agentID);
        //    System.out.println(" repcan");
    	} else {
    		info.canNewAction=false;
    		info.target = null;
    	//	System.out.print(info.agentID);
        //    System.out.println(" repcannot");
    	}
    	return info;
    }
    
    private PoliceForceInfo update(PoliceForceInfo info, MessagePoliceForce message) {
        info.commandTime = this.agentInfo.getTime();
        if(message.isBuriednessDefined() && message.getBuriedness() > 0) {
            info.canNewAction = false;
            if (info.target != null) {
                this.targetAreas.add(info.target);
                info.target = null;
            }
            return info;
        }
        if (message.getAction() == MessagePoliceForce.ACTION_CLEAR) {
            if (message.isTargetDefined()) {
                this.targetAreas.remove(message.getTargetID());
                this.priorityAreas.remove(message.getTargetID());
                this.clearedAreas.put(message.getTargetID(),info);
            }
            if (message.isPositionDefined()) {
                this.targetAreas.remove(message.getPosition());
                this.priorityAreas.remove(message.getPosition());
                this.clearedAreas.put(message.getPosition(),info);
            }
        }
        return info;
    }
    
    private List<StandardEntity> getActionAgents(Map<EntityID, PoliceForceInfo> infoMap) {
        List<StandardEntity> result = new ArrayList<>();
        for(StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)) {
            PoliceForceInfo info = infoMap.get(entity.getID());
            if(info != null && info.canNewAction && ((PoliceForce)entity).isPositionDefined()) {
                result.add(entity);
            }
        }
        return result;
    }
   

    
 /*   private List<StandardEntity> getAllAgents(Map<EntityID, PoliceForceInfo> infoMap) {
        List<StandardEntity> result = new ArrayList<>();
        for(StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)) {
            PoliceForceInfo info = infoMap.get(entity.getID());
            if(info != null && ((PoliceForce)entity).isPositionDefined()) {
                result.add(entity);
            }
        }
        return result;
    }*/

    private Map<EntityID, EntityID> convert(Map<EntityID, PoliceForceInfo> infoMap) {
        Map<EntityID, EntityID> result = new HashMap<>();
        for(EntityID id : infoMap.keySet()) {
            PoliceForceInfo info = infoMap.get(id);
            if(info != null && info.target != null) {
                result.put(id, info.target);
            }
        }
        return result;
    }

    private Set<EntityID> getAllEntrancesOfBuilding(Building building) {
        EntityID buildingID = building.getID();
        Set<EntityID> entrances = new HashSet<>();
        Set<EntityID> visited = new HashSet<>();
        Stack<EntityID> stack = new Stack<>();
        stack.push(buildingID);
        visited.add(buildingID);
        while (!stack.isEmpty()) {
            EntityID id = stack.pop();
            visited.add(id);
            Area area = (Area)this.worldInfo.getEntity(id);
            if (area instanceof Road) {
                entrances.add(id);
            } else {
                for (EntityID neighbor : area.getNeighbours()) {
                    if (visited.contains(neighbor)) {
                        continue;
                    }
                    stack.push(neighbor);
                }
            }
        }
        return entrances;
    }

    private Set<Edge> getPassableEdge(Area area) {
        Set<Edge> passableEdges = new HashSet<>();
        for (Edge edge : area.getEdges()) {
            if (edge.isPassable()) {
                passableEdges.add(edge);
            }
        }
        return passableEdges;
    }

    private class PoliceForceInfo {
        EntityID agentID;
        EntityID target;
        boolean canNewAction;
        int commandTime;

        PoliceForceInfo(EntityID id) {
            agentID = id;
            target = null;
            canNewAction = false;
            commandTime = -1;
        }
    }

    private class DistanceSorter implements Comparator<StandardEntity> {
        private StandardEntity reference;
        private WorldInfo worldInfo;

        DistanceSorter(WorldInfo wi, StandardEntity reference) {
            this.reference = reference;
            this.worldInfo = wi;
        }

        public int compare(StandardEntity a, StandardEntity b) {
            int d1 = this.worldInfo.getDistance(this.reference, a);
            int d2 = this.worldInfo.getDistance(this.reference, b);
            return d1 - d2;
        }
    }

    private EntityID getClosestEntityID(Collection<EntityID> IDs, EntityID reference) {
        if (IDs.isEmpty()) {
            return null;
        }
        double minDistance = Double.MAX_VALUE;
        EntityID closestID = null;
        for (EntityID id : IDs) {
            double distance = this.worldInfo.getDistance(id, reference);
            if (distance < minDistance) {
                minDistance = distance;
                closestID = id;
            }
        }
        return closestID;
    }
}
