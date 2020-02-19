package AUR.util.knd;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import viewer.K_ScreenTransform;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class AURFireZonesCalculator {

	public AURWorldGraph wsg = null;
	private int lastUpdateTime = -1;
	
	public AURFireZonesCalculator(AURWorldGraph wsg) {
		this.wsg = wsg;
	}
	
	public void update() {
		if(this.lastUpdateTime == wsg.ai.getTime()) {
			return;
		}
		_update();
		this.lastUpdateTime = wsg.ai.getTime();
	}
	
	private void _update() {
//		long t = System.currentTimeMillis();
		
		this.zones = new ArrayList<>();
		
		
		ArrayList<AURAreaGraph> fires = new ArrayList<>();
		
		for(AURAreaGraph ag : this.wsg.areas.values()) {
			ag.vis = false;
			if(ag.isBuilding() == false) {
				continue;
			}
			ag.getBuilding().fireSimBuilding.fireZone = null;
			if(ag.getBuilding().fireSimBuilding.isOnFire() == false) {
				continue;
			}
			fires.add(ag);
		}
		
		for(AURAreaGraph ag : fires) {
			if(ag.vis == true) {
				continue;
			}
			
			AURFireZone zone = new AURFireZone(this.wsg);
			zones.add(zone);
			Queue<AURAreaGraph> qu = new LinkedList<>();
			qu.add(ag);
			ag.vis = true;
			
			while (qu.isEmpty() == false) {
				AURAreaGraph buildingAreaGraph = qu.poll();
				zone.add(buildingAreaGraph.getBuilding());
				buildingAreaGraph.getBuilding().fireSimBuilding.fireZone = zone;
				for(AURBuilding b : buildingAreaGraph.getCloseBuildings()) {
					if(b.ag.vis == false && (b.fireSimBuilding.isOnFire() || b.fireSimBuilding.getEstimatedFieryness() == 8)) {
						b.ag.vis = true;
						qu.add(b.ag);
					}
				}
			}
			
		}
		
//		System.out.println("fire zone update: " + (System.currentTimeMillis() - t) + " ms");
	}
	
	
	
	
	public ArrayList<AURFireZone> zones = null;
	
	public void paint(Graphics2D g2, K_ScreenTransform kst) {
		this.update();
		for(AURFireZone zone : this.zones) {
			zone.paint(g2, kst);
		}
	}
}
