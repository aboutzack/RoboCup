package CSU_Yunlu_2019.module.complex.at;

import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

import java.util.HashSet;
import java.util.Set;

public class ATBuilding{
    private int priority = 0;
    private EntityID id;
    private boolean isVisited = false;
    private boolean isBurning;
    private boolean isReachable;
    private int clusterIndex;
    private HangUpTimer hangUpTimer;

    private Set<EntityID> civilianMayBe;
    private Set<EntityID> civilianConfirmed;

    public static boolean changed = false;

    private ATBuilding(EntityID id){
        this.id = id;
    }

    public ATBuilding(EntityID id, int priority){
        this.id = id;
        this.priority = priority;
    }

    public EntityID getId(){
        return id;
    }

    public int getPriority(){
        return priority;
    }

    public boolean addCivilianMaybe(EntityID entityID){
        if(civilianMayBe.contains(entityID)){
            return false;
        }else{
            civilianMayBe.add(entityID);
            return true;
        }
    }

    public void setPriority(int priority){
        if(priority != CSUSearchUtil.FIFTH_CLASS){
            if(priority > this.priority){
                this.priority = priority;
            }
        }else{
            this.priority = priority;
        }

    }

    public void setClusterIndex(int index){
        this.clusterIndex = index;
    }

    public void setBurning(boolean burning){
        this.isBurning = burning;
    }

    public void setReachable(boolean reachable){
        this.isReachable = reachable;
    }

    public void setVisited(){
        isVisited = true;
    }

    public boolean isBurning(){
        return isBurning;
    }

    public boolean isReachable(){
        return isReachable;
    }

    public void hangUp(){
        hangUpTimer = new HangUpTimer();
    }

    public void releaseHangUp(){
        hangUpTimer = null;
    }

    public boolean isHangUp(){
        return hangUpTimer == null;
    }

    public boolean isVisited(){
        return isVisited;
    }

    public void passTime(){
        if(hangUpTimer != null){
            hangUpTimer.passTime();
        }
    }

    public static ATBuilding getSample(EntityID id){
        return new ATBuilding(id, CSUSearchUtil.UNKNOWN_CLASS);
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
