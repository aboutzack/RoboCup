package AUR.centralized;

import adf.agent.action.Action;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.centralized.CommandFire;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.centralized.CommandExecutor;

public class AURCommandExecutorFire extends CommandExecutor<CommandFire> 
{
    public AURCommandExecutorFire(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) 
    {
        super(ai, wi, si, moduleManager, developData);
    }

    @Override
    public CommandExecutor setCommand(CommandFire command) {return this;}
    
    @Override
	public Action getAction() {return null;}

    public CommandExecutor precompute(PrecomputeData precomputeData) 
    {
        super.precompute(precomputeData);
        return this;
    }

    public CommandExecutor resume(PrecomputeData precomputeData) 
    {
        super.resume(precomputeData);
        return this;
    }

    public CommandExecutor preparate() 
    {
        super.preparate();
        return this;
    }

    public CommandExecutor updateInfo(MessageManager messageManager)
    {
        super.updateInfo(messageManager);
        return this;
    }

    @Override
    public CommandExecutor calc() {return this;}
}
