package AUR.util.ambulance;

import adf.agent.info.AgentInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by armanaxh in 2017
 */

public class ClusterIndex {

    private int changeCounter = 0 ;
    private int agentClusterIndex ;
    private AgentInfo agentInfo ;
    private WorldInfo worldInfo ;
    private Clustering clustering ;
    private PathPlanning pathPlanning ;
    private List<EntityID> agentFirstClusterList;
    private List<Integer> lastClusterIndex ;



    public ClusterIndex(AgentInfo ai , WorldInfo wi , Clustering clustering , PathPlanning pathPlanning){
        this.agentInfo = ai ;
        this.worldInfo = wi ;
        this.clustering = clustering ;
        this.pathPlanning = pathPlanning ;
        this.lastClusterIndex = new LinkedList<>();
        this.agentFirstClusterList = new LinkedList<>();
        this.agentClusterIndex = this.clustering.getClusterIndex(ai.me());
        this.agentClusterIndex  = this.clustering.getClusterIndex(agentInfo.me());


    }
    public ClusterIndex calc(){
        this.clustering.calc();
        if(lastClusterIndex.size() == 0 ){
            this.agentClusterIndex  = this.clustering.getClusterIndex(agentInfo.me());
            lastClusterIndex.add(this.agentClusterIndex);
            return this;
        }
        Collection<EntityID> map = worldInfo.getEntityIDsOfType(StandardEntityURN.BUILDING);
//        Collection<EntityID> clusterEntity = this.clustering.getClusterEntityIDs(agentClusterIndex);
        //map.removeAll(clusterEntity);
        for(int index : this.lastClusterIndex ){
            map.removeAll(this.clustering.getClusterEntityIDs(index));
        }
        this.pathPlanning.setFrom(agentInfo.getPosition());
        this.pathPlanning.setDestination(map);
        List<EntityID> path = this.pathPlanning.calc().getResult();
        if(path != null && path.size() > 0 ) {
            EntityID target = path.get(path.size() - 1);
            if(target != null ){
                this.agentClusterIndex = this.clustering.getClusterIndex(worldInfo.getEntity(target));
            }
        }else {
            int clusterIndex = this.clustering.getClusterIndex(agentInfo.me()) ;
            int clusterSize = this.clustering.getClusterNumber();
            clusterIndex = (clusterIndex + this.changeCounter) % clusterSize;
            this.agentClusterIndex = clusterIndex ;
            this.changeCounter++;
        }
        if(!this.lastClusterIndex.contains(this.agentClusterIndex))
            this.lastClusterIndex.add(this.agentClusterIndex);

        return this;
    }
    public int getIndex(){
        if(this.agentClusterIndex == -1 ){
            this.agentClusterIndex  = this.clustering.getClusterIndex(agentInfo.me());
        }
        return this.agentClusterIndex;
    }
}
