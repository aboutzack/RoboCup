package AUR.module.complex.self;

import java.util.ArrayList;
import java.util.LinkedList;
import AUR.util.AURCommunication;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURFireValueSetter;
import AUR.util.knd.AURAreaGraphValue;
import AUR.util.knd.AURBuilding;
import AUR.util.knd.AURFireZone;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.complex.BuildingDetector;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

public class AURFireBrigadeBuildingDetector extends BuildingDetector {
	private EntityID result;

	public AUR.util.knd.AURWorldGraph wsg = null;
	AgentInfo ai = null;
	double maxExtDist = 0;

	private AURCommunication acm = null;

	public AURFireBrigadeBuildingDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager,
			DevelopData developData) {
		super(ai, wi, si, moduleManager, developData);

		this.ai = ai;
		this.wsg = moduleManager.getModule("knd.AuraWorldGraph");
		maxExtDist = si.getFireExtinguishMaxDistance();
		acm = new AURCommunication(ai, wi, scenarioInfo, developData);
	}

	@Override
	public BuildingDetector updateInfo(MessageManager messageManager) {
		super.updateInfo(messageManager);
		wsg.updateInfo(messageManager);
		if (this.getCountUpdateInfo() >= 2) {
			return this;
		}
		this.acm.updateInfo(messageManager);
		return this;
	}

//	public LinkedList<AURAreaGraph> getFires() {
//		LinkedList<AURAreaGraph> result = new LinkedList<>();
//		wsg.dijkstra(ai.getPosition());
//		for (AURAreaGraph ag : wsg.areas.values()) {
//			if(ag.isBuilding() == false) {
//				continue;
//			}
//			if (ag.getBuilding().fireSimBuilding.isOnFire()) {
//
////				if (ag.noSeeTime() <= 4) { //  || ag.isRecentlyReportedFire()
//					result.add(ag);
////				}
//			}/* else {
//				if(ag.noSeeTime() == 0 && ag.isBuilding()) {
//					Building b = (Building) (ag.area);
//					if(b.isTemperatureDefined() && b.getTemperature() >= 40) {
//						result.add(ag); // check refuge & ...
//					}
//				}
//			}*/
//		}
//		return result;
//	}
	
	public boolean good(AURAreaGraph ag) {
		
		boolean result = true;
		
		if(ag.getBuilding().fireSimBuilding.ignoreFire() == true) {
			return false;
		}
		
		boolean noPath = ag.getBuilding().edgeToPereceptAndExtinguish == null && ag.lastDijkstraEntranceNode == null;
		
		double dist = ag.distFromAgent();
		double er = this.scenarioInfo.getFireExtinguishMaxDistance() - 1;
		if(noPath == true && dist > er) {
			return false;
		}
		
		//if(ag.isRecentlyReportedFire())
		
		return result;
		
	}
	
	public LinkedList<AURAreaGraph> getFires_new() {
		LinkedList<AURAreaGraph> result = new LinkedList<>();
		wsg.KStar(ai.getPosition());
		wsg.fireZonesCalculator.update();
		for(AURFireZone fireZone : wsg.fireZonesCalculator.zones) {
			if(fireZone.ok()) {
				for(AURBuilding b : fireZone.buildings) {
					if (good(b.ag)) { //  || ag.isRecentlyReportedFire()
						result.add(b.ag);
					}
				}
			}
		}
		
//		for (AURAreaGraph ag : wsg.areas.values()) {
//			if(ag.isBuilding() == false) {
//				continue;
//			}
//			
////			if() && ag.noSeeTime() > 0 && ag.isRecentlyReportedFire() == false) {
////				continue;
////			}
//			
//			if (ag.getBuilding().fireSimBuilding.isOnFire()) {
//
//
//			}/* else {
//				if(ag.noSeeTime() == 0 && ag.isBuilding()) {
//					Building b = (Building) (ag.area);
//					if(b.isTemperatureDefined() && b.getTemperature() >= 40) {
//						result.add(ag); // check refuge & ...
//					}
//				}
//			}*/
//		}
		return result;
	}
	
	@Override
	public BuildingDetector calc() {

		ArrayList<AURAreaGraphValue> points = new ArrayList<>();
		LinkedList<AURAreaGraph> closeFires = getFires_new();
		for (AURAreaGraph ag : closeFires) {
			points.add(new AURAreaGraphValue(ag));
		}

		if (points.size() > 0) {
			AURFireValueSetter vs = new AURFireValueSetter();
			vs.calc(wsg, points);
			if (vs.points.size() <= 0) {
				this.result = null;
				return this;
			}

			//for (int i = 0; i < vs.points.size(); i++) {
				//if (vs.points.get(i).ag.distFromAgent() < maxExtDist) {
					//this.result = vs.points.get(i).ag.area.getID();
					this.result = vs.points.get(0).ag.area.getID();
					return this;
				//}

			//}

		}
		this.result = null;
		return this;
	}

	@Override
	public EntityID getTarget() {
		return this.result;
	}

	@Override
	public BuildingDetector precompute(PrecomputeData precomputeData) {
		super.precompute(precomputeData);
		if (this.getCountPrecompute() >= 2) {
			return this;
		}
		this.wsg.precompute(precomputeData);
		return this;
	}

	@Override
	public BuildingDetector resume(PrecomputeData precomputeData) {
		super.resume(precomputeData);
		if (this.getCountPrecompute() >= 2) {
			return this;
		}
		this.wsg.resume(precomputeData);
		return this;
	}

	@Override
	public BuildingDetector preparate() {
		super.preparate();
		if (this.getCountPreparate() >= 2) {
			return this;
		}
		this.wsg.preparate();
		return this;
	}
}
