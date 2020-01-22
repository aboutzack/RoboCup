package AIT_2019.extaction;

import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.*;
import adf.agent.info.*;
import adf.agent.module.ModuleManager;
import adf.agent.develop.DevelopData;
import adf.agent.precompute.PrecomputeData;
import adf.agent.communication.MessageManager;
import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.action.police.ActionClear;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.*;
import static rescuecore2.standard.entities.StandardEntityURN.*;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.*;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.awt.geom.Ellipse2D;
import java.util.*;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

//  @ DEBUG
import com.mrl.debugger.remote.VDClient;
//  @ END OF DEBUG

public class AITActionExtClear extends ExtAction
{
    private EntityID target;
    private Map<EntityID, Action> cache = new HashMap<>();

    private PathPlanning pathPlanning;
    private Map<EntityID, List<Line2D>> concretePath;
    private List<EntityID> path;
    private Point2D pointOnMove;
    private boolean needToEscape = true;
    private boolean needToShrink = true;

    private Clustering stuckedHumans;

    //  @ DEBUG
    //  private VDClient vdclient = VDClient.getInstance();
    //  @ END OF DEBUG

    public AITActionExtClear(
        AgentInfo ai, WorldInfo wi, ScenarioInfo si,
        ModuleManager mm, DevelopData dd)
    {
        super(ai, wi, si, mm, dd);

        this.pathPlanning = mm.getModule(
            "ActionExtClear.PathPlanning",
            "adf.sample.module.algorithm.SamplePathPlanning");

        this.stuckedHumans = mm.getModule(
            "AITActionExtClear.StuckedHumans",
            "AIT_2019.module.algorithm.StuckedHumans");

        //  @ DEBUG
        //  this.vdclient.init("localhost", 1099);
        //  @ END OF DEBUG
    }

    @Override
    public ExtAction precompute(PrecomputeData pd)
    {
        super.precompute(pd);
        this.pathPlanning.precompute(pd);
        this.stuckedHumans.precompute(pd);
        return this;
    }

    @Override
    public ExtAction resume(PrecomputeData pd)
    {
        super.resume(pd);
        this.pathPlanning.resume(pd);
        this.stuckedHumans.resume(pd);
        return this;
    }

    @Override
    public ExtAction preparate()
    {
        super.preparate();
        this.pathPlanning.preparate();
        this.stuckedHumans.preparate();
        return this;
    }

    @Override
    public ExtAction updateInfo(MessageManager mm)
    {
        super.updateInfo(mm);
        if (this.getCountUpdateInfo() >= 2) return this;
        this.pathPlanning.updateInfo(mm);
        this.stuckedHumans.updateInfo(mm);

        this.target = null;
        this.cache.clear();

        if (this.needIdle()) return this;

        this.needToEscape = !this.isActionMoveSucceeded();
        this.needToShrink &= this.isStucked();

        return this;
    }

    @Override
    public ExtAction setTarget(EntityID id)
    {
        this.target = id;
        return this;
    }

    @Override
    public ExtAction calc()
    {
        if (this.target == null) return this;
        if (this.cache.containsKey(this.target))
        {
            this.result = this.cache.get(this.target);
            return this;
        }

        final EntityID position = this.agentInfo.getPosition();
        if (this.needToShrink)
        {
            this.result = this.makeActionToClear(position);
            this.cache.put(this.target, this.result);
            return this;
        }

        if (this.needToEscape)
        {
            this.result = this.makeActionToAvoidError();
            this.cache.put(this.target, this.result);
            return this;
        }

        this.result = null;

        if (this.needIdle()) return this;
        if (this.needRest()) this.target = this.seekBestRefuge();
        if (this.target == null) return this;

        this.pathPlanning.setFrom(position);
        this.pathPlanning.setDestination(this.target);
        this.pathPlanning.calc();
        this.path = this.normalize(this.pathPlanning.getResult());

        this.concretePath = this.makeConcretePath(this.path);
        for (EntityID id : this.path)
        {
            final List<Line2D> concrete = this.concretePath.get(id);
            final List<Line2D> addition =
                this.seekConcretePathToStuckedHumans(id, concrete);
            this.concretePath.get(id).addAll(addition);
        }

        List<EntityID> actualPath = new LinkedList<>();
        for (EntityID id : this.path)
        {
            actualPath.add(id);
            final List<Line2D> concrete = this.concretePath.get(id);
            final Line2D clearline = this.seekClearLine(id, concrete);
            if (clearline == null) continue;

            this.result = this.makeActionToClear(actualPath, clearline);
            this.cache.put(this.target, this.result);
            return this;
        }

        this.result = this.makeActionToMove(this.path);
        if (this.isCompleted()) this.result = null;
        this.cache.put(this.target, this.result);
        return this;
    }

    private boolean isCompleted()
    {
        final EntityID position = this.agentInfo.getPosition();
        final List<EntityID> nbpath = this.makePathNoBuilding(this.path);
        final int n = nbpath.size();

        return nbpath.isEmpty() || nbpath.get(n-1).equals(position);
    }

    private boolean needIdle()
    {
        final int time = this.agentInfo.getTime();
        final int ignored = this.scenarioInfo.getKernelAgentsIgnoreuntil();
        return time < ignored;
    }

    private static final int DAMAGE_NEEDED_REST = 100;
    private boolean needRest()
    {
        final PoliceForce me = (PoliceForce)this.agentInfo.me();
        final int hp = me.getHP();
        final int damage = me.getDamage();

        if (hp == 0) return false;
        if (damage == 0) return false;

        final int time = this.agentInfo.getTime();
        final int die = (int)Math.ceil((double)hp/damage);
        final int finish = 300;

        return damage >= DAMAGE_NEEDED_REST || (time + die) < finish;
    }

    private EntityID seekBestRefuge()
    {
        final EntityID me = this.agentInfo.getID();
        final Optional<EntityID> ret = this.worldInfo.getEntityIDsOfType(REFUGE)
                .stream()
                .min((r1, r2) -> {
                    final double d1 =
                        this.worldInfo.getDistance(me, r1) +
                        this.worldInfo.getDistance(r1, this.target);
                    final double d2 =
                        this.worldInfo.getDistance(me, r2) +
                        this.worldInfo.getDistance(r2, this.target);
                    return Double.compare(d1, d2);
                });

        return ret.orElse(null);
    }

    private List<EntityID> normalize(List<EntityID> path)
    {
        List<EntityID> ret = new ArrayList<>(path);

        final PoliceForce me = (PoliceForce)this.agentInfo.me();
        final EntityID position = me.getPosition();
        if (ret.isEmpty() || !ret.get(0).equals(position)) ret.add(0, position);

        return ret;
    }

    private Map<EntityID, List<Line2D>> makeConcretePath(List<EntityID> path)
    {
        final int n = path.size();
        final Area area = (Area)this.worldInfo.getEntity(path.get(n-1));
        final Point2D centroid = getPoint(area);
        return this.makeConcretePath(path, centroid);
    }

    private Map<EntityID, List<Line2D>> makeConcretePath(
        List<EntityID> path, Point2D dest)
    {
        Map<EntityID, List<Line2D>> ret = new HashMap<>();

        final int n = path.size();
        final EntityID s = path.get(0);
        final EntityID g = path.get(n-1);

        if (n == 1)
        {
            final List<Line2D> concrete =
                this.makeConcretePath(s, this.getPoint(), dest);
            ret.put(s, this.cut(concrete));
            return ret;
        }

        for (int i=1; i<n-1; ++i)
        {
            final EntityID id = path.get(i);
            final EntityID prev = path.get(i-1);
            final EntityID next = path.get(i+1);
            final List<Line2D> concrete = this.makeConcretePath(id, prev, next);
            ret.put(id, this.cut(concrete));
        }

        List<Line2D> concrete =
            this.makeConcretePath(s, this.getPoint(), path.get(1));
        ret.put(s, this.cut(concrete));

        concrete = this.makeConcretePath(g, path.get(n-2), dest);
        ret.put(g, this.cut(concrete));

        return ret;
    }

    private List<Line2D> makeConcretePath(
        EntityID id, EntityID prev, EntityID next)
    {
        final Area area = (Area)this.worldInfo.getEntity(id);
        final Edge pe = area.getEdgeTo(prev);
        final Edge ne = area.getEdgeTo(next);

        final Point2D centroid = getPoint(area);

        List<Line2D> ret = new ArrayList<>(2);
        ret.add(new Line2D(computeMiddlePoint(pe.getLine()), centroid));
        ret.add(new Line2D(centroid, computeMiddlePoint(ne.getLine())));
        return ret;
    }

    private List<Line2D> makeConcretePath(
        EntityID id, Point2D from, Point2D dest)
    {
        final Area area = (Area)this.worldInfo.getEntity(id);
        final Point2D centroid = getPoint(area);

        List<Line2D> ret = new LinkedList<>();
        ret.add(new Line2D(from, centroid));
        ret.addAll(this.makeConcretePathToAllNeighbor(id, null));
        return ret;
    }

    private List<Line2D> makeConcretePath(
        EntityID id, Point2D from, EntityID next)
    {
        final Area area = (Area)this.worldInfo.getEntity(id);
        final Point2D centroid = getPoint(area);
        final Edge ne = area.getEdgeTo(next);
        final Point2D np = computeMiddlePoint(ne.getLine());

        final Line2D cn = new Line2D(centroid, np);
        final Point2D closest =
            GeometryTools2D.getClosestPointOnSegment(cn, from);

        List<Line2D> ret = new LinkedList<>();
        if (closest.equals(centroid))
        {
            ret.add(new Line2D(from, centroid));
            ret.add(cn);
        }
        else
        {
            ret.add(new Line2D(from, np));
        }
        return ret;
    }

    private List<Line2D> makeConcretePath(
        EntityID id, EntityID prev, Point2D dest)
    {
        final Area area = (Area)this.worldInfo.getEntity(id);
        final Point2D centroid = getPoint(area);
        final Edge pe = area.getEdgeTo(prev);
        final Point2D pp = computeMiddlePoint(pe.getLine());

        List<Line2D> ret = new LinkedList<>();
        ret.add(new Line2D(pp, centroid));
        ret.addAll(this.makeConcretePathToAllNeighbor(id, prev));
        return ret;
    }

    private List<Line2D> makeConcretePathToAllNeighbor(
        EntityID id, EntityID ignored)
    {
        final Area area = (Area)this.worldInfo.getEntity(id);
        final Point2D centroid = getPoint(area);

        final List<Line2D> ret = new LinkedList<>();
        final List<EntityID> neighbors = area.getNeighbours();
        for (EntityID neighbor : neighbors)
        {
            if (neighbor.equals(ignored)) continue;
            final Edge ne = area.getEdgeTo(neighbor);
            final Point2D np = computeMiddlePoint(ne.getLine());
            ret.add(new Line2D(centroid, np));
        }
        return ret;
    }

    private List<Line2D> seekConcretePathToStuckedHumans(
        EntityID id, List<Line2D> others)
    {
        final List<Human> humans = this.seekStuckedHumansOn(id);
        List<Line2D> ret = new LinkedList<>();
        for (Human human : humans)
        {
            final Point2D point = getPoint(human);
            Point2D closest = computeClosestPoint(others, point);
            if (closest == null) closest = this.getPoint();
            ret.add(new Line2D(closest, point));
        }
        return this.cut(ret);
    }

    private List<Human> seekStuckedHumansOn(EntityID id)
    {
        this.stuckedHumans.calc();
        final Stream<Human> ret = this.stuckedHumans.getClusterEntities(0)
            .stream().map(Human.class::cast)
            .filter(h -> h.getStandardURN() != POLICE_FORCE)
            .filter(h -> h.getPosition().equals(id));
        return ret.collect(toList());
    }

    private List<Line2D> cut(List<Line2D> lines)
    {
        List<Line2D> ret = new LinkedList<>();
        for (Line2D line : lines)
        {
            final double l = line.getDirection().getLength();
            final double d = this.scenarioInfo.getClearRepairDistance()*0.3;
            final int n = (int)Math.ceil(l/d);

            for (int i=0; i<n; ++i)
            {
                final Point2D op = line.getPoint(d*i/l);
                final Point2D ep = line.getPoint(Math.min(d*(i+1)/l, 1.0));
                ret.add(new Line2D(op, ep));
            }
        }
        return ret;
    }

    private Line2D seekClearLine(EntityID id, List<Line2D> concrete)
    {
        final Area area = (Area)this.worldInfo.getEntity(id);
        if (!area.isBlockadesDefined()) return null;

        final List<EntityID> blockades = area.getBlockades();
        if (blockades.isEmpty()) return null;

        final Optional<java.awt.geom.Area> obstacle = blockades
            .stream()
            .map(this.worldInfo::getEntity)
            .map(Blockade.class::cast)
            .map(Blockade::getShape)
            .map(java.awt.geom.Area::new)
            .reduce((acc, v) -> { acc.add(v); return acc; });

        final int n = concrete.size();
        Line2D ret = null;
        int i;
        for (i=0; i<n; ++i)
        {
            java.awt.geom.Area shape = computeShape(concrete.get(i));
            shape.intersect(obstacle.get());

            if (!shape.isEmpty()) { ret = concrete.get(i); break; }
        }
        if (ret == null) return null;

        for (++i; i<n; ++i)
        {
            final Line2D next = concrete.get(i);
            if (!canUnite(ret, next)) break;
            ret = new Line2D(ret.getOrigin(), next.getEndPoint());
        }
        return ret;
    }

    private static final double AGENT_RADIUS = 500.0;
    private static java.awt.geom.Area computeShape(Line2D line)
    {
        final double x1 = line.getOrigin().getX();
        final double x2 = line.getEndPoint().getX();
        final double y1 = line.getOrigin().getY();
        final double y2 = line.getEndPoint().getY();

        final double length = Math.hypot(x2-x1, y2-y1);
        final double ldx = (y2-y1) * AGENT_RADIUS / length;
        final double ldy = (x1-x2) * AGENT_RADIUS / length;
        final double rdx = (y1-y2) * AGENT_RADIUS / length;
        final double rdy = (x2-x1) * AGENT_RADIUS / length;

        final Point2D p1 = new Point2D(x1+ldx, y1+ldy);
        final Point2D p2 = new Point2D(x2+ldx, y2+ldy);
        final Point2D p3 = new Point2D(x2+rdx, y2+rdy);
        final Point2D p4 = new Point2D(x1+rdx, y1+rdy);

        return makeAWTArea(new Point2D[]{p1, p2, p3, p4});
    }

    private Action makeActionToClear(List<EntityID> path, Line2D clearline)
    {
        final Point2D op = clearline.getOrigin();
        final Point2D ep = clearline.getEndPoint();

        final double d = GeometryTools2D.getDistance(this.getPoint(), op);

        final Vector2D vec = clearline.getDirection();
        final double l = vec.getLength();
        final Vector2D extvec = vec.normalised().scale(l+AGENT_RADIUS);
        final Action clear = new ActionClear(this.agentInfo, extvec);
        if (d <= AGENT_RADIUS) return clear;

        final int x = (int)op.getX();
        final int y = (int)op.getY();
        final Action move = new ActionMove(path, x, y);
        this.pointOnMove = this.getPoint();
        return move;
    }

    private Action makeActionToClear(EntityID id)
    {
        final Area area = (Area)this.worldInfo.getEntity(id);
        if (!area.isBlockadesDefined()) return null;
        final List<EntityID> blockades = area.getBlockades();

        final double rad = AGENT_RADIUS;
        final java.awt.geom.Area agent = makeAWTArea(this.getPoint(), rad);

        for (EntityID blockade : blockades)
        {
            final Blockade tmp = (Blockade)this.worldInfo.getEntity(blockade);
            java.awt.geom.Area shape = new java.awt.geom.Area(tmp.getShape());
            shape = (java.awt.geom.Area)shape.clone();
            shape.intersect(agent);
            if (!shape.isEmpty()) return new ActionClear(blockade);
        }

        return null;
    }

    private Action makeActionToMove(List<EntityID> path)
    {
        this.pointOnMove = this.getPoint();

        final List<EntityID> nbpath = this.makePathNoBuilding(path);
        return nbpath.isEmpty() ? null : new ActionMove(nbpath);
    }

    private List<EntityID> makePathNoBuilding(List<EntityID> path)
    {
        int i = path.size()-1;
        for (; i>=0; --i)
        {
            final StandardEntity entity = this.worldInfo.getEntity(path.get(i));
            if (entity.getStandardURN() != BUILDING) break;
        }
        return path.subList(0, i+1);
    }

    private Point2D getPoint()
    {
        final double x = this.agentInfo.getX();
        final double y = this.agentInfo.getY();
        return new Point2D(x, y);
    }

    private static Point2D getPoint(Area area)
    {
        final double x = area.getX();
        final double y = area.getY();
        return new Point2D(x, y);
    }

    private static Point2D getPoint(Human human)
    {
        final double x = human.getX();
        final double y = human.getY();
        return new Point2D(x, y);
    }

    private static Point2D computeMiddlePoint(Line2D line)
    {
        return line.getPoint(0.5);
    }

    private static Point2D computeClosestPoint(List<Line2D> lines, Point2D p)
    {
        final Optional<Point2D> ret = lines
            .stream()
            .map(l -> GeometryTools2D.getClosestPointOnSegment(l, p))
            .min((p1, p2) -> {
                final double d1 = GeometryTools2D.getDistance(p, p1);
                final double d2 = GeometryTools2D.getDistance(p, p2);
                return Double.compare(d1, d2);
            });

        return ret.orElse(null);
    }

    private static java.awt.geom.Area makeAWTArea(Point2D[] ps)
    {
        final int n = ps.length;

        Path2D path = new Path2D.Double();
        path.moveTo(ps[0].getX(), ps[0].getY());

        for (int i=1; i<n; ++i)
            path.lineTo(ps[i].getX(), ps[i].getY());

        path.closePath();
        return new java.awt.geom.Area(path);
    }

    public static java.awt.geom.Area makeAWTArea(Point2D p, double rad)
    {
        final double d = rad * 2.0;
        final double x = p.getX() - rad;
        final double y = p.getY() - rad;
        return new java.awt.geom.Area(new Ellipse2D.Double(x, y, d, d));
    }

    private static boolean canUnite(Line2D line1, Line2D line2)
    {
        if (!line1.getEndPoint().equals(line2.getOrigin())) return false;

        final Vector2D v1 = line1.getDirection().normalised();
        final Vector2D v2 = line2.getDirection().normalised();
        final boolean condx = GeometryTools2D.nearlyZero(v1.getX()-v2.getX());
        final boolean condy = GeometryTools2D.nearlyZero(v1.getY()-v2.getY());
        return condx && condy;
    }

    private boolean isActionMoveSucceeded()
    {
        if (!ActionMove.class.isInstance(this.result)) return true;

        final double d =
            GeometryTools2D.getDistance(this.getPoint(), this.pointOnMove);
        return d >= AGENT_RADIUS;
    }

    private Action makeActionToAvoidError()
    {
        for (EntityID id : this.path)
        {
            final List<Line2D> concrete = this.concretePath.get(id);
            if (concrete.isEmpty()) continue;

            final int d = this.scenarioInfo.getClearRepairDistance();
            final Vector2D vec = concrete.get(0).getDirection();
            final Vector2D extvec = vec.normalised().scale(d);
            return new ActionClear(this.agentInfo, extvec);
        }
        return null;
    }

    private boolean isStucked()
    {
        final EntityID me = this.agentInfo.getID();
        this.stuckedHumans.calc();
        return this.stuckedHumans.getClusterEntityIDs(0).contains(me);
    }

    //  @ DEBUG
    private static java.awt.geom.Line2D convertToAWTLine(Line2D line)
    {
        final double x1 = line.getOrigin().getX();
        final double x2 = line.getEndPoint().getX();
        final double y1 = line.getOrigin().getY();
        final double y2 = line.getEndPoint().getY();
        return new java.awt.geom.Line2D.Double(x1, y1, x2, y2);
    }

    private static java.awt.Polygon convertToAWTPolygon(Point2D[] ps)
    {
        final int n = ps.length;
        int[] xs = new int[n];
        int[] ys = new int[n];
        for (int i=0; i<n; ++i)
        {
            xs[i] = (int)ps[i].getX();
            ys[i] = (int)ps[i].getY();
        }
        return new java.awt.Polygon(xs, ys, n);
    }
    //  @ END OF DEBUG
}
