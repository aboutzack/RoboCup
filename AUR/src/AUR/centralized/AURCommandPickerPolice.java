package AUR.centralized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.centralized.CommandPicker;
import adf.component.communication.CommunicationMessage;
import rescuecore2.worldmodel.EntityID;

public class AURCommandPickerPolice extends CommandPicker 
{
    private Collection<CommunicationMessage> messages;
    private Map<EntityID, EntityID> allocationData;

    public AURCommandPickerPolice(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) 
    {
        super(ai, wi, si, moduleManager, developData);
        this.messages = new ArrayList<>();
        this.allocationData = null;
    }

    @Override
    public CommandPicker setAllocatorResult(Map<EntityID, EntityID> allocationData) 
    {
        this.allocationData = allocationData;
        return this;
    }

    @Override
    public CommandPicker calc() 
    {
        this.messages.clear();
        if(this.allocationData == null) return this;
        return this;
    }

    @Override
    public Collection<CommunicationMessage> getResult() {return this.messages;}
}