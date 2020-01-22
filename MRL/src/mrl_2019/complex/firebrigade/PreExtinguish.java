package mrl_2019.complex.firebrigade;

import adf.agent.action.Action;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.module.algorithm.PathPlanning;
import firesimulator.world.Building;
import mrl_2019.algorithm.clustering.Cluster;
import mrl_2019.algorithm.clustering.ConvexHull;
import mrl_2019.algorithm.simpleSearch.Graph;
import mrl_2019.extaction.ActionFireFighting;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static rescuecore2.standard.entities.StandardEntityURN.*;
import static rescuecore2.standard.entities.StandardEntityURN.FIRE_STATION;


/**
 * @author ShivaZarei
 * 28/05/2019
 */



    /**this class examines whether the building will ignite in 1 or 2 next cycles
     */

    public class PreExtinguish  {

        private AgentInfo agentInfo;
        //private WorldInfo worldInfo;
        private Cluster targetCluster;
        private MrlFireBrigadeWorld worldHelper;
        private Building building;
        private rescuecore2.standard.entities.Building buildings;
        private Set Entities;
        private WorldInfo worldInfo;
        Collection<StandardEntity> clusterEntities;
        private EntityID entityId;
        private List entityIDs;
        //private Map<EntityID, Set<EntityID>> graph;
        private Set<EntityID> neighbors;
        private double value;
        private StandardEntity entity;
        private ScenarioInfo scenarioInfo;
        private ModuleManager moduleManager;
        private DevelopData developData;
        private int k;
        protected Action result;




        public Action calc(FireBrigade fireBrigadeAgent, PathPlanning pathPlanning, EntityID entityId) {
            try {



            targetCluster = worldHelper.getFireClustering().findNearestCluster((worldHelper.getSelfLocation()));


            Entities = targetCluster.getEntities();
            //  StandardEntity entity = (StandardEntity) buildingEntities;

            WorldInfo buildingEntities = (WorldInfo) Entities;


            //MrlBuilding greyPrediction = new MrlBuilding(entity, worldHelper, worldInfo, agentInfo);


            ActionFireFighting actionFireFighting = new ActionFireFighting(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);



            for (StandardEntity entity : buildingEntities.getEntitiesOfType(BUILDING, AMBULANCE_CENTRE, POLICE_OFFICE, FIRE_STATION, GAS_STATION)) {


                rescuecore2.standard.entities.Building building = (rescuecore2.standard.entities.Building) entity;
                GreyPrediction greyPrediction = new GreyPrediction();


                if (!(building.isOnFire())) {
                    value = greyPrediction.calc(k = 2);
                    if (value >= 20) {
                        actionFireFighting.calc();
                    }
                }


                if (building.isOnFire()) {


                    clusterEntities.add(building);
                    entityId = building.getID();
//                    Area entityID = (Area) entityId;
                    Object entityID = (Object) entityId;
                    entityIDs.add(entityID);


                    for (Object id : entityIDs) {
                        EntityID ID = (EntityID) id;
                        Graph graph = new Graph(entityIDs);
                        neighbors = graph.getNeighbors(ID);


                        for (EntityID neighbor : neighbors) {
//                rescuecore2.standard.entities.Building neighborBuilding = (rescuecore2.standard.entities.Building) neighbor;
                            Object object = (Object) neighbor;
                            rescuecore2.standard.entities.Building neighborBuilding = (rescuecore2.standard.entities.Building) object;


                            if (!(neighborBuilding.isOnFire())) {
                                //MrlBuilding greyPrediction = new MrlBuilding(StandardEntity entities, MrlWorldHelper worldHelper, WorldInfo worldInfo, AgentInfo agentInfo);
                                //    value=greyPrediction.getEstimatedTemperature();

                                value = greyPrediction.calc(k = 1);


                                if (value >= 20) {//**?

                                    this.result = actionFireFighting.calcExtinguish(fireBrigadeAgent, pathPlanning, entityId);
                                }
                            }
                        }
                    }
                }
            }
                return this.result;
            }

            catch(Exception e){
                e.printStackTrace();
            }
            return this.result;

        }


        private Polygon createConvexHull(Collection<StandardEntity> clusterEntities) {
            ConvexHull convexHull = new ConvexHull();


            for (StandardEntity entity : clusterEntities) {

                if (entity instanceof rescuecore2.standard.entities.Building) {
                    rescuecore2.standard.entities.Building building = (rescuecore2.standard.entities.Building) entity;
                    for (int i = 0; i < building.getApexList().length; i += 2) {
                        convexHull.addPoint(building.getApexList()[i],
                                building.getApexList()[i + 1]);
                    }
                }
            }
            return convexHull.convex();
        }


    }
