package AUR.util.ambulance.sightAreaLegacy;


import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;


/**
 * Created by armanaxh on 12/21/17.
 */
public class SpectatoLine extends Line2D {

    private final double DISPERSION_Distance =  1000;// //TODO bayad payda konim in add ro
    private Point2D mPoint;


    public SpectatoLine(MultiEdge e) {
        super( e.getStartPoint(),  e.getEndPoint() );
        this.mPoint = new Point2D(
                (this.getOrigin().getX()+getEndPoint().getX())/2,
                (this.getOrigin().getY()+getEndPoint().getY())/2
        );
    }


    public SpectatoLine reduce(){
        this.setOrigin(this.getOrigin().plus(this.getDirection().normalised()));
        this.setEnd(this.getEndPoint().plus(this.getDirection().scale(-1).normalised()));
        return this;
    }
    public SpectatoLine transfer(Point2D mToPoint){
        this.mPoint = mToPoint;
        Vector2D dir = this.getDirection().normalised().scale(DISPERSION_Distance);

        this.setOrigin(new Point2D(
                mToPoint.getX() - dir.getX()  ,
                mToPoint.getY() - dir.getY()
        ));
        this.setEnd(new Point2D(
                mToPoint.getX() + dir.getX()  ,
                mToPoint.getY() + dir.getY()
        ));
        return this;
    }

    public Point2D getmPoint() {
        return mPoint;
    }

    @Override
    public String toString(){
        return " ( "+getOrigin()+" "+mPoint+" "+getEndPoint()+" ) ";
    }
}
