package AUR.util.ambulance.Information;

import AUR.util.ambulance.DeathTime.FireDeathTime;
import AUR.util.ambulance.DeathTime.SimpleDeathTime;
import AUR.util.ambulance.DeathTime.ZJUParticleFilter;
import AUR.util.ambulance.ProbabilityDeterminant.HumanRateDeterminer;
import AUR.util.knd.AURWorldGraph;
import maps.convert.legacy2gml.BuildingInfo;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collection;


/**
 * Created by armanaxh on 2018.
 */

public class CivilianInfo {

    public final Civilian me;
    private RescueInfo rescueInfo;
    private AURWorldGraph wsg;
    private boolean isDead = false;
    public int saveTime = 420; //TODO
    private BuildingInfo closestOnFireBuilding;
    public RefugeInfo bestRefuge;
    public int travelTimeToRefuge;
    public int travelTimeToMe;
    public double rate;


    private ZJUParticleFilter predictor;
    private FireDeathTime predictorFire;
    public HumanRateDeterminer rateDeterminer;



    public CivilianInfo(Civilian ci, RescueInfo rescueInfo){
        this.me = ci;
        this.rescueInfo = rescueInfo;
        this.wsg = rescueInfo.wsg;
        this.predictor = new ZJUParticleFilter();
        this.predictorFire = new FireDeathTime();
        this.rateDeterminer = new HumanRateDeterminer(wsg, this);

        init();
    }
    // init **********************************************************************************
    private void init(){


        this.travelTimeToMe = this.calcTravelTimeToMe();
        this.bestRefuge  = this.assignmentRefuge();
        this.travelTimeToRefuge = this.calcTravelTimeToRefuge(bestRefuge);



        this.updateSaveTime();
        this.updatePredictor();
        this.updateFirePredictor();

        this.rateDeterminer.calc();
    }


    public EntityID getID() {
        return me.getID();
    }

    public EntityID getPosition() {
        return me.getPosition();
    }

    public int getHp() {
        return me.getHP();
    }

    public int getDmg() {
        return me.getDamage();
    }

    public int getBuriedness() {
        return me.getBuriedness();
    }

    public int getDeadTime() {
        int offset = 60 - this.getBuriedness();
        if (offset < 5) {
            offset = 5;
        } else if (offset > 55) {
            offset = 55;
        }

        int worstCaseDeathTime = SimpleDeathTime.getDeathTimeInWorstCase(rescueInfo,me.getHP(),me.getDamage());
        int deathTimeEst = this.predictor.getDeadTime()[offset];

        if(worstCaseDeathTime > deathTimeEst){
            return deathTimeEst;
        }else {
            return worstCaseDeathTime;
        }

    }
    public int getWorstCaseDeathTime(){
        return SimpleDeathTime.getDeathTimeInWorstCase(rescueInfo,me.getHP(),me.getDamage());
    }

    public int getDeadTimeFire() {
        return this.predictorFire.getDeathTime();
    }

    public RefugeInfo assignmentRefuge(){

        Collection<EntityID> refuges = wsg.getAllRefuges();


        if(refuges == null || refuges.size() == 0){
            return null;
        }
        if(me.isPositionDefined()){
            return null;
        }

        ArrayList<EntityID> path = wsg.getPathToClosest(me.getPosition(), refuges);
        if(path == null || path.size() == 0){
            path = wsg.getNoBlockadePathToClosest(me.getPosition(), refuges);
        }
        if(path != null && path.size() > 0){
            EntityID refugeID = path.get(path.size()-1);
            RefugeInfo refuge =  rescueInfo.refugesInfo.get(refugeID);




            if(refuge != null) {
                return refuge;
            }
        }
        return null;//TODO
    }

    // Calc *******************************************************************************************
    public int calcTravelTimeToRefuge(RefugeInfo refuge){
        if(refuge == null || refuge.refuge == null){
            return RescueInfo.maxTravelTime;
        }
        if(me.getPosition().equals(refuge.refuge.getID())){
            return 0;
        }
        if(refuge != null && me.isPositionDefined()){
            wsg.KStar(wsg.ai.getPosition());
            //TODO me.getPosioeion
//            double distance = wsg.getAreaGraph(refuge.refuge.getID()).getLastDijkstraCost();
//            double distance = wsg.wi.getDistance(me.getPosition(), refuge.refuge.getID());
//            int tempT = (int)(distance/RescueInfo.moveDistance);

            int tempT;
            if(wsg.getAreaGraph(refuge.refuge.getID()).lastDijkstraEntranceNode != null) {
                tempT = wsg.getAreaGraph(refuge.refuge.getID()).getTravelTime();


//            if(tempT == 0){
//                if(!me.getPosition().equals(refuge.refuge.getID())){
//                    tempT =  1;
//                }
//            }
                return tempT;
            }
        }
        return RescueInfo.maxTravelTime;
    }

    public int calcTravelTimeToMe(){
        if(me.isPositionDefined()) {
            wsg.KStar(wsg.ai.getPosition());
//            double distance = wsg.getAreaGraph(me.getPosition()).getLastDijkstraCost();
//            double distance = wsg.wi.getDistance(wsg.ai.getPosition(), me.getPosition());
//            int tempT = (int)(distance/RescueInfo.moveDistance);
            int tempT = RescueInfo.maxTravelTime;
            StandardEntity pos = wsg.wi.getEntity(me.getPosition());
            if(pos instanceof Area) {
                if(wsg.getAreaGraph(me.getPosition()) != null) {
                    if(wsg.getAreaGraph(me.getPosition()).lastDijkstraEntranceNode != null) {
                        tempT = wsg.getAreaGraph(me.getPosition()).getTravelTime();
                    }
                }
            }else if(pos instanceof AmbulanceTeam){
                AmbulanceTeam amtPos = (AmbulanceTeam)pos;
                if(wsg.getAreaGraph(amtPos.getPosition()) != null) {
                    if(wsg.getAreaGraph(amtPos.getPosition()).lastDijkstraEntranceNode != null) {
                        tempT = wsg.getAreaGraph(amtPos.getPosition()).getTravelTime();
                    }
                }
            }
//            if(tempT == 0){
//                if(!me.getPosition().equals(wsg.ai.getPosition())){
//                    tempT =  1;
//                }
//            }
            return tempT;
        }
        return RescueInfo.maxTravelTime;
    }



    // update **********************************************************************************************
    public void updateCycle(){
        this.saveTime--;
        this.travelTimeToMe = this.calcTravelTimeToMe();
        this.rateDeterminer.calc();

    }


    public void updateInformation(){

        if(me.isHPDefined() && me.getHP() <= 0 && this.isDead ) {
            return;
        }else if( me.isHPDefined() && me.getHP() <= 0){
            this.isDead = true;
        }
        this.updateSaveTime();
        this.updatePredictor();
        this.updateFirePredictor();
        this.travelTimeToRefuge = this.calcTravelTimeToRefuge(bestRefuge);

    }

    private void updateSaveTime(){

        int deathTimeBuri = this.getDeadTime();
        int deathTimeFire = this.getDeadTimeFire();//
        int travelTime = this.travelTimeToMe;
        int travelTimeToRefuge = this.travelTimeToRefuge;
        int buriedness = this.getBuriedness(); //Time for rescueing Civilian

        int deadLine = 420;
        if(deathTimeBuri < deathTimeFire){
            deadLine = deathTimeBuri - wsg.ai.getTime();
        }else{
            deadLine = deathTimeFire - wsg.ai.getTime();
        }

        int saveTime = deadLine - ( travelTime + travelTimeToRefuge + buriedness );
        this.saveTime = saveTime;
    }

    public void updateFirePredictor(){

    }

    public void updatePredictor() {

        if( me.isHPDefined() && me.isDamageDefined() && me.isBuriednessDefined() ) {
            int time = wsg.ai.getTime();
            this.predictor.setHp(me.getHP(), time);
            this.predictor.setDmg(me.getDamage(), time);
            this.predictor.step(time);
        }
    }

    public String toString(){
        return " "+getID()+" > rate : "+rate + " | ";
    }

    //TODO
//    public void nullClass for
}


