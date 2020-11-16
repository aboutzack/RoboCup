package CSU_Yunlu_2020.extaction.pf;

import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;






public class guidelineHelper {

	private Point2D start = null;
	private Point2D end = null;
	private Line2D guideline =null;

	private Road selfRoad;
	private EntityID selfId;
	private Boolean isEntrance = false;


	public guidelineHelper(Road road,Point2D start,Point2D end,Boolean isEntrance) {
		this.selfId = road.getID();
		this.start = start;
		this.end = end;
		this.selfRoad = road;
		this.guideline = createGuideline();
		this.isEntrance = isEntrance;

	}

	public guidelineHelper(Line2D line,Road road,Boolean isEntrance) {
		this.guideline = line;
		this.start = line.getOrigin();
		this.end = line.getEndPoint();
		this.selfRoad = road;
		this.selfId = road.getID();
		this.isEntrance = isEntrance;
	}

	public guidelineHelper() {

	}


	public Line2D createGuideline() {
		return new Line2D(start,end);
	}


	public Line2D getGuideline() {
		return this.guideline;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof guidelineHelper) {
			return this.selfId.equals(((guidelineHelper) o).getSelfID());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.selfId.getValue();
	}


	public EntityID getSelfID() {
		return this.selfId;
	}

	public Road getSelfRoad() {
		return this.selfRoad;
	}

	public Point2D getStartPoint() {
		return this.start;
	}

	public Point2D getEndPoint() {
		return this.end;
	}

	public Boolean getEntranceState() { return this.isEntrance;	}

}
