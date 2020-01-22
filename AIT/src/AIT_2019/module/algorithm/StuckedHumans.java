package AIT_2019.module.algorithm;

import adf.component.module.algorithm.*;
import adf.agent.info.*;
import adf.agent.module.ModuleManager;
import adf.agent.develop.DevelopData;
import adf.agent.precompute.PrecomputeData;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.centralized.*;
import adf.agent.communication.standard.bundle.information.*;
import rescuecore2.worldmodel.*;
import rescuecore2.standard.entities.*;
import static rescuecore2.standard.entities.StandardEntityURN.*;
import rescuecore2.misc.geometry.*;
import java.awt.geom.Ellipse2D;
import java.util.*;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

public class StuckedHumans extends DynamicClustering
{
    private Set<EntityID> stuckedHumans = new HashSet<>();
    private Set<EntityID> updates = new HashSet<>();
    private Set<EntityID> cannotTracks = new HashSet<>();

    public StuckedHumans(
        AgentInfo ai, WorldInfo wi, ScenarioInfo si,
        ModuleManager mm, DevelopData dd)
    {
        super(ai, wi, si, mm, dd);
    }

    @Override
    public Clustering precompute(PrecomputeData pd)
    {
        super.precompute(pd);
        return this;
    }

    @Override
    public Clustering resume(PrecomputeData pd)
    {
        super.resume(pd);
        return this;
    }

    @Override
    public Clustering preparate()
    {
        super.preparate();
        return this;
    }

    @Override
    public Clustering updateInfo(MessageManager mm)
    {
        super.updateInfo(mm);
        if (this.getCountUpdateInfo() >= 2) return this;

        final ChangeSet changes = this.worldInfo.getChanged();
        for (EntityID id : changes.getChangedEntities())
        {
            StandardEntity entity = this.worldInfo.getEntity(id);
            if (entity instanceof Human)
            {
                this.updates.add(id);
                this.cannotTracks.remove(id);
            }
        }

        final EntityID position = this.agentInfo.getPosition();
        final Collection<EntityID> humans =
            this.worldInfo.getEntityIDsOfType(CIVILIAN);
        for (EntityID human : humans)
        {
            final Human entity = (Human)this.worldInfo.getEntity(human);
            if (!entity.isPositionDefined()) continue;
            if (!entity.getPosition().equals(position)) continue;
            if (changes.getChangedEntities().contains(human)) continue;
            this.cannotTracks.add(human);
        }

        return this;
    }

    @Override
    public int getClusterNumber()
    {
        return 1;
    }

    @Override
    public int getClusterIndex(StandardEntity entity)
    {
        return this.getClusterIndex(entity.getID());
    }

    @Override
    public int getClusterIndex(EntityID id)
    {
        return this.stuckedHumans.contains(id) ? 0 : -1;
    }

    @Override
    public Collection<EntityID> getClusterEntityIDs(int i)
    {
        return i == 0 ? new HashSet<>(this.stuckedHumans) : null;
    }

    @Override
    public Collection<StandardEntity> getClusterEntities(int i)
    {
        final Collection<EntityID> ids = this.getClusterEntityIDs(i);
        if (ids == null) return null;

        final Stream<StandardEntity> entities = ids
            .stream().map(this.worldInfo::getEntity);
        return entities.collect(toList());
    }

    @Override
    public Clustering calc()
    {
        for (EntityID id : this.updates)
        {
            final Human human = (Human)this.worldInfo.getEntity(id);
            if (this.isHumanStucked(human)) this.stuckedHumans.add(id);
            else this.stuckedHumans.remove(id);
        }

        this.stuckedHumans.removeAll(this.cannotTracks);

        this.updates.clear();
        return this;
    }

    private static final double AGENT_RADIUS = 500.0;
    private static final double CIVILIAN_RADIUS = 300.0;

    private boolean isHumanStucked(Human human)
    {
        final EntityID position = human.getPosition();
        final StandardEntity entity = this.worldInfo.getEntity(position);
        if (!Area.class.isInstance(entity)) return false;
        final Area area = (Area)entity;

        if (!area.isBlockadesDefined()) return false;
        final Optional<java.awt.geom.Area> obstacle = area.getBlockades()
            .stream()
            .map(this.worldInfo::getEntity)
            .map(Blockade.class::cast)
            .map(Blockade::getShape)
            .map(java.awt.geom.Area::new)
            .reduce((acc, v) -> { acc.add(v); return acc; });
        if (!obstacle.isPresent()) return false;

        final Point2D point = getPoint(human);
        final double rad = human.getStandardURN() == CIVILIAN
            ? CIVILIAN_RADIUS : AGENT_RADIUS;
        final java.awt.geom.Area shape = makeAWTArea(point, rad);

        shape.intersect(obstacle.get());
        return !shape.isEmpty();
    }

    public static Point2D getPoint(Human human)
    {
        final double x = human.getX();
        final double y = human.getY();
        return new Point2D(x, y);
    }

    public static java.awt.geom.Area makeAWTArea(Point2D p, double rad)
    {
        final double d = rad * 2.0;
        final double x = p.getX() - rad;
        final double y = p.getY() - rad;
        return new java.awt.geom.Area(new Ellipse2D.Double(x, y, d, d));
    }
}
