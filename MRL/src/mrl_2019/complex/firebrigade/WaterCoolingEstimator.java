package mrl_2019.complex.firebrigade;


import mrl_2019.MRLConstants;
import mrl_2019.world.entity.MrlBuilding;

/**
 * User: roohola
 * Date: 3/30/11
 * Time: 11:53 PM
 */
public class WaterCoolingEstimator {
    public static double WATER_COEFFICIENT = 20f;
    public static int FLOOR_HEIGHT = 7;

    protected static double getBuildingEnergy(int buildingCode, int groundArea, int floors, double temperature) {
        return temperature * getBuildingCapacity(buildingCode, groundArea, floors);
    }

    protected static double getBuildingCapacity(int buildingCode, int groundArea, int floors) {
        double thermoCapacity;
        switch (buildingCode) {
            case 0:
                //wooden
                thermoCapacity = 1.1;
                break;
            case 1:
                //steel
                thermoCapacity = 1.0;
                break;
            default:
                //concrete
                thermoCapacity = 1.5;
                break;
        }
        return thermoCapacity * groundArea * floors * FLOOR_HEIGHT;
    }

    public static int getWaterNeeded(int groundArea, int floors, int buildingCode, double temperature, double finalTemperature) {
        int waterNeeded = 0;
        double currentTemperature = temperature;
        int step = 500;
        while (true) {
            currentTemperature = waterCooling(groundArea, floors, buildingCode, currentTemperature, step);
            waterNeeded += step;
            if (currentTemperature <= finalTemperature) {
                break;
            }
        }
        if (MRLConstants.DEBUG_WATER_COOLING) {
            System.out.println("water cooling predicts: " + waterNeeded);
        }
        return waterNeeded;
    }


    private static double waterCooling(int groundArea, int floors, int buildingCode, double temperature, int water) {
        if (water > 0) {
            double effect = water * WATER_COEFFICIENT;
            return (getBuildingEnergy(buildingCode, groundArea, floors, temperature) - effect) / getBuildingCapacity(buildingCode, groundArea, floors);
//            return (((groundArea*floors*temperature*1.1) - (water*WATER_COEFFICIENT))/(groundArea*floors*1.1));
        } else
            throw new RuntimeException("WTF water=" + water);
    }

    //    public static void main(String[] args) {
//        WaterCoolingEstimator estimator = new WaterCoolingEstimator();
//        for (int i = 10; i < 100; i++) {
//            System.out.println("c=" + (1000 * i) + "  " + estimator.getWaterNeeded(100, 1, 0, 10 * i, 30));
//        }
//    }
    public static int waterNeededToExtinguish(MrlBuilding building) {
        return getWaterNeeded(building.getSelfBuilding().getGroundArea(), building.getSelfBuilding().getFloors(),
                building.getSelfBuilding().getBuildingCode(), building.getEstimatedTemperature(), 20);
    }

}
