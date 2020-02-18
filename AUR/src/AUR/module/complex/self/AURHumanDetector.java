package AUR.module.complex.self;

import AUR.util.ambulance.AmbulanceUtil;
import AUR.util.ambulance.Information.CivilianInfo;
import AUR.util.ambulance.Information.RescueInfo;
import AUR.util.knd.AURWorldGraph;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.Clustering;
import adf.component.module.complex.HumanDetector;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static rescuecore2.standard.entities.StandardEntityURN.AMBULANCE_TEAM;
import static rescuecore2.standard.entities.StandardEntityURN.REFUGE;

/**
 * Created by armanaxh on 2018.
 */

/**
 *
 * @author armanaxh - 2018
 */

public class AURHumanDetector extends HumanDetector
{
    private Clustering clustering;
    private EntityID result;
    private AURWorldGraph wsg;
    private RescueInfo rescueInfo;


    public AURHumanDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData)
    {
        super(ai, wi, si, moduleManager, developData);
        this.result = null;

        switch (scenarioInfo.getMode())
        {
            case PRECOMPUTATION_PHASE:
                this.clustering = moduleManager.getModule("SampleHumanDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case PRECOMPUTED:
                this.clustering = moduleManager.getModule("SampleHumanDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case NON_PRECOMPUTE:
                this.clustering = moduleManager.getModule("SampleHumanDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
        }
        this.wsg = moduleManager.getModule("knd.AuraWorldGraph", "AUR.util.knd.AURWorldGraph");
        this.rescueInfo = moduleManager.getModule("ambulance.RescueInfo", "AUR.util.ambulance.Information.RescueInfo");


        registerModule(this.clustering);

        init();
    }

    // init *************************************************************************************

    private void init(){

    }


    // Update ***********************************************************************************
    int countPos = 0;
    EntityID lastResult = null;
    @Override
    public HumanDetector updateInfo(MessageManager messageManager)
    {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() > 1)
        {
            return this;
        }

        //
        if(lastResult != null && result != null) {
            if (lastResult.equals(result)) {
                countPos++;
            } else {
                countPos = 0;
            }

            if (countPos > 18) {
                if (rescueInfo.civiliansInfo.get(result) != null) {
                    rescueInfo.civiliansInfo.get(result).rate = 0;
                    result = null;
                }
            }

        }lastResult = result;


        this.wsg.updateInfo(messageManager);
        this.clustering.updateInfo(messageManager);
        this.rescueInfo.updateInformation(messageManager);

        return this;
    }

    //DEBUG

    //TODO agent property , in bredness , in fire , in black , ...

    // Calc ***************************************************************************************
    @Override
    public HumanDetector calc()
    {
        Human transportHuman = this.agentInfo.someoneOnBoard();
        if (transportHuman != null) {
            this.result = transportHuman.getID();
            return this;
        }

        if (this.nullResult()) {
            this.result = null;
        }

        if (this.result == null)
        {
            this.result = this.calcTargetAgent();

            if(this.result == null) {
                this.result = this.calcTarget();
            }
        }

        StandardEntity st = worldInfo.getEntity(result);
        if(st instanceof Human) {
            this.rescueInfo.ambo.workOnIt = (Human)(st);
        }
        return this;
    }



    private boolean chackAmboWork(){
        return true;
    }


    private boolean nullResult() {

        if (this.result != null) {
            Human target = (Human) this.worldInfo.getEntity(this.result);
            if (target != null) {
                if (!target.isHPDefined() || target.getHP() == 0) {
                    return true;
                } else if (!target.isPositionDefined()) {
                    return true;
                } else if(rescueInfo.civiliansInfo.get(result) == null && rescueInfo.agentsRate.get(result) == null){
                    return true;
                }
                else {
                    StandardEntity position = this.worldInfo.getPosition(target);
                    if (position != null) {
                        StandardEntityURN positionURN = position.getStandardURN();
                        if (positionURN.equals(REFUGE) || positionURN.equals(AMBULANCE_TEAM)) {
                            return true;
                        }
                    }
                    if(position instanceof Road){
                        return true;
                    }
                    if (position instanceof Building) {
                        Building b = (Building) position;
                        if (b.isOnFire()) {
                            // TODO FIRE
                            return true;
                        }
                        if (b.isTemperatureDefined() && b.getTemperature() > 44) {
                            return true;
                        }
                    }

                    if(!position.getID().equals(agentInfo.getPosition())){
                        return true;
                    }


                }
                if (target instanceof AmbulanceTeam
                        || target instanceof FireBrigade
                        || target instanceof PoliceForce) {
                    if (target.isBuriednessDefined() && target.getBuriedness() == 0) {
                        return true;
                    }
                }

                if(target.isBuriednessDefined() && target.getBuriedness() < 2) {
                    if (agentInfo.me() instanceof AmbulanceTeam) {
                        AmbulanceTeam loadAmbulance = (AmbulanceTeam) agentInfo.me();
                        for (EntityID entityID : worldInfo.getChanged().getChangedEntities()) {
                            StandardEntity entity = worldInfo.getEntity(entityID);
                            if (entity instanceof AmbulanceTeam && worldInfo.getPosition(target) != null) {
                                AmbulanceTeam at = (AmbulanceTeam) entity;
                                if (at.isBuriednessDefined() && at.getBuriedness() == 0) {
                                    if (at.getPosition().equals(worldInfo.getPosition(target).getID())) {
                                        if (entityID.getValue() < loadAmbulance.getID().getValue()) {
                                            loadAmbulance = at;
                                        }
                                    }
                                }
                            }
                        }
                        if (!loadAmbulance.getID().equals(agentInfo.me().getID())) {
                            return true;
                        }
                    }
                }

            }
        }

        return false;
    }

    private boolean changeCivilianView() {

        return false;
    }

    private EntityID calcTarget()
    {

        List<CivilianInfo> civilians = new LinkedList<>();
        civilians.addAll(rescueInfo.civiliansInfo.values());


        // TODO HaHa:D
        this.removeCantRescue(civilians);
        this.removeLowRate(civilians);
        this.removeCantPass(civilians);
        //TODO You Shall Not Pass  (Gandalf the grey)

        Collections.sort(civilians , AmbulanceUtil.CivilianRateSorter);
        if(civilians.size() > 0 ){

            return civilians.get(0).getID();

        }

        return null;
    }

    private EntityID calcTargetAgent() {

        List<EntityID> agents = new LinkedList<>();
        agents.addAll(rescueInfo.agentsRate.keySet());

        agents = this.removeCantPassAgent(agents);

        EntityID maxAgent = null;
        double maxValue = 0;
        for(EntityID id : agents) {
            if (rescueInfo.agentsRate.get(id).doubleValue() > 1) {
                if (rescueInfo.agentsRate.get(id).doubleValue() > maxValue) {
                    maxValue = rescueInfo.agentsRate.get(id);
                    maxAgent = id;
                }
            }
        }

        if(maxAgent != null){
            return maxAgent;
        }
        return null;
    }


    private List<EntityID> removeCantPassAgent(List<EntityID> agents){

        wsg.KStar(agentInfo.getPosition());

        Collection<EntityID> temp = new LinkedList<>();
        for(EntityID id: agents){
            StandardEntity entity = worldInfo.getEntity(id);
            if(entity instanceof Human) {
                Human h = (Human)entity;
                if (wsg.getAreaGraph(h.getPosition()).lastDijkstraEntranceNode == null) {
                    temp.add(id);
                }
            }
        }
        agents.removeAll(temp);
        return agents;
    }

    private List<CivilianInfo> removeCantRescue(List<CivilianInfo> civilians){

        rescueInfo.canNotRescueCivilian.clear();
        Collection<CivilianInfo> temp = new LinkedList<>();
        for(CivilianInfo civilian : civilians){
            if(civilian.saveTime <= 0){
                temp.add(civilian);
                //TODO remove Set list
                rescueInfo.canNotRescueCivilian.add(civilian);
            }
        }
        civilians.removeAll(temp);

        return civilians;
    }

    private List<CivilianInfo> removeCantPass(List<CivilianInfo> civilians){

        wsg.KStar(agentInfo.getPosition());

        Collection<CivilianInfo> temp = new LinkedList<>();
        for(CivilianInfo ci: civilians){
            if(ci.travelTimeToMe == Integer.MAX_VALUE){
                temp.add(ci);
                continue;
            }

            if(wsg.getAreaGraph(ci.getPosition()).lastDijkstraEntranceNode == null){
                temp.add(ci);
            }
        }
        civilians.removeAll(temp);
        return civilians;
    }

    private List<CivilianInfo> removeLowRate(List<CivilianInfo> civilians){

        Collection<CivilianInfo> temp = new LinkedList<>();
        for(CivilianInfo civilian : civilians){
            if(civilian.rate <= 1){
                temp.add(civilian);
            }
        }
        civilians.removeAll(temp);
        return civilians;
    }
    private EntityID calcTargetInWorld()
    {

        return null;
    }




    // preprate & precompute **************************************************************************
    @Override
    public EntityID getTarget()
    {
        return this.result;
    }

    @Override
    public HumanDetector precompute(PrecomputeData precomputeData)
    {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        this.wsg.precompute(precomputeData);
        return this;
    }

    @Override
    public HumanDetector resume(PrecomputeData precomputeData)
    {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2)
        {
            return this;
        }
        this.wsg.resume(precomputeData);
        clustering.preparate();
        int index = clustering.getClusterIndex(agentInfo.me());
        rescueInfo.clusterEntity.addAll(clustering.getClusterEntities(index));
        for(Integer i : wsg.neighbourClusters) {
            rescueInfo.neaberClusterEntity.addAll(clustering.getClusterEntities(i));
        }
        rescueInfo.initCalc();
        return this;
    }

    @Override
    public HumanDetector preparate()
    {
        super.preparate();
        if (this.getCountPreparate() >= 2)
        {
            return this;
        }
        this.wsg.preparate();
        clustering.preparate();
        int index = clustering.getClusterIndex(agentInfo.me());
        rescueInfo.clusterEntity.addAll(clustering.getClusterEntities(index));
        for(Integer i : wsg.neighbourClusters) {
            rescueInfo.neaberClusterEntity.addAll(clustering.getClusterEntities(i));
        }
        rescueInfo.initCalc();
        return this;
    }

}

