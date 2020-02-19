package AUR.extaction;

import AUR.util.aslan.AURAreaCostComparator;
import AUR.util.aslan.AURBuildingsEntrancePerpendicularLine;
import AUR.util.aslan.AURClearWatcher;
import AUR.util.aslan.AURDijkstra;
import AUR.util.aslan.AURPoliceUtil;
import AUR.util.aslan.AURGeoMetrics;
import AUR.util.aslan.AURGeoTools;
import AUR.util.aslan.AURPoliceScoreGraph;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURConstants;
import AUR.util.knd.AURGeoUtil;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import AUR.util.knd.AURWorldGraph;
import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.action.common.ActionRest;
import adf.agent.action.police.ActionClear;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.PathPlanning;
import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Amir Aslan Aslani - 2017 & 2018
 */

public class AURActionExtClear extends ExtAction {
        private final int clearDistance;
        private final int distanceLimit;
        private final int forcedMove;
        private final int thresholdRest;
        private int kernelTime;

        private EntityID target;
        private int oldClearX;
        private int oldClearY;
        private int count;

        private final double agentSize = AURConstants.Agent.RADIUS;

        private final AURPoliceScoreGraph psg;
        private AURWorldGraph wsg = null;
        private PathPlanning pathPlanning;
        
        private final AURClearWatcher cw;
        private final Point2D lastHomeComing = null;
        
        private int[] agentPosition;

//        private boolean isThereDecidedCleaningLine = false;
//        private Pair<Point2D, EntityID> decidedCleaningLineTarget = null;
        
        private AURBuildingsEntrancePerpendicularLine bp = null;

        public AURActionExtClear(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
                super(ai, wi, si, moduleManager, developData);
                this.clearDistance = si.getClearRepairDistance();
                
                this.distanceLimit = 9 * this.clearDistance / 10;
                this.forcedMove = developData.getInteger("ActionExtClear.forcedMove", 1);
                this.thresholdRest = developData.getInteger("ActionExtClear.rest", 100);

                this.target = null;
                this.oldClearX = 0;
                this.oldClearY = 0;
                this.count = 0;

                this.wsg = moduleManager.getModule("knd.AuraWorldGraph");
                this.psg = moduleManager.getModule("aslan.PoliceScoreGraph","AUR.util.aslan.AURPoliceScoreGraph");
                this.cw = moduleManager.getModule("aslan.PoliceClearWatcher","AUR.util.aslan.AURClearWatcher");
                
                this.bp = new AURBuildingsEntrancePerpendicularLine(ai, wi, cw, wsg);
                
        }

        @Override
        public ExtAction precompute(PrecomputeData precomputeData) {
                super.precompute(precomputeData);
                wsg.precompute(precomputeData);
                if (this.getCountPrecompute() >= 2) {
                        return this;
                }
                try {
                        this.kernelTime = this.scenarioInfo.getKernelTimesteps();
                } catch (NoSuchConfigOptionException e) {
                        this.kernelTime = -1;
                }
                return this;
        }

        @Override
        public ExtAction resume(PrecomputeData precomputeData) {
                super.resume(precomputeData);
                wsg.resume(precomputeData);
                if (this.getCountResume() >= 2) {
                        return this;
                }
                try {
                        this.kernelTime = this.scenarioInfo.getKernelTimesteps();
                } catch (NoSuchConfigOptionException e) {
                        this.kernelTime = -1;
                }
                return this;
        }

        @Override
        public ExtAction preparate() {
                super.preparate();
                wsg.preparate();
                if (this.getCountPreparate() >= 2) {
                        return this;
                }
                try {
                        this.kernelTime = this.scenarioInfo.getKernelTimesteps();
                } catch (NoSuchConfigOptionException e) {
                        this.kernelTime = -1;
                }
                return this;
        }

        @Override
        public ExtAction updateInfo(MessageManager messageManager) {
                super.updateInfo(messageManager);
                wsg.updateInfo(messageManager);
                if (this.getCountUpdateInfo() >= 2) {
                        return this;
                }
                wsg.updateInfo(messageManager);
                return this;
        }

        @Override
        public ExtAction setTarget(EntityID target) {
                this.target = null;
                StandardEntity entity = this.worldInfo.getEntity(target);
                if (entity != null) {
                        if (entity instanceof Road) {
                                this.target = target;
                        } else if (entity.getStandardURN().equals(StandardEntityURN.BLOCKADE)) {
                                this.target = ((Blockade) entity).getPosition();
                        } else if (entity instanceof Building) {
                                this.target = target;
                        }
                }
                return this;
        }

        @Override
        public ExtAction calc() {
                wsg.calc();
                this.agentPosition = new int[]{(int) agentInfo.getX(),(int) agentInfo.getY()};
                this.cw.updateAgentInformations();
                
                this.result = null;
                PoliceForce policeForce = (PoliceForce) this.agentInfo.me();

                /**
                 * if agent need rest then change target to nearest refuge.
                 */
                if (this.needRest(policeForce)) {
                        if(worldInfo.getEntity(policeForce.getPosition()) instanceof Refuge){
                                this.result = new ActionRest();
                                return this;
                        }
                        Pair<EntityID, Boolean> tempTarget = this.calcRest(policeForce);
                        if(tempTarget != null){
                                if(tempTarget.second()){
                                        this.result = new ActionMove(
                                                wsg.getPathToClosest(policeForce.getPosition(), Lists.newArrayList(tempTarget.first()))
                                        );
                                        return this;
                                }
                                else{
                                        this.target = tempTarget.first();
                                }
                        }
                }
                
                if (this.target == null) {
                        return this;
                }

//                this.target = new EntityID(949);
                
                EntityID agentPosition = policeForce.getPosition();
                StandardEntity positionStandardEntity = Objects.requireNonNull(this.worldInfo.getEntity(agentPosition));
                StandardEntity targetStandardEntity = this.worldInfo.getEntity(this.target);

                if (targetStandardEntity == null || !(targetStandardEntity instanceof Area)) {
                        return this;
                }

                /**
                 * if there is agent that blocked in police clear area, then rescue that.
                 * agent is standing on anywhere
                 * and there is another human object, trapped in a blockade
                 */
                int[] blockedAgentInClearArea = getBlockedAgentInClearArea();
                if(blockedAgentInClearArea != null){
//                        System.out.println("Get Rescue Agent That Blocked...");
                        this.result = this.cw.getAction(
                                getOpenPointAction(blockedAgentInClearArea)
                        );
                        if(this.result != null)
                                return this;
//                        System.out.println("Failed!");
                }

                /**
                 * if there is buildings that blocked, then open that.
                 */
                int[] blockedBuildingsEntranceInClearArea = getblockedBuildingsEntranceInClearArea();
                if(blockedBuildingsEntranceInClearArea != null){
//                        System.out.println("Get Open Building That Blocked...");
                        this.result = this.cw.getAction(
                                getOpenPointAction(blockedBuildingsEntranceInClearArea)
                        );
                        if(this.result != null)
                                return this;
//                        System.out.println("Failed!");
                }
                
                /**
                 * Buildings Entrance Perpendicular Line Settings.
                 */
                Action settingAction = this.bp.setChangesOfBuildingsEntrancePerpendicularLine(this.target);
                if(settingAction != null){
                        this.result = settingAction;
                        return this;
                }
                
                if(cw.dontMoveCounter == 8){
                        this.result = new ActionMove(Lists.newArrayList(agentPosition), agentInfo.getPositionArea().getX(), agentInfo.getPositionArea().getY());
                        return this;
                }
                
                /**
                 * if agent is standing on the target
                 */
                if (agentPosition.equals(this.target)) {
//                        System.out.println("getAreaClearAction <- beacause of reach to target :)");
                        this.result = this.cw.getAction(
                                this.getAreaClearAction(policeForce, targetStandardEntity)
                        );
                        if(this.result != null)
                                return this;
                }
                
                /**
                 * Improved road clearing if agent is standind on the area then
                 * open road to target
                 */
//                System.out.println("Get improved road clearing...");
                this.result = improvedRoadClearing(policeForce, target);
                return this;
        }

        private Point2D getPointClear(double startX, double startY, double endX, double endY) {
                Vector2D vector = this.scaleClear(this.getVector(startX, startY, endX, endY));
                int clearX = (int) (startX + vector.getX());
                int clearY = (int) (startY + vector.getY());

                return new Point2D(clearX, clearY);
        }

        private Action getAreaClearAction(PoliceForce police, StandardEntity targetEntity) {
                if (targetEntity instanceof Building) {
                        return null;
                }
                Road road = (Road) targetEntity;
                if (!road.isBlockadesDefined() || road.getBlockades().isEmpty()) {
                        return null;
                }

                double agentX = police.getX();
                double agentY = police.getY();

                Area areaTargetEntity = (Area) targetEntity;
                Edge edge = null;

                Collection<Blockade> blockades = this.worldInfo.getBlockades(road).stream().filter(Blockade::isApexesDefined)
                        .collect(Collectors.toSet());

                Point2D PointBestCollideWithBlocakde = null;
                double distanceMin = Double.MAX_VALUE;

                for (EntityID ied : areaTargetEntity.getNeighbours()) {
                        StandardEntity se = worldInfo.getEntity(ied);
                        if (se instanceof Building) {
                                Area building = (Area) se;
                                edge = building.getEdgeTo(targetEntity.getID());

                                double midX = (edge.getStartX() + edge.getEndX()) / 2;
                                double midY = (edge.getStartY() + edge.getEndY()) / 2;

                                for (Blockade blockade : blockades) {
                                        Point2D l2d = this.getPointIntersectLine2D(agentX, agentY, midX, midY, blockade);
                                        if (l2d != null) {
                                                double temp = getDistance(agentX, agentY, l2d.getX(), l2d.getY());
                                                if (temp < distanceMin) {
                                                        distanceMin = temp;
                                                        PointBestCollideWithBlocakde = l2d;
                                                }
                                        }
                                }
                        }

                        if (PointBestCollideWithBlocakde != null) {
                                break;
                        }
                }

                Action actionClear = null;

                if (PointBestCollideWithBlocakde != null) {
                        double midx = PointBestCollideWithBlocakde.getX();
                        double midy = PointBestCollideWithBlocakde.getY();
                        Point2D pointClear = getPointClear(agentX, agentY, midx, midy);
                        int clearX = (int) (pointClear.getX());
                        int clearY = (int) (pointClear.getY());
                        if (this.getDistance(agentX, agentY, midx, midy) < this.distanceLimit) {
                                actionClear = new ActionClear(clearX, clearY);
                                if (this.equalsPoint(this.oldClearX, this.oldClearY, clearX, clearY)) {
                                        if (this.count >= this.forcedMove) {
                                                this.count = 0;
                                                return new ActionMove(Lists.newArrayList(road.getID()), (int) midx, (int) midy);
                                        }
                                        this.count++;
                                }
                                this.oldClearX = clearX;
                                this.oldClearY = clearY;
                        } else {
                                return new ActionMove(Lists.newArrayList(road.getID()), (int) midx, (int) midy);
                        }

                        return actionClear;
                }
                return null;
        }
        
        private boolean equalsPoint(double p1X, double p1Y, double p2X, double p2Y) {
                return this.equalsPoint(p1X, p1Y, p2X, p2Y, 1000.0D);
        }

        private boolean equalsPoint(double p1X, double p1Y, double p2X, double p2Y, double range) {
                return (p2X - range < p1X && p1X < p2X + range) && (p2Y - range < p1Y && p1Y < p2Y + range);
        }
        
        private boolean intersect(double agentX, double agentY, double pointX, double pointY, Area area) {
                for (Edge edge : area.getEdges()) {
                        double startX = edge.getStartX();
                        double startY = edge.getStartY();
                        double endX = edge.getEndX();
                        double endY = edge.getEndY();
                        if (java.awt.geom.Line2D.linesIntersect(agentX, agentY, pointX, pointY, startX, startY, endX, endY)) {
                                double midX = (edge.getStartX() + edge.getEndX()) / 2;
                                double midY = (edge.getStartY() + edge.getEndY()) / 2;
                                if (!equalsPoint(pointX, pointY, midX, midY) && !equalsPoint(agentX, agentY, midX, midY)) {
                                        return true;
                                }
                        }
                }
                return false;
        }

        private Point2D getPointIntersectLine2D(double agentX, double agentY, double pointX, double pointY, Blockade blockade) {
                double minDistance = Double.MAX_VALUE;
                Point2D l2d = null;
                Polygon shape = (Polygon) blockade.getShape();
                int size = 500;
                Point2D pointAgent = new Point2D(agentX, agentY);
                Point2D pointTarget = new Point2D(pointX, pointY);

                Line2D line = new Line2D(pointAgent, pointTarget);
                Vector2D mainLineVector = line.getDirection();
                Vector2D normalisedMainLineVector = mainLineVector.normalised();

                Vector2D normalMainLineVector1 = normalisedMainLineVector.getNormal();
                Vector2D normalMainLineVector2 = normalisedMainLineVector.getNormal().getNormal().getNormal();

                Vector2D MainLineVector1 = normalMainLineVector1.scale(size);
                Vector2D MainLineVector2 = normalMainLineVector2.scale(size);

                Line2D line1 = new Line2D(new Point2D(agentX + MainLineVector1.getX(), agentY + MainLineVector1.getY()), new Point2D(pointX + MainLineVector1.getX(), pointY + MainLineVector1.getY()));
                Line2D line2 = new Line2D(new Point2D(agentX + MainLineVector2.getX(), agentY + MainLineVector2.getY()), new Point2D(pointX + MainLineVector2.getX(), pointY + MainLineVector2.getY()));

                line.setOrigin(new Point2D(line.getOrigin().getX() - line.getDirection().getX() / 4, line.getOrigin().getY() - line.getDirection().getY() / 4));

                for (int i = 0; i < shape.npoints; i++) {
                        double[] pp = new double[2];
                        if (findLineSegmentIntersection(line.getOrigin().getX(), line.getOrigin().getY(), pointX, pointY, shape.xpoints[i], shape.ypoints[i], shape.xpoints[(i + 1) % shape.npoints], shape.ypoints[(i + 1) % shape.npoints], pp) != 1) {
                                continue;
                        }

                        Point2D p2d = new Point2D(pp[0], pp[1]);

                        if (p2d != null) {
                                double temp = GeometryTools2D.getDistance(new Point2D(agentX, agentY), p2d);
                                if (temp < minDistance) {
                                        minDistance = temp;
                                        l2d = p2d;
                                }
                        }
                }
                for (int i = 0; i < shape.npoints; i++) {
                        double[] pp1 = new double[2];
                        if (findLineSegmentIntersection(line1.getOrigin().getX(), line1.getOrigin().getY(), line1.getEndPoint().getX(), line1.getEndPoint().getY(), shape.xpoints[i], shape.ypoints[i], shape.xpoints[(i + 1) % shape.npoints], shape.ypoints[(i + 1) % shape.npoints], pp1) != 1) {
                                continue;
                        }

                        Point2D p2d1 = new Point2D(pp1[0], pp1[1]);

                        if (p2d1 != null) {
                                double temp = GeometryTools2D.getDistance(new Point2D(agentX, agentY), p2d1);
                                if (temp < minDistance) {
                                        minDistance = temp;
                                        l2d = p2d1;
                                }
                        }
                }

                for (int i = 0; i < shape.npoints; i++) {
                        double[] pp2 = new double[2];
                        if (findLineSegmentIntersection(line2.getOrigin().getX(), line2.getOrigin().getY(), line2.getEndPoint().getX(), line2.getEndPoint().getY(), shape.xpoints[i], shape.ypoints[i], shape.xpoints[(i + 1) % shape.npoints], shape.ypoints[(i + 1) % shape.npoints], pp2) != 1) {
                                continue;
                        }

                        Point2D p2d2 = new Point2D(pp2[0], pp2[1]);

                        if (p2d2 != null) {
                                double temp = GeometryTools2D.getDistance(new Point2D(agentX, agentY), p2d2);
                                if (temp < minDistance) {
                                        minDistance = temp;
                                        l2d = p2d2;
                                }
                        }
                }
                return l2d;
        }

        private Point2D getClosestPointtoBlockade(double agentX, double agentY, Blockade blockade) {
                double minDistance = Double.MAX_VALUE;
                Point2D l2d = null;
                Polygon shape = (Polygon) blockade.getShape();
                for (int i = 0; i < shape.npoints; i++) {
                        Point2D temp = GeometryTools2D.getClosestPoint(new Line2D(shape.xpoints[i], shape.ypoints[i], shape.xpoints[(i + 1) % shape.npoints], shape.ypoints[(i + 1) % shape.npoints]), new Point2D(agentX, agentY));
                        if (getDistance(agentX, agentY, temp.getX(), temp.getY()) < minDistance) {
                                minDistance = getDistance(agentX, agentY, temp.getX(), temp.getY());
                                l2d = temp;
                        }
                }
                if (l2d == null) {
                        return null;
                }
                return l2d;
        }

        private double max(double a, double b, double c, double d) {
                return Math.max(Math.max(a, b), Math.max(c, d));
        }

        private boolean equals(double a, double b, double limit) {
                return Math.abs(a - b) < limit;
        }

        private boolean equals(double a, double b) {
                return equals(a, b, 1.0e-5);
        }

        private double min(double a, double b, double c, double d) {
                return Math.min(Math.min(a, b), Math.min(c, d));
        }

        private int findLineSegmentIntersection(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3, double[] intersection) {
                // Make limit depend on input domain
                final double LIMIT = 1e-5;
                final double INFINITY = 1e10;

                double x, y;

                //
                // Convert the lines to the form y = ax + b
                //
                // Slope of the two lines
                double a0 = equals(x0, x1, LIMIT) ? INFINITY : (y0 - y1) / (x0 - x1);
                double a1 = equals(x2, x3, LIMIT) ? INFINITY : (y2 - y3) / (x2 - x3);

                double b0 = y0 - a0 * x0;
                double b1 = y2 - a1 * x2;

                // Check if lines are parallel
                if (equals(a0, a1)) {
                        if (!equals(b0, b1)) {
                                return -1; // Parallell non-overlapping
                        } else {
                                if (equals(x0, x1)) {
                                        if (Math.min(y0, y1) < Math.max(y2, y3)
                                                || Math.max(y0, y1) > Math.min(y2, y3)) {
                                                double twoMiddle = y0 + y1 + y2 + y3
                                                        - min(y0, y1, y2, y3)
                                                        - max(y0, y1, y2, y3);
                                                y = (twoMiddle) / 2.0;
                                                x = (y - b0) / a0;
                                        } else {
                                                return -1;  // Parallell non-overlapping
                                        }
                                } else if (Math.min(x0, x1) < Math.max(x2, x3)
                                        || Math.max(x0, x1) > Math.min(x2, x3)) {
                                        double twoMiddle = x0 + x1 + x2 + x3
                                                - min(x0, x1, x2, x3)
                                                - max(x0, x1, x2, x3);
                                        x = (twoMiddle) / 2.0;
                                        y = a0 * x + b0;
                                } else {
                                        return -1;
                                }

                                intersection[0] = x;
                                intersection[1] = y;
                                return -2;
                        }
                }

                // Find correct intersection point
                if (equals(a0, INFINITY)) {
                        x = x0;
                        y = a1 * x + b1;
                } else if (equals(a1, INFINITY)) {
                        x = x2;
                        y = a0 * x + b0;
                } else {
                        x = -(b0 - b1) / (a0 - a1);
                        y = a0 * x + b0;
                }

                intersection[0] = x;
                intersection[1] = y;

                // Then check if intersection is within line segments
                double distanceFrom1;
                if (equals(x0, x1)) {
                        if (y0 < y1) {
                                distanceFrom1 = y < y0 ? length(x, y, x0, y0)
                                        : y > y1 ? length(x, y, x1, y1) : 0.0;
                        } else {
                                distanceFrom1 = y < y1 ? length(x, y, x1, y1)
                                        : y > y0 ? length(x, y, x0, y0) : 0.0;
                        }
                } else if (x0 < x1) {
                        distanceFrom1 = x < x0 ? length(x, y, x0, y0)
                                : x > x1 ? length(x, y, x1, y1) : 0.0;
                } else {
                        distanceFrom1 = x < x1 ? length(x, y, x1, y1)
                                : x > x0 ? length(x, y, x0, y0) : 0.0;
                }

                double distanceFrom2;
                if (equals(x2, x3)) {
                        if (y2 < y3) {
                                distanceFrom2 = y < y2 ? length(x, y, x2, y2)
                                        : y > y3 ? length(x, y, x3, y3) : 0.0;
                        } else {
                                distanceFrom2 = y < y3 ? length(x, y, x3, y3)
                                        : y > y2 ? length(x, y, x2, y2) : 0.0;
                        }
                } else if (x2 < x3) {
                        distanceFrom2 = x < x2 ? length(x, y, x2, y2)
                                : x > x3 ? length(x, y, x3, y3) : 0.0;
                } else {
                        distanceFrom2 = x < x3 ? length(x, y, x3, y3)
                                : x > x2 ? length(x, y, x2, y2) : 0.0;
                }

                return equals(distanceFrom1, 0.0)
                        && equals(distanceFrom2, 0.0) ? 1 : 0;
        }

        private double length(double x0, double y0, double x1, double y1) {
                double dx = x1 - x0;
                double dy = y1 - y0;

                return Math.sqrt(dx * dx + dy * dy);
        }

        private double getDistance(double fromX, double fromY, double toX, double toY) {
                double dx = toX - fromX;
                double dy = toY - fromY;
                return Math.hypot(dx, dy);
        }

        private double getAngle(Vector2D v1, Vector2D v2) {
                double flag = (v1.getX() * v2.getY()) - (v1.getY() * v2.getX());
                double angle = Math
                        .acos(((v1.getX() * v2.getX()) + (v1.getY() * v2.getY())) / (v1.getLength() * v2.getLength()));
                if (flag > 0) {
                        return angle;
                }
                if (flag < 0) {
                        return -1 * angle;
                }
                return 0.0D;
        }

        private Vector2D getVector(double fromX, double fromY, double toX, double toY) {
                return (new Point2D(toX, toY)).minus(new Point2D(fromX, fromY));
        }

        private Vector2D scaleClear(Vector2D vector) {
                return vector.normalised().scale(this.clearDistance);
        }

        private boolean needRest(Human agent) {
                int hp = agent.getHP();
                int damage = agent.getDamage();
                if (damage == 0 || hp == 0) {
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

        private Pair<EntityID, Boolean> calcRest(Human human) {
                EntityID position = human.getPosition();
                Collection<EntityID> refuges = this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE);
                if(refuges.size() > 0){
                        wsg.getNoBlockadePathToClosest(position, refuges);
                        wsg.getPathToClosest(position, refuges);

                        ArrayList<AURAreaGraph> closeRefuges = wsg.getAreaGraph(refuges);
                        closeRefuges.sort(new AURAreaCostComparator());
                        ArrayList<AURAreaGraph> closeRefugesNoBlockades = wsg.getAreaGraph(refuges);
                        closeRefugesNoBlockades.sort(new AURAreaCostComparator());
                        
                        if(closeRefuges.get(0).getTravelTime() > closeRefugesNoBlockades.get(0).getNoBlockadeTravelTime() * 1.5){
                                return new Pair(
                                        closeRefugesNoBlockades.get(0).area.getID(),
                                        Boolean.FALSE
                                );
                        }
                        else{
                                return new Pair(
                                        closeRefuges.get(0).area.getID(),
                                        Boolean.TRUE
                                );
                        }
                }
                return null;
        }

        // Improved Road Clearing Methodes
        private Action improvedRoadClearing(PoliceForce policeForce, EntityID target) {
//                System.out.println("From: " + agentInfo.getPosition());
//                System.out.println("Target: " + target);
                ArrayList<EntityID> path = AURPoliceUtil.filterAlirezaPathBug(this.wsg.getNoBlockadePathToClosest(policeForce.getPosition(), Lists.newArrayList(target)));
                
                ArrayList<Pair<Point2D, EntityID>> pathNodes = getPathNodes(path);
		
                if(pathNodes == null &&
                   (path.size() > 1)){
                        
//                        System.out.println("Full Clear Action... ( " + path.get(1) + " )");
                        return getAreaFullClearActionOrIgnoreBlockades(path.get(1));
                }
                else{
                        isLastFullClear = false;
                }

                double[] buildingEntranceLine = bp.getBuildingEntranceLine(path);
                Pair<Point2D, EntityID> decidedLine = bp.isGoingToPoint(buildingEntranceLine);
                
                if(decidedLine == null){
                        int index = 1;
                        ArrayList<Point2D> k = new ArrayList<>();
                        int intersectCounter = 0;
                        Point2D policePoint = new Point2D(policeForce.getX(), policeForce.getY());
                        for(int i = 1;i < pathNodes.size(); i ++){
                                if (hasRoadIntersect(policePoint, pathNodes, i)) {
                                        k.add(pathNodes.get(i).first());
                                        if(intersectCounter >= 5){
                                                break;
                                        }
                                        else{
                                                intersectCounter ++;
                                        }
                                }
                                else{
                                        intersectCounter = 0;
                                        index = i;
                                }
                        }
                        decidedLine = pathNodes.get(index);
                        
                        if(AURConstants.PoliceExtClear.USE_STRAIGHT_ROAD_DETECTION && isThereStraightRoadExistsOnPath(pathNodes,index)){ // If there is straight road exists then just move!
//                                System.out.println("Straight Road Found!!");
                                return cw.getAction(
                                        new ActionMove(
                                                wsg.getPathToClosest(agentInfo.getPosition(), Lists.newArrayList(decidedLine.second()))
                                        )
                                );
                        }
                }
                
                return continueToDecidedCleaningLine(decidedLine);
        }

        private ArrayList<Pair<Point2D, EntityID>> getPathNodes(ArrayList<EntityID> path) {
                ArrayList<Pair<Point2D, EntityID>> result = new ArrayList<>();
                Point2D policeForcePoint = new Point2D(agentInfo.getX(), agentInfo.getY());
                result.add(new Pair(policeForcePoint,agentInfo.getPosition()));
                
                if(path.size() > 1 &&
                   (
                        ! isPassable(new Point2D(agentInfo.getX(), agentInfo.getY()),agentInfo.getPosition(),path.get(1))
                   )
                ){
//                        System.out.println("Area Guid Point For First Area Added...");
                        ArrayList<Pair<Point2D, EntityID>> areaGuidPoints = getAreaGuidPoints(policeForcePoint,agentInfo.getPosition(),path.get(1));
                        if(areaGuidPoints == null){
                                return null;
                        }
                        result.addAll( areaGuidPoints );
                }
                
                for (int i = 1; i < path.size(); i++) {
                        Area a1 = (Area) worldInfo.getEntity(path.get(i)); 
                        EntityID a2 = path.get(i - 1);
                        Pair<Point2D, EntityID> pair = new Pair<>(AURGeoTools.getEdgeMid(a1.getEdgeTo(a2)), path.get(i));
                        result.add(pair);
                        if (i < (path.size() - 1) && ! isPassable(path.get(i - 1), path.get(i), path.get(i + 1)))  {
                                ArrayList<Pair<Point2D, EntityID>> areaGuidPoints = getAreaGuidPoints(path.get(i - 1), path.get(i), path.get(i + 1));
                                if(areaGuidPoints == null){
                                        Point2D p1 = result.get( result.size() - 1 ).first(),
                                                p2 = result.get( result.size() - 2 ).first();
                                        
                                        double p11[] = AURGeoMetrics.getPointFromPoint2D(p1),
                                               p22[] = AURGeoMetrics.getPointFromPoint2D(p2);
                                        
                                        double plus[] = AURGeoMetrics.getPointsPlus(
                                                AURGeoMetrics.getVectorScaled(
                                                        AURGeoMetrics.getVectorNormal(
                                                                AURGeoMetrics.getPointsMinus(
                                                                        p11,
                                                                        p22
                                                                )
                                                        ),
                                                        AURConstants.Agent.RADIUS
                                                ),
                                                p11
                                        );
                                        
                                        result.add(
                                                new Pair(
                                                        new Point2D(
                                                                plus[0],
                                                                plus[1]
                                                        ),
                                                        path.get(i)
                                                )
                                        );
                                }
                                else{
                                        result.addAll( areaGuidPoints );
                                }
                        }
                }
                
                Area lastAreaOfPath = (Area) worldInfo.getEntity(path.get(path.size() - 1));
                result.add(
                        new Pair(
                                new Point2D(lastAreaOfPath.getX(), lastAreaOfPath.getY()),
                                lastAreaOfPath.getID()
                        )
                );
                return result;
        }
        
        private boolean isPassable(EntityID a1, EntityID a2, EntityID a3){
                Area a = (Area) worldInfo.getEntity(a2);
                Point2D p1 = AURGeoTools.getEdgeMid(a.getEdgeTo(a1));
                return isPassable(p1,a2,a3);
        }
        
        private boolean isPassable(Point2D a1, EntityID a2, EntityID a3){
                Area a = (Area) worldInfo.getEntity(a2);
                ArrayList<Area> areas = new ArrayList<>();
                areas.add(a);
                for(EntityID eid : a.getNeighbours())
                        areas.add((Area) worldInfo.getEntity(eid));
                Point2D p1 = a1,
                        p2 = AURGeoTools.getEdgeMid(a.getEdgeTo(a3));
                Polygon clearPolygon = AURPoliceUtil.getClearPolygon(p1, p2);
                return ! AURGeoTools.intersect(clearPolygon, areas);
        }
        
        private boolean hasRoadIntersect(Point2D policeForce, ArrayList<Pair<Point2D, EntityID>> path, int to) {

                Polygon clearLine = AURPoliceUtil.getClearPolygon(policeForce, path.get(to).first());

                double[] vectorScaled = AURGeoMetrics.getVectorScaled( 
                        AURGeoMetrics.getVectorNormal( 
                                AURGeoMetrics.getPointsMinus( 
                                        AURGeoMetrics.getPointFromPoint2D(policeForce), 
                                        AURGeoMetrics.getPointFromPoint2D(path.get(to).first()) 
                                ) 
                        ), 
                        agentSize 
                ); 
                 
                Polygon clearLineAgentSpace = AURPoliceUtil.getClearPolygon( 
                        path.get(to).first(), 
                        new Point2D( 
                                path.get(to).first().getX() + vectorScaled[0], 
                                path.get(to).first().getY() + vectorScaled[1] 
                        ) 
                ); 
                
//                wsg.policeClearArea.put(agentInfo.getID(),clearLine);
                Rectangle er = clearLine.getBounds();
                for (EntityID aid : worldInfo.getObjectIDsInRectangle(er.x, er.y, er.x + er.width, er.y + er.height)) {
                        if (worldInfo.getEntity(aid) instanceof Area && ((Area) worldInfo.getEntity(aid)).isEdgesDefined()) {
                                for (Edge e : ((Area) worldInfo.getEntity(aid)).getEdges()) {
                                        if ((!e.isPassable())
                                            && ((AURGeoTools.getIntersection(clearLine, e.getLine()))
                                            || (AURGeoTools.getIntersection(clearLineAgentSpace, e.getLine())))) {
                                                return true;
                                        }
                                }
                        }
                }

                return false;
        }

        private Action continueToDecidedCleaningLine(Pair<Point2D, EntityID> decidedCleaningLineTarget) {
                
                Point2D policePoint = new Point2D(agentInfo.getX(), agentInfo.getY());
                Vector2D vectorToTarget = decidedCleaningLineTarget.first().minus(policePoint).normalised();
                
                int distanceToTarget = (int) AURGeoUtil.length(
                        agentInfo.getX(),
                        agentInfo.getY(),
                        decidedCleaningLineTarget.first().getX(),
                        decidedCleaningLineTarget.first().getY()
                );
                
                int clearVectorLen = Math.min(distanceToTarget + AURConstants.PoliceExtClear.CLEAR_POLYGON_HEIGHT, this.clearDistance);
                
                Vector2D clearVector = vectorToTarget.scale(clearVectorLen * 98 / 100);
                Point2D clearPoint = new Point2D(policePoint.getX() + clearVector.getX(), policePoint.getY() + clearVector.getY());
                Polygon clearPolygon = AURPoliceUtil.getClearPolygon(policePoint, clearPoint);
                
                Pair<Integer, ArrayList<Blockade>> blockadesList = isThereBlockadesIntersectWithClearPolygon(clearPolygon, (Area) worldInfo.getEntity(agentInfo.getPosition()));
                if(blockadesList.first() == 1){ // Then Clear
                        this.cw.setBlockadeList(blockadesList.second());
                        return this.cw.getAction(
                                new ActionClear(agentInfo, vectorToTarget.scale(clearVectorLen))
                        );
                }
                else if(blockadesList.first() == 2){ // Then clear with old function
                        return this.cw.getAction(
                                new ActionClear(AURPoliceUtil.getNearestBlockadeToAgentFromList(agentInfo, blockadesList.second()))
                        );
                }
                else{ // Then Move
                        Vector2D moveVector;
                        if(distanceToTarget < this.clearDistance - agentSize)
                                moveVector = vectorToTarget.scale(distanceToTarget);
                        else{
                                moveVector = vectorToTarget.scale(this.clearDistance - agentSize);
//                                for(double to = moveVector.getLength() + AURConstants.PoliceExtClear.MOVE_LENGTH_CALCULATE_ERROR;to < distanceToTarget;to += AURConstants.PoliceExtClear.MOVE_LENGTH_CALCULATE_ERROR){ // deghate mohasebe toole masire ghabele tey kardan
//                                        if(thereIsNoBlockade(to,vectorToTarget)){
//                                                moveVector = vectorToTarget.scale(to - agentSize - 10);
//                                        }
//                                        else
//                                                break;
//                                }
                        }
                        
                        ArrayList<EntityID> noBlockadePathToClosest = AURPoliceUtil.filterAlirezaPathBug(wsg.getNoBlockadePathToClosest(agentInfo.getPosition(), Lists.newArrayList(decidedCleaningLineTarget.second())));
                        ArrayList<EntityID> pathToClosest = AURPoliceUtil.filterAlirezaPathBug(wsg.getPathToClosest(agentInfo.getPosition(), Lists.newArrayList(decidedCleaningLineTarget.second())));
                        
                        if(pathToClosest.size() > 1 && pathToClosest.size() == noBlockadePathToClosest.size()){
                                return this.cw.getAction(
                                        new ActionMove(
                                                pathToClosest/*,
                                                (int) decidedCleaningLineTarget.first().getX(),
                                                (int) decidedCleaningLineTarget.first().getY()*/
                                        )
                                );
                        }
                        
                        int[] lastPoint = new int[]{
                                (int) (policePoint.getX() + moveVector.getX()),
                                (int) (policePoint.getY() + moveVector.getY())
                        };
                        
                        Collection<StandardEntity> objectsInRange = worldInfo.getObjectsInRange(lastPoint[0], lastPoint[1], 1);
                        EntityID targetEntityID = null;
                        if(! objectsInRange.isEmpty()){
                                for(StandardEntity se : objectsInRange){
                                        if(se instanceof Area && ((Area) se).getShape().contains(lastPoint[0], lastPoint[1])){
                                                targetEntityID = se.getID();
                                                break;
                                        }
                                }
                        }
                        
                        EntityID areaOfLastPoint = targetEntityID == null ? agentInfo.getPosition() : targetEntityID;
                        
                        return this.cw.getAction(
                                new ActionMove(
                                        wsg.getNoBlockadePathToClosest(agentInfo.getPosition(), Lists.newArrayList(areaOfLastPoint)),
                                        lastPoint[0],
                                        lastPoint[1]
                                )
                        );
                }
        }
        
        private Pair<Integer, ArrayList<Blockade> > isThereBlockadesInBlockadesListInIntersectWithClearPolygon(Polygon clearPolygon, ArrayList<Blockade> blockades){
                ArrayList<Blockade> blockadesThatInClearPolygon = new ArrayList<>();
                int resultStatus = 0;
                
                for(Blockade blockade : blockades){
                        if(AURGeoTools.intersect(blockade, clearPolygon)){
                                if(resultStatus == 0)
                                        resultStatus = 1;
                                
                                blockadesThatInClearPolygon.add(blockade);
                        }
                        else if(blockade.getShape().contains(clearPolygon.xpoints[0], clearPolygon.ypoints[0])){
                                resultStatus = 2;
                                blockadesThatInClearPolygon.add(blockade);
                        }
                }
                
                return new Pair(
                        resultStatus,
                        blockadesThatInClearPolygon
                );
        }
        
        private Pair<Integer, ArrayList<Blockade> > isThereBlockadesIntersectWithClearPolygon(Polygon clearPolygon, Area start){
                Pair<ArrayList<Area>, ArrayList<Blockade> > areasAndBlockadesOfClearPolygon = getAreasAndBlockadesInBound(clearPolygon.getBounds(), start);
                return isThereBlockadesInBlockadesListInIntersectWithClearPolygon(clearPolygon, areasAndBlockadesOfClearPolygon.second());
        }
        
        private Pair<ArrayList<Area>, ArrayList<Blockade>> getAreasAndBlockadesInBound(Rectangle bound, Area start){
                ArrayList<Area> areasList = new ArrayList<>();
                ArrayList<Blockade> blocksList = new ArrayList<>();
                
                Stack<Area> areas = new Stack<>();
                areas.add(start);
                areasList.add(start);
                while (!areas.empty()) {
                        Area area = areas.pop();
                                        
                        for (EntityID id : area.getNeighbours()) {
                                Area tmp = (Area) worldInfo.getEntity(id);
                                if (
                                        (
                                                tmp.getShape().getBounds().contains(bound) ||
                                                tmp.getShape().getBounds().intersects(bound) ||
                                                bound.contains(tmp.getShape().getBounds())
                                        ) && ! areasList.contains(tmp)) {
                                        areasList.add(tmp);
                                }
                        }
                }
                
                for(Area area : areasList){
                        if(area instanceof Road){
                                if(area.isBlockadesDefined()){
                                        for (EntityID bid : area.getBlockades()) {
                                                Blockade btmp = (Blockade) worldInfo.getEntity(bid);
                                                blocksList.add(btmp);
                                        }
                                }
                        }
                }
                
                return new Pair(
                        areasList,
                        blocksList
                );
        }

        
        private ArrayList<Pair<Point2D, EntityID>> getAreaGuidPoints(EntityID a2, Point2D p1, Point2D p2){
                ArrayList<Point2D> points = new ArrayList<>();
                
                Area a = (Area) worldInfo.getEntity(a2);
                
                ArrayList areas = new ArrayList<>();
                areas.add(a);
                for(EntityID eid : a.getNeighbours())
                        areas.add(worldInfo.getEntity(eid));
                
                points.add(p1);
                points.add(p2);
                
                for(Edge e : a.getEdges()){
                        if(e.isPassable()){
                                points.add(AURGeoTools.getEdgeMid(e));
                        }
                }
                
                for(int i = 2;i < a.getApexList().length;i += 2){
                        for(int j = 0;j < i;j += 2){
                                if(
                                        Math.abs(j - i) > 2 &&
                                        a.getShape().contains(
                                                (a.getApexList()[i] + a.getApexList()[j]) / 2,
                                                (a.getApexList()[i + 1] + a.getApexList()[j + 1]) / 2
                                        )                                                
                                ){
                                        points.add(new Point2D(
                                                (a.getApexList()[i] + a.getApexList()[j]) / 2,
                                                (a.getApexList()[i + 1] + a.getApexList()[j + 1]) / 2)
                                        );
                                        
                                        
                                }
                        }
                }
                
                int matrix[][] = new int[points.size()][points.size()];
                for(int i = 0;i < points.size();i ++){
                        for(int j = 0;j < points.size();j ++){
                                if(
                                        i != j &&
                                        ! AURGeoTools.intersect(AURGeoTools.getClearPolygon(points.get(i), points.get(j), AURConstants.PoliceExtClear.GUID_POINT_CLEAR_POLYGON_HEIGHT , true),
                                                areas
                                        )
                                ){
                                        matrix[i][j] = matrix[j][i] = (int) AURGeoUtil.dist(points.get(i).getX(), points.get(i).getY(), points.get(j).getX(), points.get(j).getY());
                                }
                                else
                                        matrix[i][j] = 0;
                        }
                }
                
                AURDijkstra dijkstra = new AURDijkstra();
                dijkstra.dijkstra(matrix, 0, points.size());
                
                if(dijkstra.getDistanceTo(1) == Integer.MAX_VALUE)
                        return null;
                
                ArrayList<Integer> nodes = dijkstra.getPathTo(1);
                ArrayList<Pair<Point2D, EntityID>> result = new ArrayList<>();
                
                for(int i = 1;i < nodes.size() - 1;i ++){
                        result.add(
                                new Pair(
                                        points.get(nodes.get(i)),
                                        a2
                                )
                        );
                }
                
                return result;
        }
        
        private ArrayList<Pair<Point2D, EntityID> > getAreaGuidPoints(EntityID a1, EntityID a2, EntityID a3){
                return getAreaGuidPoints(
                        a2,
                        AURGeoTools.getEdgeMid(
                                ((Area) worldInfo.getEntity(a2)).getEdgeTo(a1)
                        ),
                        AURGeoTools.getEdgeMid(
                                ((Area) worldInfo.getEntity(a2)).getEdgeTo(a3)
                        )
                );
        }
        
        private ArrayList<Pair<Point2D, EntityID> > getAreaGuidPoints(Point2D a1, EntityID a2, EntityID a3){
                return getAreaGuidPoints(
                        a2,
                        a1,
                        AURGeoTools.getEdgeMid(
                                ((Area) worldInfo.getEntity(a2)).getEdgeTo(a3)
                        )
                );
        }

        private Action getOpenPointAction(int[] targetPosition) {
                Vector2D v = new Vector2D(
                        (int) (targetPosition[0] - agentPosition[0]), 
                        (int) (targetPosition[1] - agentPosition[1])
                );
                v = v.normalised().scale(v.getLength() + agentSize);
                
                
                Polygon clearPolygon = AURGeoTools.getClearPolygon(new Point2D(agentInfo.getX(), agentInfo.getY()), new Point2D(agentInfo.getX() + v.getX(), agentInfo.getY() + v.getY()), AURConstants.Agent.RADIUS * 3, true);
                Pair<Integer, ArrayList<Blockade>> blockadesList = isThereBlockadesIntersectWithClearPolygon(clearPolygon, (Area) worldInfo.getEntity(agentInfo.getPosition()));
                cw.setBlockadeList(blockadesList.second());
                
                return new ActionClear(
                        (int)(agentPosition[0] + v.getX()),
                        (int)(agentPosition[1] + v.getY())
                );
        }

        private int[] getBlockedAgentInClearArea() {
                int distToRescueBlockedAgents = this.clearDistance - (int) this.agentSize;
                boolean isThereGasStation = false;
                for(StandardEntity se : worldInfo.getObjectsInRange(agentPosition[0], agentPosition[1], 50000 + AURConstants.Agent.RADIUS * 3)){
                        if(se.getStandardURN().equals(StandardEntityURN.GAS_STATION)){
                                isThereGasStation = true;
                                break;
                        }
                }
                
                Set<EntityID> changedEntities = agentInfo.getChanged().getChangedEntities();
                Collection<EntityID> objectIDsInRange = worldInfo.getObjectIDsInRange(agentPosition[0], agentPosition[1], distToRescueBlockedAgents);
                for(EntityID o : objectIDsInRange){
                        StandardEntity se = worldInfo.getEntity(o);
                        if(se instanceof Human &&
                           ((changedEntities != null &&
                           changedEntities.contains(se.getID())) ||
                           changedEntities == null) &&
                           (AURConstants.PoliceExtClear.IGNORE_POLICES_RESCUE || ! (se instanceof PoliceForce)) &&
                           (!(se instanceof Civilian) ||
                           (se instanceof Civilian && isThereGasStation)) &&
                           ! se.getID().equals(wsg.ai.getID())){
                                
                                int humanPosition[] = new int[]{((Human)se).getX(), ((Human)se).getY()};
                                if(AURGeoUtil.dist(humanPosition[0], humanPosition[1], agentPosition[0], agentPosition[1]) < distToRescueBlockedAgents){
                                        if(worldInfo.getEntity(((Human)se).getPosition()) instanceof Road &&
                                           ((Road) worldInfo.getEntity(((Human)se).getPosition())).isBlockadesDefined()){
                                                for(EntityID b : (((Road) worldInfo.getEntity(((Human)se).getPosition())).getBlockades())){
                                                        Blockade blockade = (Blockade) worldInfo.getEntity(b);
                                                        if(blockade.getShape().contains(humanPosition[0], humanPosition[1])){
                                                                return humanPosition;
                                                        }
                                                }
                                        }
                                }
                        }
                }
                return null;
        }
        
        boolean isLastFullClear = false;
        private Action getAreaFullClearActionOrIgnoreBlockades(EntityID nextArea){
                boolean isLastFullClearTemp = isLastFullClear;
                isLastFullClear = true;
                
                if(! isLastFullClearTemp){
                        return new ActionClear(
                                agentInfo.getPositionArea().getX(),
                                agentInfo.getPositionArea().getY()
                        );
                }
                
                ArrayList<EntityID> pathToNext = wsg.getPathToClosest(
                        agentInfo.getPosition(),
                        Lists.newArrayList(nextArea)
                );
                
                if(pathToNext.size() <= 2 && ! pathToNext.isEmpty()){
                        return new ActionMove(pathToNext);
                }
                
                Area pArea = agentInfo.getPositionArea();
                
                if(pArea.isBlockadesDefined()){
                        Blockade selected = null;
                        double dist = Double.MAX_VALUE;
                        boolean isInRange = false;
                        
                        for(EntityID beid : pArea.getBlockades()){
                                Blockade entity = (Blockade) worldInfo.getEntity(beid);
                                double disTemp = Math.hypot(entity.getX() - agentPosition[0], entity.getY() - agentPosition[1]);
                                if(dist > disTemp){
                                        selected = entity;
                                        dist = disTemp;
                                        if(disTemp < this.clearDistance){
                                                isInRange = true;
                                        }
                                        else{
                                                isInRange = false;
                                        }
                                }
                                
                        }
                        if(isInRange){
                                return new ActionClear(selected);
                        }
                        else if(selected != null){
                                return new ActionMove(Lists.newArrayList(agentInfo.getPosition()), selected.getX(), selected.getY());
                        }
                }
                
                return new ActionMove(
                        wsg.getNoBlockadePathToClosest(
                                agentInfo.getPosition(),
                                Lists.newArrayList(nextArea)
                        )
                );
        }
        
        private boolean isThereStraightRoadFromPointToEdgeInArea(Area area,Point2D fromP, Edge toE){

                HashSet<Point2D> toSet = new HashSet<>();
                Point2D startP = toE.getStart();
                Vector2D minus = toE.getEnd().minus(startP);
                Vector2D v = minus.normalised().scale(AURConstants.Agent.RADIUS);
                for(int i = 1;i < minus.getLength() / AURConstants.Agent.RADIUS;i ++){
                        startP = startP.plus(v);
                        toSet.add(startP);
                }
                
                
                for(Point2D p2 : toSet){
                        Polygon clearPolygon = AURPoliceUtil.getClearPolygon(fromP, p2);
                        Pair<ArrayList<Area>, ArrayList<Blockade>> areasAndBlockadesInBound = getAreasAndBlockadesInBound(clearPolygon.getBounds(), area);
                        if(0 == isThereBlockadesInBlockadesListInIntersectWithClearPolygon(clearPolygon, areasAndBlockadesInBound.second()).first() && AURGeoTools.intersect(clearPolygon, areasAndBlockadesInBound.first())){
                                return true;
                        }
                }
                return false;
        }
        
        private boolean isThereStraightRoadFromEdgeToEdgeInArea(Area area,Edge fromE, Edge toE){
                HashSet<Point2D> fromSet = new HashSet<>();
                {
                        Point2D startP = fromE.getStart();
                        Vector2D minus = fromE.getEnd().minus(startP);
                        Vector2D v = minus.normalised().scale(AURConstants.Agent.RADIUS);
                        for(int i = 1;i < minus.getLength() / AURConstants.Agent.RADIUS;i ++){
                                startP = startP.plus(v);
                                fromSet.add(startP);
                        }
                }
                HashSet<Point2D> toSet = new HashSet<>();
                {
                        Point2D startP = toE.getStart();
                        Vector2D minus = toE.getEnd().minus(startP);
                        Vector2D v = minus.normalised().scale(AURConstants.Agent.RADIUS);
                        for(int i = 1;i < minus.getLength() / AURConstants.Agent.RADIUS;i ++){
                                startP = startP.plus(v);
                                toSet.add(startP);
                        }
                }
                for(Point2D p1 : fromSet){
                        for(Point2D p2 : toSet){
                                Polygon clearPolygon = AURPoliceUtil.getClearPolygon(p1, p2);
                                Pair<ArrayList<Area>, ArrayList<Blockade>> areasAndBlockadesInBound = getAreasAndBlockadesInBound(clearPolygon.getBounds(), area);
                                if(0 == isThereBlockadesInBlockadesListInIntersectWithClearPolygon(clearPolygon, areasAndBlockadesInBound.second()).first() && AURGeoTools.intersect(clearPolygon, areasAndBlockadesInBound.first())){
                                        return true;
                                }
                        }
                }
                return false;
        }

        private boolean isThereStraightRoadExistsOnPath(ArrayList<Pair<Point2D, EntityID>> pathNodes, int index) {
                if(index == 0)
                        return false;
                
                Pair<Point2D, EntityID> get = pathNodes.get(index);
                wsg.KStar(agentInfo.getPosition());
                
                ArrayList<EntityID> list1 = new ArrayList<>();
                for(int i = 0;i <= index;i ++){
                        if(! list1.contains(pathNodes.get(i).second())){
                                list1.add(pathNodes.get(i).second());
                        }
                }
                
                ArrayList<EntityID> list2 = new ArrayList<>();
                ArrayList<EntityID> pathToClosest = wsg.getPathToClosest(agentInfo.getPosition(), Lists.newArrayList(get.second()));
                
                if(pathToClosest.isEmpty())
                        return false;
                
                for(int i = 0;i < pathToClosest.size();i ++){
                        if(list2.isEmpty() || ! list2.contains(pathToClosest.get(i))){
                                list2.add(pathToClosest.get(i));
                        }
                }
                
                Area firstArea = (Area) worldInfo.getEntity(list1.get(0));
                if(list1.size() < 2 || ! isThereStraightRoadFromPointToEdgeInArea(firstArea, pathNodes.get(0).first(), firstArea.getEdgeTo(list1.get(1)) )){
                        return false;
                }
                
                if(list1.size() == list2.size() && list2.equals(list1)){
                        boolean isThereStraightRoad = true;
                        for(int i = 1;i < list1.size() - 1;i ++){
                                Area area = (Area) worldInfo.getEntity(list1.get(i));
                                if(! isThereStraightRoadFromEdgeToEdgeInArea(area, area.getEdgeTo(list1.get(i - 1)), area.getEdgeTo(list1.get(i + 1)))){
                                        isThereStraightRoad = false;
                                        break;
                                }
                        }
                        return isThereStraightRoad;
                }
                return false;
        }

        private int[] getblockedBuildingsEntranceInClearArea() {
                if(! AURConstants.PoliceExtClear.OPEN_NEAR_BUILDINGS_ENTRANCES)
                        return null;
                
                Collection<StandardEntity> objectsInRange = worldInfo.getObjectsInRange(agentPosition[0], agentPosition[1], this.clearDistance);
                int[] result = null;
                double dis = 0;
                
                for(StandardEntity se : objectsInRange){
                        if(se instanceof Building){
                                Building b = (Building) se;
                                for(Edge e : b.getEdges()){
                                        Point2D edgeMid = AURGeoTools.getEdgeMid(e);
                                        double disTemp = Math.hypot(edgeMid.getX() - agentInfo.getX(), edgeMid.getY() - agentInfo.getY());
                                        if(e.isPassable() &&
                                           disTemp > dis &&
                                           edgeMid.minus(new Point2D(agentInfo.getX(), agentInfo.getY())).getLength() < this.clearDistance - 50 &&
                                           isPolygonOnBlockades(AURGeoTools.getClearPolygon(new Point2D(agentInfo.getX(), agentInfo.getY()), edgeMid, AURConstants.Agent.RADIUS * 2 + 100, true))){
                                                dis = disTemp;
                                                result = new int[]{(int) edgeMid.getX(),(int) edgeMid.getY()};
                                        }
                                }
                        }
                }
                return result;
        }

        private boolean isPolygonOnBlockades(Polygon clearPolygon) {
                Rectangle bounds = clearPolygon.getBounds();
                Collection<StandardEntity> objectsInRectangle = worldInfo.getObjectsInRectangle(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height);
                for(StandardEntity se : objectsInRectangle){
                        if(se instanceof Road){
                                Road r = (Road) se;
                                if(r.isBlockadesDefined()){
                                        for(EntityID eid : r.getBlockades()){
                                                Blockade entity = (Blockade) worldInfo.getEntity(eid);
                                                if(AURGeoTools.intersect(entity, clearPolygon)){
                                                        return true;
                                                }
                                        }
                                }
                        }
                }
                return false;
        }
}
