package CSU_Yunlu_2020.module.complex.at;

import CSU_Yunlu_2020.module.complex.at.CSUATTimer.ATTimer;
import CSU_Yunlu_2020.module.complex.at.CSUATTimer.ATTimerContainer;
import rescuecore2.standard.entities.Human;
import rescuecore2.worldmodel.EntityID;

public class ATHuman {
    private final EntityID id;
    private EntityID position;

    private int HP = UNINITIALISED;
    private ATTimer isHeardButNotFoundTimer; //被救的时候解除，可以添加heardPosition

    private Human human;
//    private Civilian civilian;
    private static final int UNINITIALISED = -999;
    private CSUSearchUtil util;
    private ATTimerContainer container;

    //还没用
    private boolean isSaved;

    public ATHuman(EntityID id, CSUSearchUtil util, ATTimerContainer container){
        this.id = id;
        isSaved = false;
        this.util = util;
        this.container = container;
        initialise();
    }

    private void initialise(){
        Human human = util.getHuman(id);
        if(human != null && human.isPositionDefined()){
            position = human.getPosition();
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

    public Human getHuman(){
        if(this.human == null){
            Human human = util.getHuman(id);
            if(human != null) this.human = human;
            return human;
        }else{
            return human;
        }
    }

    public void setHuman(Human human){
        this.human = human;
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
