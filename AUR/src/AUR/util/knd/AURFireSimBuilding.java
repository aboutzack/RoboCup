package AUR.util.knd;

import AUR.util.ResqFireGeometry;
import adf.agent.precompute.PrecomputeData;
import firesimulator.simulator.Simulator;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.Scanner;
import org.uncommons.maths.random.GaussianGenerator;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;
import viewer.K_ScreenTransform;

/**
 *
 * @author Alireza Kandeh - 2018
 */

public class AURFireSimBuilding {

	private double estimatedEnergy = 0;
	private ArrayList<int[]> airCells = null;
	public AURWorldGraph wsg = null;
	public AURAreaGraph ag = null;
	public ArrayList<AURBuildingConnection> connections = null;
	public AURBuilding building = null;
	private short vis_ = 0;
	public int floors = 1;
	private double estimatedFuel = 0;
	private boolean wasEverWatered = false;
	private double waterQuantity = 0;
//	private boolean ignite = false;
	public int lastRealFieryness = -1;
	public double lastRealTemperature = -1;
	
	public AURFireZone fireZone = null;
	
	
	public double fireProbability = 0.2;
	
	private GaussianGenerator burnRate = new GaussianGenerator(0.14, 0.025, new Random(0));
	
	public double tempVar = 0;
	
	private static Random rand = new Random(0);
	
	public boolean isOnFireZoneBorder() {
		if(this.fireZone == null) {
			return false;
		}
		if(this.fireZone.buildings.size() <= 4) {
			return true;
		}
		
		Polygon fireZonePolygon = this.fireZone.getPolygon();
		
		if(fireZonePolygon.intersects(this.ag.getOffsettedBounds(AURConstants.Misc.FIRE_ZONE_BORDER_INTERSECT_THRESHOLD))) {
			if(fireZonePolygon.contains(this.ag.getOffsettedBounds(AURConstants.Misc.FIRE_ZONE_BORDER_INTERSECT_THRESHOLD)) == false) {
				return true;
			}
		}
		return false;
	}
	
	public AURFireSimBuilding(AURBuilding building) {
		this.building = building;
		this.wsg = building.wsg;
		this.ag = building.ag;
		
		Building b = building.building;
		this.floors = 1;
		if(b.isFloorsDefined()) {
			this.floors = b.getFloors();
		}
		this.estimatedEnergy = 0;
		this.estimatedFuel = getInitialFuel();
//		this.ignite = false;

//		if(rand.nextFloat() < 0.09) {
//			this.ignite();
//		}
//		
//		if(this.building.building.getID().getValue() == 49308) {
//			this.ignite();
//		}

//		if(Math.random() < 0.5) {
//			this.wsg.wi.getObjectIDsInRectangle(cands);
//		}
	}

	public void update() {		
		if(this.building.building.isTemperatureDefined()) {
			double t = this.building.building.getTemperature();
			if(Math.abs(this.lastRealTemperature - t) > 1e-8 || building.ag.noSeeTime() == 0) {
				this.onRealTemperatureChange(t);
				onRealFierynessChange(this.lastRealFieryness);
				this.lastRealTemperature = t;
				
			}
		}
		
		if(this.building.building.isFierynessDefined()) {
			int f = this.building.building.getFieryness();
			if(f >= 4 && f <= 7) {
				this.setWasEverWatered(true);
			} else {
				if(f == 0) {
					this.setWasEverWatered(false);
				}
			}
			if(f != this.lastRealFieryness || building.ag.noSeeTime() == 0) {
				onRealFierynessChange(f);
				this.lastRealFieryness = f;
			}
		}
		

		if(getEstimatedFieryness() == 8 || this.ag.noSeeTime() <= 1) {
			this.fireProbability = 0.01;
		}

		if (isOnFire()) {
			this.fireProbability = 1;
		} else {
			this.fireProbability *= 1.002;
			
			if(this.ag.noSeeTime() < 1) {
				if (this.connections != null) {
					if (this.getEstimatedTemperature() < 1) {
						for (AURBuildingConnection bc : this.connections) {
							AURAreaGraph cAg = this.wsg.getAreaGraph(new EntityID(bc.toID));
							if (cAg != null && cAg.isBuilding() == true) {
								cAg.getBuilding().fireSimBuilding.fireProbability *= 0.5;
							}
						}
					} else {
						
						if(this.getEstimatedFieryness() == 0) {
							for (AURBuildingConnection bc : this.connections) {
								AURAreaGraph cAg = this.wsg.getAreaGraph(new EntityID(bc.toID));
								if (cAg != null && cAg.isBuilding() == true) {
									cAg.getBuilding().fireSimBuilding.fireProbability *= 2;
								}
							}
						}
						

					}

				}
			}
			



		}
		
		


		this.fireProbability = Math.min(1, this.fireProbability);
		
	}
	
	public void onRealFierynessChange(int newFieryness) {
		switch(newFieryness) {
			case 0: 
			case 1: {
				setEstimatedFuel(getInitialFuel());
				break;
			}
			case 4:
			case 5: {
				setEstimatedFuel(getInitialFuel() * 0.8);
				break;
			}
			case 2:
			case 6: {
				setEstimatedFuel(getInitialFuel() * 0.48);
				break;
			}
			case 3:
			case 7: {
				setEstimatedFuel(getInitialFuel() * 0.16);
				break;
			}
			
			default: {
				setEstimatedFuel(0);
				break;
			}
		}
	}
	
	public void onRealTemperatureChange(double newTemperature) {
		if(newTemperature >= getIgnitionPoint()) {
			setWaterQuantity(0);
		}
		this.setEstimatedEnergy(newTemperature * getCapacity());
		
		for (int[] nextCell : getAirCells()) {
			this.building.wsg.fireSimulator.airCells.getCells()[nextCell[0]][nextCell[1]][0] = (float) newTemperature;
		}
	}
	
	public void precomputeRadiation(PrecomputeData pd) {
		pd.setString("connectedBuildingsFrom_" + this.ag.area.getID(), connectionsToString(calcConnectionsAndPaint(null, null)));
	}
	
	public String connectionsToString(ArrayList<AURBuildingConnection> connections) {
		String result = "";
		for(AURBuildingConnection c : connections) {
			result += c.toID + " " + c.weight + " ";
		}
		return result;
	}
	
	public ArrayList<AURBuildingConnection> calcConnectionsAndPaint(Graphics2D g2, K_ScreenTransform kst) {
		
		boolean paint = g2 != null && kst != null;
		double maxDist = AURConstants.FireSim.MAX_RADIATION_DISTANCE;
		
		Polygon bp = this.ag.polygon;
		Rectangle bounds = bp.getBounds();
		
		bounds = new Rectangle(
				(int) (bounds.getMinX() - maxDist),
				(int) (bounds.getMinY() - maxDist),
				(int) (bounds.getWidth() + 2 * maxDist),
				(int) (bounds.getHeight() + 2 * maxDist)
		);
		
		Collection<StandardEntity> cands = this.wsg.wi.getObjectsInRectangle(
			(int) bounds.getMinX(),
			(int) bounds.getMinY(),
			(int) bounds.getMaxX(),
			(int) bounds.getMaxY()
		);
		//cands.remove(this.ag.area);
		
		ArrayList<AURFireSimBuilding> aroundBuildings = new ArrayList<>();
		
		for(StandardEntity sent : cands) {
			if(sent.getStandardURN().equals(StandardEntityURN.BUILDING) == false) {
				continue;
			}
			AURBuilding b = wsg.getAreaGraph(sent.getID()).getBuilding();
			b.fireSimBuilding.vis_ = 0;
			aroundBuildings.add(b.fireSimBuilding);
		}
		
		ArrayList<AURBuildingConnection> result = new ArrayList<>();
		
		int rays = 0;
		
		Polygon selfPolygon = this.ag.polygon;
		
		for(int i = 0; i < selfPolygon.npoints; i++) {
			ArrayList<double[]> randomOrigins = AURGeoUtil.getRandomPointsOnSegmentLine(
				selfPolygon.xpoints[i],
				selfPolygon.ypoints[i],
				selfPolygon.xpoints[(i + 1) % selfPolygon.npoints],
				selfPolygon.ypoints[(i + 1) % selfPolygon.npoints],
				AURConstants.FireSim.RADIATION_RAY_RATE
			);
			
			rays += randomOrigins.size();
			
			double rv[] = new double[2];
			
			float ray[] = new float[4];
			
			if(paint) {
				g2.setStroke(new BasicStroke(1));
			}
			
			for(double[] o : randomOrigins) {
				if(paint) {
					g2.setColor(Color.white);
					kst.fillTransformedOvalFixedRadius(g2, o[0], o[1], 2);
					g2.setColor(Color.red);
				}

				AURGeoUtil.getRandomUnitVector(rv);
				
				ray[0] = (float) o[0];
				ray[1] = (float) o[1];
				ray[2] = (float) (o[0] + rv[0] * maxDist);
				ray[3] = (float) (o[1] + rv[1] * maxDist);
				
				AURFireSimBuilding last = null;
				
				for(AURFireSimBuilding building : aroundBuildings) {
					Polygon p = building.ag.polygon;
					//boolean b = AURGeoUtil.hitRayAllEdges(building.ag.polygon , ray);
					
					for(int j = 0; j < p.npoints; j++) {
						if(building == this && i == j) {
							continue;
						}
						
						Point ip = ResqFireGeometry.intersect(
							new Point(p.xpoints[j], p.ypoints[j]),
							new Point(p.xpoints[(j + 1) % p.npoints], p.ypoints[(j + 1) % p.npoints]),
							new Point((int) ray[0], (int) ray[1]),
							new Point((int) ray[2], (int) ray[3])
						);
						if(ip != null) {
							ray[2] = (float) ip.getX();
							ray[3] = (float) ip.getY();
							last = building;
						}
					}
				}
				
				if(paint) {
					kst.drawTransformedLine(g2, ray[0], ray[1], ray[2], ray[3]);
				}
				
				if(last != null) {
					last.vis_++;
				}
			}
		}
		for(AURFireSimBuilding b : aroundBuildings) {
			if(b.vis_ > 0 && b != this) {
				result.add(new AURBuildingConnection(b.ag.area.getID().getValue(), ((float) b.vis_ / rays) / 1) );
			}
			
		}
		return result;
	}
	
	public ArrayList<AURBuildingConnection> stringToConnections(String str) {
		ArrayList<AURBuildingConnection> result = new ArrayList<>();
		Scanner scn = new Scanner(str);
		while(scn.hasNextInt()) {
			int id = scn.nextInt();
			float weight = scn.nextFloat();
			result.add(new AURBuildingConnection(id, weight));
		}
		
		return result;
	}
	
	public void resumeRadiation(PrecomputeData pd) {
		String str = pd.getString("connectedBuildingsFrom_" + this.ag.area.getID());
		if(str == null) {
			return;
		}
		this.connections = stringToConnections(str);
	}
	
	private void findAirCells() {
		airCells = new ArrayList<>();
		Polygon buildingPolygon = (Polygon) this.building.ag.area.getShape();
		Rectangle2D buildingBounds = buildingPolygon.getBounds();
		int ij[] = building.wsg.fireSimulator.airCells.getCell_ij(buildingBounds.getMinX(), buildingBounds.getMinY());
		int i0 = ij[0];
		int j0 = ij[1];
		ij = building.wsg.fireSimulator.airCells.getCell_ij(buildingBounds.getMaxX(), buildingBounds.getMaxY());
		int i1 = ij[0];
		int j1 = ij[1];
		for(int i = i0; i <= i1; i++) {
			for(int j = j0; j <= j1; j++) {
				int xy[] = this.building.wsg.fireSimulator.airCells.getCell_xy(i, j);
				if(buildingPolygon.intersects(xy[0], xy[1], this.building.wsg.fireSimulator.airCells.getCellSize(), this.building.wsg.fireSimulator.airCells.getCellSize())) {
					int[] cell = new int[] {i, j, 0};
					setAirCellPercent(building.wsg.fireSimulator, cell, building.wsg.fireSimulator.airCells.getCellSize(), buildingPolygon);
					airCells.add(cell);
				}
			}
		}
	}
	
	private void setAirCellPercent(AURFireSimulator fs, int airCell[], int airCellSize, Polygon buildingPolygon) {
		double dw = (double) airCellSize / 10;
		double dh = (double) airCellSize / 10;
		int xy[] = fs.airCells.getCell_xy(airCell[0], airCell[1]);
		int count = 0;
		double x = 0;
		double y = 0;
		for(int i = 0; i < 10; i++) {
			for(int j = 0; j < 10; j++) {
				x = xy[0] + j * dw;
				y = xy[1] + i * dh;
				if(buildingPolygon.contains(x, y)) {
					count++;
				}
			}
		}
		airCell[2] = count;
	}
	
	public double getRadiationEnergy() {
		double t = getEstimatedTemperature() + 293; // Assume ambient temperature is 293 Kelvin.
		double radEn = (t * t * t * t) * getTotalWallArea() * AURConstants.FireSim.RADIATION_COEFFICENT * AURConstants.FireSim.STEFAN_BOLTZMANN_CONSTANT;
		if (radEn == Double.NaN || radEn == Double.POSITIVE_INFINITY || radEn == Double.NEGATIVE_INFINITY) {
			radEn = Double.MAX_VALUE * 0.75;
		}
		if (radEn > getEstimatedEnergy()) {
			radEn = getEstimatedEnergy();
		}
		if(this.ag.isOnFire() == false) {
			radEn *= 0.5;
		}
//		if(getEstimatedFieryness() == 2) {
//			radEn *= 0.9;
//		}
//		if(getEstimatedFieryness() == 3) {
//			radEn *= 0.5;
//		}
		return radEn;
	}
	
	public double getConsum() {
		if (this.estimatedFuel <= 1e-8) {
			return 0;
		}
		//double r = 0.142;
		double r = burnRate.nextValue();
		float tf = (float) (getEstimatedTemperature() / 1000f);
		float lf = (float) getEstimatedFuel() / (float) getInitialFuel();
		
		float f = (float) (tf * lf * new Double(r));
		if (f < 0.005f) {
			f = 0.005f;
		}
		
//		if(this.building.building.getID().getValue() == 958) {
//			System.out.println("temp = " + getEstimatedTemperature());
//			System.out.println("br = " + r);
//			System.out.println("tf = " + tf);
//			System.out.println("lf = " + lf);
//			System.out.println("f = " + f);
//			System.out.println("c = " + (getInitialFuel()*f));
//		}
	
//		if(getEstimatedFieryness() == 2) {
//			f *= 0.9;
//		}
//		if(getEstimatedFieryness() == 3) {
//			f *= 0.5;
//		}
		if(this.ag.isOnFire() == false) {
			f *= 0.5;
		}
		return getInitialFuel() * f;
	}
		
	public boolean inflammable() {
		StandardEntityURN urn = this.building.building.getStandardURN();
		switch(urn) {
			case REFUGE: {
				return AURConstants.FireSim.REFUGE_INFLAMMABLE;
			}
			case AMBULANCE_CENTRE: {
				return AURConstants.FireSim.AMBULANCE_CENTRE_INFLAMMABLE;
			}
			case FIRE_STATION: {
				return AURConstants.FireSim.FIRE_STATION_INFLAMMABLE;
			}
			case POLICE_OFFICE: {
				return AURConstants.FireSim.POLICE_OFFICE_INFLAMMABLE;
			}
			default: {
				return true;
			}
		}
	}
	
	public void addWater(double waterQuantity) {
		this.setWaterQuantity(this.getWaterQuantity() + waterQuantity);
	}
	
	public void setWaterQuantity(double waterQuantity) {
		if(waterQuantity > 0) {
			setWasEverWatered(true);
		}
		this.waterQuantity = waterQuantity;
	}
	
	public double getWaterQuantity() {
		return this.waterQuantity;
	}
	
	public void setWasEverWatered(boolean b) {
		this.wasEverWatered = b;
	}
	
	public boolean wasEverWatered() {
		return this.wasEverWatered;
	}
	
	public void ignite() {
		setEstimatedEnergy(getCapacity() * getIgnitionPoint() * 1.5);
//		setIgnite(true);
	}
	
//	public void setIgnite(boolean i) {
//		this.ignite = i;
//	}
//	
//	public boolean getIgnite() {
//		return this.ignite;
//	}
	
	public double getIgnitionPoint() {
		switch (building.building.getBuildingCodeEnum()) {
			case STEEL: {
				return AURConstants.FireSim.STEEL_IGNITION;
			}
			case WOOD: {
				return AURConstants.FireSim.WOODEN_IGNITION;
			}
			case CONCRETE: {
				return AURConstants.FireSim.CONCRETE_IGNITION;
			}
			default: {
				return AURConstants.FireSim.CONCRETE_IGNITION;
			}
		}
	}
	
	public void setEstimatedFuel(double f) {
		this.estimatedFuel = f;
	}
	
	public double getEstimatedFuel() {
		return (double) this.estimatedFuel;
	}

	public double getInitialFuel() {
		return (double) getFuelDensity() * getVolume();
	}
	
	public double getFuelDensity() {
		switch (building.building.getBuildingCodeEnum()) {
			case STEEL: {
				return AURConstants.FireSim.STEEL_ENERGY;
			}
			case WOOD: {
				return AURConstants.FireSim.WOODEN_ENERGY;
			}
			case CONCRETE: {
				return AURConstants.FireSim.CONCRETE_ENERGY;
			}
			default: {
				return AURConstants.FireSim.CONCRETE_ENERGY;
			}
		}
	}
	
	public double getThermoCapacity() {
		switch (building.building.getBuildingCodeEnum()) {
			case STEEL: {
				return AURConstants.FireSim.STEEL_CAPACITY;
			}
			case WOOD: {
				return AURConstants.FireSim.WOODEN_CAPACITY;
			}
			case CONCRETE: {
				return AURConstants.FireSim.CONCRETE_CAPACITY;
			}
			default: {
				return AURConstants.FireSim.CONCRETE_CAPACITY;
			}
		}
	}
	
	public double getCapacity() {
		return getThermoCapacity() * getVolume();
	}
	
	public double getPerimeter() {
		return ((double) this.ag.perimeter / 1000d);
	}
	
	public double getGroundArea() {
		return ((double) this.ag.goundArea / 1000000d);
	}
	
	public double getTotalWallArea() {
		// according to the old fire simulator
		return ((double) this.ag.perimeter * AURConstants.FireSim.FLOOR_HEIGHT) / 1000d;
	}
	
	public double getVolume() {
		return ((double) this.ag.goundArea / 1000000d) * this.floors * AURConstants.FireSim.FLOOR_HEIGHT;
	}

	public ArrayList<int[]> getAirCells() {
		if(airCells == null) {
			findAirCells();
		}
		return airCells;
	}

	public double getEstimatedEnergy() {
		if(this.estimatedEnergy == Double.NaN || this.estimatedEnergy == Double.POSITIVE_INFINITY || this.estimatedEnergy == Double.NEGATIVE_INFINITY) {
			this.estimatedEnergy = Double.MAX_VALUE  * 0.75d;
		}
		return this.estimatedEnergy;
	}

	public void setEstimatedEnergy(double energy) {
		if(energy == Double.NaN || energy == Double.POSITIVE_INFINITY || energy == Double.NEGATIVE_INFINITY) {
			energy = Double.MAX_VALUE  * 0.75d;
		}
		this.estimatedEnergy = energy;
	}

	public double getEstimatedTemperature() {
		return (double) this.getEstimatedEnergy() / this.getCapacity();
	}
	
	public int getEstimatedFieryness() {
		if (inflammable() == false) {
			return 0;
		}
		if (getEstimatedTemperature() >= getIgnitionPoint()) {
			if (estimatedFuel >= getInitialFuel() * 0.66) {
				return 1;   // burning, slightly damaged
			}
			if (estimatedFuel >= getInitialFuel() * 0.33) {
				return 2;   // burning, more damaged
			}
			if (estimatedFuel > 0) {
				return 3;    // burning, severly damaged
			}
		}
		if (Math.abs(estimatedFuel - getInitialFuel()) < 1e-8) {
			if (wasEverWatered == true) {
				return 4;   // not burnt, but watered-damaged
			} else {
				return 0;   // not burnt, no water damage
			}
		}
		if (estimatedFuel >= getInitialFuel() * 0.66) {
			return 5;        // extinguished, slightly damaged
		}
		if (estimatedFuel >= getInitialFuel() * 0.33) {
			return 6;        // extinguished, more damaged
		}
		if (estimatedFuel > 0) {
			return 7;        // extinguished, severely damaged
		}
		return 8;           // completely burnt down
	}
	
	public boolean isOnFire() {
		
		if(inflammable() == false) {
			return false;
		}
		
		int f = getEstimatedFieryness();
		
		if(getEstimatedTemperature() >= getIgnitionPoint() * 0.9 && f != 8) {
			return true;
		}
		
		return f > 0 && f < 4;
	}
	
	public boolean ignoreFire() {
		if(getEstimatedFieryness() == 8) {
			return true;
		}
//		if((double) getEstimatedFuel() / getInitialFuel() < 0.1) {
//			return true;
//		}
		
		int count = 0;
		
//		for(AURBuildingConnection bc : this.connections) {
//			
//			AURAreaGraph toAg = this.wsg.getAreaGraph(new EntityID(bc.toID));
//			
//			if(toAg == null || toAg.isBuilding() == false) {
//				continue;
//			}
//			
//			AURBuilding b = toAg.getBuilding();
//			
//			if(b.fireSimBuilding.getEstimatedFieryness() == 8 || b.fireSimBuilding.isOnFire()) {
//				continue;
//			}
//			
//			count++;
//		}
//		
//		if(count < 1) {
//			return true;
//		}
		

		if(getEffectiveRadiation() < 100) {
			return true;
		}

//		if(getEffectiveRadiationDT() < 0.5) {
//			return true;
//		}

		return false;
	}
	
	public double getEffectiveRadiation() {
		double sumW = 0;
		double raEn = getRadiationEnergy();
		if(this.connections == null) {
			return raEn;
		}
		for(AURBuildingConnection bc : this.connections) {
			
			AURAreaGraph toAg = this.wsg.getAreaGraph(new EntityID(bc.toID));
			
			if(toAg == null || toAg.isBuilding() == false) {
				continue;
			}
			
			AURBuilding b = toAg.getBuilding();
			
			if(b.fireSimBuilding.getEstimatedFieryness() == 8 || b.fireSimBuilding.isOnFire()) {
				continue;
			}
			
			sumW += bc.weight;
		}
		return sumW * raEn;
	}
	
	public int getWaterNeeded() {
		if(isOnFire() == true) {
			
//			int maxWater = this.wsg.si.getFireExtinguishMaxSum();
//			int a = 0;
//			int b = maxWater;
//			double requiredTemerature = 10;
//			int w = (a + b) / 2;
//			while(a < b && w < maxWater && w > 0 && a != w && b != w) {
//				w = (a + b) / 2;
//				
//				double tempAfterCooling = temperatureAfterCooling(w);
////				System.out.println(w + ", " + tempAfterCooling);
//				if(tempAfterCooling > requiredTemerature) {
//					a = w;
//				} else {
//					b = w;
//				}
//			}
//			return (int) (w * 1.5);
			double wq = getWaterNeeded_() * 2.5;
			return (int) Math.min(wq, ag.wsg.si.getFireExtinguishMaxSum() - 1);
//			return (Math.max(ag.wsg.si.getFireExtinguishMaxSum() - 1, 1));
			
		} else {
			return 0;
		}
	}
	
	
	private double temperatureAfterCooling(double waterQuantity) {
		double lWATER_COEFFICIENT = (getEstimatedFieryness() > 0 && getEstimatedFieryness() < 4 ? AURConstants.FireSim.WATER_COEFFICIENT : AURConstants.FireSim.WATER_COEFFICIENT * AURConstants.FireSim.GAMMA);
		if (waterQuantity > 0.0) {
			double dE = getEstimatedTemperature() * getCapacity();
			if (dE <= 0) {
				return 0;
			}
			double effect = waterQuantity * lWATER_COEFFICIENT;
			if (effect > dE) {
				double pc = 1 - ((effect - dE) / effect);
				effect *= pc;
			}
			double energy = getEstimatedEnergy() - effect;
			return energy / getCapacity();
		}
		return getEstimatedTemperature();
	}
	
	
	// mrl
	private int getWaterNeeded_() {
		int waterNeeded = 0;
		double currentTemperature = getEstimatedTemperature();
		int step = 500;
		while (true) {
			currentTemperature = waterCooling(currentTemperature, step);
			waterNeeded += step;
			if (currentTemperature <= 0) {
				break;
			}
		}

		return waterNeeded;
	}

	// mrl
	private double waterCooling(double temperature, int water) {
		if (water > 0) {
			double effect = water * AURConstants.FireSim.WATER_COEFFICIENT;
			return (temperature * getCapacity() - effect) / getCapacity();
		}
		return water;
	}
	
}
