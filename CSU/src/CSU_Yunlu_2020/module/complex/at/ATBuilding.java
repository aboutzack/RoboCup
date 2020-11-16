package CSU_Yunlu_2020.module.complex.at;

import CSU_Yunlu_2020.CSUConstants;
import CSU_Yunlu_2020.module.complex.at.CSUATTimer.ATBuildingTimer;
import CSU_Yunlu_2020.module.complex.at.CSUATTimer.ATTimerContainer;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

import java.util.HashSet;
import java.util.Set;

//挂起和解挂都是在更新建筑状态的同时进行的，所以只需要额外对被抢占的建筑进行计算
public class ATBuilding {
    private int priority = 0;
    private final EntityID id;
    private int clusterIndex = UNINITIALISED;
    private ATBuildingTimer hangUpTimer;
    private Building me;

    //建筑属性  通过updateSingle更新
    private int fieriness;
    private int brokenness;

    //判断挂起用
    private boolean isOccupied;
    private boolean isBurning; //与fieriness绑定
    private boolean isReachable;//只判断当前目标能不能到，每20秒全体不能到的设置为true。
    private boolean isWayBurning;

    private EntityID wayBurningBuilding;

    //用来判断有没有搜的必要
    private boolean isBurnt; //与fieriness绑定
    private boolean isBroken; //与brokenness绑定
    private boolean isVisited;

    private Set<EntityID> humanMayBe;
    private Set<EntityID> humanConfirmed;
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
        this.isOccupied = false;
        this.isBurnt = false;
        this.brokenness = 999999;
        this.fieriness = -1;
        this.isBroken = true;
        this.isVisited = false;
        this.isWayBurning = false;
        humanMayBe = new HashSet<>();
        humanConfirmed = new HashSet<>();
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
        if(humanMayBe.contains(entityID)){
            return false;
        }else{
            humanMayBe.add(entityID);
            return true;
        }
    }

    public boolean addHumanConfirmed(EntityID entityID){
        if(humanConfirmed.contains(entityID)){
            return false;
        }else{
            humanConfirmed.add(entityID);
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

//    public void setFromOccupiedToBurning(){
//        this.occupied = false;
//        this.isBurning = true;
//        if(!isHangUp()){
//            hangUp("从被抢占到燃烧");
//        }
//    }

//    public void setFromOccupiedToUnreachable(){
//        this.isOccupied = false;
//        this.isReachable = true;
//        if(!isHangUp()){
//            hangUp("从被抢占到无法到达");
//        }
//    }

    public void setBurning(boolean burning){
        this.isBurning = burning;
        if(burning){
            if(!isHangUp()){
                hangUp("正在燃烧");
            }
            hangUpTimer.reasonBurning();
        }else{
            if(isHangUp()) hangUpTimer.removeBurningReason();
            tryReleaseHangUp();
        }
    }

    public void setReachable(boolean reachable){
        this.isReachable = reachable;
        if(!reachable){
            if(!isHangUp()){
                hangUp("找不到路");
            }
            hangUpTimer.reasonUnReachable();
        }else{
            if(isHangUp()) hangUpTimer.removeUnReachableReason();
            tryReleaseHangUp();
        }
    }

    public void setOccupied(boolean occupied){
        this.isOccupied = occupied;
        if(occupied){
            if(!isHangUp()){
                hangUp("被抢占");
            }
            hangUpTimer.reasonOccupied();
        }else{
            if(isHangUp()) hangUpTimer.removeOccupiedReason();
            tryReleaseHangUp();
        }
    }

    public void setVisited(){
        isVisited = true;
        tryReleaseHangUp();
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

//    public boolean isSearchable(){
//        return !isBurning && isReachable && !isVisited;
//    }

    public boolean isOccupied(){
        return isOccupied;
    }

    //可能：燃烧、找不到路、被抢占、路上有燃烧
    private void hangUp(String message){
        hangUpTimer = new ATBuildingTimer(id);
        container.register(hangUpTimer);
        util.debugSpecific(id+"挂起,因为"+message);
//        if(!util.safeHangUpScope()){
//            hangUpTimer = new ATTimer(id);
//            container.register(hangUpTimer);
//            util.debugSpecific(id+"挂起,因为"+message);
//        }
    }

    private void tryReleaseHangUp(){
        if(!isBurning && !isOccupied && isReachable){
            container.release(hangUpTimer);
            hangUpTimer = null;
        }

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

    public String getStatusString(){
        String status = id+":{";
        if(isBurning){
            status += "isBurning : true,";
        }else{
            status += "isBurning : false,";
        }
        if(isReachable){
            status += "isReachable : true,";
        }else{
            status += "isReachable : false,";
        }
        if(isWayBurning){
            status += "isWayBurning : true,";
        }else{
            status += "isWayBurning : false,";
        }
        if(isBurnt){
            status += "isBurnt : true,";
        }else{
            status += "isBurnt : false,";
        }
        if(isBroken){
            status += "isBroken : true,";
        }else{
            status += "isBroken : false,";
        }
        if(isVisited){
            status += "isVisited : true";
        }else{
            status += "isVisited : false";
        }
        return "";
    }

    //11.7
    public boolean isBurnt(){
        return isBurnt;
    }

    public boolean isBroken(){
        return isBroken;
    }



    public void setBrokenness(int brokenness){
        isBroken = brokenness > 0;
        this.brokenness = brokenness;
    }

    public void removeBrokenness(){
        this.brokenness = -1;
    }

    public void setFieriness(int fieriness){
        if(fieriness > 0 && fieriness < 4){
            setBurning(true);
        }else{
            setBurning(false);
        }
        if(fieriness == 8){
            isBurnt = true;
        }
        this.fieriness = fieriness;
    }

    public void setWayBurning(boolean wayBurning, EntityID wayBurningBuilding){
        this.isWayBurning = wayBurning;
        if(wayBurning){
            if(!isHangUp()){
                hangUp("去"+id+"的路上有"+wayBurningBuilding+"在燃烧");
            }
            this.wayBurningBuilding = wayBurningBuilding;
            hangUpTimer.reasonWayBurning();
        }else{
            this.wayBurningBuilding = null;
            if(isHangUp()) hangUpTimer.removeWayBurning();
            tryReleaseHangUp();
        }
    }

    public boolean isWayBurning(){
        return this.isWayBurning;
    }

    public EntityID getWayBurningBuilding(){
        return wayBurningBuilding;
    }

    public int getUnReachableHangUpTime(){
        return isHangUp()? hangUpTimer.getUnReachableTime() : -1;
    }

//    public boolean isNeedToSearch(){
//        return !isBurnt && isBroken;
//    }

    public boolean isNeedToSearch(){
//        return !isVisited && !isBurnt && isBroken;
        return !isVisited && !isBurnt;
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
