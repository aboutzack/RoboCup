package mrl_2019.complex.firebrigade;

import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.module.complex.BuildingDetector;
import rescuecore2.worldmodel.EntityID;

/**
 * Created by Peyman on 7/12/2017.
 */
public class MrlFBSearchBuildingDetector extends BuildingDetector {

    public MrlFBSearchBuildingDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
    }

    @Override
    public EntityID getTarget() {
        return null;
    }

    @Override
    public BuildingDetector calc() {
        return null;
    }
}
