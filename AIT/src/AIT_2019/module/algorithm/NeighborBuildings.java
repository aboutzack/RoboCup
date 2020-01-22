package AIT_2019.module.algorithm;

import adf.component.module.algorithm.*;
import adf.agent.info.*;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import rescuecore2.standard.entities.*;
import static rescuecore2.standard.entities.StandardEntityURN.*;
import rescuecore2.worldmodel.EntityID;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

public class NeighborBuildings extends StaticClustering
{
    private final Map<EntityID, Set<EntityID>> neighbors = new HashMap<>();
    private final static double SCALE = 1.5;

    public NeighborBuildings(
        AgentInfo ai, WorldInfo wi, ScenarioInfo si,
        ModuleManager mm, DevelopData dd)
    {
        super(ai, wi, si, mm, dd);
    }

    @Override
    public Clustering calc()
    {
        final Collection<EntityID> buildings =
            this.worldInfo.getEntityIDsOfType(BUILDING);

        this.neighbors.clear();
        for (EntityID id : buildings)
        {
            final Rectangle rect = this.toRect(id);
            final int range = (int)(Math.max(rect.width, rect.height)*SCALE);

            final Stream<EntityID> tmp =
                this.worldInfo.getObjectIDsInRange(id, range)
                    .stream()
                    .filter(buildings::contains);
            this.neighbors.put(id, tmp.collect(toSet()));
        }

        return this;
    }

    @Override
    public Clustering precompute(PrecomputeData pd)
    {
        super.precompute(pd);
        if(this.getCountPrecompute() >= 2) return this;
        return this;
    }

    @Override
    public Clustering resume(PrecomputeData precomputeData)
    {
        super.resume(precomputeData);
        if(this.getCountResume() >= 2) return this;
        this.calc();
        return this;
    }

    @Override
    public Clustering preparate()
    {
        super.preparate();
        if(this.getCountPreparate() >= 2) return this;
        this.calc();
        return this;
    }

    @Override
    public int getClusterNumber()
    {
        return this.neighbors.size();
    }

    @Override
    public int getClusterIndex(StandardEntity entity)
    {
        return this.getClusterIndex(entity.getID());
    }

    @Override
    public int getClusterIndex(EntityID id)
    {
        return id.getValue();
    }

    @Override
    public Collection<StandardEntity> getClusterEntities(int i)
    {
        final Stream<StandardEntity> tmp = this.getClusterEntityIDs(i)
            .stream()
            .map(this.worldInfo::getEntity);

        return tmp.collect(toList());
    }

    @Override
    public Collection<EntityID> getClusterEntityIDs(int i)
    {
        return this.neighbors.get(new EntityID(i));
    }

    private Rectangle toRect(EntityID id)
    {
        final Area area = (Area)this.worldInfo.getEntity(id);
        return area.getShape().getBounds();
    }
}
