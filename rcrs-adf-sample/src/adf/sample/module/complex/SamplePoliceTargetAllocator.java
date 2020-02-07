package adf.sample.module.complex;

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

import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class SamplePoliceTargetAllocator extends PoliceTargetAllocator
{
	//worldinfo中警察优先要去的地区
    private Collection<EntityID> priorityAreas;		
    //worldinfo中警察需要去清理的地区
    private Collection<EntityID> targetAreas;		
	//worldinfo中所有的pf
    private Map<EntityID, PoliceForceInfo> agentInfoMap;

    public SamplePoliceTargetAllocator(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData)
    {
        super(ai, wi, si, moduleManager, developData);
        this.priorityAreas = new HashSet<>();
        this.targetAreas = new HashSet<>();
        this.agentInfoMap = new HashMap<>();
    }

    @Override
    public PoliceTargetAllocator resume(PrecomputeData precomputeData)
    {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2)
        {
            return this;
        }
        for (EntityID id : this.worldInfo.getEntityIDsOfType(StandardEntityURN.POLICE_FORCE))
        {
            this.agentInfoMap.put(id, new PoliceForceInfo(id));
        }
        for (StandardEntity e : this.worldInfo.getEntitiesOfType(REFUGE, BUILDING, GAS_STATION))
        {
            for (EntityID id : ((Building) e).getNeighbours())
            {
                StandardEntity neighbour = this.worldInfo.getEntity(id);
                if (neighbour instanceof Road)
                {
                    this.targetAreas.add(id);
                }
            }
        }
        for (StandardEntity e : this.worldInfo.getEntitiesOfType(REFUGE))
        {
            for (EntityID id : ((Building) e).getNeighbours())
            {
                StandardEntity neighbour = this.worldInfo.getEntity(id);
                if (neighbour instanceof Road)
                {
                    this.priorityAreas.add(id);
                }
            }
        }
        return this;
    }

    @Override
    public PoliceTargetAllocator preparate()
    {
        super.preparate();
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        for (EntityID id : this.worldInfo.getEntityIDsOfType(StandardEntityURN.POLICE_FORCE))
        {
            this.agentInfoMap.put(id, new PoliceForceInfo(id));
        }
        for (StandardEntity e : this.worldInfo.getEntitiesOfType(REFUGE, BUILDING, GAS_STATION))
        {
            for (EntityID id : ((Building) e).getNeighbours())
            {
                StandardEntity neighbour = this.worldInfo.getEntity(id);
                if (neighbour instanceof Road)
                {
                    this.targetAreas.add(id);
                }
            }
        }
        for (StandardEntity e : this.worldInfo.getEntitiesOfType(REFUGE))
        {
            for (EntityID id : ((Building) e).getNeighbours())
            {
                StandardEntity neighbour = this.worldInfo.getEntity(id);
                if (neighbour instanceof Road)
                {
                    this.priorityAreas.add(id);
                }
            }
        }
        return this;
    }

    @Override
    public Map<EntityID, EntityID> getResult()
    {
        return this.convert(this.agentInfoMap);
    }

    @Override
    public PoliceTargetAllocator calc()
    {
        List<StandardEntity> agents = this.getActionAgents(this.agentInfoMap);
        Collection<EntityID> removes = new ArrayList<>();
        int currentTime = this.agentInfo.getTime();
        for (EntityID target : this.priorityAreas)
        {
            if (agents.size() > 0)
            {
                StandardEntity targetEntity = this.worldInfo.getEntity(target);
                if (targetEntity != null)
                {
                    agents.sort(new DistanceSorter(this.worldInfo, targetEntity));
                    StandardEntity result = agents.get(0);
                    agents.remove(0);
                    PoliceForceInfo info = this.agentInfoMap.get(result.getID());
                    if (info != null)
                    {
                        info.canNewAction = false;
                        info.target = target;
                        info.commandTime = currentTime;
                        this.agentInfoMap.put(result.getID(), info);
                        removes.add(target);
                    }
                }
            }
        }
        this.priorityAreas.removeAll(removes);
        List<StandardEntity> areas = new ArrayList<>();
        for (EntityID target : this.targetAreas)
        {
            StandardEntity targetEntity = this.worldInfo.getEntity(target);
            if (targetEntity != null)
            {
                areas.add(targetEntity);
            }
        }
        for (StandardEntity agent : agents)
        {
            if (areas.size() > 0)
            {
                areas.sort(new DistanceSorter(this.worldInfo, agent));
                StandardEntity result = areas.get(0);
                areas.remove(0);
                this.targetAreas.remove(result.getID());
                PoliceForceInfo info = this.agentInfoMap.get(agent.getID());
                if (info != null)
                {
                    info.canNewAction = false;
                    info.target = result.getID();
                    info.commandTime = currentTime;
                    this.agentInfoMap.put(agent.getID(), info);
                }
            }
        }
        return this;
    }

    @Override
    //message是按照优先级来的,优先级高的在前
    public PoliceTargetAllocator updateInfo(MessageManager messageManager)		  
    {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2)
        {
            return this;
        }
        int currentTime = this.agentInfo.getTime();
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageRoad.class))
        {
            MessageRoad mpf = (MessageRoad) message;
            MessageUtil.reflectMessage(this.worldInfo, mpf);
        }
      //pf发送消息
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessagePoliceForce.class))	
        {
            MessagePoliceForce mpf = (MessagePoliceForce) message;
            MessageUtil.reflectMessage(this.worldInfo, mpf);
            PoliceForceInfo info = this.agentInfoMap.get(mpf.getAgentID());
            if (info == null)
            {
                info = new PoliceForceInfo(mpf.getAgentID());
            }
          //发送消息的pf没有更新
            if (currentTime >= info.commandTime + 2)	
            {
                this.agentInfoMap.put(mpf.getAgentID(), this.update(info, mpf));
            }
        }
        for (CommunicationMessage message : messageManager.getReceivedMessageList(CommandPolice.class))
        {
            CommandPolice command = (CommandPolice) message;
          //被广播去清障
            if (command.getAction() == CommandPolice.ACTION_CLEAR && command.isBroadcast())		
            {
                this.priorityAreas.add(command.getTargetID());
                this.targetAreas.add(command.getTargetID());
            }
        }
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageReport.class))
        {
            MessageReport report = (MessageReport) message;
            PoliceForceInfo info = this.agentInfoMap.get(report.getSenderID());
          //清障任务完成
            if (info != null && report.isDone())		
            {
                info.canNewAction = true;
                this.priorityAreas.remove(info.target);
                this.targetAreas.remove(info.target);
                info.target = null;
                this.agentInfoMap.put(info.agentID, info);
            }
        }
        return this;
    }
    //更新pf的信息以及还没清的路障
    private PoliceForceInfo update(PoliceForceInfo info, MessagePoliceForce message)
    {
    	//救被埋了的人
        if (message.isBuriednessDefined() && message.getBuriedness() > 0)	
        {
            info.canNewAction = false;
            if (info.target != null)
            {
                this.targetAreas.add(info.target);
                info.target = null;
            }
            return info;
        }
      //休息
        if (message.getAction() == MessagePoliceForce.ACTION_REST)		
        {
            info.canNewAction = true;
            if (info.target != null)
            {
                this.targetAreas.add(info.target);
                info.target = null;
            }
        }
      //移动
        else if (message.getAction() == MessagePoliceForce.ACTION_MOVE)		
        {
            if (message.getTargetID() != null)
            {
                StandardEntity entity = this.worldInfo.getEntity(message.getTargetID());
                if (entity != null && entity instanceof Area)
                {
                    if (info.target != null)
                    {
                        StandardEntity targetEntity = this.worldInfo.getEntity(info.target);
                        if (targetEntity != null && targetEntity instanceof Area)
                        {
                        	//接收到的target和当前target相同
                            if (message.getTargetID().getValue() == info.target.getValue())		
                            {
                                info.canNewAction = false;
                            }
                            else
                            {
                                info.canNewAction = true;
                                this.targetAreas.add(info.target);
                                info.target = null;
                            }
                        }
                        else
                        {
                            info.canNewAction = true;
                            info.target = null;
                        }
                    }
                    else
                    {
                        info.canNewAction = true;
                    }
                }
                else
                {
                    info.canNewAction = true;
                    if (info.target != null)
                    {
                        this.targetAreas.add(info.target);
                        info.target = null;
                    }
                }
            }
          //message里没有target
            else	
            {
                info.canNewAction = true;
                if (info.target != null)
                {
                    this.targetAreas.add(info.target);
                    info.target = null;
                }
            }
        }
      //清障
        else if (message.getAction() == MessagePoliceForce.ACTION_CLEAR)		
        {
            info.canNewAction = false;
        }
        return info;
    }
  //返回所有可以活动的pf
    private List<StandardEntity> getActionAgents(Map<EntityID, PoliceForceInfo> infoMap)	
    {
        List<StandardEntity> result = new ArrayList<>();
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE))
        {
            PoliceForceInfo info = infoMap.get(entity.getID());
            if (info != null && info.canNewAction && ((PoliceForce) entity).isPositionDefined())
            {
                result.add(entity);
            }
        }
        return result;
    }
  //返回所有pf的pf-target
    private Map<EntityID, EntityID> convert(Map<EntityID, PoliceForceInfo> infoMap)		
    {
        Map<EntityID, EntityID> result = new HashMap<>();
        for (EntityID id : infoMap.keySet())
        {
            PoliceForceInfo info = infoMap.get(id);
            if (info != null && info.target != null)
            {
                result.put(id, info.target);
            }
        }
        return result;
    }

    private class PoliceForceInfo
    {
        EntityID agentID;
        EntityID target;
      //是否可以进行新的action
        boolean canNewAction;	
      //消息时差
        int commandTime;		

        PoliceForceInfo(EntityID id)
        {
            agentID = id;
            target = null;
            canNewAction = true;
            commandTime = -1;
        }
    }
  //距离比较器
    private class DistanceSorter implements Comparator<StandardEntity>		
    {
        private StandardEntity reference;
        private WorldInfo worldInfo;

        DistanceSorter(WorldInfo wi, StandardEntity reference)
        {
            this.reference = reference;
            this.worldInfo = wi;
        }

        public int compare(StandardEntity a, StandardEntity b)
        {
            int d1 = this.worldInfo.getDistance(this.reference, a);
            int d2 = this.worldInfo.getDistance(this.reference, b);
            return d1 - d2;
        }
    }
}
