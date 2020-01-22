package CSU_Yunlu_2019.extaction.fb;


import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.action.common.ActionRest;
import adf.agent.action.fire.ActionExtinguish;
import adf.agent.action.fire.ActionRefill;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.extaction.ExtAction;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.HYDRANT;
import static rescuecore2.standard.entities.StandardEntityURN.REFUGE;

public class FB_ActionFireFighting extends ExtAction
{
    private PathPlanning pathPlanning;

    private int maxExtinguishDistance;
    private int maxExtinguishPower;
    private int thresholdRest;
    private int kernelTime;
    private int refillCompleted;
    private int refillRequest;
    private boolean refillFlag;

    private EntityID target;
    private List<Building> unSearchBuildings;   //尚未搜索过的建筑
    private List<Building> fireBuildings;       //燃烧的建筑物
    private int areaConstant = 10;                   //面积的系数
    private int temperatureConstant = 15;            //温度的系数
    private Map<EntityID,Integer> fireBrigadesWaterMap = new HashMap<>();   //每个消防员与其相应水量的键值对

    public FB_ActionFireFighting(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, DevelopData developData)
    {
        super(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
	this.fireBuildings = new ArrayList<>(); 
        this.maxExtinguishDistance = scenarioInfo.getFireExtinguishMaxDistance();
        this.maxExtinguishPower = scenarioInfo.getFireExtinguishMaxSum();
        this.thresholdRest = developData.getInteger("ActionFireFighting.rest", 100);
        int maxWater = scenarioInfo.getFireTankMaximum();
        this.refillCompleted = (maxWater / 10) * developData.getInteger("ActionFireFighting.refill.completed", 10);
        //this.refillRequest = this.maxExtinguishPower * developData.getInteger("ActionFireFighting.refill.request", 1);
        this.refillRequest = 1000 * developData.getInteger("ActionFireFighting.refill.request", 1);
        this.refillFlag = false;

        this.target = null;


        switch  (scenarioInfo.getMode()) {
	            case PRECOMPUTATION_PHASE:
	                this.pathPlanning = moduleManager.getModule("ActionFireFighting.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
	                break;
	            case PRECOMPUTED:
	                this.pathPlanning = moduleManager.getModule("ActionFireFighting.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
	                break;
	            case NON_PRECOMPUTE:
	                this.pathPlanning = moduleManager.getModule("ActionFireFighting.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
	                break;
	        }
    }

    @Override
    public ExtAction precompute(PrecomputeData precomputeData)
    {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        this.pathPlanning.precompute(precomputeData);
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
    public ExtAction resume(PrecomputeData precomputeData)
    {
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
    public ExtAction preparate()
    {
        super.preparate();
        if (this.getCountPreparate() >= 2)
        {
            return this;
        }
        this.pathPlanning.preparate();
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
    public ExtAction updateInfo(MessageManager messageManager)
    {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2)
        {
            return this;
        }
        this.pathPlanning.updateInfo(messageManager);
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
        return this;
    }

    @Override
    public ExtAction setTarget(EntityID target)
    {
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
    public ExtAction calc()
    {
        this.result = null;
        FireBrigade agent = (FireBrigade) this.agentInfo.me();

        this.refillFlag = this.needRefill(agent, this.refillFlag);
        this.updateWater();
        if (this.refillFlag)
        {
            this.result = this.calcRefill(agent, this.pathPlanning, this.target);
            if (this.result != null)
            {
                this.updateWater();
                return this;
            }
        }

        if (this.needRest(agent))
        {
            this.result = this.calcRefugeAction(agent, this.pathPlanning, this.target, false);
            if (this.result != null)
            {
                this.updateWater();
                return this;
            }
        }

        if (this.target == null)
        {
            this.updateWater();
            return this;
        }
        this.result = this.calcExtinguish(agent, this.pathPlanning, this.target);
        return this;
    }

    private Action calcExtinguish(FireBrigade agent, PathPlanning pathPlanning, EntityID target)
    {/*System.out.println("\n********fireExtinguish00000*******\n");*/
    	EntityID agentPosition = agent.getPosition();
        StandardEntity positionEntity = Objects.requireNonNull(this.worldInfo.getPosition(agent));
        
        if(this.worldInfo.getDistance(positionEntity, this.worldInfo.getEntity(target)) < this.maxExtinguishDistance) {
		//System.out.println("\n********fireExtinguish11111*******\n");
        	return new ActionExtinguish(target, this.maxExtinguishPower);
        }

        //跑出火区
        StandardEntity standardEntity = this.worldInfo.getEntity(agentPosition);
        if(standardEntity instanceof Building && ((Building) standardEntity).isOnFire()) {
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

        //如果人就在避难所
        if (StandardEntityURN.REFUGE == positionEntity.getStandardURN())
        {
            if(agent.getWater() < 0.9*maxExtinguishPower){
                return new ActionRefill();
            }
            Action action = this.getMoveAction(pathPlanning, agentPosition, target);
            if (action != null)
            {
                return action;
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

        if(isInBlockade(agent) && dangerBuilding.size() > 0){
            FierynessSorter fierynessSorter = new FierynessSorter();
            Collections.sort(dangerBuilding, fierynessSorter);   //未测试
	    //System.out.println("\n********fireExtinguish22222*******\n");
            return new ActionExtinguish(dangerBuilding.get(0).getID(), this.calcExtinguishTargetWater(dangerBuilding.get(0).getID()));
        }
        if (burningBuilding.size() > 0)
        {
            FierynessSorter fierynessSorter = new FierynessSorter();
            Collections.sort(burningBuilding, fierynessSorter);   //未测试

            //neighbourBuilding.sort(new DistanceSorter(this.worldInfo, agent));
            if(calcExtinguishTargetWater(burningBuilding.get(0).getID()) > agent.getWater()){
                return this.getMoveAction(pathPlanning, agentPosition, calcRefillWaterTarget().getID());
            }else{System.out.println("********fireExtinguish3333333*******");
                return new ActionExtinguish(burningBuilding.get(0).getID(), this.maxExtinguishPower);
            }
        }
        return this.getMoveAction(pathPlanning, agentPosition, target);
    }

    private Action getMoveAction(PathPlanning pathPlanning, EntityID from, EntityID target)
    {
        pathPlanning.setFrom(from);
        pathPlanning.setDestination(target);
        List<EntityID> path = pathPlanning.calc().getResult();
        if (path != null && path.size() > 0)
        {
            StandardEntity entity = this.worldInfo.getEntity(path.get(path.size() - 1));
            if (entity instanceof Building)
            {
                if (entity.getStandardURN() != StandardEntityURN.REFUGE)
                {
                    path.remove(path.size() - 1);
                }
            }
            return new ActionMove(path);
        }
        return null;
    }

    private boolean needRefill(FireBrigade agent, boolean refillFlag)
    {
        if (refillFlag)
        {
            StandardEntityURN positionURN = Objects.requireNonNull(this.worldInfo.getPosition(agent)).getStandardURN();
            return !(positionURN == REFUGE || positionURN == HYDRANT) || agent.getWater() < this.refillCompleted;
        }
        return agent.getWater() <= this.refillRequest;
    }

    private boolean needRest(Human agent)
    {
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

    private Action calcRefill(FireBrigade agent, PathPlanning pathPlanning, EntityID target)
    {
        StandardEntityURN positionURN = Objects.requireNonNull(this.worldInfo.getPosition(agent)).getStandardURN();
        if (positionURN == REFUGE)
        {
            return new ActionRefill();
        }
        Action action = this.calcRefugeAction(agent, pathPlanning, target, true);
        if (action != null)
        {
            return action;
        }
        action = this.calcHydrantAction(agent, pathPlanning, target);
        if (action != null)
        {
            if (positionURN == HYDRANT && action.getClass().equals(ActionMove.class))
            {
                pathPlanning.setFrom(agent.getPosition());
                pathPlanning.setDestination(target);
                double currentDistance = pathPlanning.calc().getDistance();
                List<EntityID> path = ((ActionMove) action).getPath();
                pathPlanning.setFrom(path.get(path.size() - 1));
                pathPlanning.setDestination(target);
                double newHydrantDistance = pathPlanning.calc().getDistance();
                if (currentDistance <= newHydrantDistance)
                {
                    return new ActionRefill();
                }
            }
            return action;
        }
        return null;
    }

    private Action calcRefugeAction(Human human, PathPlanning pathPlanning, EntityID target, boolean isRefill)
    {
        return this.calcSupplyAction(
                human,
                pathPlanning,
                this.worldInfo.getEntityIDsOfType(StandardEntityURN.REFUGE),
                target,
                isRefill
        );
    }

    private Action calcHydrantAction(Human human, PathPlanning pathPlanning, EntityID target)
    {
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

    private Action calcSupplyAction(Human human, PathPlanning pathPlanning, Collection<EntityID> supplyPositions, EntityID target, boolean isRefill)
    {
        EntityID position = human.getPosition();
        int size = supplyPositions.size();
        if (supplyPositions.contains(position))
        {
            return isRefill ? new ActionRefill() : new ActionRest();
        }
        List<EntityID> firstResult = null;
        while (supplyPositions.size() > 0)
        {
            pathPlanning.setFrom(position);
            pathPlanning.setDestination(supplyPositions);
            List<EntityID> path = pathPlanning.calc().getResult();
            if (path != null && path.size() > 0)
            {
                if (firstResult == null)
                {
                    firstResult = new ArrayList<>(path);
                    if (target == null)
                    {
                        break;
                    }
                }
                EntityID supplyPositionID = path.get(path.size() - 1);
                pathPlanning.setFrom(supplyPositionID);
                pathPlanning.setDestination(target);
                List<EntityID> fromRefugeToTarget = pathPlanning.calc().getResult();
                if (fromRefugeToTarget != null && fromRefugeToTarget.size() > 0)
                {
                    return new ActionMove(path);
                }
                supplyPositions.remove(supplyPositionID);
                //remove failed
                if (size == supplyPositions.size())
                {
                    break;
                }
                size = supplyPositions.size();
            }
            else
            {
                break;
            }
        }
        return firstResult != null ? new ActionMove(firstResult) : null;
    }

    private class DistanceSorter implements Comparator<StandardEntity>
    {
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
    private int calcExtinguishTargetWater(EntityID target)
    {
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

    //判断一个消防员是否正在补水
    private boolean isRefilling(EntityID entityID)
    {
        if(this.fireBrigadesWaterMap.get(entityID) != null)
        {
            FireBrigade fireBrigade = (FireBrigade)this.worldInfo.getEntity(entityID);
            int lastWater = this.fireBrigadesWaterMap.get(entityID);
            int nowWater = fireBrigade.getWater();
            if(nowWater - lastWater > 0)
            {
                return true;
            }
        }
        return false;
    }

    //判断一个消防栓是否正在被占用
    private boolean isOccupied(EntityID hydrantID)
    {
        EntityID myID = this.agentInfo.getID();
        for(StandardEntity agents:this.worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE))
        {
            if(agents instanceof FireBrigade)
            {
                FireBrigade firebrigade=(FireBrigade) agents;
                if(firebrigade.getPosition().equals(hydrantID) && this.isRefilling(firebrigade.getID()) && !firebrigade.getID().equals(myID))
                {
                    return true;
                }
            }
        }
        return false;
    }

    //返回补水的目标
    private Entity calcRefillWaterTarget()
    {
        Collection<StandardEntity> Targets=this.worldInfo.getEntitiesOfType(HYDRANT,REFUGE);
        List<StandardEntity> RefillTargets=new ArrayList<>();
        for (StandardEntity s:Targets)
        {
            RefillTargets.add(s);
        }

        RefillTargets.sort(new DistanceSorter(this.worldInfo,this.agentInfo.me()));
        for(StandardEntity s:RefillTargets)
        {
            if(s instanceof Refuge)
            {
                return s;
            }
            if(s instanceof Hydrant && !isOccupied(s.getID()))
            {
                return s;
            }
        }

        return null;
    }

    //更新fireBrigadesWaterMap
    private void updateWater()
    {
        this.fireBrigadesWaterMap.clear();
        for(StandardEntity entity:this.worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE))
        {
            FireBrigade fireBrigade = (FireBrigade) entity;
            this.fireBrigadesWaterMap.put(entity.getID(), fireBrigade.getWater());
        }
    }

}




