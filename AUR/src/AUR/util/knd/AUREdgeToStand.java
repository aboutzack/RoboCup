package AUR.util.knd;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class AUREdgeToStand {
	
	public AURAreaGraph toSeeAreaGraph = null;
	public AURAreaGraph ownerAg = null;
	public int weight = 0;
	public int standCost = 0;
	public AURNode fromNode = null;
	
	public int standX = 0;
	public int standY = 0;

	public AUREdgeToStand(AURAreaGraph ownerAg, AURAreaGraph toSeeAreaGraph, int weight, AURNode fromNode, int standX, int standY) {
		this.ownerAg = ownerAg;
		this.toSeeAreaGraph = toSeeAreaGraph;
		this.weight = weight;
		this.standCost = 0 + weight;
		this.fromNode = fromNode;
		this.standX = standX;
		this.standY = standY;
	}

}
