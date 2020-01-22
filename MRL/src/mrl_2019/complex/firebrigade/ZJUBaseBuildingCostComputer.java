package mrl_2019.complex.firebrigade;

import mrl_2019.algorithm.clustering.Cluster;
import mrl_2019.util.Util;
import mrl_2019.world.entity.MrlBuilding;
import mrl_2019.world.helper.PropertyHelper;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityConstants;
import rescuecore2.worldmodel.EntityID;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 12/13/13
 * Time: 4:59 PM
 *
 * @Author: Mostafa Shabani
 */
public class ZJUBaseBuildingCostComputer {
    private static final double INITIAL_COST = 500;
    private static final double AGENT_SPEED = 32000;
    private static final double BASE_PER_MOVE_COST = 30;
    private static final double SHOULD_MOVE_COST = BASE_PER_MOVE_COST * 2.2;
    private static final double MAX_DISTANCE_COST = BASE_PER_MOVE_COST * 10;
    private static final double NOT_IN_CHANGESET_COST = BASE_PER_MOVE_COST * 1.2;
    private static final int RECENT_UPDATE_TIME_MAX = 4;
    private static final double MAX_FUEL_VALUE_FOR_IGNITION_BUILDING = 4000000;
    private static final double IGNITION_BUILDING_MAX_COST = 25;
    private static final double IGNITION_BUILDING_FUEL_COEFFICIENT = MAX_FUEL_VALUE_FOR_IGNITION_BUILDING
            / IGNITION_BUILDING_MAX_COST;
    private static final double IGNITION_BUILDING_MIN_COST = 6;
    private static final double MAX_FUEL_VALUE_FOR_ESTI_IGNITION_BUILDING = 4000000;
    private static final double ESTI_IGNITION_BUILDING_MAX_COST = 25;
    private static final double ESTI_IGNITION_BUILDING_FUEL_COEFFICIENT = MAX_FUEL_VALUE_FOR_ESTI_IGNITION_BUILDING
            / ESTI_IGNITION_BUILDING_MAX_COST;
    private static final double ESTI_IGNITION_BUILDING_MIN_AWARD = 6;
    private static final double MAX_FUEL_VALUE_FOR_HEATING_BUILDING = 4000000;
    private static final double RECENT_UPDATED_HEATING_BUILDING_MAX_COST = 78;
    private static final double RECENT_UPDATED_HEATING_BUILDING_FUEL_COEFFICIENT = MAX_FUEL_VALUE_FOR_HEATING_BUILDING
            / RECENT_UPDATED_HEATING_BUILDING_MAX_COST;
    private static final double DEFAULT_HEATING_BUILDING_MAX_COST = 65;
    private static final double DEFAULT_HEATING_BUILDING_FUEL_COEFFICIENT = MAX_FUEL_VALUE_FOR_HEATING_BUILDING
            / DEFAULT_HEATING_BUILDING_MAX_COST;
    private static final double HEATING_BUILDING_MIN_COST = 20;
    private static final double MAX_FUEL_VALUE_FOR_BURNING_BUILDING = 4000000;
    private static final double BURNING_BUILDING_MAX_COST = 80;
    private static final double BURNING_BUILDING_MIN_COST = 36;
    private static final double BURNING_BUILDING_FUEL_COEFFICIENT = MAX_FUEL_VALUE_FOR_BURNING_BUILDING
            * 2 / (BURNING_BUILDING_MAX_COST + BURNING_BUILDING_MIN_COST);
    private static final double MAX_FUEL_VALUE_FOR_INFERNO_BUILDING = 4000000;
    private static final double INFERNO_BUILDING_MAX_COST = 60;
    private static final double INFERNO_BUILDING_MIN_COST = 25;
    private static final double INFERNO_BUILDING_FUEL_COEFFICIENT = MAX_FUEL_VALUE_FOR_INFERNO_BUILDING
            * 2 / (INFERNO_BUILDING_MAX_COST + INFERNO_BUILDING_MIN_COST);
    private static final double MAX_FUEL_LEFT_VALUE_FOR_AWARD = 4000000;
    private static final double FUEL_LEFT_MAX_AWARD = 40;
    private static final double FUEL_LEFT_COEFFICIENT = FUEL_LEFT_MAX_AWARD
            / MAX_FUEL_LEFT_VALUE_FOR_AWARD;
    private static final float MAX_TEMPERATURE = 400;
    private static final float TEMPERATURE_COEFFICIENT = MAX_TEMPERATURE / 32;
    private static final int lOW_TEMPERATURE = 180;
    private static final double LOW_TEMPERATURE_AWARD = 15;
    private static final double IN_SIGHT_HEATING_BUILDING_AWARD = 25;
    private static final double JUST_NOTICED_HEATING_BUILDING_AWARD = 18;
    private static final double CONFIRM_COST = 22;
    private static final int HIT_RATE_COEFFICIENT = 18;
    private static final double LAST_TARGET_COST = 7;
    private static final int CONDITION_RECENT_LEVEL_MAX = 5;
    private static final double COEFFICIENT_EXTINGUISH_TIME = 0.5;
    private static final int COMMAND_MAX = 4;
    private static final double CONFIRM_ALONE_DISTANCE = 100000;
    private MrlFireBrigadeWorld world;
    private PropertyHelper propertyHelper;
    private Set<EntityID> buildingsInSight;
    private int maxExtinguishDistance;
    private double perMoveCost;
    private double notInChangeSetCost;
    private double shouldMoveCost;
    private MrlBuilding lastTarget;

    public ZJUBaseBuildingCostComputer(MrlFireBrigadeWorld world) {
        this.world = world;
        this.maxExtinguishDistance = world.getScenarioInfo().getFireExtinguishMaxDistance();
        propertyHelper = world.getHelper(PropertyHelper.class);
    }

    public void updateFor(Cluster targetCluster, MrlBuilding lastTarget) {
        this.buildingsInSight = world.getVisitedBuildings();
        double clusterSize = Math.max(targetCluster.getConvexHullObject().getConvexPolygon().getBounds2D().getWidth(), targetCluster.getConvexHullObject().getConvexPolygon().getBounds2D().getHeight());
        double mapSize = Math.max(world.getBounds().getWidth(), world.getBounds().getHeight());
        double worldFireBuildingSituation = clusterSize / mapSize;
        double coefficient = worldFireBuildingSituation;
        this.perMoveCost = BASE_PER_MOVE_COST * coefficient;
        this.shouldMoveCost = SHOULD_MOVE_COST * coefficient;
        this.notInChangeSetCost = NOT_IN_CHANGESET_COST * coefficient;
        this.lastTarget = lastTarget;
    }

    public int getCost(MrlBuilding fireBuilding) {
        double cost = INITIAL_COST;

        FireBrigade me = (FireBrigade) world.getSelfHuman();
        Building building = fireBuilding.getSelfBuilding();

        // distance and should move    //todo: change with pathPlaner mostafas
        double distance = Util.distance(me.getX(), me.getY(), building.getX(), building.getY());
        if (distance > maxExtinguishDistance) {
            double timeToMove = (distance - maxExtinguishDistance) / AGENT_SPEED;
            double distanceCost;
            if (timeToMove <= 0.5) {
                distanceCost = timeToMove * perMoveCost * 1.3;//0.4;
            } else if (timeToMove <= 2) {
                distanceCost = timeToMove * perMoveCost * 1.9;//0.6;
            } else if (timeToMove <= 4) {
                distanceCost = timeToMove * perMoveCost * 2.6;//0.8;
            } else {
                distanceCost = timeToMove * perMoveCost * 3.1;//1.0;
            }
            if (distanceCost > MAX_DISTANCE_COST) {
                distanceCost = MAX_DISTANCE_COST;
            }
            cost += distanceCost + shouldMoveCost;
        }

        // currentEstimatedFieryness
        StandardEntityConstants.Fieryness currentEstimatedFieryness = StandardEntityConstants.Fieryness.values()[fireBuilding.getEstimatedFieryness()];
        float currentEstimatedFuel = fireBuilding.getFuel();
        float initFuel = fireBuilding.getInitialFuel();
        // ignition
        if (currentEstimatedFieryness == StandardEntityConstants.Fieryness.HEATING
                && world.getTime() - fireBuilding.getIgnitionTime() <= RECENT_UPDATE_TIME_MAX) {
            double award = Math.min(IGNITION_BUILDING_MAX_COST, currentEstimatedFuel
                    / IGNITION_BUILDING_FUEL_COEFFICIENT);
            cost -= Math.max(award, IGNITION_BUILDING_MIN_COST);
        } else if (fireBuilding.getIgnitionTime() != -1
                && world.getTime() - fireBuilding.getIgnitionTime() <= RECENT_UPDATE_TIME_MAX) {
            double award = Math.min(ESTI_IGNITION_BUILDING_MAX_COST, currentEstimatedFuel
                    / ESTI_IGNITION_BUILDING_FUEL_COEFFICIENT);
            cost -= Math.max(award, ESTI_IGNITION_BUILDING_MIN_AWARD);
        }

        // fuel
        boolean isRecentlyUpdated = (world.getTime() - propertyHelper.getEntityLastUpdateTime(building) <= RECENT_UPDATE_TIME_MAX);
        if (currentEstimatedFieryness == StandardEntityConstants.Fieryness.HEATING) {
            double award;
            if (isRecentlyUpdated) {
                award = Math.min(RECENT_UPDATED_HEATING_BUILDING_MAX_COST, currentEstimatedFuel
                        / RECENT_UPDATED_HEATING_BUILDING_FUEL_COEFFICIENT);
            } else {
                award = Math.min(DEFAULT_HEATING_BUILDING_MAX_COST, currentEstimatedFuel
                        / DEFAULT_HEATING_BUILDING_FUEL_COEFFICIENT);
            }
            cost -= Math.max(award, HEATING_BUILDING_MIN_COST);
        } else if (currentEstimatedFieryness == StandardEntityConstants.Fieryness.BURNING) {
            double dCost = initFuel / BURNING_BUILDING_FUEL_COEFFICIENT;
            if (dCost > BURNING_BUILDING_MAX_COST) {
                dCost = BURNING_BUILDING_MAX_COST;
            } else if (dCost < BURNING_BUILDING_MIN_COST) {
                dCost = BURNING_BUILDING_MIN_COST;
            }
            cost += dCost;
        } else if (currentEstimatedFieryness == StandardEntityConstants.Fieryness.INFERNO) {
            double dCost = initFuel / INFERNO_BUILDING_FUEL_COEFFICIENT;
            if (dCost > INFERNO_BUILDING_MAX_COST) {
                dCost = INFERNO_BUILDING_MAX_COST;
            } else if (dCost < INFERNO_BUILDING_MIN_COST) {
                dCost = INFERNO_BUILDING_MIN_COST;
            }
            cost += dCost;
        }

        // totalHits/totalRays
        cost += HIT_RATE_COEFFICIENT * fireBuilding.getHitRate();

        // Temperature
        if (currentEstimatedFieryness == StandardEntityConstants.Fieryness.HEATING) {
            double dTemperature = fireBuilding.getEstimatedTemperature()
                    - fireBuilding.getIgnitionPoint();
            cost += Math.min(MAX_TEMPERATURE, dTemperature) / TEMPERATURE_COEFFICIENT;
            if (dTemperature < lOW_TEMPERATURE) {
                cost -= LOW_TEMPERATURE_AWARD;
            }
        }

        float fuelLeft = initFuel - currentEstimatedFuel;
        cost -= Math.min(FUEL_LEFT_MAX_AWARD, fuelLeft * FUEL_LEFT_COEFFICIENT);

        // better to extinguish the buildings in the changeset
        if (!buildingsInSight.contains(building.getID())) {
            cost += notInChangeSetCost;
        }

        // just updated
        if (fireBuilding.getSensedTime() == world.getTime()
                && currentEstimatedFieryness == StandardEntityConstants.Fieryness.HEATING) {
            cost -= IN_SIGHT_HEATING_BUILDING_AWARD;
        } else if (world.getTime() - fireBuilding.getSensedTime() <= 2
                && currentEstimatedFieryness == StandardEntityConstants.Fieryness.HEATING) {
            cost -= JUST_NOTICED_HEATING_BUILDING_AWARD;
        }

//        // need confirm?
        if (shouldConfirmBuildingCondition(fireBuilding)) {
            cost += CONFIRM_COST;
        }

        if (lastTarget != null && lastTarget.equals(fireBuilding)) {
            cost -= LAST_TARGET_COST;
        }


        StandardEntity gasStation = findNearestGasStation(fireBuilding);
        Building gasBuilding = (Building) gasStation;

        if (gasStation != null
                && gasBuilding.isFierynessDefined()
                && (gasBuilding.getFieryness() == 0 || gasBuilding.getFieryness() == 4)
                && gasBuilding.getTemperature() < 40) {
            int distanceToGasStation = world.getWorldInfo().getDistance(gasStation, fireBuilding.getSelfBuilding());
            int maxDistance = Math.max(world.getScenarioInfo().getPerceptionLosMaxDistance(), world.getScenarioInfo().getFireExtinguishMaxDistance());
//            int diff = maxDistance - distanceToGasStation;

            if (distanceToGasStation < maxDistance) {
                cost *= 0.8;
            } else if (distanceToGasStation < maxDistance * 2) {
                cost *= 0.9;
            } else {
                cost *= 1;
            }
        }


       /* if (diff > maxDistance / 2) {
//            cost *= 2;
        } else if (diff < -1 * maxDistance) {
            cost = 1 * cost;
        } else {
            cost *= 0.8;
        }
*/
        return (int) (cost < 0 ? 0 : cost);
    }

    private StandardEntity findNearestGasStation(MrlBuilding fireBuilding) {

        if (world.getGasStations().isEmpty()) {
            return null;
        }
        int minDistance = Integer.MAX_VALUE;
        int distance = 0;
        StandardEntity nearestGasStation = null;
        for (StandardEntity gasEntity : world.getGasStations()) {
            distance = world.getWorldInfo().getDistance(gasEntity, fireBuilding.getSelfBuilding());
            if (distance < minDistance) {
                minDistance = distance;
                nearestGasStation = gasEntity;
            }
        }

        return nearestGasStation;

    }

    public boolean shouldConfirmBuildingCondition(MrlBuilding fireBuilding) {
        Building b = fireBuilding.getSelfBuilding();

        int timeNow = world.getTime();
        int buildingUpdateTime = propertyHelper.getEntityLastUpdateTime(b);

        if (timeNow - buildingUpdateTime <= 1) {
            // just updated
            return false;
        }
        if (timeNow - buildingUpdateTime > CONDITION_RECENT_LEVEL_MAX) {
            // long time no update
            return true;
        }

        int waterQuantity = fireBuilding.getWaterQuantity();
        if (waterQuantity <= 0) {
            return false;
        }
        double needExtinguishWater = FireBrigadeUtilities.waterNeededToExtinguish(fireBuilding) * 0.7;
        return waterQuantity > needExtinguishWater;
    }
}
