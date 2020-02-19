package AUR.util.aslan;

import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;

/**
 *
 * @author Amir Aslan Aslani - Mar 2018
 */
public class AURGeoMetrics {
        public static double getVectorLen(double[] v){
                return Math.hypot(v[0], v[1]);
        }
        
        public static double[] getVectorNormal(double[] v){
                double len = getVectorLen(v);
                return new double[]{
                        (v[0] / len),
                        (v[1] / len)
                };
        }
        
        public static double[] getVectorScaled(double[] v, double s){
                return new double[]{
                        (v[0] * s),
                        (v[1] * s)
                };
        }
        
        public static double[] getVectorFromVector2D(Vector2D v2d){
                return new double[]{
                        v2d.getX(),
                        v2d.getY()
                };
        }
        
        public static double[] getNormalVectorWithAngle(double rad){
                return new double[]{
                        Math.cos(rad),
                        Math.sin(rad)
                };
        }
        
        public static Point2D getPoint2DFromPoint(double p[]){
                return new Point2D(p[0], p[1]);
        }
        
        public static double[] getPointFromPoint2D(Point2D p){
                return new double[]{
                        p.getX(),
                        p.getY()
                };
        }
        
        public static double[] getPointsMinus(double[] p1, double[] p2){
                return new double[]{
                        p1[0] - p2[0],
                        p1[1] - p2[1]
                };
        }
        
        public static double[] getPointsPlus(double[] p1, double[] p2){
                return new double[]{
                        p1[0] + p2[0],
                        p1[1] + p2[1]
                };
        }
        
        public static double[] getPerpendicularVector(double v[]) {
                return new double[]{
                        - v[1],
                        v[0]
                };
        }
        
        public static int[] getInt(double[] i){
                int[] result = new int[i.length];
                for(int j = 0;j < i.length;j ++)
                        result[j] = (int) i[j];
                return result;
        }
}
