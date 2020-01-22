package AIT_2019.module.complex.self;

import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.*;
import adf.agent.module.ModuleManager;
import adf.component.module.complex.*;
import rescuecore2.worldmodel.EntityID;

public class DummyHumanDetector extends HumanDetector
{
	public DummyHumanDetector(
        AgentInfo ai, WorldInfo wi, ScenarioInfo si,
        ModuleManager mm, DevelopData dd)
    {
		super(ai, wi, si, mm, dd);
	}

	@Override
	public HumanDetector updateInfo(MessageManager mm)
    {
		super.updateInfo(mm);
		return this;
	}

	@Override
	public HumanDetector calc()
    {
		return this;
	}

	@Override
	public EntityID getTarget()
    {
        return null;
	}
}
