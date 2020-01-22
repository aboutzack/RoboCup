package mrl_2019.complex.firebrigade;

import mrl_2019.MRLConstants;
import mrl_2019.world.entity.MrlBuilding;
import org.uncommons.maths.number.NumberGenerator;
import org.uncommons.maths.random.GaussianGenerator;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Simulator {
    private MrlFireBrigadeWorld world;

    public static float GAMMA = 0.5f;
    public static float WATER_COEFFICIENT = 20f;

    //    private static float PEYMAN_VALUE = 45f;
    private static double HUGE_MAP_MIN_SIZE = 650000;
    private static final float CONSUME_COEFFICIENT_FOR_HUGE_MAP = 0.91f; // for huge map
    private static final float CONSUME_COEFFICIENT_FOR_NORMAL_MAP = 0.97f; // for others
    private static final float CONSUME_COEFFICIENT_FOR_CL = 0.85f; // for communication less
    private float mrlConsumeCoefficient = 0.95f; // default

    NumberGenerator<Double> burnRate;


    public Simulator(MrlFireBrigadeWorld world) {
        this.world = world;
//        Rnd.setSeed((long) 23);
        java.util.Random random = new java.util.Random((long) 23);
        burnRate = new GaussianGenerator(0.15, 0.02, random);

        if (world.getBounds().getWidth() > HUGE_MAP_MIN_SIZE && world.getBounds().getHeight() > HUGE_MAP_MIN_SIZE) {
            mrlConsumeCoefficient = CONSUME_COEFFICIENT_FOR_HUGE_MAP;
        } else {
            mrlConsumeCoefficient = CONSUME_COEFFICIENT_FOR_NORMAL_MAP;
        }
        if (world.isCommunicationLess()) {
            mrlConsumeCoefficient *= CONSUME_COEFFICIENT_FOR_CL;
        }

        if (MRLConstants.DEBUG_FIRE_BRIGADE) {
            DecimalFormat df = new DecimalFormat("#0.0");
            System.out.println(" worldWidth = " + df.format(world.getBounds().getWidth()) + " worldHeight = " + df.format(world.getBounds().getHeight()) + "  consumeCoef= " + mrlConsumeCoefficient);
        }
    }

    /**
     * this method update building fuel and energy.
     * and get new fieriness and temperature like main fireSimulator.
     */
    public void update() {
        burn();
        cool();
//        exchangeBuilding();
//        cool();
    }

    private void burn() {
        double burnRate = this.burnRate.nextValue();
        for (MrlBuilding b : world.getMrlBuildings()) {
            if (b.getEstimatedTemperature() >= b.getIgnitionPoint() && b.getFuel() > 0.0f && b.isFlammable()) {
                float consumed = b.getConsume(burnRate) * mrlConsumeCoefficient;
                if (consumed > b.getFuel()) {
                    consumed = b.getFuel();
                }
                b.setEnergy(b.getEnergy() + consumed);
                b.setFuel(b.getFuel() - consumed);
                b.setPrevBurned(consumed);
            } else {
                b.setPrevBurned(0.0f);
            }
        }
    }

    private void exchangeBuilding() {
//        for (MrlBuilding b : world.getMrlBuildings()) {
//            exchangeWithAir(b);
//        }
        Map<MrlBuilding, Double> radiation = new HashMap<MrlBuilding, Double>();
        for (MrlBuilding b : world.getMrlBuildings()) {
            exchangeWithAir(b);
            double radEn = b.getRadiationEnergy();
            radiation.put(b, radEn);
        }
        for (MrlBuilding b : world.getMrlBuildings()) {
            double radEn = radiation.get(b);
            List<MrlBuilding> bs = b.getConnectedBuilding();
            List<Float> vs = b.getConnectedValues();

            for (int c = 0; c < vs.size(); c++) {
                double oldEnergy = bs.get(c).getEnergy();
                double connectionValue = vs.get(c);
                double a = radEn * connectionValue;
                double sum = oldEnergy + a;
                bs.get(c).setEnergy(sum);
            }
            b.setEnergy(b.getEnergy() - radEn);
        }
    }

    private void exchangeWithAir(MrlBuilding b) {
// Give/take heat to/from air cells

        double oldTemperature = b.getEstimatedTemperature();
        double oldEnergy = b.getEnergy();

        if (oldTemperature > 100) {
            b.setEnergy(oldEnergy - (oldEnergy * 0.042));
        }
    }

    private void cool() {
        for (MrlBuilding building : world.getMrlBuildings()) {
            waterCooling(building);
        }
    }

    private void waterCooling(MrlBuilding b) {
        double lWATER_COEFFICIENT = (b.getEstimatedFieryness() > 0 && b.getEstimatedFieryness() < 4 ? WATER_COEFFICIENT : WATER_COEFFICIENT * GAMMA);
        if (b.getWaterQuantity() > 0) {
            double dE = b.getEstimatedTemperature() * b.getCapacity();
            if (dE <= 0) {
                return;
            }
            double effect = b.getWaterQuantity() * lWATER_COEFFICIENT;
            int consumed = b.getWaterQuantity();
            if (effect > dE) {
                double pc = 1 - ((effect - dE) / effect);
                effect *= pc;
                consumed *= pc;
            }
            b.setWaterQuantity(b.getWaterQuantity() - consumed);
            b.setEnergy(b.getEnergy() - effect);
        }
    }
}
