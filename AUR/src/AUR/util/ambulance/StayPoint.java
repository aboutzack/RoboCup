package AUR.util.ambulance;


import adf.agent.info.WorldInfo;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Edge;
import rescuecore2.worldmodel.EntityID;

/**
 * Created by armanaxh in 2017
 */
public  class StayPoint {
    private WorldInfo worldInfo ;
    public StayPoint(WorldInfo wi ){
        this.worldInfo = wi ;
    }

    public Point2D calc(Area entity , Edge edge){
        Point2D point = null ;

        point = this.checkGridPoint(entity , edge );
        if(point != null )
            return point ;

        point = this.escapePoint(entity , edge );
        if(point != null )
            return point ;

        point = this.finalLastStand(entity , edge );
        if(point != null )
            return point ;

        return null;
    }

    private Point2D checkGridPoint(Area entity , Edge edge){
        final int gredSize = 400 ;
        final int numEdgeGredD2 = (int)this.getDistance(edge.getStartX() , edge.getStartY() , edge.getEndX() , edge.getEndY())/gredSize/2 ;
        long midx = (edge.getStartX()+edge.getEndX())/2 ;
        long midy = (edge.getStartY()+ edge.getEndY())/2;
        Point2D point = null ;
        for(int  grednum = 0 ; grednum < numEdgeGredD2 ; grednum++ ){
            if(grednum == 0 ){
                point = this.cheakPerpendicularPoint(entity , edge , 0 );
                if(point != null )
                    return point ;
            }else{
                point = this.cheakPerpendicularPoint(entity , edge , gredSize*grednum );
                if(point != null )
                    return point ;
                point = this.cheakPerpendicularPoint(entity , edge , -1*gredSize*grednum );
                if(point != null )
                    return point;
            }
        }
        return  null;
    }


    private Point2D cheakPerpendicularPoint(Area entity , Edge edge , int scale){
        final int distanceFromEdge = 300 ;
        int x1, x2 , y1 ,y2 , midX , midY ;
        x1 = edge.getStartX() ;
        x2 = edge.getEndX();
        y1 = edge.getStartY();
        y2 = edge.getEndY();
        midX = (x1+x2)/2 ;
        midY = (y1+y2)/2 ;
        Vector2D vec = new Vector2D(x1-x2, y1-y2 );
        Vector2D vec2 = new Vector2D(x2-x1 ,y2-y1 );
        Vector2D vecNorm = vec.getNormal().normalised().scale(distanceFromEdge);
        Vector2D vec2Norm = vec2.getNormal().normalised().scale(distanceFromEdge);
        vec = vec.normalised().scale(scale);

        Point2D point1 = new Point2D(vecNorm.getX()+midX+vec.getX() , vecNorm.getY()+midY+vec.getY());
        Point2D point2 = new Point2D(vec2Norm.getX()+midX+vec.getX() ,vec2Norm.getY()+midY+vec.getY());

        if(this.isInside(point1.getX() , point1.getY(),entity.getApexList()) ){
            boolean inBlockade = false ;
            if(entity.getBlockades() != null)
                for(EntityID id : entity.getBlockades()){
                    Blockade block = (Blockade)worldInfo.getEntity(id);
                    if(this.isInside(point1.getX() , point1.getY() ,block.getApexes() )){
                        inBlockade = true ;
                    }
                }
            if(!inBlockade)
                return point1;
        }

        if(this.isInside(point2.getX() , point2.getY(),entity.getApexList())){
            boolean inBlockade = false ;
            if(entity.getBlockades() != null)
                for(EntityID id : entity.getBlockades()){
                    Blockade block = (Blockade)worldInfo.getEntity(id);
                    if(this.isInside(point2.getX() , point2.getY() ,block.getApexes() )){
                        inBlockade = true ;
                    }
                }
            if(!inBlockade)
                return point2;
        }
        return null ;
    }

    private Point2D escapePoint(Area entity , Edge edge ){
        final int distanceFromEdge = 500 ;
        int x1, x2 , y1 ,y2 , midX , midY ;
        x1 = edge.getStartX() ;
        x2 = edge.getEndX();
        y1 = edge.getStartY();
        y2 = edge.getEndY();
        midX = (x1+x2)/2 ;
        midY = (y1+y2)/2 ;
        x1 = (entity.getX()+3*midX)/4;
        y1 = (entity.getY()+3*midY)/4;
        if(entity.getShape().contains(x1,y1)){
            boolean inBlockade = false ;
            if(entity.getBlockades() != null)
                for(EntityID id : entity.getBlockades()){
                    Blockade block = (Blockade)worldInfo.getEntity(id);
                    if(this.isInside(x1, y1 ,block.getApexes() )){
                        inBlockade = true ;
                    }
                }
            if(!inBlockade)
                return new Point2D(x1,y1);
        }

        return null ;
    }
    private Point2D finalLastStand(Area entity , Edge edge ){

        final int distanceFromEdge = 25  ;
        int x1, x2 , y1 ,y2 , midX , midY ;
        x1 = edge.getStartX() ;
        x2 = edge.getEndX();
        y1 = edge.getStartY();
        y2 = edge.getEndY();
        midX = (x1+x2)/2 ;
        midY = (y1+y2)/2 ;
        Vector2D vec = new Vector2D(x1-x2, y1-y2 );
        Vector2D vec2 = new Vector2D(x2-x1 ,y2-y1 );
        vec = vec.getNormal().normalised().scale(distanceFromEdge);
        vec2 = vec2.getNormal().normalised().scale(distanceFromEdge);
        Point2D point1 = new Point2D(vec.getX()+midX , vec.getY()+midY);
        Point2D point2 = new Point2D(vec2.getX()+midX ,vec2.getY()+midY);

        //BE GAAA be khater VC
        if(this.isInside(point1.getX() , point1.getY(),entity.getApexList()) ){
            boolean inBlockade = false ;
            if(entity.getBlockades() != null)
                for(EntityID id : entity.getBlockades()){
                    Blockade block = (Blockade)worldInfo.getEntity(id);
                    if(this.isInside(point1.getX() , point1.getY() ,block.getApexes() )){
                        inBlockade = true ;
                    }
                }
            if(!inBlockade)
                return point1;
        }
//            if(entity.getShape().contains(point2.getX() , point2.getY())){
        if(this.isInside(point2.getX() , point2.getY(),entity.getApexList())){
            boolean inBlockade = false ;
            if(entity.getBlockades() != null)
                for(EntityID id : entity.getBlockades()){
                    Blockade block = (Blockade)worldInfo.getEntity(id);
                    if(this.isInside(point2.getX() , point2.getY() ,block.getApexes() )){
                        inBlockade = true ;
                    }
                }
            if(!inBlockade)
                return point2;
        }

        return null ;
    }

    private  boolean isInside(double pX, double pY, int[] apex)
    {
        Point2D p = new Point2D(pX, pY);
        Vector2D v1 = (new Point2D(apex[apex.length - 2], apex[apex.length - 1])).minus(p);
        Vector2D v2 = (new Point2D(apex[0], apex[1])).minus(p);
        double theta = this.getAngle(v1, v2);
        for (int i = 0; i < apex.length - 2; i += 2) {
            v1 = (new Point2D(apex[i], apex[i + 1])).minus(p);
            v2 = (new Point2D(apex[i + 2], apex[i + 3])).minus(p);
            theta += this.getAngle(v1, v2);
        }
        return Math.round(Math.abs((theta / 2) / Math.PI)) >= 1;
    }
    private  double getAngle(Vector2D v1, Vector2D v2) {
        double flag = (v1.getX() * v2.getY()) - (v1.getY() * v2.getX());
        double angle = Math
                .acos(((v1.getX() * v2.getX()) + (v1.getY() * v2.getY())) / (v1.getLength() * v2.getLength()));
        if (flag > 0) {
            return angle;
        }
        if (flag < 0) {
            return -1 * angle;
        }
        return 0.0D;
    }
    private double getDistance(double fromX, double fromY, double toX, double toY) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        return Math.hypot(dx, dy);
    }
}
