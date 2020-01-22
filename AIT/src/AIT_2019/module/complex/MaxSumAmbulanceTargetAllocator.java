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
import adf.component.module.complex.AmbulanceTargetAllocator;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import static rescuecore2.standard.entities.StandardEntityURN.*;
import es.csic.iiia.bms.*;
import es.csic.iiia.bms.factors.*;
import es.csic.iiia.bms.factors.CardinalityFactor.CardinalityFunction;
import java.util.*;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

public class MaxSumAmbulanceTargetAllocator extends AmbulanceTargetAllocator
{
    private final static StandardEntityURN URL = AMBULANCE_CENTRE;
    private final static StandardEntityURN AGENT_URL = AMBULANCE_TEAM;
    private final static int ITERATIONS = 100;
    private final static double PENALTY = 600.0;
    private final static EntityID SEARCHING_TASK = new EntityID(-1);

    private final Map<EntityID, EntityID> result = new HashMap<>();
    private Set<EntityID> agents = new HashSet<>();
    private final Set<EntityID> tasks = new HashSet<>();
    private final Set<EntityID> ignored = new HashSet<>();

    private final Map<EntityID, Factor<EntityID>> nodes = new HashMap<>();
    private final BufferedCommunicationAdapter adapter;

    private final Set<EntityID> received = new HashSet<>();

    public MaxSumAmbulanceTargetAllocator(
        AgentInfo ai, WorldInfo wi, ScenarioInfo si,
        ModuleManager mm, DevelopData dd)
    {
        super(ai, wi, si, mm, dd);
        this.adapter = new BufferedCommunicationAdapter();
    }

    @Override
    public Map<EntityID, EntityID> getResult()
    {
        return this.result;
    }

    @Override
    public AmbulanceTargetAllocator calc()
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
//        System.out.println("AMBULANCE ALLOCATOR -> " + n);
        //  @ END OF DEBUG

        return this;
    }

    @Override
    public AmbulanceTargetAllocator updateInfo(MessageManager mm)
    {
        super.updateInfo(mm);
        if (this.getCountUpdateInfo() >= 2) return this;

        this.received.clear();

        final Collection<CommunicationMessage> civmessages =
            mm.getReceivedMessageList(MessageCivilian.class);
        for (CommunicationMessage tmp : civmessages)
        {
            MessageCivilian message = (MessageCivilian)tmp;
            MessageUtil.reflectMessage(this.worldInfo, message);

            final EntityID id = message.getAgentID();
            Human cv = (Human)this.worldInfo.getEntity(id);
            cv.undefineX();
            cv.undefineY();
        }

        final Collection<CommunicationMessage> fbmessages =
            mm.getReceivedMessageList(MessageFireBrigade.class);
        for (CommunicationMessage tmp : fbmessages)
        {
            MessageFireBrigade message = (MessageFireBrigade)tmp;
            MessageUtil.reflectMessage(this.worldInfo, message);

            final EntityID id = message.getAgentID();
            Human fb = (Human)this.worldInfo.getEntity(id);
            fb.undefineX();
            fb.undefineY();
        }

        final Collection<CommunicationMessage> pfmessages =
            mm.getReceivedMessageList(MessagePoliceForce.class);
        for (CommunicationMessage tmp : pfmessages)
        {
            MessagePoliceForce message = (MessagePoliceForce)tmp;
            MessageUtil.reflectMessage(this.worldInfo, message);

            final EntityID id = message.getAgentID();
            Human pf = (Human)this.worldInfo.getEntity(id);
            pf.undefineX();
            pf.undefineY();
        }

        final Collection<CommunicationMessage> atmessages =
            mm.getReceivedMessageList(MessageAmbulanceTeam.class);
        for (CommunicationMessage tmp : atmessages)
        {
            MessageAmbulanceTeam message = (MessageAmbulanceTeam)tmp;
            MessageUtil.reflectMessage(this.worldInfo, message);

            final EntityID id = message.getAgentID();
            this.received.add(id);
            Human at = (Human)this.worldInfo.getEntity(id);
            at.undefineX();
            at.undefineY();
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

        final Stream<EntityID> tmp = this.worldInfo.getEntitiesOfType(CIVILIAN)
            .stream()
            .map(Human.class::cast)
            .filter(h ->
                h.isPositionDefined() &&
                this.worldInfo.getEntity(h.getPosition())
                    .getStandardURN() == BUILDING)
            .filter(h -> h.isDamageDefined() && h.getDamage() > 0)
            .map(StandardEntity::getID);
        this.tasks.addAll(tmp.collect(toSet()));

        //final StandardEntityURN[] agentURNs =
        //    {FIRE_BRIGADE, POLICE_FORCE, AMBULANCE_TEAM};
        //this.worldInfo.getEntitiesOfType(agentURNs)
        //    .stream()
        //    .map(Human.class::cast)
        //    .filter(Human::isBuriednessDefined)
        //    .filter(h -> h.getBuriedness() > 0)
        //    .map(StandardEntity::getID)
        //    .map(this.tasks::add);

        this.tasks.removeAll(this.ignored);
        this.tasks.add(SEARCHING_TASK);
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
                    return MaxSumAmbulanceTargetAllocator
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
        for (EntityID vnodeid : vnodeids)
        {
            final List<EntityID> closer = fnodeids
                .stream()
                .sorted((i1, i2) -> {
                    if (i1.equals(SEARCHING_TASK)) return Integer.compare(0, 1);
                    if (i2.equals(SEARCHING_TASK)) return Integer.compare(1, 0);
                    final double d1 = this.worldInfo.getDistance(i1, vnodeid);
                    final double d2 = this.worldInfo.getDistance(i2, vnodeid);
                    return Double.compare(d1, d2);
                })
                .collect(toList());

            for (int i=0; i<Math.min(4, closer.size()); ++i)
            {
                final EntityID fnodeid = closer.get(i);
                WeightingFactor<EntityID> vnode =
                    (WeightingFactor<EntityID>)this.nodes.get(vnodeid);
                vnode.addNeighbor(fnodeid);

                Factor<EntityID> fnode = this.nodes.get(fnodeid);
                fnode.addNeighbor(vnodeid);

                final double penalty = this.computePenalty(vnodeid, fnodeid);
                vnode.setPotential(fnodeid, penalty);
            }
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

        final Civilian entity = (Civilian)this.worldInfo.getEntity(task);
        if (nAgents == 0) return PENALTY;

        final int hp = entity.getHP();
        final int damage = entity.getDamage();
        int buriedness = 0;
        if (entity.isBuriednessDefined()) buriedness = entity.getBuriedness();

        final double remaining  = (double)hp / (double)damage;
        final double nRequested = (double)buriedness / remaining;

        final double nLeasts = Math.ceil(nRequested+1.0);
        final double ratio = Math.min((double)nAgents, nLeasts) / nLeasts;

        return PENALTY * (1.0-Math.pow(ratio, 2.0));
    }
}
