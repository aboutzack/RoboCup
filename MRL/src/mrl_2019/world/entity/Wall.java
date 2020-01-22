package mrl_2019.world.entity;


import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

/**
 * User: mrl_2019
 * Date: May 17, 2010
 * Time: 7:10:06 PM
 */


/**
 * @author tn
 */
public class Wall {
    public static Random rnd = new Random(23);
    public static final int MAX_SAMPLE_DISTANCE = 200000;//50000;
    public static final int MAX_FIRE_DISTANCE = 25000;
    public int x1;
    public int y1;
    public int x2;
    public int y2;
    public MrlBuilding owner;
    public int rays;
    public int hits;
    public int selfHits;
    public int strange;
    public double length;
    public Point a;
    public Point b;

    public boolean right;
    public int distance;

    public Wall(int x1, int y1, int x2, int y2, MrlBuilding owner, float rayRate) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        a = new Point(x1, y1);
        b = new Point(x2, y2);
        length = a.distance(b);
        rays = (int) Math.ceil(length * rayRate);
        hits = 0;
        this.owner = owner;
    }

    public boolean validate() {
        return !(a.x == b.x && a.y == b.y);
    }

    public void findHits(MrlBuilding mrlBuilding) {
//        BuildingHelper buildingHelper=world.getHelper(BuildingHelper.class);
        selfHits = 0;
        strange = 0;
        for (int emitted = 0; emitted < rays; emitted++) {
            //creating ray
            Point start = getRndPoint(a, b);
            if (start == null) {
                strange++;
                System.out.println("strange -> " + a.x + "," + a.y + "/" + b.x + "," + b.y);
                continue;
            }
            Point end = getRndPoint(start, (double) MAX_SAMPLE_DISTANCE);
            //intersect
            Wall closest = null;
            double minDist = Double.MAX_VALUE;
            ArrayList<Wall> listWall = mrlBuilding.getAllWalls();

            for (Wall other : listWall) {
                if (other == this) continue;
                Point cross = intersect(start, end, other.a, other.b);
                if (cross != null && cross.distance(start) < minDist) {
                    minDist = cross.distance(start);
                    closest = other;
                }
            }
            if (closest == null) {
                //Nothing was hit
                continue;
            }
            if (closest.owner == this.owner) {
                //The source building was hit
                selfHits++;
            }
            if (closest != this && closest != null && closest.owner != owner) {
                hits++;
//                Hashtable hashtable=buildingHelper.getBuildingConnectedTable(this.owner.getID());
                Hashtable hashtable = mrlBuilding.getConnectedBuildingsTable();
                Integer value = (Integer) hashtable.get(closest.owner);
                int temp = 0;
                if (value != null) {
                    temp = value;
                }
                temp++;
                hashtable.put(closest.owner, temp);
            }
        }
    }

    public String toString() {
        return "wall (" + a.x + "," + a.y + ")-(" + b.x + "," + b.y + "), length=" + length + "mm, rays=" + rays;
    }

    //---------------------------Geometry Functions -------------------------------

    public static Point getRndPoint(Point a, Point b) {
        float[] mb = getAffineFunction((float) a.x, (float) a.y, (float) b.x, (float) b.y);
        float dx = (Math.max((float) a.x, (float) b.x) - Math.min((float) a.x, (float) b.x));
        dx *= rnd.nextDouble();
        dx += Math.min((float) a.x, (float) b.x);
        if (mb == null) {
            //vertical line
            int p = Math.max(a.y, b.y) - Math.min(a.y, b.y);
            p = (int) (p * Math.random());
            p = p + Math.min(a.y, b.y);
            return new Point(a.x, p);
        }
        float y = mb[0] * dx + mb[1];
        Point rtv = new Point((int) dx, (int) y);
        if (rtv == null) {
            System.currentTimeMillis();
        }
        return rtv;
    }

    public static Point getRndPoint(Point a, double length) {
        double angel = rnd.nextDouble() * 2d * Math.PI;
        double x = Math.sin(angel) * length;
        double y = Math.cos(angel) * length;
        return new Point((int) x + a.x, (int) y + a.y);
    }

    public static Point intersect(Point a, Point b, Point c, Point d) {
        float[] rv = intersect(new float[]{a.x, a.y, b.x, b.y, c.x, c.y, d.x, d.y});
        if (rv == null) return null;
        return new Point((int) rv[0], (int) rv[1]);
    }

    public static float[] getAffineFunction(float x1, float y1, float x2, float y2) {
        //System.out.println("("+x1+","+y1+") to ("+x2+","+y2+")");
        if (x1 == x2) return null;
        float m = (y1 - y2) / (x1 - x2);
        float b = y1 - m * x1;
        //System.out.println("b="+b+", m="+m);
        return new float[]{m, b};
    }

    public static float[] intersect(float[] points) {
        float[] l1 = getAffineFunction(points[0], points[1], points[2], points[3]);
        float[] l2 = getAffineFunction(points[4], points[5], points[6], points[7]);
        float[] crossing;
        if (l1 == null && l2 == null) {
            return null;
        } else if (l1 == null && l2 != null) {
            crossing = intersect(l2[0], l2[1], points[0]);
        } else if (l1 != null && l2 == null) {
            crossing = intersect(l1[0], l1[1], points[4]);
        } else {
            crossing = intersect(l1[0], l1[1], l2[0], l2[1]);
        }
        if (crossing == null) {
            return null;
        }
        if (!(inBounds(points[0], points[1], points[2], points[3], crossing[0], crossing[1]) &&
                inBounds(points[4], points[5], points[6], points[7], crossing[0], crossing[1]))) return null;
        return crossing;
    }

    public static float[] intersect(float m1, float b1, float x) {
        return new float[]{x, m1 * x + b1};
    }

    public static float[] intersect(float m1, float b1, float m2, float b2) {
        if (m1 == m2) {
            return null;
        }
        float x = (b2 - b1) / (m1 - m2);
        float y = m1 * x + b1;
        return new float[]{x, y};
    }

    public static boolean inBounds(float bx1, float by1, float bx2, float by2, float x, float y) {
        if (bx1 < bx2) {
            if (x < bx1 || x > bx2) return false;
        } else {
            if (x > bx1 || x < bx2) return false;
        }
        if (by1 < by2) {
            if (y < by1 || y > by2) return false;
        } else {
            if (y > by1 || y < by2) return false;
        }
        return true;
    }

    public Line2D getLine() {
        if (a == null || b == null)
            return null;

        return new Line2D.Double(a, b);
    }


}

