package AUR.util.knd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Alireza Kandeh - 2017 & 2018
 */

public class AURFireSearchValueSetter {

	public AURConvexHull convexHullInstance = new AURConvexHull();
	public ArrayList<AURAreaGraphValue> points = new ArrayList<AURAreaGraphValue>();
	public AURFireSimulator fireSimulatorInstance = null;

	public void calc(AURWorldGraph wsg, ArrayList<AURAreaGraphValue> points, Collection<EntityID> initialCluster, EntityID lastTarget) {

		// long t = System.currentTimeMillis();

		wsg.updateInfo(null);
		wsg.KStar(wsg.ai.getPosition());
                
		this.fireSimulatorInstance = wsg.fireSimulator;

		this.points.clear();
		this.points.addAll(points);
		for (AURAreaGraphValue p : this.points) {
			p.value = 0;
		}

		//add_Fieryness(this.points, 1.5);
		//add_EstimatedFieryness(this.points, 1.5);
		
		add_GasStation(this.points, 0.55);
                calc_Capacity(this.points, 0.55);
		add_TravelCost(this.points, 1.9);
		
		//mul_Color(wsg, this.points, 1.1);
		add_FireProbability(this.points, 1.6);
                
		//calc_noName(this.points, 1.0);
		//mul_Color(wsg, this.points, 1.1);
		add_NoSeeTime(this.points, 1.3);
		mul_Safety(this.points, 2);
		mul_InitialCluster(this.points, initialCluster, 2.0);
		mul_soClose(this.points, 0.4);
		mul_lastTarget(lastTarget, this.points, 1.8);
		Collections.sort(this.points, new Comparator<AURAreaGraphValue>() {
			@Override
			public int compare(AURAreaGraphValue o1, AURAreaGraphValue o2) {
				return Double.compare(o2.value, o1.value);
			}
		});
	}
	
	private void mul_Safety(ArrayList<AURAreaGraphValue> points, double coefficient) {
		for (AURAreaGraphValue p : points) {
			if (p.ag.getBuilding().isSafePerceptible()) {
				p.value *= coefficient;
			}
		}
	}
	
	public void mul_lastTarget(EntityID lastTarget, ArrayList<AURAreaGraphValue> points, double coefficient) {
		for (AURAreaGraphValue p : points) {
			if(p.ag.area.getID().equals(lastTarget)) {
				p.value *= 1.08 * coefficient;
			}
		}
	}
	
	public void mul_Color(AURWorldGraph wsg, ArrayList<AURAreaGraphValue> points, double coefficient) {
		int agentColor = wsg.getAgentColor();

		for (AURAreaGraphValue p : points) {
			if (p.ag.color == agentColor) {
				p.value *= (wsg.colorCoe[p.ag.color][agentColor]) * coefficient;
			}

		}
	}
	
	private void calc_noName(ArrayList<AURAreaGraphValue> points, double coefficient) {
//		double max = 0;
//		for (AURValuePoint p : points) {
//			p.temp_value = p.areaGraph.countUnburntsInGrid();
//			if (p.temp_value > max) {
//				max = p.temp_value;
//			}
//		}
//
//		if (max > 0) {
//			for (AURValuePoint p : points) {
//				p.value += ((p.temp_value / max)) * coefficient;
//			}
//		}
	}

	public void calcNoBlockade(AURWorldGraph wsg, ArrayList<AURAreaGraphValue> points, Collection<EntityID> initialCluster) {
		wsg.updateInfo(null);
		wsg.KStarNoBlockade(wsg.ai.getPosition());

		this.points.clear();
		this.points.addAll(points);
		for (AURAreaGraphValue p : this.points) {
			p.value = 0;
		}

		// calc_Capacity(this.points, 0.69);

		add_Fieryness(this.points, 1.5);
		add_GasStation(this.points, 0.55);
		// add_CloseFire(this.points, 1.3);

		// add_NoSeeTime(this.points, 1.1);
		add_NoBlockadeTravelCost(this.points, 1.5);
		
		mul_Color(wsg, this.points, 1.1);
		add_FireProbability(this.points, 1.9);
		// calc_ConvexHull(this.points, 0.5);
		calc_noName(this.points, 1.0);
		mul_InitialCluster(this.points, initialCluster, 1.1);
		mul_Color(wsg, this.points, 1.1);
		add_NoSeeTime(this.points, 1.08);
		mul_soClose(this.points, 0.5);
		Collections.sort(this.points, new Comparator<AURAreaGraphValue>() {
			@Override
			public int compare(AURAreaGraphValue o1, AURAreaGraphValue o2) {
				return Double.compare(o2.value, o1.value);
			}
		});
	}

	private void add_NoSeeTime(ArrayList<AURAreaGraphValue> points, double coefficient) {
		double max_ = 0;
		for (AURAreaGraphValue p : points) {
			p.temp_value = p.ag.noSeeTime();
			if (p.temp_value > max_) {
				max_ = p.temp_value;
			}
		}
		if (max_ > 0.5) {
			for (AURAreaGraphValue p : points) {
				p.temp_value /= max_;
			}
		}
		for (AURAreaGraphValue p : points) {
			p.value += p.temp_value * coefficient;
		}
	}

	private void add_Fieryness(ArrayList<AURAreaGraphValue> points, double coefficient) {
		for (AURAreaGraphValue p : points) {
			Building b = (Building) (p.ag.area);
			if (b.isFierynessDefined() == false) {
				// p.value += 0.5 * coefficient;
				continue;
			}
			switch (b.getFierynessEnum()) {
			case HEATING: {
				p.value += 1 * coefficient;
				break;
			}
			case BURNING: {
				p.value += 0.5 * coefficient;
				break;
			}
			case INFERNO: {
				p.value += 0.1 * coefficient;
				break;
			}
			}

		}
	}
	
	private void add_EstimatedFieryness(ArrayList<AURAreaGraphValue> points, double coefficient) {
		for (AURAreaGraphValue p : points) {
			if(p.ag.isBuilding() == false) {
				continue;
			}
			switch (p.ag.getBuilding().fireSimBuilding.getEstimatedFieryness()) {
				case 1: {
					p.value += 1 * coefficient;
					break;
				}
				case 2: {
					p.value += 0.5 * coefficient;
					break;
				}
				case 3: {
					p.value += 0.1 * coefficient;
					break;
				}
			}

		}
	}
	
	private void calc_Capacity(ArrayList<AURAreaGraphValue> points, double coefficient) {
		for (AURAreaGraphValue p : points) {
			
			if(p.ag.isBig()) {
				p.value += 1 * coefficient;
			} else {
				if(p.ag.isSmall() == false) {
					p.value += 0.3 * coefficient;
				}
			}
			

		}
	}
	
	private void add_FireProbability(ArrayList<AURAreaGraphValue> points, double coefficient) {
		
		double max = -100;
		for (AURAreaGraphValue p : points) {
			
			max = Math.max(max, p.ag.getBuilding().fireSimBuilding.fireProbability);

		}
		if(max <= 0.00001) {
			return;
		}
		
		for (AURAreaGraphValue p : points) {
			
			if (true) {
				p.value += ((double) p.ag.getBuilding().fireSimBuilding.fireProbability / max) * coefficient;
			}
		}

//		for (AURAreaGraphValue p : points) {
//			if (p.ag.onFireProbability) {
//				p.value += (1 + 0) * coefficient;
//			}
//		}
	}

	private void add_GasStation(ArrayList<AURAreaGraphValue> points, double coefficient) {
		double maxDist = 0;
		for (AURAreaGraphValue p : points) {
			p.temp_value = p.ag.lineDistToClosestGasStation();
			if (p.temp_value > maxDist) {
				maxDist = p.temp_value;
			}
		}
		if (maxDist > 0.5) {
			for (AURAreaGraphValue p : points) {
				p.value += (1 - (p.temp_value / maxDist)) * coefficient;
			}
		}
	}

	private void add_TravelCost(ArrayList<AURAreaGraphValue> points, double coefficient) {
		double maxDist = 0;
		for (AURAreaGraphValue p : points) {
			p.temp_value = p.ag.getBuilding().getPerceptCost();
			if (p.temp_value > maxDist) {
				maxDist = p.temp_value;
			}
		}

		if(maxDist <= 0.5) {
			return;
		}
		for (AURAreaGraphValue p : points) {
			p.value += (1 - (p.temp_value / maxDist)) * coefficient;
		}
	}
	
	private void mul_soClose(ArrayList<AURAreaGraphValue> points, double coefficient) {
		for (AURAreaGraphValue p : points) {
			if(p.ag.getBuilding().getPerceptCost() < AURConstants.Agent.VELOCITY * 0.5) {
				p.value *= coefficient;
			}
		}

	}

	private void add_NoBlockadeTravelCost(ArrayList<AURAreaGraphValue> points, double coefficient) {
		double maxDist = 0;
		for (AURAreaGraphValue p : points) {
			p.temp_value = p.ag.getNoBlockadeTravelCost();
			if (p.temp_value > maxDist) {
				maxDist = p.temp_value;
			}
		}

		for (AURAreaGraphValue p : points) {
			p.value += (1 - (p.temp_value / maxDist)) * coefficient;
		}
	}

	private void mul_InitialCluster(ArrayList<AURAreaGraphValue> points, Collection<EntityID> initialCluster,
			double coefficient) {
		for (AURAreaGraphValue p : points) {
			if (true && p.ag.seen() == false && p.ag.burnt == false
					&& initialCluster.contains(p.ag.area.getID())) {
				p.value *= (1 * coefficient);
			}

		}
	}

	/*
	 * public void draw(Graphics2D g) { convexHullInstance.draw(g); int a = 5;
	 * g.setFont(new Font("TimesRoman", Font.PLAIN, 1500)); for(K_ValuePoint
	 * point : this.points) { g.setColor(Color.gray); g.fillRect((int) (point.x
	 * - a), (int) (point.y - a), 2 * a, 2 * a); g.setColor(Color.black);
	 * g.drawString(point.value + "", (int) (point.x), (int) (point.y)); } }
	 */
}