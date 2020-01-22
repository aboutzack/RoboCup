package AIT_2019.extaction;

import static rescuecore2.standard.entities.StandardEntityURN.FIRE_BRIGADE;
import static rescuecore2.standard.entities.StandardEntityURN.HYDRANT;
import static rescuecore2.standard.entities.StandardEntityURN.REFUGE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.action.common.ActionRest;
import adf.agent.action.fire.ActionExtinguish;
import adf.agent.action.fire.ActionRefill;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.StandardMessagePriority;
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.agent.communication.standard.bundle.information.MessageBuilding;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;


public class AITActionFireFighting extends ExtAction
{
    private PathPlanning pathPlanning = null;
    private Clustering stuckedHumans = null;

    private int kernelTime = -1;
    private int maxExtinguishDistance = -1;
    private int maxExtinguishPower = -1;
    private int maxWater = -1;
    private int thresholdRefillWater = -1;
    private int thresholdEnoughWater = -1;
    private int thresholdRestDamage = -1;
    private boolean shouldRefill = false;

    private EntityID targetID = null;

    private boolean shouldSendCommandPolice = false;
    private int avoidTimeSendingCommandPolice = -1;
    private Integer sentTimeCommandPolice = null;
    private Action lastAction = null;
    private Point2D lastPoint = null;

    private Building extinguishedBuilding = null;

    private int countIsStuckMoving = -1;
    private int thresholdExtinguishStucking = -1;

    public AITActionFireFighting(AgentInfo ai, WorldInfo wi, ScenarioInfo si,
            ModuleManager mm, DevelopData dd)
    {
        super(ai, wi, si, mm, dd);
        this.pathPlanning = moduleManager.getModule(
                "ActionFireFighting.PathPlanning",
                "adf.sample.module.algorithm.SamplePathPlanning");
        this.stuckedHumans = moduleManager.getModule(
                "AITActionExtClear.StuckedHumans",
                "AIT_2019.module.algorithm.StuckedHumans");

        this.maxExtinguishDistance = si.getFireExtinguishMaxDistance();
        this.maxExtinguishPower = si.getFireExtinguishMaxSum();
        this.maxWater = si.getFireTankMaximum();

        this.thresholdRefillWater = this.maxExtinguishPower;
        this.thresholdEnoughWater = (int) (this.maxWater * 0.9);
        this.thresholdRestDamage = 100;

        this.avoidTimeSendingCommandPolice = 4;

        this.countIsStuckMoving = 1;
        this.thresholdExtinguishStucking = 3;
    }

    @Override
    public ExtAction precompute(PrecomputeData precomputeData)
    {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() > 1)
        {
            return this;
        }
        return this;
    }

    @Override
    public ExtAction resume(PrecomputeData precomputeData)
    {
        super.resume(precomputeData);
        if (this.getCountResume() > 1)
        {
            return this;
        }
        this.preparate();
        return this;
    }

    @Override
    public ExtAction preparate()
    {
        super.preparate();
        if (this.getCountPreparate() > 1)
        {
            return this;
        }

        this.pathPlanning.preparate();
        this.stuckedHumans.preparate();

        try { this.kernelTime = this.scenarioInfo.getKernelTimesteps(); }
        catch (Exception e) { this.kernelTime = -1; }

        return this;
    }

    @Override
    public ExtAction updateInfo(MessageManager messageManager)
    {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() > 1)
        {
            return this;
        }

        this.pathPlanning.updateInfo(messageManager);
        this.stuckedHumans.updateInfo(messageManager);

        if (this.shouldSendCommandPolice)
        {
//            System.out.println("🚒 #" + this.agentInfo.getID() + " FIRE-FIGHTING -> " + "SEND COMMAND-POLICE FOR STUCKING-BUG");
            int currTime = this.agentInfo.getTime();
            if (this.sentTimeCommandPolice != null && this.sentTimeCommandPolice <= currTime)
            {
                EntityID agentPosID = this.agentInfo.getPosition();
                messageManager.addMessage(new CommandPolice(
                        true, StandardMessagePriority.HIGH, null,
                        agentPosID, CommandPolice.ACTION_CLEAR));
                messageManager.addMessage(new CommandPolice(
                        false, StandardMessagePriority.HIGH, null,
                        agentPosID, CommandPolice.ACTION_CLEAR));
                this.sentTimeCommandPolice = currTime + this.avoidTimeSendingCommandPolice;
                this.shouldSendCommandPolice = false;
            }
        }

        if (this.extinguishedBuilding != null)
        {
            messageManager.addMessage(new MessageBuilding(
                    true, StandardMessagePriority.NORMAL, this.extinguishedBuilding));
            this.out("EXTINGUISHED #" + this.extinguishedBuilding);
            this.extinguishedBuilding = null;
        }

        return this;
    }

    @Override
    public ExtAction setTarget(EntityID id)
    {
        this.targetID = null;
        if (id == null)
        {
            if (this.lastAction != null && this.lastAction.getClass() == ActionExtinguish.class)
            {
                ActionExtinguish lastAction = (ActionExtinguish) this.lastAction;
                Building lastTarget = (Building) this.worldInfo.getEntity(lastAction.getTarget());
                this.extinguishedBuilding = lastTarget;
            }
            return this;
        }

        Building building = (Building) this.worldInfo.getEntity(id);
        if (!building.isOnFire())
        {
            this.extinguishedBuilding = building;
            return this;
        }

        StandardEntity entity = this.worldInfo.getEntity(id);
        if (entity instanceof Building) { this.targetID = id; }
        return this;
    }

    @Override
    public ExtAction calc()
    {
        this.result = null;
        if (this.targetID == null) { return this; }
        StandardEntity agent = this.agentInfo.me();

        if (this.isStuckMoving(this.result)) { this.countIsStuckMoving++; }
        else { this.countIsStuckMoving = 1; }

        this.shouldRefill = this.needRefill(agent, shouldRefill);
        if (this.shouldRefill)
        {
            this.result = this.calcRefill(agent, targetID);
            if (this.result != null)
            {
                if (this.isStuckMoving(this.result)) { this.shouldSendCommandPolice = true; }
                lastAction = this.result;
                lastPoint = new Point2D(this.agentInfo.getX(), this.agentInfo.getY());
                return this;
            }
        }
        if (this.needRest(agent))
        {
            this.result = this.calcRest(agent, targetID);
            if (this.result != null)
            {
                if (this.isStuckMoving(this.result)) { this.shouldSendCommandPolice = true; }
                lastAction = this.result;
                lastPoint = new Point2D(this.agentInfo.getX(), this.agentInfo.getY());
                return this;
            }
        }
        this.result = this.calcExtinguish(agent, targetID);
        if (this.isStuckMoving(this.result)) { this.shouldSendCommandPolice = true; }

        lastAction = this.result;
        lastPoint = new Point2D(this.agentInfo.getX(), this.agentInfo.getY());
        return this;
    }


    private Boolean needRefill(StandardEntity agent, Boolean shouldRefill)
    {
        if (!(agent instanceof FireBrigade)) { return false; }
        FireBrigade fireBrigade = (FireBrigade) agent;

        if (shouldRefill)
        {
            StandardEntity agentPos = this.worldInfo.getPosition(agent.getID());

            boolean cond1 = !(agentPos instanceof Refuge || agentPos instanceof Hydrant);
            boolean cond2 = fireBrigade.getWater() < this.thresholdEnoughWater;
            return cond1 || cond2;
        }
        return fireBrigade.getWater() < this.thresholdRefillWater;
    }

    private Boolean needRest(StandardEntity agent)
    {
        if (!(agent instanceof Human))
        {
            return false;
        }
        Human human = (Human) agent;

        int hp = human.getHP();
        int damage = human.getDamage();
        if (hp == 0 || damage == 0)
        {
            return false;
        }
        int aliveTime = (hp / damage) + ((hp % damage != 0) ? 1 : 0);

        boolean cond1 = damage > this.thresholdRestDamage;
        boolean cond2 = aliveTime + this.agentInfo.getTime() < this.kernelTime;
        return cond1 || cond2;
    }

    private Action calcRefill(StandardEntity agent, EntityID targetID)
    {
        StandardEntity agentPos = this.worldInfo.getPosition(agent.getID());
        if (agentPos instanceof Refuge) { return new ActionRefill(); }
        if (agentPos instanceof Hydrant)
        {
            // this.out("STILL-CHECK HYDRANT#" + agentPos.getID());
            if(!this.isOccupied(agentPos.getID())) { return new ActionRefill(); }
        }
        // for refuges
        Collection<EntityID> refuges = this.worldInfo.getEntityIDsOfType(REFUGE);
        int currSizeRefuges = refuges.size();
        List<EntityID> path4Refuge = null;
        while (refuges.size() > 0)
        {
            this.pathPlanning.setFrom(agentPos.getID());
            this.pathPlanning.setDestination(refuges);
            List<EntityID> path2Refuge = this.pathPlanning.calc().getResult();
            if (path2Refuge != null && path2Refuge.size() > 0)
            {
                if (this.targetID == null)
                {
                    path4Refuge = path2Refuge;
                    break;
                }
                EntityID refugeID = path2Refuge.get(path2Refuge.size() - 1);
                this.pathPlanning.setFrom(refugeID);
                this.pathPlanning.setDestination(targetID);
                List<EntityID> path2Target = this.pathPlanning.calc().getResult();
                if (path2Target != null && path2Target.size() > 0)
                {
                    path4Refuge = path2Refuge;
                    break;
                }
                refuges.remove(refugeID);
                if (currSizeRefuges == refuges.size()) { break; }
                currSizeRefuges = refuges.size();
            }
            else
            {
                break;
            }
        }
        if (path4Refuge != null)
        {
            return new ActionMove(path4Refuge);
        }

        // for hydrants
        Collection<EntityID> hydrants = this.worldInfo.getEntityIDsOfType(HYDRANT);
        int currSizeHydrants = hydrants.size();
        List<EntityID> path4Hydrant = null;
        while (hydrants.size() > 0) {
            List<EntityID> path2Hydrant = this.calcPath(agentPos.getID(), hydrants);
            if (path2Hydrant != null && path2Hydrant.size() > 0)
            {
                EntityID hydrantID = path2Hydrant.get(path2Hydrant.size() - 1);
                // this.out("PRE-CHECK HYDRANT#" + hydrantID);
                if (this.isOccupied(hydrantID))
                {
                    hydrants.remove(hydrantID);
                    continue;
                }

                if (this.targetID == null)
                {
                    path4Hydrant = path2Hydrant;
                    break;
                }

                List<EntityID> path2Target = this.calcPath(hydrantID, targetID);
                if (path2Target != null && path2Target.size() > 0)
                {
                    path4Hydrant = path2Hydrant;
                    break;
                }

                hydrants.remove(hydrantID);
                if (currSizeHydrants == hydrants.size()) { break; }
                currSizeHydrants = hydrants.size();
            }
            else
            {
                break;
            }
        }
        if (path4Hydrant != null)
        {
            // this.out("HYDRANT#" + path4Hydrant.get(path4Hydrant.size() -1));
            return new ActionMove(path4Hydrant);
        }
        // this.out("NO HYDRANT");
        return null;
    }

    private Action calcRest(StandardEntity agent, EntityID targetID)
    {
        StandardEntity agentPos = this.worldInfo.getPosition(agent.getID());
        if (agentPos instanceof Refuge)
        {
            return new ActionRest();
        }

        Collection<EntityID> refuges = this.worldInfo.getEntityIDsOfType(REFUGE);
        int currSizeRefuges = refuges.size();
        List<EntityID> path4Refuge = null;
        while (refuges.size() > 0)
        {
            List<EntityID> path2Refuge = this.calcPath(agentPos.getID(), refuges);
            if (path2Refuge != null && path2Refuge.size() > 0)
            {
                if (this.targetID == null)
                {
                    path4Refuge = path2Refuge;
                    break;
                }

                EntityID refugeID = path2Refuge.get(path2Refuge.size() - 1);
                List<EntityID> path2Target = this.calcPath(refugeID, targetID);
                if (path2Target != null && path2Target.size() > 0)
                {
                    path4Refuge = path2Refuge;
                    break;
                }

                refuges.remove(refugeID);
                if (currSizeRefuges == refuges.size()) { break; }
                currSizeRefuges = refuges.size();
            }
            else
            {
                break;
            }
        }
        if (path4Refuge != null)
        {
            return new ActionMove(path4Refuge);
        }

        return null;
    }

    private Action calcExtinguish(StandardEntity agent, EntityID targetID)
    {
        StandardEntity agentPos = this.worldInfo.getPosition(agent.getID());
        if (this.isStuckInBlockades() ||
                (this.isStuckMoving(this.result) && this.countIsStuckMoving > this.thresholdExtinguishStucking))
        {
            double distance = this.worldInfo.getDistance(this.agentInfo.getPosition(), targetID);
            if (distance < this.maxExtinguishDistance)
            {
                return new ActionExtinguish(targetID, this.maxExtinguishPower);
            }
        }
        if (this.worldInfo.getChanged().getChangedEntities().contains(targetID))
        {
            return new ActionExtinguish(targetID, this.maxExtinguishPower);
        }

        this.pathPlanning.setFrom(agentPos.getID());
        this.pathPlanning.setDestination(targetID);
        List<EntityID> path = this.pathPlanning.calc().getResult();
        return this.calcMove(agentPos.getID(), path.get(path.size() - 1));
    }

    private Action calcMove(EntityID fromID, EntityID destinationID)
    {
        List<EntityID> path = this.calcPath(fromID, destinationID);
        if (path != null && path.size() > 1)
        {
            StandardEntity destination = this.worldInfo.getEntity(destinationID);
            if (destination instanceof Building && !(destination instanceof Refuge))
            {
                path.remove(path.size() - 1);
            }
            if (path.size() > 1) { path.remove(path.size() - 1); }
            return new ActionMove(path);
        }
        return null;
    }

    // for simulator bug
    private Boolean isStuckMoving(Action action)
    {
        if (action == null || (action != null && action.getClass() != ActionMove.class))
        {
            return false;
        }
        if (this.lastAction == null ||
                    (this.lastAction != null && this.lastAction.getClass() != ActionMove.class))
        {
            return false;
        }
        if (isStuckInBlockades())
        {
            return false;
        }
        List<EntityID> path = ((ActionMove) action).getPath();
        List<EntityID> lastPath = ((ActionMove) lastAction).getPath();
        if (path.equals(lastPath))
        {
            return true;
        }
        return false;
    }

    private Boolean isStuckInBlockades()
    {
        return this.stuckedHumans.calc().getClusterIndex(this.agentInfo.getID()) == 0;
    }

    protected List<EntityID> calcPath(EntityID fromID, EntityID destinationID)
    {
        pathPlanning.setFrom(fromID);
        pathPlanning.setDestination(destinationID);
        return pathPlanning.calc().getResult();
    }

    protected List<EntityID> calcPath(EntityID fromID, Collection<EntityID> destinationIDs)
    {
        pathPlanning.setFrom(fromID);
        pathPlanning.setDestination(destinationIDs);
        return pathPlanning.calc().getResult();
    }

    protected Boolean isOccupied(EntityID hydrantID)
    {
        if (!(this.worldInfo.getEntity(hydrantID) instanceof Hydrant))
        {
            return false;
        }
        // Set<EntityID> changedEntityIDs = this.worldInfo.getChanged().getChangedEntities();
        // if (!changedEntityIDs.contains(hydrantID))
        // {
            // return false;
        // }
        List<EntityID> agentIDsInTheHydrant = new ArrayList<>();
        for (EntityID fbID : this.worldInfo.getEntityIDsOfType(FIRE_BRIGADE))
        {
            if (this.worldInfo.getPosition(fbID).getID() == hydrantID)
            {
                agentIDsInTheHydrant.add(fbID);
            }
        }
        Optional<EntityID> ret = this.worldInfo.getEntityIDsOfType(FIRE_BRIGADE).stream()
                .filter(id -> this.worldInfo.getPosition(id).getID().equals(hydrantID))
                .max((id1, id2) -> {
                    final int i1 = id1.getValue();
                    final int i2 = id2.getValue();
                    return Integer.compare(i1, i2);
                });
        // this.out("OCCUPIED CHECK[" + hydrantID + "]: " + ret.orElse(null));
        return ret.orElse(null) != null && !(ret.orElse(null).equals(this.agentInfo.getID()));
    }

    private void out(String str)
    {
        String ret;
        ret  = "🚒  [" + String.format("%10d", this.agentInfo.getID().getValue()) + "]";
        ret += " FIRE-FIGHTING ";
        ret += "@" + String.format("%3d", this.agentInfo.getTime());
        ret += " -> ";
//        System.out.println(ret + str);
    }
}
