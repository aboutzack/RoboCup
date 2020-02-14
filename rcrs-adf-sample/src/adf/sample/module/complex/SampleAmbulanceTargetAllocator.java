package adf.sample.module.complex;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.centralized.CommandAmbulance;
import adf.agent.communication.standard.bundle.centralized.MessageReport;
import adf.agent.communication.standard.bundle.information.MessageAmbulanceTeam;
import adf.agent.communication.standard.bundle.information.MessageCivilian;
import adf.agent.communication.standard.bundle.information.MessageFireBrigade;
import adf.agent.communication.standard.bundle.information.MessagePoliceForce;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.complex.AmbulanceTargetAllocator;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;

//返回所有可活动at和at分配的目标
public class SampleAmbulanceTargetAllocator extends AmbulanceTargetAllocator
{
    private Collection<EntityID> priorityHumans;
    //worldInfo中所有at发现的所有需要救援的humans
    private Collection<EntityID> targetHumans;

    //worldInfo中的所有at-atInfo
    private Map<EntityID, AmbulanceTeamInfo> ambulanceTeamInfoMap;

    public SampleAmbulanceTargetAllocator(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData)
    {
        super(ai, wi, si, moduleManager, developData);
        this.priorityHumans = new HashSet<>();
        this.targetHumans = new HashSet<>();
        this.ambulanceTeamInfoMap = new HashMap<>();
    }

    @Override
    public AmbulanceTargetAllocator resume(PrecomputeData precomputeData)
    {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2)
        {
            return this;
        }
        for (EntityID id : this.worldInfo.getEntityIDsOfType(StandardEntityURN.AMBULANCE_TEAM))
        {
            this.ambulanceTeamInfoMap.put(id, new AmbulanceTeamInfo(id));
        }
        return this;
    }

    @Override
    public AmbulanceTargetAllocator preparate()
    {
        super.preparate();
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        for (EntityID id : this.worldInfo.getEntityIDsOfType(StandardEntityURN.AMBULANCE_TEAM))
        {
            this.ambulanceTeamInfoMap.put(id, new AmbulanceTeamInfo(id));
        }
        return this;
    }

    @Override
    public Map<EntityID, EntityID> getResult()
    {
        return this.convert(this.ambulanceTeamInfoMap);
    }

    @Override
    public AmbulanceTargetAllocator calc()
    {
        List<StandardEntity> agents = this.getActionAgents(this.ambulanceTeamInfoMap);
        Collection<EntityID> removes = new ArrayList<>();
        int currentTime = this.agentInfo.getTime();
        for (EntityID target : this.priorityHumans)
        {
            if (agents.size() > 0)
            {
                StandardEntity targetEntity = this.worldInfo.getEntity(target);
                if (targetEntity != null && targetEntity instanceof Human && ((Human) targetEntity).isPositionDefined())
                {
                    agents.sort(new DistanceSorter(this.worldInfo, targetEntity));
                    StandardEntity result = agents.get(0);
                    agents.remove(0);
                    AmbulanceTeamInfo info = this.ambulanceTeamInfoMap.get(result.getID());
                    if (info != null)
                    {
                        info.canNewAction = false;
                        info.target = target;
                        info.commandTime = currentTime;
                        this.ambulanceTeamInfoMap.put(result.getID(), info);
                        removes.add(target);
                    }
                }
            }
        }
        this.priorityHumans.removeAll(removes);
        removes.clear();
        for (EntityID target : this.targetHumans)
        {
            if (agents.size() > 0)
            {
                StandardEntity targetEntity = this.worldInfo.getEntity(target);
                if (targetEntity != null && targetEntity instanceof Human && ((Human) targetEntity).isPositionDefined())
                {
                    agents.sort(new DistanceSorter(this.worldInfo, targetEntity));
                    StandardEntity result = agents.get(0);
                    agents.remove(0);
                    AmbulanceTeamInfo info = this.ambulanceTeamInfoMap.get(result.getID());
                    if (info != null)
                    {
                        info.canNewAction = false;
                        info.target = target;
                        info.commandTime = currentTime;
                        this.ambulanceTeamInfoMap.put(result.getID(), info);
                        removes.add(target);
                    }
                }
            }
        }
        this.targetHumans.removeAll(removes);
        return this;
    }

    //message是按照优先级来的,优先级高的在前
    @Override
    public AmbulanceTargetAllocator updateInfo(MessageManager messageManager)
    {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2)
        {
            return this;
        }
        int currentTime = this.agentInfo.getTime();
        for (CommunicationMessage message : messageManager.getReceivedMessageList())
        {
            Class<? extends CommunicationMessage> messageClass = message.getClass();
            //市民的消息
            if (messageClass == MessageCivilian.class)
            {
                MessageCivilian mc = (MessageCivilian) message;
                MessageUtil.reflectMessage(this.worldInfo, mc);
                //被埋了
                if (mc.isBuriednessDefined() && mc.getBuriedness() > 0)
                {
                    this.targetHumans.add(mc.getAgentID());
                }
                //没被埋
                else
                {
                    this.priorityHumans.remove(mc.getAgentID());
                    this.targetHumans.remove(mc.getAgentID());
                }
            }
            else if (messageClass == MessageFireBrigade.class)
            {
                MessageFireBrigade mfb = (MessageFireBrigade) message;
                MessageUtil.reflectMessage(this.worldInfo, mfb);
                if (mfb.isBuriednessDefined() && mfb.getBuriedness() > 0)
                {
                    this.priorityHumans.add(mfb.getAgentID());
                }
                else
                {
                    this.priorityHumans.remove(mfb.getAgentID());
                    this.targetHumans.remove(mfb.getAgentID());
                }
            }
            else if (messageClass == MessagePoliceForce.class)
            {
                MessagePoliceForce mpf = (MessagePoliceForce) message;
                MessageUtil.reflectMessage(this.worldInfo, mpf);
                if (mpf.isBuriednessDefined() && mpf.getBuriedness() > 0)
                {
                    this.priorityHumans.add(mpf.getAgentID());
                }
                else
                {
                    this.priorityHumans.remove(mpf.getAgentID());
                    this.targetHumans.remove(mpf.getAgentID());
                }
            }
        }
        //at发送的消息
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageAmbulanceTeam.class))
        {
            MessageAmbulanceTeam mat = (MessageAmbulanceTeam) message;
            MessageUtil.reflectMessage(this.worldInfo, mat);
            if (mat.isBuriednessDefined() && mat.getBuriedness() > 0)
            {
                this.priorityHumans.add(mat.getAgentID());
            }
            else
            {
                this.priorityHumans.remove(mat.getAgentID());
                this.targetHumans.remove(mat.getAgentID());
            }
            AmbulanceTeamInfo info = this.ambulanceTeamInfoMap.get(mat.getAgentID());
            if (info == null)
            {
                info = new AmbulanceTeamInfo(mat.getAgentID());
            }
            //发送消息的at没有更新
            if (currentTime >= info.commandTime + 2)
            {
                this.ambulanceTeamInfoMap.put(mat.getAgentID(), this.update(info, mat));
            }
        }
        for (CommunicationMessage message : messageManager.getReceivedMessageList(CommandAmbulance.class))
        {
            CommandAmbulance command = (CommandAmbulance) message;
            //广播挖人
            if (command.getAction() == CommandAmbulance.ACTION_RESCUE && command.isBroadcast())
            {
                this.priorityHumans.add(command.getTargetID());
                this.targetHumans.add(command.getTargetID());
            }
            //广播救人
            else if (command.getAction() == CommandAmbulance.ACTION_LOAD && command.isBroadcast())
            {
                this.priorityHumans.add(command.getTargetID());
                this.targetHumans.add(command.getTargetID());
            }
        }
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageReport.class))
        {
            MessageReport report = (MessageReport) message;
            AmbulanceTeamInfo info = this.ambulanceTeamInfoMap.get(report.getSenderID());
            //报告自己救完认了
            if (info != null && report.isDone())
            {
                info.canNewAction = true;
                this.priorityHumans.remove(info.target);
                this.targetHumans.remove(info.target);
                info.target = null;
                this.ambulanceTeamInfoMap.put(info.agentID, info);
            }
        }
        return this;
    }

    //返回所有at的at-target
    private Map<EntityID, EntityID> convert(Map<EntityID, AmbulanceTeamInfo> map)
    {
        //key:at value:target
        Map<EntityID, EntityID> result = new HashMap<>();
        for (EntityID id : map.keySet())
        {
            AmbulanceTeamInfo info = map.get(id);
            if (info != null && info.target != null)
            {
                result.put(id, info.target);
            }
        }
        return result;
    }

    //返回所有可以活动的at
    private List<StandardEntity> getActionAgents(Map<EntityID, AmbulanceTeamInfo> map)
    {
        List<StandardEntity> result = new ArrayList<>();
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM))
        {
            AmbulanceTeamInfo info = map.get(entity.getID());
            if (info != null && info.canNewAction && ((AmbulanceTeam) entity).isPositionDefined())
            {
                result.add(entity);
            }
        }
        return result;
    }

    //更新所有at的信息和等待救援的human
    private AmbulanceTeamInfo update(AmbulanceTeamInfo info, MessageAmbulanceTeam message)
    {
        //被埋了
        if (message.isBuriednessDefined() && message.getBuriedness() > 0)
        {
            info.canNewAction = false;
            if (info.target != null)
            {
                this.targetHumans.add(info.target);
                info.target = null;
            }
            return info;
        }
        if (message.getAction() == MessageAmbulanceTeam.ACTION_REST)
        {
            info.canNewAction = true;
            if (info.target != null)
            {
                this.targetHumans.add(info.target);
                info.target = null;
            }
        }
        else if (message.getAction() == MessageAmbulanceTeam.ACTION_MOVE)
        {
            if (message.getTargetID() != null)
            {
                //发送者
                StandardEntity entity = this.worldInfo.getEntity(message.getTargetID());
                if (entity != null)
                {
                    if (entity instanceof Area)
                    {
                        if (entity.getStandardURN() == REFUGE)
                        {
                            info.canNewAction = false;
                            return info;
                        }
                        StandardEntity targetEntity = this.worldInfo.getEntity(info.target);
                        if (targetEntity != null)
                        {
                            if (targetEntity instanceof Human)
                            {
                                targetEntity = this.worldInfo.getPosition((Human) targetEntity);
                                if (targetEntity == null)
                                {
                                    this.priorityHumans.remove(info.target);
                                    this.targetHumans.remove(info.target);
                                    info.canNewAction = true;
                                    info.target = null;
                                    return info;
                                }
                            }
                            //接收到的target和当前target相同
                            if (targetEntity.getID().getValue() == entity.getID().getValue())
                            {
                                info.canNewAction = false;
                            }
                            else
                            {
                                info.canNewAction = true;
                                if (info.target != null)
                                {
                                    this.targetHumans.add(info.target);
                                    info.target = null;
                                }
                            }
                        }
                        else
                        {
                            info.canNewAction = true;
                            info.target = null;
                        }
                        return info;
                    }
                    else if (entity instanceof Human)
                    {
                        if (entity.getID().getValue() == info.target.getValue())
                        {
                            info.canNewAction = false;
                        }
                        else
                        {
                            info.canNewAction = true;
                            this.targetHumans.add(info.target);
                            this.targetHumans.add(entity.getID());
                            info.target = null;
                        }
                        return info;
                    }
                }
            }
            //message里没有target
            info.canNewAction = true;
            if (info.target != null)
            {
                this.targetHumans.add(info.target);
                info.target = null;
            }
        }
        else if (message.getAction() == MessageAmbulanceTeam.ACTION_RESCUE)
        {
            info.canNewAction = true;
            if (info.target != null)
            {
                this.targetHumans.add(info.target);
                info.target = null;
            }
        }
        else if (message.getAction() == MessageAmbulanceTeam.ACTION_LOAD)
        {
            info.canNewAction = false;
        }
        else if (message.getAction() == MessageAmbulanceTeam.ACTION_UNLOAD)
        {
            info.canNewAction = true;
            this.priorityHumans.remove(info.target);
            this.targetHumans.remove(info.target);
            info.target = null;
        }
        return info;
    }


    private class AmbulanceTeamInfo
    {
        EntityID agentID;
        EntityID target;
        //是否可以更换当前action
        boolean canNewAction;
        int commandTime;

        AmbulanceTeamInfo(EntityID id)
        {
            agentID = id;
            target = null;
            canNewAction = true;
            commandTime = -1;
        }
    }

    //距离比较器(排序用)
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
