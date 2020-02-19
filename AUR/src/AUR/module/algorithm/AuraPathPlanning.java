package AUR.module.algorithm;

import java.util.Collection;
import java.util.List;

import AUR.util.knd.AURWorldGraph;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.worldmodel.EntityID;

public class AuraPathPlanning extends PathPlanning {
    private EntityID from;
    private Collection<EntityID> targets;
    private List<EntityID> result;

    public AURWorldGraph wsg = null;

    public AuraPathPlanning(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.wsg = moduleManager.getModule("knd.AuraWorldGraph");
        //AUR.util.knd.testingViewer.instance.addGraph(wsg);
    }
    @Override
    public List<EntityID> getResult() {
        return this.result;
    }

    @Override
    public PathPlanning setFrom(EntityID id) {
        this.from = id;
        return this;
    }


    @Override
    public PathPlanning setDestination(Collection<EntityID> targets) {
        this.targets = targets;
        return this;
    }

    @Override
    public PathPlanning updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        this.result = null;
        wsg.updateInfo(messageManager);
        return this;
    }

    @Override
    public PathPlanning precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        return this;
    }

    @Override
    public PathPlanning resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
	this.preparate();
        return this;
    }

    @Override
    public PathPlanning preparate() {
        super.preparate();
        return this;
    }

    @Override
    public PathPlanning calc() {
        //long t = System.currentTimeMillis();
        this.result = null;
        if(this.targets == null || this.targets.size() <= 0 || this.from == null) {
            return this;
        }
        this.result = wsg.getPathToClosest(this.from, targets);

        //System.out.println((System.currentTimeMillis() - t) + "ms | PathPlanning.calc()");
        return this;
    }

}
