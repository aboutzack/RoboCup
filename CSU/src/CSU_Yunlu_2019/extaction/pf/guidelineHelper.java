package CSU_Yunlu_2019.extaction.pf;

import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;






public class guidelineHelper {
	
	private Point2D start = null;
	private Point2D end = null;
	private Line2D guideline =null;
	
	private Road selfRoad;
	private EntityID selfId;
	
	
	public guidelineHelper(Road road,Point2D start,Point2D end) {
		this.selfId = road.getID();
		this.start = start;
		this.end = end;
		this.selfRoad = road;
		this.guideline = createGuideline();
		
	}
	
	public guidelineHelper(Line2D line,Road road) {
		this.guideline = line;
		this.start = line.getOrigin();
		this.end = line.getEndPoint();
		this.selfRoad = road;
		this.selfId = road.getID();
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
	
	
}
