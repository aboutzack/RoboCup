package CSU_Yunlu_2020.module.complex.fb.tools;

import CSU_Yunlu_2020.module.algorithm.fb.FireCluster;
import CSU_Yunlu_2020.util.Util;
import CSU_Yunlu_2020.world.CSUWorldHelper;
import rescuecore2.standard.entities.FireBrigade;

import java.awt.*;

/**
 * Find the main expand direction of fires.
 * 
 * @author appreciation-csu
 */
public class DirectionManager {
	private CSUWorldHelper world;
	private int theta = 30;
	public DirectionManager(CSUWorldHelper world) {
		this.world = world;
	}

    public Point findFarthestPointOfMap(FireCluster fireCluster, FireBrigade fbAgent) {
        math.geom2d.Point2D fireClusterCenter = new math.geom2d.Point2D(fireCluster.getCenter());
        math.geom2d.Point2D[] points = new math.geom2d.Point2D[4];
        points[0] = new math.geom2d.Point2D(0, 0);
        points[1] = new math.geom2d.Point2D(0, world.getMapHeight());
        points[2] = new math.geom2d.Point2D(world.getMapWidth(), 0);
        points[3] = new math.geom2d.Point2D(world.getMapWidth(), world.getMapHeight());


        math.geom2d.Point2D farthestPointOfMap = 
        		Util.findFarthestPoint(fireCluster.getConvexHull().getConvexPolygon(), points);

        math.geom2d.Point2D targetPoint = 
        		new math.geom2d.Point2D(farthestPointOfMap.getX(), farthestPointOfMap.getY());

        double degree = 0;
        while (degree < 360) {
            if (fireCluster.haveBuildingInDirectionOf(new Point((int) targetPoint.getX(), 
            		(int) targetPoint.getY()))) {
                break;
            }
            targetPoint = targetPoint.rotate(fireClusterCenter, 
            		Math.toRadians((fbAgent.getID().getValue() % 2) == 0 ? theta : -theta));
            degree += theta;
        }
        return new Point((int) targetPoint.getX(), (int) targetPoint.getY());
    }
}