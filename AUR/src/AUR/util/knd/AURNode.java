package AUR.util.knd;

import java.util.ArrayList;
import AUR.util.FibonacciHeap.Entry;

/**
 *
 * @author Alireza Kandeh - 2017
 */

public class AURNode {

	public int x;
	public int y;
	public ArrayList<AUREdge> edges = null;
	public ArrayList<AUREdgeToStand> edgesToPerceptAndExtinguish = null;
	public ArrayList<AUREdgeToStand> edgesToSeeInside = null;
	public AURAreaGraph ownerArea1 = null;
	public AURAreaGraph ownerArea2 = null;
	public int cost;
	public AURNode pre = null;

	public Entry<AURNode> pQueEntry = null;
	
	public boolean vis = false;

	public AURNode(int x, int y, AURAreaGraph ownerArea1, AURAreaGraph ownerArea2) {
		edges = new ArrayList<AUREdge>();
		this.ownerArea1 = ownerArea1;
		this.ownerArea2 = ownerArea2;
		this.x = x;
		this.y = y;
	}
	
	public AURAreaGraph getPreAreaGraph() {
		if (pre == null) {
			return null;
		}
		if (this.ownerArea1 == pre.ownerArea1) {
			return this.ownerArea1;
		}
		if (this.ownerArea1 == pre.ownerArea2) {
			return this.ownerArea1;
		}
		if (this.ownerArea2 == pre.ownerArea1) {
			return this.ownerArea2;
		}
		if (this.ownerArea2 == pre.ownerArea2) {
			return this.ownerArea2;
		}
		return null;
	}
	
}
