package AUR.util.knd;

import adf.agent.precompute.PrecomputeData;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Alireza Kandeh - 2017 & 2018
 */

public class AURFireSimulator {

	private AURWorldGraph wsg = null;
	public AURWorldAirCells airCells = null;
	public boolean isPrecomputedConnections = false;
	
	public AURFireSimulator(AURWorldGraph wsg) {
		this.wsg = wsg;
		this.airCells = new AURWorldAirCells(wsg);
	}
	
	public void step() {
		long t = System.currentTimeMillis();
		
		if(wsg.ai.getTime() < 2) {
			return;
		}
		
		burn();
//		System.out.println("1- step update time: " + (System.currentTimeMillis() - t));
		cool();
//		System.out.println("2- step update time: " + (System.currentTimeMillis() - t));
		updateGrid();
//		System.out.println("3- step update time: " + (System.currentTimeMillis() - t));
		exchangeBuilding();
//		System.out.println("4- step update time: " + (System.currentTimeMillis() - t));
		cool();
//		System.out.println("5- step update time: " + (System.currentTimeMillis() - t));
	}
	
	private void burn() {
		for(AURAreaGraph ag : wsg.areas.values()) {
			if(ag.isBuilding() == true) {
				AURFireSimBuilding b = ag.getBuilding().fireSimBuilding;
				if(b.getEstimatedTemperature() >= b.getIgnitionPoint() && b.getEstimatedFuel() > 0 && b.inflammable() == true) {
					double consumed = b.getConsum();
					if(consumed > b.getEstimatedFuel()) {
					    consumed = b.getEstimatedFuel();
					}
					b.setEstimatedEnergy(b.getEstimatedEnergy() + consumed);
					b.setEstimatedFuel(b.getEstimatedFuel() - consumed);
				}
			}
		}
	}
	
	private void cool() {
		for(AURAreaGraph ag : this.wsg.areas.values()) {
			if(ag.isBuilding()) {
				waterCooling(ag.getBuilding().fireSimBuilding);
			}
		}
	}
	
	private void waterCooling(AURFireSimBuilding b) {
		double lWATER_COEFFICIENT = (b.getEstimatedFieryness() > 0 && b.getEstimatedFieryness() < 4 ? AURConstants.FireSim.WATER_COEFFICIENT : AURConstants.FireSim.WATER_COEFFICIENT * AURConstants.FireSim.GAMMA);
		if (b.getWaterQuantity() > 0.0) {
			double dE = b.getEstimatedTemperature() * b.getCapacity();
			if (dE <= 0) {
				return;
			}
			double effect = b.getWaterQuantity() * lWATER_COEFFICIENT;
			int consumed = (int) b.getWaterQuantity();
			if (effect > dE) {
				double pc = 1 - ((effect - dE) / effect);
				effect *= pc;
				consumed *= pc;
			}
			
			b.setWaterQuantity(b.getWaterQuantity() - consumed);			
			b.setEstimatedEnergy(b.getEstimatedEnergy() - effect);
		}
	}
	
	private void updateGrid() {
		float[][][] airtemp = this.airCells.getCells();

		for (int x = 0; x < airtemp.length; x++) {
			for (int y = 0; y < airtemp[0].length; y++) {
				float dt = (averageTemp(x, y) - airtemp[x][y][1]);
				float change = (dt * AURConstants.FireSim.AIR_TO_AIR_COEFFICIENT * AURConstants.FireSim.TIME_STEP_LENGTH);
				
				airtemp[x][y][1] = relTemp(airtemp[x][y][0] + change);

				if (!(airtemp[x][y][1] > -Float.MAX_VALUE && airtemp[x][y][1] < Float.MAX_VALUE)) {
					airtemp[x][y][1] = Float.MAX_VALUE * 0.75f;
				}

			}
		}
		for (int x = 0; x < airtemp.length; x++) {
			for (int y = 0; y < airtemp[0].length; y++) {
				airtemp[x][y][0] = airtemp[x][y][1];
			}
		}
		
	}
	
	
	private float relTemp(float deltaT) {
		return Math.max(0, deltaT * AURConstants.FireSim.ENERGY_LOSS * AURConstants.FireSim.TIME_STEP_LENGTH);
	}

	private float averageTemp(int x, int y) {
		float rv = neighbourCellAverage(x, y) / weightSummCells(x, y);
		return rv;
	}
	
	private float neighbourCellAverage(int x, int y) {
		float total = getTempAt(x + 1, y - 1);
		total += getTempAt(x + 1, y);
		total += getTempAt(x + 1, y + 1);
		total += getTempAt(x, y - 1);
		total += getTempAt(x, y + 1);
		total += getTempAt(x - 1, y - 1);
		total += getTempAt(x - 1, y);
		total += getTempAt(x - 1, y + 1);
		return total * AURConstants.FireSim.WEIGHT_GRID;
	}
	
	private float weightSummCells(int x, int y) {
		return 8 * AURConstants.FireSim.WEIGHT_GRID;
	}

	protected float getTempAt(int x, int y) {
		if (x < 0 || y < 0 || x >= this.airCells.getCells().length || y >= this.airCells.getCells()[0].length) {
			return 0;
		}
		return this.airCells.getCells()[x][y][0];
	}

	
	private void exchangeBuilding() {
		for (AURAreaGraph ag : wsg.areas.values()) {
			if(ag.isBuilding()) {
				AURFireSimBuilding b = ag.getBuilding().fireSimBuilding;
				exchangeWithAir(b);
			}
		}
		
		for (AURAreaGraph ag : wsg.areas.values()) {
			if(ag.isBuilding()) {
				AURFireSimBuilding b = ag.getBuilding().fireSimBuilding;
				b.tempVar = 0;
				//ag.isOnFire() || ag.getBuilding().fireSimBuilding.isOnFire()
				int ef = ag.getBuilding().fireSimBuilding.getEstimatedFieryness();
				if(ag.isOnFire() || ag.getBuilding().fireSimBuilding.isOnFire()) { // || ag.getBuilding().fireSimBuilding.isOnFire()
					b.tempVar = b.getRadiationEnergy();
				}
			}
		}
		for (AURAreaGraph ag : wsg.areas.values()) {
			if(ag.isBuilding()) {
				AURFireSimBuilding b = ag.getBuilding().fireSimBuilding;
				double radEn = b.tempVar;
				
				
				if(b.connections != null) {
					for(AURBuildingConnection bc : b.connections) {

						AURFireSimBuilding cb = wsg.getAreaGraph(new EntityID(bc.toID)).getBuilding().fireSimBuilding;
						double oldEnergy = cb.getEstimatedEnergy();
						double connectionValue = bc.weight;
						double a = radEn * connectionValue;
						double sum = oldEnergy + a;
						cb.setEstimatedEnergy(sum);
					}
				}
				
				b.setEstimatedEnergy(b.getEstimatedEnergy() - radEn);
			}
		}
	}
	
	private void exchangeWithAir(AURFireSimBuilding building) {
		
		float oldEnergy = (float) building.getEstimatedEnergy();
		float energyDelta = 0;

		for (int[] nextCell : building.getAirCells()) {
			int cellX = nextCell[0];
			int cellY = nextCell[1];
			float cellCover = (float) ((float) nextCell[2] / 100.0);
			float cellTemp = this.airCells.getCells()[cellX][cellY][0];
			float dT = cellTemp - (float) building.getEstimatedTemperature();
			float energyTransferToBuilding = dT * AURConstants.FireSim.AIR_TO_BUILDING_COEFFICIENT * AURConstants.FireSim.TIME_STEP_LENGTH * cellCover * AURConstants.FireSim.WORLD_AIR_CELL_SIZE;
			
//			if(building.ag.isOnFire() == false) {
//				energyTransferToBuilding *= 0.9;
//			}


			energyDelta += energyTransferToBuilding;
			float newCellTemp = cellTemp - energyTransferToBuilding / (AURConstants.FireSim.AIR_CELL_HEAT_CAPACITY * AURConstants.FireSim.WORLD_AIR_CELL_SIZE);

//			if(dT < 0) {
//				if(building.ag.isOnFire()) {
//					this.airCells.getCells()[cellX][cellY][0] = newCellTemp;
//				}
//			} else {
//				this.airCells.getCells()[cellX][cellY][0] = newCellTemp;
//			}
			
			this.airCells.getCells()[cellX][cellY][0] = newCellTemp;
			
			
		}
		building.setEstimatedEnergy(oldEnergy + energyDelta);
	}
	
	public void precompute(PrecomputeData pd) {
		for (AURAreaGraph ag : wsg.areas.values()) {
			if (ag.isBuilding()) {
				ag.getBuilding().fireSimBuilding.precomputeRadiation(pd);
			}
		}
		pd.setBoolean("radiation", true);
	}

	public void resume(PrecomputeData pd) {
		Boolean b = pd.getBoolean("radiation");
		if (b == null || b == false) {

			return;
		}
		this.isPrecomputedConnections = true;
		for (AURAreaGraph ag : wsg.areas.values()) {
			if (ag.isBuilding()) {
				ag.getBuilding().fireSimBuilding.resumeRadiation(pd);
			}
		}
	}

}
