package mrl_2019.extaction.clear;

import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.action.police.ActionClear;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.PathPlanning;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.entity.MrlRoad;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Mahdi
 */
public abstract class ClearActExecutor {
    protected ClearTools clearTools;
    protected WorldInfo worldInfo;
    protected AgentInfo agentInfo;
    protected ScenarioInfo scenarioInfo;
    protected MrlWorldHelper worldHelper;
    protected PathPlanning pathPlanning;

    protected ClearActExecutor(MrlWorldHelper worldHelper, WorldInfo worldInfo, AgentInfo agentInfo, ScenarioInfo scenarioInfo, PathPlanning pathPlanning) {
        this.worldHelper = worldHelper;
        this.worldInfo = worldInfo;
        this.agentInfo = agentInfo;
        this.scenarioInfo = scenarioInfo;
        clearTools = new ClearTools(worldHelper, worldInfo, agentInfo, scenarioInfo);
        this.pathPlanning = pathPlanning;
    }


    public abstract Action clearWay(List<EntityID> path, EntityID target);

    public abstract Action clearAroundTarget(Pair<Integer, Integer> targetLocation);

    protected Pair<Integer, Integer> getSelfLocation() {
        return worldInfo.getLocation(agentInfo.getID());
    }

    protected Area getSelfPosition() {
        return agentInfo.getPositionArea();
    }

    protected Action moveAction(List<EntityID> path) {
        return new ActionMove(path);
    }

    protected Action moveAction(int time, List<EntityID> path, int x, int y) {
        return new ActionMove(path, x, y);
    }

    protected Action moveAction(Area entity, boolean force) {
        List<EntityID> result = pathPlanning.setFrom(agentInfo.getPosition()).setDestination(entity.getID()).calc().getResult();
        if (result == null) {
            return null;
        }
        return moveAction(result);
    }

    protected Action moveToPoint(EntityID position, int x, int y) {
        if (agentInfo.getPosition().equals(position)) {
            List<EntityID> list = new ArrayList<EntityID>();
            list.add(position);

            return moveAction(agentInfo.getTime(), list, x, y);
        } else {
            return moveAction((Area) worldInfo.getEntity(position), false);
        }
    }

    protected Action sendClearAct(int time, int x, int y) {
        return new ActionClear(x, y);
    }

    protected Action sendClearAct(int time, EntityID blockadeId) {
        return new ActionClear(blockadeId);
    }

    protected Map<EntityID, EntityID> getEntranceRoads() {
        return worldHelper.getEntranceRoads();
    }


    protected Set<Blockade> getBlockadeSeen() {
        return worldHelper.getBlockadesSeen();
    }

    protected MrlRoad getMrlRoad(EntityID roadId) {
        return worldHelper.getMrlRoad(roadId);
    }


    protected Set<Road> getRoadsSeen() {
        return worldHelper.getRoadsSeen();
    }

    protected void printData(String s) {
        worldHelper.printData(s);
    }

    protected List<StandardEntity> getEntities(List<EntityID> idList) {
        List<StandardEntity> entities = new ArrayList<>();
        for (EntityID id : idList) {
            entities.add(worldInfo.getEntity(id));
        }
        return entities;
    }
}
