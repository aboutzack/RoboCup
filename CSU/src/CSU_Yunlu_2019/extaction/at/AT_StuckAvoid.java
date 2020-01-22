
package CSU_Yunlu_2019.extaction.at;

import CSU_Yunlu_2019.module.algorithm.StuckDetector;
import adf.agent.action.common.ActionMove;
import adf.agent.info.AgentInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import static rescuecore2.standard.entities.StandardEntityURN.*;

import CSU_Yunlu_2019.module.algorithm.DebugLog;

public class AT_StuckAvoid {
    
    private AgentInfo agentInfo;
    private WorldInfo worldInfo;

    private final int MAXTRIAL = 200;
    private final int MAXRANGE = 50000;
    private StuckDetector stuckDetector;
    private DebugLog logger;
    private Random random = new Random();

    public AT_StuckAvoid(AgentInfo ai, WorldInfo wi) {
        agentInfo = ai;
        worldInfo = wi;

        stuckDetector = new StuckDetector(agentInfo);
        logger = new DebugLog(agentInfo);
    }

    public boolean check(EntityID edi) {
		stuckDetector.update(edi);
        stuckDetector.warnStuck();
        return stuckDetector.isStucked();
    }

	public ActionMove avoidStuck(PathPlanning pathPlanning) {
		EntityID position = agentInfo.getPosition();

		Collection<StandardEntity> neighbors;
		if (agentInfo.someoneOnBoard() != null) {
			neighbors = worldInfo.getEntitiesOfType(REFUGE);
		} else {
			neighbors = worldInfo.getObjectsInRange(agentInfo.getID(), MAXRANGE);
		}
		Object[] array = neighbors.toArray();

		if (array.length > 0) {
			StandardEntity ne = (StandardEntity) array[random.nextInt(array.length)];
			int cnt = 0;
			while (cnt < MAXTRIAL) {
				++cnt;
				if ((ne instanceof Building && ((Building)ne).isOnFire()) ||
					(ne instanceof Blockade) ||
					(ne.getID().getValue() == agentInfo.getID().getValue()) ||
					(position.getValue() == ne.getID().getValue()))
				{
					ne = (StandardEntity) array[random.nextInt(array.length)];
					continue;
				}
				pathPlanning.setFrom(position);
				pathPlanning.setDestination(ne.getID());
				List<EntityID> path = pathPlanning.calc().getResult();
				if (path != null && path.size() > 0) {
					logger.log("[AvoidStuck]: neighbors: " + array.length);
					logger.log("[AvoidStuck] Succeed");
					return new ActionMove(path);
				}
			}
		}

		logger.log("[AvoidStuck]: neighbors: " + array.length);
		logger.log("[AvoidStuck] Failed.");
		return null;
	}
    
}
