package mrl_2019.complex.firebrigade;

import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.PathPlanning;
import javolution.util.FastSet;
import mrl_2019.MRLConstants;
import mrl_2019.algorithm.clustering.FireCluster;
import mrl_2019.util.ConstantComparators;
import mrl_2019.util.Util;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.entity.MrlBuilding;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 2/19/13
 * Time: 12:45 AM
 * Author: Mostafa Movahedi
 */
public class FireBrigadeUtilities {
    private static final int EXTINGUISH_DISTANCE_THRESHOLD = 5000;
    private MrlFireBrigadeWorld world;
    private Human selfHuman;
    private Set<StandardEntity> readyFireBrigades;
    //    private HumanHelper humanHelper;
    private PathPlanning pathPlanning;


    public FireBrigadeUtilities(MrlFireBrigadeWorld world) {
        this.world = world;
        this.selfHuman = world.getSelfHuman();
//        this.humanHelper = world.getHelper(HumanHelper.class);
        readyFireBrigades = new FastSet<StandardEntity>();
//        this.pathPlanning=pathPlanning;

    }

    private static double calcEffectiveWaterPerCycle(MrlFireBrigadeWorld world, Point targetPoint) {
        int waterQuantity = world.getMaxWater();
        int maxPower = world.getMaxPower();
        int refillRate = world.getWaterRefillRate();
        int waterQuantityPerRefillRate = waterQuantity / refillRate;
        double waterQuantityPerMaxPower = waterQuantity / maxPower;
        return maxPower * (waterQuantityPerMaxPower / (waterQuantityPerMaxPower + waterQuantityPerRefillRate + (Util.findDistanceToNearest(world, world.getWorldInfo().getEntitiesOfType(StandardEntityURN.REFUGE), targetPoint) / MRLConstants.MEAN_VELOCITY_OF_MOVING)));
    }

    public static int waterNeededToExtinguish(MrlBuilding building) {
        return WaterCoolingEstimator.getWaterNeeded(building.getSelfBuilding().getGroundArea(), building.getSelfBuilding().getFloors(),
                building.getSelfBuilding().getBuildingCode(), building.getEstimatedTemperature(), 20);
    }

    public static int waterNeededToExtinguishNotEstimated(Building building) {
        return WaterCoolingEstimator.getWaterNeeded(building.getGroundArea(), building.getFloors(),
//                building.getBuildingCode(), building.getEstimatedTemperature(), 20);
                building.getBuildingCode(), building.getTemperature(), 20);
    }

    public static int calculateWaterPower(MrlFireBrigadeWorld world, MrlBuilding building) {
        return Math.min(((FireBrigade) world.getSelfHuman()).getWater(), Math.min(world.getMaxPower(), Math.max(500, waterNeededToExtinguish(building))));
    }

    public static int calculateWaterPower(int remainedWater, int maxPower, MrlBuilding building) {
        return Math.min(remainedWater, Math.min(maxPower, Math.max(500, waterNeededToExtinguish(building))));
    }

    public static int calculateWaterPowerNotEstimated(int remainedWater, int maxPower, Building building) {
        return Math.min(remainedWater, Math.min(maxPower, Math.max(500, waterNeededToExtinguishNotEstimated(building))));
    }

    /* public static Map<MrlBuilding, Boolean> calcBuildingsReachability(MrlFireBrigadeWorld world, PathPlanning pathPlanning, List<MrlBuilding> buildings) {
         Map<EntityID, List<MrlBuilding>> mutualExtinguishLocation = findMutualExtinguishLocation(buildings);
         Map<MrlBuilding, Boolean> buildingsReachability = new FastMap<MrlBuilding, Boolean>();

         for (EntityID next : mutualExtinguishLocation.keySet()) {
             boolean isReachable = isReachable(world, pathPlanning, next);
             for (MrlBuilding b : mutualExtinguishLocation.get(next)) {
                 if (buildingsReachability.containsKey(b)) {
                     buildingsReachability.put(b, isReachable || buildingsReachability.get(b));
                 } else {
                     buildingsReachability.put(b, isReachable);
                 }
             }
         }
         return buildingsReachability;
     }

 */  /*  private static boolean isReachable(MrlFireBrigadeWorld world, PathPlanning pathPlanning, EntityID location) {
        int size = pathPlanner.planMove((Area) world.getSelfPosition(), (Area) world.getEntity(location), MRLConstants.IN_TARGET, false).size();
        double timeToArrive = pathPlanner.getPathCost() / MRLConstants.MEAN_VELOCITY_OF_MOVING;
        double normalizedDirectDistance = world.getDistance(world.getSelfPosition().getID(), location) / MRLConstants.MEAN_VELOCITY_OF_MOVING;
        return size != 0 && timeToArrive < 3 * normalizedDirectDistance*//* && !world.getPlatoonAgent().getUnReachablePositions().contains(world.getEntity(location))*//*;
    }
*/
    public static Map<EntityID, List<MrlBuilding>> findMutualExtinguishLocation(List<MrlBuilding> fieryBuildings) {
        throw new UnsupportedOperationException();
    }

    public static Set<EntityID> findAreaIDsInExtinguishRange(WorldInfo worldInfo, ScenarioInfo scenarioInfo, EntityID source) {
        Set<EntityID> result = new HashSet<>();
        int maxExtinguishDistance = scenarioInfo.getFireExtinguishMaxDistance() - EXTINGUISH_DISTANCE_THRESHOLD;
        for (StandardEntity next : worldInfo.getObjectsInRange(source, (int) (maxExtinguishDistance * 1.5))) {
            if (next instanceof Area && worldInfo.getDistance(next.getID(), source) < maxExtinguishDistance) {
                result.add(next.getID());
            }
        }
        return result;
    }

    public static Set<MrlBuilding> getBuildingsInMyExtinguishRange(MrlFireBrigadeWorld world) {
        Set<MrlBuilding> result = new FastSet<MrlBuilding>();
        int maxExtinguishDistance = world.getMaxExtinguishDistance() - EXTINGUISH_DISTANCE_THRESHOLD;
        for (StandardEntity next : world.getObjectsInRange(world.getAgentInfo().getID(), (int) (maxExtinguishDistance * 1.5))) {
            if (next instanceof Building) {
                MrlBuilding building = world.getMrlBuilding(next.getID());
                if (world.getDistance(next.getID(), world.getAgentInfo().getID()) < maxExtinguishDistance) {
                    result.add(building);
                }
            }
        }
        return result;
    }

    public static List<MrlBuilding> findBuildingsInExtinguishRangeOf(MrlWorldHelper worldHelper, WorldInfo worldInfo,ScenarioInfo scenarioInfo, EntityID source) {
        List<MrlBuilding> result = new ArrayList<MrlBuilding>();
        int maxExtinguishDistance = scenarioInfo.getFireExtinguishMaxDistance()- EXTINGUISH_DISTANCE_THRESHOLD;
        for (StandardEntity next : worldInfo.getObjectsInRange(source, (int) (maxExtinguishDistance * 1.5))) {
            if (next instanceof Building) {
                MrlBuilding building = worldHelper.getMrlBuilding(next.getID());
                if (worldInfo.getDistance(next.getID(), source) < maxExtinguishDistance) {
                    result.add(building);
                }
            }
        }
        return result;
    }

    public static void refreshFireEstimator(MrlWorldHelper world) {
        for (StandardEntity entity : world.getBuildings()) {
            Building building = (Building) entity;
            int fieryness = building.isFierynessDefined() ? building.getFieryness() : 0;
            int temperature = building.isTemperatureDefined() ? building.getTemperature() : 0;
            MrlBuilding mrlBuilding = world.getMrlBuilding(building.getID());

           /* //age estimator mige khamoosh shode vali man ghablan didam fieryness 1 boode, hamoon estimator doroste
            if (building.isFierynessDefined()) {
                if (mrlBuilding.getEstimatedFieryness() > 4 && building.getFieryness() == 1) {
                    continue;
                }
            }*/

            //age estimator mige khamoosh sode yani khamoosh shode
            if (mrlBuilding.getEstimatedFieryness() > 4) {
                continue;
            }


            mrlBuilding.setEnergy(temperature * mrlBuilding.getCapacity());
            switch (fieryness) {
                case 0:
                    mrlBuilding.setFuel(mrlBuilding.getInitialFuel());
                    if (mrlBuilding.getEstimatedTemperature() >= mrlBuilding.getIgnitionPoint()) {
                        mrlBuilding.setEnergy(mrlBuilding.getIgnitionPoint() / 2);
                    }
                    break;
                case 1:
                    if (mrlBuilding.getFuel() < mrlBuilding.getInitialFuel() * 0.66) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.75));
                    } else if (mrlBuilding.getFuel() == mrlBuilding.getInitialFuel()) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.90));
                    }
                    break;

                case 2:
                    if (mrlBuilding.getFuel() < mrlBuilding.getInitialFuel() * 0.33
                            || mrlBuilding.getFuel() > mrlBuilding.getInitialFuel() * 0.66) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.50));
                    }
                    break;

                case 3:
                    if (mrlBuilding.getFuel() < mrlBuilding.getInitialFuel() * 0.01
                            || mrlBuilding.getFuel() > mrlBuilding.getInitialFuel() * 0.33) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.15));
                    }
                    break;

                case 8:
                    mrlBuilding.setFuel(0);
                    break;
            }
        }
    }

    public void calcClusterCondition(MrlFireBrigadeWorld world, FireCluster fireCluster) {
        double effectiveWater = calcEffectiveWaterPerCycle(world, fireCluster.getCenter());
        int neededWater = fireCluster.calcNeededWaterToExtinguish() / 10;
        double totalEffectiveWater = effectiveWater * (world.getWorldInfo().getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE).size() / 2);
        if (neededWater < totalEffectiveWater) {
            fireCluster.setCondition(FireCluster.Condition.largeControllable);
        } else {
            fireCluster.setCondition(FireCluster.Condition.edgeControllable);
        }

//        MrlPersonalData.VIEWER_DATA.setFireClusterCondition(world.getSelf().getID(), fireCluster);
    }

    //selects some of top elements of the input list
    public SortedSet<Pair<EntityID, Double>> selectTop(int number, SortedSet<Pair<EntityID, Double>> inputList, Comparator<Pair<EntityID, Double>> comparator) {
        /*List outPut = new ArrayList();
        for (int i = 0; i < number; i++) {
            outPut.add(inputList.remove(0));
        }*/

        SortedSet<Pair<EntityID, Double>> outPut = new TreeSet<Pair<EntityID, Double>>(comparator);
        Pair<EntityID, Double> temp;
        for (int i = 0; i < number; i++) {
            if (!inputList.isEmpty()) {
                temp = inputList.first();
                inputList.remove(temp);
                outPut.add(temp);
            }
        }

        return outPut;
    }

    /*  //This function looks in final list of buildings, if there is a building that seems to be unreachable, its value multiplies to 100, else its value multiplies to the estimated time to get there. Remember that we want to minimize the cost of extinguishing buildings.
      public SortedSet<Pair<EntityID, Double>> oldReRankBuildings(SortedSet<Pair<EntityID, Double>> finalBuildings) {
          MrlBuilding building;
          SortedSet<Pair<EntityID, Double>> rankedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
          for (int i = 0; i < finalBuildings.size(); i++) {
              building = world.getMrlBuilding(finalBuildings.first().first());
              finalBuildings.remove(finalBuildings.first());
              if (pathPlanner.planMove((Area) world.getSelfPosition(), building.getSelfBuilding(), world.getMaxExtinguishDistance(), false).size() != 0) {
                  building.BUILDING_VALUE *= pathPlanner.getPathCost() / (MRLConstants.MEAN_VELOCITY_OF_MOVING);
                  rankedBuildings.add(new Pair<EntityID, Double>(building.getID(), building.BUILDING_VALUE));
              } else {
                  building.BUILDING_VALUE = Integer.MAX_VALUE;

  //                System.out.println("Agent " + world.getSelf().getID() + " found an unreachable fiery building: " + building.getID());
                  rankedBuildings.add(new Pair<EntityID, Double>(building.getID(), building.BUILDING_VALUE));
              }
          }
          return rankedBuildings;
      }
  */
  /*  public SortedSet<Pair<EntityID, Double>> reRankBuildings(SortedSet<Pair<EntityID, Double>> finalBuildings) {
        MrlBuilding building;
        SortedSet<Pair<EntityID, Double>> rankedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        Set<MrlBuilding> buildingsInMyExtinguishRange = getBuildingsInMyExtinguishRange(world);
        boolean isInMyExtinguishDistance;

        int i = 0;
        int AStarCount = 10;
        if (world.isMapMedium()) AStarCount = 5;
        if (world.isMapHuge()) AStarCount = 3;
        for (Pair<EntityID, Double> next : finalBuildings) {
            building = world.getMrlBuilding(next.first());
            if (i >= AStarCount) {
                rankedBuildings.add(new Pair<EntityID, Double>(building.getID(), building.BUILDING_VALUE));
                continue;
            }

            isInMyExtinguishDistance = buildingsInMyExtinguishRange.contains(building);
//            EntityID location = Util.getNearest(world, world.getAreaIDsInExtinguishRange(building.getID()), self.getID());
            EntityID location = Util.getNearest(world.getWorldInfo(), building.getExtinguishableFromAreas(), world.getAgentInfo().getID());

            if (isInMyExtinguishDistance || isReachable(world, pathPlanner, location)) {
                //building.BUILDING_VALUE *= pathPlanner.getPathCost() / (MRLConstants.MEAN_VELOCITY_OF_MOVING);
                rankedBuildings.add(new Pair<EntityID, Double>(building.getID(), building.BUILDING_VALUE));
            } else {
                building.BUILDING_VALUE *= 10;
//                building.BUILDING_VALUE *= 10000;
//                System.out.println("Agent " + world.getSelf().getID() + " found an unreachable fiery building: " + building.getID());
                rankedBuildings.add(new Pair<EntityID, Double>(building.getID(), building.BUILDING_VALUE));
            }
        }
        return rankedBuildings;
    }
*/
    public List<Area> getAreasInExtinguishRange(EntityID source) {
        List<Area> result = new ArrayList<Area>();
        int maxExtinguishDistance = world.getMaxExtinguishDistance() - EXTINGUISH_DISTANCE_THRESHOLD;
        for (StandardEntity next : world.getObjectsInRange(source, (int) (maxExtinguishDistance * 1.5))) {
            if (next instanceof Area && world.getDistance(next.getID(), source) <= maxExtinguishDistance) {
                result.add((Area) next);
            }
        }
        return result;
    }

    /*   public void updateReadyFireBrigades() {
           for (StandardEntity fireBrigadeEntity : world.getFireBrigades()) {
               FireBrigade fireBrigade=(FireBrigade)fireBrigadeEntity;
               if (fireBrigade.getID().equals(world.getAgentInfo().getID())) {      // This "fireBrigade" is actually "me"
                   if (world.getSelfPosition() instanceof Building) {
                       if (world.getSelfHuman().getBuriedness() == 0            // Means that I'm not buried!
                               && world.getTime() >= 2) {                          // exactly at the second cycle, ambulance Team agents might get buried.
                           readyFireBrigades.add(fireBrigade);
                       } else {
                           // Too soon to decide if I will be buried (at time:2)
                       }
                   } else { // I am on the road!
                       readyFireBrigades.add(fireBrigade);
                   }
               } else { // This "fireBrigade" is someone else (not me)
                   if (humanHelper.getAgentState(fireBrigade.getID()) == null) { // I have no idea in what state this fireBrigade is.
                       //TODO @BrainX Is there someway better to access others' states?
                       if (fireBrigade.isBuriednessDefined() && fireBrigade.getBuriedness() == 0) { // I have information about this fireBrigade's buriedness and it's not buried.
                           readyFireBrigades.add(fireBrigade);
                       } else if (world.getWorldInfo().getPosition(fireBrigade) instanceof Road) { // If I don't know if this fireBrigade is buried, I consider it healthy if it's on the road.
                           readyFireBrigades.add(fireBrigade);
                       }
                   } else { // I know this fireBrigade's state
                       if (humanHelper.isAgentStateHealthy(fireBrigade.getID())) {
                           readyFireBrigades.add(fireBrigade);
                       }
                   }
               }
           }

       }
   */
    public Set<StandardEntity> getReadyFireBrigades() {
        return readyFireBrigades;
    }

    public MrlBuilding findSmallestBuilding(List<MrlBuilding> buildings) {
        int minArea = Integer.MAX_VALUE;
        MrlBuilding smallestBuilding = null;
        for (MrlBuilding building : buildings) {
            if (building.getSelfBuilding().getTotalArea() < minArea) {
                minArea = building.getSelfBuilding().getTotalArea();
                smallestBuilding = building;
            }
        }
        return smallestBuilding;

    }

    public MrlBuilding findNewestIgnitedBuilding(List<MrlBuilding> buildings) {
        int minTime = Integer.MAX_VALUE;
        int tempTime;
        MrlBuilding smallestBuilding = null;
        for (MrlBuilding building : buildings) {
            tempTime = world.getTime() - building.getIgnitionTime();
            if (tempTime < minTime) {
                minTime = tempTime;
                smallestBuilding = building;
            }
        }
        return smallestBuilding;
    }

    public boolean amINeededForCluster(FireCluster targetCluster) {
        boolean needed = false;
        List<Pair<EntityID, Integer>> agentPairs = new ArrayList<>();
        for (StandardEntity fireBrigadeEntity : world.getFireBrigades()) {
            Human brigadeEntity = (Human) fireBrigadeEntity;
            int distance = Util.distance(world.getWorldInfo().getLocation(brigadeEntity.getPosition()), targetCluster.getCenter());
            agentPairs.add(new Pair<>(fireBrigadeEntity.getID(), distance));
        }
        Collections.sort(agentPairs, ConstantComparators.DISTANCE_VALUE_COMPARATOR);
        double fireBrigadesEnergy = 0;
        int i = 1;
        for (Pair<EntityID, Integer> pair : agentPairs) {
            fireBrigadesEnergy += world.getMaxPower();
            if (pair.first().equals(world.getAgentInfo().getID())) {
                needed = true;
            }
            if (fireBrigadesEnergy >= targetCluster.getClusterEnergy() / i++) {
                break;
            }
        }
        if (needed) {
            world.printData("I am needed for cluster: " + targetCluster.getCenter());
        }
        return needed;
    }

    public int findNumberOfNeededAgents(FireCluster cluster) {
//        return (int) Math.ceil(cluster.getClusterEnergy() / world.getMaxPower());
        return (int) Math.ceil(cluster.getClusterVolume() * 3 / world.getMaxPower());
    }

    /**
     * Effective buildings are those connected buildings that can ignite a put off fire
     *
     * @param building
     */
    public List<MrlBuilding> getBuildingsCanIgnite(MrlBuilding building) {

        List<MrlBuilding> candidateBuildings = new ArrayList<>();

        List<MrlBuilding> connectedBuildings = building.getConnectedBuilding();
       /* if (connectedBuildings.isEmpty()) {
            Collection<StandardEntity> neighbour = world.getObjectsInRange(building.getSelfBuilding(), Wall.MAX_SAMPLE_DISTANCE);
            List<EntityID> neighbourBuildings = new ArrayList<>();
            for (StandardEntity entity : neighbour) {
                if (entity instanceof Building) {
                    neighbourBuildings.add(entity.getID());
                    MrlBuilding mrlBuilding = world.getMrlBuilding(entity.getID());
                    if (mrlBuilding.getWalls().isEmpty()) {
                        mrlBuilding.initWalls(world);
                        mrlBuilding.initWallValues(world);
                    }
//                    building.addMrlBuildingNeighbour(mrlBuilding);
                    building.getAllWalls().addAll(mrlBuilding.getWalls());
                }
            }
            building.setNeighbourIdBuildings(neighbourBuildings);
            building.initWallValues(world);
        }*/
        double sumOfNeighbourEnergies = 0;
        for (int i = 0; i < connectedBuildings.size(); i++) {
            MrlBuilding connectedBuilding = connectedBuildings.get(i);
            if (connectedBuilding.getSelfBuilding().isTemperatureDefined() && connectedBuilding.getSelfBuilding().getTemperature() > 40) {
                candidateBuildings.add(connectedBuilding);
            }
            double radiation = connectedBuilding.getRadiationEnergy();
            double connectionValue = building.getConnectedValues().get(i);
            sumOfNeighbourEnergies += radiation * connectionValue;
        }

        if ((sumOfNeighbourEnergies + building.getEnergy()) / building.getVolume() > 40) {
            return candidateBuildings;
        } else {
            return new ArrayList<>();
        }
    }

    public int findNumberOfNeededAgentsBasedOnValue(int numberOfAgents, double sumOfValues, double value, double clusterVolumeRatio, double sumOfVolumeRatios) {
//        final int groupCount = 5;
//        final int agentsInEachGroup = numberOfAgents / groupCount;
        final int agentsInEachGroup = 5;

        double rate = clusterVolumeRatio / sumOfVolumeRatios;
        int needed = (int) (rate * numberOfAgents);


        int groupNeeded = (int) Math.ceil(needed * 1.0d / agentsInEachGroup);
        needed = groupNeeded * agentsInEachGroup;
//        if (needed <= agentsInEachGroup) {
//            needed = agentsInEachGroup;
//        }

        int numberOfNeeded = (int) Math.ceil(value * 3 / world.getMaxPower());

        if (numberOfNeeded < needed) {
            groupNeeded = (int) Math.ceil(numberOfNeeded * 1.0d / agentsInEachGroup);
            needed = groupNeeded * agentsInEachGroup;
        }

        return needed;


    }
}
