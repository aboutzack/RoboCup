package AUR.centralized;

import adf.agent.action.Action;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.centralized.CommandAmbulance;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.centralized.CommandExecutor;

public class AURCommandExecutorAmbulance extends CommandExecutor<CommandAmbulance> 
{
    public AURCommandExecutorAmbulance(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) 
    {
        super(ai, wi, si, moduleManager, developData);
    }

    @Override
	public Action getAction() {return null;}

	@Override
    public CommandExecutor setCommand(CommandAmbulance command) {return this;}

    @Override
    public CommandExecutor updateInfo(MessageManager messageManager)
    {
        super.updateInfo(messageManager);
        return this;
    }

    @Override
    public CommandExecutor precompute(PrecomputeData precomputeData) 
    {
        super.precompute(precomputeData);
        return this;
    }

    @Override
    public CommandExecutor resume(PrecomputeData precomputeData) 
    {
        super.resume(precomputeData);
        return this;
    }

    @Override
    public CommandExecutor preparate() 
    {
        super.preparate();
        return this;
    }

    @Override
    public CommandExecutor calc() {return this;}
}