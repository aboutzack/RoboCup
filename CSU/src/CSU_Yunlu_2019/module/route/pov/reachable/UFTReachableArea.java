package CSU_Yunlu_2019.module.route.pov.reachable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import CSU_Yunlu_2019.module.route.pov.POVRouter;
import CSU_Yunlu_2019.module.route.pov.graph.AreaNode;
import CSU_Yunlu_2019.module.route.pov.graph.EdgeNode;
import CSU_Yunlu_2019.module.route.pov.graph.PassableDictionary;
import CSU_Yunlu_2019.util.UnionFindTree;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

/**
 * Huge map only.
 * 
 * @author utisam
 * 
 */
public class UFTReachableArea {
	private UnionFindTree<EntityID> sureReachableTree;

	public UFTReachableArea(WorldInfo world) {
		Collection<StandardEntity> areas = PassableDictionary.getEntitiesOfType(world, PassableDictionary.AREAS);
		ArrayList<EntityID> ids = new ArrayList<EntityID>(areas.size());
		for (StandardEntity se : areas) {
			ids.add(se.getID());
		}
		sureReachableTree = new UnionFindTree<EntityID>(ids);
	}

	public void update(final AgentInfo agentInfo, final WorldInfo worldInfo, final ScenarioInfo scenarioInfo,
			POVRouter router, final Set<EdgeNode> newPassables) {
		if (agentInfo.getTime() > scenarioInfo.getKernelAgentsIgnoreuntil()) {
			updateSureReachable(agentInfo, worldInfo, scenarioInfo, router, newPassables);
		}
	}

	private void updateSureReachable(final AgentInfo agentInfo, final WorldInfo worldInfo,
			final ScenarioInfo scenarioInfo, POVRouter router, Set<EdgeNode> newPassables) {
		// sureReachableTree.resetAll();
		final PassableDictionary passableDic = router.getPassableDic();
		for (EdgeNode edge : newPassables) {
			AreaNode first = null;
			for (AreaNode area : edge.getNeighbours()) {
				if (passableDic.getPassableLevel(area, edge, null).isPassable()) {
					if (first == null) {
						first = area;
					} else {
						sureReachableTree.unite(first.getBelong().getID(), area.getBelong().getID());
					}
				}
			}
		}
	}

	public boolean isSureReachable(EntityID id, EntityID id2) {
		return sureReachableTree.same(id, id2);
	}
}
