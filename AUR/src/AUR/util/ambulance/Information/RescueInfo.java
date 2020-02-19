package AUR.util.ambulance.Information;


import AUR.util.AURCommunication;
import AUR.util.ambulance.ProbabilityDeterminant.AgentRateDeterminer;
import AUR.util.ambulance.ProbabilityDeterminant.BuildingRateDeterminer;
import AUR.util.knd.AURGeoUtil;
import AUR.util.knd.AURWorldGraph;
import adf.agent.action.common.ActionMove;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.information.MessageCivilian;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.module.AbstractModule;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;

/**
 * Created by armanaxh on 3/3/18.
 */
public class RescueInfo extends AbstractModule {


    // const *****************************
    public static final int simulationTime = 420;
    public static final int maxBuriedness = 100;
    public static final int maxDamage = 500;
    public static final int maxHp = 10000;
    public static final int thresholdRestDmg = 60;
    public static final int gasStationExplosionRange = 50000;
    public static final int maxTravelTime = 50;
    public static final int moveDistance = 40000;
    public int maxTravelCost = 1000000;
    public int maxDistance = 1000000;
    public static final int MAXDistance = 1000000;
    public static final int maxBrokness = 100;
    public static final int maxTemperature = 47;
    public  int losDamge;
    public  int losHp;
    private boolean initB = false;

    public AURWorldGraph wsg;
    public AmbulanceInfo ambo;
    private AURCommunication acm;


    public ActionMove temptest;
    public int agentSpeed;

    //Detector
    public Set<StandardEntity> clusterEntity;
    public Set<StandardEntity> neaberClusterEntity;
    public HashMap<EntityID, CivilianInfo> civiliansInfo;
    public HashMap<EntityID, RefugeInfo> refugesInfo;
    public Set<CivilianInfo> canNotRescueCivilian;

    //Search
    public Map<EntityID, BuildingInfo> buildingsInfo;
    public Map<EntityID, Double> agentsRate;
    public Set<BuildingInfo> searchList;
    public Set<BuildingInfo> visitedList;




    public RescueInfo(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData){
        super(ai, wi, si, moduleManager, developData);
        if(ai.me() instanceof AmbulanceTeam) {
            this.ambo = new AmbulanceInfo((AmbulanceTeam) ai.me());
        }
        this.refugesInfo = new HashMap<>();
        this.civiliansInfo = new HashMap<>();
        this.clusterEntity = new HashSet<>();
        this.neaberClusterEntity = new HashSet<>();
        this.canNotRescueCivilian = new HashSet<>();
        this.agentsRate = new HashMap<>();
        this.buildingsInfo = new HashMap<>();
        this.searchList = new HashSet<>();
        this.visitedList = new HashSet<>();
        this.acm = new AURCommunication(ai, wi, si, developData);
        this.wsg = moduleManager.getModule("knd.AuraWorldGraph", "AUR.util.knd.AURWorldGraph");
        this.wsg.rescueInfo = this;
    }


    // init *************************************************************************************

    private void init(){
        this.agentSpeed = 30000;
        this.losDamge = wsg.si.getPerceptionLosPrecisionDamage();
        this.losHp = wsg.si.getPerceptionLosPrecisionHp();
        double maxX = worldInfo.getBounds().getMaxX();
        double maxY = worldInfo.getBounds().getMaxY();
        this.maxDistance = (int)( Math.sqrt( (maxX*maxX) + (maxY*maxY) ) );
        this.initRefuge();
        this.initBulding();

    }

    private void initBulding(){

        for(StandardEntity entity : worldInfo.getEntitiesOfType(
                BUILDING,
                REFUGE,
                GAS_STATION,
                AMBULANCE_CENTRE,
                FIRE_STATION,
                POLICE_OFFICE ))
        {
            if(entity instanceof Building) {

                Building b = (Building)entity;

                this.buildingsInfo.put(entity.getID(), new BuildingInfo(wsg, this , b));
            }
        }

        int maxD = 0;
        for(BuildingInfo b : this.buildingsInfo.values()){

            if( maxD < b.travelCostTobulding && (b.travelCostTobulding < Integer.MAX_VALUE)){
                maxD = b.travelCostTobulding;
            }
        }
        this.maxTravelCost = maxD;
    }

    private void initRefuge(){

        for(StandardEntity entity: wsg.wi.getEntitiesOfType(StandardEntityURN.REFUGE)){
            if(entity instanceof Refuge){
                Refuge refuge = (Refuge)entity;
                RefugeInfo refugeInfo = new RefugeInfo(refuge, this);
                this.refugesInfo.put(refuge.getID(), refugeInfo);
            }
        }

    }

    public RescueInfo initCalc(){
        if(!initB) {
            init();
            initB = true;
        }
        return this;
    }

    // Update ***********************************************************************************

    @Override
    public AbstractModule updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() > 1)
        {
            return this;
        }
        this.updateInformation(messageManager);
        return this;
    }

    int lastRun = -1;
    public void updateInformation(MessageManager messageManager){
//        long time = System.currentTimeMillis();
        if(lastRun == agentInfo.getTime())
            return;
        lastRun = agentInfo.getTime();

        if(agentInfo.getTime() > 1 && !this.initB){
            init();
            initB = true;
        }
        this.acm.updateInfo(messageManager);

        this.updateChanges();
//        System.out.println("t1 : " + (System.currentTimeMillis() - time) );
//        time = System.currentTimeMillis();
        this.updateMessageCivilian();
//        System.out.println("t2 : " + (System.currentTimeMillis() - time) );
//        time = System.currentTimeMillis();
        this.updateBuildingInfo();
//        System.out.println("t3 : " + (System.currentTimeMillis() - time) );
//        time = System.currentTimeMillis();
        this.updateCycle();
//        System.out.println("t4 : " + (System.currentTimeMillis() - time) );


    }

    private void updateMessageCivilian(){
        for(MessageCivilian cm : acm.getCivilianMessage()){
            if(cm.isPositionDefined() && cm.isBuriednessDefined() && cm.isHPDefined() && cm.isDamageDefined() ){
                StandardEntity entity = worldInfo.getEntity(cm.getAgentID());
                if(entity != null && entity instanceof Civilian) {
                    Civilian c = (Civilian) entity;
                    if(c.isPositionDefined()) {
                        if (wsg.getAreaGraph(c.getPosition()) != null){
                            if(wsg.getAreaGraph(c.getPosition()).clusterIndex == wsg.agentCluster || wsg.neighbourClusters.contains(wsg.getAreaGraph(c.getPosition()).clusterIndex)){
                                updateCivilianInfo(c);
                            }
                        }
                    }
                }
            }
        }
    }


    private void updateBuildingInfo(){
        int maxD = 0;
        for(BuildingInfo b : this.buildingsInfo.values()){
            //Update
            b.updateInformation();
            //
            if( maxD < b.travelCostTobulding && (b.travelCostTobulding <= Integer.MAX_VALUE) ){
                maxD = b.travelCostTobulding;
            }
        }
        System.out.println("max Dis:" +  maxD);
        this.maxTravelCost = maxD;

    }

    private void updateCycle(){
        for(CivilianInfo c : civiliansInfo.values()){
            c.updateCycle();
        }
    }
    private void updateChanges(){
        Set<EntityID> changes = worldInfo.getChanged().getChangedEntities();
        for(EntityID id: changes){
            StandardEntity entity = worldInfo.getEntity(id);
            if(entity.getStandardURN().equals(StandardEntityURN.CIVILIAN) && entity instanceof Civilian) {
                Civilian civilian = (Civilian) entity;
                updateCivilianInfo(civilian);
                //TODO
            }else if(      entity.getStandardURN().equals(StandardEntityURN.POLICE_FORCE)
                    || entity.getStandardURN().equals(StandardEntityURN.AMBULANCE_TEAM)
                    || entity.getStandardURN().equals(StandardEntityURN.FIRE_BRIGADE)){
                Human human = (Human)entity;
                updateAgentInfo(human);
            }
        }
        if(ambo != null) {
            updateViwe();
        }


    }



    public ArrayList<Line2D> testLine = new ArrayList<>();//For debug
    public ArrayList<Edge> areasInter = new ArrayList<>();//For debug

    private void updateViwe(){
//TODO BUGFIX

        testLine.clear();
        areasInter.clear();

        Set<EntityID> change = worldInfo.getChanged().getChangedEntities();
        Set<EntityID> temp = new HashSet<>();
        for(CivilianInfo ci : this.civiliansInfo.values()){
            if(!ci.me.isPositionDefined() || !ci.me.isXDefined() || !ci.me.isYDefined()){
                continue;
            }
            if(change.contains(ci.getPosition())
                    && !change.contains(ci.me.getID())
                    && worldInfo.getDistance(ambo.me, ci.me) <= scenarioInfo.getPerceptionLosMaxDistance() ){

                testLine.add(new Line2D(new Point2D(ambo.me.getX(), ambo.me.getY()) , new Point2D(ci.me.getX(), ci.me.getY()) ));
                boolean intersect = false;
                for(StandardEntity entity : worldInfo.getObjectsInRange( ambo.me , scenarioInfo.getPerceptionLosMaxDistance()) ) {

                    if (entity instanceof Area) {
                        Area area = (Area) entity;


                        if(entity instanceof Road){
                            continue;
                        }
                        for (Edge e : area.getEdges()) {
                            double[] d = new double[2];
                            if (e.isPassable()) {
                                continue;
                            }
                            if (AURGeoUtil.getIntersection(
                                    e.getStartX(), e.getStartY(),
                                    e.getEndX(), e.getEndY(),
                                    ambo.me.getX(), ambo.me.getY(),
                                    ci.me.getX(), ci.me.getY(),
                                    d)) {
                                intersect = true;
                                areasInter.add(e);
                                break;
                            }
                        }
                        if(intersect == true) {
                            break;
                        }
                    }

                }
                if (intersect == false) {
                    temp.add(ci.getID());
                }
            }
        }

        for(EntityID id : temp) {
            civiliansInfo.remove(id);
        }

    }

    private void updateCivilianInfo(Civilian civilian){
        //just when see it

        CivilianInfo civilianInfo = null;
        if(!civiliansInfo.containsKey(civilian.getID())) {
            if(civilian.isBuriednessDefined() && civilian.isHPDefined() && civilian.isPositionDefined()) {
                civilianInfo = new CivilianInfo(civilian, this);
                civiliansInfo.put(civilian.getID(), civilianInfo);
            }
        }else{
            civilianInfo = civiliansInfo.get(civilian.getID());
        }

        if(civilianInfo != null){
            civilianInfo.updateInformation();
        }

    }

    private void updateAgentInfo(Human human) {
        if(human.getID().equals(agentInfo.getID())){
            return;
        }
        if(!agentsRate.containsKey(human.getID())){
            Double rate = AgentRateDeterminer.calc(wsg, this, human);
            agentsRate.put(human.getID(), rate);
        }else{
            Double rate = AgentRateDeterminer.calc(wsg, this, human);
            agentsRate.put(human.getID(), rate);
        }
    }


    public void updateAgentSpeed(){

    }

    // Calc ***************************************************************************************
    @Override
    public AbstractModule calc() {
        return null;
    }
}
