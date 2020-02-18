package AUR.module.complex.self;

import AUR.util.AURCommunication;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.HumanDetector;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * Created by armanaxh in 2017
 */

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class LegacyAURHumanDetector extends HumanDetector {
    private Clustering clustering;
    private PathPlanning pathPlanning;
    private List<EntityID> activeCivilian;
    private EntityID result;
    private List<Human> rescueTargets;
    private AURCommunication communication;
    private int sendTime;
    private int sendingAvoidTimeClearRequest;

    private Collection<EntityID> agentPositions;
    private Map<EntityID, Integer> sentTimeMap;
    private int sendingAvoidTimeReceived;
    private int sendingAvoidTimeSent;
    private int moveDistance;
    private EntityID lastPosition;
    private int positionCount;
    private List<EntityID> vistedcivilian;

    public LegacyAURHumanDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);


        this.result = null;
        this.sendTime = 0;
        this.vistedcivilian = new LinkedList<>();
        this.sendingAvoidTimeClearRequest = developData.getInteger("SampleHumanDetector.sendingAvoidTimeClearRequest", 5);
        this.rescueTargets = new ArrayList<>();
        this.agentPositions = new HashSet<>();
        this.sentTimeMap = new HashMap<>();
        this.sendingAvoidTimeReceived = developData.getInteger("SampleHumanDetector.sendingAvoidTimeReceived", 3);
        this.sendingAvoidTimeSent = developData.getInteger("SampleHumanDetector.sendingAvoidTimeSent", 5);
        this.moveDistance = developData.getInteger("SampleHumanDetector.moveDistance", 40000);
        this.activeCivilian = new LinkedList<>();
        this.communication = new AURCommunication(ai, wi, si, developData);

        switch (scenarioInfo.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("SampleHumanDetector.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("SampleHumanDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("SampleHumanDetector.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("SampleHumanDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("SampleHumanDetector.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("SampleHumanDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
        }

    }

    @Override
    public HumanDetector updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.communication.updateInfo(messageManager);
        this.clustering.updateInfo(messageManager);
        this.pathPlanning.updateInfo(messageManager);

        return this;
    }

    @Override
    public HumanDetector calc() {
        Human transportHuman = this.agentInfo.someoneOnBoard();
        if (transportHuman != null) {
            this.result = transportHuman.getID();
            return this;
        }
        if (this.nullResult()) {
            this.result = null;
        }
        if (this.result == null) {
            this.result = this.calcTarget();
        }
        return this;
    }

    private boolean nullResult() {
        if (this.result != null) {
            Human target = (Human) this.worldInfo.getEntity(this.result);
            if (target != null) {
                if (!target.isHPDefined() || target.getHP() == 0) {
                    return true;
                } else if (!target.isPositionDefined()) {
                    return true;
                } else if (this.civilianDoesNotNeedRescue(target)) {
                    return true;
                } else if (this.changeCivilianView()) {
                    return true;
                } else if (target.getPosition().equals(agentInfo.getPosition())
                        && !agentInfo.getChanged().getChangedEntities().contains(target.getID())) {
                    this.activeCivilian.add(target.getID());
                    return true;
                } else {
                    StandardEntity position = this.worldInfo.getPosition(target);
                    if (position != null) {
                        StandardEntityURN positionURN = position.getStandardURN();
                        if (positionURN.equals(REFUGE) || positionURN.equals(AMBULANCE_TEAM)) {
                            return true;
                        }
                    }
                }
                if ((target.isHPDefined() && target.getHP() < 600)
                        || (target.isBuriednessDefined() && target.getBuriedness() >= 5)) {
                    StandardEntity targetPosition = worldInfo.getEntity(target.getPosition());
                    if (targetPosition != null
                            & targetPosition instanceof Building) {
                        if (((Building) targetPosition).isOnFire()) {
                            return true;
                        }
                    }
                }

            }
            if (target instanceof AmbulanceTeam
                    || target instanceof FireBrigade
                    || target instanceof PoliceForce) {
                if (target.isBuriednessDefined() && target.getBuriedness() == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private EntityID calcTarget() {
        Rectangle2D band = worldInfo.getBounds();
        final double distansTORescureAgent = ((band.getMaxX() - band.getMinX()) + (band.getMaxY() - band.getMinY())) / 2 / 3;
        final double distanseAzCivilian = scenarioInfo.getPerceptionLosMaxDistance() * 3;
        this.rescueTargets.clear();
        List<Human> loadTargets = new ArrayList<>();
        List<EntityID> rescueTargetPosision = new ArrayList<>();
        List<EntityID> loadTargetPosision = new ArrayList<>();
        List<Human> rescueAgent = new ArrayList<>();
        List<EntityID> rescueAgentPosision = new ArrayList<>();


        for (StandardEntity next : this.worldInfo.getEntitiesOfType(AMBULANCE_TEAM, FIRE_BRIGADE, POLICE_FORCE)) {
            Human h = (Human) next;
            StandardEntity positionEntity = this.worldInfo.getPosition(h);
            if (this.agentInfo.getID().equals(h.getID())) {
                continue;
            }
            if (positionEntity instanceof Area) {
                Area area = (Area) positionEntity;
                if (h.isPositionDefined() && this.getDistance(agentInfo.getX(), agentInfo.getY(), area.getX(), area.getY()) > distansTORescureAgent) {
                    continue;
                }
            }

            if (positionEntity != null) {
                if (h.isHPDefined() && h.isBuriednessDefined() && h.getHP() > 0 && h.getBuriedness() > 0) {
                    if (activeTime(h) > h.getBuriedness()) {
                        rescueAgent.add(h);
                        rescueAgentPosision.add(h.getPosition());
                    }
                }
            }
        }
        this.pathPlanning.setFrom(agentInfo.getPosition());
        if (rescueAgentPosision.size() > 0) {
            this.pathPlanning.setDestination(rescueAgentPosision);
            List<EntityID> path = this.pathPlanning.calc().getResult();
            if (path != null && path.size() > 0) {
                EntityID targetPosision = path.get(path.size() - 1);
                for (Human h : rescueAgent) {
                    if (h.getPosition().equals(targetPosision)) {
                        return h.getID();
                    }
                }
            }
        }

        for (StandardEntity next : this.worldInfo.getEntitiesOfType(CIVILIAN)) {
            Human h = (Human) next;
            StandardEntity positionEntity = this.worldInfo.getPosition(h);

            if (this.activeCivilian.contains(next.getID()))
                continue;
            if (positionEntity instanceof Area) {
                Area area = (Area) positionEntity;
                if (h.isPositionDefined() && this.getDistance(agentInfo.getX(), agentInfo.getY(), area.getX(), area.getY()) > distanseAzCivilian) {
                    continue;
                }
            }
            this.vistedcivilian.add(next.getID());

            if (positionEntity != null && positionEntity instanceof Area && positionEntity.getStandardURN() != REFUGE) {
                if (this.needRescueFromCollapse(h)) {
                    rescueTargets.add(h);
                    rescueTargetPosision.add(h.getPosition());
                } else if (this.needRescueFromRoad(h)) {
                    loadTargets.add(h);
                    loadTargetPosision.add(h.getPosition());
                }
            }
        }


        this.removeDangerPos(rescueTargetPosision);

        List<EntityID> rescueTargeWithLowBuriednessPosision = new ArrayList<>();
        for (Human human : this.rescueTargets) {
            if (human.isBuriednessDefined() && human.getBuriedness() < 25) {
                rescueTargeWithLowBuriednessPosision.add(human.getPosition());
            }
        }
        if (rescueTargeWithLowBuriednessPosision.size() > 0) {
            this.pathPlanning.setDestination(rescueTargeWithLowBuriednessPosision);
            List<EntityID> path = this.pathPlanning.calc().getResult();
            if (path != null && path.size() > 0) {
                EntityID targetPosision = path.get(path.size() - 1);
                return this.getWeakerCivilian(targetPosision);
            }
        }


        if (rescueTargets.size() > 0) {
            this.pathPlanning.setDestination(rescueTargetPosision);
            List<EntityID> path = this.pathPlanning.calc().getResult();
            if (path != null && path.size() > 0) {
                EntityID targetPosision = path.get(path.size() - 1);
                return this.getWeakerCivilian(targetPosision);
            }
        }

        if (loadTargets.size() > 0) {
            this.pathPlanning.setDestination(loadTargetPosision);
            List<EntityID> path = this.pathPlanning.calc().getResult();
            if (path != null && path.size() > 0) {
                EntityID targetPosision = path.get(path.size() - 1);
                return this.getWeakerCivilian(targetPosision);
            }
        }
        return null;
    }

    private List<EntityID> removeDangerPos(List<EntityID> targetPos) {
        final int removeRangeforEachBurdness = 1100;
        List<Human> removeCivilian = new LinkedList<>();
        List<EntityID> removePos = new LinkedList<>();

        for (Human civilian : this.rescueTargets) {
            Collection<EntityID> range = worldInfo.getObjectIDsInRange(civilian, (civilian.getBuriedness() + 30) * removeRangeforEachBurdness);
            for (EntityID id : range) {
                StandardEntity entity = worldInfo.getEntity(id);
                if (entity instanceof Building) {
                    Building building = (Building) entity;
                    if (building.isFierynessDefined()
                            && building.getFieryness() > 0
                            && building.getFieryness() < 4) {
                        removePos.add(civilian.getPosition());
                        removeCivilian.add(civilian);
                    }
                }
            }
        }
        this.rescueTargets.removeAll(removeCivilian);
        targetPos.removeAll(removePos);
        return targetPos;
    }


    private EntityID getWeakerCivilian(EntityID targetPosision) {
        List<Human> civilian = new LinkedList<>();
        for (Human h : this.rescueTargets) {
            if (h.getPosition().equals(targetPosision)) {
                civilian.add(h);
            }
        }
        if (civilian == null || civilian.size() == 0)
            return null;
        Human temp = null;
        if (civilian.size() > 0)
            temp = civilian.get(0);
        if (temp != null) {
            for (Human h : civilian) {
                if (h.isDamageDefined() && h.getDamage() > temp.getDamage()) {
                    temp = h;
                }
            }
            return temp.getID();
        }
        return null;
    }

    //TODO fill it
    private boolean needRescueFromCollapse(Human human) {
        if (human.getPosition() != null) {
            StandardEntity targetPosition = worldInfo.getEntity(human.getPosition());
            if (targetPosition != null
                    && targetPosition instanceof Building
                    && ((human.isHPDefined() && human.getHP() < 600)
                    || (human.isBuriednessDefined() && human.getBuriedness() >= 5))) {
                if (((Building) targetPosition).isOnFire()) {
                    return false;
                }
            }
            if (human.isDamageDefined() && human.getDamage() > 0)
                if (human.isBuriednessDefined() && human.getBuriedness() == 0
                        && targetPosition instanceof Building) {
                    return true;
                }
        }
        if (human.isHPDefined() && human.getHP() > 0) {
            if (human.isBuriednessDefined() && human.getBuriedness() > 0
                    && this.activeTime(human) >= human.getBuriedness()) {
                return true;
            }
        }

        return false;
    }

    //TODO fill it
    private boolean needRescueFromRoad(Human human) {
        if (human.isPositionDefined()
                && worldInfo.getEntity(human.getPosition()) instanceof Road) {
            if (human.isDamageDefined() && human.getDamage() > 0) {
                if (human.isBuriednessDefined() && human.getBuriedness() == 0) {
                    if (agentInfo.getTime() + this.activeTime(human) <= 300
                            ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private boolean civilianDoesNotNeedRescue(Human human) {
        if (!human.isPositionDefined()
                || worldInfo.getEntity(human.getPosition()) instanceof Building) {
            return false;
        }
        if (human.isBuriednessDefined() && human.getBuriedness() == 0) {
            if (agentInfo.getTime() + this.activeTime(human) >= 300
                    ) {
                return true;
            }
        } else if (human.isBuriednessDefined()) {
            if (this.activeTime(human) < human.getBuriedness()) {
                return true;
            }
        }
        return false;
    }

    private boolean changeCivilianView() {
        Set<EntityID> changeSet_Civilian = worldInfo.getChanged().getChangedEntities();

        changeSet_Civilian.removeAll(this.vistedcivilian);
        for (EntityID id : changeSet_Civilian) {
            StandardEntity entity = worldInfo.getEntity(id);
            if (entity != null && entity instanceof Civilian) {
                return true;
            }
        }
        return false;
    }

    private int activeTime(Human human) {
        if (human.isHPDefined() && human.isDamageDefined()) {
            int hp = human.getHP();
            int damage = human.getDamage();
            if (hp == 0) {
                return 0;
            }
            if (human.isBuriednessDefined() && human.getBuriedness() > 0) {
                int damageIdea = 45;
                damage += human.getBuriedness() - damageIdea > 0 ? human.getBuriedness() - damageIdea : damageIdea;
                return (hp / damage) + ((hp % damage) != 0 ? 1 : 0) - 20;
            } else {
                damage += 45;
                return (hp / damage) + ((hp % damage) != 0 ? 1 : 0) - 25;
            }
        }
        return -1;
    }

    @Override
    public EntityID getTarget() {
        return this.result;
    }

    @Override
    public HumanDetector precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        this.clustering.precompute(precomputeData);
        return this;
    }

    @Override
    public HumanDetector resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
        this.clustering.resume(precomputeData);
        return this;
    }

    @Override
    public HumanDetector preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }
        this.clustering.preparate();
        return this;
    }


    private boolean isInside(double pX, double pY, int[] apex) {
        Point2D p = new Point2D(pX, pY);
        Vector2D v1 = (new Point2D(apex[apex.length - 2], apex[apex.length - 1])).minus(p);
        Vector2D v2 = (new Point2D(apex[0], apex[1])).minus(p);
        double theta = this.getAngle(v1, v2);

        for (int i = 0; i < apex.length - 2; i += 2) {
            v1 = (new Point2D(apex[i], apex[i + 1])).minus(p);
            v2 = (new Point2D(apex[i + 2], apex[i + 3])).minus(p);
            theta += this.getAngle(v1, v2);
        }
        return Math.round(Math.abs((theta / 2) / Math.PI)) >= 1;
    }

    private double getAngle(Vector2D v1, Vector2D v2) {
        double flag = (v1.getX() * v2.getY()) - (v1.getY() * v2.getX());
        double angle = Math.acos(((v1.getX() * v2.getX()) + (v1.getY() * v2.getY())) / (v1.getLength() * v2.getLength()));
        if (flag > 0) {
            return angle;
        }
        if (flag < 0) {
            return -1 * angle;
        }
        return 0.0D;
    }


    private int getDistance(double fromX, double fromY, double toX, double toY) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        return (int) Math.hypot(dx, dy);
    }
}
