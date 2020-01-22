package mrl_2019.complex.firebrigade;

import javolution.util.FastSet;
import math.geom2d.Point2D;
import mrl_2019.MRLConstants;
import mrl_2019.algorithm.clustering.ConvexHull;
import mrl_2019.world.MrlWorldHelper;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Sajjad
 * Date: 5/5/12
 * Time: 6:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class BorderEntities {

    MrlWorldHelper world;

    public BorderEntities(MrlWorldHelper world) {
        this.world = world;
    }

    public Set<EntityID> getBordersOf(double scale) {
        String mapName = world.getMapName();
        if (mapName == null) {
            mapName = Long.toString(world.getUniqueMapNumber());
        }
        String filename = MRLConstants.PRECOMPUTE_DIRECTORY + mapName + ".bom";

        if (new File(filename).exists()) {
            try {
                return readBOM(filename);
            } catch (Exception e) {
                System.err.println("Cannot load BOM data!!!!   " + e.getMessage());
            }
        }

        Set<EntityID> allEntities = world.getBuildingIDs();
        Set<EntityID> borderBuildings = getBordersOf(allEntities, scale);

//        if (LaunchMRL.shouldPrecompute) {
            try {
                return createBOM(filename, borderBuildings);
            } catch (IOException e) {
                System.err.println("Cannot create BOM data!!!!   " + e.getMessage());
            }
//        }
        return borderBuildings;
    }

    private Set<EntityID> readBOM(String fileName) throws IOException {
        Set<EntityID> borderBuildings = new HashSet<EntityID>();
        File f = new File(fileName);
        BufferedReader br = new BufferedReader(new FileReader(f));
        String nl;
        while (null != (nl = br.readLine())) {
            String[] xy = nl.split(",");
            int x = Integer.parseInt(xy[0]);
            int y = Integer.parseInt(xy[1]);

            Building b = world.getBuildingInPoint(x, y);
            borderBuildings.add(b.getID());
        }
        br.close();
        if (MRLConstants.DEBUG_FIRE_BRIGADE) {
            System.out.println("Read from file:" + fileName);
        }
        return borderBuildings;
    }

    private Set<EntityID> createBOM(String fileName, Set<EntityID> borderBuildings) throws IOException {
        if (MRLConstants.DEBUG_FIRE_BRIGADE) {
            System.out.println("  Creating BOM Files .... ");
        }

        File f;
        BufferedWriter bw = null;
//        if (world.shouldPrecompute) {
//            f = new File(fileName);
//            f.createNewFile();
//            bw = new BufferedWriter(new FileWriter(f));
//        }

        for (EntityID id : borderBuildings) {
            Building b = (Building) world.getEntity(id);
            if (bw != null) {
                bw.write(b.getX() + "," + b.getY() + "\n");
            }
        }
        if (bw != null) {
            bw.close();
        }
        return borderBuildings;
    }

    public Set<EntityID> getBordersOf(Set<EntityID> allEntities, double scale) {
        ConvexHull convexHull = new ConvexHull();

        for (EntityID id : allEntities) {
            Pair<Integer, Integer> location = world.getWorldInfo().getLocation(id);
            convexHull.addPoint(location.first(), location.second());
        }

        Set<EntityID> borderEntities = getBorderEntities(convexHull, allEntities, scale);

        return borderEntities;
    }

    /**
     * this method calculates the border entities, using entities and convexHull of the entities
     *
     * @param convex   is the convex of all Entities
     * @param entities are the self entities
     * @param scale    is the scale for making smaller convex hull
     */
    public Set<EntityID> getBorderEntities(ConvexHull convex, Set<EntityID> entities, double scale) {
        if (scale >= 1.0) {
            System.err.println("scale should not be over 1.0! check it in border entities, border entities doesn't work now!");
            return null;
        }

        Building building;
        Polygon convexObject = convex.convex();
        Set<EntityID> borderEntities = new FastSet<EntityID>();

        if (convexObject.npoints == 0) { // I don't know why this happens, report me if this error writes usually! TODO check this if something comes wrong here
            System.out.println("Something gone wrong in setting border entities for Firebrigade!!!");
            return null;
        }

        Polygon smallBorderPolygon = scalePolygon(convexObject, scale);
//        Polygon bigBorderPolygon = scalePolygon(convexObject, 1.1);

        for (EntityID entityID : entities) {

            StandardEntity entity = world.getEntity(entityID);

            if (entity instanceof Refuge)
                continue;
            if (!(entity instanceof Building))
                continue;
            building = (Building) entity;
            int vertexes[] = building.getApexList();
            for (int i = 0; i < vertexes.length; i += 2) {

                if ((convexObject.contains(vertexes[i], vertexes[i + 1])) && !(smallBorderPolygon.contains(vertexes[i], vertexes[i + 1]))) {
                    borderEntities.add(building.getID());
                    break;
                }
            }
        }

        return borderEntities;
    }

    /**
     * This function scales a polygon by the scale coefficient
     *
     * @param sourcePolygon : Is the Polygon that we want to scale
     * @param scale         : Is the scale coefficient, It actually multiplies to the points and makes the new shape
     * @return : returns the scaled polygon which, its center is on the center of the last polygon
     */
    protected Polygon scalePolygon(Polygon sourcePolygon, double scale) {
        Polygon scaledPolygon;

        int xs[] = new int[sourcePolygon.npoints];
        int ys[] = new int[sourcePolygon.npoints];
        Point2D p, p1;
        int sumX = 0;
        int sumY = 0;

        for (int i = 0; i < sourcePolygon.npoints; i++) {
            p = new Point2D(sourcePolygon.xpoints[i], sourcePolygon.ypoints[i]);
            p1 = p.scale(scale);
            sumX += p1.getX();
            sumY += p1.getY();
            xs[i] = (int) p1.getX();
            ys[i] = (int) p1.getY();
            p.clone();
        }

        Polygon preScaledPolygon = new Polygon(xs, ys, sourcePolygon.npoints);
        scaledPolygon = reAllocatePolygon(preScaledPolygon, sourcePolygon);
        if (scaledPolygon == null)
            scaledPolygon = preScaledPolygon;
        return scaledPolygon;
    }

    /**
     * This function changes the position of the polygon which is scaled by the "scalePolygon" function. If we don't use this function the scaled polygon does not appear in the right place.
     *
     * @param scaled: is the scaled polygon of our source (notice that it is not in the right place)
     * @param source: is the source polygon, (that is not scaled) we want it to determine the exact position of our scaled polygon
     * @return: returns the new polygon that is in the right place (its center is exactly on the old center)
     */
    protected Polygon reAllocatePolygon(Polygon scaled, Polygon source) {
        if (source == null || scaled == null || source.npoints == 0 || scaled.npoints == 0)
            return null;
        Polygon reAllocated;
        int xs[] = new int[scaled.npoints];
        int ys[] = new int[scaled.npoints];

        int sourceCenterX = 0;
        int sourceCenterY = 0;

        int scaledCenterX = 0;
        int scaledCenterY = 0;

        for (int i = 0; i < scaled.npoints; i++) {
            sourceCenterX += source.xpoints[i];
            sourceCenterY += source.ypoints[i];

            scaledCenterX += scaled.xpoints[i];
            scaledCenterY += scaled.ypoints[i];
        }

        sourceCenterX = sourceCenterX / source.npoints;
        sourceCenterY = sourceCenterY / source.npoints;

        scaledCenterX = scaledCenterX / scaled.npoints;
        scaledCenterY = scaledCenterY / scaled.npoints;

        int xDistance = sourceCenterX - scaledCenterX;
        int yDistance = sourceCenterY - scaledCenterY;

        for (int i = 0; i < scaled.npoints; i++) {
            xs[i] = scaled.xpoints[i] + xDistance;
            ys[i] = scaled.ypoints[i] + yDistance;
        }
        reAllocated = new Polygon(xs, ys, scaled.npoints);

        return reAllocated;
    }

}
