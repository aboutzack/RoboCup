package AUR.extaction;

import AUR.util.ambulance.RunPoint;
import AUR.util.knd.AURWalkWatcher;
import AUR.util.ambulance.StayPoint;
import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.action.common.ActionRest;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.standard.entities.Area;
import rescuecore2.worldmodel.EntityID;
import java.util.*;
import java.util.List;

/**
 * Created by armanaxh in 2017
 */

public class AURAmbulanceActionExtMoveLegacy extends ExtAction {
    private PathPlanning pathPlanning;
    private RunPoint runPoint;
    private int thresholdRest;
    private int kernelTime;
    private AURWalkWatcher walkWatcher = null;

    private class EdgeView {
        public EdgeView(Edge e, int t) {
            this.time = t;
            this.edge = e;
        }

        public Edge edge;
        public int time;
    }

    private Collection<EdgeView> edgeViewList;
    private Collection<Edge> edgeView;
    private Area target;
    private Area roadTarget;
    private StayPoint stayPoint;

    public AURAmbulanceActionExtMoveLegacy(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, DevelopData developData) {
        super(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
        this.edgeViewList = new LinkedList<>();
        this.edgeView = new LinkedList<>();
        this.runPoint = new RunPoint(agentInfo , worldInfo );
        this.target = null;
        this.roadTarget = null;
        stayPoint = new StayPoint(worldInfo);
        this.thresholdRest = developData.getInteger("ActionExtMove.rest", 100);
        this.walkWatcher = moduleManager.getModule("knd.AuraWalkWatcher");
        switch (scenarioInfo.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("ActionExtMove.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("ActionExtMove.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("ActionExtMove.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
        }
    }

    @Override
    public ExtAction precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        this.pathPlanning.precompute(precomputeData);
        try {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        } catch (NoSuchConfigOptionException e) {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override
    public ExtAction resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
        this.pathPlanning.resume(precomputeData);
        try {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        } catch (NoSuchConfigOptionException e) {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override
    public ExtAction preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }
        this.pathPlanning.preparate();
        try {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        } catch (NoSuchConfigOptionException e) {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override
    public ExtAction updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        Collection<EdgeView> temp = new LinkedList<>();
        for (EdgeView edgeView : this.edgeViewList) {
            if (agentInfo.getTime() - edgeView.time > 15) {
                this.edgeView.remove(edgeView.edge);
                temp.add(edgeView);
            }
        }
        this.edgeViewList.removeAll(temp);

        for (Edge edge : agentInfo.getPositionArea().getEdges())
            if (this.cheakAgentAchieveTarget(edge)) {
                this.edgeView.add(edge);
                this.edgeViewList.add(new EdgeView(edge, agentInfo.getTime()));
                continue;
            }
        this.pathPlanning.updateInfo(messageManager);
        return this;
    }

    @Override
    public ExtAction setTarget(EntityID target) {
        this.target = null;
        StandardEntity entity = this.worldInfo.getEntity(target);
        if (entity != null) {
            if (entity.getStandardURN().equals(StandardEntityURN.BLOCKADE)) {
                entity = this.worldInfo.getEntity(((Blockade) entity).getPosition());
            } else if (entity instanceof Human) {
                entity = this.worldInfo.getPosition((Human) entity);
            }
            if (entity != null && entity instanceof Area) {
                this.target = (Area) entity;
            }
        }
        return this;
    }

    @Override
    public ExtAction calc() {
        this.result = null;

        Human agent = (Human) this.agentInfo.me();

        if (this.needRest(agent)) {
            this.result = this.calcRest();
            if (this.result != null) {
                return this;
            }
        }

        if (this.target == null) {
            if(this.result == null ) {
                List<EntityID> runPath = runPoint.find().getPath();
                this.result = walkWatcher.check(new ActionMove(runPath));
            }
            return this;
        }
        this.pathPlanning.setFrom(agent.getPosition());
        this.pathPlanning.setDestination(this.target.getID());
        List<EntityID> path = this.pathPlanning.calc().getResult();
        if (path != null && path.size() > 1) {
            this.roadTarget = (Area) worldInfo.getEntity(path.get(path.size() - 2));
            path.remove(path.size() - 1);
        } else {
            return this;
        }
        if (path != null && path.size() > 0) {
            for (Edge edge : this.roadTarget.getEdges()) {
                if (worldInfo.getEntity(edge.getNeighbour()) instanceof Building) {
                    if (!this.edgeView.contains(edge)) {
                        Point2D distinationPoint = this.stayPoint.calc(this.roadTarget, edge);
                        if (distinationPoint != null) {
                            ActionMove action = (new ActionMove(path, (int) distinationPoint.getX(), (int) distinationPoint.getY()));
                            //walkWatcher.check
                            //  TODO ActionMove action = walkWatcher.check(new ActionMove(path, (int) distinationPoint.getX(), (int) distinationPoint.getY()));
                            this.result = action;
                            break;
                        }
                    }
                }

            }
            if (this.result == null) {
                ActionMove action = walkWatcher.check(new ActionMove(path));
                this.result = action;
            }
        }
        if(this.result == null ) {
            List<EntityID> runPath = runPoint.find().getPath();
            this.result = walkWatcher.check(new ActionMove(runPath));
        }
        return this;
    }

    private boolean cheakAgentAchieveTarget(Edge edge) {

        final int distanceOfRoad = 1500;
        Area agentPosition = agentInfo.getPositionArea();
        StandardEntity e = worldInfo.getEntity(edge.getNeighbour());
        if (e instanceof Building) {
            Double midX, midY, agentX, agentY;
            midX = Double.valueOf((edge.getStartX() + edge.getEndX()) / 2);
            midY = Double.valueOf((edge.getStartY() + edge.getEndY()) / 2);
            agentX = Double.valueOf((int) agentInfo.getX());
            agentY = Double.valueOf((int) agentInfo.getY());
            Double distanceFormola = (midX - agentX) * (midX - agentX) + (midY - agentY) * (midY - agentY);
            if (distanceFormola < distanceOfRoad * distanceOfRoad && distanceFormola > -1) {
                return true;
            }
        }

        return false;
    }

    private boolean needRest(Human agent) {
        int hp = agent.getHP();
        int damage = agent.getDamage();
        if (hp == 0 || damage == 0) {
            return false;
        }
        int activeTime = (hp / damage) + ((hp % damage) != 0 ? 1 : 0);
        if (this.kernelTime == -1) {
            try {
                this.kernelTime = this.scenarioInfo.getKernelTimesteps();
            } catch (NoSuchConfigOptionException e) {
                this.kernelTime = -1;
            }
        }
        return damage >= this.thresholdRest || (activeTime + this.agentInfo.getTime()) < this.kernelTime;
    }

    private Action calcRest() {
        EntityID position = agentInfo.getPosition();
        Collection<EntityID> refuges = this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE);
        int currentSize = refuges.size();
        if (refuges.contains(position)) {
            return new ActionRest();
        }
        this.pathPlanning.setFrom(position);
        this.pathPlanning.setDestination(refuges);
        List<EntityID> path = this.pathPlanning.calc().getResult();
        if (path != null && path.size() > 0) {
            ActionMove action = walkWatcher.check(new ActionMove(path));
            this.result = action;
        }
        return null;
    }
}
