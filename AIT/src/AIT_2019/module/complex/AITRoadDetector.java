package AIT_2019.module.complex;

import adf.component.module.complex.RoadDetector;
import adf.component.module.algorithm.*;
import adf.component.extaction.ExtAction;
import adf.component.communication.CommunicationMessage;
import adf.agent.info.*;
import adf.agent.module.ModuleManager;
import adf.agent.develop.DevelopData;
import adf.agent.precompute.PrecomputeData;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.centralized.*;
import adf.agent.communication.standard.bundle.information.*;
import adf.agent.action.Action;
import adf.agent.action.common.*;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.*;
import static rescuecore2.standard.entities.StandardEntityURN.*;
import java.awt.Polygon;
import java.util.*;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

//  @ DEBUG
import com.mrl.debugger.remote.VDClient;
//  @ END OF DEBUG

public class AITRoadDetector extends RoadDetector
{
    private EntityID result = null;

    private Random random = new Random();

    private ExtAction extaction;
    private PathPlanning pathPlanning;
    private Clustering clustering;
    private Clustering stuckedHumans;

    private Set<EntityID> completed = new HashSet<>();
    private Set<EntityID> cluster = new HashSet<>();
    private Set<EntityID> requests = new HashSet<>();
    private Set<EntityID> buildings = new HashSet<>();

    //  @ DEBUG
    //  private VDClient vdclient = VDClient.getInstance();
    //  @ END OF DEBUG

    public AITRoadDetector(
        AgentInfo ai, WorldInfo wi, ScenarioInfo si,
        ModuleManager mm, DevelopData dd)
    {
        super(ai, wi, si, mm, dd);

        this.extaction = mm.getExtAction(
            "TacticsPoliceForce.ActionExtClear",
            "adf.sample.extaction.ActionExtClear");

        this.pathPlanning = mm.getModule(
            "SampleRoadDetector.PathPlanning",
            "adf.sample.module.algorithm.SamplePathPlanning");
        this.registerModule(this.pathPlanning);

        this.clustering = mm.getModule(
            "SampleRoadDetector.Clustering",
            "adf.sample.module.algorithm.SampleKMeans");
        this.registerModule(this.clustering);

        this.stuckedHumans = mm.getModule(
            "AITRoadDetector.StuckedHumans",
            "AIT_2019.module.algorithm.StuckedHumans");
        this.registerModule(this.stuckedHumans);

        //  @ DEBUG
        //  this.vdclient.init("localhost", 1099);
        //  @ END OF DEBUG
    }

    @Override
    public RoadDetector calc()
    {
        while (this.result == null)
        {
            this.result = this.selectInRequests();
            if (this.result == null) break;
            if (this.haveCurrentTaskCompleted())
            {
                this.completed.add(this.result);
                this.result = null;
            }
        }

        while (this.result == null)
        {
            this.result = this.selectInBuildings();
            if (this.result == null) break;
            if (this.haveCurrentTaskCompleted())
            {
                this.completed.add(this.result);
                this.result = null;
            }
        }

        while (this.result == null)
        {
            this.result = this.selectInCluster();
            if (this.result == null) break;
            if (this.haveCurrentTaskCompleted())
            {
                this.completed.add(this.result);
                this.result = null;
            }
        }

        if (this.result == null)
        {
            final int n = this.cluster.size();
            this.gainAdditionalCandidates();
            if (n != this.cluster.size()) this.calc();
        }

//        System.out.println("[POLICE] ROAD-DETECTOR -> " + this.result);
        return this;
    }

    @Override
    public EntityID getTarget()
    {
        return this.result;
    }

    @Override
    public RoadDetector precompute(PrecomputeData pd)
    {
        super.precompute(pd);
        this.extaction.precompute(pd);
        return this;
    }

    @Override
    public RoadDetector resume(PrecomputeData pd)
    {
        super.resume(pd);
        this.extaction.resume(pd);
        return this;
    }

    @Override
    public RoadDetector preparate()
    {
        super.preparate();
        this.extaction.preparate();
        return this;
    }

    @Override
    public RoadDetector updateInfo(MessageManager mm)
    {
        super.updateInfo(mm);
        this.extaction.updateInfo(mm);

        if (this.cluster.isEmpty())
        {
            this.initCluster();
            this.initBuildingsInCluster();
            this.initRequestsWithRefugesInCluster();
        }

        if (this.result != null & this.haveCurrentTaskCompleted())
        {
            this.completed.add(this.result);
            this.result = null;
        }

        this.addRequestsWithPercept();

        final List<CommunicationMessage> cmdpols =
            mm.getReceivedMessageList(CommandPolice.class);
        for (CommunicationMessage tmp : cmdpols)
        {
            final CommandPolice message = (CommandPolice)tmp;
            this.handleMessage(message);
        }

        final List<CommunicationMessage> messagefbs =
            mm.getReceivedMessageList(MessageFireBrigade.class);
        for (CommunicationMessage tmp : messagefbs)
        {
            final MessageFireBrigade message = (MessageFireBrigade)tmp;
            this.handleMessage(message);
        }

        return this;
    }

    private void initCluster()
    {
        this.clustering.calc();
        final EntityID me = this.agentInfo.getID();
        final int idx = this.clustering.getClusterIndex(me);
        final Collection<EntityID> tmp =
            this.clustering.getClusterEntityIDs(idx);
        this.cluster.addAll(tmp);
    }

    private void gainAdditionalCandidates()
    {
        final int n = this.clustering.getClusterNumber();

        EntityID[] samples = new EntityID[n];
        for (int i=0; i<n; ++i)
        {
            final Collection<EntityID> tmp =
                this.clustering.getClusterEntityIDs(i);
            if (tmp == null || tmp.isEmpty()) continue;
            samples[i] = tmp.iterator().next();
        }

        final int idx = this.clustering.getClusterIndex(this.agentInfo.getID());
        final EntityID primal = samples[idx];
        if (primal == null) return;

        int expander = -1;
        double mind = Double.POSITIVE_INFINITY;
        for (int i=0; i<n; ++i)
        {
            if (samples[i] == null) continue;
            if (i == idx) continue;

            final double d = this.worldInfo.getDistance(primal, samples[i]);
            if (d < mind) { expander = i ; mind = d; }
        }

        if (expander == -1) return;
        this.cluster.addAll(this.clustering.getClusterEntityIDs(expander));
    }

    private void initBuildingsInCluster()
    {
        final Stream<EntityID> tmp = this.cluster
            .stream()
            .map(this.worldInfo::getEntity)
            .filter(Building.class::isInstance)
            .map(StandardEntity::getID);
        this.buildings.addAll(tmp.collect(toSet()));
    }

    private void initRequestsWithRefugesInCluster()
    {
        final Stream<EntityID> tmp = this.cluster
            .stream()
            .map(this.worldInfo::getEntity)
            .filter(Refuge.class::isInstance)
            .map(StandardEntity::getID);
        this.requests.addAll(tmp.collect(toSet()));
    }

    private boolean haveCurrentTaskCompleted()
    {
        if (this.needIdle()) return false;

        this.extaction.setTarget(this.result);
        this.extaction.calc();
        final Action action = this.extaction.getAction();
        return this.isEmptyAction(action);
    }

    private boolean isEmptyAction(Action action)
    {
        if (action == null) return true;
        if (action instanceof ActionRest) return true;
        if (action instanceof ActionMove)
        {
            final ActionMove move = (ActionMove)action;
            final int ax = (int)this.agentInfo.getX();
            final int ay = (int)this.agentInfo.getY();
            final int mx = move.getPosX();
            final int my = move.getPosY();
            return ax == mx && ay == my;
        }
        return false;
    }

    private boolean needIdle()
    {
        final int time = this.agentInfo.getTime();
        final int ignored = this.scenarioInfo.getKernelAgentsIgnoreuntil();
        return time < ignored;
    }

    private void addRequestsWithPercept()
    {
        final EntityID me = this.agentInfo.getID();
        final Set<EntityID> changes =
            this.worldInfo.getChanged().getChangedEntities();

        this.stuckedHumans.calc();
        final Collection<EntityID> stuckeds =
            this.stuckedHumans.getClusterEntityIDs(0);
        for (EntityID id : changes)
        {
            final StandardEntity entity = this.worldInfo.getEntity(id);
            if (entity.getStandardURN() == POLICE_FORCE) continue;
            if (!Human.class.isInstance(entity)) continue;

            if (!stuckeds.contains(id)) continue;

            final Human human = (Human)entity;
            if (!human.isPositionDefined()) continue;
            final EntityID position = human.getPosition();
            //  @ NOTE: need to refactor
            if (!this.cluster.contains(position)) continue;
            if (!this.completed.contains(position))
            {
                if (!this.requests.contains(position)) this.result = null;
                this.requests.add(position);
            }
        }
    }

    private void handleMessage(CommandPolice command)
    {
        if (command.getAction() != CommandPolice.ACTION_CLEAR) return;
        if (!command.isTargetIDDefined()) return;

        final EntityID target = command.getTargetID();
        if (this.cluster.contains(target))
        {
            this.requests.add(target);
            this.completed.remove(target);
            this.result = null;
        }
    }

    private void handleMessage(MessageFireBrigade message)
    {
        if (message.getAction() != MessageFireBrigade.ACTION_MOVE) return;
        if (!message.isTargetDefined()) return;

        final EntityID target = message.getTargetID();
        if (this.cluster.contains(target) && !this.completed.contains(target))
        {
            this.requests.add(target);
            this.result = null;
        }
    }

    private EntityID selectInRequests()
    {
        final EntityID me = this.agentInfo.getID();
        final Optional<EntityID> ret = this.requests
            .stream()
            .filter(this.cluster::contains)
            .filter(i -> !this.completed.contains(i))
            .min((i1, i2) -> {
                final double d1 = this.worldInfo.getDistance(me, i1);
                final double d2 = this.worldInfo.getDistance(me, i2);
                return Double.compare(d1, d2);
            });
        return ret.orElse(null);
    }

    private EntityID selectInBuildings()
    {
        final EntityID me = this.agentInfo.getID();
        final Optional<EntityID> ret = this.buildings
            .stream()
            .filter(i -> !this.completed.contains(i))
            .min((i1, i2) -> {
                final double d1 = this.worldInfo.getDistance(me, i1);
                final double d2 = this.worldInfo.getDistance(me, i2);
                return Double.compare(d1, d2);
            });
        return ret.orElse(null);
    }

    private EntityID selectInCluster()
    {
        final EntityID me = this.agentInfo.getID();
        final Optional<EntityID> ret = this.cluster
            .stream()
            .filter(i -> !this.completed.contains(i))
            .max((i1, i2) -> {
                final double d1 = this.worldInfo.getDistance(me, i1);
                final double d2 = this.worldInfo.getDistance(me, i2);
                return Double.compare(d1, d2);
            });
        return ret.orElse(null);
    }
}
