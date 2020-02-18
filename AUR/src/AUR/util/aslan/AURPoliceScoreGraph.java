package AUR.util.aslan;

import AUR.util.AURCommunication;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURConstants;
import AUR.util.knd.AURGeoUtil;
import AUR.util.knd.AURUtil;
import AUR.util.knd.AURWorldGraph;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.information.MessageAmbulanceTeam;
import adf.agent.communication.standard.bundle.information.MessageBuilding;
import adf.agent.communication.standard.bundle.information.MessageCivilian;
import adf.agent.communication.standard.bundle.information.MessageFireBrigade;
import adf.agent.communication.standard.bundle.information.MessagePoliceForce;
import adf.agent.communication.standard.bundle.information.MessageRoad;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.AbstractModule;
import adf.component.module.algorithm.Clustering;
import com.google.common.collect.Lists;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.GasStation;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Amir Aslan Aslani - Mar 2018
 */
public class AURPoliceScoreGraph extends AbstractModule {
        public HashMap<EntityID, AURAreaGraph> areas = new HashMap<>();
        private final Clustering clustering;
        public AgentInfo ai;
        public WorldInfo wi;
        public ScenarioInfo si;
        public AURWorldGraph wsg;
        
        private final AURCommunication communication;
        
        public ArrayList<AURAreaGraph> areasForScoring = new ArrayList<>();
        
        public Collection<EntityID> clusterEntityIDs;
        public Collection<EntityID> neighbourClustersEntityIDs = new HashSet<>();
        int cluseterIndex;
        double myClusterCenter[] = new double[2];
        StandardEntity myClusterCenterEntity = null;
        double maxDistToCluster = 0;
        HashMap<EntityID, EntityID> startPositionOfAgents = new HashMap<>();
        
        double maxDisFromAgentStartPoint = 0;
        
        public AURPoliceScoreGraph(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
                super(ai, wi, si, moduleManager, developData);
                this.ai = ai;
                this.si = si;
                this.wi = wi;
                
                this.communication = new AURCommunication(ai, wi, si, developData);
                this.wsg = moduleManager.getModule("knd.AuraWorldGraph");
                this.wsg.calc();
                
                this.clustering = moduleManager.getModule("SampleRoadDetector.Clustering", "AUR.module.algorithm.AURWorldClusterer");
                this.cluseterIndex = this.clustering.calc().getClusterIndex(ai.me());
                this.clusterEntityIDs = this.clustering.getClusterEntityIDs(cluseterIndex);
                
                fillLists();
                setScores();
        }

        private void fillLists(){
                this.areas = wsg.areas;
                
                // ---
                
                Rectangle clusterBound = wsg.myClusterBounds;
                wi.getObjectsInRectangle(clusterBound.x, clusterBound.y, clusterBound.x + clusterBound.width, clusterBound.y + clusterBound.height);
                
                // ---
                
                int counter = 0;
                
                for(StandardEntity se : this.clustering.getClusterEntities(cluseterIndex)){
                        if(se instanceof Area){
                                myClusterCenter[0] += ((Area) se).getX();
                                myClusterCenter[1] += ((Area) se).getY();
                                counter ++;
                        }
                }
                myClusterCenter[0] /= counter;
                myClusterCenter[1] /= counter;
                double dis = Double.MAX_VALUE;
                
                for(StandardEntity se : this.clustering.getClusterEntities(cluseterIndex)){
                        if(se instanceof Area && Math.hypot(myClusterCenter[0] - ((Area) se).getX(), myClusterCenter[1] - ((Area) se).getY()) < dis){
                                dis = Math.hypot(myClusterCenter[0] - ((Area) se).getX(), myClusterCenter[1] - ((Area) se).getY());
                                myClusterCenterEntity = se;
                        }
                }
                myClusterCenter[0] = ((Area) myClusterCenterEntity).getX();
                myClusterCenter[1] = ((Area) myClusterCenterEntity).getY();
                
                // ---
                
                for(Integer index : wsg.neighbourClusters){
                        neighbourClustersEntityIDs.addAll(clustering.getClusterEntityIDs(index));
                }
                
                // ---
                
                for(AURAreaGraph entity : wsg.areas.values()){
                        if( Math.hypot(entity.getX() - myClusterCenter[0], entity.getY() - myClusterCenter[1]) > maxDistToCluster){
                                maxDistToCluster = Math.hypot(entity.getX() - myClusterCenter[0], entity.getY() - myClusterCenter[1]);
                        }
                }
                maxDistToCluster += 50;
                
                // --
                
                Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> worldBounds = wi.getWorldBounds();
                
                int[][] bound = new int[][]{
                        {worldBounds.first().first(),worldBounds.first().second()},
                        {worldBounds.second().first(),worldBounds.second().second()},
                        {worldBounds.first().first(),worldBounds.second().second()},
                        {worldBounds.second().first(),worldBounds.first().second()}
                };
                for(int[] point : bound){
                        if(Math.hypot(point[0] - agentInfo.getX(), point[1] - agentInfo.getY()) > maxDisFromAgentStartPoint)
                                maxDisFromAgentStartPoint = Math.hypot(point[0] - agentInfo.getX(), point[1] - agentInfo.getY());
                }
        }
        
        @Override
        public AbstractModule calc() {
                wsg.calc();
                return this;
        }

        @Override
        public AbstractModule resume(PrecomputeData precomputeData) {
                super.resume(precomputeData); 
                wsg.resume(precomputeData);
                return this;
        }

        @Override
        public AbstractModule precompute(PrecomputeData precomputeData) {
                super.precompute(precomputeData);
                wsg.precompute(precomputeData);
                return this;
        }

        @Override
        public AbstractModule preparate() {
                super.preparate();
                wsg.preparate();
                return this;
        }
        
        public EntityID getAreaWithMaximumScore(){
                return this.areasForScoring.get(0).area.getID();
        }
        
        public HashSet<EntityID> visitedBuildings = new HashSet<>();
        public void updateVisitedBuildings() {
                boolean intersect = false;
                Set<EntityID> changedEntities = agentInfo.getChanged().getChangedEntities();
                
                if(changedEntities == null)
                        return;
                
                for (EntityID id : changedEntities) {
                        StandardEntity se = worldInfo.getEntity(id);
                        
                        if (se instanceof Civilian){
                                AURAreaGraph ag = wsg.getAreaGraph(((Civilian) se).getPosition());
                                if(ag != null && ag.isBuilding() && ! ag.isRefuge()){
                                        visitedBuildings.add(((Civilian) se).getPosition());
                                }
                        }
                        
                        if (se instanceof Building
                            && worldInfo.getDistance(agentInfo.getID(), id) < scenarioInfo.getPerceptionLosMaxDistance()) {
                            Building building = (Building) se;
                            intersect = false;
                            
                                for (StandardEntity entity : worldInfo.getObjectsInRange(agentInfo.getID(), scenarioInfo.getPerceptionLosMaxDistance())) {
                                        
                                        if (entity instanceof Area) {
                                                Area area = (Area) entity;
                                                
                                                if (entity instanceof Road) {
                                                        continue;
                                                }
                                                
                                                for (Edge edge : area.getEdges()) {
                                                        double[] d = new double[2];
                                                        if (edge.isPassable()) {
                                                                continue;
                                                        }
                                                        
                                                        if (AURGeoUtil.getIntersection(
                                                            edge.getStartX(), edge.getStartY(),
                                                            edge.getEndX(), edge.getEndY(),
                                                            agentInfo.getX(), agentInfo.getY(),
                                                            building.getX(), building.getY(),
                                                            d)) {
                                                                
                                                                intersect = true;
                                                                break;
                                                        }
                                                }
                                                if (intersect == true) {
                                                        break;
                                                }
                                        }

                                }
                                AURAreaGraph b = wsg.getAreaGraph(building.getID());
                                if(b != null && b.isBuilding()){
                                        Polygon sightAreaPolygon = wsg.getAreaGraph(building.getID()).getBuilding().getSightAreaPolygon();
                                        if (intersect == false && sightAreaPolygon != null && sightAreaPolygon.contains(ai.getX(), ai.getY())) {
                                                visitedBuildings.add(building.getID());
                                        }
                                }
                        }
                }
        }

        @Override
        public AbstractModule updateInfo(MessageManager messageManager) {
                super.updateInfo(messageManager);
                communication.updateInfo(messageManager);
                
                long sTime = System.currentTimeMillis();
                System.out.println("Updating RoadDetector Scores...");
                
                wsg.updateInfo(messageManager);
                wsg.KStarNoBlockade(ai.getPosition());
                wsg.KStar(ai.getPosition());
                updateVisitedBuildings();
                
                // set dynamic communication score
                setPoliceForcesComScore(communication.getPfMessage());
                setAmbulanceTeamComScore(communication.getAtMessage());
                setFireBrigadeComScore(communication.getFbMessage());
                setBuildingsComScore(communication.getBuildingMessage());
                setCiviliansComScore(communication.getCivilianMessage());
                setRoadsComScore(communication.getRoadMessage());
                
                // Set dynamic agent changeset scores
                decreasePoliceTravelAreasScore(AURConstants.RoadDetector.DECREASE_POLICE_AREA_SCORE);
                setDeadPoliceClusterScore(AURConstants.RoadDetector.SecondaryScore.DEAD_POLICE_CLUSTER / this.clustering.getClusterNumber() * 2);
                setReleasedAgentStartEntityScore(AURConstants.RoadDetector.SecondaryScore.RELEASED_AGENTS_START_POSITION_SCORE);
                setBuildingsThatIKnowWhatInThat(AURConstants.RoadDetector.SecondaryScore.BUILDINGS_THAT_I_KNOW_WHAT_IN_THAT); // Building Info
                
                if(ai.getChanged().getChangedEntities() != null){
                        for(EntityID eid : ai.getChanged().getChangedEntities()){
                                resetAgentsBlockadeScoreWhenNoBodyIsHere(eid);
                                
                                setPolicesClusterThatMaybeBlockedWhenSeeThatNotBlockedLooool(eid, - AURConstants.RoadDetector.BaseScore.CLUSTER);
                                setFiredBuildingsScore(eid);
                                setBlockedHumansScore(eid, AURConstants.RoadDetector.SecondaryScore.BLOCKED_HUMAN);
                                setRoadsWithoutBlockadesScore(eid, AURConstants.RoadDetector.SecondaryScore.ROADS_WITHOUT_BLOCKADES);
//                                setBlockedBuildingsThatContainsCiviliansScore(eid, AURConstants.RoadDetector.SecondaryScore.BUILDINGS_THAT_CONTAINS_CIVILANS);
//                                setBuildingsDontContainsCivilianScore(eid, AURConstants.RoadDetector.SecondaryScore.BUILDINGS_DONT_CONTAINS_CIVILIAN);
                                setBlockedBuildingScore(eid, AURConstants.RoadDetector.SecondaryScore.BLOCKED_BUILDINGS);
                                setOpenBuildingsScore(eid); // Score (Range) is (0 - ) 1 (Because of default value of targetScore)
                        }
                }
                
                for(AURAreaGraph area : wsg.areas.values()){
                        setDistanceScore(area, AURConstants.RoadDetector.SecondaryScore.DISTANCE);
                }
                
                areasForScoring.sort(new AURPoliceAreaScoreComparator());
                
                System.out.println("PSG UpdateInfo Time (Contains WSG UpdateInfo): " + (System.currentTimeMillis() - sTime));
                return this;
        }

        private void setScores() {
                long sTime = System.currentTimeMillis();
                
                wsg.KStarNoBlockade(ai.getPosition());
                
                decreasePoliceTravelAreasScore(AURConstants.RoadDetector.DECREASE_POLICE_AREA_SCORE);
                
                setPoliceForceScore(AURConstants.RoadDetector.BaseScore.POLICE_FORCE);
                setFireBrigadeScore(AURConstants.RoadDetector.BaseScore.FIRE_BRIGADE);
                setAmbulanceTeamScore(AURConstants.RoadDetector.BaseScore.AMBULANCE_TEAM);
                setPolicesClusterThatMaybeBlocked(AURConstants.RoadDetector.BaseScore.CLUSTER);
                
                for(AURAreaGraph area : wsg.areas.values()){
                        /* Distance Score */
                        setDistanceScore(area, AURConstants.RoadDetector.BaseScore.DISTANCE);
                        
                        /* Building Importance */
                        addRefugeScore(area, AURConstants.RoadDetector.BaseScore.REFUGE);
                        addGasStationScore(area, AURConstants.RoadDetector.BaseScore.GAS_STATION);
                        addHydrandScore(area, AURConstants.RoadDetector.BaseScore.HYDRANT);
                        
                        /* WSG Road Score */
                        addWSGRoadScores(area, AURConstants.RoadDetector.BaseScore.WSG_ROAD);
                        
                        /* Cluster Score */
                        addClusterScore(area, AURConstants.RoadDetector.BaseScore.CLUSTER);
                        
                        /* Entrance Number Score */
                        addEntrancesNumberScore(area, AURConstants.RoadDetector.BaseScore.ENTRANCES_NUMBER);
                        
                        /* Small Areas Score */
                        addSmallRoadsScore(area);
                }
                areasForScoring.addAll(wsg.areas.values());
                areasForScoring.sort(new AURPoliceAreaScoreComparator());
                
                System.out.println("PSG Setting Base Scores Time: " + (System.currentTimeMillis() - sTime));
        }

        public static double addWSGRoadScores(AURAreaGraph area, double score) {
                if(area.isRoad()){
                        area.baseScore += area.getScore() * score;
                        if(area.getScore() < 0.005)
                                area.targetScore = 0.2;
                        return area.getScore() * score;
                }
                else{
                        area.baseScore += score / 2;
                        return score / 2;
                }
        }

        public static double addRefugeScore(AURAreaGraph area, double score) {
                if(area.isBuilding() && area.isRefuge()){
                        for(AURAreaGraph ag : area.neighbours){
                                if(ag.isRoad()){
                                        ag.baseScore += score * 2 / 3;
                                }
                        }
                        area.baseScore += score;
                }
                score = 0;
                return score;
        }

        private void addClusterScore(AURAreaGraph area, double score) {
                if(clusterEntityIDs.contains(area.area.getID())){
                        // Full Score Added
                }
                else if(area.isRefuge() && neighbourClustersEntityIDs.contains(area.area.getID())){
                        // Full Score Added
                }
                else if(neighbourClustersEntityIDs.contains(area.area.getID())){
                        score *= 1 / 2;
                }
                else{
                        double distanceFromCluster = Math.hypot(area.getX() - myClusterCenter[0], area.getY() - myClusterCenter[1]) / this.maxDistToCluster;
                        score *= (1 - distanceFromCluster) * 1 / 3;
                }
                
                area.baseScore += score;
        }
        
        HashSet<EntityID> visitedAreas = new HashSet<>();
        private void decreasePoliceTravelAreasScore(double score) {
                ArrayList<StandardEntity> travelAreas = AURUtil.getTravelAreas(wi, (Human) ai.me());
                if(travelAreas != null){
                        for(StandardEntity se : travelAreas){
                                setTargetAsReached(se.getID(), score);
                        }
                }
        }

        private void setTargetAsReached(EntityID entity,double score){
                AURAreaGraph get = this.areas.get(entity);
                get.targetScore = score;
                visitedAreas.add(entity);
        }
        
        public void setTargetScore(EntityID entity,double score){
                this.areas.get(entity).secondaryScore += score;
        }
        
        HashSet<EntityID> visitedBlockedHumans = new HashSet<>();
        private void setBlockedHumansScore(EntityID eid, double score) {
                StandardEntity entity = wi.getEntity(eid);
                if(entity instanceof Civilian || entity instanceof AmbulanceTeam || entity instanceof FireBrigade){
                        if(visitedBlockedHumans.contains(eid))
                                return;
                        
                        if(entity instanceof Civilian){
                                Civilian civilian = ((Civilian)entity);
                                
                                boolean isThereGasStation = false;
                                for(StandardEntity se : wi.getEntitiesOfType(StandardEntityURN.GAS_STATION)){
                                        GasStation gs = (GasStation) se;
                                        if(Math.hypot(gs.getX() - civilian.getX(), gs.getY() - civilian.getY()) < AURConstants.RoadDetector.GAS_STATION_EXPLODE_DISTANCE){
                                                isThereGasStation = true;
                                                break;
                                        }
                                }
                                
                                if(! isThereGasStation)
                                        score = 0;
                        }
                        
                        AURAreaGraph pos = wsg.getAreaGraph(((Human) entity).getPosition());
                        if(pos.getTravelCost() == AURConstants.Math.INT_INF || pos.getNoBlockadeTravelCost() * 4 < pos.getTravelCost()){
                                pos.secondaryScore += score;
                                visitedBlockedHumans.add(eid);
                        }
                }
        }

        public void addGasStationScore(AURAreaGraph area, double score) {
                if(area.isGasStation()){
                        Collection<EntityID> objectIDsInRange = wi.getObjectIDsInRange(
                                area.getX(),
                                area.getY(),
                                si.getFireExtinguishMaxDistance()
                        );
                        
                        Polygon circle = AURGeoTools.getCircle(new int[]{area.getX(), area.getY()}, si.getFireExtinguishMaxDistance());
                        
                        for(AURAreaGraph ag : wsg.getAreaGraph(objectIDsInRange)){
                                if(ag.isRoad() && AURGeoUtil.intersectsOrContains(circle, ag.polygon)){
                                        ag.baseScore += score;
                                }
                        }
                }
        }

        public static double addHydrandScore(AURAreaGraph area, double score) {
                if(! area.isHydrant()){
                        area.baseScore += score;
                        return score;
                }
                return 0;
        }

        private void setDistanceScore(AURAreaGraph area, double score) {
                area.distanceScore = (1 - score) + Math.min((AURConstants.RoadDetector.DIST_SCORE_COEFFICIENT / (double) area.getNoBlockadeTravelCost()) * score, score);
        }

        HashSet<EntityID> visitedDeadPoliceForces = new HashSet<>();
        private void setDeadPoliceClusterScore(double score) {
                for(StandardEntity se : wi.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)){
                        if(! visitedDeadPoliceForces.contains(se.getID()) && ((PoliceForce) se).isHPDefined() && ((PoliceForce) se).getHP() < 100 && ! se.getID().equals(ai.getID())){
                                visitedDeadPoliceForces.add(se.getID());
                                int clusterIndex = this.clustering.getClusterIndex(se);
                                for(AURAreaGraph area : wsg.getAreaGraph(clustering.getClusterEntityIDs(clusterIndex))){
                                        area.baseScore += score;
                                }
                        }         
                }
        }

        HashMap<EntityID,AURAreaGraph> agentsInBuildings = new HashMap<>();
        
        private void setReleasedAgentStartEntityScore(double score){
                for(EntityID eid : ai.getChanged().getChangedEntities()){
                        if(agentsInBuildings.containsKey(eid) && wsg.getAreaGraph(((Human) wi.getEntity(eid)).getPosition()).isRoad()){
                                wsg.getAreaGraph(((Human) wi.getEntity(eid)).getPosition()).targetScore = score;
                                agentsInBuildings.remove(eid);
                        }
                }
        }
        
        private void setPoliceForceScore(double score) {
                for(StandardEntity se : wi.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)){
                        if(se.getID().equals(ai.getID())){
                                continue;
                        }
                        
                        AURAreaGraph areaGraph = wsg.getAreaGraph(((PoliceForce) se).getPosition());
                        
                        if(areaGraph.isBuilding() || (areaGraph.isRoad() && areaGraph.goundArea > wsg.worldGridSize * wsg.worldGridSize * 2 && areaGraph.getScore() > 0.8)){
                                agentsInBuildings.put(se.getID(),areaGraph);
                                areaGraph.baseScore += score;
                        }
                }
        }
        
        private void setFireBrigadeScore(double score) {
                for(StandardEntity se : wi.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE)){
                        AURAreaGraph areaGraph = wsg.getAreaGraph(((FireBrigade) se).getPosition());
                        
                        if(areaGraph.isBuilding()){
                                agentsInBuildings.put(se.getID(),areaGraph);
                                score *= 2.0 / 3.0;
                        }
                        else if(areaGraph.getScore() < 0.5){
                                score = 0;
                        }
                        
                        areaGraph.baseScore += score;
                }
        }

        private void setAmbulanceTeamScore(double score) {
                for(StandardEntity se : wi.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM)){
                        AURAreaGraph areaGraph = wsg.getAreaGraph(((AmbulanceTeam) se).getPosition());
                        
                        if(areaGraph.isBuilding()){
                                agentsInBuildings.put(se.getID(),areaGraph);
                                score *= 2.0 / 3.0;
                        }
                        else if(areaGraph.getScore() < 0.5){
                                score = 0;
                        }
                        
                        areaGraph.baseScore += score;
                }
        }

        private void setRoadsWithoutBlockadesScore(EntityID eid, double score) {
                AURAreaGraph areaGraph = wsg.getAreaGraph(eid);

                if(areaGraph != null &&
                   areaGraph.isRoad() &&
                   areaGraph.area.isBlockadesDefined() &&
                   areaGraph.area.getBlockades().isEmpty()){

                        areaGraph.targetScore = score;
                }
        }

        private void setOpenBuildingsScore(EntityID eid) {
                AURAreaGraph areaGraph = wsg.getAreaGraph(eid);
                if(areaGraph != null && areaGraph.isBuilding()){
                        int all = 0, open = 0;

                        for(AURAreaGraph ag : areaGraph.neighbours){
                                Edge edgeTo = ag.area.getEdgeTo(areaGraph.area.getID());
                                if(edgeTo.isPassable()){
                                        all ++;

                                        if(!ag.area.isBlockadesDefined()){
                                                continue;
                                        }

                                        int size = AURPoliceUtil.filterAlirezaPathBug(wsg.getPathToClosest(ai.getPosition(), Lists.newArrayList(ag.area.getID()))).size();
                                        int size1 = AURPoliceUtil.filterAlirezaPathBug(wsg.getPathToClosest(ai.getPosition(), Lists.newArrayList(areaGraph.area.getID()))).size();
                                        if(size1 != 0 && Math.abs(size - size1) == 1){
                                                open ++;
                                        }
                                }
                        }

                        areaGraph.targetScore = Math.min((all - open) / all, areaGraph.targetScore);
                }
        }

        private void setFiredBuildingsScore(EntityID eid) {
                AURAreaGraph areaGraph = wsg.getAreaGraph(eid);
                if(areaGraph != null && areaGraph.isBuilding()){
                        Building b = (Building) areaGraph.area;
                        if(b.isFierynessDefined() && ! isThereCivilanInBuilding(b)){
                                if(b.getFieryness() >= 2){
                                        areaGraph.targetScore = 0;
                                }
                        }
                }
        }

        public static double addEntrancesNumberScore(AURAreaGraph area, double score) {
                if(area.borders.size() == 0)
                        return 0;
                
                if(area.borders.size() >= 3){
                        double coo = 1 - (1 / area.borders.size());
                        score *= coo;
                }
                else{
                        score = 0;
                }
                area.baseScore += score;
                return score;
        }

        private boolean isThereCivilanInBuilding(Building b) {
                Collection<StandardEntity> entitiesOfType = wi.getEntitiesOfType(StandardEntityURN.CIVILIAN);
                for(StandardEntity entity : entitiesOfType){
                        if(entity instanceof Civilian && ((Civilian) entity).getPosition().equals(b.getID())){
                                return true;
                        }
                }
                return false;
        }

        HashSet<EntityID> visitedCivilians = new HashSet<>();
        private void setBlockedBuildingsThatContainsCiviliansScore(EntityID eid, double score) {
                StandardEntity entity = wi.getEntity(eid);
                if(entity instanceof Civilian){
                        AURAreaGraph areaGraph = wsg.getAreaGraph(((Civilian) entity).getPosition());
                        if(areaGraph.isBuilding()){
                                if((! ((Civilian)entity).isHPDefined() ||
                                   (((Civilian)entity).isHPDefined() &&
                                   ((Civilian)entity).getHP() > 20)) &&
                                   areaGraph.getTravelCost() == AURConstants.Math.INT_INF &&
                                   ! visitedCivilians.contains(eid)){
                                        
                                        areaGraph.secondaryScore += score;
                                        areaGraph.targetScore = 1;
                                        visitedCivilians.add(eid);
                                }
                                else if(areaGraph.getTravelCost() != AURConstants.Math.INT_INF){
                                        
                                        areaGraph.targetScore = 0;
                                        visitedCivilians.remove(eid);
                                }
                        }
                }
        }

        private void setBuildingsDontContainsCivilianScore(EntityID eid, double score) {
                StandardEntity entity = wi.getEntity(eid);
                
                if(entity instanceof Building && ! (entity instanceof Refuge)){
                        Building b = (Building) entity;
                        int[] line = new int[]{
                                (int) ai.getX(),
                                (int) ai.getY(),
                                b.getX(),
                                b.getY()
                        };
                        
                        if(!( b.isBrokennessDefined() && b.getBrokenness() == 0 && ! isThereCivilanInBuilding(b)))
                                return;
                        
                        double len = Math.hypot(line[0] - line[2], line[1] - line[3]);
                        Collection<StandardEntity> objectsInRange = wi.getObjectsInRectangle(line[0], line[1], line[2], line[3]);
                        for(StandardEntity se : objectsInRange){
                                if(se instanceof Building){
                                        for(Edge e : ((Building)se).getEdges()){
                                                if(AURGeoUtil.getIntersection(line[0], line[1], line[2], line[3], e.getEndX(), e.getEndY(), e.getStartX(), e.getStartY(), new double[2]))
                                                        return;
                                        }
                                }
                        }
                        
                        wsg.getAreaGraph(eid).secondaryScore = score;
                }
        }

        HashSet<EntityID> visitedBuildingsThatBlocked = new HashSet<>();
        private void setBlockedBuildingScore(EntityID eid, double score){
                AURAreaGraph areaGraph = wsg.getAreaGraph(eid);
                if(areaGraph != null &&
                   areaGraph.getTravelCost() == AURConstants.Math.INT_INF &&
                   ! visitedBuildingsThatBlocked.contains(eid)){
                        areaGraph.secondaryScore += score;
                        visitedBuildingsThatBlocked.add(eid);
                }
        }

        HashSet<EntityID> policesMaybeBlocked = new HashSet<>();
        private void setPolicesClusterThatMaybeBlocked(double score) {
                if(wsg.neighbourClusters.isEmpty())
                        return;
                
                for(StandardEntity se : wi.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)){
                        PoliceForce p = (PoliceForce) se;
                        
                        if(p.getID().equals(ai.me().getID()))
                                continue;
                        
                        AURAreaGraph entity = wsg.getAreaGraph(p.getPosition());
                        if(entity.isBuilding() && ! entity.isRefuge()){
                                int clusterIndex = clustering.getClusterIndex(se.getID());
                                if(wsg.neighbourClusters.contains(new Integer(clusterIndex))){
                                        policesMaybeBlocked.add(se.getID());
                                        for(AURAreaGraph ag : wsg.getAreaGraph(clustering.getClusterEntityIDs(clusterIndex))){
                                                if(ag != null){
                                                        ag.baseScore += score;
                                                }
                                        }
                                }
                        }
                }
        }

        private void setPolicesClusterThatMaybeBlockedWhenSeeThatNotBlockedLooool(EntityID eid, double score) {
                if(policesMaybeBlocked.contains(eid)){
                        PoliceForce p = (PoliceForce) wi.getEntity(eid);
                        AURAreaGraph areaGraph = wsg.getAreaGraph(p.getID());
                        if(areaGraph != null && (! areaGraph.isBuilding() || areaGraph.isRefuge())){
                                int clusterIndex = clustering.getClusterIndex(eid);
                                for(AURAreaGraph ag : wsg.getAreaGraph(clustering.getClusterEntityIDs(clusterIndex))){
                                        if(ag != null){
                                                ag.baseScore += score;
                                        }
                                }
                                policesMaybeBlocked.remove(eid);
                        }
                }
        }

        private void addSmallRoadsScore(AURAreaGraph area) {
                if(area.isExtraSmall())
                        area.targetScore = 0;
                else if(area.isSmall())
                        area.targetScore = 0.5;
        }

        HashSet<EntityID> settedBuildings = new HashSet<>();
        private void setBuildingsThatIKnowWhatInThat(double score) {
                if(ai.getTime() <= 1)
                        return;
                
                HashSet<EntityID> civiliansPosition = new HashSet<>();
                Collection<StandardEntity> civilians = wi.getEntitiesOfType(StandardEntityURN.CIVILIAN);
                for(StandardEntity se : civilians){
                        Civilian civilian = (Civilian) se;
                        if(civilian.isHPDefined() && civilian.getHP() > 0){
                                civiliansPosition.add(civilian.getPosition());
                        }
                }
                
                for(EntityID id : visitedBuildings){
                        AURAreaGraph areaGraph = wsg.getAreaGraph(id);

                        if(areaGraph.isRefuge()){
                                continue;
                        }
                        
                        if(civiliansPosition.contains(id) && areaGraph.getTravelCost() == AURConstants.Math.INT_INF){
                                areaGraph.targetScore = 1.5;
                                if(! settedBuildings.contains(id)){
                                        areaGraph.secondaryScore += score;
                                        settedBuildings.add(id);
                                }
                        }
                        else{
                                areaGraph.targetScore = 0.1;
                        }
                }
                
                for(StandardEntity se : civilians){
                        Civilian civilian = (Civilian) se;
                        if(civilian.isHPDefined() && civilian.getHP() > 0){
                                AURAreaGraph areaGraph = wsg.getAreaGraph(civilian.getPosition());
                                if(areaGraph != null && areaGraph.isBuilding() && ! areaGraph.isRefuge() && areaGraph.getTravelCost() == AURConstants.Math.INT_INF){
                                        areaGraph.targetScore = 1.5;
                                        if(! settedBuildings.contains(civilian.getPosition())){
                                                areaGraph.secondaryScore += score;
                                                settedBuildings.add(civilian.getPosition());
                                        }
                                }
                        }
                }
        }

        private void setPoliceForcesComScore(List<MessagePoliceForce> pfMessage) {
                for(MessagePoliceForce msg : pfMessage){
                        if(msg.isPositionDefined()){
                                AURAreaGraph areaGraph = wsg.getAreaGraph(msg.getPosition());

                                if(msg.isBuriednessDefined() && msg.getBuriedness() > 0 && (msg.isHPDefined() && msg.getHP() > 0) || ! msg.isBuriednessDefined()){
                                        if(areaGraph.getTravelCost() == AURConstants.Math.INT_INF || areaGraph.getNoBlockadeTravelCost() * 4 < areaGraph.getTravelCost()){
                                                areaGraph.secondaryScore += AURConstants.RoadDetector.SecondaryScore.BLOCKED_HUMAN;
                                        }
                                }
                                else{
                                        setTargetAsReached(ai.getPosition(), AURConstants.RoadDetector.DECREASE_POLICE_AREA_SCORE);
                                }

                                if(msg.isHPDefined() && msg.getHP() == 0){
                                        areaGraph.targetScore = 0;
                                }
                        }
                }
        }

        private void setAmbulanceTeamComScore(List<MessageAmbulanceTeam> atMessage) {
                for(MessageAmbulanceTeam msg : atMessage){
                        if(msg != null && msg.isPositionDefined()){
                                AURAreaGraph areaGraph = wsg.getAreaGraph(msg.getPosition());
                                if(areaGraph != null){
                                        
                                        if(msg.isBuriednessDefined() && msg.getBuriedness() > 0 && (msg.isHPDefined() && msg.getHP() > 0) || ! msg.isBuriednessDefined()){
                                                if(areaGraph.getTravelCost() == AURConstants.Math.INT_INF || areaGraph.getNoBlockadeTravelCost() * 4 < areaGraph.getTravelCost()){
                                                        areaGraph.secondaryScore += AURConstants.RoadDetector.SecondaryScore.BLOCKED_HUMAN;
                                                }
                                        }

                                        if(msg.isHPDefined() && msg.getHP() == 0){
                                                areaGraph.targetScore = 0;
                                        }

                                        if(msg.getAgentID() != null &&
                                           msg.isPositionDefined() &&
                                           msg.getPosition() != null &&
                                           startPositionOfAgents.get(msg.getAgentID()) != null){
                                                if(! startPositionOfAgents.get(msg.getAgentID()).equals(msg.getPosition())){
                                                        areaGraph.targetScore = 0;
                                                }
                                        }
                                }
                        }
                }
        }

        private void setFireBrigadeComScore(List<MessageFireBrigade> fbMessage) {
                for(MessageFireBrigade msg : fbMessage){
                        if(msg != null && msg.isPositionDefined()){
                                AURAreaGraph areaGraph = wsg.getAreaGraph(msg.getPosition());
                                if(areaGraph != null){

                                        if(msg.isBuriednessDefined() && msg.getBuriedness() > 0 && (msg.isHPDefined() && msg.getHP() > 0) || ! msg.isBuriednessDefined()){
                                                if(areaGraph.getTravelCost() == AURConstants.Math.INT_INF || areaGraph.getNoBlockadeTravelCost() * 4 < areaGraph.getTravelCost()){
                                                        areaGraph.secondaryScore += AURConstants.RoadDetector.SecondaryScore.BLOCKED_HUMAN;
                                                }
                                        }

                                        if(msg.isHPDefined() && msg.getHP() == 0){
                                                areaGraph.targetScore = 0;
                                        }

                                        if(msg.getAgentID() != null &&
                                           msg.isPositionDefined() && 
                                           msg.getPosition() != null &&
                                           startPositionOfAgents.get(msg.getAgentID()) != null){
                                                if(! startPositionOfAgents.get(msg.getAgentID()).equals(msg.getPosition())){
                                                        areaGraph.targetScore = 0;
                                                }
                                        }
                                }
                        }
                }
        }

        private void setBuildingsComScore(List<MessageBuilding> buildingMessage) {
                for(MessageBuilding msg : buildingMessage){
                        if(msg != null){
                                AURAreaGraph ag = wsg.getAreaGraph(msg.getBuildingID());
                                if(msg.isFierynessDefined() && msg.getFieryness() > 0){
                                        ag.targetScore = 0;
                                }
                        }
                }
        }

        private void setCiviliansComScore(List<MessageCivilian> civilianMessage) {
                for(MessageCivilian msg : civilianMessage){
                        if(msg != null && msg.isPositionDefined()){
                                AURAreaGraph ag = wsg.getAreaGraph(msg.getPosition());
                                if(ag != null &&
                                   msg.getSenderID() != null &&
                                   ag.isBuilding() &&
                                   wi.getEntity(msg.getSenderID()) != null &&
                                   !(wi.getEntity(msg.getSenderID()) instanceof PoliceForce)){
                                        
                                        ag.secondaryScore += AURConstants.RoadDetector.SecondaryScore.BUILDINGS_THAT_I_KNOW_WHAT_IN_THAT;
                                }
                        }
                }
        }

        private void resetAgentsBlockadeScoreWhenNoBodyIsHere(EntityID eid) {
                AURAreaGraph ag = wsg.getAreaGraph(eid);
                if(ag != null && this.startPositionOfAgents.containsValue(eid)){
                        Set<EntityID> changedEntities = ai.getChanged().getChangedEntities();
                        for(EntityID e : startPositionOfAgents.keySet()){
                                if(startPositionOfAgents.get(e).equals(eid) && (! ((Human) wi.getEntity(e)).getPosition().equals(eid) || ! changedEntities.contains(e) )){
                                        if(wi.getEntity(eid).getStandardURN() == StandardEntityURN.AMBULANCE_TEAM){
                                                ag.baseScore -= AURConstants.RoadDetector.BaseScore.AMBULANCE_TEAM;
                                        }
                                        else if(wi.getEntity(eid).getStandardURN() == StandardEntityURN.FIRE_BRIGADE){
                                                ag.baseScore -= AURConstants.RoadDetector.BaseScore.FIRE_BRIGADE;
                                        }
                                        else if(wi.getEntity(eid).getStandardURN() == StandardEntityURN.POLICE_FORCE && ag.isBuilding()){
                                                ag.baseScore -= AURConstants.RoadDetector.BaseScore.POLICE_FORCE;
                                        }
                                }
                        }
                }
        }

        private void setRoadsComScore(List<MessageRoad> roadMessage) {
                for(MessageRoad msg : roadMessage){
                        AURAreaGraph ag = wsg.getAreaGraph(msg.getRoadID());
                        if(ag != null && ag.isRoad()){
                                ag.targetScore = 0;
                        }
                }
        }
}
