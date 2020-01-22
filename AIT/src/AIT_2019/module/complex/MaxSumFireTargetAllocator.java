package AIT_2019.module.complex;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.*;
import adf.agent.communication.standard.bundle.information.*;
import adf.agent.communication.standard.bundle.centralized.*;
import adf.agent.develop.DevelopData;
import adf.agent.info.*;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.algorithm.*;
import adf.component.module.complex.FireTargetAllocator;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import static rescuecore2.standard.entities.StandardEntityURN.*;
import es.csic.iiia.bms.*;
import es.csic.iiia.bms.factors.*;
import es.csic.iiia.bms.factors.CardinalityFactor.CardinalityFunction;
import java.util.*;
import java.util.stream.*;

public class MaxSumFireTargetAllocator extends FireTargetAllocator
{
    private final static StandardEntityURN URL = FIRE_STATION;
    private final static StandardEntityURN AGENT_URL = FIRE_BRIGADE;
    private final static int ITERATIONS = 100;
    private final static double PENALTY = 600.0;
    private final static EntityID SEARCHING_TASK = new EntityID(-1);

    private final Map<EntityID, EntityID> result = new HashMap<>();
    private Set<EntityID> agents = new HashSet<>();
    private final Set<EntityID> tasks = new HashSet<>();
    private final Set<EntityID> ignored = new HashSet<>();

    private final Map<EntityID, Factor<EntityID>> nodes = new HashMap<>();
    private final BufferedCommunicationAdapter adapter;

    private final Clustering neighbors;

    private final Set<EntityID> received = new HashSet<>();

    public MaxSumFireTargetAllocator(
        AgentInfo ai, WorldInfo wi, ScenarioInfo si,
        ModuleManager mm, DevelopData dd)
    {
        super(ai, wi, si, mm, dd);
        this.adapter = new BufferedCommunicationAdapter();

        this.neighbors = mm.getModule(
            "MaxSumFireTargetAllocator.Clustering",
            "AIT_2019.module.algorithm.NeighborBuildings");
        this.registerModule(this.neighbors);
    }

    @Override
    public Map<EntityID, EntityID> getResult()
    {
        return this.result;
    }

    @Override
    public FireTargetAllocator calc()
    {
        this.result.clear();
        if (this.agents.isEmpty()) this.initializeAgents();
        if (!this.have2allocate()) return this;

        this.initializeTasks();
        this.initializeFactorGraph();
        for (int i=0; i<ITERATIONS; ++i)
        {
            this.nodes.values().stream().forEach(Factor::run);
            this.adapter.execute(this.nodes);
        }

        for (EntityID agent : this.agents)
        {
            final Factor<EntityID> node = this.nodes.get(agent);
            EntityID task = selectTask((ProxyFactor<EntityID>)node);
            if (task.equals(SEARCHING_TASK)) task = null;
            this.result.put(agent, task);
        }
        //  @ DEBUG
        int n = 0;
        for (EntityID id : this.agents) if (this.result.get(id) != null) ++n;
//        System.out.println("FIRE ALLOCATOR -> " + n);
        //  @ END OF DEBUG

        return this;
    }

    @Override
    public FireTargetAllocator updateInfo(MessageManager mm)
    {
        super.updateInfo(mm);
        if (this.getCountUpdateInfo() >= 2) return this;

        this.received.clear();

        final Collection<CommunicationMessage> bldmessages =
            mm.getReceivedMessageList(MessageBuilding.class);
        for (CommunicationMessage tmp : bldmessages)
        {
            MessageBuilding message = (MessageBuilding)tmp;
            MessageUtil.reflectMessage(this.worldInfo, message);
        }

        final Collection<CommunicationMessage> fbmessages =
            mm.getReceivedMessageList(MessageFireBrigade.class);
        for (CommunicationMessage tmp : fbmessages)
        {
            MessageFireBrigade message = (MessageFireBrigade)tmp;
            MessageUtil.reflectMessage(this.worldInfo, message);

            final EntityID id = message.getAgentID();
            this.received.add(id);
            Human fb = (Human)this.worldInfo.getEntity(id);
            fb.undefineX();
            fb.undefineY();
        }

        final Collection<CommunicationMessage> repmessages =
            mm.getReceivedMessageList(MessageReport.class);
        for (CommunicationMessage tmp : repmessages)
        {
            MessageReport message = (MessageReport)tmp;
            if (message.isFromIDDefined())
                this.ignored.add(message.getFromID());
        }

        return this;
    }

    private boolean have2allocate()
    {
        if (!this.allCentersExists()) return false;
        final int nAgents = this.agents.size();
        if (this.received.size() != nAgents) return false;

        final int lowest = this.worldInfo.getEntityIDsOfType(URL)
            .stream()
            .mapToInt(EntityID::getValue)
            .min().orElse(-1);

        final int me = this.agentInfo.getID().getValue();
        final int time = this.agentInfo.getTime();
        final int ignored = this.scenarioInfo.getKernelAgentsIgnoreuntil();
        return time >= ignored && me == lowest;
    }

    private boolean allCentersExists()
    {
        final int fss = this.scenarioInfo.getScenarioAgentsFs();
        final int pos = this.scenarioInfo.getScenarioAgentsPo();
        final int acs = this.scenarioInfo.getScenarioAgentsAc();
        return fss > 0 && pos > 0 && acs > 0;
    }

    private void initializeAgents()
    {
        final Collection<EntityID> tmp =
            this.worldInfo.getEntityIDsOfType(AGENT_URL);
        this.agents = new HashSet<>(tmp);
    }

    private void initializeTasks()
    {
        this.tasks.clear();

        this.worldInfo.getEntitiesOfType(BUILDING)
            .stream()
            .map(Building.class::cast)
            .filter(Building::isOnFire)
            .filter(this::isCornerInFireCluster)
            .map(StandardEntity::getID)
            .forEach(this.tasks::add);

        this.tasks.removeAll(this.ignored);
        this.tasks.add(SEARCHING_TASK);
    }

    private boolean isCornerInFireCluster(Building building)
    {
        final Collection<StandardEntity> affecteds =
            this.neighbors.getClusterEntities(
                this.neighbors.getClusterIndex(building));

        for (StandardEntity tmp : affecteds)
        {
            final Building affected = (Building)tmp;

            if (!affected.isFierynessDefined()) return true;
            final int fieryness = affected.getFieryness();
            if (fieryness == 0) return true;
        }

        return false;
    }

    private void initializeFactorGraph()
    {
        this.initializeVariableNodes(this.agents);
        this.initializeFactorNodes(this.tasks);
        this.connectNodes(this.agents, this.tasks);
    }

    private void initializeVariableNodes(Collection<EntityID> ids)
    {
        for (EntityID id : ids)
        {
            final Factor<EntityID> tmp = new BMSSelectorFactor<>();
            final WeightingFactor<EntityID> vnode = new WeightingFactor<>(tmp);
            vnode.setMaxOperator(new Minimize());
            vnode.setIdentity(id);
            vnode.setCommunicationAdapter(this.adapter);
            this.nodes.put(id, vnode);
        }
    }

    private void initializeFactorNodes(Collection<EntityID> ids)
    {
        for (EntityID id : ids)
        {
            final CardinalityFactor<EntityID> fnode = new BMSCardinalityFactor<>();
            final CardinalityFunction func = new CardinalityFunction()
            {
                @Override
                public double getCost(int nActiveVariables)
                {
                    return MaxSumFireTargetAllocator
                        .this.computePenalty(id, nActiveVariables);
                }
            };
            fnode.setFunction(func);

            fnode.setMaxOperator(new Minimize());
            fnode.setIdentity(id);
            fnode.setCommunicationAdapter(this.adapter);
            this.nodes.put(id, fnode);
        }
    }

    private void connectNodes(
        Collection<EntityID> vnodeids, Collection<EntityID> fnodeids)
    {
        for (EntityID vnodeid : vnodeids) for (EntityID fnodeid : fnodeids)
        {
            WeightingFactor<EntityID> vnode =
                (WeightingFactor<EntityID>)this.nodes.get(vnodeid);
            vnode.addNeighbor(fnodeid);

            Factor<EntityID> fnode = this.nodes.get(fnodeid);
            fnode.addNeighbor(vnodeid);

            final double penalty = this.computePenalty(vnodeid, fnodeid);
            vnode.setPotential(fnodeid, penalty);
        }
    }

    private static EntityID selectTask(ProxyFactor<EntityID> proxy)
    {
        final SelectorFactor<EntityID> selector =
            (SelectorFactor<EntityID>)proxy.getInnerFactor();
        return selector.select();
    }

    private double computePenalty(EntityID agent, EntityID task)
    {
        if (task.equals(SEARCHING_TASK)) return 0.0;

        final double d = this.worldInfo.getDistance(agent, task);
        return d / (42000.0 / 1.5);
    }

    private double computePenalty(EntityID task, int nAgents)
    {
        if (task.equals(SEARCHING_TASK)) return 0.0;

        final Building entity = (Building)this.worldInfo.getEntity(task);
        if (nAgents == 0) return PENALTY;

        final int fieryness = entity.getFieryness();
        final int volume = entity.getGroundArea() * entity.getFloors() * 3;
        final double nRequested = fieryness * fieryness * volume / 500.0;

        final double nLeasts = Math.ceil(nRequested);
        final double ratio = Math.min((double)nAgents, nLeasts) / nLeasts;

        return PENALTY * (1.0-Math.pow(ratio, 2.0));
    }
}
