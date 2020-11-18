package CSU_Yunlu_2020.module.complex.at;

import CSU_Yunlu_2020.CSUConstants;
import CSU_Yunlu_2020.debugger.DebugHelper;
import CSU_Yunlu_2020.util.Util;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
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

import java.io.Serializable;
import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;

/**
 * @description: decision maker for ambulanceTeam search target
 * @author: Yiji-Gao
 * @Date: 03/09/2020
 */
/**
 * 所有removeBuilding方法都不适用，因为如果移出了Set，就丢失了他的优先级
 * 代替方法：每次calc之前判断其在不在burningBuilding等里
 */
//todo:存在问题：1、自己受困时不会通知pf。(完成)
// 2、自己受伤时会陷入自己救自己死循环。
// 3、救人人手不够时要通知。
// 4、知道有人但是门口堵住时通知pf （完成）p.s.或许发前可以检测其当前目标是不是消息发送的建筑
// 5、失效建筑如何处理：目前仅移出，不处理。p.s.或许不处理才是最好的方案
// 6、处理收到的消息
// 7、全图搜索策略很捞，需要修改
//8、由于最优建筑不可抢占，以及次优建筑抢占条件苛刻，如果它们的actionmove是null，则有可能永远陷入null。尝试加入逻辑修改（添加新挂起列表）
//当前如果最优目标和次优目标不能到达，通知pf
public class CSUSearchOld extends Search {

	/**
	 * 用于测试
	 */
	private int monitorID = 398802115;//监视对象ID
	private boolean monitorExact = false;//监视单个对象开关
	private boolean monitorAll = false;//监视全体开关
	public EntityID myID = agentInfo.getID();
	/**
	 * 用于测试
	 */

	private PathPlanning pathPlanning;
	private Clustering clustering;

	private int voiceRange;//声音传播距离
	private Collection<StandardEntity> knownCivilians;//所有已知的平民(选为target时就移出)
	//private Set<EntityID> knownHeardCivilians;//已知位置的呼救平民(选为target时就移出)
	private Set<EntityID> heardCivilians;//未知位置的呼救平民
	private Set<EntityID> searchedBuildings;//已经到访过的建筑
	private Set<EntityID> optimalBuildings;//最优访问的建筑(已知有人的建筑)
	private Set<EntityID> secondaryBuildings;//次级优先访问的建筑(听到声音范围的建筑)
	private Set<EntityID> unsearchedBuildings;//其余等待搜索的建筑

	private Set<EntityID> burnningBuildings;//燃烧的建筑

	private int currentTargetPriority;//当前任务优先级(1:普通目标,2:次优目标,3:最优目标,4:全图目标,5:搜索过的目标)
	//private Set<EntityID> grabedBuildings;//被抢占的建筑
	private boolean first = true;//忘记有啥用了，但是不敢删掉
	private Set<EntityID> hangUpBuildings;//挂起池。加入条件:1、普通建筑被抢占。2、普通建筑超时
//	private final int maxTargetTime = 6;//执行一个普通建筑最长的时间,超出时间加入挂起池
//	private int targetTime = 0;//已经执行普通建筑的时长
	private final int maxHangUpTime = 15;//最长挂起时间
	private int hangUpTime = 0;//挂起计时
	private Set<EntityID> hangUpOptimal;//最优建筑超时，挂起
	private Set<EntityID> hangUpSecondary;//次优建筑超时,挂起
//	private int maxOptimalTargetTime = 8;//最优建筑执行的最长时长
//	private int optimalTargetTime = 0;//已经执行最优建筑的时长
	private int nonMoveTime = 0;//原地不动的时长

//	private final int maxPopSearchedTime = 15;
//	private int popSearchedTime = 0;
	//private int roudNum = 0;//回合数
	//private EntityID lastresult;//上一次的目标
	private EntityID lastPosition = null;//上次位置
	private EntityID nowPosition = null;//当前位置
	private MessageManager messageManager;//通讯类
	private Area positionArea;//当前地形
	//----------------------该类的作用就是确定一个最优的的搜索目标存进result------------------------
	//----------------------------------------------------------------------------------------
	private EntityID result;//目标搜索建筑
	//----------------------------------------------------------------------------------------

	public CSUSearchOld(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager,
                        DevelopData developData) {
		super(ai, wi, si, moduleManager, developData);

		this.burnningBuildings = new HashSet<EntityID>();

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
		hangUpSecondary = new HashSet<EntityID>();
		hangUpOptimal = new HashSet<EntityID>();
		this.currentTargetPriority = 0;
//        System.out.println("--------------------");
//        System.out.println(CSUSearchUtil.getBuildingIDs());
//        System.out.println("--------------------");
	}

	//更新已知平民(每回合)
	private void updateKnownCivilians(){
		Set<StandardEntity> civilians = new HashSet<StandardEntity>();
		civilians.addAll(worldInfo.getEntitiesOfType(StandardEntityURN.CIVILIAN));
		for(StandardEntity e : civilians){
			Civilian civ = (Civilian) e;
			if(civ.isHPDefined() && civ.getHP() > 0){
				this.knownCivilians.add(civ);
			}else{
				this.knownCivilians.add(civ);
			}
		}
		if(!civilians.isEmpty()){
			if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
				if(myID.getValue() == monitorID)
				System.out.println("AT"+monitorID+"知道有"+civilians.size()+"人");
			}
			if(CSUConstants.DEBUG_AT_SEARCH && monitorAll) System.out.println("ID:"+myID+"知道有"+civilians.size()+"人");
		}
		//this.knownCivilians.addAll(worldInfo.getEntitiesOfType(StandardEntityURN.CIVILIAN));//原策略
	}

	//更新听到的平民(每回合)
	private void updateHeardCivilians(){
		Collection<Command> heard = this.getHeard();//获取听到的声音
		//knownHeardCivilians.clear();
		if (heard != null) {
			if(CSUConstants.DEBUG_AT_SEARCH && monitorAll && monitorAll) System.out.println("ID:"+myID+",听到了呼救");
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
					}
				}
			}
		}
	}

	//更新搜过的建筑(每回合)(如果在楼里，就判断距离，小于观察距离就当作搜过了)
	private void updateSearchedBuildings(){
		if (worldInfo.getEntity(agentInfo.getPosition()) instanceof Building) {
			Building building = (Building) worldInfo.getEntity(agentInfo.getPosition());
			int distance = Util.getdistance(worldInfo.getLocation(agentInfo.getID()), worldInfo.getLocation(building));
			if (distance < scenarioInfo.getPerceptionLosMaxDistance()) {
				if(CSUConstants.DEBUG_AT_SEARCH && monitorAll && monitorAll){
					System.out.println("视距:"+scenarioInfo.getPerceptionLosMaxDistance()+"，距离:"+distance+",搜了"+building.getID());
				}
//				messageManager.addMessage(new CommandAmbulance(true ,null, agentInfo.getPosition()));
				searchedBuildings.add(building.getID());
			}
		}
	}

	//更新最优建筑(每回合)(根据knownCivilians)  ok
	private void updateOptimalBuildings(){
		Collection<EntityID> civlianIDs = Handy.objectsToIDs(knownCivilians);
		for (EntityID civID : civlianIDs) {
			if (worldInfo.getPosition((Human) worldInfo.getEntity(civID)).getStandardURN() == BUILDING){
				Building building = (Building) worldInfo.getPosition((Human) worldInfo.getEntity(civID));
				if(!searchedBuildings.contains(building) && !optimalBuildings.contains(building) && !hangUpOptimal.contains(building)){
					optimalBuildings.add(building.getID());
					unsearchedBuildings.remove(building.getID());
				}
			}
		}
		if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
			if(myID.getValue() == monitorID) System.out.println("AT"+myID+"知道有"+optimalBuildings.size()+"栋建筑里有人");
		}
		this.removeUnbrokenBuildings(optimalBuildings);
		this.removeBurningBuildings(optimalBuildings);
		this.removeSearchedBuildings(optimalBuildings);
	}

	//更新次优建筑(每回合)(根据heardCivilians)  ok
	private void updateSecondaryBuildings(){
		if(!heardCivilians.isEmpty()){
			for (EntityID entityID: unsearchedBuildings){
				if(getDistanceFrom(entityID) <= voiceRange && !secondaryBuildings.contains(entityID) && !unsearchedBuildings.contains(entityID)){
					secondaryBuildings.add(entityID);
				}
			}
		}

		this.removeUnbrokenBuildings(secondaryBuildings);
		this.removeBurningBuildings(secondaryBuildings);
		this.removeSearchedBuildings(secondaryBuildings);
	}

	//更新普通建筑(其他没有搜过的建筑)(每回合)
	private void updateUnsearchedBuildings(){
		/**
		 * 3/7/2020破损为0不搜，neighbor不搜，正在烧不搜。
		 */
		this.removeUnbrokenBuildings(unsearchedBuildings);//破损为0的
		//this.removeMyNeighbour();//邻居
		this.removeSearchedBuildings(unsearchedBuildings);//搜过的
		this.removeBurningBuildings(unsearchedBuildings);//在烧的

		this.unsearchedBuildings.removeAll(optimalBuildings);
		this.unsearchedBuildings.removeAll(secondaryBuildings);
		//if(CSUConstants.DEBUG_AT_SEARCH && monitorAll) System.out.println("没重置，待搜索列表为空:"+this.unsearchedBuildings.isEmpty());
		if (this.unsearchedBuildings.isEmpty()) {
			this.reset();
			//	if(CSUConstants.DEBUG_AT_SEARCH && monitorAll) System.out.println("重置，待搜索列表为空:"+this.unsearchedBuildings.isEmpty());
			//this.removeMyNeighbour();
			this.removeUnbrokenBuildings(unsearchedBuildings);//破损为0的
			this.removeSearchedBuildings(unsearchedBuildings);
			this.removeBurningBuildings(unsearchedBuildings);

			this.unsearchedBuildings.removeAll(optimalBuildings);
			this.unsearchedBuildings.removeAll(secondaryBuildings);
		}

	}

	@Override
	public Search updateInfo(MessageManager messageManager) {
		super.updateInfo(messageManager);
		if (this.getCountUpdateInfo() >= 2) {
			return this;
		}

		clustering.updateInfo(messageManager);
		pathPlanning.updateInfo(messageManager);

		lastPosition = nowPosition;
		nowPosition  = agentInfo.getPosition();

		this.updateKnownCivilians();
		this.updateHeardCivilians();

		this.updateSearchedBuildings();
		this.updateUnsearchedBuildings();
		this.updateSecondaryBuildings();
		this.updateOptimalBuildings();
		this.messageManager = messageManager;

		messageManager.getReceivedMessageList();
//		int clusterIndex = this.clustering.getClusterIndex(this.agentInfo.getID());
//		Collection<StandardEntity> clusterEntities = this.clustering.getClusterEntities(clusterIndex);
//		if (clusterEntities != null && clusterEntities.size() > 0) {
//			System.out.println();
//			System.out.println("聚类ID:"+clusterIndex);
//			for (StandardEntity entity : clusterEntities) {
//				if (entity instanceof Building && entity.getStandardURN() != REFUGE) {
//					this.unsearchedBuildings.add(entity.getID());
//					System.out.print(entity.getID()+" ");
//				}
//			}
//		}

		return this;
	}

	@Override
	public Search calc() {
		if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
			if(myID.getValue() == monitorID)
				System.out.println("第"+this.agentInfo.getTime()+"回合");
		}
		//开局前三秒不能行动，生成地形
		if (agentInfo.getTime() < scenarioInfo.getKernelAgentsIgnoreuntil()) {
			return this;
		}
		if(CSUConstants.DEBUG_AT_SEARCH && monitorAll ){
			if(agentInfo.getID().getValue() == 1297269710){
				System.out.println("agent1297269710执行了calc()");
			}
		}

		//定期释放挂起的普通建筑
		periodicalReleaseHangUp();

		//防止多个at选择同一个建筑
		if(avoidRedundant()) return this;

		EntityID lastTarget = this.result;

		//如果目标失效,重新选定
		if(!isLastTargetValid()){
			if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
				if(myID.getValue() == monitorID) System.out.println("AT"+myID+":当前目标失效("+this.result.getValue()+")");
			}

			if (calcOptimalTarget()) {
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为当前目标失效切换到最优建筑("+this.result.getValue()+")");
				}
				if (CSUConstants.DEBUG_AT_SEARCH && monitorAll && monitorAll) {
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("无目标换为最优目标:" + building.getID());
				}
			} else if (calcSecondaryTarget()) {
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为当前目标失效切换到次优建筑("+this.result.getValue()+")");
				}
				if (CSUConstants.DEBUG_AT_SEARCH && monitorAll && monitorAll) {
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("agent" + agentInfo.getID() + ":无目标换为次优目标:" + building.getID());
				}
			} else if (calcUnsearchedTarget()) {
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为当前目标失效切换到普通建筑("+this.result.getValue()+")");
				}
				if (CSUConstants.DEBUG_AT_SEARCH && monitorAll && monitorAll) {
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("agent" + agentInfo.getID() + ":无目标换为普通目标:" + building.getID());
				}
			} else if (calcWorldTarget()) {
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为当前目标失效切换到全图搜索("+this.result.getValue()+")");
				}
				if (CSUConstants.DEBUG_AT_SEARCH && monitorAll && monitorAll) {
					System.out.println("agent" + agentInfo.getID() + ":无目标换为移动寻找下一个聚类");
				}
			} else if (calcSearchedTarget()) {
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为当前目标失效切换到重复搜索("+this.result.getValue()+")");
				}
				if (CSUConstants.DEBUG_AT_SEARCH && monitorAll && monitorAll) {
					System.out.println("agent" + agentInfo.getID() + ":无目标换为搜搜过的");
				}
			} else {
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact) {
					if(myID.getValue() == monitorID)
						System.out.println("AT"+myID+",目标失效而且找不到目标");
				}
				if (CSUConstants.DEBUG_AT_SEARCH && monitorAll) {
					System.out.println("agent" + agentInfo.getID() + ":无论如何都找不到");
				}
			}
			return this;
		}

		//如果好久没移动
		if(nonMove()){
			nonMoveTime++;
			if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
				if(myID.getValue() == monitorID){
					System.out.println("AT"+myID+",当前没动");
				}
			}
			if(nonMoveTime >=1){
				if(currentTargetPriority == 1){
					if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
						if(myID.getValue() == monitorID){
							System.out.println("AT"+myID+",当前目标为重复搜索,因为原地不动所以更换目标");
						}
					}
				}
				if(currentTargetPriority == 2){
					if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
						if(myID.getValue() == monitorID){
							System.out.println("AT"+myID+",当前目标为全图搜索,因为原地不动所以更换目标");
						}
					}
				}
				if(currentTargetPriority == 3){
					if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
						if(myID.getValue() == monitorID){
							System.out.println("AT"+myID+",当前目标为普通建筑,因为原地不动所以更换目标");
						}
					}
					hangUpBuildings.add(this.result);
					unsearchedBuildings.remove(this.result);
					this.result = null;
				}
				if(currentTargetPriority == 4){
					if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
						if(myID.getValue() == monitorID){
							System.out.println("AT"+myID+",当前目标为次优建筑,因为原地不动所以更换目标");
						}
					}
					EntityID nearestPF = findClosestPF();
					//如果最优建筑堵住了，跟最近的警察通信;没有最近的警察，就都发
					if(nearestPF != null){
						messageManager.addMessage(new CommandPolice(
								true,
								nearestPF,
								this.result,
								CommandPolice.ACTION_CLEAR));
					}else{
						messageManager.addMessage(new CommandPolice(
								true,
								null,
								this.result,
								CommandPolice.ACTION_CLEAR));
					}
					hangUpSecondary.add(this.result);
					secondaryBuildings.remove(this.result);
					this.result = null;
				}
				if(currentTargetPriority == 5){
					if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
						if(myID.getValue() == monitorID){
							System.out.println("AT"+myID+",当前目标为最优建筑,因为原地不动所以更换目标");
						}
					}
					EntityID nearestPF = findClosestPF();
					//如果最优建筑堵住了，跟最近的警察通信;没有最近的警察，就都发
					if(nearestPF != null){
						messageManager.addMessage(new CommandPolice(
								true,
								nearestPF,
								this.result,
								CommandPolice.ACTION_CLEAR));
					}else{
						messageManager.addMessage(new CommandPolice(
								true,
								null,
								this.result,
								CommandPolice.ACTION_CLEAR));
					}
					hangUpOptimal.add(this.result);
					optimalBuildings.remove(this.result);
					this.result = null;
				}
				nonMoveTime = 0;
			}

		}

		//如果已经搜过了
		if(searchedBuildings.contains(this.result)){
			//搜过的移出去
			if(currentTargetPriority == 5) optimalBuildings.remove(lastTarget);
			if(currentTargetPriority == 4) secondaryBuildings.remove(lastTarget);
			if(currentTargetPriority == 3) unsearchedBuildings.remove(lastTarget);
			if(currentTargetPriority == 2){
				if(CSUConstants.DEBUG_AT_SEARCH && monitorAll) System.out.println("已经随机移动到另一建筑");
			}
			if(currentTargetPriority == 1){
				if(CSUConstants.DEBUG_AT_SEARCH && monitorAll && monitorAll) System.out.println("搜搜过的");
			}
			this.result = null;
		}

		//如果没有正在执行的目标
		if(this.result == null) {
			if (calcOptimalTarget()) {
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为没有目标切换到最优建筑("+this.result.getValue()+")");
				}
				if (CSUConstants.DEBUG_AT_SEARCH && monitorAll && monitorAll) {
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("无目标换为最优目标:" + building.getID());
				}
			} else if (calcSecondaryTarget()) {
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为没有目标切换到次优筑("+this.result.getValue()+")");
				}
				if (CSUConstants.DEBUG_AT_SEARCH && monitorAll && monitorAll) {
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("agent" + agentInfo.getID() + ":无目标换为次优目标:" + building.getID());
				}
			} else if (calcUnsearchedTarget()) {
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为没有目标切换到普通建筑("+this.result.getValue()+")");
				}
				if (CSUConstants.DEBUG_AT_SEARCH && monitorAll && monitorAll) {
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("agent" + agentInfo.getID() + ":无目标换为普通目标:" + building.getID());
				}
			} else if (calcWorldTarget()) {
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为没有目标切换到全图搜索("+this.result.getValue()+")");
				}
				if (CSUConstants.DEBUG_AT_SEARCH && monitorAll && monitorAll) {
					System.out.println("agent" + agentInfo.getID() + ":无目标换为移动寻找下一个聚类");
				}
			} else if (calcSearchedTarget()) {
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为没有目标切换到重复搜索("+this.result.getValue()+")");
				}
				if (CSUConstants.DEBUG_AT_SEARCH && monitorAll) {
					System.out.println("agent" + agentInfo.getID() + ":无目标换为搜搜过的");
				}
			} else {
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID)
						System.out.println("AT"+myID+",没有目标而且找不到目标");
				}
				if (CSUConstants.DEBUG_AT_SEARCH && monitorAll) {
					System.out.println("agent" + agentInfo.getID() + ":无论如何都找不到");
				}
			}
			return this;
		}

		//如果正在重复搜索
		if(currentTargetPriority == 1){
			if(calcOptimalTarget()){
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为抢占重复搜索切换到最优建筑("+this.result.getValue()+")");
				}
				if(CSUConstants.DEBUG_AT_SEARCH && monitorAll){
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("访问过的目标换为最优目标:"+building.getID());
				}
			}else if(calcSecondaryTarget()){
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为抢占重复搜索切换到次优建筑("+this.result.getValue()+")");
				}
				if(CSUConstants.DEBUG_AT_SEARCH && monitorAll){
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("无目标换为次优目标:"+building.getID());
				}
			}else if(calcUnsearchedTarget()){
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为抢占重复搜索切换到普通建筑("+this.result.getValue()+")");
				}
				if (CSUConstants.DEBUG_AT_SEARCH && monitorAll){
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("访问过的目标换为普通目标:"+building.getID());
				}
			}else if(calcWorldTarget()){
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为抢占重复搜索切换到全图搜索("+this.result.getValue()+")");
				}
				if(CSUConstants.DEBUG_AT_SEARCH && monitorAll){
					System.out.println("agent"+agentInfo.getID()+":移动寻找下一个聚类");
				}
			}else{
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact) {
					if(myID.getValue() == monitorID)
						System.out.println("AT"+myID+",重复目标抢占失败");
				}
				if(CSUConstants.DEBUG_AT_SEARCH && monitorAll){
					System.out.println("agent"+agentInfo.getID()+":预想中不可能的情况:全世界都搜遍了");
				}
			}
			return this;
		}
		//如果正在全图搜索
		if(currentTargetPriority == 2){
			if(calcOptimalTarget()){
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为抢占全图搜索切换到最优建筑("+this.result.getValue()+")");
				}
				if(CSUConstants.DEBUG_AT_SEARCH && monitorAll){
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("agent"+agentInfo.getID()+":访问过的目标换为最优目标:"+building.getID());
				}
			}else if(calcSecondaryTarget()){
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为抢占全图搜索切换到次优建筑("+this.result.getValue()+")");
				}
				if(CSUConstants.DEBUG_AT_SEARCH && monitorAll){
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("agent"+agentInfo.getID()+":无目标换为次优目标:"+building.getID());
				}
			}else if(calcUnsearchedTarget()){
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为抢占全图搜索切换到普通建筑("+this.result.getValue()+")");
				}
				if (CSUConstants.DEBUG_AT_SEARCH && monitorAll){
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("agent"+agentInfo.getID()+":访问过的目标换为普通目标:"+building.getID());
				}
			}else{
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact) {
					if(myID.getValue() == monitorID)
						System.out.println("ID:"+myID+"全图搜索抢占失败");
				}
				if(CSUConstants.DEBUG_AT_SEARCH && monitorAll){
					System.out.println("agent"+agentInfo.getID()+"还在原先的聚类");
				}
			}
			return this;
		}
		//如果正在执行普通建筑
		if(currentTargetPriority == 3){
			if(calcOptimalTarget()){
				//如果上次目标还没完成，压入普通建筑
				if(!searchedBuildings.contains(lastTarget)) unsearchedBuildings.add(lastTarget);
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为抢占普通建筑切换到最优建筑("+this.result.getValue()+")");
				}
				if(CSUConstants.DEBUG_AT_SEARCH && monitorAll){
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("agent"+agentInfo.getID()+":普通目标换为最优目标:"+building.getID());
				}
			}else if(calcSecondaryTarget()){
				//如果上次目标还没完成，挂起
				if(!searchedBuildings.contains(lastTarget)) hangUpBuildings.add(lastTarget);
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为抢占普通建筑切换到次优建筑("+this.result.getValue()+")");
				}
				if(CSUConstants.DEBUG_AT_SEARCH && monitorAll){
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("agent"+agentInfo.getID()+":普通目标换为次优目标:"+building.getID());
				}
			}else {
//				targetTime++;//普通建筑已执行时长
//				if(targetTime >= maxTargetTime || searchedBuildings.contains(lastTarget)){
//					if(CSUConstants.DEBUG_AT_SEARCH && monitorAll){
//						if(targetTime >= maxTargetTime) System.out.println("agent"+agentInfo.getID()+":普通目标超时");
//						if(searchedBuildings.contains(lastTarget)) System.out.println("agent"+agentInfo.getID()+"目标建筑已经搜过");
//					}
//					unsearchedBuildings.remove(lastTarget);//防止下个普通目标重复
//					hangUpBuildings.add(lastTarget);
//					if(calcUnsearchedTarget()){
//						if(!searchedBuildings.contains(lastTarget)) hangUpBuildings.add(lastTarget);
//						if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
//							if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为普通建筑超时切换到普通建筑("+this.result.getValue()+")");
//						}
//						if (CSUConstants.DEBUG_AT_SEARCH && monitorAll) System.out.println("agent"+agentInfo.getID()+":更换普通目标"+result);
//					}else if(calcWorldTarget()){
//						if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
//							if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为普通建筑超时切换到全图搜索("+this.result.getValue()+")");
//						}
//						if (CSUConstants.DEBUG_AT_SEARCH && monitorAll) System.out.println("agent"+agentInfo.getID()+":普通目标换为移动寻找下一个聚类"+this.result);
//					} else if(!calcSearchedTarget()){
//						if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
//							if(myID.getValue() == monitorID) System.out.println("AT"+myID+":困住了("+this.result.getValue()+")");
//						}
//						if(CSUConstants.DEBUG_AT_SEARCH && monitorAll) System.out.println("agent"+agentInfo.getID()+":自己被困住了(普通目标)");
//					}
//					targetTime = 0;
//				}
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID)
						System.out.println("AT"+myID.getValue()+",抢占普通目标失败");
				}
			}
			return this;
		}
		//如果正在执行次级建筑
		if(currentTargetPriority == 4){
			if(calcOptimalTarget()){
				if(!searchedBuildings.contains(lastTarget)) secondaryBuildings.add(lastTarget);
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
					if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为抢占次优建筑切换到最优建筑("+this.result.getValue()+")");
				}
				if(CSUConstants.DEBUG_AT_SEARCH && monitorAll){
					Building building = (Building) worldInfo.getEntity(result);
					System.out.println("agent"+agentInfo.getID()+":次优目标换为最优目标:"+building.getID());
				}
				//如果搜过了这栋
			}else {
				if(CSUConstants.DEBUG_AT_SEARCH && monitorExact) {
					if(myID.getValue() == monitorID)
						System.out.println("ID:"+myID+"次级目标抢占失败");
				}
			}
			return this;
		}
		//如果正在执行最优建筑
		if(currentTargetPriority == 5){
			if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
				if(myID.getValue() == monitorID) System.out.println("AT"+myID+":正在执行最优建筑("+this.result.getValue()+")");
			}
			return this;
		}

		visualDebug();
		return this;
	}

	private void visualDebug() {
		if (DebugHelper.DEBUG_MODE) {
			try {
				DebugHelper.drawSearchTarget(worldInfo, agentInfo.getID(), result);
				List<Integer> elementList = Util.fetchIdValueFromElementIds(unsearchedBuildings);
				DebugHelper.VD_CLIENT.drawAsync(agentInfo.getID().getValue(), "UnsearchedBuildings", (Serializable) elementList);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
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


	//重新获取unsearchedBuildingIDs（同一聚类的building） ok
	private void reset(){
		if(CSUConstants.DEBUG_AT_SEARCH){
//			System.out.println("unsearched为空，重新获取.");
		}
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
		}
//		else {
//			this.unsearchedBuildings.addAll(this.worldInfo.getEntityIDsOfType(BUILDING, GAS_STATION, AMBULANCE_CENTRE,
//					FIRE_STATION, POLICE_OFFICE));
//		}
	}

	//最优目标:已知平民所在的building(权值 = 5)
	private boolean calcOptimalTarget(){
		//this.result = null;
		this.pathPlanning.setFrom(this.agentInfo.getPosition());
		this.pathPlanning.setDestination(optimalBuildings);
		if(!optimalBuildings.isEmpty()){
			List<EntityID> path = this.pathPlanning.calc().getResult();
			if (path != null && path.size() > 0) {
				//if (CSUConstants.DEBUG_AT_SEARCH) System.out.println("找到最优");
				this.result = path.get(path.size() - 1);//获取终点
				if(CSUConstants.DEBUG_AT_SEARCH){
					if(this.result == null) System.out.println("agent"+agentInfo.getID()+"由于未知错误,最优建筑返回null");
				}
				currentTargetPriority = 5;
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

	//次级目标:获取声音范围内的building加入列表(权值 = 4)
	private boolean calcSecondaryTarget(){
		this.pathPlanning.setFrom(this.agentInfo.getPosition());
		this.pathPlanning.setDestination(secondaryBuildings);
		if(!secondaryBuildings.isEmpty()){
			List<EntityID> path = this.pathPlanning.calc().getResult();
			if (path != null && path.size() > 0) {
				//if (CSUConstants.DEBUG_AT_SEARCH) System.out.println("找到次优");
				this.result = path.get(path.size() - 1);//获取终点
				if(CSUConstants.DEBUG_AT_SEARCH){
					if(this.result == null) System.out.println("agent"+agentInfo.getID()+"由于未知错误,次优建筑返回null");
				}

				currentTargetPriority = 4;
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

	//随便找个没访问过的作为目标(权值 = 3)
	private boolean calcUnsearchedTarget(){
		//this.result = null;
		this.pathPlanning.setFrom(this.agentInfo.getPosition());
		this.pathPlanning.setDestination(this.unsearchedBuildings);
		if(!unsearchedBuildings.isEmpty()){
			List<EntityID> path = this.pathPlanning.calc().getResult();
			if (path != null && path.size() > 0) {
				//if (CSUConstants.DEBUG_AT_SEARCH) System.out.println("找到普通");
				this.result = path.get(path.size() - 1);//获取终点
				if(CSUConstants.DEBUG_AT_SEARCH){
					if(this.result == null) System.out.println("agent"+agentInfo.getID()+"由于未知错误,普通建筑返回null");
				}
				currentTargetPriority = 3;
				knownCivilians.removeAll(this.worldInfo.getBuriedHumans(result));
				heardCivilians.removeAll(this.worldInfo.getBuriedHumans(result));
				//searchedBuildings.add(this.result);
				unsearchedBuildings.remove(this.result);
				secondaryBuildings.remove(this.result);
				optimalBuildings.remove(this.result);
				return true;
			} else {//剩下的building都不可到达,重置
				Collection<EntityID> toRemove = new HashSet<>(unsearchedBuildings);
				this.reset();
				unsearchedBuildings.removeAll(toRemove);
				return false;
			}
		}
		return false;
	}

	//全图里随便选一个没搜过的(权值 = 2)
	private boolean calcWorldTarget(){
		Set<EntityID> buildings = new HashSet<EntityID>(this.worldInfo.getEntityIDsOfType(BUILDING));
		removeBurningBuildings(buildings);
		removeUnbrokenBuildings(buildings);
		removeSearchedBuildings(buildings);
		this.pathPlanning.setFrom(this.agentInfo.getPosition());
		this.pathPlanning.setDestination(buildings);
		if(!buildings.isEmpty()){
			List<EntityID> path = this.pathPlanning.calc().getResult();
			if (path != null && path.size() > 0) {
				if (CSUConstants.DEBUG_AT_SEARCH && monitorAll) System.out.println("agent"+agentInfo.getID()+":搜完了当前cluster,去世界其他地方搜");
				this.result = path.get(path.size() - 1);//获取终点
				if(CSUConstants.DEBUG_AT_SEARCH){
					if(this.result == null) System.out.println("agent"+agentInfo.getID()+"由于未知错误,全图随机建筑返回null");
				}
				currentTargetPriority = 2;
				return true;
			}
		}else{
			if(CSUConstants.DEBUG_AT_SEARCH){
				System.out.println("啥都搜过了?????????????????????(全图)");
			}
		}
		return false;
	}

	//随便找个访问过的作为目标 主要是为了防止at不动(权值 = 1)
	private boolean calcSearchedTarget(){
		EntityID lastTarget = this.result;
		searchedBuildings.remove(this.result);
		this.pathPlanning.setFrom(this.agentInfo.getPosition());
		this.pathPlanning.setDestination(this.searchedBuildings);
		if(!searchedBuildings.isEmpty()){
			List<EntityID> path = this.pathPlanning.calc().getResult();
			if (path != null && path.size() > 0) {
				//if (CSUConstants.DEBUG_AT_SEARCH) System.out.println("去搜搜过的");
				this.result = path.get(path.size() - 1);//获取终点
				if(CSUConstants.DEBUG_AT_SEARCH){
					if(this.result == null) System.out.println("agent"+agentInfo.getID()+"由于未知错误,搜过的建筑返回null");
				}
				currentTargetPriority = 1;
				//knownCivilians.removeAll(this.worldInfo.getBuriedHumans(result));
				//heardCivilians.removeAll(this.worldInfo.getBuriedHumans(result));
				searchedBuildings.add(lastTarget);
				//unsearchedBuildings.remove(this.result);
				//secondaryBuildings.remove(this.result);
				//optimalBuildings.remove(this.result);
				return true;
			} else {//剩下的building都不可到达,重置
//				Collection<EntityID> toRemove = new HashSet<>(unsearchedBuildings);
//				this.reset();
//				unsearchedBuildings.removeAll(toRemove);
//				return false;
			}
		}
		secondaryBuildings.add(lastTarget);
		return false;
	}

	//获取听到的消息    ok
	public Collection<Command> getHeard(){
		return agentInfo.getHeard();
	}

	//取得所有已知的智能体(用于筛选civilian)  ok
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
	private void removeUnbrokenBuildings(Set<EntityID> set){
		if (CSUConstants.DEBUG_AT_SEARCH && monitorAll){
			//System.out.print("移出没坏的建筑:");
		}
		Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
		for (EntityID entityID : changedEntities) {
			StandardEntity entity = worldInfo.getEntity(entityID);
			if (entity instanceof Building) {
				Building building = (Building) entity;
				if(building.isBrokennessDefined()&&CSUConstants.DEBUG_AT_SEARCH && monitorAll){
					//System.out.println("类型:"+building.getURN()+"面积:"+building.getTotalArea()+"，破损:"+building.getBrokenness()+",groundArea:"+building.getGroundArea());
				}
				if (building.isBrokennessDefined() && building.getBrokenness() == 0) {
					set.remove(entityID);
					if(CSUConstants.DEBUG_AT_SEARCH && monitorAll){
						//System.out.print(entityID+",");
					}
				}
			}
		}
	}

	//更新搜过的建筑（根据和建筑的距离）
	//删去搜过的建筑
	private void removeSearchedBuildings(Set<EntityID> set){
		set.removeAll(searchedBuildings);
		if(CSUConstants.DEBUG_AT_SEARCH && monitorAll){
			//System.out.print("agentID"+agentInfo.getID()+",已经搜过的:");
			for (EntityID entityID:searchedBuildings){
				//System.out.print(entityID+",");
			}
			//System.out.println();
		}

	}

	//删除正在烧的建筑
	private void removeBurningBuildings(Set<EntityID> set) {
		if(CSUConstants.DEBUG_AT_SEARCH && monitorAll){
			//System.out.print("移出燃烧建筑:");
		}

		//Set<EntityID> burnningBuildings = new HashSet<>();
		for (EntityID entityID : set) {
			StandardEntity entity = worldInfo.getEntity(entityID);
			if (entity instanceof Building) {
				Building building = (Building) entity;
				if (building.isFierynessDefined() && building.getFieryness() > 0 && building.getFieryness() != 4) {
					burnningBuildings.add(entityID);
					if (CSUConstants.DEBUG_AT_SEARCH && monitorAll){
						//System.out.print(buildingID+",");
					}
				}
			}
		}
		//if (CSUConstants.DEBUG_AT_SEARCH && monitorAll) System.out.println();
		set.removeAll(burnningBuildings);
	}

	//获得智能体距离指定建筑的距离
	private int getDistanceFrom(EntityID id){
		if(worldInfo.getEntity(id) instanceof Building){
			Building building = (Building) worldInfo.getEntity(id);
			return Util.getdistance(worldInfo.getLocation(agentInfo.getID()), worldInfo.getLocation(building));
		}
		return -1;
	}

	//定时释放挂起的普通目标
	private void periodicalReleaseHangUp(){
		hangUpTime++;
		if(hangUpTime >= maxHangUpTime){
			hangUpTime = 0;
			Iterator it = hangUpBuildings.iterator();
			if(it.hasNext()){
				EntityID i = (EntityID) it.next();
				unsearchedBuildings.add(i);
				hangUpBuildings.remove(i);
			}
			it = hangUpOptimal.iterator();
			if(it.hasNext()){
				EntityID i = (EntityID) it.next();
				optimalBuildings.add(i);
				hangUpBuildings.remove(i);
			}
			it = hangUpSecondary.iterator();
			if(it.hasNext()){
				EntityID i = (EntityID) it.next();
				secondaryBuildings.add(i);
				hangUpSecondary.remove(i);
			}
		}

	}

	//防止过多的人以同一建筑为目标
	private boolean avoidRedundant(){
		if(this.result != null){
			Entity entity = this.worldInfo.getEntity(this.result);
			if(entity instanceof Building){
				Building building = (Building)entity;
				int buriedHumanNum = worldInfo.getBuriedHumans(building).size();
				int rescuingATNum = 0;
				//数at的数量
				Set<Entity> entities = new HashSet<Entity>(worldInfo.getEntitiesOfType(AMBULANCE_TEAM));
				for(Entity e : entities){
					AmbulanceTeam at = (AmbulanceTeam)e;
					if(worldInfo.getEntity(at.getPosition()) == worldInfo.getEntity(this.result)) rescuingATNum++;
				}
				//如果其他at已经够救建筑里的人，此at更换目标
				if(rescuingATNum >= buriedHumanNum && buriedHumanNum != 0){
					if(CSUConstants.DEBUG_AT_SEARCH && monitorExact) {
						if(myID.getValue() == monitorID) System.out.println("AT"+myID+":目标建筑("+this.result.getValue()+")里的人手够了");
					}
					EntityID lastTarget = this.result;
					//防止又选到相同的
					if(currentTargetPriority == 5) optimalBuildings.remove(lastTarget);
					else if(currentTargetPriority == 4) secondaryBuildings.remove(lastTarget);
					else if(currentTargetPriority == 3) unsearchedBuildings.remove(lastTarget);
					else if(currentTargetPriority == 2){
						if(CSUConstants.DEBUG_AT_SEARCH && monitorAll) System.out.println("已经随机移动到另一建筑");
					}else if(currentTargetPriority == 1){
						if(CSUConstants.DEBUG_AT_SEARCH && monitorAll) System.out.println("搜搜过的");
					}
					//重新计算目标
					if(calcOptimalTarget()){
						if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
							if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为人手足够切换最优建筑("+this.result.getValue()+")");
						}
						if(CSUConstants.DEBUG_AT_SEARCH && monitorAll){
							building = (Building) worldInfo.getEntity(result);
							System.out.println("重复目标换为最优目标:"+building.getID());
						}
					}else if(calcSecondaryTarget()){
						if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
							if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为人手足够切换次优建筑("+this.result.getValue()+")");
						}
						if(CSUConstants.DEBUG_AT_SEARCH && monitorAll){
							building = (Building) worldInfo.getEntity(result);
							System.out.println("重复目标换为次优目标:"+building.getID());
						}
					}else if(calcUnsearchedTarget()){
						if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
							if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为人手足够切换普通建筑("+this.result.getValue()+")");
						}
						if (CSUConstants.DEBUG_AT_SEARCH && monitorAll){
							building = (Building) worldInfo.getEntity(result);
							//unsearchedBuildings.add(lastTarget);
							System.out.println("重复目标换为普通目标:"+building.getID());
						}
					}else if(calcWorldTarget()){
						if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
							if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为人手足够切换全图搜索("+this.result.getValue()+")");
						}
						if(CSUConstants.DEBUG_AT_SEARCH && monitorAll){
							System.out.println("重复目标换为移动寻找下一个聚类");
						}
					}
					else if(calcSearchedTarget()) {
						if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
							if(myID.getValue() == monitorID) System.out.println("AT"+myID+":因为人手足够切换重复搜索("+this.result.getValue()+")");
						}
					}
//                if(currentTargetPriority == 3) optimalBuildings.add(lastTarget);
//                else if(currentTargetPriority == 2) secondaryBuildings.add(lastTarget);
//                else if(currentTargetPriority == 1) unsearchedBuildings.add(lastTarget);
					return true;
				}
			}
		}
		return false;
	}

	//如果当前目标建筑着火了，或者里面的人死完了
	private boolean isLastTargetValid(){
		if(burnningBuildings.contains(result)) {
			searchedBuildings.remove(this.result);
			if(currentTargetPriority == 3) unsearchedBuildings.add(this.result);
			if(currentTargetPriority == 4) secondaryBuildings.add(this.result);
			if(currentTargetPriority == 5) optimalBuildings.add(this.result);
			return false;
		}
		Entity e = this.worldInfo.getEntity(this.result);
		if(e instanceof Building){
			Building building = (Building) e;
			int deadNum = 0;
			if(this.result != null){
				for(Human human : worldInfo.getBuriedHumans(building)){
					if(human.isHPDefined() && human.getHP() == 0){
						deadNum++;
					}
				}
				//如果建筑内死亡人数等于建筑内被埋的人数，就说明目标失效
				if(deadNum == worldInfo.getBuriedHumans(building).size() && worldInfo.getBuriedHumans(building).size()!=0) {
//                if(currentTargetPriority == 3) {
//					hangUpBuildings.add(this.result);
//				}
//                if(currentTargetPriority == 4) {
//                	hangUpSecondary.add(this.result);
//				}
//                if(currentTargetPriority == 5) {
//					this.worldInfo.getEntity(this.result);
//				}
					return false;
				}
			}
		}
		return true;
	}

	//这回合没动
	private boolean nonMove(){
		if(lastPosition!=null ){
			if(CSUConstants.DEBUG_AT_SEARCH && monitorExact){
				if(myID.getValue() == monitorID){
					System.out.println("本回合走了:"+Util.getdistance(worldInfo.getLocation(lastPosition),worldInfo.getLocation(nowPosition)));
				}
			}
			if(Util.getdistance(worldInfo.getLocation(lastPosition),worldInfo.getLocation(nowPosition)) < 10 && worldInfo.getEntity(nowPosition).getStandardURN() != BLOCKADE){
				return true;
			}else if(worldInfo.getEntity(nowPosition).getStandardURN() == BLOCKADE){//如果卡在了障碍里，给pf发消息
				messageManager.addMessage(new CommandPolice(
						true,
						null,
						agentInfo.getPosition(),
						CommandPolice.ACTION_CLEAR));
			}
		}
		return false;
	}

	//寻找最近的PF
	private EntityID findClosestPF(){
		Set<StandardEntity> standardEntities = new HashSet<StandardEntity>(worldInfo.getEntitiesOfType(POLICE_FORCE));
		int distance = 1999999999;
		Set<EntityID> entityIDS = Handy.objectsToIDs(standardEntities);
		EntityID nearestPF = null;
		for(EntityID entityID: entityIDS){
			if(Util.getdistance(worldInfo.getLocation(entityID),worldInfo.getLocation(myID))<distance){
				nearestPF = entityID;
			}
		}
		return nearestPF;
	}

}
