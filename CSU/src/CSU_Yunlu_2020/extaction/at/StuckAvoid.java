
package CSU_Yunlu_2020.extaction.at;

import CSU_Yunlu_2020.module.algorithm.StuckDetector;
import adf.agent.action.common.ActionMove;
import adf.agent.info.AgentInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import static rescuecore2.standard.entities.StandardEntityURN.REFUGE;

public class StuckAvoid {
    
    private AgentInfo agentInfo;
    private WorldInfo worldInfo;

    private final int MAXTRIAL = 200;//最大循环次数
    private final int MAXRANGE = 50000;//获取的neighbors最大范围
    private StuckDetector stuckDetector;
    //private DebugLog logger;
    private Random random = new Random();

    public StuckAvoid(AgentInfo ai, WorldInfo wi) {
        agentInfo = ai;
        worldInfo = wi;

        stuckDetector = new StuckDetector(agentInfo);
        //logger = new DebugLog(agentInfo);
    }

    //检查是否stuck,是的话返回真
    public boolean check(EntityID entityID) {

		stuckDetector.update(entityID);
        stuckDetector.warnStuck();
        return stuckDetector.isStucked();
    }

    //附近有REFUGE就设为目标,没有就随便选一个邻居设为目标(目标必须可达到)
	public ActionMove avoidStuck(PathPlanning pathPlanning) {
		EntityID position = agentInfo.getPosition();

		Collection<StandardEntity> neighbors;
		if (agentInfo.someoneOnBoard() != null) {
			//获取已知的REFUGE列表
			neighbors = worldInfo.getEntitiesOfType(REFUGE);
		} else {
			//获取以当前智能体为中心50000码以内的所有东西
			neighbors = worldInfo.getObjectsInRange(agentInfo.getID(), MAXRANGE);
		}
		Object[] array = neighbors.toArray();

		if (array.length > 0) {
			//随机获取一个邻居
			StandardEntity neighbor = (StandardEntity) array[random.nextInt(array.length)];
			int i = 0;//用来记录循环次数
			while (i < MAXTRIAL) {//最多循环200次
				++i;
				//是建筑且着火||是路障||是自己||自己和邻居位置重叠(背着伤者neighbor)
				if ((neighbor instanceof Building && ((Building)neighbor).isOnFire()) ||
					(neighbor instanceof Blockade) ||
					(neighbor.getID().getValue() == agentInfo.getID().getValue()) ||
					(position.getValue() == neighbor.getID().getValue()))
				{
					//从换neighbors中换个目标
					neighbor = (StandardEntity) array[random.nextInt(array.length)];
					continue;
				}
				//寻路
				pathPlanning.setFrom(position);
				pathPlanning.setDestination(neighbor.getID());
				List<EntityID> path = pathPlanning.calc().getResult();
				//有路,走:没路,返回null.
				if (path != null && path.size() > 0) {
//					logger.log("[AvoidStuck]: neighbors: " + array.length);
//					logger.log("[AvoidStuck] Succeed");
					return new ActionMove(path);
				}
			}
		}

//		logger.log("[AvoidStuck]: neighbors: " + array.length);
//		logger.log("[AvoidStuck] Failed.");
		return null;
	}
    
}
