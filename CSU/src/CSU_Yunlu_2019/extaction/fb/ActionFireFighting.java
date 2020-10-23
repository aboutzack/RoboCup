package CSU_Yunlu_2019.extaction.fb;


import CSU_Yunlu_2019.CSUConstants;
import CSU_Yunlu_2019.debugger.DebugHelper;
import CSU_Yunlu_2019.standard.DistanceComparator;
import CSU_Yunlu_2019.standard.Ruler;
import CSU_Yunlu_2019.util.Util;
import CSU_Yunlu_2019.world.CSUWorldHelper;
import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.action.common.ActionRest;
import adf.agent.action.fire.ActionExtinguish;
import adf.agent.action.fire.ActionRefill;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.information.MessageFireBrigade;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class ActionFireFighting extends ExtAction
{
    private PathPlanning pathPlanning;
    private MessageManager messageManager;

    private int maxExtinguishDistance;
    private int maxExtinguishPower;
    private int thresholdRest;
    private int kernelTime;
    private int refillCompletedThreshold;
    private int refillRequestThreshold;
    private boolean refillFlag;

    private EntityID target;
    private List<EntityID> targetHistory;
    private List<Integer> waterHistory;
    private Set<EntityID> availableHydrants; //需要补水时重置，一直到补水完毕，去掉各个时刻被使用的hydrants
    private List<Building> unSearchBuildings;   //尚未搜索过的建筑
    private List<Building> fireBuildings;       //燃烧的建筑物
    private int areaConstant = 10;                   //面积的系数
    private int temperatureConstant = 15;            //温度的系数
    private Map<EntityID, Integer> fireBrigadesWaterMap = new HashMap<>();   //每个消防员与其相应水量的键值对
    private Action lastResult;  //保存上次的result

    private ExtAction actionExtMove;
    private CSUWorldHelper world;

    public ActionFireFighting(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, DevelopData developData) {
        super(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
        this.fireBuildings = new ArrayList<>();
        this.maxExtinguishDistance = scenarioInfo.getFireExtinguishMaxDistance();
        this.maxExtinguishPower = scenarioInfo.getFireExtinguishMaxSum();
        this.thresholdRest = developData.getInteger("ActionFireFighting.rest", 100);
        int maxWater = scenarioInfo.getFireTankMaximum();
        this.refillCompletedThreshold = (maxWater / 10) * developData.getInteger("ActionFireFighting.refill.completed", 10);
        //this.refillRequest = this.maxExtinguishPower * developData.getInteger("ActionFireFighting.refill.request", 1);
        this.refillRequestThreshold = 1000 * developData.getInteger("ActionFireFighting.refill.request", 1);
        this.refillFlag = false;

        this.target = null;
        this.targetHistory = new ArrayList<>();
        this.waterHistory = new ArrayList<>();
        this.availableHydrants = new HashSet<>();

        switch (scenarioInfo.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("ActionFireFighting.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.actionExtMove = moduleManager.getExtAction("TacticsFireBrigade.ActionExtMove", "adf.sample.extaction.ActionExtMove");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("ActionFireFighting.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.actionExtMove = moduleManager.getExtAction("TacticsFireBrigade.ActionExtMove", "adf.sample.extaction.ActionExtMove");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("ActionFireFighting.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.actionExtMove = moduleManager.getExtAction("TacticsFireBrigade.ActionExtMove", "adf.sample.extaction.ActionExtMove");
                break;
        }
        if (agentInfo.me() instanceof FireBrigade) {
            world = moduleManager.getModule("WorldHelper.FireBrigade", CSUConstants.WORLD_HELPER_FIRE_BRIGADE);
        } else {
            world = moduleManager.getModule("WorldHelper.Default", CSUConstants.WORLD_HELPER_DEFAULT);
        }
    }

    @Override
    public ExtAction precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        this.pathPlanning.precompute(precomputeData);
        this.actionExtMove.precompute(precomputeData);
        try {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        }
        catch (NoSuchConfigOptionException e)
        {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override
    public ExtAction resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2)
        {
            return this;
        }
        this.pathPlanning.resume(precomputeData);
        try
        {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        }
        catch (NoSuchConfigOptionException e)
        {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override
    public ExtAction preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2)
        {
            return this;
        }
        this.pathPlanning.preparate();
        this.actionExtMove.preparate();
        try {
            this.kernelTime = this.scenarioInfo.getKernelTimesteps();
        }
        catch (NoSuchConfigOptionException e)
        {
            this.kernelTime = -1;
        }
        return this;
    }

    @Override
    public ExtAction updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2)
        {
            return this;
        }
        this.pathPlanning.updateInfo(messageManager);
        this.actionExtMove.updateInfo(messageManager);
        //更新unSearchBuildings和fireBuildings
        List<Building> tempBuildings = new ArrayList<>();   //用来暂存着火的建筑
        for(EntityID id : this.worldInfo.getChanged().getChangedEntities()){
            StandardEntity standardEntity = worldInfo.getEntity(id);
            if(standardEntity instanceof Building){
                //if(unSearchBuildings.contains(standardEntity)){
                    //unSearchBuildings.remove(standardEntity);
                //}

                if (((Building) standardEntity).isOnFire()){
                    tempBuildings.add((Building) standardEntity);
                }
            }
        }
        this.updateWater();
        fireBuildings.clear();
        fireBuildings.addAll(tempBuildings);
        targetHistory.add(target);
        this.messageManager = messageManager;
        return this;
    }

    @Override
    public ExtAction setTarget(EntityID target) {
        this.target = null;
        if (target != null)
        {
            StandardEntity entity = this.worldInfo.getEntity(target);
            if (entity instanceof Building)
            {
                this.target = target;
            }
        }
        return this;
    }

    @Override
    public ExtAction calc() {
        this.result = null;
        FireBrigade agent = (FireBrigade) this.agentInfo.me();

        this.refillFlag = this.needRefill(agent, this.refillFlag);
        if (DebugHelper.DEBUG_MODE){
            List<Integer> elements = new ArrayList<>();
            if (refillFlag) {
                elements.add(agentInfo.getID().getValue());
            }
            DebugHelper.VD_CLIENT.drawAsync(agentInfo.getID().getValue(), "NeedRefillFB", (Serializable) elements);
        }

        List<Integer> elementList = Util.fetchIdValueFromElementIds(getAvailableHydrants());
        if (DebugHelper.DEBUG_MODE) {
            DebugHelper.VD_CLIENT.drawAsync(agentInfo.getID().getValue(), "AvailableHydrants", (Serializable) elementList);
        }
        if (this.refillFlag)
        {
            this.result = this.calcRefill(agent, this.pathPlanning, this.target);
            if (this.result != null && result instanceof ActionRefill)
            {
                messageManager.addMessage(new MessageFireBrigade(false, ((FireBrigade) agentInfo.me()),
                        MessageFireBrigade.ACTION_REFILL, agent.getPosition()));
                return this;
            }
        }else {
            //不需要补水时每个时刻重置没被占用的hydrant
             resetAvailableHydrants();
        }

        if (this.target == null) {
            return this;
        }

        this.result = this.calcExtinguish(agent, this.pathPlanning, this.target);
        return this;
    }

    /**
     * crf:火警不同情况的判断以及设定target等。
     */
    private Action calcExtinguish(FireBrigade agent, PathPlanning pathPlanning, EntityID target) {
        EntityID agentPosition = agent.getPosition();
        StandardEntity positionEntity = Objects.requireNonNull(this.worldInfo.getPosition(agent));


        // TODO 灭火位置的选取
        if(this.worldInfo.getDistance(agentInfo.me(), this.worldInfo.getEntity(target)) < this.maxExtinguishDistance ) {

//            //灭同一个建筑，但是水不减，说明那个建筑烧没了
//            if (lastTimeExtinguished && waterHistory.get(waterHistory.size() - 1).equals(waterHistory.get(waterHistory.size() - 2)) &&
//                    targetHistory.get(targetHistory.size() - 1).equals(targetHistory.get(targetHistory.size() - 2))) {
//                ((Building)worldInfo.getEntity(target)).setFieryness(8);
//
//            }else{
//                return new ActionExtinguish(target, this.maxExtinguishPower);
//            }
//            //如果上次也在灭火
            //处理偏差情况
            if (this.worldInfo.getDistance(positionEntity,this.worldInfo.getEntity(target)) < this.worldInfo.getDistance(agentInfo.me(), this.worldInfo.getEntity(target))){
                //向target方向调整位置坐标
//                int destinationY = (int) ((locationAgent.second()-locationAgentTarget.second())*0.05+locationAgent.second());
//                int destinationX = (int) ((locationAgent.first()-locationAgentTarget.first())*0.05+locationAgent.first());
//                // set MovePath
//                pathPlanning.setDestination(target);
//                List<EntityID> path = pathPlanning.calc().getResult();
////                ActionMove moveAction = (ActionMove) actionExtMove.setTarget(path.get(path.size() - 1)).calc().getAction();
//                Action adjustAction = new ActionMove(path,destinationX,destinationY);
//                if (adjustAction != null) {
//                    if (StandardEntityURN.BLOCKADE != positionEntity.getStandardURN()){
//                        System.out.println(agentInfo.getID()+" is moving to new place "+agentInfo.getX()+"  "+agentInfo.getY());
//                        return adjustAction;
//                    }
//                }
            }
//            System.out.println("checking ================= "+this.worldInfo.getDistance(agentInfo.me(),this.worldInfo.getEntity(target))+"  "+this.worldInfo.getDistance(positionEntity,this.worldInfo.getEntity(target)));
      	    return new ActionExtinguish(target, Math.min(agentInfo.getWater(), this.maxExtinguishPower));
        }

        //跑出火区
        StandardEntity standardEntity = this.worldInfo.getEntity(agentPosition);
        if (standardEntity instanceof Building && ((Building) standardEntity).isOnFire()) {
            List<StandardEntity> noBuilding = new ArrayList<>();
            for (StandardEntity en : this.worldInfo.getAllEntities()) {
                if (!(en instanceof Building)) {
                    noBuilding.add(en);
                }
            }
            if (noBuilding.isEmpty()) {
                return this.getMoveAction(pathPlanning, agentPosition, target);
            } else {
                noBuilding.sort(new DistanceSorter(this.worldInfo, this.agentInfo.me()));
                return this.getMoveAction(pathPlanning, agentPosition, noBuilding.get(0).getID());
            }
        }

        //如果火警在避难所
        if (StandardEntityURN.REFUGE == positionEntity.getStandardURN())
        {
//            System.out.println(agent.getID()+" is in a refuge=========");
            if(agent.getWater() < 0.9*maxExtinguishPower){
                return new ActionRefill();
            }
            Action action = this.getMoveAction(pathPlanning, agentPosition, target);
            if (action != null)
            {
                return action;
            }
        }

        //ROAD且target为null:寻找范围fb并跟随
        if (StandardEntityURN.ROAD == positionEntity.getStandardURN()) {
//            System.out.println(agent.getID()+" is on a road and its target is "+this.target);
            if (this.target == null) {
                for (Map.Entry<EntityID, Integer> entry : this.fireBrigadesWaterMap.entrySet()) {
                    if (this.worldInfo.getDistance(standardEntity, this.worldInfo.getEntity(entry.getKey())) < 4 * scenarioInfo.getFireExtinguishMaxDistance())
                        this.setTarget(this.worldInfo.getEntity(entry.getKey()).getID());
//                    System.out.println("fb id = "+entry.getKey()+"       water ="+entry.getValue());
                }
                return new ActionRest();
            }
        }



//        List<StandardEntity> neighbourBuilding = new ArrayList<>();
//        StandardEntity entity = this.worldInfo.getEntity(target);
//        if (entity instanceof Building)
//        {
//            if (this.worldInfo.getDistance(positionEntity, entity) < this.maxExtinguishDistance)
//            {
//                neighbourBuilding.add(entity);
//            }
//        }

        //找出着火的和温度达到40以上的建筑
        List<StandardEntity> dangerBuilding = new ArrayList<>();
        List<StandardEntity> burningBuilding = new ArrayList<>();
        for(EntityID entityID : this.worldInfo.getChanged().getChangedEntities()){
            if(this.worldInfo.getDistance(positionEntity, this.worldInfo.getEntity(entityID)) < this.maxExtinguishDistance){

                if (this.worldInfo.getEntity(entityID) instanceof  Building){
                    if(((Building)(this.worldInfo.getEntity(entityID))).isOnFire() &&
                    		((Building)(this.worldInfo.getEntity(entityID))).isFierynessDefined() &&
                    		((Building)(this.worldInfo.getEntity(entityID))).getFieryness() != 8){
                        burningBuilding.add(this.worldInfo.getEntity(entityID));
                    }
                    if(((Building)(this.worldInfo.getEntity(entityID))).getTemperature() > 40){
                        dangerBuilding.add(this.worldInfo.getEntity(entityID));
                    }
                }
            }

        }

        //如果在blockade中
        if (StandardEntityURN.BLOCKADE == positionEntity.getStandardURN()){
//            System.out.println(agent+"--------------");
            //给pf 发送信息
            if (dangerBuilding.size() > 0){
                FierynessSorter fierynessSorter = new FierynessSorter();
                Collections.sort(dangerBuilding, fierynessSorter);   //未测试
                //System.out.println("\n********fireExtinguish22222*******\n");
                return new ActionExtinguish(dangerBuilding.get(0).getID(),
                        Math.min(this.calcExtinguishTargetWater(dangerBuilding.get(0).getID()), agent.getWater()));
            }
        }

        //有正在燃烧的建筑物,水量够灭火，不够补水
        // TODO: 2020/10/17 如果看不见而且灭了怎么办
        if (burningBuilding.size() > 0)
        {
            FierynessSorter fierynessSorter = new FierynessSorter();
            Collections.sort(burningBuilding, fierynessSorter);   //未测试

            //neighbourBuilding.sort(new DistanceSorter(this.worldInfo, agent));
            if(calcExtinguishTargetWater(burningBuilding.get(0).getID()) > agent.getWater()){
                return this.getMoveAction(pathPlanning, agentPosition, calcRefillWaterTarget().getID());
            }else{
//                System.out.println("********fireExtinguish3333333*******");
                return new ActionExtinguish(burningBuilding.get(0).getID(), Math.min(this.maxExtinguishPower, agent.getWater()));
            }
        }
        List<StandardEntity> objectsInRange = new ArrayList<>(worldInfo.getObjectsInRange(target, maxExtinguishDistance));
        Collections.sort(objectsInRange, new DistanceComparator(worldInfo.getEntity(target), worldInfo));
        for (StandardEntity entity : objectsInRange) {
            if (entity instanceof Area) {
                Action moveAction = getMoveAction(pathPlanning, agentPosition, entity.getID());
                if (moveAction != null) {
                    return moveAction;
                }
            }
        }
        return null;
    }

    private Action getMoveAction(PathPlanning pathPlanning, EntityID from, EntityID target) {
        pathPlanning.setFrom(from);
        pathPlanning.setDestination(target);
        List<EntityID> path = pathPlanning.calc().getResult();
        return getMoveAction(path);
    }

    /**
     * 调用actionExtMove,实现判断stuck和通过stuckHelper获取路径
     */
    private Action getMoveAction(List<EntityID> path) {
        if (path != null && path.size() > 0) {
            StandardEntity entity = this.worldInfo.getEntity(path.get(path.size() - 1));
            if (entity instanceof Building) {
                if (entity.getStandardURN() != StandardEntityURN.REFUGE) {
                    path.remove(path.size() - 1);
                }
            }
            if (!path.isEmpty()) {
                ActionMove moveAction = (ActionMove) actionExtMove.setTarget(path.get(path.size() - 1)).calc().getAction();
                if (moveAction != null) {
                    return moveAction;
                }
            }
            return null;
        }
        return null;
    }


    /**
     *
     * @param agent 当前智能体
     * @param refillFlag 上一个时刻是否在补水
     * @return 水量低于refillRequest触发needRefill，直到补水到refillCompleted
     */
    private boolean needRefill(FireBrigade agent, boolean refillFlag) {
        if (refillFlag)
        {
            return agent.getWater() < this.refillCompletedThreshold;
        }
        return agent.getWater() <= this.refillRequestThreshold;
    }

    /**
     * crf:防止聚集fb在hydrant排队等待补水的情况
     * @param agent
     * @param pathPlanning
     * @param target
     * @return
     */
    private Action calcRefill(FireBrigade agent, PathPlanning pathPlanning, EntityID target) {
        //如果自己之前在这里补水，且还没满，就接着补水
        if (world.getSelfPosition() instanceof Refuge || world.getSelfPosition() instanceof Hydrant &&
                waterHistory.size() >= 2 &&
                this.waterHistory.get(waterHistory.size() - 1) > this.waterHistory.get(waterHistory.size() - 2)) {
            if (CSUConstants.DEBUG_WATER_REFILL) {
                System.out.println("water history: " + waterHistory);
            }
            return new ActionRefill();
        }
        return calcRefugeAndHydrantAction(agent, pathPlanning, target);
    }


    private boolean needRest(Human agent) {
        int hp = agent.getHP();
        int damage = agent.getDamage();
        if (hp == 0 || damage == 0)
        {
            return false;
        }
        int activeTime = (hp / damage) + ((hp % damage) != 0 ? 1 : 0);
        if (this.kernelTime == -1)
        {
            try
            {
                this.kernelTime = this.scenarioInfo.getKernelTimesteps();
            }
            catch (NoSuchConfigOptionException e)
            {
                this.kernelTime = -1;
            }
        }
        return damage >= this.thresholdRest || (activeTime + this.agentInfo.getTime()) < this.kernelTime;
    }


    /** hydrant and refuge */
    private Action calcRefugeAndHydrantAction(Human human, PathPlanning pathPlanning, EntityID target) {
        Set<EntityID> availableSupplier = getAvailableHydrants();
        availableSupplier.addAll(this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE));
        return this.calcSupplyAction(
                human,
                pathPlanning,
                availableSupplier,
                target,
                true
        );
    }

    /** refuge */
    private Action calcRefugeAction(Human human, PathPlanning pathPlanning, EntityID target, boolean isRefill) {
        return this.calcSupplyAction(
                human,
                pathPlanning,
                this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE),
                target,
                isRefill
        );
    }

    private Action calcHydrantAction(Human human, PathPlanning pathPlanning, EntityID target) {
        Collection<EntityID> hydrants = this.worldInfo.getEntityIDsOfType(HYDRANT);
        hydrants.remove(human.getPosition());
        return this.calcSupplyAction(
                human,
                pathPlanning,
                hydrants,
                target,
                true
        );
    }

    /**
     * 计算路途开销和refill开销最小的补水处
     *
     * @param human
     * @param pathPlanning
     * @param supplyPositions
     * @param target
     * @param isRefill
     * @return
     */
    private Action calcSupplyAction(Human human, PathPlanning pathPlanning, Collection<EntityID> supplyPositions, EntityID target, boolean isRefill) {
        if (supplyPositions.contains(human.getPosition())) {
            return new ActionRefill();
        }
        int refillHydrantRate = scenarioInfo.getFireTankRefillHydrantRate();
        int refillRefugeRate = scenarioInfo.getFireTankRefillRate();
        //路上和refill所用step之和
        HashMap<EntityID, Double> refillTimeCost = new HashMap<>();
        supplyPositions.forEach(entityID -> {
            double roadCost = Ruler.getManhattanDistance(world.getSelfLocation(), world.getLocation(entityID)) / CSUConstants.MEAN_VELOCITY_DISTANCE;
            int refillCost;
            if (worldInfo.getEntity(entityID) instanceof Refuge) {
                refillCost = (scenarioInfo.getFireTankMaximum() - agentInfo.getWater()) / refillRefugeRate + 1;
            } else {
                refillCost = (scenarioInfo.getFireTankMaximum() - agentInfo.getWater()) / refillHydrantRate + 1;
            }
            refillTimeCost.put(entityID, roadCost + refillCost);
        });

        //开销从小到大排序
        ArrayList<Map.Entry<EntityID, Double>> sortedSupplyPositions = new ArrayList<>(refillTimeCost.entrySet());
        sortedSupplyPositions.sort((t0, t1) -> (int) (t0.getValue() - t1.getValue()));
        if (CSUConstants.DEBUG_WATER_REFILL) {
            System.out.println("agentid: " + agentInfo.getID() + ", sortedSupplyPositions: " + sortedSupplyPositions);
        }
        for (Map.Entry<EntityID, Double> sortedSupplyPosition : sortedSupplyPositions) {
            Action action = getMoveAction(pathPlanning, world.getSelfPositionId(), sortedSupplyPosition.getKey());
            if (action != null) {
                return action;
            }
        }
        return null;
    }

    /**
     * CSU-zack
     * @return 从需要补水开始，去掉
     */
    private Set<EntityID> getAvailableHydrants() {
        availableHydrants.removeAll(getOccupiedHydrantsThisTime());
        return availableHydrants;
    }

    /**
     * CSU-zack
     */
    private void resetAvailableHydrants() {
        availableHydrants.clear();
        availableHydrants.addAll(Util.fetchEntityIdFromElements(worldInfo.getEntitiesOfType(HYDRANT)));
    }



    /**
     * CSU-zack
     * @return 当前时刻已知被占用的Hydrants
     */
    private Set<EntityID> getOccupiedHydrantsThisTime() {

        Set<EntityID> result = new HashSet<>();
        for (CommunicationMessage communicationMessage : messageManager.getReceivedMessageList()) {
            if (communicationMessage instanceof MessageFireBrigade &&
                    !((MessageFireBrigade) communicationMessage).getAgentID().equals(agentInfo.getID())) {
                EntityID targetID = ((MessageFireBrigade) communicationMessage).getTargetID();
                int action = ((MessageFireBrigade) communicationMessage).getAction();
                if (action == MessageFireBrigade.ACTION_REFILL) {
                    result.add(targetID);
                }
            }
        }
        if (agentInfo.getPositionArea() instanceof Hydrant) {
            //在这个hydrant上的所有智能体
            Set<EntityID> fbs = worldInfo.getEntitiesOfType(FIRE_BRIGADE)
                    .stream()
                    .filter(e -> ((FireBrigade)e).getPosition().equals(agentInfo.getPosition()))
                    .map(StandardEntity::getID)
                    .collect(Collectors.toSet());
            //如果有需要补水的智能体同时到，让id最小的补水
            fbs.forEach(e -> {
                if (fireBrigadesWaterMap.get(e) < refillRequestThreshold && agentInfo.getID().getValue() > e.getValue()) {
                    result.add(agentInfo.getPositionArea().getID());
                }
            });
        }
        return result;
    }


    private class DistanceSorter implements Comparator<StandardEntity> {
        private StandardEntity reference;
        private WorldInfo worldInfo;

        DistanceSorter(WorldInfo wi, StandardEntity reference)
        {
            this.reference = reference;
            this.worldInfo = wi;
        }

        public int compare(StandardEntity a, StandardEntity b)
        {
            int d1 = this.worldInfo.getDistance(this.reference, a);
            int d2 = this.worldInfo.getDistance(this.reference, b);
            return d1 - d2;
        }
    }

    //建筑物按燃烧度来排序
    private class FierynessSorter implements Comparator<StandardEntity>{

        public int compare(StandardEntity a, StandardEntity b) {
            return ((Building)a).getFieryness() - ((Building)b).getFieryness();
        }
    }

    //判断是否被困住
    private boolean isInBlockade(Human human) {
        if(!human.isXDefined() || !human.isYDefined()) return false;
        int agentX = human.getX();
        int agentY = human.getY();
        StandardEntity positionEntity = this.worldInfo.getPosition(human);
        if(positionEntity instanceof Road){
            Road road = (Road)positionEntity;
            if(road.isBlockadesDefined() && road.getBlockades().size() > 0){
                for(Blockade blockade : worldInfo.getBlockades(road)){
                    if(blockade.getShape().contains(agentX, agentY)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    //计算扑灭一个目标建筑所需水量
    private int calcExtinguishTargetWater(EntityID target) {
        if(this.worldInfo.getEntity(target) instanceof Building)
        {
            Building targetBuilding = (Building)this.worldInfo.getEntity(target);
            int area=0;
            if(targetBuilding.isTotalAreaDefined())
                area = targetBuilding.getTotalArea();
            int temperature = 0;
            if(targetBuilding.isTemperatureDefined())
                temperature = targetBuilding.getTemperature();

            return area * areaConstant + temperature * temperatureConstant;
            //this.refillRequest=1000;
        }

        return 0;
    }



    /**
     * CSU-zack
     * @return 当前时刻fb是否正在补水
     */
    private boolean isRefilling(EntityID entityID) {
        for (CommunicationMessage communicationMessage : messageManager.getReceivedMessageList()) {
            if (communicationMessage instanceof MessageFireBrigade) {
                MessageFireBrigade messageFireBrigade = (MessageFireBrigade) communicationMessage;
                if (messageFireBrigade.getAgentID().equals(entityID) && messageFireBrigade.getAction() == MessageFireBrigade.ACTION_REFILL) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * crf:
     * 判断一个消防栓是否正在被占用:
     * 确定hydrant范围内的fb isrefill() && 范围内等待中的fb(getwater = 0)
     */
    private boolean isOccupied(EntityID hydrantID) {
        Set<EntityID> availableHydrants = getAvailableHydrants();
        return !availableHydrants.contains(hydrantID);
    }

    //返回补水的目标
    private Entity calcRefillWaterTarget() {
        Collection<StandardEntity> Targets=this.worldInfo.getEntitiesOfType(HYDRANT,REFUGE);
        List<StandardEntity> RefillTargets=new ArrayList<>();
        for (StandardEntity s:Targets)
        {
            RefillTargets.add(s);
        }

        //优先遍历hydrant，然后refuge，保证水量充足
        RefillTargets.sort(new DistanceSorter(this.worldInfo,this.agentInfo.me()));
        for(StandardEntity s:RefillTargets)
        {
            if(s instanceof Hydrant && !isOccupied(s.getID())) {
                return s;
            }
        }
        for(StandardEntity s:RefillTargets)
        {
            if(s instanceof Refuge) {
                return s;
            }
        }
        return null;
    }

    //更新fireBrigadesWaterMap
    private void updateWater() {
        this.fireBrigadesWaterMap.clear();
        for(StandardEntity entity:this.worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE))
        {
            FireBrigade fireBrigade = (FireBrigade) entity;
            this.fireBrigadesWaterMap.put(entity.getID(), fireBrigade.getWater());
        }
        waterHistory.add(agentInfo.getWater());
    }

    public int getWaterHistory(Integer time) {
        return waterHistory.get(time - 1);
    }

}




