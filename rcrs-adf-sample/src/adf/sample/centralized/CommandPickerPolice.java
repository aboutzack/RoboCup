package adf.sample.centralized;
//警员指令分配
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.centralized.CommandPicker;
import adf.component.communication.CommunicationMessage;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class CommandPickerPolice extends CommandPicker {
	  //存储所有即将发布的命令
    private Collection<CommunicationMessage> messages;	
  //可被分配的警察
    private Map<EntityID, EntityID> allocationData;		
    public CommandPickerPolice(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {    //构造函数    
        super(ai, wi, si, moduleManager, developData);
        this.messages = new ArrayList<>();
        this.allocationData = null;
    }

    @Override
    //指令分配结果
    public CommandPicker setAllocatorResult(Map<EntityID, EntityID> allocationData) {   
        this.allocationData = allocationData;
        return this;
    }

    @Override
    //获取指令算法
    public CommandPicker calc() {  
        this.messages.clear();
        if(this.allocationData == null) {
            return this;
        }
        //获取每个agent
        for(EntityID agentID : this.allocationData.keySet()) {   
            StandardEntity agent = this.worldInfo.getEntity(agentID);
            //agent存在
            if(agent != null && agent.getStandardURN() == StandardEntityURN.POLICE_FORCE) {  
            	  //分配任务
                StandardEntity target = this.worldInfo.getEntity(this.allocationData.get(agentID));  
                if(target != null) {
                	  //目标地点存在
                    if(target instanceof Area) {  
                        CommandPolice command = new CommandPolice(
                                true,
                                agentID,
                                target.getID(),
                            	//自主选择
                                CommandPolice.ACTION_AUTONOMY
                        );
                      //添加command至message
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
