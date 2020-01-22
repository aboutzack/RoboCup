package AIT_2019.module.algorithm;

import adf.agent.info.*;
import adf.agent.develop.DevelopData;
import adf.agent.precompute.PrecomputeData;
import adf.agent.module.ModuleManager;
import adf.agent.communication.MessageManager;
import adf.component.module.algorithm.*;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import java.util.*;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

public class KmeansClustering extends DynamicClustering
{
    private StandardEntityURN urn;
    private KmeansPP kmeans;
    private Map<EntityID, Integer> assigns = new HashMap<>();

    private static final int KMEANS_REPEATS_PRECOMPUTE = 20;
    private static final int KMEANS_REPEATS_PREPARE = 10;

    public KmeansClustering(
        AgentInfo ai, WorldInfo wi, ScenarioInfo si,
        ModuleManager mm, DevelopData dd)
    {
        super(ai, wi, si, mm, dd);
        this.urn = this.agentInfo.me().getStandardURN();
    }

    private static final String MODULE_NAME =
        "AIT_2019.module.algorithm.KmeansClustering";
    private static final String PD_CLUSTER_N = MODULE_NAME + ".n";
    private static final String PD_CLUSTER_X = MODULE_NAME + ".x";
    private static final String PD_CLUSTER_Y = MODULE_NAME + ".y";
    private static final String PD_CLUSTER_M = MODULE_NAME + ".m";

    @Override
    public Clustering precompute(PrecomputeData pd)
    {
        super.precompute(pd);
        if (this.getCountPrecompute() > 1) return this;

        this.initKmeans(KMEANS_REPEATS_PRECOMPUTE);

        final int n = this.kmeans.getClusterNumber();
        pd.setInteger(this.addURNSuffix(PD_CLUSTER_N), n);

        for (int i=0; i<n; ++i)
        {
            final double x = this.kmeans.getClusterX(i);
            final double y = this.kmeans.getClusterY(i);
            final StandardEntity[] members = this.kmeans.getClusterMembers(i);

            final Stream<EntityID> ids =
                Arrays.stream(members).map(StandardEntity::getID);

            pd.setEntityIDList(
                this.addURNISuffix(PD_CLUSTER_M, i), ids.collect(toList()));
            pd.setDouble(this.addURNISuffix(PD_CLUSTER_X, i), x);
            pd.setDouble(this.addURNISuffix(PD_CLUSTER_Y, i), y);
        }
        return this;
    }

    @Override
    public Clustering resume(PrecomputeData pd)
    {
        super.resume(pd);
        if (this.getCountResume() > 1) return this;

        final int n = pd.getInteger(this.addURNSuffix(PD_CLUSTER_N));
        double[] xs = new double[n];
        double[] ys = new double[n];
        List<List<StandardEntity>> memberz = new LinkedList<>();

        for (int i=0; i<n; ++i)
        {
            final List<EntityID> ids = pd.getEntityIDList(
                this.addURNISuffix(PD_CLUSTER_M, i));
            final Stream<StandardEntity> members =
                ids.stream().map(this.worldInfo::getEntity);

            memberz.add(members.collect(toList()));
            xs[i] = pd.getDouble(this.addURNISuffix(PD_CLUSTER_X, i));
            ys[i] = pd.getDouble(this.addURNISuffix(PD_CLUSTER_Y, i));
        }

        this.kmeans = new KmeansPP(n, xs, ys, memberz);
        return this;
    }

    @Override
    public Clustering preparate()
    {
        super.preparate();
        if (this.getCountPreparate() > 1) return this;

        this.initKmeans(KMEANS_REPEATS_PREPARE);
        return this;
    }

    @Override
    public Clustering updateInfo(MessageManager mm)
    {
        super.updateInfo(mm);
        return this;
    }

    @Override
    public int getClusterNumber()
    {
        return this.kmeans.getClusterNumber();
    }

    @Override
    public int getClusterIndex(StandardEntity entity)
    {
        return this.getClusterIndex(entity.getID());
    }

    @Override
    public int getClusterIndex(EntityID id)
    {
        if (!this.assigns.containsKey(id)) return -1;
        return this.assigns.get(id);
    }

    @Override
    public Collection<StandardEntity> getClusterEntities(int i)
    {
        if (i < 0) return null;
        if (i >= this.kmeans.getClusterNumber()) return null;

        final StandardEntity[] entities = this.kmeans.getClusterMembers(i);
        return Arrays.asList(entities);
    }

    @Override
    public Collection<EntityID> getClusterEntityIDs(int i)
    {
        final Collection<StandardEntity> entities = this.getClusterEntities(i);
        if (entities == null) return null;

        final Stream<EntityID> ids =
            entities.stream().map(StandardEntity::getID);
        return ids.collect(toSet());
    }

    @Override
    public Clustering calc()
    {
        if (!this.assigns.isEmpty()) return this;

        final int time = this.agentInfo.getTime();
        if (time < 1) return this;

        this.makePair();

        return this;
    }

    private void initKmeans(int repeats)
    {
        int n = 0;
        switch (this.urn)
        {
            case FIRE_BRIGADE:
                n = this.scenarioInfo.getScenarioAgentsFb();
                break;
            case POLICE_FORCE:
                n = this.scenarioInfo.getScenarioAgentsPf();
                break;
            case AMBULANCE_TEAM:
                n = this.scenarioInfo.getScenarioAgentsAt();
                break;
        }

        final Stream<StandardEntity> tmp =
            this.worldInfo.getAllEntities()
                .stream().filter(Area.class::isInstance);
        final StandardEntity[] entities = tmp.toArray(StandardEntity[]::new);

        this.kmeans = new KmeansPP(entities, n, repeats);
        this.kmeans.execute();
    }

    private void makePair()
    {
        final List<Human> agents =
            this.worldInfo.getEntitiesOfType(this.urn)
                .stream()
                .map(Human.class::cast)
                .sorted((h1, h2) -> {
                    final int v1 = h1.getID().getValue();
                    final int v2 = h2.getID().getValue();
                    return Integer.compare(v1, v2);
                })
                .collect(toList());
        final int n = agents.size();

        int[][] costs = new int[n][n];
        for (int row=0; row<n; ++row)
        {
            final double cx = this.kmeans.getClusterX(row);
            final double cy = this.kmeans.getClusterY(row);

            for (int col=0; col<n; ++col)
            {
                final Human agent = agents.get(col);

                final double ax = (double)agent.getX();
                final double ay = (double)agent.getY();
                costs[row][col] = (int)Math.hypot(cx-ax, cy-ay);
            }
        }

        boolean[][] result = Hungarian.execute(costs);
        for (int row=0; row<n; ++row)
        {
            final int col = seek(result[row]);
            final Human agent = agents.get(col);
            this.assigns.put(agent.getID(), row);
        }
    }

    private static int seek(boolean[] array)
    {
        final int n = array.length;
        for (int i=0; i<n; ++i) if (array[i]) return i;

        return -1;
    }

    private String addURNSuffix(String path)
    {
        return path + "." + this.urn;
    }

    private String addURNISuffix(String path, int i)
    {
        return path + "." + this.urn + "." + i;
    }
}
