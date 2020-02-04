package adf.sample.centralized;

import adf.agent.communication.standard.bundle.centralized.CommandAmbulance;
import adf.agent.communication.standard.bundle.centralized.CommandScout;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.centralized.CommandPicker;
import adf.component.communication.CommunicationMessage;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

//从allocationdata获取信息并make命令保存在message中
public class CommandPickerAmbulance extends CommandPicker {
    //侦查距离
    private int scoutDistance;
    //存储所有即将发布的命令
    private Collection<CommunicationMessage> messages;
    //agent-target
    private Map<EntityID, EntityID> allocationData;

    public CommandPickerAmbulance(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.messages = new ArrayList<>();
        this.allocationData = null;
        this.scoutDistance = developData.getInteger("CommandPickerAmbulance.scoutDistance", 40000);
    }

    @Override
    public CommandPicker setAllocatorResult(Map<EntityID, EntityID> allocationData) {
        this.allocationData = allocationData;
        return this;
    }

    @Override
    public CommandPicker calc() {
        this.messages.clear();
        if(this.allocationData == null) {
            return this;
        }
        for(EntityID agentID : this.allocationData.keySet()) {
            //从entityid获取agent
            StandardEntity agent = this.worldInfo.getEntity(agentID);
            //如果是at
            if(agent != null && agent.getStandardURN() == StandardEntityURN.AMBULANCE_TEAM) {
                //获取目标entity
                StandardEntity target = this.worldInfo.getEntity(this.allocationData.get(agentID));
                //分配agent自动行动
                if(target != null) {
                    if(target instanceof Human) {
                        CommandAmbulance command = new CommandAmbulance(
                                true,
                                agentID,
                                target.getID(),
                                CommandAmbulance.ACTION_AUTONOMY
                        );
                        //将命令添加进message
                        this.messages.add(command);
                    } else if(target instanceof Area) {
                        //侦查命令
                        CommandScout command = new CommandScout(
                                true,
                                agentID,
                                target.getID(),
                                this.scoutDistance
                        );
                        this.messages.add(command);
                    }
                }
            }
        }
        return this;
    }

    @Override
    public Collection<CommunicationMessage> getResult() {
        return this.messages;
    }


}
