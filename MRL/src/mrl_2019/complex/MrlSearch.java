package mrl_2019.complex;

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
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static rescuecore2.standard.entities.StandardEntityURN.*;


/**
 * created on: 6/21/2017
 *
 * @author Peyman
 *
*/
public class MrlSearch extends Search {
    private PathPlanning pathPlanning;
    private Clustering clustering;

    private EntityID result;
    private EntityID destination;
    private EntityID lastPosition;
    private Collection<EntityID> unSearchedBuildingIDs;
    List<EntityID> allRoads = new ArrayList<>(worldInfo.getEntityIDsOfType(StandardEntityURN.ROAD));

//    private boolean isSendBuildingMessage;
//    private boolean isSendCivilianMessage;
//    private boolean isSendRoadMessage;
//    private Collection<EntityID> agentPositions;
//    private Map<EntityID, Integer> sentTimeMap;
//    private int sendingAvoidTimeReceived;
//    private int sendingAvoidTimeSent;


/*--------------------------------------------------------------------------------------------------------------------*/
/*--------------------------C---O---N---S---T---R---U---C---T---O---R-------------------------------------------------*/
/*--------------------------------------------------------------------------------------------------------------------*/

    public MrlSearch(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);

        this.unSearchedBuildingIDs = new HashSet<>();
//        this.agentPositions = new HashSet<>();

        StandardEntityURN agentURN = ai.me().getStandardURN();
        if(agentURN == AMBULANCE_TEAM) {
            this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Ambulance", "adf.sample.module.algorithm.SamplePathPlanning");
        } else if(agentURN == FIRE_BRIGADE) {
            this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire", "adf.sample.module.algorithm.SamplePathPlanning");
        } else if(agentURN == POLICE_FORCE) {
            this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Police", "adf.sample.module.algorithm.SamplePathPlanning");
        }
    }
/*--------------------------------------------------------------------------------------------------------------------*/
/*----------------------E---N---D------O---F------C---O---N---S---T---R---U---C---T---O---R---------------------------*/
/*--------------------------------------------------------------------------------------------------------------------*/


    @Override
    public Search updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.pathPlanning.updateInfo(messageManager);
//        this.clustering.updateInfo(messageManager);

        if (this.unSearchedBuildingIDs.isEmpty()) {
            this.reset();
        }
        this.unSearchedBuildingIDs.removeAll(this.worldInfo.getChanged().getChangedEntities());

        if (this.unSearchedBuildingIDs.isEmpty()) {
            this.reset();
            this.unSearchedBuildingIDs.removeAll(this.worldInfo.getChanged().getChangedEntities());
        }
        return this;
    }


    @Override
    public Search calc() {
        if (lastPosition==null){
            lastPosition=agentInfo.getPosition();
        }
        int count = 0;
        if (agentInfo.getTime()>2) {
            return calc(count);
        }
        return this;
    }

    private Search calc(int count) {

        count++;
        this.result = null;
        this.pathPlanning.setFrom(this.agentInfo.getPosition());

//        ArrayList<EntityID> destination = new ArrayList<>();
//        destination.add(allRoads.get(Math.round((float) allRoads.size()-1)));
        if(destination == null ||
                agentInfo.getPosition().equals(destination) ||
                worldInfo.getDistance(agentInfo.getPosition(),lastPosition)<10000){
            allRoads.remove(destination);
            destination = allRoads.get((int) (Math.random()* allRoads.size()-1));
        }
        this.pathPlanning.setDestination(destination);
        allRoads.remove(destination);

        List<EntityID> path = this.pathPlanning.calc().getResult();
        if (path != null && path.size() > 0) {
            System.out.println("path found, time:"+agentInfo.getTime()+" destination:"+destination.getValue());
            this.result = path.get(path.size() - 1);
        } else if (count > 15) {
            System.err.println("time:"+agentInfo.getTime()+" destination:"+destination+" Search Path not found");
        } else {
            calc(count);
        }
        lastPosition = agentInfo.getPosition();
        return this;
    }

    private void reset() {
        this.unSearchedBuildingIDs.clear();

//        Collection<StandardEntity> clusterEntities = null;
//        if (this.clustering != null) {
//            int clusterIndex = this.clustering.getClusterIndex(this.agentInfo.getID());
//            clusterEntities = this.clustering.getClusterEntities(clusterIndex);
//
//        }
//        if (clusterEntities != null && clusterEntities.size() > 0) {
//            for (StandardEntity entity : clusterEntities) {
//                if (entity instanceof Building && entity.getStandardURN() != REFUGE) {
//                    this.unSearchedBuildingIDs.add(entity.getID());
//                }
//            }
//        } else {
//            this.unSearchedBuildingIDs.addAll(this.worldInfo.getEntityIDsOfType(
//                    BUILDING,
//                    GAS_STATION,
//                    AMBULANCE_CENTRE,
//                    FIRE_STATION,
//                    POLICE_OFFICE
//            ));
//        }
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
//        this.clustering.resume(precomputeData);
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
//        this.clustering.preparate();
        return this;
    }

}