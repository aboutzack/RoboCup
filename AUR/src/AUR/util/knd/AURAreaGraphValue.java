package AUR.util.knd;

/**
 *
 * @author Alireza Kandeh - 2017 & 2018
 */

public class AURAreaGraphValue {
	
	public AURAreaGraph ag = null;
	public double value = 0;
	public double temp_value = 0;
	
	public AURAreaGraphValue(AURAreaGraph ag) {
		this.ag = ag;
	}

	public double dist(AURAreaGraphValue agv) {
		return AURGeoUtil.dist(this.ag.getX(), this.ag.getY(), agv.ag.getX(), agv.ag.getY());
	}

}