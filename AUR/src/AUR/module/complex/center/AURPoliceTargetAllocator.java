package AUR.module.complex.center;

import java.util.HashMap;
import java.util.Map;

import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.complex.PoliceTargetAllocator;
import rescuecore2.worldmodel.EntityID;

public class AURPoliceTargetAllocator extends PoliceTargetAllocator
{
	public AURPoliceTargetAllocator(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData)
	{
		super(ai, wi, si, moduleManager, developData);
	}

	@Override
	public PoliceTargetAllocator resume(PrecomputeData precomputeData)
	{
		super.resume(precomputeData);
		return this;
	}

	@Override
	public PoliceTargetAllocator preparate()
	{
		super.preparate();
		return this;
	}

	@Override
	public Map<EntityID, EntityID> getResult() {return new HashMap<EntityID, EntityID>();}

	@Override
	public PoliceTargetAllocator calc()	{return this;}

	@Override
	public PoliceTargetAllocator updateInfo(MessageManager messageManager)
	{
		super.updateInfo(messageManager);
		return this;
	}
}
