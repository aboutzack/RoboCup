package mrl_2019.complex.firebrigade;

import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import javolution.util.FastMap;
import mrl_2019.MRLConstants;
import mrl_2019.algorithm.clustering.MrlFireClustering;
import mrl_2019.viewer.MrlPersonalData;
import mrl_2019.world.MrlWorldHelper;
import mrl_2019.world.entity.MrlBuilding;
import mrl_2019.world.entity.MrlRoad;
import mrl_2019.world.helper.PropertyHelper;
import mrl_2019.world.helper.RoadHelper;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User: roohi
 * Date: 2/17/11
 * Time: 7:40 PM
 */
public class MrlFireBrigadeWorld extends MrlWorldHelper {
    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(MrlFireBrigadeWorld.class);
    protected RoadHelper roadHelper;
    protected Simulator simulator;
    //    private WaterCoolingEstimator coolingEstimator;
    private Map<EntityID, EntityID> gotoMap = new FastMap<EntityID, EntityID>();

    //----------------- connection value ---------------
    private boolean isPolyLoaded;
    private float rayRate = 0.0025f;
    //--------------------------------------------------

    private int maxWater;
    private int waterRefillRate;
    private int waterRefillRateInHydrant;
    private boolean isVisibilityAreaDataLoaded;
    private boolean isBorderEntitiesDataLoaded;
    private String fileName;
    private MrlFireClustering fireClustering;


    public MrlFireBrigadeWorld(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, DevelopData developData) {
        super(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);

//        createClusterManager();
        fileName = getMapName() + ".rays";
    }

    private void prepareFirebrigadeWorld(ScenarioInfo scenarioInfo) {
        isVisibilityAreaDataLoaded = false;
        isBorderEntitiesDataLoaded = false;
        roadHelper = getHelper(RoadHelper.class);

        //----------------- connection value ---------------
        initSimulator();


//        coolingEstimator = new WaterCoolingEstimator();

        setMaxWater(scenarioInfo.getFireTankMaximum());
        int refugeRefillRateTemp = MRLConstants.WATER_REFILL_RATE;
        try {
            refugeRefillRateTemp = scenarioInfo.getRawConfig().getIntValue(MRLConstants.WATER_REFILL_RATE_KEY);
            isWaterRefillRateInRefugeSet = true;
        } catch (NoSuchConfigOptionException ignored) {
            isWaterRefillRateInRefugeSet = false;
        }
        setWaterRefillRate(refugeRefillRateTemp);//It can not be reached from config.getIntValue(WATER_REFILL_RATE_KEY);

        int hydrantRefillRateTemp = MRLConstants.WATER_REFILL_RATE_IN_HYDRANT;
        try {
            hydrantRefillRateTemp = scenarioInfo.getRawConfig().getIntValue(MRLConstants.WATER_REFILL_HYDRANT_RATE_KEY);
            isWaterRefillRateInHydrantSet = true;
        } catch (NoSuchConfigOptionException ignored) {
            isWaterRefillRateInHydrantSet = false;
        }
        setWaterRefillRateInHydrant(hydrantRefillRateTemp);
        setBorderBuildings();


//        MrlPersonalData.VIEWER_DATA.setExtinguishRange(getMaxExtinguishDistance());
        //call process area visibility
//   ProcessAreaVisibility.process(this, config);
    }


    @Override
    public MrlWorldHelper precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }

        fireClustering = new MrlFireClustering(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);

        try {
            long before = System.currentTimeMillis();
            createCND(PrecomputeData.PRECOMP_DATA_DIR.getAbsolutePath() + File.separator + fileName);
            long after = System.currentTimeMillis();
            MrlPersonalData.VIEWER_DATA.print("Creation of cnd took " + (after - before) + "ms");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }

    @Override
    public MrlWorldHelper resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
        fireClustering = new MrlFireClustering(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);

        readCND(PrecomputeData.PRECOMP_DATA_DIR.getAbsolutePath() + File.separator + fileName);

        prepareFirebrigadeWorld(scenarioInfo);

        return this;
    }

    @Override
    public MrlWorldHelper preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }
        fireClustering = new MrlFireClustering(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);

        readCND(PrecomputeData.PRECOMP_DATA_DIR.getAbsolutePath() + File.separator + fileName);

        prepareFirebrigadeWorld(scenarioInfo);

        return this;
    }


    private void initSimulator() {
        threadPool.submit(() -> {
            simulator = new Simulator(MrlFireBrigadeWorld.this);
        });
    }

    private int lastUpdateTime = 0;

    @Override
    public MrlWorldHelper updateInfo(MessageManager messageManager) {
        if (lastUpdateTime >= getTime()) {
            return this;
        }
        updateBeforeSense();
        super.updateInfo(messageManager);
        updateAfterSense();
        lastUpdateTime = getTime();
        fireClustering.updateInfo(messageManager);
        return this;
    }

    public void updateBeforeSense() {
        if (simulator != null) {
            simulator.update();
        }
    }

    public void updateAfterSense() {

        estimatedBurningBuildings.clear();
        for (MrlBuilding mrlBuilding : getMrlBuildings()) {
            if (mrlBuilding.getEstimatedFieryness() >= 1 && mrlBuilding.getEstimatedFieryness() <= 3) {
                estimatedBurningBuildings.add(mrlBuilding);
            }
        }

    }

    private void readCND(String fileName) {
        File f = new File(fileName);
        if (!f.exists() || !f.canRead()) {
            createCND();
            return;
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            float rayDens = Float.parseFloat(br.readLine());
            String nl;
            while (null != (nl = br.readLine())) {
                int x = Integer.parseInt(nl);
                int y = Integer.parseInt(br.readLine());
                int quantity = Integer.parseInt(br.readLine());
                double hitRate = Double.parseDouble(br.readLine());
                List<MrlBuilding> bl = new ArrayList<MrlBuilding>();
                List<EntityID> bIDs = new ArrayList<EntityID>();
                List<Float> weight = new ArrayList<Float>();
                for (int c = 0; c < quantity; c++) {
                    int ox = Integer.parseInt(br.readLine());
                    int oy = Integer.parseInt(br.readLine());
                    Building building = getBuildingInPoint(ox, oy);
                    if (building == null) {
                        System.err.println("building not found: " + ox + "," + oy);
                        br.readLine();
                    } else {
                        bl.add(getMrlBuilding(building.getID()));
                        bIDs.add(building.getID());
                        weight.add(Float.parseFloat(br.readLine()));
                    }

                }
                Building b = getBuildingInPoint(x, y);
                MrlBuilding building = getMrlBuilding(b.getID());
//            buildingHelper.setConnectedBuildings(b.getID(), bl);
//            buildingHelper.setConnectedValue(b.getID(), weight);
                building.setConnectedBuilding(bl);
                building.setConnectedValues(weight);
                building.setHitRate(hitRate);
//            MrlPersonalData.VIEWER_DATA.setConnectedBuildings(b.getID(), bIDs);
            }
            br.close();
            if (MRLConstants.DEBUG_FIRE_BRIGADE) {
                System.out.println("Read from file:" + fileName);
            }
        } catch (IOException ex) {
            createCND();
            ex.printStackTrace();
        }
    }

    private void createCND() {
        MrlPersonalData.VIEWER_DATA.print("  Init CND .... ");
        getMrlBuildings().parallelStream().forEach(mrlB -> {
            mrlB.initWallValues(this);
            mrlB.cleanup();
        });
    }

    private void createCND(String fileName) throws IOException {
        MrlPersonalData.VIEWER_DATA.print("  Creating CND Files .... ");
        createCND();
        File f = new File(fileName);

        f.delete();
        f.createNewFile();

        final BufferedWriter bw = new BufferedWriter(new FileWriter(f));
        bw.write(rayRate + "\n");
        getMrlBuildings().forEach(mrlB -> {

//        for (MrlBuilding mrlB : getMrlBuildings()) {

            try {

//                mrlB.initWallValues(this);

                bw.write(mrlB.getSelfBuilding().getX() + "\n");
                bw.write(mrlB.getSelfBuilding().getY() + "\n");
                bw.write(mrlB.getConnectedBuilding().size() + "\n");
                bw.write(mrlB.getHitRate() + "\n");

                for (int c = 0; c < mrlB.getConnectedBuilding().size(); c++) {
                    MrlBuilding building = mrlB.getConnectedBuilding().get(c);
                    Float val = mrlB.getConnectedValues().get(c);
                    bw.write(building.getSelfBuilding().getX() + "\n");
                    bw.write(building.getSelfBuilding().getY() + "\n");
                    bw.write(val + "\n");
                }
//                mrlB.cleanup();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        });

        bw.close();

        MrlPersonalData.VIEWER_DATA.print("CND is created.");
    }

    private ExecutorService threadPool = Executors.newCachedThreadPool();

    public void setBorderBuildings() {
        threadPool.submit(() -> {
            long tm1 = System.currentTimeMillis();
            borderBuildings = borderFinder.getBordersOf(0.9);
            long tm2 = System.currentTimeMillis();
            long tm = tm2 - tm1;
            int number = getBuildingIDs().size();
            MrlPersonalData.VIEWER_DATA.print("Create BOM in " + tm + "ms for " + number + " Buildings.");
            setBorderEntitiesDataLoaded(true);
        });

    }


    public int getMaxWater() {
        return maxWater;
    }

    public void setMaxWater(int maxWater) {
        this.maxWater = maxWater;
    }

    public int getWaterRefillRate() {
        return waterRefillRate;
    }

    public int getWaterRefillRateInHydrant() {
        return waterRefillRateInHydrant;
    }

    public void setWaterRefillRate(int waterRefillRate) {
        this.waterRefillRate = waterRefillRate;
    }

    public void setWaterRefillRateInHydrant(int waterRefillRate) {
        this.waterRefillRateInHydrant = waterRefillRate;
    }

    public boolean isPrecomputedDataLoaded() {
        return isVisibilityAreaDataLoaded && isBorderEntitiesDataLoaded;
    }

    public boolean isVisibilityAreaDataLoaded() {
        return isVisibilityAreaDataLoaded;
    }

    public void setProcessVisibilityDataLoaded(boolean isPrecomputedDataLoaded) {
        this.isVisibilityAreaDataLoaded = isPrecomputedDataLoaded;
    }

    public boolean isBorderEntitiesDataLoaded() {
        return isBorderEntitiesDataLoaded;
    }

    public void setBorderEntitiesDataLoaded(boolean isBorderEntitesDataLoaded) {
        this.isBorderEntitiesDataLoaded = isBorderEntitesDataLoaded;
    }

    public int getMaxPower() {
        return scenarioInfo.getFireExtinguishMaxSum();
    }


    public MrlFireClustering getFireClustering() {
        return fireClustering;
    }

    public void setFireClustering(MrlFireClustering fireClustering) {
        this.fireClustering = fireClustering;
    }

    //    private Set<StandardEntity> availableHydrants = new HashSet<>();
    private int lastUpdateHydrants = -1;

    public Set<StandardEntity> getAvailableHydrants() {
        Set<StandardEntity> availableHydrants = new HashSet<>();
        if (lastUpdateHydrants < getTime() && selfHuman instanceof FireBrigade && !getHydrants().isEmpty()) {
            lastUpdateHydrants = getTime();
            availableHydrants.clear();
            availableHydrants.addAll(getHydrants());
            StandardEntity position;
            MrlRoad hydrantMrlRoad;
            PropertyHelper propertyHelper = getHelper(PropertyHelper.class);
            for (StandardEntity fireBrigadeEntity : fireBrigades) {
                if (fireBrigadeEntity.getID().equals(selfHuman.getID())) {
                    continue;
                }
                FireBrigade fireBrigade = (FireBrigade) fireBrigadeEntity;
                if (fireBrigade.isPositionDefined()) {
                    position = worldInfo.getPosition(fireBrigade);
                    if (position instanceof Hydrant) {
                        hydrantMrlRoad = getMrlRoad(position.getID());
                        int agentDataTime = propertyHelper.getEntityLastUpdateTime(fireBrigade);
                        int hydrantSeenTime = hydrantMrlRoad.getLastSeenTime();
                        if (getTime() - agentDataTime > 10 && getTime() - hydrantSeenTime > 10) {
                            continue;
                        }
                        availableHydrants.remove(position);
                    }
                }
            }
        }

        return availableHydrants;
    }
}
