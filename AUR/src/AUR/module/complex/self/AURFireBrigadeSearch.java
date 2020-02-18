package AUR.module.complex.self;

import java.util.ArrayList;

import AUR.module.algorithm.AURWorldClusterer;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURFireSearchValueSetter;
import AUR.util.knd.AURAreaGraphValue;
import AUR.util.knd.AURWorldGraph;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.algorithm.StaticClustering;
import adf.component.module.complex.Search;
import rescuecore2.worldmodel.EntityID;

public class AURFireBrigadeSearch extends Search {

	private PathPlanning pathPlanning;
	public EntityID result = null;
	public AURWorldGraph wsg = null;
	AURFireSearchValueSetter svs = new AURFireSearchValueSetter();
	public AgentInfo ai = null;

	StaticClustering buildingClusterer = null;

	public AURFireBrigadeSearch(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager,
			DevelopData developData) {
		super(ai, wi, si, moduleManager, developData);

		this.ai = ai;
		this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire",
				"adf.sample.module.algorithm.SamplePathPlanning");
		this.wsg = moduleManager.getModule("knd.AuraWorldGraph");
		this.buildingClusterer = moduleManager.getModule("SampleSearch.Clustering.Fire",
				"adf.sample.module.algorithm.SampleKMeans");

	}

	@Override
	public Search updateInfo(MessageManager messageManager) {
		super.updateInfo(messageManager);
		wsg.updateInfo(messageManager);
		if (this.getCountUpdateInfo() >= 2) {
			return this;
		}
		this.pathPlanning.updateInfo(messageManager);
		return this;
	}

	int waitCounter = 0;

	EntityID lastTarget = null; 
	@Override
	public Search calc() {

		buildingClusterer.calc();
		this.result = null;
		wsg.updateInfo(null);
		ArrayList<AURAreaGraphValue> list = new ArrayList<>();
		AURAreaGraph agentAg = wsg.getAreaGraph(ai.getPosition());
		ArrayList<AURAreaGraph> rubs = wsg.getPerceptibleUnburntBuildingIDs();
		rubs.remove(agentAg);
		for (AURAreaGraph ag : rubs) {
			list.add(new AURAreaGraphValue(ag));
		}
		int initialClusterIndex = buildingClusterer.getClusterIndex(ai.me());
		if (rubs.size() > 0) {
			svs.calc(wsg, list, buildingClusterer.getClusterEntityIDs(initialClusterIndex), lastTarget);
			if (svs.points.size() > 0) {
				this.result = svs.points.get(0).ag.area.getID();
				lastTarget = this.result;
				return this;
			}
		}

		ArrayList<AURAreaGraph> nrus = wsg.getNoBlockadeReachableUnburntBuildingIDs();
		if (nrus.size() > 0) {
			nrus.remove(agentAg);
			list.clear();
			for (AURAreaGraph ag : nrus) {
				list.add(new AURAreaGraphValue(ag));
			}

			svs.calcNoBlockade(wsg, list, buildingClusterer.getClusterEntityIDs(initialClusterIndex));
			if (svs.points.size() > 0) {
				this.result = svs.points.get(0).ag.area.getID();
				return this;
			}
		}
		return this;
	}

	@Override
	public EntityID getTarget() {
		return this.result;
	}

	@Override
	public Search precompute(PrecomputeData precomputeData) {
		super.precompute(precomputeData);
		this.buildingClusterer.precompute(precomputeData);
		this.wsg.precompute(precomputeData);
		return this;
	}

	@Override
	public Search resume(PrecomputeData precomputeData) {
		super.resume(precomputeData);
		buildingClusterer.preparate();
		this.wsg.preparate();
		this.worldInfo.requestRollback();
		return this;
	}

	@Override
	public Search preparate() {
		super.preparate();
		buildingClusterer.preparate();
		this.wsg.preparate();
		return this;
	}
}