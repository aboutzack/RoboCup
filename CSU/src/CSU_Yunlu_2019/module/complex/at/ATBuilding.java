package CSU_Yunlu_2019.module.complex.at;

import CSU_Yunlu_2019.CSUConstants;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

import java.util.HashSet;
import java.util.Set;

//挂起和解挂都是在更新建筑状态的同时进行的，所以只需要额外对被抢占的建筑进行计算
public class ATBuilding {
    private int priority = 0;
    private EntityID id;
    private boolean isVisited = false;
    private boolean isBurning;
    private boolean isReachable;
    private int clusterIndex = UNINITIALISED;
    private ATTimer hangUpTimer;
    private Building me;
    private boolean occupied;
    private boolean isBurnt;

    private Set<EntityID> civilianMayBe;
    private Set<EntityID> civilianConfirmed;
    private ATTimerContainer container;

    private CSUSearchUtil util;

    private static final int UNINITIALISED = -999;

    private static final boolean debug = false;

//    public ATBuilding(EntityID id, int priority){
//        if(id == null) util.debugOverall("初始化ATBuilding错误-\'id为null()\'(impossible)");
//        this.id = id;
//        this.priority = priority;
//        civilianMayBe = new HashSet<>();
//        civilianConfirmed = new HashSet<>();
//        initialise();
//    }

    public ATBuilding(EntityID id,CSUSearchUtil util,ATTimerContainer container){
        this.id = id;
        this.priority = CSUSearchUtil.UNKNOWN_CLASS;
        this.util = util;
        this.isReachable = true;
        this.isBurning = false;
        this.occupied = false;
        this.isBurnt = false;
        civilianMayBe = new HashSet<>();
        civilianConfirmed = new HashSet<>();
        this.container = container;
        if(id == null) util.debugOverall("初始化ATBuilding错误-\'id为null()\'(impossible)");
        initialise();
    }

    private void initialise(){
        Building building = util.getBuilding(id);
        if(building != null){
            me = building;
        }
    }

    public EntityID getId(){
        return id;
    }

    public int getPriority(){
        return priority;
    }

    public Building getBuilding(){
        if(me == null){
            Building building = util.getBuilding(id);
            if(building != null) me = building;
            return building;
        }else{
            return me;
        }
    }

    public boolean addCivilianMaybe(EntityID entityID){
        if(civilianMayBe.contains(entityID)){
            return false;
        }else{
            civilianMayBe.add(entityID);
            return true;
        }
    }

    public boolean addCivilianConfirmed(EntityID entityID){
        if(civilianConfirmed.contains(entityID)){
            return false;
        }else{
            civilianConfirmed.add(entityID);
            return true;
        }
    }

    public void setPriority(int priority){
        if(this.priority != CSUSearchUtil.FIFTH_CLASS) {
            if (priority > this.priority) {
                this.priority = priority;
            }
        }
    }

    public void setClusterIndex(int index){
        if(clusterIndex != index && clusterIndex != UNINITIALISED){
            util.debugOverall("重复初始化cluster index(impossible)");
        }
        this.clusterIndex = index;
    }

    public void setFromOccupiedToBurning(){
        this.occupied = false;
        this.isBurning = true;
        if(!isHangUp()){
            hangUp("从被抢占到燃烧");
        }
    }

    public void setFromOccupiedToUnreachable(){
        this.occupied = false;
        this.isReachable = true;
        if(!isHangUp()){
            hangUp("从被抢占到无法到达");
        }
    }

    public void setBurning(boolean burning){
        this.isBurning = burning;
        if(burning){
            if(!isHangUp()){
                hangUp("正在燃烧");
            }
        }else{
            if(isReachable && !occupied){
                releaseHangUp();
            }
        }
    }

    public void setReachable(boolean reachable){
        this.isReachable = reachable;
        if(!reachable){
            if(!isHangUp()){
                hangUp("找不到路");
            }
        }else{
            if(!isBurning && !occupied){
                releaseHangUp();
            }
        }
    }

    public void setOccupied(boolean occupied){
        this.occupied = occupied;
        if(occupied){
            if(!isHangUp()){
                hangUp("被抢占");
            }
        }else{
            if(!isBurning && isReachable){
                releaseHangUp();
            }
        }
    }

    public void setVisited(){
        isVisited = true;
        releaseHangUp();
        priority = CSUSearchUtil.FIFTH_CLASS;
    }

    public boolean isBurning(){
        return isBurning;
    }

    public boolean isReachable(){
        return isReachable;
    }

    public boolean isHangUp(){
        return hangUpTimer != null;
    }

    public boolean isVisited(){
        return isVisited;
    }

    public boolean isSearchable(){
        return !isBurning && isReachable && !isVisited;
    }

    public boolean isOccupied(){
        return occupied;
    }

    private void hangUp(String message){
        hangUpTimer = new ATTimer(id);
        container.register(hangUpTimer);
        util.debugSpecific(id+"挂起,因为"+message);
//        if(!util.safeHangUpScope()){
//            hangUpTimer = new ATTimer(id);
//            container.register(hangUpTimer);
//            util.debugSpecific(id+"挂起,因为"+message);
//        }
    }

    private void releaseHangUp(){
        container.release(hangUpTimer);
        hangUpTimer = null;
    }

    public int getHangUptime(){
        return isHangUp()? hangUpTimer.getHangUpTime() : -1;
    }

    @Override
    public int hashCode(){
        return (int)this.id.getValue();
    }

    @Override
    public boolean equals(Object o){
        if(o instanceof ATBuilding){
            ATBuilding atBuilding = (ATBuilding) o;
            return this.id == atBuilding.id;
        }else{
            return false;
        }
    }

    //debug
    private void debugATBuilding(String message){
        if(CSUConstants.DEBUG_AT_SEARCH && debug){
            System.out.println("ATBuilding:"+message);
        }
    }

    @Override
    public String toString(){
        return this.id.getValue()+"";
    }


//    public static void main(String[] args) {
//        B b1 = new B(111);
//        A a1 = new A(b1);
//        A a2 = new A(b1);
//        System.out.println(a1.b.num+"-"+a2.b.num);
//        b1.num = 222;
//        System.out.println(a1.b.num+"-"+a2.b.num);
//    }
//
//    static class A{
//        public B b;
//        A(B b){
//            this.b = b;
//        }
//    }
//
//    static class B{
//        public int num;
//        public B(int num){
//           this.num = num;
//        }
//    }
}
