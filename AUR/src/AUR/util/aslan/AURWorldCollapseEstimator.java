package AUR.util.aslan;

import AUR.util.knd.AURConstants;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.uncommons.maths.Maths;
import org.uncommons.maths.number.NumberGenerator;
import org.uncommons.maths.random.GaussianGenerator;
import org.uncommons.maths.random.MersenneTwisterRNG;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityConstants;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Amir Aslan Aslani - Mar 2018
 */
public class AURWorldCollapseEstimator {
        
        private Map<StandardEntityConstants.BuildingCode, CollapseStats> stats = new EnumMap<>(StandardEntityConstants.BuildingCode.class);
        public HashMap<EntityID, AURBuildingCollapseEstimator> map = new HashMap<>();
        private Random random = new MersenneTwisterRNG();

        public AURWorldCollapseEstimator(Collection<StandardEntity> buildings) {
                
		for (StandardEntityConstants.BuildingCode code : StandardEntityConstants.BuildingCode.values()) {
			stats.put(code, new CollapseStats(code));
		}
                
                for(StandardEntity b : buildings){
                        StandardEntityConstants.BuildingCode code = ((Building) b).getBuildingCodeEnum();
			int damage = code == null ? 0 : stats.get(code).damage();
			damage = Maths.restrictRange(damage, 0, (int) AURConstants.CollapseEstimator.MAX_COLLAPSE);
                        
                        map.put(b.getID(), new AURBuildingCollapseEstimator(((Building) b), random, damage));
                }
                
        }
        
        private class CollapseStats {
		private double pDestroyed;
		private double pSevere;
		private double pModerate;
		private double pSlight;
                
                private NumberGenerator<Double> destroyed = new GaussianGenerator(AURConstants.CollapseEstimator.DESTROYED_MEAN,AURConstants.CollapseEstimator.DESTROYED_SD,random);
                private NumberGenerator<Double> severe = new GaussianGenerator(AURConstants.CollapseEstimator.SEVERE_MEAN,AURConstants.CollapseEstimator.SEVERE_SD,random);
                private NumberGenerator<Double> moderate = new GaussianGenerator(AURConstants.CollapseEstimator.MODERATE_MEAN,AURConstants.CollapseEstimator.MODERATE_SD,random);
                private NumberGenerator<Double> slight = new GaussianGenerator(AURConstants.CollapseEstimator.SLIGHT_MEAN,AURConstants.CollapseEstimator.SLIGHT_SD,random);

		CollapseStats(StandardEntityConstants.BuildingCode code) {
                        if(code.equals(code.WOOD)){
                                pDestroyed = AURConstants.CollapseEstimator.WOOD_DESTROYED;
                                pSevere = AURConstants.CollapseEstimator.WOOD_SEVERE;
                                pModerate = AURConstants.CollapseEstimator.WOOD_MODERATE;
                                pSlight = AURConstants.CollapseEstimator.WOOD_SLIGHT;
                        }
                        if(code.equals(code.STEEL)){
                                pDestroyed = AURConstants.CollapseEstimator.STEEL_DESTROYED;
                                pSevere = AURConstants.CollapseEstimator.STEEL_SEVERE;
                                pModerate = AURConstants.CollapseEstimator.STEEL_MODERATE;
                                pSlight = AURConstants.CollapseEstimator.STEEL_SLIGHT;
                        }
                        if(code.equals(code.CONCRETE)){
                                pDestroyed = AURConstants.CollapseEstimator.CONCRETE_DESTROYED;
                                pSevere = AURConstants.CollapseEstimator.CONCRETE_SEVERE;
                                pModerate = AURConstants.CollapseEstimator.CONCRETE_MODERATE;
                                pSlight = AURConstants.CollapseEstimator.CONCRETE_SLIGHT;
                        }
		}

		int damage() {
			double d = random.nextDouble();
			if (d < pDestroyed) {
				return destroyed.nextValue().intValue();
			}
			if (d < pSevere) {
				return severe.nextValue().intValue();
			}
			if (d < pModerate) {
				return moderate.nextValue().intValue();
			}
			if (d < pSlight) {
				return slight.nextValue().intValue();
			}
			return 0;
		}
	}
}
