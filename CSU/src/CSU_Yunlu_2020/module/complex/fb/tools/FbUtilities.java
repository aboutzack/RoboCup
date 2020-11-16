package CSU_Yunlu_2020.module.complex.fb.tools;

import CSU_Yunlu_2020.standard.Ruler;
import CSU_Yunlu_2020.world.CSUWorldHelper;
import CSU_Yunlu_2020.world.object.CSUBuilding;
import javolution.util.FastMap;
import javolution.util.FastSet;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * This class offer some utility tools for FB.
 * 
 * @author appreciation-csu
 *
 */
public class FbUtilities {
	private static final int EXTINGUISH_DISTANCE_THRESHOLD = 5000;

	private CSUWorldHelper worldHelper;

	// constructor
	public FbUtilities(CSUWorldHelper worldHelper) {
		this.worldHelper = worldHelper;
	}
	
	/**
	 * Calculate the total needed water to extinguish the target building
	 * 
	 * @param building
	 *            the target building
	 * @return the total needed water of the target building
	 */
	public static int waterNeededToExtinguish(CSUBuilding building) {
		int groundArea = building.getSelfBuilding().getGroundArea();
    	int floors = building.getSelfBuilding().getFloors();
    	int buildingCode = building.getSelfBuilding().getBuildingCode();
    	double temperature = building.getEstimatedTemperature();
    	
    	return WaterCoolingEstimator.getWaterNeeded(groundArea, floors, buildingCode, temperature, 20);
	}
	
	/**
	 * Calculate the water power of an Agent to extinguish the target building.
	 * 
	 * @param world
	 *            the world model
	 * @param building
	 *            the target building
	 * @return the water power FB Agent will used to extinguish the target
	 *         building
	 */
	public static int calculateWaterPower(CSUWorldHelper world, CSUBuilding building) {
		int agentWater = ((FireBrigade) world.getSelfHuman()).getWater();
		int maxPower = world.getConfig().maxPower;
//		int neededWater = waterNeededToExtinguish(building);

		return Math.min(agentWater, maxPower);

		/* 500 is the lower limit of maxPower */
//		return Math.min(agentWater, Math.min(maxPower, Math.max(500, neededWater)));
	}

	/**
	 * Rerank buildings to increase the building value of unreachable buildings.
	 *
	 * @param buildings
	 *            a set of old ranked building
	 * @param fbAgent
	 *            FB Agent
	 * @return the reranked building set
	 */
	public SortedSet<Pair<Pair<EntityID, Double>, Double>> reRankBuildings(
            SortedSet<Pair<Pair<EntityID, Double>, Double>> buildings, FireBrigade fbAgent) {
		CSUBuilding csuBuilding;
		EntityID agentId = fbAgent.getID();
//		SortedSet<Pair<Pair<EntityID, Double>, Double>> result = new TreeSet<>(pairComparator);
		SortedSet<Pair<Pair<EntityID, Double>, Double>> result = new TreeSet<>(pairComparator_new);
		Set<CSUBuilding> buildingsExtinguishable = getBuildingInExtinguishableRange(this.worldHelper, agentId);
		boolean inMyExtiguishableRange;

		/*
		 * For building that has higher BUILDING_VALUE has small possibility in
		 * extinguishable distance, so we do not need to consider its
		 * reachability and just put it in is ok.
		 */
		int i = 0, AstarCount = 10;
		if (worldHelper.isMapMedium())
			AstarCount = 5;
		if (worldHelper.isMapHuge())
			AstarCount = 3;

		for (Pair<Pair<EntityID, Double>, Double> next : buildings) {
			csuBuilding = worldHelper.getCsuBuilding(next.first().first());

			if (i >= AstarCount) {  ///just put in
				result.add(new Pair<Pair<EntityID, Double>, Double>(next.first(), csuBuilding.BUILDING_VALUE));
				i++;
				continue;
			}

			inMyExtiguishableRange = buildingsExtinguishable.contains(csuBuilding);
			EntityID location = getNearest(worldHelper,csuBuilding.getAreasInExtinguishableRange(), agentId);

			// TODO: 3/9/20 实现isReachable
			if (inMyExtiguishableRange /*&& isReacable(worldHelper, router, location)*/) {
				result.add(new Pair<Pair<EntityID, Double>, Double>(next.first(), csuBuilding.BUILDING_VALUE));
				i++;
			} else {
				csuBuilding.BUILDING_VALUE -= 10000; ///*
				result.add(new Pair<Pair<EntityID, Double>, Double>(next.first(), csuBuilding.BUILDING_VALUE));
				i++;
			}
		}
		return result;
	}
	
	/**
	 * Get all buildings within the extinguishable range of the source Agent.
	 * 
	 * @param world
	 *            the world model
	 * @param source
	 *            the source Agent
	 * @return a set of extinguishable of the source Agent
	 */
	public static Set<CSUBuilding> getBuildingInExtinguishableRange(CSUWorldHelper world, EntityID source) {
		Set<CSUBuilding> result = new FastSet<>();
		double distance = world.getConfig().extinguishableDistance - EXTINGUISH_DISTANCE_THRESHOLD;
		Collection<StandardEntity> inRange = world.getObjectsInRange(source, (int)(distance * 1.5));
		for (StandardEntity entity : inRange) {
			if (entity instanceof Building) {
				if (world.getDistance(entity.getID(), source) < distance)
					result.add(world.getCsuBuilding(entity.getID()));
			}
		}
		return result;
	}
	
	public static List<EntityID> getAreaIdInExtinguishableRange(CSUWorldHelper world, EntityID source) {
		List<EntityID> result = new ArrayList<>();
		double distance = world.getConfig().extinguishableDistance - EXTINGUISH_DISTANCE_THRESHOLD;
		Collection<StandardEntity> inRange = world.getObjectsInRange(source, (int)(distance * 1.5));
		
		for (StandardEntity entity : inRange) {
			if (entity instanceof Area && world.getDistance(entity.getID(), source) < distance)
				result.add(entity.getID());
		}
		return result;
	}

	/**
	 * Get the nearest location from a list of target location.
	 *
	 * @param world
	 *            the world model
	 * @param locations
	 *            a set of target locations
	 * @param start
	 *            the start point
	 * @return the nearest target location
	 */
	public static EntityID getNearest(CSUWorldHelper world, List<EntityID> locations, EntityID start) {
		EntityID result = null;
		double minDistance = Double.MAX_VALUE;
		for (EntityID next : locations) {
			double distance = world.getDistance(start, next);
			if (distance < minDistance) {
				minDistance = distance;
				result = next;
			}
		}
		return result;
	}
	
	public static CSUBuilding getNearest(List<CSUBuilding> buildings, Pair<Integer, Integer> source){
		CSUBuilding targetBuilding = null;
		Building building;
		Pair<Integer, Integer> buildingLocation;
		double minDistance = Double.MAX_VALUE;
		double distance;
		for (CSUBuilding next : buildings) {
			building = next.getSelfBuilding();
			buildingLocation = new Pair<Integer, Integer>(building.getX(), building.getY());
			distance = Ruler.getDistance(source, buildingLocation);
			if (distance < minDistance) {
				minDistance = distance;
				targetBuilding = next;
			}
		}
		return targetBuilding;
	}

	/**
	 * Determines whether a target location is reachable.
	 * 
	 * @param world
	 *            the world model
	 * @param router
	 *            the router do the path plan task
	 * @param location
	 *            the target location
	 * @return true when the target location is reachable. Otherwise, false.
	 */
//	private boolean isReacable(CSUWorldHelper world, POVRouter router, EntityID location) {
//
//		List<EntityID> path = router.getAStar((Human)world.getControlledEntity(),
//				(Area)world.getEntity(location), router.getStrictCostFunction());
//		double timeToArrive = router.getRouteCost() / AdvancedWorldModel.MEAN_VELOCITY_OF_MOVING;
//		double euclidDistanceTime = world.getDistance(world.getAgent().getID(), location) / AdvancedWorldModel.MEAN_VELOCITY_OF_MOVING;
//		if (path.size() != 0 && timeToArrive < 3 * euclidDistanceTime) {
//			return true;
//		}
//
//		return false;
//	}
	///oak
	public static Comparator<Pair<EntityID, Double>> pairComparator = new Comparator<Pair<EntityID, Double>>() {
		/* sorted element in increase order*/
		@Override
		public int compare(Pair<EntityID, Double> o1, Pair<EntityID, Double> o2) {
			double value1 = o1.second();
			double value2 = o2.second();
			if (value1 < value2) ///>
				return 1;
			if (value1 > value2) ///<
				return -1;
			return 0;
		}
	};  
	
	public static Comparator<Pair<Pair<EntityID, Double>, Double>> pairComparator_new =
			new Comparator<Pair<Pair<EntityID, Double>, Double>>() {
		
		@Override
		public int compare(Pair<Pair<EntityID, Double>, Double> o1, Pair<Pair<EntityID, Double>, Double> o2) {
			if (o1.second().doubleValue() < o2.second().doubleValue()) ///>
				return 1;
			if (o1.second().doubleValue() > o2.second().doubleValue()) ///<
				return -1;
			
			if (o1.second().doubleValue() == o2.second().doubleValue()) {
				if (o1.first().second().doubleValue() > o2.first().second().doubleValue()) {
					return 1;
				}
				if (o1.first().second().doubleValue() < o2.first().second().doubleValue()) {
					return -1;
				}
				
				if (o1.first().second().doubleValue() == o2.first().second().doubleValue()) {
					return -1;
				}
			}
			
			return 0;
		}
	};
	
	/** Refresh the fire simulator to make the estimated result more correct.*/
//	public static void refreshFireEstimator(CSUWorldHelper world) {
//		Building building;
//		CSUBuilding csuBuilding;
//		for (StandardEntity entity : world.getEntitiesOfType(AgentConstants.BUILDINGS)) {
//			building = (Building) entity;
//			int fireyness = building.isFierynessDefined() ? building.getFieryness() : 0;
//			int temperature = building.isTemperatureDefined() ? building.getTemperature() : 0;
//
//			csuBuilding = world.getCsuBuilding(entity.getID());
//			if (csuBuilding.getEstimatedFieryness() > 4)
//				continue;
//			csuBuilding.setEnergy(temperature * csuBuilding.getCapacity(), "refreshFireEstimator");
//
//			switch (fireyness) {
//			case 0:
//				csuBuilding.setFuel(csuBuilding.getInitialFuel());
//				if (csuBuilding.getEstimatedTemperature() > csuBuilding.getIgnitionPoint())
//					csuBuilding.setEnergy(csuBuilding.getIgnitionPoint() / 2, "refreshFireEstimator case 0");
//				break;
//			case 1:
//				if (csuBuilding.getFuel() < csuBuilding.getInitialFuel() * 0.66)
//					csuBuilding.setFuel(csuBuilding.getInitialFuel() * 0.75f);
//				else
//					csuBuilding.setFuel(csuBuilding.getInitialFuel() * 0.9f);
//				break;
//			case 2:
//				if (csuBuilding.getFuel() < csuBuilding.getInitialFuel() * 0.33
//						|| csuBuilding.getFuel() > csuBuilding.getInitialFuel() * 0.66)
//					csuBuilding.setFuel(csuBuilding.getInitialFuel() * 0.5f);
//				break;
//			case 3:
//				if (csuBuilding.getFuel() < csuBuilding.getInitialFuel() * 0.01
//						|| csuBuilding.getFuel() > csuBuilding.getInitialFuel() * 0.33)
//					csuBuilding.setFuel(csuBuilding.getInitialFuel() * 0.15f);
//				break;
//			case 8:
//				csuBuilding.setFuel(0f);
//				break;
//			default:
//				break;
//			}
//		}
//	}

	/** Find the newest ignite building for a list of buildings.*/
	public CSUBuilding findNewestIgniteBuilding(List<CSUBuilding> buildings) {
		int minTime = Integer.MAX_VALUE;
		int tempTime;
		CSUBuilding newestIgniteBuilding = null;
		
		for (CSUBuilding next : buildings) {
			tempTime = worldHelper.getTime() - next.getIgnitionTime();
			if (tempTime < minTime) {
				minTime = tempTime;
				newestIgniteBuilding = next;
				if (minTime == 0)
					break;
			}
		}
		return newestIgniteBuilding;
	}

	public Set<EntityID> findMaximalCovering(Set<CSUBuilding> buildings) {
		Map<EntityID, Set<CSUBuilding>> areasMap = new FastMap<EntityID, Set<CSUBuilding>>(); // visibleFrom - buildings
		Map<CSUBuilding, Set<EntityID>> buildingsMap = new FastMap<CSUBuilding, Set<EntityID>>(); // building - visibleFroms


		// fill buildings and possible areas map
		for (CSUBuilding building : buildings) {
			buildingsMap.put(building, new FastSet<EntityID>(building.getVisibleFrom()));
			for (EntityID id : building.getVisibleFrom()) {
				Set<CSUBuilding> bs = areasMap.get(id);
				if (bs == null) {
					bs = new FastSet<CSUBuilding>();
				}
				bs.add(building);
				areasMap.put(id, bs);
			}
		}

		// call maximal covering method


		return processMatrix(buildingsMap, areasMap);
	}

	private Set<EntityID> processMatrix(Map<CSUBuilding, Set<EntityID>> buildingsMap, Map<EntityID, Set<CSUBuilding>> areasMap) {

		//step one
		Set<CSUBuilding> buildingsToRemove = new FastSet<CSUBuilding>();
		int i = 0, j;
		for (CSUBuilding building1 : buildingsMap.keySet()) {
			j = 0;
			if (!buildingsToRemove.contains(building1)) {
				for (CSUBuilding building2 : buildingsMap.keySet()) {
					if (i > j++ || building1.equals(building2) || buildingsToRemove.contains(building2)) { //continue;
					} else if (buildingsMap.get(building1).containsAll(buildingsMap.get(building2))) {
						buildingsToRemove.add(building1);
					} else if (buildingsMap.get(building2).containsAll(buildingsMap.get(building1))) {
						buildingsToRemove.add(building2);
					}
				}
			}
			i++;
		}
		for (CSUBuilding b : buildingsToRemove) {
			buildingsMap.remove(b);
			for (Set<CSUBuilding> bs : areasMap.values()) {
				bs.remove(b);
			}
		}

		// step two
		i = 0;
		Set<EntityID> areasToRemove = new FastSet<EntityID>();
		for (EntityID area1 : areasMap.keySet()) {
			j = 0;
			if (!areasToRemove.contains(area1)) {
				for (EntityID area2 : areasMap.keySet()) {
					if (i > j++ || area1.equals(area2) || areasToRemove.contains(area2)) { //continue;
					} else if (areasMap.get(area1).containsAll(areasMap.get(area2))) {
						areasToRemove.add(area2);
					} else if (areasMap.get(area2).containsAll(areasMap.get(area1))) {
						areasToRemove.add(area1);
					}
				}
			}
			i++;
		}
		for (EntityID id : areasToRemove) {
			areasMap.remove(id);
			for (Set<EntityID> ids : buildingsMap.values()) {
				ids.remove(id);
			}
		}

		// call again
		if (!areasToRemove.isEmpty() || !buildingsToRemove.isEmpty()) {
			return processMatrix(buildingsMap, areasMap);
		}
		return areasMap.keySet();
	}
}
