package AUR.util.knd;

import java.util.ArrayList;
import java.util.List;
import adf.agent.action.common.*;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.module.AbstractModule;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Alireza Kandeh - 2017 & 2018
 */

public class AURWalkWatcher extends AbstractModule {

	AgentInfo ai = null;
	AURRandomDirectSelector randomDirectSelector = null;
	private int ignoreUntil = 0;

	public AURWalkWatcher(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager,
			DevelopData developData) {
		super(ai, wi, si, moduleManager, developData);
		this.ai = ai;
		randomDirectSelector = new AURRandomDirectSelector(ai, wi);
		this.ignoreUntil = si.getKernelAgentsIgnoreuntil();
	}

	@Override
	public AbstractModule calc() {
		return null;
	}

	class Step {

		int fromID;
		double fromX = 0;
		double fromY = 0;
		double destX = 0;
		double destY = 0;
		boolean destXYDefined = false;

		public Step(int fromID, int destX, int destY, boolean destXYDefined) {
			this.fromID = fromID;
			this.fromX = ai.getX();
			this.fromY = ai.getY();
			this.destX = destX;
			this.destY = destY;
			this.destXYDefined = destXYDefined;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || ((obj instanceof Step) == false)) {
				return false;
			}
			Step step = (Step) obj;
			double dist = AURGeoUtil.dist(step.fromX, step.fromY, this.fromX, this.fromY);
			return dist < 750;
		}
	}

	public ArrayList<Step> recentSteps = new ArrayList<>();

	private void add(ActionMove actMove) {
		Step step = new Step(
			ai.getPosition().getValue(),
			actMove.getPosX(),
			actMove.getPosY(),
			actMove.getUsePosition()
		);
		recentSteps.add(step);
		if (recentSteps.size() > 4) {
			recentSteps.remove(0);
		}
	}

	private boolean anyProblem() {
		int size = recentSteps.size();
		if (size < 2) {
			return false;
		}
		Step last = recentSteps.get(size - 1);
		Step beforeLast = recentSteps.get(size - 2);

		
		if(last.destXYDefined) {
			double dist = AURGeoUtil.dist(this.ai.getX(), this.ai.getY(), last.destX, last.destY);
			if(dist < 40) {
				return false;
			}
		}
		
		if (last.equals(beforeLast)) {
			return true;
		}
		return false;
	}

	public ActionMove check(ActionMove moveAction) {
		if (moveAction == null || moveAction.getPath() == null || moveAction.getPath().size() == 0) {
			return null;
		}

		/*System.out.print(moveAction.getPath().get(0).getValue() + ": ");
		for(int i = 0; i < moveAction.getPath().size(); i++) {
			System.out.print(moveAction.getPath().get(i).getValue() + " > ");
		}
		System.out.print(moveAction.getPosX() + ", " + moveAction.getPosY());
		System.out.println();*/
		 
		if (ai.getTime() < ignoreUntil) {
			return moveAction;
		}
		randomDirectSelector.update();
		add(moveAction);
		if (anyProblem()) {
			System.out.println("problem");
			randomDirectSelector.generate();
			List<EntityID> path = new ArrayList<>();
			path.add(ai.getPosition());
			return new ActionMove(path, (int) (randomDirectSelector.generatedPoint.getX()),
					(int) (randomDirectSelector.generatedPoint.getY()));

		} else {
			return moveAction;
		}
	}
}
