package AUR.module.complex.self;


import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.Search;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import AUR.util.ambulance.ClusterIndex;

import java.util.*;

/**
 * Created by armanaxh in 2017
 */

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class AURPoliceSearch extends Search {
    private PathPlanning pathPlanning;
    private Clustering clustering;
    private ClusterIndex clusterIndex;
    private EntityID result;
    private ArrayList<EntityID> unsearchedBuildingIDs;
    private EntityID lastResult = new EntityID(0);
    private int resultCounter = 0;
    private Collection<StandardEntity> clusterEntities;
    private Boolean DoNotRouteinCluster = false;
    private Collection<EntityID> agentPositions;


    public AURPoliceSearch(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.unsearchedBuildingIDs = new ArrayList<>();
        this.agentPositions = new HashSet<>();
        StandardEntityURN agentURN = ai.me().getStandardURN();
        switch (si.getMode()) {
            case PRECOMPUTATION_PHASE:
                if (agentURN == AMBULANCE_TEAM) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Ambulance", "adf.sample.module.algorithm.SamplePathPlanning");
                    this.clustering = moduleManager.getModule("SampleSearch.Clustering.Ambulance", "adf.sample.module.algorithm.SampleKMeans");
                } else if (agentURN == FIRE_BRIGADE) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire", "adf.sample.module.algorithm.SamplePathPlanning");
                    this.clustering = moduleManager.getModule("SampleSearch.Clustering.Fire", "adf.sample.module.algorithm.SampleKMeans");
                } else if (agentURN == POLICE_FORCE) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Police", "adf.sample.module.algorithm.SamplePathPlanning");
                    this.clustering = moduleManager.getModule("SampleSearch.Clustering.Police", "adf.sample.module.algorithm.SampleKMeans");
                }
                break;
            case PRECOMPUTED:
                if (agentURN == AMBULANCE_TEAM) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Ambulance", "adf.sample.module.algorithm.SamplePathPlanning");
                    this.clustering = moduleManager.getModule("SampleSearch.Clustering.Ambulance", "adf.sample.module.algorithm.SampleKMeans");
                } else if (agentURN == FIRE_BRIGADE) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire", "adf.sample.module.algorithm.SamplePathPlanning");
                    this.clustering = moduleManager.getModule("SampleSearch.Clustering.Fire", "adf.sample.module.algorithm.SampleKMeans");
                } else if (agentURN == POLICE_FORCE) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Police", "adf.sample.module.algorithm.SamplePathPlanning");
                    this.clustering = moduleManager.getModule("SampleSearch.Clustering.Police", "adf.sample.module.algorithm.SampleKMeans");
                }
                break;
            case NON_PRECOMPUTE:
                if (agentURN == AMBULANCE_TEAM) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Ambulance", "adf.sample.module.algorithm.SamplePathPlanning");
                    this.clustering = moduleManager.getModule("SampleSearch.Clustering.Ambulance", "adf.sample.module.algorithm.SampleKMeans");
                } else if (agentURN == FIRE_BRIGADE) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire", "adf.sample.module.algorithm.SamplePathPlanning");
                    this.clustering = moduleManager.getModule("SampleSearch.Clustering.Fire", "adf.sample.module.algorithm.SampleKMeans");
                } else if (agentURN == POLICE_FORCE) {
                    this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Police", "adf.sample.module.algorithm.SamplePathPlanning");
                    this.clustering = moduleManager.getModule("SampleSearch.Clustering.Police", "adf.sample.module.algorithm.SampleKMeans");
                }
                break;
        }
        this.clusterIndex = new ClusterIndex(ai, wi, this.clustering, pathPlanning);

    }

    @Override
    public Search updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        if (agentInfo.getTime() < 2) {
            return this;
        }

        this.pathPlanning.updateInfo(messageManager);
        this.clustering.updateInfo(messageManager);

        this.fillAgetnPoisions();


        this.unsearchedBuildingIDs.remove(agentInfo.getPosition());
        this.removeSearchedTarget();
        this.removeFireTarget();
        this.removeOtherAgetnPos();

        if (this.DoNotRouteinCluster.equals(true) || this.unsearchedBuildingIDs.isEmpty()) {
            this.fillNextClusterEntity();
            this.DoNotRouteinCluster = false;
        }

        if (this.clusterEntities == null || this.clusterEntities.size() < 2) {
            this.fillNextClusterEntity();
        }

        if (this.unsearchedBuildingIDs.isEmpty()) {
            this.resetTarget();
            this.removeSearchedTarget();
        }
        this.targetChange();
        this.lastResult = this.result;

        return this;
    }

    private void fillAgetnPoisions() {

        for (StandardEntity entity : worldInfo.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM)) {
            if (entity.getID().equals(agentInfo.getID()))
                continue;
            if (entity instanceof AmbulanceTeam) {
                AmbulanceTeam ambo = (AmbulanceTeam) entity;
                if (ambo.getPosition() != null) {
                    if (!this.agentPositions.contains(ambo.getPosition()))
                        this.agentPositions.add(ambo.getPosition());
                }
            }
        }
    }

    private void removeOtherAgetnPos() {


        for (EntityID id : this.agentPositions) {
            StandardEntity entity = worldInfo.getEntity(id);
            if (entity instanceof Area) {
                Area area = (Area) entity;
                if (area.getNeighbours() != null)
                    this.unsearchedBuildingIDs.removeAll(area.getNeighbours());
            }
        }
    }

    private void removeFireTarget() {
        boolean removeKOnam = false;
        for (EntityID id : worldInfo.getChanged().getChangedEntities()) {
            StandardEntity entity = worldInfo.getEntity(id);
            if (entity instanceof Building) {
                Building building = (Building) entity;
                if (building.isFierynessDefined()) {

                    if (building.getFieryness() < 4
                            && building.getFieryness() > 0) {
                        removeKOnam = true;
                        break;
                    } else if (building.getFieryness() == 8) {
                        removeKOnam = true;
                        break;
                    }
                }
            }
        }
        if (removeKOnam)
            this.unsearchedBuildingIDs.removeAll(worldInfo.getChanged().getChangedEntities());

    }

    private void targetChange() {
        if (this.result == null)
            return;
        if (this.result.equals(this.lastResult)) {
            this.resultCounter++;
        } else {
            this.resultCounter = 0;
        }
        if (this.resultCounter >= 15) {
            this.unsearchedBuildingIDs.remove(this.result);
        }
    }

    private void removeSearchedTarget() {
        final int distanceOfRoad = 1500;
        Area agentPosition = agentInfo.getPositionArea();
        this.unsearchedBuildingIDs.remove(agentPosition.getID());

        for (Edge edge : agentPosition.getEdges()) {
            StandardEntity e = worldInfo.getEntity(edge.getNeighbour());
            if (e instanceof Building) {
                Double midX, midY, agentX, agentY;
                midX = Double.valueOf((edge.getStartX() + edge.getEndX()) / 2);
                midY = Double.valueOf((edge.getStartY() + edge.getEndY()) / 2);
                agentX = Double.valueOf((int) agentInfo.getX());
                agentY = Double.valueOf((int) agentInfo.getY());
                Double distanceFormola = this.getDistance(midX, midY, agentX, agentY);//(midX-agentX)*(midX-agentX) + (midY-agentY)*(midY-agentY);
                if (distanceFormola < distanceOfRoad && distanceFormola > -1) {
                    this.unsearchedBuildingIDs.remove(e.getID());
                    continue;
                }//TODO
                //ina ro karetoun nabashe :D ActionMoveNULL
                Double x1 = ((agentPosition).getX() + 3 * midX) / 4;
                Double y1 = ((agentPosition).getY() + 3 * midY) / 4;
                distanceFormola = this.getDistance(x1, y1, agentX, agentY); //(x1-agentX)*(x1-agentX) + (y1-agentY)*(y1-agentY) ;
                if (distanceFormola < 200 && distanceFormola > -1) {
                    this.unsearchedBuildingIDs.remove(e.getID());
                }
            }
        }
        for (EntityID id : agentInfo.getChanged().getChangedEntities()) {
            StandardEntity entity = worldInfo.getEntity(id);
            if (entity instanceof Building
                    && (((Building) entity).isOnFire()
                    || (((Building) entity).isBrokennessDefined() && ((Building) entity).getBrokenness() == 0)
                    || (((Building) entity).isFierynessDefined() && ((Building) entity).getFieryness() == 8))) {
                this.unsearchedBuildingIDs.remove(entity.getID());
            }
        }
    }

    //TODO
    private double getDistance(double fromX, double fromY, double toX, double toY) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        return Math.hypot(dx, dy);
    }

    @Override
    public Search calc() {
        this.result = null;
        if (agentInfo.getTime() < 2) {
            return this;
        }

        this.removeSearchedTarget();

        this.pathPlanning.setFrom(this.agentInfo.getPosition());
        this.pathPlanning.setDestination(this.unsearchedBuildingIDs);
        List<EntityID> path = this.pathPlanning.calc().getResult();
        if (path != null && path.size() > 0) {
            this.result = path.get(path.size() - 1);
        } else if (path == null || path.size() < 1) {
//            System.out.println("  Has NOT a Route for agent ++++++++++++");
            this.DoNotRouteinCluster = true;
            this.unsearchedBuildingIDs.clear();
        }

        return this;
    }

    private void fillNextClusterEntity() {
        if (this.clustering != null) {
//            int clusterIndex = this.clustering.getClusterIndex(agentInfo.me());
//            int clusterSize = this.clustering.getClusterNumber();
//            clusterIndex = (clusterIndex + this.clusterIndexCounter) % clusterSize;
            int clusterIndex = this.clusterIndex.calc().getIndex();
            clusterEntities = this.clustering.getClusterEntities(clusterIndex);
        }
    }

    private void resetTarget() {
        this.unsearchedBuildingIDs.clear();
        if (clusterEntities != null && clusterEntities.size() > 0) {

            for (StandardEntity entity : clusterEntities) {
                if (entity instanceof Building
                        && !(entity instanceof Refuge)
                        && !(entity instanceof AmbulanceCentre)
                        && !(entity instanceof PoliceOffice)
                        && !(entity instanceof FireStation)
                        && !(((Building) entity).isOnFire())
                        && !(((Building) entity).isFierynessDefined() && ((Building) entity).getFieryness() == 8)
                        && (!((Building) entity).isBrokennessDefined() || (((Building) entity).isBrokennessDefined()
                        && ((Building) entity).getBrokenness() != 0))) {
                    this.unsearchedBuildingIDs.add(entity.getID());
                }
            }
        }
    }


    private boolean isInsideBlockade() {

        Human agent = (Human) this.agentInfo.me();
        int agentX = agent.getX();
        int agentY = agent.getY();
        StandardEntity positionEntity = this.worldInfo.getPosition(agent);

        if (positionEntity instanceof Road) {
            Road road = (Road) positionEntity;
            if (road.isBlockadesDefined() && road.getBlockades().size() > 0) {
                for (Blockade blockade : this.worldInfo.getBlockades(road)) {
                    if (blockade == null || !blockade.isApexesDefined()) {
                        continue;
                    }
                    if (this.isInside(agentX, agentY, blockade.getApexes())) {
                        return true;
                    }
                }
            }
        }
        return false;
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


    @Override
    public EntityID getTarget() {
        return this.result;
    }

    @Override
    public Search precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        this.pathPlanning.precompute(precomputeData);
        this.clustering.precompute(precomputeData);

        return this;
    }

    @Override
    public Search resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
        this.worldInfo.requestRollback();
        this.pathPlanning.resume(precomputeData);
        this.clustering.resume(precomputeData);
        return this;
    }

    @Override
    public Search preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }
        this.worldInfo.requestRollback();
        this.pathPlanning.preparate();
        this.clustering.preparate();

        return this;
    }
}