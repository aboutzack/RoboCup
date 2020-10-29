package CSU_Yunlu_2019.module.complex.at;

import rescuecore2.standard.entities.Civilian;
import rescuecore2.worldmodel.EntityID;

public class ATCivilian {
    private EntityID id;
    private EntityID position;
    private boolean isSaved;
    private int HP = UNINITIALISED;
    private ATTimer isHeardButNotFoundTimer; //被救的时候解除，可以添加heardPosition

    private Civilian civilian;
    private static final int UNINITIALISED = -999;
    private CSUSearchUtil util;
    private ATTimerContainer container;

    public ATCivilian(EntityID id, CSUSearchUtil util, ATTimerContainer container){
        this.id = id;
        isSaved = false;
        this.util = util;
        this.container = container;
        initialise();
    }

    private void initialise(){
        Civilian civ = util.getCivilian(id);
        if(civ != null && civ.isPositionDefined()){
            position = civ.getPosition();
        }
    }

    private void setSaved(){
        if(isSaved){
            util.debugOverall("repeated Saving(impossible)");
        }else{
            isSaved = true;
        }
    }

    public EntityID getId(){
        return id;
    }

    public Civilian getCivilian(){
        if(civilian == null){
            Civilian civ = util.getCivilian(id);
            if(civ != null) civilian = civ;
            return civilian;
        }else{
            return civilian;
        }
    }

    public void setHeard(){
        if(isHeardButNotFoundTimer == null){
            isHeardButNotFoundTimer = new ATTimer(id);
            container.register(isHeardButNotFoundTimer);
        }else{
            //再次被听到
        }
    }

    public boolean isHeardButNotFound(){
        return isHeardButNotFoundTimer != null;
    }

    public void releaseHeard(){
        container.release(isHeardButNotFoundTimer);
        isHeardButNotFoundTimer = null;
    }

    @Override
    public String toString(){
        return this.id.getValue()+"";
    }
}
