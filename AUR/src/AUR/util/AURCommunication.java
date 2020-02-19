package AUR.util;


import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.StandardMessage;
import adf.agent.communication.standard.bundle.information.*;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.component.communication.CommunicationMessage;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

public class AURCommunication {

    private WorldInfo worldInfo ;
    private AgentInfo agentInfo ;
    private Map<EntityID, Integer> receivedTimeMap;
    private int sendingAvoidTimeReceived;
    private List<MessageAmbulanceTeam> atMessage;
    private List<MessageFireBrigade> fbMessage;
    private List<MessagePoliceForce> pfMessage;
    private List<MessageCivilian> civilianMessage;
    private List<MessageBuilding> buildingMessage;
    private List<MessageRoad> roadMessage;


    public AURCommunication(AgentInfo ai , WorldInfo wi , ScenarioInfo scenarioInfo , DevelopData developData){
        this.agentInfo = ai ;
        this.worldInfo = wi ;
        this.receivedTimeMap = new HashMap<>();
        this.sendingAvoidTimeReceived = developData.getInteger("sample.tactics.MessageTool.sendingAvoidTimeReceived", 3);
        this.atMessage = new LinkedList<>();
        this.fbMessage = new LinkedList<>();
        this.pfMessage = new LinkedList<>();
        this.civilianMessage = new LinkedList<>();
        this.buildingMessage = new LinkedList<>();
        this.roadMessage = new LinkedList<>();
    }

    public void updateInfo(MessageManager messageManager){
        this.atMessage.clear();
        this.fbMessage.clear();
        this.pfMessage.clear();
        this.civilianMessage.clear();
        this.buildingMessage.clear();
        this.roadMessage.clear();

        this.reflectMessage(messageManager);
    }
    private  void reflectMessage(MessageManager messageManager)
    {
        Set<EntityID> changedEntities = worldInfo.getChanged().getChangedEntities();
        changedEntities.add(agentInfo.getID());
        int time = agentInfo.getTime();
        for (CommunicationMessage message : messageManager.getReceivedMessageList())
        {
            if(message instanceof StandardMessage) {
                StandardEntity entity = null;
                entity = this.reflectMessage(worldInfo, (StandardMessage) message);
                this.messageInfoUpdate((StandardMessage) message);
                if (entity != null) {
                    this.receivedTimeMap.put(entity.getID(), time);
                }
            }
        }
    }


    public static StandardEntity reflectMessage(WorldInfo worldInfo, StandardMessage message)
    {
        StandardEntity entity = null;
        Set<EntityID> changedEntities = worldInfo.getChanged().getChangedEntities();
        Class<? extends StandardMessage> messageClass = message.getClass();

        if (messageClass == MessageCivilian.class)
        {
            MessageCivilian mc = (MessageCivilian) message;
            if (!changedEntities.contains(mc.getAgentID()))
            {
                entity = MessageUtil.reflectMessage(worldInfo, mc);
            }
        }
        else if (messageClass == MessageAmbulanceTeam.class)
        {
            MessageAmbulanceTeam mat = (MessageAmbulanceTeam) message;
            if (!changedEntities.contains(mat.getAgentID()))
            {
                entity = MessageUtil.reflectMessage(worldInfo, mat);
            }
        }
        else if (messageClass == MessageFireBrigade.class)
        {
            MessageFireBrigade mfb = (MessageFireBrigade) message;
            if (!changedEntities.contains(mfb.getAgentID()))
            {
                entity = MessageUtil.reflectMessage(worldInfo, mfb);
            }
        }
        else if (messageClass ==  MessagePoliceForce.class )
        {
            MessagePoliceForce mpf = (MessagePoliceForce) message;
            if (!changedEntities.contains(mpf.getAgentID()))
            {
                entity = MessageUtil.reflectMessage(worldInfo, mpf);
            }
        }
        else if (messageClass == MessageBuilding.class)
        {
            MessageBuilding mb = (MessageBuilding) message;
            if (!changedEntities.contains(mb.getBuildingID()))
            {
                entity = MessageUtil.reflectMessage(worldInfo, mb);
            }
        }
        else if (messageClass == MessageRoad.class)
        {
            MessageRoad mr = (MessageRoad) message;
            if (!changedEntities.contains(mr.getRoadID()))
            {
                entity = MessageUtil.reflectMessage(worldInfo, mr);
            }
        }

        return entity;
    }

    public void messageInfoUpdate(StandardMessage message){

        StandardEntity entity = null;
        Set<EntityID> changedEntities = worldInfo.getChanged().getChangedEntities();
        Class<? extends StandardMessage> messageClass = message.getClass();

        if (messageClass == MessageCivilian.class)
        {
            if(!worldInfo.getEntity(message.getSenderID()).getStandardURN().equals(StandardEntityURN.AMBULANCE_TEAM)) {
                MessageCivilian mc = (MessageCivilian) message;
                if (!changedEntities.contains(mc.getAgentID())) {
                    civilianMessage.add(mc);
                }
            }
        }
        else if (messageClass == MessageAmbulanceTeam.class)
        {
            MessageAmbulanceTeam mat = (MessageAmbulanceTeam) message;
            if (!changedEntities.contains(mat.getAgentID()))
            {
                if(!agentInfo.getID().equals(mat.getAgentID())){
                    atMessage.add(mat);
                }
            }
        }
        else if (messageClass == MessageFireBrigade.class)
        {
            MessageFireBrigade mfb = (MessageFireBrigade) message;
            if (!changedEntities.contains(mfb.getAgentID()))
            {
                if(!agentInfo.getID().equals(mfb.getAgentID())){
                    fbMessage.add(mfb);
                }
            }
        }
        else if (messageClass ==  MessagePoliceForce.class )
        {
            MessagePoliceForce mpf = (MessagePoliceForce) message;
            if (!changedEntities.contains(mpf.getAgentID()))
            {
                if(!agentInfo.getID().equals(mpf.getAgentID())){
                    pfMessage.add(mpf);
                }
            }
        }
        else if (messageClass == MessageBuilding.class)
        {
            MessageBuilding mb = (MessageBuilding) message;
            if (!changedEntities.contains(mb.getBuildingID()))
            {
                buildingMessage.add(mb);
            }
        }
        else if (messageClass == MessageRoad.class)
        {
            MessageRoad mr = (MessageRoad) message;
            if (!changedEntities.contains(mr.getRoadID()))
            {
                roadMessage.add(mr);
            }
        }


    }
    public List<MessageAmbulanceTeam> getAtMessage() {
        return atMessage;
    }

    public List<MessageFireBrigade> getFbMessage() {
        return fbMessage;
    }

    public List<MessagePoliceForce> getPfMessage() {
        return pfMessage;
    }

    public List<MessageCivilian> getCivilianMessage() {
        return civilianMessage;
    }

    public List<MessageBuilding> getBuildingMessage() {
        return buildingMessage;
    }

    public List<MessageRoad> getRoadMessage() {
        return roadMessage;
    }

    private boolean isRecentlyReceived(AgentInfo agentInfo, EntityID id)
    {
        return (this.receivedTimeMap.containsKey(id)
                && ((agentInfo.getTime() - this.receivedTimeMap.get(id)) < this.sendingAvoidTimeReceived));
    }
}

