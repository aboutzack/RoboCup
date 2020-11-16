package CSU_Yunlu_2020.module.complex.fb.targetSelection;

import CSU_Yunlu_2020.module.algorithm.fb.FireCluster;
import CSU_Yunlu_2020.module.complex.fb.tools.DirectionManager;
import CSU_Yunlu_2020.module.complex.fb.tools.FbUtilities;
import CSU_Yunlu_2020.world.CSUFireBrigadeWorld;
import CSU_Yunlu_2020.world.object.CSUBuilding;
import rescuecore2.standard.entities.Human;

public abstract class TargetSelector implements IFireBrigadeTargetSelector{

	protected CSUFireBrigadeWorld world;
	protected Human selfHuman;
	protected FbUtilities fbUtilities;
	protected DirectionManager directionManager;

	protected CSUBuilding target;
	protected CSUBuilding lastTarget;
	protected FireCluster targetCluster;

	protected TargetSelector(CSUFireBrigadeWorld world) {
		this.world = world;
		this.fbUtilities = new FbUtilities(world);
		this.directionManager = new DirectionManager(world);
		this.selfHuman = world.getSelfHuman();
	}
}
