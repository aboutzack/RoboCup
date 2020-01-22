package AIT_2019.module.algorithm;

import static rescuecore2.standard.entities.StandardEntityURN.AMBULANCE_CENTRE;
import static rescuecore2.standard.entities.StandardEntityURN.BUILDING;
import static rescuecore2.standard.entities.StandardEntityURN.FIRE_STATION;
import static rescuecore2.standard.entities.StandardEntityURN.GAS_STATION;
import static rescuecore2.standard.entities.StandardEntityURN.POLICE_OFFICE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.DynamicClustering;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

public class HierarchicalFireClustering extends DynamicClustering{

    private final double COEFF_DIST = .6;
    private int avgDistBtwBuilding = 10000;
    private int maxBuildingArea = -1;
    // private int minBuildingArea = -1;
    private int iterationNumber = 10;

    List<List<EntityID>> clusters = new LinkedList<>();

    // Debug
    // private final VDClient vdclient = VDClient.getInstance();
    // /Debug

    public HierarchicalFireClustering(AgentInfo ai, WorldInfo wi, ScenarioInfo si,
            ModuleManager mm, DevelopData dd)
    {
        super(ai, wi, si, mm, dd);

        // Debug
        // this.vdclient.init("localhost", 1099);
        // /Debug
    }

    @Override
    public Clustering updateInfo(MessageManager mm)
    {
        super.updateInfo(mm);
        if(this.getCountUpdateInfo() > 1) { return this; }

        this.calc();
        return this;
    }

    @Override
    public Clustering precompute(PrecomputeData pd)
    {
        super.precompute(pd);
        if(this.getCountPrecompute() > 1) { return this; }
        return this;
    }

    @Override
    public Clustering resume(PrecomputeData pd)
    {
        super.resume(pd);
        if(this.getCountResume() > 1) { return this; }
        this.preparate();
        return this;
    }

    @Override
    public Clustering preparate()
    {
        super.preparate();
        if(this.getCountPreparate() > 1) { return this; }

        this.getAverageDistanceBetweenBuilding();
        return this;
    }

    @Override
    public int getClusterNumber()
    {
        return this.clusters.size();
    }

    @Override
    public int getClusterIndex(EntityID id)
    {
        int ret = -1;
        for (List<EntityID> cluster : this.clusters) {
            if (cluster.contains(id))
            {
                ret = this.clusters.indexOf(cluster);
                break;
            }
        }
        return ret;
    }

    @Override
    public int getClusterIndex(StandardEntity entity)
    {
        return this.getClusterIndex(entity.getID());

    }

    @Override
    public Collection<EntityID> getClusterEntityIDs(int i)
    {
        return this.clusters.get(i);
    }

    @Override
    public Collection<StandardEntity> getClusterEntities(int i)
    {
        List<StandardEntity> ret = this.clusters.get(i).stream()
                .map(this.worldInfo::getEntity)
                .collect(Collectors.toList());
        return ret;
    }

    @Override
    public Clustering calc()
    {
        this.clusters.clear();
        List<EntityID> fireBuildingIDs = this.worldInfo.getEntityIDsOfType(
                BUILDING, GAS_STATION,
                AMBULANCE_CENTRE, FIRE_STATION, POLICE_OFFICE).stream()
                .map(this.worldInfo::getEntity)
                .map(Building.class::cast)
                .filter(b -> b.isOnFire())
                .map(Building::getID)
                .collect(Collectors.toList());

        if (fireBuildingIDs.isEmpty()) {return this;}

        this.clustering(fireBuildingIDs);
        this.combining();

        // Debug
        // for (List<EntityID> cluster : this.clusters)
        // {
            // StandardEntityURN urn = this.agentInfo.me().getStandardURN();
            // if (urn != StandardEntityURN.FIRE_BRIGADE) { continue; }
            // ConvexHull convexHull = new ConvexHull();
            // for (EntityID id : cluster)
            // {
                // StandardEntity entity = this.worldInfo.getEntity(id);
                // if (!(entity instanceof Area)) { continue; }
                // Area area = (Area) entity;
                // convexHull.add(area);
            // }
            // convexHull.compute();

            // this.vdclient.drawAsync(
                    // this.agentInfo.getID().getValue(),
                    // "ClusterConvexhull",
                    // (Serializable) Arrays.asList(convexHull.get()));
        // }
        // /Debug

        return this;
    }

    private void clustering(List<EntityID> fireBuildingIDs)
    {
        // for create clusters
        List<EntityID> newCluster = new ArrayList<>();
        newCluster.add(fireBuildingIDs.get(0));
        this.clusters.add(newCluster);
        for (int i = 1; i < fireBuildingIDs.size(); i++)
        {
            Building bld1 = (Building) this.worldInfo.getEntity(fireBuildingIDs.get(i));
            double minDist = Double.MAX_VALUE;
            int minIndex = -1;

            int buildingArea = bld1.getGroundArea();
            double thresholdDistanceBtwBld =
                    this.avgDistBtwBuilding * COEFF_DIST +
                    this.avgDistBtwBuilding * COEFF_DIST * buildingArea / this.maxBuildingArea * .1;
            thresholdDistanceBtwBld *= COEFF_DIST;

            for (int j = 0; j < this.clusters.size(); j++)
            {
                List<EntityID> cluster = this.clusters.get(j);
                for (EntityID bldID : cluster)
                {
                    Building bld2 = (Building) this.worldInfo.getEntity(bldID);
                    double  distance = this.worldInfo.getDistance(bld1.getID(), bld2.getID());

                    if (distance < thresholdDistanceBtwBld && distance < minDist)
                    {
                        minDist = distance;
                        minIndex = j;
                    }
                }
            }

            if (minIndex != -1)
            {
                List<EntityID> cluster = this.clusters.get(minIndex);
                cluster.add(fireBuildingIDs.get(i));
                this.clusters.set(minIndex, cluster);
            }
            else
            {
                newCluster = new ArrayList<>();
                newCluster.add(fireBuildingIDs.get(i));
                this.clusters.add(newCluster);
            }
        }
    }

    private void combining()
    {
        // for iterate to union clusters close to others
        for (int i = 0; i < this.iterationNumber; i++)
        {
            List<List<EntityID>> clusterIterated = new ArrayList<>();
            while (!this.clusters.isEmpty())
            {
                List<EntityID> newCluster = this.clusters.remove(0);
                List<List<EntityID>> removeClusters = new ArrayList<>();
                for (List<EntityID> leftCluster : this.clusters)
                {
                    if (this.isClose(newCluster, leftCluster))
                    {
                        newCluster.addAll(leftCluster);
                        removeClusters.add(leftCluster);
                    }
                }
                this.clusters.removeAll(removeClusters);
                clusterIterated.add(newCluster);
            }
            this.clusters = clusterIterated;
        }
    }

    private void getAverageDistanceBetweenBuilding()
    {
        int count = 0;
        double sumDist = 0;
        int maxArea = Integer.MIN_VALUE;
        int minArea = Integer.MAX_VALUE;

        List<StandardEntity> buildings =
                this.worldInfo.getEntitiesOfType(BUILDING).stream()
            .collect(Collectors.toList());
        for (int i = 0; i < buildings.size(); i++)
        {
            EntityID bld1ID = buildings.get(i).getID();
            int area = ((Building) buildings.get(i)).getGroundArea();
            if (area > maxArea) { maxArea = area; }
            if (area < minArea) { minArea = area; }

            for (int j = i + 1; j < buildings.size(); j++)
            {
                EntityID bld2ID = buildings.get(j).getID();
                double distance = this.worldInfo.getDistance(bld1ID, bld2ID);
                sumDist += distance;
                count++;
            }
        }

        this.avgDistBtwBuilding = (int) (sumDist / count);
        this.maxBuildingArea = maxArea;
        // this.minBuildingArea = minArea;
    }

    private boolean isClose(List<EntityID> cluster1, List<EntityID> cluster2)
    {
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < cluster1.size(); i++)
        {
            for (int j = 0; j < cluster2.size(); j++)
            {
                double distance = this.worldInfo.getDistance(
                        cluster1.get(i), cluster2.get(j));
                if (distance > minDist) { minDist = distance; }
            }
        }
        if (minDist < this.avgDistBtwBuilding * Math.pow(COEFF_DIST, 2f)) return true;
        return false;
    }
}
