package AUR.extaction;

import AUR.util.ambulance.Information.RescueInfo;
import AUR.util.knd.AURWalkWatcher;
import AUR.util.knd.AURWorldGraph;
import adf.agent.action.Action;
import adf.agent.action.ambulance.ActionLoad;
import adf.agent.action.ambulance.ActionRescue;
import adf.agent.action.ambulance.ActionUnload;
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
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static rescuecore2.standard.entities.StandardEntityURN.BLOCKADE;
import static rescuecore2.standard.entities.StandardEntityURN.CIVILIAN;

/**
 * Created by armanaxh in 2017
 */

public class AURActionTransport extends ExtAction {
    private PathPlanning pathPlanning;

    private int thresholdRest;
    private int kernelTime;
    private AURWalkWatcher walkWatcher = null;
    private AURWorldGraph wsg = null;
    private EntityID target;

    public AURActionTransport(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, DevelopData developData) {
        super(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
        this.target = null;
        this.thresholdRest = RescueInfo.thresholdRestDmg;
        this.walkWatcher = moduleManager.getModule("knd.AuraWalkWatcher");
        switch (scenarioInfo.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("ActionTransport.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("ActionTransport.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("ActionTransport.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
        }
        this.wsg = moduleManager.getModule("knd.AuraWorldGraph", "AUR.util.knd.AURWorldGraph");
    }

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
        this.wsg.precompute(precomputeData);
        return this;
    }

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
        this.wsg.resume(precomputeData);
        return this;
    }

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
        this.wsg.preparate();
        return this;
    }

    public ExtAction updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.pathPlanning.updateInfo(messageManager);
        return this;
    }

    @Override
    public ExtAction setTarget(EntityID target) {
        this.target = null;
        if (target != null) {
            StandardEntity entity = this.worldInfo.getEntity(target);
            if (entity instanceof Human || entity instanceof Area) {
                this.target = target;
                return this;
            }
        }
        return this;
    }

    @Override
    public ExtAction calc() {
        this.result = null;
        AmbulanceTeam agent = (AmbulanceTeam) this.agentInfo.me();
        Human transportHuman = this.agentInfo.someoneOnBoard();

        if (transportHuman != null) {
            this.result = this.calcUnload(transportHuman, this.target);
            if (this.result != null) {
                return this;
            }
        }
        if (this.needRest(agent)) {
//            EntityID areaID = this.convertArea(this.target);
//            ArrayList<EntityID> targets = new ArrayList<>();
//            if (areaID != null) {
//                targets.add(areaID);
//            }
            this.result = this.calcRefugeAction(false);
            if (this.result != null) {
                return this;
            }
        }
        if (this.target != null) {
            this.result = this.calcRescue();
        }
        return this;
    }

    private Action calcRescue() {
        StandardEntity targetEntity = this.worldInfo.getEntity(this.target);
        if (targetEntity == null) {
            return null;
        }
        EntityID agentPosition = agentInfo.getPosition();
        if (targetEntity instanceof Human) {
            Human human = (Human) targetEntity;
            if (!human.isPositionDefined()) {
                return null;
            }
            if (human.isHPDefined() && human.getHP() == 0) {
                return null;
            }
            EntityID targetPosition = human.getPosition();
            if (agentPosition.equals(targetPosition) ) {
                if (human.isBuriednessDefined() && human.getBuriedness() > 0) {
                    return new ActionRescue(human);
                } else if (human.getStandardURN() == CIVILIAN
                                && this.calcLoad() ) {
                    StandardEntity pos = worldInfo.getEntity(agentInfo.getPosition());
                    if(pos instanceof Road) {
                        ArrayList<EntityID> path = wsg.getPathToClosest(agentInfo.getPosition(), wsg.getAllRefuges());
                        if (path != null && path.size() > 0) {
                            return new ActionUnload();
                        }
                    }else {
                        return new ActionLoad(human.getID());
                    }
                }
            } else {
                this.pathPlanning.setFrom(agentPosition);
                this.pathPlanning.setDestination(targetPosition);
                List<EntityID> path = this.pathPlanning.calc().getResult();
                if (path != null && path.size() > 0) {
                    ActionMove action = walkWatcher.check(new ActionMove(path));
                    return  action;
                }
            }
            return null;
        }
        if (targetEntity.getStandardURN() == BLOCKADE) {

            Blockade blockade = (Blockade) targetEntity;
            if (blockade.isPositionDefined()) {
                targetEntity = this.worldInfo.getEntity(blockade.getPosition());
            }

        }
        if (targetEntity instanceof Area) {

            this.pathPlanning.setFrom(agentPosition);
            this.pathPlanning.setDestination(targetEntity.getID());
            List<EntityID> path = this.pathPlanning.calc().getResult();
            if (path != null && path.size() > 0) {
                ActionMove action = walkWatcher.check(new ActionMove(path));
                return  action;
            }
        }
        return null;

    }

    private boolean calcLoad(){

        //TODO
        if(!wsg.wi.getChanged().getChangedEntities().contains(target)){
            return false;
        }

        if(agentInfo.me() instanceof AmbulanceTeam) {
            AmbulanceTeam loadAmbulance = (AmbulanceTeam) agentInfo.me();
            for (EntityID entityID : worldInfo.getChanged().getChangedEntities()) {
                StandardEntity entity = worldInfo.getEntity(entityID);
                if (entity instanceof AmbulanceTeam && worldInfo.getPosition(target) != null ) {
                    AmbulanceTeam at = (AmbulanceTeam)entity;
                    if(at.getPosition().equals(worldInfo.getPosition(target).getID())) {
                        if (entityID.getValue() < loadAmbulance.getID().getValue()) {
                            loadAmbulance = at;
                        }
                    }
                }
            }

            if(loadAmbulance.getID().equals(agentInfo.getID())) {
                return true;
            }else {
                if ( loadAmbulance.isHPDefined() && loadAmbulance.getHP() != 0) {
                    return false;
                }
            }
        }

        return false;
    }


    private Action calcUnload(Human transportHuman, EntityID targetID) {
        final int distanceAzRefuge = 18000 ;
        if (transportHuman == null) {
            return null;
        }
        if (transportHuman.isHPDefined() && transportHuman.getHP() == 0) {
            return new ActionUnload();
        }

        if(!transportHuman.isPositionDefined()
                || worldInfo.getEntity(transportHuman.getPosition()) instanceof Building){
            return null ;
        }
        boolean unloadInRoad = true ;

//        for(StandardEntity entity : worldInfo.getEntitiesOfType(GAS_STATION)) {
//            if(entity instanceof GasStation) {
//                GasStation gasStation = (GasStation)entity ;
//                if (gasStation.isFierynessDefined() && gasStation.getFieryness() == 8  ) continue;
//                Pair<Integer, Integer> loc = worldInfo.getLocation(entity);
//                if (loc != null)
//                    if (getDistance(agentInfo.getX(), agentInfo.getY(), loc.first().doubleValue(), loc.second().doubleValue()) < RescueInfo.gasStationExplosionRange) {
//                        unloadInRoad = false;
//                    }
//            }
//        }
//        for(StandardEntity entity : worldInfo.getEntitiesOfType(REFUGE)) {
//            Pair<Integer,Integer> loc = worldInfo.getLocation(entity);
//            if(loc != null )
//                if (getDistance( agentInfo.getX() , agentInfo.getY() , loc.first().doubleValue() , loc.second().doubleValue() ) < distanceAzRefuge ){
//                    unloadInRoad = false ;
//                }
//        }
        unloadInRoad = false;
        if(unloadInRoad)
            if (agentInfo.getTime() + this.activeTime(transportHuman)  >= 320
                    && agentInfo.getPositionArea() instanceof Road) {
                return new ActionUnload();
            }
            /* TODO behene be shekli ke age zood tarin zaman marg az 300 bishtar bashe
            dar in sorat mishe gof damage sabet hast va damage afzayesh nadare
             */

        StandardEntity pos = worldInfo.getEntity(agentInfo.getPosition());
        if(pos instanceof Road) {
            ArrayList<EntityID> path = wsg.getPathToClosest(agentInfo.getPosition(), wsg.getAllRefuges());
            if (path == null || path.size() == 0) {
                return new ActionUnload();
            }
        }




        Action action = this.calcRefugeAction(true);
        if (action != null) {
            return action;
        }
        return null;
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


    private int activeTime(Human human) {
        if (human.isHPDefined() && human.isDamageDefined()) {
            int hp = human.getHP();
            int damage = human.getDamage();
            if (hp == 0) {
                return 0;
            }
            if (human.isBuriednessDefined() && human.getBuriedness() > 0) {
                int damageIdea = 45;
                damage += human.getBuriedness() - damageIdea > 0 ? human.getBuriedness() - damageIdea : damageIdea;
                return (hp / damage) + ((hp % damage) != 0 ? 1 : 0) - 20;
            } else {
                damage += 45 ;
                return (hp / damage) + ((hp % damage) != 0 ? 1 : 0) - 25;
            }
        }
        return -1;
    }
    private EntityID convertArea(EntityID targetID) {
        StandardEntity entity = this.worldInfo.getEntity(targetID);
        if (entity == null) {
            return null;
        }
        if (entity instanceof Human) {
            Human human = (Human) entity;
            if (human.isPositionDefined()) {
                EntityID position = human.getPosition();
                if (this.worldInfo.getEntity(position) instanceof Area) {
                    return position;
                }
            }
        } else if (entity instanceof Area) {
            return targetID;
        } else if (entity.getStandardURN() == BLOCKADE) {
            Blockade blockade = (Blockade) entity;
            if (blockade.isPositionDefined()) {
                return blockade.getPosition();
            }
        }
        return null;
    }

    private Action calcRefugeAction(boolean isUnload) {
        EntityID position = agentInfo.getPosition();
        Collection<EntityID> refuges = this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE);
        if (refuges.contains(position)) {
            return isUnload ? new ActionUnload() : new ActionRest();
        }
        this.pathPlanning.setFrom(position);
        this.pathPlanning.setDestination(refuges);
        List<EntityID> path = this.pathPlanning.calc().getResult();
        if (path != null && path.size() > 0) {
            ActionMove action = walkWatcher.check(new ActionMove(path));
            return  action;
        }
        return null;
    }


    private int getDistance(double fromX, double fromY, double toX, double toY) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        return (int) Math.hypot(dx, dy);
    }
}
