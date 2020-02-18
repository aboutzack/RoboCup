package AUR.util.aslan;

import AUR.util.knd.AURConstants;
import java.util.Random;
import org.uncommons.maths.number.NumberGenerator;
import org.uncommons.maths.random.ContinuousUniformGenerator;
import rescuecore2.standard.entities.Building;

/**
 *
 * @author Amir Aslan Aslani - Mar 2018
 */
public class AURBuildingCollapseEstimator {
        
        
        public final Building building;
        public double collapsedRatio = 0;
        public final double floorHeight = AURConstants.CollapseEstimator.FLOOR_HEIGHT * 1000;
        
        public NumberGenerator<Double> extent;
        public int estimatedDamage = 0;

        public AURBuildingCollapseEstimator(Building building, Random rnd, int estimatedDamage) {
                this(building, rnd);
                this.estimatedDamage = estimatedDamage;
        }

        public AURBuildingCollapseEstimator(Building building, Random rnd) {
                this.extent = new ContinuousUniformGenerator(
                        AURConstants.CollapseEstimator.WALL_COLLAPSE_EXTENT_MIN,
                        AURConstants.CollapseEstimator.WALL_COLLAPSE_EXTENT_MAX,
//                        new MersenneTwisterRNG(new BigInteger("3", 16).toByteArray())
                        rnd
                );
                
                this.building = building;
        }
        
        public double d(){
                return getRemainingFloors() * (getDamage() / AURConstants.CollapseEstimator.MAX_COLLAPSE) * extent.nextValue();
        }
        
        public double getStaticD(double damage){
                return getRemainingFloors() * (damage / AURConstants.CollapseEstimator.MAX_COLLAPSE) * (AURConstants.CollapseEstimator.WALL_COLLAPSE_EXTENT_MIN + AURConstants.CollapseEstimator.WALL_COLLAPSE_EXTENT_MAX) * 2 / 3;
        }

        public double getRemainingFloors() {
                return floorHeight * building.getFloors() * (1 - getCollapsedRatio());
        }

        public double getDamage() {
                return building.isBrokennessDefined() ? building.getBrokenness() : 0;
        }

        public double getCollapsedRatio() {
                return collapsedRatio;
        }
        
        public void setAftershockCollapsedRatio(){
                collapsedRatio += d() / getTotalCollapse(floorHeight);
        }
        
        public double getTotalCollapse(double floorHeight){
		return floorHeight * building.getFloors();
	}
}
