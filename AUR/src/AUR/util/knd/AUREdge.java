package AUR.util.knd;

public class AUREdge {

	public AURNode A;
	public AURNode B;
	public int weight = 0;
	public AURAreaGraph areaGraph;

	public AUREdge(AURNode A, AURNode B, int weight, AURAreaGraph areaGraph) {
		this.A = A;
		this.B = B;
		this.weight = weight;
		this.areaGraph = areaGraph;
	}

	public AURNode nextNode(AURNode from) {
		if (from == A) {
			return B;
		} else if (from == B) {
			return A;
		}
		return null;
	}

	public AURAreaGraph getNextAreaGraph(AURNode fromNode) {
		AURNode toNode = nextNode(fromNode);
		if (toNode.ownerArea1 == areaGraph) {
			return toNode.ownerArea2;
		}
		return toNode.ownerArea1;
	}

	public double getPriority() {
		double result = 0;
		AURAreaGraph ag = areaGraph;
		double w = Math.max(weight / 100, 10);
		if (ag.longTimeNoSee() && ag.hasBlockade()) {
			w *= 2;
		}
		result += w;

		boolean isSmallOrExtraSmall = (ag.isExtraSmall() || ag.isSmall());
		
		if(isSmallOrExtraSmall == true && ag.isAlmostConvex() == false) {
			result += w * 2;
		}
		
		if(ag.isExtraSmall()) {
			result += w * 10;
		}
		
		AURBuilding b = ag.getBuilding();
		if(b != null) {
			result += w * 10;	
		}
		if(b != null && b.fireSimBuilding.inflammable()) {
			if(b.getFieryness() != 8) {
				result += Math.max(w, 25) * 200;
			}
		}
		
		if(isSmallOrExtraSmall && ag.isBuildingNeighbour() == true) {
			result += w * 40;
		}
		
		result += 3 * (1 - ((double) Math.min(500, ag.noSeeTime()) / 500)) * w;
		
		if(ag.isPassed()) {
			result *= 1.1;
		}
		
		return result;
	}
	
	public double getNoBlockadePriority() {
		double result = 0;
		AURAreaGraph ag = areaGraph;
		double w = Math.max(weight / 100, 10);
		if (ag.longTimeNoSee() && ag.hasBlockade()) {
			w *= 2;
		}
		result += w;

		boolean isSmallOrExtraSmall = (ag.isExtraSmall() || ag.isSmall());

		if (isSmallOrExtraSmall == true && ag.isAlmostConvex() == false) {
			result += w * 2;
		}

		if (ag.isExtraSmall()) {
			result += w * 10;
		}

		AURBuilding b = ag.getBuilding();
		if (b != null) {
			result += w * 10;
		}
		if (b != null && b.fireSimBuilding.inflammable()) {
			if (b.getFieryness() != 8) {
				result += Math.max(w, 25) * 200;
			}
		}

		if (isSmallOrExtraSmall && ag.isBuildingNeighbour() == true) {
			result += w * 40;
		}

		result += 3 * (1 - ((double) Math.min(500, ag.noSeeTime()) / 500)) * w;

		switch (this.areaGraph.wsg.ai.me().getStandardURN()) {
			case FIRE_BRIGADE:
			case AMBULANCE_TEAM: {
				result *= 1.1;
			}
			case POLICE_FORCE: {
				result *= 1.65;
			}
		}
		
		return result;
	}
	
}
