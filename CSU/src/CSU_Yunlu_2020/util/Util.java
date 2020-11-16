package CSU_Yunlu_2020.util;

import CSU_Yunlu_2020.CSUConstants;
import CSU_Yunlu_2020.standard.Ruler;
import CSU_Yunlu_2020.world.object.CSUEdge;
import CSU_Yunlu_2020.world.object.CSULineOfSightPerception;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.AbstractEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class Util {

	public static double gaussmf(double x, double sig, double c) {
		return Math.exp(-((x - c) * (x - c)) / (2.0 * sig * sig));
	}
	
	public static double gauss2mf(double x, double sig1, double c1, double sig2, double c2) {
		if (x <= c1) {
			return gaussmf(x, sig1, c1);
		} else {
			return gaussmf(x, sig2, c2);
		}
	}
	
	public static Polygon getPolygon(int[] apexes) {
        Polygon polygon = new Polygon();
        for (int i = 0; i < apexes.length; i += 2) {
            polygon.addPoint(apexes[i], apexes[i + 1]);
        }

        return polygon;
    }
	
	/** Convert EntityID list to integer list.*/
	public static List<Integer> entityIdListToIntegerList(List<EntityID> entityIds) {
		List<Integer> returnList = new LinkedList<Integer>();
		for (EntityID entityId: entityIds) {
			returnList.add(entityId.getValue());
		}
		return returnList;
	}
	
	/** Convert integer list to EntityID list.*/
	public static List<EntityID> integerListToEntityIdList(List<Integer> integerIds) {
		List<EntityID> returnList = new LinkedList<EntityID>();
		for (Integer next : integerIds) {
			returnList.add(new EntityID(next.intValue()));
		}
		return returnList;
	}
	
	/** Determines whether target polygon has intersect with target line.*/
	public static boolean hasIntersectLine(Polygon polygon, Line2D line) {
		List<rescuecore2.misc.geometry.Line2D> polyLines = getLines(polygon);
		if (polygon.contains(line.getOrigin().getX(), line.getOrigin().getY()) ||
				polygon.contains(line.getEndPoint().getX(), line.getEndPoint().getY())) {
			return true;
		}
		for (rescuecore2.misc.geometry.Line2D ln : polyLines) {
			rescuecore2.misc.geometry.Point2D intersectPoint = GeometryTools2D.getSegmentIntersectionPoint(line, ln);
			if (/*contains(ln, intersectPoint, 5)*/ intersectPoint != null) {
				return true;
			}
		}
		return false;
	}

	/** Get all boundary lines of target polygon.*/
	public static List<Line2D> getLine2DOfPolygon(Polygon polygon) {
		List<Line2D> allLines = new LinkedList<>();
		int count = polygon.npoints;
		for (int i = 0; i < count; i++) {
			int j = (i + 1) % count;
			Point2D first = new Point2D(polygon.xpoints[i], polygon.ypoints[i]);
			Point2D second = new Point2D(polygon.xpoints[j], polygon.ypoints[j]);
			Line2D line = new Line2D(first, second);
			allLines.add(line);
		}
		return allLines;
	}
	
	/** Get the intersection point of two line.*/
	public static Point2D getIntersection(Line2D line1, Line2D line2) {
		final double x1, y1, x2, y2, x3, y3, x4, y4;
		x1 = line1.getOrigin().getX();
		y1 = line1.getOrigin().getY();
		x2 = line1.getEndPoint().getX();
		y2 = line1.getEndPoint().getY();
		x3 = line2.getOrigin().getX();
		y3 = line2.getOrigin().getY();
		x4 = line2.getEndPoint().getX();
		y4 = line2.getEndPoint().getY();
		final double x = ((x2 - x1) * (x3 * y4 - x4 * y3) - (x4 - x3) * (x1 * y2 - x2 * y1))
				/ ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));
		final double y = ((y3 - y4) * (x1 * y2 - x2 * y1) - (y1 - y2) * (x3 * y4 - x4 * y3))
				/ ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));

		return new Point2D(x, y);
	}

	/**
	* @Description: 判断两条线段是否相交
	* @Author: Guanyu-Cai
	* @Date: 3/13/20
	*/
	public static boolean isSegmentIntersecting(Line2D l1, Line2D l2) {
		final double x1, y1, x2, y2, x3, y3, x4, y4;
		x1 = l1.getOrigin().getX();
		y1 = l1.getOrigin().getY();
		x2 = l1.getEndPoint().getX();
		y2 = l1.getEndPoint().getY();
		x3 = l2.getOrigin().getX();
		y3 = l2.getOrigin().getY();
		x4 = l2.getEndPoint().getX();
		y4 = l2.getEndPoint().getY();
		//快速排斥实验
		if ((Math.max(x1, x2)) < (Math.min(x3, x4)) ||
				(Math.max(y1, y2)) < (Math.min(y3, y4)) ||
				(Math.max(x3, x4)) < (Math.min(x1, x2)) ||
				(Math.max(y3, y4)) < (Math.min(y1, y2))) {
			return false;
		}
		//跨立实验
		if ((((x1 - x3) * (y4 - y3) - (y1 - y3) * (x4 - x3)) *
				((x2 - x3) * (y4 - y3) - (y2 - y3) * (x4 - x3))) > 0 ||
				(((x3 - x1) * (y2 - y1) - (y3 - y1) * (x2 - x1)) *
						((x4 - x1) * (y2 - y1) - (y4 - y1) * (x2 - x1))) > 0) {
			return false;
		}
		return true;
	}

	/**
	* @Description: 获取线段交点(非直线)
	* @Author: Guanyu-Cai
	* @Date: 3/13/20
	*/
	public static List<Point2D> getSegmentIntersections(Set<CSULineOfSightPerception.CsuRay> rays1, Set<CSULineOfSightPerception.CsuRay> rays2) {
		ArrayList<Point2D> result = new ArrayList<>();
		for (CSULineOfSightPerception.CsuRay ray1 : rays1) {
			for (CSULineOfSightPerception.CsuRay ray2 : rays2) {
				if (isSegmentIntersecting(ray1.getRay(), ray2.getRay())) {
					Point2D intersection = getIntersection(ray1.getRay(), ray2.getRay());
					result.add(intersection);
				}
			}
		}
		return result;
	}
	
	public static math.geom2d.Point2D findFarthestPoint(Polygon polygon, math.geom2d.Point2D[] points) {
        math.geom2d.Point2D farthestPoint = null;
        List<Pair<math.geom2d.Point2D, Double>> pointsDistancesToPolygon = new LinkedList<>();
        for (math.geom2d.Point2D point : points) {
        	double distance = Ruler.getDistance(polygon, point);
            pointsDistancesToPolygon.add(new Pair<math.geom2d.Point2D, Double>(point, distance));
        }
        double maxDistance = Double.MIN_VALUE;
        for (Pair<math.geom2d.Point2D, Double> pair : pointsDistancesToPolygon) {
            if (pair.second() > maxDistance) {
                maxDistance = pair.second();
                farthestPoint = pair.first();
            }
        }
        return farthestPoint;
    }
	
	public static Object readObject(String filePath) throws IOException, ClassNotFoundException {
        ObjectInputStream objInput = new ObjectInputStream(new FileInputStream(filePath));
        Object o = objInput.readObject();
        objInput.close();
        return o;
    }

    public static void writeObject(Object object, String filePath) throws IOException {
//		File file = new File(filePath);
//		if (!file.exists()) {
//			try {
//				file.createNewFile();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		} else {
//			file.
//		}
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(filePath));
        objectOutputStream.writeObject(object);
        objectOutputStream.close();
    }
    
	// public static Set<EntityID> entityToIds(BurningBuildings entities) {
	// Set<EntityID> entityIds = new FastSet<>();
	// for (StandardEntity next :entities) {
	// entityIds.add(next.getID());
	// }
	// return entityIds;
	// }
	//
	// public static Set<EntityID> csuBuildingToId(Set<CSUBuilding>
	// csuBuildings) {
	// Set<EntityID> entityIds = new FastSet<>();
	// for (CSUBuilding next : csuBuildings) {
	// entityIds.add(next.getId());
	// }
	// return entityIds;
	// }
	//
	// public static Set<EntityID> entityToIds(Collection<StandardEntity>
	// entities) {
	// Set<EntityID> entityIds = new FastSet<>();
	// for (StandardEntity next :entities) {
	// entityIds.add(next.getID());
	// }
	// return entityIds;
	// }
	//
	// public static Set<StandardEntity> idToEntities(Collection<EntityID> ids,
	// AdvancedWorldModel world) {
	// Set<StandardEntity> entities = new FastSet<>();
	// for (EntityID next : ids) {
	// entities.add(world.getEntity(next));
	// }
	// return entities;
	// }
	//
	// public static List<CSUBuilding> integerToCsuBuilding(AdvancedWorldModel
	// world, Collection<Integer> integers) {
	// List<CSUBuilding> buildings = new LinkedList<>();
	// for (Integer next : integers) {
	// EntityID id = new EntityID(next.intValue());
	// id = world.getEntity(id).getID();
	// buildings.add(world.getCsuBuilding(id));
	// }
	// return buildings;
	// }
	//
	// public static List<CSUBuilding>
	// burnBuildingToCsuBuilding(AdvancedWorldModel world) {
	// List<CSUBuilding> buildings = new LinkedList<>();
	// if(AgentConstants.FB) {
	// if(world.getBurningBuildings().isEmpty())
	// System.out.println(world.getTime() + ", " + world.me + ", burning
	// buildings is empty");
	// else
	// System.out.println(world.getTime() + ", " + world.me
	// + ",world.getBurningBuildings()" + world.getBurningBuildings());
	// }
	//
	// for (Building building : world.getBurningBuildings()) {
	// buildings.add(world.getCsuBuilding(building.getID()));
	// }
	// return buildings;
	// }
    
    /**
     * @param line
     * @param size
     * @return 延长size
     */
    public static Line2D improveLine(Line2D line, double size) {
    	double molecular = line.getEndPoint().getY() - line.getOrigin().getY();
    	double denominaor = line.getEndPoint().getX() - line.getOrigin().getX();
    	double slope;
    	if (denominaor != 0) {
    		slope = molecular / denominaor;
    	} else {
    		if (molecular > 0)
    			slope = Double.MAX_VALUE;
    		else
    			slope = -Double.MAX_VALUE;
    		// Double.MIN_VALUE -- smallest positive nozero value of type double
    	}
    	
    	double newPointX, newPointY;
    	double theta = Math.atan(slope);
    	if (denominaor > 0) {
    		newPointX = line.getEndPoint().getX() + size * Math.abs(Math.cos(theta));
    	} else {
    		newPointX = line.getEndPoint().getX() - size * Math.abs(Math.cos(theta));
    	}
    	if (molecular > 0) {
    		newPointY = line.getEndPoint().getY() + Math.abs(Math.sin(theta)) * size;
    	} else {
    		newPointY = line.getEndPoint().getY() - Math.abs(Math.sin(theta)) * size;
    	}
    	
    	Point2D newEndPoint = new Point2D(newPointX, newPointY);
    	
    	return new Line2D(line.getOrigin(), newEndPoint);
    }

    /**
    * @Description: start和end都延长size的长度
    * @Author: Guanyu-Cai
    * @Date: 3/16/20
    */
	public static Line2D improveLineBothSides(Line2D line, double size) {
		double molecular = line.getEndPoint().getY() - line.getOrigin().getY();
		double denominaor = line.getEndPoint().getX() - line.getOrigin().getX();
		double slope;
		if (denominaor != 0) {
			slope = molecular / denominaor;
		} else {
			if (molecular > 0)
				slope = Double.MAX_VALUE;
			else
				slope = -Double.MAX_VALUE;
			// Double.MIN_VALUE -- smallest positive nozero value of type double
		}

		double newOriginPointX, newOriginPointY, newEndPointX, newEndPointY;
		double theta = Math.atan(slope);
		if (denominaor > 0) {
			newOriginPointX = line.getOrigin().getX() - size * Math.abs(Math.cos(theta));
			newEndPointX = line.getEndPoint().getX() + size * Math.abs(Math.cos(theta));
		} else {
			newOriginPointX = line.getOrigin().getX() + size * Math.abs(Math.cos(theta));
			newEndPointX = line.getEndPoint().getX() - size * Math.abs(Math.cos(theta));
		}
		if (molecular > 0) {
			newOriginPointY = line.getOrigin().getY() - size * Math.abs(Math.sin(theta));
			newEndPointY = line.getEndPoint().getY() + size * Math.abs(Math.sin(theta));
		} else {
			newOriginPointY = line.getOrigin().getY() + size * Math.abs(Math.sin(theta));
			newEndPointY = line.getEndPoint().getY() - size * Math.abs(Math.sin(theta));
		}

		Point2D newOriginPoint = new Point2D(newOriginPointX, newOriginPointY);
		Point2D newEndPoint = new Point2D(newEndPointX, newEndPointY);


		return new Line2D(newOriginPoint, newEndPoint);
	}

	public static rescuecore2.misc.geometry.Line2D clipLine(rescuecore2.misc.geometry.Line2D line, double size) {
		double length = Ruler.getLength(line);
		return improveLine(line, size - length);
	}
    
	/**
	 * Get all intersection points of the given polygon and line.
	 * 
	 * @param poly
	 *            the given polygon
	 * @param line
	 *            the given line
	 * @return Set of intersect Point
	 */
	public static Set<Point2D> getIntersections(Polygon poly, Line2D line) {
		Set<Point2D> result = new HashSet<>();
		List<Line2D> polyLine = getLine2DOfPolygon(poly);
		Point2D point = null;
		for (Line2D next : polyLine) {
			point = GeometryTools2D.getSegmentIntersectionPoint(next, line);
			if (point != null)
				result.add(point);
		}
		
		return result;
	}
	
	public static boolean isIntersect(Line2D p_line, Line2D q_line){
		if (isSameLine(p_line, q_line))
			return false;

		p_line.getEndPoint().getX();
		double P1Q1_x = p_line.getOrigin().getX() - q_line.getOrigin().getX();
		double P1Q1_y = p_line.getOrigin().getY() - q_line.getOrigin().getY();
		double Q2Q1_x = q_line.getEndPoint().getX() - q_line.getOrigin().getX();
		double Q2Q1_y = q_line.getEndPoint().getY() - q_line.getOrigin().getY();
		double P2Q1_x = p_line.getEndPoint().getX() - q_line.getOrigin().getX();
		double P2Q1_y = p_line.getEndPoint().getY() - q_line.getOrigin().getY();
		double reslut_1 = (P1Q1_x * Q2Q1_y - Q2Q1_x * P1Q1_y) * (P2Q1_x * Q2Q1_y - Q2Q1_x * P2Q1_y);
		boolean flag_1 = (reslut_1 <= 0);
		if (!flag_1)
			return false;

		double Q1P1_x = q_line.getOrigin().getX() - p_line.getOrigin().getX();
		double Q1P1_y = q_line.getOrigin().getY() - p_line.getOrigin().getY();
		double P2P1_x = p_line.getEndPoint().getX() - p_line.getOrigin().getX();
		double P2P1_y = p_line.getEndPoint().getY() - p_line.getOrigin().getY();
		double Q2P1_x = q_line.getEndPoint().getX() - p_line.getOrigin().getX();
		double Q2P1_y = q_line.getEndPoint().getY() - p_line.getOrigin().getY();
		double result_2 = (Q1P1_x * P2P1_y - P2P1_x * Q1P1_y) * (Q2P1_x * P2P1_y - P2P1_x * Q2P1_y);
		boolean flag_2 = (result_2 <= 0);
		if (!flag_2)
			return false;

		return true;
	}
	
	public static boolean isSameLine(Line2D line1, Line2D line2){
		boolean flag_1 = (Math.abs(line1.getOrigin().getX() - line2.getOrigin().getX()) < 0.000001)
			&& (Math.abs(line1.getOrigin().getY() - line2.getOrigin().getY()) < 0.000001)
			&& (Math.abs(line1.getEndPoint().getX() - line2.getEndPoint().getX()) < 0.000001)
			&& (Math.abs(line1.getEndPoint().getY() - line2.getEndPoint().getY()) < 0.000001);
		
		boolean flag_2 = (Math.abs(line1.getOrigin().getX() - line2.getEndPoint().getX()) < 0.000001)
			&& (Math.abs(line1.getOrigin().getY() - line2.getEndPoint().getY()) < 0.000001)
			&& (Math.abs(line1.getEndPoint().getX() - line2.getOrigin().getX()) < 0.000001)
			&& (Math.abs(line1.getEndPoint().getY() - line2.getOrigin().getY()) < 0.000001);

		if (flag_1 || flag_2)
			return true;
		else
			return false;
	}

	
	
	public static boolean isIntersectBetweenLines(Line2D li_1, Line2D li_2) {
		return isIntersectBetweenLines(li_1.getOrigin(), li_1.getEndPoint(),
				li_2.getOrigin(), li_2.getEndPoint());
	}
	
	public static boolean isIntersectBetweenLines(Point2D p1, Point2D q1, Point2D p2, Point2D q2) {
		int p1q1p2 = getDirection(p1, q1, p2);
		int p1q1q2 = getDirection(p1, q1, q2);
		int p2q2p1 = getDirection(p2, q2, p1);
		int p2q2q1 = getDirection(p2, q2, q1);
		if (p1q1p2 * p1q1q2 != 1 && p2q2p1 * p2q2q1 != 1)
			return true;

		if ((p1q1p2 == 0 && p1q1q2 == 0 && p2q2p1 == 0 && p2q2q1 == 0)
				&& (isInBetween(p1.getX(), p2.getX(), q1.getX())
						|| isInBetween(p1.getX(), q2.getX(), q1.getX()) 
						|| isInBetween(p2.getX(), p1.getX(), q2.getX()))
						
				&& (isInBetween(p1.getY(), p2.getY(), q1.getY())
						|| isInBetween(p1.getY(), q2.getY(), q1.getY()) 
						|| isInBetween(p2.getY(), p1.getY(), q2.getY())))
			return true;

		return false;
	}

	public static int getDirection(Point2D p1, Point2D p2, Point2D p3) {
		// int v1 = (p2.getY() - p1.getY()) * (p3.getX() - p2.getX());
		// int v2 = (p3.getY() - p2.getY()) * (p2.getX() - p1.getX());
		int p1X = (int) p1.getX();
		int p1Y = (int) p1.getY();
		int p2X = (int) p2.getX();
		int p2Y = (int) p2.getY();
		int p3X = (int) p3.getX();
		int p3Y = (int) p3.getY();

		int minX = Math.min(p1X, Math.min(p2X, p3X));
		int minY = Math.min(p1Y, Math.min(p2Y, p3Y));
		p1X -= minX;
		p2X -= minX;
		p3X -= minX;
		p1Y -= minY;
		p2Y -= minY;
		p3Y -= minY;

		long v1 = ((long) p2Y - (long) p1Y) * ((long) p3X - (long) p2X);
		long v2 = ((long) p3Y - (long) p2Y) * ((long) p2X - (long) p1X);
		if (v1 > v2)
			return 1;
		if (v1 < v2)
			return -1;
		return 0;
	}
	
	public static boolean isInBetween(double a, double b, double c) {
		double min = a, max = c;
		if (min > max) {
			min = c;
			max = a;
		}
		
		return min < b && b < max;
	}

	//作每条边的垂线,然后延长size长度,将每条线平移
	public static Polygon scaleBySize(Polygon polygon, double size) {
		Polygon result = new Polygon();
		rescuecore2.misc.geometry.Point2D center = new rescuecore2.misc.geometry.Point2D(polygon.getBounds().getCenterX(), polygon.getBounds().getCenterY());
		List<rescuecore2.misc.geometry.Line2D> polyLines = getLines(polygon);

		//遍历polygon的所有lines
		for (rescuecore2.misc.geometry.Line2D line2D : polyLines) {

			//获取边到中心最近的点
			rescuecore2.misc.geometry.Point2D p1 = closestPoint(line2D, center);
			//获取中心到边最近的点的连线
			rescuecore2.misc.geometry.Line2D ln = new rescuecore2.misc.geometry.Line2D(center, p1);
			//延长size长度
			ln = improveLine(ln, size);
			rescuecore2.misc.geometry.Point2D p2 = ln.getEndPoint();
			double dx = p2.getX() - p1.getX();
			double dy = p2.getY() - p1.getY();

			rescuecore2.misc.geometry.Point2D origin = new rescuecore2.misc.geometry.Point2D(
					line2D.getOrigin().getX() + dx,
					line2D.getOrigin().getY() + dy
			);
			result.addPoint((int) origin.getX(), (int) origin.getY());

			rescuecore2.misc.geometry.Point2D end = new rescuecore2.misc.geometry.Point2D(
					line2D.getEndPoint().getX() + dx,
					line2D.getEndPoint().getY() + dy
			);
			result.addPoint((int) end.getX(), (int) end.getY());
		}
		return result;

	}

	public static List<rescuecore2.misc.geometry.Line2D> getLines(Polygon polygon) {
		List<rescuecore2.misc.geometry.Line2D> lines = new ArrayList<Line2D>();
		int count = polygon.npoints;
		for (int i = 0; i < count; i++) {
			int j = (i + 1) % count;
			rescuecore2.misc.geometry.Point2D p1 = new rescuecore2.misc.geometry.Point2D(polygon.xpoints[i], polygon.ypoints[i]);
			rescuecore2.misc.geometry.Point2D p2 = new rescuecore2.misc.geometry.Point2D(polygon.xpoints[j], polygon.ypoints[j]);
			rescuecore2.misc.geometry.Line2D line = new rescuecore2.misc.geometry.Line2D(p1, p2);
			lines.add(line);
		}
		return lines;
	}

	/**
	 * MTN
	 * get closest point of line from a point
	 *
	 * @param line  target point that we want get nearest point of it from another point
	 * @param point a point2D
	 * @return Point2D
	 */
	public static rescuecore2.misc.geometry.Point2D closestPoint(rescuecore2.misc.geometry.Line2D line, rescuecore2.misc.geometry.Point2D point) {
		return GeometryTools2D.getClosestPoint(line, point);
	}

	public static List<rescuecore2.misc.geometry.Point2D> getPoint2DList(int[] xs, int[] ys) {

		List<rescuecore2.misc.geometry.Point2D> points = new ArrayList<rescuecore2.misc.geometry.Point2D>();
		for (int i = 0; i < xs.length; i++) {
			points.add(new rescuecore2.misc.geometry.Point2D(xs[i], ys[i]));
		}

		return points;
	}

	public static double getAngle(rescuecore2.misc.geometry.Vector2D v1, rescuecore2.misc.geometry.Vector2D v2) {
		double flag = (v1.getX() * v2.getY()) - (v1.getY() * v2.getX());
		double angle = Math.acos(((v1.getX() * v2.getX()) + (v1.getY() * v2.getY())) / (v1.getLength() * v2.getLength()));
		if (flag > 0) {
			return angle;
		}
		if (flag < 0) {
			return -1 * angle;
		}
		return 0.0D;
	}

	/**
	* @Description: 获取两直线所成锐角
	* @Author: Guanyu-Cai
	* @Date: 3/16/20
	*/
	public static double getAngle(Line2D l1, Line2D l2) {
		Vector2D v1 = l1.getDirection();
		Vector2D v2 = l2.getDirection();
		double temp = (v1.getX() * v2.getX() + v1.getY() * v2.getY()) / (v1.getLength() * v2.getLength());
		if (temp > 0) {
			return Math.acos(temp);
		}
		if (temp < 0) {
			return Math.acos(-1 * temp);
		}
		return 0.0D;
	}

	/**
	* @Description: convert from rescuecore2.misc.geometry.Line2D to math.geom2d.line.Line2D
	* @Author: Guanyu-Cai
	* @Date: 3/16/20
	*/
	public static math.geom2d.line.Line2D convertLine(Line2D line2D) {
		double x1 = line2D.getOrigin().getX();
		double y1 = line2D.getOrigin().getY();
		double x2 = line2D.getEndPoint().getX();
		double y2 = line2D.getEndPoint().getY();
		return new math.geom2d.line.Line2D(x1, y1, x2, y2);
	}

	/**
	 * @Description: convert from rescuecore2.misc.geometry.Line2D to java.awt.geom.Line2D
	 * @Author: CSU-zack
	 */
	public static java.awt.geom.Line2D convertLine2(Line2D line2D) {
		double x1 = line2D.getOrigin().getX();
		double y1 = line2D.getOrigin().getY();
		double x2 = line2D.getEndPoint().getX();
		double y2 = line2D.getEndPoint().getY();
		return new java.awt.geom.Line2D.Double(x1, y1, x2, y2);
	}

	/**
	 * @Description: 判断两条直线是否共线
	 * @Author: Guanyu-Cai
	 * @Date: 3/16/20
	 */
	public static boolean isCollinear(math.geom2d.line.Line2D l1, math.geom2d.line.Line2D l2, double threshold) {
		if (!isCollinear(l1.getVector(), l2.getVector(), threshold)) {//判断向量是否共线
			return false;
		} else {
			double dx = l1.getX2() - l1.getX1();
			double dy = l1.getY2() - l1.getY1();
			if (Math.abs(dx) > Math.abs(dy)) {
				return Math.abs((l2.getX1() - l1.getX1()) * dy / dx + l1.getY1() - l2.getY1()) <= CSUConstants.COLLINEAR_THRESHOLD;
			} else {
				return Math.abs((l2.getY1() - l1.getY1()) * dx / dy + l1.getX1() - l2.getX1()) <= CSUConstants.COLLINEAR_THRESHOLD;
			}
		}
	}

	public static boolean containsEach(Collection collection1, Collection collection2) {
		for (Object object : collection1) {
			if (collection2.contains(object)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isPassable(Polygon polygon, Polygon polygon1, int agentPassingThreshold) {

		int count = polygon1.npoints;
		int j;
		double tempDistance;
		boolean isPassable = false;
		for (int i = 0; i < count; i++) {
			j = (i + 1) % count;
			rescuecore2.misc.geometry.Point2D startPoint = new rescuecore2.misc.geometry.Point2D(polygon1.xpoints[i], polygon1.ypoints[i]);
			rescuecore2.misc.geometry.Point2D endPoint = new rescuecore2.misc.geometry.Point2D(polygon1.xpoints[j], polygon1.ypoints[j]);
			if (startPoint.equals(endPoint)) {
				continue;
			}
			rescuecore2.misc.geometry.Line2D poly2Line = new rescuecore2.misc.geometry.Line2D(startPoint, endPoint);
			tempDistance = Ruler.getDistance(poly2Line, polygon);
			if (tempDistance < agentPassingThreshold) {
				isPassable = true;
				break;
			}
		}
		return isPassable;

	}

	/**
	* @Description: 判断两条line是否共线
	* @param thresholdAngle 弧度制角度
	* @Author: Guanyu-Cai
	* @Date: 3/16/20
	*/
//	public static boolean isColinear(Line2D l1, Line2D l2, double thresholdAngle) {
//		// TODO: 3/17/20 有问题 
//		double angle = getAngle(l1.getDirection(), l2.getDirection());
//		return angle <= thresholdAngle;
//	}

	/**
	* @Description: 判断两向量是否共线
	* @Author: Guanyu-Cai
	* @Date: 3/17/20
	*/
	public static boolean isCollinear(math.geom2d.Vector2D v1, math.geom2d.Vector2D v2, double threshold) {
		v1 = v1.getNormalizedVector();
		v2 = v2.getNormalizedVector();
		return Math.abs(v1.getX() * v2.getY() - v1.getY() * v2.getX()) < threshold;
	}

	public static List<Integer> fetchIdValueFromElements(Collection<StandardEntity> elements) {
		try {
			return elements.stream().map(entity -> entity.getID().getValue()).collect(Collectors.toList());
		} catch (Exception ex) {
			return new ArrayList<>();
		}
	}

	public static List<Integer> fetchIdValueFromElementIds(Collection<EntityID> elementEntityIDs) {
		try {
			return elementEntityIDs.stream().map(EntityID::getValue).collect(Collectors.toList());
		} catch (Exception ex) {
			return new ArrayList<>();
		}
	}

	public static List<EntityID> fetchEntityIdFromElements(Collection<StandardEntity> elements) {
		try {
			return elements.stream().map(AbstractEntity::getID).collect(Collectors.toList());
		} catch (Exception ex) {
			return new ArrayList<>();
		}
	}

	public static Set<EntityID> fetchEntityIdFromIdValues(Collection<Integer> ids) {
		try {
			return ids.stream().map(EntityID::new).collect(Collectors.toSet());
		} catch (Exception ex) {
			return new HashSet<>();
		}
	}

	public static class DistanceComparator implements Comparator<Pair<Point2D, Point2D>> {
		private Point2D reference;

		/**
		 * Create a DistanceSorter.
		 *
		 * @param reference The reference point to measure distances from.
		 */
		public DistanceComparator(Point2D reference) {
			this.reference = reference;
		}

		/**
		 * Compares the standard entities according to distance.
		 *
		 * @param a First pair points to compare
		 * @param b Second pair points to compare
		 * @return The difference between distances.
		 */

		@Override
		public int compare(Pair<Point2D, Point2D> a, Pair<Point2D, Point2D> b) {
			double d1 = Ruler.getDistance(reference, a.first());
			double d2 = Ruler.getDistance(reference, b.first());
			return Double.compare(d1, d2);
		}
	}

	public static class LengthComparator implements Comparator<Line2D> {

		public LengthComparator() {
		}

		@Override
		public int compare(Line2D a, Line2D b) {
			double l1 = Ruler.getLength(a);
			double l2 = Ruler.getLength(b);
			return Double.compare(l1, l2);
		}
	}

	public static class AngleComparator implements Comparator<Pair<CSUEdge, Line2D>> {
		private Line2D reference;

		public AngleComparator(Line2D reference) {
			this.reference = reference;
		}

		@Override
		public int compare(Pair<CSUEdge, Line2D> a, Pair<CSUEdge, Line2D> b) {
			double angle1 = Math.abs(getAngle(reference, a.second()));
			double angle2 = Math.abs(getAngle(reference, b.second()));
			return Double.compare(angle1, angle2);
		}
	}

//	public static boolean isIntersect(Line2D p_line, Line2D q_line) {
//		double P1Q1_x = p_line.getOrigin().getX() - q_line.getOrigin().getX();
//		double P1Q1_y = p_line.getOrigin().getY() - q_line.getOrigin().getY();
//		double Q2Q1_x = q_line.getEndPoint().getX() - q_line.getOrigin().getX();
//		double Q2Q1_y = q_line.getEndPoint().getY() - q_line.getOrigin().getY();
//		double P2Q1_x = p_line.getEndPoint().getX() - q_line.getOrigin().getX();
//		double P2Q1_y = p_line.getEndPoint().getY() - q_line.getOrigin().getY();
//		double reslut_1 = (P1Q1_x * Q2Q1_y - Q2Q1_x * P1Q1_y)
//				* (P2Q1_x * Q2Q1_y - Q2Q1_x * P2Q1_y);
//		boolean flag_1 = (reslut_1 <= 0);
//		if (!flag_1)
//			return false;
//
//		double Q1P1_x = q_line.getOrigin().getX() - p_line.getOrigin().getX();
//		double Q1P1_y = q_line.getOrigin().getY() - p_line.getOrigin().getY();
//		double P2P1_x = p_line.getEndPoint().getX() - p_line.getOrigin().getX();
//		double P2P1_y = p_line.getEndPoint().getY() - p_line.getOrigin().getY();
//		double Q2P1_x = q_line.getEndPoint().getX() - p_line.getOrigin().getX();
//		double Q2P1_y = q_line.getEndPoint().getY() - p_line.getOrigin().getY();
//		double result_2 = (Q1P1_x * P2P1_y - P2P1_x * Q1P1_y)
//				* (Q2P1_x * P2P1_y - P2P1_x * Q2P1_y);
//		boolean flag_2 = (result_2 <= 0);
//		if (!flag_2)
//			return false;
//
//		return true;
//	}
//    
//    /**
//     * This method finds the blockade which the [code]human[/code] is in it.
//     *
//     * @param world Model of the world
//     * @param human The selected human
//     * @return human covering blockade
//     */
//    public static Blockade findCoveringBlockade(AdvancedWorldModel world, Human human) {
//        Blockade coveringBlockade = null;
//        if (human != world.getControlledEntity() && !human.isPositionDefined()) {
//            // Do nothing.
//        } else {
//            StandardEntity se = world.getEntity(human.getPosition());
//            if (se instanceof Road) {
//                Blockade blockade;
//                Road road = (Road) se;
//                Shape shape;
//                if (road.isBlockadesDefined()) {
//                    for (EntityID id : road.getBlockades()) {
//                        blockade = (Blockade) world.getEntity(id);
//                        if (blockade != null && blockade.isApexesDefined()) {
//                            shape = Util.getPolygon(blockade.getApexes());
//                            if (shape.contains(human.getX(), human.getY())) {
//                                coveringBlockade = blockade;
//                                break;
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return coveringBlockade;
//    }
//    
//    /**
//     * This method determines whether the <code>human</code> is inside the <code>coveringBlockade</code> or not
//     *
//     * @param world            Model of the world
//     * @param human            The human to check his stickiness
//     * @param coveringBlockade The blockade to check it covering
//     * @return True if the <code>human</code> is inside the <code>coveringBlockade</code>
//     */
//    public static boolean isOnBlockade(AdvancedWorldModel world, Human human, Blockade coveringBlockade) {
//        boolean isOnBlockade = false;
//        if (!human.getID().equals(world.getControlledEntity().getID()) 
//        		&& !human.isPositionDefined() || !isBlockadeExist(world, coveringBlockade)) {
//            isOnBlockade = false;
//        } else {
//
//            Polygon shape;
//            if (coveringBlockade.isApexesDefined()) {
//                shape = Util.getPolygon(coveringBlockade.getApexes());
//                if (shape.contains(human.getX(), human.getY())) {
//                    isOnBlockade = true;
//                }
//            }
//        }
//        return isOnBlockade;
//    }
//    
//    private static boolean isBlockadeExist(AdvancedWorldModel world, Blockade blockade) {
//
//        boolean isExist;
//        if (blockade == null) {
//            isExist = false;
//        } else {
//            Road containingRoad = (Road) world.getEntity(blockade.getPosition());
//            if (!containingRoad.isBlockadesDefined() 
//            		|| !containingRoad.getBlockades().contains(blockade.getID())) {
//                isExist = false;
//            } else {
//                isExist = true;
//            }
//        }
//
//        return isExist;
//    }

	public static int getdistance(Pair<Integer, Integer> position1, Pair<Integer, Integer> position2){
		float x1 = position1.first();
		float y1 = position1.second();
		float x2 = position2.first();
		float y2 = position2.second();
		float dx = x1 - x2;
		float dy = y1 - y2;
		return (int) Math.sqrt(dx * dx + dy * dy);
	}

	public static Point convertPoint(Point2D point2D) {
		return new Point((int) point2D.getX(), (int) point2D.getY());
	}
}
