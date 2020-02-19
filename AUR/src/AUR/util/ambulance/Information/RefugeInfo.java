package AUR.util.ambulance.Information;

import AUR.util.knd.AURWorldGraph;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author armanaxh - 2018
 */

public class RefugeInfo {

    public final Refuge refuge;
    private AURWorldGraph wsg;
    public int copacity;
    public int numOfCivInside;
    public Set<Civilian> insideCivilian;



    public RefugeInfo(Refuge refuge, RescueInfo rescueInfo){
        this.refuge = refuge;
        this.insideCivilian = new HashSet<>();
        init();
    }
    private void init(){

    }

    public void updateInformation(){
//TODO BUG
        //update insideCivilian
        if(wsg.ai.getPosition().equals(refuge.getID())){
            for(EntityID id : wsg.wi.getChanged().getChangedEntities()){
                StandardEntity entity = wsg.wi.getEntity(id);
                if(entity.getStandardURN().equals(StandardEntityURN.CIVILIAN)){
                    this.insideCivilian.add((Civilian)entity);
                }
            }
        }

    }

}
