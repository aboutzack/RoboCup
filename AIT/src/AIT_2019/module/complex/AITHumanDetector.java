package AIT_2019.module.complex;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.extaction.ExtAction;
import adf.agent.Agent;
import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.HumanDetector;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.misc.geometry.Point2D;

import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class AITHumanDetector extends HumanDetector
{
    private PathPlanning pathPlanning;
    private Clustering clustering;
    private ExtAction extaction;
    private Clustering stuckedHumans;

    private EntityID result;
    private EntityID lastResult;
    private Action lastAction;
    private Point2D lastPoint;

    private Set<Human> avoidRescueTargets = new HashSet<>();
    private Set<Human> noExitedTargets = new HashSet<>();

    private int positionCount = 0;
    private int avoidingRescueCount = 0;
    private boolean isAvoidingRescue = false;
    private boolean isWaitingClear = false;

    private static final int AGENT_MOVEMENT = 7000;
    private static final int SENDING_AVOID_TIME_CLEAR_REQUEST_NEAR_TARGET = 3;
    private static final int SENDING_AVOID_TIME_CLEAR_REQUEST_TRANSPORTING = 3;
    private static final int SENDING_AVOID_TIME_CLEAR_REQUEST = 5;
    private static final int GIVE_UP_TIME_TO_RESCUE = 5;
    private static final int GIVE_UP_TIME_TO_RESCUE_WHEN_WAITING_CREAR = 8;
    private static final int GIVE_UP_TIME_TO_RESCUE_AND_SEARCH = 15;
    private static final int AVOID_TIME_TO_RESCUE_AFTER_GIVE_UP = 10;

    public AITHumanDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData)
    {
        super(ai, wi, si, moduleManager, developData);

        this.result = null;
        this.lastResult = null;
        this.lastAction = null;
        this.lastPoint = null;

        this.pathPlanning = moduleManager.getModule("ActionTransport.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
        this.clustering = moduleManager.getModule("SampleHumanDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans"); 
        this.extaction = moduleManager.getExtAction("TacticsAmbulanceTeam.ActionTransport", "adf.sample.extaction.ActionTransport");
        this.stuckedHumans = moduleManager.getModule("AITActionExtClear.StuckedHumans", "AIT_2019.module.algorithm.StuckedHumans");

        registerModule(this.pathPlanning);
        registerModule(this.clustering);
        registerModule(this.stuckedHumans);
    }

    @Override
    public HumanDetector updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);

        this.pathPlanning.updateInfo(messageManager);
        this.extaction.updateInfo(messageManager);

        if (isStuckMoving()) {
            this.positionCount++;
        } else {
            this.isWaitingClear = false;
            this.positionCount = 0;
        }

        // Call POLICE!
        if (isNeedToCallPolice()) {

            EntityID requestPointRadio;
            EntityID requestPointVoice;

            boolean isTargetNear =
                worldInfo.getDistance(this.result, agentInfo.getID()) < this.scenarioInfo.getPerceptionLosMaxDistance(); 
            boolean isTargetHuman = worldInfo.getEntity(this.result) instanceof Human;
            boolean isTransporting = this.agentInfo.someoneOnBoard() != null;

            if (isTransporting) {
                requestPointRadio = this.result;
                requestPointVoice = this.getRefuge();
            } else if (isTargetNear && isTargetHuman) {
                requestPointRadio = this.result;
                requestPointVoice = this.result;
            } else {
                requestPointRadio = this.agentInfo.getPosition();
                requestPointVoice = this.agentInfo.getPosition();
            }

            this.isWaitingClear = true;
            messageManager.addMessage(
                new CommandPolice(
                    true,
                    null,
                    requestPointRadio,
                    CommandPolice.ACTION_CLEAR
                )
            );
            messageManager.addMessage(
                new CommandPolice(
                    false,
                    null,
                    requestPointVoice,
                    CommandPolice.ACTION_CLEAR
                )
            );
//            System.out.println(this.agentInfo.getTime() + " 🚑 < HELP ME!!!!! AGENT(" + this.agentInfo.getID() + ") COUNT(" + this.positionCount + ")");
        }

        return this;

    }

    @Override
    public HumanDetector calc()
    {

        this.result = null;
        final Set<EntityID> changes = this.worldInfo.getChanged().getChangedEntities();
        List<Human> targets = new ArrayList<>();

        this.clustering.calc();
        int clusterIndex = clustering.getClusterIndex(this.agentInfo.getID());

        // avoid to rescue, and search
        if (this.isAvoidingRescue) {
            if (this.avoidingRescueCount <= AVOID_TIME_TO_RESCUE_AFTER_GIVE_UP) {
                this.avoidingRescueCount++;
                this.result = null;
                return this;
            } else {
                this.isAvoidingRescue = false;
                this.avoidingRescueCount = 0;
            }
        }

        // remove civilians from list, if find out
        java.util.Iterator<Human> iter = noExitedTargets.iterator();
        while (iter.hasNext()) {
            if (changes.contains(iter.next().getID())) {
                iter.remove();
            }
        }

        // get option
        final Human transportHuman = this.agentInfo.someoneOnBoard();
        final boolean isTransporting = transportHuman != null;
        if (isTransporting == true) {
            targets.add(transportHuman);
        } 

        if (targets == null || targets.isEmpty()) {
            targets = this.calcTargetInCluster(clusterIndex);
        }

        if (targets == null || targets.isEmpty()) {
            targets = this.calcTargetInWorld();
        }

        // select option
        if (targets != null && !targets.isEmpty()) {

            targets.sort(new PrioritySorter(worldInfo, this.agentInfo));

            for (Human target : targets) {

                if (avoidRescueTargets.contains(target) || noExitedTargets.contains(target)) {
                    continue;
                }

                // give up to rescue when it takes too many steps
                final boolean shouldGiveup = !this.isWaitingClear && this.positionCount >= GIVE_UP_TIME_TO_RESCUE
                    || this.isWaitingClear && this.positionCount >= GIVE_UP_TIME_TO_RESCUE_WHEN_WAITING_CREAR;
                final boolean isNoTargets = (targets.size() - avoidRescueTargets.size() - noExitedTargets.size()) <= 0;
                if (this.positionCount >= GIVE_UP_TIME_TO_RESCUE_AND_SEARCH) {
                    this.isAvoidingRescue = true;
                    this.avoidingRescueCount = 0;
                    this.result = null;
                    break;
                }
                if (shouldGiveup) {
                    this.avoidRescueTargets.add(target);
                    if (isNoTargets) {
                        this.isAvoidingRescue = true;
                        this.avoidingRescueCount = 0;
                        this.positionCount = 0;
                        this.result = null;
                        break;
                    }
                }

                // check WorldInfo is not lie?
                final boolean isOnSamePositionWithHuman = target.getPosition().equals(this.agentInfo.getPosition());
                final boolean isTargetTrulyExist = changes.contains(target.getID());
                if (isOnSamePositionWithHuman && !isTargetTrulyExist) {
                    noExitedTargets.add(target);
                    continue;
                }

                // check agent can execute command?
                if (canExecuteCommand(target.getID())) {
                    this.result = target.getID();
                    break;
                };

            }
        }

        if (this.result == null) {
            avoidRescueTargets.clear();
            noExitedTargets.clear();
        }

        this.lastResult = this.result;
        this.lastAction = this.getAction(this.result);
        this.lastPoint = new Point2D(this.agentInfo.getX(), this.agentInfo.getY());

//        System.out.println(this.agentInfo.getTime() + " 🚑 < HumanDetector AGENT(" + this.agentInfo.getID() + ") TARGET(" + this.result + ")");

        return this;
    }

    private List<Human> calcTargetInCluster(int clusterIndex)
    {

        Collection<StandardEntity> entitiesInCluster = clustering.getClusterEntities(clusterIndex);
        if (entitiesInCluster == null || entitiesInCluster.isEmpty())
        {
            return null;
        }

        List<Human> rescueTargets = new ArrayList<>();
        List<Human> loadTargets = new ArrayList<>();

        // Rescue agents(not include Civilian) first
        for (StandardEntity next : this.worldInfo.getEntitiesOfType(AMBULANCE_TEAM, FIRE_BRIGADE, POLICE_FORCE))
        {
            Human h = (Human) next;
            StandardEntity positionEntity = this.worldInfo.getPosition(h);
            boolean isThisMe = this.agentInfo.getID().equals(h.getID());

            if (isThisMe || (positionEntity.getStandardURN() == StandardEntityURN.REFUGE)) {
                continue;
            }
            else if (entitiesInCluster.contains(positionEntity)) {
                boolean needToRescue = h.isHPDefined() && h.isBuriednessDefined() && h.getHP() > 0 && h.getBuriedness() > 0;
                if (needToRescue) {
                    rescueTargets.add(h);
                }
            }
        }

        // Rescue Civilian
        if (rescueTargets.isEmpty() && loadTargets.isEmpty()) {
            for (StandardEntity next : this.worldInfo.getEntitiesOfType(CIVILIAN)) {
                Human h = (Human) next;
                StandardEntity positionEntity = this.worldInfo.getPosition(h);

                if(positionEntity.getStandardURN() == StandardEntityURN.REFUGE) {
                    continue;
                }
                else if (entitiesInCluster.contains(positionEntity)) {
                    if (isBetterToRescue(h, positionEntity)) rescueTargets.add(h);
                    if (isBetterToLoad(h, positionEntity)) loadTargets.add(h);
                }
            }
        }

        // Is still the list is empty, add all agents that this agents sensable
        if (rescueTargets.isEmpty() && loadTargets.isEmpty()) {
            for (StandardEntity next :this.worldInfo.getEntitiesOfType(CIVILIAN)) {
                Human h = (Human) next;
                StandardEntity positionEntity = this.worldInfo.getPosition(h);

                if(positionEntity.getStandardURN() == StandardEntityURN.REFUGE) {
                    continue;
                }
                else if (entitiesInCluster.contains(positionEntity)) {
                    if ((h.isBuriednessDefined() &&  h.getBuriedness() > 0) 
                        || (h.isDamageDefined() && h.getDamage() > 0)) {
                        rescueTargets.add(h);
                    }
                }
            }
        }

        if (rescueTargets.size() > 0)
        {
            return rescueTargets;
        }

        if (loadTargets.size() > 0)
        {
            return loadTargets;
        }

        return null;

    }

    private List<Human> calcTargetInWorld()
    {
        List<Human> rescueTargets = new ArrayList<>();
        List<Human> loadTargets = new ArrayList<>();

        for (StandardEntity next : this.worldInfo.getEntitiesOfType(AMBULANCE_TEAM, FIRE_BRIGADE, POLICE_FORCE)) {
            Human human = (Human) next;
            StandardEntity positionEntity = this.worldInfo.getPosition(human);
            boolean isThisMe = this.agentInfo.getID().getValue() == human.getID().getValue();
            boolean humanInRefuge = positionEntity.getStandardURN() == StandardEntityURN.REFUGE;

            if (isThisMe || humanInRefuge) {
                continue;
            }
            else if (positionEntity != null && human.isHPDefined() && human.isBuriednessDefined()) {
                if (human.getHP() > 0 && human.getBuriedness() > 0) {
                    rescueTargets.add(human);
                }
            }
        }

        if (rescueTargets.isEmpty()) {
            for (StandardEntity next : this.worldInfo.getEntitiesOfType(CIVILIAN)) {
                Human human = (Human) next;
                StandardEntity positionEntity = this.worldInfo.getPosition(human);

                if(positionEntity.getStandardURN() == StandardEntityURN.REFUGE) {
                    continue;
                }
                else {
                    if (isBetterToRescue(human, positionEntity)) rescueTargets.add(human);
                    if (isBetterToLoad(human, positionEntity)) loadTargets.add(human);
                }
            }
        }

        if (rescueTargets.size() > 0)
        {
            return rescueTargets;
        }

        if (loadTargets.size() > 0)
        {
            return loadTargets;
        }

        return null;

    }

    @Override
    public EntityID getTarget()
    {
        return this.result;
    }

    @Override
    public HumanDetector precompute(PrecomputeData precomputeData)
    {
        super.precompute(precomputeData);
        this.pathPlanning.precompute(precomputeData);
        this.extaction.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        return this;
    }

    @Override
    public HumanDetector resume(PrecomputeData precomputeData)
    {
        super.resume(precomputeData);
        this.pathPlanning.resume(precomputeData);
        this.extaction.resume(precomputeData);
        if (this.getCountResume() >= 2)
        {
            return this;
        }
        return this;
    }

    @Override
    public HumanDetector preparate()
    {
        super.preparate();
        this.pathPlanning.preparate();
        this.extaction.preparate();
        if (this.getCountPreparate() >= 2)
        {
            return this;
        }
        return this;
    }

    private class PrioritySorter implements Comparator<Human> {
        private WorldInfo worldInfo;
        private AgentInfo agentInfo;

        PrioritySorter(WorldInfo wi, AgentInfo ai) {
            this.worldInfo = wi;
            this.agentInfo = ai;
        }

        public int compare(Human a, Human b) {
            return Integer.compare(priorityOfRescue(a), priorityOfRescue(b));
        }

        private int priorityOfRescue(Human victim) {
            int time2victim = this.worldInfo.getDistance(this.agentInfo.getID(), victim.getID()) / AGENT_MOVEMENT;
            int time2rescue = victim.getBuriedness();

            return (int)(((time2victim + time2rescue) * victim.getDamage()) - victim.getHP());
        }
    }

    private Boolean isBetterToRescue(Human h, StandardEntity positionEntity) {
        int time2victim = this.worldInfo.getDistance(this.agentInfo.getID(), h.getID()) / AGENT_MOVEMENT;
        int time2rescue = h.getBuriedness();

        if (h.getBuriedness() <= 0) {
            return false;
        }

        if (positionEntity.getStandardURN() == BUILDING) {
            Building b = (Building)positionEntity;
            boolean canRescue = ((time2victim + time2rescue) * h.getDamage()) <= h.getHP();
            if (canRescue) {
//                System.out.println(this.agentInfo.getID() + ": civilian who can alive if amb rescue(" + h.getID() + ")");
                return true;
            }
        }

        return false;
    }

    private Boolean isBetterToLoad(Human h, StandardEntity positionEntity) {

        if (!h.isDamageDefined() || h.getDamage() <= 0) {
            return false;
        }

        if (positionEntity.getStandardURN() == BUILDING) {
            if (h.getBuriedness() == 0) {
//                System.out.println(this.agentInfo.getID() + ": civilian who is in building and not burying(" + h.getID() + ")");
                return true;
            }
        }
        if (positionEntity.getStandardURN() == ROAD) {
            if (h.getDamage() * 50 > h.getHP()) {
//                System.out.println(this.agentInfo.getID() + ": civilian who is almost dead(" + h.getID() + ")");
                return true;
            }
        }

        return false;
    }

    private boolean canExecuteCommand(EntityID target) {
        return this.getAction(target) != null;
    }

    private boolean isNeedToCallPolice() {
        boolean isTransporting = this.agentInfo.someoneOnBoard() != null;
        boolean isTargetNear =
            worldInfo.getDistance(this.result, agentInfo.getID()) < this.scenarioInfo.getPerceptionLosMaxDistance();

        if (this.positionCount == 0) {
            return false;
        }
        if (this.positionCount >= SENDING_AVOID_TIME_CLEAR_REQUEST_TRANSPORTING && isTransporting) {
            return true;
        } else if (this.positionCount >= SENDING_AVOID_TIME_CLEAR_REQUEST_NEAR_TARGET && isTargetNear) { 
            return true;
        } else if (this.positionCount >= SENDING_AVOID_TIME_CLEAR_REQUEST) {
            return true;
        }
        return false;
    }

    private Boolean isStuckMoving() {
        if (this.lastAction == null || this.lastAction.getClass() != ActionMove.class) {
            return false;
        }

        if (isStuckInBlockades()) {
            return true;
        }

        Point2D nowPoint = new Point2D(this.agentInfo.getX(), this.agentInfo.getY());
        if (nowPoint.equals(lastPoint)) {
//            System.out.println(" < AAAAAAAA STUCKED!!!!!!!!!! ) AGENT(" + this.agentInfo.getID() + ")");
            return true;
        }

        return false;
    }

    private Boolean isStuckInBlockades() {
        return this.stuckedHumans.calc().getClusterIndex(this.agentInfo.getID()) == 0;
    }

    private Action getAction(EntityID target) {
        this.extaction.setTarget(target);
        this.extaction.calc();
        final Action action = this.extaction.getAction();
        return action;
    }

    private EntityID getRefuge()
    {
        PathPlanning pathPlanning = this.pathPlanning;

        Human agent = (Human)this.agentInfo.me();
        EntityID position = worldInfo.getPosition(agent).getID();
        EntityID target = this.agentInfo.someoneOnBoard().getPosition();

        Collection<EntityID> refuges = this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE);
        int size = refuges.size();
        List<EntityID> firstResult = null;
        while (refuges.size() > 0)
        {
            pathPlanning.setFrom(position);
            pathPlanning.setDestination(refuges);
            List<EntityID> path = pathPlanning.calc().getResult();
            if (path != null && path.size() > 0)
            {
                if (firstResult == null)
                {
                    firstResult = new ArrayList<>(path);
                }
                EntityID refugeID = path.get(path.size() - 1);
                pathPlanning.setFrom(refugeID);
                pathPlanning.setDestination(target);
                List<EntityID> fromRefugeToTarget = pathPlanning.calc().getResult();
                if (fromRefugeToTarget != null && fromRefugeToTarget.size() > 0)
                {
                    return refugeID;
                }
                refuges.remove(refugeID);
                //remove failed
                if (size == refuges.size())
                {
                    break;
                }
                size = refuges.size();
            }
            else
            {
                break;
            }
        }
        return firstResult != null ? firstResult.get(firstResult.size() - 1) : null;
    }

}

