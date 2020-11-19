package CSU_Yunlu_2020.world;

import CSU_Yunlu_2020.CSUConstants;
import CSU_Yunlu_2020.module.algorithm.fb.CSUFireClustering;
import CSU_Yunlu_2020.world.object.CSUBuilding;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.AbstractModule;
import javolution.util.FastSet;
import rescuecore.Simulator;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @description: 改进自csu_2016
 * @Date: 03/07/2020
 */
public class CSUFireBrigadeWorld extends CSUWorldHelper{
    private CSUFireClustering fireClustering;
    private Simulator simulator;
    private float rayRate = 0.0025f;
    private CurrentState currentState = CurrentState.notExplored;

    private Set<CSUBuilding> estimatedBurningBuildings = new FastSet<CSUBuilding>();
    private String buildingConnectedFilename;

    public CSUFireBrigadeWorld(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        buildingConnectedFilename = getUniqueMapNumber() + ".cnd";
    }

    @Override
    public AbstractModule precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        try {
            long before = System.currentTimeMillis();
            processConnected(PrecomputeData.PRECOMP_DATA_DIR.getAbsolutePath() + File.separator + buildingConnectedFilename);
            long after = System.currentTimeMillis();
            System.out.println("Creation of cnd took " + (after - before) + "ms");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public CSUWorldHelper resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
        loadCND(PrecomputeData.PRECOMP_DATA_DIR.getAbsolutePath() + File.separator + buildingConnectedFilename);
        return this;
    }

    @Override
    public CSUWorldHelper preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }
        loadCND(PrecomputeData.PRECOMP_DATA_DIR.getAbsolutePath() + File.separator + buildingConnectedFilename);
        return this;
    }

    @Override
    public CSUWorldHelper updateInfo(MessageManager messageManager) {
        return super.updateInfo(messageManager);
    }

    public Set<EntityID> getAreaInShape(Shape shape) {
        Set<EntityID> result = new FastSet<>();
        for (StandardEntity next : getBuildingsWithURN(worldInfo)) {
            Area area = (Area) next;
            if (!(area.isXDefined() && area.isYDefined()))
                continue;
            Point p = new Point(area.getX(), area.getY());
            if (shape.contains(p))
                result.add(area.getID());
        }
        return result;
    }

    public Simulator getSimulator() {
        return this.simulator;
    }

//	public Map<EntityID, CSUBuilding> getCsuBuildingMap() {
//		return csuBuildingMap;
//	}
//
//	public List<EntityID> getFreeFireBrigades() {
//		List<EntityID> freeFireBrigade = new ArrayList<>();
//		List<EntityID> atRefuge = new ArrayList<>();
//		FireBrigade fireBrigade;
//
//		freeFireBrigade.addAll(getFireBrigadeIdList());
//		freeFireBrigade.removeAll(this.getStuckHandle().getStuckedAgent());
//		freeFireBrigade.removeAll(this.getBuriedHumans().getTotalBuriedHuman());
//		for (EntityID next : freeFireBrigade) {
//			fireBrigade = getEntity(next, FireBrigade.class);
//			if (!fireBrigade.isPositionDefined() || getEntity(fireBrigade.getPosition()) instanceof Refuge) {
//				atRefuge.add(next);
//			}
//		}
//		freeFireBrigade.removeAll(atRefuge);
//		return freeFireBrigade;
//	}

    public Set<CSUBuilding> getEstimatedBurningBuildings() {
        return this.estimatedBurningBuildings;
    }
//	public void setEstimatedBurningBuildings(Set<CSUBuilding> estimatedBurningBuildings) {
//	this.estimatedBurningBuildings = estimatedBurningBuildings;
//}

    /**
     * We define the four directions N, E, S, W as 0, 1, 2, 3,
     * and get the buildings in a rectangle defined by two points in the same
     * direction of the fbAgent at the diagonal.
     * When the fbAgent is in a building, we use the desktop directions.
     * When in a road, we use the road as the separator, defining L and R as 0,1.
     * @param dir
     * @return the buildings in particular direction of the fbAgent
     */
    public Set<EntityID> getBuildingsInNESW(int dir, EntityID position) {
        Set<EntityID> buildingsInDir = new FastSet<>();
        Pair<Integer, Integer> location = worldInfo.getLocation(position);
        int x = location.first();
        int y = location.second();
        int range = (int)(this.config.extinguishableDistance*0.9);
        Point pointLT = new Point(x-range, y+range);
        Point pointT = new Point(x, y+range);
        Point pointR = new Point(x+range, y);
        Point pointRB = new Point(x+range, y-range);
        Point pointB = new Point(x, y-range);
        Point pointL = new Point(x-range, y);
        Collection<StandardEntity> entities;
        switch(dir) {
            case 0:
                entities = worldInfo.getObjectsInRange(pointLT.x, pointLT.y, pointR.x, pointR.y);
                break;
            case 1:
                entities = worldInfo.getObjectsInRange(pointT.x, pointT.y, pointRB.x, pointRB.y);
                break;
            case 2:
                entities = worldInfo.getObjectsInRange(pointL.x, pointL.y, pointRB.x, pointRB.y);
                break;
            case 3:
                entities = worldInfo.getObjectsInRange(pointLT.x, pointLT.y, pointB.x, pointB.y);
                break;
            default:
                entities = worldInfo.getObjectsInRange(x-range, y-range, x+range, y+range);
        }

        for(StandardEntity se : entities) {
            if(se instanceof Building) {
                EntityID id = se.getID();
                buildingsInDir.add(id);
            }
        }
//        System.out.println(agentInfo.getTime() + ", " + this.me + ", buildingsInDir" + dir + ":   " + buildingsInDir);
        return buildingsInDir;
    }
//	/**
//	 * When in a road, we use the road as the separator, defining L and R as 0,1.
//	 * @param dir
//	 * @return
//	 */
//	public Set<EntityID> getBuilldingsInLR(int dir, EntityID position) {
//		Set<EntityID> buildingsInDir = new FastSet<>();
//		Road road = (Road)this.getEntity(position);
//		List<Edge> roadEdge = road.getEdges();
//		int x1, y1, x2, y2;
//		double theta;
//		for(Edge edge : roadEdge) {
//			if(!edge.isPassable()) {
//				x1 = edge.getStartX(); y1 = edge.getStartY();
//				x2 = edge.getEndX(); y2 = edge.getEndY();
//				theta = Math.atan((double)(y2-y1)/(x2-x1));
//				break;
//			}
//		}
//
//		System.out.println(position + ", the edges: " + roadEdge);
//		return null;
//	}
//		Pair<Integer, Integer> location = this.getEntity(position).getLocation(this);
//		int x = location.first();
//		int y = location.second();
//		int range = this.getConfig().extinguishableDistance;
//		Collection<StandardEntity> entities;
//		switch(dir) {
//		case 0:
//			entities = this.getObjectsInRectangle(pointLT.x, pointLT.y, pointR.x, pointR.y);
//			break;
//		case 1:
//			entities = this.getObjectsInRectangle(pointT.x, pointT.y, pointRB.x, pointRB.y);
//			break;
//		default:
//			entities = this.getObjectsInRectangle(x-range, y-range, x+range, y+range);
//	}
//
    //local cluster and global map, fire condition, search globally.
    /**
     * used to set the global map's state for selecting particular extinguish strategy
     */
    public enum CurrentState {
        notExplored,
        searched,
        burning,
        extiniguished,
        needResearched
    }

    public void updateCurrentState() {

    }

    public CSUFireClustering getFireClustering() {
        return fireClustering;
    }

    public void setFireClustering(CSUFireClustering fireClustering) {
        this.fireClustering = fireClustering;
    }

    private void loadCND(String fileName) {
        File f = new File(fileName);
        if (!f.exists() || !f.canRead()) {
            processConnected();
            return;
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
//            float rayDens = Float.parseFloat(br.readLine());
            String nl;
            while (null != (nl = br.readLine())) {
                int x = Integer.parseInt(nl);
                int y = Integer.parseInt(br.readLine());
                int quantity = Integer.parseInt(br.readLine());
                double hitRate = Double.parseDouble(br.readLine());
                java.util.List<CSUBuilding> bl = new ArrayList<CSUBuilding>();
                java.util.List<EntityID> bIDs = new ArrayList<EntityID>();
                List<Float> weight = new ArrayList<Float>();
                for (int c = 0; c < quantity; c++) {
                    int ox = Integer.parseInt(br.readLine());
                    int oy = Integer.parseInt(br.readLine());
                    Building building =  getBuildingInPoint(ox, oy);
                    if (building == null) {
                        System.err.println("building not found: " + ox + "," + oy);
                        br.readLine();
                    } else {
                        bl.add(getCsuBuilding(building.getID()));
                        bIDs.add(building.getID());
                        weight.add(Float.parseFloat(br.readLine()));
                    }

                }
                Building b = getBuildingInPoint(x, y);
                CSUBuilding building = getCsuBuilding(b.getID());
                building.setConnectedBuildins(bl);
                building.setConnectedValues(weight);
                building.setHitRate(hitRate);
            }
            br.close();
            System.out.println("Read from file:" + fileName);
        } catch (IOException ex) {
            processConnected();
            ex.printStackTrace();
        }
    }

    private void processConnected() {
        if (CSUConstants.DEBUG_INIT_CND) {
            System.out.println("  Init CND .... ");
        }
        getCsuBuildings().parallelStream().forEach(csuBuilding -> {
            csuBuilding.initWallValue(this);
        });
    }

    private void processConnected(String fileName) throws IOException {
//        System.out.println("  Creating CND Files .... ");
        processConnected();
        File f = new File(fileName);

        f.delete();
        f.createNewFile();

        final BufferedWriter bw = new BufferedWriter(new FileWriter(f));
//        bw.write(rayRate + "\n");
        getCsuBuildings().forEach(csuBuilding -> {

            try {
                bw.write(csuBuilding.getSelfBuilding().getX() + "\n");
                bw.write(csuBuilding.getSelfBuilding().getY() + "\n");
                bw.write(csuBuilding.getConnectedBuildings().size() + "\n");

                bw.write(csuBuilding.getHitRate() + "\n");

                for (int c = 0; c < csuBuilding.getConnectedBuildings().size(); c++) {
                    CSUBuilding building = csuBuilding.getConnectedBuildings().get(c);
                    Float val = csuBuilding.getConnectedValues().get(c);
                    bw.write(building.getSelfBuilding().getX() + "\n");
                    bw.write(building.getSelfBuilding().getY() + "\n");
                    bw.write(val + "\n");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        });

        bw.close();

        System.out.println("CND is created.");
    }
}
