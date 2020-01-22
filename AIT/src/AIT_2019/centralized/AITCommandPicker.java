package AIT_2019.centralized;

import adf.agent.info.*;
import adf.agent.develop.DevelopData;
import adf.agent.module.ModuleManager;
import adf.agent.communication.standard.bundle.centralized.*;
import adf.component.centralized.CommandPicker;
import adf.component.communication.CommunicationMessage;
import rescuecore2.standard.entities.*;
import static rescuecore2.standard.entities.StandardEntityURN.*;
import rescuecore2.worldmodel.EntityID;
import java.util.*;

public class AITCommandPicker extends CommandPicker
{
    private Collection<CommunicationMessage> messages = new LinkedList<>();
    private Map<EntityID, EntityID> allocations = null;

    private StandardEntityURN urn;

    public AITCommandPicker(
        AgentInfo ai, WorldInfo wi, ScenarioInfo si,
        ModuleManager mm, DevelopData dd)
    {
        super(ai, wi, si, mm, dd);
        this.urn = ai.me().getStandardURN();
    }

    @Override
    public CommandPicker setAllocatorResult(Map<EntityID, EntityID> allocations)
    {
        this.allocations = allocations;
        return this;
    }

    @Override
    public CommandPicker calc()
    {
        if(this.allocations == null) return this;
        this.messages.clear();

        for(EntityID agent : this.allocations.keySet())
        {
            final EntityID task = this.allocations.get(agent);
            final CommunicationMessage command = this.makeCommand(agent, task);
            this.messages.add(command);
        }

        return this;
    }

    private CommunicationMessage makeCommand(EntityID agent, EntityID task)
    {
        if (this.urn == FIRE_STATION)
        {
            final int action = CommandFire.ACTION_AUTONOMY;
            return new CommandFire(true, agent, task, action);
        }

        if (this.urn == AMBULANCE_CENTRE)
        {
            final int action = CommandAmbulance.ACTION_AUTONOMY;
            return new CommandAmbulance(true, agent, task, action);
        }

        if (this.urn == POLICE_OFFICE)
        {
            final int action = CommandPolice.ACTION_AUTONOMY;
            return new CommandPolice(true, agent, task, action);
        }

        return null;
    }

    @Override
    public Collection<CommunicationMessage> getResult()
    {
        return this.messages;
    }
}
