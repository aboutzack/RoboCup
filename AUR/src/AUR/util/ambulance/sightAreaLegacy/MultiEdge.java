package AUR.util.ambulance.sightAreaLegacy;

import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Edge;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by armanaxh on 12/22/17.
 */
public class MultiEdge {
    private ArrayList<Point2D> points;
    private final int numPointForEachEdge = 15;

    public MultiEdge(Edge edge) {
        this.points = new ArrayList<>();
        this.points.add(edge.getStart());
        this.points.add(edge.getEnd());
    }
    public MultiEdge() {
        this.points = new ArrayList<>();
    }
    public void addEdge(Edge edge){
        if(!getPoints().contains(edge.getStart())) {
            this.points.add(edge.getStart());
        }
        if(!getPoints().contains(edge.getEnd())){
            this.points.add(edge.getEnd());
        }
    }
    public void addFirstInList(){
        this.points.add(this.points.get(0));
    }
    public ArrayList<Point2D> getPoints(){
        return this.points;
    }
    public Point2D getStartPoint(){
        if(!points.isEmpty())
            return points.get(0);
        return null;
    }
    public Point2D getEndPoint(){
        if(!points.isEmpty())
            return points.get(points.size()-1);
        return null;
    }

    public List<Point2D> getMidPoint(){
        increasePoints();
        if(points.size()>2)
            return points.subList(1,points.size()-1);
        return null;
    }


    //// TODO edge ha va Point saat ghard hastan ya ina check nashode hataman ye baresii boukon
    /// age mokaya bddouni chera jaye i-1 ba i ro avaz kon to x, y
    public void increasePoints(){
        if(points.isEmpty()){
            throw new Error("fuckkkk");
        }
        int pointSize = this.points.size();
        int n = numPointForEachEdge;
        ArrayList<Point2D> temp = new ArrayList<>();
        temp.add(points.get(0));
        for(int i=1 ; i<pointSize ; i++){
            for(int j=1 ; j<= numPointForEachEdge ; j++){
//                int x = (int) (((j*1D)/(n+1))*(points.get(i-1).getX()+ (((n-j+1)*1D)/(n+1))*points.get(i).getX()));
//                int y = (int) (((j*1D)/(n+1))*(points.get(i-1).getY()+ (((n-j+1)*1D)/(n+1))*points.get(i).getY()));

                 int x = (int)((j*points.get(i).getX()+(n-j+1)*points.get(i-1).getX())/(n+1));
                 int y = (int)((j*points.get(i).getY()+(n-j+1)*points.get(i-1).getY())/(n+1));
                    temp.add(new Point2D(x,y));
            }
            temp.add(points.get(i));
        }
        this.points = temp;
    }
    public static List<MultiEdge> getPassEdge(List<Edge> edges) {
        ArrayList<MultiEdge> result = new ArrayList();
        int edgesSize = edges.size();
        boolean flagPass = true;
        for(Edge e:edges){
            if(!e.isPassable()) {
                flagPass = false;
                break;
            }
        }
        if(flagPass == true ){
            MultiEdge me = new MultiEdge();
            for(Edge e:edges){
                me.addEdge(e);
            }
            me.addFirstInList();
            result.add(me);
            return result;
        }
        int factor = 0;
        for(int i=0 ; i<edgesSize ; i++){
            if(!edges.get(i).isPassable()){
                factor = i;
                break;
            }
        }
        boolean flag = true;
        MultiEdge temp = new MultiEdge();
        for (int i = 0; i< edgesSize ; i++) {
            int index = (factor + i) % edgesSize;
            int nextIndex = (index + 1) % edgesSize;

            if (true
                    && flag == true
                    && edges.get(index).isPassable()
                    && ((i+1 == edgesSize) || !edges.get(nextIndex).isPassable() )
                    ) {
                temp.addEdge(edges.get(index));
                result.add(temp);
                temp = new MultiEdge();
                flag = false;
            }else if (true
                    &&(edges.get(index)).isPassable()
                    && (edges.get(nextIndex)).isPassable()) {
                temp.addEdge(edges.get(index));
                flag = true;
            } else if (edges.get(index).isPassable()) {
                result.add(new MultiEdge(edges.get(index)));
            }
        }
        return result;
    }

    public String toString(){
        return points.toString();
    }
}
