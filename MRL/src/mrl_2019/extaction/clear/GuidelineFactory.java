package mrl_2019.extaction.clear;


import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import mrl_2019.MRLConstants;
import mrl_2019.algorithm.simpleSearch.BreadthFirstSearch;
import mrl_2019.algorithm.simpleSearch.DistanceInterface;
import mrl_2019.algorithm.simpleSearch.Graph;
import mrl_2019.util.Util;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.entity.MrlEdge;
import mrl_2019.world.entity.MrlRoad;
import mrl_2019.world.entity.Path;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Mahdi
 */
public class GuidelineFactory {
    protected WorldInfo worldInfo;
    protected MrlWorldHelper worldHelper;
    protected AgentInfo agentInfo;
    protected ScenarioInfo scenarioInfo;
    protected List<Path> paths;
//    private boolean fileReadWrite;
    private double distanceThreshold = MRLConstants.AGENT_SIZE / 2;
//    private File file;

    public GuidelineFactory(MrlWorldHelper worldHelper,WorldInfo world, AgentInfo agentInfo, ScenarioInfo scenarioInfo) {
        this.worldInfo = worldInfo;
        this.agentInfo = agentInfo;
        this.scenarioInfo = scenarioInfo;
        paths = new ArrayList<>(worldHelper.getPaths());

//        fileReadWrite = false;
//        file = new File(MRLConstants.PRECOMPUTE_DIRECTORY + world.getUniqueMapNumber() + ".txt");
    }


    public void generateGuidelines() {
        Long before = System.currentTimeMillis();
        //prepare file
//        if (fileReadWrite) {
//            prepareFile();
//        }

//        if (!fileReadWrite) {//should read from file
//            if (readFromFile()) {
//                //System.out.println("Generate guidelines took " + (System.currentTimeMillis() - before) + "ms");
//                return;//successful
//            }
//        }

        //merge inline paths
        mergePaths(paths);

        for (Path path : paths) {
            Set<GuideLine> guidelines = findGuidelines(path);
            path.setGuideLines(guidelines);
        }

        //write into file
//        if (fileReadWrite) {
//            putIntoFile(paths);
//        }

        //System.out.println("Generate guidelines took " + (System.currentTimeMillis() - before) + "ms");
    }

    private Set<GuideLine> findGuidelines(Path path) {
        Road absSource = path.getHeadOfPath();
        Road absDestination = path.getEndOfPath();
        if (absSource.equals(absDestination)) {
            System.out.println("HEAD AND END OF PATH ARE EQUAL<----------------");
            return new HashSet<>();
        }


        List<EntityID> wholePath = new ArrayList<>(planMove(absSource, absDestination, new ArrayList<>(path)));
        path.setHeadToEndRoads(wholePath);

        Set<GuideLine> guideLines = new HashSet<>();


        int srcIndex = 0;
        int dstIndex = wholePath.size() - 1;
        Road dst;


        Edge edgeTo1 = absSource.getEdgeTo(wholePath.get(srcIndex + 1));
        if (edgeTo1 == null) {
            System.out.println("EDGE-TO IS NULL <------------------");
            return new HashSet<>();
        }
        Point2D mid1 = Util.getMiddle(edgeTo1.getLine());
        Edge edgeTo = absDestination.getEdgeTo(wholePath.get(dstIndex - 1));
        if (edgeTo == null) {
            System.out.println("EDGE-TO IS NULL <------------------");
            return new HashSet<>();
        }
        Point2D mid2 = Util.getMiddle(edgeTo.getLine());
        GuideLine semiGuideline = new GuideLine(mid1.getX(), mid1.getY(), mid2.getX(), mid2.getY());

        do {
            dst = absDestination;
            dstIndex = wholePath.size() - 1;

            for (int i = dstIndex; i > srcIndex; i--) {
                MrlRoad road = worldHelper.getMrlRoad(wholePath.get(i));
                if (road == null) {
                    System.out.println("THIS IS NOT ROAD! <----------------");
                    continue;
                }
                List<MrlEdge> mrlEdgesTo = road.getMrlEdgesTo(wholePath.get(i - 1));
                boolean closeEnough = false;
                Point2D dstPoint = new Point2D(dst.getX(), dst.getY());

                for (MrlEdge mrlEdge : mrlEdgesTo) {
                    dstPoint = mrlEdge.getMiddle();
                    double distance = Util.distance(semiGuideline, dstPoint);

                    if (distance < distanceThreshold) {
                        closeEnough = true;
                        break;
                    }

                }

                if (!closeEnough && srcIndex < i - 1) {//todo 2nd condition should reviewed
                    dstIndex = i - 1;
                    dst = (Road) worldInfo.getEntity(wholePath.get(dstIndex));
                    semiGuideline = new GuideLine(semiGuideline.getOrigin().getX(), semiGuideline.getOrigin().getY(), dstPoint.getX(), dstPoint.getY());
                }

            }
            if (Util.lineLength(semiGuideline) < 1) {
//                System.out.println("Unexpected guideline. src:" + srcIndex + " \tdst:" + dstIndex);
            } else {
                semiGuideline.setAreas(wholePath.subList(srcIndex + 1, dstIndex + 1));
                semiGuideline.setMinor(false);
                guideLines.add(semiGuideline);
            }
            if (!dst.getID().equals(absDestination.getID())) {
                srcIndex = dstIndex;
                semiGuideline = new GuideLine(semiGuideline.getEndPoint().getX(), semiGuideline.getEndPoint().getY(), mid2.getX(), mid2.getY());
            }
        } while (!dst.getID().equals(absDestination.getID()));
        return guideLines;
    }


    /**
     * BFS plan move instead of A*
     *
     * @param src  source road
     * @param dst  destination road
     * @param path list of Road that should we should find path in it.
     * @return path
     */
    private List<EntityID> planMove(Road src, Road dst, List<Area> path) {
        List<EntityID> plan = new ArrayList<>();
        Graph graph = new Graph(path);
        BreadthFirstSearch bfs = new BreadthFirstSearch();
        plan.add(src.getID());
        plan.addAll(bfs.search(src.getID(), dst.getID(), graph, new DistanceInterface(worldInfo)));
        return plan;
    }


    private void mergePaths(List<Path> paths) {
        //do nothing
    }


//    private void putIntoFile(List<Path> paths) {
//        boolean couldWrite = false;
////        File file = null;
////        file = new File(MRLConstants.PRECOMPUTE_DIRECTORY + world.getUniqueMapNumber() + ".gdln");
//        FileOutputStream fileOutputStream = null;
//        try {
//            fileOutputStream = new FileOutputStream(file);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return;
//        }
//
//        StringBuilder strBuilder = new StringBuilder();
//        for (Path path : paths) {
//            strBuilder.append(path.getId());
//            strBuilder.append("\n");
//
//            strBuilder.append(path.getGuideLines().size());
//            strBuilder.append("\n");
//
//            for (GuideLine guideLine : path.getGuideLines()) {
//                strBuilder.append(guideLine.getOrigin());
//                strBuilder.append("\n");
//                strBuilder.append(guideLine.getEndPoint());
//                strBuilder.append("\n");
//
//                strBuilder.append(guideLine.getAreas());
//                strBuilder.append("\n");
//
//            }
//        }
//        OutputStreamWriter osr = new OutputStreamWriter(fileOutputStream);
//        try {
//            osr.write(strBuilder.toString());
//            osr.flush();
//            osr.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//    }


//    private boolean readFromFile() {
//
//        if (!file.exists() || !file.isFile() || !file.canRead()) {
//            return false;
//        }
//
//
//        String line;
//        try {
////            FileReader fileReader = new FileReader(file);
//
////            BufferedReader br = new BufferedReader(fileReader);
//
////            Scanner scan = new Scanner(file);
//            String s = new Scanner(file).useDelimiter("\\Z").next();
//            Scanner scan = new Scanner(s);
//
////            InputStream fis = new FileInputStream(file);
////            InputStreamReader isr = new InputStreamReader(fis);
////            BufferedReader br = new BufferedReader(isr);
//
//            //path id
//            Path path;
//            while (scan.hasNextLine()) {
//                line = scan.nextLine();
//                if (line == null) {
//                    return false;
//                }
//                path = world.getPath(new EntityID(Integer.parseInt(line)));
//
//                //////guidelines
//                //guideline count
//                line = scan.nextLine();
//
//                int count = Integer.parseInt(line);
//                Set<GuideLine> guideLines = new HashSet<>();
//                for (int i = 0; i < count; i++) {
//                    line = scan.nextLine();
//                    Point2D head = convertToPoint(line);
//
//                    line = scan.nextLine();
//                    Point2D end = convertToPoint(line);
//                    GuideLine guideLine = new GuideLine(head, end);
//
//                    //guideline areas contains
//                    line = scan.nextLine();
//                    guideLine.setAreas(convertToEntityIDList(line));
//
//                    guideLine.setMinor(false);
//                    guideLines.add(guideLine);
//                }
//
//
//                path.setGuideLines(guideLines);
////            while ((line = scan.nextLine()) != null) {
////               line
////            }
//            }
//            scan.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        } catch (NoSuchElementException ex) {
//            return false;
//        }
//        return true;
//    }

    private Point2D convertToPoint(String str) {
        int index = str.indexOf(',');
        double x = Double.parseDouble(str.substring(0, index - 1));
        double y = Double.parseDouble(str.substring(index + 2));
        return new Point2D(x, y);
    }

    private List<EntityID> convertToEntityIDList(String str) {

        int index, begin = 1;
        List<EntityID> list = new ArrayList<>();
        while ((index = str.indexOf(',', begin)) > 0) {
            EntityID id = new EntityID(Integer.parseInt(str.substring(begin, index)));

            list.add(id);
            begin = index + 2;
        }
        if (begin > 1) {
            index = str.indexOf(']', begin);
            EntityID id = new EntityID(Integer.parseInt(str.substring(begin, index)));
            list.add(id);
        }
        return list;
    }


//    private void prepareFile() {
//        boolean couldCreate = false;
//        try {
////            File file = new File(MRLConstants.PRECOMPUTE_DIRECTORY + world.getUniqueMapNumber() + ".gdln");
//            if (!file.exists() || !file.isFile()) {
//                couldCreate = file.createNewFile();
//
//            } else {
////                file.delete();
////                file.createNewFile();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            if (!couldCreate) {
////                fileReadWrite = false;
//            }
//        }
//    }


}
