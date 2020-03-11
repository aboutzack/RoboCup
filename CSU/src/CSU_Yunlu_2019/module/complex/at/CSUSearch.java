package CSU_Yunlu_2019.module.complex.at;

import CSU_Yunlu_2019.CSUConstants;
import CSU_Yunlu_2019.util.Util;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.information.MessageBuilding;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.Search;
import rescuecore2.messages.Command;
import rescuecore2.misc.Handy;
import rescuecore2.standard.entities.*;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;

/**
 * 同sample的Search，作用为选择一个最优的搜索目标并且返回
 */
public class CSUSearch extends Search {
	private PathPlanning pathPlanning;
	private Clustering clustering;

	//todo 防止多个at选中同一个building
	private int voiceRange;//声音传播距离
	private Collection<StandardEntity> knownCivilians;//所有已知的平民(选为target时就移出)
	//private Set<EntityID> knownHeardCivilians;//已知位置的呼救平民(选为target时就移出)
	private Set<EntityID> heardCivilians;//未知位置的呼救平民
	private Set<EntityID> searchedBuildings;//已经到访过的建筑（永久）
	private Set<EntityID> optimalBuildings;//最优访问的建筑(已知有人的建筑)
	private Set<EntityID> secondaryBuildings;//次级优先访问的建筑(听到声音范围的建筑)
	private Set<EntityID> unsearchedBuildings;//其余等待搜索的建筑
	private int currentTargetPriority;//当前任务优先级(0:访问过或者无目标,1:普通目标,2:次优目标,3:最优目标)
	//private Set<EntityID> grabedBuildings;//被抢占的建筑
	private boolean first = true;
	private final int maxTargetTime = 6;//执行一个普通建筑最长的时间,超出时间加入挂起池
	private int targetTime = 0;//已经执行了普通任务的时长
	private Set<EntityID> hangUpBuildings;//挂起池
	private final int maxHangUpTime = 15;//最长挂起时间
	private int hangUpTime = 0;
	//private int roudNum = 0;//回合数
	//private EntityID lastresult;//上一次的目标
	private MessageManager messageManager;

	private EntityID result;//寻路结果

	private Area positionArea;//当前地形

	public CSUSearch(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager,
					 DevelopData developData) {
		super(ai, wi, si, moduleManager, developData);

		this.unsearchedBuildings = new HashSet<EntityID>();
		this.positionArea= agentInfo.getPositionArea();

		StandardEntityURN agentURN = ai.me().getStandardURN();
		switch (si.getMode()) {
		case PRECOMPUTATION_PHASE:
			if (agentURN == AMBULANCE_TEAM) {
				this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Ambulance",
						"adf.sample.module.algorithm.SamplePathPlanning");
				this.clustering = moduleManager.getModule("SampleSearch.Clustering.Ambulance",
						"adf.sample.module.algorithm.SampleKMeans");
			} else if (agentURN == FIRE_BRIGADE) {
				this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire",
						"adf.sample.module.algorithm.SamplePathPlanning");
				this.clustering = moduleManager.getModule("SampleSearch.Clustering.Fire",
						"adf.sample.module.algorithm.SampleKMeans");
			} else if (agentURN == POLICE_FORCE) {
				this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Police",
						"adf.sample.module.algorithm.SamplePathPlanning");
				this.clustering = moduleManager.getModule("SampleSearch.Clustering.Police",
						"adf.sample.module.algorithm.SampleKMeans");
			}
			break;
		case PRECOMPUTED:
			if (agentURN == AMBULANCE_TEAM) {
				this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Ambulance",
						"adf.sample.module.algorithm.SamplePathPlanning");
				this.clustering = moduleManager.getModule("SampleSearch.Clustering.Ambulance",
						"adf.sample.module.algorithm.SampleKMeans");
			} else if (agentURN == FIRE_BRIGADE) {
				this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire",
						"adf.sample.module.algorithm.SamplePathPlanning");
				this.clustering = moduleManager.getModule("SampleSearch.Clustering.Fire",
						"adf.sample.module.algorithm.SampleKMeans");
			} else if (agentURN == POLICE_FORCE) {
				this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Police",
						"adf.sample.module.algorithm.SamplePathPlanning");
				this.clustering = moduleManager.getModule("SampleSearch.Clustering.Police",
						"adf.sample.module.algorithm.SampleKMeans");
			}
			break;
		case NON_PRECOMPUTE:
			if (agentURN == AMBULANCE_TEAM) {
				this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Ambulance",
						"adf.sample.module.algorithm.SamplePathPlanning");
				this.clustering = moduleManager.getModule("SampleSearch.Clustering.Ambulance",
						"adf.sample.module.algorithm.SampleKMeans");
			} else if (agentURN == FIRE_BRIGADE) {
				this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Fire",
						"adf.sample.module.algorithm.SamplePathPlanning");
				this.clustering = moduleManager.getModule("SampleSearch.Clustering.Fire",
						"adf.sample.module.algorithm.SampleKMeans");
			} else if (agentURN == POLICE_FORCE) {
				this.pathPlanning = moduleManager.getModule("SampleSearch.PathPlanning.Police",
						"adf.sample.module.algorithm.SamplePathPlanning");
				this.clustering = moduleManager.getModule("SampleSearch.Clustering.Police",
						"adf.sample.module.algorithm.SampleKMeans");
			}
			break;
		}
		registerModule(this.pathPlanning);
		registerModule(this.clustering);

		this.voiceRange = scenarioInfo.getRawConfig().getIntValue("comms.channels.0.range");
		knownCivilians = new HashSet<StandardEntity>();
		//knownHeardCivilians = new HashSet<EntityID>();
		heardCivilians = new HashSet<EntityID>();
		searchedBuildings = new HashSet<EntityID>();
		optimalBuildings = new HashSet<EntityID>();
		secondaryBuildings = new HashSet<EntityID>();
		hangUpBuildings = new HashSet<EntityID>();
		this.currentTargetPriority = 0;
	}

	@Override//更新search的属性，
	public Search updateInfo(MessageManager messageManager) {
		super.updateInfo(messageManager);
		if (this.getCountUpdateInfo() >= 2) {
			return this;
		}

		clustering.updateInfo(messageManager);
		pathPlanning.updateInfo(messageManager);

		this.updateKnownCivilians();
		this.updateHeardCivilians();

		this.updateSearchedBuildings();
		this.updateUnsearchedBuildings();
		this.updateSecondaryBuildings();
		this.updateOptimalBuildings();
		this.messageManager = messageManager;
		if(CSUConstants.DEBUG_AT_SEARCH && false){
			int i = 0;
			for (EntityID b:unsearchedBuildings){
					System.out.print(b+":"+Util.getdistance(worldInfo.getLocation(agentInfo.getID()), worldInfo.getLocation(b))+" ");
					i++;
					if(i >= 10) {
						System.out.println();
						i = 0;
					}
			}
			System.out.println("-------------------------------");
		}

		return this;
	}

	@Override
	public Search calc() {
		boolean targetRepeat = false;
		if (CSUConstants.DEBUG_AT_SEARCH) System.out.println("ATSearchID:"+agentInfo.getID());
		//开局前三秒不能行动，生成地形
		if (agentInfo.getTime() < scenarioInfo.getKernelAgentsIgnoreuntil()) {
			return this;
		}
		hangUpTime++;
		if(hangUpTime >= maxHangUpTime){
			hangUpTime = 0;
			Iterator it = hangUpBuildings.iterator();
			if(it.hasNext()) unsearchedBuildings.add((EntityID) it.next());
		}
		//防止多个at选择同一个建筑
		if(this.result != null){
			Building building = (Building)this.worldInfo.getEntity(this.result);
			int buriedHumanNum = worldInfo.getBuriedHumans(building).size();
			int rescuingATNum = 0;
			Set<Entity> entities = new HashSet<Entity>(worldInfo.getEntitiesOfType(AMBULANCE_TEAM));
			for(Entity entity : entities){
				AmbulanceTeam at = (AmbulanceTeam)entity;
				if(worldInfo.getEntity(at.getPosition()) == worldInfo.getEntity(this.result)) rescuingATNum++;
			}
			if(rescuingATNum >= buriedHumanNum && rescuingATNum != 0) targetRepeat = true;
			if(targetRepeat){
				EntityID lastTarget = this.result;
				optimalBuildings.remove(this.result);
				secondaryBuildings.remove(this.result);
				unsearchedBuildings.remove(this.result);
				if(calcOptimalTarget()){
					if(CSUConstants.DEBUG_AT_SEARCH){
						targetTime = 0;
						building = (Building) worldInfo.getEntity(result);
						optimalBuildings.add(lastTarget);
						System.out.println("重复目标换为最优目标:"+building.getID());
					}
				}else if(calcSecondaryTarget()){
					if(CSUConstants.DEBUG_AT_SEARCH){
						targetTime = 0;
						building = (Building) worldInfo.getEntity(result);
						secondaryBuildings.add(lastTarget);
						System.out.println("重复目标换为次优目标:"+building.getID());
					}
				}else if(calcUnsearchedTarget()){
					targetTime = 0;
					if (CSUConstants.DEBUG_AT_SEARCH){
						building = (Building) worldInfo.getEntity(result);
						unsearchedBuildings.add(lastTarget);
						System.out.println("重复目标换为普通目标:"+building.getID());
					}
				}else{
					if(CSUConstants.DEBUG_AT_SEARCH){
						System.out.println("当前无合适替换重复目标");
					}
				}
				return this;
			}
		}
		//最高级抢占次级需要
		if(searchedBuildings.contains(this.result) || this.result == null){
//			this.sortBuildings(optimalBuildings,0,optimalBuildings.size()-1);
//			this.sortBuildings(secondaryBuildings,0,secondaryBuildings.size()-1);
//			this.sortBuildings(unsearchedBuildings,0,unsearchedBuildings.size()-1);
			if (CSUConstants.DEBUG_AT_SEARCH) System.out.println("因为:"+this.result+"进来");
			if(calcOptimalTarget()){
				if(CSUConstants.DEBUG_AT_SEARCH){
					targetTime = 0;
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("无目标换为最优目标:"+building.getID());
				}
			}else if(calcSecondaryTarget()){
				if(CSUConstants.DEBUG_AT_SEARCH){
					targetTime = 0;
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("无目标换为次优目标:"+building.getID());
				}
			}else if(calcUnsearchedTarget()){
				targetTime = 0;
				if (CSUConstants.DEBUG_AT_SEARCH){
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("无目标换为普通目标:"+building.getID());
				}
			}else{
				if(CSUConstants.DEBUG_AT_SEARCH){
					System.out.println("当前无合适目标");
				}
			}
			return this;
		}else if(currentTargetPriority == 1){
//			this.sortBuildings(optimalBuildings,0,optimalBuildings.size()-1);
//			this.sortBuildings(secondaryBuildings,0,secondaryBuildings.size()-1);
			if(calcOptimalTarget()){
				targetTime = 0;
				if(CSUConstants.DEBUG_AT_SEARCH){
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("普通目标换为最优目标:"+building.getID());
				}
			}else if(calcSecondaryTarget()){
				targetTime = 0;
				if(CSUConstants.DEBUG_AT_SEARCH){
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("普通目标换为次优目标:"+building.getID());
				}
			}else {
				targetTime++;
				if(targetTime >= maxTargetTime){
					unsearchedBuildings.remove(result);//防止下个普通目标重复
					hangUpBuildings.add(result);
					calcUnsearchedTarget();
					if (CSUConstants.DEBUG_AT_SEARCH) System.out.println("更换普通目标"+result);
					targetTime = 0;
				}
				if (CSUConstants.DEBUG_AT_SEARCH){
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("保持普通目标:"+building.getID());
				}
			}
			return this;
		}else if(currentTargetPriority == 2){
//			this.sortBuildings(optimalBuildings,0,optimalBuildings.size()-1);
			if(calcOptimalTarget()){
				if(CSUConstants.DEBUG_AT_SEARCH){
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("次优目标换为最优目标:"+building.getID());
				}
			}
			return this;
		}else if(currentTargetPriority == 3){
			return this;
		}
		return this;
	}

	@Override
	public EntityID getTarget() {
		return this.result;
	}

	@Override
	public Search precompute(PrecomputeData precomputeData) {
		super.precompute(precomputeData);
		if (this.getCountPrecompute() >= 2) {
			return this;
		}
		return this;
	}

	@Override
	public Search resume(PrecomputeData precomputeData) {
		super.resume(precomputeData);
		if (this.getCountResume() >= 2) {
			return this;
		}
		this.worldInfo.requestRollback();
		return this;
	}


	@Override
	public Search preparate() {
		super.preparate();
		if (this.getCountPreparate() >= 2) {
			return this;
		}
		this.worldInfo.requestRollback();
		return this;
	}


	//重新获取unsearchedBuildingIDs（同一聚类的building）
	private void reset() {
		this.unsearchedBuildings.clear();

		Collection<StandardEntity> clusterEntities = null;
		if (this.clustering != null) {
			int clusterIndex = this.clustering.getClusterIndex(this.agentInfo.getID());
			clusterEntities = this.clustering.getClusterEntities(clusterIndex);

		}
		if (clusterEntities != null && clusterEntities.size() > 0) {
			for (StandardEntity entity : clusterEntities) {
				if (entity instanceof Building && entity.getStandardURN() != REFUGE) {
					this.unsearchedBuildings.add(entity.getID());
				}
			}
		} else {
			this.unsearchedBuildings.addAll(this.worldInfo.getEntityIDsOfType(BUILDING, GAS_STATION, AMBULANCE_CENTRE,
					FIRE_STATION, POLICE_OFFICE));
		}
	}

	//最优目标:已知平民所在的building
	private boolean calcOptimalTarget(){
		//this.result = null;
		this.pathPlanning.setFrom(this.agentInfo.getPosition());
		this.pathPlanning.setDestination(optimalBuildings);
		if(!optimalBuildings.isEmpty()){
			List<EntityID> path = this.pathPlanning.calc().getResult();
			if (path != null && path.size() > 0) {
				if (CSUConstants.DEBUG_AT_SEARCH) System.out.println("找到最优");
				this.result = path.get(path.size() - 1);//获取终点
				currentTargetPriority = 3;
				knownCivilians.removeAll(this.worldInfo.getBuriedHumans(result));
				heardCivilians.removeAll(this.worldInfo.getBuriedHumans(result));
				//searchedBuildings.add(this.result);
				unsearchedBuildings.remove(this.result);
				secondaryBuildings.remove(this.result);
				optimalBuildings.remove(this.result);
				return true;
			} else {//building都不可到达,去除
				//todo:叫附近的警察来帮忙清障
//			Collection<EntityID> toRemove = new HashSet<>(optimalBuildings);
//			this.reset();
//			unsearchedBuildings.removeAll(toRemove);
				for (EntityID id : optimalBuildings){
					if(worldInfo.getEntity(id) instanceof Building){
						Building building = (Building) worldInfo.getEntity(id);
						messageManager.addMessage(new MessageBuilding(false,building));
					}

				}

				return false;
			}
		}
		return false;
	}

	//次级目标:获取声音范围内的building加入列表
	private boolean calcSecondaryTarget(){
		//this.result = null;
		this.pathPlanning.setFrom(this.agentInfo.getPosition());
		this.pathPlanning.setDestination(secondaryBuildings);
		if(!secondaryBuildings.isEmpty()){
			List<EntityID> path = this.pathPlanning.calc().getResult();
			if (path != null && path.size() > 0) {
				if (CSUConstants.DEBUG_AT_SEARCH) System.out.println("找到次优");
				this.result = path.get(path.size() - 1);//获取终点
				currentTargetPriority = 2;
				knownCivilians.removeAll(this.worldInfo.getBuriedHumans(result));
				heardCivilians.removeAll(this.worldInfo.getBuriedHumans(result));
				//searchedBuildings.add(this.result);
				unsearchedBuildings.remove(this.result);
				secondaryBuildings.remove(this.result);
				optimalBuildings.remove(this.result);
				return true;
			} else {//剩下的building都不可到达,重置
				//todo: 通知警察
//				Collection<EntityID> toRemove = new HashSet<>(secondaryBuildings);
//				this.reset();
//				unsearchedBuildings.removeAll(toRemove);
//				return false;
				for (EntityID id : optimalBuildings){
					if(worldInfo.getEntity(id) instanceof Building){
						Building building = (Building) worldInfo.getEntity(id);
						messageManager.addMessage(new MessageBuilding(false,building));
					}

				}
			}
		}
		return false;
	}

	//随便找个没访问过的作为目标 todo 还可以将building按距离排序(快速排序)
	private boolean calcUnsearchedTarget(){
		//this.result = null;
		this.pathPlanning.setFrom(this.agentInfo.getPosition());
		this.pathPlanning.setDestination(this.unsearchedBuildings);
		if(!unsearchedBuildings.isEmpty()){
			List<EntityID> path = this.pathPlanning.calc().getResult();
			if (path != null && path.size() > 0) {
				if (CSUConstants.DEBUG_AT_SEARCH) System.out.println("找到普通");
				this.result = path.get(path.size() - 1);//获取终点
				currentTargetPriority = 1;
				knownCivilians.removeAll(this.worldInfo.getBuriedHumans(result));
				heardCivilians.removeAll(this.worldInfo.getBuriedHumans(result));
				//searchedBuildings.add(this.result);
				unsearchedBuildings.remove(this.result);
				secondaryBuildings.remove(this.result);
				optimalBuildings.remove(this.result);
				return true;
			} else {//剩下的building都不可到达,重置
//				Collection<EntityID> toRemove = new HashSet<>(unsearchedBuildings);
//				this.reset();
//				unsearchedBuildings.removeAll(toRemove);
//				return false;
			}
		}
		return false;
	}

	//获取听到的消息
	public Collection<Command> getHeard(){
		return agentInfo.getHeard();
	}

	//取得所有已知的智能体
	private Collection<StandardEntity> getAgentsOfAllTypes() {
		return worldInfo.getEntitiesOfType(
				StandardEntityURN.FIRE_BRIGADE,
				StandardEntityURN.POLICE_FORCE,
				StandardEntityURN.AMBULANCE_TEAM,
				StandardEntityURN.FIRE_STATION,
				StandardEntityURN.POLICE_OFFICE,
				StandardEntityURN.AMBULANCE_CENTRE);
	}

	//删除所有没有broken的建筑（学长代码）
	private void removeUnbrokenBuildings(){
		if (CSUConstants.DEBUG_AT_SEARCH){
			System.out.print("移出没坏的建筑:");
		}
		Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
		for (EntityID entityID : changedEntities) {
			StandardEntity entity = worldInfo.getEntity(entityID);
			if (entity instanceof Building) {
				Building building = (Building) entity;
				if(building.isBrokennessDefined()&&CSUConstants.DEBUG_AT_SEARCH){
					//System.out.println("类型:"+building.getURN()+"面积:"+building.getTotalArea()+"，破损:"+building.getBrokenness()+",groundArea:"+building.getGroundArea());
				}
				if (building.isBrokennessDefined() && building.getBrokenness() == 0) {
					unsearchedBuildings.remove(entityID);
					if(CSUConstants.DEBUG_AT_SEARCH){
						System.out.print(entityID+",");
					}
				}
			}
		}
		if (CSUConstants.DEBUG_AT_SEARCH) System.out.println();
	}

	//todo：思路是照搬mrl的,需要酌情修改
	//更新搜过的建筑（根据和建筑的距离）
	private void updateSearchedBuildings(){
		//如果在楼里，就判断距离，小于观察距离就当作搜过了
		if (worldInfo.getEntity(agentInfo.getPosition()) instanceof Building) {
			Building building = (Building) worldInfo.getEntity(agentInfo.getPosition());
			int distance = Util.getdistance(worldInfo.getLocation(agentInfo.getID()), worldInfo.getLocation(building));
			if (CSUConstants.DEBUG_AT_SEARCH){
				System.out.println("视距:"+scenarioInfo.getPerceptionLosMaxDistance()+"，距离:"+distance);
			}
			if (distance < scenarioInfo.getPerceptionLosMaxDistance()) {
				if(CSUConstants.DEBUG_AT_SEARCH){
					System.out.println("视距:"+scenarioInfo.getPerceptionLosMaxDistance()+",搜了"+building.getID());
				}
				searchedBuildings.add(building.getID());
				if (CSUConstants.DEBUG_AT_SEARCH){
					System.out.println(building.getID()+"已经搜过了");
				}
			}
		}
	}

	//删去搜过的建筑
	private void removeSearchedBuildings(){
		this.updateSearchedBuildings();
		unsearchedBuildings.removeAll(searchedBuildings);
		if(CSUConstants.DEBUG_AT_SEARCH){
			System.out.print("已经搜过的:");
			for (EntityID entityID:searchedBuildings){
				System.out.print(entityID+",");
			}
			System.out.println();
		}

	}

	//删除正在烧的建筑
	private void removeBurningBuildings() {
		if(CSUConstants.DEBUG_AT_SEARCH){
			System.out.print("移出燃烧建筑:");
		}
		Building building;
		Set<EntityID> burnningBuildings = new HashSet<>();
		for (EntityID buildingID : unsearchedBuildings) {
			building = (Building) worldInfo.getEntity(buildingID);
			if (building.isFierynessDefined() && building.getFieryness() > 0 && building.getFieryness() != 4) {
				burnningBuildings.add(buildingID);
				if (CSUConstants.DEBUG_AT_SEARCH){
					System.out.print(buildingID+",");
				}
			}
		}
		if (burnningBuildings.size() > 0) {
			System.out.print("");
		}
		if (CSUConstants.DEBUG_AT_SEARCH) System.out.println();
		unsearchedBuildings.removeAll(burnningBuildings);
	}

	//移出我附近的建筑（逻辑不合理）
	private Area removeMyNeighbour(){
		if (this.positionArea instanceof Building) {//在屋子里，把这建屋子移出待搜索列表
			//unsearchedBuildingIDs.remove(this.positionArea.getID());
		}else {//在门口，邻居移出
			List<EntityID> neighbours = this.positionArea.getNeighbours();
			unsearchedBuildings.removeAll(neighbours);
		}
		return this.positionArea;
	}

	//更新已知平民
	private void updateKnownCivilians(){
		this.knownCivilians.addAll(worldInfo.getEntitiesOfType(StandardEntityURN.CIVILIAN));
	}

	//更新听到的平民
	private void updateHeardCivilians(){
		Collection<Command> heard = this.getHeard();//获取听到的声音
		//knownHeardCivilians.clear();
		//mrl原采用heard.forEach(next -> {});遍历
		if (heard != null) {
			for (Command next : heard){
				//排除其他agent的声音
				if (next instanceof AKSpeak && ((AKSpeak) next).getChannel() == 0 && !next.getAgentID().equals(agentInfo.getID())) {
					AKSpeak speak = (AKSpeak) next;
					Collection<EntityID> knownAgent = Handy.objectsToIDs(getAgentsOfAllTypes());//把entity列表转换城entityID
					if (!knownAgent.contains(speak.getAgentID())) {//如果已知的agent里没有
						Civilian civilian = (Civilian) worldInfo.getEntity(speak.getAgentID());
						if(civilian == null){//说明不知道entity的位置
							heardCivilians.add(speak.getAgentID());
						}
						//knownHeardCivilians.add(speak.getAgentID());
						//knownCivilians.add(speak.getAgentID());
					}
				}
			}
		}
	}

	//更新最优建筑
	private void updateOptimalBuildings(){
		Collection<EntityID> civlianIDs = Handy.objectsToIDs(knownCivilians);
		for (EntityID civID : civlianIDs) {
			if (worldInfo.getEntity(agentInfo.getID()) instanceof Building){
				Building building = (Building) worldInfo.getEntity(civID);
				if(!searchedBuildings.contains(building)){
					optimalBuildings.add(building.getID());
					unsearchedBuildings.remove(building.getID());
				}
			}
		}
	}

	//听力范围的建筑
	private void updateSecondaryBuildings(){
		if(!heardCivilians.isEmpty()){
			for (EntityID entityID: unsearchedBuildings){
				if(getDistanceFrom(entityID) <= voiceRange){
					secondaryBuildings.add(entityID);
					unsearchedBuildings.add(entityID);
				}
			}
		}
	}

	//更新没有搜过的建筑
	private void updateUnsearchedBuildings(){
		/**
		 * 3/7/2020破损为0不搜，neighbor不搜，正在烧不搜。
		 */
		this.removeUnbrokenBuildings();//破损为0的
		//this.removeMyNeighbour();//邻居
		this.removeSearchedBuildings();//搜过的
		this.removeBurningBuildings();//在烧的
		//if(CSUConstants.DEBUG_AT_SEARCH) System.out.println("没重置，待搜索列表为空:"+this.unsearchedBuildings.isEmpty());
		if (this.unsearchedBuildings.isEmpty()) {
			this.reset();
		//	if(CSUConstants.DEBUG_AT_SEARCH) System.out.println("重置，待搜索列表为空:"+this.unsearchedBuildings.isEmpty());
			//this.removeMyNeighbour();
			this.removeUnbrokenBuildings();//破损为0的
			this.removeSearchedBuildings();
			this.removeBurningBuildings();
		}

	}

	//todo 将建筑按距离排序(排序无效，越界错误)
	private void sortBuildings(List<EntityID> buildings,int low,int high){
		int i,j,temp;
		EntityID bj,bi;
		if(low>high){
			return;
		}
		i=low;
		j=high;
		//temp就是基准位
		temp = getDistanceFrom(buildings.get(low));

		while (i<j) {
			//先看右边，依次往左递减
			while (temp <= getDistanceFrom(buildings.get(j)) && i<j) {
				j--;
			}
			//再看左边，依次往右递增
			while (temp >= getDistanceFrom(buildings.get(i))&&i<j) {
				i++;
			}
			//如果满足条件则交换
			if (i<j) {
				bj = buildings.get(j);
				bi = buildings.get(i);
				buildings.remove(bj);
				buildings.remove(bi);
				buildings.add(i,bj);
				buildings.add(j,bi);
			}

		}
		//最后将基准为与i和j相等位置的数字交换
		bj = buildings.get(low);
		bi = buildings.get(i);
		buildings.remove(bj);
		buildings.remove(bi);
		buildings.add(low,bi);
		buildings.add(i,bj);
		//递归调用左半数组
		sortBuildings(buildings, low, j-1);
		//递归调用右半数组
		sortBuildings(buildings, j+1, high);
	}

	//获得智能体距离制定建筑的距离
	private int getDistanceFrom(EntityID id){
		if(worldInfo.getEntity(id) instanceof Building){
			Building building = (Building) worldInfo.getEntity(id);
			return Util.getdistance(worldInfo.getLocation(agentInfo.getID()), worldInfo.getLocation(building));
		}
		return -1;
	}
}