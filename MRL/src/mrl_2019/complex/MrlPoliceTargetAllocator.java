package mrl_2019.complex;

import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.module.complex.PoliceTargetAllocator;
import rescuecore2.worldmodel.EntityID;

import java.util.Map;

public class MrlPoliceTargetAllocator extends PoliceTargetAllocator {


    public MrlPoliceTargetAllocator(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
    }

    @Override
    public Map<EntityID, EntityID> getResult() {
        return null;
    }

    @Override
    public PoliceTargetAllocator calc() {
        return this;
    }
}
